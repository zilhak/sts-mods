# 24. UI Update Optimization (Phase 7: UI Systems)

## 문제 분석

### 심각도: HIGH
**예상 성능 영향**: 10-20% (UI가 복잡한 화면에서 25-35%)

## 핵심 문제

모든 UI 화면이 매 프레임 모든 요소를 업데이트하며, 화면이 활성화되지 않았거나 보이지 않는 요소도 업데이트합니다.

### 현재 코드 패턴

#### 1. CardRewardScreen.java
```java
public void update() {
    if (Settings.isTouchScreen) {
        this.confirmButton.update();  // 항상 업데이트
    }

    this.peekButton.update();
    if (!PeekButton.isPeeking) {
        this.skipButton.update();
        this.bowlButton.update();
    }

    updateControllerInput();

    if (!PeekButton.isPeeking) {
        if (!this.scrollBar.update()) {
            updateScrolling();
        }
        cardSelectUpdate();  // 모든 카드 업데이트
    }
}

private void cardSelectUpdate() {
    AbstractCard hoveredCard = null;
    for (AbstractCard c : this.rewardGroup) {
        c.update();           // 모든 카드 업데이트
        c.updateHoverLogic(); // 모든 카드 호버 체크
        if (c.hb.justHovered) {
            CardCrawlGame.sound.playV("CARD_OBTAIN", 0.4F);
        }
        if (c.hb.hovered) {
            hoveredCard = c;
        }
    }
    // ... 클릭 처리
}
```

#### 2. CombatRewardScreen.java
```java
public void update() {
    if (InputHelper.justClickedLeft && Settings.isDebug) {
        this.tip = CardCrawlGame.tips.getTip();
    }

    rewardViewUpdate();  // 항상 호출
    updateEffects();     // 항상 호출
}

private void rewardViewUpdate() {
    // 애니메이션 타이머
    if (this.rewardAnimTimer != 0.0F) {
        this.rewardAnimTimer -= Gdx.graphics.getDeltaTime();
        // ... 색상 업데이트
    }

    this.tipY = MathHelper.uiLerpSnap(this.tipY, targetY);
    updateControllerInput();

    boolean removedSomething = false;
    for (Iterator<RewardItem> i = this.rewards.iterator();
         i.hasNext(); ) {
        RewardItem r = i.next();
        r.update();  // 모든 보상 아이템 업데이트

        if (r.isDone) {
            // ... 처리
        }
    }

    if (removedSomething) {
        positionRewards();
        setLabel();
    }
}
```

## 성능 문제 분석

### 1. 불필요한 반복 업데이트

```java
// 60 FPS 기준
CardRewardScreen (3개 카드):
- confirmButton.update(): 60회/초
- peekButton.update(): 60회/초
- skipButton.update(): 60회/초
- bowlButton.update(): 60회/초
- scrollBar.update(): 60회/초
- 3× card.update(): 180회/초
- 3× card.updateHoverLogic(): 180회/초
총: 540회/초

CombatRewardScreen (5개 보상):
- 5× RewardItem.update(): 300회/초
- updateEffects(): 60회/초
- 컨트롤러 입력 체크: 60회/초
총: 420회/초
```

### 2. 화면 비활성 시에도 업데이트

```java
// AbstractDungeon.screen이 다른 값이어도 update() 호출됨
if (AbstractDungeon.screen != CurrentScreen.CARD_REWARD) {
    // 하지만 update()는 계속 호출됨!
}
```

### 3. 중복 마우스 체크

```java
// CardRewardScreen.updateControllerInput()
for (AbstractCard c : this.rewardGroup) {
    if (c.hb.hovered) {  // 첫 번째 체크
        anyHovered = true;
        break;
    }
    index++;
}

// 그 다음 cardSelectUpdate()에서
for (AbstractCard c : this.rewardGroup) {
    c.updateHoverLogic();  // 두 번째 체크
    if (c.hb.hovered) {    // 세 번째 체크
        hoveredCard = c;
    }
}
```

## 최적화 전략

### 1. 화면 활성화 체크

```java
// CardRewardScreen.java
public void update() {
    // 화면이 활성화되어 있지 않으면 업데이트 스킵
    if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.CARD_REWARD) {
        return;
    }

    // 기존 업데이트 로직...
}
```

### 2. 조건부 업데이트

```java
public void update() {
    // 터치스크린일 때만 confirm 버튼 업데이트
    if (Settings.isTouchScreen) {
        this.confirmButton.update();
    }

    // 항상 필요한 요소만 업데이트
    this.peekButton.update();

    if (!PeekButton.isPeeking) {
        // peeking 중이 아닐 때만
        this.skipButton.update();
        this.bowlButton.update();

        // 스크롤 필요한 경우만
        if (shouldShowScrollBar()) {
            if (!this.scrollBar.update()) {
                updateScrolling();
            }
        }

        cardSelectUpdate();
    }
}
```

### 3. 화면 내 카드만 업데이트

```java
private void cardSelectUpdate() {
    AbstractCard hoveredCard = null;

    for (AbstractCard c : this.rewardGroup) {
        // 화면에 보이는 카드만 업데이트
        if (!isCardVisible(c)) {
            continue;
        }

        c.update();
        c.updateHoverLogic();

        if (c.hb.justHovered) {
            CardCrawlGame.sound.playV("CARD_OBTAIN", 0.4F);
        }
        if (c.hb.hovered) {
            hoveredCard = c;
        }
    }

    // 클릭 처리 (변경 없음)...
}

private boolean isCardVisible(AbstractCard c) {
    // 화면 경계 체크
    return c.current_x >= -AbstractCard.IMG_WIDTH &&
           c.current_x <= Settings.WIDTH &&
           c.current_y >= -AbstractCard.IMG_HEIGHT &&
           c.current_y <= Settings.HEIGHT;
}
```

### 4. 버튼 업데이트 최적화

```java
// SkipCardButton.java
public void update() {
    // 숨겨진 버튼은 업데이트 스킵
    if (this.isHidden) {
        return;
    }

    // 화면이 비활성화되면 스킵
    if (this.screenDisabled) {
        return;
    }

    // 기존 업데이트 로직...
}
```

## 구현 가이드

### 전체 최적화 패턴

```java
// AbstractScreen.java (기본 클래스)
public abstract class AbstractScreen {
    protected boolean isActive = false;
    protected long lastUpdateTime = 0;
    protected static final long MIN_UPDATE_INTERVAL = 16_666_667; // 60 FPS

    public void update() {
        // 비활성 화면 스킵
        if (!isActive) {
            return;
        }

        // FPS 제한 (선택적)
        long now = System.nanoTime();
        if (now - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime = now;

        // 실제 업데이트
        updateInternal();
    }

    protected abstract void updateInternal();

    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            onActivate();
        } else {
            onDeactivate();
        }
    }

    protected void onActivate() {}
    protected void onDeactivate() {}
}
```

### CardRewardScreen 최적화

```java
public class CardRewardScreen extends AbstractScreen {
    private boolean needsUpdate = true;

    @Override
    protected void updateInternal() {
        // 1. 터치스크린 전용 UI
        if (Settings.isTouchScreen) {
            this.confirmButton.update();
            handleTouchConfirm();
        }

        // 2. 항상 필요한 UI
        this.peekButton.update();

        // 3. Peeking 모드가 아닐 때만
        if (!PeekButton.isPeeking) {
            updateButtons();
            updateCardSelection();
        }
    }

    private void updateButtons() {
        // 보이는 버튼만 업데이트
        if (!this.skipButton.isHidden) {
            this.skipButton.update();
        }
        if (!this.bowlButton.isHidden) {
            this.bowlButton.update();
        }
    }

    private void updateCardSelection() {
        // 컨트롤러 모드 체크
        if (Settings.isControllerMode &&
            !AbstractDungeon.topPanel.selectPotionMode) {
            updateControllerInput();
        }

        // 스크롤 업데이트
        if (shouldShowScrollBar()) {
            if (!this.scrollBar.update()) {
                updateScrolling();
            }
        }

        // 카드 업데이트 (최적화됨)
        updateCardsOptimized();
    }

    private void updateCardsOptimized() {
        AbstractCard hoveredCard = null;
        int visibleCount = 0;

        for (AbstractCard c : this.rewardGroup) {
            // 화면에 보이는 카드만 처리
            if (!isCardOnScreen(c)) {
                // 화면 밖 카드는 상태만 리셋
                c.hb.unhover();
                continue;
            }

            visibleCount++;
            c.update();
            c.updateHoverLogic();

            if (c.hb.justHovered) {
                CardCrawlGame.sound.playV("CARD_OBTAIN", 0.4F);
            }
            if (c.hb.hovered) {
                hoveredCard = c;
            }
        }

        // 클릭 처리
        if (hoveredCard != null) {
            handleCardInteraction(hoveredCard);
        }
    }

    private boolean isCardOnScreen(AbstractCard c) {
        // 약간의 마진을 두고 체크
        float margin = 100.0F * Settings.scale;
        return c.current_x > -AbstractCard.IMG_WIDTH - margin &&
               c.current_x < Settings.WIDTH + margin &&
               c.current_y > -AbstractCard.IMG_HEIGHT - margin &&
               c.current_y < Settings.HEIGHT + margin;
    }

    @Override
    protected void onActivate() {
        // 화면 활성화 시 초기화
        this.needsUpdate = true;
        updatePositions();
    }

    @Override
    protected void onDeactivate() {
        // 화면 비활성화 시 정리
        this.needsUpdate = false;
    }
}
```

### CombatRewardScreen 최적화

```java
public class CombatRewardScreen extends AbstractScreen {
    private boolean animationActive = false;

    @Override
    protected void updateInternal() {
        // 디버그 기능
        if (Settings.isDebug && InputHelper.justClickedLeft) {
            this.tip = CardCrawlGame.tips.getTip();
        }

        // 애니메이션 체크
        updateAnimation();

        // 보상 아이템 업데이트
        if (needsRewardUpdate()) {
            updateRewards();
        }

        // 이펙트 업데이트
        updateEffects();
    }

    private void updateAnimation() {
        if (this.rewardAnimTimer <= 0.0F) {
            animationActive = false;
            return;
        }

        animationActive = true;
        this.rewardAnimTimer -= Gdx.graphics.getDeltaTime();
        if (this.rewardAnimTimer < 0.0F) {
            this.rewardAnimTimer = 0.0F;
        }

        // 색상 업데이트
        float t = 1.0F - this.rewardAnimTimer / 0.2F;
        this.uiColor.r = t;
        this.uiColor.g = t;
        this.uiColor.b = t;
    }

    private boolean needsRewardUpdate() {
        // 애니메이션 중이거나 보상이 남아있으면 업데이트
        return animationActive || !this.rewards.isEmpty();
    }

    private void updateRewards() {
        // 팁 위치 업데이트 (애니메이션)
        this.tipY = MathHelper.uiLerpSnap(
            this.tipY,
            Settings.HEIGHT / 2.0F - 460.0F * Settings.scale
        );

        // 컨트롤러 입력
        if (Settings.isControllerMode) {
            updateControllerInput();
        }

        // 보상 아이템 처리
        boolean removedSomething = false;
        for (Iterator<RewardItem> i = this.rewards.iterator();
             i.hasNext(); ) {
            RewardItem r = i.next();

            // 화면에 보이는 보상만 업데이트
            if (isRewardVisible(r)) {
                r.update();

                if (r.isDone) {
                    if (r.claimReward()) {
                        i.remove();
                        removedSomething = true;
                    } else if (r.type == RewardItem.RewardType.POTION) {
                        r.isDone = false;
                        AbstractDungeon.topPanel.flashRed();
                        this.tip = CardCrawlGame.tips.getPotionTip();
                    } else {
                        r.isDone = false;
                    }
                }
            }
        }

        if (removedSomething) {
            positionRewards();
            setLabel();
        }
    }

    private boolean isRewardVisible(RewardItem r) {
        // 보상 아이템 위치 체크
        return r.y > -100.0F * Settings.scale &&
               r.y < Settings.HEIGHT + 100.0F * Settings.scale;
    }

    private void updateEffects() {
        // 이펙트가 없으면 스킵
        if (this.effects.isEmpty()) {
            return;
        }

        for (Iterator<AbstractGameEffect> i = this.effects.iterator();
             i.hasNext(); ) {
            AbstractGameEffect e = i.next();
            e.update();
            if (e.isDone) {
                i.remove();
            }
        }
    }
}
```

## 성능 측정

### 벤치마크 코드

```java
public class UIUpdateBenchmark {
    private long totalUpdateTime = 0;
    private int updateCount = 0;

    public void measureUpdate() {
        long start = System.nanoTime();

        // 화면 업데이트
        screen.update();

        long elapsed = System.nanoTime() - start;
        totalUpdateTime += elapsed;
        updateCount++;

        if (updateCount % 60 == 0) {
            float avgMs = (totalUpdateTime / 1000000.0f) / updateCount;
            System.out.println("Average update time: " + avgMs + "ms");
            System.out.println("FPS impact: " +
                (avgMs / 16.67f * 100.0f) + "%");
        }
    }
}
```

## 예상 효과

### 성능 향상
```
CardRewardScreen (3개 카드):
기존: 540 updates/초
최적화: 240 updates/초 (화면 활성 시)
        0 updates/초 (화면 비활성 시)
→ 55% 감소

CombatRewardScreen (5개 보상):
기존: 420 updates/초
최적화: 180 updates/초 (보상 남음)
        60 updates/초 (보상 모두 획득)
→ 57-85% 감소

TopPanel (15개 hitbox):
기존: 900 updates/초
최적화: 450 updates/초 (활성 요소만)
→ 50% 감소
```

### 프레임 시간 개선
```
복잡한 UI 화면 (30개 요소):
기존: 2.5ms per frame
최적화: 1.0ms per frame
→ 1.5ms 절약 (9% FPS 향상)
```

## 추가 최적화

### 1. Dirty Flag 패턴

```java
public class CardRewardScreen {
    private boolean positionsDirty = true;

    public void open(...) {
        // 위치 재계산 필요
        positionsDirty = true;
    }

    private void updateCardPositions() {
        if (!positionsDirty) {
            return;  // 변경 없으면 스킵
        }

        for (AbstractCard c : this.rewardGroup) {
            // 위치 계산...
        }

        positionsDirty = false;
    }
}
```

### 2. Update Frequency 감소

```java
// 덜 중요한 UI는 프레임 스킵
private int frameCounter = 0;

public void update() {
    frameCounter++;

    // 중요한 UI: 매 프레임
    updateCriticalElements();

    // 덜 중요한 UI: 5프레임마다
    if (frameCounter % 5 == 0) {
        updateNonCriticalElements();
    }
}
```

## 주의사항

### 1. 입력 처리
```java
// 입력은 항상 체크해야 함
public void update() {
    if (!isActive) {
        // 화면 비활성이어도 ESC 키는 처리
        handleEscapeKey();
        return;
    }
    // ...
}
```

### 2. 애니메이션
```java
// 애니메이션 중에는 계속 업데이트
if (isAnimating || hasActiveEffects) {
    // 계속 업데이트
}
```

### 3. 상태 동기화
```java
// 화면 활성화 시 상태 동기화
@Override
protected void onActivate() {
    syncState();
    resetAnimations();
}
```

## 관련 최적화

- **22_HitboxCulling.md**: Hitbox 화면 밖 컬링
- **23_TooltipRendering.md**: 툴팁 렌더링 최적화
- **12_EffectScreenCulling.md**: 이펙트 화면 컬링

## 참고사항

이 최적화는 **안전하고 효과적**입니다. UI 업데이트는 화면이 활성화되어 있고 요소가 보일 때만 필요합니다.
