# 메아리 (Echo Form)

## 기본 정보

**클래스명**: `EchoPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.EchoPower`
**ID**: `"Echo Form"`
**타입**: 버프
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 매 턴 처음 N개의 카드를 2번 사용
**수치 설명**: `amount`는 턴당 복제 가능한 카드 개수
**적용 시점**: 카드 사용 시 (onUseCard)

**메커니즘**:
- 턴당 `amount`개의 카드까지 복제
- 복제된 카드는 같은 타겟에 즉시 재사용됨
- 복제된 카드는 사용 후 소진됨 (원본은 소진되지 않음)

**제외 대상**:
- `purgeOnUse = true` 카드 (소멸 카드)는 복제되지 않음

---

## 코드 분석

### 생성자
```java
public EchoPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Echo Form";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("echo");
}

private int cardsDoubledThisTurn = 0;  // 필드 변수
```

**주요 파라미터**:
- `owner`: Echo Form을 가진 대상 (플레이어)
- `amount`: 턴당 복제 가능한 카드 개수 (보통 1)
- `cardsDoubledThisTurn`: 이번 턴에 복제된 카드 개수 추적

### 핵심 메서드

**atStartOfTurn()**: 턴 시작 시 카운터 초기화

```java
public void atStartOfTurn() {
    this.cardsDoubledThisTurn = 0;  // 매 턴 시작마다 리셋
}
```

**onUseCard()**: 카드 사용 시 복제 로직

```java
public void onUseCard(AbstractCard card, UseCardAction action) {
    // 복제 조건 체크
    if (!card.purgeOnUse  // 소멸 카드가 아니고
        && this.amount > 0  // Echo 스택이 남아있고
        && AbstractDungeon.actionManager.cardsPlayedThisTurn.size() - this.cardsDoubledThisTurn <= this.amount) {

        this.cardsDoubledThisTurn++;  // 복제 카운터 증가
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

        // 타겟에 대한 대미지 재계산
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
    }
}
```

**중요 로직**:
1. 복제 조건: `cardsPlayedThisTurn.size() - cardsDoubledThisTurn <= amount`
   - 실제로 플레이한 카드 개수 = 총 플레이 - 복제된 개수
   - 이 값이 amount 이하일 때만 복제
2. `makeSameInstanceOf()`: 카드의 완전한 복사본 생성
3. `purgeOnUse = true`: 복제본은 사용 후 게임에서 제거됨
4. `addCardQueueItem()`: 카드를 즉시 재사용 큐에 추가

### 설명 업데이트

```java
public void updateDescription() {
    if (this.amount == 1) {
        this.description = DESCRIPTIONS[0];  // "다음 카드를 2번 사용"
    } else {
        this.description = DESCRIPTIONS[1] + this.amount + DESCRIPTIONS[2];  // "다음 N개 카드를 2번 사용"
    }
}
```

---

## 수정 방법

### 복제 횟수 변경

**변경 대상**: 카드 복제 로직
**목적**: 2번이 아닌 3번 사용

```java
@SpirePatch(
    clz = EchoPower.class,
    method = "onUseCard"
)
public static class TripleEchoPatch {
    @SpirePostfixPatch
    public static void Postfix(EchoPower __instance, AbstractCard card, UseCardAction action) {
        // 이미 1번 복제됨, 추가로 1번 더 복제
        if (__instance.cardsDoubledThisTurn > 0 && !card.purgeOnUse) {
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

### 영구 Echo (무제한 복제)

**변경 대상**: 복제 조건 체크
**목적**: 모든 카드를 복제

```java
@SpirePatch(
    clz = EchoPower.class,
    method = "onUseCard"
)
public static class InfiniteEchoPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(EchoPower __instance, AbstractCard card, UseCardAction action) {
        // 조건 체크 제거: 모든 카드 복제
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
        }

        return SpireReturn.Return(null);
    }
}
```

### 특정 카드 타입만 복제

**목적**: 스킬/공격/파워만 복제

```java
@SpirePatch(
    clz = EchoPower.class,
    method = "onUseCard"
)
public static class SkillOnlyEchoPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(EchoPower __instance, AbstractCard card, UseCardAction action) {
        // 스킬만 복제
        if (card.type != AbstractCard.CardType.SKILL) {
            return SpireReturn.Return(null);
        }

        // 원래 로직 계속 진행
        return SpireReturn.Continue();
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Echo Form (디펙트 - 파워 카드)
- Echo Form+ (업그레이드 버전, 코스트 감소)

**시너지 카드**:
- **높은 대미지 카드**: Sunder, Whirlwind, Meteor Strike 등
- **드로우 카드**: Acrobatics, Backflip 등 (2번 드로우)
- **버프 카드**: Inflame, Demon Form 등 (2번 버프)
- **0코스트 카드**: Defend, Zap 등 (무료로 2번 사용)

**관련 파워**:
- **Burst**: 다음 스킬만 2번 사용 (일회성)
- **Double Tap**: 다음 공격만 2번 사용 (일회성)
- Echo Form: 매 턴 모든 카드 복제 (영구)

---

## 참고사항

1. **복제 카운팅 로직**: `cardsPlayedThisTurn.size() - cardsDoubledThisTurn`
   - 복제된 카드도 `cardsPlayedThisTurn`에 포함됨
   - `cardsDoubledThisTurn`을 빼서 실제 플레이한 카드만 계산
2. **purgeOnUse 체크**: 소멸 카드(Miracle, Apparition 등)는 복제되지 않음
3. **에너지 소비 없음**: 복제된 카드는 에너지를 소비하지 않음
   - `card.energyOnUse` 사용: 원본 카드가 소비한 에너지 그대로 사용
4. **타겟 유지**: 원본 카드의 타겟을 복제본도 그대로 사용
5. **limbo 영역**: 복제 카드는 `limbo` 영역에 임시 저장됨
6. **calculateCardDamage()**: 타겟이 있는 경우 대미지 재계산 (파워/버프 반영)
7. **amount 값**: Echo Form 카드는 보통 amount=1 (1개 복제), amount=2면 2개 복제
8. **영구 지속**: 제거되지 않는 영구 버프 (일반적으로)
