# 적 체력 수정 가이드

적의 체력(HP)을 수정하는 모든 방법을 상세히 설명합니다.

## 📑 목차

1. [기본 체력 수정](#기본-체력-수정)
2. [타입별 체력 수정](#타입별-체력-수정)
3. [막별 체력 수정](#막별-체력-수정)
4. [복합 체력 수정](#복합-체력-수정)
5. [특정 적 체력 수정](#특정-적-체력-수정)
6. [실전 예제](#실전-예제)

---

## 기본 체력 수정

### 패치 시점: `init()` 메서드

적의 체력은 **`init()` 메서드**에서 설정됩니다. 이 메서드를 패치하여 체력을 수정합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class HealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 체력 수정 코드
    }
}
```

### 패턴 1: 배율 적용 (권장)

모든 적에게 일정 비율로 체력 증가:

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class HealthMultiplierPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (!AbstractDungeon.isAscensionMode) {
            return;
        }

        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // 레벨 21: 모든 적 체력 20% 증가
        if (level >= 21) {
            multiplier = 1.2f;
        }

        if (multiplier > 1.0f) {
            int originalHP = __instance.maxHealth;

            // 체력 적용 (올림)
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;

            logger.info(String.format(
                "Ascension %d: %s HP %d → %d (x%.2f)",
                level, __instance.name, originalHP, __instance.maxHealth, multiplier
            ));
        }
    }
}
```

**중요**:
- `MathUtils.ceil()`: 올림 처리 (체력 소수점 방지)
- `currentHealth`도 함께 업데이트 필수
- `maxHealth` 먼저 변경 후 `currentHealth` 설정

### 패턴 2: 고정값 추가

모든 적에게 일정 HP 추가:

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class HealthBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            int bonus = 15;  // 모든 적 HP +15

            __instance.maxHealth += bonus;
            __instance.currentHealth += bonus;
        }
    }
}
```

### 패턴 3: 누적 배율 (여러 레벨)

여러 Ascension 레벨에서 누적 증가:

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class CumulativeHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // 누적 적용
        if (level >= 21) multiplier *= 1.1f;   // 10% 증가
        if (level >= 32) multiplier *= 1.1f;   // 추가 10% (총 21%)
        if (level >= 51) multiplier *= 1.2f;   // 추가 20% (총 45.2%)

        if (multiplier > 1.0f) {
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}
```

**계산 예시**:
- Level 21: 1.1 (10% 증가)
- Level 32: 1.1 × 1.1 = 1.21 (21% 증가)
- Level 51: 1.1 × 1.1 × 1.2 = 1.452 (45.2% 증가)

---

## 타입별 체력 수정

### 적 타입 구분

```java
public enum EnemyType {
    NORMAL,  // 일반 적
    ELITE,   // 엘리트
    BOSS     // 보스
}
```

### 패턴 1: 엘리트만 증가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class EliteHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 21 &&
            __instance.type == AbstractMonster.EnemyType.ELITE) {

            // 엘리트만 체력 10% 증가
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}
```

### 패턴 2: 타입별 다른 배율

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class TypeBasedHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            float multiplier = 1.0f;

            switch (__instance.type) {
                case NORMAL:
                    multiplier = 1.1f;  // 일반: 10% 증가
                    break;
                case ELITE:
                    multiplier = 1.2f;  // 엘리트: 20% 증가
                    break;
                case BOSS:
                    multiplier = 1.15f; // 보스: 15% 증가
                    break;
            }

            if (multiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
                __instance.currentHealth = __instance.maxHealth;
            }
        }
    }
}
```

### 패턴 3: 타입별 고정값

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class TypeBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 59) {
            int bonus = 0;

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                bonus = 10;  // 엘리트만 HP +10
            }

            if (bonus > 0) {
                __instance.maxHealth += bonus;
                __instance.currentHealth += bonus;
            }
        }
    }
}
```

---

## 막별 체력 수정

### 패턴 1: 막별 다른 배율

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class ActBasedHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            int act = AbstractDungeon.actNum;
            float multiplier = 1.0f;

            switch (act) {
                case 1:
                    multiplier = 1.1f;  // 1막: 10% 증가
                    break;
                case 2:
                    multiplier = 1.2f;  // 2막: 20% 증가
                    break;
                case 3:
                    multiplier = 1.3f;  // 3막: 30% 증가
                    break;
                default:
                    multiplier = 1.4f;  // 4막: 40% 증가
                    break;
            }

            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}
```

### 패턴 2: 보스 막별 강화 (Level 69 예시)

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class BossActHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 69 &&
            __instance.type == AbstractMonster.EnemyType.BOSS) {

            int act = AbstractDungeon.actNum;
            float multiplier = 1.0f;

            if (act == 1) {
                multiplier = 1.1f;  // 1막 보스: 10%
            } else if (act == 2) {
                multiplier = 1.2f;  // 2막 보스: 20%
            } else if (act >= 3) {
                multiplier = 1.3f;  // 3막+ 보스: 30%
            }

            if (multiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
                __instance.currentHealth = __instance.maxHealth;
            }
        }
    }
}
```

---

## 복합 체력 수정

### 패턴: 레벨 + 타입 + 막 복합

ascension-100 모드의 실제 구현 방식:

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class ComplexHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        float multiplier = getHPMultiplier(__instance);

        if (multiplier > 1.0f) {
            int originalHP = __instance.maxHealth;
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;

            logger.info(String.format(
                "Ascension %d: %s (%s, Act %d) HP %d → %d (x%.2f)",
                AbstractDungeon.ascensionLevel,
                __instance.name,
                __instance.type,
                AbstractDungeon.actNum,
                originalHP,
                __instance.maxHealth,
                multiplier
            ));
        }
    }

    private static float getHPMultiplier(AbstractMonster monster) {
        int level = AbstractDungeon.ascensionLevel;
        int act = AbstractDungeon.actNum;
        float multiplier = 1.0f;

        // 기본 레벨별 증가
        if (level >= 21 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;  // 엘리트 10%
        }
        if (level >= 22 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;  // 일반 10%
        }
        if (level >= 32 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;  // 엘리트 추가 10%
        }
        if (level >= 33 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;  // 일반 추가 10%
        }

        // 전체 대폭 증가
        if (level >= 51) {
            multiplier *= 1.2f;  // 모든 적 20%
        }
        if (level >= 57) {
            multiplier *= 1.1f;  // 모든 적 10%
        }
        if (level >= 61) {
            multiplier *= 1.1f;  // 모든 적 10%
        }

        // 보스 추가 증가
        if (level >= 63 && monster.type == AbstractMonster.EnemyType.BOSS) {
            multiplier *= 1.2f;  // 보스 20%
        }

        if (level >= 67) {
            multiplier *= 1.15f; // 모든 적 15%
        }

        // 보스 막별 추가 증가
        if (level >= 69 && monster.type == AbstractMonster.EnemyType.BOSS) {
            if (act == 1) {
                multiplier *= 1.1f;
            } else if (act == 2) {
                multiplier *= 1.2f;
            } else if (act >= 3) {
                multiplier *= 1.3f;
            }
        }

        return multiplier;
    }
}
```

**계산 예시 (Level 70, 3막 보스)**:
```
1.0 (기본)
× 1.2 (Level 51)
× 1.1 (Level 57)
× 1.1 (Level 61)
× 1.2 (Level 63 보스)
× 1.15 (Level 67)
× 1.3 (Level 69, 3막 보스)
= 약 2.39 (139% 증가)
```

---

## 특정 적 체력 수정

### 패턴 1: 생성자 패치

```java
import com.megacrit.cardcrawl.monsters.exordium.Cultist;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Cultist만 HP +20
            __instance.maxHealth += 20;
            __instance.currentHealth += 20;
        }
    }
}
```

### 패턴 2: 이름으로 구분

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class SpecificMonsterPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 이름으로 구분
            if (__instance.name.equals("Cultist")) {
                __instance.maxHealth += 10;
                __instance.currentHealth += 10;
            } else if (__instance.name.equals("Jaw Worm")) {
                __instance.maxHealth += 15;
                __instance.currentHealth += 15;
            }
        }
    }
}
```

### 패턴 3: ID로 구분

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class MonsterIDPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // ID로 구분 (더 정확)
            if (__instance.id.equals("Cultist")) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.2f);
                __instance.currentHealth = __instance.maxHealth;
            }
        }
    }
}
```

---

## 실전 예제

### 예제 1: ascension-100 Level 21-22

```java
package com.stsmod.ascension100.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterHealthPatch {
    private static final Logger logger = LogManager.getLogger(MonsterHealthPatch.class.getName());

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class HealthIncreasePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (!AbstractDungeon.isAscensionMode) {
                return;
            }

            float hpMultiplier = getHPMultiplier(__instance);

            if (hpMultiplier > 1.0f) {
                int originalMaxHP = __instance.maxHealth;

                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * hpMultiplier);
                __instance.currentHealth = MathUtils.ceil(__instance.currentHealth * hpMultiplier);

                logger.info(String.format(
                    "Ascension %d: %s (%s) HP increased from %d to %d (x%.2f)",
                    AbstractDungeon.ascensionLevel,
                    __instance.name,
                    __instance.type,
                    originalMaxHP,
                    __instance.maxHealth,
                    hpMultiplier
                ));
            }
        }
    }

    private static float getHPMultiplier(AbstractMonster monster) {
        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // Ascension 21: Elite enemies +10% HP
        if (level >= 21 && monster.type == AbstractMonster.EnemyType.ELITE) {
            multiplier *= 1.1f;
        }

        // Ascension 22: Normal enemies +10% HP
        if (level >= 22 && monster.type == AbstractMonster.EnemyType.NORMAL) {
            multiplier *= 1.1f;
        }

        return multiplier;
    }
}
```

### 예제 2: 특정 적 개별 조정 (Level 25)

```java
import com.megacrit.cardcrawl.monsters.beyond.Darkling;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = SpirePatch.CONSTRUCTOR
)
public static class DarklingHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(Darkling __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Darkling HP +25
            __instance.maxHealth += 25;
            __instance.currentHealth += 25;
        }
    }
}
```

---

## 💡 중요 팁

### 1. currentHealth 업데이트 필수

```java
// ❌ 잘못된 예
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.2f);
// currentHealth 업데이트 누락!

// ✅ 올바른 예
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.2f);
__instance.currentHealth = __instance.maxHealth;
```

### 2. 배율 계산 순서

```java
// ❌ 잘못된 예: 중복 적용
if (level >= 21) {
    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
}
if (level >= 32) {
    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f); // 21 적용된 HP에 또 적용
}

// ✅ 올바른 예: 배율 먼저 계산
float multiplier = 1.0f;
if (level >= 21) multiplier *= 1.1f;
if (level >= 32) multiplier *= 1.1f;
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
```

### 3. 올림 처리

```java
// MathUtils.ceil() 사용 (올림)
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.15f);

// 예: 50 HP × 1.15 = 57.5 → 58 HP
```

### 4. 로그 추가

```java
logger.info(String.format(
    "Ascension %d: %s HP %d → %d (x%.2f)",
    AbstractDungeon.ascensionLevel,
    __instance.name,
    originalHP,
    __instance.maxHealth,
    multiplier
));
```

### 5. Ascension 모드 확인

```java
if (!AbstractDungeon.isAscensionMode) {
    return;  // Ascension 모드 아니면 적용 안 함
}
```

### 6. 레벨 범위 주의

```java
// ❌ 잘못된 예
if (level >= 21 && level < 32) multiplier *= 1.1f;  // 21~31만
if (level >= 32) multiplier *= 1.1f;                // 32 이상만

// ✅ 올바른 예 (누적)
if (level >= 21) multiplier *= 1.1f;  // 21 이상
if (level >= 32) multiplier *= 1.1f;  // 32 이상 추가 (총 1.21배)
```

---

## 📚 관련 문서

- [ENEMY_LIST.md](ENEMY_LIST.md) - 모든 적 목록 및 클래스명
- [MONSTER_DAMAGE.md](MONSTER_DAMAGE.md) - 적 공격력 수정
- [ENEMY_MODIFY.md](ENEMY_MODIFY.md) - 적 수정 종합 가이드
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 공통 패턴 모음

---

**작성 기준**: ascension-100 모드 실제 구현 코드
**검증**: MonsterHealthPatch.java, Level31to39Patches.java, Level51to70Patches.java
