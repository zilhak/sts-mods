# Dexterity Power

**파일:** `com.megacrit.cardcrawl.powers.DexterityPower`

## 개요

방어도(Block) 획득량을 증감시키는 기본 스탯 파워. 양수일 때 버프, 음수일 때 디버프로 작동.

## 기본 구조

```java
public class DexterityPower extends AbstractPower {
    public static final String POWER_ID = "Dexterity";

    public DexterityPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = "Dexterity";
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
        loadRegion("dexterity");
        this.canGoNegative = true;  // 음수 허용
    }
}
```

## 핵심 메커니즘

### 1. 방어도 증감 (modifyBlock)

**가장 중요한 메서드** - 방어도 획득에 직접 적용:

```java
public float modifyBlock(float blockAmount) {
    if ((blockAmount += this.amount) < 0.0F) {
        return 0.0F;  // 음수 방지
    }
    return blockAmount;
}
```

**중요 특징:**
- 방어도에 민첩 값 **가산** (`blockAmount += this.amount`)
- **음수 방지**: 최종 방어도가 음수가 되면 0으로 처리
- 모든 방어도 획득에 적용 (카드, 파워, 렐릭 등)

**적용 예시:**
```java
// 원래 방어도: 5
// 민첩: +3
// 최종 방어도: 5 + 3 = 8

// 원래 방어도: 5
// 민첩: -2
// 최종 방어도: 5 + (-2) = 3

// 원래 방어도: 3
// 민첩: -5
// 최종 방어도: 3 + (-5) = -2 → 0 (음수 방지)
```

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
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Dexterity"));
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

**Strength와의 차이:**
- 업적 트리거 없음 (Strength는 50 이상 시 "JAXXED" 업적)
- 나머지 로직은 동일

### 4. 감소 처리 (reducePower)

```java
public void reducePower(int reduceAmount) {
    this.fontScale = 8.0F;
    this.amount -= reduceAmount;

    if (this.amount == 0) {
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Dexterity"));
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

### 패턴 1: 민첩 부여 카드

```java
public class MyDexterityCard extends AbstractCard {
    private static final int DEX_AMT = 2;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 플레이어에게 민첩 부여
        addToBot(new ApplyPowerAction(p, p,
            new DexterityPower(p, DEX_AMT), DEX_AMT));
    }
}
```

### 패턴 2: 방어 + 민첩 콤보

```java
public class MyDefendPlusCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 즉시 방어도 획득
        addToBot(new GainBlockAction(p, p, block));

        // 지속 효과로 민첩 부여
        addToBot(new ApplyPowerAction(p, p,
            new DexterityPower(p, magicNumber), magicNumber));
    }
}
```

### 패턴 3: 임시 민첩 버프

```java
public class MyTemporaryDexCard extends AbstractCard {
    private static final int DEX_AMT = 3;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 민첩 부여
        addToBot(new ApplyPowerAction(p, p,
            new DexterityPower(p, DEX_AMT), DEX_AMT));

        // 턴 종료 시 민첩 감소
        addToBot(new ApplyPowerAction(p, p,
            new LoseDexterityPower(p, DEX_AMT), DEX_AMT));
    }
}
```

### 패턴 4: 민첩 스케일링 카드

```java
public class MyScalingDefendCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 기본 방어도
        int blockAmount = this.block;

        // 민첩 추가 적용
        AbstractPower dex = p.getPower("Dexterity");
        if (dex != null) {
            blockAmount += dex.amount;
        }

        addToBot(new GainBlockAction(p, p, blockAmount));
    }
}
```

### 패턴 5: 적에게 민첩 디버프

```java
public class MyDexDebuffCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 적에게 -2 민첩 부여 (방어도 감소)
        addToBot(new ApplyPowerAction(m, p,
            new DexterityPower(m, -2), -2));

        // 음수 민첩은 자동으로 디버프로 표시됨
    }
}
```

## 상호작용 주의사항

### 1. Footwork / LoseDexterityPower

```java
// Footwork 카드: 지속 민첩 증가
addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, 2), 2));

// Flex처럼 LoseDexterityPower와 조합 가능
addToBot(new ApplyPowerAction(p, p, new LoseDexterityPower(p, 2), 2));
```

### 2. 음수 방어도 방지

```java
// modifyBlock()에서 자동으로 0 이하로 떨어지지 않도록 처리
if ((blockAmount += this.amount) < 0.0F) {
    return 0.0F;
}

// 예: 방어도 3, 민첩 -5 → 최종 0
```

### 3. 민첩 0일 때 자동 제거

```java
// 민첩이 정확히 0이 되면 파워가 자동으로 제거됨
if (this.amount == 0) {
    addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Dexterity"));
}
```

### 4. 범위 제한

```java
// 민첩은 항상 [-999, 999] 범위 내로 제한됨
// 모딩 시 극단적인 값 설정 주의
```

## 방어도 계산 흐름

```
카드 사용 (Defend 등)
  ↓
calculateCardDamage() - 카드 자체 방어도 계산
  ↓
GainBlockAction 생성
  ↓
modifyBlock() 호출 - Dexterity 적용 ★
  ↓
onGainedBlock() 호출 - 다른 파워 처리
  ↓
최종 방어도 획득
```

## 실전 예시

### 예시 1: Footwork

```java
// Footwork: 민첩 영구 증가
public void use(AbstractPlayer p, AbstractMonster m) {
    addToBot(new ApplyPowerAction(p, p,
        new DexterityPower(p, this.magicNumber), this.magicNumber));
}
```

### 예시 2: Finesse

```java
// Finesse: 즉시 방어도 + 카드 드로우
public void use(AbstractPlayer p, AbstractMonster m) {
    addToBot(new GainBlockAction(p, p, this.block));
    addToBot(new DrawCardAction(p, this.magicNumber));
}

// 민첩이 있으면 방어도 자동 증가
```

### 예시 3: Burst + Footwork

```java
// Burst: 다음 스킬 2회 사용
// Footwork를 Burst로 사용하면 민첩 2배 증가

// 1회: 민첩 +2
addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, 2), 2));

// 2회: 민첩 +2 (총 +4)
addToBot(new ApplyPowerAction(p, p, new DexterityPower(p, 2), 2));
```

## 모딩 체크리스트

- [ ] 민첩 증감 시 음수 처리 고려했는가?
- [ ] 음수 방어도 방지 메커니즘을 이해했는가?
- [ ] 민첩 0일 때 자동 제거 처리 확인했는가?
- [ ] 범위 제한 [-999, 999] 인지했는가?
- [ ] stackPower vs reducePower 차이 이해했는가?
- [ ] 임시 민첩 증가 시 LoseDexterityPower 사용했는가?

## Strength vs Dexterity 비교

| 항목 | Strength | Dexterity |
|------|----------|-----------|
| **적용 대상** | 공격 데미지 | 방어도 |
| **적용 메서드** | `atDamageGive()` | `modifyBlock()` |
| **적용 조건** | DamageType.NORMAL만 | 모든 방어도 획득 |
| **음수 처리** | 그대로 적용 (데미지 감소) | 0 이하 방지 |
| **업적** | 50 이상 시 JAXXED | 없음 |
| **주요 카드** | Heavy Blade, Inflame | Footwork, Finesse |

## 관련 파워

- **LoseDexterityPower**: 턴 종료 시 민첩 감소
- **StrengthPower**: 공격 데미지 증감 (Dexterity와 유사 구조)
- **FrailPower**: 방어도 획득 75% 감소 (곱연산)
- **BlurPower**: 방어도 턴 종료 시 유지
- **NextTurnBlockPower**: 다음 턴에 방어도 획득

## 참고사항

1. **적용 범위**
   - 모든 방어도 획득에 적용
   - 카드, 파워, 렐릭 모두 포함
   - Strength처럼 타입 제한 없음

2. **음수 민첩의 의미**
   - 방어도 획득 감소
   - 자동으로 디버프로 표시
   - 최소 0 보장 (음수 방어도 불가)

3. **민첩 스케일링**
   - 가산 연산 (+amount)
   - Frail은 곱연산 (×0.75)
   - 계산 순서: 민첩 → Frail → 기타 효과

4. **파워 중첩**
   - stackPower()로 자동 합산
   - 반대 효과 상쇄 가능 (민첩 +3, -2 → 민첩 +1)

5. **Silent 특화**
   - Silent의 주요 스탯
   - Footwork, Finesse 등 핵심 카드
   - 방어 중심 빌드의 핵심
