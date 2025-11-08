# Dungeon Initialization System

**Critical Reference for Dungeon Re-entry/Reverse Traversal Mods**

This document analyzes the dungeon initialization process in Slay the Spire to understand what causes crashes when implementing "floor backtracking" features.

---

## Table of Contents

1. [Dungeon Constructor Flow](#1-dungeon-constructor-flow)
2. [AbstractDungeon.reset() - Complete Analysis](#2-abstractdungeonreset---complete-analysis)
3. [RNG Seed Initialization](#3-rng-seed-initialization)
4. [Map Generation Process](#4-map-generation-process)
5. [Boss Selection Process](#5-boss-selection-process)
6. [nextRoomTransition() - Per-Floor Cleanup](#6-nextroomtransition---per-floor-cleanup)
7. [Critical Issues for Dungeon Re-entry](#7-critical-issues-for-dungeon-re-entry)
8. [Modification Guide](#8-modification-guide)

---

## 1. Dungeon Constructor Flow

### 1.1 First Act Constructor (Exordium Example)

**File:** `com.megacrit.cardcrawl.dungeons.Exordium.java`

```java
public Exordium(AbstractPlayer p, ArrayList<String> emptyList) {
    super(NAME, "Exordium", p, emptyList);  // Calls AbstractDungeon constructor

    initializeRelicList();

    // Blight pool handling
    if (Settings.isEndless) {
        if (floorNum <= 1) {
            blightPool.clear();
            blightPool = new ArrayList<>();
        }
    } else {
        blightPool.clear();
    }

    // Scene setup
    if (scene != null) {
        scene.dispose();  // CRITICAL: Dispose old scene
    }
    scene = new TheBottomScene();
    scene.randomizeScene();
    fadeColor = Color.valueOf("1e0f0aff");
    sourceFadeColor = Color.valueOf("1e0f0aff");

    // Event initialization
    initializeSpecialOneTimeEventList();
    initializeLevelSpecificChances();

    // Map generation with RNG
    mapRng = new Random(Settings.seed + actNum);  // ACT-SPECIFIC SEED
    generateMap();

    // Music
    CardCrawlGame.music.changeBGM(id);

    // Starting room
    currMapNode = new MapRoomNode(0, -1);
    if (Settings.isShowBuild || !TipTracker.tips.get("NEOW_SKIP")) {
        currMapNode.room = new EmptyRoom();
    } else {
        currMapNode.room = new NeowRoom(false);
        SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
    }
}
```

### 1.2 AbstractDungeon Base Constructor

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 330-376)

```java
public AbstractDungeon(String name, String levelId, AbstractPlayer p,
                       ArrayList<String> newSpecialOneTimeEventList) {
    ascensionCheck = UnlockTracker.isAscensionUnlocked(p);
    CardCrawlGame.dungeon = this;  // Set global dungeon instance

    this.name = name;
    id = levelId;
    player = p;
    topPanel.setPlayerName();

    // Core systems
    actionManager = new GameActionManager();
    overlayMenu = new OverlayMenu(p);
    dynamicBanner = new DynamicBanner();
    unlocks.clear();

    specialOneTimeEventList = newSpecialOneTimeEventList;

    // State initialization
    isFadingIn = false;
    isFadingOut = false;
    waitingOnFadeOut = false;
    fadeTimer = 1.0F;
    isDungeonBeaten = false;
    isScreenUp = false;

    // CRITICAL: Initialization order
    dungeonTransitionSetup();    // Step 1: Increment actNum, heal player, clear lists
    generateMonsters();           // Step 2: Populate monster/elite lists
    initializeBoss();            // Step 3: Select boss for this act
    setBoss(bossList.get(0));    // Step 4: Load boss graphics
    initializeEventList();       // Step 5: Populate event pool
    initializeEventImg();        // Step 6: Load event graphics
    initializeShrineList();      // Step 7: Populate shrine pool
    initializeCardPools();       // Step 8: Initialize card reward pools

    if (floorNum == 0) {
        p.initializeStarterDeck();  // Only on game start
    }

    initializePotions();         // Step 9: Set potion slots
    BlightHelper.initialize();   // Step 10: Blight system

    // Screen state
    if (id.equals("Exordium")) {
        screen = CurrentScreen.NONE;
        isScreenUp = false;
    } else {
        screen = CurrentScreen.MAP;
        isScreenUp = true;
    }
}
```

**⚠️ CRITICAL ORDER:** The initialization sequence cannot be reordered:
1. `dungeonTransitionSetup()` must run first (increments actNum, clears lists)
2. Monster generation depends on cleared lists
3. Boss selection uses `monsterRng` seeded during `generateSeeds()`
4. Map generation uses `mapRng` seeded after level-specific chances

---

## 2. AbstractDungeon.reset() - Complete Analysis

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 3173-3214)

### 2.1 Full Method Implementation

```java
public static void reset() {
    logger.info("Resetting variables...");

    // Score/stats reset
    CardCrawlGame.resetScoreVars();
    ModHelper.setModsFalse();

    // Floor/Act counters
    floorNum = 0;
    actNum = 0;

    // Room cleanup - CRITICAL FOR MEMORY
    if (currMapNode != null && getCurrRoom() != null) {
        getCurrRoom().dispose();  // Dispose current room
        if (getCurrRoom().monsters != null) {
            for (AbstractMonster m : getCurrRoom().monsters.monsters) {
                m.dispose();  // Dispose all monsters
            }
        }
    }

    // Map state
    currMapNode = null;

    // Event/shrine pools
    shrineList.clear();
    relicsToRemoveOnStart.clear();

    // Screen state
    previousScreen = null;

    // Action system
    actionManager.clear();
    actionManager.clearNextRoomCombatActions();

    // Reward screens
    combatRewardScreen.clear();
    cardRewardScreen.reset();

    // Map screen
    if (dungeonMapScreen != null) {
        dungeonMapScreen.closeInstantly();
    }

    // Visual effects - ALL MUST BE CLEARED
    effectList.clear();
    effectsQueue.clear();
    topLevelEffectsQueue.clear();
    topLevelEffects.clear();

    // Card rendering state
    cardBlizzRandomizer = cardBlizzStartOffset;

    // Player relics
    if (player != null) {
        player.relics.clear();
    }

    // Render state
    rs = RenderScene.NORMAL;

    // Blight system
    blightPool.clear();
}
```

### 2.2 What reset() Does NOT Clear

**⚠️ CRITICAL OMISSIONS** - These are NOT reset and will cause issues:

```java
// NOT CLEARED - Will persist between runs
map                      // Dungeon map structure
pathX, pathY            // Path taken through dungeon
monsterList             // Remaining monster encounters
eliteMonsterList        // Remaining elite encounters
bossList                // Boss pool
bossKey                 // Current boss selection
eventList               // Event pool
specialOneTimeEventList // One-time events
player.masterDeck       // Player's deck
player.relics           // Already cleared above, but important
player.potions          // Potion slots
player.gold             // Player gold
commonRelicPool         // Relic pools
uncommonRelicPool
rareRelicPool
shopRelicPool
bossRelicPool

// RNG States - NOT RESET
monsterRng
eventRng
merchantRng
cardRng
treasureRng
relicRng
potionRng
aiRng
shuffleRng
cardRandomRng
miscRng
mapRng
```

**💀 DANGER:** If you call `reset()` mid-dungeon, RNG states are not reset, so events/monsters will continue their sequence.

---

## 3. RNG Seed Initialization

### 3.1 Initial Seed Generation

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 473-487)

```java
public static void generateSeeds() {
    logger.info("Generating seeds: " + Settings.seed);

    // All RNGs initialize from the same base seed
    monsterRng = new Random(Settings.seed);
    eventRng = new Random(Settings.seed);
    merchantRng = new Random(Settings.seed);
    cardRng = new Random(Settings.seed);
    treasureRng = new Random(Settings.seed);
    relicRng = new Random(Settings.seed);
    monsterHpRng = new Random(Settings.seed);
    potionRng = new Random(Settings.seed);
    aiRng = new Random(Settings.seed);
    shuffleRng = new Random(Settings.seed);
    cardRandomRng = new Random(Settings.seed);
    miscRng = new Random(Settings.seed);
}
```

**⚠️ CRITICAL:** `generateSeeds()` is called once at game start. It is NOT called when entering new acts.

### 3.2 Per-Floor RNG Re-seeding

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 2205-2209)

Called in `nextRoomTransition()` after incrementing `floorNum`:

```java
monsterHpRng = new Random(Settings.seed + floorNum);
aiRng = new Random(Settings.seed + floorNum);
shuffleRng = new Random(Settings.seed + floorNum);
cardRandomRng = new Random(Settings.seed + floorNum);
miscRng = new Random(Settings.seed + floorNum);
```

**⚠️ CRITICAL:** Only 5 RNGs are re-seeded per floor. The main RNGs (monsterRng, eventRng, etc.) maintain state across floors using counter advancement.

### 3.3 Per-Act Map RNG

**Different seed calculation per act:**

```java
// Act 1 (Exordium)
mapRng = new Random(Settings.seed + actNum);

// Act 2 (TheCity)
mapRng = new Random(Settings.seed + (actNum * 100));

// Act 3 (TheBeyond)
mapRng = new Random(Settings.seed + (actNum * 200));
```

**💀 DANGER:** If you re-enter an act, the map will be identical unless you modify the seed calculation.

---

## 4. Map Generation Process

### 4.1 generateMap() Overview

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 619-650)

```java
protected static void generateMap() {
    long startTime = System.currentTimeMillis();

    // Map dimensions
    int mapHeight = 15;
    int mapWidth = 7;
    int mapPathDensity = 6;  // Affects path diversity

    ArrayList<AbstractRoom> roomList = new ArrayList<>();

    // Generate map structure
    map = MapGenerator.generateDungeon(mapHeight, mapWidth, mapPathDensity, mapRng);

    // Count valid nodes (excluding boss row and disconnected nodes)
    int count = 0;
    for (ArrayList<MapRoomNode> a : map) {
        for (MapRoomNode n : a) {
            if (!n.hasEdges() || n.y == map.size() - 2) {
                continue;
            }
            count++;
        }
    }

    // Assign room types
    generateRoomTypes(roomList, count);
    RoomTypeAssigner.assignRowAsRoomType(map.get(map.size() - 1), RestRoom.class);  // Row 14: Rest
    RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);             // Row 0: Combat

    // Special: Endless mode elite row
    if (Settings.isEndless && player.hasBlight("MimicInfestation")) {
        RoomTypeAssigner.assignRowAsRoomType(map.get(8), MonsterRoomElite.class);
    } else {
        RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class);  // Row 8: Treasure
    }

    // Boss row assignment handled separately
}
```

### 4.2 MapGenerator.generateDungeon()

**File:** `com.megacrit.cardcrawl.map.MapGenerator.java` (Line 37-46)

```java
public static ArrayList<ArrayList<MapRoomNode>> generateDungeon(int height, int width,
                                                                  int pathDensity, Random rng) {
    ArrayList<ArrayList<MapRoomNode>> map = createNodes(height, width);

    // Path density affects branching
    if (ModHelper.isModEnabled("Uncertain Future")) {
        map = createPaths(map, 1, rng);  // Linear path
    } else {
        map = createPaths(map, pathDensity, rng);  // Standard branching
    }

    map = filterRedundantEdgesFromRow(map);
    return map;
}
```

**⚠️ RNG DEPENDENCY:** The map structure depends entirely on `mapRng` state. Same seed = same map.

---

## 5. Boss Selection Process

### 5.1 Boss Initialization (Act 1 Example)

**File:** `com.megacrit.cardcrawl.dungeons.Exordium.java` (Line 218-258)

```java
protected void initializeBoss() {
    bossList.clear();

    // Daily run: Always all three bosses
    if (Settings.isDailyRun) {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    // First time seeing each boss - sequential unlock
    else if (!UnlockTracker.isBossSeen("GUARDIAN")) {
        bossList.add("The Guardian");
    } else if (!UnlockTracker.isBossSeen("GHOST")) {
        bossList.add("Hexaghost");
    } else if (!UnlockTracker.isBossSeen("SLIME")) {
        bossList.add("Slime Boss");
    }
    // All bosses unlocked - random selection
    else {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // Safety checks
    if (bossList.size() == 1) {
        bossList.add(bossList.get(0));  // Duplicate if only one
    } else if (bossList.isEmpty()) {
        logger.warn("Boss list was empty. How?");
        // Add all bosses as fallback
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // Demo mode override
    if (Settings.isDemo) {
        bossList.clear();
        bossList.add("Hexaghost");
    }
}
```

### 5.2 setBoss() - Loading Boss Graphics

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 420-465)

```java
private void setBoss(String key) {
    bossKey = key;

    // Dispose old boss graphics
    if (DungeonMap.boss != null && DungeonMap.bossOutline != null) {
        DungeonMap.boss.dispose();
        DungeonMap.bossOutline.dispose();
    }

    // Load new boss graphics
    if (key.equals("The Guardian")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/guardian.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/guardian.png");
    } else if (key.equals("Hexaghost")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/hexaghost.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/hexaghost.png");
    }
    // ... other bosses ...
    else {
        logger.info("WARNING: UNKNOWN BOSS ICON: " + key);
        DungeonMap.boss = null;
    }

    logger.info("[BOSS] " + key);
}
```

**⚠️ MEMORY LEAK:** If you don't call `setBoss()` with a different boss, old graphics remain loaded.

---

## 6. nextRoomTransition() - Per-Floor Cleanup

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 2118-2280)

### 6.1 Player State Reset

```java
public void nextRoomTransition(SaveFile saveFile) {
    overlayMenu.proceedButton.setLabel(TEXT[0]);
    combatRewardScreen.clear();

    if (nextRoom != null && nextRoom.room != null) {
        nextRoom.room.rewards.clear();
    }

    // Monster list management
    if (getCurrRoom() instanceof MonsterRoomElite) {
        if (!eliteMonsterList.isEmpty()) {
            logger.info("Removing elite: " + eliteMonsterList.get(0) + " from monster list.");
            eliteMonsterList.remove(0);
        } else {
            generateElites(10);  // Regenerate if exhausted
        }
    } else if (getCurrRoom() instanceof MonsterRoom) {
        if (!monsterList.isEmpty()) {
            logger.info("Removing monster: " + monsterList.get(0) + " from monster list.");
            monsterList.remove(0);
        } else {
            generateStrongEnemies(12);  // Regenerate if exhausted
        }
    }

    // Event special handling
    else if (getCurrRoom() instanceof EventRoom && getCurrRoom().event instanceof NoteForYourself) {
        AbstractCard tmpCard = ((NoteForYourself)getCurrRoom().event).saveCard;
        if (tmpCard != null) {
            CardCrawlGame.playerPref.putString("NOTE_CARD", tmpCard.cardID);
            CardCrawlGame.playerPref.putInteger("NOTE_UPGRADE", tmpCard.timesUpgraded);
            CardCrawlGame.playerPref.flush();
        }
    }

    // Sound cleanup
    if (RestRoom.lastFireSoundId != 0L) {
        CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
    }
    if (!player.stance.ID.equals("Neutral") && player.stance != null) {
        player.stance.stopIdleSfx();
    }

    // Screen/UI cleanup
    gridSelectScreen.upgradePreviewCard = null;
    previousScreen = null;
    dynamicBanner.hide();
    dungeonMapScreen.closeInstantly();
    closeCurrentScreen();
    topPanel.unhoverHitboxes();
    fadeIn();
    player.resetControllerValues();

    // Visual effects cleanup
    effectList.clear();
    for (Iterator<AbstractGameEffect> i = topLevelEffects.iterator(); i.hasNext(); ) {
        AbstractGameEffect e = i.next();
        if (!(e instanceof ObtainKeyEffect)) {
            i.remove();  // Keep key effects, remove others
        }
    }
    topLevelEffectsQueue.clear();
    effectsQueue.clear();

    dungeonMapScreen.dismissable = true;
    dungeonMapScreen.map.legend.isLegendHighlighted = false;

    // Player combat state reset
    resetPlayer();

    // Floor increment and metrics
    if (!CardCrawlGame.loadingSave) {
        incrementFloorBasedMetrics();
        floorNum++;
        if (!TipTracker.tips.get("INTENT_TIP") && floorNum == 6) {
            TipTracker.neverShowAgain("INTENT_TIP");
        }
        StatsScreen.incrementFloorClimbed();
        SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
    }

    // RNG re-seeding for new floor
    monsterHpRng = new Random(Settings.seed + floorNum);
    aiRng = new Random(Settings.seed + floorNum);
    shuffleRng = new Random(Settings.seed + floorNum);
    cardRandomRng = new Random(Settings.seed + floorNum);
    miscRng = new Random(Settings.seed + floorNum);

    // ... next room generation ...
}
```

### 6.2 resetPlayer() - Combat State Cleanup

**File:** `com.megacrit.cardcrawl.dungeons.AbstractDungeon.java` (Line 2095-2116)

```java
public static void resetPlayer() {
    player.orbs.clear();
    player.animX = 0.0F;
    player.animY = 0.0F;
    player.hideHealthBar();
    player.hand.clear();
    player.powers.clear();
    player.drawPile.clear();
    player.discardPile.clear();
    player.exhaustPile.clear();
    player.limbo.clear();
    player.loseBlock(true);
    player.damagedThisCombat = 0;

    // Stance reset
    if (!player.stance.ID.equals("Neutral")) {
        player.stance = new NeutralStance();
        player.onStanceChange("Neutral");
    }

    GameActionManager.turn = 1;
}
```

**⚠️ CRITICAL:** `resetPlayer()` clears ALL combat-related state. If you return to a previous floor without calling this, combat state will persist.

---

## 7. Critical Issues for Dungeon Re-entry

### 7.1 The Core Problem

**Issue:** Calling `reset()` or creating a new dungeon instance clears essential state, but does not properly handle:

1. **RNG State Desynchronization**
   - Main RNGs (monsterRng, eventRng, etc.) maintain counters across floors
   - Reverting to a previous floor does NOT rewind RNG counters
   - Result: Same floor will have different encounters on re-entry

2. **Monster List Exhaustion**
   - `monsterList` and `eliteMonsterList` are consumed as you progress
   - Re-entering an act does NOT regenerate these lists
   - Result: Crash when trying to draw from empty list

3. **Map Persistence**
   - `map` variable is NOT cleared by `reset()`
   - Re-generating a map with same seed produces identical layout
   - But room instances are not properly restored

4. **Memory Leaks**
   - Old room instances, monsters, and effects remain in memory
   - Textures and audio are not properly disposed
   - Result: Memory usage increases with each re-entry

### 7.2 Why Floor Backtracking Crashes

**Scenario:** Player completes floor 10, then returns to floor 5.

**What Happens:**

```java
// Current state at floor 10
floorNum = 10
monsterList = [monster8, monster9, monster10, ...]  // First 7 consumed
eliteMonsterList = [elite3, elite4, ...]            // First 2 consumed
monsterRng.counter = 150                             // Advanced from use

// Attempt to return to floor 5
// Option A: Call reset() - DISASTER
reset()  // Clears floorNum, currMapNode, but NOT monsterList, NOT RNGs

// Option B: Create new dungeon - PARTIAL SUCCESS
new Exordium(player, specialOneTimeEventList)
// This calls dungeonTransitionSetup() which DOES clear monster lists
// But now the lists are re-generated with monsterRng at counter=150
// Result: Different monsters than original floor 5

// Option C: Try to restore floor 5 state - IMPOSSIBLE
// No mechanism to save/restore:
// - Exact map layout with room states
// - RNG counter positions
// - Monster list consumption state
// - Event list state
```

### 7.3 Essential Variables for Dungeon Re-entry

To implement proper floor backtracking, you MUST save/restore:

```java
// Floor identification
int floorNum
int actNum

// RNG states (cannot be reset, only saved/restored)
Random monsterRng      // With counter position
Random eventRng        // With counter position
Random merchantRng     // With counter position
Random cardRng         // With counter position
Random treasureRng     // With counter position
Random relicRng        // With counter position
Random potionRng       // With counter position
Random aiRng           // With counter position
Random shuffleRng      // With counter position
Random cardRandomRng   // With counter position
Random miscRng         // With counter position
Random mapRng          // With counter position

// List consumption states
ArrayList<String> monsterList        // Current state, not regenerated
ArrayList<String> eliteMonsterList   // Current state, not regenerated
ArrayList<String> bossList           // Usually doesn't change
ArrayList<String> eventList          // Current state after events consumed
ArrayList<String> specialOneTimeEventList
ArrayList<String> shrineList

// Map structure and path
ArrayList<ArrayList<MapRoomNode>> map
ArrayList<Integer> pathX
ArrayList<Integer> pathY
MapRoomNode currMapNode
MapRoomNode nextRoom

// Room state (EXTREMELY COMPLEX)
AbstractRoom getCurrRoom()  // Current room instance with ALL state
// Including:
// - monsters (with HP, powers, intents)
// - rewards
// - event choices made
// - combat state

// Pool states
ArrayList<String> commonRelicPool
ArrayList<String> uncommonRelicPool
ArrayList<String> rareRelicPool
ArrayList<String> shopRelicPool
ArrayList<String> bossRelicPool

// Chances and modifiers
float shopRoomChance
float restRoomChance
float treasureRoomChance
float eventRoomChance
float eliteRoomChance
int AbstractRoom.blizzardPotionMod
int ShopScreen.purgeCost
```

---

## 8. Modification Guide

### 8.1 Adding a New Boss

**Example: Add "Custom Boss" to Act 1**

1. **Modify initializeBoss()** in `Exordium.java`:

```java
protected void initializeBoss() {
    bossList.clear();

    if (Settings.isDailyRun) {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        bossList.add("Custom Boss");  // ADD THIS
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }
    // ... existing unlock logic ...
    else {
        bossList.add("The Guardian");
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        bossList.add("Custom Boss");  // ADD THIS
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // ... safety checks ...
}
```

2. **Add boss graphics loading** in `AbstractDungeon.setBoss()`:

```java
private void setBoss(String key) {
    bossKey = key;

    if (DungeonMap.boss != null && DungeonMap.bossOutline != null) {
        DungeonMap.boss.dispose();
        DungeonMap.bossOutline.dispose();
    }

    // ... existing bosses ...
    else if (key.equals("Custom Boss")) {
        DungeonMap.boss = ImageMaster.loadImage("images/ui/map/boss/customboss.png");
        DungeonMap.bossOutline = ImageMaster.loadImage("images/ui/map/bossOutline/customboss.png");
    }

    logger.info("[BOSS] " + key);
}
```

3. **Create boss encounter** in `MonsterHelper.getEncounter()` (separate file)

### 8.2 Changing Map Structure

**Example: Make map 20 floors instead of 15**

**⚠️ WARNING:** Changing map height affects:
- Rest room placement (row 14 → row 19)
- Treasure room placement (row 8 → row 10?)
- Boss room (always last row)

**Modification:**

```java
// In AbstractDungeon.generateMap()
protected static void generateMap() {
    int mapHeight = 20;  // Changed from 15
    int mapWidth = 7;
    int mapPathDensity = 6;

    // ... rest of method ...

    // Update row assignments
    RoomTypeAssigner.assignRowAsRoomType(map.get(map.size() - 1), RestRoom.class);  // Row 19
    RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);             // Row 0
    RoomTypeAssigner.assignRowAsRoomType(map.get(10), TreasureRoom.class);           // Row 10 (mid-point)
}
```

### 8.3 Implementing Safe Floor Backtracking

**⚠️ EXTREMELY COMPLEX - Proof of Concept Only**

```java
// Custom class to save dungeon state
public class DungeonSnapshot {
    // All variables from section 7.3
    public int floorNum;
    public int actNum;

    // RNG states (must save counter positions)
    public long monsterRngCounter;
    public long eventRngCounter;
    // ... all other RNG counters ...

    // Lists (deep copy required)
    public ArrayList<String> monsterList;
    public ArrayList<String> eliteMonsterList;
    // ... all other lists ...

    // Map structure (deep copy with room instances)
    public ArrayList<ArrayList<MapRoomNode>> map;
    // ... etc ...

    public static DungeonSnapshot capture() {
        DungeonSnapshot snap = new DungeonSnapshot();
        snap.floorNum = AbstractDungeon.floorNum;
        snap.actNum = AbstractDungeon.actNum;

        // Save RNG counters
        snap.monsterRngCounter = AbstractDungeon.monsterRng.counter;
        snap.eventRngCounter = AbstractDungeon.eventRng.counter;
        // ... all other RNGs ...

        // Deep copy lists
        snap.monsterList = new ArrayList<>(AbstractDungeon.monsterList);
        snap.eliteMonsterList = new ArrayList<>(AbstractDungeon.eliteMonsterList);
        // ... all other lists ...

        // Deep copy map (THIS IS EXTREMELY COMPLEX)
        // Would require custom serialization for MapRoomNode and AbstractRoom

        return snap;
    }

    public void restore() {
        AbstractDungeon.floorNum = this.floorNum;
        AbstractDungeon.actNum = this.actNum;

        // Restore RNG states
        AbstractDungeon.monsterRng = new Random(Settings.seed, this.monsterRngCounter);
        AbstractDungeon.eventRng = new Random(Settings.seed, this.eventRngCounter);
        // ... all other RNGs ...

        // Restore lists
        AbstractDungeon.monsterList = new ArrayList<>(this.monsterList);
        AbstractDungeon.eliteMonsterList = new ArrayList<>(this.eliteMonsterList);
        // ... all other lists ...

        // Restore map (EXTREMELY COMPLEX - may be impossible)
        // Room instances cannot be easily serialized
    }
}

// Usage
HashMap<Integer, DungeonSnapshot> floorSnapshots = new HashMap<>();

// Before advancing floor
floorSnapshots.put(AbstractDungeon.floorNum, DungeonSnapshot.capture());

// To return to previous floor
DungeonSnapshot snap = floorSnapshots.get(targetFloor);
if (snap != null) {
    snap.restore();
    // May still crash due to room state incompatibility
}
```

**💀 REALITY CHECK:** True floor backtracking is nearly impossible because:
1. Room instances contain live game state (monsters with HP, powers, intents)
2. Serializing/deserializing entire room state is not supported by base game
3. Many objects hold references that break on deserialization
4. The game's save system only saves between floors, not within them

---

## Conclusion

### Key Takeaways

1. **Initialization Order Matters**
   - `dungeonTransitionSetup()` → monster generation → boss selection → map generation
   - Changing this order causes crashes or incorrect state

2. **reset() is Incomplete**
   - Does NOT clear: map, lists, RNG states, pools
   - Only clears: UI state, effects, current room

3. **RNG State is Persistent**
   - Cannot be "reset" to produce same results
   - Must save/restore counter positions for deterministic behavior

4. **Floor Backtracking is Nearly Impossible**
   - Would require complete dungeon state serialization
   - Room instances cannot be easily saved/restored
   - RNG state must be tracked for every floor
   - Memory management becomes extremely complex

### Recommended Approach for "Reverse Floor" Mods

Instead of true backtracking, consider:

1. **One-Way Descent with Checkpoints**
   - Save state at specific floors (e.g., rest sites)
   - Allow return ONLY to checkpoints
   - Regenerate intermediate floors on return (different encounters)

2. **Parallel Timeline Approach**
   - Generate new dungeon instance for "return"
   - Use different seed to avoid confusion
   - Accept that it's not the same dungeon

3. **Replay Mode**
   - Record all encounters and events
   - Replay them with same seed
   - This is closer to "watch a recording" than actual backtracking

**Bottom Line:** The game was not designed for bidirectional floor traversal. Any implementation will be a significant architectural modification with high crash risk.

---

**Document Version:** 1.0
**Based on:** Slay the Spire Desktop 1.0 (2019-01-23 build)
**Decompiled Source:** JD-Core 1.1.3
