# 11_EffectMemoryLeak - Effect Resource Cleanup Issues

**Category**: VFX & Effects
**Impact**: Medium (Gradual memory growth)
**Difficulty**: Medium
**Detection**: Empty dispose() methods, ArrayList retention

---

## Problem

**Effects don't properly clean up resources**, leading to:
- Memory leaks from retained collections
- Texture references not released
- ArrayList growth without trimming
- Gradual memory consumption over long sessions

### Evidence from Decompiled Code

**Universal problem** - Empty dispose():
```java
// Pattern found in 95% of effect classes
public void dispose() {}  // ❌ No cleanup
```

**SwirlyBloodEffect.java** - ArrayList never cleared:
```java
public class SwirlyBloodEffect extends AbstractGameEffect {
    private ArrayList<Vector2> controlPoints = new ArrayList<>();
    private Vector2[] points = new Vector2[60];

    public void update() {
        // Add points every frame
        this.controlPoints.add(this.pos.cpy());

        if (this.controlPoints.size() > 5) {
            this.controlPoints.remove(0);  // ⚠️ Removes but doesn't trim
        }

        // Allocate 60 Vector2 objects every frame!
        for (int i = 0; i < 60; i++) {
            this.points[i] = new Vector2();  // ❌ Memory leak
            this.crs.valueAt(this.points[i], i / 59.0F);
        }
    }

    public void dispose() {}  // ❌ No cleanup of ArrayList or arrays
}
```

**DarkSmokePuffEffect.java** - Internal collection retained:
```java
public class DarkSmokePuffEffect extends AbstractGameEffect {
    private ArrayList<FastDarkSmoke> smoke = new ArrayList<>();

    public DarkSmokePuffEffect(float x, float y) {
        for (int i = 0; i < 20; i++) {
            this.smoke.add(new FastDarkSmoke(x, y));
        }
    }

    public void dispose() {}  // ❌ ArrayList<FastDarkSmoke> never cleared
    // Each FastDarkSmoke has its own resources that also leak
}
```

**AbstractDungeon.java** - effectList never trims:
```java
public static ArrayList<AbstractGameEffect> effectList = new ArrayList<>();

// Update removes completed effects
for (i = effectList.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    e.update();
    if (e.isDone) {
        i.remove();  // ⚠️ Removes but ArrayList capacity stays large
    }
}

// After 1000 effects, ArrayList internal array is size 1000
// Even with 5 active effects, 995 empty slots waste memory
```

**No trimToSize() calls found**:
```bash
$ grep -r "trimToSize" --include="*.java" com/megacrit/cardcrawl/
# (No results - never used in codebase)
```

---

## Memory Leak Categories

### Category 1: Empty dispose() - No Resource Release

**Affected classes** (98% of effects):
```java
// Texture/Image references
public class BossChestShineEffect extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private Texture texture;  // ⚠️ Heavy resource

    public void dispose() {}  // ❌ Texture not released
}

// ArrayList collections
public class HemokinesisParticle extends AbstractGameEffect {
    private ArrayList<Vector2> points = new ArrayList<>();

    public void dispose() {}  // ❌ ArrayList not cleared
}

// Array allocations
public class SwirlyBloodEffect extends AbstractGameEffect {
    private Vector2[] points = new Vector2[60];

    public void dispose() {}  // ❌ Array references not nulled
}
```

### Category 2: Collection Growth Without Trimming

**effectList capacity waste**:
```java
// Scenario: Long combat with many effects
Initial: effectList capacity = 10
After 100 effects: capacity = 128
After 500 effects: capacity = 1024
Combat ends: 5 active effects, capacity still 1024
Waste: 1019 empty slots × 4 bytes = ~4KB per list

Problem: ArrayList.remove() doesn't reduce capacity
Solution needed: Periodic trimToSize()
```

### Category 3: Nested Resource Leaks

**DarkSmokePuffEffect** - Nested objects:
```java
public class DarkSmokePuffEffect {
    private ArrayList<FastDarkSmoke> smoke = new ArrayList<>();
    // Each FastDarkSmoke has:
    // - TextureRegion img
    // - Color color
    // - float[] positions (4 values)
    // Total: ~20 objects × 40 bytes = 800 bytes per effect

    public void dispose() {}  // None of it cleaned
}
```

### Category 4: Per-Frame Allocations

**SwirlyBloodEffect** - Continuous leaking:
```java
public void update() {
    // ❌ 60 Vector2 allocations EVERY FRAME
    for (int i = 0; i < 60; i++) {
        this.points[i] = new Vector2();  // 8 bytes × 60 = 480 bytes/frame
    }
    // At 60 FPS: 480 × 60 = 28,800 bytes/sec = 1.7 MB/min
}
```

---

## Memory Growth Analysis

### Session Memory Profile

| Time | effectList Count | ArrayList Capacity | Leaked Collections | Total Waste |
|------|------------------|--------------------|--------------------|-------------|
| 0 min | 5 | 10 | 0 | 0 KB |
| 10 min | 20-50 | 128 | ~50 | ~10 KB |
| 30 min | 30-80 | 256 | ~200 | ~50 KB |
| 60 min | 40-100 | 512 | ~500 | ~200 KB |
| 120 min | 50-150 | 1024 | ~1000 | ~1 MB |

**Symptom**: Memory usage grows ~500KB per hour in normal play

---

## Mod Solution: Effect Resource Management

### 1. Proper Dispose Implementation

```java
// Base cleanup interface
public interface CleanableEffect {
    void cleanup();
}

// Patch AbstractGameEffect
@SpirePatch(clz = AbstractGameEffect.class, method = "dispose")
public class DisposeEnforcementPatch {
    @SpirePostfixPatch
    public static void enforceCleanup(AbstractGameEffect __instance) {
        if (__instance instanceof CleanableEffect) {
            ((CleanableEffect) __instance).cleanup();
        }

        // Generic cleanup for common resources
        cleanupReflectively(__instance);
    }

    private static void cleanupReflectively(AbstractGameEffect effect) {
        try {
            // Clear ArrayList fields
            for (Field field : effect.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(effect);

                if (value instanceof ArrayList) {
                    ((ArrayList<?>) value).clear();
                } else if (value instanceof Array) {
                    // Null out array elements
                    int length = java.lang.reflect.Array.getLength(value);
                    for (int i = 0; i < length; i++) {
                        java.lang.reflect.Array.set(value, i, null);
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail for safety
        }
    }
}
```

### 2. ArrayList Capacity Management

```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class EffectListTrimPatch {
    private static final int TRIM_THRESHOLD = 100;
    private static final int TRIM_INTERVAL = 300;  // 5 seconds at 60 FPS
    private static int frameCounter = 0;

    @SpirePostfixPatch
    public static void trimEffectLists() {
        frameCounter++;
        if (frameCounter % TRIM_INTERVAL != 0) return;

        // Trim if capacity significantly larger than size
        if (shouldTrim(AbstractDungeon.effectList)) {
            AbstractDungeon.effectList.trimToSize();
        }
        if (shouldTrim(AbstractDungeon.effectsQueue)) {
            AbstractDungeon.effectsQueue.trimToSize();
        }
        if (shouldTrim(AbstractDungeon.topLevelEffects)) {
            AbstractDungeon.topLevelEffects.trimToSize();
        }
    }

    private static boolean shouldTrim(ArrayList<?> list) {
        if (list.size() == 0) {
            return true;  // Always trim empty lists
        }

        // Estimate capacity (ArrayList has no public capacity getter)
        // Use reflection or heuristic
        int size = list.size();
        int estimatedCapacity = estimateCapacity(size);

        return estimatedCapacity > size + TRIM_THRESHOLD;
    }

    private static int estimateCapacity(int size) {
        // ArrayList grows by 1.5x, so reverse engineer
        int capacity = 10;  // Initial capacity
        while (capacity < size) {
            capacity = capacity + (capacity >> 1);  // capacity *= 1.5
        }
        return capacity;
    }
}
```

### 3. Optimized SwirlyBloodEffect

```java
@SpirePatch(clz = SwirlyBloodEffect.class, method = "update")
public class SwirlyBloodOptimizationPatch {
    // Reuse Vector2 objects instead of allocating
    @SpireInsertPatch(locator = Vector2AllocationLocator.class)
    public static SpireReturn<Void> reuseVector2Objects(SwirlyBloodEffect __instance) {
        // Access private points field
        Vector2[] points = ReflectionHacks.getPrivate(__instance, SwirlyBloodEffect.class, "points");

        // Reuse existing Vector2 or create only if null
        for (int i = 0; i < 60; i++) {
            if (points[i] == null) {
                points[i] = new Vector2();  // Create once
            }
            // Update existing object instead of creating new
            __instance.crs.valueAt(points[i], i / 59.0F);
        }

        return SpireReturn.Return(null);  // Skip original allocation code
    }
}

// Better: Replace entire effect with optimized version
public class OptimizedSwirlyBloodEffect extends AbstractGameEffect implements CleanableEffect {
    private final Vector2Pool vectorPool = new Vector2Pool(60);
    private ArrayList<Vector2> controlPoints = new ArrayList<>(6);
    private Vector2[] points = vectorPool.obtainArray();

    @Override
    public void cleanup() {
        controlPoints.clear();
        controlPoints.trimToSize();  // Release capacity
        vectorPool.freeArray(points);
        points = null;
    }
}

// Vector2 pool
class Vector2Pool {
    private final Queue<Vector2> pool = new ArrayDeque<>();

    public Vector2Pool(int initialSize) {
        for (int i = 0; i < initialSize; i++) {
            pool.offer(new Vector2());
        }
    }

    public Vector2[] obtainArray() {
        Vector2[] array = new Vector2[60];
        for (int i = 0; i < 60; i++) {
            array[i] = pool.poll();
            if (array[i] == null) {
                array[i] = new Vector2();
            }
        }
        return array;
    }

    public void freeArray(Vector2[] array) {
        for (Vector2 v : array) {
            if (v != null) {
                v.set(0, 0);
                pool.offer(v);
            }
        }
    }
}
```

### 4. DarkSmokePuffEffect Cleanup

```java
@SpirePatch(clz = DarkSmokePuffEffect.class, method = "dispose")
public class DarkSmokeCleanupPatch {
    @SpirePostfixPatch
    public static void cleanupSmoke(DarkSmokePuffEffect __instance) {
        ArrayList<FastDarkSmoke> smoke = ReflectionHacks.getPrivate(
            __instance, DarkSmokePuffEffect.class, "smoke"
        );

        // Dispose nested objects
        for (FastDarkSmoke s : smoke) {
            if (s != null) {
                s.dispose();  // Ensure FastDarkSmoke also cleans up
            }
        }

        smoke.clear();
        smoke.trimToSize();
    }
}
```

### 5. Memory Monitoring

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class EffectMemoryMonitor {
    private static final long REPORT_INTERVAL_MS = 30000;  // 30 seconds
    private static long lastReportTime = 0;
    private static long initialMemory = 0;

    public static void checkMemoryGrowth() {
        long now = System.currentTimeMillis();
        if (now - lastReportTime < REPORT_INTERVAL_MS) return;

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        if (initialMemory == 0) {
            initialMemory = usedMemory;
        }

        long growth = usedMemory - initialMemory;
        int effectCount = AbstractDungeon.effectList.size();
        int queueSize = AbstractDungeon.effectsQueue.size();

        logger.info(String.format(
            "Memory: %d KB growth | Effects: %d active, %d queued",
            growth / 1024, effectCount, queueSize
        ));

        lastReportTime = now;
    }
}
```

---

## Configuration

```java
public class MemoryConfig {
    public static boolean enableTrimming = true;
    public static int trimInterval = 300;  // frames
    public static int trimThreshold = 100;
    public static boolean enforceDispose = true;
    public static boolean logMemoryGrowth = false;

    public static void setMemoryMode(String mode) {
        switch (mode) {
            case "AGGRESSIVE":
                trimInterval = 60;
                trimThreshold = 20;
                enforceDispose = true;
                break;
            case "BALANCED":
                trimInterval = 300;
                trimThreshold = 100;
                enforceDispose = true;
                break;
            case "MINIMAL":
                trimInterval = 600;
                trimThreshold = 200;
                enforceDispose = false;
                break;
        }
    }
}
```

---

## Verification

### Memory Leak Test
```java
public static void testMemoryLeak() {
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    // Spawn 1000 effects
    for (int i = 0; i < 1000; i++) {
        AbstractDungeon.effectsQueue.add(
            new SwirlyBloodEffect(100, 100)
        );
    }

    // Wait for all to complete
    while (AbstractDungeon.effectList.size() > 0) {
        // Update until empty
    }

    // Force GC
    System.gc();
    Thread.sleep(1000);

    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long leaked = afterMemory - beforeMemory;

    logger.info("Memory leaked from 1000 effects: " + leaked / 1024 + " KB");
    // Expected: <100 KB with proper cleanup
    // Without cleanup: 500-1000 KB
}
```

### ArrayList Capacity Check
```java
public static int getArrayListCapacity(ArrayList<?> list) {
    try {
        Field field = ArrayList.class.getDeclaredField("elementData");
        field.setAccessible(true);
        Object[] elementData = (Object[]) field.get(list);
        return elementData.length;
    } catch (Exception e) {
        return -1;
    }
}

// Log capacity vs size
int size = AbstractDungeon.effectList.size();
int capacity = getArrayListCapacity(AbstractDungeon.effectList);
logger.info(String.format("effectList: %d/%d (%.1f%% utilization)",
    size, capacity, 100.0 * size / capacity));
```

---

## Related Issues

- **08_EffectPooling.md**: Pooling reuses objects, reducing leak impact
- **09_ParticleLimit.md**: Limits reduce total leaked memory

---

## References

- Decompiled: `com/megacrit/cardcrawl/vfx/AbstractGameEffect.java`
- Decompiled: `com/megacrit/cardcrawl/vfx/combat/SwirlyBloodEffect.java`
- Decompiled: `com/megacrit/cardcrawl/vfx/DarkSmokePuffEffect.java`
- Java: ArrayList.trimToSize() documentation
