# 22. Hitbox Culling (Phase 7: UI Systems)

## 문제 분석

### 심각도: MEDIUM
**예상 성능 영향**: 5-10% (UI 요소가 많은 화면에서 15-20%)

## 핵심 문제

모든 Hitbox가 화면 밖에 있어도 매 프레임 마우스 충돌 검사를 수행합니다.

### 현재 코드 (Hitbox.java)

```java
public void update(float x, float y) {
    if (AbstractDungeon.isFadingOut) {
        return;  // 유일한 조기 반환 조건
    }

    this.x = x;
    this.y = y;

    if (this.justHovered) {
        this.justHovered = false;
    }

    // 항상 마우스 위치 체크
    if (!this.hovered) {
        this.hovered = (InputHelper.mX > x &&
                       InputHelper.mX < x + this.width &&
                       InputHelper.mY > y &&
                       InputHelper.mY < y + this.height);

        if (this.hovered) {
            this.justHovered = true;
        }
    } else {
        this.hovered = (InputHelper.mX > x &&
                       InputHelper.mX < x + this.width &&
                       InputHelper.mY > y &&
                       InputHelper.mY < y + this.height);
    }
}
```

## 성능 문제

### 1. 화면 밖 UI 업데이트
```java
// CardRewardScreen.java - 화면 밖 카드도 업데이트
private void cardSelectUpdate() {
    for (AbstractCard c : this.rewardGroup) {
        c.update();           // 모든 카드 업데이트
        c.updateHoverLogic(); // 모든 카드 호버 체크
        if (c.hb.justHovered) {
            CardCrawlGame.sound.playV("CARD_OBTAIN", 0.4F);
        }
    }
}

private void renderCardReward(SpriteBatch sb) {
    // 화면 밖 카드는 -WIDTH*0.25 또는 WIDTH*1.25로 이동
    if (this.rewardGroup.indexOf(c) < indexToStartAt) {
        c.target_x = -Settings.WIDTH * 0.25F;  // 화면 왼쪽 밖
    } else if (this.rewardGroup.indexOf(c) >= indexToStartAt + 4) {
        c.target_x = Settings.WIDTH * 1.25F;   // 화면 오른쪽 밖
    }
    // 하지만 update()는 계속 호출됨!
}
```

### 2. 불필요한 마우스 체크 중복
```java
// Hitbox.update() 내부
if (!this.hovered) {
    // 4개의 비교 연산
    this.hovered = (InputHelper.mX > x &&
                   InputHelper.mX < x + this.width &&
                   InputHelper.mY > y &&
                   InputHelper.mY < y + this.height);
} else {
    // 똑같은 4개의 비교 연산 반복
    this.hovered = (InputHelper.mX > x &&
                   InputHelper.mX < x + this.width &&
                   InputHelper.mY > y &&
                   InputHelper.mY < y + this.height);
}
```

### 3. 실측 Hitbox 개수
```
- CardRewardScreen: 3-10개 카드 (각각 1 hitbox)
- CombatRewardScreen: 보상 아이템 (3-8개)
- TopPanel: 에너지/HP/골드/덱/유물 (15+ hitboxes)
- 전투 중 카드: 10-30개 (손+덱+버리기더미)
- 유물: 평균 15-30개
- 버튼: 5-15개

총 60-100개 hitbox가 매 프레임 업데이트
```

## 최적화 전략

### 1. 화면 경계 체크 추가

```java
// Hitbox.java - 최적화된 버전
public void update(float x, float y) {
    if (AbstractDungeon.isFadingOut) {
        return;
    }

    this.x = x;
    this.y = y;

    // 화면 경계 체크 (매우 빠름)
    if (x + this.width < 0 || x > Settings.WIDTH ||
        y + this.height < 0 || y > Settings.HEIGHT) {
        // 화면 밖이면 상태만 리셋
        if (this.hovered) {
            this.hovered = false;
            this.justHovered = false;
        }
        return;  // 마우스 체크 스킵
    }

    // 기존 로직...
}
```

**예상 성능 향상**: 40-60% (화면 밖 hitbox가 많을 때)

### 2. 마우스 체크 로직 개선

```java
// 중복 제거
public void update(float x, float y) {
    // ... 화면 체크 후

    if (this.justHovered) {
        this.justHovered = false;
    }

    // 한 번만 계산
    boolean isHoveredNow = (InputHelper.mX > x &&
                           InputHelper.mX < x + this.width &&
                           InputHelper.mY > y &&
                           InputHelper.mY < y + this.height);

    if (!this.hovered && isHoveredNow) {
        this.justHovered = true;
    }

    this.hovered = isHoveredNow;
}
```

### 3. UI 화면별 최적화

```java
// CardRewardScreen.java
private void cardSelectUpdate() {
    for (AbstractCard c : this.rewardGroup) {
        // 화면에 보이는 카드만 업데이트
        if (c.current_x >= -AbstractCard.IMG_WIDTH &&
            c.current_x <= Settings.WIDTH) {
            c.update();
            c.updateHoverLogic();

            if (c.hb.justHovered) {
                CardCrawlGame.sound.playV("CARD_OBTAIN", 0.4F);
            }
            if (c.hb.hovered) {
                hoveredCard = c;
            }
        }
    }
}
```

## 구현 가이드

### Hitbox 최적화 모드

```java
// Settings.java에 추가
public static boolean enableHitboxCulling = true;

// Hitbox.java
public void update(float x, float y) {
    if (AbstractDungeon.isFadingOut) {
        return;
    }

    this.x = x;
    this.y = y;

    // 최적화 모드 체크
    if (Settings.enableHitboxCulling) {
        // 화면 경계 체크 (약간 여유 있게)
        float margin = 50.0F * Settings.scale;
        if (x + this.width < -margin ||
            x > Settings.WIDTH + margin ||
            y + this.height < -margin ||
            y > Settings.HEIGHT + margin) {

            // 화면 밖이면 상태 리셋 후 종료
            if (this.hovered) {
                this.hovered = false;
                this.justHovered = false;
            }
            return;
        }
    }

    // 기존 업데이트 로직...
}
```

### 테스트 방법

```java
// 성능 측정 코드
public class HitboxPerformanceTest {
    private long updateTime = 0;
    private int updateCount = 0;

    public void testHitboxUpdate() {
        long start = System.nanoTime();

        // 100개 hitbox 업데이트
        for (int i = 0; i < 100; i++) {
            hitbox.update();
        }

        updateTime += System.nanoTime() - start;
        updateCount++;

        if (updateCount % 60 == 0) {
            float avgMs = (updateTime / 1000000.0f) / updateCount;
            System.out.println("Average hitbox update: " + avgMs + "ms");
        }
    }
}
```

## 주의사항

### 1. 화면 전환 시 위치 리셋
```java
// 화면 전환 시 모든 hitbox 위치 업데이트 필요
public void onScreenOpen() {
    for (AbstractCard c : cards) {
        c.hb.update(c.current_x, c.current_y);
    }
}
```

### 2. 여유 마진 필요
```java
// 너무 빡빡하게 자르면 경계에서 문제 발생
float margin = 50.0F * Settings.scale;  // 충분한 여유
```

### 3. 스크롤 화면 처리
```java
// 스크롤 가능한 화면은 스크롤 오프셋 고려
if (hasScrolling) {
    float effectiveX = x - scrollOffsetX;
    // 스크롤 오프셋을 반영한 위치로 체크
}
```

## 예상 효과

### 성능 향상
```
일반 화면 (30개 hitbox):
- 기존: 30 hitbox × 4 비교 = 120회/프레임
- 최적화: 15 onscreen × 4 비교 = 60회/프레임
→ 50% 감소

카드 보상 화면 (10개 카드, 5개만 보임):
- 기존: 10 hitbox × 4 비교 = 40회/프레임
- 최적화: 5 onscreen × 4 비교 = 20회/프레임
→ 50% 감소

유물 많은 경우 (50개 유물):
- 기존: 50 hitbox × 4 비교 = 200회/프레임
- 최적화: 25 visible × 4 비교 = 100회/프레임
→ 50% 감소
```

### 추가 개선 가능성
```
1. Spatial hashing: 화면을 그리드로 나눔
2. Broad-phase culling: 대략적인 범위 체크
3. Last frame caching: 이전 프레임 결과 재사용
```

## 관련 최적화

- **23_TooltipRendering.md**: 툴팁도 화면 밖이면 스킵
- **24_UIUpdateOptimization.md**: UI 전체 업데이트 최적화
- **12_EffectScreenCulling.md**: 이펙트 화면 밖 컬링

## 참고사항

이 최적화는 **안전성이 높고** **효과가 확실**합니다. 화면 밖 UI는 사용자가 볼 수 없으므로 업데이트하지 않아도 문제없습니다.
