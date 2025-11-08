# 멀티 보스 시스템 완전 분석 (Ascension 20 Double Boss System)

## 목차
1. [Ascension 20 보스 2마리 시스템 분석](#1-ascension-20-보스-2마리-시스템-분석)
2. [3막 보스 종류와 특징](#2-3막-보스-종류와-특징)
3. [보스 2마리 생성 전체 흐름](#3-보스-2마리-생성-전체-흐름)
4. [실전 수정 방법](#4-실전-수정-방법)
5. [주의사항](#5-주의사항)
6. [고급 수정](#6-고급-수정)

---

## 1. Ascension 20 보스 2마리 시스템 분석

### 1.1 핵심 메커니즘

Ascension 20에서 3막 보스가 2마리가 되는 시스템은 **매우 간단한 로직**으로 구현되어 있습니다.

**핵심 코드 위치: `ProceedButton.java`**
```java
// Line 116-117
if (AbstractDungeon.ascensionLevel >= 20 && AbstractDungeon.bossList.size() == 2) {
    goToDoubleBoss();
}
```

**작동 원리:**
1. 첫 번째 보스를 처치하고 Proceed 버튼을 누를 때 체크
2. Ascension 20 이상이고, `bossList`에 보스가 2개 남아있으면
3. `goToDoubleBoss()` 메서드로 두 번째 보스전으로 이동

### 1.2 goToDoubleBoss() 메서드 분석

```java
// ProceedButton.java Line 263-271
private void goToDoubleBoss() {
    AbstractDungeon.bossKey = AbstractDungeon.bossList.get(0);  // 남은 보스 선택
    CardCrawlGame.music.fadeOutBGM();
    CardCrawlGame.music.fadeOutTempBGM();
    MapRoomNode node = new MapRoomNode(-1, 15);
    node.room = (AbstractRoom)new MonsterRoomBoss();  // 새 보스룸 생성
    AbstractDungeon.nextRoom = node;
    AbstractDungeon.closeCurrentScreen();
    AbstractDungeon.nextRoomTransitionStart();
    hide();
}
```

**프로세스:**
1. `bossList.get(0)` - 리스트의 첫 번째 보스를 다음 보스로 설정
2. 새로운 `MonsterRoomBoss` 생성
3. 전투 화면으로 전환

### 1.3 bossList 관리

**TheBeyond.java (3막) - initializeBoss() 메서드**
```java
// Line 170-201
protected void initializeBoss() {
    bossList.clear();

    if (Settings.isDailyRun) {
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    } else if (!UnlockTracker.isBossSeen("CROW")) {
        bossList.add("Awakened One");
    } else if (!UnlockTracker.isBossSeen("DONUT")) {
        bossList.add("Donu and Deca");
    } else if (!UnlockTracker.isBossSeen("WIZARD")) {
        bossList.add("Time Eater");
    } else {
        bossList.add("Awakened One");
        bossList.add("Time Eater");
        bossList.add("Donu and Deca");
        Collections.shuffle(bossList, new Random(monsterRng.randomLong()));
    }

    // 중요: 최소 2개 보장
    if (bossList.size() == 1) {
        bossList.add(bossList.get(0));  // 보스 복제
    }
}
```

**핵심 포인트:**
- `bossList`는 항상 최소 2개의 보스를 포함
- 첫 보스 처치 시 `bossList.remove(0)` (MonsterRoomBoss.java Line 24)
- 두 번째 보스는 `bossList.get(0)`으로 접근

### 1.4 보스 배치 로직

**MonsterHelper.java - 보스 생성 코드**
```java
// Line 554-560
case "Time Eater":
    return new MonsterGroup((AbstractMonster)new TimeEater());

case "Awakened One":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Cultist(-590.0F, 10.0F, false),
        (AbstractMonster)new Cultist(-298.0F, -10.0F, false),
        (AbstractMonster)new AwakenedOne(100.0F, 15.0F)
    });

case "Donu and Deca":
    return new MonsterGroup(new AbstractMonster[] {
        (AbstractMonster)new Deca(),
        (AbstractMonster)new Donu()
    });
```

**좌표 시스템:**
- **단일 보스:** 중앙 (0.0F, 0.0F 또는 약간 오프셋)
- **Awakened One:** 컬티스트 2마리(-590, -298) + 보스(100)
- **Donu & Deca:** 이미 2마리 (좌우 배치)

### 1.5 보스 AI 조정

**Ascension 20에서도 보스 AI는 변경되지 않습니다!**
- 각 보스는 독립적으로 행동
- 두 번째 보스는 첫 번째와 동일한 난이도 (Ascension 보정 포함)

---

## 2. 3막 보스 종류와 특징

### 2.1 Awakened One (각성자)

**파일:** `com/megacrit/cardcrawl/monsters/beyond/AwakenedOne.java`

**특징:**
- **2페이즈 보스** (Phase 1: 300/320 HP, Phase 2: 300/320 HP)
- Phase 2에서 부활 (Rebirth)
- 파워 카드 사용 시 Curiosity 발동

**2마리 시 문제점:**
- 2페이즈 * 2 = 4페이즈 전투
- 각각 독립적으로 부활하므로 매우 길어짐
- 파워 카드 견제가 2배로 힘듦

**생성 코드:**
```java
new MonsterGroup(new AbstractMonster[] {
    (AbstractMonster)new Cultist(-590.0F, 10.0F, false),
    (AbstractMonster)new Cultist(-298.0F, -10.0F, false),
    (AbstractMonster)new AwakenedOne(100.0F, 15.0F)
});
```

### 2.2 Time Eater (시간 포식자)

**파일:** `com/megacrit/cardcrawl/monsters/beyond/TimeEater.java`

**특징:**
- **카드 카운트 기반** (12장마다 Time Warp 발동)
- 456 HP (A9: 480 HP)
- Haste, Reverberate, Ripple, Head Slam

**2마리 시 문제점:**
- 카드 카운트가 각각 독립적
- 한 전투에서 2번의 Time Warp 가능
- 매우 긴 전투 시간

**생성 코드:**
```java
new MonsterGroup((AbstractMonster)new TimeEater());
```

### 2.3 Donu and Deca (도누와 데카)

**파일:**
- `com/megacrit/cardcrawl/monsters/beyond/Donu.java`
- `com/megacrit/cardcrawl/monsters/beyond/Deca.java`

**특징:**
- **이미 2마리 보스**
- Donu: 공격 중심, Deca: 방어/디버프 중심
- 상호작용 (한쪽이 죽으면 다른 쪽 강화)

**2마리 시 문제점:**
- 실제로는 4마리가 됨 (2 + 2)
- 화면 배치 문제
- 난이도 폭발

**생성 코드:**
```java
new MonsterGroup(new AbstractMonster[] {
    (AbstractMonster)new Deca(),
    (AbstractMonster)new Donu()
});
```

---

## 3. 보스 2마리 생성 전체 흐름

### 3.1 던전 초기화 (게임 시작)

```
1. AbstractDungeon 생성자 호출
   └─> TheBeyond(player, theList)
       └─> initializeBoss()
           └─> bossList에 보스 3개 추가 및 셔플
               └─> bossList = ["Time Eater", "Awakened One", "Donu and Deca"]
```

### 3.2 첫 번째 보스 선택

```
2. setBoss(bossList.get(0))  // AbstractDungeon.java Line 356
   └─> AbstractDungeon.bossKey = "Time Eater"
```

### 3.3 보스룸 진입

```
3. MonsterRoomBoss.onPlayerEntry()  // MonsterRoomBoss.java Line 19-31
   └─> this.monsters = CardCrawlGame.dungeon.getBoss()
       └─> AbstractDungeon.getBoss()  // AbstractDungeon.java Line 2499-2501
           └─> MonsterHelper.getEncounter(bossKey)
               └─> new MonsterGroup(new TimeEater())
   └─> bossList.remove(0)  // Line 24
       └─> bossList = ["Awakened One", "Donu and Deca"]
```

### 3.4 첫 번째 보스 처치 후

```
4. ProceedButton 클릭
   └─> if (ascensionLevel >= 20 && bossList.size() == 2)
       └─> goToDoubleBoss()
           └─> AbstractDungeon.bossKey = bossList.get(0)  // "Awakened One"
           └─> new MonsterRoomBoss() 생성
           └─> 전투 화면 전환
```

### 3.5 두 번째 보스 전투

```
5. MonsterRoomBoss.onPlayerEntry() 재호출
   └─> this.monsters = getBoss()  // "Awakened One"
   └─> bossList.remove(0)
       └─> bossList = ["Donu and Deca"]
```

### 3.6 시퀀스 다이어그램

```
던전 시작 → initializeBoss() → bossList [Boss1, Boss2, Boss3]
                                    ↓
                        setBoss(Boss1) → bossKey = Boss1
                                    ↓
                        첫 보스전 → bossList.remove(0) → [Boss2, Boss3]
                                    ↓
                (A20+) goToDoubleBoss() → bossKey = Boss2
                                    ↓
                        두 번째 보스전 → bossList.remove(0) → [Boss3]
                                    ↓
                        게임 클리어 (A20) or 4막 진입 (일반)
```

---

## 4. 실전 수정 방법

### 4.1 예제 1: 3막 보스를 3마리로 만들기

**목표:** Ascension 30+에서 3막 보스 3연전

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class TripleBossPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.id.equals("TheBeyond") &&
            AbstractDungeon.ascensionLevel >= 30) {

            // 보스가 3개 이상 남아있으면 계속 진행
            if (AbstractDungeon.bossList.size() >= 2) {
                goToNextBoss();
                return SpireReturn.Return(null);
            }
        }

        return SpireReturn.Continue();
    }

    private static void goToNextBoss() {
        AbstractDungeon.bossKey = AbstractDungeon.bossList.get(0);
        CardCrawlGame.music.fadeOutBGM();
        CardCrawlGame.music.fadeOutTempBGM();
        MapRoomNode node = new MapRoomNode(-1, 15);
        node.room = new MonsterRoomBoss();
        AbstractDungeon.nextRoom = node;
        AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.nextRoomTransitionStart();
        ReflectionHacks.privateMethod(ProceedButton.class, "hide")
            .invoke(__instance);
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                AbstractDungeon.class, "closeCurrentScreen"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}

// bossList에 3개 보스 보장
@SpirePatch(
    clz = TheBeyond.class,
    method = "initializeBoss"
)
public class ThreeBossListPatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            // bossList에 항상 3개 보스 유지
            while (AbstractDungeon.bossList.size() < 3) {
                String randomBoss = getRandomBoss();
                AbstractDungeon.bossList.add(randomBoss);
            }
        }
    }

    private static String getRandomBoss() {
        String[] bosses = {"Awakened One", "Time Eater", "Donu and Deca"};
        return bosses[MathUtils.random(bosses.length - 1)];
    }
}
```

**배치 위치 조정 (3마리용):**

보스를 동시에 3마리 싸우게 하려면 MonsterHelper 패치 필요:

```java
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public class TripleBossEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (AbstractDungeon.ascensionLevel >= 35 &&
            AbstractDungeon.id.equals("TheBeyond")) {

            // 3막 보스를 3마리 동시 배치
            switch (key) {
                case "Awakened One":
                    return SpireReturn.Return(new MonsterGroup(
                        new AwakenedOne(-500.0F, 15.0F),
                        new AwakenedOne(0.0F, 15.0F),
                        new AwakenedOne(500.0F, 15.0F)
                    ));

                case "Time Eater":
                    return SpireReturn.Return(new MonsterGroup(
                        new TimeEater(-400.0F, 30.0F),
                        new TimeEater(0.0F, 30.0F),
                        new TimeEater(400.0F, 30.0F)
                    ));
            }
        }

        return SpireReturn.Continue();
    }
}
```

**주의:** X좌표는 화면 폭에 따라 조정 필요 (±600 정도가 한계)

---

### 4.2 예제 2: 2막 보스를 2마리로 만들기

**목표:** Ascension 20+에서 2막 보스도 2연전

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class Act2DoubleBossPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.id.equals("TheCity") &&  // 2막
            AbstractDungeon.ascensionLevel >= 20 &&
            AbstractDungeon.bossList.size() == 2) {

            goToDoubleBoss(__instance);
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }

    private static void goToDoubleBoss(ProceedButton button) {
        AbstractDungeon.bossKey = AbstractDungeon.bossList.get(0);
        CardCrawlGame.music.fadeOutBGM();
        CardCrawlGame.music.fadeOutTempBGM();
        MapRoomNode node = new MapRoomNode(-1, 15);
        node.room = new MonsterRoomBoss();
        AbstractDungeon.nextRoom = node;
        AbstractDungeon.closeCurrentScreen();
        AbstractDungeon.nextRoomTransitionStart();
        ReflectionHacks.privateMethod(ProceedButton.class, "hide")
            .invoke(button);
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                AbstractDungeon.class, "closeCurrentScreen"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}

// TheCity의 bossList에 2개 보스 보장
@SpirePatch(
    clz = TheCity.class,
    method = "initializeBoss"
)
public class Act2BossListPatch {
    @SpirePostfixPatch
    public static void Postfix(TheCity __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // bossList에 최소 2개 보장
            if (AbstractDungeon.bossList.size() == 1) {
                AbstractDungeon.bossList.add(
                    AbstractDungeon.bossList.get(0)
                );
            }
        }
    }
}
```

**2막 보스 특성 고려:**
- **Bronze Automaton:** 하이퍼빔 패턴, 2마리 시 매우 위험
- **Champ:** 2페이즈, 분노 시스템
- **Collector:** 소환수 관리, 2마리 시 화면 혼잡

---

### 4.3 예제 3: 1막 보스를 2마리로 만들기

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class Act1DoubleBossPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.id.equals("Exordium") &&  // 1막
            AbstractDungeon.ascensionLevel >= 20 &&
            AbstractDungeon.bossList.size() == 2) {

            goToDoubleBoss(__instance);
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }

    // goToDoubleBoss() 메서드는 동일
}

@SpirePatch(
    clz = Exordium.class,
    method = "initializeBoss"
)
public class Act1BossListPatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            if (AbstractDungeon.bossList.size() == 1) {
                AbstractDungeon.bossList.add(
                    AbstractDungeon.bossList.get(0)
                );
            }
        }
    }
}
```

**1막 보스 2마리 시 고려사항:**
- **Slime Boss:** 분열 패턴, 2마리 시 슬라임 폭발
- **Guardian:** 방어 모드, 모드 체인지 타이밍
- **Hexaghost:** 불 구체 관리

---

### 4.4 예제 4: 특정 Ascension 레벨에서만 적용

**Ascension 25+에서 모든 막 보스 2마리:**

```java
public class UniversalDoubleBossPatch {

    @SpirePatch(
        clz = ProceedButton.class,
        method = "update"
    )
    public static class ProceedButtonPatch {
        @SpireInsertPatch(
            locator = Locator.class
        )
        public static SpireReturn<Void> Insert(ProceedButton __instance) {
            if (AbstractDungeon.ascensionLevel < 25) {
                return SpireReturn.Continue();
            }

            AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

            if (currentRoom instanceof MonsterRoomBoss &&
                AbstractDungeon.bossList.size() == 2) {

                // 모든 막에서 작동
                goToDoubleBoss(__instance);
                return SpireReturn.Return(null);
            }

            return SpireReturn.Continue();
        }

        private static void goToDoubleBoss(ProceedButton button) {
            AbstractDungeon.bossKey = AbstractDungeon.bossList.get(0);
            CardCrawlGame.music.fadeOutBGM();
            CardCrawlGame.music.fadeOutTempBGM();
            MapRoomNode node = new MapRoomNode(-1, 15);
            node.room = new MonsterRoomBoss();
            AbstractDungeon.nextRoom = node;
            AbstractDungeon.closeCurrentScreen();
            AbstractDungeon.nextRoomTransitionStart();
            ReflectionHacks.privateMethod(ProceedButton.class, "hide")
                .invoke(button);
        }

        private static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(
                    AbstractDungeon.class, "closeCurrentScreen"
                );
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    // 모든 던전에 적용
    @SpirePatch(
        clz = AbstractDungeon.class,
        method = "initializeBoss",
        paramtypez = {}
    )
    public static class BossListPatch {
        @SpirePostfixPatch
        public static void Postfix(AbstractDungeon __instance) {
            if (AbstractDungeon.ascensionLevel >= 25 &&
                AbstractDungeon.bossList.size() == 1) {

                AbstractDungeon.bossList.add(
                    AbstractDungeon.bossList.get(0)
                );
            }
        }
    }
}
```

**Ascension 30+에서 보스 3마리:**

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class TripleBossAt30Patch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        if (AbstractDungeon.ascensionLevel < 30) {
            return SpireReturn.Continue();
        }

        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.bossList.size() >= 2) {  // 2개 이상이면 계속

            goToNextBoss(__instance);
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }

    // goToNextBoss() 메서드는 동일
}

@SpirePatch(
    clz = AbstractDungeon.class,
    method = "initializeBoss"
)
public class ThreeBossListPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            while (AbstractDungeon.bossList.size() < 3) {
                // 현재 막의 보스 중 랜덤 선택
                String randomBoss = getBossForCurrentAct();
                AbstractDungeon.bossList.add(randomBoss);
            }
        }
    }

    private static String getBossForCurrentAct() {
        if (AbstractDungeon.id.equals("Exordium")) {
            String[] bosses = {"The Guardian", "Hexaghost", "Slime Boss"};
            return bosses[MathUtils.random(bosses.length - 1)];
        } else if (AbstractDungeon.id.equals("TheCity")) {
            String[] bosses = {"Automaton", "Champ", "Collector"};
            return bosses[MathUtils.random(bosses.length - 1)];
        } else if (AbstractDungeon.id.equals("TheBeyond")) {
            String[] bosses = {"Awakened One", "Time Eater", "Donu and Deca"};
            return bosses[MathUtils.random(bosses.length - 1)];
        }
        return "The Guardian";
    }
}
```

---

### 4.5 예제 5: 특정 보스만 제외

**Donu & Deca 제외 (이미 2마리이므로):**

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class DoubleBossExcludeDonuDecaPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.ascensionLevel >= 20 &&
            AbstractDungeon.bossList.size() == 2) {

            // Donu & Deca는 제외
            String nextBoss = AbstractDungeon.bossList.get(0);
            if (nextBoss.equals("Donu and Deca")) {
                // 두 번째 보스를 건너뛰고 일반 클리어
                return SpireReturn.Continue();
            }

            goToDoubleBoss(__instance);
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }

    // goToDoubleBoss() 메서드는 동일
}

// Donu & Deca를 bossList에서 제거
@SpirePatch(
    clz = TheBeyond.class,
    method = "initializeBoss"
)
public class RemoveDonuDecaFromBossListPatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            AbstractDungeon.bossList.removeIf(
                boss -> boss.equals("Donu and Deca")
            );

            // 리스트가 비었으면 다른 보스 추가
            if (AbstractDungeon.bossList.isEmpty()) {
                AbstractDungeon.bossList.add("Awakened One");
                AbstractDungeon.bossList.add("Time Eater");
            }

            // 최소 2개 보장
            if (AbstractDungeon.bossList.size() == 1) {
                AbstractDungeon.bossList.add(
                    AbstractDungeon.bossList.get(0)
                );
            }
        }
    }
}
```

**Heart (4막 보스) 제외:**

```java
@SpirePatch(
    clz = ProceedButton.class,
    method = "update"
)
public class DoubleBossExcludeHeartPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Void> Insert(ProceedButton __instance) {
        // 4막에서는 적용하지 않음
        if (AbstractDungeon.id.equals("TheEnding")) {
            return SpireReturn.Continue();
        }

        AbstractRoom currentRoom = AbstractDungeon.getCurrRoom();

        if (currentRoom instanceof MonsterRoomBoss &&
            AbstractDungeon.ascensionLevel >= 20 &&
            AbstractDungeon.bossList.size() == 2) {

            goToDoubleBoss(__instance);
            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}
```

---

## 5. 주의사항

### 5.1 화면 배치 문제

**문제:** 3마리 이상의 보스를 배치하면 화면을 벗어남

**해결책:**
```java
// 화면 폭 계산
float screenWidth = Settings.WIDTH;
float usableWidth = screenWidth * 0.8f;  // 80%만 사용

// 3마리 균등 배치
float spacing = usableWidth / 3;
float[] positions = {
    -usableWidth/2 + spacing/2,  // 왼쪽
    0.0f,                         // 중앙
    usableWidth/2 - spacing/2     // 오른쪽
};

return new MonsterGroup(
    new TimeEater(positions[0], 30.0F),
    new TimeEater(positions[1], 30.0F),
    new TimeEater(positions[2], 30.0F)
);
```

### 5.2 보스 HP/데미지 밸런스

**권장 조정:**
```java
@SpirePatch(
    clz = AwakenedOne.class,
    method = SpirePatch.CONSTRUCTOR
)
public class AwakenedOneBalancePatch {
    @SpirePostfixPatch
    public static void Postfix(AwakenedOne __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 30) {
            // 보스 3마리 시 HP 70%로 감소
            int currentHP = __instance.currentHealth;
            int newHP = (int)(currentHP * 0.7f);
            __instance.currentHealth = newHP;
            __instance.maxHealth = newHP;
        } else if (AbstractDungeon.ascensionLevel >= 20) {
            // 보스 2마리 시 HP 80%로 감소
            int currentHP = __instance.currentHealth;
            int newHP = (int)(currentHP * 0.8f);
            __instance.currentHealth = newHP;
            __instance.maxHealth = newHP;
        }
    }
}
```

### 5.3 특수 보스 처리

**Donu & Deca (이미 2마리):**
```java
// 이 보스는 제외하거나 4마리로 만들지 말 것
if (bossKey.equals("Donu and Deca") && multiMode) {
    // 일반 전투로 진행
    return SpireReturn.Continue();
}
```

**Shapes (3마리/4마리 일반 몬스터):**
```java
// Shapes 보스전 만들 때 주의
// MonsterHelper.java에서 이미 여러 마리 생성
case "3 Shapes":
    return new MonsterGroup(
        new Sentry(-330.0F, 25.0F),
        new Sentry(-85.0F, 10.0F),
        new Sentry(140.0F, 30.0F)
    );
```

### 5.4 보상 중복 방지

**문제:** 보스 2마리 = 보상 2배?

**해결책:**
```java
@SpirePatch(
    clz = MonsterRoomBoss.class,
    method = "onPlayerEntry"
)
public class SingleRewardPatch {
    private static boolean isSecondBoss = false;

    @SpirePostfixPatch
    public static void Postfix(MonsterRoomBoss __instance) {
        if (AbstractDungeon.ascensionLevel >= 20 &&
            AbstractDungeon.bossList.size() == 1) {  // 두 번째 보스

            isSecondBoss = true;
        }
    }
}

@SpirePatch(
    clz = AbstractRoom.class,
    method = "addGoldToRewards"
)
public class NoGoldOnSecondBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractRoom __instance, int gold) {
        if (SingleRewardPatch.isSecondBoss) {
            // 두 번째 보스는 골드 보상 없음
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 5.5 메모리/성능 고려

**주의사항:**
- 보스 3마리 이상 시 파티클 이펙트 과다
- 슬라임 보스 분열 등으로 인한 몬스터 수 폭발

**최적화:**
```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "usePreBattleAction"
)
public class ReduceEffectsPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.getCurrRoom().monsters.monsters.size() > 3) {
            // 몬스터 4마리 이상 시 이펙트 감소
            Settings.FAST_MODE = true;
        }
    }
}
```

---

## 6. 고급 수정

### 6.1 보스 조합 커스터마이징

**목표:** 다른 막의 보스를 섞어서 배치

```java
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public class CrossActBossPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (AbstractDungeon.ascensionLevel >= 40 &&
            key.equals("CUSTOM_CROSS_ACT_BOSS")) {

            // Slime Boss + Guardian 조합
            return SpireReturn.Return(new MonsterGroup(
                new SlimeBoss(-300.0F, 0.0F),
                new TheGuardian(300.0F, 0.0F)
            ));
        }

        return SpireReturn.Continue();
    }
}

@SpirePatch(
    clz = TheBeyond.class,
    method = "initializeBoss"
)
public class CustomBossListPatch {
    @SpirePostfixPatch
    public static void Postfix(TheBeyond __instance) {
        if (AbstractDungeon.ascensionLevel >= 40) {
            AbstractDungeon.bossList.clear();
            AbstractDungeon.bossList.add("CUSTOM_CROSS_ACT_BOSS");
            AbstractDungeon.bossList.add("Time Eater");
        }
    }
}
```

### 6.2 막 구분 없이 무작위 보스 2마리

```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "initializeBoss"
)
public class RandomAnyBossPatch {
    private static final String[] ALL_BOSSES = {
        "The Guardian", "Hexaghost", "Slime Boss",      // Act 1
        "Automaton", "Champ", "Collector",              // Act 2
        "Awakened One", "Time Eater", "Donu and Deca"  // Act 3
    };

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            AbstractDungeon.bossList.clear();

            // 랜덤 보스 2마리 선택 (중복 가능)
            for (int i = 0; i < 2; i++) {
                String randomBoss = ALL_BOSSES[
                    MathUtils.random(ALL_BOSSES.length - 1)
                ];
                AbstractDungeon.bossList.add(randomBoss);
            }
        }
    }
}

// 보스 encounter 등록 필요
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public class CrossActEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            switch (key) {
                // Act 1 보스를 다른 막에서도 사용 가능하게
                case "The Guardian":
                    return SpireReturn.Return(new MonsterGroup(
                        new TheGuardian(0.0F, 0.0F)
                    ));

                case "Hexaghost":
                    return SpireReturn.Return(new MonsterGroup(
                        new Hexaghost(0.0F, 0.0F)
                    ));

                case "Slime Boss":
                    return SpireReturn.Return(new MonsterGroup(
                        new SlimeBoss(0.0F, 0.0F)
                    ));

                // Act 2 보스
                case "Automaton":
                    return SpireReturn.Return(new MonsterGroup(
                        new BronzeAutomaton()
                    ));

                case "Champ":
                    return SpireReturn.Return(new MonsterGroup(
                        new TheChamp()
                    ));

                case "Collector":
                    return SpireReturn.Return(new MonsterGroup(
                        new TheCollector()
                    ));
            }
        }

        return SpireReturn.Continue();
    }
}
```

### 6.3 보스 HP 공유 시스템

**목표:** 2마리 보스가 HP를 공유하여 한 마리 죽으면 다른 마리도 죽음

```java
public class SharedHPBossPower extends AbstractPower {
    private AbstractMonster linkedBoss;

    public SharedHPBossPower(AbstractCreature owner, AbstractMonster linked) {
        this.owner = owner;
        this.linkedBoss = linked;
        this.ID = "SharedHPBoss";
        this.name = "Shared HP";
        this.type = PowerType.BUFF;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = "HP is shared with another boss.";
    }

    @Override
    public void onDeath() {
        // 한 마리가 죽으면 다른 마리도 죽임
        if (linkedBoss != null && !linkedBoss.isDead) {
            linkedBoss.die();
        }
    }
}

@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public class SharedHPEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (AbstractDungeon.ascensionLevel >= 60 &&
            key.equals("Time Eater")) {

            // 2마리의 Time Eater 생성
            TimeEater boss1 = new TimeEater(-300.0F, 30.0F);
            TimeEater boss2 = new TimeEater(300.0F, 30.0F);

            // HP 공유 파워 적용
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    boss1, boss1,
                    new SharedHPBossPower(boss1, boss2)
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    boss2, boss2,
                    new SharedHPBossPower(boss2, boss1)
                )
            );

            return SpireReturn.Return(new MonsterGroup(boss1, boss2));
        }

        return SpireReturn.Continue();
    }
}
```

### 6.4 보스 페이즈 동기화

**목표:** 2마리 보스의 페이즈를 동기화 (한 마리가 페이즈 전환하면 다른 마리도 전환)

```java
public class SyncPhasePower extends AbstractPower {
    private AbstractMonster[] linkedBosses;

    public SyncPhasePower(AbstractCreature owner, AbstractMonster[] bosses) {
        this.owner = owner;
        this.linkedBosses = bosses;
        this.ID = "SyncPhase";
        this.name = "Synchronized Phase";
        this.type = PowerType.BUFF;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = "Phase changes are synchronized.";
    }

    @Override
    public void atStartOfTurn() {
        // Awakened One의 페이즈 체크
        if (owner instanceof AwakenedOne) {
            boolean ownerInPhase2 = !ReflectionHacks.getPrivate(
                owner, AwakenedOne.class, "form1"
            );

            // 다른 보스들의 페이즈 동기화
            for (AbstractMonster boss : linkedBosses) {
                if (boss != owner && boss instanceof AwakenedOne) {
                    boolean bossInPhase2 = !ReflectionHacks.getPrivate(
                        boss, AwakenedOne.class, "form1"
                    );

                    // 페이즈 불일치 시 동기화
                    if (ownerInPhase2 != bossInPhase2 && ownerInPhase2) {
                        AbstractDungeon.actionManager.addToBottom(
                            new ChangeStateAction(boss, "REBIRTH")
                        );
                    }
                }
            }
        }
    }
}

@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public class SyncPhaseEncounterPatch {
    @SpirePrefixPatch
    public static SpireReturn<MonsterGroup> Prefix(String key) {
        if (AbstractDungeon.ascensionLevel >= 70 &&
            key.equals("Awakened One")) {

            // 2마리의 Awakened One 생성
            AwakenedOne boss1 = new AwakenedOne(-300.0F, 15.0F);
            AwakenedOne boss2 = new AwakenedOne(300.0F, 15.0F);

            AbstractMonster[] bosses = {boss1, boss2};

            // 페이즈 동기화 파워 적용
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    boss1, boss1,
                    new SyncPhasePower(boss1, bosses)
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    boss2, boss2,
                    new SyncPhasePower(boss2, bosses)
                )
            );

            return SpireReturn.Return(new MonsterGroup(boss1, boss2));
        }

        return SpireReturn.Continue();
    }
}
```

---

## 요약

### 핵심 포인트

1. **Ascension 20 시스템은 매우 간단**
   - `ProceedButton.java`의 조건문 하나로 구현
   - `bossList.size() == 2` 체크

2. **bossList 관리가 핵심**
   - 각 막의 `initializeBoss()`에서 보스 리스트 생성
   - 최소 2개 보장 (`bossList.add(bossList.get(0))`)

3. **보스 배치는 MonsterHelper**
   - `getEncounter(String key)`에서 보스 생성
   - 좌표는 수동 설정 필요

4. **주의사항**
   - Donu & Deca는 이미 2마리
   - 화면 배치 한계
   - 밸런스 조정 필수
   - 보상 중복 방지

### 난이도별 권장 설정

- **Ascension 20-24:** 3막 보스 2연전 (원작)
- **Ascension 25-29:** 모든 막 보스 2연전
- **Ascension 30-39:** 보스 3연전
- **Ascension 40-49:** 크로스 막 보스 조합
- **Ascension 50+:** 완전 랜덤 보스 + 특수 효과

---

**작성일:** 2025-01-08
**기반 버전:** Slay the Spire v2.0 (01-23-2019)
**참고 파일:**
- `com/megacrit/cardcrawl/rooms/MonsterRoomBoss.java`
- `com/megacrit/cardcrawl/ui/buttons/ProceedButton.java`
- `com/megacrit/cardcrawl/dungeons/TheBeyond.java`
- `com/megacrit/cardcrawl/helpers/MonsterHelper.java`
- `com/megacrit/cardcrawl/monsters/beyond/AwakenedOne.java`
- `com/megacrit/cardcrawl/monsters/beyond/TimeEater.java`
