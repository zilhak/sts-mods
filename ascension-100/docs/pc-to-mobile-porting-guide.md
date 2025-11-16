# PC판 디컴파일 소스 → 모바일 포팅 가이드

PC판 Slay the Spire 디컴파일 소스를 Android/iOS용으로 재컴파일하는 완전 가이드

**⚠️ 법적 주의사항**:
- 개인 사용 목적으로만 진행
- 수정된 앱 배포 금지
- 게임은 정식 구매 필요
- 개발사의 지적 재산권 존중

---

## 전제 조건

### 필요한 것

✅ **디컴파일된 PC판 소스 코드**
```
decompiled_source/
├── com/megacrit/cardcrawl/
│   ├── characters/
│   ├── dungeons/
│   ├── monsters/
│   ├── cards/
│   └── ... (모든 게임 로직)
└── resources/
    ├── images/
    ├── audio/
    └── localization/
```

✅ **게임 에셋 (리소스)**
```
PC 설치 경로:
Steam\steamapps\common\SlayTheSpire\
└── (이미지, 사운드, 데이터 파일)
```

✅ **개발 환경**
- JDK 8-11
- Android Studio
- Gradle
- (iOS의 경우) Mac + Xcode

---

## 이론적 배경: libGDX 구조

### Slay the Spire는 libGDX로 제작됨

**libGDX 프로젝트 구조**:
```
typical-libgdx-game/
├── core/              ← 플랫폼 독립적 (게임 로직)
│   ├── src/
│   │   └── com/game/
│   │       ├── GameLogic.java
│   │       ├── Player.java
│   │       └── Monster.java
│   └── assets/        ← 공유 리소스
│       ├── images/
│       └── sounds/
│
├── desktop/           ← PC 전용 (런처만)
│   └── src/
│       └── DesktopLauncher.java
│
├── android/           ← Android 전용 (런처만)
│   └── src/
│       └── AndroidLauncher.java
│
└── ios/              ← iOS 전용 (런처만)
    └── src/
        └── IOSLauncher.java
```

**핵심 아이디어**:
- **core 모듈**: 모든 게임 로직 (플랫폼 독립적)
- **desktop/android/ios**: 단순한 런처 (5-10줄 코드)
- **같은 core 코드를 여러 플랫폼에서 재사용**

---

## 전체 프로세스

### Phase 1: PC판 디컴파일 및 정리

#### 1.1 PC 게임 디컴파일

```bash
# jadx 사용
jadx-gui desktop-1.0.jar

# 또는 CLI
jadx -d output_dir desktop-1.0.jar

# 결과
output_dir/
├── sources/
│   └── com/megacrit/cardcrawl/
│       ├── AbstractDungeon.java
│       ├── characters/
│       ├── monsters/
│       └── ...
└── resources/
```

#### 1.2 소스 코드 정리

**디컴파일된 코드는 완벽하지 않습니다**:

```java
// 디컴파일 결과 (문제 있음)
public void someMethod() {
    label0: {
        if (condition) break label0;
        doSomething();
    }
    // 변수 이름: var1, var2, var3
    int var1 = 10;
}

// 수동으로 정리 필요
public void someMethod() {
    if (!condition) {
        doSomething();
    }
    // 의미있는 이름으로 변경
    int maxHealth = 10;
}
```

**정리 작업**:
- [ ] label 구문을 if/else로 변환
- [ ] 변수명을 의미있게 변경 (선택사항)
- [ ] 불필요한 캐스팅 제거
- [ ] 컴파일 에러 수정

---

### Phase 2: libGDX 프로젝트 생성

#### 2.1 libGDX Project Generator 사용

```bash
# libGDX Setup App 다운로드
# https://libgdx.com/dev/project-generation/

# 실행 후 설정:
Name: SlayTheSpire
Package: com.megacrit.cardcrawl
Game Class: SlayTheSpire
Platforms: [✓] Desktop [✓] Android [ ] iOS

# 생성 결과
SlayTheSpire/
├── build.gradle
├── settings.gradle
├── core/
│   ├── build.gradle
│   └── src/
├── desktop/
│   ├── build.gradle
│   └── src/
└── android/
    ├── build.gradle
    ├── AndroidManifest.xml
    └── src/
```

#### 2.2 build.gradle 설정

**root build.gradle**:
```gradle
buildscript {
    ext {
        gdxVersion = '1.11.0'  // 원본 게임과 동일 버전 사용
        roboVMVersion = '2.3.16'
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
    }
}

allprojects {
    apply plugin: "eclipse"

    version = '1.0'
    ext {
        appName = "SlayTheSpire"
        gdxVersion = '1.11.0'
        roboVMVersion = '2.3.16'
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        maven { url "https://oss.sonatype.org/content/repositories/releases/" }
    }
}
```

**core/build.gradle**:
```gradle
apply plugin: "java-library"

sourceCompatibility = 1.8
targetCompatibility = 1.8

dependencies {
    api "com.badlogicgames.gdx:gdx:$gdxVersion"

    // 원본 게임의 추가 의존성 확인 필요
    api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    api "com.badlogicgames.gdx:gdx-controllers:$gdxVersion"
    // 기타 필요한 라이브러리들...
}
```

**android/build.gradle**:
```gradle
android {
    compileSdkVersion 33

    sourceSets {
        main {
            manifest.srcFile 'AndroidManifest.xml'
            java.srcDirs = ['src']
            aidl.srcDirs = ['src']
            renderscript.srcDirs = ['src']
            res.srcDirs = ['res']
            assets.srcDirs = ['../core/assets']
            jniLibs.srcDirs = ['libs']
        }
    }

    packagingOptions {
        exclude 'META-INF/robovm/ios/robovm.xml'
    }

    defaultConfig {
        applicationId "com.megacrit.cardcrawl.modded"
        minSdkVersion 19
        targetSdkVersion 33
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation project(":core")
    api "com.badlogicgames.gdx:gdx-backend-android:$gdxVersion"
    api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64"
}
```

---

### Phase 3: 소스 코드 통합

#### 3.1 디컴파일된 코드를 core 모듈로 복사

```bash
# 디컴파일된 소스
decompiled/sources/com/megacrit/cardcrawl/

# 복사 대상
SlayTheSpire/core/src/com/megacrit/cardcrawl/

# 전체 복사
cp -r decompiled/sources/* SlayTheSpire/core/src/
```

#### 3.2 모드 코드 통합

**PC판 ModTheSpire 패치를 직접 코드로 변환**:

```java
// 기존 ModTheSpire 방식 (Level21.java)
@SpirePatch(
    clz = AbstractMonster.class,
    method = "init"
)
public static class EliteHealthIncrease {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 21) {
            if (__instance.type == EnemyType.ELITE) {
                __instance.maxHealth = (int) Math.ceil(__instance.maxHealth * 1.1f);
            }
        }
    }
}

// 모바일용 변환: 직접 AbstractMonster.java 수정
public class AbstractMonster extends AbstractCreature {
    public void init() {
        // ... 원본 코드 ...

        // ===== Level 21: 엘리트 체력 10% 증가 =====
        if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 21) {
            if (this.type == EnemyType.ELITE) {
                this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
                this.currentHealth = this.maxHealth;
            }
        }
        // ========================================

        // ... 원본 코드 계속 ...
    }
}
```

**모든 Ascension 100 레벨 통합**:

```java
// AbstractMonster.java
public void init() {
    // 원본 초기화 로직
    calculateStats();

    // ===== Ascension 100 Mod =====
    if (AbstractDungeon.isAscensionMode) {
        int level = AbstractDungeon.ascensionLevel;

        // Level 21: 엘리트 체력 10% 증가
        if (level >= 21 && this.type == EnemyType.ELITE) {
            this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
        }

        // Level 22: 일반 적 체력 10% 증가
        if (level >= 22 && this.type == EnemyType.NORMAL) {
            this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
        }

        // Level 32: 엘리트 추가 10% 증가
        if (level >= 32 && this.type == EnemyType.ELITE) {
            this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
        }

        // ... 기타 레벨들 ...

        // 체력 변경 후 currentHealth 동기화
        this.currentHealth = this.maxHealth;
    }
    // ============================
}
```

#### 3.3 주석으로 모드 표시

```java
// ===== ASCENSION 100 MOD - Level 21 =====
// 엘리트 적의 체력이 10% 증가합니다.
if (level >= 21 && this.type == EnemyType.ELITE) {
    this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
}
// ========================================
```

이렇게 하면:
- 나중에 원본으로 돌리기 쉬움
- 어떤 코드가 모드인지 명확
- 디버깅 용이

---

### Phase 4: 리소스 통합

#### 4.1 게임 에셋 복사

```bash
# PC 게임 설치 경로
PC_GAME="C:/Program Files (x86)/Steam/steamapps/common/SlayTheSpire"

# libGDX 프로젝트 assets 경로
PROJECT_ASSETS="SlayTheSpire/android/assets"

# 에셋 복사
cp -r "$PC_GAME/images" "$PROJECT_ASSETS/"
cp -r "$PC_GAME/audio" "$PROJECT_ASSETS/"
cp -r "$PC_GAME/localization" "$PROJECT_ASSETS/"
cp -r "$PC_GAME/shaders" "$PROJECT_ASSETS/"
# 기타 리소스 파일들...
```

#### 4.2 Android 리소스 경로 확인

**리소스 로딩 코드 확인**:
```java
// PC판 (절대 경로일 수 있음)
Texture texture = new Texture("C:/game/images/card.png");

// 모바일용 (상대 경로로 변경)
Texture texture = new Texture("images/card.png");

// 또는 Gdx.files 사용
Texture texture = new Texture(Gdx.files.internal("images/card.png"));
```

**일괄 변경**:
```bash
# 프로젝트 내 모든 Java 파일에서
# 절대 경로를 상대 경로로 변경

find core/src -name "*.java" -exec sed -i 's|C:/game/||g' {} +
```

---

### Phase 5: Android 런처 작성

#### 5.1 AndroidLauncher.java

```java
// android/src/com/megacrit/cardcrawl/AndroidLauncher.java
package com.megacrit.cardcrawl;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        // 설정
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;

        // 메인 게임 클래스 시작
        // SlayTheSpire는 ApplicationAdapter 또는 Game을 상속받은 메인 클래스
        initialize(new SlayTheSpire(), config);
    }
}
```

#### 5.2 메인 게임 클래스 찾기

**디컴파일된 코드에서**:
```java
// desktop-1.0.jar 디컴파일
// 런처 클래스 찾기

public class DesktopLauncher {
    public static void main(String[] args) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        new LwjglApplication(new SlayTheSpire(), config);  // ← 메인 클래스 발견!
    }
}

// SlayTheSpire.java가 메인 게임 클래스
public class SlayTheSpire extends Game {
    @Override
    public void create() {
        // 게임 초기화
    }
}
```

---

### Phase 6: AndroidManifest.xml 설정

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.megacrit.cardcrawl.modded">

    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="Slay the Spire (Modded)"
        android:theme="@style/GdxTheme">

        <activity
            android:name=".AndroidLauncher"
            android:label="Slay the Spire (Modded)"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenSize|screenLayout"
            android:exported="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

**주의사항**:
- `package`를 원본과 다르게 설정 (`com.megacrit.cardcrawl.modded`)
- 이렇게 해야 원본 앱과 공존 가능

---

### Phase 7: 빌드 및 테스트

#### 7.1 첫 빌드 시도

```bash
cd SlayTheSpire

# Desktop 빌드 먼저 (빠름)
./gradlew desktop:run

# 에러 확인 및 수정
# - 누락된 클래스
# - 컴파일 에러
# - 리소스 경로 문제
```

#### 7.2 컴파일 에러 수정

**일반적인 문제들**:

```java
// 문제 1: 중복 import
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard;  // ← 제거

// 문제 2: 잘못된 타입 추론
var list = new ArrayList();  // Java 8에서 안 됨
ArrayList list = new ArrayList();  // 수정

// 문제 3: 디컴파일 오류
public void method() {
    // 불완전한 switch 문
    switch (type) {
        case TYPE1:
            doSomething();
            // break 누락!
    }
}

// 수정
public void method() {
    switch (type) {
        case TYPE1:
            doSomething();
            break;  // 추가
        case TYPE2:
            doOther();
            break;
        default:
            break;
    }
}
```

#### 7.3 Android 빌드

```bash
# Android APK 생성
./gradlew android:assembleDebug

# 결과
android/build/outputs/apk/debug/android-debug.apk

# 설치
adb install android/build/outputs/apk/debug/android-debug.apk
```

---

### Phase 8: 문제 해결

#### 8.1 메모리 부족

**Android는 메모리 제한 있음**:

```gradle
// android/build.gradle
android {
    defaultConfig {
        // 힙 크기 증가
        multiDexEnabled true
    }

    dexOptions {
        javaMaxHeapSize "4g"
    }
}
```

**코드 최적화**:
```java
// 사용하지 않는 기능 제거
// - 모드 지원 코드
// - 개발자 도구
// - 디버그 코드
```

#### 8.2 성능 최적화

```java
// GC 최소화
public class ObjectPool<T> {
    private final Queue<T> pool = new LinkedList<>();

    public T obtain() {
        return pool.isEmpty() ? createNew() : pool.poll();
    }

    public void free(T object) {
        pool.offer(object);
    }
}

// 사용
ObjectPool<Card> cardPool = new ObjectPool<>();
```

#### 8.3 터치 입력 처리

**PC 마우스 → 모바일 터치 변환**:

```java
// 원본 (마우스)
if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
    handleClick(Gdx.input.getX(), Gdx.input.getY());
}

// 모바일용 (터치)
if (Gdx.input.isTouched()) {
    handleTouch(Gdx.input.getX(), Gdx.input.getY());
}
```

---

## 전체 워크플로우 요약

```
1. PC판 디컴파일
   ├─ jadx로 desktop-1.0.jar 디컴파일
   └─ 소스 코드 정리

2. libGDX 프로젝트 생성
   ├─ Project Generator 사용
   └─ build.gradle 설정

3. 소스 통합
   ├─ 디컴파일 코드 → core/src/
   ├─ Ascension 100 모드 통합
   └─ 주석으로 모드 구분

4. 리소스 통합
   ├─ PC 게임 에셋 → android/assets/
   └─ 경로 수정 (절대 → 상대)

5. 런처 작성
   ├─ AndroidLauncher.java
   └─ AndroidManifest.xml

6. 빌드
   ├─ Desktop 빌드 (테스트)
   └─ Android APK 빌드

7. 테스트 및 최적화
   ├─ 기능 테스트
   ├─ 성능 최적화
   └─ 버그 수정

8. 배포
   └─ 개인 기기 설치
```

---

## 예상 문제와 해결책

### 문제 1: 누락된 라이브러리

**증상**:
```
error: cannot find symbol
  symbol:   class SomeLibraryClass
  location: package com.external.library
```

**해결**:
```gradle
// build.gradle에 의존성 추가
dependencies {
    api "com.external:library:1.0.0"
}
```

**라이브러리 찾기**:
- PC 게임 폴더의 lib/ 디렉토리 확인
- JAR 파일 확인
- Maven Central에서 검색

### 문제 2: 네이티브 라이브러리 의존성

**증상**:
```
UnsatisfiedLinkError: no gdx64 in java.library.path
```

**해결**:
```gradle
// android/build.gradle
dependencies {
    // 모든 아키텍처용 네이티브 라이브러리
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86"
    natives "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64"
}
```

### 문제 3: 리소스 경로 문제

**증상**:
```
GdxRuntimeException: File not found: images/card.png
```

**해결**:
```java
// 로그 추가로 실제 경로 확인
System.out.println("Asset path: " + Gdx.files.getLocalStoragePath());

// 경로 수정
Texture texture = new Texture(Gdx.files.internal("images/card.png"));
```

### 문제 4: 화면 해상도 차이

**증상**: UI가 너무 작거나 큼

**해결**:
```java
// 화면 크기 감지
float screenWidth = Gdx.graphics.getWidth();
float screenHeight = Gdx.graphics.getHeight();

// UI 스케일 조정
float scale = Math.min(screenWidth / 1920f, screenHeight / 1080f);
batch.setScale(scale);
```

---

## 최종 체크리스트

### 빌드 전
- [ ] 디컴파일 코드 정리 완료
- [ ] 모든 모드 코드 통합
- [ ] 리소스 경로 수정
- [ ] 의존성 라이브러리 확인
- [ ] AndroidManifest.xml 설정

### 빌드
- [ ] Desktop 빌드 성공
- [ ] Android Debug APK 빌드 성공
- [ ] 컴파일 에러 없음
- [ ] 경고 확인 및 수정

### 테스트
- [ ] 게임 시작 확인
- [ ] 메인 메뉴 작동
- [ ] 전투 시스템 작동
- [ ] 승천 레벨 변경 확인
- [ ] 모든 Ascension 100 레벨 테스트

### 최적화
- [ ] 메모리 사용량 확인
- [ ] 프레임율 측정 (60 FPS 목표)
- [ ] 배터리 소모 확인
- [ ] 앱 크기 확인 (APK)

---

## 성공 가능성 평가

### ✅ 가능한 이유

1. **libGDX는 크로스 플랫폼**
   - core 코드는 플랫폼 독립적
   - 런처만 변경하면 됨

2. **PC판 디컴파일 소스 있음**
   - 전체 게임 로직 접근 가능
   - 직접 수정 가능

3. **공식 모바일 버전 존재**
   - 이미 모바일 최적화됨
   - 동일한 core 코드 사용

### ⚠️ 어려운 점

1. **디컴파일 코드 품질**
   - 변수명 손실
   - 일부 로직 불완전
   - 수동 수정 필요

2. **의존성 라이브러리**
   - 모든 외부 라이브러리 찾기
   - 버전 호환성 맞추기

3. **리소스 관리**
   - 대용량 에셋
   - 모바일 최적화 필요

4. **시간 소요**
   - 첫 빌드까지 수일~수주
   - 버그 수정에 추가 시간

### 💡 추천 접근법

**단계적 접근**:

```
Phase 1 (1-2일): 기본 빌드
├─ libGDX 프로젝트 생성
├─ 디컴파일 코드 복사
└─ 첫 빌드 시도

Phase 2 (3-7일): 컴파일 성공
├─ 컴파일 에러 수정
├─ 의존성 해결
└─ 리소스 통합

Phase 3 (1-2주): 게임 실행
├─ 런처 완성
├─ 게임 시작 성공
└─ 기본 기능 작동

Phase 4 (추가 시간): 모드 통합
├─ Ascension 100 코드 추가
├─ 테스트 및 버그 수정
└─ 최적화
```

---

## 결론

**가능합니다!** 하지만:

✅ **장점**:
- 완전한 코드 제어
- 모든 기능 수정 가능
- 영구적인 모드

❌ **단점**:
- 매우 시간 소요
- 기술적 난이도 높음
- 게임 업데이트 시 재작업 필요

**현실적인 권장**:
1. **먼저 Frida 시도** (1-2일) - 빠른 테스트
2. **마음에 들면 포팅 시작** (2-4주) - 영구적 해결

궁금한 점이나 막히는 부분이 있으면 언제든 물어보세요! 🚀
