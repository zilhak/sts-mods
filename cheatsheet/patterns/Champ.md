# 챔피언 (Champ)

## 기본 정보

**클래스명**: `Champ`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Champ`
**ID**: `"Champ"`
**타입**: 보스 (BOSS)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 420 |
| A9+ | 440 |

**코드 위치**: 85-89줄

```java
if (AbstractDungeon.ascensionLevel >= 9) {
    setHp(440);
} else {
    setHp(420);
}
```

---

## 페이즈 시스템

### 페이즈 1 (HP > 50%)
- 일반 공격 패턴

### 페이즈 2 (HP < 50%)
- **전환 트리거**: `currentHealth < maxHealth / 2` (미만, 50% 미만)
- **전환 효과**: Anger 패턴 강제 발동
- **Execute 사용 시작**

**코드 위치**: 296-301줄

```java
if (this.currentHealth < this.maxHealth / 2 && !this.thresholdReached) {
    this.thresholdReached = true;
    setMove((byte)7, AbstractMonster.Intent.BUFF);
    return;
}
```

**주의**: 정확히 50%가 아닌 **미만**이므로 210 HP 초과 시 아직 페이즈 1

---

## 패턴 정보

### 패턴 1: Heavy Slash (일반 베기)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름 (페이즈 1만)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A3) | 16 |
| A4+ | 18 |

**코드 위치**: 54, 59, 113, 116, 121, 163-174줄

```java
// 상수 및 생성자 설정
private static final int SLASH_DMG = 16;
private static final int A_2_SLASH_DMG = 18;

if (AbstractDungeon.ascensionLevel >= 4) {
    this.slashDmg = 18;
} else {
    this.slashDmg = 16;
}

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4F));
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new GoldenSlashEffect(
                AbstractDungeon.player.hb.cX - 60.0F * Settings.scale,
                AbstractDungeon.player.hb.cY,
                false
            ), vfxSpeed
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.NONE
        )
    );
    break;
```

**특징**:
- 황금색 베기 이펙트
- 단일 타격

---

### 패턴 2: Defensive Stance (방어 태세)

**바이트 값**: `2`
**의도**: `DEFEND_BUFF`
**발동 조건**: AI 로직에 따름 (최대 2회)

**효과**:
- **방어도 획득**
- **Metallicize 부여** (일반 Metallicize - 감소 메커니즘 없음)

**방어도**:
| 난이도 | 방어도 |
|--------|--------|
| 기본 (A0-A8) | 15 |
| A9-A18 | 18 |
| A19+ | 20 |

**Metallicize**:
| 난이도 | Metallicize |
|--------|-------------|
| 기본 (A0-A8) | 5 |
| A9-A18 | 6 |
| A19+ | 7 |

**코드 위치**: 67, 97, 104, 111, 118, 176-178줄

```java
// 상수 및 생성자 설정
private static final int FORGE_AMT = 5;
private static final int BLOCK_AMT = 15;
private static final int A_9_FORGE_AMT = 6;
private static final int A_9_BLOCK_AMT = 18;
private static final int A_19_FORGE_AMT = 7;
private static final int A_19_BLOCK_AMT = 20;

if (AbstractDungeon.ascensionLevel >= 19) {
    this.forgeAmt = 7;
    this.blockAmt = 20;
} else if (AbstractDungeon.ascensionLevel >= 9) {
    this.forgeAmt = 6;
    this.blockAmt = 18;
} else {
    this.forgeAmt = 5;
    this.blockAmt = 15;
}

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction((AbstractCreature)this, (AbstractCreature)this, this.blockAmt)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            new MetallicizePower((AbstractCreature)this, this.forgeAmt),
            this.forgeAmt
        )
    );
    break;
```

**특징**:
- 방어도와 Metallicize 동시 부여
- 최대 2회 사용 (forgeThreshold = 2)
- **일반 Metallicize**: 피격 시 감소하지 않음

---

### 패턴 3: Execute (처형)

**바이트 값**: `3`
**의도**: `ATTACK` (2회 공격)
**발동 조건**: 페이즈 2 전환 후 (HP < 50%)

**데미지**:
- **10 데미지 x 2회** (모든 난이도 동일)

**코드 위치**: 56, 114, 122, 181-204줄

```java
// 상수 및 생성자 설정
private static final int EXECUTE_DMG = 10;
private static final int EXEC_COUNT = 2;

this.executeDmg = 10; // 모든 난이도 동일

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(new AnimateJumpAction((AbstractCreature)this));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));

    // 첫 번째 타격
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new GoldenSlashEffect(
                AbstractDungeon.player.hb.cX - 60.0F * Settings.scale,
                AbstractDungeon.player.hb.cY,
                true
            ), vfxSpeed
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.NONE
        )
    );

    // 두 번째 타격
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new GoldenSlashEffect(
                AbstractDungeon.player.hb.cX + 60.0F * Settings.scale,
                AbstractDungeon.player.hb.cY,
                true
            ), vfxSpeed
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.NONE
        )
    );
    break;
```

**특징**:
- 2회 연속 공격 (10 x 2 = 20 총 데미지)
- 점프 애니메이션
- 페이즈 2에서만 사용
- Execute를 2회 연속으로 사용하지 않음

---

### 패턴 4: Face Slap (뺨 때리기)

**바이트 값**: `4`
**의도**: `ATTACK_DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- **데미지**
- **Frail 2** 부여
- **Vulnerable 2** 부여

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A3) | 12 |
| A4+ | 14 |

**디버프**: Frail 2 + Vulnerable 2 (모든 난이도 동일)

**코드 위치**: 58, 60-61, 65, 115, 123, 206-217줄

```java
// 상수 및 생성자 설정
private static final int SLAP_DMG = 12;
private static final int A_2_SLAP_DMG = 14;
private static final int DEBUFF_AMT = 2;

if (AbstractDungeon.ascensionLevel >= 4) {
    this.slapDmg = 14;
} else {
    this.slapDmg = 12;
}

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_CHAMP_SLAP")
    );
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(2),
            AbstractGameAction.AttackEffect.BLUNT_LIGHT
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new FrailPower((AbstractCreature)AbstractDungeon.player, 2, true),
            2
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new VulnerablePower((AbstractCreature)AbstractDungeon.player, 2, true),
            2
        )
    );
    break;
```

**특징**:
- 공격 + 2가지 디버프
- 음성 효과 (MONSTER_CHAMP_SLAP)
- Frail과 Vulnerable 동시 부여

---

### 패턴 5: Gloat (자랑)

**바이트 값**: `5`
**의도**: `BUFF`
**발동 조건**: AI 로직에 따름 (페이즈 1만)

**효과**:
- 자신에게 **Strength 부여**

**Strength**:
| 난이도 | Strength |
|--------|----------|
| 기본 (A0-A3) | 2 |
| A4-A18 | 3 |
| A19+ | 4 |

**코드 위치**: 67, 95, 102, 109, 116, 224-225줄

```java
// 상수 및 생성자 설정
private static final int STR_AMT = 2;
private static final int A_4_STR_AMT = 3;
private static final int A_19_STR_AMT = 4;

if (AbstractDungeon.ascensionLevel >= 19) {
    this.strAmt = 4;
} else if (AbstractDungeon.ascensionLevel >= 4) {
    this.strAmt = 3;
} else {
    this.strAmt = 2;
}

// takeTurn 실행
case 5:
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, this.strAmt),
            this.strAmt
        )
    );
    break;
```

---

### 패턴 6: Taunt (도발)

**바이트 값**: `6`
**의도**: `DEBUFF`
**발동 조건**: 4턴째 (페이즈 1만)

**효과**:
- **Weak 2** 부여
- **Vulnerable 2** 부여

**디버프**: Weak 2 + Vulnerable 2 (모든 난이도 동일)

**코드 위치**: 65, 228-236줄

```java
// 상수
private static final int DEBUFF_AMT = 2;

// takeTurn 실행
case 6:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("VO_CHAMP_2A")
    );
    AbstractDungeon.actionManager.addToBottom(
        new TalkAction((AbstractCreature)this, getTaunt())
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new WeakPower((AbstractCreature)AbstractDungeon.player, 2, true),
            2
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new VulnerablePower((AbstractCreature)AbstractDungeon.player, 2, true),
            2
        )
    );
    break;
```

**특징**:
- 4턴째 고정 발동
- 음성 효과 + 대사 출력
- Weak와 Vulnerable 동시 부여

---

### 패턴 7: Anger (분노)

**바이트 값**: `7`
**의도**: `BUFF`
**발동 조건**: 페이즈 2 전환 시 자동 발동 (1회만)

**효과**:
- **모든 디버프 제거** (RemoveDebuffsAction)
- **Shackled 파워 제거**
- **Strength 대량 부여** (strAmt x 3)

**Strength**:
| 난이도 | Strength (x3) |
|--------|---------------|
| 기본 (A0-A3) | 2 x 3 = 6 |
| A4-A18 | 3 x 3 = 9 |
| A19+ | 4 x 3 = 12 |

**코드 위치**: 151-160줄

```java
case 7:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_CHAMP_CHARGE")
    );
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction((AbstractCreature)this, getLimitBreak(), 2.0F, 3.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction((AbstractCreature)this, new InflameEffect((AbstractCreature)this), 0.25F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction((AbstractCreature)this, new InflameEffect((AbstractCreature)this), 0.25F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction((AbstractCreature)this, new InflameEffect((AbstractCreature)this), 0.25F)
    );
    AbstractDungeon.actionManager.addToBottom(new RemoveDebuffsAction((AbstractCreature)this));
    AbstractDungeon.actionManager.addToBottom(
        new RemoveSpecificPowerAction((AbstractCreature)this, (AbstractCreature)this, "Shackled")
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, this.strAmt * 3),
            this.strAmt * 3
        )
    );
    break;
```

**특징**:
- 페이즈 2 전환 시 자동 발동
- 모든 디버프 클렌징
- 대량의 Strength 획득
- 3회의 화염 이펙트

---

## AI 로직 (getMove)

**코드 위치**: 293-347줄

### 페이즈 전환 체크 (최우선)
```java
if (this.currentHealth < this.maxHealth / 2 && !this.thresholdReached) {
    this.thresholdReached = true;
    setMove((byte)7, AbstractMonster.Intent.BUFF); // Anger
    return;
}
```
**HP < 50%** 시 Anger 강제 발동

### Execute 우선순위 (페이즈 2)
```java
if (!lastMove((byte)3) && !lastMoveBefore((byte)3) && this.thresholdReached) {
    AbstractDungeon.actionManager.addToTop(
        new TalkAction((AbstractCreature)this, getDeathQuote(), 2.0F, 2.0F)
    );
    setMove(EXECUTE_NAME, (byte)3, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(1)).base, 2, true);
    return;
}
```
- 페이즈 2에서 Execute를 2회 연속으로 사용하지 않음
- Execute 이외 패턴 사용 후 다시 Execute 가능

### Taunt 고정 (4턴째, 페이즈 1)
```java
if (this.numTurns == 4 && !this.thresholdReached) {
    setMove((byte)6, AbstractMonster.Intent.DEBUFF); // Taunt
    this.numTurns = 0;
    return;
}
```
4턴째 Taunt 강제 발동 후 턴 카운터 리셋

### Defensive Stance 조건 (최대 2회)
```java
if (AbstractDungeon.ascensionLevel >= 19) {
    // A19+: 30% 확률
    if (!lastMove((byte)2) && this.forgeTimes < this.forgeThreshold && num <= 30) {
        this.forgeTimes++;
        setMove(STANCE_NAME, (byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        return;
    }
} else {
    // A0-A18: 15% 확률
    if (!lastMove((byte)2) && this.forgeTimes < this.forgeThreshold && num <= 15) {
        this.forgeTimes++;
        setMove(STANCE_NAME, (byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        return;
    }
}
```
- A19+: 30% 확률
- A0-A18: 15% 확률
- 최대 2회 사용
- 이전 턴이 Defensive Stance가 아닐 때만

### Gloat (30% 확률)
```java
if (!lastMove((byte)5) && !lastMove((byte)2) && num <= 30) {
    setMove((byte)5, AbstractMonster.Intent.BUFF); // Gloat
    return;
}
```
- 30% 확률
- 이전 턴이 Gloat 또는 Defensive Stance가 아닐 때

### Face Slap (55% 확률)
```java
if (!lastMove((byte)4) && num <= 55) {
    setMove(SLAP_NAME, (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(2)).base);
    return;
}
```
- 55% 확률
- 이전 턴이 Face Slap이 아닐 때

### 기본 패턴 (폴백)
```java
if (!lastMove((byte)1)) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base); // Heavy Slash
} else {
    setMove(SLAP_NAME, (byte)4, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(2)).base); // Face Slap
}
```
- 이전 턴이 Heavy Slash가 아니면 Heavy Slash
- 그렇지 않으면 Face Slap

---

## 특수 동작

### Champion Belt 소지 대사
**코드 위치**: 142-147줄

```java
if (this.firstTurn) {
    this.firstTurn = false;
    if (AbstractDungeon.player.hasRelic("Champion Belt")) {
        AbstractDungeon.actionManager.addToBottom(
            new TalkAction((AbstractCreature)this, DIALOG[8], 0.5F, 2.0F)
        );
    }
}
```

플레이어가 Champion Belt 유물 소지 시 특별 대사

---

## 수정 예시

### 1. Defensive Stance 방어도 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Champ",
    method = "takeTurn"
)
public static class ChampDefensePatch {
    @SpirePostfixPatch
    public static void Postfix(Champ __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && __instance.nextMove == 2) {
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(__instance, __instance, 5)
            );
        }
    }
}
```

### 2. Execute 데미지 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Champ",
    method = SpirePatch.CONSTRUCTOR
)
public static class ChampExecutePatch {
    @SpirePostfixPatch
    public static void Postfix(Champ __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // damage.get(1) = Execute 데미지
            if (__instance.damage.size() > 1) {
                __instance.damage.get(1).base += 2; // 10 → 12
            }
        }
    }
}
```

### 3. Anger Strength 증가 (A25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Champ",
    method = "takeTurn"
)
public static class ChampAngerPatch {
    @SpirePrefixPatch
    public static void Prefix(Champ __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && __instance.nextMove == 7) {
            // Anger 사용 시 추가 Strength 부여
            // strAmt * 3 + 3 = strAmt * 3 + 추가 3
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `numTurns` | int | 턴 카운터 (Taunt 4턴 체크) |
| `slashDmg` | int | Heavy Slash 데미지 |
| `executeDmg` | int | Execute 데미지 |
| `slapDmg` | int | Face Slap 데미지 |
| `blockAmt` | int | Defensive Stance 방어도 |
| `strAmt` | int | Gloat/Anger Strength |
| `forgeAmt` | int | Defensive Stance Metallicize |
| `forgeTimes` | int | Defensive Stance 사용 횟수 |
| `forgeThreshold` | int | Defensive Stance 최대 횟수 (2) |
| `thresholdReached` | boolean | 페이즈 2 전환 여부 |
| `firstTurn` | boolean | 첫 턴 여부 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Champ.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.StrengthPower`
  - `com.megacrit.cardcrawl.powers.MetallicizePower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
  - `com.megacrit.cardcrawl.powers.FrailPower`
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.combat.GoldenSlashEffect`
  - `com.megacrit.cardcrawl.vfx.combat.InflameEffect`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`
  - `RemoveDebuffsAction`
  - `RemoveSpecificPowerAction`
  - `ChangeStateAction`
  - `AnimateJumpAction`
  - `AnimateFastAttackAction`
  - `VFXAction`
  - `ShoutAction`
  - `TalkAction`
  - `SFXAction`

---

## 참고사항

1. **페이즈 전환**: HP < 50% (미만) 시 Anger 발동
2. **Metallicize**: 일반 Metallicize - 피격 시 감소하지 않음
3. **Defensive Stance**: 최대 2회만 사용 가능
4. **4턴 고정 패턴**: 페이즈 1에서 4턴째 Taunt 강제 발동
5. **Execute**: 페이즈 2에서만 사용, 2회 연속 사용 불가
6. **Anger**: 페이즈 2 전환 시 1회만 발동, 모든 디버프 제거
7. **Champion Belt**: 소지 시 첫 턴 특별 대사
8. **AI 우선순위**: 페이즈 전환 > Execute > Taunt > Defensive Stance > Gloat > Face Slap > Heavy Slash
