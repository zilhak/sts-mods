# LouseDefensive (공벌레 방어형)

## 기본 정보
- **클래스명**: `LouseDefensive`
- **파일 경로**: `com/megacrit/cardcrawl/monsters/exordium/LouseDefensive.java`
- **Monster ID**: `FuzzyLouseDefensive`
- **타입**: 일반 몬스터
- **지역**: Exordium (1막)

## HP 정보
| 난이도 | HP 범위 | 코드 위치 |
|--------|---------|-----------|
| 기본 (A0-6) | 11-17 | Line 54 |
| A7+ | 12-18 | Line 52 |

## 생성자 정보
- 애니메이션: `images/monsters/theBottom/louseGreen/skeleton.atlas` (녹색 공벌레)
- biteDamage는 생성 시 랜덤 결정

## 패턴 정보

### 1. BITE (물기) - 바이트값: 3
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 5-7 (랜덤) | Line 60 |
| A2+ | 6-8 (랜덤) | Line 58 |

```java
// Line 85-91
if (!this.isOpen) {
  AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "OPEN"));
  AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
}
AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
  this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
```

### 2. WEAKEN (약화) - 바이트값: 4
**의도**: DEBUFF
**효과**: 플레이어에게 약화 2턴 부여

**애니메이션 + 특수효과**:
- 닫힌 상태: REAR 전환 + 1.2초 대기 + 거미줄 이펙트 (Line 94-99)
- 열린 상태: REAR_IDLE 전환 + 0.9초 대기 + 거미줄 이펙트 (Line 106-110)
- 거미줄 위치: `hb.cX - 70.0F * Settings.scale, hb.cY + 10.0F * Settings.scale`

```java
// Line 93-118
case 4:
  if (!this.isOpen) {
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REAR"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(1.2F));
    AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_MAGIC_FAST_3",
      MathUtils.random(0.88F, 0.92F), true));
    AbstractDungeon.actionManager.addToBottom(new VFXAction(new WebEffect(
      AbstractDungeon.player, this.hb.cX - 70.0F * Settings.scale,
      this.hb.cY + 10.0F * Settings.scale)));
  } else {
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "REAR_IDLE"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.9F));
    AbstractDungeon.actionManager.addToBottom(new SFXAction("ATTACK_MAGIC_FAST_3",
      MathUtils.random(0.88F, 0.92F), true));
    AbstractDungeon.actionManager.addToBottom(new VFXAction(new WebEffect(
      AbstractDungeon.player, this.hb.cX - 70.0F * Settings.scale,
      this.hb.cY + 10.0F * Settings.scale)));
  }
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new WeakPower(AbstractDungeon.player, 2, true), 2));
```

## 특수 동작

### usePreBattleAction() - CurlUpPower
LouseNormal과 동일한 값 사용 (Line 68-79):
| 난이도 | 블록량 | 코드 위치 |
|--------|--------|-----------|
| A0-6 | 3-7 (랜덤) | Line 76-77 |
| A7-16 | 4-8 (랜덤) | Line 73-74 |
| A17+ | 9-12 (랜덤) | Line 70-71 |

### changeState() - 상태 전환
LouseNormal과 동일 (Line 131-150)

## AI 로직 (getMove)

### A17+ AI (공격적 버전, Line 154-167)
```
Roll 0-24 (25%):
  - lastMove(WEAKEN) → BITE
  - else → WEAKEN

Roll 25-99 (75%):
  - lastTwoMoves(BITE) → WEAKEN
  - else → BITE
```

**특징**: LouseNormal A17+와 동일한 로직, WEAKEN/BUFF 차이만 존재

### A0-16 AI (균형 버전, Line 169-180)
```
Roll 0-24 (25%):
  - lastTwoMoves(WEAKEN) → BITE
  - else → WEAKEN

Roll 25-99 (75%):
  - lastTwoMoves(BITE) → WEAKEN
  - else → BITE
```

**특징**: LouseNormal A0-16과 동일한 로직

```java
// Line 153-181: 전체 AI 로직
protected void getMove(int num) {
  if (AbstractDungeon.ascensionLevel >= 17) {
    if (num < 25) {
      if (lastMove((byte)4)) {
        setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
      } else {
        setMove(MOVES[0], (byte)4, AbstractMonster.Intent.DEBUFF);
      }
    } else if (lastTwoMoves((byte)3)) {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.DEBUFF);
    } else {
      setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    }
  } else if (num < 25) {
    if (lastTwoMoves((byte)4)) {
      setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    } else {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.DEBUFF);
    }
  } else if (lastTwoMoves((byte)3)) {
    setMove(MOVES[0], (byte)4, AbstractMonster.Intent.DEBUFF);
  } else {
    setMove((byte)3, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  }
}
```

## 수정 예시

### 1. 약화 턴수 증가 (3턴으로 변경)
```java
// Line 117 수정
AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
  this, new WeakPower(AbstractDungeon.player, 3, true), 3)); // 2 → 3
```

### 2. A17+에서 약화 + 취약 동시 부여
```java
// Line 117 이후에 추가
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new VulnerablePower(AbstractDungeon.player, 1, true), 1));
}
```

### 3. 거미줄 이펙트 위치 변경
```java
// Line 99, 110의 WebEffect 좌표 수정
new WebEffect(AbstractDungeon.player,
  this.hb.cX - 100.0F * Settings.scale,  // -70 → -100
  this.hb.cY + 30.0F * Settings.scale)    // +10 → +30
```

## 중요 필드
| 필드명 | 타입 | 설명 | 코드 위치 |
|--------|------|------|-----------|
| biteDamage | final int | 물기 데미지 (생성 시 결정) | Line 38 |
| isOpen | boolean | 열린 상태 여부 | Line 34 |
| WEAK_AMT | static final int | 약화 턴수 (2) | Line 39 |

## LouseNormal과의 차이점
1. **애니메이션**: 빨간색(Normal) vs 녹색(Defensive)
2. **HP**: Defensive가 1 높음 (기본 11-17 vs 10-15)
3. **패턴2 효과**:
   - Normal: 자신에게 힘 +3/4
   - Defensive: 플레이어에게 약화 2턴
4. **시각 효과**: Defensive는 거미줄(WebEffect) 사용
5. **사운드**: Defensive는 "ATTACK_MAGIC_FAST_3" 추가

## 관련 파일
- **애니메이션**: `images/monsters/theBottom/louseGreen/skeleton.atlas/json`
- **유사 몬스터**: `LouseNormal` (공격형 공벌레 - 빨간색)
- **Powers**: `CurlUpPower`, `WeakPower`
- **Effects**: `WebEffect`
