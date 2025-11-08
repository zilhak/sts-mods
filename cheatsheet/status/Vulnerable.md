# 취약 (Vulnerable)

## 기본 정보

**클래스명**: `VulnerablePower`
**전체 경로**: `com.megacrit.cardcrawl.powers.VulnerablePower`
**ID**: `"Vulnerable"`
**타입**: DEBUFF
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 받는 대미지가 50% 증가
**수치 설명**: `amount`는 취약이 지속되는 턴 수
**적용 시점**: 대미지를 **받을 때** (atDamageReceive)

**특수 상호작용**:
- **Odd Mushroom 유물 소지 시** (플레이어가 취약 상태): 50% → 25% 증가로 감소 (1.25배)
- **Paper Frog 유물 소지 시** (적이 취약 상태): 50% → 75% 증가로 강화 (1.75배)
- 일반적으로는 150% 대미지 (1.5배)

---

## 코드 분석

### 생성자
```java
public VulnerablePower(AbstractCreature owner, int amount, boolean isSourceMonster) {
    this.name = NAME;
    this.ID = "Vulnerable";
    this.owner = owner;
    this.amount = amount;
    updateDescription();
    loadRegion("vulnerable");

    // 턴이 이미 끝난 상태에서 몬스터가 적용하면 첫 턴에 감소하지 않음
    if (AbstractDungeon.actionManager.turnHasEnded && isSourceMonster) {
        this.justApplied = true;
    }

    this.type = AbstractPower.PowerType.DEBUFF;
    this.isTurnBased = true;
}
```

**주요 파라미터**:
- `owner`: 취약 상태가 적용된 대상
- `amount`: 지속 턴 수
- `isSourceMonster`: 몬스터가 적용했는지 여부

**WeakPower와의 차이점**:
- `turnHasEnded` 체크 추가 → 턴 도중 적용된 취약은 즉시 감소 시작
- `priority` 값 지정 없음 (기본값 사용)

### 핵심 메서드

**atDamageReceive()**: 대미지를 받을 때 호출

```java
public float atDamageReceive(float damage, DamageInfo.DamageType type) {
    if (type == DamageInfo.DamageType.NORMAL) {
        // Odd Mushroom 유물 체크 (플레이어가 취약 상태이고 유물 소지)
        if (this.owner.isPlayer && AbstractDungeon.player.hasRelic("Odd Mushroom")) {
            return damage * 1.25F;  // 25% 증가
        }

        // Paper Frog 유물 체크 (적이 취약 상태이고 플레이어가 유물 소지)
        if (this.owner != null && !this.owner.isPlayer &&
            AbstractDungeon.player.hasRelic("Paper Frog")) {
            return damage * 1.75F;  // 75% 증가
        }

        return damage * 1.5F;  // 50% 증가
    }
    return damage;  // NORMAL 타입이 아니면 그대로
}
```

**중요**: `DamageInfo.DamageType.NORMAL`만 영향을 받음

### 스택/감소 로직

**턴 종료 시 자동 감소**:

```java
public void atEndOfRound() {
    if (this.justApplied) {
        this.justApplied = false;
        return;  // 적용된 턴에는 감소하지 않음
    }

    if (this.amount == 0) {
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Vulnerable"));
    } else {
        addToBot(new ReducePowerAction(this.owner, this.owner, "Vulnerable", 1));
    }
}
```

**감소 메커니즘**:
1. `turnHasEnded=true` 상태에서 몬스터가 적용한 경우만 첫 턴에 감소 안 함
2. 그 외 모든 경우는 적용된 턴 종료 시부터 감소 시작
3. 매 턴 종료마다 1씩 감소
4. 0이 되면 자동으로 제거

---

## 수정 방법

### 효과 수치 변경

**변경 대상**: `atDamageReceive()` 메서드의 곱셈 계수
**코드 위치**: 줄 79-94

**예시: 100% 증가로 변경**

```java
@SpirePatch(
    clz = VulnerablePower.class,
    method = "atDamageReceive"
)
public static class VulnerableDamageIncreasePatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Float> Insert(
        VulnerablePower __instance,
        float damage,
        DamageInfo.DamageType type
    ) {
        if (type == DamageInfo.DamageType.NORMAL) {
            // Odd Mushroom: 50% 증가
            if (__instance.owner.isPlayer &&
                AbstractDungeon.player.hasRelic("Odd Mushroom")) {
                return SpireReturn.Return(damage * 1.5F);
            }

            // Paper Frog: 125% 증가
            if (__instance.owner != null && !__instance.owner.isPlayer &&
                AbstractDungeon.player.hasRelic("Paper Frog")) {
                return SpireReturn.Return(damage * 2.25F);
            }

            // 기본: 100% 증가
            return SpireReturn.Return(damage * 2.0F);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                DamageInfo.DamageType.class, "NORMAL"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 지속 시간 변경

**방법 1: 생성 시 amount 조정**
```java
// 취약을 3배 지속시간으로 적용
new VulnerablePower(target, 6, true);  // 기존 2 → 6으로 변경
```

**방법 2: 턴당 감소량 변경**
```java
@SpirePatch(
    clz = VulnerablePower.class,
    method = "atEndOfRound"
)
public static class VulnerableDurationPatch {
    @SpireReplacePatch
    public static void Replace(VulnerablePower __instance) {
        if (__instance.justApplied) {
            __instance.justApplied = false;
            return;
        }

        // 2씩 감소하도록 수정
        if (__instance.amount <= 1) {
            AbstractDungeon.actionManager.addToBot(
                new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Vulnerable")
            );
        } else {
            AbstractDungeon.actionManager.addToBot(
                new ReducePowerAction(__instance.owner, __instance.owner, "Vulnerable", 2)
            );
        }
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Iron Wave (아이언클래드)
- Uppercut (아이언클래드)
- Shockwave (아이언클래드)
- Bash+ (아이언클래드 업그레이드)
- Bouncing Flask (사일런트)
- Leg Sweep (사일런트)
- Terror (사일런트)
- Doom and Gloom (사일런트)
- Genetic Algorithm (디펙트)
- Melter (디펙트)
- Weave (와쳐)
- Crescendo (와쳐)

**적용하는 몬스터**:
- Hexaghost (헥사고스트)
- Bronze Automaton (청동 자동인형)
- Lagavulin (라가블린)
- Gremlin Wizard (그렘린 마법사)
- Book of Stabbing (찌르기의 책)

**관련 유물**:
- **Odd Mushroom**: 플레이어가 취약 상태일 때 받는 추가 대미지 감소 (50% → 25%)
- **Paper Frog**: 적이 취약 상태일 때 받는 추가 대미지 증가 (50% → 75%)
- **Champion Belt**: 엘리트에게 취약 적용 시 추가 에너지

---

## 참고사항

1. **우선순위**: 기본 priority 사용 (명시적 지정 없음)
2. **NORMAL 대미지만 적용**: THORNS, HP_LOSS 같은 특수 대미지는 영향받지 않음
3. **곱셈 적용**:
   - 기본: `damage * 1.5F` (50% 증가)
   - Odd Mushroom (플레이어): `damage * 1.25F` (25% 증가)
   - Paper Frog (적): `damage * 1.75F` (75% 증가)
4. **justApplied 조건 차이**:
   - `turnHasEnded && isSourceMonster` 두 조건 모두 만족해야 첫 턴 유지
   - 턴 도중 적용된 취약은 해당 턴 종료 시 바로 감소
5. **스택 가능**: 취약 2를 가진 상태에서 취약 1을 추가로 받으면 3이 됨
6. **Artifact로 무효화 가능**: Artifact 파워가 있으면 취약 적용을 막을 수 있음
7. **Weak과의 차이점**:
   - Weak: 공격 시 적용 (atDamageGive)
   - Vulnerable: 방어 시 적용 (atDamageReceive)
8. **유물 상호작용**:
   - Odd Mushroom은 **플레이어 자신**의 취약만 영향
   - Paper Frog은 **적**의 취약만 영향
   - 두 유물 동시 소지 시 각각 독립적으로 적용
