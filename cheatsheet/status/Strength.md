# Strength Power

**파일:** `com.megacrit.cardcrawl.powers.StrengthPower`

## 개요

공격 카드의 데미지를 증감시키는 기본 스탯 파워. 양수일 때 버프, 음수일 때 디버프로 작동.

## 기본 구조

```java
public class StrengthPower extends AbstractPower {
    public static final String POWER_ID = "Strength";

    public StrengthPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = "Strength";
        this.owner = owner;
        this.amount = amount;

        // 범위 제한 [-999, 999]
        if (this.amount >= 999) {
            this.amount = 999;
        }
        if (this.amount <= -999) {
            this.amount = -999;
        }

        updateDescription();
        loadRegion("strength");
        this.canGoNegative = true;  // 음수 허용
    }
}
```

## 핵심 메커니즘

### 1. 데미지 증감 (atDamageGive)

**가장 중요한 메서드** - 공격 카드 데미지에 직접 적용:

```java
public float atDamageGive(float damage, DamageInfo.DamageType type) {
    if (type == DamageInfo.DamageType.NORMAL) {
        return damage + this.amount;
    }
    return damage;
}
```

**적용 범위:**
- `DamageInfo.DamageType.NORMAL` 타입만 적용
- 공격(Attack) 카드의 데미지
- 일부 파워/렐릭 효과의 데미지

**적용되지 않는 경우:**
- `DamageInfo.DamageType.THORNS` (가시 데미지)
- `DamageInfo.DamageType.HP_LOSS` (체력 손실)

### 2. 음수/양수 처리 (updateDescription)

```java
public void updateDescription() {
    if (this.amount > 0) {
        // 양수 = 버프
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[2];
        this.type = AbstractPower.PowerType.BUFF;
    } else {
        // 음수 = 디버프
        int tmp = -this.amount;
        this.description = DESCRIPTIONS[1] + tmp + DESCRIPTIONS[2];
        this.type = AbstractPower.PowerType.DEBUFF;
    }
}
```

**중요 특징:**
- `amount > 0`: 버프 (초록색 표시)
- `amount < 0`: 디버프 (빨간색 표시)
- 설명 텍스트도 양수/음수에 따라 변경
- PowerType 자동 변경

### 3. 스택 관리 (stackPower)

```java
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;  // 시각 효과
    this.amount += stackAmount;

    if (this.amount == 0) {
        // 0이 되면 파워 제거
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Strength"));
    }

    // 업적: 힘 50 이상 달성
    if (this.amount >= 50 && this.owner == AbstractDungeon.player) {
        UnlockTracker.unlockAchievement("JAXXED");
    }

    // 범위 제한
    if (this.amount >= 999) {
        this.amount = 999;
    }
    if (this.amount <= -999) {
        this.amount = -999;
    }
}
```

### 4. 감소 처리 (reducePower)

```java
public void reducePower(int reduceAmount) {
    this.fontScale = 8.0F;
    this.amount -= reduceAmount;

    if (this.amount == 0) {
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, NAME));
    }

    // 범위 제한
    if (this.amount >= 999) {
        this.amount = 999;
    }
    if (this.amount <= -999) {
        this.amount = -999;
    }
}
```

## 모딩 활용 패턴

### 패턴 1: 힘 부여 카드 만들기

```java
public class MyStrengthCard extends AbstractCard {
    private static final int STRENGTH_AMT = 2;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 플레이어에게 힘 부여
        addToBot(new ApplyPowerAction(p, p,
            new StrengthPower(p, STRENGTH_AMT), STRENGTH_AMT));
    }
}
```

### 패턴 2: 적에게 힘 디버프

```java
public class MyWeakenAttackCard extends AbstractCard {
    private static final int STRENGTH_LOSS = -2;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 데미지 + 힘 감소
        addToBot(new DamageAction(m,
            new DamageInfo(p, damage, damageTypeForTurn)));
        addToBot(new ApplyPowerAction(m, p,
            new StrengthPower(m, STRENGTH_LOSS), STRENGTH_LOSS));
    }
}
```

### 패턴 3: 임시 힘 버프

```java
public class MyTemporaryStrengthCard extends AbstractCard {
    private static final int STRENGTH_AMT = 3;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 힘 부여
        addToBot(new ApplyPowerAction(p, p,
            new StrengthPower(p, STRENGTH_AMT), STRENGTH_AMT));

        // 턴 종료 시 힘 감소
        addToBot(new ApplyPowerAction(p, p,
            new LoseStrengthPower(p, STRENGTH_AMT), STRENGTH_AMT));
    }
}
```

### 패턴 4: 힘 스케일링 카드

```java
public class MyScalingCard extends AbstractCard {
    @Override
    public void calculateCardDamage(AbstractMonster mo) {
        super.calculateCardDamage(mo);

        // 플레이어의 현재 힘 확인
        AbstractPower strength = AbstractDungeon.player.getPower("Strength");
        if (strength != null) {
            // 힘 1당 추가 데미지 증가
            int bonus = strength.amount;
            this.damage += bonus;
            this.isDamageModified = (bonus != 0);
        }
    }
}
```

### 패턴 5: 음수 힘 활용 (디버프 메커니즘)

```java
public class MyDebuffCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 적에게 -3 힘 부여 (데미지 감소)
        addToBot(new ApplyPowerAction(m, p,
            new StrengthPower(m, -3), -3));

        // 음수 힘은 자동으로 디버프로 표시됨
    }
}
```

## 상호작용 주의사항

### 1. Flex / LoseStrengthPower

```java
// Flex 카드: 임시 힘 증가
addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, 2), 2));
addToBot(new ApplyPowerAction(p, p, new LoseStrengthPower(p, 2), 2));

// LoseStrengthPower가 턴 종료 시 StrengthPower를 감소시킴
```

### 2. 힘 0일 때 자동 제거

```java
// 힘이 정확히 0이 되면 파워가 자동으로 제거됨
if (this.amount == 0) {
    addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Strength"));
}
```

### 3. 범위 제한

```java
// 힘은 항상 [-999, 999] 범위 내로 제한됨
// 모딩 시 극단적인 값 설정 주의
```

### 4. 업적 트리거

```java
// 플레이어가 힘 50 이상 달성 시 "JAXXED" 업적
if (this.amount >= 50 && this.owner == AbstractDungeon.player) {
    UnlockTracker.unlockAchievement("JAXXED");
}
```

## 데미지 계산 흐름

```
카드 사용
  ↓
calculateCardDamage() - 카드 자체 데미지 계산
  ↓
DamageAction 생성
  ↓
atDamageGive() 호출 - Strength 적용 ★
  ↓
atDamageReceive() 호출 - 방어측 처리
  ↓
최종 데미지 적용
```

## 실전 예시

### 예시 1: Heavy Blade

```java
// Heavy Blade: 힘 3배 적용
public void use(AbstractPlayer p, AbstractMonster m) {
    addToBot(new DamageAction(m,
        new DamageInfo(p, damage, damageTypeForTurn)));
}

public void calculateCardDamage(AbstractMonster mo) {
    int realBaseDamage = this.baseDamage;

    // 힘 3배 적용
    AbstractPower strength = AbstractDungeon.player.getPower("Strength");
    if (strength != null) {
        this.baseDamage += strength.amount * 2;  // 기본 1배 + 추가 2배 = 3배
    }

    super.calculateCardDamage(mo);
    this.baseDamage = realBaseDamage;
    this.isDamageModified = true;
}
```

### 예시 2: Spot Weakness

```java
// Spot Weakness: 적이 공격 의도가 아니면 힘 부여
public void use(AbstractPlayer p, AbstractMonster m) {
    if (m != null && m.getIntentBaseDmg() < 0) {
        addToBot(new ApplyPowerAction(p, p,
            new StrengthPower(p, magicNumber), magicNumber));
    }
}
```

## 모딩 체크리스트

- [ ] 힘 증감 시 음수 처리 고려했는가?
- [ ] DamageType.NORMAL만 적용됨을 이해했는가?
- [ ] 힘 0일 때 자동 제거 처리 확인했는가?
- [ ] 범위 제한 [-999, 999] 인지했는가?
- [ ] stackPower vs reducePower 차이 이해했는가?
- [ ] 임시 힘 증가 시 LoseStrengthPower 사용했는가?

## 관련 파워

- **LoseStrengthPower**: 턴 종료 시 힘 감소
- **DexterityPower**: 방어도 증감 (Strength와 유사 구조)
- **FocusPower**: 오브 효과 증감 (Defect 전용)
- **WeakPower**: 데미지 75% 감소 (곱연산)
- **VulnerablePower**: 받는 데미지 50% 증가

## 참고사항

1. **데미지 타입 확인 필수**
   - NORMAL: 적용 ○
   - THORNS: 적용 ×
   - HP_LOSS: 적용 ×

2. **음수 힘의 의미**
   - 공격 시 데미지 감소
   - 자동으로 디버프로 표시
   - 적에게 유용한 디버프

3. **힘 스케일링**
   - 가산 연산 (+amount)
   - Weak/Vulnerable는 곱연산 (×0.75, ×1.5)
   - 계산 순서: 힘 → Weak → Vulnerable

4. **파워 중첩**
   - stackPower()로 자동 합산
   - 반대 효과 상쇄 가능 (힘 +3, -2 → 힘 +1)
