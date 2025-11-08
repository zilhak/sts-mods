# LouseNormal (공벌레 일반)

## 기본 정보
- **클래스명**: `LouseNormal`
- **파일 경로**: `com/megacrit/cardcrawl/monsters/exordium/LouseNormal.java`
- **Monster ID**: `FuzzyLouseNormal`
- **타입**: 일반 몬스터
- **지역**: Exordium (1막)
- **특수 조우**: ThreeLouse (3마리 조우 가능)

## HP 정보
| 난이도 | HP 범위 | 코드 위치 |
|--------|---------|-----------|
| 기본 (A0-6) | 10-15 | Line 51 |
| A7+ | 11-16 | Line 49 |

```java
// Line 48-52
if (AbstractDungeon.ascensionLevel >= 7) {
  setHp(11, 16);
} else {
  setHp(10, 15);
}
```

## 생성자 정보
```java
// Line 38
public LouseNormal(float x, float y)
```
- 애니메이션: `images/monsters/theBottom/louseRed/skeleton.atlas` (빨간색 공벌레)
- HP는 `AbstractDungeon.monsterHpRng`를 사용하여 랜덤 결정
- biteDamage는 생성 시 랜덤으로 결정됨

## 패턴 정보

### 1. BITE (물기) - 바이트값: 3
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 5-7 (랜덤) | Line 57 |
| A2+ | 6-8 (랜덤) | Line 55 |

**효과**:
- 단일 공격
- 데미지는 몬스터 생성 시 결정되며 전투 중 고정

**애니메이션 특이사항**:
- 닫힌 상태(!isOpen)일 경우: CLOSED → OPEN 전환 후 공격 (Line 81-84)
- 열린 상태일 경우: 즉시 공격

```java
// Line 80-88: BITE 패턴 실행
case 3:
  if (!this.isOpen) {
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OPEN"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
  }
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction((AbstractCreature)this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
```

### 2. STRENGTHEN (강화) - 바이트값: 4
**의도**: BUFF
**효과**:
| 난이도 | 힘 증가 | 코드 위치 |
|--------|---------|-----------|
| A0-16 | +3 | Line 101 |
| A17+ | +4 | Line 98 |

**애니메이션 특이사항**:
- 닫힌 상태(!isOpen): CLOSED → REAR → idle 전환 (1.2초 대기, Line 91-92)
- 열린 상태(isOpen): REAR_IDLE → idle 전환 (0.9초 대기, Line 94-95)

```java
// Line 89-102: STRENGTHEN 패턴 실행
case 4:
  if (!this.isOpen) {
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REAR"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(1.2F));
  } else {
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REAR_IDLE"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.9F));
  }
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, 4), 4));
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new StrengthPower(this, 3), 3));
  }
```

## 특수 동작

### usePreBattleAction() - 전투 시작 시
**CurlUpPower 부여** (방어막을 얻으면 블록 해제 및 데미지):
| 난이도 | 블록량 | 코드 위치 |
|--------|--------|-----------|
| A0-6 | 3-7 (랜덤) | Line 72-73 |
| A7-16 | 4-8 (랜덤) | Line 69-70 |
| A17+ | 9-12 (랜덤) | Line 66-67 |

```java
// Line 64-75
public void usePreBattleAction() {
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new CurlUpPower(this, AbstractDungeon.monsterHpRng.random(9, 12))));
  } else if (AbstractDungeon.ascensionLevel >= 7) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new CurlUpPower(this, AbstractDungeon.monsterHpRng.random(4, 8))));
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
      new CurlUpPower(this, AbstractDungeon.monsterHpRng.random(3, 7))));
  }
}
```

### changeState(String stateName) - 상태 변경
4가지 상태 지원 (Line 112-131):
1. **"CLOSED"**: 닫힌 상태로 전환
2. **"OPEN"**: 열린 상태로 전환
3. **"REAR_IDLE"**: 뒤로 물러서는 애니메이션 (열린 상태 유지)
4. **"REAR"**: 닫힌 상태에서 뒤로 물러서기 (CLOSED → OPEN → REAR 순서)

## AI 로직 (getMove)

### A17+ AI (공격적 버전, Line 135-148)
```
Roll 0-24 (25%):
  - lastMove(STRENGTHEN) → BITE
  - else → STRENGTHEN

Roll 25-99 (75%):
  - lastTwoMoves(BITE) → STRENGTHEN
  - else → BITE
```

**특징**:
- STRENGTHEN 후 반드시 BITE
- BITE를 2연속 사용하면 STRENGTHEN
- 25% 확률로 STRENGTHEN 우선 고려

### A0-16 AI (균형 버전, Line 150-161)
```
Roll 0-24 (25%):
  - lastTwoMoves(STRENGTHEN) → BITE
  - else → STRENGTHEN

Roll 25-99 (75%):
  - lastTwoMoves(BITE) → STRENGTHEN
  - else → BITE
```

**특징**:
- STRENGTHEN 2연속 방지
- BITE 2연속 후 STRENGTHEN
- 기본적으로 75% BITE 선호

```java
// Line 134-162: 전체 AI 로직
protected void getMove(int num) {
  if (AbstractDungeon.ascensionLevel >= 17) {
    if (num < 25) {
      if (lastMove((byte)4)) {
        setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
      } else {
        setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
      }
    } else if (lastTwoMoves((byte)3)) {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
    } else {
      setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    }
  } else if (num < 25) {
    if (lastTwoMoves((byte)4)) {
      setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    } else {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
    }
  } else if (lastTwoMoves((byte)3)) {
    setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
  } else {
    setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  }
}
```

## 수정 예시

### 1. 데미지 증가 (A2+ 기준 10-12로 변경)
```java
// Line 54-58 수정
if (AbstractDungeon.ascensionLevel >= 2) {
  this.biteDamage = AbstractDungeon.monsterHpRng.random(10, 12); // 6,8 → 10,12
} else {
  this.biteDamage = AbstractDungeon.monsterHpRng.random(5, 7);
}
```

### 2. 강화 효과 증가 (A17+ 기준 +6으로 변경)
```java
// Line 97-101 수정
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new StrengthPower(this, 6), 6)); // 4 → 6
}
```

### 3. CurlUp 블록량 증가 (A17+ 기준 12-15로 변경)
```java
// Line 65-67 수정
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new CurlUpPower(this, AbstractDungeon.monsterHpRng.random(12, 15)))); // 9,12 → 12,15
}
```

### 4. AI 변경: 항상 STRENGTHEN 먼저 사용
```java
// Line 134-162를 다음으로 교체
protected void getMove(int num) {
  if (this.moveHistory.isEmpty()) {
    // 첫 턴은 항상 STRENGTHEN
    setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
  } else if (lastMove((byte)4)) {
    // STRENGTHEN 후에는 BITE
    setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  } else {
    // BITE 후에는 STRENGTHEN
    setMove(MOVES[0], (byte)4, AbstractMonster.Intent.BUFF);
  }
}
```

## 중요 필드
| 필드명 | 타입 | 설명 | 코드 위치 |
|--------|------|------|-----------|
| biteDamage | int | 물기 데미지 (생성 시 결정) | Line 35 |
| isOpen | boolean | 열린 상태 여부 | Line 31 |
| STR_AMOUNT | static final int | 강화 기본량 (3) | Line 36 |

## 관련 파일
- **애니메이션**: `images/monsters/theBottom/louseRed/skeleton.atlas/json`
- **유사 몬스터**: `LouseDefensive` (방어형 공벌레 - 녹색)
- **Powers**:
  - `CurlUpPower`: 블록 획득 시 데미지 + 블록 해제
  - `StrengthPower`: 공격력 증가
