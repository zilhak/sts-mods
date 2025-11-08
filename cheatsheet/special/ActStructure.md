# 던전 막(Act) 구조 편집 완전 가이드

> **Slay the Spire의 막 시스템을 완전히 분해하고 재구성하는 방법**

---

## 📋 목차

1. [기본 막 시스템 분석](#1-기본-막-시스템-분석)
2. [막 전환 메커니즘](#2-막-전환-메커니즘)
3. [막 순서/구성 변경](#3-막-순서구성-변경)
4. [새 막 추가 방법](#4-새-막-추가-방법)
5. [고급 수정](#5-고급-수정)
6. [주의사항](#6-주의사항)

---

## 1. 기본 막 시스템 분석

### 1.1 actNum 변수

**위치**: `AbstractDungeon.java` Line 186
```java
public static int actNum = 0;
```

**역할**:
- 현재 막 번호 추적 (0부터 시작, 1막 진입 시 1로 증가)
- RNG 시드 계산에 사용 (`mapRng = new Random(seed + actNum)`)
- Ascension 효과 일부가 actNum 기반 적용

**변경 시점**:
- `dungeonTransitionSetup()` 메서드에서 `actNum++` (Line 3107)
- 새 막 생성자에서 호출됨

### 1.2 각 막 클래스 구조

| 막 | 클래스 | ID | 층수 | 특징 |
|----|--------|-----|------|------|
| **1막** | `Exordium` | "Exordium" | 15층 | The Bottom, 기본 몬스터 |
| **2막** | `TheCity` | "TheCity" | 15층 | The City, 중급 몬스터 |
| **3막** | `TheBeyond` | "TheBeyond" | 15층 | The Beyond, 고급 몬스터 |
| **4막** | `TheEnding` | "TheEnding" | 3층 | Heart 보스만 |

### 1.3 AbstractDungeon 생성자 초기화 순서

**모든 막이 따르는 필수 순서** (AbstractDungeon.java Line 330-376):

```java
public AbstractDungeon(String name, String levelId, AbstractPlayer p,
                       ArrayList<String> newSpecialOneTimeEventList) {
    CardCrawlGame.dungeon = this;  // 전역 던전 설정

    // 1. 기본 설정
    this.name = name;
    this.id = levelId;
    this.player = p;

    // 2. 시스템 초기화
    actionManager = new GameActionManager();
    overlayMenu = new OverlayMenu(p);
    dynamicBanner = new DynamicBanner();

    // 3. 필수 초기화 순서 (순서 바뀌면 crash!)
    dungeonTransitionSetup();    // actNum++, 리스트 클리어
    generateMonsters();           // 몬스터 풀 생성
    initializeBoss();            // 보스 선택
    setBoss(bossList.get(0));    // 보스 그래픽 로드
    initializeEventList();       // 이벤트 풀
    initializeEventImg();        // 이벤트 그래픽
    initializeShrineList();      // 신전 풀
    initializeCardPools();       // 카드 보상 풀
    initializePotions();         // 포션 슬롯 설정
    BlightHelper.initialize();   // Blight 시스템
}
```

### 1.4 각 막 클래스 예시 (Exordium)

```java
// Exordium.java
public class Exordium extends AbstractDungeon {
    public static final String NAME = "The Bottom";
    public static final String ID = "Exordium";

    public Exordium(AbstractPlayer p, ArrayList<String> emptyList) {
        super(NAME, ID, p, emptyList);

        // 유물 풀 초기화
        initializeRelicList();

        // Scene 설정
        if (scene != null) {
            scene.dispose();
        }
        scene = new TheBottomScene();
        fadeColor = Color.valueOf("1e0f0aff");

        // 이벤트 초기화
        initializeSpecialOneTimeEventList();
        initializeLevelSpecificChances();

        // 지도 생성 (actNum 기반 시드)
        mapRng = new Random(Settings.seed + actNum);
        generateMap();

        // 배경 음악
        CardCrawlGame.music.changeBGM(id);

        // 시작 방
        currMapNode = new MapRoomNode(0, -1);
        if (Settings.isShowBuild || !TipTracker.tips.get("NEOW_SKIP")) {
            currMapNode.room = new EmptyRoom();
        } else {
            currMapNode.room = new NeowRoom(false);
        }
    }
}
```

---

## 2. 막 전환 메커니즘

### 2.1 전체 흐름

```
보스 처치 → isDungeonBeaten = true → CardCrawlGame.update()
    ↓
fadeColor.a == 1.0F (페이드 완료) 체크
    ↓
CardCrawlGame.nextDungeon 설정 ("TheCity", "TheBeyond" 등)
    ↓
dungeonTransitionScreen = new DungeonTransitionScreen(nextDungeon)
    ↓
DUNGEON_TRANSITION 모드 진입
    ↓
dungeonTransitionScreen.isComplete == true
    ↓
getDungeon(nextDungeon, player) 호출 → 새 던전 생성
```

### 2.2 CardCrawlGame.getDungeon() 메서드

**위치**: `CardCrawlGame.java` Line 1342-1356

```java
public AbstractDungeon getDungeon(String key, AbstractPlayer p) {
    ArrayList<String> emptyList;
    switch (key) {
        case "Exordium":
            emptyList = new ArrayList<>();
            return new Exordium(p, emptyList);
        case "TheCity":
            return new TheCity(p, AbstractDungeon.specialOneTimeEventList);
        case "TheBeyond":
            return new TheBeyond(p, AbstractDungeon.specialOneTimeEventList);
        case "TheEnding":
            return new TheEnding(p, AbstractDungeon.specialOneTimeEventList);
    }
    return null;
}
```

**핵심**: 막 이름 문자열로 다음 막 결정!

### 2.3 dungeonTransitionSetup() 메서드

**위치**: `AbstractDungeon.java` Line 3106-3160

**역할**:
```java
public static void dungeonTransitionSetup() {
    actNum++;  // 막 번호 증가

    // RNG 카운터 정리 (250단위로)
    if (cardRng.counter > 0 && cardRng.counter < 250) {
        cardRng.setCounter(250);
    }

    // 경로 데이터 초기화
    pathX.clear();
    pathY.clear();

    // 이벤트/몬스터 리스트 초기화
    EventHelper.resetProbabilities();
    eventList.clear();
    shrineList.clear();
    monsterList.clear();
    eliteMonsterList.clear();
    bossList.clear();

    // Ascension 5+: 플레이어 힐
    if (ascensionLevel >= 5) {
        player.heal(player.maxHealth);
    }
}
```

---

## 3. 막 순서/구성 변경

### 예제 1: 막 순서 변경 (3막 → 2막 → 1막 역순)

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "update"
)
public static class ReverseActOrderPatch {
    @SpireInsertPatch(
        locator = DungeonTransitionLocator.class
    )
    public static SpireReturn<Void> Insert(CardCrawlGame __instance) {
        if (AbstractDungeon.isDungeonBeaten &&
            AbstractDungeon.fadeColor.a == 1.0F) {

            // 현재 막에 따라 다음 막 결정
            String currentDungeon = AbstractDungeon.id;
            String nextDungeon;

            switch (currentDungeon) {
                case "Exordium":  // 1막 클리어 후
                    nextDungeon = null;  // 종료
                    break;
                case "TheCity":   // 2막 클리어 후
                    nextDungeon = "Exordium";  // 1막으로
                    break;
                case "TheBeyond": // 3막 클리어 후
                    nextDungeon = "TheCity";   // 2막으로
                    break;
                default:
                    nextDungeon = "TheBeyond";  // 시작은 3막
            }

            if (nextDungeon != null) {
                CardCrawlGame.nextDungeon = nextDungeon;
                CardCrawlGame.dungeonTransitionScreen =
                    new DungeonTransitionScreen(nextDungeon);
            }

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}

public static class DungeonTransitionLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher finalMatcher = new Matcher.FieldAccessMatcher(
            AbstractDungeon.class, "isDungeonBeaten"
        );
        return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
    }
}
```

### 예제 2: 막 생략 (1막 → 3막 직행)

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "getDungeon",
    paramtypez = {String.class, AbstractPlayer.class}
)
public static class SkipActPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractDungeon> Prefix(
        CardCrawlGame __instance, String key, AbstractPlayer p) {

        // 1막 클리어 후 바로 3막
        if (key.equals("TheCity") && AbstractDungeon.actNum == 1) {
            ArrayList<String> emptyList = new ArrayList<>();
            // actNum을 2로 수정 (3막용)
            AbstractDungeon.actNum = 2;
            return SpireReturn.Return(
                new TheBeyond(p, AbstractDungeon.specialOneTimeEventList)
            );
        }

        return SpireReturn.Continue();
    }
}
```

### 예제 3: 층수 변경 (15층 → 20층)

```java
@SpirePatch(
    clz = Exordium.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class TallerActPatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        // 지도 재생성 (20층)
        ArrayList<ArrayList<MapRoomNode>> newMap =
            MapGenerator.generateDungeon(
                20,  // 높이 15 → 20
                7,   // 너비 (동일)
                6,   // 경로 개수 (동일)
                __instance.mapRng
            );

        // 보스 층 변경 (15층 → 20층)
        MapRoomNode bossNode = new MapRoomNode(3, 20);
        bossNode.room = new MonsterRoomBoss();

        // 필수 방 재배치
        // 14층 → 19층 (휴식처)
        // 8층은 그대로

        ReflectionHacks.setPrivate(__instance, AbstractDungeon.class,
            "map", newMap);
    }
}
```

### 예제 4: 무한 반복 모드 (1→2→3→1→2→3...)

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "update"
)
public static class InfiniteActsPatch {
    private static int loopCount = 0;

    @SpireInsertPatch(locator = DungeonTransitionLocator.class)
    public static SpireReturn<Void> Insert(CardCrawlGame __instance) {
        if (AbstractDungeon.isDungeonBeaten &&
            AbstractDungeon.fadeColor.a == 1.0F) {

            String currentDungeon = AbstractDungeon.id;
            String nextDungeon;

            switch (currentDungeon) {
                case "Exordium":
                    nextDungeon = "TheCity";
                    break;
                case "TheCity":
                    nextDungeon = "TheBeyond";
                    break;
                case "TheBeyond":
                    loopCount++;
                    nextDungeon = "Exordium";  // 다시 1막

                    // 난이도 증가 (루프마다)
                    applyLoopDifficulty(loopCount);
                    break;
                default:
                    nextDungeon = "Exordium";
            }

            CardCrawlGame.nextDungeon = nextDungeon;
            CardCrawlGame.dungeonTransitionScreen =
                new DungeonTransitionScreen(nextDungeon);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static void applyLoopDifficulty(int loops) {
        // 루프마다 몬스터 강화
        // (별도 구현 필요)
    }
}
```

---

## 4. 새 막 추가 방법

### 4.1 새 막 클래스 작성

```java
package com.mymod.dungeons;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
// ... imports

public class MyCustomAct extends AbstractDungeon {
    public static final String ID = "MyCustomAct";
    public static final String NAME = "커스텀 막";

    public MyCustomAct(AbstractPlayer p, ArrayList<String> eventList) {
        super(NAME, ID, p, eventList);

        // 1. 유물 풀
        initializeRelicList();

        // 2. Scene 설정
        if (scene != null) {
            scene.dispose();
        }
        scene = new MyCustomScene();  // 커스텀 배경
        fadeColor = Color.valueOf("ff0000ff");  // 빨간색 페이드

        // 3. 이벤트 초기화
        initializeSpecialOneTimeEventList();
        initializeLevelSpecificChances();

        // 4. 지도 생성
        mapRng = new Random(Settings.seed + actNum);
        generateMap();

        // 5. 음악
        CardCrawlGame.music.changeBGM("custom_bgm");

        // 6. 시작 방
        currMapNode = new MapRoomNode(0, -1);
        currMapNode.room = new MonsterRoom();
    }

    @Override
    protected void initializeMonsters() {
        // 몬스터 풀 설정
        monsterList.add("Cultist");
        monsterList.add("JawWorm");
        monsterList.add("2 Louse");
        // ... 더 추가

        // 엘리트
        eliteMonsterList.add("Lagavulin");
        eliteMonsterList.add("3 Sentries");
    }

    @Override
    protected void initializeEventList() {
        // 이벤트 풀
        eventList.add("Falling");
        eventList.add("Shining Light");
        // ... 더 추가
    }

    @Override
    protected void initializeBoss() {
        // 보스 풀
        bossList.add("Hexaghost");
        bossList.add("Slime Boss");
        bossList.add("The Guardian");

        Collections.shuffle(bossList,
            new Random(monsterRng.randomLong()));
    }

    @Override
    protected void initializeRelicList() {
        // 이 막에서 등장할 유물
        commonRelicPool.add("Blood Vial");
        uncommonRelicPool.add("Ornamental Fan");
        rareRelicPool.add("Bird Faced Urn");
        // ... 더 추가
    }
}
```

### 4.2 getDungeon()에 새 막 등록

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "getDungeon",
    paramtypez = {String.class, AbstractPlayer.class}
)
public static class AddCustomActPatch {
    @SpirePostfixPatch
    public static AbstractDungeon Postfix(
        AbstractDungeon result,
        CardCrawlGame __instance,
        String key,
        AbstractPlayer p) {

        // 우리의 커스텀 막 처리
        if (key.equals("MyCustomAct")) {
            return new MyCustomAct(p,
                AbstractDungeon.specialOneTimeEventList);
        }

        return result;
    }
}
```

### 4.3 막 삽입 (1.5막: 1막과 2막 사이)

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "update"
)
public static class InsertCustomActPatch {
    @SpireInsertPatch(locator = DungeonTransitionLocator.class)
    public static SpireReturn<Void> Insert(CardCrawlGame __instance) {
        if (AbstractDungeon.isDungeonBeaten &&
            AbstractDungeon.fadeColor.a == 1.0F) {

            String currentDungeon = AbstractDungeon.id;
            String nextDungeon;

            if (currentDungeon.equals("Exordium")) {
                // 1막 클리어 후 → 커스텀 막
                nextDungeon = "MyCustomAct";
            } else if (currentDungeon.equals("MyCustomAct")) {
                // 커스텀 막 클리어 후 → 2막
                nextDungeon = "TheCity";
            } else if (currentDungeon.equals("TheCity")) {
                // 2막 클리어 후 → 3막
                nextDungeon = "TheBeyond";
            } else {
                nextDungeon = "TheEnding";
            }

            CardCrawlGame.nextDungeon = nextDungeon;
            CardCrawlGame.dungeonTransitionScreen =
                new DungeonTransitionScreen(nextDungeon);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 5. 고급 수정

### 5.1 분기형 막 (선택에 따라 다른 막)

```java
// 1막 클리어 후 선택지 화면
@SpirePatch(
    clz = VictoryScreen.class,
    method = "reopen"
)
public static class BranchingActPatch {
    @SpirePostfixPatch
    public static void Postfix(VictoryScreen __instance) {
        if (AbstractDungeon.id.equals("Exordium")) {
            // 선택 화면 표시
            showActChoiceScreen();
        }
    }

    private static void showActChoiceScreen() {
        // GridCardSelectScreen을 활용하여 선택지 표시
        // 버튼 A: "도시로 (The City)"
        // 버튼 B: "황야로 (The Wilderness - 커스텀)"

        AbstractDungeon.gridSelectScreen.open(
            Arrays.asList(
                makeChoiceCard("TheCity", "도시로"),
                makeChoiceCard("TheWilderness", "황야로")
            ),
            1,  // 1개만 선택
            "다음 목적지를 선택하세요",
            false
        );
    }

    // 선택 처리
    @SpirePatch(
        clz = GridCardSelectScreen.class,
        method = "update"
    )
    public static class HandleChoicePatch {
        @SpirePostfixPatch
        public static void Postfix(GridCardSelectScreen __instance) {
            if (__instance.selectedCards.size() > 0) {
                AbstractCard choice = __instance.selectedCards.get(0);
                String chosenAct = choice.cardID;

                CardCrawlGame.nextDungeon = chosenAct;
                CardCrawlGame.dungeonTransitionScreen =
                    new DungeonTransitionScreen(chosenAct);

                __instance.selectedCards.clear();
            }
        }
    }
}
```

### 5.2 조건부 비밀 막

```java
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "update"
)
public static class SecretActPatch {
    @SpireInsertPatch(locator = DungeonTransitionLocator.class)
    public static SpireReturn<Void> Insert(CardCrawlGame __instance) {
        if (AbstractDungeon.isDungeonBeaten &&
            AbstractDungeon.fadeColor.a == 1.0F) {

            String currentDungeon = AbstractDungeon.id;
            String nextDungeon;

            if (currentDungeon.equals("TheCity")) {
                // 조건 체크: HP 50% 이하
                if (AbstractDungeon.player.currentHealth <
                    AbstractDungeon.player.maxHealth * 0.5f) {

                    // 비밀 막 진입!
                    nextDungeon = "SecretAct";
                } else {
                    // 일반 3막
                    nextDungeon = "TheBeyond";
                }

                CardCrawlGame.nextDungeon = nextDungeon;
                CardCrawlGame.dungeonTransitionScreen =
                    new DungeonTransitionScreen(nextDungeon);

                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }
}
```

### 5.3 난이도 증가 무한 모드 (상세)

```java
public class InfiniteActsManager {
    public static int loopCount = 0;
    public static HashMap<String, Float> difficultyMultipliers =
        new HashMap<>();

    public static void applyLoopDifficulty(int loops) {
        // HP 배율
        float hpMult = 1.0f + (loops * 0.2f);  // 루프당 +20%
        difficultyMultipliers.put("hp", hpMult);

        // 데미지 증가
        int dmgIncrease = loops * 2;  // 루프당 +2
        difficultyMultipliers.put("dmg", (float)dmgIncrease);

        logger.info("Loop " + loops + ": HP x" + hpMult +
                   ", Damage +" + dmgIncrease);
    }
}

// 몬스터 HP 증가 적용
@SpirePatch(
    clz = AbstractMonster.class,
    method = "init"
)
public static class LoopHPPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (InfiniteActsManager.loopCount > 0) {
            float mult = InfiniteActsManager.difficultyMultipliers.get("hp");
            __instance.maxHealth = (int)(__instance.maxHealth * mult);
            __instance.currentHealth = __instance.maxHealth;
        }
    }
}

// 몬스터 데미지 증가 적용
@SpirePatch(
    clz = AbstractMonster.class,
    method = "usePreBattleAction"
)
public static class LoopDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (InfiniteActsManager.loopCount > 0) {
            int dmgInc = InfiniteActsManager.difficultyMultipliers.get("dmg").intValue();

            for (DamageInfo info : __instance.damage) {
                if (info != null && info.base > 0) {
                    info.base += dmgInc;
                }
            }
        }
    }
}
```

---

## 6. 주의사항

### 6.1 actNum 관리

**문제**: actNum은 RNG 시드에 사용되므로 잘못 설정하면 예측 불가능한 결과

**해결**:
- actNum을 직접 수정하지 말고 `dungeonTransitionSetup()` 호출
- 막 순서를 바꿔도 actNum은 계속 증가하도록 유지
- 무한 모드에서는 별도 loopCount 변수 사용

### 6.2 던전 초기화 순서

**반드시 지켜야 할 순서** (AbstractDungeon 생성자):
1. `dungeonTransitionSetup()` - actNum 증가, 리스트 클리어
2. `generateMonsters()` - 몬스터 풀 (클리어된 리스트에 추가)
3. `initializeBoss()` - 보스 선택 (monsterRng 사용)
4. `initializeEventList()` - 이벤트 풀
5. `generateMap()` - 지도 생성 (mapRng 사용)

**순서 위반 시 문제**:
- 보스 선택 전 몬스터 생성 → bossList 비어있음
- 지도 생성 전 RNG 사용 → 시드 불일치

### 6.3 메모리 관리

**필수 정리 항목**:
```java
// 이전 막 dispose
if (scene != null) {
    scene.dispose();  // Scene 리소스 정리
}

// 던전 전환 시 자동 정리 (dungeonTransitionSetup)
eventList.clear();
shrineList.clear();
monsterList.clear();
eliteMonsterList.clear();
bossList.clear();
```

### 6.4 세이브 파일 호환성

**문제**: 새 막 추가 시 기존 세이브 파일에서 막 ID 인식 불가

**해결**:
```java
// getDungeon(String key, AbstractPlayer p, SaveFile saveFile) 오버로드
@SpirePatch(
    clz = CardCrawlGame.class,
    method = "getDungeon",
    paramtypez = {String.class, AbstractPlayer.class, SaveFile.class}
)
public static class SaveFileCompatPatch {
    @SpirePostfixPatch
    public static AbstractDungeon Postfix(
        AbstractDungeon result,
        CardCrawlGame __instance,
        String key,
        AbstractPlayer p,
        SaveFile saveFile) {

        if (key.equals("MyCustomAct")) {
            return new MyCustomAct(p, saveFile);
        }

        return result;
    }
}
```

### 6.5 UI/화면 크기 제한

**지도 높이 제한**:
- 화면 높이: 1080p 기준
- 15층 이상 → 스크롤 또는 축소 필요
- 20층 초과 → UI 깨짐 가능성

**해결**:
- 지도 렌더링 스케일 조정
- 스크롤 가능한 지도 UI 구현

---

## 7. 실전 활용 예제

### 예제 A: "5막 모드"

**구성**: 1막 → 2막 → 3막 → 커스텀 막 → Heart

```java
// 3막 클리어 후 커스텀 막으로
if (currentDungeon.equals("TheBeyond")) {
    nextDungeon = "BossRushAct";  // 모든 보스 혼합
}
// 커스텀 막 클리어 후 Heart로
else if (currentDungeon.equals("BossRushAct")) {
    nextDungeon = "TheEnding";
}
```

### 예제 B: "로그라이크 무한 모드"

**구성**: 막 무한 반복, 매 순환마다 +10% 난이도

```java
// 위의 InfiniteActsPatch + 난이도 증가 시스템 사용
```

### 예제 C: "분기 스토리 모드"

**구성**: 1막 후 선택 → A 경로 (전투) vs B 경로 (이벤트)

```java
// 위의 BranchingActPatch 사용
// A 경로: TheCity (기존)
// B 경로: EventHeavyAct (커스텀, 이벤트 80%)
```

---

## 8. 디버깅

### 8.1 막 전환 로깅

```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "dungeonTransitionSetup"
)
public static class DebugActTransitionPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        logger.info("=== Dungeon Transition ===");
        logger.info("actNum: " + AbstractDungeon.actNum);
        logger.info("Current dungeon: " + AbstractDungeon.id);
        logger.info("Next dungeon: " + CardCrawlGame.nextDungeon);
        logger.info("Monster pool size: " + AbstractDungeon.monsterList.size());
        logger.info("Boss pool size: " + AbstractDungeon.bossList.size());
        logger.info("=========================");
    }
}
```

### 8.2 막 초기화 체크리스트

```
[ ] 몬스터 풀 비어있지 않음 (monsterList.size() > 0)
[ ] 보스 풀 비어있지 않음 (bossList.size() > 0)
[ ] 지도 생성 성공 (map != null && map.size() == 15)
[ ] RNG 시드 초기화 (mapRng != null)
[ ] Scene 로드 완료 (scene != null)
[ ] actNum 정상 증가 (actNum == 예상값)
```

### 8.3 일반적인 오류

| 오류 | 원인 | 해결 |
|------|------|------|
| NullPointerException in getMonster | monsterList 비어있음 | generateMonsters() 호출 확인 |
| 지도 생성 실패 | mapRng null 또는 시드 오류 | mapRng 초기화 확인 |
| 보스 없음 | bossList 비어있음 | initializeBoss() 호출 확인 |
| actNum 불일치 | dungeonTransitionSetup() 미호출 | 생성자 초기화 순서 확인 |
| 리소스 누수 | scene.dispose() 누락 | 이전 scene dispose 확인 |

---

## 9. 참고 자료

### 9.1 관련 파일

- **AbstractDungeon.java** - 던전 기본 클래스
- **Exordium.java, TheCity.java, TheBeyond.java, TheEnding.java** - 막 구현
- **CardCrawlGame.java** - getDungeon(), 전환 로직
- **DungeonTransitionScreen.java** - 막 전환 화면

### 9.2 관련 문서

- **system/DungeonInitialization.md** - 던전 초기화 상세
- **system/StateTransition.md** - 게임 상태 전환
- **special/MapGeneration.md** - 지도 생성 시스템

---

## 10. 요약

**막 시스템 핵심**:
1. `actNum` 변수로 현재 막 추적
2. `CardCrawlGame.nextDungeon` 문자열로 다음 막 결정
3. `getDungeon(String key)` 메서드로 막 생성
4. `dungeonTransitionSetup()` 필수 호출 (actNum++, 리스트 초기화)

**막 추가 3단계**:
1. AbstractDungeon 상속 클래스 작성
2. getDungeon() 패치로 등록
3. 전환 로직 패치로 삽입

**주의사항**:
- 초기화 순서 엄수
- actNum 관리 주의
- 리소스 정리 필수
- 세이브 파일 호환성 고려
