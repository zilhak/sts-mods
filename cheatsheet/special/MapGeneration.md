# Map Generation System - Complete Technical Reference

> **Slay the Spire 지도 생성 시스템 완전 분석**
> 모든 층 배치, 확률 계산, 경로 알고리즘 및 수정 방법 제공

---

## 📋 목차

1. [지도 생성 전체 개요](#1-지도-생성-전체-개요)
2. [방 타입 (Room Types) 전체 목록](#2-방-타입-room-types-전체-목록)
3. [지도 생성 단계별 분석](#3-지도-생성-단계별-분석)
4. [방 타입 배정 확률 시스템](#4-방-타입-배정-확률-시스템)
5. [경로 생성 알고리즘 상세](#5-경로-생성-알고리즘-상세)
6. [실전 수정 방법](#6-실전-수정-방법)
7. [고급 수정](#7-고급-수정)
8. [주의사항](#8-주의사항)

---

## 1. 지도 생성 전체 개요

### 1.1 지도 생성 프로세스

```java
// AbstractDungeon.java - generateMap()
protected static void generateMap() {
    int mapHeight = 15;        // 세로 15층 (0~14)
    int mapWidth = 7;          // 가로 7칸 (0~6)
    int mapPathDensity = 6;    // 6개의 경로

    // 1단계: 빈 노드 그리드 생성
    map = MapGenerator.generateDungeon(mapHeight, mapWidth, mapPathDensity, mapRng);

    // 2단계: 방 타입 개수 계산
    generateRoomTypes(roomList, count);

    // 3단계: 특수 층 고정 배치
    RoomTypeAssigner.assignRowAsRoomType(map.get(14), RestRoom.class);  // 14층: 휴식처
    RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class); // 0층: 전투
    RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class); // 8층: 보물

    // 4단계: 나머지 방 배치
    map = RoomTypeAssigner.distributeRoomsAcrossMap(mapRng, map, roomList);
}
```

**핵심 구조:**
- **7x15 그리드**: 가로 7칸, 세로 15층 (총 105개 노드)
- **6개 경로**: 하단에서 상단으로 연결되는 6개의 주요 경로
- **RNG 시드**: `mapRng` 사용 (Settings.seed + actNum 기반)
- **도달 가능 노드만 사용**: hasEdges() == true인 노드만 방 배정

### 1.2 주요 클래스

| 클래스 | 역할 |
|--------|------|
| `MapGenerator` | 노드 그리드 생성 & 경로 연결 |
| `RoomTypeAssigner` | 방 타입 배정 규칙 적용 |
| `MapRoomNode` | 개별 노드 (위치, 방 타입, 부모/자식 관계) |
| `MapEdge` | 노드 간 연결선 |
| `AbstractDungeon` | 전체 지도 생성 조율 |

---

## 2. 방 타입 (Room Types) 전체 목록

### 2.1 기본 방 타입

| 타입 | 심볼 | 클래스 | 설명 | 색상 |
|------|------|--------|------|------|
| **MONSTER** | `M` | `MonsterRoom` | 일반 전투 | 회색 |
| **ELITE** | `E` | `MonsterRoomElite` | 엘리트 전투 | 노란색 |
| **REST** | `R` | `RestRoom` | 휴식처 (회복/업그레이드) | 초록색 |
| **SHOP** | `$` | `ShopRoom` | 상점 | 파란색 |
| **TREASURE** | `T` | `TreasureRoom` | 보물 (유물) | 황금색 |
| **EVENT** | `?` | `EventRoom` | 이벤트 | 주황색 |
| **BOSS** | `B` | `MonsterRoomBoss` | 보스 (15층 고정) | 빨간색 |

### 2.2 특수 방 타입

```java
// 보스 보물방 (보스 처치 후)
TreasureRoomBoss extends TreasureRoom
  - 보스 유물 선택

// 비어있는 방 (시작 지점)
EmptyRoom extends AbstractRoom
  - Neow 방 이전 상태

// 승리 방 (최종 승리 시)
VictoryRoom extends AbstractRoom
  - 엔딩 크레딧
```

### 2.3 방 타입별 이미지 & 아이콘

```java
// 각 Room 클래스는 자동으로 이미지 로드
// ImageMaster에서 관리:
- "images/ui/map/monster.png"       // M
- "images/ui/map/elite.png"         // E
- "images/ui/map/rest.png"          // R
- "images/ui/map/shop.png"          // $
- "images/ui/map/chest.png"         // T
- "images/ui/map/event.png"         // ?
- "images/ui/map/boss/{boss}.png"   // B
```

---

## 3. 지도 생성 단계별 분석

### 3.1 1단계: 노드 그리드 생성

```java
// MapGenerator.java - createNodes()
private static ArrayList<ArrayList<MapRoomNode>> createNodes(int height, int width) {
    ArrayList<ArrayList<MapRoomNode>> nodes = new ArrayList<>();
    for (int y = 0; y < height; y++) {
        ArrayList<MapRoomNode> row = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            row.add(new MapRoomNode(x, y));  // 빈 노드 생성
        }
        nodes.add(row);
    }
    return nodes;  // 7x15 = 105개 노드
}
```

**MapRoomNode 구조:**
```java
public class MapRoomNode {
    public int x, y;                          // 그리드 위치
    public float offsetX, offsetY;            // 렌더링 지터 (랜덤)
    public AbstractRoom room;                 // 배정된 방 타입
    private ArrayList<MapEdge> edges;         // 자식 노드로의 연결
    private ArrayList<MapRoomNode> parents;   // 부모 노드 목록
    public boolean taken = false;             // 플레이어 방문 여부
}
```

### 3.2 2단계: 경로 생성

```java
// MapGenerator.java - createPaths()
private static ArrayList<ArrayList<MapRoomNode>> createPaths(
    ArrayList<ArrayList<MapRoomNode>> nodes,
    int pathDensity,  // 6
    Random rng
) {
    int first_row = 0;
    int row_size = nodes.get(first_row).size() - 1;  // 6 (0~6)

    // 6개의 시작점에서 경로 생성
    for (int i = 0; i < pathDensity; i++) {
        int startingNode = randRange(rng, 0, row_size);

        // 첫 번째와 두 번째 경로는 서로 다른 시작점
        if (i == 0) firstStartingNode = startingNode;
        while (startingNode == firstStartingNode && i == 1) {
            startingNode = randRange(rng, 0, row_size);
        }

        // 재귀적 경로 생성
        _createPaths(nodes, new MapEdge(startingNode, -1, startingNode, 0), rng);
    }
    return nodes;
}
```

**경로 생성 규칙:**
- 각 층에서 다음 층으로 연결 시 `-1, 0, +1` 중 랜덤 선택
- 가장자리 노드는 안쪽으로만 연결 (벽 충돌 방지)
- 경로 병합 방지: 최근 3~5층 이내 공통 조상이 있으면 방향 조정
- 이웃 노드의 Edge와 교차하지 않도록 조정

### 3.3 3단계: Edge 생성 & 중복 제거

```java
// MapGenerator.java - _createPaths() 핵심 로직
private static ArrayList<ArrayList<MapRoomNode>> _createPaths(
    ArrayList<ArrayList<MapRoomNode>> nodes,
    MapEdge edge,
    Random rng
) {
    MapRoomNode currentNode = getNode(edge.dstX, edge.dstY, nodes);

    // 최상층 도달 시 종료
    if (edge.dstY + 1 >= nodes.size()) {
        currentNode.addEdge(new MapEdge(..., true));  // isBossEdge = true
        return nodes;
    }

    // 다음 노드 X 좌표 결정 (좌우 ±1 or 직진)
    int newEdgeX = edge.dstX + randRange(rng, min, max);
    int newEdgeY = edge.dstY + 1;

    // 경로 병합 방지 (3~5층 이내 공통 조상 체크)
    MapRoomNode targetNode = getNode(newEdgeX, newEdgeY, nodes);
    ArrayList<MapRoomNode> parents = targetNode.getParents();
    for (MapRoomNode parent : parents) {
        if (parent != currentNode) {
            MapRoomNode ancestor = getCommonAncestor(parent, currentNode, 5);
            if (ancestor != null && (newEdgeY - ancestor.y) < 3) {
                // 방향 조정 (경로 분리)
                newEdgeX = adjustDirection(...);
            }
        }
    }

    // 이웃 노드의 Edge와 교차 방지
    if (edge.dstX > 0) {
        MapRoomNode left_node = nodes.get(edge.dstY).get(edge.dstX - 1);
        MapEdge right_edge = getMaxEdge(left_node.getEdges());
        if (right_edge.dstX > newEdgeX) newEdgeX = right_edge.dstX;
    }

    // Edge 생성 & 재귀 호출
    MapEdge newEdge = new MapEdge(edge.dstX, edge.dstY, ..., newEdgeX, newEdgeY, ...);
    currentNode.addEdge(newEdge);
    targetNode.addParent(currentNode);

    return _createPaths(nodes, newEdge, rng);  // 재귀
}
```

**중복 Edge 제거:**
```java
// MapGenerator.java - filterRedundantEdgesFromRow()
// 0층(시작)에서 같은 목적지로 가는 중복 Edge 제거
for (MapRoomNode node : map.get(0)) {
    for (MapEdge edge : node.getEdges()) {
        for (MapEdge prevEdge : existingEdges) {
            if (edge.dstX == prevEdge.dstX && edge.dstY == prevEdge.dstY) {
                deleteList.add(edge);  // 중복 제거
            }
        }
    }
}
```

### 3.4 4단계: 방 타입 개수 계산

```java
// AbstractDungeon.java - generateRoomTypes()
private static void generateRoomTypes(ArrayList<AbstractRoom> roomList, int availableRoomCount) {
    // 확률 기반 개수 계산
    int shopCount = Math.round(availableRoomCount * shopRoomChance);         // 5%
    int restCount = Math.round(availableRoomCount * restRoomChance);         // 12%
    int treasureCount = Math.round(availableRoomCount * treasureRoomChance); // 0%
    int eventCount = Math.round(availableRoomCount * eventRoomChance);       // 22%

    // 엘리트 개수 (Ascension 보정)
    if (ModHelper.isModEnabled("Elite Swarm")) {
        eliteCount = Math.round(availableRoomCount * eliteRoomChance * 2.5F);
    } else if (ascensionLevel >= 1) {
        eliteCount = Math.round(availableRoomCount * eliteRoomChance * 1.6F);  // +60%
    } else {
        eliteCount = Math.round(availableRoomCount * eliteRoomChance);         // 8%
    }

    // 몬스터 방 = 나머지 전부
    int monsterCount = availableRoomCount - shopCount - restCount - treasureCount - eliteCount - eventCount;

    // roomList에 추가
    for (int i = 0; i < shopCount; i++) roomList.add(new ShopRoom());
    for (int i = 0; i < restCount; i++) roomList.add(new RestRoom());
    for (int i = 0; i < eliteCount; i++) roomList.add(new MonsterRoomElite());
    for (int i = 0; i < eventCount; i++) roomList.add(new EventRoom());
    // MonsterRoom은 나중에 자동 채움
}
```

**Act별 확률 (모든 Act 동일):**
```java
// Exordium.java, TheCity.java, TheBeyond.java, TheEnding.java
protected void initializeLevelSpecificChances() {
    shopRoomChance = 0.05F;      // 5%
    restRoomChance = 0.12F;      // 12%
    treasureRoomChance = 0.0F;   // 0% (8층 고정)
    eventRoomChance = 0.22F;     // 22%
    eliteRoomChance = 0.08F;     // 8% (Asc1+: 12.8%)
    // monsterRoomChance = 나머지 (53%)
}
```

### 3.5 5단계: 방 타입 배정

```java
// RoomTypeAssigner.java - distributeRoomsAcrossMap()
public static ArrayList<ArrayList<MapRoomNode>> distributeRoomsAcrossMap(
    Random rng,
    ArrayList<ArrayList<MapRoomNode>> map,
    ArrayList<AbstractRoom> roomList
) {
    // 1. 부족한 몬스터 방 추가
    int nodeCount = getConnectedNonAssignedNodeCount(map);
    while (roomList.size() < nodeCount) {
        roomList.add(new MonsterRoom());
    }

    // 2. 셔플
    Collections.shuffle(roomList, rng.random);

    // 3. 규칙 기반 배정
    assignRoomsToNodes(map, roomList);

    // 4. 미배정 노드에 MonsterRoom 강제 할당
    lastMinuteNodeChecker(map, null);

    return map;
}
```

**배정 규칙 (assignRoomsToNodes):**

```java
// RoomTypeAssigner.java - getNextRoomTypeAccordingToRules()
private static AbstractRoom getNextRoomTypeAccordingToRules(...) {
    ArrayList<MapRoomNode> parents = n.getParents();
    ArrayList<MapRoomNode> siblings = getSiblings(map, parents, n);

    for (AbstractRoom roomToBeSet : roomList) {
        // 규칙 1: 층 제한
        if (!ruleAssignableToRow(n, roomToBeSet)) continue;

        // 규칙 2: 부모/형제 중복 방지
        if (ruleParentMatches(parents, roomToBeSet)) continue;
        if (ruleSiblingMatches(siblings, roomToBeSet)) continue;

        return roomToBeSet;  // 통과 시 배정
    }
    return null;  // 실패 시 null (나중에 MonsterRoom)
}
```

**세부 규칙:**

1. **층 제한 규칙 (ruleAssignableToRow):**
   ```java
   if (n.y <= 4) {
       // 0~4층: REST, ELITE 배치 불가
       if (RestRoom.class 또는 MonsterRoomElite.class) return false;
   }
   if (n.y >= 13) {
       // 13~14층: REST 배치 불가 (14층은 이미 고정)
       if (RestRoom.class) return false;
   }
   ```

2. **부모 중복 방지 (ruleParentMatches):**
   ```java
   // REST, TREASURE, SHOP, ELITE는 부모와 같은 타입 금지
   for (MapRoomNode parent : parents) {
       if (parent.getRoom().getClass() == roomToBeSet.getClass()) {
           return true;  // 중복 → 이 방 건너뜀
       }
   }
   ```

3. **형제 중복 방지 (ruleSiblingMatches):**
   ```java
   // REST, MONSTER, EVENT, ELITE, SHOP는 형제 노드와 같은 타입 금지
   for (MapRoomNode sibling : siblings) {
       if (sibling.getRoom() != null &&
           sibling.getRoom().getClass() == roomToBeSet.getClass()) {
           return true;  // 중복 → 이 방 건너뜀
       }
   }
   ```

### 3.6 6단계: 고정 방 배치

```java
// AbstractDungeon.java - generateMap()
// 특정 층 전체를 특정 타입으로 강제 설정

RoomTypeAssigner.assignRowAsRoomType(map.get(14), RestRoom.class);
// 14층 전체 = 휴식처 (보스 이전)

RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);
// 0층 전체 = 전투

// 8층 = 보물 or 엘리트 (Mimic Infestation blight)
if (Settings.isEndless && player.hasBlight("MimicInfestation")) {
    RoomTypeAssigner.assignRowAsRoomType(map.get(8), MonsterRoomElite.class);
} else {
    RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class);
}
```

### 3.7 7단계: 에메랄드 열쇠 배치

```java
// AbstractDungeon.java - setEmeraldElite()
protected static void setEmeraldElite() {
    if (Settings.isFinalActAvailable && !Settings.hasEmeraldKey) {
        // 모든 엘리트 방 수집
        ArrayList<MapRoomNode> eliteNodes = new ArrayList<>();
        for (ArrayList<MapRoomNode> row : map) {
            for (MapRoomNode node : row) {
                if (node.room instanceof MonsterRoomElite) {
                    eliteNodes.add(node);
                }
            }
        }

        // 랜덤 선택 후 플래그 설정
        MapRoomNode chosenNode = eliteNodes.get(mapRng.random(0, eliteNodes.size() - 1));
        chosenNode.hasEmeraldKey = true;

        // 이 엘리트를 처치하면 에메랄드 열쇠 획득
    }
}
```

---

## 4. 방 타입 배정 확률 시스템

### 4.1 층별 등장 가능 방 타입

| 층 범위 | MONSTER | ELITE | REST | SHOP | TREASURE | EVENT |
|---------|---------|-------|------|------|----------|-------|
| 0층 | ✅ 고정 | ❌ | ❌ | ❌ | ❌ | ❌ |
| 1~4층 | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ |
| 5~12층 | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| 8층 | ❌ | ❌ | ❌ | ❌ | ✅ 고정 | ❌ |
| 13층 | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ |
| 14층 | ❌ | ❌ | ✅ 고정 | ❌ | ❌ | ❌ |
| 15층 | ❌ | ❌ | ❌ | ❌ | ❌ | BOSS 고정 |

### 4.2 확률 계산 상세

**기본 확률 (모든 Act 동일):**
```java
shopRoomChance    = 0.05F;  // 5%
restRoomChance    = 0.12F;  // 12%
treasureRoomChance = 0.0F;  // 0% (8층 고정)
eventRoomChance   = 0.22F;  // 22%
eliteRoomChance   = 0.08F;  // 8%
monsterRoomChance = 0.53F;  // 53% (계산값)
```

**Ascension 보정:**
```java
if (ascensionLevel >= 1) {
    eliteCount = Math.round(availableRoomCount * 0.08F * 1.6F);
    // 8% → 12.8% (+60%)
}
```

**예시 계산 (60개 도달 가능 노드):**
```
SHOP: 60 * 0.05 = 3개
REST: 60 * 0.12 = 7개 (14층 포함 시 8개)
ELITE: 60 * 0.08 = 5개 (Asc1+: 8개)
EVENT: 60 * 0.22 = 13개
MONSTER: 60 - 3 - 7 - 5 - 13 = 32개
```

### 4.3 EventHelper 시스템 (이벤트 방 내부)

**이벤트 방에 진입 시 추가 롤:**
```java
// EventHelper.java - roll()
public static RoomResult roll(Random eventRng) {
    float roll = eventRng.random();  // 0.0 ~ 1.0

    // 확률 배열 (100칸)
    RoomResult[] possibleResults = new RoomResult[100];
    Arrays.fill(possibleResults, RoomResult.EVENT);  // 기본 = 이벤트

    // ELITE 확률 채우기 (DeadlyEvents 모드 or 6층 이상)
    int eliteSize = (int)(ELITE_CHANCE * 100.0F);
    Arrays.fill(possibleResults, 0, eliteSize, RoomResult.ELITE);

    // MONSTER 확률
    int monsterSize = (int)(MONSTER_CHANCE * 100.0F);
    Arrays.fill(possibleResults, eliteSize, eliteSize + monsterSize, RoomResult.MONSTER);

    // SHOP 확률
    int shopSize = (int)(SHOP_CHANCE * 100.0F);
    Arrays.fill(possibleResults, ..., RoomResult.SHOP);

    // TREASURE 확률
    int treasureSize = (int)(TREASURE_CHANCE * 100.0F);
    Arrays.fill(possibleResults, ..., RoomResult.TREASURE);

    // 나머지 = EVENT

    RoomResult choice = possibleResults[(int)(roll * 100.0F)];

    // 확률 갱신 (램프 시스템)
    if (choice == RoomResult.ELITE) {
        ELITE_CHANCE = 0.0F;
    } else {
        ELITE_CHANCE += 0.1F;  // 10%씩 증가
    }
    // 다른 타입도 동일

    return choice;
}
```

**EventHelper 확률 (초기값):**
```java
BASE_ELITE_CHANCE = 0.1F;      // 10%
BASE_MONSTER_CHANCE = 0.1F;    // 10%
BASE_SHOP_CHANCE = 0.03F;      // 3%
BASE_TREASURE_CHANCE = 0.02F;  // 2%
// EVENT = 75%

RAMP_ELITE_CHANCE = 0.1F;      // 매번 +10%
RAMP_MONSTER_CHANCE = 0.1F;    // 매번 +10%
RAMP_SHOP_CHANCE = 0.03F;      // 매번 +3%
RAMP_TREASURE_CHANCE = 0.02F;  // 매번 +2%
```

**램프 시스템:**
- 이벤트 방 진입 → EventHelper.roll() 호출
- 결과가 ELITE면 ELITE_CHANCE = 0, 아니면 +10%
- 다음 이벤트 방에서 확률 증가
- 최대 100%까지 증가 (보장)

---

## 5. 경로 생성 알고리즘 상세

### 5.1 경로 분기 알고리즘

```java
// MapGenerator.java - _createPaths() 분기 결정
int min, max;

if (edge.dstX == 0) {          // 왼쪽 끝
    min = 0;  max = 1;         // 직진 or 우측
} else if (edge.dstX == row_end_node) {  // 오른쪽 끝
    min = -1; max = 0;         // 좌측 or 직진
} else {
    min = -1; max = 1;         // 좌/직진/우측
}

int newEdgeX = edge.dstX + randRange(rng, min, max);
```

**분기 확률 (중앙 노드 기준):**
- 좌측: 33.3%
- 직진: 33.3%
- 우측: 33.3%

### 5.2 경로 병합 방지 알고리즘

```java
// MapGenerator.java - getCommonAncestor()
private static MapRoomNode getCommonAncestor(
    MapRoomNode node1,
    MapRoomNode node2,
    int max_depth  // 5층
) {
    MapRoomNode l_node = node1;
    MapRoomNode r_node = node2;

    int current_y = node1.y;
    while (current_y >= node1.y - max_depth) {
        // 각각의 가장 가까운 부모로 이동
        l_node = getNodeWithMaxX(l_node.getParents());
        r_node = getNodeWithMinX(r_node.getParents());

        if (l_node == r_node) {
            return l_node;  // 공통 조상 발견
        }
        current_y--;
    }
    return null;  // 5층 이내에 없음
}
```

**병합 방지 로직:**
```java
MapRoomNode targetNode = getNode(newEdgeX, newEdgeY, nodes);
ArrayList<MapRoomNode> parents = targetNode.getParents();

for (MapRoomNode parent : parents) {
    if (parent != currentNode) {
        MapRoomNode ancestor = getCommonAncestor(parent, currentNode, 5);
        if (ancestor != null) {
            int gap = newEdgeY - ancestor.y;
            if (gap < 3) {
                // 3층 이내 공통 조상 → 방향 조정
                if (targetNode.x > currentNode.x) {
                    newEdgeX = edge.dstX + randRange(rng, -1, 0);  // 좌측
                } else if (targetNode.x == currentNode.x) {
                    newEdgeX = edge.dstX + randRange(rng, -1, 1);  // 좌우
                } else {
                    newEdgeX = edge.dstX + randRange(rng, 0, 1);   // 우측
                }
            }
        }
    }
}
```

### 5.3 경로 교차 방지 알고리즘

```java
// 왼쪽 이웃 노드 체크
if (edge.dstX > 0) {
    MapRoomNode left_node = nodes.get(edge.dstY).get(edge.dstX - 1);
    if (left_node.hasEdges()) {
        MapEdge right_edge = getMaxEdge(left_node.getEdges());
        if (right_edge.dstX > newEdgeX) {
            newEdgeX = right_edge.dstX;  // 교차 방지
        }
    }
}

// 오른쪽 이웃 노드 체크
if (edge.dstX < row_end_node) {
    MapRoomNode right_node = nodes.get(edge.dstY).get(edge.dstX + 1);
    if (right_node.hasEdges()) {
        MapEdge left_edge = getMinEdge(right_node.getEdges());
        if (left_edge.dstX < newEdgeX) {
            newEdgeX = left_edge.dstX;  // 교차 방지
        }
    }
}
```

**EdgeComparator:**
```java
public class EdgeComparator implements Comparator<MapEdge> {
    public int compare(MapEdge o1, MapEdge o2) {
        return Integer.compare(o1.dstX, o2.dstX);  // X 좌표 정렬
    }
}
```

---

## 6. 실전 수정 방법

### 6.1 예제 1: 새로운 방 타입 추가 (MINI_BOSS)

**목표:** 엘리트보다 강하지만 보스보다 약한 중간 보스 방 추가

**1단계: Room 클래스 생성**
```java
package mymod.rooms;

import com.megacrit.cardcrawl.rooms.MonsterRoomElite;

public class MiniBossRoom extends MonsterRoomElite {
    public MiniBossRoom() {
        this.mapSymbol = "MB";
        this.mapImg = ImageMaster.loadImage("mymod/images/map/miniboss.png");
        this.mapImgOutline = ImageMaster.loadImage("mymod/images/map/miniboss_outline.png");
    }

    @Override
    public void onPlayerEntry() {
        // MiniBoss 몬스터 생성
        AbstractDungeon.overlayMenu.proceedButton.hide();
        this.monsters = MonsterHelper.getEncounter("MiniBoss_" + AbstractDungeon.actNum);
        this.monsters.init();
        AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_MINIBOSS");
    }
}
```

**2단계: AbstractDungeon 패치 (방 개수 추가)**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateRoomTypes")
public static class AddMiniBossRoomPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert(ArrayList<AbstractRoom> roomList, int availableRoomCount) {
        // 엘리트 8% 중 2%를 미니보스로 전환
        float miniBossChance = 0.02F;
        int miniBossCount = Math.round(availableRoomCount * miniBossChance);

        for (int i = 0; i < miniBossCount; i++) {
            roomList.add(new MiniBossRoom());
        }
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                Math.class, "round"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

**3단계: RoomTypeAssigner 규칙 추가**
```java
@SpirePatch(clz = RoomTypeAssigner.class, method = "ruleAssignableToRow")
public static class MiniBossRowRulePatch {
    @SpireInsertPatch(locator = Locator.class)
    public static SpireReturn<Boolean> Insert(MapRoomNode n, AbstractRoom roomToBeSet) {
        if (roomToBeSet instanceof MiniBossRoom) {
            // 미니보스는 6~12층에만 배치
            if (n.y < 6 || n.y > 12) {
                return SpireReturn.Return(false);
            }
        }
        return SpireReturn.Continue();
    }
}
```

**4단계: 이미지 추가**
```
mymod/images/map/miniboss.png         (128x128)
mymod/images/map/miniboss_outline.png (128x128)
```

---

### 6.2 예제 2: 지도 구조 변경 (7x15 → 10x20)

**1단계: 상수 패치**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class ExpandMapSizePatch {
    @SpireInsertPatch(rloc = 0)
    public static void Insert() {
        // 기본값 변경
    }
}

@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class CustomMapGenerationPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        int mapHeight = 20;        // 15 → 20
        int mapWidth = 10;         // 7 → 10
        int mapPathDensity = 8;    // 6 → 8

        AbstractDungeon.map = MapGenerator.generateDungeon(
            mapHeight,
            mapWidth,
            mapPathDensity,
            AbstractDungeon.mapRng
        );

        // 나머지 로직 복사...
        // (보물방 11층, 휴식처 19층 등)

        return SpireReturn.Return(null);  // 원본 메서드 스킵
    }
}
```

**2단계: UI 조정 (화면 크기 제한)**
```java
@SpirePatch(clz = Settings.class, method = "<clinit>")
public static class AdjustMapDisplayPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        // MAP_DST_Y 간격 조정 (노드가 더 많으므로 좁히기)
        Settings.MAP_DST_Y = Settings.MAP_DST_Y * 0.75F;  // 15/20 비율
    }
}
```

---

### 6.3 예제 3: 방 타입 확률 조정

**목표:** ELITE 확률 2배, REST 확률 절반 (하드 모드)

**1단계: initializeLevelSpecificChances 패치**
```java
@SpirePatch(clz = Exordium.class, method = "initializeLevelSpecificChances")
public static class HardModeChancesPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        AbstractDungeon.eliteRoomChance = 0.16F;   // 8% → 16%
        AbstractDungeon.restRoomChance = 0.06F;    // 12% → 6%
    }
}

// TheCity, TheBeyond도 동일하게 패치
```

**2단계: Ascension 보정 추가 조정**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateRoomTypes")
public static class ExtraEliteBoostPatch {
    @SpireInsertPatch(locator = Locator.class, localvars = {"eliteCount"})
    public static void Insert(@ByRef int[] eliteCount) {
        // Ascension 1.6배 → 2.0배
        if (AbstractDungeon.ascensionLevel >= 1) {
            eliteCount[0] = (int)(eliteCount[0] * 1.25F);  // 추가 25%
        }
    }
}
```

---

### 6.4 예제 4: 층별 특수 규칙

**목표:** 3층마다 휴식처 보장, 5층마다 상점 보장

**1단계: 고정 배치 패치**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class GuaranteedRoomsPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert() {
        // 3층: 휴식처
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(3),
            RestRoom.class
        );

        // 6층: 휴식처
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(6),
            RestRoom.class
        );

        // 9층: 휴식처 (8층 보물 제외)
        // 이미 8층에 보물이 있으므로 9층만 설정
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(9),
            RestRoom.class
        );

        // 5층: 상점
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(5),
            ShopRoom.class
        );

        // 10층: 상점
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(10),
            ShopRoom.class
        );
    }

    private static class Locator extends SpireInsertLocator {
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                RoomTypeAssigner.class, "distributeRoomsAcrossMap"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

**2단계: roomList 조정 (중복 방지)**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateRoomTypes")
public static class AdjustRoomCountPatch {
    @SpirePostfixPatch
    public static void Postfix(ArrayList<AbstractRoom> roomList) {
        // 이미 고정된 휴식처/상점 제외
        int fixedRestCount = 3;   // 3층, 6층, 9층
        int fixedShopCount = 2;   // 5층, 10층

        // roomList에서 해당 개수만큼 제거
        for (int i = 0; i < fixedRestCount; i++) {
            roomList.removeIf(r -> r instanceof RestRoom);
        }
        for (int i = 0; i < fixedShopCount; i++) {
            roomList.removeIf(r -> r instanceof ShopRoom);
        }
    }
}
```

---

### 6.5 예제 5: 경로 커스터마이징

**목표:** 직선 경로만 생성 (분기 없음)

**1단계: createPaths 패치**
```java
@SpirePatch(clz = MapGenerator.class, method = "_createPaths")
public static class StraightPathsPatch {
    @SpirePrefixPatch
    public static SpireReturn<ArrayList<ArrayList<MapRoomNode>>> Prefix(
        ArrayList<ArrayList<MapRoomNode>> nodes,
        MapEdge edge,
        Random rng
    ) {
        MapRoomNode currentNode = MapGenerator.getNode(edge.dstX, edge.dstY, nodes);

        if (edge.dstY + 1 >= nodes.size()) {
            currentNode.addEdge(new MapEdge(..., true));
            return SpireReturn.Return(nodes);
        }

        // 항상 직진 (newEdgeX = edge.dstX)
        int newEdgeX = edge.dstX;  // 분기 없음!
        int newEdgeY = edge.dstY + 1;

        MapRoomNode targetNode = MapGenerator.getNode(newEdgeX, newEdgeY, nodes);
        MapEdge newEdge = new MapEdge(
            edge.dstX, edge.dstY,
            currentNode.offsetX, currentNode.offsetY,
            newEdgeX, newEdgeY,
            targetNode.offsetX, targetNode.offsetY,
            false
        );

        currentNode.addEdge(newEdge);
        targetNode.addParent(currentNode);

        return MapGenerator._createPaths(nodes, newEdge, rng);
    }
}
```

**결과:**
- 6개의 완전히 독립적인 수직 경로
- 경로 간 이동 불가 (Winged Greaves 필요)

---

### 6.6 예제 6: 특정 방 강제 배치

**목표:** 7층에 항상 상점, 10층에 항상 휴식처

**방법 1: assignRowAsRoomType 사용**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class Force7ShopPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert() {
        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(7),
            ShopRoom.class
        );

        RoomTypeAssigner.assignRowAsRoomType(
            AbstractDungeon.map.get(10),
            RestRoom.class
        );
    }
}
```

**방법 2: 특정 노드만 강제 설정**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class ForceSpecificNodePatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert() {
        // 7층의 X=3 노드만 상점으로 강제
        MapRoomNode node = AbstractDungeon.map.get(7).get(3);
        if (node.hasEdges()) {
            node.setRoom(new ShopRoom());
        }
    }
}
```

---

## 7. 고급 수정

### 7.1 동적 지도 생성

**목표:** 플레이어 HP가 낮으면 휴식처 증가

**1단계: HP 기반 확률 조정**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "initializeLevelSpecificChances")
public static class DynamicRestChancePatch {
    @SpirePostfixPatch
    public static void Postfix() {
        float hpPercent = (float)AbstractDungeon.player.currentHealth /
                          (float)AbstractDungeon.player.maxHealth;

        if (hpPercent < 0.3F) {
            // HP 30% 이하 → 휴식처 2배
            AbstractDungeon.restRoomChance = 0.24F;
            AbstractDungeon.eventRoomChance = 0.16F;  // 이벤트 감소로 보상
        } else if (hpPercent < 0.5F) {
            // HP 50% 이하 → 휴식처 1.5배
            AbstractDungeon.restRoomChance = 0.18F;
            AbstractDungeon.eventRoomChance = 0.19F;
        }
    }
}
```

**2단계: 저주 개수 기반 상점 증가**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "initializeLevelSpecificChances")
public static class DynamicShopChancePatch {
    @SpirePostfixPatch
    public static void Postfix() {
        int curseCount = 0;
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (c.type == AbstractCard.CardType.CURSE) {
                curseCount++;
            }
        }

        if (curseCount >= 3) {
            // 저주 3개 이상 → 상점 2배 (상점에서 제거 가능)
            AbstractDungeon.shopRoomChance = 0.10F;
            AbstractDungeon.monsterRoomChance -= 0.05F;
        }
    }
}
```

### 7.2 분기형 지도

**목표:** 중간에 2개 경로로 완전 분리 후 다른 보스

**1단계: 8층에서 경로 분리**
```java
@SpirePatch(clz = MapGenerator.class, method = "generateDungeon")
public static class SplitPathMapPatch {
    @SpirePrefixPatch
    public static SpireReturn<ArrayList<ArrayList<MapRoomNode>>> Prefix(
        int height, int width, int pathDensity, Random rng
    ) {
        // 0~8층: 일반 생성
        ArrayList<ArrayList<MapRoomNode>> lowerMap =
            MapGenerator.generateDungeon(9, width, pathDensity, rng);

        // 9~15층: 좌우 분리
        ArrayList<ArrayList<MapRoomNode>> upperMap = new ArrayList<>();
        for (int y = 9; y < height; y++) {
            ArrayList<MapRoomNode> row = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                MapRoomNode node = new MapRoomNode(x, y);

                // 좌측 3칸, 우측 3칸만 활성화 (중간 비움)
                if (x < 3 || x >= 4) {
                    row.add(node);
                } else {
                    row.add(new MapRoomNode(x, y));  // 비활성
                }
            }
            upperMap.add(row);
        }

        // 경로 연결 (좌측 3칸 → 좌측 보스, 우측 3칸 → 우측 보스)
        createSplitPaths(upperMap, 3, rng);  // 좌측
        createSplitPaths(upperMap, 3, rng);  // 우측 (x >= 4)

        // 병합
        lowerMap.addAll(upperMap);
        return SpireReturn.Return(lowerMap);
    }
}
```

**2단계: 보스 선택 분기**
```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class DualBossPatch {
    @SpireInsertPatch(locator = Locator.class)
    public static void Insert() {
        // 플레이어가 선택한 경로에 따라 다른 보스
        MapRoomNode currNode = AbstractDungeon.getCurrMapNode();

        if (currNode.x < 3) {
            // 좌측 경로 → 보스 A
            AbstractDungeon.setBoss("The Guardian");
        } else {
            // 우측 경로 → 보스 B
            AbstractDungeon.setBoss("Hexaghost");
        }
    }
}
```

### 7.3 특수 층 추가 (비밀 층)

**목표:** 특정 조건 충족 시에만 등장하는 비밀 층

**1단계: 비밀 방 클래스**
```java
public class SecretRoom extends EventRoom {
    public SecretRoom() {
        this.mapSymbol = "S";
        this.mapImg = ImageMaster.loadImage("mymod/images/map/secret.png");
        this.mapImgOutline = ImageMaster.loadImage("mymod/images/map/secret_outline.png");
    }

    @Override
    public void onPlayerEntry() {
        // 비밀 이벤트 (특별 보상)
        this.event = new SecretEvent();
        this.event.onEnterRoom();
    }
}
```

**2단계: 조건부 노드 생성**
```java
@SpirePatch(clz = MapGenerator.class, method = "generateDungeon")
public static class SecretFloorPatch {
    @SpirePostfixPatch
    public static void Postfix(ArrayList<ArrayList<MapRoomNode>> __result) {
        // 조건: 3개 이상의 유물 보유
        if (AbstractDungeon.player.relics.size() >= 3) {
            // 7층 오른쪽 끝에 비밀 노드 추가
            MapRoomNode secretNode = new MapRoomNode(7, 7);  // X=7 (추가)
            secretNode.setRoom(new SecretRoom());

            // 6층 오른쪽 노드와 연결
            MapRoomNode parent = __result.get(6).get(6);
            MapEdge edge = new MapEdge(6, 6, parent.offsetX, parent.offsetY,
                                       7, 7, secretNode.offsetX, secretNode.offsetY, false);
            parent.addEdge(edge);
            secretNode.addParent(parent);

            // 8층 오른쪽 노드와 연결
            MapRoomNode child = __result.get(8).get(6);
            MapEdge edge2 = new MapEdge(7, 7, secretNode.offsetX, secretNode.offsetY,
                                        6, 8, child.offsetX, child.offsetY, false);
            secretNode.addEdge(edge2);
            child.addParent(secretNode);

            __result.get(7).add(secretNode);
        }
    }
}
```

### 7.4 지도 시각화 수정

**노드 색상 변경:**
```java
@SpirePatch(clz = MapRoomNode.class, method = "render")
public static class CustomNodeColorPatch {
    @SpireInsertPatch(locator = Locator.class, localvars = {"sb"})
    public static void Insert(MapRoomNode __instance, SpriteBatch sb) {
        if (__instance.room instanceof MiniBossRoom) {
            // 미니보스는 보라색
            sb.setColor(new Color(0.8F, 0.2F, 0.8F, 1.0F));
        }
    }
}
```

**아이콘 변경:**
```java
@SpirePatch(clz = MonsterRoom.class, method = "<init>")
public static class CustomMonsterIconPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoom __instance) {
        // 엘리트 이후 일반 전투는 다른 아이콘
        if (AbstractDungeon.getCurrMapNode().room instanceof MonsterRoomElite) {
            __instance.setMapImg(
                ImageMaster.loadImage("mymod/images/map/monster_hard.png"),
                ImageMaster.loadImage("mymod/images/map/monster_hard_outline.png")
            );
        }
    }
}
```

---

## 8. 주의사항

### 8.1 mapRng 시드 일관성

**문제:**
```java
// 잘못된 예
Random myRng = new Random();
MapGenerator.generateDungeon(15, 7, 6, myRng);  // ❌ 재현 불가
```

**올바른 예:**
```java
// mapRng 사용
MapGenerator.generateDungeon(15, 7, 6, AbstractDungeon.mapRng);  // ✅
```

**이유:**
- `mapRng`는 `Settings.seed + actNum` 기반
- 같은 시드 → 같은 지도
- 재현 가능한 플레이 보장

### 8.2 도달 불가 노드 방지

**문제:**
```java
// 부모 없이 Edge만 추가
MapEdge edge = new MapEdge(2, 5, ..., 3, 6, ..., false);
node.addEdge(edge);  // ❌ 자식만 설정, 부모 미설정
```

**올바른 예:**
```java
MapEdge edge = new MapEdge(2, 5, ..., 3, 6, ..., false);
parentNode.addEdge(edge);
childNode.addParent(parentNode);  // ✅ 양방향 설정
```

**검증:**
```java
// RoomTypeAssigner.lastMinuteNodeChecker()
for (MapRoomNode node : row) {
    if (node.hasEdges() && node.getRoom() == null) {
        logger.info("WARNING: Node " + node + " is unreachable!");
        node.setRoom(new MonsterRoom());  // 강제 할당
    }
}
```

### 8.3 필수 방 누락 방지

**체크리스트:**
- ✅ 0층: MonsterRoom (전투)
- ✅ 8층: TreasureRoom (보물)
- ✅ 14층: RestRoom (휴식)
- ✅ 15층: MonsterRoomBoss (보스)

**패치 시 주의:**
```java
// generateMap()를 완전히 교체할 때
@SpirePrefixPatch
public static SpireReturn<Void> Prefix() {
    // ... 커스텀 생성 ...

    // 필수 방 설정 잊지 말 것!
    RoomTypeAssigner.assignRowAsRoomType(map.get(14), RestRoom.class);
    RoomTypeAssigner.assignRowAsRoomType(map.get(0), MonsterRoom.class);
    RoomTypeAssigner.assignRowAsRoomType(map.get(8), TreasureRoom.class);

    return SpireReturn.Return(null);
}
```

### 8.4 경로 간 최소 거리 유지

**getCommonAncestor() 파라미터:**
- `min_ancestor_gap = 3`: 최소 3층 이상 떨어진 후 병합
- `max_ancestor_gap = 5`: 5층 이내 공통 조상 탐색

**수정 시:**
```java
// 더 빨리 병합하려면
int min_ancestor_gap = 1;  // 위험! 경로가 너무 얽힘

// 더 늦게 병합하려면
int min_ancestor_gap = 5;  // 안전
```

### 8.5 메모리 효율성

**대형 지도 (예: 30x30):**
```java
// 노드 개수: 30 * 30 = 900개
// Edge 개수: ~2700개 (경로 밀도에 따라)
// 메모리 사용량: ~5MB (객체 + 이미지)

// 최적화:
@SpirePatch(clz = MapRoomNode.class, method = "<init>")
public static class LazyImageLoadPatch {
    @SpirePostfixPatch
    public static void Postfix(MapRoomNode __instance) {
        // 이미지 로드를 hasEdges() 체크 후로 연기
        if (!__instance.hasEdges()) {
            __instance.mapImg = null;
            __instance.mapImgOutline = null;
        }
    }
}
```

### 8.6 UI 표시 제한

**화면 크기:**
- 1920x1080: 최대 ~20층 표시 가능
- 1280x720: 최대 ~15층 표시 가능

**스크롤 구현:**
```java
@SpirePatch(clz = DungeonMapScreen.class, method = "update")
public static class MapScrollPatch {
    @SpirePostfixPatch
    public static void Postfix(DungeonMapScreen __instance) {
        if (InputHelper.scrolledUp) {
            DungeonMapScreen.offsetY += 50.0F * Settings.scale;
        } else if (InputHelper.scrolledDown) {
            DungeonMapScreen.offsetY -= 50.0F * Settings.scale;
        }

        // 경계 제한
        DungeonMapScreen.offsetY = MathHelper.clamp(
            DungeonMapScreen.offsetY,
            0.0F,
            (AbstractDungeon.map.size() - 15) * Settings.MAP_DST_Y
        );
    }
}
```

### 8.7 세이브 파일 호환성

**주의:**
```java
// 지도 구조 변경 시 기존 세이브 파일 깨질 수 있음!

@SpirePatch(clz = SaveFile.class, method = "loadMap")
public static class MapCompatibilityPatch {
    @SpirePostfixPatch
    public static void Postfix(SaveFile __instance) {
        // 기존 세이브: 15층
        // 새 모드: 20층

        if (__instance.floor_num > 15) {
            // 경고 표시 또는 강제 종료
            logger.error("Save file incompatible with new map!");
        }
    }
}
```

---

## 9. 디버깅 & 로깅

### 9.1 지도 출력

```java
// MapGenerator.toString() 사용
String mapString = MapGenerator.toString(AbstractDungeon.map, true);
logger.info("Generated Map:\n" + mapString);

// 출력 예시:
/*
14 R R R R R R R
13 M E $ M ? M E
12 M M ? E M $ M
11 ? M M M E M ?
10 M $ M ? M ? M
 9 E M ? M M M E
 8 T T T T T T T
 7 M ? M E M $ M
 6 M M E M ? M M
 5 $ M M M M E ?
 4 M E ? M $ M M
 3 ? M M $ M M E
 2 M M E M ? M M
 1 M ? M M M E $
 0 M M M M M M M
*/
```

### 9.2 확률 로깅

```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateRoomTypes")
public static class LogChancesPatch {
    @SpirePostfixPatch
    public static void Postfix(ArrayList<AbstractRoom> roomList, int availableRoomCount) {
        logger.info("=== Room Generation Stats ===");
        logger.info("Total Nodes: " + availableRoomCount);
        logger.info("Shop: " + countRoomType(roomList, ShopRoom.class));
        logger.info("Rest: " + countRoomType(roomList, RestRoom.class));
        logger.info("Elite: " + countRoomType(roomList, MonsterRoomElite.class));
        logger.info("Event: " + countRoomType(roomList, EventRoom.class));
        logger.info("Monster: " + countRoomType(roomList, MonsterRoom.class));
    }

    private static int countRoomType(ArrayList<AbstractRoom> list, Class<?> type) {
        return (int)list.stream().filter(r -> type.isInstance(r)).count();
    }
}
```

---

## 10. 참고 자료

### 10.1 주요 클래스 파일

```
com/megacrit/cardcrawl/map/
├── MapGenerator.java           # 노드 & 경로 생성
├── MapRoomNode.java            # 노드 정의
├── MapEdge.java                # 연결선 정의
├── RoomTypeAssigner.java       # 방 타입 배정 규칙
└── DungeonMap.java             # 지도 UI 렌더링

com/megacrit/cardcrawl/dungeons/
├── AbstractDungeon.java        # 지도 생성 조율
├── Exordium.java               # Act 1
├── TheCity.java                # Act 2
├── TheBeyond.java              # Act 3
└── TheEnding.java              # Act 4

com/megacrit/cardcrawl/rooms/
├── AbstractRoom.java           # 방 기본 클래스
├── MonsterRoom.java            # M
├── MonsterRoomElite.java       # E
├── RestRoom.java               # R
├── ShopRoom.java               # $
├── TreasureRoom.java           # T
├── EventRoom.java              # ?
└── MonsterRoomBoss.java        # B

com/megacrit/cardcrawl/helpers/
└── EventHelper.java            # 이벤트 방 내부 롤
```

### 10.2 관련 상수

```java
// AbstractDungeon.java
public static final int MAP_HEIGHT = 15;
public static final int MAP_WIDTH = 7;
public static final int MAP_DENSITY = 6;
public static final int FINAL_ACT_MAP_HEIGHT = 3;  // Act 4

// MapRoomNode.java
public static final float OFFSET_X = 560.0F * Settings.xScale;
private static final float OFFSET_Y = 180.0F * Settings.scale;
private static final float SPACING_X = IMG_WIDTH * 2.0F;
private static final float JITTER_X = 27.0F * Settings.xScale;
private static final float JITTER_Y = 37.0F * Settings.xScale;

// Settings.java
public static float MAP_DST_Y = 180.0F * Settings.scale;  // 층 간격
```

---

## 결론

이 문서는 Slay the Spire의 지도 생성 시스템을 완전히 분석하고, 실전에서 바로 사용할 수 있는 수정 방법을 제공합니다.

**핵심 포인트:**
1. **7x15 그리드 + 6개 경로** 구조 이해
2. **확률 기반 방 배정** (5% 상점, 12% 휴식, 8% 엘리트, 22% 이벤트)
3. **규칙 기반 배치** (층 제한, 부모/형제 중복 방지)
4. **경로 알고리즘** (분기, 병합 방지, 교차 방지)
5. **SpirePatch로 안전하게 수정**

**추가 학습:**
- ModTheSpire Wiki: https://github.com/kiooeht/ModTheSpire/wiki
- BaseMod Documentation: https://github.com/daviscook477/BaseMod/wiki
- Slay the Spire Modding Discord: https://discord.gg/STS

---

**작성:** Claude Code AI
**버전:** 1.0.0
**최종 수정:** 2025-01-08
