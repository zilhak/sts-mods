# 데미지 (Damage) 수정 가이드

## 1. 데미지 시스템 개요

### 핵심 클래스: `DamageInfo`
```java
public class DamageInfo {
    public AbstractCreature owner;  // 데미지 소스 (공격자)
    public String name;             // 데미지 소스 이름
    public DamageType type;         // 데미지 타입
    public int base;                // 기본 데미지
    public int output;              // 최종 데미지 (모든 수정자 적용 후)
    public boolean isModified;      // 데미지가 수정되었는지 여부
}
```

### 데미지 타입 (DamageType)
```java
public enum DamageType {
    NORMAL,   // 일반 공격 (파워/유물 수정자 적용)
    THORNS,   // 가시 데미지 (Block 무시)
    HP_LOSS;  // 순수 HP 감소 (모든 수정자 무시)
}
```

## 2. 데미지 계산 파이프라인

### 데미지 계산 순서: `applyPowers(AbstractCreature owner, AbstractCreature target)`

**전체 흐름:**
```
1. base → output 복사
2. 공격자 파워 적용 (atDamageGive)
3. 방어자 파워 적용 (atDamageReceive)
4. Stance 적용
5. 공격자 파워 최종 적용 (atDamageFinalGive)
6. 방어자 파워 최종 적용 (atDamageFinalReceive)
7. 음수 체크 후 output 확정
```

**상세 구현 (플레이어 → 적):**
```java
public void applyPowers(AbstractCreature owner, AbstractCreature target) {
    this.output = this.base;
    float tmp = this.output;

    // 1단계: 공격자의 파워 (Strength 등)
    for (AbstractPower p : owner.powers) {
        tmp = p.atDamageGive(tmp, this.type);
        if (this.base != (int)tmp) {
            this.isModified = true;
        }
    }

    // 2단계: Stance 적용 (Wrath: 2배, Calm: 감소 등)
    tmp = AbstractDungeon.player.stance.atDamageGive(tmp, this.type);

    // 3단계: 방어자의 파워 (Vulnerable 등)
    for (AbstractPower p : target.powers) {
        tmp = p.atDamageReceive(tmp, this.type);
        if (this.base != (int)tmp) {
            this.isModified = true;
        }
    }

    // 4단계: 최종 공격자 파워
    for (AbstractPower p : owner.powers) {
        tmp = p.atDamageFinalGive(tmp, this.type);
    }

    // 5단계: 최종 방어자 파워
    for (AbstractPower p : target.powers) {
        tmp = p.atDamageFinalReceive(tmp, this.type);
    }

    // 6단계: 반올림 및 음수 방지
    this.output = MathUtils.floor(tmp);
    if (this.output < 0) {
        this.output = 0;
    }
}
```

**적 → 플레이어 (특수 처리):**
```java
if (!owner.isPlayer) {
    // Deadly Enemies Blight (무한 모드 전용)
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("DeadlyEnemies")) {
        float mod = AbstractDungeon.player.getBlight("DeadlyEnemies").effectFloat();
        tmp *= mod;  // 적 공격력 증가
    }

    // 이후 동일한 파워 적용 순서
    ...
}
```

## 3. 데미지가 발생하는 시점

### 3.1 플레이어 → 적

#### 공격 카드
```java
// DamageAction 생성
AbstractDungeon.actionManager.addToBottom(
    new DamageAction(
        target,                          // 대상
        new DamageInfo(player, damage),  // 데미지 정보
        AttackEffect.SLASH_HORIZONTAL    // 공격 이펙트
    )
);
```

#### 유물 (Tough Bandages 등)
```java
// AbstractRelic.onManualDiscard() 등
AbstractDungeon.actionManager.addToBottom(
    new DamageAction(
        AbstractDungeon.getRandomMonster(),
        new DamageInfo(AbstractDungeon.player, 3, DamageType.THORNS)
    )
);
```

#### 파워 (Poison 등)
```java
// AbstractPower.atEndOfTurn()
AbstractDungeon.actionManager.addToBottom(
    new DamageAction(
        owner,
        new DamageInfo(null, poisonAmount, DamageType.HP_LOSS)
    )
);
```

### 3.2 적 → 플레이어

#### 몬스터 공격
```java
// AbstractMonster.damage 계산
DamageInfo info = new DamageInfo(this, 10);
info.applyPowers(this, AbstractDungeon.player);

// 실제 공격
AbstractDungeon.actionManager.addToBottom(
    new DamageAction(
        AbstractDungeon.player,
        info,
        AttackEffect.BLUNT_HEAVY
    )
);
```

#### THORNS 데미지
```java
// 가시 유물/파워
new DamageInfo(this, 3, DamageType.THORNS)
```

#### HP_LOSS 데미지
```java
// Necronomicon 저주 등
new DamageInfo(null, 1, DamageType.HP_LOSS)
```

## 4. 데미지 수정 방법

### 4.1 전역 데미지 배율
**목표:** 모든 데미지 2배

```java
@SpirePatch(
    clz = DamageInfo.class,
    method = "applyPowers",
    paramtypez = {AbstractCreature.class, AbstractCreature.class}
)
public static class DoubleDamage {
    @SpirePostfixPatch
    public static void Postfix(DamageInfo __instance, AbstractCreature owner, AbstractCreature target) {
        // NORMAL 타입만 2배 (HP_LOSS는 제외)
        if (__instance.type == DamageInfo.DamageType.NORMAL) {
            __instance.output *= 2;
            __instance.isModified = true;
        }
    }
}
```

### 4.2 플레이어 공격력 증가
**목표:** 플레이어가 주는 모든 데미지 +10

```java
@SpirePatch(
    clz = DamageInfo.class,
    method = "applyPowers",
    paramtypez = {AbstractCreature.class, AbstractCreature.class}
)
public static class PlayerDamageBoost {
    @SpirePostfixPatch
    public static void Postfix(DamageInfo __instance, AbstractCreature owner, AbstractCreature target) {
        // 플레이어가 공격자일 때만
        if (owner != null && owner.isPlayer && __instance.type == DamageInfo.DamageType.NORMAL) {
            __instance.output += 10;
            __instance.isModified = true;
        }
    }
}
```

### 4.3 받는 데미지 감소
**목표:** 플레이어가 받는 모든 데미지 50% 감소

```java
@SpirePatch(
    clz = DamageInfo.class,
    method = "applyPowers",
    paramtypez = {AbstractCreature.class, AbstractCreature.class}
)
public static class ReduceIncomingDamage {
    @SpirePostfixPatch
    public static void Postfix(DamageInfo __instance, AbstractCreature owner, AbstractCreature target) {
        // 플레이어가 타겟이고, 적이 공격자일 때
        if (target != null && target.isPlayer && owner != null && !owner.isPlayer) {
            __instance.output = (int)(__instance.output * 0.5f);
            __instance.isModified = true;
        }
    }
}
```

### 4.4 특정 카드 데미지 증가
**목표:** Strike 카드 데미지 +3

```java
@SpirePatch(
    clz = Strike_Red.class,  // Ironclad Strike
    method = "use",
    paramtypez = {AbstractPlayer.class, AbstractMonster.class}
)
public static class BuffStrike {
    @SpirePrefixPatch
    public static void Prefix(AbstractCard __instance, AbstractPlayer p, AbstractMonster m) {
        // 카드의 기본 데미지 증가
        __instance.baseDamage += 3;
        __instance.calculateCardDamage(m);
    }
}
```

**더 나은 방법 - DamageInfo 직접 수정:**
```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "calculateCardDamage"
)
public static class BuffAllStrikes {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance, AbstractMonster mo) {
        // Strike 태그를 가진 카드만
        if (__instance.hasTag(CardTags.STRIKE)) {
            __instance.damage += 3;
            __instance.isDamageModified = true;
        }
    }
}
```

### 4.5 크리티컬 시스템 추가
**목표:** 10% 확률로 3배 데미지

```java
@SpirePatch(
    clz = DamageAction.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = {AbstractCreature.class, DamageInfo.class, AttackEffect.class}
)
public static class CriticalHitSystem {
    @SpirePostfixPatch
    public static void Postfix(DamageAction __instance, AbstractCreature target, DamageInfo info, AttackEffect effect) {
        // 플레이어 공격만
        if (info.owner != null && info.owner.isPlayer && info.type == DamageInfo.DamageType.NORMAL) {
            // 10% 확률
            if (AbstractDungeon.cardRandomRng.randomBoolean(0.1f)) {
                info.output *= 3;
                info.isModified = true;

                // 크리티컬 효과 표시
                AbstractDungeon.effectList.add(
                    new TextAboveCreatureEffect(
                        target.hb.cX,
                        target.hb.cY,
                        "CRITICAL!",
                        Color.GOLD
                    )
                );
            }
        }
    }
}
```

## 5. 데미지 타입별 처리

### NORMAL (일반 공격)
- **특징:** 모든 파워/유물 수정자 적용
- **Block 적용:** O
- **사용처:** 대부분의 공격 카드

```java
new DamageInfo(player, 10, DamageType.NORMAL)
```

### THORNS (가시 데미지)
- **특징:** Block 무시, 파워 수정자는 적용됨
- **Block 적용:** X
- **사용처:** Thorns 유물, Bronze Scales

```java
new DamageInfo(player, 3, DamageType.THORNS)
```

**Block 무시 구현 (AbstractCreature.damage):**
```java
// THORNS는 decrementBlock을 건너뜀
if (info.type != DamageType.THORNS) {
    damageAmount = decrementBlock(info, damageAmount);
}
```

### HP_LOSS (순수 HP 감소)
- **특징:** 모든 수정자 무시 (Strength, Vulnerable 등)
- **Block 적용:** X
- **사용처:** Poison, Necronomicon 저주

```java
new DamageInfo(null, poisonDamage, DamageType.HP_LOSS)
```

**수정자 무시 (applyPowers는 호출되지 않음):**
```java
// HP_LOSS는 applyPowers 없이 바로 적용
info.output = info.base;
```

## 6. 실용적인 수정 예시

### 예시 1: 받는 데미지 50% 감소
**목표:** 플레이어가 받는 모든 데미지를 절반으로

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "damage"
)
public static class HalfDamage {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, DamageInfo info) {
        // HP_LOSS 제외
        if (info.type != DamageInfo.DamageType.HP_LOSS) {
            info.output = (int)(info.output * 0.5f);
            // 최소 1 데미지
            if (info.output < 1 && info.base > 0) {
                info.output = 1;
            }
        }
    }
}
```

### 예시 2: 공격 카드 데미지 +10
**목표:** ATTACK 태그 카드만 데미지 증가

```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "calculateCardDamage"
)
public static class BuffAttackCards {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance, AbstractMonster mo) {
        if (__instance.type == CardType.ATTACK) {
            __instance.damage += 10;
            __instance.isDamageModified = true;
        }
    }
}
```

### 예시 3: 10% 확률로 3배 데미지
**목표:** 크리티컬 히트 시스템

```java
@SpirePatch(
    clz = DamageInfo.class,
    method = "applyPowers",
    paramtypez = {AbstractCreature.class, AbstractCreature.class}
)
public static class CriticalSystem {
    @SpirePostfixPatch
    public static void Postfix(DamageInfo __instance, AbstractCreature owner, AbstractCreature target) {
        if (owner != null && owner.isPlayer && __instance.type == DamageInfo.DamageType.NORMAL) {
            // 10% 확률
            if (AbstractDungeon.cardRandomRng.randomBoolean(0.1f)) {
                __instance.output *= 3;
                __instance.isModified = true;
            }
        }
    }
}
```

## 7. 관련 클래스

### DamageInfo
- **생성자:**
  ```java
  DamageInfo(AbstractCreature owner, int base, DamageType type)
  DamageInfo(AbstractCreature owner, int base)  // 기본 NORMAL
  ```
- **메서드:**
  - `applyPowers(owner, target)`: 데미지 계산
  - `applyEnemyPowersOnly(target)`: 적 파워만 적용

### DamageAction
- **생성자:**
  ```java
  DamageAction(AbstractCreature target, DamageInfo info, AttackEffect effect)
  DamageAction(AbstractCreature target, DamageInfo info)
  DamageAction(AbstractCreature target, DamageInfo info, boolean superFast)
  ```
- **실행:** `update()` 메서드에서 `target.damage(info)` 호출

### AbstractCreature.damage()
- **역할:** 실제 데미지 처리
- **순서:**
  1. Block 감소 (THORNS/HP_LOSS 제외)
  2. HP 감소
  3. 사망 체크
  4. 유물/파워 트리거

### AbstractPower 데미지 관련 메서드
- `atDamageGive(float damage, DamageType type)`: 공격자 파워
- `atDamageReceive(float damage, DamageType type)`: 방어자 파워
- `atDamageFinalGive(float damage, DamageType type)`: 최종 공격자 파워
- `atDamageFinalReceive(float damage, DamageType type)`: 최종 방어자 파워

## 8. 주의사항

### 8.1 데미지 수정 순서 중요
```
atDamageGive → atDamageReceive → Stance → atDamageFinalGive → atDamageFinalReceive
```
Postfix 패치는 모든 수정자 적용 **후** 실행되므로 최종 조정에 적합

### 8.2 음수 데미지 방지
```java
if (__instance.output < 0) {
    __instance.output = 0;
}
```
데미지 감소 패치 시 항상 음수 체크 필수

### 8.3 HP_LOSS 타입 처리
HP_LOSS는 `applyPowers()`를 호출하지 않음:
```java
if (info.type == DamageInfo.DamageType.HP_LOSS) {
    // 수정자 적용 불가
    return;
}
```

### 8.4 isModified 플래그
```java
if (this.base != (int)tmp) {
    this.isModified = true;  // UI에 노란색 표시
}
```
데미지를 수정했다면 `isModified = true` 설정 권장

### 8.5 DamageAction vs AbstractCreature.damage()
- **DamageAction**: 게임 액션 큐에 추가, 시각 효과 포함
- **damage()**: 직접 HP 감소, 효과 없음

대부분의 경우 `DamageAction` 사용 권장

### 8.6 멀티타겟 데미지
```java
int[] damageMatrix = DamageInfo.createDamageMatrix(baseDamage);
// 각 적마다 다른 데미지 계산 (Strength, Vulnerable 등 고려)

for (int i = 0; i < monsters.size(); i++) {
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            monsters.get(i),
            new DamageInfo(player, damageMatrix[i], DamageType.NORMAL)
        )
    );
}
```

### 8.7 카드 데미지 vs DamageInfo
- **AbstractCard.damage**: 카드에 표시되는 데미지
- **DamageInfo.output**: 실제 적용되는 데미지

카드 데미지 수정:
```java
@SpirePatch(clz = AbstractCard.class, method = "calculateCardDamage")
```

실제 데미지 수정:
```java
@SpirePatch(clz = DamageInfo.class, method = "applyPowers")
```

### 8.8 Stance 고려
Wrath Stance는 데미지를 2배로 만듦:
```java
tmp = AbstractDungeon.player.stance.atDamageGive(tmp, this.type);
```
Postfix로 수정하면 Stance 효과가 이미 적용된 상태

### 8.9 특정 공격만 수정
```java
// 카드 이름으로 구분
if (__instance.cardID.equals(Strike_Red.ID)) {
    __instance.damage += 5;
}

// 태그로 구분
if (__instance.hasTag(CardTags.STRIKE)) {
    __instance.damage += 3;
}

// 레어도로 구분
if (__instance.rarity == CardRarity.RARE) {
    __instance.damage *= 2;
}
```

### 8.10 패치 위치 선택
- **DamageInfo.applyPowers()**: 데미지 계산 과정 수정
- **DamageAction.update()**: 데미지 적용 직전 수정
- **AbstractCreature.damage()**: 데미지 적용 시 수정
- **AbstractCard.calculateCardDamage()**: 카드 표시 데미지 수정

각 위치마다 적용 시점과 범위가 다르므로 목적에 맞게 선택
