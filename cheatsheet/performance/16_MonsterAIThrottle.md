# 16. Monster AI Per-Frame Updates

## 문제 설명

몬스터의 `update()` 메서드가 매 프레임마다 모든 파워 시스템, 인텐트 애니메이션, 체력바를 업데이트합니다. 몬스터가 많은 전투에서는 불필요한 연산이 누적됩니다.

## 성능 영향

**비용**: 몬스터당 매 프레임 5-10 연산
**빈도**: 60 FPS × 몬스터 수
**예상 절감**: 3-5 몬스터 전투에서 ~40% 감소

## 코드 위치

```
AbstractMonster.java:218-229
```

## 현재 구현

```java
public void update() {
    // 매 프레임마다 실행
    for (AbstractPower p : this.powers) {
        p.updateParticles();  // 파워별 파티클 업데이트
    }
    updateReticle();          // 타겟팅 표시 업데이트
    updateHealthBar();        // 체력바 업데이트
    updateAnimations();       // 애니메이션 업데이트
    updateDeathAnimation();   // 사망 애니메이션
    updateEscapeAnimation();  // 도망 애니메이션
    updateIntent();           // 인텐트 아이콘 업데이트
    this.tint.update();       // 색조 효과 업데이트
}
```

## 문제 분석

1. **파워 파티클 업데이트**: 모든 파워가 매 프레임 파티클을 업데이트하지만, 대부분의 파워는 시각 효과가 없음
2. **체력바 불필요 업데이트**: 체력 변화가 없어도 매 프레임 업데이트
3. **인텐트 업데이트**: 인텐트가 변경되지 않아도 계속 업데이트

## 최적화 전략

### 방법 1: 더티 플래그 패턴

```java
private boolean healthDirty = false;
private boolean intentDirty = false;

public void update() {
    // 파워 파티클: 활성 파워만 업데이트
    for (AbstractPower p : this.powers) {
        if (p.hasParticles()) {  // 새 메서드: 파티클 존재 여부 확인
            p.updateParticles();
        }
    }

    updateReticle();
    updateAnimations();
    updateDeathAnimation();
    updateEscapeAnimation();

    // 체력바: 변경 시만 업데이트
    if (healthDirty) {
        updateHealthBar();
        healthDirty = false;
    }

    // 인텐트: 변경 시만 업데이트
    if (intentDirty) {
        updateIntent();
        intentDirty = false;
    }

    this.tint.update();
}

public void damage(DamageInfo info) {
    // 체력 변경 시 플래그 설정
    this.currentHealth -= damageAmount;
    healthDirty = true;
    // ...
}

public void createIntent() {
    // 인텐트 생성 시 플래그 설정
    this.intent = this.move.intent;
    intentDirty = true;
    // ...
}
```

### 방법 2: 업데이트 간격 조절

```java
private static final float HEALTH_UPDATE_INTERVAL = 0.1f; // 100ms마다 업데이트
private float healthUpdateTimer = 0.0f;

public void update() {
    // 파워 파티클 최적화
    for (AbstractPower p : this.powers) {
        if (p.hasParticles()) {
            p.updateParticles();
        }
    }

    updateReticle();
    updateAnimations();
    updateDeathAnimation();
    updateEscapeAnimation();
    updateIntent();
    this.tint.update();

    // 체력바: 주기적으로만 업데이트
    healthUpdateTimer -= Gdx.graphics.getDeltaTime();
    if (healthUpdateTimer <= 0 || healthDirty) {
        updateHealthBar();
        healthUpdateTimer = HEALTH_UPDATE_INTERVAL;
        healthDirty = false;
    }
}
```

### 방법 3: 조건부 업데이트

```java
public void update() {
    // 죽거나 도망중인 몬스터는 최소 업데이트만
    if (isDying || isEscaping) {
        updateDeathAnimation();
        updateEscapeAnimation();
        this.tint.update();
        return;
    }

    // 활성 파워만 업데이트
    if (!this.powers.isEmpty()) {
        for (AbstractPower p : this.powers) {
            if (p.hasParticles()) {
                p.updateParticles();
            }
        }
    }

    // 호버 중이거나 타겟팅 중일 때만 상세 업데이트
    if (this.hb.hovered || AbstractDungeon.player.hoveredCard != null) {
        updateReticle();
        updateHealthBar();
    }

    updateAnimations();
    updateIntent();
    this.tint.update();
}
```

## 구현 가이드

### 단계 1: AbstractPower에 파티클 확인 메서드 추가

```java
// AbstractPower.java
public boolean hasParticles() {
    // 기본값: 파티클 없음
    return false;
}

// 파티클이 있는 파워에서 오버라이드
public class SomeBuffPower extends AbstractPower {
    @Override
    public boolean hasParticles() {
        return true;
    }
}
```

### 단계 2: 더티 플래그 필드 추가

```java
// AbstractMonster.java
private boolean healthDirty = false;
private boolean intentDirty = false;
```

### 단계 3: 체력/인텐트 변경 시 플래그 설정

```java
// damage(), heal(), createIntent() 등에서 플래그 설정
public void damage(DamageInfo info) {
    // ... 기존 코드 ...
    this.currentHealth -= damageAmount;
    healthDirty = true;
    // ...
}
```

### 단계 4: update() 메서드 최적화

```java
public void update() {
    // 조기 종료 최적화
    if (isDying || isEscaping) {
        updateDeathAnimation();
        updateEscapeAnimation();
        this.tint.update();
        return;
    }

    // 조건부 업데이트 적용
    // (위의 방법 1, 2, 3 중 선택)
}
```

## 측정 방법

```java
// 성능 측정 코드
long start = System.nanoTime();
for (AbstractMonster m : monsters) {
    m.update();
}
long elapsed = System.nanoTime() - start;
System.out.println("Monster update time: " + (elapsed / 1000) + "μs");
```

## 예상 결과

- **Before**: 3 몬스터 × 8연산 × 60fps = 1,440 ops/sec
- **After**: 3 몬스터 × 3연산 × 60fps = 540 ops/sec
- **절감**: ~62.5% 감소

## 주의사항

1. **애니메이션 끊김**: `updateAnimations()`는 계속 호출 필요
2. **인텐트 변경 감지**: `applyPowers()` 호출 시 `intentDirty = true` 필요
3. **체력바 동기화**: 즉각 반영이 필요한 경우 `healthDirty = true` 명시

## 관련 이슈

- [17_IntentRecalculation.md](17_IntentRecalculation.md): 인텐트 재계산 중복
- [18_PowerStackIteration.md](18_PowerStackIteration.md): 파워 스택 순회 최적화

## 참고 자료

- `AbstractMonster.java:218-229` - update() 메서드
- `AbstractMonster.java:237-266` - updateIntent() 메서드
- `AbstractCreature.java:800-818` - updatePowers() 메서드
