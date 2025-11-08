# Phase 5: Monster & Combat - Performance Analysis Summary

## 개요

**분석 대상**: 몬스터 AI, 인텐트 시스템, 파워 메커니즘
**작성일**: 2025-11-08
**디컴파일 소스**: `E:\workspace\sts-decompile`

## 발견된 주요 성능 이슈

### 1. 몬스터 업데이트 오버헤드
- **파일**: [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md)
- **위치**: `AbstractMonster.java:218-229`
- **문제**: 매 프레임(60 FPS) 모든 몬스터 업데이트
- **영향**: 몬스터당 5-10 연산 × 60 FPS
- **해결**: 더티 플래그 패턴, 조건부 업데이트
- **절감**: ~62% 감소

### 2. 인텐트 데미지 중복 계산
- **파일**: [17_IntentRecalculation.md](17_IntentRecalculation.md)
- **위치**: `AbstractMonster.java:1343-1363`
- **문제**: 파워 변경 없어도 매번 4회 파워 스택 순회
- **영향**: 카드 사용마다 (몬스터 파워 + 플레이어 파워) × 4
- **해결**: 파워 버전 추적, 캐싱
- **절감**: ~70% 감소

### 3. 파워 스택 불필요 순회
- **파일**: [18_PowerStackIteration.md](18_PowerStackIteration.md)
- **위치**: `AbstractCreature.java:610-630`
- **문제**: 빈 메서드를 가진 파워도 매번 호출
- **영향**: 파워 10개 × 수십 가지 이벤트
- **해결**: 이벤트 리스너 분류, 비트마스크 필터링
- **절감**: ~60-80% 감소

### 4. MonsterGroup 중복 업데이트
- **파일**: [19_MonsterGroupUpdate.md](19_MonsterGroupUpdate.md)
- **위치**: `MonsterGroup.java:335-362`
- **문제**: 마우스 정지 중에도 매 프레임 히트박스 업데이트
- **영향**: 몬스터 × 3 히트박스 × 60 FPS
- **해결**: 마우스 이동 감지, 공간 분할
- **절감**: ~70% 감소

### 5. getMove() 중복 호출
- **파일**: [20_MonsterGetMoveThrottle.md](20_MonsterGetMoveThrottle.md)
- **위치**: `AbstractMonster.java:563-565`
- **문제**: 턴당 1회만 필요한데 2-3회 호출
- **영향**: 복잡한 AI 로직 × 중복 실행
- **해결**: 캐싱, 무효화 플래그
- **절감**: ~60% 감소

## 심각도 분류

### 🔴 Critical (즉시 수정 권장)
1. **파워 스택 순회** (18번): 모든 전투에서 지속적 영향
2. **인텐트 재계산** (17번): 카드 사용마다 발생

### 🟡 High (우선순위 높음)
3. **몬스터 업데이트** (16번): FPS 저하의 주요 원인
4. **MonsterGroup 업데이트** (19번): 히트박스 중복 체크

### 🟢 Medium (점진적 개선)
5. **getMove() 호출** (20번): 턴 전환 시에만 영향

## 최적화 우선순위

### 1차: 파워 시스템 최적화 (18, 17번)
```
예상 효과: 전투 전반적 성능 30-40% 향상
구현 복잡도: 중간
호환성 리스크: 낮음
```

**구현 순서**:
1. `AbstractPower`에 이벤트 비트마스크 추가
2. 각 파워 클래스에서 처리하는 이벤트 선언
3. 순회 로직에 필터링 적용
4. 파워 버전 추적 시스템 구현

### 2차: 몬스터 업데이트 최적화 (16, 19번)
```
예상 효과: 프레임 드롭 감소, 안정적 60 FPS
구현 복잡도: 낮음
호환성 리스크: 매우 낮음
```

**구현 순서**:
1. 더티 플래그 필드 추가
2. 마우스 이동 감지 로직 구현
3. 조건부 업데이트 적용
4. 활성 몬스터 필터링

### 3차: AI 최적화 (20번)
```
예상 효과: 턴 전환 속도 개선
구현 복잡도: 낮음
호환성 리스크: 중간 (상태 변경 동기화 필요)
```

**구현 순서**:
1. 캐싱 플래그 추가
2. rollMove() 최적화
3. 특수 케이스 처리 (상태 변경)

## 통합 최적화 전략

### 패턴 1: 이벤트 기반 업데이트
```java
// 모든 업데이트를 이벤트 기반으로 전환
public class CombatUpdateManager {
    private boolean powersDirty = false;
    private boolean intentDirty = false;
    private boolean healthDirty = false;

    public void onPowerChanged() {
        powersDirty = true;
        intentDirty = true;
    }

    public void onHealthChanged() {
        healthDirty = true;
    }

    public void update() {
        if (powersDirty) {
            updatePowers();
            powersDirty = false;
        }

        if (intentDirty) {
            updateIntents();
            intentDirty = false;
        }

        if (healthDirty) {
            updateHealthBars();
            healthDirty = false;
        }
    }
}
```

### 패턴 2: 계층적 캐싱
```java
// 계산 결과를 계층적으로 캐싱
public class DamageCache {
    // Level 1: Base damage
    private int baseDamage;

    // Level 2: With monster powers
    private int damageAfterMonsterPowers;
    private boolean monsterPowersValid = false;

    // Level 3: With player powers
    private int finalDamage;
    private boolean playerPowersValid = false;

    public int getFinalDamage() {
        if (!playerPowersValid) {
            if (!monsterPowersValid) {
                recalculateWithMonsterPowers();
            }
            recalculateWithPlayerPowers();
        }
        return finalDamage;
    }

    public void invalidateMonsterPowers() {
        monsterPowersValid = false;
        playerPowersValid = false;
    }

    public void invalidatePlayerPowers() {
        playerPowersValid = false;
    }
}
```

### 패턴 3: 통합 업데이트 스케줄러
```java
// 우선순위 기반 업데이트 스케줄링
public class UpdateScheduler {
    private static final float CRITICAL_INTERVAL = 0.0f;    // 매 프레임
    private static final float HIGH_INTERVAL = 0.016f;      // 60 FPS
    private static final float MEDIUM_INTERVAL = 0.1f;      // 10 FPS
    private static final float LOW_INTERVAL = 0.5f;         // 2 FPS

    private float healthBarTimer = 0.0f;
    private float intentParticleTimer = 0.0f;

    public void update(float deltaTime) {
        // Critical: 애니메이션, 입력
        updateAnimations();
        updateInput();

        // High: 게임 로직
        updateGameLogic();

        // Medium: 체력바 (10 FPS로 충분)
        healthBarTimer -= deltaTime;
        if (healthBarTimer <= 0) {
            updateHealthBars();
            healthBarTimer = MEDIUM_INTERVAL;
        }

        // Low: 인텐트 파티클 (2 FPS로 충분)
        intentParticleTimer -= deltaTime;
        if (intentParticleTimer <= 0) {
            updateIntentParticles();
            intentParticleTimer = LOW_INTERVAL;
        }
    }
}
```

## 예상 종합 효과

### Before (현재 상태)
```
3 몬스터 전투, 각 5개 파워, 10턴:

- 몬스터 업데이트: 3 × 8연산 × 60fps × 10턴 = 14,400 ops
- 파워 순회: 3 × 5파워 × 10이벤트 × 10턴 = 1,500 ops
- 인텐트 재계산: 3 × 10회/턴 × 4순회 × 10턴 = 1,200 ops
- 히트박스 업데이트: 3 × 3박스 × 60fps × 10턴 = 5,400 ops
- getMove 호출: 3 × 2.5회/턴 × 10턴 = 75 ops

총계: ~22,575 operations
```

### After (최적화 후)
```
동일한 전투:

- 몬스터 업데이트: 3 × 3연산 × 60fps × 10턴 = 5,400 ops (-62%)
- 파워 순회: 3 × 2파워 × 10이벤트 × 10턴 = 600 ops (-60%)
- 인텐트 재계산: 3 × 3회/턴 × 1순회 × 10턴 = 90 ops (-92%)
- 히트박스 업데이트: 3 × 3박스 × 10fps × 10턴 = 900 ops (-83%)
- getMove 호출: 3 × 1회/턴 × 10턴 = 30 ops (-60%)

총계: ~7,020 operations (-69% 전체)
```

## 구현 로드맵

### Week 1: 파운데이션
- [ ] AbstractPower 이벤트 시스템 설계
- [ ] 더티 플래그 인프라 구현
- [ ] 성능 측정 도구 구축

### Week 2: 파워 시스템
- [ ] 이벤트 비트마스크 구현
- [ ] 파워 순회 최적화 적용
- [ ] 파워 버전 추적 시스템

### Week 3: 몬스터 업데이트
- [ ] 조건부 업데이트 로직
- [ ] 마우스 이동 감지
- [ ] 활성 몬스터 필터링

### Week 4: 통합 및 테스트
- [ ] 통합 테스트
- [ ] 성능 벤치마크
- [ ] 호환성 검증

## 측정 및 검증

### 성능 메트릭
```java
public class CombatPerformanceMetrics {
    // 측정 항목
    private long monsterUpdateTime;
    private long powerIterationTime;
    private long intentCalculationTime;
    private long hitboxUpdateTime;

    // 호출 횟수
    private int getMoveCallCount;
    private int powerIterationCount;
    private int intentRecalcCount;

    public void printReport() {
        System.out.println("=== Combat Performance Report ===");
        System.out.println("Monster Update: " + monsterUpdateTime + "μs");
        System.out.println("Power Iteration: " + powerIterationTime + "μs");
        System.out.println("Intent Calc: " + intentCalculationTime + "μs");
        System.out.println("Hitbox Update: " + hitboxUpdateTime + "μs");
        System.out.println();
        System.out.println("getMove calls: " + getMoveCallCount);
        System.out.println("Power iterations: " + powerIterationCount);
        System.out.println("Intent recalcs: " + intentRecalcCount);
    }
}
```

### 자동화 테스트
```java
@Test
public void testMonsterUpdatePerformance() {
    MonsterGroup group = createTestMonsters(3);

    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
        group.update();
    }
    long elapsed = System.nanoTime() - start;

    long avgTime = elapsed / 1000;
    assertTrue("Update too slow: " + avgTime + "ns",
               avgTime < 50000);  // 50μs threshold
}
```

## 참고 자료

### 소스 파일
- `AbstractMonster.java` - 몬스터 기본 클래스
- `MonsterGroup.java` - 몬스터 그룹 관리
- `AbstractPower.java` - 파워 시스템
- `AbstractCreature.java` - 생명체 기본 클래스

### 관련 문서
- [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md)
- [17_IntentRecalculation.md](17_IntentRecalculation.md)
- [18_PowerStackIteration.md](18_PowerStackIteration.md)
- [19_MonsterGroupUpdate.md](19_MonsterGroupUpdate.md)
- [20_MonsterGetMoveThrottle.md](20_MonsterGetMoveThrottle.md)

## 다음 단계

Phase 6에서 다룰 주제:
- 카드 렌더링 최적화
- 손패 관리 시스템
- 드래그 앤 드롭 성능
- 카드 선택 UI 최적화
