# Slay the Spire - State Transition System

**분석 대상 버전**: 2022-12-18 (V2.3.4)
**분석 파일**: CardCrawlGame.java, AbstractDungeon.java, Hitbox.java, InputHelper.java

---

## 1. 게임 모드 (CardCrawlGame.mode)

### GameMode 열거형
**위치**: `com.megacrit.cardcrawl.core.CardCrawlGame.GameMode`

```java
public enum GameMode {
    CHAR_SELECT,        // 캐릭터 선택 화면 (메인 메뉴)
    GAMEPLAY,           // 던전 진행 중
    DUNGEON_TRANSITION, // 던전 전환 화면 (미사용)
    SPLASH              // 시작 스플래시 화면
}
```

### 모드별 설명

#### SPLASH
- **진입**: 게임 시작 시 (`create()` 메서드)
- **역할**: 로고/스플래시 화면 표시
- **전환 조건**: `splashScreen.isDone == true`
- **다음 상태**: `CHAR_SELECT`
- **정리**: `splashScreen = null`

#### CHAR_SELECT
- **진입**: SPLASH 완료 후, 게임 재시작 시
- **역할**: 메인 메뉴 화면 (캐릭터 선택, 옵션 등)
- **관리 객체**: `mainMenuScreen`
- **전환 조건**: `mainMenuScreen.fadedOut == true`
- **다음 상태**: `GAMEPLAY`

#### GAMEPLAY
- **진입**: 캐릭터 선택 완료 후
- **역할**: 실제 던전 진행 (전투, 이벤트, 상점 등)
- **관리 객체**: `dungeon`, `dungeonTransitionScreen`
- **전환 조건**: 없음 (게임 내에서 지속)
- **종료**: `startOver()` 호출 시 CHAR_SELECT로 복귀

---

## 2. 던전 스크린 (AbstractDungeon.screen)

### CurrentScreen 열거형
**위치**: `com.megacrit.cardcrawl.dungeons.AbstractDungeon.CurrentScreen`

```java
public enum CurrentScreen {
    NONE,               // 일반 게임 진행 (전투, 탐험)
    MASTER_DECK_VIEW,   // 덱 전체 보기
    SETTINGS,           // 설정 화면
    INPUT_SETTINGS,     // 입력 설정
    GRID,               // 카드 그리드 선택 (변환, 제거 등)
    MAP,                // 맵 화면
    FTUE,               // 튜토리얼 팁
    CHOOSE_ONE,         // 변환 카드 선택 (미사용)
    HAND_SELECT,        // 핸드 카드 선택
    SHOP,               // 상점
    COMBAT_REWARD,      // 전투 보상
    DISCARD_VIEW,       // 버린 카드 더미 보기
    EXHAUST_VIEW,       // 소멸 카드 더미 보기
    GAME_DECK_VIEW,     // 현재 덱 보기 (뽑을 카드 더미)
    BOSS_REWARD,        // 보스 유물 선택
    DEATH,              // 패배 화면
    CARD_REWARD,        // 카드 보상 선택
    TRANSFORM,          // 카드 변환 (미사용)
    VICTORY,            // 승리 화면
    UNLOCK,             // 캐릭터 언락 화면
    DOOR_UNLOCK,        // 문 언락 화면 (첫 승리)
    CREDITS,            // 크레딧
    NO_INTERACT,        // 상호작용 불가 (미사용)
    NEOW_UNLOCK         // Neow 언락 화면
}
```

### 주요 스크린 설명

#### NONE
- **역할**: 일반 게임 진행 (전투, 방 탐험)
- **업데이트**: `dungeonMapScreen.update()`, `currMapNode.room.update()`, `scene.update()`
- **렌더링**: 방 렌더링, 씬 렌더링

#### MAP
- **역할**: 던전 맵 화면
- **업데이트**: `dungeonMapScreen.update()`
- **전환**: 맵에서 방 선택 시 NONE으로 전환

#### COMBAT_REWARD
- **역할**: 전투 승리 후 보상 선택
- **업데이트**: `combatRewardScreen.update()`
- **전환**: 보상 획득 완료 시 NONE으로 전환
- **종속 화면**: CARD_REWARD, GRID (카드 선택/변환 시)

#### CARD_REWARD
- **역할**: 카드 보상 선택
- **업데이트**: `cardRewardScreen.update()`
- **전환**: 카드 선택 완료 시 COMBAT_REWARD로 복귀
- **특이사항**: `previousScreen`에 COMBAT_REWARD 저장

#### GRID
- **역할**: 카드 그리드 선택 (제거, 업그레이드, 변환 등)
- **업데이트**: `gridSelectScreen.update()`
- **전환**: 선택 완료 시 이전 화면으로 복귀

#### SHOP
- **역할**: 상점 화면
- **업데이트**: `shopScreen.update()`
- **종료 시**: `SHOP_CLOSE` 사운드 재생

#### DEATH
- **역할**: 패배 화면
- **업데이트**: `deathScreen.update()`
- **종료**: 게임 재시작 또는 메인 메뉴

#### VICTORY
- **역할**: 승리 화면
- **업데이트**: `victoryScreen.update()`
- **종료**: 크레딧 또는 메인 메뉴

---

## 3. 상태 전환 메커니즘

### 3.1 GameMode 전환 (CardCrawlGame.java)

#### render() 메서드 분기
```java
switch (mode) {
    case SPLASH:
        splashScreen.render(sb);
        break;
    case CHAR_SELECT:
        mainMenuScreen.render(sb);
        break;
    case GAMEPLAY:
        if (dungeonTransitionScreen != null) {
            dungeonTransitionScreen.render(sb);
        } else if (dungeon != null) {
            dungeon.render(sb);
        }
        break;
    default:
        logger.info("Unknown Game Mode: " + mode.name());
        break;
}
```

#### update() 메서드 분기
```java
switch (mode) {
    case SPLASH:
        splashScreen.update();
        if (splashScreen.isDone) {
            mode = GameMode.CHAR_SELECT;
            splashScreen = null;
            mainMenuScreen = new MainMenuScreen();
        }
        break;

    case CHAR_SELECT:
        mainMenuScreen.update();
        if (mainMenuScreen.fadedOut) {
            // 플레이어 생성, 던전 초기화
            mode = GameMode.GAMEPLAY;
            nextDungeon = "Exordium";
            dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");
        }
        break;

    case GAMEPLAY:
        if (dungeonTransitionScreen != null) {
            dungeonTransitionScreen.update();
            if (dungeonTransitionScreen.isComplete) {
                dungeonTransitionScreen = null;
                getDungeon(nextDungeon, AbstractDungeon.player);
            }
        } else if (dungeon != null) {
            dungeon.update();
        }

        // 던전 클리어 시 다음 던전으로 전환
        if (dungeon != null && AbstractDungeon.isDungeonBeaten
            && AbstractDungeon.fadeColor.a == 1.0F) {
            dungeon = null;
            AbstractDungeon.scene.fadeOutAmbiance();
            dungeonTransitionScreen = new DungeonTransitionScreen(nextDungeon);
        }
        break;
}
```

### 3.2 CurrentScreen 전환 (AbstractDungeon.java)

#### update() 메서드 분기
```java
switch (screen) {
    case NONE:
        dungeonMapScreen.update();
        currMapNode.room.update();
        scene.update();
        currMapNode.room.eventControllerInput();
        break;

    case FTUE:
        ftue.update();
        // 입력 차단
        InputHelper.justClickedRight = false;
        InputHelper.justClickedLeft = false;
        currMapNode.room.update();
        break;

    case MASTER_DECK_VIEW:
        deckViewScreen.update();
        break;

    case GAME_DECK_VIEW:
        gameDeckViewScreen.update();
        break;

    case COMBAT_REWARD:
        combatRewardScreen.update();
        break;

    case CARD_REWARD:
        cardRewardScreen.update();
        if (PeekButton.isPeeking) {
            currMapNode.room.update();  // Peek 버튼 사용 시 배경 업데이트
        }
        break;

    case GRID:
        gridSelectScreen.update();
        if (PeekButton.isPeeking) {
            currMapNode.room.update();
        }
        break;

    case SHOP:
        shopScreen.update();
        break;

    case MAP:
        dungeonMapScreen.update();
        break;

    case DEATH:
        deathScreen.update();
        break;

    case VICTORY:
        victoryScreen.update();
        break;

    default:
        logger.info("ERROR: UNKNOWN SCREEN TO UPDATE: " + screen.name());
        break;
}
```

#### closeCurrentScreen() 메커니즘
```java
public static void closeCurrentScreen() {
    PeekButton.isPeeking = false;

    if (previousScreen == screen) {
        previousScreen = null;
    }

    switch (screen) {
        case MASTER_DECK_VIEW:
            overlayMenu.cancelButton.hide();
            genericScreenOverlayReset();
            // 카드 언호버 처리
            for (AbstractCard c : player.masterDeck.group) {
                c.unhover();
                c.untip();
            }
            break;

        case DISCARD_VIEW:
            overlayMenu.cancelButton.hide();
            genericScreenOverlayReset();
            // 버린 카드 더미로 텔레포트
            for (AbstractCard c : player.discardPile.group) {
                c.drawScale = 0.12F;
                c.targetDrawScale = 0.12F;
                c.teleportToDiscardPile();
                c.darken(true);
                c.unhover();
            }
            break;

        case CARD_REWARD:
            overlayMenu.cancelButton.hide();
            dynamicBanner.hide();
            genericScreenOverlayReset();
            if (!screenSwap) {
                cardRewardScreen.onClose();
            }
            break;

        case COMBAT_REWARD:
            genericScreenOverlayReset();
            if (!combatRewardScreen.rewards.isEmpty()) {
                previousScreen = CurrentScreen.COMBAT_REWARD;  // 재진입 가능
            }
            break;

        case MAP:
            genericScreenOverlayReset();
            dungeonMapScreen.close();
            if (!firstRoomChosen && nextRoom != null && !dungeonMapScreen.dismissable) {
                firstRoomChosen = true;
                firstRoomLogic();  // 첫 방 진입 로직
            }
            break;

        case SHOP:
            CardCrawlGame.sound.play("SHOP_CLOSE");
            genericScreenOverlayReset();
            overlayMenu.cancelButton.hide();
            break;

        // ... 기타 화면들
    }

    // 이전 화면 복원 또는 NONE으로 전환
    if (previousScreen == null) {
        screen = CurrentScreen.NONE;
    } else if (screenSwap) {
        screenSwap = false;
    } else {
        screen = previousScreen;
        previousScreen = null;
        if (getCurrRoom().rewardTime) {
            previousScreen = CurrentScreen.COMBAT_REWARD;
        }
        isScreenUp = true;
        openPreviousScreen(screen);
    }
}
```

---

## 4. 방 전환 시스템 (Room Transition)

### 4.1 방 전환 시작
```java
public static void nextRoomTransitionStart() {
    fadeOut();                          // 페이드 아웃 시작
    waitingOnFadeOut = true;
    overlayMenu.proceedButton.hide();

    // Terminal 모드: 최대 체력 1 감소
    if (ModHelper.isModEnabled("Terminal")) {
        player.decreaseMaxHealth(1);
    }
}
```

### 4.2 페이드 아웃 완료 시
```java
public void updateFading() {
    if (isFadingOut) {
        fadeTimer -= Gdx.graphics.getDeltaTime();
        fadeColor.a = Interpolation.fade.apply(1.0F, 0.0F, fadeTimer / 0.8F);

        if (fadeTimer < 0.0F) {
            fadeTimer = 0.0F;
            isFadingOut = false;
            fadeColor.a = 1.0F;

            if (!isDungeonBeaten) {
                nextRoomTransition();  // 다음 방으로 전환
            }
        }
    }
}
```

### 4.3 nextRoomTransition() - 방 전환 핵심 로직

#### 필수 클린업 항목
```java
public void nextRoomTransition(SaveFile saveFile) {
    // 1. 보상 화면 정리
    overlayMenu.proceedButton.setLabel(TEXT[0]);
    combatRewardScreen.clear();
    if (nextRoom != null && nextRoom.room != null) {
        nextRoom.room.rewards.clear();
    }

    // 2. 몬스터 리스트 정리 (엘리트/일반 몬스터)
    if (getCurrRoom() instanceof MonsterRoomElite) {
        if (!eliteMonsterList.isEmpty()) {
            eliteMonsterList.remove(0);
        } else {
            generateElites(10);
        }
    } else if (getCurrRoom() instanceof MonsterRoom) {
        if (!monsterList.isEmpty()) {
            monsterList.remove(0);
        } else {
            generateStrongEnemies(12);
        }
    }

    // 3. 특수 이벤트 정리 (Note For Yourself)
    if (getCurrRoom() instanceof EventRoom
        && getCurrRoom().event instanceof NoteForYourself) {
        // 카드 저장 처리
    }

    // 4. 사운드 페이드 아웃
    if (RestRoom.lastFireSoundId != 0L) {
        CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
    }
    if (!player.stance.ID.equals("Neutral") && player.stance != null) {
        player.stance.stopIdleSfx();
    }

    // 5. UI 상태 초기화
    gridSelectScreen.upgradePreviewCard = null;
    previousScreen = null;
    dynamicBanner.hide();
    dungeonMapScreen.closeInstantly();
    closeCurrentScreen();
    topPanel.unhoverHitboxes();

    // 6. 페이드 인 시작
    fadeIn();
    player.resetControllerValues();

    // 7. 이펙트 정리 (ObtainKeyEffect 제외)
    effectList.clear();
    for (Iterator<AbstractGameEffect> i = topLevelEffects.iterator(); i.hasNext(); ) {
        AbstractGameEffect e = i.next();
        if (!(e instanceof ObtainKeyEffect)) {
            i.remove();
        }
    }
    topLevelEffectsQueue.clear();
    effectsQueue.clear();

    // 8. 맵 관련 초기화
    dungeonMapScreen.dismissable = true;
    dungeonMapScreen.map.legend.isLegendHighlighted = false;

    // 9. 플레이어 상태 리셋
    resetPlayer();

    // 10. 층수 증가 및 저장
    if (!CardCrawlGame.loadingSave) {
        incrementFloorBasedMetrics();
        floorNum++;
        StatsScreen.incrementFloorClimbed();
        SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
    }

    // 11. RNG 재생성 (층수 기반)
    monsterHpRng = new Random(Settings.seed + floorNum);
    aiRng = new Random(Settings.seed + floorNum);
    shuffleRng = new Random(Settings.seed + floorNum);
    cardRandomRng = new Random(Settings.seed + floorNum);
    miscRng = new Random(Settings.seed + floorNum);

    // 12. 다음 방 설정
    if (nextRoom != null) {
        // 이벤트 방인 경우 실제 방 타입 결정
        if (nextRoom.room instanceof EventRoom) {
            Random eventRngDuplicate = new Random(Settings.seed, eventRng.counter);
            EventHelper.RoomResult roomResult = EventHelper.roll(eventRngDuplicate);

            if (!isLoadingCompletedEvent) {
                eventRng = eventRngDuplicate;
                nextRoom.room = generateRoom(roomResult);
            }

            if (nextRoom.room instanceof MonsterRoom ||
                nextRoom.room instanceof MonsterRoomElite) {
                nextRoom.room.combatEvent = true;
            }
            nextRoom.room.setMapSymbol("?");
        }

        setCurrMapNode(nextRoom);
    }

    // 13. 유물 진입 트리거
    if (getCurrRoom() != null && !isLoadingPostCombatSave) {
        for (AbstractRelic r : player.relics) {
            r.justEnteredRoom(getCurrRoom());
        }
    }

    // 14. 방 진입 처리
    getCurrRoom().onPlayerEntry();

    // 15. 플레이어 위치 설정
    if (getCurrRoom() instanceof MonsterRoom && lastCombatMetricKey.equals("Shield and Spear")) {
        player.movePosition(Settings.WIDTH / 2.0F, floorY);
    } else {
        player.movePosition(Settings.WIDTH * 0.25F, floorY);
        player.flipHorizontal = false;
    }

    // 16. 전투 준비 (몬스터 방인 경우)
    if (currMapNode.room instanceof MonsterRoom && !isLoadingPostCombatSave) {
        player.preBattlePrep();
    }

    // 17. 씬 전환
    scene.nextRoom(currMapNode.room);

    // 18. 렌더 씬 설정
    if (currMapNode.room instanceof RestRoom) {
        rs = RenderScene.CAMPFIRE;
    } else if (currMapNode.room.event instanceof AbstractImageEvent) {
        rs = RenderScene.EVENT;
    } else {
        rs = RenderScene.NORMAL;
    }
}
```

---

## 5. 게임 재시작 (startOver)

### 5.1 startOver() 호출
```java
public static void startOver() {
    startOver = true;
    fadeToBlack(2.0F);  // 2초간 페이드 투 블랙
}
```

### 5.2 updateFade() - 재시작 처리
```java
public void updateFade() {
    if (!fadeIn && screenTimer == 0.0F && startOver) {
        // 1. 사운드 페이드 아웃
        if (AbstractDungeon.scene != null) {
            AbstractDungeon.scene.fadeOutAmbiance();
        }

        long startTime = System.currentTimeMillis();

        // 2. 던전 리셋
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NONE;
        AbstractDungeon.reset();

        // 3. 폰트 스케일 리셋
        FontHelper.cardTitleFont.getData().setScale(1.0F);

        // 4. 유물 페이지 리셋
        AbstractRelic.relicPage = 0;

        // 5. 시드 관련 리셋
        SeedPanel.textField = "";
        ModHelper.setModsFalse();
        SeedHelper.cachedSeed = null;
        Settings.seed = null;
        Settings.seedSet = false;
        Settings.specialSeed = null;

        // 6. 게임 모드 리셋
        Settings.isTrial = false;
        Settings.isDailyRun = false;
        Settings.isEndless = false;

        // 7. 키 관련 리셋
        Settings.isFinalActAvailable = false;
        Settings.hasRubyKey = false;
        Settings.hasEmeraldKey = false;
        Settings.hasSapphireKey = false;
        CustomModeScreen.finalActAvailable = false;

        // 8. Trial 리셋
        trial = null;

        // 9. 상점/팁/메트릭 리셋
        ShopScreen.resetPurgeCost();
        tips.initialize();
        metricData.clearData();

        // 10. 언락 트래커 리프레시
        UnlockTracker.refresh();

        // 11. 메인 메뉴 화면 재생성
        mainMenuScreen = new MainMenuScreen();
        mainMenuScreen.bg.slideDownInstantly();

        // 12. 저장 슬롯 정보 업데이트
        saveSlotPref.putFloat(
            SaveHelper.slotName("COMPLETION", saveSlot),
            UnlockTracker.getCompletionPercentage());
        saveSlotPref.putLong(
            SaveHelper.slotName("PLAYTIME", saveSlot),
            UnlockTracker.getTotalPlaytime());
        saveSlotPref.flush();

        // 13. 카드 헬퍼 정리
        CardHelper.clear();

        // 14. 게임 모드 변경
        mode = GameMode.CHAR_SELECT;

        // 15. 던전 전환 화면 재생성
        nextDungeon = "Exordium";
        dungeonTransitionScreen = new DungeonTransitionScreen("Exordium");

        // 16. 팁 트래커 리프레시
        TipTracker.refresh();

        // 17. 가비지 컬렉션
        logger.info("[GC] BEFORE: " + SystemStats.getUsedMemory());
        System.gc();
        logger.info("[GC] AFTER: " + SystemStats.getUsedMemory());

        // 18. 페이드 인
        fadeIn(2.0F);

        // 19. 크레딧 큐 처리 (승리 시)
        if (queueCredits) {
            queueCredits = false;
            mainMenuScreen.creditsScreen.open(playCreditsBgm);
            mainMenuScreen.hideMenuButtons();
        }
    }
}
```

### 5.3 AbstractDungeon.reset()
```java
public static void reset() {
    logger.info("Resetting variables...");

    // 1. 스코어 변수 리셋
    CardCrawlGame.resetScoreVars();

    // 2. 모드 리셋
    ModHelper.setModsFalse();

    // 3. 층수/막 리셋
    floorNum = 0;
    actNum = 0;

    // 4. 현재 방 정리
    if (currMapNode != null && getCurrRoom() != null) {
        getCurrRoom().dispose();
        if (getCurrRoom().monsters != null) {
            for (AbstractMonster m : getCurrRoom().monsters.monsters) {
                m.dispose();
            }
        }
    }

    // 5. 맵 노드 리셋
    currMapNode = null;

    // 6. 리스트 정리
    shrineList.clear();
    relicsToRemoveOnStart.clear();

    // 7. 화면 상태 리셋
    previousScreen = null;

    // 8. 액션 매니저 정리
    actionManager.clear();
    actionManager.clearNextRoomCombatActions();

    // 9. 보상 화면 정리
    combatRewardScreen.clear();
    cardRewardScreen.reset();

    // 10. 맵 화면 정리
    if (dungeonMapScreen != null) {
        dungeonMapScreen.closeInstantly();
    }

    // 11. 이펙트 정리
    effectList.clear();
    effectsQueue.clear();
    topLevelEffectsQueue.clear();
    topLevelEffects.clear();

    // 12. 카드 블리자드 리셋
    cardBlizzRandomizer = cardBlizzStartOffset;

    // 13. 플레이어 유물 정리
    if (player != null) {
        player.relics.clear();
    }

    // 14. 렌더 씬 리셋
    rs = RenderScene.NORMAL;

    // 15. 블라이트 풀 정리
    blightPool.clear();
}
```

---

## 6. 주요 정리 항목 (Cleanup Checklist)

### 6.1 방 전환 시 필수 정리
- [ ] `combatRewardScreen.clear()` - 보상 화면 정리
- [ ] `nextRoom.room.rewards.clear()` - 다음 방 보상 초기화
- [ ] `eliteMonsterList.remove(0)` or `monsterList.remove(0)` - 몬스터 리스트 정리
- [ ] `RestRoom.lastFireSoundId` 사운드 페이드 아웃
- [ ] `player.stance.stopIdleSfx()` - 스탠스 사운드 정지
- [ ] `gridSelectScreen.upgradePreviewCard = null` - 업그레이드 프리뷰 초기화
- [ ] `previousScreen = null` - 이전 화면 참조 제거
- [ ] `dynamicBanner.hide()` - 배너 숨김
- [ ] `dungeonMapScreen.closeInstantly()` - 맵 화면 즉시 닫기
- [ ] `closeCurrentScreen()` - 현재 화면 정리
- [ ] `topPanel.unhoverHitboxes()` - 상단 패널 히트박스 언호버
- [ ] `effectList.clear()` - 일반 이펙트 정리
- [ ] `topLevelEffects` 정리 (ObtainKeyEffect 제외)
- [ ] `topLevelEffectsQueue.clear()` - 큐잉된 이펙트 정리
- [ ] `effectsQueue.clear()` - 큐잉된 일반 이펙트 정리
- [ ] `resetPlayer()` - 플레이어 상태 리셋
- [ ] RNG 재생성 (층수 기반)
- [ ] `scene.nextRoom(currMapNode.room)` - 씬 전환

### 6.2 게임 재시작 시 필수 정리
- [ ] `AbstractDungeon.scene.fadeOutAmbiance()` - 앰비언스 사운드 페이드 아웃
- [ ] `AbstractDungeon.screen = CurrentScreen.NONE` - 화면 상태 초기화
- [ ] `AbstractDungeon.reset()` - 던전 전체 리셋
- [ ] `FontHelper.cardTitleFont.getData().setScale(1.0F)` - 폰트 스케일 리셋
- [ ] `AbstractRelic.relicPage = 0` - 유물 페이지 리셋
- [ ] `SeedPanel.textField = ""` - 시드 입력 필드 초기화
- [ ] `ModHelper.setModsFalse()` - 모드 비활성화
- [ ] 시드 관련 변수 초기화 (seed, seedSet, specialSeed)
- [ ] 게임 모드 플래그 초기화 (isTrial, isDailyRun, isEndless)
- [ ] 키 플래그 초기화 (isFinalActAvailable, 3개 키)
- [ ] `trial = null` - Trial 초기화
- [ ] `ShopScreen.resetPurgeCost()` - 상점 제거 비용 리셋
- [ ] `tips.initialize()` - 팁 초기화
- [ ] `metricData.clearData()` - 메트릭 데이터 정리
- [ ] `UnlockTracker.refresh()` - 언락 트래커 리프레시
- [ ] `mainMenuScreen = new MainMenuScreen()` - 메인 메뉴 재생성
- [ ] 저장 슬롯 정보 업데이트 (completion, playtime)
- [ ] `CardHelper.clear()` - 카드 헬퍼 정리
- [ ] `mode = GameMode.CHAR_SELECT` - 게임 모드 변경
- [ ] `dungeonTransitionScreen = new DungeonTransitionScreen("Exordium")` - 전환 화면 재생성
- [ ] `TipTracker.refresh()` - 팁 트래커 리프레시
- [ ] `System.gc()` - 가비지 컬렉션

### 6.3 화면 전환 시 필수 정리
- [ ] `overlayMenu.cancelButton.hide()` - 취소 버튼 숨김 (대부분 화면)
- [ ] `genericScreenOverlayReset()` - 오버레이 리셋
- [ ] `dynamicBanner.hide()` - 배너 숨김 (해당 화면)
- [ ] 화면별 특수 정리 (카드 언호버, 텔레포트 등)
- [ ] `PeekButton.isPeeking = false` - Peek 상태 리셋
- [ ] `previousScreen` 관리 (복귀 가능 화면의 경우)

---

## 7. 잘못된 상태 전환 시 문제점

### 7.1 방 전환 순서 오류
**증상**:
- 다음 방이 null인 상태에서 전환 시도
- 페이드 아웃 완료 전 다음 방 설정

**결과**:
- `NullPointerException` 발생
- 게임 크래시 또는 빈 방 진입

**해결**:
```java
// 올바른 순서:
// 1. nextRoom 설정
// 2. nextRoomTransitionStart() 호출
// 3. 페이드 아웃 완료 대기
// 4. nextRoomTransition() 자동 호출
```

### 7.2 화면 정리 누락
**증상**:
- 이전 화면의 UI 요소가 남아있음
- 히트박스가 활성화된 상태로 유지

**결과**:
- UI 중복 렌더링
- 클릭 이벤트 충돌
- 메모리 누수

**해결**:
```java
// closeCurrentScreen()에서 화면별 정리 필수:
overlayMenu.cancelButton.hide();
genericScreenOverlayReset();
dynamicBanner.hide();
// 카드 언호버, 텔레포트 등
```

### 7.3 이펙트 정리 누락
**증상**:
- 이전 방의 이펙트가 다음 방에 표시됨

**결과**:
- 시각적 버그
- 성능 저하 (누적)

**해결**:
```java
// nextRoomTransition()에서 이펙트 정리:
effectList.clear();
for (Iterator<AbstractGameEffect> i = topLevelEffects.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    if (!(e instanceof ObtainKeyEffect)) {  // 키 획득 이펙트만 유지
        i.remove();
    }
}
topLevelEffectsQueue.clear();
effectsQueue.clear();
```

### 7.4 RNG 재생성 누락
**증상**:
- 층수가 변경되었지만 RNG가 재생성되지 않음

**결과**:
- 몬스터 HP, AI 패턴 등이 예측 가능해짐
- 시드 재현성 손상

**해결**:
```java
// nextRoomTransition()에서 RNG 재생성 필수:
monsterHpRng = new Random(Settings.seed + floorNum);
aiRng = new Random(Settings.seed + floorNum);
shuffleRng = new Random(Settings.seed + floorNum);
cardRandomRng = new Random(Settings.seed + floorNum);
miscRng = new Random(Settings.seed + floorNum);
```

### 7.5 사운드 정리 누락
**증상**:
- 이전 방의 사운드가 계속 재생됨

**결과**:
- 사운드 중복 재생
- 오디오 채널 고갈

**해결**:
```java
// nextRoomTransition()에서 사운드 페이드 아웃:
if (RestRoom.lastFireSoundId != 0L) {
    CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
}
if (!player.stance.ID.equals("Neutral") && player.stance != null) {
    player.stance.stopIdleSfx();
}
```

### 7.6 층 역행 시 문제
**증상**:
- `floorNum`을 감소시키면서 방 전환 시도

**결과**:
- RNG가 이전 층의 값으로 재생성 → 다른 몬스터/이벤트 출현
- 메트릭 데이터 손상 (current_hp_per_floor 등)
- 저장 파일 불일치

**해결**:
```java
// 층 역행 시 추가 고려사항:
// 1. floorNum 감소
// 2. 메트릭 데이터 롤백 (current_hp_per_floor 등)
// 3. RNG 재생성 (감소된 floorNum 기반)
// 4. 몬스터 리스트 복원 (이전에 제거된 몬스터 추가)
// 5. 이벤트 리스트 복원
// 6. 경로 데이터 롤백 (pathX, pathY)
```

---

## 8. 상태 추가/수정 방법

### 8.1 새 GameMode 추가
```java
// 1. GameMode enum에 추가
public enum GameMode {
    CHAR_SELECT,
    GAMEPLAY,
    DUNGEON_TRANSITION,
    SPLASH,
    MY_NEW_MODE  // 새 모드 추가
}

// 2. render() 분기 추가
switch (mode) {
    // ... 기존 케이스들
    case MY_NEW_MODE:
        myNewScreen.render(sb);
        break;
}

// 3. update() 분기 추가
switch (mode) {
    // ... 기존 케이스들
    case MY_NEW_MODE:
        myNewScreen.update();
        if (myNewScreen.shouldExit) {
            mode = GameMode.GAMEPLAY;  // 다음 모드로 전환
        }
        break;
}
```

### 8.2 새 CurrentScreen 추가
```java
// 1. CurrentScreen enum에 추가
public enum CurrentScreen {
    NONE,
    MASTER_DECK_VIEW,
    // ... 기존 스크린들
    MY_NEW_SCREEN  // 새 스크린 추가
}

// 2. update() 분기 추가
switch (screen) {
    // ... 기존 케이스들
    case MY_NEW_SCREEN:
        myNewScreenObject.update();
        break;
}

// 3. render() 분기 추가
switch (screen) {
    // ... 기존 케이스들
    case MY_NEW_SCREEN:
        myNewScreenObject.render(sb);
        break;
}

// 4. closeCurrentScreen() 정리 추가
switch (screen) {
    // ... 기존 케이스들
    case MY_NEW_SCREEN:
        overlayMenu.cancelButton.hide();
        genericScreenOverlayReset();
        myNewScreenObject.cleanup();  // 화면별 정리
        break;
}

// 5. openPreviousScreen() 재진입 로직 추가 (필요 시)
private static void openPreviousScreen(CurrentScreen s) {
    switch (s) {
        // ... 기존 케이스들
        case MY_NEW_SCREEN:
            myNewScreenObject.reopen();
            break;
    }
}
```

### 8.3 새 RenderScene 추가
```java
// 1. RenderScene enum에 추가
public enum RenderScene {
    NORMAL,
    EVENT,
    CAMPFIRE,
    MY_NEW_SCENE  // 새 씬 추가
}

// 2. render() 분기 추가
switch (rs) {
    // ... 기존 케이스들
    case MY_NEW_SCENE:
        scene.renderMyNewScene(sb);
        break;
}

// 3. nextRoomTransition()에서 씬 설정 로직 추가
if (currMapNode.room instanceof MyNewRoomType) {
    rs = RenderScene.MY_NEW_SCENE;
}
```

---

## 9. 층 역행 모드 구현 시 고려사항

### 9.1 필수 저장 데이터
```java
// 각 층마다 저장해야 할 데이터:
class FloorSnapshot {
    int floorNum;
    int actNum;

    // RNG 상태
    long monsterRngCounter;
    long eventRngCounter;
    long cardRngCounter;
    // ... 기타 RNG

    // 몬스터 리스트 (제거 전 상태)
    ArrayList<String> monsterList;
    ArrayList<String> eliteMonsterList;

    // 이벤트 리스트
    ArrayList<String> eventList;
    HashMap<String, Float> eventChances;

    // 메트릭 데이터
    ArrayList<Integer> currentHpPerFloor;
    ArrayList<Integer> maxHpPerFloor;
    ArrayList<Integer> goldPerFloor;
    ArrayList<String> pathPerFloor;

    // 경로 데이터
    ArrayList<Integer> pathX;
    ArrayList<Integer> pathY;

    // 플레이어 상태 (선택사항)
    int playerGold;
    int playerCurrentHp;
    int playerMaxHp;
    ArrayList<String> playerDeck;
    ArrayList<String> playerRelics;
}
```

### 9.2 역행 시 복원 절차
```java
public void revertToFloor(int targetFloor, FloorSnapshot snapshot) {
    // 1. 현재 방 정리
    if (currMapNode != null && getCurrRoom() != null) {
        getCurrRoom().dispose();
        closeCurrentScreen();
    }

    // 2. 층수 복원
    floorNum = snapshot.floorNum;
    actNum = snapshot.actNum;

    // 3. RNG 복원
    monsterRng.setCounter(snapshot.monsterRngCounter);
    eventRng.setCounter(snapshot.eventRngCounter);
    cardRng.setCounter(snapshot.cardRngCounter);
    // ... 기타 RNG

    // 4. 몬스터/이벤트 리스트 복원
    monsterList = new ArrayList<>(snapshot.monsterList);
    eliteMonsterList = new ArrayList<>(snapshot.eliteMonsterList);
    eventList = new ArrayList<>(snapshot.eventList);
    EventHelper.setChances(snapshot.eventChances);

    // 5. 메트릭 데이터 롤백
    CardCrawlGame.metricData.current_hp_per_floor =
        new ArrayList<>(snapshot.currentHpPerFloor);
    CardCrawlGame.metricData.max_hp_per_floor =
        new ArrayList<>(snapshot.maxHpPerFloor);
    CardCrawlGame.metricData.gold_per_floor =
        new ArrayList<>(snapshot.goldPerFloor);
    CardCrawlGame.metricData.path_per_floor =
        new ArrayList<>(snapshot.pathPerFloor);

    // 6. 경로 데이터 복원
    pathX = new ArrayList<>(snapshot.pathX);
    pathY = new ArrayList<>(snapshot.pathY);

    // 7. 플레이어 상태 복원 (선택사항)
    player.gold = snapshot.playerGold;
    player.currentHealth = snapshot.playerCurrentHp;
    player.maxHealth = snapshot.playerMaxHp;
    // 덱, 유물 복원 등

    // 8. 맵 재생성
    map = MapGenerator.generateDungeon(floorNum, actNum, /* ... */);

    // 9. 화면 상태 초기화
    screen = CurrentScreen.MAP;
    isScreenUp = true;
    dungeonMapScreen.open(false);

    // 10. 페이드 인
    fadeIn();
}
```

### 9.3 역행 시 주의사항
1. **RNG 일관성**: RNG 카운터를 정확히 복원해야 동일한 몬스터/이벤트 재현 가능
2. **메트릭 데이터**: 층별 메트릭 데이터를 롤백하지 않으면 통계가 왜곡됨
3. **몬스터 리스트**: 제거된 몬스터를 리스트에 다시 추가해야 함
4. **이벤트 확률**: EventHelper.setChances()로 이벤트 확률 복원 필수
5. **저장 파일**: 층 역행 시 저장 파일 구조 변경 필요 (층별 스냅샷 저장)
6. **UI 상태**: 화면 전환 시 현재 화면 정리 필수
7. **메모리 관리**: 모든 층의 스냅샷 저장 시 메모리 사용량 증가 주의

---

## 10. 참고 자료

### 10.1 주요 클래스 경로
- `com.megacrit.cardcrawl.core.CardCrawlGame`
- `com.megacrit.cardcrawl.dungeons.AbstractDungeon`
- `com.megacrit.cardcrawl.helpers.Hitbox`
- `com.megacrit.cardcrawl.helpers.input.InputHelper`

### 10.2 주요 메서드
- `CardCrawlGame.render()` - 게임 모드별 렌더링
- `CardCrawlGame.update()` - 게임 모드별 업데이트
- `CardCrawlGame.startOver()` - 게임 재시작
- `AbstractDungeon.update()` - 던전 스크린별 업데이트
- `AbstractDungeon.render()` - 던전 스크린별 렌더링
- `AbstractDungeon.nextRoomTransitionStart()` - 방 전환 시작
- `AbstractDungeon.nextRoomTransition()` - 방 전환 실행
- `AbstractDungeon.closeCurrentScreen()` - 현재 화면 닫기
- `AbstractDungeon.reset()` - 던전 완전 리셋

### 10.3 중요 변수
- `CardCrawlGame.mode` - 현재 게임 모드
- `AbstractDungeon.screen` - 현재 던전 스크린
- `AbstractDungeon.previousScreen` - 이전 던전 스크린
- `AbstractDungeon.isScreenUp` - 화면 활성화 여부
- `AbstractDungeon.currMapNode` - 현재 맵 노드
- `AbstractDungeon.nextRoom` - 다음 방
- `AbstractDungeon.floorNum` - 현재 층수
- `AbstractDungeon.actNum` - 현재 막
- `AbstractDungeon.rs` - 렌더 씬 모드

---

**작성일**: 2025-01-08
**버전**: 1.0
**작성자**: Claude (Anthropic)
