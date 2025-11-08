# 무적 (Invincible)

## 기본 정보

**클래스명**: `InvinciblePower`
**전체 경로**: `com.megacrit.cardcrawl.powers.InvinciblePower`
**ID**: `"Invincible"`
**타입**: BUFF (명시적 타입 지정 없음, 보스 전용 파워)
**적용 대상**: CorruptHeart (타락한 심장) 전용

---

## 효과

**기본 효과**: 받는 대미지를 Block처럼 흡수하는 특수 방어막
**수치 설명**: `amount`는 현재 남은 방어막 수치
**적용 시점**: 공격받을 때 (onAttackedToChangeDamage)

**특수 메커니즘**:
- **매 턴 시작 시 완전 복구**: `atStartOfTurn()`에서 `maxAmt`로 리셋
- **대미지 흡수 방식**: 대미지가 남은 방어막보다 크면 방어막 수치만큼만 대미지 입음
- **승천 난이도 조정**:
  - 일반 (A0-18): 300 방어막
  - 높은 난이도 (A19+): 200 방어막 (100 감소)

---

## 코드 분석

### 생성자
```java
public InvinciblePower(AbstractCreature owner, int amount) {
    this.name = NAME;
    this.ID = "Invincible";
    this.owner = owner;
    this.amount = amount;
    this.maxAmt = amount;        // 최대치 저장
    updateDescription();
    loadRegion("heartDef");      // 하트 방어 아이콘
    this.priority = 99;          // 높은 우선순위
}
```

**주요 파라미터**:
- `owner`: CorruptHeart 보스
- `amount`: 초기 방어막 수치 (200 또는 300)
- `maxAmt`: 턴마다 복구할 최대 방어막 수치

### 핵심 메서드

**onAttackedToChangeDamage()**: 공격받을 때 대미지 조정

```java
public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
    // 대미지가 남은 방어막보다 크면 방어막 수치로 제한
    if (damageAmount > this.amount) {
        damageAmount = this.amount;
    }

    // 방어막에서 대미지만큼 차감
    this.amount -= damageAmount;
    if (this.amount < 0) {
        this.amount = 0;
    }

    updateDescription();
    return damageAmount;  // 실제로 받을 대미지 반환
}
```

**작동 예시**:
- 방어막 100 남음, 150 대미지 → 100 대미지만 받음, 방어막 0
- 방어막 100 남음, 50 대미지 → 50 대미지 받음, 방어막 50 남음
- 방어막 0 남음, 100 대미지 → 0 대미지 받음 (무적 상태)

**atStartOfTurn()**: 턴 시작 시 완전 복구

```java
public void atStartOfTurn() {
    this.amount = this.maxAmt;  // 최대치로 복구
    updateDescription();
}
```

**중요**: Block과 달리 매 턴 완전히 리셋됨 (누적 대미지 무시)

### 설명 텍스트 업데이트

**updateDescription()**: 방어막 상태에 따른 설명 변경

```java
public void updateDescription() {
    if (this.amount <= 0) {
        this.description = DESCRIPTIONS[2];  // 방어막 0: "방어막이 깨짐"
    } else {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
        // "NL 모든 대미지를 {amount}까지 흡수합니다."
    }
}
```

---

## 수정 방법

### 방어막 최대치 변경

**변경 대상**: CorruptHeart 클래스의 `usePreBattleAction()` 메서드
**코드 위치**: CorruptHeart.java 줄 84-92

**예시: 방어막을 500으로 증가**

```java
@SpirePatch(
    clz = CorruptHeart.class,
    method = "usePreBattleAction"
)
public static class InvincibleAmountPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(CorruptHeart __instance) {
        int invincibleAmt = 500;  // 기존 200-300을 500으로 변경
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new InvinciblePower(__instance, invincibleAmt),
                invincibleAmt)
        );
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                InvinciblePower.class, "POWER_ID"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 턴당 복구량 조정

**방법 1: 부분 복구로 변경**
```java
@SpirePatch(
    clz = InvinciblePower.class,
    method = "atStartOfTurn"
)
public static class PartialRecoveryPatch {
    @SpirePostfixPatch
    public static void Postfix(InvinciblePower __instance) {
        // 최대치의 50%만 복구
        __instance.amount = __instance.maxAmt / 2;
        __instance.updateDescription();
    }
}
```

**방법 2: 복구량 추가 증가**
```java
@SpirePatch(
    clz = InvinciblePower.class,
    method = "atStartOfTurn"
)
public static class OverhealPatch {
    @SpirePostfixPatch
    public static void Postfix(InvinciblePower __instance) {
        // 최대치보다 더 많이 복구 (150%)
        __instance.amount = (int)(__instance.maxAmt * 1.5f);
        __instance.updateDescription();
    }
}
```

### 대미지 흡수 방식 변경

**예시: 대미지 감소율 적용 (50% 감소)**

```java
@SpirePatch(
    clz = InvinciblePower.class,
    method = "onAttackedToChangeDamage"
)
public static class DamageReductionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(InvinciblePower __instance,
                                              DamageInfo info,
                                              int damageAmount) {
        // 대미지의 50%만 흡수
        int reducedDamage = (int)(damageAmount * 0.5f);

        if (reducedDamage > __instance.amount) {
            reducedDamage = __instance.amount;
        }

        __instance.amount -= reducedDamage;
        if (__instance.amount < 0) {
            __instance.amount = 0;
        }

        __instance.updateDescription();
        return SpireReturn.Return(reducedDamage);
    }
}
```

---

## 관련 파일

**적용하는 몬스터**:
- **CorruptHeart** (타락한 심장): Act 4 최종 보스 전용

**관련 파워**:
- **BeatOfDeathPower**: CorruptHeart가 함께 사용하는 또 다른 파워
  - 카드 사용 시 플레이어에게 HP 손실 (THORNS 타입 대미지)
  - A19+에서는 카드당 2 대미지 (기본 1 대미지)

**승천 난이도별 수치**:
- **A0-18**: 300 방어막, 카드당 1 대미지
- **A19+**: 200 방어막, 카드당 2 대미지

---

## 참고사항

1. **우선순위**: `priority = 99`로 매우 높음 → 대부분의 다른 파워보다 먼저 계산됨
2. **Block과의 차이점**:
   - Block: 턴 종료 시 사라짐, 누적 가능
   - Invincible: 턴 시작 시 완전 복구, 대미지 상한 제한
3. **매 턴 리셋**: 이전 턴의 누적 대미지와 관계없이 매 턴 최대치로 복구됨
4. **대미지 타입 무관**: 모든 타입의 대미지를 흡수 (NORMAL, THORNS, HP_LOSS 등)
5. **제거 불가**: 일반적인 디버프 제거 수단으로는 제거 불가능 (보스 전용 고정 파워)
6. **최대 대미지 제한**: 한 턴에 받을 수 있는 최대 대미지가 `amount` 값으로 제한됨
7. **maxAmt 필드**: 생성 시 설정되며, 매 턴 복구량을 결정하는 핵심 필드
8. **방어막 0 상태**: amount가 0이 되면 완전 무적 상태 (대미지 0으로 변환)
