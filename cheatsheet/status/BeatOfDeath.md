# 죽음의 박동 (Beat of Death)

## 기본 정보

**클래스명**: `BeatOfDeathPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.BeatOfDeathPower`
**ID**: `"BeatOfDeath"`
**타입**: BUFF
**적용 대상**: CorruptHeart (타락한 심장) 전용

---

## 효과

**기본 효과**: 플레이어가 카드를 사용할 때마다 HP 손실 발생
**수치 설명**: `amount`는 카드당 HP 손실량
**적용 시점**: 카드 사용 직후 (onAfterUseCard)

**특수 메커니즘**:
- **대미지 타입**: `DamageInfo.DamageType.THORNS` (가시 대미지)
- **Block 무시**: THORNS 타입이므로 Block으로 막을 수 없음
- **Intangible 적용됨**: Intangible이 있으면 1 대미지로 감소
- **승천 난이도 조정**:
  - 일반 (A0-18): 카드당 1 HP 손실
  - 높은 난이도 (A19+): 카드당 2 HP 손실

---

## 코드 분석

### 생성자
```java
public BeatOfDeathPower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "BeatOfDeath";
    this.owner = owner;
    this.amount = amount;
    this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
    loadRegion("beat");
    this.type = AbstractPower.PowerType.BUFF;  // 보스 입장에서 BUFF
}
```

**주요 파라미터**:
- `owner`: CorruptHeart 보스
- `amount`: 카드당 HP 손실량 (1 또는 2)

### 핵심 메서드

**onAfterUseCard()**: 카드 사용 직후 호출

```java
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    flash();  // 파워 아이콘 반짝임 효과

    // THORNS 타입 대미지로 플레이어에게 HP 손실
    addToBot(new DamageAction(
        AbstractDungeon.player,
        new DamageInfo(this.owner, this.amount, DamageInfo.DamageType.THORNS),
        AbstractGameAction.AttackEffect.BLUNT_LIGHT
    ));

    updateDescription();
}
```

**작동 방식**:
1. 플레이어가 **아무 카드**나 사용 (공격/스킬/파워/저주/상태이상 모두 포함)
2. `onAfterUseCard()` 호출
3. `amount`만큼 THORNS 대미지 발생
4. Block으로 막을 수 없음, Intangible은 1로 감소

**updateDescription()**: 설명 텍스트 업데이트

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    // "카드를 사용할 때마다 {amount}의 HP를 잃습니다."
}
```

---

## 수정 방법

### HP 손실량 변경

**변경 대상**: CorruptHeart 클래스의 `usePreBattleAction()` 메서드
**코드 위치**: CorruptHeart.java 줄 88-94

**예시: HP 손실량을 5로 증가**

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "usePreBattleAction"
)
public static class BeatOfDeathAmountPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(CorruptHeart __instance) {
        int beatAmount = 5;  // 기존 1-2를 5로 변경
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new BeatOfDeathPower(__instance, beatAmount),
                beatAmount)
        );
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                BeatOfDeathPower.class, "POWER_ID"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 특정 카드 타입만 트리거

**예시: 공격 카드만 HP 손실 발생**

```java
@SpirePatch(
    clz = BeatOfDeathPower.class,
    method = "onAfterUseCard"
)
public static class AttackOnlyPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BeatOfDeathPower __instance,
                                           AbstractCard card,
                                           UseCardAction action) {
        // 공격 카드가 아니면 효과 무시
        if (card.type != AbstractCard.CardType.ATTACK) {
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 대미지 타입 변경

**예시: NORMAL 타입으로 변경 (Block으로 막기 가능)**

```java
@SpirePatch(
    clz = BeatOfDeathPower.class,
    method = "onAfterUseCard"
)
public static class NormalDamageTypePatch {
    @SpirePostfixPatch
    public static void Postfix(BeatOfDeathPower __instance,
                               AbstractCard card,
                               UseCardAction action) {
        // THORNS 대미지 액션 제거하고 NORMAL 타입으로 교체
        AbstractDungeon.actionManager.actions.removeIf(a ->
            a instanceof DamageAction &&
            ((DamageAction)a).damageType == DamageInfo.DamageType.THORNS
        );

        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                AbstractDungeon.player,
                new DamageInfo(__instance.owner, __instance.amount,
                              DamageInfo.DamageType.NORMAL),
                AbstractGameAction.AttackEffect.BLUNT_LIGHT
            )
        );
    }
}
```

### 카드 코스트에 비례한 대미지

**예시: 카드 코스트 × amount의 HP 손실**

```java
@SpirePatch(
    clz = BeatOfDeathPower.class,
    method = "onAfterUseCard"
)
public static class CostBasedDamagePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(BeatOfDeathPower __instance,
                                           AbstractCard card,
                                           UseCardAction action) {
        __instance.flash();

        // 카드 코스트에 비례한 대미지 (X 코스트는 0으로 처리)
        int costValue = card.costForTurn >= 0 ? card.costForTurn : 0;
        int damage = __instance.amount * costValue;

        if (damage > 0) {
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    new DamageInfo(__instance.owner, damage,
                                  DamageInfo.DamageType.THORNS),
                    AbstractGameAction.AttackEffect.BLUNT_LIGHT
                )
            );
        }

        return SpireReturn.Return(null);  // 원본 메서드 실행 방지
    }
}
```

---

## 관련 파일

**적용하는 몬스터**:
- **CorruptHeart** (타락한 심장): Act 4 최종 보스 전용

**관련 파워**:
- **InvinciblePower**: CorruptHeart가 함께 사용하는 방어 파워
  - 매 턴 고정된 Block 획득 (200-300)
  - 받는 대미지를 Block처럼 흡수

**승천 난이도별 수치**:
- **A0-18**: 카드당 1 HP 손실
- **A19+**: 카드당 2 HP 손실

**대응 전략**:
- 카드 사용 횟수를 최소화하는 덱 구성 (고대미지 카드 중심)
- Intangible 효과 활용 (1 대미지로 감소)
- HP 회복 카드/유물 준비 (Reaper, Bite, Fairy in a Bottle 등)
- 0코스트 카드 남발 주의 (Defend도 HP 손실 발생)

---

## 참고사항

1. **대미지 타입**: `DamageInfo.DamageType.THORNS`
   - Block으로 막을 수 없음
   - Intangible이 있으면 1로 감소
   - Buffer는 소모됨
2. **모든 카드에 적용**: 공격/스킬/파워/저주/상태이상 모두 HP 손실 발생
3. **Flash 효과**: 카드 사용할 때마다 파워 아이콘이 반짝임
4. **턴 제한 없음**: InvinciblePower와 달리 턴 종료 시 사라지지 않음 (전투 내내 지속)
5. **제거 불가**: 일반적인 디버프 제거 수단으로는 제거 불가능 (보스 전용 고정 파워)
6. **카드 효과와 별개**: 카드 자체의 효과와는 무관하게 추가로 HP 손실 발생
7. **X 코스트 카드**: X 코스트 카드도 1회로 계산 (사용한 에너지양과 무관)
8. **복사 카드**: Echo Form, Omniscience 등으로 복사된 카드도 각각 카운트
9. **Ethereal/Exhaust**: 카드가 소멸되거나 제거되어도 사용한 것으로 카운트
10. **플레이어 전용 패널티**: 몬스터는 카드를 사용하지 않으므로 플레이어만 영향받음
