# Slimes Complete Guide (슬라임 완전 가이드)

1막의 모든 슬라임 몬스터 패턴 정리 (Acid 3종, Spike 3종)

---

## AcidSlime_S (산성 슬라임 소형)

### 기본 정보
- **ID**: `AcidSlime_S`
- **HP**: 8-12 (A0-6) / 9-13 (A7+)
- **특징**: 가장 작고 약한 슬라임, 약화 디버프 보유

### 패턴
1. **TACKLE (1)**: 공격 3 (A0-1) / 4 (A2+)
2. **DEBUFF (2)**: 약화 1턴 부여

### AI 로직

**getMove() - 첫 턴 설정**:

**A17+** (Line 85-90):
```
- lastTwoMoves(TACKLE) → TACKLE 반복
- else → DEBUFF
```

**A0-16** (Line 92-96):
```
- 50% random → TACKLE
- 50% random → DEBUFF
```

**takeTurn() - 자동 교대 시스템** (Line 63-80):
```java
case 1: // TACKLE
  // 공격 실행
  setMove((byte)2, DEBUFF);  // 다음 턴: DEBUFF
  break;

case 2: // DEBUFF
  // 약화 부여
  setMove((byte)1, ATTACK, damage);  // 다음 턴: TACKLE
  break;
```

**중요**: getMove()는 **첫 턴에만** 호출되며, 이후 턴은 takeTurn()에서 자동으로 다음 패턴 설정

**실제 동작**:
- **첫 턴**: getMove()가 패턴 결정 (A17+는 DEBUFF 우선, A0-16은 랜덤)
- **이후 턴**: TACKLE ↔ DEBUFF 완벽한 교대 반복 (확률 없음)

**특징**: A17+는 첫 턴 DEBUFF 후 교대, A0-16은 첫 턴 랜덤 후 교대

---

## AcidSlime_M (산성 슬라임 중형)

### 기본 정보
- **ID**: `AcidSlime_M`
- **HP**: 28-32 (A0-6) / 29-34 (A7+)
- **특징**: 점액 카드 부여 + 약화 디버프

### 생성자
```java
public AcidSlime_M(float x, float y, int poisonAmount, int newHealth)
```
- poisonAmount: 생성 시 독 파워 부여 (slime split 용)
- newHealth: 분열 후 HP 조정용

### 패턴
1. **WOUND_TACKLE (1)**: 데미지 7 (A0-1) / 8 (A2+) + 점액 카드 1장
2. **NORMAL_TACKLE (2)**: 데미지 10 (A0-1) / 12 (A2+)
3. **WEAK_LICK (4)**: 약화 1턴 부여

### AI 로직 (복잡)

**A17+** (Line 106-143):
```
Roll 0-39 (40%):
  lastTwoMoves(WOUND_TACKLE) → 50% NORMAL_TACKLE, 50% WEAK_LICK
  else → WOUND_TACKLE

Roll 40-79 (40%):
  lastTwoMoves(NORMAL_TACKLE) → 50% WOUND_TACKLE, 40% WEAK_LICK
  else → NORMAL_TACKLE

Roll 80-99 (20%):
  lastMove(WEAK_LICK) → 40% WOUND_TACKLE, 60% NORMAL_TACKLE
  else → WEAK_LICK
```

**A0-16** (Line 145-179):
- 유사하지만 확률 및 lastMove 조건이 약간 다름
- 전반적으로 NORMAL_TACKLE 선호도가 높음

### die() - Boss Slime Check
Line 184-193: SlimeBoss 방에서 죽으면 업적 해제

---

## AcidSlime_L (산성 슬라임 대형)

### 기본 정보
- **ID**: `AcidSlime_L`
- **HP**: 65-69 (A0-6) / 68-72 (A7+)
- **특징**: **분열(SPLIT)** 메커니즘 보유

### 패턴
1. **SLIME_TACKLE (1)**: 11 (A0-1) / 12 (A2+) + 점액 2장
2. **NORMAL_TACKLE (2)**: 16 (A0-1) / 18 (A2+)
3. **SPLIT (3)**: **HP 50% 이하 시 강제 발동**
4. **WEAK_LICK (4)**: 약화 2턴 부여

### SPLIT 메커니즘 (Line 123-141)
```java
case 3:
  AbstractDungeon.actionManager.addToBottom(new CannotLoseAction());
  AbstractDungeon.actionManager.addToBottom(new AnimateShakeAction(this, 1.0F, 0.1F));
  AbstractDungeon.actionManager.addToBottom(new HideHealthBarAction(this));
  AbstractDungeon.actionManager.addToBottom(new SuicideAction(this, false));
  AbstractDungeon.actionManager.addToBottom(new WaitAction(1.0F));
  AbstractDungeon.actionManager.addToBottom(new SFXAction("SLIME_SPLIT"));

  // 2마리의 AcidSlime_M 생성 (현재 HP 상속)
  AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(
    new AcidSlime_M(this.saveX - 134.0F, this.saveY + MathUtils.random(-4.0F, 4.0F),
      0, this.currentHealth), false));
  AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(
    new AcidSlime_M(this.saveX + 134.0F, this.saveY + MathUtils.random(-4.0F, 4.0F),
      0, this.currentHealth), false));

  AbstractDungeon.actionManager.addToBottom(new CanLoseAction());
```

### damage() - 강제 분열 트리거 (Line 146-156)
```java
if (!this.isDying && this.currentHealth <= this.maxHealth / 2.0F &&
    this.nextMove != 3 && !this.splitTriggered) {
  setMove(SPLIT_NAME, (byte)3, AbstractMonster.Intent.UNKNOWN);
  createIntent();
  AbstractDungeon.actionManager.addToBottom(new TextAboveCreatureAction(this,
    TextAboveCreatureAction.TextType.INTERRUPTED));
  AbstractDungeon.actionManager.addToBottom(new SetMoveAction(this, SPLIT_NAME, (byte)3,
    AbstractMonster.Intent.UNKNOWN));
  this.splitTriggered = true;
}
```

**특징**: HP 50% 이하가 되는 순간 **현재 패턴 중단**하고 SPLIT 강제 실행

### AI 로직
AcidSlime_M과 유사하지만 SPLIT가 강제로 끼어듦

---

## SpikeSlime_S (가시 슬라임 소형)

### 기본 정보
- **ID**: `SpikeSlime_S`
- **HP**: 10-14 (A0-6) / 11-15 (A7+)
- **특징**: 가장 단순, 공격만 함

### 패턴
1. **TACKLE (1)**: 5 (A0-1) / 6 (A2+)

### AI 로직
```java
// Line 74-76
protected void getMove(int num) {
  setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
}
```

**특징**: **AI가 존재하지 않음** - 항상 TACKLE만 사용

---

## SpikeSlime_M (가시 슬라임 중형)

### 기본 정보
- **ID**: `SpikeSlime_M`
- **HP**: 28-32 (A0-6) / 29-34 (A7+)
- **특징**: 점액 + 나약(Frail) 디버프

### 패턴
1. **FLAME_TACKLE (1)**: 8 (A0-1) / 10 (A2+) + 점액 1장
2. **FRAIL_LICK (4)**: 나약 1턴 부여

### AI 로직

**A17+** (Line 96-111):
```
Roll 0-29 (30%):
  lastTwoMoves(FLAME_TACKLE) → FRAIL_LICK
  else → FLAME_TACKLE

Roll 30-99 (70%):
  lastMove(FRAIL_LICK) → FLAME_TACKLE
  else → FRAIL_LICK
```

**A0-16** (Line 113-125):
- lastTwoMoves 조건 동일
- lastMove → lastTwoMoves로 변경 (덜 공격적)

---

## SpikeSlime_L (가시 슬라임 대형)

### 기본 정보
- **ID**: `SpikeSlime_L`
- **HP**: 64-70 (A0-6) / 67-73 (A7+)
- **특징**: **분열(SPLIT)** + 나약 디버프

### 패턴
1. **FLAME_TACKLE (1)**: 16 (A0-1) / 18 (A2+) + 점액 2장
2. **SPLIT (3)**: HP 50% 이하 시 강제 발동
3. **FRAIL_LICK (4)**: 나약 2턴 (A0-16) / 3턴 (A17+)

### SPLIT 메커니즘
AcidSlime_L과 동일하지만 **SpikeSlime_M 2마리**로 분열 (Line 125-137)

### FRAIL A17+ 강화 (Line 94-104)
```java
case 4:
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new FrailPower(AbstractDungeon.player, 3, true), 3)); // 2턴 → 3턴
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new FrailPower(AbstractDungeon.player, 2, true), 2));
  }
```

### AI 로직
SpikeSlime_M과 동일

---

## 슬라임 공통 특징

### 1. 애니메이션
- **Acid**: `images/monsters/theBottom/slimeS/M/L/skeleton.atlas`
- **Spike**: `images/monsters/theBottom/slimeAltS/M/L/skeleton.atlas`
- 모두 `SlimeAnimListener` 사용

### 2. 생성 시 독 파워
모든 Medium/Large 슬라임은 생성자에서 poisonAmount 지원:
```java
if (poisonAmount >= 1) {
  this.powers.add(new PoisonPower(this, this, poisonAmount));
}
```
이는 분열 시 독을 상속하기 위한 메커니즘

### 3. 분열(SPLIT) 공통 로직
- HP 50% 이하 시 damage() 메서드에서 강제 트리거
- splitTriggered 플래그로 1회만 실행
- CannotLoseAction → SuicideAction → SpawnMonsterAction 순서
- 생성된 슬라임은 현재 HP를 상속

### 4. Boss Slime 체크
Medium/Large 슬라임의 die()에서 보스방 확인:
```java
if (AbstractDungeon.getMonsters().areMonstersBasicallyDead() &&
    AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) {
  onBossVictoryLogic();
  UnlockTracker.hardUnlockOverride("SLIME");
  UnlockTracker.unlockAchievement("SLIME_BOSS");
}
```

---

## 슬라임 수정 예시

### 1. 분열 HP 임계값 변경 (75%로)
```java
// AcidSlime_L, SpikeSlime_L의 damage() 메서드 Line 149 수정
if (!this.isDying && this.currentHealth <= this.maxHealth * 0.75F && // 0.5 → 0.75
    this.nextMove != 3 && !this.splitTriggered) {
```

### 2. 분열 후 슬라임 HP 증가
```java
// SPLIT 패턴의 SpawnMonsterAction에서 currentHealth * 1.5 적용
new AcidSlime_M(x, y, 0, (int)(this.currentHealth * 1.5))
```

### 3. 작은 슬라임에 약화 추가
```java
// AcidSlime_S, SpikeSlime_S의 TACKLE에 디버프 추가
case 1:
  AbstractDungeon.actionManager.addToBottom(new AnimateFastAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
  // 추가
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new WeakPower(AbstractDungeon.player, 1, true), 1));
```

### 4. 점액 카드 개수 증가
```java
// Medium/Large의 WOUND_TACKLE/FLAME_TACKLE에서
new MakeTempCardInDiscardAction(new Slimed(), 3) // 1 or 2 → 3
```

---

## 슬라임 전투 팁

1. **소형(S)**: 가장 약하지만 무시하면 디버프 누적
2. **중형(M)**: 점액 카드로 덱 오염 주의
3. **대형(L)**: HP 50% 전에 강력한 공격으로 한 번에 처치 권장
4. **분열 대비**: 광역 공격 또는 단일 고화력 준비
5. **Acid vs Spike**:
   - Acid: 약화(데미지 25% 감소) - 공격 위주 덱에 위협
   - Spike: 나약(블록 25% 감소) - 방어 위주 덱에 위협

---

## 관련 파일
- **Powers**: `PoisonPower`, `WeakPower`, `FrailPower`, `SplitPower`
- **Cards**: `Slimed` (저주-아님, 무색 1코 아무것도 안 함)
- **Actions**: `SpawnMonsterAction`, `SuicideAction`, `CannotLoseAction`, `CanLoseAction`
- **Helpers**: `SlimeAnimListener` (애니메이션 리스너)
