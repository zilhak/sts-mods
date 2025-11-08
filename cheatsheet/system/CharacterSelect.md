# 캐릭터 선택 시스템 (Character Selection System)

캐릭터 선택 화면의 구조와 던전 진입까지의 전체 초기화 과정을 설명합니다.

## 목차
1. [캐릭터 선택 화면 진입](#1-캐릭터-선택-화면-진입)
2. [캐릭터 옵션 구성](#2-캐릭터-옵션-구성)
3. [캐릭터별 초기 데이터](#3-캐릭터별-초기-데이터)
4. [Ascension 레벨 선택](#4-ascension-레벨-선택)
5. [캐릭터 확정 및 던전 시작](#5-캐릭터-확정-및-던전-시작)
6. [필수 초기화 체크리스트](#6-필수-초기화-체크리스트)
7. [새 캐릭터 추가 가이드](#7-새-캐릭터-추가-가이드)

---

## 1. 캐릭터 선택 화면 진입

### CharacterSelectScreen.open(boolean isEndless)

캐릭터 선택 화면을 열 때 호출되는 진입점입니다.

```java
public void open(boolean isEndless) {
    Settings.isEndless = isEndless;           // Endless 모드 설정
    Settings.seedSet = false;                 // 시드 초기화
    Settings.seed = null;
    Settings.specialSeed = null;
    Settings.isTrial = false;                 // Trial 모드 초기화
    CardCrawlGame.trial = null;
    this.cancelButton.show(TEXT[5]);
    CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CHAR_SELECT;
}
```

**핵심 동작:**
- 게임 모드 플래그 설정 (일반/Endless)
- 이전 시드 정보 제거
- Trial 모드 플래그 초기화
- 화면 전환

---

## 2. 캐릭터 옵션 구성

### CharacterSelectScreen.initialize()

캐릭터 선택 화면 초기화 시 4개의 캐릭터 옵션을 생성합니다.

```java
public void initialize() {
    // 1. Ironclad (항상 잠금 해제)
    this.options.add(new CharacterOption(
        TEXT[2],
        CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.IRONCLAD),
        ImageMaster.CHAR_SELECT_IRONCLAD,
        ImageMaster.CHAR_SELECT_BG_IRONCLAD
    ));

    // 2. The Silent (잠금 조건 확인)
    if (!UnlockTracker.isCharacterLocked("The Silent")) {
        this.options.add(new CharacterOption(
            TEXT[3],
            CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.THE_SILENT),
            ImageMaster.CHAR_SELECT_SILENT,
            ImageMaster.CHAR_SELECT_BG_SILENT
        ));
    } else {
        this.options.add(new CharacterOption(
            CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.THE_SILENT)
        ));
    }

    // 3. Defect (잠금 조건 확인)
    if (!UnlockTracker.isCharacterLocked("Defect")) {
        this.options.add(new CharacterOption(
            TEXT[4],
            CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.DEFECT),
            ImageMaster.CHAR_SELECT_DEFECT,
            ImageMaster.CHAR_SELECT_BG_DEFECT
        ));
    } else {
        this.options.add(new CharacterOption(
            CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.DEFECT)
        ));
    }

    // 4. Watcher (잠금 조건 확인)
    if (!UnlockTracker.isCharacterLocked("Watcher")) {
        addCharacterOption(AbstractPlayer.PlayerClass.WATCHER);
    } else {
        this.options.add(new CharacterOption(
            CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.WATCHER)
        ));
    }

    positionButtons();  // 화면 배치 계산

    // Ascension 모드 설정 로드
    this.isAscensionMode = Settings.gamePref.getBoolean("Ascension Mode Default", false);
}
```

**CharacterOption 생성 패턴:**

1. **잠금 해제된 캐릭터:**
```java
new CharacterOption(displayName, player, buttonImg, portraitImg)
```

2. **잠긴 캐릭터:**
```java
new CharacterOption(player)  // 잠금 이미지 자동 설정
```

---

## 3. 캐릭터별 초기 데이터

### CharSelectInfo 구조

각 캐릭터는 `getLoadout()` 메서드를 통해 초기 데이터를 반환합니다.

```java
public CharSelectInfo getLoadout() {
    return new CharSelectInfo(
        name,           // 캐릭터 이름
        flavorText,     // 설명 텍스트
        currentHp,      // 시작 체력
        maxHp,          // 최대 체력
        maxOrbs,        // 최대 오브 슬롯 (0 for non-Defect)
        gold,           // 시작 골드
        cardDraw,       // 매 턴 카드 드로우 수
        player,         // AbstractPlayer 인스턴스
        relics,         // 시작 유물 목록
        deck,           // 시작 덱 목록
        resumeGame      // false (새 게임)
    );
}
```

### 캐릭터별 초기값 비교

| 캐릭터 | HP | 골드 | 카드 드로우 | 오브 슬롯 | 시작 유물 | 시작 덱 |
|--------|-----|------|-------------|-----------|-----------|---------|
| **Ironclad** | 80/80 | 99 | 5 | 0 | Burning Blood | 5x Strike_R<br>4x Defend_R<br>1x Bash |
| **The Silent** | 70/70 | 99 | 5 | 0 | Ring of the Snake | 5x Strike_G<br>5x Defend_G<br>1x Survivor<br>1x Neutralize |
| **Defect** | 75/75 | 99 | 5 | 3 | Cracked Core | 4x Strike_B<br>4x Defend_B<br>1x Zap<br>1x Dualcast |
| **Watcher** | 72/72 | 99 | 5 | 0 | Pure Water | 4x Strike_P<br>4x Defend_Watcher<br>1x Eruption<br>1x Vigilance |

### Ironclad 예시

```java
// Ironclad.java
public CharSelectInfo getLoadout() {
    return new CharSelectInfo(
        NAMES[0],                    // "Ironclad"
        TEXT[0],                     // "The remaining soldier..."
        80,                          // 시작 체력
        80,                          // 최대 체력
        0,                           // 오브 슬롯 없음
        99,                          // 시작 골드
        5,                           // 카드 드로우
        this,
        getStartingRelics(),         // ["Burning Blood"]
        getStartingDeck(),           // 10장 덱
        false
    );
}

public ArrayList<String> getStartingRelics() {
    ArrayList<String> retVal = new ArrayList<>();
    retVal.add("Burning Blood");
    UnlockTracker.markRelicAsSeen("Burning Blood");
    return retVal;
}

public ArrayList<String> getStartingDeck() {
    ArrayList<String> retVal = new ArrayList<>();
    retVal.add("Strike_R");  // x5
    retVal.add("Strike_R");
    retVal.add("Strike_R");
    retVal.add("Strike_R");
    retVal.add("Strike_R");
    retVal.add("Defend_R");  // x4
    retVal.add("Defend_R");
    retVal.add("Defend_R");
    retVal.add("Defend_R");
    retVal.add("Bash");      // x1
    return retVal;
}
```

---

## 4. Ascension 레벨 선택

### Ascension 모드 토글

사용자가 캐릭터를 선택하면 Ascension 모드 UI가 활성화됩니다.

```java
private void updateAscensionToggle() {
    if (this.isAscensionModeUnlocked) {
        if (this.anySelected) {
            this.ascensionModeHb.update();
            this.ascRightHb.update();
            this.ascLeftHb.update();
        }

        // 모드 토글
        if (this.ascensionModeHb.clicked) {
            this.isAscensionMode = !this.isAscensionMode;
            Settings.gamePref.putBoolean("Ascension Mode Default", this.isAscensionMode);
            Settings.gamePref.flush();
        }

        // 레벨 감소 (왼쪽 화살표)
        if (this.ascLeftHb.clicked) {
            for (CharacterOption o : this.options) {
                if (o.selected) {
                    o.decrementAscensionLevel(this.ascensionLevel - 1);
                    break;
                }
            }
        }

        // 레벨 증가 (오른쪽 화살표)
        if (this.ascRightHb.clicked) {
            for (CharacterOption o : this.options) {
                if (o.selected) {
                    o.incrementAscensionLevel(this.ascensionLevel + 1);
                    break;
                }
            }
        }
    }
}
```

### 레벨 저장 및 로드

```java
// CharacterOption.java
public void saveChosenAscensionLevel(int level) {
    Prefs pref = this.c.getPrefs();
    pref.putInteger("LAST_ASCENSION_LEVEL", level);
    pref.flush();
}

public void incrementAscensionLevel(int level) {
    if (level > this.maxAscensionLevel) {
        return;  // 최대 레벨 제한 (20)
    }

    saveChosenAscensionLevel(level);
    CardCrawlGame.mainMenuScreen.charSelectScreen.ascensionLevel = level;
    CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString =
        CharacterSelectScreen.A_TEXT[level - 1];
}
```

**Ascension 레벨 범위:**
- **최소:** 0 (일반 모드)
- **최대:** 20

**캐릭터별 진행도:**
- 각 캐릭터마다 독립적인 Ascension 레벨 저장
- `LAST_ASCENSION_LEVEL`: 마지막 선택한 레벨
- `ASCENSION_LEVEL`: 현재 해금된 최대 레벨

---

## 5. 캐릭터 확정 및 던전 시작

### Confirm 버튼 클릭 처리

```java
if (this.confirmButton.hb.clicked) {
    this.confirmButton.hb.clicked = false;
    this.confirmButton.isDisabled = true;
    this.confirmButton.hide();

    // 1. 시드 설정
    if (Settings.seed == null) {
        setRandomSeed();  // 랜덤 시드 생성
    } else {
        Settings.seedSet = true;  // 사용자 지정 시드 사용
    }

    // 2. 화면 전환 시작
    CardCrawlGame.mainMenuScreen.isFadingOut = true;
    CardCrawlGame.mainMenuScreen.fadeOutMusic();

    // 3. 모드 플래그 설정
    Settings.isDailyRun = false;
    boolean isTrialSeed = TrialHelper.isTrialSeed(
        SeedHelper.getString(Settings.seed.longValue())
    );

    if (isTrialSeed) {
        Settings.specialSeed = Settings.seed;
        long sourceTime = System.nanoTime();
        Random rng = new Random(Long.valueOf(sourceTime));
        Settings.seed = Long.valueOf(SeedHelper.generateUnoffensiveSeed(rng));
        Settings.isTrial = true;
    }

    // 4. 모드 초기화 및 시드 생성
    ModHelper.setModsFalse();
    AbstractDungeon.generateSeeds();

    // 5. Ascension 레벨 설정
    AbstractDungeon.isAscensionMode = this.isAscensionMode;
    if (this.isAscensionMode) {
        AbstractDungeon.ascensionLevel = this.ascensionLevel;
    } else {
        AbstractDungeon.ascensionLevel = 0;
    }

    // 6. 캐릭터 선택 확정
    this.confirmButton.hb.clicked = false;
    this.confirmButton.hide();

    // 7. 선택된 캐릭터 찾기
    CharacterOption selected = null;
    for (CharacterOption o : this.options) {
        if (o.selected) {
            selected = o;
        }
    }

    // 8. 통계 추적 (SteelSeries 이벤트)
    if (selected != null &&
        CardCrawlGame.steelSeries.isEnabled.booleanValue()) {
        CardCrawlGame.steelSeries.event_character_chosen(selected.c.chosenClass);
    }
}
```

### 랜덤 시드 생성

```java
private void setRandomSeed() {
    long sourceTime = System.nanoTime();
    Random rng = new Random(Long.valueOf(sourceTime));
    Settings.seedSourceTimestamp = sourceTime;
    Settings.seed = Long.valueOf(SeedHelper.generateUnoffensiveSeed(rng));
    Settings.seedSet = false;  // 사용자 지정이 아닌 랜덤 시드
}
```

---

## 6. 필수 초기화 체크리스트

### 캐릭터 선택 시 반드시 초기화해야 할 항목

#### 6.1 Settings 플래그
```java
✓ Settings.isEndless           // Endless 모드 여부
✓ Settings.seedSet              // 시드 설정 여부
✓ Settings.seed                 // 게임 시드
✓ Settings.specialSeed          // Trial 시드
✓ Settings.isTrial              // Trial 모드 여부
✓ Settings.isDailyRun           // 데일리 런 여부 (false)
```

#### 6.2 AbstractDungeon 초기화
```java
✓ AbstractDungeon.generateSeeds()          // RNG 시드 생성
✓ AbstractDungeon.isAscensionMode          // Ascension 모드 활성화
✓ AbstractDungeon.ascensionLevel           // Ascension 레벨 (0~20)
```

#### 6.3 캐릭터 데이터 (CharSelectInfo)
```java
✓ name              // 캐릭터 이름
✓ flavorText        // 설명 텍스트
✓ currentHp         // 시작 체력
✓ maxHp             // 최대 체력
✓ maxOrbs           // 오브 슬롯 수
✓ gold              // 시작 골드
✓ cardDraw          // 카드 드로우 수
✓ relics            // 시작 유물 목록
✓ deck              // 시작 덱 목록
```

#### 6.4 AbstractPlayer 초기화 (initializeClass)
```java
✓ img               // 캐릭터 이미지
✓ shoulderImg       // 어깨 이미지
✓ shoulder2Img      // 어깨2 이미지
✓ corpseImg         // 시체 이미지
✓ maxHealth         // 최대 체력 설정
✓ startingMaxHP     // 시작 최대 체력 기록
✓ currentHealth     // 현재 체력
✓ masterMaxOrbs     // 오브 슬롯
✓ energy            // 에너지 관리자
✓ masterHandSize    // 핸드 사이즈
✓ gameHandSize      // 게임 핸드 사이즈
✓ gold              // 골드
✓ displayGold       // 표시 골드
✓ hb                // 히트박스
✓ healthHb          // 체력바 히트박스
```

#### 6.5 덱 및 유물 초기화
```java
✓ initializeStarterDeck()      // 시작 덱 생성
✓ initializeStarterRelics()    // 시작 유물 획득
```

#### 6.6 Ascension 패널티 적용
```java
if (AbstractDungeon.ascensionLevel >= 11) {
    this.potionSlots--;  // 물약 슬롯 감소
}

// maxHealth 감소는 캐릭터별 getAscensionMaxHPLoss()에서 정의
if (AbstractDungeon.ascensionLevel >= 14) {
    this.maxHealth -= getAscensionMaxHPLoss();
    this.currentHealth = this.maxHealth;
}
```

---

## 7. 새 캐릭터 추가 가이드

### 7.1 AbstractPlayer 상속 클래스 작성

```java
package com.megacrit.cardcrawl.characters;

import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import java.util.ArrayList;

public class MyNewCharacter extends AbstractPlayer {

    public static final String[] NAMES = {"My Character"};
    public static final String[] TEXT = {"A new challenger appears!"};

    MyNewCharacter(String playerName) {
        super(playerName, PlayerClass.MY_CHARACTER);

        // 초기화
        initializeClass(
            null,  // img (skeleton animation 사용 시 null)
            "images/characters/mychar/shoulder2.png",
            "images/characters/mychar/shoulder.png",
            "images/characters/mychar/corpse.png",
            getLoadout(),
            0.0F,    // hb_x
            0.0F,    // hb_y
            220.0F,  // hb_w
            290.0F,  // hb_h
            new EnergyManager(3)  // 시작 에너지
        );

        // 애니메이션 로드 (선택사항)
        loadAnimation(
            "images/characters/mychar/idle/skeleton.atlas",
            "images/characters/mychar/idle/skeleton.json",
            1.0F
        );

        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTimeScale(0.6F);
    }

    @Override
    public CharSelectInfo getLoadout() {
        return new CharSelectInfo(
            NAMES[0],                    // 캐릭터 이름
            TEXT[0],                     // 설명
            75,                          // 시작 체력
            75,                          // 최대 체력
            0,                           // 오브 슬롯
            99,                          // 시작 골드
            5,                           // 카드 드로우
            this,
            getStartingRelics(),
            getStartingDeck(),
            false
        );
    }

    @Override
    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<>();
        retVal.add("MyStarterRelic");
        UnlockTracker.markRelicAsSeen("MyStarterRelic");
        return retVal;
    }

    @Override
    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<>();
        // 기본 10장 덱 구성
        for (int i = 0; i < 5; i++) {
            retVal.add("Strike_MyColor");
        }
        for (int i = 0; i < 4; i++) {
            retVal.add("Defend_MyColor");
        }
        retVal.add("MySpecialCard");
        return retVal;
    }

    @Override
    public String getPortraitImageName() {
        return "myCharacterPortrait.jpg";
    }

    @Override
    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.sound.playA("MY_SOUND", 0.0F);
        CardCrawlGame.screenShake.shake(
            ScreenShake.ShakeIntensity.MED,
            ScreenShake.ShakeDur.SHORT,
            false
        );
    }

    @Override
    public int getAscensionMaxHPLoss() {
        return 5;  // Ascension 14+ 체력 감소량
    }

    // ... 나머지 필수 메서드 구현
}
```

### 7.2 CharacterSelectScreen에 등록

```java
// CharacterSelectScreen.initialize()
public void initialize() {
    // 기존 캐릭터들...

    // 새 캐릭터 추가
    if (!UnlockTracker.isCharacterLocked("My Character")) {
        this.options.add(new CharacterOption(
            TEXT[5],  // 표시 이름
            CardCrawlGame.characterManager.recreateCharacter(
                AbstractPlayer.PlayerClass.MY_CHARACTER
            ),
            ImageMaster.CHAR_SELECT_MY_CHARACTER,      // 버튼 이미지
            ImageMaster.CHAR_SELECT_BG_MY_CHARACTER    // 배경 이미지
        ));
    } else {
        this.options.add(new CharacterOption(
            CardCrawlGame.characterManager.recreateCharacter(
                AbstractPlayer.PlayerClass.MY_CHARACTER
            )
        ));
    }

    positionButtons();
}
```

### 7.3 PlayerClass enum 추가

```java
// AbstractPlayer.java
public enum PlayerClass {
    IRONCLAD,
    THE_SILENT,
    DEFECT,
    WATCHER,
    MY_CHARACTER  // 새 캐릭터 추가
}
```

---

## 주의사항

### ⚠️ 초기화 누락 시 발생 가능한 문제

1. **시드 미설정**
   - 증상: 랜덤성 없음, 동일한 전투 반복
   - 원인: `AbstractDungeon.generateSeeds()` 미호출

2. **Ascension 레벨 미적용**
   - 증상: 난이도 변화 없음
   - 원인: `AbstractDungeon.ascensionLevel` 미설정

3. **덱 초기화 누락**
   - 증상: 빈 덱으로 시작
   - 원인: `initializeStarterDeck()` 미호출

4. **유물 초기화 누락**
   - 증상: 시작 유물 없음
   - 원인: `initializeStarterRelics()` 미호출

5. **체력 초기화 오류**
   - 증상: 0 HP로 시작 또는 비정상적 체력
   - 원인: `CharSelectInfo`의 hp 값 미설정

6. **에너지 관리자 미생성**
   - 증상: 에너지 표시 오류, 카드 사용 불가
   - 원인: `EnergyManager` 미초기화

### ✅ 검증 방법

```java
// 던전 시작 전 검증
assert Settings.seed != null : "Seed must be initialized";
assert AbstractDungeon.player != null : "Player must be selected";
assert AbstractDungeon.player.masterDeck.size() > 0 : "Deck must not be empty";
assert AbstractDungeon.player.relics.size() > 0 : "Starting relic required";
assert AbstractDungeon.player.currentHealth > 0 : "Health must be positive";
assert AbstractDungeon.player.energy != null : "Energy manager required";
```

---

## 참고 자료

### 관련 클래스
- `com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen`
- `com.megacrit.cardcrawl.screens.charSelect.CharacterOption`
- `com.megacrit.cardcrawl.characters.AbstractPlayer`
- `com.megacrit.cardcrawl.screens.CharSelectInfo`
- `com.megacrit.cardcrawl.core.Settings`
- `com.megacrit.cardcrawl.dungeons.AbstractDungeon`

### 핵심 메서드
- `CharacterSelectScreen.open(boolean)` - 화면 진입
- `CharacterSelectScreen.initialize()` - 캐릭터 옵션 생성
- `CharacterOption.updateHitbox()` - 캐릭터 선택 처리
- `CharacterSelectScreen.updateButtons()` - Confirm 처리
- `AbstractPlayer.initializeClass()` - 캐릭터 초기화
- `AbstractPlayer.initializeStarterDeck()` - 덱 생성
- `AbstractPlayer.initializeStarterRelics()` - 유물 획득

### Ascension 관련
- Ascension 레벨은 각 캐릭터별로 독립적으로 관리됨
- 최대 레벨 20, 레벨별로 다양한 난이도 증가 요소 적용
- 레벨 11: 물약 슬롯 -1
- 레벨 14: 최대 체력 감소 (캐릭터별 `getAscensionMaxHPLoss()`)
