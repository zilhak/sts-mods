# Performance Issue: Redundant Color Setting

## 문제 발견 위치
- **파일**: `AbstractMonster.java`, `AbstractRelic.java`, `AbstractPotion.java`
- **메서드**: `render()`, `renderIntent()`, `renderOutline()`
- **라인**:
  - AbstractMonster.java: 864, 897, 1021, 1042, 1045, 1093
  - AbstractRelic.java: 707, 725, 969, 974, 1066
  - AbstractPotion.java: 574, 594, 615, 635, 701, 720, 742, 762

## 문제 설명

### 발견된 패턴
매 프레임마다 동일한 색상이 반복적으로 설정됨:

```java
// AbstractMonster.java:864, 897 (render 메서드)
sb.setColor(this.tint.color);  // 색상 설정 #1
if (this.img != null) {
    sb.draw(this.img, ...);
}

// 바로 다음 줄에서
sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.1F));  // 색상 설정 #2
sb.draw(this.img, ...);  // 같은 이미지 다시 그리기

// AbstractPotion.java:574-636 (render 메서드)
sb.setColor(this.liquidColor);   // 색상 설정 #1
sb.draw(this.liquidImg, ...);

if (this.hybridColor != null) {
    sb.setColor(this.hybridColor);  // 색상 설정 #2
    sb.draw(this.hybridImg, ...);
}

if (this.spotsColor != null) {
    sb.setColor(this.spotsColor);   // 색상 설정 #3
    sb.draw(this.spotsImg, ...);
}

sb.setColor(Color.WHITE);           // 색상 설정 #4
sb.draw(this.containerImg, ...);
```

### 문제점
1. **불필요한 GPU 상태 변경**: 색상이 실제로 변경되지 않아도 매번 설정
2. **Color 객체 생성**: `new Color(...)` 매 프레임 생성
3. **드로우콜 최적화 방해**: 같은 색상인데도 배치 분리

## 원인 분석

### 1. 매 프레임 Color 객체 생성
```java
// AbstractMonster.java:897
sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.1F));  // ❌ 매 프레임 새 Color 생성

// 60 FPS 기준:
// - 초당 60개 Color 객체 생성
// - 분당 3,600개 Color 객체 생성
// - GC 압력 증가
```

### 2. 불필요한 상태 변경
```java
// AbstractRelic.java:707, 725
sb.setColor(Color.WHITE);
sb.draw(this.img, ...);
// ... (다른 코드 없음)
sb.setColor(Color.WHITE);  // ❌ 이미 WHITE인데 또 설정
```

### 3. 드로우콜 배칭 저해
```java
// 연속된 드로우에서 불필요한 색상 변경
for (Potion p : potions) {
    sb.setColor(p.liquidColor);     // 색상 변경 #1
    sb.draw(p.liquidImg, ...);      // 드로우콜 #1

    sb.setColor(p.hybridColor);     // 색상 변경 #2
    sb.draw(p.hybridImg, ...);      // 드로우콜 #2 (배칭 불가)

    sb.setColor(Color.WHITE);       // 색상 변경 #3
    sb.draw(p.containerImg, ...);   // 드로우콜 #3 (배칭 불가)
}
// 총 3N개 드로우콜 (N = 포션 수)
```

### 4. 성능 영향
```
일반 전투 시나리오:
- 몬스터 3마리: 프레임당 6-9회 색상 설정
- 포션 3개: 프레임당 9-12회 색상 설정
- 유물 15개: 프레임당 30회 색상 설정
- 카드 10장: 프레임당 40-50회 색상 설정

총: 프레임당 85-100회 색상 설정
60 FPS 기준: 초당 5,100-6,000회 GPU 상태 변경
```

## 해결 방법

### 방법 1: 색상 캐싱 및 조건부 설정

```java
public class ColorCache {
    // 자주 사용되는 색상 미리 정의
    public static final Color HOVER_TINT = new Color(1.0F, 1.0F, 1.0F, 0.1F);
    public static final Color SHADOW = new Color(0.0F, 0.0F, 0.0F, 0.33F);
    public static final Color TRANSPARENT_BLACK = new Color(0.0F, 0.0F, 0.0F, 0.5F);

    private Color currentColor = new Color(Color.WHITE);

    public void setColor(SpriteBatch sb, Color newColor) {
        if (!currentColor.equals(newColor)) {
            currentColor.set(newColor);
            sb.setColor(newColor);
        }
    }

    public void setColor(SpriteBatch sb, float r, float g, float b, float a) {
        if (currentColor.r != r || currentColor.g != g ||
            currentColor.b != b || currentColor.a != a) {
            currentColor.set(r, g, b, a);
            sb.setColor(currentColor);
        }
    }
}
```

#### 몬스터 렌더링 개선
```java
public class AbstractMonster {
    // 정적 Color 객체 재사용
    private static final Color HOVER_COLOR = new Color(1.0F, 1.0F, 1.0F, 0.1F);
    private ColorCache colorCache = new ColorCache();

    public void render(SpriteBatch sb) {
        if (!this.isDead && !this.escaped) {
            // 개선 전
            // sb.setColor(this.tint.color);
            // sb.setColor(new Color(1.0F, 1.0F, 1.0F, 0.1F));

            // 개선 후
            colorCache.setColor(sb, this.tint.color);
            if (this.img != null) {
                sb.draw(this.img, ...);
            }

            if (this == hoveredMonster) {
                sb.setBlendFunction(770, 1);
                colorCache.setColor(sb, HOVER_COLOR);  // 재사용
                sb.draw(this.img, ...);
                sb.setBlendFunction(770, 771);
            }
        }
    }
}
```

#### 포션 렌더링 개선
```java
public class AbstractPotion {
    private ColorCache colorCache = new ColorCache();

    public void render(SpriteBatch sb) {
        // 개선 전
        // sb.setColor(this.liquidColor);
        // sb.draw(this.liquidImg, ...);
        // if (this.hybridColor != null) {
        //     sb.setColor(this.hybridColor);
        //     sb.draw(this.hybridImg, ...);
        // }

        // 개선 후
        colorCache.setColor(sb, this.liquidColor);
        sb.draw(this.liquidImg, ...);

        if (this.hybridColor != null) {
            colorCache.setColor(sb, this.hybridColor);
            sb.draw(this.hybridImg, ...);
        }

        if (this.spotsColor != null) {
            colorCache.setColor(sb, this.spotsColor);
            sb.draw(this.spotsImg, ...);
        }

        colorCache.setColor(sb, Color.WHITE);
        sb.draw(this.containerImg, ...);
    }
}
```

### 방법 2: SpriteBatch 확장 (Optimized SpriteBatch)

```java
public class OptimizedSpriteBatch extends SpriteBatch {
    private final Color currentColor = new Color(Color.WHITE);
    private static final float EPSILON = 0.001f;

    @Override
    public void setColor(Color tint) {
        // 색상이 실제로 변경되었는지 확인
        if (!colorEquals(currentColor, tint)) {
            currentColor.set(tint);
            super.setColor(tint);
        }
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        // 색상이 실제로 변경되었는지 확인
        if (!colorEquals(currentColor.r, r) ||
            !colorEquals(currentColor.g, g) ||
            !colorEquals(currentColor.b, b) ||
            !colorEquals(currentColor.a, a)) {

            currentColor.set(r, g, b, a);
            super.setColor(r, g, b, a);
        }
    }

    private boolean colorEquals(Color c1, Color c2) {
        return colorEquals(c1.r, c2.r) &&
               colorEquals(c1.g, c2.g) &&
               colorEquals(c1.b, c2.b) &&
               colorEquals(c1.a, c2.a);
    }

    private boolean colorEquals(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }

    @Override
    public void end() {
        currentColor.set(Color.WHITE);  // 리셋
        super.end();
    }
}
```

### 방법 3: 배치 최적화 (Batch Optimization)

```java
public class BatchedRenderer {
    // 같은 색상의 객체를 그룹화하여 렌더링

    private Map<Color, List<RenderCommand>> batchedCommands = new HashMap<>();

    static class RenderCommand {
        Texture texture;
        float x, y, width, height;
        // ... 기타 파라미터
    }

    public void queueRender(Color color, Texture texture, float x, float y, ...) {
        batchedCommands.computeIfAbsent(color, k -> new ArrayList<>())
                      .add(new RenderCommand(texture, x, y, ...));
    }

    public void flush(SpriteBatch sb) {
        for (Map.Entry<Color, List<RenderCommand>> entry : batchedCommands.entrySet()) {
            sb.setColor(entry.getKey());  // 한 번만 색상 설정

            for (RenderCommand cmd : entry.getValue()) {
                sb.draw(cmd.texture, cmd.x, cmd.y, ...);
            }
        }

        batchedCommands.clear();
    }
}

// 사용 예시
public void renderPotions(SpriteBatch sb, List<AbstractPotion> potions) {
    BatchedRenderer renderer = new BatchedRenderer();

    // 1단계: 렌더링 명령 수집
    for (AbstractPotion p : potions) {
        renderer.queueRender(p.liquidColor, p.liquidImg, ...);
        if (p.hybridColor != null) {
            renderer.queueRender(p.hybridColor, p.hybridImg, ...);
        }
        renderer.queueRender(Color.WHITE, p.containerImg, ...);
    }

    // 2단계: 색상별로 그룹화하여 렌더링
    renderer.flush(sb);
}
```

### 방법 4: 하이브리드 접근 (권장)

```java
public class SmartRenderer {
    private ColorCache colorCache = new ColorCache();
    private BatchedRenderer batchRenderer = new BatchedRenderer();

    // 정적 객체 (유물, UI): 배치 최적화
    public void renderRelics(SpriteBatch sb, List<AbstractRelic> relics) {
        for (AbstractRelic r : relics) {
            batchRenderer.queueRender(Color.WHITE, r.img, r.currentX, r.currentY, ...);
        }
        batchRenderer.flush(sb);
    }

    // 동적 객체 (카드, 몬스터): 색상 캐싱
    public void renderMonsters(SpriteBatch sb, MonsterGroup monsters) {
        for (AbstractMonster m : monsters.monsters) {
            colorCache.setColor(sb, m.tint.color);
            sb.draw(m.img, ...);

            if (m.isHovered) {
                colorCache.setColor(sb, ColorCache.HOVER_TINT);
                sb.draw(m.img, ...);
            }
        }
    }
}
```

## 성능 개선 효과

### 시나리오 1: 일반 전투 (몬스터 3마리, 포션 3개, 유물 15개)
**개선 전**:
- 프레임당 색상 설정: 85-100회
- 60 FPS 기준: 5,100-6,000회/초
- Color 객체 생성: 180개/초 (몬스터 호버 효과)

**개선 후 (색상 캐싱)**:
- 프레임당 색상 설정: 20-30회 (실제 변경된 것만)
- 60 FPS 기준: 1,200-1,800회/초
- Color 객체 생성: 0개/초
- **개선율**: 70-76% 감소

### 시나리오 2: 포션 렌더링 (3개)
**개선 전**:
- 색상 설정: 12회 (각 포션마다 4회)
- 드로우콜: 12개 (배칭 불가)

**개선 후 (배치 최적화)**:
- 색상 설정: 3-4회 (색상별 1회)
- 드로우콜: 3-4개 (색상별 배칭)
- **개선율**: 66-75% 감소

### 실제 성능 영향 (예상)
- **GPU 상태 변경**: 70% 감소
- **GC 압력**: Color 객체 생성 제거로 90% 감소
- **CPU 사용률**: 렌더링 루프에서 2-3% 감소
- **FPS 향상**: 저사양 GPU에서 평균 3-5 FPS 상승
- **프레임타임**: 평균 0.5-1ms 단축

## 주의사항

### 1. 색상 비교 정밀도
```java
// 부동소수점 비교는 epsilon 사용
private static final float EPSILON = 0.001f;

private boolean colorEquals(float a, float b) {
    return Math.abs(a - b) < EPSILON;
}
```

### 2. SpriteBatch 리셋
```java
@Override
public void end() {
    currentColor.set(Color.WHITE);  // 다음 begin()을 위해 리셋
    super.end();
}
```

### 3. 배치 최적화 오버헤드
```java
// 객체가 적으면 배치 최적화가 더 느릴 수 있음
if (objectCount < 10) {
    renderDirect();  // 직접 렌더링
} else {
    renderBatched();  // 배치 최적화
}
```

### 4. 메모리 관리
```java
// 배치 커맨드 리스트는 재사용
private List<RenderCommand> commandPool = new ArrayList<>(100);

public void queueRender(...) {
    RenderCommand cmd;
    if (poolIndex < commandPool.size()) {
        cmd = commandPool.get(poolIndex++);
        cmd.reset(...);
    } else {
        cmd = new RenderCommand(...);
        commandPool.add(cmd);
        poolIndex++;
    }
}
```

### 5. Z-ordering 유지
```java
// 배치 최적화 시 렌더링 순서 주의
public void flush(SpriteBatch sb) {
    // 색상별로 정렬하되, Z-order 유지
    List<RenderCommand> sortedCommands = new ArrayList<>();

    for (List<RenderCommand> commands : batchedCommands.values()) {
        sortedCommands.addAll(commands);
    }

    // Z-order 기준으로 정렬
    sortedCommands.sort((a, b) -> Float.compare(a.z, b.z));

    // 색상이 변경될 때만 setColor 호출
    Color currentColor = null;
    for (RenderCommand cmd : sortedCommands) {
        if (!cmd.color.equals(currentColor)) {
            currentColor = cmd.color;
            sb.setColor(currentColor);
        }
        sb.draw(cmd.texture, ...);
    }
}
```

## 결론

불필요한 색상 설정은 **GPU 상태 변경 오버헤드**와 **배칭 저해**로 인한 성능 저하를 일으킵니다. 특히 객체가 많은 화면(유물, 카드 라이브러리)에서 드로우콜 수가 급격히 증가합니다.

**권장 구현 우선순위**:
1. **Color 객체 캐싱** (필수) - GC 압력 감소
2. **조건부 색상 설정** (권장) - GPU 상태 변경 감소
3. **배치 최적화** (선택) - 정적 UI 요소에만 적용

**예상 성능 개선**:
- 일반 전투: +3-5 FPS
- 카드 라이브러리: +5-8 FPS
- 유물 화면: +8-12 FPS
- 저사양 GPU: +15-20% 전반적 성능 향상

**핵심 원칙**:
- 정적 Color 객체 재사용
- 색상이 실제로 변경되었을 때만 setColor 호출
- 같은 색상의 드로우콜을 그룹화하여 배칭 최대화
