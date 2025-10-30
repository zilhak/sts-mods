# Slay the Spire Mods Collection

Gradle 기반 멀티모듈 프로젝트로 Slay the Spire 모드를 개발합니다. 각 모듈은 독립적으로 빌드되어 개별 JAR 파일로 생성됩니다.

## 📦 모듈

### 1. **Ascension 100** (`ascension-100/`)
Slay the Spire의 승천 모드를 20레벨에서 100레벨까지 확장합니다.

**현재 구현 상태**:
- ✅ UI 확장: 캐릭터 선택 화면에서 승천 1~100 선택 가능
- ✅ 로컬라이제이션: 한국어/영어 전체 번역 (100레벨)
- ✅ 승천 21+ 효과: 모든 적 체력 20% 증가 (테스트용)
- 🚧 승천 22~100 효과: 향후 구현 예정

**빌드 결과**: `Ascension100.jar` (14.0KB)

### 2. **Custom Relics** (`custom-relics/`)
고유하고 강력한 커스텀 유물을 추가하여 전략적 선택지를 확장합니다.

**현재 구현 상태**:
- ✅ 기본 구조 및 예제 유물
- 🚧 추가 유물: 향후 구현 예정

**빌드 결과**: `CustomRelics.jar` (6.1KB)

## 🛠️ 필수 요구사항

### 소프트웨어
- **Java JDK 8+** (JDK 25까지 테스트 완료)
  - 권장: JDK 17 (LTS) 또는 JDK 21 (LTS)
  - 최소: JDK 8
  - JDK 25 사용 시: [`JDK25-SETUP.md`](JDK25-SETUP.md) 참고
- **Gradle 9.1+** (Gradle Wrapper 포함됨)
- **Slay the Spire** (Steam 설치)

### 필수 모드 (Steam Workshop 또는 수동 설치)
1. **ModTheSpire** (v3.29.3+) - 모드 로더
2. **BaseMod** (v5.48.0+) - 필수 모딩 API
3. **StSLib** (v2.3.0+) - 추가 유틸리티 (선택사항)

## ⚙️ 설치 및 빌드

### 1. 환경 변수 설정

`STS_INSTALL_DIR` 환경 변수를 Slay the Spire 설치 경로로 설정합니다.

**Windows**:
```bash
set STS_INSTALL_DIR=C:\Program Files (x86)\Steam\steamapps\common\SlayTheSpire
```

**Linux/Mac**:
```bash
export STS_INSTALL_DIR=~/.steam/steam/steamapps/common/SlayTheSpire
```

또는 `gradle.properties` 파일을 생성하여 설정:
```properties
stsInstallDir=C:/Program Files (x86)/Steam/steamapps/common/SlayTheSpire
```

### 2. 의존성 확인

Slay the Spire 설치 경로에 다음 파일들이 있는지 확인:
```
SlayTheSpire/
├── desktop-1.0.jar          # 기본 게임
└── mods/
    ├── ModTheSpire.jar      # 모드 로더
    ├── BaseMod.jar          # 모딩 API
    └── StSLib.jar           # 선택사항
```

### 3. 환경 검증 (선택사항)

**Windows**:
```cmd
verify-setup.bat
```

**Linux/Mac**:
```bash
./verify-setup.sh
```

검증 항목:
- ✓ Java 설치 및 버전
- ✓ Gradle 설치
- ✓ STS 설치 경로
- ✓ 필수 모드 파일
- ✓ 컴파일 호환성

### 4. 빌드

**모든 모듈 빌드**:
```bash
./gradlew build
```

**특정 모듈만 빌드**:
```bash
# Ascension 100만 빌드
./gradlew :ascension-100:build

# Custom Relics만 빌드
./gradlew :custom-relics:build
```

**빌드 결과**:
```
build/libs/
├── Ascension100.jar    # 14.0KB
└── CustomRelics.jar    # 6.1KB
```

## 🚀 모드 설치

### 1. JAR 파일 복사

빌드된 JAR 파일을 ModTheSpire mods 폴더로 복사합니다.

**Windows**:
```bash
copy build\libs\*.jar "%LOCALAPPDATA%\ModTheSpire\mods\"
```

**Linux/Mac**:
```bash
cp build/libs/*.jar ~/.config/ModTheSpire/mods/
```

### 2. 게임 실행

1. ModTheSpire를 통해 게임 실행
2. 모드 목록에서 원하는 모드 활성화
3. Play 버튼 클릭

## 🎮 모드 사용법

### Ascension 100

1. 캐릭터 선택 화면에서 Ascension 모드 선택
2. 좌우 화살표로 승천 레벨 조정 (1~100)
3. 레벨 21부터는 모든 적 체력이 20% 증가
4. 각 레벨의 효과는 화면에 표시됨 (한국어/영어)

### Custom Relics

- 게임 플레이 중 유물 보상에서 커스텀 유물이 등장합니다
- 현재는 예제 유물만 포함되어 있습니다

## 🧪 개발 가이드

### 프로젝트 구조

```
sts-mods/
├── build.gradle                          # 루트 Gradle 설정
├── settings.gradle                       # 멀티모듈 설정
├── gradle/wrapper/                       # Gradle Wrapper
├── build/libs/                           # 빌드 결과 (collectJars 태스크)
├── ascension-100/                        # Ascension 100 모듈
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/stsmod/ascension100/
│       │   ├── Ascension100Mod.java      # 메인 모드 클래스
│       │   └── patches/
│       │       ├── Ascension100Patches.java    # UI 확장 패치
│       │       └── MonsterHealthPatch.java     # 체력 증가 패치
│       └── resources/ascension100Resources/
│           ├── ModTheSpire.json          # 모드 메타데이터
│           └── localization/
│               ├── eng/UIStrings.json    # 영어 번역
│               └── kor/UIStrings.json    # 한국어 번역
└── custom-relics/                        # Custom Relics 모듈
    ├── build.gradle
    └── src/main/
        ├── java/com/stsmod/relics/
        │   ├── CustomRelicsMod.java
        │   └── relics/ExampleRelic.java
        └── resources/customrelicsResources/
            ├── ModTheSpire.json
            ├── localization/
            └── images/relics/
```

### 새 모듈 추가

1. **디렉토리 생성**:
```bash
mkdir my-new-mod
```

2. **`settings.gradle`에 추가**:
```groovy
include 'ascension-100'
include 'custom-relics'
include 'my-new-mod'  // 추가
```

3. **`my-new-mod/build.gradle` 생성** (기존 모듈 참고)

4. **모드 코드 구현**

### SpirePatch 사용 시 주의사항

**❌ 잘못된 예 (임의로 메서드명 지어냄)**:
```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
    method = "getMaxAscensionLevel"  // 존재하지 않는 메서드!
)
```

**✅ 올바른 예 (실제 존재하는 메서드 사용)**:
```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.screens.charSelect.CharacterOption",
    method = "incrementAscensionLevel",  // 실제 메서드
    paramtypes = {"int"}
)
```

**메서드 찾는 방법**:
1. 다른 작동하는 모드의 소스코드 참고 (예: Ascension Reborn)
2. desktop-1.0.jar 디컴파일하여 실제 메서드 확인
3. BaseMod/ModTheSpire 위키 문서 참고

### 리소스 경로 규칙

**⚠️ 중요**: 모든 모드 리소스는 `{modId}Resources/` 폴더 아래에 위치해야 합니다.

**❌ 잘못된 구조**:
```
src/main/resources/
└── localization/
    └── eng/
        └── UIStrings.json
```

**✅ 올바른 구조**:
```
src/main/resources/
└── ascension100Resources/        # modId + "Resources"
    └── localization/
        └── eng/
            └── UIStrings.json
```

### IDE 설정 (IntelliJ IDEA)

1. **프로젝트 열기**: File → Open → `build.gradle` 선택
2. **JDK 설정**: File → Project Structure → SDK: Java 8+
3. **Gradle 새로고침**: View → Tool Windows → Gradle → 새로고침 아이콘
4. **빌드**: Gradle → Tasks → build → build 더블클릭
5. **실행**: ModTheSpire를 외부 도구로 설정

## 🔧 트러블슈팅

### 로컬라이제이션 파일을 찾을 수 없음
```
File not found: ascension100Resources\localization\kor\UIStrings.json
```

**해결방법**:
1. 리소스 폴더 구조 확인: `{modId}Resources/localization/{lang}/`
2. 파일이 `src/main/resources/` 아래에 있는지 확인
3. 로컬라이제이션 코드에 폴백 로직 추가:
```java
if (!Gdx.files.internal(localizationPath).exists()) {
    logger.warn("Localization file not found: " + localizationPath);
    language = "eng";  // 영어로 폴백
}
```

### Badge 이미지 NullPointerException
```
java.lang.NullPointerException at basemod.ModBadge.<init>
```

**해결방법**:
- Badge 이미지가 없으면 등록하지 않도록 null 체크:
```java
Texture badgeTexture = null;
if (Gdx.files.internal(badgePath).exists()) {
    badgeTexture = ImageMaster.loadImage(badgePath);
}

if (badgeTexture != null) {
    BaseMod.registerModBadge(badgeTexture, MOD_NAME, AUTHOR, DESCRIPTION, settingsPanel);
}
```

### SpirePatch 메서드를 찾을 수 없음
```
NoSuchMethodException: No method named [getMaxAscensionLevel] found
```

**해결방법**:
1. 실제 존재하는 메서드명 사용 (위 "SpirePatch 사용 시 주의사항" 참고)
2. 작동하는 다른 모드 소스코드 참고
3. 메서드명을 임의로 짓지 말 것

### Gradle 빌드 실패

**"desktop-1.0.jar을 찾을 수 없습니다"**:
- `STS_INSTALL_DIR` 환경 변수 확인
- `gradle.properties`에 경로 설정 확인
- Slay the Spire 설치 경로 확인

**컴파일 오류**:
```bash
./gradlew clean build --refresh-dependencies
```

### 게임 실행 시 ClassNotFoundException

- ModTheSpire와 BaseMod이 설치되어 있는지 확인
- JAR 파일이 올바른 mods 폴더에 있는지 확인
- `ModTheSpire.json`의 의존성 확인

## 📚 참고 자료

- **ModTheSpire Wiki**: https://github.com/kiooeht/ModTheSpire/wiki
- **BaseMod Documentation**: https://github.com/daviscook477/BaseMod/wiki
- **Discord**: Slay the Spire Modding Community
- **Example Mods**: https://github.com/topics/slay-the-spire-mod
- **Ascension Reborn** (참고한 모드): https://github.com/BetaChess/Ascension-Reborn

## 📄 라이선스

이 프로젝트는 교육 및 모딩 목적으로 제공됩니다.

## 🤝 기여

각 모듈은 독립적입니다. 자유롭게:
- 새 모듈 추가
- 기존 모드 개선
- 개선사항 공유

---

**Happy Modding!** 🎮
