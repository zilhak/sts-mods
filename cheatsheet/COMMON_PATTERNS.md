# 자주 사용되는 패턴 모음

Slay the Spire 모딩에서 자주 사용되는 코드 패턴과 유틸리티를 정리한 문서입니다.

## 📑 목차

1. [조건부 적용](#조건부-적용)
2. [능력치 수정](#능력치-수정)
3. [버프/디버프](#버프디버프)
4. [카드 추가](#카드-추가)
5. [유틸리티 함수](#유틸리티-함수)

---

## 조건부 적용

### Ascension 레벨별 적용

```java
// 패턴 1: 단일 레벨
if (AbstractDungeon.ascensionLevel == 25) {
    // Level 25만
}

// 패턴 2: 레벨 이상
if (AbstractDungeon.ascensionLevel >= 21) {
    // Level 21 이상
}

// 패턴 3: 범위
if (AbstractDungeon.ascensionLevel >= 21 && AbstractDungeon.ascensionLevel < 30) {
    // Level 21~29
}

// 패턴 4: 누적 효과
int level = AbstractDungeon.ascensionLevel;
float multiplier = 1.0f;

if (level >= 21) multiplier *= 1.1f;   // +10%
if (level >= 32) multiplier *= 1.1f;   // +10% (총 21%)
if (level >= 51) multiplier *= 1.2f;   // +20% (총 45.2%)
```

### Act별 차등 적용

```java
int actNum = AbstractDungeon.actNum;

// 패턴 1: if-else
if (actNum == 1) {
    // 1막
} else if (actNum == 2) {
    // 2막
} else if (actNum >= 3) {
    // 3막 이상
}

// 패턴 2: switch
switch (actNum) {
    case 1:
        // 1막
        break;
    case 2:
        // 2막
        break;
    default:
        // 3막 이상
        break;
}

// 패턴 3: 배율 적용
float multiplier = 1.0f;
if (actNum == 1) multiplier = 1.1f;
else if (actNum == 2) multiplier = 1.2f;
else multiplier = 1.3f;
```

### 적 타입별 적용

```java
import com.megacrit.cardcrawl.monsters.AbstractMonster;

// 패턴 1: if-else
if (__instance.type == AbstractMonster.EnemyType.NORMAL) {
    // 일반 적
} else if (__instance.type == AbstractMonster.EnemyType.ELITE) {
    // 엘리트
} else if (__instance.type == AbstractMonster.EnemyType.BOSS) {
    // 보스
}

// 패턴 2: switch
switch (__instance.type) {
    case NORMAL:
        // 일반 적
        break;
    case ELITE:
        // 엘리트
        break;
    case BOSS:
        // 보스
        break;
}

// 패턴 3: 조합
if (AbstractDungeon.ascensionLevel >= 21 &&
    __instance.type == AbstractMonster.EnemyType.ELITE) {
    // Level 21 이상의 엘리트만
}
```

### 복합 조건

```java
// Ascension 모드 확인
if (!AbstractDungeon.isAscensionMode) {
    return;  // Ascension 아니면 종료
}

int level = AbstractDungeon.ascensionLevel;
int act = AbstractDungeon.actNum;

// 복합 조건 1: 레벨 + 타입
if (level >= 69 && __instance.type == AbstractMonster.EnemyType.BOSS) {
    // Level 69 이상의 보스
    if (act == 1) {
        // 1막 보스
    } else if (act == 2) {
        // 2막 보스
    }
}

// 복합 조건 2: 레벨 + 막 + 타입
if (level >= 70 &&
    act >= 3 &&
    __instance.type == AbstractMonster.EnemyType.ELITE) {
    // Level 70, 3막, 엘리트
}
```

---

## 능력치 수정

### 체력 수정

```java
import com.badlogic.gdx.math.MathUtils;

// 패턴 1: 고정값 추가
__instance.maxHealth += 10;
__instance.currentHealth += 10;

// 패턴 2: 배율 적용 (올림)
float multiplier = 1.2f;  // 20% 증가
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
__instance.currentHealth = __instance.maxHealth;

// 패턴 3: 배율 적용 (내림)
__instance.maxHealth = (int)(__instance.maxHealth * 0.9f);  // 10% 감소
__instance.currentHealth = Math.min(__instance.currentHealth, __instance.maxHealth);

// 패턴 4: 최소값 보장
__instance.maxHealth = Math.max(1, __instance.maxHealth - 50);
__instance.currentHealth = __instance.maxHealth;

// 패턴 5: 로그 포함
int originalHP = __instance.maxHealth;
__instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
__instance.currentHealth = __instance.maxHealth;

logger.info(String.format(
    "Ascension %d: %s HP %d → %d (x%.2f)",
    AbstractDungeon.ascensionLevel,
    __instance.name,
    originalHP,
    __instance.maxHealth,
    multiplier
));
```

### 공격력 수정

```java
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.badlogic.gdx.math.MathUtils;

// 패턴 1: 모든 공격력 고정값 추가
for (DamageInfo dmg : __instance.damage) {
    if (dmg != null && dmg.base > 0) {
        dmg.base += 2;
    }
}

// 패턴 2: 모든 공격력 배율 적용
float multiplier = 1.1f;
for (DamageInfo dmg : __instance.damage) {
    if (dmg != null && dmg.base > 0) {
        dmg.base = MathUtils.ceil(dmg.base * multiplier);
    }
}

// 패턴 3: 첫 번째 공격만 수정
if (!__instance.damage.isEmpty()) {
    DamageInfo dmg = __instance.damage.get(0);
    if (dmg != null && dmg.base > 0) {
        dmg.base += 2;
    }
}

// 패턴 4: 배율 + 고정값
for (DamageInfo dmg : __instance.damage) {
    if (dmg != null && dmg.base > 0) {
        dmg.base = MathUtils.ceil(dmg.base * 1.1f) + 2;
    }
}

// 패턴 5: 최소값 보장
for (DamageInfo dmg : __instance.damage) {
    if (dmg != null && dmg.base > 0) {
        dmg.base = Math.max(1, dmg.base - 2);  // 최소 1
    }
}
```

### 방어도 수정

```java
import com.megacrit.cardcrawl.actions.common.GainBlockAction;

// 패턴 1: 방어도 부여
AbstractDungeon.actionManager.addToBottom(
    new GainBlockAction(__instance, __instance, 12)
);

// 패턴 2: 조건부 방어도
if (__instance.nextMove == 2) {  // 특정 행동 ID
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(__instance, __instance, 15)
    );
}
```

---

## 버프/디버프

### 전투 시작 시 버프

```java
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.powers.*;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

// 패턴 1: Strength (힘)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(
        __instance,                           // 대상
        __instance,                           // 소스
        new StrengthPower(__instance, 3),    // 파워
        3                                     // 양
    )
);

// 패턴 2: Metallicize (금속화)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new MetallicizePower(__instance, 4), 4)
);

// 패턴 3: Regeneration (재생)
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;

AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new RegenerateMonsterPower(__instance, 5), 5)
);

// 패턴 4: Artifact (인공물)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new ArtifactPower(__instance, 1), 1)
);

// 패턴 5: Intangible (불가침)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new IntangiblePower(__instance, 2), 2)
);

// 패턴 6: Plated Armor (판금 갑옷)
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new PlatedArmorPower(__instance, 3), 3)
);
```

### 랜덤 버프

```java
import com.badlogic.gdx.math.MathUtils;

// 패턴 1: 확률 체크
if (MathUtils.randomBoolean(0.15f)) {  // 15% 확률
    // 버프 부여
}

// 패턴 2: 랜덤 선택
int randomBuff = MathUtils.random(2);  // 0, 1, 2

switch (randomBuff) {
    case 0:
        // Strength
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new StrengthPower(__instance, 2), 2)
        );
        break;
    case 1:
        // Metallicize
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new MetallicizePower(__instance, 2), 2)
        );
        break;
    case 2:
        // Regeneration
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(__instance, __instance,
                new RegenerateMonsterPower(__instance, 4), 4)
        );
        break;
}

// 패턴 3: 막별 랜덤 버프
int actNum = AbstractDungeon.actNum;
int randomBuff = MathUtils.random(2);

if (actNum == 1) {
    // 1막: 약한 버프
    switch (randomBuff) {
        case 0: applyStrength(__instance, 2); break;
        case 1: applyMetallicize(__instance, 2); break;
        case 2: applyRegeneration(__instance, 4); break;
    }
} else if (actNum == 2) {
    // 2막: 중간 버프
    switch (randomBuff) {
        case 0: applyStrength(__instance, 3); break;
        case 1: applyMetallicize(__instance, 5); break;
        case 2: applyRegeneration(__instance, 8); break;
    }
} else {
    // 3막: 강한 버프
    switch (randomBuff) {
        case 0: applyStrength(__instance, 6); break;
        case 1: applyMetallicize(__instance, 8); break;
        case 2: applyRegeneration(__instance, 15); break;
    }
}
```

### 특수 파워

```java
// CurlUp (Louse)
import com.megacrit.cardcrawl.powers.CurlUpPower;

AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new CurlUpPower(__instance, 3), 3)
);

// Ritual (Cultist)
import com.megacrit.cardcrawl.powers.RitualPower;

AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new RitualPower(__instance, 5, false), 5)
);

// SporeCloud (Fungi Beast)
import com.megacrit.cardcrawl.powers.SporeCloudPower;

AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new SporeCloudPower(__instance, 1), 1)
);

// Malleable (Snake Plant)
import com.megacrit.cardcrawl.powers.MalleablePower;

AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(__instance, __instance,
        new MalleablePower(__instance, 1), 1)
);
```

---

## 카드 추가

### 상태이상 카드 추가

```java
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.status.*;

// Slimed
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAction(new Slimed(), 2)  // 2장 추가
);

// Dazed
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAction(new Dazed(), 1)
);

// Wound
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAction(new Wound(), 1)
);

// Burn
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAction(new Burn(), 3)
);

// Void
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDiscardAction(new VoidCard(), 1)
);
```

### 저주 카드 추가

```java
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.curses.*;

// Curse cards go to draw pile, not discard
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDrawPileAction(new Clumsy(), 1, true, true)
    // (카드, 개수, randomSpot, autoPosition)
);

// Pain
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDrawPileAction(new Pain(), 1, true, true)
);

// Injury
AbstractDungeon.actionManager.addToBottom(
    new MakeTempCardInDrawPileAction(new Injury(), 1, true, true)
);
```

---

## 유틸리티 함수

### 공통 유틸리티 클래스

```java
package com.yourmod.utils;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.powers.*;
import org.apache.logging.log4j.Logger;

public class ModUtils {

    // Ascension 레벨 체크
    public static boolean shouldApplyPatch(int minLevel) {
        return AbstractDungeon.isAscensionMode &&
               AbstractDungeon.ascensionLevel >= minLevel;
    }

    // 체력 배율 적용
    public static void applyHPMultiplier(AbstractMonster monster, float multiplier) {
        int original = monster.maxHealth;
        monster.maxHealth = MathUtils.ceil(monster.maxHealth * multiplier);
        monster.currentHealth = monster.maxHealth;
    }

    // 체력 고정값 추가
    public static void addHP(AbstractMonster monster, int amount) {
        monster.maxHealth += amount;
        monster.currentHealth += amount;
    }

    // 공격력 배율 적용
    public static void applyDamageMultiplier(AbstractMonster monster, float multiplier) {
        for (DamageInfo dmg : monster.damage) {
            if (dmg != null && dmg.base > 0) {
                dmg.base = MathUtils.ceil(dmg.base * multiplier);
            }
        }
    }

    // 공격력 고정값 추가
    public static void addDamage(AbstractMonster monster, int amount) {
        for (DamageInfo dmg : monster.damage) {
            if (dmg != null && dmg.base > 0) {
                dmg.base += amount;
            }
        }
    }

    // Strength 부여
    public static void applyStrength(AbstractMonster monster, int amount) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(monster, monster,
                new StrengthPower(monster, amount), amount)
        );
    }

    // Metallicize 부여
    public static void applyMetallicize(AbstractMonster monster, int amount) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(monster, monster,
                new MetallicizePower(monster, amount), amount)
        );
    }

    // Regeneration 부여
    public static void applyRegeneration(AbstractMonster monster, int amount) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(monster, monster,
                new RegenerateMonsterPower(monster, amount), amount)
        );
    }

    // Artifact 부여
    public static void applyArtifact(AbstractMonster monster, int amount) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(monster, monster,
                new ArtifactPower(monster, amount), amount)
        );
    }

    // 로그 유틸리티
    public static void logHPChange(Logger logger, AbstractMonster monster,
                                    int originalHP, int newHP, float multiplier) {
        logger.info(String.format(
            "Ascension %d: %s HP %d → %d (x%.2f)",
            AbstractDungeon.ascensionLevel,
            monster.name,
            originalHP,
            newHP,
            multiplier
        ));
    }

    public static void logDamageChange(Logger logger, AbstractMonster monster,
                                       int originalDamage, int newDamage, int increase) {
        logger.info(String.format(
            "Ascension %d: %s damage %d → %d (+%d)",
            AbstractDungeon.ascensionLevel,
            monster.name,
            originalDamage,
            newDamage,
            increase
        ));
    }
}
```

### 사용 예제

```java
import com.yourmod.utils.ModUtils;

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class SimplePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 깔끔한 코드
        if (ModUtils.shouldApplyPatch(21)) {
            ModUtils.applyHPMultiplier(__instance, 1.2f);
        }

        if (ModUtils.shouldApplyPatch(52)) {
            ModUtils.addDamage(__instance, 1);
        }
    }
}

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class BuffPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (ModUtils.shouldApplyPatch(70)) {
            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                int act = AbstractDungeon.actNum;
                if (act == 1) {
                    ModUtils.applyMetallicize(__instance, 4);
                } else if (act == 2) {
                    ModUtils.applyStrength(__instance, 2);
                }
            }
        }
    }
}
```

---

## 📚 관련 문서

- [INDEX.md](INDEX.md) - 전체 가이드
- [PATCH_BASICS.md](PATCH_BASICS.md) - 패치 기본 개념
- [ENEMY_MODIFY.md](ENEMY_MODIFY.md) - 적 수정 실전 예제

---

**참고**: 이 패턴들은 ascension-100 모드에서 실제로 사용되고 검증된 코드입니다.
