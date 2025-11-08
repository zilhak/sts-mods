# FungiBeast (동물하초)

## 기본 정보
- **클래스명**: `FungiBeast`
- **파일 경로**: `com/megacrit/cardcrawl/monsters/exordium/FungiBeast.java`
- **Monster ID**: `FungiBeast`
- **타입**: 일반 몬스터
- **지역**: Exordium (1막)
- **특수 조우**: TwoFungiBeasts (2마리 조우 가능)

## HP 정보
| 난이도 | HP 범위 | 코드 위치 |
|--------|---------|-----------|
| 기본 (A0-6) | 22-28 | Line 47 |
| A7+ | 24-28 | Line 45 |

## 생성자 정보
```java
// Line 41
public FungiBeast(float x, float y)
```
- 애니메이션: `images/monsters/theBottom/fungi/skeleton.atlas`
- 히트박스: (0.0F, -16.0F, 260.0F, 170.0F)
- 애니메이션 속도: 0.7-1.0 (랜덤) - Line 64

## 패턴 정보

### 1. BITE (물기) - 바이트값: 1
**의도**: ATTACK
**데미지**: 6 (모든 난이도 동일)
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| 모든 난이도 | 6 | Line 52, 55 |

**실행**: Line 79-82
```java
case 1:
  AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
  AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
```

### 2. GROW (성장) - 바이트값: 2
**의도**: BUFF
**효과**: 자신에게 힘 증가
| 난이도 | 힘 증가 | 코드 위치 |
|--------|---------|-----------|
| A0-16 | +3 | Line 54, 89 |
| A2-16 | +4 | Line 51 |
| A17+ | +4/+5 | Line 86 |

**실행**: Line 85-90
```java
case 2:
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, this.strAmt + 1), this.strAmt + 1)); // strAmt는 3 or 4
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, this.strAmt), this.strAmt));
  }
```

**주의**: A17+에서는 `strAmt + 1`이므로 A2 기준으로 5 증가

## 특수 동작

### usePreBattleAction() - SporeCloudPower
전투 시작 시 **포자 구름(SporeCloudPower)** 2 부여 (Line 70-72):
- 죽을 때 플레이어에게 취약 2턴 부여
- 모든 난이도 동일 (2 고정)

```java
public void usePreBattleAction() {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new SporeCloudPower(this, 2)));
}
```

### changeState() - 애니메이션
"ATTACK" 상태만 지원 (Line 120-127):
```java
case "ATTACK":
  this.state.setAnimation(0, "Attack", false);
  this.state.addAnimation(0, "Idle", true, 0.0F);
```

### damage() - 피격 애니메이션
데미지 받을 때마다 "Hit" 애니메이션 재생 (Line 132-138):
```java
public void damage(DamageInfo info) {
  super.damage(info);
  if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
    this.state.setAnimation(0, "Hit", false);
    this.state.addAnimation(0, "Idle", true, 0.0F);
  }
}
```

## AI 로직 (getMove)

### 모든 난이도 공통 (Line 101-116)
```
Roll 0-59 (60%):
  - lastTwoMoves(BITE) → GROW
  - else → BITE

Roll 60-99 (40%):
  - lastMove(GROW) → BITE
  - else → GROW
```

**특징**:
- 기본 60% BITE 선호
- BITE 2연속 후 반드시 GROW
- GROW 직후 반드시 BITE
- 매우 단순하고 예측 가능한 패턴

```java
// Line 101-116
protected void getMove(int num) {
  if (num < 60) {
    if (lastTwoMoves((byte)1)) {
      setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
    } else {
      setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    }
  } else if (lastMove((byte)2)) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  } else {
    setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
  }
}
```

## 수정 예시

### 1. BITE 데미지 증가 (9로 변경)
```java
// Line 52, 55 수정
this.biteDamage = 9; // 기존: 6
this.damage.add(new DamageInfo((AbstractCreature)this, 9));
```

### 2. GROW 효과 대폭 증가 (A17+ 기준 +7)
```java
// Line 85-90 수정
case 2:
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, 7), 7)); // 5 → 7
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, this.strAmt), this.strAmt));
  }
```

### 3. SporeCloudPower 강화 (4로 증가)
```java
// Line 71 수정
AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
  new SporeCloudPower(this, 4))); // 2 → 4
```

### 4. AI 변경: GROW 우선 전략
```java
// Line 101-116을 다음으로 교체
protected void getMove(int num) {
  if (this.moveHistory.isEmpty()) {
    // 첫 턴은 항상 GROW
    setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
  } else if (num < 40) { // 40% BITE
    if (lastMove((byte)1)) {
      setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
    } else {
      setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    }
  } else { // 60% GROW
    if (lastMove((byte)2)) {
      setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    } else {
      setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
    }
  }
}
```

### 5. 죽을 때 추가 효과 (독 부여)
```java
// Line 138 이후에 die() 메서드 추가
@Override
public void die() {
  super.die();
  // 죽을 때 플레이어에게 독 3 부여
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(
    AbstractDungeon.player, this,
    new PoisonPower(AbstractDungeon.player, this, 3), 3));
}
```

## 중요 필드
| 필드명 | 타입 | 설명 | 코드 위치 |
|--------|------|------|-----------|
| biteDamage | int | 물기 데미지 (6 고정) | Line 32 |
| strAmt | int | 성장 힘 증가량 (3 or 4) | Line 33 |
| BITE_DMG | static final int | 물기 기본 데미지 (6) | Line 34 |
| GROW_STR | static final int | 성장 기본량 (3) | Line 35 |
| A_2_GROW_STR | static final int | A2+ 성장량 (4) | Line 36 |

## 전투 팁
1. **SporeCloudPower 주의**: 죽일 때 취약 2턴 부여됨
2. **GROW 패턴 예측**: BITE 2연속 후 반드시 GROW 사용
3. **힘 증가 누적**: GROW를 여러 번 사용하면 데미지가 급증
4. **2마리 조우**: TwoFungiBeasts 조우 시 SporeCloud 효과 2배 주의

## 관련 파일
- **애니메이션**: `images/monsters/theBottom/fungi/skeleton.atlas/json`
- **Powers**:
  - `SporeCloudPower`: 죽을 때 플레이어에게 취약 부여
  - `StrengthPower`: 공격력 증가
- **특이사항**: 가장 단순한 AI 패턴 (2가지 행동만 존재)
