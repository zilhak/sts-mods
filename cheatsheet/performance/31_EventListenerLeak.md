# Phase 6: Memory Management - Event Listener Memory Leaks

## Pattern Discovery

**Category**: Memory Leaks - Event Listeners
**Priority**: HIGH (리스너 누수는 심각한 메모리 누수 원인)
**Impact**: 메모리 누수, 이벤트 중복 처리, 성능 저하

---

## Pattern 1: Listener Registration without Removal

### 발견된 패턴

#### Good Example: CorruptHeart (Proper Cleanup)

```java
// ✅ GOOD - Listener cleanup (CorruptHeart.java:59, 284)
public class CorruptHeart extends AbstractMonster {
    private HeartAnimListener animListener;

    public CorruptHeart() {
        // 리스너 생성 및 등록
        this.animListener = new HeartAnimListener();
        this.state.addListener((AnimationState.AnimationStateListener)this.animListener);
    }

    @Override
    public void die() {
        // ⭐ 중요: 죽을 때 리스너 제거
        this.state.removeListener((AnimationState.AnimationStateListener)this.animListener);
        super.die();
    }
}
```

**올바른 패턴**:
1. **필드 저장**: 리스너를 필드로 저장 (제거 시 참조 필요)
2. **명시적 제거**: die() 같은 정리 메서드에서 removeListener 호출
3. **Type Cast**: AnimationStateListener로 캐스팅 (타입 일치 필요)

#### Bad Example: Most Other Monsters (No Cleanup)

```java
// ❌ BAD - No listener removal (AcidSlime_L.java:92)
public class AcidSlime_L extends AbstractMonster {
    public AcidSlime_L() {
        // 리스너 등록
        this.state.addListener((AnimationState.AnimationStateListener)new SlimeAnimListener());
        // ⚠️ 문제: 제거 코드 없음!
    }

    // die() 메서드에 removeListener 호출 없음
}
```

**문제점**:
- 몬스터가 죽어도 리스너는 AnimationState에 남아있음
- 다음 전투에서 이전 리스너들이 계속 실행됨
- 전투를 반복할수록 리스너 수가 누적됨

**메모리 누수 시나리오**:
```
전투 1: 슬라임 3마리 → 리스너 3개 등록
전투 2: 슬라임 3마리 → 리스너 6개 (3+3)
전투 3: 슬라임 3마리 → 리스너 9개 (3+3+3)
...
전투 10: 리스너 30개 → 불필요한 이벤트 처리
```

---

## Pattern 2: Anonymous Listener (Removal Impossible)

### 문제점

```java
// ❌ BAD - Anonymous listener (cannot be removed)
public class AcidSlime_M extends AbstractMonster {
    public AcidSlime_M() {
        this.state.addListener((AnimationState.AnimationStateListener)new SlimeAnimListener());
        // 익명 객체라 removeListener 불가능!
    }
}
```

**왜 제거가 불가능한가?**:
- `new SlimeAnimListener()`는 즉시 생성되어 등록
- 참조를 저장하지 않아서 나중에 찾을 수 없음
- `removeListener()`는 동일한 객체 참조가 필요

**해결 방법**:
```java
// ✅ GOOD - Field reference
public class AcidSlime_M extends AbstractMonster {
    private SlimeAnimListener listener;

    public AcidSlime_M() {
        this.listener = new SlimeAnimListener();
        this.state.addListener((AnimationState.AnimationStateListener)this.listener);
    }

    @Override
    public void die() {
        this.state.removeListener((AnimationState.AnimationStateListener)this.listener);
        super.die();
    }
}
```

---

## Pattern 3: Controller Listener Management

### CInputHelper의 안전한 패턴

```java
// ✅ GOOD - Explicit add/remove (CInputHelper.java:67, 91-92)
public static void initialize() {
    for (Controller controller : Controllers.getControllers()) {
        controller.addListener(listener);
    }
}

public static void regainInputFocus() {
    controller.addListener(listener);
    Controllers.removeListener(listener);  // ⭐ 이전 리스너 제거 후 재등록
}
```

**패턴 분석**:
1. **중복 방지**: 재등록 전에 기존 리스너 제거
2. **전역 관리**: Static 리스너로 생명주기 관리
3. **명시적 정리**: 포커스 변경 시 정리

---

## Pattern 4: NPC Event Listener

### SpireHeart의 패턴

```java
// ⚠️ POTENTIAL ISSUE (SpireHeart.java:68)
public class SpireHeart extends AbstractEvent {
    private AnimatedNpc npc;

    public SpireHeart() {
        this.npc = new AnimatedNpc(...);
        this.npc.addListener(new HeartAnimListener());
        // ⚠️ 리스너 제거 코드 없음
    }
}
```

**리스크**:
- 이벤트가 종료되어도 NPC 객체가 리스너를 보유
- NPC가 dispose되지 않으면 리스너도 메모리에 남음
- 여러 번 이벤트 발생 시 리스너 누적

**개선 방안**:
```java
// ✅ BETTER - Cleanup in dispose
public class SpireHeart extends AbstractEvent {
    private AnimatedNpc npc;
    private HeartAnimListener listener;

    public SpireHeart() {
        this.npc = new AnimatedNpc(...);
        this.listener = new HeartAnimListener();
        this.npc.addListener(this.listener);
    }

    @Override
    public void dispose() {
        if (this.npc != null) {
            this.npc.removeListener(this.listener);
            this.npc.dispose();
        }
        super.dispose();
    }
}
```

---

## AnimatedNpc Listener API Analysis

### 현재 API

```java
// AnimatedNpc.java:78-79
public void addListener(HeartAnimListener listener) {
    this.state.addListener((AnimationState.AnimationStateListener)listener);
}

// ⚠️ 문제: removeListener 메서드가 없음!
```

**API 설계 결함**:
- `addListener()`만 제공
- `removeListener()` 메서드 없음
- 클라이언트가 직접 정리할 방법이 없음

**임시 해결 방법**:
```java
// Reflection을 사용한 강제 제거 (권장하지 않음)
public void forceRemoveListener(AnimatedNpc npc, HeartAnimListener listener) {
    try {
        Field stateField = AnimatedNpc.class.getDeclaredField("state");
        stateField.setAccessible(true);
        AnimationState state = (AnimationState) stateField.get(npc);
        state.removeListener(listener);
    } catch (Exception e) {
        // Handle error
    }
}
```

**올바른 해결: API 개선**
```java
// ✅ GOOD - Complete API
public class AnimatedNpc {
    public void addListener(HeartAnimListener listener) {
        this.state.addListener((AnimationState.AnimationStateListener)listener);
    }

    public void removeListener(HeartAnimListener listener) {
        this.state.removeListener((AnimationState.AnimationStateListener)listener);
    }

    public void dispose() {
        // 모든 리스너 자동 제거
        this.state.clearListeners();
    }
}
```

---

## Listener Lifecycle Patterns

### Pattern A: Object-scoped Listener

```java
// ✅ GOOD - 객체 생명주기와 연동
public class MyMonster extends AbstractMonster {
    private MyListener listener;

    public MyMonster() {
        listener = new MyListener(this);
        registerListener(listener);
    }

    @Override
    public void die() {
        unregisterListener(listener);
        super.die();
    }
}
```

### Pattern B: Temporary Listener

```java
// ✅ GOOD - 일회성 리스너
public class MyAction extends AbstractGameAction {
    public void update() {
        // 리스너 등록
        OneTimeListener listener = new OneTimeListener(() -> {
            // 작업 완료 후 자동 제거
            unregisterListener(this);
        });
        registerListener(listener);
    }
}
```

### Pattern C: Static Listener (위험)

```java
// ⚠️ RISKY - Static 리스너 (신중히 사용)
public class GlobalInputHandler {
    private static final InputListener INSTANCE = new InputListener();

    public static void initialize() {
        Controllers.addListener(INSTANCE);
        // ⚠️ 절대 제거하지 않음 - 의도적인 전역 리스너
    }
}
```

---

## Memory Leak Detection

### 1. Heap Dump 분석

```java
// VisualVM으로 확인:
// 1. 전투 전 heap dump
// 2. 전투 10회 반복
// 3. 전투 후 heap dump
// 4. AnimationStateListener 인스턴스 수 비교

// 정상: 리스너 수 일정 (약 5-10개)
// 누수: 리스너 수 선형 증가 (50-100개)
```

### 2. Reference Tracking

```java
// Debug 모드에서 리스너 추적
public class DebugAnimatedNpc extends AnimatedNpc {
    private static int listenerCount = 0;

    @Override
    public void addListener(HeartAnimListener listener) {
        super.addListener(listener);
        listenerCount++;
        System.out.println("Listeners added: " + listenerCount);
    }

    public void removeListener(HeartAnimListener listener) {
        super.removeListener(listener);
        listenerCount--;
        System.out.println("Listeners removed: " + listenerCount);
        System.out.println("Active listeners: " + listenerCount);
    }
}
```

### 3. Weak Reference Pattern

```java
// ✅ ADVANCED - WeakReference로 자동 정리
public class AutoCleanupListener {
    private WeakReference<MyMonster> monsterRef;

    public AutoCleanupListener(MyMonster monster) {
        this.monsterRef = new WeakReference<>(monster);
    }

    public void onEvent() {
        MyMonster monster = monsterRef.get();
        if (monster == null) {
            // 몬스터가 GC되면 자동으로 리스너 무효화
            return;
        }
        // 처리 로직
    }
}
```

---

## Mod Development Guidelines

### 1. 리스너 등록 시 체크리스트

```java
public class MyCustomMonster extends AbstractMonster {
    // ✅ 1. 리스너를 필드로 저장
    private MyAnimListener animListener;

    public MyCustomMonster() {
        // ✅ 2. 명시적 생성
        this.animListener = new MyAnimListener();

        // ✅ 3. 등록
        this.state.addListener(animListener);
    }

    @Override
    public void die() {
        // ✅ 4. 제거 (가장 중요!)
        if (this.animListener != null) {
            this.state.removeListener(this.animListener);
            this.animListener = null;
        }
        super.die();
    }
}
```

### 2. Event 클래스 리스너 관리

```java
public class MyEvent extends AbstractEvent {
    private AnimatedNpc npc;
    private MyListener listener;

    public MyEvent() {
        this.npc = new AnimatedNpc(...);
        this.listener = new MyListener();
        this.npc.addListener(listener);
    }

    @Override
    public void dispose() {
        // ⭐ 중요: NPC dispose 전에 리스너 제거
        if (this.npc != null && this.listener != null) {
            // AnimatedNpc에 removeListener가 없다면:
            // Reflection 또는 직접 state 접근
            this.npc.dispose();
        }
        super.dispose();
    }
}
```

### 3. 컨트롤러 리스너 (전역)

```java
public class MyInputHandler {
    private static ControllerListener listener;

    // 게임 시작 시 1회만
    public static void initialize() {
        if (listener == null) {
            listener = new ControllerListener();
            Controllers.addListener(listener);
        }
    }

    // 게임 종료 시
    public static void cleanup() {
        if (listener != null) {
            Controllers.removeListener(listener);
            listener = null;
        }
    }
}
```

---

## Testing Strategies

### 1. Listener Count Test

```java
@Test
public void testListenerCleanup() {
    // Given
    AnimationState state = new AnimationState();
    MyMonster monster = new MyMonster();
    int initialCount = getListenerCount(state);

    // When
    monster.die();

    // Then
    int finalCount = getListenerCount(state);
    assertEquals(initialCount, finalCount);
}

private int getListenerCount(AnimationState state) {
    // Reflection을 사용해 listeners 필드 접근
    try {
        Field listenersField = AnimationState.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Array listeners = (Array) listenersField.get(state);
        return listeners.size;
    } catch (Exception e) {
        return -1;
    }
}
```

### 2. Memory Leak Test

```java
@Test
public void testNoMemoryLeak() {
    // 전투 100회 시뮬레이션
    for (int i = 0; i < 100; i++) {
        // 몬스터 생성
        MyMonster monster = new MyMonster();

        // 리스너 등록 확인
        assertNotNull(monster.getListener());

        // 전투 종료
        monster.die();

        // 명시적 null 할당
        monster = null;
    }

    // GC 강제 실행
    System.gc();
    Thread.sleep(1000);

    // 메모리 사용량 확인
    long usedMemory = Runtime.getRuntime().totalMemory() -
                      Runtime.getRuntime().freeMemory();

    // 메모리 증가가 없어야 함 (허용 오차 10MB)
    assertTrue(usedMemory < 10 * 1024 * 1024);
}
```

---

## Performance Impact Analysis

### 리스너 누수 시뮬레이션

**시나리오**: 슬라임과 100번 전투

**누수 없음**:
```
전투당 리스너: 3개
총 리스너: 3개 (재사용)
이벤트 처리: O(3) = 상수 시간
메모리: 1KB
```

**누수 있음**:
```
전투 1: 3개
전투 2: 6개
전투 3: 9개
...
전투 100: 300개
이벤트 처리: O(300) = 100배 느림
메모리: 100KB (100배 증가)
```

### 이벤트 처리 오버헤드

```java
// 리스너가 누적되면:
public void onAnimationComplete(String animationName) {
    // 모든 리스너가 호출됨
    // 누수 있음: 300번 호출 (297번은 쓸모없음)
    // 누수 없음: 3번 호출
}
```

---

## Common Pitfalls

### 1. 조건부 등록/제거 불일치

```java
// ❌ BAD - 등록은 항상, 제거는 조건부
public MyMonster() {
    listener = new Listener();
    state.addListener(listener);  // 항상 등록
}

public void die() {
    if (someCondition) {  // ⚠️ 조건부 제거
        state.removeListener(listener);
    }
}

// ✅ GOOD - 대칭적 처리
public void die() {
    if (listener != null) {  // null 체크만
        state.removeListener(listener);
    }
}
```

### 2. Super 호출 순서

```java
// ❌ BAD - super 먼저 호출
public void die() {
    super.die();  // 부모가 state를 정리할 수도
    state.removeListener(listener);  // 이미 늦음!
}

// ✅ GOOD - 정리 먼저
public void die() {
    state.removeListener(listener);
    super.die();
}
```

### 3. 다중 등록

```java
// ❌ BAD - 중복 등록
public void resetAnimation() {
    state.addListener(listener);  // 매번 추가!
}

// ✅ GOOD - 등록 전 제거
public void resetAnimation() {
    state.removeListener(listener);
    state.addListener(listener);
}
```

---

## Summary

| 패턴 | 위험도 | 발견 빈도 | 수정 난이도 |
|------|--------|-----------|-------------|
| Anonymous listener | HIGH | 매우 높음 | 중간 |
| No removal in die() | HIGH | 높음 | 낮음 |
| Missing removeListener API | MEDIUM | 낮음 | 높음 |
| Conditional cleanup | MEDIUM | 중간 | 낮음 |

**핵심 원칙**:
1. ✅ 모든 addListener는 대응하는 removeListener 필요
2. ✅ 리스너를 필드로 저장 (제거 시 참조 필요)
3. ✅ 객체 정리 시 (die, dispose) 리스너 제거
4. ✅ API 제공 시 add/remove 쌍으로 제공
5. ❌ 익명 리스너는 제거 불가능하므로 피할 것

**모드 개발 체크리스트**:
- [ ] 모든 addListener 호출에 대응하는 removeListener 확인
- [ ] 리스너를 필드로 저장하여 참조 유지
- [ ] die()/dispose() 메서드에 리스너 제거 코드 추가
- [ ] 조건부 등록/제거 대칭성 확인
- [ ] 메모리 프로파일러로 리스너 누적 테스트
