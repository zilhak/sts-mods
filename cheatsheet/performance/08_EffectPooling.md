# 08_EffectPooling - No Object Pooling for Effects

**Category**: VFX & Effects
**Impact**: High (30-50% allocation reduction potential)
**Difficulty**: Medium
**Detection**: Every `new Effect()` creates heap allocation

---

## Problem

**Slay the Spire never reuses effect objects**. Every visual effect creates new instances that get garbage collected, causing:
- Constant memory allocation/deallocation cycles
- GC pauses during intense combat
- Memory fragmentation over time
- Cascading allocation chains (one effect spawns many others)

### Evidence from Decompiled Code

**AbstractGameEffect.java** - No pooling support:
```java
public abstract class AbstractGameEffect implements Disposable {
    public float duration;
    public boolean isDone = false;

    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0F) {
            this.isDone = true;  // ❌ No reset() or return to pool
        }
    }

    public void dispose() {}  // ❌ Empty - no cleanup
}
```

**BloodShotEffect.java** - Cascading allocations:
```java
public void update() {
    this.timer -= Gdx.graphics.getDeltaTime();
    if (this.timer < 0.0F) {
        this.timer += MathUtils.random(0.05F, 0.15F);
        // ❌ Creates new particle every 50-150ms
        AbstractDungeon.effectsQueue.add(
            new BloodShotParticleEffect(this.sX, this.sY, this.tX, this.tY)
        );
        this.count--;
    }
}
```

**DaggerSprayEffect.java** - Batch allocation spike:
```java
public void update() {
    this.isDone = true;
    for (int i = 0; i < 12; i++) {  // ❌ 12 allocations instantly
        AbstractDungeon.effectsQueue.add(
            new FlyingDaggerEffect(x, y, angle, flipX)
        );
    }
}
```

**LightningEffect.java** - Nested cascades:
```java
if (this.duration == this.startingDuration) {
    for (int i = 0; i < 15; i++) {  // ❌ 15 allocations
        AbstractDungeon.topLevelEffectsQueue.add(
            new ImpactSparkEffect(x, y)
        );
    }
}
```

---

## Performance Impact

### Allocation Hotspots

| Effect Type | Allocations/Call | Frequency | Impact |
|-------------|------------------|-----------|---------|
| `DaggerSprayEffect` | 12 objects | Per card use | High |
| `LightningEffect` | 15 objects | Per lightning orb | High |
| `BloodShotEffect` | 1/50-150ms | Continuous | Medium |
| `RainingGoldEffect` | 3-18/tick | Gold rewards | High |
| `DarkSmokePuffEffect` | 20 objects | Per smoke puff | Medium |

### Memory Pressure Example
```
Turn 1:
- Play Whirlwind (5 enemies)
  → 5 × CleaveEffect
  → Each spawns 3 slash particles
  → 15 SlashEffect + 5 CleaveEffect = 20 allocations

- Enemy attacks
  → 5 × DamageNumberEffect
  → 5 × BlockedNumberEffect
  → 10 allocations

- Cast Lightning
  → 1 × LightningEffect
  → 15 × ImpactSparkEffect
  → 16 allocations

Total: 46 effect objects in one turn (no reuse)
```

---

## Mod Solution: Effect Object Pool

### 1. Generic Effect Pool

```java
public class EffectPool<T extends AbstractGameEffect> {
    private final Queue<T> pool;
    private final Supplier<T> factory;
    private final int maxPoolSize;

    public EffectPool(Supplier<T> factory, int initialSize, int maxSize) {
        this.pool = new ArrayDeque<>(initialSize);
        this.factory = factory;
        this.maxPoolSize = maxSize;

        // Pre-warm pool
        for (int i = 0; i < initialSize; i++) {
            pool.offer(factory.get());
        }
    }

    public T obtain() {
        T effect = pool.poll();
        if (effect == null) {
            // Pool empty, create new (should be rare)
            return factory.get();
        }
        return effect;
    }

    public void free(T effect) {
        if (pool.size() < maxPoolSize) {
            effect.reset();  // Reset state
            pool.offer(effect);
        }
    }

    public void clear() {
        pool.clear();
    }
}
```

### 2. Poolable Effect Interface

```java
public interface Poolable {
    void reset();  // Reset to initial state
}

// Example implementation
public class BloodShotParticleEffect extends AbstractGameEffect implements Poolable {
    private float sX, sY, tX, tY;
    // ... other fields

    public void init(float sX, float sY, float tX, float tY) {
        this.sX = sX + MathUtils.random(-90.0F, 90.0F) * Settings.scale;
        this.sY = sY + MathUtils.random(-90.0F, 90.0F) * Settings.scale;
        this.tX = tX;
        this.tY = tY;
        this.duration = this.startingDuration;
        this.isDone = false;
        this.activated = false;
        // ... reset other fields
    }

    @Override
    public void reset() {
        this.sX = this.sY = this.tX = this.tY = 0;
        this.duration = 0;
        this.isDone = false;
        this.activated = false;
        this.color.set(1, 1, 1, 1);
    }
}
```

### 3. Pool Manager Patch

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class EffectPoolManager {
    public static EffectPool<BloodShotParticleEffect> bloodShotPool;
    public static EffectPool<ImpactSparkEffect> impactSparkPool;
    public static EffectPool<DamageNumberEffect> damageNumberPool;

    @SpirePostfixPatch
    public static void Initialize(AbstractDungeon __instance) {
        bloodShotPool = new EffectPool<>(
            BloodShotParticleEffect::new, 20, 100
        );
        impactSparkPool = new EffectPool<>(
            ImpactSparkEffect::new, 30, 200
        );
        damageNumberPool = new EffectPool<>(
            DamageNumberEffect::new, 10, 50
        );
    }
}
```

### 4. Effect Update Patch with Auto-Return

```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class EffectPoolReturnPatch {
    @SpireInsertPatch(locator = EffectRemoveLocator.class)
    public static void returnToPool(AbstractGameEffect e, Iterator<?> i) {
        if (e.isDone && e instanceof Poolable) {
            // Return to appropriate pool before removal
            if (e instanceof BloodShotParticleEffect) {
                EffectPoolManager.bloodShotPool.free((BloodShotParticleEffect) e);
            } else if (e instanceof ImpactSparkEffect) {
                EffectPoolManager.impactSparkPool.free((ImpactSparkEffect) e);
            }
            // ... handle other types
        }
        i.remove();
    }
}
```

### 5. Usage Example

```java
// ❌ Before: Direct allocation
AbstractDungeon.effectsQueue.add(
    new BloodShotParticleEffect(sX, sY, tX, tY)
);

// ✅ After: Pool allocation
BloodShotParticleEffect effect = EffectPoolManager.bloodShotPool.obtain();
effect.init(sX, sY, tX, tY);
AbstractDungeon.effectsQueue.add(effect);
```

---

## Verification

### Pool Hit Rate Metrics
```java
public class EffectPool<T extends AbstractGameEffect> {
    private int obtainCount = 0;
    private int createCount = 0;

    public T obtain() {
        obtainCount++;
        T effect = pool.poll();
        if (effect == null) {
            createCount++;
            return factory.get();
        }
        return effect;
    }

    public float getHitRate() {
        if (obtainCount == 0) return 0f;
        return 1.0f - ((float) createCount / obtainCount);
    }
}

// Log in dev console
System.out.printf("Blood Shot Pool Hit Rate: %.2f%%\n",
    EffectPoolManager.bloodShotPool.getHitRate() * 100);
```

### Expected Results
- Pool hit rate >90% after warm-up
- GC pause reduction: 30-50%
- Memory allocation reduction: 40-60%

---

## Related Issues

- **09_ParticleLimit.md**: Combine with particle throttling for maximum effect
- **10_EffectCascades.md**: Pooling helps but doesn't prevent cascades

---

## References

- Decompiled: `com/megacrit/cardcrawl/vfx/AbstractGameEffect.java`
- Decompiled: `com/megacrit/cardcrawl/vfx/combat/*Effect.java`
- Pattern: LibGDX Pooling (com.badlogic.gdx.utils.Pool)
