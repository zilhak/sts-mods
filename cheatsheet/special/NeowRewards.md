# Neow Rewards System - Complete Guide

## 1. System Overview

### 1.1 Core Classes

#### NeowRoom
- **Location**: `com.megacrit.cardcrawl.neow.NeowRoom`
- **Purpose**: Container for the Neow event room
- **Key Features**:
  - Creates and manages `NeowEvent` instance
  - Handles room phase transitions
  - Renders the event UI

```java
public NeowRoom(boolean isDone) {
    this.phase = AbstractRoom.RoomPhase.EVENT;
    this.event = new NeowEvent(isDone);
    this.event.onEnterRoom();
}
```

#### NeowEvent
- **Location**: `com.megacrit.cardcrawl.neow.NeowEvent`
- **Purpose**: Main event controller for Neow interactions
- **Key Features**:
  - Manages dialogue flow
  - Creates reward options
  - Handles button clicks
  - Tracks boss kill count for blessing tier

```java
public static Random rng = null;  // Initialized with Settings.seed
private ArrayList<NeowReward> rewards = new ArrayList<>();
private int screenNum = 2;  // Controls dialogue state
```

#### NeowReward
- **Location**: `com.megacrit.cardcrawl.neow.NeowReward`
- **Purpose**: Represents individual reward options
- **Key Features**:
  - Defines reward types and drawbacks
  - Handles reward activation
  - Generates card/relic selections

### 1.2 Reward Flow Timeline

```
Game Start → NeowRoom created → NeowEvent initialized → RNG seeded
    ↓
Boss Count Check (0 = mini blessing, >0 = full blessing)
    ↓
blessing() called → 4 NeowReward objects created (categories 0,1,2,3)
    ↓
Each NeowReward: getRewardOptions(category) → random selection
    ↓
UI displays 4 buttons with reward text
    ↓
Player clicks → buttonEffect() → activate() → apply effects
    ↓
SaveHelper.saveIfAppropriate() → Room complete
```

## 2. NeowReward Enum Types

### 2.1 Complete NeowRewardType Enum

```java
public enum NeowRewardType {
    RANDOM_COLORLESS_2,      // 2 colorless cards (rare only)
    THREE_CARDS,             // Choose 1 from 3 cards
    ONE_RANDOM_RARE_CARD,    // 1 random rare card
    REMOVE_CARD,             // Remove 1 card
    UPGRADE_CARD,            // Upgrade 1 card
    RANDOM_COLORLESS,        // 1 colorless card
    TRANSFORM_CARD,          // Transform 1 card
    THREE_SMALL_POTIONS,     // 3 random potions
    RANDOM_COMMON_RELIC,     // 1 random common relic
    TEN_PERCENT_HP_BONUS,    // +10% max HP
    HUNDRED_GOLD,            // +100 gold
    THREE_ENEMY_KILL,        // Neow's Lament (kills 3 enemies)
    REMOVE_TWO,              // Remove 2 cards
    TRANSFORM_TWO_CARDS,     // Transform 2 cards
    ONE_RARE_RELIC,          // 1 random rare relic
    THREE_RARE_CARDS,        // Choose 1 from 3 rare cards
    TWO_FIFTY_GOLD,          // +250 gold
    TWENTY_PERCENT_HP_BONUS, // +20% max HP
    BOSS_RELIC               // Swap starter relic for random boss relic
}
```

### 2.2 NeowRewardDrawback Enum

```java
public enum NeowRewardDrawback {
    NONE,                // No drawback
    TEN_PERCENT_HP_LOSS, // -10% max HP
    NO_GOLD,             // Lose all gold
    CURSE,               // Obtain 1 random curse
    PERCENT_DAMAGE       // Take damage (30% of current HP)
}
```

## 3. Reward Categories and Selection

### 3.1 Category 0: Card Rewards (No Drawback)

```java
case 0:
    rewardOptions.add(new NeowRewardDef(NeowRewardType.THREE_CARDS, TEXT[0]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.ONE_RANDOM_RARE_CARD, TEXT[1]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.REMOVE_CARD, TEXT[2]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.UPGRADE_CARD, TEXT[3]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.TRANSFORM_CARD, TEXT[4]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.RANDOM_COLORLESS, TEXT[30]));
    break;
```

**Pool**: 6 options → randomly selects 1

| Reward Type | Effect | Localization Key |
|-------------|--------|------------------|
| THREE_CARDS | Choose 1 card from 3 options | TEXT[0] |
| ONE_RANDOM_RARE_CARD | Obtain 1 random rare card | TEXT[1] |
| REMOVE_CARD | Remove 1 card from deck | TEXT[2] |
| UPGRADE_CARD | Upgrade 1 card | TEXT[3] |
| TRANSFORM_CARD | Transform 1 card | TEXT[4] |
| RANDOM_COLORLESS | Choose 1 colorless card | TEXT[30] |

### 3.2 Category 1: Small Rewards (No Drawback)

```java
case 1:
    rewardOptions.add(new NeowRewardDef(NeowRewardType.THREE_SMALL_POTIONS, TEXT[5]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.RANDOM_COMMON_RELIC, TEXT[6]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.TEN_PERCENT_HP_BONUS,
                                        TEXT[7] + this.hp_bonus + " ]"));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.THREE_ENEMY_KILL, TEXT[28]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.HUNDRED_GOLD,
                                        TEXT[8] + 'd' + TEXT[9]));
    break;
```

**Pool**: 5 options → randomly selects 1

| Reward Type | Effect | Localization Key |
|-------------|--------|------------------|
| THREE_SMALL_POTIONS | Obtain 3 random potions | TEXT[5] |
| RANDOM_COMMON_RELIC | Obtain 1 random common relic | TEXT[6] |
| TEN_PERCENT_HP_BONUS | Gain max HP (10% of current max) | TEXT[7] + hp_bonus + " ]" |
| THREE_ENEMY_KILL | Neow's Lament (first 3 combats end instantly) | TEXT[28] |
| HUNDRED_GOLD | Gain 100 gold | TEXT[8] + 'd' + TEXT[9] |

### 3.3 Category 2: Large Rewards (WITH Drawback)

```java
case 2:
    // First, randomly select a drawback
    drawbackOptions = getRewardDrawbackOptions();
    this.drawbackDef = drawbackOptions.get(NeowEvent.rng.random(0, drawbackOptions.size() - 1));
    this.drawback = this.drawbackDef.type;

    // Then add rewards (some conditional on drawback type)
    rewardOptions.add(new NeowRewardDef(NeowRewardType.RANDOM_COLORLESS_2, TEXT[31]));
    if (this.drawback != NeowRewardDrawback.CURSE) {
        rewardOptions.add(new NeowRewardDef(NeowRewardType.REMOVE_TWO, TEXT[10]));
    }
    rewardOptions.add(new NeowRewardDef(NeowRewardType.ONE_RARE_RELIC, TEXT[11]));
    rewardOptions.add(new NeowRewardDef(NeowRewardType.THREE_RARE_CARDS, TEXT[12]));
    if (this.drawback != NeowRewardDrawback.NO_GOLD) {
        rewardOptions.add(new NeowRewardDef(NeowRewardType.TWO_FIFTY_GOLD,
                                            TEXT[13] + 'ú' + TEXT[14]));
    }
    rewardOptions.add(new NeowRewardDef(NeowRewardType.TRANSFORM_TWO_CARDS, TEXT[15]));
    if (this.drawback != NeowRewardDrawback.TEN_PERCENT_HP_LOSS) {
        rewardOptions.add(new NeowRewardDef(NeowRewardType.TWENTY_PERCENT_HP_BONUS,
                                            TEXT[16] + (this.hp_bonus * 2) + " ]"));
    }
    break;
```

**Pool**: 5-7 options (depends on drawback) → randomly selects 1

**Drawback Options**:
```java
private ArrayList<NeowRewardDrawbackDef> getRewardDrawbackOptions() {
    ArrayList<NeowRewardDrawbackDef> drawbackOptions = new ArrayList<>();
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.TEN_PERCENT_HP_LOSS,
        TEXT[17] + this.hp_bonus + TEXT[18]));
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.NO_GOLD, TEXT[19]));
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.CURSE, TEXT[20]));
    drawbackOptions.add(new NeowRewardDrawbackDef(
        NeowRewardDrawback.PERCENT_DAMAGE,
        TEXT[21] + (AbstractDungeon.player.currentHealth / 10 * 3) + TEXT[29] + " "));
    return drawbackOptions;
}
```

| Reward Type | Effect | Conditional | Localization Key |
|-------------|--------|-------------|------------------|
| RANDOM_COLORLESS_2 | Choose 1 from 3 rare colorless cards | Always | TEXT[31] |
| REMOVE_TWO | Remove 2 cards from deck | If drawback ≠ CURSE | TEXT[10] |
| ONE_RARE_RELIC | Obtain 1 random rare relic | Always | TEXT[11] |
| THREE_RARE_CARDS | Choose 1 from 3 rare cards | Always | TEXT[12] |
| TWO_FIFTY_GOLD | Gain 250 gold | If drawback ≠ NO_GOLD | TEXT[13] + 'ú' + TEXT[14] |
| TRANSFORM_TWO_CARDS | Transform 2 cards | Always | TEXT[15] |
| TWENTY_PERCENT_HP_BONUS | Gain max HP (20% of current max) | If drawback ≠ TEN_PERCENT_HP_LOSS | TEXT[16] + (hp_bonus*2) + " ]" |

### 3.4 Category 3: Boss Relic Swap

```java
case 3:
    rewardOptions.add(new NeowRewardDef(NeowRewardType.BOSS_RELIC, UNIQUE_REWARDS[0]));
    break;
```

**Pool**: 1 option (always selected)

## 4. Activation Process

### 4.1 Button Click Flow

```java
// In NeowEvent.buttonEffect()
case 3:  // Screen showing 4 reward options
    dismissBubble();
    this.roomEventText.clearRemainingOptions();

    switch (buttonPressed) {
        case 0: this.rewards.get(0).activate(); break;
        case 1: this.rewards.get(1).activate(); break;
        case 2: this.rewards.get(2).activate(); break;
        case 3: this.rewards.get(3).activate(); break;
    }

    this.screenNum = 99;  // End state
    this.roomEventText.updateDialogOption(0, OPTIONS[3]);  // "Leave"
    waitingToSave = true;
```

### 4.2 Drawback Application

**Drawbacks are applied BEFORE rewards** in `activate()`:

```java
public void activate() {
    this.activated = true;

    // Apply drawback first
    switch (this.drawback) {
        case CURSE:
            this.cursed = true;  // Applied in update() after screen closes
            break;
        case NO_GOLD:
            AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
            break;
        case TEN_PERCENT_HP_LOSS:
            AbstractDungeon.player.decreaseMaxHealth(this.hp_bonus);
            break;
        case PERCENT_DAMAGE:
            AbstractDungeon.player.damage(new DamageInfo(null,
                AbstractDungeon.player.currentHealth / 10 * 3,
                DamageInfo.DamageType.HP_LOSS));
            break;
    }

    // Then apply reward (see section 4.3)
    // ...
}
```

### 4.3 Reward Implementation (Full activate() Switch)

```java
switch (this.type) {
    case RANDOM_COLORLESS:
        AbstractDungeon.cardRewardScreen.open(
            getColorlessRewardCards(true), null,
            CardCrawlGame.languagePack.getUIString("CardRewardScreen").TEXT[1]);
        break;

    case RANDOM_COLORLESS_2:
        AbstractDungeon.cardRewardScreen.open(
            getColorlessRewardCards(false), null,
            CardCrawlGame.languagePack.getUIString("CardRewardScreen").TEXT[1]);
        break;

    case THREE_RARE_CARDS:
        AbstractDungeon.cardRewardScreen.open(getRewardCards(true), null, TEXT[22]);
        break;

    case HUNDRED_GOLD:
        CardCrawlGame.sound.play("GOLD_JINGLE");
        AbstractDungeon.player.gainGold(100);
        break;

    case ONE_RANDOM_RARE_CARD:
        AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(
            AbstractDungeon.getCard(AbstractCard.CardRarity.RARE, NeowEvent.rng).makeCopy(),
            Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
        break;

    case RANDOM_COMMON_RELIC:
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
            Settings.WIDTH / 2, Settings.HEIGHT / 2,
            AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.COMMON));
        break;

    case ONE_RARE_RELIC:
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
            Settings.WIDTH / 2, Settings.HEIGHT / 2,
            AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.RARE));
        break;

    case BOSS_RELIC:
        AbstractDungeon.player.loseRelic(
            AbstractDungeon.player.relics.get(0).relicId);
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
            Settings.WIDTH / 2, Settings.HEIGHT / 2,
            AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
        break;

    case THREE_ENEMY_KILL:
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
            Settings.WIDTH / 2, Settings.HEIGHT / 2,
            new NeowsLament());
        break;

    case REMOVE_CARD:
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getPurgeableCards(),
            1, TEXT[23], false, false, false, true);
        break;

    case REMOVE_TWO:
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getPurgeableCards(),
            2, TEXT[24], false, false, false, false);
        break;

    case TEN_PERCENT_HP_BONUS:
        AbstractDungeon.player.increaseMaxHp(this.hp_bonus, true);
        break;

    case THREE_CARDS:
        AbstractDungeon.cardRewardScreen.open(
            getRewardCards(false), null,
            CardCrawlGame.languagePack.getUIString("CardRewardScreen").TEXT[1]);
        break;

    case THREE_SMALL_POTIONS:
        CardCrawlGame.sound.play("POTION_1");
        for (int i = 0; i < 3; i++) {
            AbstractDungeon.getCurrRoom().addPotionToRewards(
                PotionHelper.getRandomPotion());
        }
        AbstractDungeon.combatRewardScreen.open();
        AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.0F;
        // Remove card reward from potion screen
        int remove = -1;
        for (int j = 0; j < AbstractDungeon.combatRewardScreen.rewards.size(); j++) {
            if (AbstractDungeon.combatRewardScreen.rewards.get(j).type ==
                RewardItem.RewardType.CARD) {
                remove = j;
                break;
            }
        }
        if (remove != -1) {
            AbstractDungeon.combatRewardScreen.rewards.remove(remove);
        }
        break;

    case TRANSFORM_CARD:
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getPurgeableCards(),
            1, TEXT[25], false, true, false, false);
        break;

    case TRANSFORM_TWO_CARDS:
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getPurgeableCards(),
            2, TEXT[26], false, false, false, false);
        break;

    case TWENTY_PERCENT_HP_BONUS:
        AbstractDungeon.player.increaseMaxHp(this.hp_bonus * 2, true);
        break;

    case TWO_FIFTY_GOLD:
        CardCrawlGame.sound.play("GOLD_JINGLE");
        AbstractDungeon.player.gainGold(250);
        break;

    case UPGRADE_CARD:
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getUpgradableCards(),
            1, TEXT[27], true, false, false, false);
        break;
}

CardCrawlGame.metricData.addNeowData(this.type.name(), this.drawback.name());
```

### 4.4 Card/Grid Selection Handling

**Grid selections (transform, remove, upgrade) are handled asynchronously in `NeowReward.update()`**:

```java
public void update() {
    if (this.activated) {
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            switch (this.type) {
                case UPGRADE_CARD:
                    AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                    c.upgrade();
                    AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(
                        c.makeStatEquivalentCopy()));
                    AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(
                        Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                    break;

                case REMOVE_CARD:
                    CardCrawlGame.sound.play("CARD_EXHAUST");
                    AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(
                        AbstractDungeon.gridSelectScreen.selectedCards.get(0),
                        Settings.WIDTH / 2, Settings.HEIGHT / 2));
                    AbstractDungeon.player.masterDeck.removeCard(
                        AbstractDungeon.gridSelectScreen.selectedCards.get(0));
                    break;

                case REMOVE_TWO:
                    // Similar to REMOVE_CARD but for 2 cards
                    // Removes gridSelectScreen.selectedCards.get(0) and get(1)
                    break;

                case TRANSFORM_CARD:
                    AbstractDungeon.transformCard(
                        AbstractDungeon.gridSelectScreen.selectedCards.get(0),
                        false, NeowEvent.rng);
                    AbstractDungeon.player.masterDeck.removeCard(
                        AbstractDungeon.gridSelectScreen.selectedCards.get(0));
                    AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(
                        AbstractDungeon.getTransformedCard(),
                        Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                    break;

                case TRANSFORM_TWO_CARDS:
                    // Similar to TRANSFORM_CARD but for 2 cards
                    break;
            }

            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.overlayMenu.cancelButton.hide();
            SaveHelper.saveIfAppropriate(SaveFile.SaveType.POST_NEOW);
            this.activated = false;
        }

        // Curse is applied after all screens close
        if (this.cursed) {
            this.cursed = !this.cursed;
            AbstractDungeon.topLevelEffects.add(new ShowCardAndObtainEffect(
                AbstractDungeon.getCardWithoutRng(AbstractCard.CardRarity.CURSE),
                Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
        }
    }
}
```

## 5. Practical Modification Examples

### 5.1 Add New Reward Type

```java
@SpirePatch(
    clz = NeowReward.class,
    method = SpirePatch.CLASS
)
public class NewRewardTypePatch {
    public static SpireEnum NEW_REWARD_TYPE;
}

@SpirePatch(
    clz = NeowReward.class,
    method = "getRewardOptions"
)
public class AddNewRewardOptionPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(NeowReward __instance, ArrayList<NeowRewardDef> rewardOptions,
                              int category) {
        if (category == 1) {  // Add to category 1
            rewardOptions.add(new NeowRewardDef(
                NewRewardTypePatch.NEW_REWARD_TYPE,
                "Gain 5 random relics"  // Or use localization
            ));
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                ArrayList.class, "add");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}

@SpirePatch(
    clz = NeowReward.class,
    method = "activate"
)
public class ActivateNewRewardPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(NeowReward __instance) {
        if (__instance.type == NewRewardTypePatch.NEW_REWARD_TYPE) {
            // Implement new reward effect
            for (int i = 0; i < 5; i++) {
                AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
                    Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F,
                    AbstractDungeon.returnRandomRelic(
                        AbstractDungeon.returnRandomRelicTier()));
            }
            return SpireReturn.Return(null);  // Skip original switch
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                CardCrawlGame.class, "metricData");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 5.2 Modify Existing Reward Value

**Example: Change HUNDRED_GOLD to 200 gold**

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "activate"
)
public class ModifyGoldRewardPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(NeowReward __instance) {
        if (__instance.type == NeowRewardType.HUNDRED_GOLD) {
            CardCrawlGame.sound.play("GOLD_JINGLE");
            AbstractDungeon.player.gainGold(200);  // Changed from 100
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                NeowReward.class, "type");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 5.3 Change Number of Reward Options

**Example: Show 6 options instead of 4**

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "blessing"
)
public class SixRewardsPatch {
    public static void Postfix(NeowEvent __instance) {
        ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
            ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");

        // Add 2 more rewards
        rewards.add(new NeowReward(0));  // Category 0
        rewards.add(new NeowReward(1));  // Category 1

        // Add UI buttons for options 4 and 5
        RoomEventDialog roomEventText = (RoomEventDialog)
            ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");
        roomEventText.addDialogOption(rewards.get(4).optionLabel);
        roomEventText.addDialogOption(rewards.get(5).optionLabel);
    }
}

@SpirePatch(
    clz = NeowEvent.class,
    method = "buttonEffect"
)
public class SixRewardsButtonPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(NeowEvent __instance, int buttonPressed) {
        if (__instance.screenNum == 3 && buttonPressed >= 4 && buttonPressed <= 5) {
            ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
                ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");

            // Activate reward 4 or 5
            rewards.get(buttonPressed).activate();

            // Update dialogue
            RoomEventDialog roomEventText = (RoomEventDialog)
                ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");
            roomEventText.clearRemainingOptions();
            roomEventText.updateDialogOption(0, NeowEvent.OPTIONS[3]);

            ReflectionHacks.setPrivate(__instance, NeowEvent.class, "screenNum", 99);
            NeowEvent.waitingToSave = true;

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                NeowEvent.class, "screenNum");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 5.4 Add Conditional Rewards Based on Character

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "getRewardOptions"
)
public class CharacterSpecificRewardPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(NeowReward __instance, ArrayList<NeowRewardDef> rewardOptions,
                              int category) {
        if (category == 1) {
            // Ironclad-specific reward
            if (AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.IRONCLAD) {
                rewardOptions.add(new NeowRewardDef(
                    CustomRewardTypes.IRONCLAD_SPECIAL,
                    "Gain 3 Strength"
                ));
            }

            // Silent-specific reward
            if (AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
                rewardOptions.add(new NeowRewardDef(
                    CustomRewardTypes.SILENT_SPECIAL,
                    "Gain 3 Shivs and 2 Energy"
                ));
            }
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                ArrayList.class, "add");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 5.5 Adjust Reward Probabilities

**Example: Make rare rewards more common**

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "getRewardOptions"
)
public class RareRewardProbabilityPatch {
    public static ArrayList<NeowRewardDef> Postfix(ArrayList<NeowRewardDef> result,
                                                    NeowReward __instance, int category) {
        if (category == 2) {
            // Add duplicate rare relic option to increase probability
            for (int i = 0; i < 3; i++) {  // Add 3 times instead of 1
                result.add(new NeowRewardDef(
                    NeowRewardType.ONE_RARE_RELIC,
                    NeowReward.TEXT[11]
                ));
            }
        }
        return result;
    }
}
```

### 5.6 Show All Rewards (Debug Mode)

**Removes randomness and shows all possible rewards**

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "blessing"
)
public class ShowAllRewardsPatch {
    public static void Replace(NeowEvent __instance) {
        if (!Settings.isDebug) {
            // Call original method if not in debug mode
            return;
        }

        NeowEvent.rng = new Random(Settings.seed);
        AbstractDungeon.bossCount = 0;

        ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
            ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");
        rewards.clear();

        // Create one reward from each category
        rewards.add(new NeowReward(0));  // Card rewards
        rewards.add(new NeowReward(1));  // Small rewards
        rewards.add(new NeowReward(2));  // Large rewards with drawback
        rewards.add(new NeowReward(3));  // Boss relic swap

        // Alternative: Show specific rewards
        // rewards.add(createSpecificReward(NeowRewardType.ONE_RARE_RELIC));
        // rewards.add(createSpecificReward(NeowRewardType.REMOVE_CARD));
        // etc.

        RoomEventDialog roomEventText = (RoomEventDialog)
            ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");

        roomEventText.clearRemainingOptions();
        roomEventText.updateDialogOption(0, rewards.get(0).optionLabel);
        roomEventText.addDialogOption(rewards.get(1).optionLabel);
        roomEventText.addDialogOption(rewards.get(2).optionLabel);
        roomEventText.addDialogOption(rewards.get(3).optionLabel);

        ReflectionHacks.setPrivate(__instance, NeowEvent.class, "screenNum", 3);
    }

    private static NeowReward createSpecificReward(NeowRewardType type) {
        NeowReward reward = new NeowReward(0);  // Dummy category
        ReflectionHacks.setPrivate(reward, NeowReward.class, "type", type);
        // Set optionLabel manually or use localization
        return reward;
    }
}
```

## 6. Advanced Modifications

### 6.1 Multi-Stage Rewards

**Example: "Choose a reward, then choose another"**

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "buttonEffect"
)
public class MultiStageRewardPatch {
    private static boolean secondStage = false;

    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(NeowEvent __instance, int buttonPressed) {
        if (__instance.screenNum == 3 && !secondStage) {
            // First stage: activate reward
            ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
                ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");
            rewards.get(buttonPressed).activate();

            // Set up second stage
            secondStage = true;
            rewards.clear();

            // Create new set of rewards for stage 2
            rewards.add(new NeowReward(0));
            rewards.add(new NeowReward(1));

            RoomEventDialog roomEventText = (RoomEventDialog)
                ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");
            roomEventText.clearRemainingOptions();
            roomEventText.updateDialogOption(0, "Second reward: " + rewards.get(0).optionLabel);
            roomEventText.addDialogOption("Second reward: " + rewards.get(1).optionLabel);
            roomEventText.addDialogOption("No thanks, I'm done");

            return SpireReturn.Return(null);
        } else if (__instance.screenNum == 3 && secondStage) {
            // Second stage: activate second reward or skip
            if (buttonPressed < 2) {
                ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
                    ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");
                rewards.get(buttonPressed).activate();
            }

            // End event
            secondStage = false;
            RoomEventDialog roomEventText = (RoomEventDialog)
                ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");
            roomEventText.clearRemainingOptions();
            roomEventText.updateDialogOption(0, NeowEvent.OPTIONS[3]);

            ReflectionHacks.setPrivate(__instance, NeowEvent.class, "screenNum", 99);
            NeowEvent.waitingToSave = true;

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                NeowEvent.class, "screenNum");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 6.2 Dynamic Rewards Based on Game State

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "getRewardOptions"
)
public class DynamicRewardPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(NeowReward __instance, ArrayList<NeowRewardDef> rewardOptions,
                              int category) {
        if (category == 1) {
            // If player has many strikes/defends, offer card removal
            int basicCards = 0;
            for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                if (c.cardID.equals("Strike_R") || c.cardID.equals("Strike_G") ||
                    c.cardID.equals("Strike_B") || c.cardID.equals("Strike_P") ||
                    c.cardID.equals("Defend_R") || c.cardID.equals("Defend_G") ||
                    c.cardID.equals("Defend_B") || c.cardID.equals("Defend_P")) {
                    basicCards++;
                }
            }

            if (basicCards >= 10) {
                rewardOptions.add(new NeowRewardDef(
                    NeowRewardType.REMOVE_CARD,
                    "Remove a card (you have many basic cards)"
                ));
            }

            // If player has low HP, offer HP bonus
            if (AbstractDungeon.player.maxHealth < 50) {
                rewardOptions.add(new NeowRewardDef(
                    NeowRewardType.TEN_PERCENT_HP_BONUS,
                    "Gain max HP (you have low HP)"
                ));
            }
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                ArrayList.class, "add");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 6.3 Prevent Duplicate Reward Categories

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "blessing"
)
public class NoDuplicateCategoriesPatch {
    public static void Postfix(NeowEvent __instance) {
        ArrayList<NeowReward> rewards = (ArrayList<NeowReward>)
            ReflectionHacks.getPrivate(__instance, NeowEvent.class, "rewards");

        // Track used categories
        HashSet<Integer> usedCategories = new HashSet<>();

        // Rebuild rewards ensuring no duplicates
        ArrayList<NeowReward> newRewards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int category;
            do {
                category = NeowEvent.rng.random(0, 2);  // 0, 1, or 2
            } while (usedCategories.contains(category));

            usedCategories.add(category);
            newRewards.add(new NeowReward(category));
        }

        // Add boss relic swap as 4th option
        newRewards.set(3, new NeowReward(3));

        // Replace rewards
        rewards.clear();
        rewards.addAll(newRewards);

        // Update UI
        RoomEventDialog roomEventText = (RoomEventDialog)
            ReflectionHacks.getPrivate(__instance, AbstractEvent.class, "roomEventText");
        roomEventText.clearRemainingOptions();
        for (int i = 0; i < 4; i++) {
            if (i == 0) {
                roomEventText.updateDialogOption(0, rewards.get(i).optionLabel);
            } else {
                roomEventText.addDialogOption(rewards.get(i).optionLabel);
            }
        }
    }
}
```

### 6.4 Custom UI for Rewards

```java
@SpirePatch(
    clz = LargeDialogOptionButton.class,
    method = "render"
)
public class CustomRewardButtonPatch {
    public static void Prefix(LargeDialogOptionButton __instance, SpriteBatch sb) {
        // Change button color based on reward quality
        String msg = ReflectionHacks.getPrivate(__instance,
            LargeDialogOptionButton.class, "msg");

        if (msg.contains("Rare Relic") || msg.contains("Boss Relic")) {
            // Gold border for rare/boss relics
            Color boxColor = ReflectionHacks.getPrivate(__instance,
                LargeDialogOptionButton.class, "boxColor");
            boxColor.set(1.0f, 0.84f, 0.0f, boxColor.a);  // Gold
        } else if (msg.contains("Curse") || msg.contains("damage")) {
            // Red border for drawbacks
            Color boxColor = ReflectionHacks.getPrivate(__instance,
                LargeDialogOptionButton.class, "boxColor");
            boxColor.set(1.0f, 0.2f, 0.2f, boxColor.a);  // Red
        }
    }
}
```

## 7. Important Notes

### 7.1 Localization

All reward text uses localization keys from `CharacterStrings` with ID "Neow Reward":

```java
private static final CharacterStrings characterStrings =
    CardCrawlGame.languagePack.getCharacterString("Neow Reward");
public static final String[] TEXT = characterStrings.TEXT;
public static final String[] UNIQUE_REWARDS = characterStrings.UNIQUE_REWARDS;
```

**Localization file location**: `localization/eng/characters.json` (or other language codes)

**Example entry**:
```json
{
  "Neow Reward": {
    "NAMES": ["Neow"],
    "TEXT": [
      "Choose a card to add to your deck.",
      "Obtain a random rare card.",
      "Remove a card from your deck.",
      // ... 31+ entries
    ],
    "UNIQUE_REWARDS": [
      "Transform your starting Relic into a random Boss Relic."
    ]
  }
}
```

### 7.2 RNG Seeding

```java
public static Random rng = null;

// Initialized in blessing() and dailyBlessing()
rng = new Random(Settings.seed);
```

- **Deterministic**: Same seed = same rewards
- **Used for**: Card generation, relic selection, reward option selection
- **Important**: Modifying RNG calls changes reward sequence

### 7.3 Boss Kill Counter

```java
// In NeowEvent constructor
if (Settings.isStandardRun() || (Settings.isEndless && AbstractDungeon.floorNum <= 1)) {
    this.bossCount = CardCrawlGame.playerPref.getInteger(
        AbstractDungeon.player.chosenClass.name() + "_SPIRITS", 0);
    AbstractDungeon.bossCount = this.bossCount;
}

// bossCount == 0 → miniBlessing() (2 options)
// bossCount > 0  → blessing() (4 options)
```

### 7.4 Mini vs Full Blessing

**Mini Blessing** (first win with character):
- 2 options only
- Option 1: `THREE_ENEMY_KILL` (Neow's Lament)
- Option 2: `TEN_PERCENT_HP_BONUS`

**Full Blessing** (subsequent wins):
- 4 options from categories 0, 1, 2, 3

### 7.5 Save Points

```java
// After reward selection
SaveHelper.saveIfAppropriate(SaveFile.SaveType.POST_NEOW);

// Delayed save for relic animations
NeowEvent.waitingToSave = true;  // Triggers save when animations complete
```

### 7.6 UI Constraints

**LargeDialogOptionButton**:
- Maximum width: 890 pixels (scaled)
- Height: 77 pixels (scaled)
- Position: Calculated by `calculateY(int size)`
- Layout: Vertical stack with spacing `-82.0F * Settings.scale`

**Practical limit**: ~8 options before overlapping UI elements

### 7.7 Screen State Machine

```java
private int screenNum = 2;

// screenNum values:
// 0: First-time intro dialogue
// 1: Returning dialogue
// 2: Initial state (unused after init)
// 3: Showing reward options
// 10: Daily/modded blessing
// 99: End state (show "Leave" button)
// 999: Endless mode blight application
```

### 7.8 Metrics Tracking

```java
// Called at end of activate()
CardCrawlGame.metricData.addNeowData(this.type.name(), this.drawback.name());
```

This records which reward was chosen for analytics/run history.

## 8. Common Pitfalls

### 8.1 Forgetting to Clear Screens

**Problem**: Multiple card/grid selection screens overlap

**Solution**: Always call `clear()` or `hide()` before opening new screens:
```java
AbstractDungeon.gridSelectScreen.selectedCards.clear();
AbstractDungeon.overlayMenu.cancelButton.hide();
```

### 8.2 Incorrect Screen Wait Logic

**Problem**: Reward effects trigger before screen closes

**Solution**: Use `activated` flag and check in `update()`:
```java
// In activate()
this.activated = true;
AbstractDungeon.gridSelectScreen.open(...);

// In update()
if (this.activated && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
    // Process selection
    this.activated = false;
}
```

### 8.3 Not Handling Purgeable Cards

**Problem**: Crashes when trying to remove/transform curse-only decks

**Solution**: Use `getPurgeableCards()`:
```java
AbstractDungeon.gridSelectScreen.open(
    AbstractDungeon.player.masterDeck.getPurgeableCards(),  // Filters non-purgeable
    1, TEXT[23], false, false, false, true);
```

### 8.4 Reward Pool Size Mismatch

**Problem**: Adding rewards to pool but not accounting for random selection

**Solution**: Remember rewards are **randomly selected from pool**, not all shown:
```java
// Category 0 has 6 options in pool
// But only 1 is randomly selected for display
ArrayList<NeowRewardDef> rewardOptions = new ArrayList<>();
rewardOptions.add(...);  // 6 total
// ...
NeowRewardDef reward = rewardOptions.get(
    NeowEvent.rng.random(0, rewardOptions.size() - 1));  // Picks 1
```

### 8.5 Modifying Active Reward List During Iteration

**Problem**: ConcurrentModificationException when modifying rewards list

**Solution**: Create copy before modification:
```java
ArrayList<NeowReward> rewardsCopy = new ArrayList<>(rewards);
for (NeowReward r : rewardsCopy) {
    // Safe to modify original rewards list here
}
```

### 8.6 HP Bonus Calculation Timing

**Problem**: hp_bonus calculated in constructor, but max HP might change

**Solution**: Recalculate if needed or patch constructor:
```java
// Original calculation
this.hp_bonus = (int)(AbstractDungeon.player.maxHealth * 0.1F);

// If max HP changes before activation, recalculate:
int actualBonus = (int)(AbstractDungeon.player.maxHealth * 0.1F);
AbstractDungeon.player.increaseMaxHp(actualBonus, true);
```

### 8.7 Text Localization Missing Color Codes

**Problem**: Disabled options don't strip color codes properly

**Solution**: Use `stripColor()` for disabled options:
```java
if (isDisabled) {
    this.msg = stripColor(msg);  // Removes #r, #g, #b, #y
} else {
    this.msg = msg;
}
```

## 9. Testing Tips

### 9.1 Enable Debug Mode

```java
// Add to your mod's initialization
Settings.isDebug = true;
Settings.isTestingNeow = true;  // Forces full blessing even on first win
```

### 9.2 Force Specific RNG Seed

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "blessing"
)
public class FixedSeedPatch {
    public static void Prefix(NeowEvent __instance) {
        NeowEvent.rng = new Random(12345L);  // Fixed seed for testing
    }
}
```

### 9.3 Log Reward Generation

```java
@SpirePatch(
    clz = NeowReward.class,
    method = "<init>"
)
public class LogRewardPatch {
    public static void Postfix(NeowReward __instance, int category) {
        System.out.println("Created reward - Category: " + category +
                           ", Type: " + __instance.type +
                           ", Drawback: " + __instance.drawback);
    }
}
```

### 9.4 Skip Neow Dialogue

```java
@SpirePatch(
    clz = NeowEvent.class,
    method = "<init>"
)
public class SkipDialoguePatch {
    public static void Postfix(NeowEvent __instance, boolean isDone) {
        if (!isDone) {
            // Force skip to blessing screen
            ReflectionHacks.setPrivate(__instance, NeowEvent.class, "screenNum", 1);
        }
    }
}
```

---

## Summary

**Key Takeaways**:
1. **4 reward categories** (0-3) → each creates 1 `NeowReward` → 4 total options
2. **Categories have reward pools** → 1 reward randomly selected from pool
3. **Category 2 has drawbacks** → randomly selected before reward
4. **Activation is 2-phase**: drawback first, then reward
5. **Grid selections are async**: handled in `update()`, not `activate()`
6. **RNG is seeded** → deterministic with same seed
7. **Mini vs Full blessing** depends on boss kill count
8. **UI shows max ~8 options** before overlap issues

**Common Modifications**:
- Add new reward types → patch enum + getRewardOptions + activate
- Change reward values → patch activate() switch cases
- Add more options → patch blessing() + buttonEffect()
- Make conditional rewards → patch getRewardOptions() with game state checks
- Custom UI → patch LargeDialogOptionButton render methods
