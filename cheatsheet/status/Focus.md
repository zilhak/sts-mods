# Focus Power

**파일:** `com.megacrit.cardcrawl.powers.FocusPower`

## 개요

오브(Orb)의 효과를 증감시키는 Defect 전용 스탯 파워. 양수일 때 버프, 음수일 때 디버프로 작동.

## 기본 구조

```java
public class FocusPower extends AbstractPower {
    public static final String POWER_ID = "Focus";

    public FocusPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = "Focus";
        this.owner = owner;
        this.amount = amount;

        updateDescription();
        loadRegion("focus");
        this.canGoNegative = true;  // 음수 허용

        // 범위 제한 없음 (Strength/Dexterity와 차이점)
    }
}
```

## 핵심 메커니즘

### 1. 오브 효과 증감 (applyFocus)

**오브 시스템과의 연동** - AbstractOrb에서 호출:

```java
// AbstractOrb.java의 applyFocus() 메서드
public void applyFocus() {
    AbstractPower power = AbstractDungeon.player.getPower("Focus");

    if (power != null && !this.ID.equals("Plasma")) {
        // Plasma를 제외한 모든 오브에 집중 적용
        this.passiveAmount = Math.max(0, this.basePassiveAmount + power.amount);
        this.evokeAmount = Math.max(0, this.baseEvokeAmount + power.amount);
    } else {
        // 집중이 없거나 Plasma인 경우
        this.passiveAmount = this.basePassiveAmount;
        this.evokeAmount = this.baseEvokeAmount;
    }
}
```

**중요 특징:**
- **passiveAmount**: 오브의 턴 종료 효과
- **evokeAmount**: 오브 발동(Evoke) 시 효과
- **Plasma 제외**: Plasma 오브는 집중 영향을 받지 않음
- **음수 방지**: Math.max(0, ...) 로 최소 0 보장

**적용 예시:**
```java
// Lightning Orb (기본 Passive 3, Evoke 8)
// 집중 +2인 경우:
passiveAmount = max(0, 3 + 2) = 5  // 턴 종료 시 5 데미지
evokeAmount = max(0, 8 + 2) = 10   // 발동 시 10 데미지

// 집중 -1인 경우:
passiveAmount = max(0, 3 + (-1)) = 2  // 턴 종료 시 2 데미지
evokeAmount = max(0, 8 + (-1)) = 7    // 발동 시 7 데미지

// 집중 -5인 경우:
passiveAmount = max(0, 3 + (-5)) = 0  // 턴 종료 시 0 데미지
evokeAmount = max(0, 8 + (-5)) = 3    // 발동 시 3 데미지
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

### 3. 스택 관리 (stackPower)

```java
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;  // 시각 효과
    this.amount += stackAmount;

    if (this.amount == 0) {
        // 0이 되면 파워 제거
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Focus"));
    }

    // 업적: 집중 25 이상 달성
    if (this.amount >= 25) {
        UnlockTracker.unlockAchievement("FOCUSED");
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
- 업적 트리거 기준이 25 (Strength는 50)
- 업적 이름: "FOCUSED"

## 오브별 집중 적용

### Lightning (번개)
```java
basePassiveAmount = 3;  // 턴 종료 시 무작위 적에게 3 데미지
baseEvokeAmount = 8;    // 발동 시 무작위 적에게 8 데미지

// 집중 +3인 경우:
passiveAmount = 6  // 턴 종료 시 6 데미지
evokeAmount = 11   // 발동 시 11 데미지
```

### Frost (서리)
```java
basePassiveAmount = 2;  // 턴 종료 시 방어도 2 획득
baseEvokeAmount = 5;    // 발동 시 방어도 5 획득

// 집중 +3인 경우:
passiveAmount = 5   // 턴 종료 시 방어도 5 획득
evokeAmount = 8     // 발동 시 방어도 8 획득
```

### Dark (어둠)
```java
basePassiveAmount = 0;  // 턴 종료 효과 없음
baseEvokeAmount = 6;    // 발동 시 6 데미지 (누적)

// 집중 +3인 경우:
passiveAmount = 0   // 턴 종료 효과 여전히 없음 (하지만 누적은 증가)
evokeAmount = 9     // 발동 시 9 데미지
```

### Plasma (플라즈마)
```java
// Plasma는 집중 영향을 받지 않음!
if (!this.ID.equals("Plasma")) {
    // 집중 적용
}

// Plasma는 항상 고정 값
// 발동 시 에너지 2 획득 (집중 무관)
```

## 모딩 활용 패턴

### 패턴 1: 집중 부여 카드

```java
public class MyFocusCard extends AbstractCard {
    private static final int FOCUS_AMT = 2;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 플레이어에게 집중 부여
        addToBot(new ApplyPowerAction(p, p,
            new FocusPower(p, FOCUS_AMT), FOCUS_AMT));
    }
}
```

### 패턴 2: 오브 생성 + 집중

```java
public class MyOrbFocusCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 번개 오브 생성
        addToBot(new ChannelAction(new Lightning()));

        // 집중 증가
        addToBot(new ApplyPowerAction(p, p,
            new FocusPower(p, this.magicNumber), this.magicNumber));
    }
}
```

### 패턴 3: 집중 스케일링 카드

```java
public class MyScalingOrbCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 번개 오브 여러 개 생성
        int orbCount = 1;

        // 집중에 따라 오브 개수 증가
        AbstractPower focus = p.getPower("Focus");
        if (focus != null && focus.amount > 0) {
            orbCount += focus.amount / 3;  // 집중 3당 오브 1개 추가
        }

        for (int i = 0; i < orbCount; i++) {
            addToBot(new ChannelAction(new Lightning()));
        }
    }
}
```

### 패턴 4: 임시 집중 버프

```java
public class MyTemporaryFocusCard extends AbstractCard {
    private static final int FOCUS_AMT = 3;

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 집중 부여
        addToBot(new ApplyPowerAction(p, p,
            new FocusPower(p, FOCUS_AMT), FOCUS_AMT));

        // 턴 종료 시 집중 감소
        addToBot(new ApplyPowerAction(p, p,
            new LoseFocusPower(p, FOCUS_AMT), FOCUS_AMT));
    }
}
```

### 패턴 5: 집중 기반 효과

```java
public class MyFocusConditionalCard extends AbstractCard {
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractPower focus = p.getPower("Focus");

        if (focus != null && focus.amount >= 3) {
            // 집중 3 이상 시 추가 효과
            addToBot(new DrawCardAction(p, this.magicNumber));
        }

        // 기본 효과
        addToBot(new ChannelAction(new Lightning()));
    }
}
```

## 상호작용 주의사항

### 1. Plasma 예외 처리

```java
// Plasma는 집중 영향을 받지 않음
if (!this.ID.equals("Plasma")) {
    // 집중 적용
}

// 모딩 시 새로운 오브를 만들 때 주의:
// - 집중 적용 원하면: Plasma가 아니면 됨
// - 집중 무시하려면: ID를 "Plasma"로 설정하거나 applyFocus() 오버라이드
```

### 2. 음수 효과 방지

```java
// applyFocus()에서 Math.max(0, ...)로 처리
passiveAmount = Math.max(0, this.basePassiveAmount + power.amount);

// 예: Lightning (기본 3), 집중 -5
// passiveAmount = max(0, 3 + (-5)) = 0
// 음수 데미지나 음수 방어도가 발생하지 않음
```

### 3. 집중 0일 때 자동 제거

```java
// 집중이 정확히 0이 되면 파워가 자동으로 제거됨
if (this.amount == 0) {
    addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, "Focus"));
}
```

### 4. 업적 트리거

```java
// 집중 25 이상 달성 시 "FOCUSED" 업적
if (this.amount >= 25) {
    UnlockTracker.unlockAchievement("FOCUSED");
}
```

## 오브 효과 계산 흐름

```
오브 생성 (ChannelAction)
  ↓
AbstractOrb 생성자
  ↓
applyFocus() 호출 - Focus 적용 ★
  ↓
passiveAmount, evokeAmount 설정
  ↓
오브 슬롯에 배치
  ↓
[턴 종료]
  ↓
onEndOfTurn() - passiveAmount 효과 발동
  ↓
[오브 발동]
  ↓
onEvoke() - evokeAmount 효과 발동
```

## 실전 예시

### 예시 1: Defragment

```java
// Defragment: 집중 영구 증가
public void use(AbstractPlayer p, AbstractMonster m) {
    addToBot(new ApplyPowerAction(p, p,
        new FocusPower(p, this.magicNumber), this.magicNumber));
}
```

### 예시 2: Biased Cognition

```java
// Biased Cognition: 집중 대량 증가 + 턴마다 감소
public void use(AbstractPlayer p, AbstractMonster m) {
    // 집중 +4
    addToBot(new ApplyPowerAction(p, p,
        new FocusPower(p, this.magicNumber), this.magicNumber));

    // 턴마다 집중 -1
    addToBot(new ApplyPowerAction(p, p,
        new BiasCognitionPower(p, this.secondMagicNumber),
        this.secondMagicNumber));
}
```

### 예시 3: Consume

```java
// Consume: 오브 슬롯 -1, 집중 +2
public void use(AbstractPlayer p, AbstractMonster m) {
    // 집중 증가
    addToBot(new ApplyPowerAction(p, p,
        new FocusPower(p, this.magicNumber), this.magicNumber));

    // 오브 슬롯 감소
    addToBot(new DecreaseMaxOrbAction(1));
}
```

## 모딩 체크리스트

- [ ] 집중 증감 시 음수 처리 고려했는가?
- [ ] Plasma 오브 예외 처리를 이해했는가?
- [ ] applyFocus()의 음수 방지를 인지했는가?
- [ ] 집중 0일 때 자동 제거 처리 확인했는가?
- [ ] passiveAmount vs evokeAmount 차이를 이해했는가?
- [ ] 새로운 오브 제작 시 집중 적용 여부를 결정했는가?

## Strength/Dexterity vs Focus 비교

| 항목 | Strength | Dexterity | Focus |
|------|----------|-----------|-------|
| **적용 대상** | 공격 데미지 | 방어도 | 오브 효과 |
| **적용 메서드** | `atDamageGive()` | `modifyBlock()` | `applyFocus()` (in AbstractOrb) |
| **적용 범위** | NORMAL 타입만 | 모든 방어도 | Plasma 제외 모든 오브 |
| **음수 처리** | 그대로 적용 | 0 이하 방지 | 0 이하 방지 |
| **업적** | 50 이상 JAXXED | 없음 | 25 이상 FOCUSED |
| **캐릭터** | 전체 | 전체 | Defect 전용 |

## 관련 파워

- **LoseFocusPower**: 턴 종료 시 집중 감소
- **BiasCognitionPower**: Biased Cognition의 디버프 (턴마다 집중 -1)
- **ElectroPower**: 번개 오브 관련
- **LoopPower**: 턴 시작 시 번개 오브 생성

## 참고사항

1. **Defect 전용**
   - 다른 캐릭터는 오브 시스템 없음
   - 집중도 Defect에게만 의미 있음
   - 모딩 시 다른 캐릭터에게 집중 부여 가능하지만 효과 없음

2. **Plasma 예외**
   - 유일하게 집중 영향을 받지 않는 오브
   - 에너지 생성 오브이므로 밸런스상 예외 처리
   - 집중과 무관하게 항상 에너지 2 생성

3. **음수 집중의 의미**
   - 오브 효과 감소
   - 자동으로 디버프로 표시
   - 최소 0 보장 (음수 효과 불가)
   - 적에게 유용한 디버프

4. **집중 스케일링**
   - 가산 연산 (+amount)
   - 오브별로 baseAmount가 다름
   - Lightning: passive 3, evoke 8
   - Frost: passive 2, evoke 5
   - Dark: passive 0, evoke 6

5. **오브 생성 타이밍**
   - 집중은 오브 생성 시점에 적용됨
   - 이후 집중이 증가해도 기존 오브는 변하지 않음
   - 새로 생성되는 오브만 현재 집중 적용

6. **커스텀 오브 제작 시**
   ```java
   public class MyCustomOrb extends AbstractOrb {
       public MyCustomOrb() {
           this.basePassiveAmount = 5;
           this.baseEvokeAmount = 10;
           this.ID = "MyOrb";  // "Plasma"가 아니면 집중 적용됨

           // 집중 적용
           applyFocus();
       }
   }
   ```
