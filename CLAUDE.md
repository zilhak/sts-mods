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


## Monster Name Translation Reference

**IMPORTANT**: Korean ↔ English monster mapping verified from official localization file (`monsters.json`).
Use these exact class names and IDs when patching monsters.

### Act 1 (Exordium) - 1막

**Normal Enemies**:
- 턱벌레 = `JawWorm` (ID: "JawWorm")
- 공벌레 = `LouseNormal` / `LouseDefensive` (ID: "FuzzyLouseNormal" / "FuzzyLouseDefensive")
- 산성 슬라임 = `AcidSlime_S` / `AcidSlime_M` / `AcidSlime_L` (ID: "AcidSlime_S/M/L")
- 가시 슬라임 = `SpikeSlime_S` / `SpikeSlime_M` / `SpikeSlime_L` (ID: "SpikeSlime_S/M/L")
- 동물하초 = `FungiBeast` (ID: "FungiBeast")
- 광신자 = `Cultist` (ID: "Cultist")
- 도적 = `Looter` (ID: "Looter")
- 노예 상인 = `SlaverBlue` / `SlaverRed` (ID: "SlaverBlue" / "SlaverRed")

**Gremlins** (그렘린):
- 뚱뚱한 그렘린 = `GremlinFat` (ID: "GremlinFat")
- 교활한 그렘린 = `GremlinThief` (ID: "GremlinThief")
- 화난 그렘린 = `GremlinWarrior` (ID: "GremlinWarrior")
- 그렘린 마법사 = `GremlinWizard` (ID: "GremlinWizard")
- 방패 그렘린 = `GremlinTsundere` (ID: "GremlinTsundere")

**Elites**:
- 귀족 그렘린 = `GremlinNob` (ID: "GremlinNob")
- 라가불린 = `Lagavulin` (ID: "Lagavulin")
- 보초기 = `Sentry` (ID: "Sentry")

**Bosses**:
- 대왕 슬라임 = `SlimeBoss` (ID: "SlimeBoss")
- 수호자 = `TheGuardian` (ID: "TheGuardian")
- 육각령 = `Hexaghost` (ID: "Hexaghost")

### Act 2 (City) - 2막

**Normal Enemies**:
- 강도 = `Mugger` (ID: "Mugger")
- 로미오 = `BanditLeader` (ID: "BanditLeader")
- 촉새 = `BanditChild` (ID: "BanditChild")
- 곰 = `BanditBear` (ID: "BanditBear")
- 스네코 = `Snecko` (ID: "Snecko")
- 구체형 수호기 = `SphericGuardian` (ID: "SphericGuardian")
- 섀 = `Byrd` (ID: "Byrd")
- 갑각기생충 = `ShelledParasite` (ID: "Shelled Parasite")
- 뱀 식물 = `SnakePlant` (ID: "SnakePlant")
- 선택받은 자 = `Chosen` (ID: "Chosen")
- 백부장 = `Centurion` (ID: "Centurion")
- 신비주의자 = `Healer` (ID: "Healer")
- 구체 = `BronzeOrb` (ID: "BronzeOrb")
- 횃불 머리 = `TorchHead` (ID: "TorchHead")

**Elites**:
- 칼부림의 책 = `BookOfStabbing` (ID: "BookOfStabbing")
- 노예 관리자 = `Taskmaster` (ID: "SlaverBoss")
- 그렘린 리더 = `GremlinLeader` (ID: "GremlinLeader")

**Bosses**:
- 청동 자동인형 = `BronzeAutomaton` (ID: "BronzeAutomaton")
- 수집가 = `TheCollector` (ID: "TheCollector")
- 투사 = `Champ` (ID: "Champ")

### Act 3 (Beyond) - 3막

**Normal Enemies**:
- 어두미 = `Darkling` (ID: "Darkling")
- 구체 순찰기 = `OrbWalker` (ID: "Orb Walker")
- 현혹기 = `Repulsor` (ID: "Repulsor")
- 첨탑 암종 = `SpireGrowth` (ID: "Serpent")
- 과도자 = `Transient` (ID: "Transient")
- 반사기 = `Spiker` (ID: "Spiker")
- 꿈틀대는 덩어리 = `WrithingMass` (ID: "WrithingMass")
- 아귀 = `Maw` (ID: "Maw")
- 폭탄기 = `Exploder` (ID: "Exploder")
- 단검 = `SnakeDagger` (ID: "Dagger")

**Elites**:
- 거인의 머리 = `GiantHead` (ID: "GiantHead")
- 파충류 주술사 = `Reptomancer` (ID: "Reptomancer")
- 네메시스 = `Nemesis` (ID: "Nemesis")

**Bosses**:
- 깨어난 자 = `AwakenedOne` (ID: "AwakenedOne")
- 시간 포식자 = `TimeEater` (ID: "TimeEater")
- 도누 = `Donu` (ID: "Donu")
- 데카 = `Deca` (ID: "Deca")

### Act 4 (Ending) - 4막

**Bosses**:
- 타락한 심장 = `CorruptHeart` (ID: "CorruptHeart")
- 첨탑의 방패 = `SpireShield` (ID: "SpireShield")
- 첨탑의 창 = `SpireSpear` (ID: "SpireSpear")

### Common Attack/Pattern Names

**Darkling (어둠이)**:
- 씹기 = Chomp (move byte 1)
- 물고 늘어지기 = Nip (move byte 2)
- 부활 = Reincarnate (move byte 5)

**Byrd (섀)**:
- 박치기 = Headbutt (move byte 2)
- 울음 = Caw (move byte 6)
- 날아오름 = Fly (move byte 1)

**BanditLeader (로미오)**:
- 십자 베기 = Cross Slash (move byte 1)

**Maw (아귀)**:
- 포효 = Roar (move byte 3)

**SpireGrowth (첨탑 암종)**:
- 신속 태클 = Quick Tackle (move byte 1)

**GiantHead (거대한 머리)**:
- 응시 = Gaze

**SlimeBoss (점액 군주)**:
- 횡혈 = Goop Spray

### Common Power/Debuff Names

**Buffs**:
- 힘 = Strength (`StrengthPower`)
- 방어도 = Block
- 가시 = Thorns (`ThornsPower`)
- 판금 갑옷 = Plated Armor (`PlatedArmorPower`)
- 금속화 = Metallicize (`MetallicizePower`)
- 재생 = Regeneration (`RegenerateMonsterPower`)
- 탄성 = Malleable (`MalleablePower`)
- 인공물 = Artifact (`ArtifactPower`)
- 불가침 = Intangible (`IntangiblePower`)

**Debuffs**:
- 느림 = Slow (`SlowPower`)
- 취약 = Vulnerable (`VulnerablePower`)
- 약화 = Weak (`WeakPower`)
- 포박 = Constricted (`ConstrictedPower`)
- 허약 = Frail (`FrailPower`)

##중요 : 기획문서
기획 문서 경로 : E:\workspace\sts-mods\.claude\ascension-detail.md
해당 문서의 내용이 절대적인 진실의 원천이 되어야하며, 모든 소스코드는 이 문서의 내용을 따라야 합니다.