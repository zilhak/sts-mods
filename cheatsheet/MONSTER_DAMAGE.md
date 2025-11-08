# 적 공격력 수정 가이드

적의 공격력(Damage)을 수정하는 모든 방법을 상세히 설명합니다.

## 📑 목차

1. [기본 공격력 수정](#기본-공격력-수정)
2. [타입별 공격력 수정](#타입별-공격력-수정)
3. [막별 공격력 수정](#막별-공격력-수정)
4. [복합 공격력 수정](#복합-공격력-수정)
5. [특정 적 공격력 수정](#특정-적-공격력-수정)
6. [실전 예제](#실전-예제)

---

## 기본 공격력 수정

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

### 8. usePreBattleAction vs Constructor

```java
// usePreBattleAction: 모든 적 일괄 수정
@SpirePatch(cls = "AbstractMonster", method = "usePreBattleAction")

// Constructor: 특정 적만 수정
@SpirePatch(cls = "Cultist", method = SpirePatch.CONSTRUCTOR, paramtypez = {...})
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

---

## 📚 관련 문서

- [MONSTER_HEALTH.md](MONSTER_HEALTH.md) - 적 체력 수정
- [MONSTER_BEHAVIOR.md](MONSTER_BEHAVIOR.md) - 적 행동 패턴 수정
- [ENEMY_LIST.md](ENEMY_LIST.md) - 모든 적 목록 및 클래스명
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 공통 패턴 모음

---

**작성 기준**: ascension-100 모드 실제 구현 코드
**검증**: MonsterDamagePatch.java, Level31to39Patches.java, Level51to70Patches.java
