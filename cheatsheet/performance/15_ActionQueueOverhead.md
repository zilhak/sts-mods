# 15. Action Queue 처리 오버헤드

## 문제 발견 위치

**파일**: `GameActionManager.java`
**메서드**:
- `getNextAction()` (209-474)
- `update()` (172-197)
- `cardQueue` 처리 (221-393)

**관련 코드**:
```java
// GameActionManager.java:54-55 - 액션 큐
public ArrayList<CardQueueItem> cardQueue = new ArrayList<>();
public ArrayList<MonsterQueueItem> monsterQueue = new ArrayList<>();

// GameActionManager.java:221-393 - cardQueue 처리
else if (!this.cardQueue.isEmpty()) {
    this.usingCard = true;
    AbstractCard c = ((CardQueueItem)this.cardQueue.get(0)).card;

    // 250ms 소요 가능한 복잡한 로직
    if (c != null && ((CardQueueItem)this.cardQueue.get(0)).randomTarget) {
        ((CardQueueItem)this.cardQueue.get(0)).monster =
            AbstractDungeon.getMonsters().getRandomMonster(null, true,
                AbstractDungeon.cardRandomRng);
    }

    // 모든 Power 순회
    for (AbstractPower p : AbstractDungeon.player.powers) {
        p.onPlayCard(((CardQueueItem)this.cardQueue.get(0)).card,
                    ((CardQueueItem)this.cardQueue.get(0)).monster);
    }

    // 모든 몬스터의 모든 Power 순회
    for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
        for (AbstractPower p : m.powers) {
            p.onPlayCard(((CardQueueItem)this.cardQueue.get(0)).card,
                        ((CardQueueItem)this.cardQueue.get(0)).monster);
        }
    }

    // 모든 Relic 순회
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onPlayCard(((CardQueueItem)this.cardQueue.get(0)).card,
                    ((CardQueueItem)this.cardQueue.get(0)).monster);
    }

    // 손패, 버리기 덱, 뽑기 덱 전체 순회
    for (AbstractCard card : AbstractDungeon.player.hand.group) {
        card.onPlayCard(((CardQueueItem)this.cardQueue.get(0)).card,
                       ((CardQueueItem)this.cardQueue.get(0)).monster);
    }
    for (AbstractCard card : AbstractDungeon.player.discardPile.group) {
        card.onPlayCard(...);
    }
    for (AbstractCard card : AbstractDungeon.player.drawPile.group) {
        card.onPlayCard(...);
    }

    this.cardQueue.remove(0);  // ArrayList의 첫 요소 제거 - O(n) 복잡도!
}
```

## 문제 설명

### 1. 중복된 큐 항목 접근

**문제 코드**:
```java
// cardQueue.get(0)를 30번 이상 반복 호출!
AbstractCard c = ((CardQueueItem)this.cardQueue.get(0)).card;

if (((CardQueueItem)this.cardQueue.get(0)).randomTarget) {
    ((CardQueueItem)this.cardQueue.get(0)).monster = ...
}

((CardQueueItem)this.cardQueue.get(0)).card.energyOnUse = ...
((CardQueueItem)this.cardQueue.get(0)).card.ignoreEnergyOnUse = ...

for (...) {
    p.onPlayCard(((CardQueueItem)this.cardQueue.get(0)).card,
                ((CardQueueItem)this.cardQueue.get(0)).monster);
}
```

**문제점**:
- 같은 객체를 30+ 번 접근
- 매번 ArrayList 인덱싱 + 타입 캐스팅
- 가독성 저하

### 2. ArrayList.remove(0)의 O(n) 복잡도

```java
// GameActionManager.java:386
this.cardQueue.remove(0);  // 첫 요소 제거 → 나머지 모두 이동!
```

**문제점**:
- ArrayList는 배열 기반
- 첫 요소 제거 시 모든 요소를 한 칸씩 이동
- O(n) 복잡도 (n = 큐 크기)

**영향**:
```
Necronomicon (카드 2회 실행):
- cardQueue 크기: 10개
- remove(0) 호출: 10회
- 총 이동 연산: 45회 (10+9+8+...+1)

Omniscience 체인:
- cardQueue 크기: 20개
- remove(0) 호출: 20회
- 총 이동 연산: 190회!
```

### 3. 과도한 순회

```java
// 카드 플레이 시마다 실행되는 순회들
for (AbstractPower p : player.powers)           // ~10개
for (AbstractMonster m : monsters)              // ~3개
    for (AbstractPower p : m.powers)            // ~5개 × 3 = 15개
for (AbstractRelic r : player.relics)           // ~15개
for (AbstractCard c : player.hand.group)        // ~10개
for (AbstractCard c : player.discardPile.group) // ~30개
for (AbstractCard c : player.drawPile.group)    // ~20개

총: 10 + 15 + 15 + 10 + 30 + 20 = 100회 순회
```

**문제점**:
- 매 카드마다 100+ 객체 순회
- 대부분 카드는 onPlayCard() 구현 없음 (빈 메서드 호출)
- 필터링 없이 전체 순회

### 4. 반복적인 몬스터 유효성 체크

```java
// GameActionManager.java:337-355
if (((CardQueueItem)this.cardQueue.get(0)).card.target == AbstractCard.CardTarget.ENEMY &&
    (((CardQueueItem)this.cardQueue.get(0)).monster == null ||
     ((CardQueueItem)this.cardQueue.get(0)).monster.isDeadOrEscaped())) {

    // 몬스터 죽었으면 카드 제거
    for (Iterator<AbstractCard> i = AbstractDungeon.player.limbo.group.iterator();
         i.hasNext(); ) {
        // ...
    }
}
```

**문제점**:
- 매 카드마다 몬스터 상태 체크
- 죽은 몬스터로 타겟팅된 카드 처리에 추가 순회

### 5. 상태 머신 복잡도

```java
// GameActionManager.java:172-197 - update()
public void update() {
    switch (this.phase) {
        case WAITING_ON_USER:
            getNextAction();
            return;
        case EXECUTING_ACTIONS:
            if (this.currentAction != null && !this.currentAction.isDone) {
                this.currentAction.update();
            } else {
                this.previousAction = this.currentAction;
                this.currentAction = null;
                getNextAction();

                if (this.currentAction == null &&
                    (AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT &&
                    !this.usingCard) {
                    this.phase = Phase.WAITING_ON_USER;
                    AbstractDungeon.player.hand.refreshHandLayout();
                    this.hasControl = false;
                }
                this.usingCard = false;
            }
            return;
    }
}
```

**문제점**: 복잡한 상태 전환 로직, 중첩된 조건문

## 원인 분석

### 1. 잘못된 자료구조 선택

```java
// ArrayList는 큐에 부적합
public ArrayList<CardQueueItem> cardQueue = new ArrayList<>();

// Queue를 사용해야 함
public Queue<CardQueueItem> cardQueue = new ArrayDeque<>();
```

**이유**:
- ArrayList: 랜덤 액세스 최적화, 삽입/삭제는 느림
- ArrayDeque: FIFO 큐에 최적화, O(1) 삽입/삭제

### 2. 캐싱 부재

```java
// 같은 값을 30번 접근
((CardQueueItem)this.cardQueue.get(0)).card
((CardQueueItem)this.cardQueue.get(0)).monster

// 한 번만 가져오면 됨
CardQueueItem item = this.cardQueue.get(0);
AbstractCard card = item.card;
AbstractMonster monster = item.monster;
```

### 3. 불필요한 순회

```java
// 대부분 카드는 onPlayCard() 없음
for (AbstractCard card : AbstractDungeon.player.hand.group) {
    card.onPlayCard(...);  // 기본 구현은 빈 메서드
}

// 해당 메서드 구현한 카드만 추적하면 됨
Set<AbstractCard> cardsWithOnPlayCard = ...;
for (AbstractCard card : cardsWithOnPlayCard) {
    card.onPlayCard(...);
}
```

## 해결 방법

### Solution 1: 로컬 변수 캐싱

```java
@SpirePatch(
    clz = GameActionManager.class,
    method = "getNextAction"
)
public class CardQueueCachingPatch {
    @SpirePatch(
        clz = GameActionManager.class,
        method = "getNextAction"
    )
    public static class LocalizePatch {
        @SpireInsertPatch(
            locator = CardQueueLocator.class
        )
        public static SpireReturn<Void> Insert(GameActionManager __instance) {
            if (__instance.cardQueue.isEmpty()) {
                return SpireReturn.Continue();
            }

            // 한 번만 가져와서 재사용
            CardQueueItem item = __instance.cardQueue.get(0);
            AbstractCard card = item.card;
            AbstractMonster monster = item.monster;

            // 처리 로직에 캐시된 변수 사용
            if (card != null && item.randomTarget) {
                monster = AbstractDungeon.getMonsters().getRandomMonster(
                    null, true, AbstractDungeon.cardRandomRng);
                item.monster = monster;
            }

            // ... 나머지 로직

            // 완료 후 제거
            __instance.cardQueue.remove(0);
            return SpireReturn.Return(null);
        }
    }

    private static class CardQueueLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctBehavior) throws Exception {
            Matcher matcher = new Matcher.FieldAccessMatcher(
                GameActionManager.class, "cardQueue");
            return LineFinder.findInOrder(ctBehavior, matcher);
        }
    }
}
```

### Solution 2: ArrayDeque 사용

```java
@SpirePatch(
    clz = GameActionManager.class,
    method = SpirePatch.CLASS
)
public class CardQueueReplacementPatch {
    // ArrayList를 ArrayDeque로 교체
    public static SpireField<ArrayDeque<CardQueueItem>> fastCardQueue =
        new SpireField<>(() -> new ArrayDeque<>());

    public static SpireField<ArrayDeque<MonsterQueueItem>> fastMonsterQueue =
        new SpireField<>(() -> new ArrayDeque<>());
}

@SpirePatch(
    clz = GameActionManager.class,
    method = "addCardQueueItem",
    paramtypez = {CardQueueItem.class, boolean.class}
)
public class AddCardQueueItemPatch {
    @SpirePrefix
    public static SpireReturn<Void> Prefix(
            GameActionManager __instance,
            CardQueueItem c,
            boolean inFrontOfQueue) {

        ArrayDeque<CardQueueItem> deque =
            CardQueueReplacementPatch.fastCardQueue.get(__instance);

        if (inFrontOfQueue && !deque.isEmpty()) {
            // 두 번째 위치에 삽입
            CardQueueItem first = deque.pollFirst();
            deque.addFirst(c);
            deque.addFirst(first);
        } else {
            deque.addLast(c);
        }

        return SpireReturn.Return(null);
    }
}

@SpirePatch(
    clz = GameActionManager.class,
    method = "getNextAction"
)
public class GetNextActionDequePatch {
    @SpirePrefix
    public static void Prefix(GameActionManager __instance) {
        // Deque에서 가져오기 - O(1)!
        ArrayDeque<CardQueueItem> deque =
            CardQueueReplacementPatch.fastCardQueue.get(__instance);

        if (!deque.isEmpty()) {
            CardQueueItem item = deque.pollFirst();  // O(1) 제거
            // ... 처리 로직
        }
    }
}
```

### Solution 3: 순회 최적화 - 이벤트 리스너

```java
@SpirePatch(
    clz = GameActionManager.class,
    method = SpirePatch.CLASS
)
public class OnPlayCardListenersPatch {
    // onPlayCard 구현한 카드만 추적
    public static SpireField<Set<AbstractCard>> activeListeners =
        new SpireField<>(() -> new HashSet<>());
}

@SpirePatch(
    clz = AbstractCard.class,
    method = "onPlayCard"
)
public class RegisterListenerPatch {
    @SpirePostfix
    public static void Postfix(AbstractCard __instance) {
        // 기본 구현이 아닌 카드만 등록
        if (hasCustomOnPlayCard(__instance)) {
            OnPlayCardListenersPatch.activeListeners
                .get(AbstractDungeon.actionManager)
                .add(__instance);
        }
    }

    private static boolean hasCustomOnPlayCard(AbstractCard card) {
        try {
            Method method = card.getClass().getMethod(
                "onPlayCard", AbstractCard.class, AbstractMonster.class);

            // 선언한 클래스가 AbstractCard가 아니면 커스텀 구현
            return method.getDeclaringClass() != AbstractCard.class;

        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}

@SpirePatch(
    clz = GameActionManager.class,
    method = "getNextAction"
)
public class OptimizedOnPlayCardPatch {
    @SpireInsertPatch(
        locator = OnPlayCardLocator.class
    )
    public static SpireReturn<Void> Insert(GameActionManager __instance) {
        CardQueueItem item = __instance.cardQueue.get(0);

        // 리스너만 순회 - 대부분 0-3개!
        Set<AbstractCard> listeners =
            OnPlayCardListenersPatch.activeListeners.get(__instance);

        for (AbstractCard listener : listeners) {
            listener.onPlayCard(item.card, item.monster);
        }

        // 원본 순회 스킵
        return SpireReturn.Return(null);
    }
}
```

### Solution 4: Batch 검증

```java
@SpirePatch(
    clz = GameActionManager.class,
    method = "getNextAction"
)
public class BatchMonsterCheckPatch {
    private static boolean allMonstersDead = false;
    private static long lastCheckTime = 0;

    @SpirePrefix
    public static void Prefix() {
        long now = System.currentTimeMillis();

        // 100ms마다 한 번만 체크
        if (now - lastCheckTime > 100) {
            allMonstersDead = AbstractDungeon.getMonsters()
                .areMonstersBasicallyDead();
            lastCheckTime = now;
        }
    }

    public static boolean areMonstersDead() {
        return allMonstersDead;
    }
}

@SpirePatch(
    clz = GameActionManager.class,
    method = "getNextAction"
)
public class SkipDeadMonsterTargetingPatch {
    @SpireInsertPatch(
        locator = MonsterTargetCheckLocator.class
    )
    public static SpireReturn<Void> Insert(GameActionManager __instance) {
        // 캐시된 결과 사용
        if (BatchMonsterCheckPatch.areMonstersDead()) {
            // 카드 큐 정리
            __instance.cardQueue.clear();
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}
```

### Solution 5: 상태 머신 단순화

```java
@SpirePatch(
    clz = GameActionManager.class,
    method = "update"
)
public class SimplifiedUpdatePatch {
    @SpirePrefix
    public static SpireReturn<Void> Prefix(GameActionManager __instance) {
        // WAITING_ON_USER는 항상 getNextAction()만 호출
        if (__instance.phase == GameActionManager.Phase.WAITING_ON_USER) {
            getNextAction(__instance);
            return SpireReturn.Return(null);
        }

        // EXECUTING_ACTIONS 간소화
        if (__instance.currentAction != null) {
            if (!__instance.currentAction.isDone) {
                __instance.currentAction.update();
                return SpireReturn.Return(null);
            }

            // 액션 완료
            __instance.previousAction = __instance.currentAction;
            __instance.currentAction = null;
        }

        // 다음 액션 가져오기
        getNextAction(__instance);

        // 더 이상 액션 없으면 대기 상태로
        if (__instance.currentAction == null &&
            !__instance.usingCard &&
            isInCombat()) {

            __instance.phase = GameActionManager.Phase.WAITING_ON_USER;
            AbstractDungeon.player.hand.refreshHandLayout();
            __instance.hasControl = false;
        }

        __instance.usingCard = false;
        return SpireReturn.Return(null);
    }

    private static boolean isInCombat() {
        return AbstractDungeon.getCurrRoom().phase ==
            AbstractRoom.RoomPhase.COMBAT;
    }

    private static void getNextAction(GameActionManager manager) {
        // 원본 getNextAction() 호출
        // ...
    }
}
```

## 성능 개선 효과

### Before vs After

**Before**:
```
카드 플레이 (Normal 덱):
- cardQueue.get(0): 30회 × 5ns = 150ns
- Power 순회: 10개 × 100ns = 1000ns
- Monster Power 순회: 3 × 5 × 100ns = 1500ns
- Relic 순회: 15개 × 100ns = 1500ns
- 손패 순회: 10개 × 50ns = 500ns
- 버리기덱 순회: 30개 × 50ns = 1500ns
- 뽑기덱 순회: 20개 × 50ns = 1000ns
- cardQueue.remove(0): O(n) = 500ns
총: ~7650ns per card

Necronomicon (카드 2회 실행):
- 위 × 2 = 15300ns
- 큐 크기 증가로 remove(0) 더 느림
총: ~16000ns
```

**After (최적화)**:
```
카드 플레이 (캐싱 + Deque + 리스너):
- 로컬 변수 캐싱: ~0ns (컴파일러 최적화)
- Power 순회: 10개 × 100ns = 1000ns
- Monster Power 순회: 1500ns (동일)
- Relic 순회: 1500ns (동일)
- 리스너 카드만 순회: 2개 × 50ns = 100ns
- deque.pollFirst(): O(1) = 10ns
총: ~4110ns per card

Necronomicon:
- 위 × 2 = 8220ns
총: ~8220ns

개선: 48-49% 감소
```

### 복잡한 시나리오

```
Omniscience → Omniscience 체인:
Before:
- cardQueue 크기: 20개
- 각 카드: 7650ns
- remove(0) 누적: 190회 이동 = 5000ns
총: (7650 × 20) + 5000 = 158000ns

After:
- cardQueue 크기: 20개
- 각 카드: 4110ns
- pollFirst() 누적: 20 × 10ns = 200ns
총: (4110 × 20) + 200 = 82400ns

개선: 48% 감소
```

## 주의사항

### 1. ArrayDeque 호환성

```java
// 일부 코드는 ArrayList 특정 메서드 사용
// 예: cardQueue.get(1)로 두 번째 요소 접근

// Adapter 패턴 필요
public class DequeAdapter extends ArrayList<CardQueueItem> {
    private ArrayDeque<CardQueueItem> deque = new ArrayDeque<>();

    @Override
    public CardQueueItem get(int index) {
        // Deque를 임시 리스트로 변환
        return new ArrayList<>(deque).get(index);
    }

    @Override
    public void add(int index, CardQueueItem item) {
        // 특정 위치 삽입 지원
    }
}
```

### 2. 리스너 등록 타이밍

```java
// 카드가 손패에 추가될 때 등록
@SpirePatch(clz = CardGroup.class, method = "addToHand")
public class RegisterOnAddPatch {
    @SpirePostfix
    public static void Postfix(AbstractCard c) {
        if (hasCustomOnPlayCard(c)) {
            OnPlayCardListenersPatch.activeListeners
                .get(AbstractDungeon.actionManager)
                .add(c);
        }
    }
}

// 카드가 제거될 때 해제
@SpirePatch(clz = CardGroup.class, method = "removeCard")
public class UnregisterOnRemovePatch {
    @SpirePostfix
    public static void Postfix(AbstractCard c) {
        OnPlayCardListenersPatch.activeListeners
            .get(AbstractDungeon.actionManager)
            .remove(c);
    }
}
```

### 3. 큐 동기화

```java
// fastCardQueue와 원본 cardQueue 동기화 유지
@SpirePatch(clz = GameActionManager.class, method = "clear")
public class ClearBothQueuesPatch {
    @SpirePostfix
    public static void Postfix(GameActionManager __instance) {
        CardQueueReplacementPatch.fastCardQueue
            .get(__instance).clear();
    }
}
```

### 4. 모드 호환성

```java
// 다른 모드가 cardQueue에 직접 접근할 수 있음
// Reflection으로 cardQueue 접근 감지

@SpirePatch(clz = GameActionManager.class, method = SpirePatch.CLASS)
public class CardQueueSyncPatch {
    // 원본 cardQueue 변경 감지
    @SpirePatch(clz = ArrayList.class, method = "add")
    public static class SyncOnAddPatch {
        @SpirePostfix
        public static void Postfix(ArrayList __instance, Object item) {
            if (isCardQueue(__instance)) {
                syncToDeque(__instance);
            }
        }
    }

    private static void syncToDeque(ArrayList queue) {
        // ArrayList → Deque 동기화
    }
}
```

## 추가 최적화 아이디어

### 1. 우선순위 큐

```java
// 특정 카드는 우선 실행 (예: Shiv)
PriorityQueue<CardQueueItem> priorityQueue = new PriorityQueue<>(
    (a, b) -> Integer.compare(a.priority, b.priority)
);
```

### 2. 큐 미리 검증

```java
// 큐에 추가하기 전에 검증
public void addCardQueueItem(CardQueueItem item) {
    if (!item.card.canUse(...)) {
        return;  // 추가하지 않음
    }
    cardQueue.add(item);
}
```

### 3. Batch 처리

```java
// 연속된 같은 카드는 batch 처리
if (nextCard.isSameAs(currentCard)) {
    batchCount++;
} else {
    processBatch(currentCard, batchCount);
    currentCard = nextCard;
    batchCount = 1;
}
```

## 참고 자료

- `GameActionManager.java`: 209-474 (getNextAction)
- `GameActionManager.java`: 172-197 (update)
- `GameActionManager.java`: 100-135 (큐 관리 메서드)
- `CardQueueItem.java`: 카드 큐 아이템 구조

## 관련 이슈

- **13_CardDamageRecalculation.md**: applyPowers 최적화
- **14_CardGroupSorting.md**: CardGroup 리스트 연산
- **06_QueueMergeWithIterator.md**: 큐 순회 패턴
