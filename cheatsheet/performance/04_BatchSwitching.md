# Performance Issue: Frequent SpriteBatch Switching

## 문제 발견 위치
- **파일**: `AbstractMonster.java`, `AbstractPlayer.java`, `Watcher.java`, `AnimatedNpc.java`
- **메서드**: `render(SpriteBatch sb)`
- **라인**:
  - AbstractMonster.java: 886-891
  - AbstractPlayer.java: 2165-2169
  - Watcher.java: 438-443
  - AnimatedNpc.java: 48-53, 62-67

## 문제 설명

### 발견된 패턴
매 프레임마다 Spine 애니메이션 렌더링을 위해 SpriteBatch를 전환하는 패턴이 반복됨:

```java
// AbstractMonster.java:886-891
sb.end();                           // 기본 SpriteBatch 종료
CardCrawlGame.psb.begin();         // Spine용 SpriteBatch 시작
sr.draw(CardCrawlGame.psb, this.skeleton);
CardCrawlGame.psb.end();           // Spine SpriteBatch 종료
sb.begin();                        // 기본 SpriteBatch 재시작
sb.setBlendFunction(770, 771);     // 블렌드 모드 복원
```

이 패턴이 적용되는 위치:
- **모든 몬스터 렌더링** (매 프레임마다 몬스터 수만큼 반복)
- **플레이어 캐릭터 렌더링** (Spine 애니메이션 사용 시)
- **NPC 렌더링** (이벤트 씬 등)

## 원인 분석

### 1. Spine 애니메이션 렌더링 구조
- **Spine 라이브러리**: 별도의 SpriteBatch (`CardCrawlGame.psb`) 필요
- **이유**: Spine은 자체 셰이더와 블렌딩 모드를 사용
- **문제점**: 각 몬스터마다 개별적으로 Batch 전환 발생

### 2. 성능 영향
```
전투 중 몬스터 3마리 가정:
- 일반 이미지: Batch 전환 없음
- Spine 몬스터 3마리: 프레임당 6회 Batch 전환
  (각 몬스터마다 end → begin → end → begin)

60fps 기준:
- 초당 360회 SpriteBatch 전환
- 분당 21,600회 SpriteBatch 전환
```

### 3. SpriteBatch 전환 비용
각 전환마다 발생하는 작업:
- **GPU 상태 변경**: 셰이더, 블렌딩 모드, 텍스처 바인딩
- **드로우콜 플러시**: 버퍼에 쌓인 모든 드로우콜 즉시 실행
- **컨텍스트 스위칭**: CPU-GPU 통신 오버헤드

## 해결 방법

### 방법 1: 배치 그룹화 (Batch Grouping)
**개념**: 같은 타입의 렌더링을 한 번에 처리

```java
// 현재 구조 (비효율)
for (Monster m : monsters) {
    if (m.hasSpineAnimation) {
        sb.end();
        CardCrawlGame.psb.begin();
        renderSpine(m);
        CardCrawlGame.psb.end();
        sb.begin();
    }
}

// 개선된 구조
// 1단계: 일반 이미지 렌더링
for (Monster m : monsters) {
    if (!m.hasSpineAnimation) {
        renderNormalImage(m);
    }
}

// 2단계: Spine 애니메이션 렌더링 (한 번에)
sb.end();
CardCrawlGame.psb.begin();
for (Monster m : monsters) {
    if (m.hasSpineAnimation) {
        renderSpine(m);
    }
}
CardCrawlGame.psb.end();
sb.begin();
```

**장점**:
- 몬스터 3마리 → 2회 전환으로 감소 (6회 → 2회)
- 드로우콜 최적화 가능

**단점**:
- Z-ordering (그리기 순서) 관리 복잡
- 렌더링 순서 변경으로 인한 시각적 차이 가능

### 방법 2: 프레임버퍼 캐싱 (Framebuffer Caching)
**개념**: Spine 애니메이션을 텍스처로 렌더링 후 재사용

```java
// 애니메이션이 변경되지 않았다면 캐시된 텍스처 사용
public class CachedSpineRenderer {
    private FrameBuffer fbo;
    private Texture cachedTexture;
    private String lastAnimationState;
    private float cacheTimer = 0f;
    private static final float CACHE_DURATION = 0.1f; // 100ms

    public void render(SpriteBatch sb, Skeleton skeleton) {
        String currentState = skeleton.getData().getName();

        // 애니메이션이 변경되었거나 캐시 만료
        if (!currentState.equals(lastAnimationState) ||
            cacheTimer <= 0) {
            updateCache(skeleton);
            cacheTimer = CACHE_DURATION;
            lastAnimationState = currentState;
        }

        // 캐시된 텍스처 그리기 (Batch 전환 없음)
        sb.draw(cachedTexture, x, y, width, height);
        cacheTimer -= Gdx.graphics.getDeltaTime();
    }

    private void updateCache(Skeleton skeleton) {
        fbo.begin();
        CardCrawlGame.psb.begin();
        sr.draw(CardCrawlGame.psb, skeleton);
        CardCrawlGame.psb.end();
        fbo.end();
        cachedTexture = fbo.getColorBufferTexture();
    }
}
```

**장점**:
- Batch 전환 거의 제거 (캐시 업데이트 시에만 발생)
- 같은 애니메이션 프레임 재사용 시 성능 향상

**단점**:
- 메모리 사용량 증가 (각 몬스터마다 FBO)
- 애니메이션 부드러움 저하 (캐시 간격에 따라)
- 모바일 환경에서 VRAM 압박

### 방법 3: 하이브리드 접근 (권장)
**개념**: 상황에 따라 다른 최적화 적용

```java
public class OptimizedMonsterRenderer {
    private boolean useGrouping = true;
    private Map<Monster, CachedSpineRenderer> cacheMap = new HashMap<>();

    public void render(SpriteBatch sb, List<Monster> monsters) {
        // 정적 몬스터(움직임 적음) → 캐싱
        // 동적 몬스터(움직임 많음) → 그룹화

        List<Monster> staticMonsters = new ArrayList<>();
        List<Monster> dynamicMonsters = new ArrayList<>();

        for (Monster m : monsters) {
            if (m.isStatic() || m.isDying()) {
                staticMonsters.add(m);
            } else {
                dynamicMonsters.add(m);
            }
        }

        // 정적 몬스터: 캐시 사용
        for (Monster m : staticMonsters) {
            getCachedRenderer(m).render(sb, m.skeleton);
        }

        // 동적 몬스터: 그룹화 렌더링
        if (!dynamicMonsters.isEmpty()) {
            sb.end();
            CardCrawlGame.psb.begin();
            for (Monster m : dynamicMonsters) {
                sr.draw(CardCrawlGame.psb, m.skeleton);
            }
            CardCrawlGame.psb.end();
            sb.begin();
        }
    }
}
```

## 성능 개선 효과

### 시나리오 1: 3마리 Spine 몬스터 전투
**개선 전**:
- 프레임당 Batch 전환: 6회
- 60fps 기준: 360회/초

**개선 후 (그룹화)**:
- 프레임당 Batch 전환: 2회
- 60fps 기준: 120회/초
- **개선율**: 66.7% 감소

### 시나리오 2: 보스 전투 (Spine 애니메이션 보스 1마리)
**개선 전**:
- 프레임당 Batch 전환: 2회

**개선 후 (캐싱, 100ms 간격)**:
- 초당 Batch 전환: 10회 (1초에 10번 캐시 업데이트)
- **개선율**: 91.7% 감소 (60fps 기준)

### 실제 성능 영향 (예상)
- **CPU 사용률**: 5-10% 감소 (렌더링 오버헤드)
- **프레임 드롭**: 심한 전투에서 10-15% 개선
- **저사양 PC**: 평균 FPS 45 → 52 (+15%)

## 주의사항

### 1. Z-ordering 문제
```java
// 그룹화 시 렌더링 순서 관리 필요
monsters.sort((m1, m2) -> Float.compare(m1.drawY, m2.drawY));
```

### 2. 블렌드 모드 복원
```java
// Spine 렌더링 후 반드시 블렌드 모드 복원
CardCrawlGame.psb.end();
sb.begin();
sb.setBlendFunction(770, 771); // 기본 블렌딩 복원
```

### 3. 메모리 관리 (캐싱 사용 시)
```java
// FBO 크기 최적화
int fboWidth = Math.min(monsterWidth, 512);
int fboHeight = Math.min(monsterHeight, 512);

// 사용하지 않는 캐시 정리
if (monster.isDead) {
    cacheMap.get(monster).dispose();
    cacheMap.remove(monster);
}
```

### 4. 모바일 최적화
```java
// 저사양 기기에서는 캐싱 비활성화
if (Settings.isMobile && !Settings.isHighPerformanceMode) {
    useGrouping = true;
    useCaching = false;
}
```

## 결론

SpriteBatch 전환은 Slay the Spire의 주요 성능 병목 중 하나입니다. Spine 애니메이션의 특성상 완전히 제거할 수는 없지만, **그룹화**와 **선택적 캐싱**을 통해 최대 90%까지 감소시킬 수 있습니다.

**권장 구현 우선순위**:
1. 배치 그룹화 (구현 쉬움, 효과 중간)
2. 정적 객체 캐싱 (메모리 여유 시)
3. 하이브리드 접근 (최적의 성능)
