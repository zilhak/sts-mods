# 엘리트 전투 (Elite Battle) Encounter 시스템

지도상 "엘리트 적" 심볼로 진입했을 때의 전투 시스템 분석

---

## 📑 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [엘리트 풀 정의](#엘리트-풀-정의)
4. [보상 시스템](#보상-시스템)
5. [특수 메커니즘](#특수-메커니즘)
6. [수정 방법](#수정-방법)
7. [관련 클래스](#관련-클래스)

---

## 시스템 개요

엘리트 전투는 **일반 전투와 동일한 사전 생성 방식**을 사용하지만, **별도의 엘리트 리스트**와 **특별한 보상 시스템**을 갖습니다.

### 핵심 특징

1. **별도 리스트**: `eliteMonsterList` 사용 (일반 전투와 독립)
2. **고정 보상**: 항상 유물 1개 (Black Star 시 2개)
3. **Emerald Key**: Act 4 진입 키 획득 가능
4. **Burning Elite 버프**: 에메랄드 키가 있는 엘리트 방은 추가 버프
5. **중복 방지**: 일반 전투와 동일한 연속 중복 방지 로직

---

## 호출 흐름

### 전체 프로세스

```
던전 시작
    ↓
generateElites(10) 호출
    ↓
eliteMonsterList에 10개 몬스터 사전 생성
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
플레이어가 엘리트 전투 방 진입
    ↓
MonsterRoomElite.onPlayerEntry() 호출 (Line 81)
    ↓
getEliteMonsterForRoomCreation() 호출 (Line 84)
    ↓
eliteMonsterList.get(0) 꺼내기 (Line 2351-2353)
    ↓
MonsterHelper.getEncounter(key) 호출
    ↓
monsters.init() → 전투 시작
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
전투 승리
    ↓
dropReward() 호출 (Line 92)
    ↓
유물 보상 생성 + Emerald Key 체크
```

### 주요 차이점 (vs 일반 전투)

| 구분 | 일반 전투 | 엘리트 전투 |
|------|-----------|-------------|
| 방 클래스 | MonsterRoom | MonsterRoomElite |
| 리스트 | monsterList | eliteMonsterList |
| 호출 메서드 | getMonsterForRoomCreation() | getEliteMonsterForRoomCreation() |
| 보상 | 골드 + 카드 | 유물 (+ Emerald Key) |
| 특수 버프 | 없음 | Burning Elite (조건부) |

---

## 엘리트 풀 정의

### Act 1 (Exordium) 엘리트

**파일**: `Exordium.java:180-187`

```java
protected void generateElites(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Gremlin Nob", 1.0F));
    monsters.add(new MonsterInfo("Lagavulin", 1.0F));
    monsters.add(new MonsterInfo("3 Sentries", 1.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, true);  // elites = true
}
```

| 엘리트 ID | 가중치 | 확률 | 설명 |
|-----------|--------|------|------|
| "Gremlin Nob" | 1.0 | 33.3% | 그렘린 놉 |
| "Lagavulin" | 1.0 | 33.3% | 라가불린 |
| "3 Sentries" | 1.0 | 33.3% | 파수꾼 3마리 |

**특징**: 모두 동일 확률, 10개 생성

### Act 2 (TheCity) 엘리트

**파일**: `TheCity.java:151-158`

```java
protected void generateElites(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Gremlin Leader", 1.0F));
    monsters.add(new MonsterInfo("Slavers", 1.0F));
    monsters.add(new MonsterInfo("Book of Stabbing", 1.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, true);
}
```

| 엘리트 ID | 가중치 | 확률 | 설명 |
|-----------|--------|------|------|
| "Gremlin Leader" | 1.0 | 33.3% | 그렘린 리더 + 부하 2 |
| "Slavers" | 1.0 | 33.3% | 노예상인 3종 조합 |
| "Book of Stabbing" | 1.0 | 33.3% | 찌르기의 책 |

### Act 3 (TheBeyond) 엘리트

**파일**: `TheBeyond.java:140-147`

```java
protected void generateElites(int count) {
    ArrayList<MonsterInfo> monsters = new ArrayList<>();
    monsters.add(new MonsterInfo("Giant Head", 2.0F));
    monsters.add(new MonsterInfo("Nemesis", 2.0F));
    monsters.add(new MonsterInfo("Reptomancer", 2.0F));
    MonsterInfo.normalizeWeights(monsters);
    populateMonsterList(monsters, count, true);
}
```

| 엘리트 ID | 가중치 | 확률 | 설명 |
|-----------|--------|------|------|
| "Giant Head" | 2.0 | 33.3% | 거대한 머리 |
| "Nemesis" | 2.0 | 33.3% | 네메시스 |
| "Reptomancer" | 2.0 | 33.3% | 렙토맨서 + 단검 2 |

### Act 4 (TheEnding) 엘리트

**파일**: `TheEnding.java:208-214`

```java
protected void initializeEliteMonsterList() {
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
    eliteMonsterList.add("Shield and Spear");
}
```

| 엘리트 ID | 확률 | 설명 |
|-----------|------|------|
| "Shield and Spear" | 100% | 방패와 창 (고정) |

**특징**: Act 4는 generateElites()가 비어있고, 대신 `initializeEliteMonsterList()` 사용

---

## 보상 시스템

### dropReward() 메서드

**파일**: `MonsterRoomElite.java:92-105`

```java
public void dropReward() {
    AbstractRelic.RelicTier tier = returnRandomRelicTier();
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("MimicInfestation")) {
        // Endless 모드 + Mimic Infestation: 보상 없음
        AbstractDungeon.player.getBlight("MimicInfestation").flash();
    } else {
        addRelicToRewards(tier);  // 유물 1개
        if (AbstractDungeon.player.hasRelic("Black Star")) {
            // Black Star 유물 소지 시 유물 2개
            addNoncampRelicToRewards(returnRandomRelicTier());
        }
        addEmeraldKey();  // Emerald Key 체크
    }
}
```

### 유물 등급 확률

**파일**: `MonsterRoomElite.java:119-136`

```java
private AbstractRelic.RelicTier returnRandomRelicTier() {
    int roll = AbstractDungeon.relicRng.random(0, 99);  // 0-99 범위

    if (ModHelper.isModEnabled("Elite Swarm")) {
        roll += 10;  // Elite Swarm 모드: +10 보정
    }

    if (roll < 50) {
        return AbstractRelic.RelicTier.COMMON;
    }
    if (roll > 82) {
        return AbstractRelic.RelicTier.RARE;
    }
    return AbstractRelic.RelicTier.UNCOMMON;
}
```

#### 기본 확률

| 등급 | 범위 | 확률 |
|------|------|------|
| COMMON | 0-49 | 50% |
| UNCOMMON | 50-82 | 33% |
| RARE | 83-99 | 17% |

#### Elite Swarm 모드 (+10 보정)

| 등급 | 범위 | 확률 |
|------|------|------|
| COMMON | 0-39 | 40% |
| UNCOMMON | 40-72 | 33% |
| RARE | 73-99 | 27% |

### Emerald Key 획득

**파일**: `MonsterRoomElite.java:107-112`

```java
private void addEmeraldKey() {
    if (Settings.isFinalActAvailable &&        // Act 4 활성화
        !Settings.hasEmeraldKey &&             // 아직 키 미획득
        !this.rewards.isEmpty() &&             // 보상 존재
        (AbstractDungeon.getCurrMapNode()).hasEmeraldKey) {  // 현재 방이 키 방
        this.rewards.add(new RewardItem(
            this.rewards.get(this.rewards.size() - 1),
            RewardItem.RewardType.EMERALD_KEY
        ));
    }
}
```

**획득 조건**:
1. Act 4가 활성화되어 있음
2. 아직 Emerald Key를 획득하지 않음
3. 현재 엘리트 방이 "burning elite" (에메랄드 키 표시)

**효과**: 보상 목록 마지막에 Emerald Key 추가

---

## 특수 메커니즘

### Burning Elite 버프

에메랄드 키가 있는 엘리트 방에 진입하면 **4가지 버프 중 1개 랜덤 적용**

**파일**: `MonsterRoomElite.java:35-71`

```java
public void applyEmeraldEliteBuff() {
    if (Settings.isFinalActAvailable &&
        (AbstractDungeon.getCurrMapNode()).hasEmeraldKey) {
        switch (AbstractDungeon.mapRng.random(0, 3)) {
            case 0:
                // 버프 1: 힘 증가
                for (AbstractMonster m : this.monsters.monsters) {
                    addToBottom(new ApplyPowerAction(m, m,
                        new StrengthPower(m, AbstractDungeon.actNum + 1),
                        AbstractDungeon.actNum + 1));
                }
                break;

            case 1:
                // 버프 2: 최대 HP 25% 증가
                for (AbstractMonster m : this.monsters.monsters) {
                    addToBottom(new IncreaseMaxHpAction(m, 0.25F, true));
                }
                break;

            case 2:
                // 버프 3: Metallicize (블록 부여)
                for (AbstractMonster m : this.monsters.monsters) {
                    addToBottom(new ApplyPowerAction(m, m,
                        new MetallicizePower(m, AbstractDungeon.actNum * 2 + 2),
                        AbstractDungeon.actNum * 2 + 2));
                }
                break;

            case 3:
                // 버프 4: Regeneration (회복)
                for (AbstractMonster m : this.monsters.monsters) {
                    addToBottom(new ApplyPowerAction(m, m,
                        new RegenerateMonsterPower(m, 1 + AbstractDungeon.actNum * 2),
                        1 + AbstractDungeon.actNum * 2));
                }
                break;
        }
    }
}
```

### Burning Elite 버프 상세

| 버프 ID | 확률 | 효과 | Act 1 | Act 2 | Act 3 |
|---------|------|------|-------|-------|-------|
| 0 | 25% | Strength | +2 | +3 | +4 |
| 1 | 25% | 최대 HP 증가 | +25% | +25% | +25% |
| 2 | 25% | Metallicize | 4 블록/턴 | 6 블록/턴 | 8 블록/턴 |
| 3 | 25% | Regeneration | 3 회복/턴 | 5 회복/턴 | 7 회복/턴 |

**호출 시점**: `monsters.init()` 직후 (MonsterGroup 초기화 후)

---

## 수정 방법

### 1. 엘리트 풀 확률 변경

**목표**: Lagavulin 확률을 50%로 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateElites",
    paramtypez = { int.class }
)
public static class LagavulinBoostPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Exordium __instance, int count) {
        ArrayList<MonsterInfo> monsters = new ArrayList<>();
        monsters.add(new MonsterInfo("Gremlin Nob", 1.0F));
        monsters.add(new MonsterInfo("Lagavulin", 3.0F));  // 3배 증가!
        monsters.add(new MonsterInfo("3 Sentries", 1.0F));

        MonsterInfo.normalizeWeights(monsters);
        // populateMonsterList 호출 (Reflection 필요)

        return SpireReturn.Return(null);
    }
}
```

**결과**: Lagavulin 60%, Nob 20%, Sentries 20%

### 2. 유물 등급 확률 변경

**목표**: 레어 유물 확률을 30%로 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "returnRandomRelicTier"
)
public static class RareRelicBoostPatch {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic.RelicTier> Prefix(MonsterRoomElite __instance) {
        int roll = AbstractDungeon.relicRng.random(0, 99);

        if (roll < 50) {
            return SpireReturn.Return(AbstractRelic.RelicTier.COMMON);  // 50%
        }
        if (roll >= 70) {  // 기존 82에서 70으로 변경
            return SpireReturn.Return(AbstractRelic.RelicTier.RARE);    // 30%
        }
        return SpireReturn.Return(AbstractRelic.RelicTier.UNCOMMON);    // 20%
    }
}
```

### 3. Burning Elite 버프 강화

**목표**: 모든 버프 동시 적용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "applyEmeraldEliteBuff"
)
public static class SuperBurningElitePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(MonsterRoomElite __instance) {
        if (Settings.isFinalActAvailable &&
            (AbstractDungeon.getCurrMapNode()).hasEmeraldKey) {

            int actNum = AbstractDungeon.actNum;

            for (AbstractMonster m : __instance.monsters.monsters) {
                // 모든 버프 동시 적용
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(m, m,
                        new StrengthPower(m, actNum + 1), actNum + 1));
                AbstractDungeon.actionManager.addToBottom(
                    new IncreaseMaxHpAction(m, 0.25F, true));
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(m, m,
                        new MetallicizePower(m, actNum * 2 + 2), actNum * 2 + 2));
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(m, m,
                        new RegenerateMonsterPower(m, 1 + actNum * 2), 1 + actNum * 2));
            }

            return SpireReturn.Return(null);
        }

        return SpireReturn.Continue();
    }
}
```

### 4. Black Star 보상 추가

**목표**: Black Star 없이도 유물 2개 드롭

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.rooms.MonsterRoomElite",
    method = "dropReward"
)
public static class DoubleRelicPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        if (AbstractDungeon.ascensionLevel >= 50) {
            // A50+에서는 항상 유물 2개
            AbstractRelic.RelicTier tier = __instance.returnRandomRelicTier();
            __instance.addNoncampRelicToRewards(tier);
        }
    }
}
```

### 5. 특정 엘리트 강제 등장

**목표**: 첫 엘리트를 항상 Lagavulin으로 고정

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.dungeons.Exordium",
    method = "generateElites",
    paramtypez = { int.class }
)
public static class ForceFirstLagavulinPatch {
    @SpirePostfixPatch
    public static void Postfix(Exordium __instance, int count) {
        // 첫 번째 엘리트를 Lagavulin으로 강제 설정
        if (!__instance.eliteMonsterList.isEmpty()) {
            __instance.eliteMonsterList.set(0, "Lagavulin");
        }
    }
}
```

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 역할 |
|--------|------|------|
| **MonsterRoomElite** | `com.megacrit.cardcrawl.rooms.MonsterRoomElite` | 엘리트 전투 방 |
| **AbstractDungeon** | `com.megacrit.cardcrawl.dungeons.AbstractDungeon` | 엘리트 리스트 관리 |
| **Exordium** | `com.megacrit.cardcrawl.dungeons.Exordium` | 1막 엘리트 풀 |
| **TheCity** | `com.megacrit.cardcrawl.dungeons.TheCity` | 2막 엘리트 풀 |
| **TheBeyond** | `com.megacrit.cardcrawl.dungeons.TheBeyond` | 3막 엘리트 풀 |
| **TheEnding** | `com.megacrit.cardcrawl.dungeons.TheEnding` | 4막 엘리트 풀 |

### 주요 메서드

#### MonsterRoomElite

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `onPlayerEntry()` | Line 81-89 | 방 진입 시 엘리트 할당 |
| `dropReward()` | Line 92-105 | 보상 생성 |
| `returnRandomRelicTier()` | Line 119-136 | 유물 등급 결정 |
| `addEmeraldKey()` | Line 107-112 | Emerald Key 보상 추가 |
| `applyEmeraldEliteBuff()` | Line 35-71 | Burning Elite 버프 |

#### AbstractDungeon

| 메서드 | 코드 위치 | 설명 |
|--------|----------|------|
| `getEliteMonsterForRoomCreation()` | Line 2347-2354 | 엘리트 몬스터 가져오기 |
| `populateMonsterList()` | Line 1324-1355 | 엘리트 리스트 채우기 (elites=true) |

### 관련 Powers

| Power | 효과 | Burning Elite 사용 |
|-------|------|---------------------|
| **StrengthPower** | 공격력 증가 | 버프 0 |
| **MetallicizePower** | 턴 시작 시 블록 획득 | 버프 2 |
| **RegenerateMonsterPower** | 턴 종료 시 HP 회복 | 버프 3 |

---

## 참고사항

### Elite Swarm 모드

**ModHelper.isModEnabled("Elite Swarm")** 활성화 시:
- 유물 등급 roll +10 (레어 확률 증가)
- 카드 보상 등급 강제 RARE

### Black Star 유물

**효과**: 엘리트 전투 승리 시 유물 1개 추가 (총 2개)

**코드 위치**: `MonsterRoomElite.java:99-101`

### Mimic Infestation Blight

**효과**: Endless 모드에서 엘리트 보상 완전 차단

**코드 위치**: `MonsterRoomElite.java:94-97`

### Emerald Key

**출현 조건**:
- Act 4가 언락되어 있음
- 아직 획득하지 않음
- 맵에서 "burning" 표시가 있는 엘리트 방

**위치**: 각 Act당 1개의 burning elite 방 생성

---

## 작성 정보

- **작성일**: 2025-11-08
- **대상 버전**: Slay the Spire 01-23-2019 빌드
- **분석 범위**: Act 1-4 엘리트 전투 시스템 전체
