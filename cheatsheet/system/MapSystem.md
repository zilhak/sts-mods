# MapSystem.md - Slay the Spire Map Generation and Navigation

이 문서는 Slay the Spire의 지도 시스템 작동 원리를 설명합니다.

---

## 1. 지도 생성 과정 (MapGenerator)

### 1.1 생성 흐름

```java
MapGenerator.generateDungeon(int height, int width, int pathDensity, Random rng)
├─> createNodes(height, width)          // 모든 노드 생성
├─> createPaths(map, pathDensity, rng)  // 경로 생성
└─> filterRedundantEdgesFromRow(map)    // 중복 엣지 제거
```

### 1.2 노드 생성

```java
ArrayList<ArrayList<MapRoomNode>> nodes = new ArrayList<>();
for (int y = 0; y < height; y++) {
    ArrayList<MapRoomNode> row = new ArrayList<>();
    for (int x = 0; x < width; x++) {
        row.add(new MapRoomNode(x, y));
    }
    nodes.add(row);
}
```

- 2차원 배열로 모든 가능한 위치에 노드 생성
- Y축: 층 (0층부터 시작, 위로 올라갈수록 증가)
- X축: 가로 위치 (보통 0~6)

### 1.3 경로 생성 알고리즘

**시작점**:
```java
int startingNode = randRange(rng, 0, row_size);
_createPaths(nodes, new MapEdge(startingNode, -1, startingNode, 0), rng);
```

**재귀적 경로 생성**:
```java
private static ArrayList<ArrayList<MapRoomNode>> _createPaths(
    ArrayList<ArrayList<MapRoomNode>> nodes,
    MapEdge edge,
    Random rng
) {
    MapRoomNode currentNode = getNode(edge.dstX, edge.dstY, nodes);

    // 최상층 도달 시 보스 엣지 생성
    if (edge.dstY + 1 >= nodes.size()) {
        MapEdge bossEdge = new MapEdge(
            edge.dstX, edge.dstY, currentNode.offsetX, currentNode.offsetY,
            3, edge.dstY + 2, 0.0F, 0.0F, true
        );
        currentNode.addEdge(bossEdge);
        return nodes;
    }

    // 다음 노드 X 좌표 결정 (좌/중/우)
    int min, max;
    if (edge.dstX == 0) {
        min = 0; max = 1;  // 왼쪽 끝: 우측만
    } else if (edge.dstX == row_end_node) {
        min = -1; max = 0; // 오른쪽 끝: 좌측만
    } else {
        min = -1; max = 1; // 중앙: 좌/중/우
    }

    int newEdgeX = edge.dstX + randRange(rng, min, max);
    int newEdgeY = edge.dstY + 1;

    // 공통 조상 체크 (경로가 너무 빨리 합쳐지는 것 방지)
    // min_ancestor_gap = 3, max_ancestor_gap = 5

    // 엣지 충돌 방지 (좌우 노드의 엣지와 교차하지 않도록)

    MapEdge newEdge = new MapEdge(...);
    currentNode.addEdge(newEdge);
    targetNodeCandidate.addParent(currentNode);

    return _createPaths(nodes, newEdge, rng); // 재귀 호출
}
```

**경로 생성 규칙**:
1. **진행 방향**: 항상 위로 (y + 1)
2. **X축 이동**: 좌(-1), 중(0), 우(+1) 중 랜덤
3. **공통 조상 제한**: 두 경로의 공통 조상이 3~5층 이내에 있으면 경로 조정
4. **엣지 교차 방지**: 좌우 노드의 엣지와 겹치지 않도록 조정

### 1.4 중복 엣지 제거

```java
private static ArrayList<ArrayList<MapRoomNode>> filterRedundantEdgesFromRow(
    ArrayList<ArrayList<MapRoomNode>> map
) {
    ArrayList<MapEdge> existingEdges = new ArrayList<>();
    ArrayList<MapEdge> deleteList = new ArrayList<>();

    for (MapRoomNode node : map.get(0)) { // 0층만 처리
        if (node.hasEdges()) {
            for (MapEdge edge : node.getEdges()) {
                for (MapEdge prevEdge : existingEdges) {
                    if (edge.dstX == prevEdge.dstX && edge.dstY == prevEdge.dstY) {
                        deleteList.add(edge); // 같은 목적지로 가는 엣지 제거
                    }
                }
                existingEdges.add(edge);
            }

            for (MapEdge edge : deleteList) {
                node.delEdge(edge);
            }
            deleteList.clear();
        }
    }
    return map;
}
```

---

## 2. MapRoomNode 구조

### 2.1 주요 필드

```java
public class MapRoomNode {
    public int x;  // 가로 좌표
    public int y;  // 세로 좌표 (층)

    public AbstractRoom room = null;  // 실제 방 인스턴스

    private ArrayList<MapRoomNode> parents = new ArrayList<>();  // 부모 노드들
    private ArrayList<MapEdge> edges = new ArrayList<>();        // 자식으로 가는 엣지들

    public boolean taken = false;      // 방문 여부
    public boolean highlighted = false; // 선택 가능 여부

    public Hitbox hb = null;  // 클릭 영역

    public float offsetX, offsetY;  // 렌더링 위치 지터 (랜덤 오프셋)
}
```

### 2.2 좌표 시스템

**화면 좌표 계산**:
```java
float screenX = x * SPACING_X + OFFSET_X + offsetX;
float screenY = y * Settings.MAP_DST_Y + OFFSET_Y + DungeonMapScreen.offsetY + offsetY;

// SPACING_X: 노드 간 가로 간격 (IMG_WIDTH * 2.0F)
// OFFSET_X: 화면 좌측 여백 (560.0F * Settings.xScale)
// OFFSET_Y: 화면 하단 여백 (180.0F * Settings.scale)
// DungeonMapScreen.offsetY: 지도 스크롤 오프셋
```

### 2.3 부모/자식 관계

```java
// 부모 추가 (하위 층에서 이 노드로 오는 노드)
public void addParent(MapRoomNode parent) {
    this.parents.add(parent);
}

// 자식 엣지 추가 (이 노드에서 상위 층으로 가는 경로)
public void addEdge(MapEdge e) {
    Boolean unique = Boolean.valueOf(true);
    for (MapEdge otherEdge : this.edges) {
        if (e.compareTo(otherEdge) == 0) {
            unique = Boolean.valueOf(false);
        }
    }
    if (unique.booleanValue()) {
        this.edges.add(e);
    }
}
```

**관계 구조**:
```
    [노드 3,2]  [노드 4,2]
        ↑  \    /  ↑
        |   \  /   |
        |    \/    |
        |    /\    |
        |   /  \   |
    [노드 2,1]  [노드 5,1]
        ↑           ↑
        |           |
    [노드 2,0]  [노드 5,0]
```

- **parents**: 아래층에서 이 노드로 연결된 노드들
- **edges**: 이 노드에서 위층으로 가는 경로들

---

## 3. 경로 선택 가능 조건

### 3.1 일반 연결 확인

```java
public boolean isConnectedTo(MapRoomNode node) {
    for (MapEdge edge : this.edges) {
        if (node.x == edge.dstX && node.y == edge.dstY) {
            return true;
        }
    }
    return false;
}
```

### 3.2 날개 달린 신발 (Winged Greaves) 연결

```java
public boolean wingedIsConnectedTo(MapRoomNode node) {
    for (MapEdge edge : this.edges) {
        // "Flight" 모드 활성화 시
        if (ModHelper.isModEnabled("Flight") && node.y == edge.dstY) {
            return true;
        }

        // Winged Greaves 렐릭 보유 시 (카운터 > 0)
        if (node.y == edge.dstY &&
            AbstractDungeon.player.hasRelic("WingedGreaves") &&
            (AbstractDungeon.player.getRelic("WingedGreaves")).counter > 0) {
            return true;
        }
    }
    return false;
}
```

**선택 가능 조건 (MapRoomNode.update)**:
```java
boolean normalConnection = AbstractDungeon.getCurrMapNode().isConnectedTo(this);
boolean wingedConnection = AbstractDungeon.getCurrMapNode().wingedIsConnectedTo(this);

if (normalConnection || Settings.isDebug || wingedConnection) {
    // 노드 선택 가능
    if (this.hb.hovered) {
        this.highlighted = true;
        oscillateColor(); // 색상 애니메이션
    }
}
```

---

## 4. 지도 화면 렌더링

### 4.1 DungeonMapScreen 구조

```java
public class DungeonMapScreen {
    public DungeonMap map = new DungeonMap();
    private ArrayList<MapRoomNode> visibleMapNodes = new ArrayList<>();

    public static float offsetY = -100.0F * Settings.scale; // 스크롤 오프셋
    private float targetOffsetY = offsetY;

    private float mapScrollUpperLimit; // 스크롤 상한
    private static final float MAP_UPPER_SCROLL_DEFAULT = -2300.0F * Settings.scale;
    private static final float MAP_SCROLL_LOWER = 190.0F * Settings.scale;
}
```

### 4.2 보이는 노드 업데이트

```java
public void updateImage() {
    this.visibleMapNodes.clear();

    for (ArrayList<MapRoomNode> rows : CardCrawlGame.dungeon.getMap()) {
        for (MapRoomNode node : rows) {
            if (node.hasEdges()) { // 엣지가 있는 노드만 표시
                this.visibleMapNodes.add(node);
            }
        }
    }
}
```

### 4.3 렌더링 순서

```java
public void render(SpriteBatch sb) {
    this.map.render(sb);  // 배경 지도

    if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
        for (MapRoomNode n : this.visibleMapNodes) {
            n.render(sb);  // 각 노드 렌더링
        }
    }

    this.map.renderBossIcon(sb);  // 보스 아이콘
}
```

### 4.4 노드 렌더링 (MapRoomNode.render)

```java
public void render(SpriteBatch sb) {
    // 1. 엣지 렌더링 (먼저)
    for (MapEdge edge : this.edges) {
        edge.render(sb);
    }

    // 2. 에메랄드 키 효과 (있으면)
    renderEmeraldVfx(sb);

    // 3. 노드 아웃라인
    if (this.highlighted) {
        sb.setColor(new Color(0.9F, 0.9F, 0.9F, 1.0F));
    } else {
        sb.setColor(OUTLINE_COLOR);
    }
    sb.draw(this.room.getMapImgOutline(), ...);

    // 4. 노드 아이콘
    if (this.taken) {
        sb.setColor(AVAILABLE_COLOR);
    } else {
        sb.setColor(this.color);
    }
    sb.draw(this.room.getMapImg(), ...);

    // 5. 현재 위치/방문한 노드 원형 표시
    if (this.taken || equals(AbstractDungeon.getCurrMapNode())) {
        sb.draw(ImageMaster.MAP_CIRCLE_5, ...);
    }
}
```

---

## 5. 노드 클릭 시 처리 과정

### 5.1 클릭 감지 및 검증

```java
// MapRoomNode.update() 내부
if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP &&
    AbstractDungeon.dungeonMapScreen.clicked &&
    this.animWaitTimer <= 0.0F) {

    // 1. 사운드 재생
    playNodeSelectedSound();

    // 2. 클릭 플래그 리셋
    AbstractDungeon.dungeonMapScreen.clicked = false;
    AbstractDungeon.dungeonMapScreen.clickTimer = 0.0F;

    // 3. Winged Greaves 카운터 감소
    if (!normalConnection && wingedConnection &&
        AbstractDungeon.player.hasRelic("WingedGreaves")) {
        (AbstractDungeon.player.getRelic("WingedGreaves")).counter--;
        if ((AbstractDungeon.player.getRelic("WingedGreaves")).counter <= 0) {
            AbstractDungeon.player.getRelic("WingedGreaves").setCounter(-2);
        }
    }

    // 4. 시각 효과 추가
    AbstractDungeon.topLevelEffects.add(new MapCircleEffect(...));
    if (!Settings.FAST_MODE) {
        AbstractDungeon.topLevelEffects.add(new FadeWipeParticle());
    }

    // 5. 애니메이션 대기 타이머 설정
    this.animWaitTimer = 0.25F;

    // 6. 이벤트 방 카운터 증가
    if (this.room instanceof EventRoom) {
        CardCrawlGame.mysteryMachine++;
    }
}
```

### 5.2 애니메이션 대기 후 실행

```java
// MapRoomNode.update() 내부
if (this.animWaitTimer != 0.0F) {
    this.animWaitTimer -= Gdx.graphics.getDeltaTime();
    if (this.animWaitTimer < 0.0F) {
        // 첫 방 선택이 아닌 경우
        if (!AbstractDungeon.firstRoomChosen) {
            AbstractDungeon.setCurrMapNode(this);
        } else {
            (AbstractDungeon.getCurrMapNode()).taken = true;
        }

        // 엣지 방문 처리
        MapEdge connectedEdge = AbstractDungeon.getCurrMapNode().getEdgeConnectedTo(this);
        if (connectedEdge != null) {
            connectedEdge.markAsTaken();
        }

        this.animWaitTimer = 0.0F;

        // 다음 방 설정
        AbstractDungeon.nextRoom = this;
        AbstractDungeon.pathX.add(Integer.valueOf(this.x));
        AbstractDungeon.pathY.add(Integer.valueOf(this.y));
        CardCrawlGame.metricData.path_taken.add(AbstractDungeon.nextRoom.getRoom().getMapSymbol());

        // 방 전환 시작
        if (!AbstractDungeon.isDungeonBeaten) {
            AbstractDungeon.nextRoomTransitionStart();
            CardCrawlGame.music.fadeOutTempBGM();
        }
    }
}
```

---

## 6. 주의사항 (잘못된 노드 연결)

### 6.1 엣지 없는 노드 문제

**증상**:
```java
if (!node.hasEdges()) {
    // 이 노드는 visibleMapNodes에 추가되지 않음
    // 렌더링되지 않고, 클릭할 수 없음
}
```

**원인**:
- `createPaths()`에서 생성되지 않은 경로
- `filterRedundantEdgesFromRow()`에서 삭제된 중복 엣지
- 직접 지도를 수정했을 때 엣지 추가 누락

### 6.2 부모 노드 없는 노드

**증상**:
```java
if (targetNodeCandidate.getParents().isEmpty()) {
    // 공통 조상 체크를 건너뜀
    // 경로 생성 알고리즘이 정상 작동하지 않을 수 있음
}
```

**원인**:
- 경로 생성 시 `targetNodeCandidate.addParent(currentNode)` 누락
- 직접 지도를 수정했을 때 부모 관계 설정 누락

### 6.3 순환 참조 문제

**위험**:
```java
// 잘못된 경로: 노드 A -> 노드 B -> 노드 A
nodeA.addEdge(new MapEdge(... nodeB ...));
nodeB.addEdge(new MapEdge(... nodeA ...));
```

**예방**:
- 항상 Y 좌표가 증가하는 방향으로만 엣지 생성 (`newEdgeY = edge.dstY + 1`)
- 하위 층으로 가는 엣지를 절대 생성하지 않음

### 6.4 엣지 중복 문제

**증상**:
```java
// 같은 노드에 같은 목적지로 가는 엣지가 여러 개
node.edges = [
    MapEdge(2, 1 -> 3, 2),
    MapEdge(2, 1 -> 3, 2)  // 중복!
]
```

**예방**:
```java
public void addEdge(MapEdge e) {
    Boolean unique = Boolean.valueOf(true);
    for (MapEdge otherEdge : this.edges) {
        if (e.compareTo(otherEdge) == 0) {
            unique = Boolean.valueOf(false);
        }
    }
    if (unique.booleanValue()) {
        this.edges.add(e);
    }
}
```

---

## 7. 수정 방법 (지도 구조 변경)

### 7.1 새 경로 추가

```java
MapRoomNode fromNode = getNode(x1, y1, map);
MapRoomNode toNode = getNode(x2, y2, map);

// 엣지 생성
MapEdge newEdge = new MapEdge(
    fromNode.x, fromNode.y, fromNode.offsetX, fromNode.offsetY,
    toNode.x, toNode.y, toNode.offsetX, toNode.offsetY,
    false  // isBossNode
);

// 관계 설정 (필수!)
fromNode.addEdge(newEdge);
toNode.addParent(fromNode);
```

### 7.2 기존 경로 제거

```java
MapRoomNode fromNode = getNode(x1, y1, map);
MapRoomNode toNode = getNode(x2, y2, map);

// 엣지 찾기
MapEdge edgeToRemove = fromNode.getEdgeConnectedTo(toNode);
if (edgeToRemove != null) {
    fromNode.delEdge(edgeToRemove);
}

// 부모 관계 제거
toNode.getParents().remove(fromNode);
```

### 7.3 커스텀 지도 생성 예시

```java
@SpirePatch(clz = AbstractDungeon.class, method = "generateMap")
public static class CustomMapPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance) {
        // 기존 지도 가져오기
        ArrayList<ArrayList<MapRoomNode>> map = AbstractDungeon.map;

        // 예: 0층의 노드 2에서 1층의 노드 5로 가는 경로 추가
        MapRoomNode node_0_2 = map.get(0).get(2);
        MapRoomNode node_1_5 = map.get(1).get(5);

        MapEdge newPath = new MapEdge(
            node_0_2.x, node_0_2.y, node_0_2.offsetX, node_0_2.offsetY,
            node_1_5.x, node_1_5.y, node_1_5.offsetX, node_1_5.offsetY,
            false
        );

        node_0_2.addEdge(newPath);
        node_1_5.addParent(node_0_2);
    }
}
```

### 7.4 지도 검증 함수

```java
public static boolean validateMap(ArrayList<ArrayList<MapRoomNode>> map) {
    for (int y = 0; y < map.size(); y++) {
        for (MapRoomNode node : map.get(y)) {
            // 1. 엣지가 있는 노드는 반드시 room이 설정되어야 함
            if (node.hasEdges() && node.room == null) {
                logger.error("Node at (" + node.x + "," + node.y + ") has edges but no room!");
                return false;
            }

            // 2. 모든 엣지의 목적지가 유효한지 확인
            for (MapEdge edge : node.getEdges()) {
                if (edge.dstY >= map.size() || edge.dstX >= map.get(edge.dstY).size()) {
                    logger.error("Invalid edge destination: (" + edge.dstX + "," + edge.dstY + ")");
                    return false;
                }

                MapRoomNode targetNode = map.get(edge.dstY).get(edge.dstX);

                // 3. 목적지 노드의 부모 목록에 현재 노드가 있는지 확인
                if (!targetNode.getParents().contains(node)) {
                    logger.error("Edge exists but parent relationship missing!");
                    return false;
                }
            }
        }
    }
    return true;
}
```

---

## 8. 요약

### 지도 생성 순서
1. `createNodes()`: 모든 가능한 위치에 노드 생성
2. `createPaths()`: 재귀적으로 경로 생성 (아래에서 위로)
3. `filterRedundantEdgesFromRow()`: 0층 중복 엣지 제거

### 노드 선택 가능 조건
1. 현재 노드와 `isConnectedTo()` 관계
2. 날개 달린 신발: 같은 Y층의 임의 노드
3. 디버그 모드: 모든 노드

### 클릭 처리 순서
1. 클릭 감지 및 연결 검증
2. 시각 효과 및 사운드
3. 0.25초 대기 (`animWaitTimer`)
4. `AbstractDungeon.nextRoom` 설정
5. `AbstractDungeon.nextRoomTransitionStart()` 호출

### 필수 주의사항
- **엣지 추가 시**: 반드시 `toNode.addParent(fromNode)` 호출
- **방 설정**: 엣지가 있는 노드는 반드시 `room` 설정
- **Y 좌표**: 항상 증가하는 방향으로만 엣지 생성 (순환 참조 방지)
