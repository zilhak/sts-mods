# Looter (도적)

## 기본 정보
- **클래스명**: `Looter`
- **파일 경로**: `com/megacrit/cardcrawl/monsters/exordium/Looter.java`
- **Monster ID**: `Looter`
- **타입**: 일반 몬스터
- **지역**: Exordium (1막)
- **특징**: **골드 도둑 메커니즘** - ThieveryPower로 공격 시 골드 훔침

## HP 정보
| 난이도 | HP 범위 | 코드 위치 |
|--------|---------|-----------|
| 기본 (A0-6) | 44-48 | Line 59 |
| A7+ | 46-50 | Line 57 |

## 생성자 정보
- 애니메이션: `images/monsters/theBottom/looter/skeleton.atlas`
- 골드 훔침량: 15 (A0-16) / 20 (A17+) - Line 51-54
- dialogX, dialogY 설정됨 (대사 위치) - Line 47-48

## 패턴 정보

### 1. MUG (강탈) - 바이트값: 1
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 10 | Line 66 |
| A2+ | 11 | Line 63 |

**특수 효과**: 데미지와 함께 골드 도둑 (Line 96-105)
- 공격 전에 stolenGold 누적 (Line 100)
- 실제 골드는 DamageAction의 goldAmt 매개변수로 처리
- 첫 사용 시 60% 확률로 대사 출력 (Line 90-92)

**AI 제어**:
- slashCount 카운터로 사용 횟수 추적 (Line 107)
- 2회 사용 후 SMOKE_BOMB 또는 LUNGE로 전환 (Line 108-114)

```java
// Line 89-118
case 1:
  if (this.slashCount == 0 && AbstractDungeon.aiRng.randomBoolean(0.6F)) {
    AbstractDungeon.actionManager.addToBottom(new TalkAction(this, SLASH_MSG1, 0.3F, 2.0F));
  }
  playSfx();
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new AbstractGameAction() {
    public void update() {
      Looter.this.stolenGold = Looter.this.stolenGold +
        Math.min(Looter.this.goldAmt, AbstractDungeon.player.gold);
      this.isDone = true;
    }
  });
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), this.goldAmt));

  this.slashCount++;
  if (this.slashCount == 2) {
    if (AbstractDungeon.aiRng.randomBoolean(0.5F)) {
      setMove((byte)2, AbstractMonster.Intent.DEFEND);
    } else {
      AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[0],
        (byte)4, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(1)).base));
    }
  } else {
    AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1],
      (byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base));
  }
```

### 2. SMOKE_BOMB (연막탄) - 바이트값: 2
**의도**: DEFEND
**효과**: 방어도 6 획득
| 난이도 | 방어도 | 코드 위치 |
|--------|--------|-----------|
| 모든 난이도 | 6 | Line 35, 138 |

**실행**: Line 136-140
```java
case 2:
  AbstractDungeon.actionManager.addToBottom(new TalkAction(this, SMOKE_BOMB_MSG, 0.75F, 2.5F));
  AbstractDungeon.actionManager.addToBottom(new GainBlockAction(this, this, this.escapeDef));
  AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)3,
    AbstractMonster.Intent.ESCAPE));
```

**특징**: 사용 후 다음 턴에 반드시 ESCAPE 사용

### 3. ESCAPE (도주) - 바이트값: 3
**의도**: ESCAPE
**효과**: 전투에서 도주 (Line 141-147)
- 도주 대사 출력
- room.mugged = true 설정 (Line 143)
- SmokeBombEffect 시각 효과 (Line 144)
- EscapeAction 실행 (Line 145)

```java
case 3:
  AbstractDungeon.actionManager.addToBottom(new TalkAction(this, RUN_MSG, 0.3F, 2.5F));
  (AbstractDungeon.getCurrRoom()).mugged = true;
  AbstractDungeon.actionManager.addToBottom(new VFXAction(new SmokeBombEffect(
    this.hb.cX, this.hb.cY)));
  AbstractDungeon.actionManager.addToBottom(new EscapeAction(this));
  AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, (byte)3,
    AbstractMonster.Intent.ESCAPE));
```

### 4. LUNGE (돌진) - 바이트값: 4
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 12 | Line 67 |
| A2+ | 14 | Line 64 |

**특수 효과**: 골드 도둑 + 다음 턴 SMOKE_BOMB (Line 120-135)
- MUG와 동일하게 골드 훔침
- slashCount++ 수행 (Line 122)
- 사용 후 반드시 SMOKE_BOMB로 전환 (Line 134)

## 특수 동작

### usePreBattleAction() - ThieveryPower
전투 시작 시 **골드 도둑 파워** 부여 (Line 82-84):
| 난이도 | 골드량 | 코드 위치 |
|--------|--------|-----------|
| A0-16 | 15 | Line 53 |
| A17+ | 20 | Line 51 |

```java
public void usePreBattleAction() {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this,
    new ThieveryPower(this, this.goldAmt)));
}
```

### die() - 골드 반환
죽을 때 훔친 골드를 보상으로 반환 (Line 176-190):
```java
public void die() {
  playDeathSfx();
  this.state.setTimeScale(0.1F);
  useShakeAnimation(5.0F);
  if (MathUtils.randomBoolean(0.3F)) {
    AbstractDungeon.effectList.add(new SpeechBubble(
      this.hb.cX + this.dialogX, this.hb.cY + this.dialogY,
      2.0F, DEATH_MSG1, false));
    if (!Settings.FAST_MODE) {
      this.deathTimer += 1.5F;
    }
  }
  if (this.stolenGold > 0) {
    AbstractDungeon.getCurrRoom().addStolenGoldToRewards(this.stolenGold);
  }
  super.die();
}
```

### playSfx() / playDeathSfx()
음성 효과 랜덤 재생 (Line 153-173):
- 공격 시: VO_LOOTER_1A/1B/1C 중 랜덤
- 죽을 때: VO_LOOTER_2A/2B/2C 중 랜덤

## AI 로직 (getMove)

### 모든 난이도 공통 (Line 193-195)
```java
protected void getMove(int num) {
  setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
}
```

**특징**: **getMove는 첫 턴만 호출됨**
- 이후는 takeTurn 내부의 SetMoveAction으로 패턴 결정
- 고정된 순서: MUG → MUG → (50% SMOKE_BOMB or 50% LUNGE) → ESCAPE

**패턴 순서**:
1. **Turn 1**: MUG (slashCount=0 → 1)
2. **Turn 2**: MUG (slashCount=1 → 2)
3. **Turn 3**:
   - 50%: SMOKE_BOMB → Turn 4 ESCAPE
   - 50%: LUNGE (slashCount=3) → Turn 4 SMOKE_BOMB → Turn 5 ESCAPE

## 수정 예시

### 1. 훔치는 골드량 증가
```java
// Line 50-54 수정
if (AbstractDungeon.ascensionLevel >= 17) {
  this.goldAmt = 30; // 20 → 30
} else {
  this.goldAmt = 25; // 15 → 25
}
```

### 2. ESCAPE 확률 낮추기 (3번 공격 후 도주)
```java
// Line 108-118 수정
if (this.slashCount == 3) { // 2 → 3
  if (AbstractDungeon.aiRng.randomBoolean(0.5F)) {
    setMove((byte)2, AbstractMonster.Intent.DEFEND);
  } else {
    // ... LUNGE
  }
}
```

### 3. 도주 불가능하게 만들기
```java
// Line 136-147 SMOKE_BOMB과 ESCAPE case를 다음으로 교체
case 2:
  // SMOKE_BOMB 사용 후 MUG로 복귀
  AbstractDungeon.actionManager.addToBottom(new TalkAction(this, SMOKE_BOMB_MSG, 0.75F, 2.5F));
  AbstractDungeon.actionManager.addToBottom(new GainBlockAction(this, this, this.escapeDef));
  this.slashCount = 0; // 카운터 초기화
  AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, MOVES[1], (byte)1,
    AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base));
  break;

// case 3 ESCAPE 제거
```

### 4. 골드 도둑 후 즉시 반환 (훔치기 제거)
```java
// Line 96-103의 Anonymous AbstractGameAction 제거
// Line 104-105 수정
AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
  this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
// goldAmt 매개변수 제거
```

## 중요 필드
| 필드명 | 타입 | 설명 | 코드 위치 |
|--------|------|------|-----------|
| swipeDmg | int | MUG 데미지 | Line 33 |
| lungeDmg | int | LUNGE 데미지 | Line 34 |
| escapeDef | int | SMOKE_BOMB 방어도 (6) | Line 35 |
| goldAmt | int | 훔치는 골드량 (15/20) | Line 35 |
| slashCount | int | 공격 횟수 카운터 | Line 41 |
| stolenGold | int | 실제 훔친 골드 누적 | Line 42 |

## 전투 팁
1. **골드 보호**: 공격 시 최소 goldAmt만큼 골드 소실 (최대 player.gold)
2. **도주 타이밍**: 2-3턴 공격 후 연막탄 → 도주 패턴
3. **죽이면 골드 회수**: die()에서 훔친 골드 보상으로 반환
4. **첫 턴 예측 불가**: getMove는 첫 턴만, 이후는 SetMoveAction

## 관련 파일
- **애니메이션**: `images/monsters/theBottom/looter/skeleton.atlas/json`
- **Powers**: `ThieveryPower` (공격 시 골드 훔치기)
- **Effects**: `SmokeBombEffect` (연막탄 시각 효과)
- **Actions**: `EscapeAction` (도주 처리)
- **특이사항**: 가장 복잡한 패턴 시퀀스 (getMove + SetMoveAction 혼합)
