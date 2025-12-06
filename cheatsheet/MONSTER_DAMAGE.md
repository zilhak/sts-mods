# 적 공격력 수정 가이드

적의 공격력(Damage)을 수정하는 모든 방법을 상세히 설명합니다.

## 📑 목차

1. [⚠️ Critical: Intent Display와 데미지 타이밍](#️-critical-intent-display와-데미지-타이밍)
2. [기본 공격력 수정](#기본-공격력-수정)
3. [타입별 공격력 수정](#타입별-공격력-수정)
4. [막별 공격력 수정](#막별-공격력-수정)
5. [복합 공격력 수정](#복합-공격력-수정)
6. [특정 적 공격력 수정](#특정-적-공격력-수정)
7. [Intent 수정이 필요한 경우](#intent-수정이-필요한-경우)
8. [실전 예제](#실전-예제)

---

## ⚠️ Critical: Intent Display와 데미지 타이밍

### 핵심 문제

**`DamageInfo.base`를 수정해도 Intent 표시가 업데이트되지 않습니다!**

Intent에 표시되는 데미지는 `DamageInfo.base`가 아닌 **`EnemyMoveInfo.baseDamage`** 값에서 가져옵니다.

### Intent Display 메커니즘

```java
// 전투 시작 시퀀스
1. Monster 생성자 → damage 배열 초기화
2. init() → rollMove() → getMove() → setMove(moveId, intent, baseDamage)
   └─> move.baseDamage = baseDamage  // Intent 데미지가 여기서 설정됨!
3. usePreBattleAction() 호출  // ← DamageInfo.base 수정은 여기서
4. applyPowers() → calculateDamage(move.baseDamage)
   └─> intentDmg = move.baseDamage (with buffs)
5. Intent 렌더링 → intentDmg 값 표시
```

**문제**: `usePreBattleAction()`은 `setMove()` **이후**에 호출됩니다!
- `DamageInfo.base`는 증가하지만
- `move.baseDamage`는 그대로
- **Intent는 옛날 값을 표시**

### 하드코딩 문제 몬스터

일부 몬스터는 `setMove()` 호출 시 `damage` 배열 대신 **하드코딩된 값**을 사용합니다:

| 몬스터 | 클래스 | 문제 | 하드코딩 값 |
|--------|--------|------|-------------|
| 노예 관리자 | `Taskmaster` | `setMove()`에서 `7` 하드코딩 | `DEBUFF_DMG = 7` |
| 거인의 머리 | `GiantHead` | `setMove()`에서 `13` 하드코딩 | `COUNT_DMG = 13` |
| 네메시스 | `Nemesis` | 화염 공격에 `fireDmg` 필드 사용 | `BURN_DMG = 45` |

**예시: Taskmaster의 문제**
```java
// Taskmaster.getMove() - 디컴파일 소스
protected void getMove(int num) {
    if (this.nextMove == 2) {
        setMove((byte)2, Intent.ATTACK_DEBUFF, 7);  // ← 하드코딩된 7!
        // damage.get(1).base가 아님!
    }
}
```

### 해결 방법 요약

| 상황 | 권장 패치 지점 | 비고 |
|------|---------------|------|
| 일반 몬스터 (DamageInfo만 수정) | `usePreBattleAction()` Postfix | Intent 불일치 무시 가능 |
| Intent 정확도 필요 | `init()` Prefix | `setMove()` 전에 실행 |
| 하드코딩 몬스터 | `getMove()` Postfix + `setMove()` 재호출 | 특수 케이스 |
| 특수 필드 사용 (Nemesis) | Reflection으로 필드 수정 | `fireDmg` 등 |

---

## 기본 공격력 수정

### ⚠️ Intent Display 주의사항

**이 섹션의 모든 패턴은 `usePreBattleAction()` 시점에 수정합니다.**
- ✅ **실제 데미지**는 정확히 증가합니다
- ⚠️ **Intent 표시**는 옛날 값을 보여줄 수 있습니다
- 💡 Intent 정확도가 중요하다면 [Intent 수정이 필요한 경우](#intent-수정이-필요한-경우) 섹션을 참조하세요

### 패치 시점: `usePreBattleAction()` 메서드

적의 공격력은 **`usePreBattleAction()` 메서드**에서 수정합니다. 전투 시작 직전에 호출됩니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "usePreBattleAction"
)
public static class DamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 공격력 수정 코드
    }
}
```

### DamageInfo 구조 이해

```java
public class AbstractMonster {
    public ArrayList<DamageInfo> damage;  // 모든 공격 데미지 리스트
}

public class DamageInfo {
    public int base;          // 기본 데미지
    public int output;        // 최종 데미지 (파워 적용 후)
    public DamageType type;   // 데미지 타입
}
```

### 패턴 1: 모든 공격력 고정값 추가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class DamageBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            int increase = 2;  // 모든 공격 +2

            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base += increase;
                }
            }
        }
    }
}
```

**중요**:
- `__instance.damage`: 모든 공격의 DamageInfo 리스트
- `dmg.base`: 기본 데미지 (이것을 수정)
- Null 체크 필수: `if (dmg != null)`
- 데미지 0 체크: `if (dmg.base > 0)` (방어 행동 등 제외)

### 패턴 2: 모든 공격력 배율 적용

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class DamageMultiplierPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            float multiplier = 1.1f;  // 10% 증가

            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    int originalDamage = dmg.base;
                    dmg.base = MathUtils.ceil(dmg.base * multiplier);

                    logger.info(String.format(
                        "%s damage %d → %d",
                        __instance.name, originalDamage, dmg.base
                    ));
                }
            }
        }
    }
}
```

### 패턴 3: 누적 증가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class CumulativeDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int level = AbstractDungeon.ascensionLevel;
        int increase = 0;

        // 누적 증가
        if (level >= 52) increase += 1;  // +1
        if (level >= 58) increase += 1;  // +2 (총)
        if (level >= 62) increase += 1;  // +3 (총)

        if (increase > 0) {
            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base += increase;
                }
            }
        }
    }
}
```

---

## 타입별 공격력 수정

### 패턴 1: 엘리트만 증가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class EliteDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 23 &&
            __instance.type == AbstractMonster.EnemyType.ELITE) {

            float multiplier = 1.1f;  // 엘리트 공격력 10% 증가

            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base = MathUtils.ceil(dmg.base * multiplier);
                }
            }
        }
    }
}
```

### 패턴 2: 타입별 다른 배율

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class TypeBasedDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            float multiplier = 1.0f;

            switch (__instance.type) {
                case NORMAL:
                    multiplier = 1.05f;  // 일반: 5% 증가
                    break;
                case ELITE:
                    multiplier = 1.1f;   // 엘리트: 10% 증가
                    break;
                case BOSS:
                    multiplier = 1.15f;  // 보스: 15% 증가
                    break;
            }

            if (multiplier > 1.0f) {
                for (DamageInfo dmg : __instance.damage) {
                    if (dmg != null && dmg.base > 0) {
                        dmg.base = MathUtils.ceil(dmg.base * multiplier);
                    }
                }
            }
        }
    }
}
```

### 패턴 3: 타입별 고정값

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class TypeBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 34) {
            int increase = 0;

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                increase = 2;  // 엘리트 공격력 +2
            } else if (__instance.type == AbstractMonster.EnemyType.NORMAL) {
                increase = 1;  // 일반 공격력 +1
            }

            if (increase > 0) {
                for (DamageInfo dmg : __instance.damage) {
                    if (dmg != null && dmg.base > 0) {
                        dmg.base += increase;
                    }
                }
            }
        }
    }
}
```

---

## 막별 공격력 수정

### 패턴 1: 막별 다른 증가량

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class ActBasedDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 68) {
            int act = AbstractDungeon.actNum;
            int increase = 0;

            switch (act) {
                case 1:
                    increase = 1;  // 1막: +1
                    break;
                case 2:
                    increase = 2;  // 2막: +2
                    break;
                default:
                    increase = 5;  // 3막+: +5
                    break;
            }

            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base += increase;
                }
            }
        }
    }
}
```

### 패턴 2: 보스 막별 강화 (Level 64 예시)

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class BossActDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 64 &&
            __instance.type == AbstractMonster.EnemyType.BOSS) {

            float multiplier = 1.1f;  // 보스 공격력 10% 증가

            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base = MathUtils.ceil(dmg.base * multiplier);
                }
            }
        }
    }
}
```

---

## 복합 공격력 수정

### 패턴: 레벨 + 타입 복합

ascension-100 모드의 실제 구현:

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class ComplexDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;
        int increase = 0;

        // 타입별 배율
        if (level >= 23 && __instance.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;  // 엘리트 10%
        }
        if (level >= 24 && __instance.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;  // 일반 10%
        }

        // 레벨별 고정값 증가
        if (level >= 34 && __instance.type == AbstractMonster.EnemyType.ELITE) {
            increase += 2;
        }
        if (level >= 35 && __instance.type == AbstractMonster.EnemyType.NORMAL) {
            increase += 1;
        }
        if (level >= 52) {
            increase += 1;  // 모든 적 +1
        }
        if (level >= 58) {
            increase += 1;  // 모든 적 +1 (총 +2)
        }
        if (level >= 62) {
            increase += 1;  // 모든 적 +1 (총 +3)
        }

        // 막별 증가
        if (level >= 68) {
            int act = AbstractDungeon.actNum;
            if (act == 1) increase += 1;
            else if (act == 2) increase += 2;
            else increase += 5;
        }

        // 보스 추가 증가
        if (level >= 64 && __instance.type == AbstractMonster.EnemyType.BOSS) {
            multiplier *= 1.1f;
        }

        // 적용
        for (DamageInfo dmg : __instance.damage) {
            if (dmg != null && dmg.base > 0) {
                int originalDamage = dmg.base;

                // 배율 먼저 적용
                if (multiplier > 1.0f) {
                    dmg.base = MathUtils.ceil(dmg.base * multiplier);
                }

                // 고정값 추가
                if (increase > 0) {
                    dmg.base += increase;
                }

                logger.info(String.format(
                    "Ascension %d: %s damage %d → %d (x%.2f, +%d)",
                    level, __instance.name, originalDamage, dmg.base, multiplier, increase
                ));
            }
        }
    }
}
```

**계산 예시 (Level 68, 3막 엘리트, 기본 공격력 10)**:
```
10 (기본)
× 1.1 (Level 23 엘리트 배율)
= 11
+ 2 (Level 34 엘리트)
+ 1 (Level 52 전체)
+ 1 (Level 58 전체)
+ 1 (Level 62 전체)
+ 5 (Level 68, 3막)
= 21 (110% 증가)
```

---

## 특정 적 공격력 수정

### 패턴 1: 생성자에서 damage 배열 수정

```java
import com.megacrit.cardcrawl.monsters.exordium.Cultist;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Cultist의 기본 공격력 -2
            if (!__instance.damage.isEmpty()) {
                DamageInfo dmg = __instance.damage.get(0);
                if (dmg != null && dmg.base > 0) {
                    dmg.base = Math.max(1, dmg.base - 2);  // 최소 1
                }
            }
        }
    }
}
```

### 패턴 2: 특정 인덱스 공격 수정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.BronzeAutomaton",
    method = "usePreBattleAction"
)
public static class AutomatonDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(BronzeAutomaton __instance) {
        if (AbstractDungeon.ascensionLevel >= 26) {
            // 첫 번째 공격(Boost)만 +2
            if (!__instance.damage.isEmpty()) {
                DamageInfo dmg = __instance.damage.get(0);
                if (dmg != null && dmg.base > 0) {
                    dmg.base += 2;
                }
            }

            // 두 번째 공격(Hyper Beam)은 그대로
        }
    }
}
```

### 패턴 3: 이름으로 구분

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class SpecificMonsterDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            int increase = 0;

            if (__instance.name.equals("Jaw Worm")) {
                increase = 3;  // Jaw Worm 공격력 +3
            } else if (__instance.name.equals("Cultist")) {
                increase = -2;  // Cultist 공격력 -2
            }

            if (increase != 0) {
                for (DamageInfo dmg : __instance.damage) {
                    if (dmg != null && dmg.base > 0) {
                        dmg.base = Math.max(1, dmg.base + increase);
                    }
                }
            }
        }
    }
}
```

---

## Intent 수정이 필요한 경우

Intent 표시를 정확히 업데이트해야 하는 경우의 패턴들입니다.

### 패턴 1: `init()` Prefix - 일반 몬스터 (권장)

`init()` 시점에 `damage` 배열을 수정하면 이후 `setMove()` 호출 시 올바른 값이 사용됩니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class InitDamagePatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 58 &&
            __instance.type == AbstractMonster.EnemyType.NORMAL) {

            int damageIncrease = 1;

            for (DamageInfo damageInfo : __instance.damage) {
                if (damageInfo != null && damageInfo.base > 0) {
                    damageInfo.base += damageIncrease;
                    damageInfo.output = damageInfo.base;  // output도 업데이트
                }
            }

            logger.info(String.format(
                "Ascension 58: %s damage increased by %d [init prefix]",
                __instance.name, damageIncrease
            ));
        }
    }
}
```

**장점**:
- `setMove()` 전에 실행되므로 Intent가 자동으로 올바른 값 표시
- 대부분의 몬스터에 안전하게 사용 가능

**단점**:
- 하드코딩 몬스터에는 효과 없음

### 패턴 2: `getMove()` Postfix - 하드코딩 몬스터 (Taskmaster)

하드코딩된 값을 사용하는 몬스터는 `getMove()` 이후 `setMove()`를 다시 호출해야 합니다.

**실제 ascension-100 Level 23 구현** (`Level23.java:106-145`):
```java
@SpirePatch(clz = Taskmaster.class, method = "getMove")
public static class TaskmasterIntentFix {
    @SpirePostfixPatch
    public static void Postfix(Taskmaster __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 23) {
            try {
                Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
                nextMoveField.setAccessible(true);
                byte move = nextMoveField.getByte(__instance);

                if (move == 2) {  // SIPHON_SOUL move
                    int updatedDamage = __instance.damage.get(1).base;
                    __instance.setMove((byte)2, AbstractMonster.Intent.ATTACK_DEBUFF, updatedDamage);

                    logger.info(String.format(
                        "Ascension 23: Taskmaster SIPHON_SOUL Intent updated from hardcoded 7 to %d",
                        updatedDamage
                    ));
                }
            } catch (Exception e) {
                logger.error("Ascension 23: Failed to update Taskmaster Intent", e);
            }
        }
    }
}
```

### 패턴 3: Reflection으로 특수 필드 수정 (GiantHead, Nemesis)

일부 몬스터는 별도의 필드에 데미지 값을 저장하고 `setMove()`에서 사용합니다.

**GiantHead 예시** (Level 35, `Level35.java:77-113`):
```java
// GiantHead는 COUNT_DMG 상수를 setMove()에서 사용
@SpirePatch(clz = GiantHead.class, method = SpirePatch.CONSTRUCTOR)
public static class GiantHeadDamageIncrease {
    @SpirePostfixPatch
    public static void Postfix(GiantHead __instance) {
        if (AbstractDungeon.ascensionLevel >= 35) {
            try {
                // COUNT_DMG 필드 직접 수정
                Field countDmgField = GiantHead.class.getDeclaredField("COUNT_DMG");
                countDmgField.setAccessible(true);
                int currentCountDmg = countDmgField.getInt(__instance);
                int newCountDmg = currentCountDmg + 1;
                countDmgField.setInt(__instance, newCountDmg);

                logger.info(String.format(
                    "Ascension 35: GiantHead COUNT_DMG increased from %d to %d",
                    currentCountDmg, newCountDmg
                ));
            } catch (Exception e) {
                logger.error("Failed to modify GiantHead COUNT_DMG", e);
            }
        }
    }
}
```

**Nemesis 예시** (Level 54, `Level54.java:68-102`):
```java
// Nemesis는 fireDmg 필드를 Burns attack에 사용
@SpirePatch(clz = Nemesis.class, method = SpirePatch.CONSTRUCTOR)
public static class NemesisDamageIncrease {
    @SpirePostfixPatch
    public static void Postfix(Nemesis __instance) {
        if (AbstractDungeon.ascensionLevel >= 54) {
            try {
                // fireDmg 필드 수정
                Field fireDmgField = Nemesis.class.getDeclaredField("fireDmg");
                fireDmgField.setAccessible(true);
                int currentFireDmg = fireDmgField.getInt(__instance);
                int newFireDmg = currentFireDmg + 2;
                fireDmgField.setInt(__instance, newFireDmg);

                logger.info(String.format(
                    "Ascension 54: Nemesis fireDmg increased from %d to %d",
                    currentFireDmg, newFireDmg
                ));
            } catch (Exception e) {
                logger.error("Failed to modify Nemesis fireDmg", e);
            }
        }
    }
}
```

### 하드코딩 몬스터 전체 리스트

| 몬스터 | 클래스 | 특수 필드 | 패턴 | 구현 위치 |
|--------|--------|-----------|------|----------|
| Taskmaster | `com.megacrit.cardcrawl.monsters.city.Taskmaster` | 없음 (하드코딩 7) | `getMove()` Postfix | Level23.java:106 |
| GiantHead | `com.megacrit.cardcrawl.monsters.beyond.GiantHead` | `COUNT_DMG` | Reflection (생성자) | Level35.java:77 |
| Nemesis | `com.megacrit.cardcrawl.monsters.beyond.Nemesis` | `fireDmg` | Reflection (생성자) | Level54.java:68 |

---

## 실전 예제

### 예제 1: ascension-100 Level 23-24

```java
package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterDamagePatch {
    private static final Logger logger = LogManager.getLogger(MonsterDamagePatch.class.getName());

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class DamageIncreasePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            float damageMultiplier = getDamageMultiplier(__instance);

            if (damageMultiplier > 1.0f) {
                for (DamageInfo dmg : __instance.damage) {
                    if (dmg != null && dmg.base > 0) {
                        int originalDamage = dmg.base;
                        dmg.base = MathUtils.ceil(dmg.base * damageMultiplier);

                        logger.info(String.format(
                            "Ascension %d: %s (%s) damage increased from %d to %d (x%.2f)",
                            AbstractDungeon.ascensionLevel,
                            __instance.name,
                            __instance.type,
                            originalDamage,
                            dmg.base,
                            damageMultiplier
                        ));
                    }
                }
            }
        }
    }

    private static float getDamageMultiplier(AbstractMonster monster) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // Ascension 23: Elite enemies deal 10% more damage
        if (level >= 23 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;
        }

        // Ascension 24: Normal enemies deal 10% more damage
        if (level >= 24 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;
        }

        return multiplier;
    }
}
```

### 예제 2: Level 25 Cultist 개별 조정

```java
import com.megacrit.cardcrawl.monsters.exordium.Cultist;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistWeakenPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Cultist 공격력 -2 (Ritual 강화와 균형)
            if (!__instance.damage.isEmpty()) {
                DamageInfo dmg = __instance.damage.get(0);
                if (dmg != null && dmg.base > 0) {
                    dmg.base = Math.max(1, dmg.base - 2);
                    logger.info("Cultist damage reduced by 2");
                }
            }
        }
    }
}
```

### 예제 3: 보스 특수 공격 강화 (Bronze Automaton)

```java
import com.megacrit.cardcrawl.monsters.city.BronzeAutomaton;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.BronzeAutomaton",
    method = "usePreBattleAction"
)
public static class AutomatonDamageBoost {
    @SpirePostfixPatch
    public static void Postfix(BronzeAutomaton __instance) {
        if (AbstractDungeon.ascensionLevel >= 26) {
            // 모든 공격 +2
            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base += 2;
                }
            }
            logger.info("Bronze Automaton damage increased by 2");
        }
    }
}
```

---

## 💡 중요 팁

### 1. Null 체크 필수

```java
// ❌ 위험
for (DamageInfo dmg : __instance.damage) {
    dmg.base += 2;  // NullPointerException 가능
}

// ✅ 안전
for (DamageInfo dmg : __instance.damage) {
    if (dmg != null && dmg.base > 0) {
        dmg.base += 2;
    }
}
```

### 2. base vs output

```java
// ✅ 올바른 예: base 수정
dmg.base += 2;  // 기본 데미지 수정 (Strength 등 파워 적용 전)

// ❌ 잘못된 예: output 수정
dmg.output += 2;  // 계산된 값 수정 (의미 없음, 재계산됨)
```

### 3. 최소값 보장

```java
// 공격력이 0 이하가 되지 않도록
dmg.base = Math.max(1, dmg.base - 2);  // 최소 1
```

### 4. 배율 먼저, 고정값 나중

```java
// ✅ 올바른 순서
dmg.base = MathUtils.ceil(dmg.base * 1.1f);  // 배율 먼저
dmg.base += 2;                                // 고정값 나중

// 예: 10 × 1.1 = 11, 11 + 2 = 13
```

### 5. damage 배열이 비어있을 수 있음

```java
// ❌ 위험
__instance.damage.get(0).base += 2;  // IndexOutOfBoundsException 가능

// ✅ 안전
if (!__instance.damage.isEmpty()) {
    DamageInfo dmg = __instance.damage.get(0);
    if (dmg != null) {
        dmg.base += 2;
    }
}
```

### 6. 올림 처리

```java
// MathUtils.ceil() 사용 (올림)
dmg.base = MathUtils.ceil(dmg.base * 1.15f);

// 예: 7 × 1.15 = 8.05 → 9
```

### 7. 로그 추가

```java
logger.info(String.format(
    "Ascension %d: %s damage %d → %d",
    AbstractDungeon.ascensionLevel,
    __instance.name,
    originalDamage,
    dmg.base
));
```

### 8. Intent 정확도 필요 시 패치 지점 변경

```java
// usePreBattleAction: 실제 데미지만 수정 (Intent 불일치 가능)
@SpirePatch(cls = "AbstractMonster", method = "usePreBattleAction")

// init() Prefix: Intent까지 정확히 수정 (권장)
@SpirePatch(cls = "AbstractMonster", method = "init")
public static class InitDamagePatch {
    @SpirePrefixPatch
    // ...
}

// 하드코딩 몬스터: getMove() Postfix + setMove() 재호출
@SpirePatch(cls = "Taskmaster", method = "getMove")
public static class TaskmasterIntentFix {
    @SpirePostfixPatch
    // ...
}
```

---

## 🔍 디버깅 팁

### 공격력이 적용 안 될 때

```java
// 1. damage 배열 확인
logger.info("Damage array size: " + __instance.damage.size());
for (int i = 0; i < __instance.damage.size(); i++) {
    DamageInfo dmg = __instance.damage.get(i);
    logger.info("  [" + i + "] base: " + (dmg != null ? dmg.base : "null"));
}

// 2. 타입 확인
logger.info("Monster type: " + __instance.type);
logger.info("Is Elite? " + (__instance.type == AbstractMonster.EnemyType.ELITE));

// 3. Ascension 확인
logger.info("Ascension Mode: " + AbstractDungeon.isAscensionMode);
logger.info("Ascension Level: " + AbstractDungeon.ascensionLevel);
```

### Intent가 업데이트 안 될 때

```java
// 1. 패치 타이밍 확인
logger.info("=== Patch Timing Debug ===");
logger.info("Monster: " + __instance.name);
logger.info("Current damage.base: " + __instance.damage.get(0).base);

// 2. move.baseDamage 확인 (Reflection 필요)
try {
    Field moveField = AbstractMonster.class.getDeclaredField("move");
    moveField.setAccessible(true);
    Object move = moveField.get(__instance);

    Field baseDmgField = move.getClass().getDeclaredField("baseDamage");
    baseDmgField.setAccessible(true);
    int moveDamage = baseDmgField.getInt(move);

    logger.info("move.baseDamage (Intent): " + moveDamage);
} catch (Exception e) {
    logger.error("Failed to check move.baseDamage", e);
}

// 3. 하드코딩 몬스터 체크
logger.info("Hardcoded monsters: Taskmaster, GiantHead, Nemesis");
logger.info("Need special handling - see Intent 수정이 필요한 경우 section");
```

---

## 📚 관련 문서

- [MONSTER_HEALTH.md](MONSTER_HEALTH.md) - 적 체력 수정
- [MONSTER_BEHAVIOR.md](MONSTER_BEHAVIOR.md) - 적 행동 패턴 수정
- [ENEMY_LIST.md](ENEMY_LIST.md) - 모든 적 목록 및 클래스명
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 공통 패턴 모음

---

**작성 기준**: ascension-100 모드 실제 구현 코드
**검증**: MonsterDamagePatch.java, Level23.java, Level35.java, Level54.java, Level58.java, Level62.java
**참고 분석**: .claude/analysis/damage-patch-intent-issue.md
