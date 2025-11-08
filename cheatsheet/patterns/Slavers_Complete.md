# Slavers Complete Guide (노예 상인 완전 가이드)

1막의 노예 상인 2종 패턴 정리 (SlaverRed, SlaverBlue)

---

## 공통 특징

### 기본 정보
- **HP**: 46-50 (A0-6) / 48-52 (A7+)
- **애니메이션**: `images/monsters/theBottom/redSlaver|blueSlaver/skeleton.atlas`
- **음성**: VO_SLAVERRED_* / VO_SLAVERBLUE_* (공격 2종, 죽음 2종)
- **특징**: 1막 후반부 강력한 디버퍼

### 공통 메서드
- `playSfx()`: 공격 시 랜덤 음성 재생
- `playDeathSfx()`: 죽을 때 음성 재생 (die() 내부 호출)

---

## SlaverRed (빨간 노예 상인)

### 기본 정보
- **ID**: `SlaverRed`
- **HP**: 46-50 (A0-6) / 48-52 (A7+)
- **특징**: **속박(Entangle)** 디버프 보유

### 패턴 정보

#### 1. STAB (찌르기) - 바이트값: 1
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 13 | Line 56 |
| A2+ | 14 | Line 53 |

**첫 턴 고정**: firstTurn 플래그로 첫 턴은 반드시 STAB (Line 153-156)

```java
// Line 95-98
case 1:
  playSfx();
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
```

#### 2. ENTANGLE (속박) - 바이트값: 2
**의도**: STRONG_DEBUFF
**효과**: **EntanglePower** 부여 (다음 턴 공격 불가)

**시각 효과**: EntangleEffect (그물 던지기) - Line 78-81
- 위치: `hb.cX - 70.0F * Settings.scale, hb.cY + 10.0F * Settings.scale`
- 플레이어 위치로 날아감

```java
// Line 76-92
case 2:
  playSfx();
  AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "Use Net"));
  if (this.hb != null && AbstractDungeon.player.hb != null && !Settings.FAST_MODE) {
    AbstractDungeon.actionManager.addToBottom(new VFXAction(new EntangleEffect(
      this.hb.cX - 70.0F * Settings.scale, this.hb.cY + 10.0F * Settings.scale,
      AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY), 0.5F));
  }
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new EntanglePower(AbstractDungeon.player)));

  this.usedEntangle = true; // 1회만 사용
```

**중요**: usedEntangle 플래그로 **전투 중 1회만** 사용 가능

#### 3. SCRAPE (긁기) - 바이트값: 3
**의도**: ATTACK_DEBUFF
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 8 | Line 57 |
| A2+ | 9 | Line 54 |

**디버프**: 취약 1턴 (A0-16) / 2턴 (A17+)

```java
// Line 100-113
case 3:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new VulnerablePower(AbstractDungeon.player, this.VULN_AMT + 1, true),
      this.VULN_AMT + 1)); // 2턴
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new VulnerablePower(AbstractDungeon.player, this.VULN_AMT, true),
      this.VULN_AMT)); // 1턴
  }
```

### changeState() - 애니메이션
"Use Net" 상태 시 그물 없는 대기 애니메이션으로 전환 (Line 145-149):
```java
public void changeState(String stateName) {
  float tmp = this.state.getCurrent(0).getTime();
  AnimationState.TrackEntry e = this.state.setAnimation(0, "idleNoNet", true);
  e.setTime(tmp);
}
```

### AI 로직 (getMove)

**첫 턴 고정** (Line 153-158):
```java
if (this.firstTurn) {
  this.firstTurn = false;
  setMove((byte)1, AbstractMonster.Intent.ATTACK, this.stabDmg);
  return;
}
```

**이후 턴** (Line 160-187):
```
Roll 0-74 (75%) AND !usedEntangle:
  → ENTANGLE (1회만)

Roll 55-99 (45%) AND usedEntangle AND !lastTwoMoves(STAB):
  → STAB

A17+:
  !lastMove(SCRAPE) → SCRAPE
  else → STAB

A0-16:
  !lastTwoMoves(SCRAPE) → SCRAPE
  else → STAB
```

**특징**:
- 첫 턴은 항상 STAB
- 75% 확률로 ENTANGLE 우선 시도 (미사용 시)
- A17+는 SCRAPE 매 턴 번갈아 가능 (더 공격적)
- A0-16은 SCRAPE 2연속 방지

```java
// Line 152-187: 전체 AI 로직
protected void getMove(int num) {
  if (this.firstTurn) {
    this.firstTurn = false;
    setMove((byte)1, AbstractMonster.Intent.ATTACK, this.stabDmg);
    return;
  }

  if (num >= 75 && !this.usedEntangle) {
    setMove(ENTANGLE_NAME, (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
  }

  if (num >= 55 && this.usedEntangle && !lastTwoMoves((byte)1)) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    return;
  }

  if (AbstractDungeon.ascensionLevel >= 17) {
    if (!lastMove((byte)3)) {
      setMove(SCRAPE_NAME, (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
      return;
    }
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  } else {
    if (!lastTwoMoves((byte)3)) {
      setMove(SCRAPE_NAME, (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
      return;
    }
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  }
}
```

---

## SlaverBlue (파란 노예 상인)

### 기본 정보
- **ID**: `SlaverBlue`
- **HP**: 46-50 (A0-6) / 48-52 (A7+)
- **특징**: **약화(Weak)** 디버프 보유

### 패턴 정보

#### 1. STAB (찌르기) - 바이트값: 1
**의도**: ATTACK
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 12 | Line 50 |
| A2+ | 13 | Line 47 |

**SlaverRed보다 1 낮음**

```java
// Line 68-72
case 1:
  playSfx();
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(0), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
```

#### 2. RAKE (할퀴기) - 바이트값: 4
**의도**: ATTACK_DEBUFF
**데미지**:
| 난이도 | 데미지 | 코드 위치 |
|--------|--------|-----------|
| A0-1 | 7 | Line 51 |
| A2+ | 8 | Line 48 |

**디버프**: 약화 1턴 (A0-16) / 2턴 (A17+)

```java
// Line 74-88
case 4:
  AbstractDungeon.actionManager.addToBottom(new AnimateSlowAttackAction(this));
  AbstractDungeon.actionManager.addToBottom(new DamageAction(AbstractDungeon.player,
    this.damage.get(1), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));

  if (AbstractDungeon.ascensionLevel >= 17) {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new WeakPower(AbstractDungeon.player, this.weakAmt + 1, true),
      this.weakAmt + 1)); // 2턴
  } else {
    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
      this, new WeakPower(AbstractDungeon.player, this.weakAmt, true),
      this.weakAmt)); // 1턴
  }
```

### AI 로직 (getMove)

**모든 턴** (Line 122-144):
```
Roll 40-99 (60%) AND !lastTwoMoves(STAB):
  → STAB

A17+:
  !lastMove(RAKE) → RAKE
  else → STAB

A0-16:
  !lastTwoMoves(RAKE) → RAKE
  else → STAB
```

**특징**:
- 첫 턴 고정 없음 (SlaverRed와 차이)
- 60% 확률로 STAB 우선 (STAB 2연속 방지)
- A17+는 RAKE 매 턴 번갈아 가능
- SlaverRed보다 단순한 AI

```java
// Line 122-144: 전체 AI 로직
protected void getMove(int num) {
  if (num >= 40 && !lastTwoMoves((byte)1)) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    return;
  }

  if (AbstractDungeon.ascensionLevel >= 17) {
    if (!lastMove((byte)4)) {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
      return;
    }
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  } else {
    if (!lastTwoMoves((byte)4)) {
      setMove(MOVES[0], (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
      return;
    }
    setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
  }
}
```

---

## Red vs Blue 비교

| 특성 | SlaverRed | SlaverBlue |
|------|-----------|------------|
| **STAB 데미지** | 13/14 | 12/13 |
| **디버프 공격** | SCRAPE (8/9) | RAKE (7/8) |
| **디버프 효과** | 취약 1/2턴 | 약화 1/2턴 |
| **특수 패턴** | ENTANGLE (1회) | 없음 |
| **첫 턴** | 항상 STAB | AI에 따름 |
| **AI 복잡도** | 높음 (3가지 조건) | 낮음 (2가지 조건) |

**전투 난이도**: SlaverRed > SlaverBlue
- Red의 Entangle이 훨씬 위협적 (공격 봉쇄)
- Blue의 약화는 공격 덱에만 영향

---

## Slaver 수정 예시

### 1. SlaverRed Entangle 2회 사용 가능
```java
// Line 76, 92 수정 - usedEntangle 제거
case 2:
  playSfx();
  // ... Entangle 적용
  // this.usedEntangle = true; // 이 줄 제거

// getMove Line 160 수정
if (num >= 75) { // && !this.usedEntangle 제거
  setMove(ENTANGLE_NAME, (byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
  return;
}
```

### 2. SlaverBlue 첫 턴 RAKE 고정
```java
// Line 122에 firstTurn 체크 추가
private boolean firstTurn = true;

protected void getMove(int num) {
  if (this.firstTurn) {
    this.firstTurn = false;
    setMove(MOVES[0], (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
      ((DamageInfo)this.damage.get(1)).base);
    return;
  }
  // 기존 로직 유지
}
```

### 3. A17+ 디버프 턴수 3턴으로 증가
```java
// SlaverRed Line 104-105, SlaverBlue Line 79-80 수정
if (AbstractDungeon.ascensionLevel >= 17) {
  AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
    this, new VulnerablePower(AbstractDungeon.player, 3, true), 3)); // 2 → 3
}
```

### 4. Entangle 대신 Shackle (구속) 사용
```java
// Line 90 수정 (더 강력한 디버프)
AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(AbstractDungeon.player,
  this, new ShackledPower(AbstractDungeon.player))); // EntanglePower → ShackledPower
// ShackledPower: 힘 감소 효과
```

---

## 전투 팁

### SlaverRed 대응
1. **Entangle 대비**: 방어 카드나 블록 카드로 다음 턴 생존 준비
2. **첫 턴 예측**: 항상 STAB이므로 방어 계획 가능
3. **취약 관리**: SCRAPE 후 공격받으면 데미지 50% 증가

### SlaverBlue 대응
1. **약화 무시**: 방어 위주 덱은 큰 영향 없음
2. **공격 타이밍**: 약화 없을 때 대미지 몰아주기
3. **예측 어려움**: 첫 턴부터 RAKE 가능성 존재

### 2마리 동시 조우
- Red + Blue 조합 시 Entangle → Weak 연계 주의
- Red 우선 처치 권장 (Entangle 봉쇄)
- Blue는 데미지가 낮아 후순위

---

## 관련 파일
- **Powers**:
  - `EntanglePower`: 다음 턴 공격 불가 (SlaverRed 전용)
  - `VulnerablePower`: 받는 데미지 50% 증가
  - `WeakPower`: 주는 데미지 25% 감소
- **Effects**: `EntangleEffect` (그물 던지기 시각 효과)
- **음성**: VO_SLAVERRED_*, VO_SLAVERBLUE_*
