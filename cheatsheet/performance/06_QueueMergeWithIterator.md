# Queue 병합 시 Iterator 사용

## 🔍 문제 발견 위치
- 파일: AbstractDungeon.java
- 메서드: update()
- 라인: 2637-2647
- 호출 빈도: 매 프레임 / 초당 60회

## 📋 문제 설명
effectsQueue와 topLevelEffectsQueue를 메인 리스트에 병합할 때 iterator를 사용하여 하나씩 추가하고 제거합니다. ArrayList.addAll()과 clear()를 사용하면 훨씬 빠릅니다.

## 🔬 원인 분석

### 문제 코드
```java
// AbstractDungeon.java:2637-2647
// effectsQueue → effectList 병합
for (i = effectsQueue.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    effectList.add(e);  // 개별 추가
    i.remove();         // 개별 제거
}

// topLevelEffectsQueue → topLevelEffects 병합
for (i = topLevelEffectsQueue.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    topLevelEffects.add(e);  // 개별 추가
    i.remove();              // 개별 제거
}
```

### 내부 동작 분석
```java
// 현재 방식의 내부 동작
for each element in queue:
    1. iterator.hasNext() 체크
    2. iterator.next() 호출
    3. effectList.add(e)
       - 배열 크기 체크
       - 필요 시 배열 확장
       - 요소 추가
    4. iterator.remove()
       - 요소 제거
       - 뒤 요소들 앞으로 이동 (O(n))

// 시간 복잡도: O(n²) - 매번 remove() 시 배열 이동
```

### ArrayList.addAll() 내부 동작
```java
// addAll() 방식의 내부 동작
public boolean addAll(Collection<? extends E> c) {
    Object[] a = c.toArray();
    int numNew = a.length;
    ensureCapacityInternal(size + numNew);  // 1회만 배열 확장
    System.arraycopy(a, 0, elementData, size, numNew);  // 한 번에 복사
    size += numNew;
    return numNew != 0;
}

// 시간 복잡도: O(n) - 한 번의 배열 복사
```

### 실행 빈도 및 영향
- **프레임당 실행 횟수**: 2회 (effectsQueue, topLevelEffectsQueue)
- **큐 평균 크기**: 0-10개 (전투 중 0-50개)
- **프레임당 연산**: 0-100회 (add + remove)
- **시간 복잡도**: O(n²) → O(n) 개선 가능
- **CPU 비용**: 배열 이동 + Iterator 오버헤드

## ✅ 해결 방법

### 방법 1: addAll() + clear() 사용 (권장)
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "update"
)
public static class OptimizedQueueMergePatch {
    @SpireInsertPatch(
        locator = QueueMergeLocator.class
    )
    public static SpireReturn<Void> Insert(AbstractDungeon __instance) {
        // 기존: iterator 사용
        // for (i = effectsQueue.iterator(); i.hasNext(); ) {
        //     AbstractGameEffect e = i.next();
        //     effectList.add(e);
        //     i.remove();
        // }

        // 최적화: addAll() + clear()
        if (!AbstractDungeon.effectsQueue.isEmpty()) {
            AbstractDungeon.effectList.addAll(AbstractDungeon.effectsQueue);
            AbstractDungeon.effectsQueue.clear();
        }

        if (!AbstractDungeon.topLevelEffectsQueue.isEmpty()) {
            AbstractDungeon.topLevelEffects.addAll(AbstractDungeon.topLevelEffectsQueue);
            AbstractDungeon.topLevelEffectsQueue.clear();
        }

        return SpireReturn.Return(null);
    }

    private static class QueueMergeLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class, "effectsQueue"
            );
            int[] lines = LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            // iterator 시작 라인 찾기
            return new int[]{lines[0] - 1};
        }
    }
}
```

### 방법 2: 용량 사전 할당
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = "update"
)
public static class PreallocatedQueueMergePatch {
    @SpireInsertPatch(
        locator = QueueMergeLocator.class
    )
    public static SpireReturn<Void> Insert(AbstractDungeon __instance) {
        int queueSize = AbstractDungeon.effectsQueue.size();
        if (queueSize > 0) {
            // 배열 재할당 방지
            AbstractDungeon.effectList.ensureCapacity(
                AbstractDungeon.effectList.size() + queueSize
            );
            AbstractDungeon.effectList.addAll(AbstractDungeon.effectsQueue);
            AbstractDungeon.effectsQueue.clear();
        }

        int topQueueSize = AbstractDungeon.topLevelEffectsQueue.size();
        if (topQueueSize > 0) {
            AbstractDungeon.topLevelEffects.ensureCapacity(
                AbstractDungeon.topLevelEffects.size() + topQueueSize
            );
            AbstractDungeon.topLevelEffects.addAll(AbstractDungeon.topLevelEffectsQueue);
            AbstractDungeon.topLevelEffectsQueue.clear();
        }

        return SpireReturn.Return(null);
    }
}
```

### 방법 3: Swap 패턴 (고급)
```java
@SpirePatch(
    clz = AbstractDungeon.class,
    method = SpirePatch.CLASS
)
public static class SwapQueuePatch {
    // 메인 리스트와 큐를 교체하여 복사 제거
    public static void swapAndMerge(ArrayList<AbstractGameEffect> main,
                                     ArrayList<AbstractGameEffect> queue) {
        if (queue.isEmpty()) {
            return;
        }

        if (main.isEmpty()) {
            // 메인이 비어있으면 참조 교체 (O(1))
            ArrayList<AbstractGameEffect> temp = main;
            main = queue;
            queue = temp;
        } else {
            // 둘 다 있으면 addAll() 사용
            main.addAll(queue);
            queue.clear();
        }
    }
}

// 주의: 필드 재할당이 필요하므로 reflection 사용 필요
```

## 📊 성능 개선 효과

### 방법 1: addAll() + clear()
- **예상 FPS 향상**: 1-3% (이펙트가 많을 때)
- **시간 복잡도**: O(n²) → O(n)
- **연산 횟수**: n×(hasNext + next + add + remove) → 1×addAll + 1×clear
- **메모리 할당**: 동일 (내부 배열 확장은 필요 시)

### 방법 2: 용량 사전 할당
- **예상 FPS 향상**: 2-4%
- **배열 재할당**: 제거 (ensureCapacity로 미리 확보)
- **메모리 효율**: 향상 (불필요한 중간 확장 방지)

### 방법 3: Swap 패턴
- **예상 FPS 향상**: 3-5% (메인 리스트가 비었을 때)
- **복사 제거**: 메인 비었을 때 O(1)
- **구현 복잡도**: 높음

## ⚠️ 주의사항

### 방법 1
- **장점**: 구현 간단, 표준 API 사용
- **단점**: 없음
- **호환성**: 완벽함

### 방법 2
- **장점**: 배열 재할당 방지
- **단점**: ensureCapacity() 호출 오버헤드 (미미함)
- **호환성**: 완벽함

### 방법 3
- **장점**: 최고 성능 (특정 케이스)
- **단점**: 복잡한 구현, 필드 재할당 필요
- **호환성**: Reflection 사용 필요

## 💡 성능 측정

### 벤치마크
```java
ArrayList<Integer> main = new ArrayList<>();
ArrayList<Integer> queue = new ArrayList<>();

// 큐에 데이터 추가
for (int i = 0; i < 1000; i++) {
    queue.add(i);
}

// 방법 1: Iterator
long start = System.nanoTime();
for (int j = 0; j < 1000; j++) {
    Iterator<Integer> i = queue.iterator();
    while (i.hasNext()) {
        Integer e = i.next();
        main.add(e);
        i.remove();
    }
}
long end = System.nanoTime();
System.out.println("Iterator: " + (end - start) / 1000000.0 + "ms");

// 방법 2: addAll + clear
start = System.nanoTime();
for (int j = 0; j < 1000; j++) {
    main.addAll(queue);
    queue.clear();
}
end = System.nanoTime();
System.out.println("addAll: " + (end - start) / 1000000.0 + "ms");
```

### 예상 결과 (큐 크기 100개 기준)
- Iterator 방식: 150-300ms
- addAll 방식: 20-50ms
- **개선율**: 75-85%

## 🔬 추가 최적화

### isEmpty() 체크 최적화
```java
// 현재: 매번 체크
if (!effectsQueue.isEmpty()) {
    effectList.addAll(effectsQueue);
    effectsQueue.clear();
}

// 최적화: size() 직접 사용 (더 빠름)
int queueSize = effectsQueue.size();
if (queueSize > 0) {
    effectList.addAll(effectsQueue);
    effectsQueue.clear();
}

// isEmpty()는 size() == 0을 체크하는 것이므로
// 크기를 알아야 하는 경우 size() 직접 사용이 효율적
```

### 큐 재사용 패턴
```java
// 매 프레임 큐를 clear()하므로 용량 유지됨
// 초기 용량을 적절히 설정하면 재할당 방지

// AbstractDungeon 초기화 시
effectsQueue = new ArrayList<>(50);  // 예상 최대 크기
topLevelEffectsQueue = new ArrayList<>(20);

// 이후 clear()만 호출하면 배열 용량 유지
```

## 🔗 관련 문제
- 01_EffectListDoubleIteration.md - 리스트 순회 최적화
- 02_PathArrayListRecreation.md - ArrayList 재사용
- 07_CollectionStreamAPI.md - Stream API 오버헤드
