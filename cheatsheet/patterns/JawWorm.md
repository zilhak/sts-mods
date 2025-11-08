# 턱벌레 (Jaw Worm)

## 기본 정보

**클래스명**: `JawWorm`
**전체 경로**: `com.megacrit.cardcrawl.monsters.exordium.JawWorm`
**ID**: `"JawWorm"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 1막 (Exordium)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 40-44 |
| A7+ | 42-46 |

**코드 위치**: 66-70줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(42, 46);
} else {
    setHp(40, 44);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public JawWorm(float x, float y, boolean hard)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표
- `hard`: 하드 모드 (시작 시 Bellow 효과 부여)

### 보조 생성자
```java
public JawWorm(float x, float y)
```
기본적으로 `hard = false`로 호출

---

## 패턴 정보

### 패턴 1: 물어뜯기 (Chomp)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: 첫 번째 턴 또는 AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 11 |
| A2+ | 12 |

**코드 위치**: 48-49줄, 75-87줄, 92줄, 115-121줄

```java
// 데미지 상수
private static final int CHOMP_DMG = 11;
private static final int A_2_CHOMP_DMG = 12;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 17) {
    this.chompDmg = 12;
} else if (AbstractDungeon.ascensionLevel >= 2) {
    this.chompDmg = 12;
} else {
    this.chompDmg = 11;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.chompDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new SetAnimationAction((AbstractCreature)this, "chomp")
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new BiteEffect(
                AbstractDungeon.player.hb.cX,
                AbstractDungeon.player.hb.cY
            ), 0.3F
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
- 물어뜯는 비주얼 이펙트 (BiteEffect)
- 첫 턴에 무조건 사용

**수정 포인트**:
- 데미지 변경: `chompDmg` 필드 또는 생성자 로직
- 이펙트 제거/변경: VFXAction 수정

---

### 패턴 2: 울부짖음 (Bellow)

**바이트 값**: `2`
**의도**: `DEFEND_BUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 자신에게 **힘(Strength)** 부여
- 자신에게 **방어도(Block)** 획득

**힘(Strength) 수치**:
| 난이도 | 힘 |
|--------|----|
| 기본 (A0-A1) | 3 |
| A2-A16 | 4 |
| A17+ | 5 |

**방어도(Block) 수치**:
| 난이도 | 방어도 |
|--------|--------|
| 기본 (A0-A16) | 6 |
| A17+ | 9 |

**코드 위치**: 52-57줄, 73-86줄, 122-132줄

```java
// 상수
private static final int BELLOW_STR = 3;
private static final int A_2_BELLOW_STR = 4;
private static final int A_17_BELLOW_STR = 5;
private static final int BELLOW_BLOCK = 6;
private static final int A_17_BELLOW_BLOCK = 9;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 17) {
    this.bellowStr = 5;
    this.bellowBlock = 9;
} else if (AbstractDungeon.ascensionLevel >= 2) {
    this.bellowStr = 4;
    this.bellowBlock = 6;
} else {
    this.bellowStr = 3;
    this.bellowBlock = 6;
}

// takeTurn 실행
case 2:
    this.state.setAnimation(0, "tailslam", false);
    this.state.addAnimation(0, "idle", true, 0.0F);
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_JAW_WORM_BELLOW")
    );
    AbstractDungeon.actionManager.addToBottom(
        new ShakeScreenAction(0.2F,
            ScreenShake.ShakeDur.SHORT,
            ScreenShake.ShakeIntensity.MED)
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, this.bellowStr),
            this.bellowStr)
    );
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction((AbstractCreature)this, (AbstractCreature)this,
            this.bellowBlock)
    );
    break;
```

**특징**:
- 화면 흔들림 효과
- 음성 효과 (MONSTER_JAW_WORM_BELLOW)

**수정 포인트**:
- 힘 수치 변경: `bellowStr` 필드
- 방어도 변경: `bellowBlock` 필드
- Ascension 25에서 방어도 12 추가 (Level25Patches.java 참조)

---

### 패턴 3: 난투 (Thrash)

**바이트 값**: `3`
**의도**: `ATTACK_DEFEND`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 **7 데미지**
- 자신에게 **5 방어도** 획득

**데미지**: 7 (모든 난이도 동일)
**방어도**: 5 (모든 난이도 동일)

**코드 위치**: 50-51줄, 76-89줄, 93줄, 133-138줄

```java
// 상수
private static final int THRASH_DMG = 7;
private static final int THRASH_BLOCK = 5;

// 생성자에서 설정 (모든 난이도 동일)
this.thrashDmg = 7;
this.thrashBlock = 5;

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.thrashDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateHopAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.BLUNT_LIGHT
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            this.thrashBlock
        )
    );
    break;
```

**특징**:
- 공격과 방어를 동시에 수행

**수정 포인트**:
- 데미지 변경: `thrashDmg` 필드
- 방어도 변경: `thrashBlock` 필드

---

## 특수 동작

### 하드 모드 (usePreBattleAction)

**조건**: `hardMode = true`로 생성된 경우
**효과**: 전투 시작 시 Bellow 효과 부여

**코드 위치**: 61-64줄, 104-110줄

```java
// 생성자에서 설정
this.hardMode = hard;
if (this.hardMode) {
    this.firstMove = false;
}

// usePreBattleAction 실행
public void usePreBattleAction() {
    if (this.hardMode) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
                new StrengthPower((AbstractCreature)this, this.bellowStr),
                this.bellowStr)
        );
        AbstractDungeon.actionManager.addToBottom(
            new GainBlockAction((AbstractCreature)this, (AbstractCreature)this,
                this.bellowBlock)
        );
    }
}
```

**특징**:
- 하드 모드에서는 첫 턴 고정 패턴이 해제됨
- 시작 시 힘과 방어도를 얻음

---

## AI 로직 (getMove)

**매우 복잡한 확률 기반 AI**

**코드 위치**: 146-188줄

### 첫 번째 턴
```java
if (this.firstMove) {
    this.firstMove = false;
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
    return;
}
```
무조건 **Chomp** 사용

### 이후 턴 (num < 25, 25% 확률)
```java
if (num < 25) {
    if (lastMove((byte)1)) {
        if (AbstractDungeon.aiRng.randomBoolean(0.5625F)) {
            setMove(MOVES[0], (byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        } else {
            setMove((byte)3, AbstractMonster.Intent.ATTACK_DEFEND,
                ((DamageInfo)this.damage.get(1)).base);
        }
    } else {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
    }
}
```

**로직**:
- 이전 패턴이 Chomp(1)이면:
  - 56.25% → Bellow(2)
  - 43.75% → Thrash(3)
- 이전 패턴이 Chomp(1)가 아니면 → Chomp(1)

### 이후 턴 (num 25~54, 30% 확률)
```java
else if (num < 55) {
    if (lastTwoMoves((byte)3)) {
        if (AbstractDungeon.aiRng.randomBoolean(0.357F)) {
            setMove((byte)1, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base);
        } else {
            setMove(MOVES[0], (byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        }
    } else {
        setMove((byte)3, AbstractMonster.Intent.ATTACK_DEFEND,
            ((DamageInfo)this.damage.get(1)).base);
    }
}
```

**로직**:
- 이전 2턴이 모두 Thrash(3)이면:
  - 35.7% → Chomp(1)
  - 64.3% → Bellow(2)
- 그 외 → Thrash(3)

### 이후 턴 (num 55~99, 45% 확률)
```java
else if (lastMove((byte)2)) {
    if (AbstractDungeon.aiRng.randomBoolean(0.416F)) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
    } else {
        setMove((byte)3, AbstractMonster.Intent.ATTACK_DEFEND,
            ((DamageInfo)this.damage.get(1)).base);
    }
} else {
    setMove(MOVES[0], (byte)2, AbstractMonster.Intent.DEFEND_BUFF);
}
```

**로직**:
- 이전 패턴이 Bellow(2)이면:
  - 41.6% → Chomp(1)
  - 58.4% → Thrash(3)
- 그 외 → Bellow(2)

**수정 포인트**:
- 패턴 순서 변경
- 확률 변경
- 첫 턴 패턴 변경

---

## 수정 예시

### 1. Bellow 방어도 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.JawWorm",
    method = "takeTurn"
)
public static class JawWormDefensePatch {
    @SpirePostfixPatch
    public static void Postfix(JawWorm __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Bellow 사용 시 (nextMove == 2) 추가 방어도
            if (__instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 12)
                );
            }
        }
    }
}
```

### 2. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.JawWorm",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class JawWormDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(JawWorm __instance, float x, float y, boolean hard) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Chomp 데미지 증가 (damage.get(0))
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 2;
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `hardMode` | boolean | 하드 모드 여부 |
| `chompDmg` | int | Chomp 데미지 |
| `thrashDmg` | int | Thrash 데미지 |
| `thrashBlock` | int | Thrash 방어도 |
| `bellowStr` | int | Bellow 힘 수치 |
| `bellowBlock` | int | Bellow 방어도 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/exordium/JawWorm.java`
- **파워**: `com.megacrit.cardcrawl.powers.StrengthPower`
- **이펙트**: `com.megacrit.cardcrawl.vfx.combat.BiteEffect`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`
  - `AnimateHopAction`
  - `SetAnimationAction`
  - `VFXAction`
  - `ShakeScreenAction`

---

## 참고사항

1. **첫 턴 고정**: 일반 모드에서는 첫 턴 무조건 Chomp
2. **하드 모드**: 시작 시 Bellow 효과, 첫 턴 고정 없음
3. **복잡한 AI**: 3가지 확률 구간과 이전 패턴 기반 결정
4. **스크린 효과**: Bellow 사용 시 화면 흔들림
5. **사망 음성**: 사망 시 "JAW_WORM_DEATH" 효과음
