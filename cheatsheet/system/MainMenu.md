# 메인 메뉴 시스템 (MainMenu System)

게임 초기화 과정과 메인 메뉴 화면 구성에 대한 상세 문서

---

## 1. 게임 시작 과정 (CardCrawlGame 초기화)

### create() 메서드 라인별 분석

게임이 시작되면 `CardCrawlGame.create()`가 호출되며, 다음 순서로 초기화됩니다:

#### 버전 정보 설정 (209-215)
```java
if (Settings.isAlpha) {
    TRUE_VERSION_NUM += " ALPHA";
    VERSION_NUM += " ALPHA";
} else if (Settings.isBeta) {
    VERSION_NUM += " BETA";
}
```
- 알파/베타 버전 표시 설정
- 이후 화면에 표시될 버전 문자열 구성

#### 배포 플랫폼 설정 (217-224)
```java
BuildSettings buildSettings = new BuildSettings(Gdx.files.internal("build.properties").reader());
publisherIntegration = DistributorFactory.getEnabledDistributor(buildSettings.getDistributor());
```
- Steam, GOG 등 배포 플랫폼 감지
- 플랫폼별 통합 기능 활성화 (업적, 클라우드 세이브 등)

#### 세이브 마이그레이션 (227)
```java
saveMigration();
```
- **중요**: 이전 버전 세이브 파일 변환
- 누락 시 세이브 손실 가능성

#### 프리퍼런스 로드 (229-242)
```java
saveSlotPref = SaveHelper.getPrefs("STSSaveSlots");
saveSlot = saveSlotPref.getInteger("DEFAULT_SLOT", 0);
playerPref = SaveHelper.getPrefs("STSPlayer");
playerName = saveSlotPref.getString(SaveHelper.slotName("PROFILE_NAME", saveSlot), "");
alias = playerPref.getString("alias", "");
```
- 세이브 슬롯 정보 로드
- 플레이어 이름 및 별칭 복원
- **순서 중요**: Settings 초기화 전에 먼저 로드해야 함

#### Settings 초기화 (245)
```java
Settings.initialize(false);
```
- **핵심 단계**: 화면 해상도, 스케일, 언어 등 전역 설정
- 모든 UI 요소의 위치/크기 계산의 기반
- **순서 위반 시**: UI 요소 위치 잘못 계산, 크래시 발생

#### 카메라 및 뷰포트 설정 (248-258)
```java
this.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
if (Settings.VERT_LETTERBOX_AMT != 0 || Settings.HORIZ_LETTERBOX_AMT != 0) {
    // 레터박스 처리
    viewport = new FitViewport(Settings.WIDTH, (Settings.M_H - Settings.HEIGHT / 2), (Camera)this.camera);
} else {
    viewport = new FitViewport(Settings.WIDTH, Settings.HEIGHT, (Camera)this.camera);
}
```
- **Settings 의존**: Settings.WIDTH, Settings.HEIGHT 사용
- **순서 위반 시**: 화면 렌더링 영역 계산 오류

#### 다국어 및 UI 팝업 초기화 (260-262)
```java
languagePack = new LocalizedStrings();
cardPopup = new SingleCardViewPopup();
relicPopup = new SingleRelicViewPopup();
```
- 다국어 텍스트 로드
- 카드/유물 확대 팝업 생성

#### SpriteBatch 생성 (268-269)
```java
this.sb = new SpriteBatch();
psb = new PolygonSpriteBatch();
```
- 렌더링 엔진 초기화
- **순서 중요**: 이미지 로드 전에 생성되어야 함

#### 오디오 시스템 초기화 (271-272)
```java
music = new MusicMaster();
sound = new SoundMaster();
```
- BGM 및 효과음 관리자 생성

#### 게임 데이터 초기화 (274-287)
```java
AbstractCreature.initialize();        // 생명체 기본 데이터
AbstractCard.initialize();            // 카드 기본 데이터
GameDictionary.initialize();          // 게임 용어 사전
ImageMaster.initialize();             // 이미지 리소스 로드
AbstractPower.initialize();           // 파워 데이터
FontHelper.initialize();              // 폰트 로드
AbstractCard.initializeDynamicFrameWidths();
UnlockTracker.initialize();           // 업적/해금 추적
CardLibrary.initialize();             // 카드 라이브러리
RelicLibrary.initialize();            // 유물 라이브러리
InputHelper.initialize();             // 입력 처리
TipTracker.initialize();              // 툴팁 추적
ModHelper.initialize();               // 모드 헬퍼
ShaderHelper.initializeShaders();     // 쉐이더
```

**초기화 순서가 중요한 이유:**
1. **ImageMaster** → 이미지 로드, 다른 초기화에서 이미지 참조 필요
2. **FontHelper** → 폰트 로드, UI 텍스트 렌더링에 필수
3. **CardLibrary/RelicLibrary** → ImageMaster, FontHelper 의존

#### 컨트롤러 및 Steam 초기화 (289-291)
```java
CInputHelper.loadSettings();
clientUtils = new SteamUtils(this.clUtilsCallback);
this.steamInputHelper = new SteamInputHelper();
```

#### 게임 시스템 객체 생성 (293-299)
```java
steelSeries = new SteelSeries();
cursor = new GameCursor();
metricData = new MetricData();
cancelButton = new CancelButton();
tips = new GameTips();
characterManager = new CharacterManager();
```

#### 스플래시 화면 시작 (299-300)
```java
splashScreen = new SplashScreen();
mode = GameMode.SPLASH;
```
- 게임 모드를 SPLASH로 설정
- 이후 update()에서 스플래시 화면 표시

---

## 2. 메인 메뉴 화면 구성

### MainMenuScreen 생성자 분석

스플래시 화면이 끝나면 `new MainMenuScreen()`이 호출됩니다 (CardCrawlGame.java:785)

#### 생성자 초기화 순서 (MainMenuScreen.java:119-143)

```java
public MainMenuScreen(boolean playBgm) {
    // 1. Steam 통합 설정
    CardCrawlGame.publisherIntegration.setRichPresenceDisplayInMenu();

    // 2. 플레이어 객체 초기화
    AbstractDungeon.player = null;

    // 3. BGM 재생
    if (playBgm) {
        CardCrawlGame.music.changeBGM("MENU");
        if (Settings.AMBIANCE_ON) {
            this.windId = CardCrawlGame.sound.playAndLoop("WIND");
        }
    }

    // 4. 해금 정보 갱신
    UnlockTracker.refresh();

    // 5. 하위 화면 초기화
    this.cardLibraryScreen.initialize();
    this.charSelectScreen.initialize();

    // 6. UI 요소 배치
    this.nameEditHb.move(200.0F * Settings.scale, Settings.HEIGHT - 50.0F * Settings.scale);

    // 7. 메뉴 버튼 생성
    setMainMenuButtons();

    // 8. 런 히스토리 스크린 생성
    this.runHistoryScreen = new RunHistoryScreen();
}
```

### 메뉴 버튼 배치 로직 (MainMenuScreen.java:145-170)

```java
private void setMainMenuButtons() {
    this.buttons.clear();
    int index = 0;

    // 1. PC 전용 버튼 (모바일/콘솔 제외)
    if (!Settings.isMobile && !Settings.isConsoleBuild) {
        this.buttons.add(new MenuButton(MenuButton.ClickResult.QUIT, index++));
        this.buttons.add(new MenuButton(MenuButton.ClickResult.PATCH_NOTES, index++));
    }

    // 2. 설정 버튼 (모든 플랫폼)
    this.buttons.add(new MenuButton(MenuButton.ClickResult.SETTINGS, index++));

    // 3. 통계/정보 버튼 (데모 버전 제외)
    if (!Settings.isShowBuild && this.statsScreen.statScreenUnlocked()) {
        this.buttons.add(new MenuButton(MenuButton.ClickResult.STAT, index++));
        this.buttons.add(new MenuButton(MenuButton.ClickResult.INFO, index++));
    }

    // 4. 플레이 또는 이어하기 버튼 (세이브 파일 존재 여부)
    if (CardCrawlGame.characterManager.anySaveFileExists()) {
        this.buttons.add(new MenuButton(MenuButton.ClickResult.ABANDON_RUN, index++));
        this.buttons.add(new MenuButton(MenuButton.ClickResult.RESUME_GAME, index++));
    } else {
        this.buttons.add(new MenuButton(MenuButton.ClickResult.PLAY, index++));
    }
}
```

**버튼 배치 계산 (MenuButton.java:49-69):**
```java
// 터치스크린/모바일
this.hb = new Hitbox(
    FontHelper.getSmartWidth(FontHelper.losePowerFont, this.label, 9999.0F, 1.0F) * 1.25F + 100.0F * Settings.scale,
    SPACE_Y * 2.0F
);
this.hb.move(this.hb.width / 2.0F + 75.0F * Settings.scale, START_Y + index * SPACE_Y * 2.0F);

// PC
this.hb = new Hitbox(
    FontHelper.getSmartWidth(FontHelper.buttonLabelFont, this.label, 9999.0F, 1.0F) + 100.0F * Settings.scale,
    SPACE_Y
);
this.hb.move(this.hb.width / 2.0F + 75.0F * Settings.scale, START_Y + index * SPACE_Y);
```

**상수 정의:**
- `START_Y = 120.0F * Settings.scale` - 첫 버튼의 Y 좌표
- `SPACE_Y = 50.0F * Settings.scale` - 버튼 간 간격
- `FONT_X = 120.0F * Settings.scale` - 텍스트 X 오프셋

---

## 3. 메뉴 버튼 종류와 기능

### MenuButton.ClickResult 열거형

```java
public enum ClickResult {
    PLAY,           // 새 게임 시작
    RESUME_GAME,    // 이어하기
    ABANDON_RUN,    // 런 포기
    INFO,           // 정보 (컴펜디움)
    STAT,           // 통계
    SETTINGS,       // 설정
    PATCH_NOTES,    // 패치노트
    QUIT            // 게임 종료
}
```

### 버튼 클릭 시 동작 (MenuButton.java:152-184)

```java
public void buttonEffect() {
    switch (this.result) {
        case PLAY:
            CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.PLAY);
            break;

        case RESUME_GAME:
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.NONE;
            CardCrawlGame.mainMenuScreen.hideMenuButtons();
            CardCrawlGame.mainMenuScreen.darken();
            resumeGame();  // 세이브 로드 시작
            break;

        case ABANDON_RUN:
            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.ABANDON_CONFIRM;
            CardCrawlGame.mainMenuScreen.abandonPopup.show();
            break;

        case INFO:
            CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.COMPENDIUM);
            break;

        case STAT:
            CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.STATS);
            break;

        case SETTINGS:
            CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.SETTINGS);
            break;

        case PATCH_NOTES:
            CardCrawlGame.mainMenuScreen.patchNotesScreen.open();
            break;

        case QUIT:
            logger.info("Quit Game button clicked!");
            Gdx.app.exit();
            break;
    }
}
```

---

## 4. 메인 메뉴 초기화 순서 요약

### 전체 초기화 플로우

```
1. CardCrawlGame.create()
   ├─ 버전 정보 설정
   ├─ 배포 플랫폼 감지
   ├─ 세이브 마이그레이션
   ├─ 프리퍼런스 로드 (세이브 슬롯, 플레이어명)
   ├─ Settings.initialize() ★ 핵심
   ├─ 카메라/뷰포트 설정
   ├─ 다국어 팩 로드
   ├─ SpriteBatch 생성
   ├─ 오디오 시스템 초기화
   ├─ 게임 데이터 초기화
   │  ├─ ImageMaster (이미지)
   │  ├─ FontHelper (폰트)
   │  ├─ CardLibrary (카드)
   │  ├─ RelicLibrary (유물)
   │  └─ 기타 라이브러리
   ├─ 컨트롤러/Steam 초기화
   ├─ 게임 시스템 객체 생성
   └─ SplashScreen 시작

2. SplashScreen.update() → isDone = true

3. MainMenuScreen 생성 (CardCrawlGame.java:785)
   ├─ Steam 통합 설정
   ├─ BGM 재생
   ├─ UnlockTracker 갱신
   ├─ 하위 화면 초기화
   │  ├─ cardLibraryScreen
   │  └─ charSelectScreen
   ├─ UI 요소 배치
   └─ 메뉴 버튼 생성
      ├─ 세이브 파일 존재 여부 확인
      └─ 플랫폼별 버튼 추가
```

---

## 5. 메뉴에서 다른 화면으로 전환

### CurScreen 열거형 (MainMenuScreen.java:111-113)

```java
public enum CurScreen {
    CHAR_SELECT,        // 캐릭터 선택
    RELIC_VIEW,         // 유물 보기
    POTION_VIEW,        // 포션 보기
    BANNER_DECK_VIEW,   // 배너 덱 보기
    DAILY,              // 일일 도전
    TRIALS,             // 트라이얼
    SETTINGS,           // 설정
    MAIN_MENU,          // 메인 메뉴
    SAVE_SLOT,          // 세이브 슬롯
    STATS,              // 통계
    RUN_HISTORY,        // 런 히스토리
    CARD_LIBRARY,       // 카드 도감
    CREDITS,            // 크레딧
    PATCH_NOTES,        // 패치노트
    NONE,               // 없음
    LEADERBOARD,        // 리더보드
    ABANDON_CONFIRM,    // 런 포기 확인
    PANEL_MENU,         // 패널 메뉴
    INPUT_SETTINGS,     // 입력 설정
    CUSTOM,             // 커스텀 모드
    NEOW_SCREEN,        // 네오우 화면
    DOOR_UNLOCK         // 문 해금
}
```

### 화면 전환 메커니즘

#### 1. PLAY 버튼 클릭 → 캐릭터 선택

```java
// MenuButton.buttonEffect() - PLAY case
CardCrawlGame.mainMenuScreen.panelScreen.open(MenuPanelScreen.PanelScreen.PLAY);

// panelScreen.open()이 내부적으로:
CardCrawlGame.mainMenuScreen.screen = CurScreen.CHAR_SELECT;
CardCrawlGame.mainMenuScreen.charSelectScreen.open();
```

#### 2. 캐릭터 선택 완료 → 게임 시작

```java
// MainMenuScreen.update() - CHAR_SELECT case (789-848)
if (mainMenuScreen.fadedOut) {
    // 경로 초기화
    AbstractDungeon.pathX = new ArrayList();
    AbstractDungeon.pathY = new ArrayList();

    // 플레이어 생성
    AbstractDungeon.player = createCharacter(chosenCharacter);

    // 카드 획득 추적
    for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
        if (c.rarity != AbstractCard.CardRarity.BASIC) {
            CardHelper.obtain(c.cardID, c.rarity, c.color);
        }
    }

    // 게임 모드 변경
    mode = GameMode.GAMEPLAY;
    nextDungeon = "Exordium";
    dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");
}
```

#### 3. RESUME_GAME 버튼 → 세이브 로드

```java
// MenuButton.resumeGame()
CardCrawlGame.loadingSave = true;
CardCrawlGame.chosenCharacter = (CardCrawlGame.characterManager.loadChosenCharacter()).chosenClass;
CardCrawlGame.mainMenuScreen.isFadingOut = true;
CardCrawlGame.mainMenuScreen.fadeOutMusic();
```

---

## 6. 주의사항 (초기화 누락 시 크래시 원인)

### 6.1 Settings.initialize() 누락 또는 순서 위반

**증상:**
- `NullPointerException` 발생
- UI 요소 위치 계산 오류 (0, 0 좌표에 모든 요소 배치)
- 화면 크기 관련 변수 미초기화

**원인:**
```java
// Settings.initialize() 전에 UI 요소 생성 시도
this.hb.move(200.0F * Settings.scale, Settings.HEIGHT - 50.0F * Settings.scale);
// Settings.scale이 초기화되지 않음 → NullPointerException 또는 0
```

**해결책:**
- `Settings.initialize()`를 가장 먼저 호출
- UI 요소 생성은 반드시 Settings 초기화 후

### 6.2 ImageMaster/FontHelper 초기화 순서

**증상:**
- `GdxRuntimeException: Texture not loaded`
- 폰트 렌더링 실패

**원인:**
```java
// 잘못된 순서
CardLibrary.initialize();  // 카드 이미지 필요
ImageMaster.initialize();   // 이미지 로드

// 올바른 순서
ImageMaster.initialize();   // 이미지 로드 먼저
CardLibrary.initialize();   // 카드 이미지 사용
```

**해결책:**
- `ImageMaster.initialize()` → `FontHelper.initialize()` → `CardLibrary/RelicLibrary`

### 6.3 SpriteBatch 생성 시점

**증상:**
- 렌더링 실패
- `IllegalStateException: SpriteBatch not initialized`

**원인:**
```java
// render()에서 SpriteBatch 사용 시도하는데 아직 미생성
this.sb.begin();  // sb가 null
```

**해결책:**
- `create()` 메서드 내에서 SpriteBatch 생성 필수
- 렌더링 전에 반드시 생성되어야 함

### 6.4 mainMenuScreen null 참조

**증상:**
- `NullPointerException at CardCrawlGame.mainMenuScreen`
- 메뉴 화면 렌더링 실패

**원인:**
```java
// SplashScreen 완료 전에 mainMenuScreen 접근
if (CardCrawlGame.mainMenuScreen.screen == ...) {  // mainMenuScreen이 아직 null
```

**해결책:**
- `mode == GameMode.CHAR_SELECT`일 때 mainMenuScreen이 생성됨을 보장
- null 체크 필수:
```java
if (mainMenuScreen != null && mainMenuScreen.screen == ...) {
```

### 6.5 던전 진입 시 필수 초기화 누락

**증상:**
- 던전 시작 후 크래시
- `NullPointerException` at AbstractDungeon

**원인:**
```java
// 층 진행 모드가 이 단계를 건너뛰면:
// AbstractDungeon.pathX, pathY 미초기화
// AbstractDungeon.player 미생성
```

**해결책:**
```java
// 반드시 초기화 필요
AbstractDungeon.pathX = new ArrayList();
AbstractDungeon.pathY = new ArrayList();
AbstractDungeon.player = createCharacter(chosenCharacter);

// 던전 생성
getDungeon("Exordium", AbstractDungeon.player);
```

### 6.6 카드 획득 추적 누락

**증상:**
- 카드 도감에 표시 안 됨
- 세이브 파일 손상

**원인:**
```java
// 시작 덱 카드 획득 추적 누락
for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
    if (c.rarity != AbstractCard.CardRarity.BASIC) {
        CardHelper.obtain(c.cardID, c.rarity, c.color);  // 이거 안 하면 문제
    }
}
```

---

## 7. 수정 방법 (모드 개발 가이드)

### 7.1 새 메뉴 버튼 추가

**단계:**

1. **ClickResult 열거형에 새 결과 추가:**
```java
public enum ClickResult {
    PLAY, RESUME_GAME, ..., CUSTOM_BUTTON  // 추가
}
```

2. **버튼 라벨 설정:**
```java
private void setLabel() {
    switch (this.result) {
        // ...
        case CUSTOM_BUTTON:
            this.label = "나만의 버튼";
            return;
    }
}
```

3. **버튼 동작 정의:**
```java
public void buttonEffect() {
    switch (this.result) {
        // ...
        case CUSTOM_BUTTON:
            // 커스텀 동작
            CardCrawlGame.mainMenuScreen.screen = CurScreen.CUSTOM;
            break;
    }
}
```

4. **메뉴에 버튼 추가:**
```java
private void setMainMenuButtons() {
    // ...
    this.buttons.add(new MenuButton(MenuButton.ClickResult.CUSTOM_BUTTON, index++));
}
```

### 7.2 게임 시작 시 추가 초기화

**BaseMod 훅 사용:**
```java
@SpireInitializer
public class MyMod implements PostInitializeSubscriber {
    @Override
    public void receivePostInitialize() {
        // MainMenuScreen 생성 후 실행됨
        // 여기서 추가 초기화 수행
    }
}
```

**주의사항:**
- `receivePostInitialize()`는 `mainMenuScreen` 생성 **후** 호출됨
- `Settings`, `ImageMaster` 등은 이미 초기화된 상태
- 안전하게 UI 요소 추가 가능

### 7.3 메뉴 화면 UI 수정

**MainMenuScreen 확장:**
```java
@SpirePatch(clz = MainMenuScreen.class, method = "render")
public static class RenderPatch {
    @SpirePostfixPatch
    public static void Postfix(MainMenuScreen __instance, SpriteBatch sb) {
        // 메뉴 화면에 커스텀 UI 렌더링
        sb.setColor(Color.WHITE);
        sb.draw(customImage, x, y, width, height);
    }
}
```

### 7.4 던전 진입 전 로직 삽입

**CharacterSelectScreen 패치:**
```java
@SpirePatch(clz = CharacterSelectScreen.class, method = "update")
public static class CharSelectPatch {
    @SpireInsertPatch(rloc = 0, localvars = {})
    public static void Insert(CharacterSelectScreen __instance) {
        // 캐릭터 선택 후, 던전 진입 전 실행
        if (CardCrawlGame.mainMenuScreen.fadedOut) {
            // 커스텀 초기화
            MyMod.customPreDungeonLogic();
        }
    }
}
```

---

## 8. 층 내려가는 모드 크래시 디버깅

### 문제 시나리오

층을 내려가는 모드가 메뉴에서 즉시 던전으로 진입하려 할 때:

**크래시 원인 체크리스트:**

1. **Settings 초기화 여부**
   - `Settings.initialize()` 호출 확인
   - `Settings.WIDTH`, `Settings.HEIGHT` 값 존재 확인

2. **AbstractDungeon 상태**
   - `AbstractDungeon.player` 생성 확인
   - `AbstractDungeon.pathX`, `pathY` 초기화 확인

3. **카드/유물 라이브러리 로드**
   - `CardLibrary.initialize()` 호출 확인
   - `RelicLibrary.initialize()` 호출 확인

4. **ImageMaster 로드**
   - 던전 이미지가 로드되었는지 확인
   - `ImageMaster.loadImage()` 성공 여부

5. **게임 모드 전환**
   - `mode = GameMode.GAMEPLAY` 설정 확인
   - `dungeonTransitionScreen` 생성 확인

**디버깅 코드:**
```java
@Override
public void receivePostInitialize() {
    logger.info("Settings.WIDTH: " + Settings.WIDTH);
    logger.info("Settings.HEIGHT: " + Settings.HEIGHT);
    logger.info("AbstractDungeon.player: " + AbstractDungeon.player);
    logger.info("CardLibrary initialized: " + (CardLibrary.cards.size() > 0));
    logger.info("mainMenuScreen: " + CardCrawlGame.mainMenuScreen);
}
```

**안전한 던전 진입 패턴:**
```java
// 메인 메뉴에서 던전으로 직접 진입 시
public void startDungeonDirectly() {
    // 1. 캐릭터 생성
    CardCrawlGame.chosenCharacter = AbstractPlayer.PlayerClass.IRONCLAD;
    AbstractDungeon.player = CardCrawlGame.characterManager.recreateCharacter(
        CardCrawlGame.chosenCharacter
    );

    // 2. 경로 초기화
    AbstractDungeon.pathX = new ArrayList<>();
    AbstractDungeon.pathY = new ArrayList<>();

    // 3. 카드 추적
    for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
        if (c.rarity != AbstractCard.CardRarity.BASIC) {
            CardHelper.obtain(c.cardID, c.rarity, c.color);
        }
    }

    // 4. 게임 모드 전환
    CardCrawlGame.mode = CardCrawlGame.GameMode.GAMEPLAY;
    CardCrawlGame.nextDungeon = "Exordium";
    CardCrawlGame.dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");

    // 5. 메뉴 페이드 아웃
    CardCrawlGame.mainMenuScreen.isFadingOut = true;
    CardCrawlGame.mainMenuScreen.fadeOutMusic();
}
```

---

## 9. 참고: GameMode 상태 전환

```java
public enum GameMode {
    CHAR_SELECT,        // 메인 메뉴 (캐릭터 선택)
    GAMEPLAY,           // 게임 플레이 중
    DUNGEON_TRANSITION, // 던전 전환 (사용 안 함, GAMEPLAY로 통합)
    SPLASH              // 스플래시 화면
}
```

**상태 전환 플로우:**
```
SPLASH (게임 시작)
  ↓ splashScreen.isDone = true
CHAR_SELECT (메인 메뉴)
  ↓ 캐릭터 선택 완료
GAMEPLAY (던전 플레이)
  ↓ 죽음/승리
CHAR_SELECT (메인 메뉴로 복귀)
```

---

## 요약

1. **초기화 순서가 생명**: Settings → ImageMaster → FontHelper → Libraries
2. **mainMenuScreen은 SPLASH 후 생성**: null 체크 필수
3. **던전 진입 전 필수 초기화**: player, pathX/pathY, 카드 추적
4. **모드 개발 시 BaseMod 훅 활용**: `receivePostInitialize()` 안전함
5. **크래시 디버깅**: 로그로 각 단계별 초기화 상태 확인

이 문서를 참고하여 층을 내려가는 모드의 초기화 누락 원인을 파악할 수 있습니다.
