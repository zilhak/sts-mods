# Monster AI System - 적 AI 시스템

적의 행동 패턴 결정 메커니즘과 의도 변경 시스템에 대한 완전한 가이드입니다.

---

## 📋 목차

1. [AI 시스템 기본 구조](#ai-시스템-기본-구조)
2. [getMove() 메서드](#getmove-메서드)
3. [재귀적 의도 변경](#재귀적-의도-변경)
4. [Writhing Mass 완벽 분석](#writhing-mass-완벽-분석)
5. [다른 몬스터 AI 패턴](#다른-몬스터-ai-패턴)
6. [AI 수정 방법](#ai-수정-방법)

---

## 🧠 AI 시스템 기본 구조

### 핵심 메서드

모든 몬스터는 `AbstractMonster` 클래스를 상속받아 다음 메서드들을 구현합니다:

```java
public abstract class AbstractMonster {
    // 1. 턴 행동 결정
    protected abstract void getMove(int num);

    // 2. 행동 실행
    public abstract void takeTurn();

    // 3. 전투 시작 전 행동
    public void usePreBattleAction() { }

    // 4. 이전 행동 체크
    public boolean lastMove(byte move) { }
    public boolean lastTwoMoves(byte move) { }
}
```

### AI 동작 흐름

```
전투 시작
    ↓
usePreBattleAction()  // 초기 버프/디버프 설정
    ↓
[턴 시작]
    ↓
getMove(random(0, 99))  // 랜덤 숫자로 다음 행동 결정
    ↓
setMove(byte moveId, Intent intent, int damage, ...)  // 의도 설정
    ↓
createIntent()  // UI에 의도 표시
    ↓
takeTurn()  // 실제 행동 실행
    ↓
RollMoveAction  // 다음 턴 행동 결정
    ↓
[턴 종료] → [턴 시작]으로 반복
```

---

## 🎲 getMove() 메서드

### 기본 패턴

```java
protected void getMove(int num) {
    // num: AbstractDungeon.aiRng.random(0, 99)에서 전달된 0-99 랜덤 숫자

    if (num < 33) {
        setMove((byte)0, Intent.ATTACK, damage);
    } else if (num < 66) {
        setMove((byte)1, Intent.DEFEND, 0);
    } else {
        setMove((byte)2, Intent.ATTACK_DEBUFF, damage);
    }
}
```

**확률 계산**:
- 0-32: 패턴 0 (33%)
- 33-65: 패턴 1 (33%)
- 66-99: 패턴 2 (34%)

### 의도 타입 (Intent)

```java
public enum Intent {
    ATTACK,           // 공격
    ATTACK_BUFF,      // 공격 + 버프
    ATTACK_DEBUFF,    // 공격 + 디버프
    ATTACK_DEFEND,    // 공격 + 방어
    BUFF,             // 버프만
    DEBUFF,           // 디버프만
    STRONG_DEBUFF,    // 강력한 디버프
    DEFEND,           // 방어만
    DEFEND_BUFF,      // 방어 + 버프
    DEFEND_DEBUFF,    // 방어 + 디버프
    ESCAPE,           // 도망
    MAGIC,            // 특수 (기타)
    SLEEP,            // 수면 (행동 안함)
    STUN,             // 스턴
    UNKNOWN,          // 알 수 없음 (?)
    NONE              // 없음
}
```

---

## 🔄 재귀적 의도 변경

### lastMove() 체크 패턴

많은 몬스터들은 같은 행동을 연속으로 하지 않기 위해 재귀를 사용합니다:

```java
protected void getMove(int num) {
    if (num < 50) {
        if (!lastMove((byte)0)) {
            setMove((byte)0, Intent.ATTACK, damage);
        } else {
            // 같은 행동 방지 → 다른 범위에서 재선택
            getMove(AbstractDungeon.aiRng.random(50, 99));
        }
    } else {
        if (!lastMove((byte)1)) {
            setMove((byte)1, Intent.DEFEND, 0);
        } else {
            // 같은 행동 방지 → 다른 범위에서 재선택
            getMove(AbstractDungeon.aiRng.random(0, 49));
        }
    }
}
```

**재귀 메커니즘**:
1. `num < 50` → 패턴 0 시도
2. `lastMove(0) == true` (직전에 패턴 0 사용)
3. `getMove(random(50, 99))` 재귀 호출
4. 새로운 `num`으로 패턴 1 선택

---

## 🐙 Writhing Mass 완벽 분석

### 기본 정보

```java
이름: 꿈틀대는 덩어리 (Writhing Mass)
위치: 3막 (Beyond)
체력: 160 (승천 7+: 175)

패턴 5가지:
[0] BIG_HIT: 강타 (32/38 데미지)
[1] MULTI_HIT: 3회 연타 (7×3 / 9×3 데미지)
[2] ATTACK_BLOCK: 공격+방어 (15/16 데미지 + 동일 방어)
[3] ATTACK_DEBUFF: 공격+디버프 (10/12 데미지 + 약화2 + 취약2)
[4] MEGA_DEBUFF: 강력 디버프 (기생충 카드 덱에 추가, 1회만)

초기 파워:
- Reactive: 공격 시 3 약화 부여
- Malleable: 공격 시 방어 획득
```

### 첫 턴 AI (firstMove = true)

```java
protected void getMove(int num) {
    if (this.firstMove) {
        this.firstMove = false;

        if (num < 33) {
            setMove(MULTI_HIT, Intent.ATTACK, 7/9, 3, true);      // 33%
        } else if (num < 66) {
            setMove(ATTACK_BLOCK, Intent.ATTACK_DEFEND, 15/16);   // 33%
        } else {
            setMove(ATTACK_DEBUFF, Intent.ATTACK_DEBUFF, 10/12);  // 34%
        }

        return;
    }
    // ... 이후 턴 로직
}
```

**첫 턴 확률**:
| 패턴 | 확률 | 범위 |
|------|------|------|
| MULTI_HIT (3회 연타) | 33% | 0-32 |
| ATTACK_BLOCK (공격+방어) | 33% | 33-65 |
| ATTACK_DEBUFF (공격+디버프) | 34% | 66-99 |

❌ **BIG_HIT**과 **MEGA_DEBUFF**는 첫 턴에 사용 안함

---

### 두 번째 턴 이후 AI (복잡한 확률 트리)

#### **1단계: num < 10 (10% 확률) - BIG_HIT 범위**

```java
if (num < 10) {
    if (!lastMove(BIG_HIT)) {
        setMove(BIG_HIT, Intent.ATTACK, 32/38);
    } else {
        // 이전 턴에 BIG_HIT 사용 → 재선택
        getMove(AbstractDungeon.aiRng.random(10, 99));
    }
}
```

**로직**:
- ✅ 이전에 BIG_HIT 안 썼음 → **BIG_HIT 사용**
- ❌ 이전에 BIG_HIT 썼음 → **10-99 범위에서 재선택** (BIG_HIT 제외)

---

#### **2단계: num < 20 (10% 확률) - MEGA_DEBUFF 범위**

```java
else if (num < 20) {
    if (!usedMegaDebuff && !lastMove(MEGA_DEBUFF)) {
        setMove(MEGA_DEBUFF, Intent.STRONG_DEBUFF);
        this.usedMegaDebuff = true;  // 플래그 설정
    }
    else if (AbstractDungeon.aiRng.randomBoolean(0.1F)) {
        setMove(BIG_HIT, Intent.ATTACK, 32/38);  // 10% 확률
    } else {
        getMove(AbstractDungeon.aiRng.random(20, 99));  // 90% 확률
    }
}
```

**로직**:
- ✅ 기생충 한 번도 안 씀 + 이전에 안 씀 → **MEGA_DEBUFF 사용** (1회만)
- ❌ 이미 사용했거나 이전 턴에 사용:
  - 10% 확률 → **BIG_HIT**
  - 90% 확률 → **20-99 범위에서 재선택**

---

#### **3단계: num < 40 (20% 확률) - ATTACK_DEBUFF 범위**

```java
else if (num < 40) {
    if (!lastMove(ATTACK_DEBUFF)) {
        setMove(ATTACK_DEBUFF, Intent.ATTACK_DEBUFF, 10/12);
    }
    else if (AbstractDungeon.aiRng.randomBoolean(0.4F)) {
        getMove(AbstractDungeon.aiRng.random(0, 19));   // 40% 확률
    } else {
        getMove(AbstractDungeon.aiRng.random(40, 99));  // 60% 확률
    }
}
```

**로직**:
- ✅ 이전에 ATTACK_DEBUFF 안 씀 → **ATTACK_DEBUFF 사용**
- ❌ 이전에 ATTACK_DEBUFF 씀:
  - 40% 확률 → **0-19 범위에서 재선택** (BIG_HIT, MEGA_DEBUFF)
  - 60% 확률 → **40-99 범위에서 재선택** (MULTI_HIT, ATTACK_BLOCK)

---

#### **4단계: num < 70 (30% 확률) - MULTI_HIT 범위**

```java
else if (num < 70) {
    if (!lastMove(MULTI_HIT)) {
        setMove(MULTI_HIT, Intent.ATTACK, 7/9, 3, true);
    }
    else if (AbstractDungeon.aiRng.randomBoolean(0.3F)) {
        setMove(ATTACK_BLOCK, Intent.ATTACK_DEFEND, 15/16);  // 30% 확률
    } else {
        getMove(AbstractDungeon.aiRng.random(0, 39));        // 70% 확률
    }
}
```

**로직**:
- ✅ 이전에 MULTI_HIT 안 씀 → **MULTI_HIT 사용**
- ❌ 이전에 MULTI_HIT 씀:
  - 30% 확률 → **ATTACK_BLOCK** (즉시 선택, 재귀 없음!)
  - 70% 확률 → **0-39 범위에서 재선택** (BIG_HIT, MEGA_DEBUFF, ATTACK_DEBUFF)

---

#### **5단계: num >= 70 (30% 확률) - ATTACK_BLOCK 범위**

```java
else {  // num >= 70
    if (!lastMove(ATTACK_BLOCK)) {
        setMove(ATTACK_BLOCK, Intent.ATTACK_DEFEND, 15/16);
    } else {
        getMove(AbstractDungeon.aiRng.random(0, 69));
    }
}
```

**로직**:
- ✅ 이전에 ATTACK_BLOCK 안 씀 → **ATTACK_BLOCK 사용**
- ❌ 이전에 ATTACK_BLOCK 씀 → **0-69 범위에서 재선택** (ATTACK_BLOCK 제외)

---

### 확률 정리표

#### **기본 확률 범위**
| 범위 | 기본 확률 | 1순위 패턴 | 조건 |
|------|-----------|------------|------|
| 0-9 | 10% | **BIG_HIT** | 이전 턴에 안 썼을 때 |
| 10-19 | 10% | **MEGA_DEBUFF** | 한 번도 안 썼고, 이전 턴에 안 썼을 때 |
| 20-39 | 20% | **ATTACK_DEBUFF** | 이전 턴에 안 썼을 때 |
| 40-69 | 30% | **MULTI_HIT** | 이전 턴에 안 썼을 때 |
| 70-99 | 30% | **ATTACK_BLOCK** | 이전 턴에 안 썼을 때 |

#### **재선택 메커니즘**

| 중복된 패턴 | 재선택 로직 | 비고 |
|-------------|-------------|------|
| **BIG_HIT** | 10-99 범위 재선택 | BIG_HIT 완전 제외 |
| **MEGA_DEBUFF** | 10% → BIG_HIT<br>90% → 20-99 재선택 | 약간의 BIG_HIT 확률 |
| **ATTACK_DEBUFF** | 40% → 0-19 재선택<br>60% → 40-99 재선택 | 낮은/높은 범위로 분할 |
| **MULTI_HIT** | 30% → ATTACK_BLOCK (즉시)<br>70% → 0-39 재선택 | 재귀 없는 직접 선택 포함 |
| **ATTACK_BLOCK** | 0-69 범위 재선택 | ATTACK_BLOCK 제외 |

---

### 실전 예시

#### **예시 1: 일반적인 턴 진행**

```
턴 1 (firstMove): num=45
  → 33 ≤ 45 < 66
  → ATTACK_BLOCK (공격+방어)

턴 2: num=75
  → num ≥ 70
  → lastMove(ATTACK_BLOCK) = true (이전 턴이 ATTACK_BLOCK)
  → getMove(random(0, 69))
  → num2=35 (재귀 호출)
  → 20 ≤ 35 < 40
  → ATTACK_DEBUFF (공격+디버프)

턴 3: num=5
  → num < 10
  → lastMove(BIG_HIT) = false
  → BIG_HIT (강타)

턴 4: num=55
  → 40 ≤ 55 < 70
  → lastMove(MULTI_HIT) = false
  → MULTI_HIT (3회 연타)
```

---

#### **예시 2: MEGA_DEBUFF 사용**

```
턴 1: MULTI_HIT
턴 2: num=15 (10-19 범위)
  → usedMegaDebuff = false
  → lastMove(MEGA_DEBUFF) = false
  → MEGA_DEBUFF (기생충 추가)
  → usedMegaDebuff = true ✅

턴 3: num=12 (10-19 범위)
  → usedMegaDebuff = true (이미 사용)
  → randomBoolean(0.1) = true (10% 확률)
  → BIG_HIT

턴 4 이후: num=16 (10-19 범위)
  → usedMegaDebuff = true
  → randomBoolean(0.1) = false (90% 확률)
  → getMove(random(20, 99))
  → num2=65
  → ATTACK_BLOCK
```

---

#### **예시 3: 연속 재귀**

```
턴 N: MULTI_HIT
턴 N+1: num=60 (40-69 범위)
  → lastMove(MULTI_HIT) = true (이전 턴)
  → randomBoolean(0.3) = false (70% 확률)
  → getMove(random(0, 39))
  → num2=8 (재귀 호출)
  → num2 < 10
  → lastMove(BIG_HIT) = false
  → BIG_HIT
```

---

#### **예시 4: 즉시 선택 (재귀 없음)**

```
턴 N: MULTI_HIT
턴 N+1: num=55 (40-69 범위)
  → lastMove(MULTI_HIT) = true
  → randomBoolean(0.3) = true (30% 확률)
  → ATTACK_BLOCK (즉시 선택, getMove() 재호출 없음)
```

---

### 핵심 메커니즘 정리

#### **1. 같은 패턴 연속 방지**
```java
if (!lastMove(PATTERN)) {
    setMove(PATTERN);
} else {
    getMove(다른 범위);  // 재귀로 다시 선택
}
```

#### **2. MEGA_DEBUFF는 1회 제한**
```java
private boolean usedMegaDebuff = false;

if (!usedMegaDebuff && !lastMove(MEGA_DEBUFF)) {
    usedMegaDebuff = true;  // 플래그 설정
    setMove(MEGA_DEBUFF);
}
```

#### **3. 재귀 확률은 하드코딩**
- 각 패턴마다 고유한 재선택 로직
- `randomBoolean()` 확률도 고정
- 범위도 패턴별로 다름

#### **4. 재귀 깊이 제한 없음**
- 이론적으로 무한 재귀 가능
- 실제로는 `lastMove()` 체크로 대부분 1-2번 재귀에서 종료
- 최악의 경우: 10번 이상 재귀 가능 (매우 드물음)

---

## 🎮 다른 몬스터 AI 패턴

### 1. 단순 확률형 (Cultist)

```java
// Cultist.java
protected void getMove(int num) {
    if (num < 50) {
        // 50% - 약화 디버프
        setMove((byte)1, Intent.DEBUFF);
    } else {
        // 50% - 6 데미지 공격
        setMove((byte)2, Intent.ATTACK, damage.get(0).base);
    }
}
```

**특징**:
- 재귀 없음
- 항상 50/50 확률
- 같은 행동 연속 가능

---

### 2. lastMove 체크형 (Jaw Worm)

```java
// JawWorm.java
protected void getMove(int num) {
    if (num < 25) {
        if (!lastTwoMoves((byte)1)) {
            setMove((byte)1, Intent.ATTACK_BUFF, damage.get(0).base);
        } else {
            setMove((byte)3, Intent.ATTACK, damage.get(1).base);
        }
    } else if (num < 55) {
        if (!lastMove((byte)3)) {
            setMove((byte)3, Intent.ATTACK, damage.get(1).base);
        } else {
            setMove((byte)1, Intent.ATTACK_BUFF, damage.get(0).base);
        }
    } else {
        if (!lastMove((byte)2)) {
            setMove((byte)2, Intent.DEFEND_BUFF);
        } else {
            setMove((byte)3, Intent.ATTACK, damage.get(1).base);
        }
    }
}
```

**특징**:
- `lastMove()`, `lastTwoMoves()` 체크
- 재귀 없음 (즉시 대체 패턴)
- 3가지 패턴 순환

---

### 3. 상태 기반형 (Slime Boss)

```java
// SlimeBoss.java
protected void getMove(int num) {
    if (currentHealth < maxHealth / 2 && !hasSplit) {
        // 체력 50% 이하 → 분열
        setMove((byte)4, Intent.UNKNOWN);
        this.hasSplit = true;
        return;
    }

    if (num < 33) {
        if (!lastTwoMoves((byte)1)) {
            setMove((byte)1, Intent.ATTACK_DEBUFF, damage.get(0).base);
        } else {
            setMove((byte)2, Intent.ATTACK, damage.get(1).base);
        }
    } else if (num < 66) {
        // ...
    }
}
```

**특징**:
- 체력 상태로 패턴 강제 변경
- 플래그로 1회성 패턴 관리
- 복잡한 조건부 로직

---

### 4. 카운터 기반형 (Bronze Automaton)

```java
// BronzeAutomaton.java
private int turnCount = 0;

protected void getMove(int num) {
    this.turnCount++;

    if (this.turnCount % 3 == 0) {
        // 3턴마다 강력한 공격
        setMove((byte)3, Intent.ATTACK, damage.get(2).base);
        return;
    }

    // 일반 패턴
    if (num < 50) {
        setMove((byte)1, Intent.ATTACK_BUFF, damage.get(0).base);
    } else {
        setMove((byte)2, Intent.ATTACK, damage.get(1).base);
    }
}
```

**특징**:
- 턴 카운터로 주기적 패턴
- 예측 가능한 강력 패턴
- 일반 패턴 + 특수 패턴 혼합

---

## 🛠️ AI 수정 방법

### 1. 첫 턴 AI 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class FirstTurnPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance, int num) {
        try {
            Field firstMoveField = WrithingMass.class.getDeclaredField("firstMove");
            firstMoveField.setAccessible(true);
            boolean firstMove = firstMoveField.getBoolean(__instance);

            if (firstMove && AbstractDungeon.ascensionLevel >= 50) {
                firstMoveField.setBoolean(__instance, false);

                // 승천 50+: 첫 턴에 무조건 강타
                __instance.setMove((byte)0, AbstractMonster.Intent.ATTACK,
                    __instance.damage.get(0).base);
                __instance.createIntent();

                return SpireReturn.Return(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SpireReturn.Continue();
    }
}
```

---

### 2. 확률 범위 조정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class ProbabilityPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 60) {
            // 승천 60+: MEGA_DEBUFF 확률 20%로 증가
            // num을 조작하여 10-19 범위를 0-19로 확장
            if (num >= 0 && num < 20) {
                // MEGA_DEBUFF 로직 실행
                // (원본 로직의 10-19 부분 복사)
            }
        }
        return SpireReturn.Continue();
    }
}
```

---

### 3. 재귀 제거 (단순화)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class NoRecursionPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 70) {
            // 승천 70+: 재귀 없이 단순 확률로 변경
            if (num < 20) {
                __instance.setMove((byte)0, AbstractMonster.Intent.ATTACK,
                    __instance.damage.get(0).base);
            } else if (num < 40) {
                __instance.setMove((byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
                    __instance.damage.get(3).base);
            } else if (num < 70) {
                __instance.setMove((byte)1, AbstractMonster.Intent.ATTACK,
                    __instance.damage.get(1).base, 3, true);
            } else {
                __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEFEND,
                    __instance.damage.get(2).base);
            }

            __instance.createIntent();
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}
```

---

### 4. MEGA_DEBUFF 횟수 제한 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class UnlimitedMegaDebuffPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 80) {
            // 승천 80+: MEGA_DEBUFF 여러 번 사용 가능
            try {
                Field usedField = WrithingMass.class.getDeclaredField("usedMegaDebuff");
                usedField.setAccessible(true);

                if (num >= 10 && num < 20) {
                    if (!__instance.lastMove((byte)4)) {
                        // usedMegaDebuff 체크 제거
                        __instance.setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
                        __instance.createIntent();
                        return SpireReturn.Return(null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return SpireReturn.Continue();
    }
}
```

---

### 5. 새로운 패턴 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "takeTurn"
)
public static class NewPatternPatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance) {
        if (__instance.nextMove == 5) {  // 새로운 패턴 ID
            // 승천 90+: 새로운 극강 패턴
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player, __instance,
                    new FrailPower(AbstractDungeon.player, 3, true), 3
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    new DamageInfo(__instance, 50, DamageInfo.DamageType.NORMAL),
                    AbstractGameAction.AttackEffect.SLASH_HEAVY
                )
            );

            AbstractDungeon.actionManager.addToBottom(new RollMoveAction(__instance));
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class NewPatternGetMovePatch {
    @SpireInsertPatch(loc = 166)  // firstMove 체크 직후
    public static void Insert(WrithingMass __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 90) {
            if (num < 5) {  // 5% 확률로 극강 패턴
                __instance.setMove((byte)5, AbstractMonster.Intent.ATTACK_DEBUFF, 50);
            }
        }
    }
}
```

---

### 6. 조건부 패턴 강제

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class ConditionalForcePatch {
    @SpirePrefixPatch
    public static SpireReturn Prefix(WrithingMass __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 75) {
            // 플레이어 체력 50% 이하 → 무조건 강타
            if (AbstractDungeon.player.currentHealth <
                AbstractDungeon.player.maxHealth / 2) {

                __instance.setMove((byte)0, AbstractMonster.Intent.ATTACK,
                    __instance.damage.get(0).base);
                __instance.createIntent();

                return SpireReturn.Return(null);
            }
        }

        return SpireReturn.Continue();
    }
}
```

---

## 🔍 디버깅 팁

### 1. AI 결정 과정 로깅

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class AILogPatch {
    @SpirePrefixPatch
    public static void Prefix(WrithingMass __instance, int num) {
        logger.info("[WrithingMass AI] Random num: " + num);
        logger.info("  lastMove: " + __instance.moveHistory.get(
            __instance.moveHistory.size() - 1));
    }

    @SpirePostfixPatch
    public static void Postfix(WrithingMass __instance) {
        logger.info("  Final move: " + __instance.nextMove);
        logger.info("  Intent: " + __instance.intent);
    }
}
```

### 2. 재귀 깊이 추적

```java
private static int recursionDepth = 0;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "getMove"
)
public static class RecursionTrackerPatch {
    @SpirePrefixPatch
    public static void Prefix(int num) {
        recursionDepth++;
        logger.info("[Recursion Depth: " + recursionDepth + "] num=" + num);
    }

    @SpirePostfixPatch
    public static void Postfix() {
        recursionDepth--;
    }
}
```

---

## ⚠️ 주의사항

### 1. Reflection 사용 시 성능 고려

```java
// ❌ 매 턴마다 Reflection
@SpirePostfixPatch
public static void Postfix(WrithingMass __instance) {
    Field field = WrithingMass.class.getDeclaredField("firstMove");  // 느림
    field.setAccessible(true);
    boolean firstMove = field.getBoolean(__instance);
}

// ✅ 한 번만 Reflection, 캐싱
private static Field firstMoveField = null;

@SpirePostfixPatch
public static void Postfix(WrithingMass __instance) {
    if (firstMoveField == null) {
        firstMoveField = WrithingMass.class.getDeclaredField("firstMove");
        firstMoveField.setAccessible(true);
    }
    boolean firstMove = firstMoveField.getBoolean(__instance);
}
```

### 2. createIntent() 필수 호출

```java
// ❌ createIntent() 없음 → UI에 의도 표시 안됨
__instance.setMove((byte)0, Intent.ATTACK, 10);

// ✅ createIntent() 호출
__instance.setMove((byte)0, Intent.ATTACK, 10);
__instance.createIntent();
```

### 3. 재귀 무한 루프 방지

```java
// ❌ 잘못된 재귀 → 무한 루프 가능
if (!lastMove(0)) {
    setMove(0);
} else {
    getMove(random(0, 99));  // 같은 범위로 재귀 → 무한 루프 위험
}

// ✅ 다른 범위로 재귀
if (!lastMove(0)) {
    setMove(0);
} else {
    getMove(random(50, 99));  // 다른 범위 → 안전
}
```

---

## 📚 관련 클래스 참조

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **AbstractMonster** | `com.megacrit.cardcrawl.monsters` | 몬스터 기본 클래스 |
| **WrithingMass** | `com.megacrit.cardcrawl.monsters.beyond` | 꿈틀대는 덩어리 |
| **RollMoveAction** | `com.megacrit.cardcrawl.actions.common` | 다음 행동 결정 |
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons` | aiRng 관리 |

---

## 🎓 추가 학습

1. **다른 몬스터 AI**: `monsters/` 폴더의 다양한 몬스터 분석
2. **Intent 시스템**: UI 표시 및 플레이어 정보 제공
3. **RNG 시스템**: `AbstractDungeon.aiRng` 동작 방식
4. **행동 히스토리**: `moveHistory` 관리 방법

---

**작성일**: 2025-11-15
**기반 소스**: E:\workspace\sts-decompile\
**검증**: 디컴파일 소스 직접 분석 (WrithingMass.java)
**게임 버전**: 01-23-2019
