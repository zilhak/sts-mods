# 적 행동 패턴 수정 가이드

적의 행동 패턴, 버프/디버프, 특수 능력을 수정하는 방법을 설명합니다.

## 📑 목차

1. [버프/디버프 추가](#버프디버프-추가)
2. [행동 패턴 수정](#행동-패턴-수정)
3. [카드 추가 효과](#카드-추가-효과)
4. [조건부 행동](#조건부-행동)
5. [특수 파워 추가](#특수-파워-추가)
6. [실전 예제](#실전-예제)

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
