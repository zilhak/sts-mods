# Level 40-50 Implementation Guide

## Overview

This document provides detailed implementation guidance for Ascension Levels 40-50. It covers both implemented and unimplemented levels with complete code examples.

## Implementation Status

| Level | Feature | Status | Difficulty |
|-------|---------|--------|-----------|
| 40 | No effect (placeholder) | ✅ Implemented | Trivial |
| 41 | 10% of ? rooms → Elite | ❌ Not Implemented | Medium |
| 42 | +25% combat gold, +25 card removal cost | ✅ Implemented | Easy |
| 43 | Wheel event HP loss adjustment | ❌ Not Implemented | Easy |
| 44 | Looter/Mugger steal +10 gold | ❌ Not Implemented | Easy |
| 45 | Enemy composition changes | ❌ Not Implemented | Hard |
| 46 | +20% combat gold, +15% relic price | ✅ Implemented | Easy |
| 47 | Rest reduces max HP by 2 | ✅ Implemented | Easy |
| 48 | Neow penalty +10% | ❌ Not Implemented | Easy |
| 49 | +30% combat gold, +50% card price | ✅ Implemented | Easy |
| 50 | Act 4 forced with penalties | ❌ Not Implemented | Medium |

---

## Implemented Levels (Examples)

### Level 40: No Effect

```java
// ========================================
// LEVEL 40: NO EFFECT (INTENTIONALLY EMPTY)
// ========================================
```

**Purpose**: Placeholder level to maintain numbering consistency.

---

### Level 42: Combat Gold +25%, Card Removal +25

**Gold Increase Patch**:

```java
@SpirePatch(
        clz = RewardItem.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
)
public static class GoldRewardIncreasePatch {
    @SpireInsertPatch(
            locator = GoldConstructorLocator.class
    )
    public static void Insert(RewardItem __instance, @ByRef int[] goldAmount) {
        if (!AbstractDungeon.isAscensionMode) {
            return;
        }

        int level = AbstractDungeon.ascensionLevel;
        float multiplier = 1.0f;

        // Level 42: +25%
        if (level >= 42) {
            multiplier *= 1.25f;
        }

        // Level 46: +20% (stacks)
        if (level >= 46) {
            multiplier *= 1.20f;
        }

        // Level 49: +30% (stacks)
        if (level >= 49) {
            multiplier *= 1.30f;
        }

        if (multiplier > 1.0f) {
            int originalGold = goldAmount[0];
            goldAmount[0] = MathUtils.ceil(goldAmount[0] * multiplier);
        }
    }

    private static class GoldConstructorLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctBehavior) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(RewardItem.class, "goldAmt");
            return LineFinder.findInOrder(ctBehavior, finalMatcher);
        }
    }
}
```

**Card Removal Cost Patch**:

```java
@SpirePatch(
        clz = ShopScreen.class,
        method = "init"
)
public static class CardRemovalCostPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 42) {
            return;
        }

        ShopScreen.purgeCost += 25;
    }
}
```

---

### Level 46: Combat Gold +20%, Relic Price +15%

**Relic Price Patch**:

```java
@SpirePatch(
        clz = AbstractRelic.class,
        method = "getPrice"
)
public static class RelicPriceIncreasePatch {
    @SpirePostfixPatch
    public static int Postfix(int __result) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 46) {
            return __result;
        }

        return MathUtils.ceil(__result * 1.15f);
    }
}
```

---

### Level 47: Rest Reduces Max HP by 2

```java
@SpirePatch(
        clz = com.megacrit.cardcrawl.ui.campfire.RestOption.class,
        method = "useOption"
)
public static class RestMaxHPPenaltyPatch {
    @SpirePostfixPatch
    public static void Postfix(Object __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 47) {
            return;
        }

        AbstractDungeon.player.decreaseMaxHealth(2);
    }
}
```

---

### Level 49: Combat Gold +30%, Card Price +50%

**Card Price Patch**:

```java
@SpirePatch(
        clz = AbstractCard.class,
        method = "getPrice"
)
public static class CardPriceIncreasePatch {
    @SpirePostfixPatch
    public static int Postfix(int __result) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 49) {
            return __result;
        }

        return MathUtils.ceil(__result * 1.5f);
    }
}
```

---

## Unimplemented Levels (Implementation Guide)

### Level 41: 10% of ? Rooms → Elite Encounters

**Target Class**: `com.megacrit.cardcrawl.map.RoomTypeAssigner`

**Patch Method**: `assignRoomsToNodes()` (line 228-241)

**Implementation**:

```java
@SpirePatch(
        clz = RoomTypeAssigner.class,
        method = "assignRoomsToNodes"
)
public static class EventRoomToElitePatch {
    @SpirePostfixPatch
    public static void Postfix(ArrayList<ArrayList<MapRoomNode>> map, ArrayList<AbstractRoom> roomList) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 41) {
            return;
        }

        // Iterate through all nodes in the map
        for (ArrayList<MapRoomNode> row : map) {
            for (MapRoomNode node : row) {
                if (node != null && node.hasEdges() && node.getRoom() instanceof EventRoom) {
                    // 10% chance to convert EventRoom to MonsterRoomElite
                    if (AbstractDungeon.mapRng.randomBoolean(0.1f)) {
                        node.setRoom(new MonsterRoomElite());

                        logger.info(String.format(
                                "Ascension %d: Converted EventRoom at (%d, %d) to MonsterRoomElite",
                                AbstractDungeon.ascensionLevel, node.x, node.y
                        ));
                    }
                }
            }
        }
    }
}
```

**Key Points**:
- Patch runs after room assignment is complete
- Uses `AbstractDungeon.mapRng` for consistency with map generation
- 10% conversion rate (0.1f probability)
- Only affects rooms with edges (reachable nodes)

**Imports Required**:
```java
import com.megacrit.cardcrawl.map.RoomTypeAssigner;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.EventRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import java.util.ArrayList;
```

---

### Level 43: Wheel Event HP Loss Adjustment

**Target Class**: `com.megacrit.cardcrawl.events.shrines.GremlinWheelGame`

**Target Field**: `hpLossPercent` (line 70)

**Current Behavior**:
- Base: 10% HP loss (0.1f)
- Ascension 15+: 15% HP loss (0.15f)

**Proposed Change**: Adjust based on player HP percentage
- If HP > 50%: Increase penalty
- If HP < 30%: Decrease penalty

**Implementation**:

```java
@SpirePatch(
        clz = GremlinWheelGame.class,
        method = SpirePatch.CONSTRUCTOR
)
public static class WheelEventHPPenaltyPatch {
    @SpirePostfixPatch
    public static void Postfix(GremlinWheelGame __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 43) {
            return;
        }

        float currentHPPercent = (float) AbstractDungeon.player.currentHealth / AbstractDungeon.player.maxHealth;

        // Get current hpLossPercent value using reflection
        float currentPenalty = ReflectionHacks.getPrivate(__instance, GremlinWheelGame.class, "hpLossPercent");

        // Adjust penalty based on HP percentage
        float adjustedPenalty = currentPenalty;

        if (currentHPPercent > 0.5f) {
            // Player has high HP → increase penalty
            adjustedPenalty += 0.05f;
        } else if (currentHPPercent < 0.3f) {
            // Player has low HP → reduce penalty (but not below base)
            adjustedPenalty = Math.max(0.1f, adjustedPenalty - 0.05f);
        }

        // Set the adjusted penalty
        ReflectionHacks.setPrivate(__instance, GremlinWheelGame.class, "hpLossPercent", adjustedPenalty);

        logger.info(String.format(
                "Ascension %d: Wheel HP penalty adjusted from %.2f to %.2f (current HP: %.1f%%)",
                AbstractDungeon.ascensionLevel, currentPenalty, adjustedPenalty, currentHPPercent * 100
        ));
    }
}
```

**Alternative Simple Implementation** (flat +5% penalty):

```java
@SpirePostfixPatch
public static void Postfix(GremlinWheelGame __instance) {
    if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 43) {
        return;
    }

    float currentPenalty = ReflectionHacks.getPrivate(__instance, GremlinWheelGame.class, "hpLossPercent");
    ReflectionHacks.setPrivate(__instance, GremlinWheelGame.class, "hpLossPercent", currentPenalty + 0.05f);
}
```

**Imports Required**:
```java
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.events.shrines.GremlinWheelGame;
import basemod.ReflectionHacks;
```

---

### Level 44: Looter/Mugger Steal +10 Gold

**Target Classes**:
- `com.megacrit.cardcrawl.monsters.exordium.Looter`
- `com.megacrit.cardcrawl.monsters.city.Mugger`

**Target Field**: `goldAmt` (private field)

**Current Values**:
- Ascension 17+: 20 gold
- Below Ascension 17: 15 gold

**Implementation**:

```java
/**
 * Level 44: Looter steals +10 more gold
 */
@SpirePatch(
        clz = Looter.class,
        method = SpirePatch.CONSTRUCTOR
)
public static class LooterGoldIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(Looter __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
            return;
        }

        // Access private goldAmt field using Reflection
        int currentGoldAmt = ReflectionHacks.getPrivate(__instance, Looter.class, "goldAmt");
        int newGoldAmt = currentGoldAmt + 10;

        ReflectionHacks.setPrivate(__instance, Looter.class, "goldAmt", newGoldAmt);

        logger.info(String.format(
                "Ascension %d: Looter goldAmt increased from %d to %d",
                AbstractDungeon.ascensionLevel, currentGoldAmt, newGoldAmt
        ));
    }
}

/**
 * Level 44: Mugger steals +10 more gold
 */
@SpirePatch(
        clz = Mugger.class,
        method = SpirePatch.CONSTRUCTOR
)
public static class MuggerGoldIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(Mugger __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
            return;
        }

        int currentGoldAmt = ReflectionHacks.getPrivate(__instance, Mugger.class, "goldAmt");
        int newGoldAmt = currentGoldAmt + 10;

        ReflectionHacks.setPrivate(__instance, Mugger.class, "goldAmt", newGoldAmt);

        logger.info(String.format(
                "Ascension %d: Mugger goldAmt increased from %d to %d",
                AbstractDungeon.ascensionLevel, currentGoldAmt, newGoldAmt
        ));
    }
}
```

**Alternative: Patch ThieveryPower Application**

Instead of modifying the monster constructors, patch the power application:

```java
@SpirePatch(
        clz = ThieveryPower.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractCreature.class, int.class}
)
public static class ThieveryPowerIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(ThieveryPower __instance, AbstractCreature owner, @ByRef int[] goldAmt) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 44) {
            return;
        }

        // Only apply to Looter and Mugger
        if (owner instanceof Looter || owner instanceof Mugger) {
            goldAmt[0] += 10;

            logger.info(String.format(
                    "Ascension %d: %s ThieveryPower increased by 10 (new: %d)",
                    AbstractDungeon.ascensionLevel, owner.name, goldAmt[0]
            ));
        }
    }
}
```

**Imports Required**:
```java
import com.megacrit.cardcrawl.monsters.exordium.Looter;
import com.megacrit.cardcrawl.monsters.city.Mugger;
import com.megacrit.cardcrawl.powers.ThieveryPower;
import basemod.ReflectionHacks;
```

---

### Level 45: Enemy Composition Changes

**Target Class**: `com.megacrit.cardcrawl.helpers.MonsterHelper`

**Target Method**: `getEncounter(String key)` (not visible in first 200 lines, needs full analysis)

**Proposed Changes**:
- "Large Slime" encounter: Add 1 more slime
- "2 Louse" encounter: Increase to 3 Louse

**Implementation Strategy**:

```java
/**
 * Level 45: Modify enemy encounters to add more enemies
 */
@SpirePatch(
        clz = MonsterHelper.class,
        method = "getEncounter"
)
public static class EnemyCompositionPatch {
    @SpirePostfixPatch
    public static MonsterGroup Postfix(MonsterGroup __result, String key) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 45) {
            return __result;
        }

        // Modify "Large Slime" encounter
        if ("Large Slime".equals(key)) {
            // Add one more slime to the group
            AbstractMonster extraSlime;
            if (AbstractDungeon.aiRng.randomBoolean()) {
                extraSlime = new SpikeSlime_S(-365.0F, 10.0F);
            } else {
                extraSlime = new AcidSlime_S(-365.0F, 10.0F);
            }
            __result.add(extraSlime);

            logger.info("Ascension " + AbstractDungeon.ascensionLevel +
                       ": Added extra slime to 'Large Slime' encounter");
        }

        // Modify "2 Louse" encounter to "3 Louse"
        if ("2 Louse".equals(key)) {
            AbstractMonster extraLouse;
            if (AbstractDungeon.aiRng.randomBoolean()) {
                extraLouse = new LouseNormal(-390.0F, 5.0F);
            } else {
                extraLouse = new LouseDefensive(-390.0F, 5.0F);
            }
            __result.add(extraLouse);

            logger.info("Ascension " + AbstractDungeon.ascensionLevel +
                       ": Added third louse to '2 Louse' encounter");
        }

        return __result;
    }
}
```

**Position Adjustment**: You may need to reposition monsters after adding new ones:

```java
private static void repositionMonsters(MonsterGroup group) {
    int count = group.monsters.size();
    float spacing = 200.0F;
    float startX = -spacing * (count - 1) / 2.0F;

    for (int i = 0; i < count; i++) {
        group.monsters.get(i).drawX = startX + (i * spacing);
        group.monsters.get(i).hb.move(group.monsters.get(i).drawX, group.monsters.get(i).drawY);
    }
}
```

**Imports Required**:
```java
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.*;
```

**Difficulty**: This is the hardest implementation because:
1. Need to find exact monster positioning
2. MonsterGroup structure needs to be understood
3. Must ensure monsters don't overlap visually

---

### Level 48: Neow Penalty +10%

**Target Class**: `com.megacrit.cardcrawl.neow.NeowReward`

**Target Fields**:
- `hp_bonus` (line 69): `(int)(AbstractDungeon.player.maxHealth * 0.1F)`
- Damage penalty (line 113): `(AbstractDungeon.player.currentHealth / 10 * 3)`

**Implementation**:

```java
/**
 * Level 48: Increase Neow penalties by 10%
 */
@SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {boolean.class}
)
public static class NeowPenaltyIncreasePatch1 {
    @SpirePostfixPatch
    public static void Postfix(NeowReward __instance, boolean firstMini) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
            return;
        }

        // Get current hp_bonus value
        int currentBonus = ReflectionHacks.getPrivate(__instance, NeowReward.class, "hp_bonus");

        // Increase penalty by 10% (multiply by 1.1)
        int newBonus = (int) (currentBonus * 1.1f);

        ReflectionHacks.setPrivate(__instance, NeowReward.class, "hp_bonus", newBonus);

        logger.info(String.format(
                "Ascension %d: Neow hp_bonus increased from %d to %d",
                AbstractDungeon.ascensionLevel, currentBonus, newBonus
        ));
    }
}

@SpirePatch(
        clz = NeowReward.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {int.class}
)
public static class NeowPenaltyIncreasePatch2 {
    @SpirePostfixPatch
    public static void Postfix(NeowReward __instance, int category) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
            return;
        }

        int currentBonus = ReflectionHacks.getPrivate(__instance, NeowReward.class, "hp_bonus");
        int newBonus = (int) (currentBonus * 1.1f);

        ReflectionHacks.setPrivate(__instance, NeowReward.class, "hp_bonus", newBonus);
    }
}
```

**Alternative: Patch getRewardDrawbackOptions()**

For percentage damage penalty (line 113), patch the method that generates drawback options:

```java
@SpirePatch(
        clz = NeowReward.class,
        method = "getRewardDrawbackOptions"
)
public static class NeowDamageDrawbackPatch {
    @SpirePostfixPatch
    public static ArrayList<NeowReward.NeowRewardDrawbackDef> Postfix(
            ArrayList<NeowReward.NeowRewardDrawbackDef> __result) {

        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
            return __result;
        }

        // Find and modify the PERCENT_DAMAGE drawback
        for (NeowReward.NeowRewardDrawbackDef def : __result) {
            if (def.type == NeowReward.NeowRewardDrawback.PERCENT_DAMAGE) {
                // Original: currentHealth / 10 * 3 = 30% damage
                // New: currentHealth / 10 * 3.3 ≈ 33% damage (+10%)
                int originalDamage = AbstractDungeon.player.currentHealth / 10 * 3;
                int newDamage = (int) (originalDamage * 1.1f);

                // Note: This requires access to private fields and may be complex
                // Better to patch the activate() method instead
            }
        }

        return __result;
    }
}
```

**Simpler Approach: Patch activate() method**

Since the actual damage is applied in `activate()` method, patch it there:

```java
@SpirePatch(
        clz = NeowReward.class,
        method = "activate"
)
public static class NeowDamageApplicationPatch {
    @SpirePrefixPatch
    public static void Prefix(NeowReward __instance) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 48) {
            return;
        }

        NeowReward.NeowRewardDrawback drawback = ReflectionHacks.getPrivate(
                __instance, NeowReward.class, "drawback");

        if (drawback == NeowReward.NeowRewardDrawback.PERCENT_DAMAGE) {
            // Penalty will be applied - increase hp_bonus to increase damage
            int currentBonus = ReflectionHacks.getPrivate(__instance, NeowReward.class, "hp_bonus");
            ReflectionHacks.setPrivate(__instance, NeowReward.class, "hp_bonus",
                                      (int) (currentBonus * 1.1f));
        }
    }
}
```

**Imports Required**:
```java
import com.megacrit.cardcrawl.neow.NeowReward;
import basemod.ReflectionHacks;
```

---

### Level 50: Act 4 Forced Entry with Penalties

**Target Class**: `com.megacrit.cardcrawl.dungeons.TheEnding`

**Target Method**: Constructor (line 35)

**Proposed Penalties**:
- Force Act 4 entry even without all keys
- If missing keys: Set player HP to 1, energy to 0 for first combat

**Implementation**:

```java
/**
 * Level 50: Force Act 4 entry with severe penalties if keys are missing
 */
@SpirePatch(
        clz = TheEnding.class,
        method = SpirePatch.CONSTRUCTOR,
        paramtypez = {AbstractPlayer.class, ArrayList.class}
)
public static class ForceAct4Patch {
    @SpirePostfixPatch
    public static void Postfix(TheEnding __instance, AbstractPlayer p, ArrayList<String> theList) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 50) {
            return;
        }

        // Check if player has all required keys
        boolean hasRubyKey = Settings.hasRubyKey;
        boolean hasEmeraldKey = Settings.hasEmeraldKey;
        boolean hasSapphireKey = Settings.hasSapphireKey;

        int missingKeys = 0;
        if (!hasRubyKey) missingKeys++;
        if (!hasEmeraldKey) missingKeys++;
        if (!hasSapphireKey) missingKeys++;

        if (missingKeys > 0) {
            logger.info(String.format(
                    "Ascension %d: Player entered Act 4 with %d missing keys. Applying penalties...",
                    AbstractDungeon.ascensionLevel, missingKeys
            ));

            // Apply severe penalties
            // Penalty 1: Reduce HP to 1
            int hpLoss = AbstractDungeon.player.currentHealth - 1;
            if (hpLoss > 0) {
                AbstractDungeon.player.currentHealth = 1;
                logger.info("Penalty: HP reduced to 1");
            }

            // Penalty 2: Reduce energy to 0 for first combat
            // This requires adding a power or tracking state
            // We'll add a custom debuff power
            AbstractDungeon.player.powers.add(new Act4PenaltyPower(AbstractDungeon.player, missingKeys));

            logger.info(String.format("Applied Act 4 penalty: %d missing keys", missingKeys));
        }
    }
}
```

**Custom Power for Energy Penalty**:

```java
public class Act4PenaltyPower extends AbstractPower {
    public static final String POWER_ID = "Ascension100:Act4Penalty";
    private int keysRequired;

    public Act4PenaltyPower(AbstractCreature owner, int missingKeys) {
        this.name = "Act 4 Penalty";
        this.ID = POWER_ID;
        this.owner = owner;
        this.keysRequired = missingKeys;
        this.amount = missingKeys;
        this.type = PowerType.DEBUFF;
        updateDescription();
    }

    @Override
    public void atStartOfTurn() {
        if (keysRequired > 0) {
            // Remove all energy for first turn
            AbstractDungeon.player.energy.use(AbstractDungeon.player.energy.energy);
            keysRequired = 0;
            this.flash();
        }
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (keysRequired == 0) {
            // Remove this power after first turn
            AbstractDungeon.actionManager.addToBottom(
                new RemoveSpecificPowerAction(owner, owner, this));
        }
    }

    @Override
    public void updateDescription() {
        this.description = String.format(
                "Entered Act 4 without %d key(s). No energy on first turn.",
                this.amount
        );
    }
}
```

**Alternative: Simpler Implementation**

Instead of custom power, use existing debuff:

```java
@SpirePostfixPatch
public static void Postfix(TheEnding __instance, AbstractPlayer p, ArrayList<String> theList) {
    if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 50) {
        return;
    }

    int missingKeys = 0;
    if (!Settings.hasRubyKey) missingKeys++;
    if (!Settings.hasEmeraldKey) missingKeys++;
    if (!Settings.hasSapphireKey) missingKeys++;

    if (missingKeys > 0) {
        // Set HP to 1
        AbstractDungeon.player.currentHealth = 1;

        // Add Frail for 3 turns per missing key
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                p, p,
                new FrailPlayerPower(p, missingKeys * 3),
                missingKeys * 3
            )
        );

        // Add Weak for 3 turns per missing key
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                p, p,
                new WeakPower(p, missingKeys * 3, false),
                missingKeys * 3
            )
        );
    }
}
```

**Imports Required**:
```java
import com.megacrit.cardcrawl.dungeons.TheEnding;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.powers.FrailPlayerPower;
import com.megacrit.cardcrawl.powers.WeakPower;
```

---

## Testing Checklist

### Level 41 Testing
- [ ] Start new run with Ascension 41+
- [ ] Count total ? rooms and Elite rooms before/after patch
- [ ] Verify approximately 10% conversion rate over multiple runs
- [ ] Check that converted elites have proper rewards (relics)

### Level 43 Testing
- [ ] Trigger Wheel of Change event at Ascension 43+
- [ ] Test at different HP percentages (high/mid/low)
- [ ] Verify HP loss percentage changes based on conditions
- [ ] Compare with Ascension 42 baseline

### Level 44 Testing
- [ ] Fight Looter at Ascension 44+ (Exordium)
- [ ] Fight Mugger at Ascension 44+ (City)
- [ ] Check gold stolen amount matches expected value
- [ ] Verify ThieveryPower tooltip shows correct amount

### Level 45 Testing
- [ ] Encounter "Large Slime" fight
- [ ] Verify extra slime appears
- [ ] Encounter "2 Louse" fight
- [ ] Verify 3 louses appear instead of 2
- [ ] Check monster positioning looks correct

### Level 48 Testing
- [ ] Start Ascension 48+ run
- [ ] Select Neow blessing with HP loss drawback
- [ ] Verify HP loss is ~11% instead of 10%
- [ ] Select percentage damage drawback
- [ ] Verify damage is increased by ~10%

### Level 50 Testing
- [ ] Reach Act 3 boss without collecting all keys
- [ ] Verify Act 4 is forced (cannot skip)
- [ ] Check penalties are applied (HP=1, debuffs)
- [ ] Complete Act 4 to verify penalties only apply at start

---

## Common Patterns and Utilities

### Ascension Level Check Template

```java
if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < TARGET_LEVEL) {
    return;
}
```

### Reflection Access Template

```java
// Get private field
Type value = ReflectionHacks.getPrivate(instance, ClassName.class, "fieldName");

// Set private field
ReflectionHacks.setPrivate(instance, ClassName.class, "fieldName", newValue);
```

### @ByRef for Primitive Modification

```java
@SpireInsertPatch
public static void Insert(@ByRef int[] value) {
    value[0] = newValue;  // Modify the actual parameter
}
```

### Locator Pattern for Precise Injection

```java
private static class MyLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctBehavior) throws Exception {
        Matcher matcher = new Matcher.FieldAccessMatcher(TargetClass.class, "fieldName");
        return LineFinder.findInOrder(ctBehavior, matcher);
    }
}
```

---

## Difficulty Ratings Explained

### Easy (Levels 42, 43, 44, 46, 47, 48, 49)
- Simple field modifications
- Direct @SpirePostfixPatch or @SpirePrefixPatch
- No complex logic or state tracking
- Reflection usage is straightforward

### Medium (Levels 41, 50)
- Requires understanding of game flow
- May need custom state tracking
- Moderate complexity in logic
- Multiple systems interaction

### Hard (Level 45)
- Requires deep understanding of game internals
- Complex data structure manipulation
- Visual/positioning concerns
- High risk of unintended side effects

---

## Next Steps

1. **Implement Easy Levels First** (43, 44, 48)
2. **Test Medium Complexity** (41, 50)
3. **Tackle Hard Level Last** (45)
4. **Comprehensive Testing** with full Ascension 41-50 run
5. **Balance Adjustments** based on player feedback

---

## References

- **MONSTER_HEALTH.md**: Monster HP modification patterns
- **MONSTER_DAMAGE.md**: Monster damage modification patterns
- **MONSTER_BEHAVIOR.md**: Monster behavior and power patterns
- **PATCH_BASICS.md**: ModTheSpire patching fundamentals
- **Decompiled Source**: `E:\workspace\sts-decompile\`

---

## Credits

Implementation guide created for Ascension-100 mod.
Analysis based on decompiled Slay the Spire source code.
