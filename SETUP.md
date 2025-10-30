# Quick Setup Guide

## 1. Prerequisites Installation

### Install Java JDK 8
**Windows**:
1. Download Oracle JDK 8 or AdoptOpenJDK 8
2. Run installer
3. Set `JAVA_HOME` environment variable
4. Verify: `java -version` (should show 1.8.x)

**Linux**:
```bash
sudo apt install openjdk-8-jdk  # Ubuntu/Debian
# or
sudo yum install java-1.8.0-openjdk-devel  # CentOS/RHEL
```

**Mac**:
```bash
brew install openjdk@8
```

### Install Maven
**Windows**:
1. Download from https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Maven`
3. Add to PATH: `C:\Program Files\Maven\bin`
4. Verify: `mvn -version`

**Linux/Mac**:
```bash
# Linux
sudo apt install maven  # Ubuntu/Debian

# Mac
brew install maven
```

## 2. Slay the Spire Setup

### Install Required Mods
1. **Launch Steam** → Library → Slay the Spire
2. **Workshop** → Subscribe to:
   - ModTheSpire
   - BaseMod
   - StSLib (optional)

### Find Installation Directory
**Windows**:
```
C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire
```

**Linux**:
```
~/.steam/steam/steamapps/common/SlayTheSpire
```

**Mac**:
```
~/Library/Application Support/Steam/steamapps/common/SlayTheSpire
```

## 3. Configure Project

### Option A: Environment Variable (Recommended)

**Windows**:
```cmd
setx STS_INSTALL_DIR "C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire"
```

**Linux/Mac** (add to `~/.bashrc` or `~/.zshrc`):
```bash
export STS_INSTALL_DIR="~/.steam/steam/steamapps/common/SlayTheSpire"
```

### Option B: Edit pom.xml Directly

Edit `pom.xml` lines 22-48, replace `${sts.install.dir}` with your actual path:

```xml
<systemPath>C:/Program Files (x86)/Steam/steamapps/common/SlayTheSpire/desktop-1.0.jar</systemPath>
```

**Important**: Use forward slashes `/` even on Windows in Maven!

## 4. Verify Setup

Run from project root:

```bash
# Test if Maven can find dependencies
mvn validate

# Should show no errors about missing jars
```

## 5. Build Your First Mod

```bash
# Build all mods
mvn clean package

# Or use convenience scripts
./build.sh all           # Linux/Mac
build.bat all            # Windows
```

Expected output:
```
ascension-100/target/Ascension100.jar
custom-relics/target/CustomRelics.jar
```

## 6. Install Mods

**Windows**:
```cmd
copy ascension-100\target\Ascension100.jar "%LOCALAPPDATA%\ModTheSpire\mods\"
copy custom-relics\target\CustomRelics.jar "%LOCALAPPDATA%\ModTheSpire\mods\"
```

**Linux/Mac**:
```bash
cp ascension-100/target/Ascension100.jar ~/.config/ModTheSpire/mods/
cp custom-relics/target/CustomRelics.jar ~/.config/ModTheSpire/mods/
```

## 7. Test Mods

1. **Launch ModTheSpire** (from Steam or standalone)
2. **Enable your mods** in the launcher
3. **Click "Play"**
4. Check logs for errors

## Troubleshooting

### "Cannot find desktop-1.0.jar"
- Verify STS installation path is correct
- Check that `desktop-1.0.jar` exists in the directory
- Use forward slashes in paths

### "ClassNotFoundException"
- Ensure ModTheSpire.jar and BaseMod.jar exist in `mods/` folder
- Try reinstalling mods from Workshop

### Build fails with "Package does not exist"
- Maven cannot find STS dependencies
- Double-check paths in pom.xml
- Restart terminal after setting environment variables

### Mod doesn't appear in launcher
- JAR must be in correct mods folder
- Check ModTheSpire.json is valid
- Look for errors in ModTheSpire logs

## IDE Setup (IntelliJ IDEA)

1. **Open Project**: File → Open → Select `pom.xml`
2. **Trust Maven Project**: Click "Trust Project" if prompted
3. **Set JDK**: File → Project Structure → Project SDK: 1.8
4. **Reload Maven**: Right-click `pom.xml` → Maven → Reload Project
5. **Build**: Maven tool window → Lifecycle → package

## Next Steps

- Read module READMEs for detailed development guides
- Check `custom-relics/README.md` for adding new relics
- Check `ascension-100/README.md` for implementing ascension levels
- Join Slay the Spire Modding Discord for help

---

**Ready to mod!** 🎮
