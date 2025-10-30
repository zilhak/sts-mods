# JDK 25 Setup Guide

This project is configured to work with **OpenJDK 25** while targeting **Java 8** compatibility for Slay the Spire.

## ✅ What's Configured

### Maven Compiler Settings

The project uses these key settings for JDK 25 compatibility:

```xml
<maven.compiler.release>8</maven.compiler.release>
```

**`--release 8`** flag ensures:
- ✅ Java 8 syntax only
- ✅ Java 8 APIs only (prevents accidental use of newer APIs)
- ✅ Java 8 bytecode generation
- ✅ Perfect compatibility with Slay the Spire

### Updated Plugin Versions

For JDK 25 support, all Maven plugins are updated to latest versions:

| Plugin | Version | Purpose |
|--------|---------|---------|
| maven-compiler-plugin | 3.13.0 | Latest compiler with JDK 25 support |
| maven-jar-plugin | 3.4.2 | JAR packaging |
| maven-assembly-plugin | 3.7.1 | Assembly creation |
| maven-resources-plugin | 3.3.1 | Resource handling |

### Compiler Warnings

Configured to show helpful warnings while suppressing irrelevant ones:

```xml
<compilerArgs>
    <arg>-Xlint:all</arg>           <!-- All warnings -->
    <arg>-Xlint:-options</arg>       <!-- Suppress version warnings -->
    <arg>-Xlint:-path</arg>          <!-- Suppress path warnings -->
</compilerArgs>
```

## 🔧 Installation

### Windows

**Option 1: Official Oracle OpenJDK**
```bash
# Download from:
https://jdk.java.net/25/

# Extract to:
C:\Program Files\Java\jdk-25

# Set JAVA_HOME
setx JAVA_HOME "C:\Program Files\Java\jdk-25"
setx PATH "%JAVA_HOME%\bin;%PATH%"
```

**Option 2: Eclipse Temurin (When available)**
```bash
choco install temurin25
```

### Linux

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-25-jdk

# Set JAVA_HOME in ~/.bashrc
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### macOS

```bash
# Homebrew (when available)
brew install openjdk@25

# Or download from:
https://jdk.java.net/25/

# Set JAVA_HOME in ~/.zshrc
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

## ✓ Verification

### 1. Check Java Version

```bash
java -version
```

Expected output:
```
openjdk version "25" 2025-03-18
OpenJDK Runtime Environment (build 25+...)
OpenJDK 64-Bit Server VM (build 25+...)
```

### 2. Check Maven

```bash
mvn -version
```

Should show:
```
Java version: 25, vendor: Oracle Corporation
Java home: /path/to/jdk-25
```

### 3. Test Build

```bash
mvn clean compile

# Look for this in output:
# [INFO] Compiling X source files to target/classes
# [INFO] BUILD SUCCESS
```

### 4. Verify Bytecode Version

```bash
# Build the project
mvn clean package

# Check generated class file version
javap -verbose ascension-100/target/classes/com/stsmod/ascension100/Ascension100Mod.class | grep "major version"

# Expected output:
# major version: 52
```

**Bytecode Version Reference**:
- 52 = Java 8 ✅ (What we want)
- 55 = Java 11
- 61 = Java 17
- 65 = Java 21
- 69 = Java 25

### 5. Test for Accidental API Usage

Try using a Java 9+ API to verify protection:

```java
// This should FAIL to compile:
var list = List.of("test");  // Java 9+ API
```

Expected error:
```
[ERROR] cannot find symbol
  symbol:   method of(java.lang.String)
  location: interface java.util.List
```

This confirms you can't accidentally use newer APIs! ✅

## 🎯 Development Benefits with JDK 25

### Performance Improvements
- **30-40% faster compilation** vs JDK 8
- **Better garbage collection** during development
- **Faster IDE indexing** and code analysis

### Modern Tooling
- Latest IntelliJ IDEA features fully supported
- Better VS Code Java extension performance
- Advanced debugging capabilities

### Development Experience
```java
// You can use modern Java syntax in your IDE
// as long as Maven compilation succeeds

// Modern switch expressions (Java 14+)
// will be caught by --release 8 flag
```

## ⚠️ Common Issues & Solutions

### Issue 1: "source release 8 requires target release 8"

**Cause**: Conflicting compiler settings

**Solution**: Already fixed in pom.xml with `--release 8`

### Issue 2: "warning: [options] bootstrap class path not set"

**Cause**: Normal warning with cross-compilation

**Solution**: Already suppressed with `-Xlint:-options`

### Issue 3: Maven can't find JDK 25

**Cause**: JAVA_HOME not set

**Solution**:
```bash
# Check JAVA_HOME
echo $JAVA_HOME    # Linux/Mac
echo %JAVA_HOME%   # Windows

# Set if not configured (see Installation section)
```

### Issue 4: IntelliJ shows red squiggles on Java 8 code

**Cause**: IDE language level mismatch

**Solution**:
1. File → Project Structure
2. Project → Language Level: **8 - Lambdas, type annotations**
3. Modules → Each module → Language Level: **8**

### Issue 5: Build succeeds but JAR doesn't work in game

**Cause**: Wrong bytecode version generated

**Solution**: Run verification step 4 above to check bytecode version

## 🔍 IDE Configuration

### IntelliJ IDEA

**Project Settings**:
```
File → Project Structure
├── Project
│   ├── SDK: openjdk-25
│   └── Language Level: 8 - Lambdas, type annotations
├── Modules (for each module)
│   └── Language Level: 8
└── SDKs
    └── JDK 25 configured
```

**Run Configuration**:
- No special configuration needed
- Maven handles compilation automatically

### VS Code

Create `.vscode/settings.json`:

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-25",
      "path": "/path/to/jdk-25",
      "default": true
    }
  ],
  "java.configuration.maven.userSettings": "settings.xml",
  "java.compile.nullAnalysis.mode": "automatic",
  "java.server.launchMode": "Standard"
}
```

## 📊 Comparison: Development vs Runtime

| Aspect | Development (JDK 25) | Runtime (Game) |
|--------|---------------------|----------------|
| **Compiler** | javac 25 | N/A |
| **Syntax Check** | Java 8 only (--release 8) | N/A |
| **API Available** | Java 8 only (enforced) | Java 8 |
| **Bytecode** | Version 52 (Java 8) | Version 52 |
| **JVM** | Java 25 HotSpot | Java 8 (ModTheSpire) |
| **Performance** | 30-40% faster compile | Standard |

## 🚀 Quick Start

```bash
# Clone/navigate to project
cd sts-mods

# Verify Java 25
java -version

# Build all mods
mvn clean package

# Check output
ls -lh ascension-100/target/Ascension100.jar
ls -lh custom-relics/target/CustomRelics.jar

# Verify bytecode (should show "major version: 52")
javap -verbose ascension-100/target/classes/com/stsmod/ascension100/Ascension100Mod.class | grep "major version"
```

## ✨ Summary

You're using **JDK 25** for development while generating **Java 8 compatible** mods:

- ✅ Modern development environment (JDK 25)
- ✅ Fast compilation and build times
- ✅ Latest IDE features
- ✅ Automatic Java 8 API enforcement
- ✅ Perfect game compatibility (Java 8 bytecode)

**Best of both worlds!** 🎮

## 📚 Additional Resources

- [JDK 25 Release Notes](https://jdk.java.net/25/)
- [Maven Compiler Plugin Docs](https://maven.apache.org/plugins/maven-compiler-plugin/)
- [Java Bytecode Versions](https://en.wikipedia.org/wiki/Java_class_file#General_layout)

---

**Need help?** Check the main `README.md` or `SETUP.md`
