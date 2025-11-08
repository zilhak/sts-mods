# 선택받은 자 (Chosen)

## 기본 정보

**클래스명**: `Chosen`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Chosen`
**ID**: `"Chosen"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 95-99 |
| A7+ | 98-103 |

**코드 위치**: 63-67줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(98, 103);
} else {
    setHp(95, 99);
}
```

---

## 패턴 정보

### 패턴 1: ZAP

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 18 |
| A2+ | 21 |

**코드 위치**: 48-49줄, 69-77줄, 79줄, 104-108줄

```java
// 데미지 상수
private static final int ZAP_DMG = 18;
private static final int A_2_ZAP_DMG = 21;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.zapDmg = 21;
} else {
    this.zapDmg = 18;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.zapDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new FastShakeAction((AbstractCreature)this, 0.3F, 0.5F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.FIRE
        )
    );
    break;
```

**특징**:
- 빠른 흔들림 효과 (FastShakeAction)
- 불 공격 이펙트 (AttackEffect.FIRE)

**수정 포인트**:
- 데미지 변경: `zapDmg` 필드 또는 생성자 로직

---

### 패턴 2: DRAIN

**바이트 값**: `2`
**의도**: `DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 **약화(Weak) 3** 부여
- 자신에게 **힘(Strength) 3** 부여

**코드 위치**: 57줄, 109-118줄

```java
// 상수
private static final int DRAIN_STR = 3;
private static final int DRAIN_WEAK = 3;

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new WeakPower((AbstractCreature)AbstractDungeon.player, 3, true),
            3
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, 3),
            3
        )
    );
    break;
```

**특징**:
- 공격 없이 디버프와 버프를 동시에 수행
- 플레이어의 공격력을 약화시키고 자신의 공격력을 강화

**수정 포인트**:
- Weak 수치 변경: `DRAIN_WEAK` 상수
- Strength 수치 변경: `DRAIN_STR` 상수

---

### 패턴 3: DEBILITATE

**바이트 값**: `3`
**의도**: `ATTACK_DEBUFF`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 10 |
| A2+ | 12 |

**효과**:
- 플레이어에게 **10 데미지** (A2+: 12)
- 플레이어에게 **취약(Vulnerable) 2** 부여

**코드 위치**: 50-51줄, 69-77줄, 80줄, 119-124줄

```java
// 데미지 상수
private static final int DEBILITATE_DMG = 10;
private static final int A_2_DEBILITATE_DMG = 12;
private static final int DEBILITATE_VULN = 2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.debilitateDmg = 12;
} else {
    this.debilitateDmg = 10;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.debilitateDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.SLASH_HEAVY
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
- 느린 공격 애니메이션 (AnimateSlowAttackAction)
- 강한 베기 이펙트 (AttackEffect.SLASH_HEAVY)
- 공격과 디버프를 동시에 수행

**수정 포인트**:
- 데미지 변경: `debilitateDmg` 필드
- Vulnerable 수치 변경: `DEBILITATE_VULN` 상수

---

### 패턴 4: HEX

**바이트 값**: `4`
**의도**: `STRONG_DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 **HexPower 1** 부여
- HexPower 효과: **공격 외 카드를 사용할 때마다 Dazed 1장을 덱에 추가**

**코드 위치**: 58줄, 130-135줄

```java
// 상수
private static final int HEX_AMT = 1;

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new TalkAction((AbstractCreature)this, DIALOG[0])
    );
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.2F));
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new HexPower((AbstractCreature)AbstractDungeon.player, 1)
        )
    );
    break;
```

**HexPower 메커니즘**:
```java
// HexPower.java - onUseCard 메서드
public void onUseCard(AbstractCard card, UseCardAction action) {
    if (card.type != AbstractCard.CardType.ATTACK) {
        flash();
        addToBot(
            new MakeTempCardInDrawPileAction(
                new Dazed(),
                this.amount,
                true,
                true
            )
        );
    }
}
```

**특징**:
- 대화 출력 (DIALOG[0])
- "ATTACK" 상태로 변경 (애니메이션)
- HexPower는 **공격 외 카드 사용 시에만** Dazed 추가
- Dazed는 덱에 추가됨 (버리는 더미가 아님)
- **전투 중 1회만 사용** (usedHex 플래그)

**수정 포인트**:
- HexPower 수치 변경: `HEX_AMT` 상수
- 발동 조건 변경 (A17+에서 첫 턴에 사용)

---

### 패턴 5: POKE

**바이트 값**: `5`
**의도**: `ATTACK` (2회 공격)
**발동 조건**: **첫 턴 고정** (A0-A16), AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 5 x 2 |
| A2+ | 6 x 2 |

**코드 위치**: 52-53줄, 69-77줄, 81줄, 97-103줄

```java
// 데미지 상수
private static final int POKE_DMG = 5;
private static final int A_2_POKE_DMG = 6;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.pokeDmg = 6;
} else {
    this.pokeDmg = 5;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.pokeDmg));

// takeTurn 실행
case 5:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(2),
            AbstractGameAction.AttackEffect.SLASH_HORIZONTAL
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(2),
            AbstractGameAction.AttackEffect.SLASH_VERTICAL
        )
    );
    break;
```

**특징**:
- 2회 연속 공격
- 가로 베기 → 세로 베기 이펙트
- **A0-A16에서 첫 턴 고정 패턴**

**수정 포인트**:
- 데미지 변경: `pokeDmg` 필드
- 타수 변경: DamageAction 추가/제거

---

## AI 로직 (getMove)

**매우 복잡한 Ascension 기반 AI**

**코드 위치**: 154-210줄

### A17+ 패턴

```java
if (AbstractDungeon.ascensionLevel >= 17) {
    // 첫 번째: HEX 무조건 사용
    if (!this.usedHex) {
        this.usedHex = true;
        setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
        return;
    }

    // 두 번째: DEBILITATE 또는 DRAIN
    if (!lastMove((byte)3) && !lastMove((byte)2)) {
        if (num < 50) {
            setMove((byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(1)).base);
            return;
        }
        setMove((byte)2, AbstractMonster.Intent.DEBUFF);
        return;
    }

    // 이후: ZAP 또는 POKE
    if (num < 40) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
        return;
    }
    setMove((byte)5, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(2)).base, 2, true);
}
```

**A17+ 패턴 순서**:
1. 첫 턴: **HEX** (무조건)
2. 둘째 턴:
   - 50% → DEBILITATE (10/12 + Vuln 2)
   - 50% → DRAIN (Weak 3 + Str 3)
3. 이후 턴:
   - 40% → ZAP (18/21)
   - 60% → POKE (5x2 / 6x2)

---

### A0-A16 패턴

```java
// 첫 턴: POKE 무조건
if (this.firstTurn) {
    this.firstTurn = false;
    setMove((byte)5, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(2)).base, 2, true);
    return;
}

// 두 번째: HEX 무조건
if (!this.usedHex) {
    this.usedHex = true;
    setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}

// 셋째 턴: DEBILITATE 또는 DRAIN
if (!lastMove((byte)3) && !lastMove((byte)2)) {
    if (num < 50) {
        setMove((byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
            ((DamageInfo)this.damage.get(1)).base);
        return;
    }
    setMove((byte)2, AbstractMonster.Intent.DEBUFF);
    return;
}

// 이후: ZAP 또는 POKE
if (num < 40) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
    return;
}
setMove((byte)5, AbstractMonster.Intent.ATTACK,
    ((DamageInfo)this.damage.get(2)).base, 2, true);
```

**A0-A16 패턴 순서**:
1. 첫 턴: **POKE** (5x2 / 6x2) - **무조건**
2. 둘째 턴: **HEX** - **무조건**
3. 셋째 턴:
   - 50% → DEBILITATE (10/12 + Vuln 2)
   - 50% → DRAIN (Weak 3 + Str 3)
4. 이후 턴:
   - 40% → ZAP (18/21)
   - 60% → POKE (5x2 / 6x2)

---

## 전투 전략

### 플레이어 대응

**A0-A16 전략**:
1. **첫 턴**: POKE (10/12) 대비 - 방어 10-12
2. **둘째 턴**: HEX - 공격 카드 위주로 플레이 준비
3. **셋째 턴**: DEBILITATE (10/12 + Vuln 2) 또는 DRAIN (Weak 3)
4. **이후**: ZAP (18/21) 대비 충분한 방어

**A17+ 전략**:
1. **첫 턴**: HEX - 즉시 공격 카드 위주 플레이 필요
2. **둘째 턴**: DEBILITATE/DRAIN - 디버프 대비
3. **이후**: 높은 데미지 패턴 (ZAP 21 또는 POKE 12) 대비

**위험 요소**:
- HexPower: 스킬/파워 사용 시마다 덱에 Dazed 추가
- DRAIN: Strength 3 누적 → 장기전 불리
- DEBILITATE: Vulnerable 2 → 다음 공격 받는 데미지 50% 증가

**카운터 전략**:
- **공격 카드 위주**: HexPower 발동 방지
- **Artifact**: HexPower, Vulnerable, Weak 무효화
- **빠른 처치**: Strength 누적 전 처치
- **Disarm**: Strength 제거

---

## 특수 동작

### 애니메이션 상태

**코드 위치**: 142-149줄

```java
public void changeState(String key) {
    switch (key) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**특징**:
- HEX 사용 시 "ATTACK" 애니메이션 재생
- 애니메이션 후 "Idle" 상태로 복귀

### 피격 애니메이션

**코드 위치**: 218-225줄

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        this.state.setTimeScale(0.8F);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**특징**:
- 가시 데미지는 피격 애니메이션 없음
- 0.8배속 애니메이션

---

## 수정 예시

### 1. A25+에서 첫 턴 ZAP으로 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Chosen",
    method = "getMove"
)
public static class ChosenFirstMovePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Chosen __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            Field firstTurnField = Chosen.class.getDeclaredField("firstTurn");
            firstTurnField.setAccessible(true);
            boolean firstTurn = (boolean)firstTurnField.get(__instance);

            if (firstTurn) {
                firstTurnField.set(__instance, false);
                // ZAP으로 변경
                __instance.setMove((byte)1,
                    AbstractMonster.Intent.ATTACK, 21);
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }
}
```

### 2. DRAIN Strength 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Chosen",
    method = "takeTurn"
)
public static class ChosenDrainPatch {
    @SpirePostfixPatch
    public static void Postfix(Chosen __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // nextMove == 2 (DRAIN)
            if (__instance.nextMove == 2) {
                // Strength 3 → 5로 증가
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        __instance,
                        __instance,
                        new StrengthPower(__instance, 2),
                        2
                    )
                );
            }
        }
    }
}
```

### 3. HexPower 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Chosen",
    method = "takeTurn"
)
public static class ChosenHexPatch {
    @SpirePostfixPatch
    public static void Postfix(Chosen __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // nextMove == 4 (HEX)
            if (__instance.nextMove == 4) {
                // HexPower 1 → 2로 증가
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(
                        AbstractDungeon.player,
                        __instance,
                        new HexPower(AbstractDungeon.player, 1)
                    )
                );
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstTurn` | boolean | 첫 턴 여부 (A0-A16 전용) |
| `usedHex` | boolean | HEX 사용 여부 플래그 |
| `zapDmg` | int | ZAP 데미지 (18/21) |
| `debilitateDmg` | int | DEBILITATE 데미지 (10/12) |
| `pokeDmg` | int | POKE 데미지 (5/6) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Chosen.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.HexPower`
  - `com.megacrit.cardcrawl.powers.StrengthPower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.Dazed` (HexPower 효과)
- **액션**:
  - `DamageAction`
  - `ApplyPowerAction`
  - `TalkAction`
  - `ChangeStateAction`
  - `AnimateSlowAttackAction`
  - `FastShakeAction`

---

## 참고사항

1. **첫 턴 고정**: A0-A16에서는 POKE, A17+에서는 HEX
2. **HEX 1회 제한**: 전투 중 1회만 사용 (usedHex 플래그)
3. **HexPower 메커니즘**: 공격 외 카드 사용 시 Dazed 추가 (덱에)
4. **DRAIN 위험**: Strength 누적으로 장기전 불리
5. **A17+ 난이도 증가**: 첫 턴부터 HEX로 즉각 압박
