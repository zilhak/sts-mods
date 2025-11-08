# 10_EffectCascades - Cascading Effect Creation

**Category**: VFX & Effects
**Impact**: Critical (Exponential growth)
**Difficulty**: Hard
**Detection**: Effects that spawn effects in update()

---

## Problem

**Effects create other effects during their lifetime**, leading to exponential growth:
- Parent effect spawns N children
- Each child spawns M grandchildren
- O(N^depth) complexity explosion
- Can trigger 500+ effects from single card

### Evidence from Decompiled Code

**BloodShotParticleEffect.java** - Effect creates effect:
```java
public void update() {
    // ... update logic
    if (this.duration < 0.0F) {
        // ❌ Dying effect spawns new effect
        AbstractDungeon.effectsQueue.add(
            new AdditiveSlashImpactEffect(this.tX, this.tY, this.color.cpy())
        );
        this.isDone = true;
    }
}
```

**BloodShotEffect.java** - Parent spawner:
```java
public void update() {
    this.timer -= Gdx.graphics.getDeltaTime();
    if (this.timer < 0.0F) {
        this.timer += MathUtils.random(0.05F, 0.15F);
        // ❌ Spawns particle every 50-150ms
        AbstractDungeon.effectsQueue.add(
            new BloodShotParticleEffect(sX, sY, tX, tY)
        );
        this.count--;
    }
}
```

**Cascade chain**:
```
BloodShotEffect (count=10)
├── BloodShotParticleEffect #1
│   └── AdditiveSlashImpactEffect (on death)
├── BloodShotParticleEffect #2
│   └── AdditiveSlashImpactEffect (on death)
...
└── BloodShotParticleEffect #10
    └── AdditiveSlashImpactEffect (on death)

Total: 1 + 10 + 10 = 21 effects from one BloodShotEffect
```

---

## Cascade Patterns

### Pattern 1: Death Spawn

**Many effects spawn on completion**:

```java
// CollectorStakeEffect.java
if (this.duration < 0.0F) {
    AbstractDungeon.effectsQueue.add(
        new AdditiveSlashImpactEffect(this.tX, this.tY, this.color)
    );
    this.isDone = true;
}

// CardPoofEffect.java
public void update() {
    for (int i = 0; i < 5; i++) {  // 5 particles
        AbstractDungeon.effectsQueue.add(new CardPoofParticle(x, y));
    }
    this.isDone = true;  // Immediately done, but spawned 5 children
}
```

### Pattern 2: Continuous Emission

**Emitter effects spawn over time**:

```java
// ClangClangClangEffect.java
for (int i = 0; i < 40; i++) {  // ❌ 40 iterations
    AbstractDungeon.effectsQueue.add(
        new UpgradeShineParticleEffect(
            this.x + MathUtils.random(-50, 50) * Settings.scale,
            this.y + MathUtils.random(-50, 50) * Settings.scale
        )
    );
}
```

### Pattern 3: Batch Spawning

**Single effect spawns many**:

```java
// LightningEffect.java - 15 sparks
for (int i = 0; i < 15; i++) {
    AbstractDungeon.topLevelEffectsQueue.add(
        new ImpactSparkEffect(x, y)
    );
}

// DaggerSprayEffect.java - 12 daggers
for (int i = 0; i < 12; i++) {
    AbstractDungeon.effectsQueue.add(
        new FlyingDaggerEffect(x, y, angle, flipX)
    );
}

// ClashEffect.java - Multiple cascading effects
AbstractDungeon.effectsQueue.add(
    new AnimatedSlashEffect(x, y, vX, vY, angle, size, c1, c2)
);
AbstractDungeon.effectsQueue.add(
    new AnimatedSlashEffect(x, y, vX2, vY2, angle2, size, c3, c4)
);
for (int i = 0; i < 40; i++) {
    AbstractDungeon.effectsQueue.add(
        new UpgradeShineParticleEffect(x, y)
    );
}
// Total: 42 effects from one ClashEffect!
```

### Pattern 4: Nested Cascades

**ClawEffect.java** - Multi-level spawning:
```java
// Parent creates 3 slash effects
AbstractDungeon.effectsQueue.add(
    new AnimatedSlashEffect(x1, y1, vX, vY, angle, color1, color2)
);
AbstractDungeon.effectsQueue.add(
    new AnimatedSlashEffect(x2, y2, vX, vY, angle, color1, color2)
);
AbstractDungeon.effectsQueue.add(
    new AnimatedSlashEffect(x3, y3, vX, vY, angle, color1, color2)
);

// Each AnimatedSlashEffect can spawn impact effects on completion
// If each spawns 5 particles: 3 + 3×5 = 18 total
```

---

## Cascade Analysis

### High-Risk Effects (Cascade Depth ≥ 2)

| Effect | Direct Children | Cascade Depth | Max Total |
|--------|-----------------|---------------|-----------|
| `ClashEffect` | 42 | 2-3 | 100+ |
| `BloodShotEffect` | 10-20 | 2 | 30-60 |
| `LightningEffect` | 15 | 1 | 15 |
| `DaggerSprayEffect` | 12 | 2 | 24-36 |
| `RainingGoldEffect` | 3-18/tick | 2 | Unbounded |
| `ClangClangClangEffect` | 40 | 1 | 40 |
| `CardPoofEffect` | 5 | 1 | 5 |

### Worst Case Scenario

```
Combo: Whirlwind + 5 enemies + Strength buffs

Per enemy:
- 1 × ClawEffect (3 slashes)
  - Each slash → 5 impact particles
  - Subtotal: 3 + 15 = 18

- 1 × DamageNumberEffect
- 1 × BlockedNumberEffect (if blocked)
- Enemy death → BloodShotEffect (count=10)
  - 10 particles
  - Each → 1 impact
  - Subtotal: 1 + 10 + 10 = 21

Per Enemy Total: 18 + 2 + 21 = 41 effects
5 Enemies: 41 × 5 = 205 effects

Plus card play VFX: ~10-20 more
Grand Total: ~220-225 effects from one card
```

---

## Mod Solution: Cascade Detection & Prevention

### 1. Cascade Depth Tracking

```java
public interface CascadeAware {
    int getCascadeDepth();
    void setCascadeDepth(int depth);
}

// Patch AbstractGameEffect
@SpirePatch(clz = AbstractGameEffect.class, method = SpirePatch.CLASS)
public class CascadeDepthField {
    public static SpireField<Integer> depth = new SpireField<>(() -> 0);
}
```

### 2. Effect Creation Interceptor

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class CascadeTracker {
    private static final int MAX_CASCADE_DEPTH = 3;
    private static final int MAX_BATCH_SIZE = 20;

    // Track current parent effect being updated
    public static AbstractGameEffect currentParent = null;

    public static boolean canSpawnChild(AbstractGameEffect child) {
        if (currentParent == null) {
            CascadeDepthField.depth.set(child, 0);
            return true;
        }

        int parentDepth = CascadeDepthField.depth.get(currentParent);
        int childDepth = parentDepth + 1;

        if (childDepth > MAX_CASCADE_DEPTH) {
            // Prevent deep cascades
            return false;
        }

        CascadeDepthField.depth.set(child, childDepth);
        return true;
    }
}

// Patch effect update to track parent
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class ParentTrackingPatch {
    @SpireInsertPatch(locator = EffectUpdateLocator.class)
    public static void setCurrentParent(AbstractGameEffect e) {
        CascadeTracker.currentParent = e;
    }

    @SpireInsertPatch(locator = AfterEffectUpdateLocator.class)
    public static void clearCurrentParent() {
        CascadeTracker.currentParent = null;
    }
}
```

### 3. Batch Spawn Detection

```java
@SpirePatch(clz = ArrayList.class, method = "add")
public class BatchSpawnPatch {
    private static int recentAddCount = 0;
    private static long lastAddTime = 0;
    private static final long BATCH_WINDOW_MS = 16;  // One frame

    @SpirePrefixPatch
    public static SpireReturn<Boolean> detectBatchSpawn(
        ArrayList<?> __instance,
        Object effect
    ) {
        if (__instance != AbstractDungeon.effectsQueue) {
            return SpireReturn.Continue();
        }

        long now = System.currentTimeMillis();
        if (now - lastAddTime > BATCH_WINDOW_MS) {
            recentAddCount = 0;
        }

        recentAddCount++;
        lastAddTime = now;

        if (recentAddCount > CascadeTracker.MAX_BATCH_SIZE) {
            // Too many effects in one frame
            logger.warn("Batch spawn limit exceeded: " + recentAddCount);
            return SpireReturn.Return(false);  // Drop effect
        }

        return SpireReturn.Continue();
    }
}
```

### 4. Smart Effect Substitution

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class CascadeOptimizer {
    // Replace high-cascade effects with optimized versions
    public static AbstractGameEffect optimize(AbstractGameEffect original) {
        // Replace particle emitters with single compound effect
        if (original instanceof BloodShotEffect) {
            BloodShotEffect bs = (BloodShotEffect) original;
            if (bs.count > 5) {
                // Use optimized batch renderer instead of individual particles
                return new OptimizedBloodEffect(bs.sX, bs.sY, bs.tX, bs.tY);
            }
        }

        // Merge impact sparks into single particle system
        if (original instanceof LightningEffect) {
            return new OptimizedLightningEffect(
                ((LightningEffect) original).x,
                ((LightningEffect) original).y
            );
        }

        return original;
    }
}

// Optimized effect uses instanced rendering
public class OptimizedBloodEffect extends AbstractGameEffect {
    private ParticleEmitter emitter;  // LibGDX particle system

    public OptimizedBloodEffect(float sX, float sY, float tX, float tY) {
        // Use single particle emitter instead of 10-20 objects
        emitter = new ParticleEmitter();
        emitter.setPosition(sX, sY);
        emitter.getEmission().setHigh(10);  // 10 particles/sec
        emitter.start();
    }

    @Override
    public void update() {
        emitter.update(Gdx.graphics.getDeltaTime());
        if (emitter.isComplete()) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        emitter.draw(sb);  // Batch render all particles
    }
}
```

### 5. Cascade Budget System

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class CascadeBudget {
    private static final int BUDGET_PER_FRAME = 30;
    private static int remainingBudget = BUDGET_PER_FRAME;
    private static long lastResetTime = 0;

    public static boolean consumeBudget(int cost) {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > 16) {  // New frame
            remainingBudget = BUDGET_PER_FRAME;
            lastResetTime = now;
        }

        if (remainingBudget >= cost) {
            remainingBudget -= cost;
            return true;
        }

        return false;  // Budget exhausted
    }

    public static int getCost(AbstractGameEffect e) {
        // High-cascade effects cost more
        if (e instanceof BloodShotEffect) return 5;
        if (e instanceof LightningEffect) return 3;
        if (e instanceof DamageNumberEffect) return 1;
        return 2;  // Default
    }
}
```

---

## Configuration

```java
public class CascadeConfig {
    public static int maxCascadeDepth = 3;
    public static int maxBatchSize = 20;
    public static int budgetPerFrame = 30;
    public static boolean enableOptimization = true;
    public static boolean logCascades = false;

    public static void setPerformanceMode(String mode) {
        switch (mode) {
            case "PERFORMANCE":
                maxCascadeDepth = 2;
                maxBatchSize = 15;
                budgetPerFrame = 20;
                enableOptimization = true;
                break;
            case "BALANCED":
                maxCascadeDepth = 3;
                maxBatchSize = 20;
                budgetPerFrame = 30;
                enableOptimization = true;
                break;
            case "QUALITY":
                maxCascadeDepth = 4;
                maxBatchSize = 30;
                budgetPerFrame = 50;
                enableOptimization = false;
                break;
        }
    }
}
```

---

## Verification

### Cascade Depth Logger
```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class CascadeDebugPatch {
    private static final Map<Integer, Integer> depthHistogram = new HashMap<>();

    @SpirePostfixPatch
    public static void logCascadeStats() {
        if (!CascadeConfig.logCascades) return;

        depthHistogram.clear();
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            int depth = CascadeDepthField.depth.get(e);
            depthHistogram.merge(depth, 1, Integer::sum);
        }

        if (!depthHistogram.isEmpty()) {
            logger.info("Cascade depth distribution: " + depthHistogram);
        }
    }
}
```

### Expected Results
- Max cascade depth: 2-3 levels
- Batch spawns: <20 per frame
- Effect count stable during intense combat

---

## Related Issues

- **08_EffectPooling.md**: Pooling helps but doesn't prevent cascades
- **09_ParticleLimit.md**: Cascades can bypass simple count limits

---

## References

- Decompiled: `com/megacrit/cardcrawl/vfx/combat/BloodShotEffect.java`
- Decompiled: `com/megacrit/cardcrawl/vfx/combat/ClashEffect.java`
- Decompiled: `com/megacrit/cardcrawl/vfx/combat/LightningEffect.java`
- Pattern: Cascading creation detected in 40+ effect classes
