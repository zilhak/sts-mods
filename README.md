# Slay the Spire Mods Collection

Multi-module Maven project for developing Slay the Spire mods. Each module builds independently into its own JAR file.

## 📦 Modules

### 1. **Ascension 100** (`ascension-100/`)
Extends Slay the Spire's Ascension mode from level 20 to level 100 with progressively harder challenges.

**Build Output**: `Ascension100.jar`

### 2. **Custom Relics** (`custom-relics/`)
Adds unique and powerful custom relics to expand strategic options.

**Build Output**: `CustomRelics.jar`

## 🛠️ Prerequisites

### Required Software
- **Java JDK 8+** (Project supports JDK 8 through JDK 25)
  - **Using JDK 25?** See [`JDK25-SETUP.md`](JDK25-SETUP.md) for configuration details
  - Recommended: JDK 17 (LTS) or JDK 21 (LTS) for modern development
  - Minimum: JDK 8 for basic compatibility
- **Maven 3.6+** (build tool)
- **Slay the Spire** (installed via Steam)

### Required Mods
Install these through Steam Workshop or manually:
1. **ModTheSpire** (v3.29.3+) - Mod loader
2. **BaseMod** (v5.48.0+) - Essential modding API
3. **StSLib** (v2.3.0+) - Additional utilities (optional but recommended)

## ⚙️ Setup

### 1. Configure Environment Variable

Set the `sts.install.dir` environment variable to your Slay the Spire installation directory.

**Windows**:
```bash
set STS_INSTALL_DIR=C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire
```

**Linux/Mac**:
```bash
export STS_INSTALL_DIR=~/.steam/steam/steamapps/common/SlayTheSpire
```

Or edit `pom.xml` and replace `${sts.install.dir}` with your actual path:
```xml
<systemPath>C:/Program Files (x86)/Steam/steamapps/common/SlayTheSpire/desktop-1.0.jar</systemPath>
```

### 2. Verify Dependencies

Ensure these files exist in your STS installation:
```
SlayTheSpire/
├── desktop-1.0.jar          # Base game
└── mods/
    ├── ModTheSpire.jar      # Mod loader
    ├── BaseMod.jar          # Modding API
    └── StSLib.jar           # Optional utilities
```

### 3. Verify Setup

Run the verification script to check your environment:

**Linux/Mac**:
```bash
./verify-setup.sh
```

**Windows**:
```cmd
verify-setup.bat
```

This will check:
- ✓ Java installation and version
- ✓ Maven installation
- ✓ STS installation path
- ✓ Required mod files
- ✓ Compilation compatibility

## 🔨 Building

### Build All Modules
```bash
mvn clean package
```

### Build Specific Module
```bash
# Build only Ascension 100
mvn clean package -pl ascension-100

# Build only Custom Relics
mvn clean package -pl custom-relics
```

### Build Output
Compiled JARs will be in each module's `target/` directory:
```
ascension-100/target/Ascension100.jar
custom-relics/target/CustomRelics.jar
```

## 🚀 Installation

1. **Build the mods** (see above)
2. **Copy JAR files** to your Slay the Spire mods folder:

**Windows**:
```bash
copy ascension-100\target\Ascension100.jar "%LOCALAPPDATA%\ModTheSpire\mods\"
copy custom-relics\target\CustomRelics.jar "%LOCALAPPDATA%\ModTheSpire\mods\"
```

**Linux/Mac**:
```bash
cp ascension-100/target/Ascension100.jar ~/.config/ModTheSpire/mods/
cp custom-relics/target/CustomRelics.jar ~/.config/ModTheSpire/mods/
```

3. **Launch the game** through ModTheSpire
4. **Enable mods** in the ModTheSpire launcher

## 🧪 Development

### Project Structure
```
sts-mods/
├── pom.xml                           # Parent POM with shared config
├── ascension-100/                    # Ascension 100 module
│   ├── pom.xml
│   └── src/main/
│       ├── java/
│       │   └── com/stsmod/ascension100/
│       │       └── Ascension100Mod.java
│       └── resources/
│           ├── ModTheSpire.json
│           └── localization/
└── custom-relics/                    # Custom Relics module
    ├── pom.xml
    └── src/main/
        ├── java/
        │   └── com/stsmod/relics/
        │       ├── CustomRelicsMod.java
        │       └── relics/
        │           └── ExampleRelic.java
        └── resources/
            ├── ModTheSpire.json
            ├── localization/
            └── images/relics/
```

### Adding New Modules

1. Create new directory: `mkdir my-new-mod`
2. Add to parent `pom.xml`:
```xml
<modules>
    <module>ascension-100</module>
    <module>custom-relics</module>
    <module>my-new-mod</module>  <!-- Add this -->
</modules>
```
3. Create `my-new-mod/pom.xml` following existing module structure
4. Implement mod code

### IDE Setup (IntelliJ IDEA)

1. **Import Project**: File → Open → Select `pom.xml`
2. **Configure JDK**: File → Project Structure → SDK: Java 8
3. **Build**: Maven → Lifecycle → package
4. **Run**: Configure ModTheSpire as external tool

## 📝 Modding Resources

- **ModTheSpire Wiki**: https://github.com/kiooeht/ModTheSpire/wiki
- **BaseMod Documentation**: https://github.com/daviscook477/BaseMod/wiki
- **Discord**: Slay the Spire Modding Community
- **Example Mods**: https://github.com/topics/slay-the-spire-mod

## 🔧 Troubleshooting

### "Cannot find desktop-1.0.jar"
- Verify `sts.install.dir` environment variable
- Check STS installation path
- Ensure game files are not corrupted

### "ClassNotFoundException" when running
- Ensure ModTheSpire and BaseMod are installed
- Check JAR is in correct mods folder
- Verify mod dependencies in ModTheSpire.json

### Maven build fails
- Confirm Java 8 JDK is installed: `java -version`
- Update Maven: `mvn -version` should be 3.6+
- Clean build: `mvn clean install`

## 📄 License

This project is provided as-is for educational and modding purposes.

## 🤝 Contributing

Each module is independent. Feel free to:
- Add new modules
- Enhance existing mods
- Share improvements

---

**Happy Modding!** 🎮
