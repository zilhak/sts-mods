# Performance Issue: Glow Effect Overhead

## 문제 발견 위치
- **파일**: `AbstractCard.java`, `AbstractRelic.java`
- **메서드**: `updateGlow()`, `renderGlow()`, `renderFlash()`
- **라인**:
  - AbstractCard.java: 1180-1197 (updateGlow)
  - AbstractCard.java: 1245-1246, 1296-1297 (renderGlow 호출)
  - AbstractRelic.java: 1256-1322 (renderFlash)

## 문제 설명

### 발견된 패턴

#### 카드 Glow 효과
```java
// AbstractCard.java:1180-1197
private void updateGlow() {
    if (this.isGlowing) {
        this.glowTimer -= Gdx.graphics.getDeltaTime();
        if (this.glowTimer < 0.0F) {
            this.glowList.add(new CardGlowBorder(this, this.glowColor));  // ❌ 매 0.3초마다 객체 생성
            this.glowTimer = 0.3F;
        }
    }

    for (Iterator<CardGlowBorder> i = this.glowList.iterator(); i.hasNext(); ) {
        CardGlowBorder e = i.next();
        e.update();  // ❌ 매 프레임 업데이트

        if (e.isDone) {
            i.remove();
        }
    }
}

// 렌더링 시 (AbstractCard.java:1296-1297)
updateGlow();     // ❌ 매 프레임 호출
renderGlow(sb);   // ❌ 모든 glow 객체 렌더링
```

#### 유물 Flash 효과
```java
// AbstractRelic.java:1256-1322
public void renderFlash(SpriteBatch sb, boolean inTopPanel) {
    float tmp = Interpolation.exp10In.apply(0.0F, 4.0F, this.flashTimer / 2.0F);

    sb.setBlendFunction(770, 1);
    this.flashColor.a = this.flashTimer * 0.2F;
    sb.setColor(this.flashColor);

    // ❌ 같은 이미지를 3번 그림 (각기 다른 크기)
    sb.draw(this.img, tmpX, tmpY, 64.0F, 64.0F, 128.0F, 128.0F,
            this.scale + tmp, this.scale + tmp, ...);  // 드로우 #1

    sb.draw(this.img, tmpX, tmpY, 64.0F, 64.0F, 128.0F, 128.0F,
            this.scale + tmp * 0.66F, this.scale + tmp * 0.66F, ...);  // 드로우 #2

    sb.draw(this.img, tmpX, tmpY, 64.0F, 64.0F, 128.0F, 128.0F,
            this.scale + tmp / 3.0F, this.scale + tmp / 3.0F, ...);  // 드로우 #3

    sb.setBlendFunction(770, 771);
}
```

### 문제점
1. **객체 생성 빈도**: 카드 1장당 초당 3.3개 `CardGlowBorder` 생성
2. **불필요한 업데이트**: 모든 glow 효과가 매 프레임 업데이트
3. **과다 드로우콜**: Flash 효과 시 같은 이미지를 3번 그림
4. **블렌드 모드 전환**: Flash 렌더링마다 블렌드 모드 변경

## 원인 분석

### 1. Glow 효과 객체 생성
```java
// 플레이 가능한 카드 10장이 모두 빛나는 경우
// 초당 생성되는 CardGlowBorder 객체:
// 10 cards × (1 / 0.3s) = 33.3개/초
// 분당: 약 2,000개

// 각 CardGlowBorder는 다음을 포함:
// - Color 객체
// - 타이머/애니메이션 상태
// - 참조 데이터

// 60 FPS 기준:
// - 매 프레임 10-15개 glow 객체 업데이트
// - GC 압력 증가
```

### 2. Flash 효과 오버헤드
```java
// 유물 15개가 모두 깜빡이는 경우 (레벨업 등)
// 프레임당:
// - 블렌드 모드 전환: 30회 (15 × 2)
// - 드로우콜: 45개 (15 × 3)
// - 보간 계산: 15회

// 60 FPS 기준:
// - 초당 블렌드 모드 전환: 1,800회
// - 초당 드로우콜: 2,700개
```

### 3. 화면 밖 효과 렌더링
```java
// 화면 밖 카드도 glow 효과 업데이트
for (AbstractCard card : allCards) {
    if (card.isGlowing) {
        updateGlow();  // ❌ 화면 밖 카드도 업데이트
        renderGlow();  // ❌ 화면 밖 카드도 렌더링
    }
}

// 덱 70장 중 10장만 화면에 보이는 경우
// 60장의 불필요한 glow 업데이트
```

### 4. 성능 영향
```
최악의 시나리오 (레벨업 직후):
- 빛나는 카드: 20장 (업그레이드 가능)
- 깜빡이는 유물: 15개 (모든 유물)
- 빛나는 포션: 3개

프레임당:
- CardGlowBorder 생성: 0.33개 (평균)
- Glow 객체 업데이트: 20-30개
- Flash 드로우콜: 45개 (유물만)
- 블렌드 모드 전환: 40-50회

60 FPS 기준:
- 초당 객체 생성: 20개
- 초당 드로우콜: 2,700개
- 초당 블렌드 전환: 2,400-3,000회
```

## 해결 방법

### 방법 1: Glow 효과 풀링 (Object Pooling)

```java
public class GlowEffectPool {
    private static final int POOL_SIZE = 50;
    private List<CardGlowBorder> pool = new ArrayList<>(POOL_SIZE);
    private int activeCount = 0;

    public GlowEffectPool() {
        // 미리 객체 생성
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.add(new CardGlowBorder());
        }
    }

    public CardGlowBorder obtain(AbstractCard card, Color glowColor) {
        CardGlowBorder glow;

        if (activeCount < pool.size()) {
            glow = pool.get(activeCount);
            glow.reset(card, glowColor);
            activeCount++;
        } else {
            // 풀이 가득 찼으면 기존 객체 재사용 (LRU)
            glow = pool.get(0);
            glow.reset(card, glowColor);
        }

        return glow;
    }

    public void release(CardGlowBorder glow) {
        // 객체를 풀로 반환 (실제로는 이미 풀에 있음)
        activeCount = Math.max(0, activeCount - 1);
    }

    public void update() {
        // 활성 객체만 업데이트
        for (int i = 0; i < activeCount; i++) {
            pool.get(i).update();
        }
    }
}

// 사용 예시
public class AbstractCard {
    private static GlowEffectPool glowPool = new GlowEffectPool();

    private void updateGlow() {
        if (this.isGlowing) {
            this.glowTimer -= Gdx.graphics.getDeltaTime();
            if (this.glowTimer < 0.0F) {
                // 개선 전: new CardGlowBorder(this, this.glowColor)
                // 개선 후: 풀에서 가져오기
                glowPool.obtain(this, this.glowColor);
                this.glowTimer = 0.3F;
            }
        }

        glowPool.update();  // 활성 객체만 업데이트
    }
}
```

### 방법 2: Flash 효과 통합 렌더링

```java
public class OptimizedFlashRenderer {
    // Flash 효과를 한 번에 렌더링

    private List<FlashCommand> flashQueue = new ArrayList<>();

    static class FlashCommand {
        Texture img;
        float x, y, scale, flashTimer;
        Color flashColor;

        void reset(Texture img, float x, float y, float scale,
                  float flashTimer, Color flashColor) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.flashTimer = flashTimer;
            this.flashColor = flashColor;
        }
    }

    public void queueFlash(Texture img, float x, float y, float scale,
                          float flashTimer, Color flashColor) {
        FlashCommand cmd = new FlashCommand();
        cmd.reset(img, x, y, scale, flashTimer, flashColor);
        flashQueue.add(cmd);
    }

    public void renderAll(SpriteBatch sb) {
        if (flashQueue.isEmpty()) return;

        // 블렌드 모드 한 번만 전환
        sb.setBlendFunction(770, 1);

        for (FlashCommand cmd : flashQueue) {
            float tmp = Interpolation.exp10In.apply(0.0F, 4.0F,
                                                   cmd.flashTimer / 2.0F);

            cmd.flashColor.a = cmd.flashTimer * 0.2F;
            sb.setColor(cmd.flashColor);

            // 3번 그리기를 그대로 유지
            sb.draw(cmd.img, cmd.x, cmd.y, 64.0F, 64.0F, 128.0F, 128.0F,
                   cmd.scale + tmp, cmd.scale + tmp, ...);

            sb.draw(cmd.img, cmd.x, cmd.y, 64.0F, 64.0F, 128.0F, 128.0F,
                   cmd.scale + tmp * 0.66F, cmd.scale + tmp * 0.66F, ...);

            sb.draw(cmd.img, cmd.x, cmd.y, 64.0F, 64.0F, 128.0F, 128.0F,
                   cmd.scale + tmp / 3.0F, cmd.scale + tmp / 3.0F, ...);
        }

        // 블렌드 모드 복원 (한 번만)
        sb.setBlendFunction(770, 771);

        flashQueue.clear();
    }
}

// 사용 예시
public void renderRelics(SpriteBatch sb, List<AbstractRelic> relics) {
    OptimizedFlashRenderer flashRenderer = new OptimizedFlashRenderer();

    // 일반 렌더링
    for (AbstractRelic r : relics) {
        r.renderNormal(sb);

        if (r.flashTimer > 0) {
            flashRenderer.queueFlash(r.img, r.currentX, r.currentY,
                                    r.scale, r.flashTimer, r.flashColor);
        }
    }

    // Flash 효과 일괄 렌더링
    flashRenderer.renderAll(sb);
}
```

### 방법 3: Shader 기반 Glow 효과

```java
// GLSL Shader (glow.frag)
/*
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform float u_intensity;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);

    // Glow 계산
    float glow = sin(u_time * 3.0) * 0.5 + 0.5;
    vec3 glowColor = vec3(1.0, 0.8, 0.3) * glow * u_intensity;

    // 원본 색상과 합성
    gl_FragColor = texColor + vec4(glowColor, 0.0) * texColor.a;
}
*/

public class ShaderGlowRenderer {
    private ShaderProgram glowShader;
    private float time = 0;

    public ShaderGlowRenderer() {
        glowShader = new ShaderProgram(
            Gdx.files.internal("shaders/default.vert"),
            Gdx.files.internal("shaders/glow.frag")
        );
    }

    public void render(SpriteBatch sb, List<AbstractCard> cards) {
        time += Gdx.graphics.getDeltaTime();

        // Shader 활성화
        sb.setShader(glowShader);
        glowShader.setUniformf("u_time", time);

        for (AbstractCard card : cards) {
            if (card.isGlowing) {
                glowShader.setUniformf("u_intensity", 0.5f);
                card.renderNormal(sb);  // Shader가 자동으로 glow 적용
            }
        }

        // Shader 비활성화
        sb.setShader(null);
    }
}
```

### 방법 4: 하이브리드 접근 (권장)

```java
public class SmartGlowRenderer {
    private GlowEffectPool glowPool = new GlowEffectPool();
    private OptimizedFlashRenderer flashRenderer = new OptimizedFlashRenderer();
    private ShaderGlowRenderer shaderGlow = null;  // 선택적 사용

    public void render(SpriteBatch sb, List<AbstractCard> cards,
                      List<AbstractRelic> relics) {

        // 1. 카드 glow: Shader 또는 Pool 사용
        if (Settings.USE_SHADER_EFFECTS && shaderGlow != null) {
            shaderGlow.render(sb, cards);
        } else {
            glowPool.update();
            for (AbstractCard card : cards) {
                if (card.isOnScreen()) {  // ✅ 화면 안만
                    card.renderWithGlow(sb);
                }
            }
        }

        // 2. 유물 flash: 통합 렌더링
        for (AbstractRelic r : relics) {
            r.renderNormal(sb);

            if (r.flashTimer > 0 && r.isOnScreen()) {
                flashRenderer.queueFlash(r.img, r.currentX, r.currentY,
                                        r.scale, r.flashTimer, r.flashColor);
            }
        }

        flashRenderer.renderAll(sb);
    }
}
```

## 성능 개선 효과

### 시나리오 1: 빛나는 카드 20장
**개선 전**:
- 초당 객체 생성: 66개 CardGlowBorder
- 분당 객체 생성: 약 4,000개
- GC 압력: 높음

**개선 후 (풀링)**:
- 초당 객체 생성: 0개 (재사용)
- 풀 크기: 50개 (고정)
- GC 압력: 거의 없음
- **개선율**: 100% 객체 생성 제거

### 시나리오 2: 깜빡이는 유물 15개
**개선 전**:
- 블렌드 모드 전환: 30회/프레임
- 드로우콜: 45개/프레임
- 60 FPS 기준: 1,800회 블렌드 전환/초

**개선 후 (통합 렌더링)**:
- 블렌드 모드 전환: 2회/프레임
- 드로우콜: 45개/프레임 (동일)
- 60 FPS 기준: 120회 블렌드 전환/초
- **개선율**: 93.3% 블렌드 전환 감소

### 시나리오 3: Shader 기반 Glow (고급)
**개선 전**:
- 객체 생성 + 업데이트: 매 프레임
- 드로우콜: 카드당 2개 (원본 + glow)

**개선 후 (Shader)**:
- 객체 생성: 0개
- 드로우콜: 카드당 1개 (Shader가 자동 처리)
- **개선율**: 50% 드로우콜 감소

### 실제 성능 영향 (예상)
- **GC 압력**: 80-90% 감소 (풀링)
- **블렌드 전환**: 90% 감소 (통합 렌더링)
- **CPU 사용률**: 3-5% 감소
- **FPS 향상**: 저사양 PC에서 평균 5-8 FPS 상승
- **메모리 사용**: 10-15% 감소 (풀링)

## 주의사항

### 1. 풀 크기 조정
```java
// 너무 크면 메모리 낭비
private static final int POOL_SIZE = 50;  // ✅ 적당

// 너무 작으면 효과 없음
private static final int POOL_SIZE = 10;  // ❌ 부족
```

### 2. Shader 호환성
```java
// 모바일/저사양 GPU는 Shader 지원 확인
if (Settings.USE_SHADER_EFFECTS &&
    Gdx.graphics.supportsExtension("GL_OES_standard_derivatives")) {
    useShaderGlow = true;
} else {
    useShaderGlow = false;  // 폴백
}
```

### 3. Flash 효과 순서
```java
// Z-order 유지 필요
flashQueue.sort((a, b) -> Float.compare(a.z, b.z));
```

### 4. 화면 밖 필터링
```java
// Glow 업데이트 전에 화면 밖 검사
if (card.isOnScreen()) {
    updateGlow();
    renderGlow();
}
```

### 5. 풀 리셋
```java
// 씬 전환 시 풀 초기화
public void dispose() {
    glowPool.clear();
    flashRenderer.clear();
}
```

## 결론

Glow 및 Flash 효과는 **시각적으로 화려하지만 성능 비용이 높습니다**. 특히 많은 객체가 동시에 빛날 때 GC 압력과 드로우콜이 급증합니다.

**권장 구현 우선순위**:
1. **객체 풀링** (필수) - GC 압력 제거
2. **통합 렌더링** (권장) - 블렌드 전환 감소
3. **화면 밖 필터링** (필수) - 불필요한 업데이트 제거
4. **Shader 기반** (선택) - 고급 최적화

**예상 성능 개선**:
- 레벨업 화면: +8-12 FPS
- 카드 선택 화면: +5-8 FPS
- 일반 전투: +3-5 FPS
- GC 빈도: 50-70% 감소

**핵심 원칙**:
- Glow 객체는 풀에서 재사용
- Flash 효과는 그룹화하여 렌더링
- 화면 밖 효과는 업데이트하지 않음
- Shader로 대체 가능한 효과는 GPU로 이동
