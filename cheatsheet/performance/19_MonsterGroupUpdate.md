# 19. MonsterGroup Redundant Updates

## 문제 설명

`MonsterGroup.update()`가 매 프레임마다 모든 몬스터의 히트박스를 업데이트하고 호버 상태를 확인합니다. 대부분의 프레임에서는 마우스 위치가 변하지 않아 불필요한 연산이 반복됩니다.

## 성능 영향

**비용**: 몬스터당 3개 히트박스 × 업데이트
**빈도**: 60 FPS
**예상 절감**: 마우스 이동 없을 때 ~70% 감소

## 코드 위치

```
MonsterGroup.java:335-362
```

## 현재 구현

```java
// MonsterGroup.java
public void update() {
    // 1. 모든 몬스터 업데이트
    for (AbstractMonster m : this.monsters) {
        m.update();  // 각 몬스터의 복잡한 업데이트 로직
    }

    // 2. 호버 감지 (매 프레임)
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
        this.hoveredMonster = null;

        // 모든 몬스터의 히트박스를 업데이트하고 호버 확인
        for (AbstractMonster m : this.monsters) {
            if (!m.isDying && !m.isEscaping) {
                m.hb.update();              // 메인 히트박스
                m.intentHb.update();        // 인텐트 히트박스
                m.healthHb.update();        // 체력바 히트박스

                // 호버 확인
                if ((m.hb.hovered || m.intentHb.hovered || m.healthHb.hovered) &&
                    !AbstractDungeon.player.isDraggingCard) {
                    this.hoveredMonster = m;
                    break;  // 첫 번째 호버된 몬스터에서 중단
                }
            }
        }

        if (this.hoveredMonster == null) {
            AbstractDungeon.player.hoverEnemyWaitTimer = -1.0F;
        }
    } else {
        this.hoveredMonster = null;
    }
}

// Hitbox.java - update() 메서드
public void update() {
    // 마우스 위치 확인
    if (InputHelper.mX > this.x && InputHelper.mX < this.x + this.width &&
        InputHelper.mY > this.y && InputHelper.mY < this.y + this.height) {
        this.hovered = true;
    } else {
        this.hovered = false;
    }

    // 클릭 확인
    if (this.hovered) {
        if (InputHelper.justClickedLeft) {
            this.clickStarted = true;
        }
        if (this.clickStarted && InputHelper.justReleasedClickLeft) {
            this.clicked = true;
            this.clickStarted = false;
        }
    } else {
        this.clickStarted = false;
    }
}
```

## 문제 분석

1. **불필요한 히트박스 업데이트**: 마우스가 움직이지 않아도 매 프레임 모든 히트박스 체크
2. **중복 순회**: `m.update()` 내부에서도 히트박스 업데이트가 일어날 수 있음
3. **조기 종료 부재**: 호버된 몬스터를 찾아도 나머지 몬스터의 히트박스 업데이트 계속
4. **죽은 몬스터 체크**: 매 프레임 `isDying`, `isEscaping` 확인

## 최적화 전략

### 방법 1: 마우스 이동 감지

```java
// MonsterGroup.java
private float lastMouseX = -1.0f;
private float lastMouseY = -1.0f;
private static final float MOUSE_MOVE_THRESHOLD = 1.0f;  // 1픽셀 이동

public void update() {
    // 1. 몬스터 업데이트 (필수)
    for (AbstractMonster m : this.monsters) {
        m.update();
    }

    // 2. 호버 감지 최적화
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
        boolean mouseMovedSignificantly =
            Math.abs(InputHelper.mX - lastMouseX) > MOUSE_MOVE_THRESHOLD ||
            Math.abs(InputHelper.mY - lastMouseY) > MOUSE_MOVE_THRESHOLD;

        // 마우스가 크게 움직였을 때만 히트박스 업데이트
        if (mouseMovedSignificantly || this.hoveredMonster != null) {
            updateHoverState();
            lastMouseX = InputHelper.mX;
            lastMouseY = InputHelper.mY;
        }
    } else {
        this.hoveredMonster = null;
    }
}

private void updateHoverState() {
    this.hoveredMonster = null;

    for (AbstractMonster m : this.monsters) {
        if (m.isDying || m.isEscaping) {
            continue;  // 죽은 몬스터 스킵
        }

        // 히트박스 업데이트
        m.hb.update();
        m.intentHb.update();
        m.healthHb.update();

        // 호버 확인
        if ((m.hb.hovered || m.intentHb.hovered || m.healthHb.hovered) &&
            !AbstractDungeon.player.isDraggingCard) {
            this.hoveredMonster = m;
            // 호버 찾았으면 나머지 몬스터는 스킵 가능
            // 단, 모든 히트박스 상태를 초기화하려면 계속 순회
            break;
        }
    }

    if (this.hoveredMonster == null) {
        AbstractDungeon.player.hoverEnemyWaitTimer = -1.0F;
    }
}
```

### 방법 2: 공간 분할 (Spatial Partitioning)

```java
// MonsterGroup.java
private static final int GRID_SIZE = 200;  // 200x200 픽셀 그리드
private HashMap<Integer, ArrayList<AbstractMonster>> spatialGrid;

public void rebuildSpatialGrid() {
    spatialGrid = new HashMap<>();

    for (AbstractMonster m : this.monsters) {
        if (m.isDying || m.isEscaping) continue;

        // 몬스터가 차지하는 그리드 셀 계산
        int gridX = (int)(m.hb.cX / GRID_SIZE);
        int gridY = (int)(m.hb.cY / GRID_SIZE);
        int key = gridX * 1000 + gridY;  // 간단한 해시

        spatialGrid.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
    }
}

public void update() {
    // 몬스터 업데이트
    for (AbstractMonster m : this.monsters) {
        m.update();
    }

    // 호버 감지
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
        // 마우스 위치에 해당하는 그리드 셀만 확인
        int gridX = (int)(InputHelper.mX / GRID_SIZE);
        int gridY = (int)(InputHelper.mY / GRID_SIZE);
        int key = gridX * 1000 + gridY;

        this.hoveredMonster = null;

        // 해당 그리드 셀의 몬스터만 확인
        ArrayList<AbstractMonster> nearbyMonsters = spatialGrid.get(key);
        if (nearbyMonsters != null) {
            for (AbstractMonster m : nearbyMonsters) {
                if (m.isDying || m.isEscaping) continue;

                m.hb.update();
                m.intentHb.update();
                m.healthHb.update();

                if ((m.hb.hovered || m.intentHb.hovered || m.healthHb.hovered) &&
                    !AbstractDungeon.player.isDraggingCard) {
                    this.hoveredMonster = m;
                    break;
                }
            }
        }

        // 인접 그리드도 확인 (경계선 케이스)
        // ...

        if (this.hoveredMonster == null) {
            AbstractDungeon.player.hoverEnemyWaitTimer = -1.0F;
        }
    } else {
        this.hoveredMonster = null;
    }
}
```

### 방법 3: 활성 몬스터 필터링

```java
// MonsterGroup.java
public ArrayList<AbstractMonster> monsters = new ArrayList<>();
private ArrayList<AbstractMonster> aliveMonsters = new ArrayList<>();
private boolean monsterStateChanged = false;

public void update() {
    // 1. 몬스터 리스트 갱신 (상태 변경 시만)
    if (monsterStateChanged) {
        updateAliveMonstersList();
        monsterStateChanged = false;
    }

    // 2. 살아있는 몬스터만 업데이트
    for (AbstractMonster m : aliveMonsters) {
        m.update();

        // 상태 변화 감지
        if (m.isDying || m.isEscaping) {
            monsterStateChanged = true;
        }
    }

    // 3. 호버 감지 (살아있는 몬스터만)
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
        this.hoveredMonster = null;

        for (AbstractMonster m : aliveMonsters) {
            m.hb.update();
            m.intentHb.update();
            m.healthHb.update();

            if ((m.hb.hovered || m.intentHb.hovered || m.healthHb.hovered) &&
                !AbstractDungeon.player.isDraggingCard) {
                this.hoveredMonster = m;
                break;
            }
        }

        if (this.hoveredMonster == null) {
            AbstractDungeon.player.hoverEnemyWaitTimer = -1.0F;
        }
    } else {
        this.hoveredMonster = null;
    }
}

private void updateAliveMonstersList() {
    aliveMonsters.clear();
    for (AbstractMonster m : this.monsters) {
        if (!m.isDying && !m.isEscaping && !m.isDead && !m.escaped) {
            aliveMonsters.add(m);
        }
    }
}
```

### 방법 4: Hitbox 업데이트 분리

```java
// Hitbox.java - 업데이트를 단계별로 분리
public class Hitbox {
    public boolean hovered = false;
    private boolean wasHovered = false;

    // 경량 업데이트: 호버 상태만 확인
    public void updateHover() {
        this.wasHovered = this.hovered;
        this.hovered = containsPoint(InputHelper.mX, InputHelper.mY);
    }

    // 완전 업데이트: 클릭 이벤트 포함
    public void update() {
        updateHover();

        if (this.hovered) {
            if (InputHelper.justClickedLeft) {
                this.clickStarted = true;
            }
            if (this.clickStarted && InputHelper.justReleasedClickLeft) {
                this.clicked = true;
                this.clickStarted = false;
            }
        } else {
            this.clickStarted = false;
        }
    }

    private boolean containsPoint(float mx, float my) {
        return mx > this.x && mx < this.x + this.width &&
               my > this.y && my < this.y + this.height;
    }

    public boolean hoverChanged() {
        return this.hovered != this.wasHovered;
    }
}

// MonsterGroup.java - 경량 업데이트 사용
public void update() {
    // 몬스터 업데이트
    for (AbstractMonster m : this.monsters) {
        m.update();
    }

    // 호버 감지 (경량 버전)
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
        this.hoveredMonster = null;

        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;

            // 호버만 확인 (클릭 이벤트 처리 생략)
            m.hb.updateHover();
            m.intentHb.updateHover();
            m.healthHb.updateHover();

            if ((m.hb.hovered || m.intentHb.hovered || m.healthHb.hovered) &&
                !AbstractDungeon.player.isDraggingCard) {
                this.hoveredMonster = m;
                break;
            }
        }

        if (this.hoveredMonster == null) {
            AbstractDungeon.player.hoverEnemyWaitTimer = -1.0F;
        }
    }
}
```

## 구현 가이드

### 단계 1: 마우스 이동 추적 변수 추가

```java
// MonsterGroup.java
private float lastMouseX = -1.0f;
private float lastMouseY = -1.0f;
private static final float MOUSE_MOVE_THRESHOLD = 1.0f;
```

### 단계 2: 마우스 이동 감지 로직 구현

```java
boolean mouseMovedSignificantly =
    Math.abs(InputHelper.mX - lastMouseX) > MOUSE_MOVE_THRESHOLD ||
    Math.abs(InputHelper.mY - lastMouseY) > MOUSE_MOVE_THRESHOLD;
```

### 단계 3: 조건부 업데이트 적용

```java
if (mouseMovedSignificantly || this.hoveredMonster != null) {
    // 히트박스 업데이트 및 호버 감지
    updateHoverState();
    lastMouseX = InputHelper.mX;
    lastMouseY = InputHelper.mY;
}
```

### 단계 4: 활성 몬스터 필터링 (선택사항)

```java
private ArrayList<AbstractMonster> aliveMonsters = new ArrayList<>();

private void updateAliveMonstersList() {
    aliveMonsters.clear();
    for (AbstractMonster m : this.monsters) {
        if (!m.isDying && !m.isEscaping) {
            aliveMonsters.add(m);
        }
    }
}
```

## 측정 방법

```java
// 성능 측정
long start = System.nanoTime();

for (int i = 0; i < 1000; i++) {
    monsterGroup.update();
}

long elapsed = System.nanoTime() - start;
System.out.println("MonsterGroup update avg: " + (elapsed / 1000 / 1000) + "μs");
```

## 예상 결과

**시나리오**: 3 몬스터, 마우스 정지 상태

- **Before**:
  - 히트박스 업데이트: 3 × 3 = 9회/프레임
  - 조건 확인: 9회/프레임
  - 총: 60fps × 9 = 540 연산/초

- **After** (마우스 이동 감지):
  - 마우스 정지: 0 히트박스 업데이트
  - 마우스 이동: 9회/프레임
  - 평균 절감: ~70% (대부분 정지 상태)

- **After** (공간 분할):
  - 관련 그리드만 확인: ~1-2 몬스터
  - 절감: ~50-67%

## 주의사항

1. **호버 상태 지연**: 마우스 이동 임계값이 너무 크면 호버 반응 느려짐
2. **활성 몬스터 동기화**: 몬스터 상태 변경 시 `aliveMonsters` 리스트 갱신 필요
3. **클릭 이벤트**: 경량 업데이트 사용 시 클릭 이벤트 처리 방법 고려
4. **메모리 사용**: 공간 분할 사용 시 추가 메모리 필요

## 관련 이슈

- [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md): 몬스터 개별 업데이트 최적화
- [20_HitboxUpdate.md](20_HitboxUpdate.md): 히트박스 업데이트 최적화

## 참고 자료

- `MonsterGroup.java:335-362` - update() 메서드
- `Hitbox.java:update()` - 히트박스 업데이트 로직
- `InputHelper.java` - 마우스 입력 처리
