# Ascension 100 Mod

Extends Slay the Spire's Ascension mode from level 20 to level 100 with progressively harder challenges.

## Features

- **Extended Ascension Levels**: Climb from level 21 to 100
- **Progressive Difficulty**: Each level adds unique modifiers
- **Compatible**: Works with base game characters and other mods

## Planned Mechanics

### Levels 21-40: Enhanced Basics
- Increased enemy health and damage
- Reduced starting gold
- More aggressive elite spawns

### Levels 41-60: Advanced Challenges
- Modified card costs
- Reduced healing effectiveness
- Tougher boss mechanics

### Levels 61-80: Expert Mode
- Limited card draws
- Energy restrictions
- Enhanced enemy abilities

### Levels 81-100: Nightmare Difficulty
- Extreme modifiers
- Combined challenges from previous tiers
- Ultimate test of skill

## Building

From the project root:
```bash
mvn clean package -pl ascension-100
```

Output: `target/Ascension100.jar`

## Installation

1. Build the mod (see above)
2. Copy JAR to ModTheSpire mods folder:
   - Windows: `%LOCALAPPDATA%\ModTheSpire\mods\`
   - Linux/Mac: `~/.config/ModTheSpire/mods/`
3. Enable in ModTheSpire launcher

## Development

### Main Class
`com.stsmod.ascension100.Ascension100Mod`

### Key Files
- `ModTheSpire.json`: Mod metadata
- `Ascension100Mod.java`: Main mod initialization
- `localization/UIStrings.json`: Text and descriptions

### Adding New Ascension Levels

To implement actual ascension level modifications:

1. Subscribe to relevant hooks (e.g., `PostBattleSubscriber`)
2. Check `AbstractDungeon.ascensionLevel`
3. Apply modifiers based on level ranges
4. Use patches to modify game behavior

Example:
```java
@Override
public void receivePostBattle(AbstractRoom room) {
    if (AbstractDungeon.ascensionLevel > 20) {
        int extraLevel = AbstractDungeon.ascensionLevel - 20;
        // Apply modifications based on extraLevel
    }
}
```

## Dependencies

- **ModTheSpire**: 3.29.3+
- **BaseMod**: 5.48.0+
- **Slay the Spire**: Latest version

## TODO

- [ ] Implement ascension level modifiers for 21-100
- [ ] Add configuration panel for customization
- [ ] Create unique challenges for each tier
- [ ] Add achievements for high ascension clears
- [ ] Implement save/load for extended ascension progress

## Contributing

Feel free to:
- Add new ascension modifiers
- Balance difficulty curves
- Suggest new mechanics

## License

Provided as-is for modding purposes.
