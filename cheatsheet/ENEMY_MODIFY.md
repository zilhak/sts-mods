# 적(Enemy) 수정 가이드

적의 능력치, 행동 패턴, 버프/디버프를 수정하는 방법을 단계별로 설명합니다.

## 📑 목차

1. [기본 개념](#기본-개념)
2. [타입별 수정](#타입별-수정)
3. [능력치 수정](#능력치-수정)
4. [행동 패턴 수정](#행동-패턴-수정)
5. [버프/디버프 추가](#버프디버프-추가)
6. [실전 예제](#실전-예제)

---

## 기본 개념

### 적 수정의 3가지 타이밍

적을 수정할 수 있는 시점은 크게 3가지입니다:

| 타이밍 | 메서드 | 용도 | 예시 |
|--------|--------|------|------|
| **생성 시** | Constructor | 기본 능력치 설정 | 체력, 공격력 초기화 |
| **초기화 시** | `init()` | 능력치 최종 조정 | 체력 배율 적용 |
| **전투 시작 전** | `usePreBattleAction()` | 버프/디버프, 공격력 설정 | Strength, Artifact 부여 |

### 패치 타입 선택 가이드

```java
// 1. Postfix - 메서드 실행 후 코드 추가 (가장 많이 사용)
@SpirePostfixPatch
public static void Postfix(ClassName __instance) {
    // 원본 메서드 실행 후 추가 작업
}

// 2. Prefix - 메서드 실행 전 코드 추가
@SpirePrefixPatch
public static void Prefix(ClassName __instance) {
    // 원본 메서드 실행 전 작업
}

// 3. Insert - 메서드 중간에 코드 삽입 (고급)
@SpireInsertPatch(rloc = 10)  // 10번째 줄에 삽입
public static void Insert(ClassName __instance) {
    // 중간 지점에서 실행
}

// 4. Return - 메서드 조기 종료 또는 반환값 변경
@SpireInsertPatch(rloc = 0)
public static SpireReturn<?> Insert(ClassName __instance) {
    if (조건) {
        return SpireReturn.Return(null);  // 조기 종료
    }
    return SpireReturn.Continue();  // 원본 계속 실행
}
```

---

## 타입별 수정

### 1. 모든 적 수정

`AbstractMonster`를 패치하면 게임의 **모든 적**에 적용됩니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class AllMonsterHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        // 모든 적의 체력 10% 증가
        __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
        __instance.currentHealth = __instance.maxHealth;
    }
}
```

### 2. 타입별 수정 (일반/엘리트/보스)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class TypeBasedPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int level = AbstractDungeon.ascensionLevel;

        // 타입별 처리
        switch (__instance.type) {
            case NORMAL:
                // 일반 적 처리
                if (level >= 22) {
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
                }
                break;

            case ELITE:
                // 엘리트 처리
                if (level >= 21) {
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
                }
                break;

            case BOSS:
                // 보스 처리
                if (level >= 26) {
                    __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.15f);
                }
                break;
        }
    }
}
```

### 3. 특정 적만 수정

특정 적의 클래스를 직접 패치합니다.

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistOnlyPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Cultist만 수정
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;
        }
    }
}
```

### 4. 막별 수정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class ActBasedPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int actNum = AbstractDungeon.actNum;

        if (actNum == 1) {
            // 1막: 체력 10% 증가
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
        } else if (actNum == 2) {
            // 2막: 체력 20% 증가
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.2f);
        } else if (actNum >= 3) {
            // 3막 이상: 체력 30% 증가
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.3f);
        }

        __instance.currentHealth = __instance.maxHealth;
    }
}
```

---

## 능력치 수정

### 체력 수정

#### 패턴 1: 배율 적용 (추천)

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class HealthMultiplierPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        float multiplier = 1.0f;

        // 조건에 따라 배율 누적
        if (AbstractDungeon.ascensionLevel >= 21) {
            multiplier *= 1.1f;  // 10% 증가
        }
        if (AbstractDungeon.ascensionLevel >= 32) {
            multiplier *= 1.1f;  // 추가 10% (총 21%)
        }

        if (multiplier > 1.0f) {
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}
```

#### 패턴 2: 고정값 추가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class HealthBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int bonus = 0;

        if (AbstractDungeon.ascensionLevel >= 59 &&
            __instance.type == AbstractMonster.EnemyType.ELITE) {
            bonus += 10;  // 엘리트에게 HP +10
        }

        if (bonus > 0) {
            __instance.maxHealth += bonus;
            __instance.currentHealth += bonus;
        }
    }
}
```

### 공격력 수정

#### 패턴 1: 모든 공격력 증가

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class DamageIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int increase = 0;

        if (AbstractDungeon.ascensionLevel >= 52) {
            increase += 1;
        }
        if (AbstractDungeon.ascensionLevel >= 58) {
            increase += 1;
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
```

#### 패턴 2: 배율 적용

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class DamageMultiplierPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        float multiplier = 1.0f;

        if (AbstractDungeon.ascensionLevel >= 64 &&
            __instance.type == AbstractMonster.EnemyType.BOSS) {
            multiplier *= 1.1f;  // 보스 공격력 10% 증가
        }

        if (multiplier > 1.0f) {
            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    int original = dmg.base;
                    dmg.base = MathUtils.ceil(dmg.base * multiplier);
                }
            }
        }
    }
}
```

#### 패턴 3: 특정 적의 특정 공격 수정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Cultist의 기본 공격 감소
            if (!__instance.damage.isEmpty()) {
                DamageInfo dmg = __instance.damage.get(0);
                dmg.base = Math.max(1, dmg.base - 2);  // 최소 1
            }
        }
    }
}
```

---

## 행동 패턴 수정

### 1. 특정 행동에 추가 효과

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.JawWorm",
    method = "takeTurn"
)
public static class JawWormBellowPatch {
    @SpirePostfixPatch
    public static void Postfix(JawWorm __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Bellow(Strength 획득) 사용 시 방어도 추가
            if (__instance.nextMove == 2) {  // Bellow move ID
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 12)
                );
            }
        }
    }
}
```

### 2. 카드 추가 효과 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.AcidSlime_M",
    method = "takeTurn"
)
public static class SlimeSlimedPatch {
    @SpirePostfixPatch
    public static void Postfix(AcidSlime_M __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Lick 공격 시 Slimed 2장 추가
            if (__instance.nextMove == 1) {  // Lick move
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(new Slimed(), 2)
                );
            }
        }
    }
}
```

### 3. 조건부 행동 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Healer",
    method = "takeTurn"
)
public static class HealerAlonePatch {
    @SpirePostfixPatch
    public static void Postfix(Healer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 혼자 남았을 때 Strength 부여
            boolean isAlone = true;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m != __instance && !m.isDying && !m.isEscaping) {
                    isAlone = false;
                    break;
                }
            }

            if (isAlone) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new StrengthPower(__instance, 8), 8)
                );
            }
        }
    }
}
```

---

## 버프/디버프 추가

### 1. 전투 시작 시 버프 부여

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "usePreBattleAction"
)
public static class StartingBuffPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 70) {
            int actNum = AbstractDungeon.actNum;

            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                if (actNum == 1) {
                    // 1막 엘리트: Metallicize 4
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new MetallicizePower(__instance, 4), 4)
                    );
                } else if (actNum == 2) {
                    // 2막 엘리트: Strength 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new StrengthPower(__instance, 2), 2)
                    );
                } else if (actNum >= 3) {
                    // 3막 엘리트: Intangible 2
                    AbstractDungeon.actionManager.addToBottom(
                        new ApplyPowerAction(__instance, __instance,
                            new IntangiblePower(__instance, 2), 2)
                    );
                }
            }
        }
    }
}
```

### 2. 랜덤 버프 부여

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "usePreBattleAction"
)
public static class RandomBuffPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 66) {
            // 15% 확률로 랜덤 버프
            if (MathUtils.randomBoolean(0.15f)) {
                int randomBuff = MathUtils.random(2);  // 0, 1, 2

                int actNum = AbstractDungeon.actNum;
                if (actNum == 1) {
                    switch (randomBuff) {
                        case 0:
                            // Strength 2
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new StrengthPower(__instance, 2), 2)
                            );
                            break;
                        case 1:
                            // Metallicize 2
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new MetallicizePower(__instance, 2), 2)
                            );
                            break;
                        case 2:
                            // Regeneration 4
                            AbstractDungeon.actionManager.addToBottom(
                                new ApplyPowerAction(__instance, __instance,
                                    new RegenerateMonsterPower(__instance, 4), 4)
                            );
                            break;
                    }
                }
            }
        }
    }
}
```

### 3. 특정 적 전용 파워

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.LouseNormal",
    method = "usePreBattleAction"
)
public static class LouseCurlUpPatch {
    @SpirePostfixPatch
    public static void Postfix(LouseNormal __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Curl Up 파워 +3
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new CurlUpPower(__instance, 3), 3)
            );
        }
    }
}
```

---

## 실전 예제

### 예제 1: Act와 Ascension에 따른 차등 적용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "init"
)
public static class ComplexHealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        int level = AbstractDungeon.ascensionLevel;
        int act = AbstractDungeon.actNum;
        float multiplier = 1.0f;

        // Level 51: 모든 적 +20%
        if (level >= 51) {
            multiplier *= 1.2f;
        }

        // Level 69: 보스는 막별 차등
        if (level >= 69 && __instance.type == AbstractMonster.EnemyType.BOSS) {
            if (act == 1) {
                multiplier *= 1.1f;
            } else if (act == 2) {
                multiplier *= 1.2f;
            } else {
                multiplier *= 1.3f;
            }
        }

        if (multiplier > 1.0f) {
            __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * multiplier);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}
```

### 예제 2: 조건부 회복

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
    method = "damage"
)
public static class CenturionHealPatch {
    @SpirePostfixPatch
    public static void Postfix(Centurion __instance, DamageInfo info) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 플레이어에게 준 데미지만큼 회복
            if (info.owner == __instance &&
                info.type == DamageInfo.DamageType.NORMAL) {
                int unblocked = info.output;
                if (unblocked > 0) {
                    AbstractDungeon.actionManager.addToTop(
                        new HealAction(__instance, __instance, unblocked)
                    );
                }
            }
        }
    }
}
```

### 예제 3: 여러 적에 동일 패치 적용

```java
// 공통 로직을 static 메서드로 분리
public class CommonPatches {
    public static void applyCurlUpBonus(AbstractMonster monster) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(monster, monster,
                    new CurlUpPower(monster, 3), 3)
            );
        }
    }
}

// Louse Normal 패치
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.LouseNormal",
    method = "usePreBattleAction"
)
public static class LouseNormalPatch {
    @SpirePostfixPatch
    public static void Postfix(LouseNormal __instance) {
        CommonPatches.applyCurlUpBonus(__instance);
    }
}

// Louse Defensive 패치
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.LouseDefensive",
    method = "usePreBattleAction"
)
public static class LouseDefensivePatch {
    @SpirePostfixPatch
    public static void Postfix(LouseDefensive __instance) {
        CommonPatches.applyCurlUpBonus(__instance);
    }
}
```

---

## 💡 중요 팁

### 1. Null 체크 필수

```java
// ❌ 잘못된 예
dmg.base += 2;  // dmg가 null이면 크래시

// ✅ 올바른 예
if (dmg != null && dmg.base > 0) {
    dmg.base += 2;
}
```

### 2. 최소값 보장

```java
// 데미지나 체력이 0 이하가 되지 않도록
dmg.base = Math.max(1, dmg.base - 2);
```

### 3. 로그 추가

```java
logger.info(String.format(
    "Ascension %d: %s HP increased from %d to %d",
    level, __instance.name, originalHP, __instance.maxHealth
));
```

### 4. Ascension 레벨 중복 적용 방지

```java
// ❌ 잘못된 예
if (level >= 21) multiplier *= 1.1f;  // 21, 22, 23... 모두 적용됨
if (level >= 32) multiplier *= 1.1f;  // 32 이상에서 중복

// ✅ 올바른 예
if (level >= 21 && level < 32) multiplier *= 1.1f;  // 21~31만
if (level >= 32) multiplier *= 1.21f;  // 32 이상: 1.1 * 1.1 = 1.21
```

---

## 📚 관련 문서

- [ENEMY_LIST.md](ENEMY_LIST.md) - 적 목록 및 클래스명
- [MONSTER_BEHAVIOR.md](MONSTER_BEHAVIOR.md) - 행동 패턴 수정 상세
- [BOSS_MODIFICATIONS.md](BOSS_MODIFICATIONS.md) - 보스 전용 수정
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 자주 쓰는 패턴 모음

---

**작성 기준**: ascension-100 모드 및 디컴파일 소스 분석
