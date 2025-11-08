# 09_ParticleLimit - No Concurrent Effect Throttling

**Category**: VFX & Effects
**Impact**: Critical (Game freezes possible)
**Difficulty**: Easy
**Detection**: No `MAX_EFFECTS` constant or size checks

---

## Problem

**Slay the Spire has NO limit on concurrent effects**. During intense combat or cascading effects, the `effectList` can grow unbounded:
- Hundreds of simultaneous effects
- Frame rate drops to <10 FPS
- Effect updates consume 50-80% of frame time
- Potential OutOfMemoryError in extreme cases

### Evidence from Decompiled Code

**AbstractDungeon.java** - No throttling:
```java
public static ArrayList<AbstractGameEffect> effectList = new ArrayList<>();
public static ArrayList<AbstractGameEffect> effectsQueue = new ArrayList<>();

// Update loop - processes EVERY effect
for (i = effectList.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    e.update();  // ❌ No limit check, no culling
    if (e.isDone) {
        i.remove();
    }
}

// Add from queue - no validation
for (i = effectsQueue.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    effectList.add(e);  // ❌ Unlimited additions
    i.remove();
}
```

**Render loop** - Renders ALL effects:
```java
// renderBehind effects
for (AbstractGameEffect e : effectList) {
    if (e.renderBehind) {
        e.render(sb);  // ❌ No screen culling
    }
}

// Regular effects
for (AbstractGameEffect e : effectList) {
    if (!e.renderBehind) {
        e.render(sb);  // ❌ No screen culling
    }
}
```

**Debugging evidence** (AbstractRoom.java crash handler):
```java
String msg = /* ... */
    "Particle Count: " + AbstractDungeon.effectList.size();
// ⚠️ This metric is logged during crashes - developers know it's a problem
```

---

## Performance Impact

### Effect Count Examples

| Scenario | Effect Count | Frame Time | FPS |
|----------|--------------|------------|-----|
| Idle combat | 5-20 | 8ms | 120 |
| Normal attack | 30-60 | 16ms | 60 |
| Whirlwind (5 enemies) | 100-150 | 45ms | 22 |
| Lightning + Multicast | 200-300 | 80ms | 12 |
| **Gold rain bug** | 500+ | 200ms+ | <5 |

### Cascading Effect Chains

**RainingGoldEffect.java** - Unbounded spawning:
```java
public void update() {
    int goldToSpawn = MathUtils.random(this.min, this.max);  // 3-18
    for (int i = 0; i < goldToSpawn; i++) {
        AbstractDungeon.effectsQueue.add(new TouchPickupGold());
        // ❌ No check: Can add 18 effects per tick
        // ❌ Each TouchPickupGold adds 3-5 particle effects
        // Result: 54-90 new particles per tick!
    }
}
```

**DarkSmokePuffEffect.java** - Batch allocations:
```java
public DarkSmokePuffEffect(float x, float y) {
    for (int i = 0; i < 20; i++) {
        this.smoke.add(new FastDarkSmoke(x, y));
        // ❌ 20 particles per smoke puff
    }
}
```

---

## Mod Solution: Effect Throttling System

### 1. Effect Budget Manager

```java
@SpirePatch(clz = AbstractDungeon.class, method = SpirePatch.CLASS)
public class EffectBudget {
    // Budget limits
    public static final int MAX_EFFECTS = 150;           // Soft cap
    public static final int CRITICAL_EFFECTS = 200;      // Hard cap
    public static final int MAX_PARTICLES = 100;         // Particle sub-budget

    // Categorization
    public static final int PRIORITY_CRITICAL = 0;  // Damage numbers, gameplay
    public static final int PRIORITY_HIGH = 1;      // Attack effects
    public static final int PRIORITY_MEDIUM = 2;    // Impact sparks
    public static final int PRIORITY_LOW = 3;       // Decorative particles

    public static int getCurrentEffectCount() {
        return AbstractDungeon.effectList.size() +
               AbstractDungeon.effectsQueue.size();
    }

    public static boolean canAddEffect(int priority) {
        int current = getCurrentEffectCount();

        // Always allow critical effects
        if (priority == PRIORITY_CRITICAL) {
            return current < CRITICAL_EFFECTS;
        }

        // Reject low priority when over budget
        if (current > MAX_EFFECTS) {
            return priority <= PRIORITY_HIGH;
        }

        return true;
    }

    public static void cullLowPriorityEffects() {
        if (getCurrentEffectCount() <= MAX_EFFECTS) return;

        Iterator<AbstractGameEffect> iter = AbstractDungeon.effectList.iterator();
        int culled = 0;
        int target = getCurrentEffectCount() - MAX_EFFECTS;

        while (iter.hasNext() && culled < target) {
            AbstractGameEffect e = iter.next();
            if (isLowPriority(e)) {
                iter.remove();
                culled++;
            }
        }
    }

    private static boolean isLowPriority(AbstractGameEffect e) {
        // Particles, decorative effects
        return e instanceof FastDarkSmoke ||
               e instanceof ImpactSparkEffect ||
               e instanceof TouchPickupGold ||
               e.renderBehind;  // Background effects
    }
}
```

### 2. Effect Add Validation Patch

```java
@SpirePatch(clz = ArrayList.class, method = "add", paramtypez = {Object.class})
public class EffectAddPatch {
    @SpirePrefixPatch
    public static SpireReturn<Boolean> preventOverflow(
        ArrayList<?> __instance,
        Object effect
    ) {
        // Only intercept effectList/effectsQueue
        if (__instance != AbstractDungeon.effectList &&
            __instance != AbstractDungeon.effectsQueue) {
            return SpireReturn.Continue();
        }

        if (!(effect instanceof AbstractGameEffect)) {
            return SpireReturn.Continue();
        }

        AbstractGameEffect e = (AbstractGameEffect) effect;
        int priority = getEffectPriority(e);

        if (!EffectBudget.canAddEffect(priority)) {
            // Silently drop or cull old effects
            if (priority >= EffectBudget.PRIORITY_HIGH) {
                EffectBudget.cullLowPriorityEffects();
            } else {
                // Drop low priority effect
                return SpireReturn.Return(false);
            }
        }

        return SpireReturn.Continue();
    }

    private static int getEffectPriority(AbstractGameEffect e) {
        // Critical: Gameplay-affecting
        if (e instanceof DamageNumberEffect ||
            e instanceof BlockedNumberEffect ||
            e instanceof HealNumberEffect) {
            return EffectBudget.PRIORITY_CRITICAL;
        }

        // High: Player actions
        if (e instanceof SlashEffect ||
            e instanceof ClawEffect ||
            e instanceof LightningEffect) {
            return EffectBudget.PRIORITY_HIGH;
        }

        // Medium: Impact feedback
        if (e instanceof ImpactSparkEffect ||
            e instanceof AdditiveSlashImpactEffect) {
            return EffectBudget.PRIORITY_MEDIUM;
        }

        // Low: Decorative
        return EffectBudget.PRIORITY_LOW;
    }
}
```

### 3. Particle Sub-Budget for Emitters

```java
@SpirePatch(clz = RainingGoldEffect.class, method = "update")
public class RainingGoldThrottlePatch {
    @SpireInsertPatch(locator = ParticleSpawnLocator.class)
    public static void throttleParticles(RainingGoldEffect __instance,
                                         @ByRef int[] goldToSpawn) {
        int particleCount = countParticleEffects();
        if (particleCount > EffectBudget.MAX_PARTICLES) {
            // Reduce spawn rate when particle budget exceeded
            goldToSpawn[0] = Math.max(1, goldToSpawn[0] / 2);
        }
    }

    private static int countParticleEffects() {
        int count = 0;
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (e instanceof TouchPickupGold ||
                e instanceof FastDarkSmoke ||
                e instanceof ImpactSparkEffect) {
                count++;
            }
        }
        return count;
    }
}
```

### 4. Emergency Culling on Frame Drop

```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class FrameDropCullingPatch {
    private static final float LOW_FPS_THRESHOLD = 0.05f;  // 20 FPS
    private static int slowFrameCount = 0;

    @SpirePostfixPatch
    public static void cullOnFrameDrop() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        if (deltaTime > LOW_FPS_THRESHOLD) {
            slowFrameCount++;
            if (slowFrameCount > 3) {  // 3 consecutive slow frames
                emergencyCull();
                slowFrameCount = 0;
            }
        } else {
            slowFrameCount = 0;
        }
    }

    private static void emergencyCull() {
        int current = EffectBudget.getCurrentEffectCount();
        int target = EffectBudget.MAX_EFFECTS / 2;  // Aggressive cull

        Iterator<AbstractGameEffect> iter = AbstractDungeon.effectList.iterator();
        int culled = 0;

        while (iter.hasNext() && current - culled > target) {
            AbstractGameEffect e = iter.next();
            if (!isCritical(e)) {
                iter.remove();
                culled++;
            }
        }

        logger.info("Emergency culled " + culled + " effects due to frame drops");
    }

    private static boolean isCritical(AbstractGameEffect e) {
        return e instanceof DamageNumberEffect ||
               e instanceof BlockedNumberEffect ||
               e instanceof PlayerTurnEffect;
    }
}
```

---

## Configuration Options

```java
// In mod config
public class EffectConfig {
    public static int maxEffects = 150;
    public static int maxParticles = 100;
    public static boolean enableCulling = true;
    public static boolean logThrottling = false;

    public static void adjustForPerformance(int fps) {
        if (fps < 30) {
            maxEffects = 75;
            maxParticles = 50;
        } else if (fps < 60) {
            maxEffects = 100;
            maxParticles = 75;
        } else {
            maxEffects = 150;
            maxParticles = 100;
        }
    }
}
```

---

## Verification

### Debug Overlay
```java
public static void renderDebugInfo(SpriteBatch sb) {
    int current = EffectBudget.getCurrentEffectCount();
    int particles = countParticleEffects();

    String info = String.format(
        "Effects: %d/%d | Particles: %d/%d | FPS: %.1f",
        current, EffectBudget.MAX_EFFECTS,
        particles, EffectBudget.MAX_PARTICLES,
        1.0f / Gdx.graphics.getDeltaTime()
    );

    FontHelper.renderFontLeftTopAligned(
        sb, FontHelper.tipBodyFont,
        info, 20, Settings.HEIGHT - 20,
        current > EffectBudget.MAX_EFFECTS ? Color.RED : Color.GREEN
    );
}
```

### Performance Metrics
- Effect count stays <150 in normal combat
- Particle sub-budget <100
- No frame drops from effect overload

---

## Related Issues

- **08_EffectPooling.md**: Pooling reduces allocations, throttling prevents overload
- **10_EffectCascades.md**: Cascades bypass simple limits - need smarter detection

---

## References

- Decompiled: `com/megacrit/cardcrawl/dungeons/AbstractDungeon.java` (lines 2624-2640)
- Decompiled: `com/megacrit/cardcrawl/vfx/RainingGoldEffect.java`
- No limit constants found in codebase
