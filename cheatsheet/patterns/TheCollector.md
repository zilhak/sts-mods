# 수집가 (The Collector)

## 기본 정보

**클래스명**: `TheCollector`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.TheCollector`
**ID**: `"TheCollector"`
**타입**: 보스 (BOSS)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 282 |
| A9+ | 300 |

**코드 위치**: 74-80줄

```java
if (AbstractDungeon.ascensionLevel >= 9) {
    setHp(300);
    this.blockAmt = 18;
} else {
    setHp(282);
    this.blockAmt = 15;
}
```

---

## 하수인 시스템

### 시작 하수인 소환 (첫 턴)

**코드 위치**: 119-127줄

```java
case 1: // 첫 턴 SPAWN
    for (i = 1; i < 3; i++) {  // 2마리 소환 (i=1,2)
        AbstractMonster m = new TorchHead(
            this.spawnX + -185.0F * i,
            MathUtils.random(-5.0F, 25.0F)
        );
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("MONSTER_COLLECTOR_SUMMON")
        );
        AbstractDungeon.actionManager.addToBottom(new SpawnMonsterAction(m, true));
        this.enemySlots.put(Integer.valueOf(i), m);
    }
    this.initialSpawn = false;
    break;
```

**특징**:
- 첫 턴에 TorchHead 2마리 소환
- `enemySlots` HashMap에 슬롯 위치 저장
- 슬롯 1, 2 사용

---

### 부활 메커니즘 (25% 확률)

**코드 위치**: 176-184, 206-210, 224-230줄

```java
// getMove에서 부활 체크
if (num <= 25 && isMinionDead() && !lastMove((byte)5)) {
    setMove((byte)5, AbstractMonster.Intent.UNKNOWN);  // REVIVE
    return;
}

// isMinionDead 체크
private boolean isMinionDead() {
    for (Map.Entry<Integer, AbstractMonster> m : this.enemySlots.entrySet()) {
        if (((AbstractMonster)m.getValue()).isDying) {
            return true;
        }
    }
    return false;
}

// takeTurn에서 부활 실행
case 5: // REVIVE
    for (Map.Entry<Integer, AbstractMonster> m : this.enemySlots.entrySet()) {
        if (((AbstractMonster)m.getValue()).isDying) {
            AbstractMonster newMonster = new TorchHead(
                this.spawnX + -185.0F * ((Integer)m.getKey()).intValue(),
                MathUtils.random(-5.0F, 25.0F)
            );
            int key = ((Integer)m.getKey()).intValue();
            this.enemySlots.put(Integer.valueOf(key), newMonster);
            AbstractDungeon.actionManager.addToBottom(
                new SpawnMonsterAction(newMonster, true)
            );
        }
    }
    break;
```

**특징**:
- 하수인이 죽으면 **25% 확률로 부활**
- 같은 슬롯 위치에 새 하수인 소환
- 이전 턴이 부활이 아닐 때만 발동

---

## 패턴 정보

### 패턴 1: Spawn (초기 소환)

**바이트 값**: `1`
**의도**: `UNKNOWN`
**발동 조건**: 첫 턴만 (initialSpawn = true)

**효과**:
- TorchHead 2마리 소환

**코드 위치**: 119-127, 195-199줄

```java
// getMove
if (this.initialSpawn) {
    setMove((byte)1, AbstractMonster.Intent.UNKNOWN);
    return;
}
```

---

### 패턴 2: Fireball (화염구)

**바이트 값**: `2`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A3) | 18 |
| A4+ | 21 |

**코드 위치**: 52, 55, 83, 87, 91, 96, 129-131, 212줄

```java
// 상수 및 생성자 설정
private static final int FIREBALL_DMG = 18;
private static final int A_2_FIREBALL_DMG = 21;

if (AbstractDungeon.ascensionLevel >= 4) {
    this.rakeDmg = 21;
} else {
    this.rakeDmg = 18;
}

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.FIRE
        )
    );
    break;
```

---

### 패턴 3: Buff (강화)

**바이트 값**: `3`
**의도**: `DEFEND_BUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- **방어도 획득**
- **모든 아군에게 Strength 부여**

**방어도**:
| 난이도 | 방어도 |
|--------|--------|
| 기본 (A0-A8) | 15 |
| A9-A18 | 18 |
| A19+ | 23 |

**Strength**:
| 난이도 | Strength |
|--------|----------|
| 기본 (A0-A3) | 3 |
| A4-A18 | 4 |
| A19+ | 5 |

**코드 위치**: 53, 56, 76, 79, 84, 88, 92, 133-143줄

```java
// 상수 및 생성자 설정
private static final int STR_AMT = 3;
private static final int BLOCK_AMT = 15;
private static final int A_2_STR_AMT = 4;
private static final int A_2_BLOCK_AMT = 18;

if (AbstractDungeon.ascensionLevel >= 19) {
    this.strAmt = 5;
} else if (AbstractDungeon.ascensionLevel >= 4) {
    this.strAmt = 4;
} else {
    this.strAmt = 3;
}

// takeTurn 실행
case 3:
    if (AbstractDungeon.ascensionLevel >= 19) {
        // A19+: 방어도 +5 추가
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction((AbstractCreature)this, (AbstractCreature)this, this.blockAmt + 5)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction((AbstractCreature)this, (AbstractCreature)this, this.blockAmt)
        );
    }
    // 모든 살아있는 몬스터에게 Strength 부여
    for (AbstractMonster m : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
        if (!m.isDead && !m.isDying && !m.isEscaping) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    (AbstractCreature)m,
                    (AbstractCreature)this,
                    new StrengthPower((AbstractCreature)m, this.strAmt),
                    this.strAmt
                )
            );
        }
    }
    break;
```

**특징**:
- 본체와 모든 하수인에게 Strength 부여
- A19+: 방어도 15+5 = 20 또는 18+5 = 23

---

### 패턴 4: Mega Debuff (대규모 디버프)

**바이트 값**: `4`
**의도**: `STRONG_DEBUFF`
**발동 조건**: 3턴 후 1회만 (`turnsTaken >= 3 && !ultUsed`)

**효과**:
- **Weak** 부여
- **Vulnerable** 부여
- **Frail** 부여

**디버프 지속**:
| 난이도 | 지속 시간 |
|--------|----------|
| 기본 (A0-A18) | 3턴 |
| A19+ | 5턴 |

**코드 위치**: 56, 85, 89, 93, 146-172, 200-204줄

```java
// 상수 및 생성자 설정
private static final int MEGA_DEBUFF_AMT = 3;

if (AbstractDungeon.ascensionLevel >= 19) {
    this.megaDebuffAmt = 5;
} else {
    this.megaDebuffAmt = 3;
}

// getMove에서 체크
if (this.turnsTaken >= 3 && !this.ultUsed) {
    setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new TalkAction((AbstractCreature)this, DIALOG[0])
    );
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_COLLECTOR_DEBUFF")
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new CollectorCurseEffect(
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY
            ), 2.0F
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new WeakPower((AbstractCreature)AbstractDungeon.player, this.megaDebuffAmt, true),
            this.megaDebuffAmt
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new VulnerablePower((AbstractCreature)AbstractDungeon.player, this.megaDebuffAmt, true),
            this.megaDebuffAmt
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new FrailPower((AbstractCreature)AbstractDungeon.player, this.megaDebuffAmt, true),
            this.megaDebuffAmt
        )
    );
    this.ultUsed = true;
    break;
```

**특징**:
- 3턴 후 1회만 발동
- Weak + Vulnerable + Frail 동시 부여
- A19+: 5턴 지속 (매우 위험)

---

### 패턴 5: Revive (부활)

**바이트 값**: `5`
**의도**: `UNKNOWN`
**발동 조건**: 하수인 사망 + 25% 확률

**효과**:
- 죽은 하수인을 같은 슬롯에 부활

**코드 위치**: 176-184, 206-210, 224-230줄

---

## AI 로직 (getMove)

**코드 위치**: 194-221줄

### 첫 턴 (초기 소환)
```java
if (this.initialSpawn) {
    setMove((byte)1, AbstractMonster.Intent.UNKNOWN);  // SPAWN
    return;
}
```

### Mega Debuff 우선순위 (3턴 후)
```java
if (this.turnsTaken >= 3 && !this.ultUsed) {
    setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}
```
- 3턴 후 1회만 발동
- 최우선 패턴

### 부활 (25% 확률)
```java
if (num <= 25 && isMinionDead() && !lastMove((byte)5)) {
    setMove((byte)5, AbstractMonster.Intent.UNKNOWN);  // REVIVE
    return;
}
```
- 하수인이 죽었을 때
- 25% 확률
- 이전 턴이 부활이 아닐 때

### Fireball (70% 확률)
```java
if (num <= 70 && !lastTwoMoves((byte)2)) {
    setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);
    return;
}
```
- 70% 확률
- 이전 2턴이 모두 Fireball이 아닐 때

### 기본 패턴 (폴백)
```java
if (!lastMove((byte)3)) {
    setMove((byte)3, AbstractMonster.Intent.DEFEND_BUFF);  // BUFF
} else {
    setMove((byte)2, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get(0)).base);  // FIREBALL
}
```
- 이전 턴이 Buff가 아니면 Buff
- 그렇지 않으면 Fireball

**AI 우선순위**:
1. 첫 턴: Spawn (2마리 소환)
2. 3턴 후: Mega Debuff (1회만)
3. 하수인 사망 + 25%: Revive
4. 70% + 이전 2턴 Fireball 아님: Fireball
5. 이전 턴 Buff 아님: Buff
6. 그 외: Fireball

---

## 수정 예시

### 1. Mega Debuff 지속 시간 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TheCollector",
    method = SpirePatch.CONSTRUCTOR
)
public static class CollectorDebuffPatch {
    @SpirePostfixPatch
    public static void Postfix(TheCollector __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // A19: 5턴 → 7턴
            // 기본: 3턴 → 5턴
        }
    }
}
```

### 2. Buff Strength 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TheCollector",
    method = "takeTurn"
)
public static class CollectorBuffPatch {
    @SpirePostfixPatch
    public static void Postfix(TheCollector __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && __instance.nextMove == 3) {
            // 모든 몬스터에게 추가 Strength +2
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!m.isDead && !m.isDying) {
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(m, __instance, new StrengthPower(m, 2), 2)
                    );
                }
            }
        }
    }
}
```

### 3. 부활 확률 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TheCollector",
    method = "getMove"
)
public static class CollectorRevivePatch {
    @SpirePrefixPatch
    public static void Prefix(TheCollector __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 25% → 40% (num <= 40)
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `turnsTaken` | int | 경과 턴 수 (Mega Debuff 체크) |
| `ultUsed` | boolean | Mega Debuff 사용 여부 |
| `initialSpawn` | boolean | 첫 턴 여부 |
| `enemySlots` | HashMap<Integer, AbstractMonster> | 하수인 슬롯 관리 |
| `rakeDmg` | int | Fireball 데미지 |
| `strAmt` | int | Buff Strength |
| `blockAmt` | int | Buff 방어도 |
| `megaDebuffAmt` | int | Mega Debuff 지속 시간 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/TheCollector.java`
- **하수인**: `com/megacrit/cardcrawl/monsters/city/TorchHead.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.StrengthPower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
  - `com.megacrit.cardcrawl.powers.FrailPower`
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.CollectorCurseEffect`
  - `com.megacrit.cardcrawl.vfx.GlowyFireEyesEffect`
  - `com.megacrit.cardcrawl.vfx.StaffFireEffect`
  - `com.megacrit.cardcrawl.vfx.combat.InflameEffect`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`
  - `SpawnMonsterAction`
  - `SuicideAction`
  - `VFXAction`
  - `TalkAction`
  - `SFXAction`
  - `HideHealthBarAction`

---

## 참고사항

1. **첫 턴 소환**: TorchHead 2마리 소환 (고정)
2. **Mega Debuff**: 3턴 후 1회만 발동, A19+는 5턴 지속
3. **부활 메커니즘**: 하수인 사망 시 25% 확률로 부활
4. **Buff**: 본체 + 모든 하수인 Strength 부여, A19+는 방어도 +5
5. **AI 우선순위**: Mega Debuff (3턴) > Revive (25%) > Fireball (70%) > Buff > Fireball
6. **슬롯 시스템**: HashMap으로 하수인 위치 관리, 같은 슬롯에 부활
7. **A19+ 강화**: Strength 5, 방어도 23, Mega Debuff 5턴
8. **Fireball 제한**: 이전 2턴 연속 사용 불가
