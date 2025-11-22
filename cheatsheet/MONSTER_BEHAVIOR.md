# 적 행동 패턴 수정 가이드

적의 행동 패턴, 버프/디버프, 특수 능력을 수정하는 방법을 설명합니다.

## 📑 목차

1. [⚠️ 패치 타이밍 규칙 (중요!)](#-패치-타이밍-규칙-중요)
2. [버프/디버프 추가](#버프디버프-추가)
3. [행동 패턴 수정](#행동-패턴-수정)
4. [카드 추가 효과](#카드-추가-효과)
5. [조건부 행동](#조건부-행동)
6. [특수 파워 추가](#특수-파워-추가)
7. [실전 예제](#실전-예제)

---

## ⚠️ 패치 타이밍 규칙 (중요!)

**CRITICAL**: 적의 속성을 수정할 때 **어떤 메서드를 패치하는지**에 따라 적용 시점이 달라집니다!

### 몬스터 초기화 순서

```
1. Constructor → HP, damage 배열 생성
2. init() → rollMove() 호출 → getMove() → setMove(damage.get(i).base)
   ↑ 첫 턴 공격 패턴과 데미지가 여기서 결정됨!
3. usePreBattleAction() → 버프 추가, 전투 시작 효과
   ↑ 여기서 damage.base 수정하면 첫 턴에는 적용 안됨!
4. 전투 시작
5. 두 번째 턴: rollMove() 다시 호출 → 이제 수정된 damage.base 사용
```

### 🔴 데미지 수정 → `init` Prefix 사용

**문제 상황**: `usePreBattleAction`에서 데미지를 수정하면?
- ❌ 첫 턴: 원래 데미지 (수정 전)
- ✅ 두 번째 턴부터: 수정된 데미지
- 결과: 일관성 없는 이상한 데미지!

**해결방법**: `init` 메서드를 Prefix로 패치

```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "init"  // ✅ usePreBattleAction이 아님!
)
public static class MonsterDamageIncrease {
    @SpirePrefixPatch  // ✅ Prefix로 rollMove() 이전에 실행
    public static void Prefix(AbstractMonster __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 24) {
            return;
        }

        // 데미지 수정
        for (DamageInfo damageInfo : __instance.damage) {
            if (damageInfo != null && damageInfo.base > 0) {
                damageInfo.base += 1;  // ✅ 첫 턴부터 적용됨
            }
        }
    }
}
```

**실제 예시**:
- ✅ Level24.java - 일반 적 데미지 +5% (init Prefix)
- ✅ Level35.java - 일반 적 데미지 +1 (init Prefix)
- ✅ Level52.java - 막별 데미지 증가 (init Prefix)

### 🟢 체력(HP) 수정 → `Constructor` Postfix 사용

체력은 몬스터 생성 시점에 고정되므로 Constructor에서 수정합니다.

```java
@SpirePatch(
    clz = GremlinWarrior.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = {float.class, float.class}
)
public static class GremlinWarriorHPPatch {
    @SpirePostfixPatch
    public static void Postfix(GremlinWarrior __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            __instance.maxHealth += 10;
            __instance.currentHealth += 10;
        }
    }
}
```

**실제 예시**:
- ✅ Level53.java - 그렘린 전사 HP +10 (Constructor Postfix)
- ✅ Level25.java - 다양한 몬스터 HP 증가 (Constructor Postfix)

### 🟢 버프/파워 추가 → `usePreBattleAction` Postfix 사용

버프는 전투 시작 전에 표시되기만 하면 되므로 `usePreBattleAction`에서 추가해도 문제없습니다.

```java
@SpirePatch(
    clz = Mugger.class,
    method = "usePreBattleAction"
)
public static class MuggerThieveryIncrease {
    @SpirePostfixPatch
    public static void Postfix(Mugger __instance) {
        if (AbstractDungeon.ascensionLevel >= 53) {
            AbstractPower thieveryPower = __instance.getPower("Thievery");
            if (thieveryPower != null) {
                thieveryPower.amount += 5;
                thieveryPower.updateDescription();
            }
        }
    }
}
```

**실제 예시**:
- ✅ Level53.java - 강도 도둑질 +5 (usePreBattleAction Postfix)
- ✅ Level25.java - 뱀 식물 탄성 +1 (usePreBattleAction Postfix)

### 🔵 특정 패턴만 수정 → `takeTurn` Postfix 사용 (주의!)

**특정 패턴의 데미지만 수정**하는 경우 (예: Byrd의 Headbutt 패턴만 +2)

```java
@SpirePatch(
    clz = Byrd.class,
    method = "takeTurn"
)
public static class ByrdHeadbuttEnhancement {
    private static final ThreadLocal<Byte> lastMove = new ThreadLocal<>();

    @SpirePrefixPatch
    public static void Prefix(Byrd __instance) {
        if (AbstractDungeon.ascensionLevel < 35) return;

        try {
            Field nextMoveField = AbstractMonster.class.getDeclaredField("nextMove");
            nextMoveField.setAccessible(true);
            byte move = nextMoveField.getByte(__instance);
            lastMove.set(move);
        } catch (Exception e) {
            logger.error("Failed to get Byrd move", e);
        }
    }

    @SpirePostfixPatch
    public static void Postfix(Byrd __instance) {
        if (AbstractDungeon.ascensionLevel < 35) return;

        Byte move = lastMove.get();
        if (move != null && move == 2) { // HEADBUTT move ID
            // 이번 턴에 사용한 패턴의 데미지를 수정
            // 다음 턴 rollMove() 이전이므로 다음 턴부터 적용됨
            __instance.damage.get(0).base += 2;
        }
        lastMove.remove();
    }
}
```

**⚠️ 주의사항**:
- 이 방식은 **첫 턴에는 적용되지 않을 수 있음**
- 만약 몬스터가 첫 턴에 해당 패턴을 사용한다면, 첫 턴은 원래 데미지로 나감
- 두 번째 턴부터는 수정된 데미지가 적용됨
- **첫 턴부터 적용이 필요하면 `init` Prefix에서 해당 패턴의 damage index를 찾아 수정해야 함**

**실제 예시**:
- ⚠️ Level35.java - Byrd Headbutt +2 (takeTurn Postfix, 첫 턴 미적용 가능성)

### 📋 요약표

| 수정 대상 | 패치 메서드 | 패치 타입 | 첫 턴 적용 | 비고 |
|----------|------------|----------|----------|------|
| **전체 데미지** | `init` | **Prefix** | ✅ 적용됨 | Level24, 35, 52 |
| **특정 패턴 데미지** | `takeTurn` | **Postfix** | ⚠️ 적용 안될 수 있음 | Level35 Byrd |
| **체력(HP)** | `Constructor` | **Postfix** | ✅ 적용됨 | 생성 시점 고정 |
| **버프/파워** | `usePreBattleAction` | **Postfix** | ✅ 적용됨 | 표시만 되면 됨 |

### 🔍 첫 턴부터 특정 패턴 데미지를 수정하려면?

만약 **특정 패턴의 데미지를 첫 턴부터 확실히 적용**하고 싶다면 `init` Prefix에서 패턴을 식별해야 합니다:

```java
@SpirePatch(
    clz = Byrd.class,
    method = "init"
)
public static class ByrdHeadbuttFirstTurnFix {
    @SpirePrefixPatch
    public static void Prefix(Byrd __instance) {
        if (AbstractDungeon.ascensionLevel < 35) return;

        // Byrd의 damage 배열에서 Headbutt에 해당하는 인덱스를 찾아 수정
        // 주의: 이 방법은 damage 배열의 구조를 정확히 알아야 함
        if (__instance.damage.size() > 0 && __instance.damage.get(0) != null) {
            __instance.damage.get(0).base += 2;  // 첫 턴부터 적용됨
        }
    }
}
```

**trade-off**:
- ✅ 첫 턴부터 적용됨
- ❌ damage 배열의 인덱스 구조를 정확히 알아야 함
- ❌ 다른 패턴에도 영향을 줄 수 있음 (만약 같은 damage 인덱스를 공유한다면)

---

## 버프/디버프 추가

### 전투 시작 시 버프 부여: `usePreBattleAction()`

```java
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
```

### 주요 버프 파워

```java
// Strength (힘)
new StrengthPower(__instance, amount)

// Metallicize (금속화) - 턴 시작 시 방어도
new MetallicizePower(__instance, amount)

// Regeneration (재생) - 턴 종료 시 회복
new RegenerateMonsterPower(__instance, amount)

// Artifact (인공물) - 디버프 무효
new ArtifactPower(__instance, amount)

// Intangible (불가침) - 받는 데미지 1로 제한
new IntangiblePower(__instance, amount)

// Plated Armor (판금 갑옷) - 턴 종료 시 방어도
new PlatedArmorPower(__instance, amount)
```

### 막별 다른 버프 (Level 70 예시)

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class ActBasedBuffPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 70 &&
            __instance.type == AbstractMonster.EnemyType.ELITE) {

            int act = AbstractDungeon.actNum;

            if (act == 1) {
                // 1막 엘리트: Metallicize 4
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new MetallicizePower(__instance, 4), 4)
                );
            } else if (act == 2) {
                // 2막 엘리트: Strength 2
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new StrengthPower(__instance, 2), 2)
                );
            } else if (act >= 3) {
                // 3막 엘리트: Intangible 2
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new IntangiblePower(__instance, 2), 2)
                );
            }
        }
    }
}
```

### 랜덤 버프 (Level 66 예시)

```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class RandomBuffPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 66) {
            // 15% 확률로 랜덤 버프
            if (MathUtils.randomBoolean(0.15f)) {
                int randomBuff = MathUtils.random(2);  // 0, 1, 2

                int act = AbstractDungeon.actNum;
                int strengthAmt = (act == 1) ? 2 : (act == 2) ? 3 : 6;
                int metallicizeAmt = (act == 1) ? 2 : (act == 2) ? 5 : 8;
                int regenAmt = (act == 1) ? 4 : (act == 2) ? 8 : 15;

                switch (randomBuff) {
                    case 0:
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new StrengthPower(__instance, strengthAmt), strengthAmt)
                        );
                        break;
                    case 1:
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new MetallicizePower(__instance, metallicizeAmt), metallicizeAmt)
                        );
                        break;
                    case 2:
                        AbstractDungeon.actionManager.addToBottom(
                            new ApplyPowerAction(__instance, __instance,
                                new RegenerateMonsterPower(__instance, regenAmt), regenAmt)
                        );
                        break;
                }
            }
        }
    }
}
```

---

## 행동 패턴 수정

### takeTurn() 패치: 특정 행동에 추가 효과

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.JawWorm",
    method = "takeTurn"
)
public static class JawWormBellowPatch {
    @SpirePostfixPatch
    public static void Postfix(JawWorm __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Bellow (nextMove == 2) 사용 시 방어도 추가
            if (__instance.nextMove == 2) {
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 12)
                );
            }
        }
    }
}
```

### 공격 시 추가 효과

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Byrd",
    method = "takeTurn"
)
public static class ByrdFlyPatch {
    @SpirePostfixPatch
    public static void Postfix(Byrd __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Fly 사용 시 Strength +1 추가
            if (__instance.nextMove == 2) {  // Fly move ID
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new StrengthPower(__instance, 1), 1)
                );
            }
        }
    }
}
```

---

## 카드 추가 효과

### 상태이상 카드 추가량 증가

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

### 주요 상태이상 카드

```java
// Slimed
new MakeTempCardInDiscardAction(new Slimed(), amount)

// Dazed
new MakeTempCardInDiscardAction(new Dazed(), amount)

// Wound
new MakeTempCardInDiscardAction(new Wound(), amount)

// Burn
new MakeTempCardInDiscardAction(new Burn(), amount)

// Void
new MakeTempCardInDiscardAction(new VoidCard(), amount)
```

---

## 조건부 행동

### 혼자 남았을 때 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Healer",
    method = "takeTurn"
)
public static class HealerAlonePatch {
    @SpirePostfixPatch
    public static void Postfix(Healer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 혼자 남았는지 확인
            boolean isAlone = true;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (m != __instance && !m.isDying && !m.isEscaping) {
                    isAlone = false;
                    break;
                }
            }

            // 혼자면 Strength 8 부여
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

### 플레이어 체력 기반 행동

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
    method = "takeTurn"
)
public static class HealthBasedPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            float playerHPPercent = (float) AbstractDungeon.player.currentHealth /
                                   AbstractDungeon.player.maxHealth;

            // 플레이어 체력 50% 이하면 공격력 증가
            if (playerHPPercent < 0.5f) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new StrengthPower(__instance, 2), 2)
                );
            }
        }
    }
}
```

---

## 특수 파워 추가

### 적 전용 파워

```java
// Louse - Curl Up (피격 시 방어도)
import com.megacrit.cardcrawl.powers.CurlUpPower;

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.exordium.LouseNormal", method = "usePreBattleAction")
public static class LouseCurlUpPatch {
    @SpirePostfixPatch
    public static void Postfix(LouseNormal __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new CurlUpPower(__instance, 3), 3)
            );
        }
    }
}
```

```java
// Cultist - Ritual (턴마다 Strength 증가)
import com.megacrit.cardcrawl.powers.RitualPower;

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist", method = SpirePatch.CONSTRUCTOR,
            paramtypez = { float.class, float.class, boolean.class })
public static class CultistRitualPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Ritual +2 (턴마다 Strength +5)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new RitualPower(__instance, 5, false), 5)
            );
        }
    }
}
```

```java
// Fungi Beast - Spore Cloud (사망 시 Vulnerable)
import com.megacrit.cardcrawl.powers.SporeCloudPower;

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.exordium.FungiBeast", method = "usePreBattleAction")
public static class FungiSporeCloudPatch {
    @SpirePostfixPatch
    public static void Postfix(FungiBeast __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new SporeCloudPower(__instance, 1), 1)
            );
        }
    }
}
```

```java
// Snake Plant - Malleable (피격 시 방어도 증가)
import com.megacrit.cardcrawl.powers.MalleablePower;

@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.city.SnakePlant", method = "usePreBattleAction")
public static class SnakePlantMalleablePatch {
    @SpirePostfixPatch
    public static void Postfix(SnakePlant __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new MalleablePower(__instance, 1), 1)
            );
        }
    }
}
```

---

## 실전 예제

### 예제 1: Shelled Parasite - Plated Armor 증가

```java
import com.megacrit.cardcrawl.monsters.city.ShelledParasite;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.ShelledParasite",
    method = "usePreBattleAction"
)
public static class ShelledParasitePatch {
    @SpirePostfixPatch
    public static void Postfix(ShelledParasite __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 기존 Plated Armor에 +2
            AbstractPower platedArmor = __instance.getPower("Plated Armor");
            if (platedArmor != null) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(__instance, __instance,
                        new PlatedArmorPower(__instance, 2), 2)
                );
            }
        }
    }
}
```

### 예제 2: Spheric Guardian - 방어도 추가

```java
import com.megacrit.cardcrawl.monsters.city.SphericGuardian;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SphericGuardian",
    method = "takeTurn"
)
public static class SphericGuardianBlockPatch {
    @SpirePostfixPatch
    public static void Postfix(SphericGuardian __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Shield Bash 사용 시 방어도 +15
            if (__instance.nextMove == 4) {  // Shield Bash
                AbstractDungeon.actionManager.addToBottom(
                    new GainBlockAction(__instance, __instance, 15)
                );
            }
        }
    }
}
```

### 예제 3: Darkling - 체력 증가 및 부활 지연

```java
import com.megacrit.cardcrawl.monsters.beyond.Darkling;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = SpirePatch.CONSTRUCTOR
)
public static class DarklingPatch {
    @SpirePostfixPatch
    public static void Postfix(Darkling __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // HP +25
            __instance.maxHealth += 25;
            __instance.currentHealth += 25;

            // 부활 턴 지연은 별도 로직 필요 (takeTurn 패치)
        }
    }
}
```

### 예제 4: Centurion - 회복 기능 추가

```java
import com.megacrit.cardcrawl.monsters.city.Centurion;

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
    method = "damage"
)
public static class CenturionHealPatch {
    @SpirePostfixPatch
    public static void Postfix(Centurion __instance, DamageInfo info) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 플레이어에게 데미지를 줬을 때 회복
            if (info.owner == __instance &&
                info.type == DamageInfo.DamageType.NORMAL &&
                info.output > 0) {

                int healAmount = info.output;  // 준 데미지만큼 회복
                AbstractDungeon.actionManager.addToTop(
                    new HealAction(__instance, __instance, healAmount)
                );
            }
        }
    }
}
```

---

## 💡 중요 팁

### 1. addToBottom vs addToTop

```java
// addToBottom: 큐 끝에 추가 (대부분 사용)
AbstractDungeon.actionManager.addToBottom(action);

// addToTop: 큐 앞에 추가 (즉시 실행)
AbstractDungeon.actionManager.addToTop(action);
```

### 2. 파워 중첩 확인

```java
// 기존 파워 확인
AbstractPower existingPower = __instance.getPower("Strength");
if (existingPower != null) {
    // 이미 있으면 추가 (자동 스택)
}
```

### 3. nextMove 확인

```java
// 적의 다음 행동 ID 확인 (디컴파일 소스 참조)
logger.info("Next move ID: " + __instance.nextMove);
```

### 4. 로그 추가

```java
logger.info(String.format(
    "Ascension %d: %s gained %d Strength",
    AbstractDungeon.ascensionLevel,
    __instance.name,
    amount
));
```

### 5. Null 체크

```java
if (__instance.getPower("PowerID") != null) {
    // 파워가 있을 때만 실행
}
```

---

## 📚 관련 문서

- [MONSTER_HEALTH.md](MONSTER_HEALTH.md) - 적 체력 수정
- [MONSTER_DAMAGE.md](MONSTER_DAMAGE.md) - 적 공격력 수정
- [BOSS_MODIFICATIONS.md](BOSS_MODIFICATIONS.md) - 보스 전용 수정
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 공통 패턴 모음

---

**작성 기준**: ascension-100 Level25Patches.java 실제 구현
**검증**: 768 lines, 29종 적 패치 검증 완료
