# SpirePatch 기본 가이드

ModTheSpire의 SpirePatch 시스템을 사용하여 게임을 수정하는 기본 개념을 설명합니다.

## 📑 목차

1. [패치란 무엇인가](#패치란-무엇인가)
2. [패치의 종류](#패치의-종류)
3. [패치 구조](#패치-구조)
4. [실전 예제](#실전-예제)
5. [디버깅](#디버깅)

---

## 패치란 무엇인가

**패치(Patch)**는 게임의 원본 코드를 수정하지 않고, 특정 클래스의 메서드에 코드를 추가/변경하는 기법입니다.

### 왜 패치를 사용하나요?

1. **원본 보존**: 게임 파일을 직접 수정하지 않음
2. **호환성**: 다른 모드와 함께 사용 가능
3. **유지보수**: 게임 업데이트 시 수정 최소화
4. **유연성**: 필요한 부분만 선택적 수정

### ModTheSpire 작동 원리

```
게임 실행 → ModTheSpire 로딩 → 패치 적용 → 수정된 게임 실행
```

---

## 패치의 종류

### 1. Postfix (가장 많이 사용)

**원본 메서드 실행 후** 추가 코드를 실행합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class HealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 원본 init() 실행 후 이 코드 실행
        __instance.maxHealth += 10;
    }
}
```

**언제 사용?**
- 능력치 증가/감소
- 버프/디버프 추가
- 추가 행동 실행
- 대부분의 경우 사용

### 2. Prefix

**원본 메서드 실행 전** 코드를 실행합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "takeTurn"
)
public static class PreTurnPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractMonster __instance) {
        // 원본 takeTurn() 실행 전 이 코드 실행
        logger.info(__instance.name + " is about to take turn");
    }
}
```

**언제 사용?**
- 로깅/디버깅
- 조건 체크
- 사전 준비 작업

### 3. Insert

**원본 메서드 중간**에 코드를 삽입합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class InsertPatch {
    @SpireInsertPatch(rloc = 10)  // 10번째 라인에 삽입
    public static void Insert(AbstractMonster __instance) {
        // 특정 위치에 코드 삽입
    }
}
```

**언제 사용?**
- 특정 시점에 코드 실행 필요
- 고급 사용 사례
- 정확한 라인 번호 파악 필요

### 4. Replace / Return

**원본 메서드를 조기 종료**하거나 반환값을 변경합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
    method = "incrementAscensionLevel",
    paramtypez = {"int"}
)
public static class AscensionCapPatch {
    @SpireInsertPatch(rloc = 0)
    public static SpireReturn<?> Insert(CharacterOption __instance, @ByRef int[] level) {
        if (level[0] > 100) {
            level[0] = 100;  // Cap at 100
            return SpireReturn.Return(null);  // 원본 메서드 실행 중단
        }
        return SpireReturn.Continue();  // 원본 메서드 계속 실행
    }
}
```

**언제 사용?**
- 메서드 동작 완전 변경
- 조건부 메서드 스킵
- 반환값 조작

---

## 패치 구조

### 기본 구조

```java
@SpirePatch(
    cls = "패키지.경로.클래스명",      // 패치할 클래스 (필수)
    method = "메서드명",               // 패치할 메서드 (필수)
    paramtypez = { 파라미터타입들 }   // 메서드 파라미터 (선택)
)
public static class YourPatchClassName {
    @SpirePostfixPatch  // 또는 @SpirePrefixPatch, @SpireInsertPatch
    public static void Postfix(TargetClass __instance, 파라미터들) {
        // 패치 코드
    }
}
```

### 주요 요소

#### 1. `cls` - 패치할 클래스

```java
// 전체 패키지 경로 필요
cls = "com.megacrit.cardcrawl.monsters.AbstractMonster"

// import하지 않고 문자열로 지정
```

#### 2. `method` - 패치할 메서드

```java
// 일반 메서드
method = "takeTurn"
method = "usePreBattleAction"

// 생성자
method = SpirePatch.CONSTRUCTOR

// 정적 메서드
method = SpirePatch.STATICINITIALIZER
```

#### 3. `paramtypez` / `paramtypes` - 메서드 파라미터

**참고**: ModTheSpire는 `paramtypez`와 `paramtypes` 모두 지원합니다. 둘 다 사용 가능하지만 `paramtypez`를 권장합니다.

```java
// 파라미터 없음
method = "init"  // paramtypez 생략 가능

// 파라미터 있음 (둘 다 가능)
method = "damage",
paramtypez = { "com.megacrit.cardcrawl.cards.DamageInfo" }
// 또는
paramtypes = { "com.megacrit.cardcrawl.cards.DamageInfo" }

// 여러 파라미터
method = SpirePatch.CONSTRUCTOR,
paramtypez = { float.class, float.class, boolean.class }
```

#### 4. `__instance` - 현재 인스턴스

```java
@SpirePostfixPatch
public static void Postfix(AbstractMonster __instance) {
    // __instance는 현재 적 객체
    __instance.maxHealth += 10;
    __instance.name;
    // 등등
}
```

### Import 문

```java
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;

// 게임 클래스들
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.cards.DamageInfo;
// 등등
```

---

## 실전 예제

### 예제 1: 간단한 체력 증가

```java
package com.yourmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class SimpleHealthPatch {

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class IncreasePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            // 모든 적 체력 20% 증가
            if (AbstractDungeon.ascensionLevel >= 21) {
                int originalHP = __instance.maxHealth;
                __instance.maxHealth = (int) Math.ceil(__instance.maxHealth * 1.2);
                __instance.currentHealth = __instance.maxHealth;

                System.out.println(
                    __instance.name + " HP: " + originalHP + " → " + __instance.maxHealth
                );
            }
        }
    }
}
```

### 예제 2: 특정 적만 수정

```java
package com.yourmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class CultistPatch {

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = { float.class, float.class, boolean.class }
    )
    public static class CultistBuffPatch {
        @SpirePostfixPatch
        public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
            if (AbstractDungeon.ascensionLevel >= 25) {
                // Cultist 전용: HP +15
                __instance.maxHealth += 15;
                __instance.currentHealth += 15;
            }
        }
    }
}
```

### 예제 3: 버프 추가

```java
package com.yourmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class BuffPatch {

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "usePreBattleAction"
    )
    public static class StartingBuffPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (AbstractDungeon.ascensionLevel >= 50) {
                // 모든 엘리트가 Strength 3으로 시작
                if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(
                            __instance,      // 대상
                            __instance,      // 소스
                            new StrengthPower(__instance, 3),  // 파워
                            3                // 양
                        )
                    );
                }
            }
        }
    }
}
```

### 예제 4: 조건부 실행 (if문 활용)

```java
package com.yourmod.patches;

import com.badlogic.gdx.math.MathUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ConditionalPatch {

    @SpirePatch(
        cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
        method = "init"
    )
    public static class ComplexPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            int level = AbstractDungeon.ascensionLevel;
            int act = AbstractDungeon.actNum;
            float multiplier = 1.0f;

            // 레벨별 배율
            if (level >= 21) multiplier *= 1.1f;
            if (level >= 51) multiplier *= 1.2f;

            // 타입별 추가 배율
            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                multiplier *= 1.1f;
            } else if (__instance.type == AbstractMonster.EnemyType.BOSS) {
                multiplier *= 1.15f;
            }

            // 막별 추가 배율
            if (act == 3) {
                multiplier *= 1.1f;
            }

            // 적용
            if (multiplier > 1.0f) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
                __instance.currentHealth = __instance.maxHealth;
            }
        }
    }
}
```

---

## 디버깅

### 1. 로깅 추가

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YourPatch {
    private static final Logger logger = LogManager.getLogger(YourPatch.class.getName());

    @SpirePatch(...)
    public static class SomePatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            logger.info("Monster: " + __instance.name);
            logger.info("HP: " + __instance.maxHealth);
            logger.info("Type: " + __instance.type);
            logger.info("Ascension: " + AbstractDungeon.ascensionLevel);
        }
    }
}
```

로그는 다음 위치에 저장됩니다:
- Windows: `%LOCALAPPDATA%\ModTheSpire\logs\`
- Linux/Mac: `~/.config/ModTheSpire/logs/`

### 2. Null 체크

```java
@SpirePostfixPatch
public static void Postfix(AbstractMonster __instance) {
    // ❌ 위험
    __instance.damage.get(0).base += 2;

    // ✅ 안전
    if (__instance.damage != null && !__instance.damage.isEmpty()) {
        DamageInfo dmg = __instance.damage.get(0);
        if (dmg != null) {
            dmg.base += 2;
        }
    }
}
```

### 3. 조건 확인

```java
@SpirePostfixPatch
public static void Postfix(AbstractMonster __instance) {
    // Ascension 모드인지 확인
    if (!AbstractDungeon.isAscensionMode) {
        return;
    }

    // 특정 레벨 이상인지 확인
    if (AbstractDungeon.ascensionLevel < 21) {
        return;
    }

    // 실제 패치 코드
    __instance.maxHealth += 10;
}
```

### 4. 게임 크래시 시

1. **로그 확인**: `ModTheSpire/logs/` 폴더
2. **클래스명 확인**: 디컴파일 소스에서 정확한 이름 확인
3. **메서드 시그니처 확인**: 파라미터 타입과 개수
4. **Import 확인**: 모든 필요한 클래스 import했는지
5. **Null 체크**: 모든 객체 접근 전 null 체크

---

## 💡 베스트 프랙티스

### 1. 한 파일에 관련 패치 모으기

```java
// MonsterHealthPatch.java
public class MonsterHealthPatch {
    // Level 21 패치
    @SpirePatch(...)
    public static class Level21Patch { ... }

    // Level 22 패치
    @SpirePatch(...)
    public static class Level22Patch { ... }
}
```

### 2. 명확한 이름 사용

```java
// ❌ 나쁜 예
public static class Patch1 { }

// ✅ 좋은 예
public static class EliteHealthIncreasePatch { }
```

### 3. 주석 추가

```java
/**
 * Ascension 21: Elite enemies gain 10% more HP
 */
@SpirePatch(...)
public static class EliteHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 엘리트만 체력 증가
        if (__instance.type == AbstractMonster.EnemyType.ELITE) {
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
        }
    }
}
```

### 4. 공통 로직 분리

```java
public class PatchUtils {
    public static boolean shouldApplyPatch(int minLevel) {
        return AbstractDungeon.isAscensionMode &&
               AbstractDungeon.ascensionLevel >= minLevel;
    }

    public static void applyHPMultiplier(AbstractMonster monster, float multiplier) {
        monster.maxHealth = MathUtils.ceil(monster.maxHealth * multiplier);
        monster.currentHealth = monster.maxHealth;
    }
}

// 사용
@SpirePostfixPatch
public static void Postfix(AbstractMonster __instance) {
    if (PatchUtils.shouldApplyPatch(21)) {
        PatchUtils.applyHPMultiplier(__instance, 1.1f);
    }
}
```

---

## 📚 관련 문서

- [INDEX.md](INDEX.md) - 전체 가이드
- [ENEMY_MODIFY.md](ENEMY_MODIFY.md) - 적 수정 실전 예제
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 자주 쓰는 패턴

---

**참고**: 이 가이드는 ModTheSpire 3.29.3 기준입니다.
