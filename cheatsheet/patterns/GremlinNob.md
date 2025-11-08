# 그렘린 두목 (Gremlin Nob)

## 기본 정보

**클래스명**: `GremlinNob`
**전체 경로**: `com.megacrit.cardcrawl.monsters.exordium.GremlinNob`
**ID**: `"GremlinNob"`
**타입**: 엘리트 (ELITE)
**등장 지역**: 1막 (Exordium)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A7) | 82-86 |
| A8+ | 85-90 |

**코드 위치**: 28-34줄, 54-58줄

```java
private static final int HP_MIN = 82;
private static final int HP_MAX = 86;
private static final int A_2_HP_MIN = 85;
private static final int A_2_HP_MAX = 90;

if (AbstractDungeon.ascensionLevel >= 8) {
    setHp(85, 90);
} else {
    setHp(82, 86);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public GremlinNob(float x, float y, boolean setVuln)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표
- `setVuln`: 취약(Vulnerable) 디버프 부여 여부

### 보조 생성자
```java
public GremlinNob(float x, float y)
```
기본적으로 `setVuln = true`로 호출

**중요**: `setVuln`은 Skull Bash 공격 시 취약 디버프를 부여할지 결정합니다.

---

## 패턴 정보

### 패턴 1: 울부짖기 (Bellow)

**바이트 값**: `3`
**의도**: `BUFF`
**발동 조건**: 첫 번째 턴에 무조건 사용
**효과**: 자신에게 **분노(Anger)** 파워 부여

**분노(Anger) 수치**:
| 난이도 | 분노 수치 |
|--------|----------|
| 기본 (A0-A17) | 2 |
| A18+ | 3 |

**코드 위치**: 42-45줄, 89-97줄, 139-143줄

```java
// 상수
private static final byte BELLOW = 3;
private static final int ANGRY_LEVEL = 2;

// takeTurn 실행
case 3:
    playSfx();
    AbstractDungeon.actionManager.addToBottom(
        new TalkAction((AbstractCreature)this, DIALOG[0], 1.0F, 3.0F)
    );
    if (AbstractDungeon.ascensionLevel >= 18) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
                new AngerPower((AbstractCreature)this, 3), 3)
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
                new AngerPower((AbstractCreature)this, 2), 2)
        );
    }
    break;

// getMove: 첫 번째 턴 무조건 사용
if (!this.usedBellow) {
    this.usedBellow = true;
    setMove((byte)3, AbstractMonster.Intent.BUFF);
    return;
}
```

**특징**:
- 첫 턴 고정 패턴
- 음성 효과 랜덤 재생 (VO_GREMLINNOB_1A/1B/1C)
- 대사 출력 ("분노" 텍스트)
- **분노 파워**: 스킬 카드 사용 시마다 힘(Strength) 획득

**수정 포인트**:
- 분노 수치 변경: A18 조건문 또는 ANGRY_LEVEL 수정
- 첫 턴 패턴 변경: `getMove()` 139-143줄 수정
- 음성 제거/변경: `playSfx()` 메서드 수정

---

### 패턴 2: 두개골 강타 (Skull Bash)

**바이트 값**: `2`
**의도**: `ATTACK_DEBUFF` 또는 `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A2) | 6 |
| A3+ | 8 |

**취약(Vulnerable) 디버프**:
- `canVuln = true`인 경우: 취약 2턴 부여
- `canVuln = false`인 경우: 디버프 없음

**코드 위치**: 36-40줄, 45줄, 60-66줄, 69줄, 100-107줄

```java
// 상수
private static final byte SKULL_BASH = 2;
private static final int BASH_DMG = 6;
private static final int A_2_BASH_DMG = 8;
private static final int DEBUFF_AMT = 2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.bashDmg = 8;
} else {
    this.bashDmg = 6;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.bashDmg));

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    if (this.canVuln) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)AbstractDungeon.player,
                (AbstractCreature)this,
                new VulnerablePower((AbstractCreature)AbstractDungeon.player, 2, true),
                2
            )
        );
    }
    break;
```

**특징**:
- 강력한 공격 이펙트 (BLUNT_HEAVY)
- 취약 디버프는 선택적 (생성자 파라미터)
- Intent가 `canVuln` 여부에 따라 변경됨

**수정 포인트**:
- 데미지 변경: `bashDmg` 필드 또는 A3 조건문
- 취약 턴 수 변경: DEBUFF_AMT 또는 ApplyPowerAction의 2 값
- 취약 강제 활성화: 생성자에서 `canVuln` 변경

---

### 패턴 3: 돌진 (Bull Rush)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A2) | 14 |
| A3+ | 16 |

**코드 위치**: 38-40줄, 45줄, 60-66줄, 68줄, 113-117줄

```java
// 상수
private static final byte BULL_RUSH = 1;
private static final int RUSH_DMG = 14;
private static final int A_2_RUSH_DMG = 16;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 3) {
    this.rushDmg = 16;
} else {
    this.rushDmg = 14;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.rushDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    break;
```

**특징**:
- 단순한 고데미지 공격
- Skull Bash와 동일한 공격 이펙트

**수정 포인트**:
- 데미지 변경: `rushDmg` 필드 또는 A3 조건문

---

## AI 로직 (getMove)

**매우 복잡한 패턴 선택 로직**

**코드 위치**: 138-185줄

### 첫 번째 턴
```java
if (!this.usedBellow) {
    this.usedBellow = true;
    setMove((byte)3, AbstractMonster.Intent.BUFF);
    return;
}
```
무조건 **Bellow** 사용

### 이후 턴 - A18+ (강화된 AI)

```java
if (AbstractDungeon.ascensionLevel >= 18) {
    if (!lastMove((byte)2) && !lastMoveBefore((byte)2)) {
        // Skull Bash를 연속 2번 안 썼으면 무조건 Skull Bash
        if (this.canVuln) {
            setMove(MOVES[0], (byte)2, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(1)).base);
        } else {
            setMove((byte)2, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(1)).base);
        }
        return;
    }
    if (lastTwoMoves((byte)1)) {
        // Bull Rush 연속 2번 사용 → Skull Bash
        [Skull Bash 설정]
    } else {
        // 그 외 → Bull Rush
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
    }
}
```

**A18+ 로직**:
1. Skull Bash를 마지막 2턴 동안 안 썼으면 → **무조건 Skull Bash**
2. Bull Rush를 연속 2번 썼으면 → **Skull Bash**
3. 그 외 → **Bull Rush**

**특징**:
- 매우 공격적인 AI
- Skull Bash의 취약 디버프가 자주 발동
- Bull Rush 연속 사용 방지

### 이후 턴 - A0-A17 (기본 AI)

```java
if (num < 33) {
    // 33% 확률로 Skull Bash
    [Skull Bash 설정]
    return;
}

if (lastTwoMoves((byte)1)) {
    // Bull Rush 연속 2번 사용 → Skull Bash
    [Skull Bash 설정]
} else {
    // 그 외 → Bull Rush
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
}
```

**기본 로직**:
1. 33% 확률로 → **Skull Bash**
2. Bull Rush 연속 2번 → **Skull Bash**
3. 그 외 67% 확률로 → **Bull Rush**

**특징**:
- 확률 기반
- Bull Rush가 더 자주 사용됨

---

## 특수 메커니즘

### 분노(Anger) 파워

**파워 클래스**: `AngerPower`
**효과**: 플레이어가 **스킬 카드**를 사용할 때마다 힘(Strength) 획득

**적용 시점**: 첫 번째 턴 Bellow 사용 시

**중요 사항**:
- 공격 카드는 분노를 발동시키지 않음
- 파워 카드도 분노를 발동시키지 않음
- **스킬 카드만** 분노를 발동시킴

**전략적 의미**:
- 그렘린 두목과의 전투에서는 스킬 카드 사용을 최소화해야 함
- 공격 카드 위주 덱이 유리함
- 방어 카드를 많이 쓰면 빠르게 강해짐

### 취약 디버프 조건부 발동

**변수**: `private boolean canVuln`
**설정 위치**: 52줄, 생성자 파라미터

```java
this.canVuln = setVuln;
```

**의미**:
- 특정 인카운터에서는 취약 없이 등장할 수 있음
- Intent 표시도 `canVuln` 여부에 따라 변경됨
- 게임 밸런스를 위한 메커니즘

### 음성 효과 랜덤 재생

**메서드**: `playSfx()` (124-133줄)

```java
private void playSfx() {
    int roll = MathUtils.random(2);
    if (roll == 0) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("VO_GREMLINNOB_1A")
        );
    } else if (roll == 1) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("VO_GREMLINNOB_1B")
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("VO_GREMLINNOB_1C")
        );
    }
}
```

**특징**:
- 3가지 음성 중 랜덤 선택
- Bellow 사용 시에만 재생

---

## 수정 예시

### 1. 분노 수치 대폭 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinNob",
    method = "takeTurn"
)
public static class GremlinNobAngerPatch {
    @SpirePostfixPatch
    public static void Postfix(GremlinNob __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Bellow 사용 시 (nextMove == 3) 추가 분노
            if (__instance.nextMove == 3) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new AngerPower(__instance, 2), 2)
                );
            }
        }
    }
}
```

**결과**: A25에서 Bellow 사용 시 분노 5 (기본 3 + 추가 2)

### 2. HP 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinNob",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class GremlinNobHPPatch {
    @SpirePostfixPatch
    public static void Postfix(GremlinNob __instance, float x, float y, boolean setVuln) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // HP 10% 증가
            int newMaxHP = (int)(__instance.maxHealth * 1.1f);
            __instance.maxHealth = newMaxHP;
            __instance.currentHealth = newMaxHP;
        }
    }
}
```

### 3. Skull Bash 취약 턴 수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinNob",
    method = "takeTurn"
)
public static class GremlinNobVulnPatch {
    @SpirePostfixPatch
    public static void Postfix(GremlinNob __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25 &&
            __instance.nextMove == 2) {
            // Skull Bash 사용 시 추가 취약 1턴
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, __instance,
                    new VulnerablePower(AbstractDungeon.player, 1, true), 1)
            );
        }
    }
}
```

**결과**: A25에서 Skull Bash 사용 시 취약 3턴 부여

### 4. A18 AI를 모든 난이도에 적용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.GremlinNob",
    method = "getMove"
)
public static class GremlinNobAIPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(GremlinNob __instance, int num) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // A25에서는 항상 A18 AI 사용
            // 원본 메서드의 A18 로직을 강제 활성화
            // (실제 구현은 더 복잡함)
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `usedBellow` | boolean | Bellow 사용 여부 (첫 턴 체크) |
| `canVuln` | boolean | 취약 디버프 부여 가능 여부 |
| `bashDmg` | int | Skull Bash 데미지 |
| `rushDmg` | int | Bull Rush 데미지 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/exordium/GremlinNob.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.AngerPower` (분노)
  - `com.megacrit.cardcrawl.powers.VulnerablePower` (취약)
- **액션**:
  - `AnimateSlowAttackAction` (공격 애니메이션)
  - `DamageAction` (데미지 적용)
  - `ApplyPowerAction` (파워 부여)
  - `TalkAction` (대사 출력)
  - `SFXAction` (음성 효과)

---

## 참고사항

1. **스킬 카드 사용 주의**: 분노 파워로 인해 스킬 카드가 매우 위험함
2. **A18 AI 변화**: A18부터 AI가 훨씬 공격적이고 예측 가능해짐
3. **첫 턴 고정**: 무조건 Bellow로 시작, 변경 시 주의 필요
4. **취약 디버프**: 게임에서 가장 위험한 디버프 중 하나
5. **엘리트 보상**: 체력 회복 및 레어 카드/유물 획득 기회
6. **애니메이션**: "images/monsters/theBottom/nobGremlin/" 경로 사용
7. **데미지 인덱스**: damage.get(0) = Bull Rush, damage.get(1) = Skull Bash
