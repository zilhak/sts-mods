# 폭발 (Burst)

## 기본 정보

**클래스명**: `BurstPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.BurstPower`
**ID**: `"Burst"`
**타입**: 버프
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 다음 N개의 스킬 카드를 2번 사용
**수치 설명**: `amount`는 복제 가능한 스킬 카드 개수
**적용 시점**: 스킬 카드 사용 시 (onUseCard)
**지속 시간**: 턴 종료 시 제거

**메커니즘**:
- 스킬 카드만 복제 (공격/파워 제외)
- 복제된 카드는 같은 타겟에 즉시 재사용됨
- 복제 시마다 amount 감소, 0이 되면 즉시 제거
- 턴 종료 시 남은 스택 관계없이 제거

**제외 대상**:
- `purgeOnUse = true` 카드 (소멸 카드)는 복제되지 않음
- 공격 카드 및 파워 카드는 복제되지 않음

---

## 코드 분석

### 생성자
```java
public BurstPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Burst";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("burst");
}
```

**주요 파라미터**:
- `owner`: Burst를 가진 대상 (플레이어)
- `amount`: 복제 가능한 스킬 카드 개수 (보통 1 또는 2)

### 핵심 메서드

**onUseCard()**: 스킬 카드 사용 시 복제 로직

```java
public void onUseCard(AbstractCard card, UseCardAction action) {
    // 복제 조건: 소멸 카드가 아니고, 스킬이고, 스택이 남아있음
    if (!card.purgeOnUse
        && card.type == AbstractCard.CardType.SKILL
        && this.amount > 0) {

        flash();  // 시각 효과

        AbstractMonster m = null;
        if (action.target != null) {
            m = (AbstractMonster)action.target;  // 타겟 저장
        }

        // 카드 복제 생성
        AbstractCard tmp = card.makeSameInstanceOf();
        AbstractDungeon.player.limbo.addToBottom(tmp);

        // 복제 카드 위치 설정
        tmp.current_x = card.current_x;
        tmp.current_y = card.current_y;
        tmp.target_x = Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
        tmp.target_y = Settings.HEIGHT / 2.0F;

        // 타겟에 대한 대미지/Block 재계산
        if (m != null) {
            tmp.calculateCardDamage(m);
        }

        // 복제 카드 사용 후 소진 설정
        tmp.purgeOnUse = true;

        // 카드 큐에 추가 (즉시 재사용)
        AbstractDungeon.actionManager.addCardQueueItem(
            new CardQueueItem(tmp, m, card.energyOnUse, true, true),
            true
        );

        // 스택 감소 및 제거 체크
        this.amount--;
        if (this.amount == 0) {
            addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Burst"));
        }
    }
}
```

**중요 로직**:
1. **스킬만 체크**: `card.type == AbstractCard.CardType.SKILL`
2. **즉시 스택 감소**: 복제 후 바로 `amount--`
3. **0 스택 시 즉시 제거**: 턴 종료를 기다리지 않음
4. **makeSameInstanceOf()**: 카드의 완전한 복사본 생성
5. **purgeOnUse = true**: 복제본은 사용 후 게임에서 제거됨

**atEndOfTurn()**: 턴 종료 시 제거

```java
public void atEndOfTurn(boolean isPlayer) {
    if (isPlayer) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Burst"));
    }
}
```

**특징**: 스택이 남아있어도 턴 종료 시 무조건 제거

### 설명 업데이트

```java
public void updateDescription() {
    if (this.amount == 1) {
        this.description = DESCRIPTIONS[0];  // "다음 스킬을 2번 사용"
    } else {
        this.description = DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];  // "다음 N개 스킬을 2번 사용"
    }
}
```

---

## 수정 방법

### 모든 카드 타입 복제

**변경 대상**: 카드 타입 체크
**목적**: 스킬뿐 아니라 공격/파워도 복제

```java
@SpirePatch(
    clz = BurstPower.class,
    method = "onUseCard"
)
public static class AllCardsBurstPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BurstPower __instance, AbstractCard card, UseCardAction action) {
        // 스킬 체크 제거: 모든 카드 타입 복제
        if (!card.purgeOnUse && __instance.amount > 0) {
            __instance.flash();

            AbstractMonster m = null;
            if (action.target != null) {
                m = (AbstractMonster)action.target;
            }

            AbstractCard tmp = card.makeSameInstanceOf();
            AbstractDungeon.player.limbo.addToBottom(tmp);
            tmp.current_x = card.current_x;
            tmp.current_y = card.current_y;
            tmp.target_x = Settings.WIDTH / 2.0F - 300.0F * Settings.scale;
            tmp.target_y = Settings.HEIGHT / 2.0F;

            if (m != null) {
                tmp.calculateCardDamage(m);
            }

            tmp.purgeOnUse = true;
            AbstractDungeon.actionManager.addCardQueueItem(
                new CardQueueItem(tmp, m, card.energyOnUse, true, true),
                true
            );

            __instance.amount--;
            if (__instance.amount == 0) {
                addToTop(new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Burst"));
            }
        }

        return SpireReturn.Return(null);
    }
}
```

### 턴 종료 시 제거 방지 (영구 Burst)

**변경 대상**: atEndOfTurn 메서드
**목적**: 스택이 남아있으면 다음 턴에도 유지

```java
@SpirePatch(
    clz = BurstPower.class,
    method = "atEndOfTurn"
)
public static class PersistentBurstPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BurstPower __instance, boolean isPlayer) {
        // 스택이 0이 아니면 제거하지 않음
        if (__instance.amount > 0) {
            return SpireReturn.Return(null);
        }

        // 원래 로직 계속 진행 (제거)
        return SpireReturn.Continue();
    }
}
```

### 복제 횟수 증가 (3배 사용)

**목적**: 2번이 아닌 3번 사용

```java
@SpirePatch(
    clz = BurstPower.class,
    method = "onUseCard"
)
public static class TripleBurstPatch {
    @SpirePostfixPatch
    public static void Postfix(BurstPower __instance, AbstractCard card, UseCardAction action) {
        // 스킬 카드이고 복제가 발동했으면 추가 1번 더 복제
        if (card.type == AbstractCard.CardType.SKILL && !card.purgeOnUse) {
            AbstractMonster m = null;
            if (action.target != null) {
                m = (AbstractMonster)action.target;
            }

            AbstractCard tmp = card.makeSameInstanceOf();
            AbstractDungeon.player.limbo.addToBottom(tmp);
            tmp.current_x = card.current_x;
            tmp.current_y = card.current_y;
            tmp.purgeOnUse = true;

            if (m != null) {
                tmp.calculateCardDamage(m);
            }

            AbstractDungeon.actionManager.addCardQueueItem(
                new CardQueueItem(tmp, m, card.energyOnUse, true, true),
                true
            );
        }
    }
}
```

### 스택 소모 방지

**목적**: 복제해도 스택이 감소하지 않음 (무제한 사용)

```java
@SpirePatch(
    clz = BurstPower.class,
    method = "onUseCard"
)
public static class InfiniteBurstPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(BurstPower __instance) {
        // amount 감소 및 제거 로직 스킵
        return SpireReturn.Return(null);
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                BurstPower.class, "amount"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Burst (사일런트 - 스킬 카드)
- Burst+ (업그레이드 버전, 2개 복제)

**시너지 카드 (스킬)**:
- **방어 스킬**: Defend, Acrobatics, Backflip (2배 Block)
- **드로우 스킬**: Prepared, Escape Plan (2배 드로우)
- **독 스킬**: Deadly Poison, Bouncing Flask (2배 독)
- **약화 스킬**: Crippling Cloud, Leg Sweep (2배 디버프)
- **0코스트 스킬**: Finesse, Backstab (무료로 2번)

**관련 파워**:
- **Double Tap**: 다음 공격만 2번 사용 (공격 버전)
- **Echo Form**: 모든 카드 복제 (스킬 제한 없음)
- Burst: 스킬만 복제 (턴 제한 있음)

---

## 참고사항

1. **스킬만 복제**: `card.type == AbstractCard.CardType.SKILL` 체크 필수
2. **즉시 스택 감소**: 복제 후 바로 `amount--` → 0 스택 시 즉시 제거
3. **턴 종료 시 제거**: 스택이 남아있어도 무조건 제거됨
4. **purgeOnUse 체크**: 소멸 카드는 복제되지 않음
5. **에너지 소비 없음**: 복제된 카드는 에너지를 소비하지 않음
6. **타겟 유지**: 원본 카드의 타겟을 복제본도 그대로 사용
7. **limbo 영역**: 복제 카드는 `limbo` 영역에 임시 저장됨
8. **calculateCardDamage()**: 타겟이 있는 경우 대미지/Block 재계산
9. **일회성**: Echo Form과 달리 한 턴만 지속됨
10. **Burst vs Burst+**:
    - Burst: amount=1 (1개 스킬 복제)
    - Burst+: amount=2 (2개 스킬 복제)
11. **사일런트 전용**: 주로 사일런트가 사용하는 파워 카드
