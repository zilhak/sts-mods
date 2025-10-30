# Custom Relics Mod

Adds unique and powerful custom relics to expand your strategic options in Slay the Spire.

## Features

- **Custom Relics**: New relics with unique effects
- **Balanced Design**: Carefully tuned to complement existing relics
- **Extensible**: Easy to add more relics

## Current Relics

### Lucky Coin (Common)
**Effect**: At the start of each combat, gain [E] and draw 1 card.

A simple but powerful starter relic that provides early combat advantage.

## Building

From the project root:
```bash
mvn clean package -pl custom-relics
```

Output: `target/CustomRelics.jar`

## Installation

1. Build the mod (see above)
2. Copy JAR to ModTheSpire mods folder:
   - Windows: `%LOCALAPPDATA%\ModTheSpire\mods\`
   - Linux/Mac: `~/.config/ModTheSpire/mods/`
3. Enable in ModTheSpire launcher

## Development

### Project Structure
```
custom-relics/
├── src/main/
│   ├── java/com/stsmod/relics/
│   │   ├── CustomRelicsMod.java      # Main mod class
│   │   └── relics/
│   │       └── ExampleRelic.java     # Example relic implementation
│   └── resources/
│       ├── ModTheSpire.json          # Mod metadata
│       ├── localization/
│       │   └── RelicStrings.json     # Relic text/descriptions
│       └── images/relics/
│           └── lucky_coin.png        # Relic images (128x128)
```

### Adding New Relics

#### 1. Create Relic Class

Create a new file in `src/main/java/com/stsmod/relics/relics/`:

```java
package com.stsmod.relics.relics;

import basemod.abstracts.CustomRelic;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.stsmod.relics.CustomRelicsMod;

public class MyNewRelic extends CustomRelic {
    public static final String ID = CustomRelicsMod.makeID("MyNewRelic");

    private static final String IMG = "my_new_relic.png";
    private static final String OUTLINE = "my_new_relic_outline.png";

    private static final RelicTier TIER = RelicTier.UNCOMMON;
    private static final LandingSound SOUND = LandingSound.MAGICAL;

    public MyNewRelic() {
        super(ID, IMG, OUTLINE, TIER, SOUND);
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    // Add your relic effect methods here
    // Common hooks:
    // - atBattleStart()
    // - atTurnStart()
    // - onPlayerEndTurn()
    // - onVictory()
    // - onEquip()

    @Override
    public AbstractRelic makeCopy() {
        return new MyNewRelic();
    }
}
```

#### 2. Add Localization

Add to `src/main/resources/localization/RelicStrings.json`:

```json
{
  "customrelics:MyNewRelic": {
    "NAME": "My New Relic",
    "FLAVOR": "Flavor text for your relic.",
    "DESCRIPTIONS": [
      "Description of what the relic does. Use #b for bold numbers."
    ]
  }
}
```

#### 3. Register Relic

In `CustomRelicsMod.java`, add to `receiveEditRelics()`:

```java
@Override
public void receiveEditRelics() {
    BaseMod.addRelic(new ExampleRelic(), AbstractRelic.RelicTier.COMMON);
    BaseMod.addRelic(new MyNewRelic(), AbstractRelic.RelicTier.UNCOMMON);  // Add this
}
```

#### 4. Add Images

Place relic images in `src/main/resources/images/relics/`:
- Main image: 128x128 pixels
- Outline: 128x128 pixels (for colorblind mode)

### Relic Tiers

```java
RelicTier.COMMON    // Common relics (white)
RelicTier.UNCOMMON  // Uncommon relics (blue)
RelicTier.RARE      // Rare relics (purple)
RelicTier.BOSS      // Boss relics (red)
RelicTier.SHOP      // Shop-only relics
RelicTier.SPECIAL   // Special relics (events, etc.)
RelicTier.STARTER   // Starting relics
```

### Common Relic Hooks

```java
// Combat Events
atBattleStart()           // Start of combat
atTurnStart()             // Start of player turn
onPlayerEndTurn()         // End of player turn
onVictory()               // After winning combat

// Card Events
onPlayCard(card, monster) // When playing a card
onUseCard(card, action)   // When card effect resolves
onExhaust(card)           // When card is exhausted

// Monster Events
onMonsterDeath(monster)   // When any monster dies
onAttack(damage, enemy)   // When dealing damage
onAttacked(damage, enemy) // When taking damage

// Other
onEquip()                 // When relic is obtained
onUnequip()               // When relic is removed
onTrigger()               // Generic trigger method
```

## Relic Ideas (TODO)

- [ ] **Mana Crystal**: Gain an extra energy every 3 turns
- [ ] **Phoenix Feather**: Revive once per combat with 50% HP
- [ ] **Gambler's Dice**: Randomize card costs at start of combat
- [ ] **Time Warp**: Replay the last card played this turn
- [ ] **Soul Gem**: Gain 1 HP for every 10 damage dealt

## Dependencies

- **ModTheSpire**: 3.29.3+
- **BaseMod**: 5.48.0+
- **StSLib**: 2.3.0+ (optional, for advanced mechanics)

## Contributing

Feel free to:
- Create new relics
- Balance existing relics
- Improve relic artwork
- Add more localization languages

## License

Provided as-is for modding purposes.
