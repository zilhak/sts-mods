# RoomTransition.md - Slay the Spire Room Transition System

이 문서는 Slay the Spire의 방 전환 시스템 작동 원리를 설명합니다.

---

## 1. 방 진입 과정 (AbstractDungeon.setCurrMapNode)

### 1.1 메서드 시그니처

```java
public static void setCurrMapNode(MapRoomNode currMapNode)
```

**호출 위치**:
- `MapRoomNode.update()` - 노드 클릭 후 0.25초 대기 후
- `AbstractDungeon.nextRoomTransition()` - 다음 방으로 전환 시

### 1.2 실행 순서

```java
public static void setCurrMapNode(MapRoomNode currMapNode) {
    // 1. SoulGroup 보존 (전투 중 생성된 영혼 카드들)
    SoulGroup group = AbstractDungeon.currMapNode.room.souls;

    // 2. 이전 방 정리 (dispose)
    if (AbstractDungeon.currMapNode != null && getCurrRoom() != null) {
        getCurrRoom().dispose();  // ← 매우 중요!
    }

    // 3. 현재 맵 노드 변경
    AbstractDungeon.currMapNode = currMapNode;

    // 4. 방 유효성 검증 (세이브 로딩 시)
    if (AbstractDungeon.currMapNode.room == null) {
        logger.warn("This player loaded into a room that no longer exists");
        // 같은 층의 다른 유효한 방 찾기
        for (int i = 0; i < 5; i++) {
            if (((MapRoomNode)((ArrayList)map.get(currMapNode.y)).get(i)).room != null) {
                AbstractDungeon.currMapNode = ((ArrayList<MapRoomNode>)map.get(currMapNode.y)).get(i);
                AbstractDungeon.currMapNode.room = ...;
                nextRoom.room = ...;
                break;
            }
        }
    } else {
        // 5. SoulGroup 복원
        AbstractDungeon.currMapNode.room.souls = group;
    }
}
```

### 1.3 중요 포인트

**SoulGroup 보존**:
```java
// 영혼 카드는 전투 중 생성되어 보상 화면에서 선택 가능
// 방 전환 시 유실되지 않도록 보존 후 복원
SoulGroup group = AbstractDungeon.currMapNode.room.souls;
// ... 방 변경 ...
AbstractDungeon.currMapNode.room.souls = group;
```

**이전 방 정리 (dispose)**:
```java
if (AbstractDungeon.currMapNode != null && getCurrRoom() != null) {
    getCurrRoom().dispose();
}
```
- **필수**: 이벤트 리스너, 텍스처, 몬스터 정리
- **누락 시**: 메모리 누수, 이벤트 중복 발생

---

## 2. 방 타입별 초기화

### 2.1 MonsterRoom (일반 전투방)

**진입 시 초기화**:
```java
// AbstractRoom.onPlayerEntry() 구현
public void onPlayerEntry() {
    // 1. 페이즈 설정
    this.phase = RoomPhase.COMBAT;

    // 2. 몬스터 그룹 생성
    this.monsters = AbstractDungeon.getMonsters();

    // 3. 전투 시작 대기 타이머 설정
    AbstractRoom.waitTimer = 1.2F;  // ← 1.2초 대기

    // 4. 플레이어 초기화
    AbstractDungeon.player.preBattlePrep();
    AbstractDungeon.player.resetControllerValues();

    // 5. BGM 재생
    playBGM(someMusicKey);
}
```

**전투 시작 대기 타이머 (waitTimer)**:
```java
// AbstractRoom.update() - COMBAT 페이즈
if (waitTimer > 0.0F) {
    if (AbstractDungeon.actionManager.currentAction != null ||
        !AbstractDungeon.actionManager.isEmpty()) {
        AbstractDungeon.actionManager.update();
    } else {
        waitTimer -= Gdx.graphics.getDeltaTime();
    }

    if (waitTimer <= 0.0F) {
        // 전투 시작!
        AbstractDungeon.actionManager.turnHasEnded = true;
        AbstractDungeon.topLevelEffects.add(new BattleStartEffect(false));

        // 에너지 부여 및 드로우
        AbstractDungeon.actionManager.addToBottom(
            new GainEnergyAndEnableControlsAction(AbstractDungeon.player.energy.energyMaster)
        );
        AbstractDungeon.actionManager.addToBottom(
            new DrawCardAction(AbstractDungeon.player, AbstractDungeon.player.gameHandSize)
        );
        AbstractDungeon.actionManager.addToBottom(new EnableEndTurnButtonAction());

        // 전투 패널 표시
        AbstractDungeon.overlayMenu.showCombatPanels();

        // 전투 시작 트리거
        AbstractDungeon.player.applyStartOfCombatLogic();
        AbstractDungeon.player.applyStartOfCombatPreDrawLogic();

        // 일일 모드 효과
        if (ModHelper.isModEnabled("Careless")) {
            Careless.modAction();
        }

        // 플레이어 턴 시작
        this.skipMonsterTurn = false;
        AbstractDungeon.player.applyStartOfTurnRelics();
        AbstractDungeon.player.applyStartOfTurnPostDrawRelics();
        AbstractDungeon.player.applyStartOfTurnCards();
        AbstractDungeon.player.applyStartOfTurnPowers();
        AbstractDungeon.player.applyStartOfTurnOrbs();
        AbstractDungeon.actionManager.useNextCombatActions();
    }
}
```

### 2.2 EventRoom (이벤트 방)

**진입 시 초기화**:
```java
public void onPlayerEntry() {
    // 1. 페이즈 설정
    this.phase = RoomPhase.EVENT;

    // 2. 이벤트 생성 (nextRoomTransition에서 설정됨)
    if (this.event == null) {
        // EventHelper.roll()로 랜덤 이벤트 결정
        Random eventRngDuplicate = new Random(Settings.seed, eventRng.counter);
        EventHelper.RoomResult roomResult = EventHelper.roll(eventRngDuplicate);
        this.event = EventHelper.getEvent(roomResult);
    }

    // 3. 이벤트 시작
    this.event.onEnterRoom();

    // 4. BGM (이벤트별로 다름)
    playBGM(someEventMusic);
}
```

### 2.3 RestRoom (휴식 방)

**진입 시 초기화**:
```java
public void onPlayerEntry() {
    // 1. 페이즈 설정
    this.phase = RoomPhase.COMPLETE;  // ← 전투 없음

    // 2. 휴식 옵션 표시
    // - 휴식: HP 회복
    // - 대장장이: 카드 업그레이드
    // - 기타 렐릭 효과 (Shovel, Peace Pipe 등)

    // 3. BGM 재생
    playBGM("SHRINE");

    // 4. 모닥불 사운드
    RestRoom.lastFireSoundId = CardCrawlGame.sound.playAndLoop("REST_FIRE_WET");
}
```

### 2.4 ShopRoom (상점 방)

**진입 시 초기화**:
```java
public void onPlayerEntry() {
    // 1. 페이즈 설정
    this.phase = RoomPhase.COMPLETE;

    // 2. 상점 아이템 생성
    this.merchant = new Merchant();

    // 3. BGM 재생
    playBGM("SHOP");
}
```

### 2.5 TreasureRoom (보물 방)

**진입 시 초기화**:
```java
public void onPlayerEntry() {
    // 1. 페이즈 설정
    this.phase = RoomPhase.COMPLETE;

    // 2. 보물 상자 생성
    this.chest = AbstractDungeon.getRandomChest();

    // 3. BGM 재생
    playBGM("TREASURE");
}
```

---

## 3. 방 나가기 과정 (endRoom, 보상 처리)

### 3.1 전투 종료 (endBattle)

```java
public void endBattle() {
    // 1. 전투 종료 플래그 설정
    this.isBattleOver = true;

    // 2. 승리 업적 체크
    if (AbstractDungeon.player.currentHealth == 1) {
        UnlockTracker.unlockAchievement("SHRUG_IT_OFF");
    }

    // 3. 렐릭 트리거
    if (AbstractDungeon.player.hasRelic("Meat on the Bone")) {
        AbstractDungeon.player.getRelic("Meat on the Bone").onTrigger();
    }
    AbstractDungeon.player.onVictory();

    // 4. 전투 종료 대기 타이머
    this.endBattleTimer = 0.25F;

    // 5. 통계 기록
    CardCrawlGame.metricData.addEncounterData();

    // 6. 액션 매니저 정리
    AbstractDungeon.actionManager.clear();

    // 7. 플레이어 상태 리셋
    AbstractDungeon.player.inSingleTargetMode = false;
    AbstractDungeon.player.releaseCard();
    AbstractDungeon.player.hand.refreshHandLayout();
    AbstractDungeon.player.resetControllerValues();

    // 8. UI 숨기기
    AbstractDungeon.overlayMenu.hideCombatPanels();

    // 9. 자세 리셋
    if (!AbstractDungeon.player.stance.ID.equals("Neutral") &&
        AbstractDungeon.player.stance != null) {
        AbstractDungeon.player.stance.stopIdleSfx();
    }
}
```

### 3.2 전투 종료 후 처리 (update - COMBAT 페이즈)

```java
// AbstractRoom.update() 내부
if (this.isBattleOver && AbstractDungeon.actionManager.actions.isEmpty()) {
    this.skipMonsterTurn = false;
    this.endBattleTimer -= Gdx.graphics.getDeltaTime();

    if (this.endBattleTimer < 0.0F) {
        // 페이즈 변경
        this.phase = RoomPhase.COMPLETE;

        // 승리 사운드
        if (!(AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss) ||
            !(CardCrawlGame.dungeon instanceof TheBeyond) ||
            Settings.isEndless) {
            CardCrawlGame.sound.play("VICTORY");
        }

        this.endBattleTimer = 0.0F;

        // 보상 추가
        if (this instanceof MonsterRoomBoss && !AbstractDungeon.loading_post_combat) {
            // 보스: 100 골드 (ascension 13+: 75 골드)
            addGoldToRewards(100);
        } else if (this instanceof MonsterRoomElite) {
            // 엘리트: 25~35 골드
            addGoldToRewards(AbstractDungeon.treasureRng.random(25, 35));
            CardCrawlGame.elites1Slain++;  // 통계
        } else if (this instanceof MonsterRoom) {
            // 일반: 10~20 골드
            addGoldToRewards(AbstractDungeon.treasureRng.random(10, 20));
            CardCrawlGame.monstersSlain++;  // 통계
        }

        // 포션/카드 보상 추가
        if (!AbstractDungeon.loading_post_combat) {
            dropReward();         // 커스텀 보상
            addPotionToRewards(); // 40% 확률 포션
        }

        // 보상 화면 열기
        if (this.rewardAllowed) {
            if (this.mugged) {
                AbstractDungeon.combatRewardScreen.openCombat(TEXT[0]);
            } else if (this.smoked) {
                AbstractDungeon.combatRewardScreen.openCombat(TEXT[1], true);
            } else {
                AbstractDungeon.combatRewardScreen.open();
            }

            // 세이브
            if (!CardCrawlGame.loadingSave && !AbstractDungeon.loading_post_combat) {
                SaveFile saveFile = new SaveFile(SaveFile.SaveType.POST_COMBAT);
                SaveAndContinue.save(saveFile);
                AbstractDungeon.effectList.add(new GameSavedEffect());
            }
            AbstractDungeon.loading_post_combat = false;
        }
    }
}
```

### 3.3 COMPLETE 페이즈 처리

```java
// AbstractRoom.update() - COMPLETE 페이즈
case COMPLETE:
    if (!AbstractDungeon.isScreenUp) {
        AbstractDungeon.actionManager.update();

        if (this.event != null) {
            this.event.updateDialog();
        }

        if (AbstractDungeon.actionManager.isEmpty() && !AbstractDungeon.isFadingOut) {
            // 보상 팝업 대기 타이머
            if (this.rewardPopOutTimer > 1.0F) {
                this.rewardPopOutTimer = 1.0F;
            }
            this.rewardPopOutTimer -= Gdx.graphics.getDeltaTime();

            if (this.rewardPopOutTimer < 0.0F) {
                if (this.event == null) {
                    // "Proceed" 버튼 표시
                    AbstractDungeon.overlayMenu.proceedButton.show();
                } else if (!(this.event instanceof AbstractImageEvent) &&
                           !this.event.hasFocus) {
                    AbstractDungeon.overlayMenu.proceedButton.show();
                }
            }
        }
    }
    break;
```

---

## 4. 다음 방으로 전환 과정

### 4.1 전환 시작 (nextRoomTransitionStart)

**호출 위치**:
- `MapRoomNode.update()` - 노드 클릭 후
- `ProceedButton.onClick()` - "Proceed" 버튼 클릭 시

```java
public static void nextRoomTransitionStart() {
    // 1. 화면 페이드 아웃
    fadeOut();
    waitingOnFadeOut = true;

    // 2. "Proceed" 버튼 숨기기
    overlayMenu.proceedButton.hide();

    // 3. 일일 모드 효과 (Terminal)
    if (ModHelper.isModEnabled("Terminal")) {
        player.decreaseMaxHealth(1);
    }
}

// 페이드 아웃 완료 시 자동 호출:
// AbstractDungeon.dungeonTransitionSetup()
// -> dungeon.nextRoomTransition(saveFile)
```

### 4.2 방 전환 처리 (nextRoomTransition)

```java
public void nextRoomTransition(SaveFile saveFile) {
    // 1. UI 리셋
    overlayMenu.proceedButton.setLabel(TEXT[0]);
    combatRewardScreen.clear();
    if (nextRoom != null && nextRoom.room != null) {
        nextRoom.room.rewards.clear();
    }

    // 2. 몬스터 리스트 업데이트
    if (getCurrRoom() instanceof MonsterRoomElite) {
        if (!eliteMonsterList.isEmpty()) {
            logger.info("Removing elite: " + eliteMonsterList.get(0));
            eliteMonsterList.remove(0);
        } else {
            generateElites(10);
        }
    } else if (getCurrRoom() instanceof MonsterRoom) {
        if (!monsterList.isEmpty()) {
            logger.info("Removing monster: " + monsterList.get(0));
            monsterList.remove(0);
        } else {
            generateStrongEnemies(12);
        }
    }

    // 3. 사운드 정리
    if (RestRoom.lastFireSoundId != 0L) {
        CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
    }
    if (!player.stance.ID.equals("Neutral") && player.stance != null) {
        player.stance.stopIdleSfx();
    }

    // 4. 화면 리셋
    gridSelectScreen.upgradePreviewCard = null;
    previousScreen = null;
    dynamicBanner.hide();
    dungeonMapScreen.closeInstantly();
    closeCurrentScreen();
    topPanel.unhoverHitboxes();
    fadeIn();
    player.resetControllerValues();

    // 5. 이펙트 정리
    effectList.clear();
    for (Iterator<AbstractGameEffect> i = topLevelEffects.iterator(); i.hasNext(); ) {
        AbstractGameEffect e = i.next();
        if (!(e instanceof ObtainKeyEffect)) {
            i.remove();  // ObtainKeyEffect만 유지
        }
    }
    topLevelEffectsQueue.clear();
    effectsQueue.clear();

    // 6. 지도 화면 리셋
    dungeonMapScreen.dismissable = true;
    dungeonMapScreen.map.legend.isLegendHighlighted = false;

    // 7. 플레이어 리셋
    resetPlayer();  // ← 매우 중요!

    // 8. floorNum 증가 (여기서!)
    if (!CardCrawlGame.loadingSave) {
        incrementFloorBasedMetrics();
        floorNum++;  // ← 층 증가!

        if (!TipTracker.tips.get("INTENT_TIP") && floorNum == 6) {
            TipTracker.neverShowAgain("INTENT_TIP");
        }

        StatsScreen.incrementFloorClimbed();
        SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
    }

    // 9. RNG 재생성 (floorNum 기반)
    monsterHpRng = new Random(Settings.seed + floorNum);
    aiRng = new Random(Settings.seed + floorNum);
    shuffleRng = new Random(Settings.seed + floorNum);
    cardRandomRng = new Random(Settings.seed + floorNum);
    miscRng = new Random(Settings.seed + floorNum);

    // 10. 세이브 로딩 체크
    boolean isLoadingPostCombatSave = (CardCrawlGame.loadingSave &&
                                        saveFile != null &&
                                        saveFile.post_combat);

    // 11. 렐릭 onEnterRoom 트리거
    if (nextRoom != null && !isLoadingPostCombatSave) {
        for (AbstractRelic r : player.relics) {
            r.onEnterRoom(nextRoom.room);
        }
    }

    // 12. 액션 매니저 정리
    if (!actionManager.actions.isEmpty()) {
        logger.info("[WARNING] Action Manager was NOT clear! Clearing");
        actionManager.clear();
    }

    // 13. 이벤트 방 처리
    if (nextRoom != null) {
        String roomMetricKey = nextRoom.room.getMapSymbol();

        if (nextRoom.room instanceof EventRoom) {
            Random eventRngDuplicate = new Random(Settings.seed, eventRng.counter);
            EventHelper.RoomResult roomResult = EventHelper.roll(eventRngDuplicate);

            isLoadingCompletedEvent = (isLoadingPostCombatSave &&
                                        roomResult == EventHelper.RoomResult.EVENT);

            if (!isLoadingCompletedEvent) {
                eventRng = eventRngDuplicate;
                nextRoom.room = generateRoom(roomResult);  // ← 실제 방 생성
            }

            roomMetricKey = nextRoom.room.getMapSymbol();

            if (nextRoom.room instanceof MonsterRoom ||
                nextRoom.room instanceof MonsterRoomElite) {
                nextRoom.room.combatEvent = true;
            }
            nextRoom.room.setMapSymbol("?");
            nextRoom.room.setMapImg(ImageMaster.MAP_NODE_EVENT,
                                     ImageMaster.MAP_NODE_EVENT_OUTLINE);
        }

        if (!isLoadingPostCombatSave) {
            CardCrawlGame.metricData.path_per_floor.add(roomMetricKey);
        }

        // 14. 현재 맵 노드 변경 (setCurrMapNode)
        setCurrMapNode(nextRoom);
    }

    // 15. 방 진입 (onPlayerEntry 호출)
    if (getCurrRoom() != null && !isLoadingPostCombatSave) {
        for (AbstractRelic r : player.relics) {
            r.onEnterRoom(getCurrRoom());
        }
    }

    // ... 이후 scene.nextRoom(currMapNode.room) 호출
}
```

### 4.3 플레이어 리셋 (resetPlayer)

```java
public static void resetPlayer() {
    // 1. 오브 정리
    player.orbs.clear();

    // 2. 애니메이션 리셋
    player.animX = 0.0F;
    player.animY = 0.0F;

    // 3. 체력바 숨기기
    player.hideHealthBar();

    // 4. 카드 더미 정리
    player.hand.clear();
    player.drawPile.clear();
    player.discardPile.clear();
    player.exhaustPile.clear();
    player.limbo.clear();

    // 5. 파워 정리
    player.powers.clear();

    // 6. 블록 제거
    player.loseBlock(true);

    // 7. 전투 통계 리셋
    player.damagedThisCombat = 0;

    // 8. 자세 리셋
    if (!player.stance.ID.equals("Neutral")) {
        player.stance = new NeutralStance();
        player.onStanceChange("Neutral");
    }

    // 9. 턴 카운터 리셋
    GameActionManager.turn = 1;
}
```

---

## 5. 층 이동 시 처리 과정

### 5.1 floorNum 증가 위치

**nextRoomTransition()에서 증가**:
```java
if (!CardCrawlGame.loadingSave) {
    incrementFloorBasedMetrics();
    floorNum++;  // ← 여기서 증가!

    StatsScreen.incrementFloorClimbed();
    SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
}
```

**중요**: `initializeFirstRoom()`에서는 증가하지 않음!
```java
public static void initializeFirstRoom() {
    fadeIn();

    floorNum++;  // 임시 증가 (세이브 체크용)
    if (currMapNode.room instanceof MonsterRoom) {
        if (!CardCrawlGame.loadingSave) {
            if (SaveHelper.shouldSave()) {
                SaveHelper.saveIfAppropriate(SaveFile.SaveType.ENTER_ROOM);
            } else {
                // 메트릭 저장
            }
        }
        floorNum--;  // 다시 감소! (실제 증가는 nextRoomTransition에서)
    }

    scene.nextRoom(currMapNode.room);
}
```

### 5.2 층 이동 시 던전 재생성

**보스 처치 후**:
```java
// MonsterRoomBoss.update()
if (boss defeated) {
    AbstractDungeon.getCurrRoom().phase = RoomPhase.COMPLETE;

    // 다음 던전으로 이동
    if (AbstractDungeon.id.equals("Exordium")) {
        // 1층 클리어 -> 2층 (The City) 생성
        AbstractDungeon.generateNextDungeon();
    } else if (AbstractDungeon.id.equals("TheCity")) {
        // 2층 클리어 -> 3층 (The Beyond) 생성
        AbstractDungeon.generateNextDungeon();
    } else if (AbstractDungeon.id.equals("TheBeyond")) {
        // 3층 클리어 -> 보스 선택 or The Ending
        // ...
    }
}
```

**generateNextDungeon()**:
```java
public static void generateNextDungeon() {
    // 1. 현재 던전 정리
    CardCrawlGame.dungeon.dispose();

    // 2. 다음 던전 생성
    if (AbstractDungeon.id.equals("Exordium")) {
        CardCrawlGame.dungeon = new TheCity(player, specialOneTimeEventList);
    } else if (AbstractDungeon.id.equals("TheCity")) {
        CardCrawlGame.dungeon = new TheBeyond(player, specialOneTimeEventList);
    }
    // ...

    // 3. 던전 초기화
    CardCrawlGame.dungeon.initializeFirstRoom();
}
```

### 5.3 층 이동 시 RNG 재생성

```java
// nextRoomTransition() 내부
monsterHpRng = new Random(Settings.seed + floorNum);
aiRng = new Random(Settings.seed + floorNum);
shuffleRng = new Random(Settings.seed + floorNum);
cardRandomRng = new Random(Settings.seed + floorNum);
miscRng = new Random(Settings.seed + floorNum);
```

**중요**: 모든 RNG는 `seed + floorNum`으로 초기화됨
- 같은 seed와 floorNum이면 항상 같은 결과 생성
- floorNum이 변경되면 RNG도 완전히 변경됨

---

## 6. 역방향 이동 시 문제점

### 6.1 이전 방으로 돌아가기 불가 이유

**1. 방 dispose 호출**:
```java
// setCurrMapNode() 내부
if (AbstractDungeon.currMapNode != null && getCurrRoom() != null) {
    getCurrRoom().dispose();  // ← 이전 방 완전 정리
}
```

**dispose() 실행 내용**:
```java
public void dispose() {
    // 이벤트 정리
    if (this.event != null) {
        this.event.dispose();
    }

    // 몬스터 정리
    if (this.monsters != null) {
        for (AbstractMonster m : this.monsters.monsters) {
            m.dispose();  // 텍스처, 애니메이션 해제
        }
    }
}
```

**결과**: 이전 방의 상태가 완전히 삭제됨
- 몬스터 상태
- 이벤트 선택지
- 전투 상태
- 텍스처 참조

**2. floorNum 증가 방향성**:
```java
// nextRoomTransition()에서 항상 증가
floorNum++;

// RNG도 증가한 floorNum으로 재생성
monsterHpRng = new Random(Settings.seed + floorNum);
```

**결과**: 이전 층으로 돌아가려면 floorNum 감소 필요
- 하지만 게임 로직이 감소를 지원하지 않음
- RNG 재생성 시 이전 상태 복원 불가

**3. 몬스터/이벤트 리스트 소진**:
```java
// nextRoomTransition() 내부
if (getCurrRoom() instanceof MonsterRoomElite) {
    if (!eliteMonsterList.isEmpty()) {
        eliteMonsterList.remove(0);  // ← 제거됨
    }
}
```

**결과**: 이전 방으로 돌아가도 몬스터 리스트에서 이미 제거됨
- 같은 몬스터를 다시 생성할 수 없음

### 6.2 이전 층으로 돌아갈 때 필요한 처리

**방법 1: 방 상태 저장**

```java
// 커스텀 데이터 구조
public class SavedRoomState {
    public AbstractRoom room;
    public int floorNum;
    public ArrayList<String> monsterList;
    public ArrayList<String> eliteMonsterList;
    public Random monsterHpRng;
    public Random aiRng;
    // ...
}

// 방 이동 시 저장
public static Stack<SavedRoomState> roomHistory = new Stack<>();

public static void saveCurrentRoom() {
    SavedRoomState state = new SavedRoomState();
    state.room = AbstractDungeon.getCurrRoom().makeCopy();  // 딥 카피 필요
    state.floorNum = AbstractDungeon.floorNum;
    state.monsterList = new ArrayList<>(AbstractDungeon.monsterList);
    state.eliteMonsterList = new ArrayList<>(AbstractDungeon.eliteMonsterList);
    state.monsterHpRng = AbstractDungeon.monsterHpRng.copy();
    // ...
    roomHistory.push(state);
}
```

**방법 2: dispose() 오버라이드**

```java
// setCurrMapNode() 수정
if (AbstractDungeon.currMapNode != null && getCurrRoom() != null) {
    // dispose() 호출하지 않음!
    // getCurrRoom().dispose();  // ← 주석 처리
}

// 대신 방 상태 보존
if (!isGoingBack) {
    getCurrRoom().phase = RoomPhase.INCOMPLETE;  // 완료 상태 유지
}
```

**방법 3: 던전 재생성 방지**

```java
// 이전 층으로 돌아갈 때
public static void goBackToPreviousFloor() {
    if (roomHistory.isEmpty()) {
        return;  // 돌아갈 방이 없음
    }

    SavedRoomState savedState = roomHistory.pop();

    // floorNum 감소
    AbstractDungeon.floorNum = savedState.floorNum;

    // RNG 복원
    AbstractDungeon.monsterHpRng = savedState.monsterHpRng;
    AbstractDungeon.aiRng = savedState.aiRng;
    // ...

    // 몬스터/이벤트 리스트 복원
    AbstractDungeon.monsterList = savedState.monsterList;
    AbstractDungeon.eliteMonsterList = savedState.eliteMonsterList;

    // 방 복원 (dispose 호출하지 않음!)
    AbstractDungeon.currMapNode.room = savedState.room;

    // 씬 전환
    AbstractDungeon.scene.nextRoom(savedState.room);
}
```

---

## 7. 방 클린업 누락 시 발생하는 문제

### 7.1 dispose() 호출 누락

**증상**:
1. **메모리 누수**
   ```java
   // 텍스처가 해제되지 않음
   public void dispose() {
       if (this.monsters != null) {
           for (AbstractMonster m : this.monsters.monsters) {
               m.dispose();  // ← 호출 안 되면 텍스처 메모리 누수
           }
       }
   }
   ```

2. **이벤트 리스너 중복**
   ```java
   // 이벤트 핸들러가 계속 누적됨
   public void onPlayerEntry() {
       InputHelper.registerListener(someListener);  // ← dispose에서 제거 안 되면 중복 등록
   }
   ```

3. **몬스터 업데이트 중복**
   ```java
   // 이전 방의 몬스터가 계속 업데이트됨
   public void update() {
       this.monsters.update();  // ← 이전 방 몬스터도 업데이트 시도
   }
   ```

### 7.2 resetPlayer() 호출 누락

**증상**:
1. **전투 상태 유지**
   ```java
   // 이전 전투의 파워가 다음 방으로 넘어감
   player.powers.clear();  // ← 호출 안 되면 버프/디버프 유지
   ```

2. **카드 더미 오염**
   ```java
   // 이전 전투의 카드가 남아있음
   player.hand.clear();
   player.drawPile.clear();
   player.discardPile.clear();
   player.exhaustPile.clear();
   ```

3. **블록 유지**
   ```java
   // 이전 전투의 블록이 유지됨
   player.loseBlock(true);  // ← 호출 안 되면 블록이 다음 방으로 넘어감
   ```

### 7.3 액션 매니저 정리 누락

**증상**:
1. **액션 누적**
   ```java
   // nextRoomTransition() 내부
   if (!actionManager.actions.isEmpty()) {
       logger.info("[WARNING] Action Manager was NOT clear! Clearing");
       actionManager.clear();
   }
   ```

2. **순환 참조**
   ```java
   // 이전 전투의 액션이 플레이어/몬스터 참조 유지
   // -> 가비지 컬렉션 불가
   ```

### 7.4 이펙트 정리 누락

**증상**:
1. **화면 오염**
   ```java
   // nextRoomTransition() 내부
   effectList.clear();
   topLevelEffectsQueue.clear();
   effectsQueue.clear();
   ```

2. **VFX 누적**
   ```java
   // 이전 방의 파티클 효과가 계속 렌더링됨
   for (AbstractGameEffect e : effectList) {
       e.render(sb);  // ← 이전 방 효과도 렌더링
   }
   ```

---

## 8. 수정 방법 (방 이동 로직 변경)

### 8.1 역방향 이동 지원

**패치 예시**:

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public static class RoomHistoryFields {
    public static SpireField<Stack<SavedRoomState>> roomHistory =
        new SpireField<>(() -> new Stack<>());
}

@SpirePatch(clz = AbstractDungeon.class, method = "nextRoomTransition")
public static class SaveRoomStatePatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractDungeon __instance, SaveFile saveFile) {
        // 현재 방 상태 저장
        SavedRoomState state = new SavedRoomState();
        state.room = AbstractDungeon.getCurrRoom();
        state.floorNum = AbstractDungeon.floorNum;
        state.monsterList = new ArrayList<>(AbstractDungeon.monsterList);
        state.eliteMonsterList = new ArrayList<>(AbstractDungeon.eliteMonsterList);
        // ... RNG 복사 ...

        RoomHistoryFields.roomHistory.get(__instance).push(state);
    }
}

@SpirePatch(clz = AbstractDungeon.class, method = "setCurrMapNode")
public static class PreserveRoomPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(MapRoomNode currMapNode) {
        // dispose() 호출하지 않음
        if (AbstractDungeon.currMapNode != null && AbstractDungeon.getCurrRoom() != null) {
            // getCurrRoom().dispose();  // ← 주석 처리
        }

        AbstractDungeon.currMapNode = currMapNode;

        // ... 나머지 로직 ...

        return SpireReturn.Return(null);  // 원본 메서드 건너뜀
    }
}

// 역방향 이동 메서드
public static void goBackToPreviousRoom() {
    Stack<SavedRoomState> history = RoomHistoryFields.roomHistory.get(AbstractDungeon.instance);
    if (history.isEmpty()) {
        return;
    }

    SavedRoomState savedState = history.pop();

    // 상태 복원
    AbstractDungeon.floorNum = savedState.floorNum;
    AbstractDungeon.monsterList = savedState.monsterList;
    AbstractDungeon.eliteMonsterList = savedState.eliteMonsterList;
    // ... RNG 복원 ...

    // 방 복원
    AbstractDungeon.currMapNode.room = savedState.room;

    // 플레이어는 리셋하지 않음 (현재 상태 유지)
    // AbstractDungeon.resetPlayer();  // ← 주석 처리

    // 씬 전환
    AbstractDungeon.scene.nextRoom(savedState.room);
}
```

### 8.2 커스텀 방 전환 로직

```java
public class CustomRoomTransition {
    // 다음 방으로 이동 (기존 로직 유지)
    public static void goToNextRoom(MapRoomNode nextNode) {
        // 1. 현재 방 정리
        if (AbstractDungeon.getCurrRoom() != null) {
            AbstractDungeon.getCurrRoom().dispose();
        }

        // 2. 플레이어 리셋
        AbstractDungeon.resetPlayer();

        // 3. floorNum 증가
        AbstractDungeon.floorNum++;

        // 4. RNG 재생성
        AbstractDungeon.monsterHpRng = new Random(Settings.seed + AbstractDungeon.floorNum);
        // ...

        // 5. 방 전환
        AbstractDungeon.setCurrMapNode(nextNode);
        AbstractDungeon.scene.nextRoom(nextNode.room);
    }

    // 이전 방으로 이동 (커스텀 로직)
    public static void goToPreviousRoom(MapRoomNode previousNode, boolean resetPlayer) {
        // 1. 현재 방 정리 (선택적)
        if (resetPlayer) {
            if (AbstractDungeon.getCurrRoom() != null) {
                AbstractDungeon.getCurrRoom().dispose();
            }
        }

        // 2. 플레이어 리셋 (선택적)
        if (resetPlayer) {
            AbstractDungeon.resetPlayer();
        }

        // 3. floorNum 감소
        AbstractDungeon.floorNum--;

        // 4. RNG 재생성
        AbstractDungeon.monsterHpRng = new Random(Settings.seed + AbstractDungeon.floorNum);
        // ...

        // 5. 방 전환 (dispose 호출하지 않음)
        AbstractDungeon.currMapNode = previousNode;

        // 6. 방이 없으면 재생성
        if (previousNode.room == null) {
            previousNode.room = generateRoomForNode(previousNode);
        }

        // 7. 씬 전환
        AbstractDungeon.scene.nextRoom(previousNode.room);
    }

    // 노드에 맞는 방 생성
    private static AbstractRoom generateRoomForNode(MapRoomNode node) {
        // 노드 타입에 따라 방 생성
        // ...
    }
}
```

---

## 9. 요약

### 방 진입 순서
1. `nextRoomTransitionStart()` - 페이드 아웃
2. `nextRoomTransition()` - 플레이어 리셋, floorNum 증가, RNG 재생성
3. `setCurrMapNode()` - 이전 방 dispose, 현재 노드 변경
4. `scene.nextRoom()` - 방 렌더링 준비
5. `room.onPlayerEntry()` - 방 초기화 (몬스터 생성, 이벤트 시작 등)

### 필수 클린업 항목
1. **이전 방 dispose**: `getCurrRoom().dispose()`
2. **플레이어 리셋**: `resetPlayer()`
3. **액션 매니저 정리**: `actionManager.clear()`
4. **이펙트 정리**: `effectList.clear()`
5. **사운드 정리**: `stance.stopIdleSfx()`, `sound.fadeOut()`

### floorNum 증가 시점
- `nextRoomTransition()`에서 증가
- `initializeFirstRoom()`에서는 임시 증가 후 다시 감소
- RNG는 항상 `seed + floorNum`으로 재생성

### 역방향 이동 시 필요한 처리
1. **방 상태 저장**: dispose 전에 상태 보존
2. **floorNum 감소**: 이전 층으로 복귀
3. **RNG 복원**: 이전 층의 RNG 상태 복원
4. **몬스터/이벤트 리스트 복원**: 제거된 항목 복원
5. **dispose 방지**: 이전 방을 다시 사용할 경우

### 클린업 누락 시 문제
1. **메모리 누수**: 텍스처, 몬스터 오브젝트 미해제
2. **이벤트 리스너 중복**: 입력 핸들러 중복 등록
3. **상태 오염**: 파워, 블록, 카드 더미가 다음 방으로 넘어감
4. **VFX 누적**: 이전 방 효과가 계속 렌더링됨
