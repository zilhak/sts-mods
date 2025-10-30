# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-module Gradle project for developing Slay the Spire mods. Uses Gradle build system (not Maven, despite README mentioning it) with Java 8 target compatibility. Supports JDK 8-25 for compilation.

**Current Modules:**
- `ascension-100`: Extends Ascension mode to level 100
- `custom-relics`: Adds custom relics to the game

## Build Commands

### Gradle (Primary Build System)
```bash
# Build all modules and collect JARs
./gradlew clean build

# Build specific module
./gradlew :ascension-100:build
./gradlew :custom-relics:build

# Show build info (versions, paths)
./gradlew info

# Collect JARs only (after build)
./gradlew collectJars
```

**Build Output:** All module JARs are automatically collected to `build/libs/` after successful build.

### Maven (Legacy Support)
```bash
# Build all modules
mvn clean package

# Build specific module
mvn clean package -pl ascension-100
mvn clean package -pl custom-relics
```

## Critical Architecture Patterns

### Resource Path Structure

**CRITICAL:** All mod resources MUST follow the `{modId}Resources/` structure:

```
src/main/resources/
└── {modId}Resources/          # e.g., ascension100Resources/
    ├── images/
    │   └── badge.png          # Optional mod badge
    ├── localization/
    │   └── eng/               # Language code folders
    │       └── UIStrings.json # Localization files
    └── other-resources/
```

The `getModPath()` method automatically prefixes paths with `{modId}Resources/`:
```java
getModPath("localization/eng/UIStrings.json")
→ "ascension100Resources/localization/eng/UIStrings.json"
```

**Common Mistake:** Placing files directly in `localization/` without the `{modId}Resources/` prefix will cause `GdxRuntimeException: File not found`.

### Localization with Fallback

All mods implement automatic fallback to English when localization files are missing:

```java
String localizationPath = getModPath(STRINGS.replace("eng", language));

// Check if file exists, fallback to English if not
if (!Gdx.files.internal(localizationPath).exists()) {
    logger.warn("Localization file not found: " + localizationPath);
    language = "eng";
    localizationPath = getModPath(STRINGS.replace("eng", language));
}

BaseMod.loadCustomStringsFile(StringsClass.class, localizationPath);
```

**Supported Languages:** kor, zhs, zht, jpn, fra, deu, ita, ptb, rus, pol, esp (defaults to eng)

### Badge Image Handling

Badge images are optional. Mods must check for existence before registration:

```java
Texture badgeTexture = null;
String badgePath = getModPath(BADGE_IMAGE);
if (Gdx.files.internal(badgePath).exists()) {
    badgeTexture = ImageMaster.loadImage(badgePath);
}

// Only register if image loaded successfully
if (badgeTexture != null) {
    BaseMod.registerModBadge(badgeTexture, MOD_NAME, AUTHOR, DESCRIPTION, panel);
}
```

**Critical:** `ModBadge` constructor does not accept null. Always check before calling `registerModBadge()`.

### Mod Initialization Pattern

All mods follow this initialization sequence:

1. **@SpireInitializer annotation** marks the class for ModTheSpire
2. **Static initialize() method** required by ModTheSpire
3. **Constructor** calls `BaseMod.subscribe(this)`
4. **Interface implementations:**
   - `PostInitializeSubscriber.receivePostInitialize()` - Setup UI, register badge
   - `EditStringsSubscriber.receiveEditStrings()` - Load localization
   - `EditRelicsSubscriber.receiveEditRelics()` - Add relics (relics mods only)

### Dependencies Configuration

Root `build.gradle` defines shared dependencies via `ext` block:

```groovy
ext {
    stsVersion = '01-23-2019'
    modthespireVersion = '3.29.3'
    basemodVersion = '5.48.0'

    stsInstallDir = System.getenv('STS_INSTALL_DIR') ?: 'D:/Steam/...'
    workshopDir = 'D:/Steam/steamapps/workshop/content/646570'

    modTheSpirePath = "${workshopDir}/1605060445/ModTheSpire.jar"
    baseModPath = "${workshopDir}/1605833019/BaseMod.jar"
    stsLibPath = "${workshopDir}/1609158507/StSLib.jar"
}
```

Subprojects automatically inherit:
- `desktop-1.0.jar` (game)
- `ModTheSpire.jar` (mod loader)
- `BaseMod.jar` (modding API)

Optional dependency (custom-relics only):
- `StSLib.jar` (advanced utilities)

## Adding New Modules

1. **Create module directory:** `mkdir my-mod`

2. **Add to `settings.gradle`:**
   ```groovy
   include 'ascension-100'
   include 'custom-relics'
   include 'my-mod'  // Add this
   ```

3. **Create `my-mod/build.gradle`:**
   ```groovy
   description = 'My mod description'

   jar {
       archiveBaseName = 'MyMod'
       archiveVersion = ''
       manifest {
           attributes('MTS-Version': rootProject.ext.modthespireVersion)
       }
       from(sourceSets.main.output)
   }

   tasks.register('modInfo') {
       doLast {
           println "Building: ${project.name}"
           println "Output: ${jar.archiveFile.get()}"
       }
   }
   build.finalizedBy modInfo
   ```

4. **Create mod class** following the initialization pattern above

5. **Create resource structure:**
   ```
   src/main/resources/
   ├── mymodResources/
   │   └── localization/eng/UIStrings.json
   └── ModTheSpire.json
   ```

6. **ModTheSpire.json structure:**
   ```json
   {
     "modid": "mymod",
     "name": "My Mod",
     "author_list": ["YourName"],
     "description": "Description",
     "version": "1.0.0",
     "sts_version": "01-23-2019",
     "mts_version": "3.29.3",
     "dependencies": ["basemod"]
   }
   ```

## Java Compilation Settings

Project compiles with modern JDK (8-25) but targets Java 8 bytecode:

```groovy
sourceCompatibility = '1.8'
targetCompatibility = '1.8'
options.release = 8  // Ensures Java 8 compatibility
```

**Important:** Use Java 8 compatible APIs only. No lambda expressions, streams, or newer Java features unless using RetroLambda.

## Environment Configuration

Set `STS_INSTALL_DIR` environment variable:
```bash
# Windows
set STS_INSTALL_DIR=C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire

# Linux/Mac
export STS_INSTALL_DIR=~/.steam/steam/steamapps/common/SlayTheSpire
```

Or edit paths directly in root `build.gradle`.

## Common Pitfalls

1. **Missing {modId}Resources/ prefix** → `File not found` exception
2. **Passing null to registerModBadge()** → `NullPointerException` in ModBadge constructor
3. **Missing localization fallback** → Crash when running in non-English languages
4. **Wrong JAR location** → Check `build/libs/` not module `build/libs/`
5. **ModTheSpire.json missing** → Mod won't be recognized by ModTheSpire

## Testing Mods

1. Build: `./gradlew clean build`
2. Copy JARs from `build/libs/` to:
   - Windows: `%LOCALAPPDATA%\ModTheSpire\mods\`
   - Linux/Mac: `~/.config/ModTheSpire/mods/`
3. Launch game through ModTheSpire
4. Enable mods in launcher

Check logs at:
- Windows: `%LOCALAPPDATA%\ModTheSpire\logs\`
- Linux/Mac: `~/.config/ModTheSpire/logs/`
