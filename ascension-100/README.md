# Ascension 100 (승천 100)

Slay the Spire의 승천 모드를 20레벨에서 100레벨까지 확장하는 모드입니다.

## 📋 현재 구현 상태

### ✅ 완료
- **UI 확장**: 캐릭터 선택 화면에서 승천 1~100 레벨 선택 가능
- **로컬라이제이션**: 한국어/영어 전체 번역 (1~100 레벨)
- **승천 21 효과**: 모든 적 체력 20% 증가 (테스트 구현)

### 🚧 진행 중
- **승천 22~100 효과**: 각 레벨별 게임플레이 효과 구현 중

## 🎮 기능

- **100개 승천 레벨**: 레벨 1부터 100까지 점진적 난이도 증가
- **다국어 지원**: 한국어, 영어 완벽 지원
- **기존 모드 호환**: 베이스 게임 캐릭터 및 다른 모드와 호환
- **세밀한 난이도 조정**: 각 레벨마다 고유한 수정자 추가

## 🔨 빌드

프로젝트 루트에서:
```bash
./gradlew :ascension-100:build
```

빌드 결과: `build/libs/Ascension100.jar` (14.0KB)

## 📦 설치

1. 모드 빌드 (위 참조)
2. JAR 파일을 ModTheSpire mods 폴더로 복사:
   - **Windows**: `%LOCALAPPDATA%\ModTheSpire\mods\`
   - **Linux/Mac**: `~/.config/ModTheSpire/mods/`
3. ModTheSpire 런처에서 모드 활성화

## 🎯 사용법

1. 게임 실행 후 캐릭터 선택 화면으로 이동
2. Ascension 모드 선택
3. 좌우 화살표로 승천 레벨 조정 (1~100)
4. 각 레벨의 효과는 화면 하단에 표시됨

## 📊 승천별 변경사항

### 레벨 1-20 (기본 게임)
기본 게임의 승천 효과가 그대로 적용됩니다.

### 레벨 21-40

<!-- 여기에 내용을 채워넣으세요 -->

### 레벨 41-60

<!-- 여기에 내용을 채워넣으세요 -->

### 레벨 61-80

<!-- 여기에 내용을 채워넣으세요 -->

### 레벨 81-100

<!-- 여기에 내용을 채워넣으세요 -->

## 🛠️ 개발

### 프로젝트 구조

```
ascension-100/
├── build.gradle                          # Gradle 빌드 설정
└── src/main/
    ├── java/com/stsmod/ascension100/
    │   ├── Ascension100Mod.java          # 메인 모드 클래스
    │   └── patches/
    │       ├── Ascension100Patches.java  # UI 확장 패치
    │       └── MonsterHealthPatch.java   # 몬스터 체력 수정 패치
    └── resources/ascension100Resources/
        ├── ModTheSpire.json              # 모드 메타데이터
        └── localization/
            ├── eng/UIStrings.json        # 영어 번역
            └── kor/UIStrings.json        # 한국어 번역
```

### 주요 클래스

#### `Ascension100Mod.java`
- 모드 초기화 및 BaseMod 구독
- 로컬라이제이션 로딩
- 모드 설정 패널

#### `Ascension100Patches.java`
- `incrementAscensionLevel`: 승천 레벨 증가 (최대 100)
- `decrementAscensionLevel`: 승천 레벨 감소 (최소 0)
- `updateHitbox`: 승천 설명 텍스트 업데이트

#### `MonsterHealthPatch.java`
- `AbstractMonster.init()` 패치
- 승천 21+ 레벨에서 몬스터 체력 증가 적용

### 새로운 승천 효과 추가하기

1. **패치 클래스 생성**
   ```java
   @SpirePatch(
       cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",
       method = "실제로_존재하는_메서드"
   )
   public static class YourPatch {
       public static void Postfix(AbstractMonster __instance) {
           if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 21) {
               // 승천 효과 구현
           }
       }
   }
   ```

2. **승천 레벨 확인**
   ```java
   if (AbstractDungeon.isAscensionMode) {
       int level = AbstractDungeon.ascensionLevel;
       // 레벨에 따른 처리
   }
   ```

3. **로컬라이제이션 업데이트**
   - `localization/eng/UIStrings.json` 수정
   - `localization/kor/UIStrings.json` 수정
   - `AscensionModeDescriptions.TEXT` 배열에 설명 추가

### 참고 자료

- **Ascension Reborn 모드**: https://github.com/BetaChess/Ascension-Reborn
  - 승천 확장 모드의 좋은 참고 예시
  - 실제 게임 메서드 사용법 확인 가능
- **ModTheSpire Wiki**: https://github.com/kiooeht/ModTheSpire/wiki
- **BaseMod Wiki**: https://github.com/daviscook477/BaseMod/wiki

## 📝 TODO

- [ ] 승천 22-100 게임플레이 효과 구현
- [ ] 각 레벨별 밸런스 테스트 및 조정
- [ ] 설정 패널 추가 (특정 레벨 효과 활성화/비활성화)
- [ ] 고승천 클리어 통계 추적
- [ ] 프리셋 시스템 (커스텀 난이도 조합)

## 🤝 기여

환영합니다:
- 새로운 승천 수정자 추가
- 난이도 밸런스 조정
- 새로운 메커닉 제안
- 버그 리포트

## 📄 라이선스

모딩 목적으로 제공됩니다.

---

**행운을 빕니다!** 🎲
