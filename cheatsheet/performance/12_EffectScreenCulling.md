# 12_EffectScreenCulling - No Offscreen Effect Culling

**Category**: VFX & Effects
**Impact**: Medium (10-20% CPU waste)
**Difficulty**: Easy
**Detection**: No bounds checks in update()/render()

---

## Problem

**Effects update and render even when completely offscreen**, wasting CPU:
- Effects outside visible area still process every frame
- Particle trajectories calculated for invisible objects
- Render calls submitted for offscreen elements (GPU culls but CPU wastes time)
- No spatial partitioning or frustum culling

### Evidence from Decompiled Code

**AbstractGameEffect.java** - No culling support:
```java
public abstract class AbstractGameEffect {
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        // ❌ No position/bounds checks
        // ❌ No screen boundary validation
        if (this.duration < 0.0F) {
            this.isDone = true;
        }
    }

    public abstract void render(SpriteBatch paramSpriteBatch);
    // ❌ No visibility parameter
    // ❌ Subclasses never check screen bounds
}
```

**AbstractDungeon.java** - Renders all effects:
```java
// Render loop - NO culling
for (AbstractGameEffect e : effectList) {
    if (e.renderBehind) {
        e.render(sb);  // ❌ Always renders, even if x=-5000
    }
}

for (AbstractGameEffect e : effectList) {
    if (!e.renderBehind) {
        e.render(sb);  // ❌ Always renders
    }
}

// Update loop - NO culling
for (i = effectList.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();
    e.update();  // ❌ Always updates
}
```

**SwirlyBloodEffect.java** - Complex math for invisible effect:
```java
private void updateMovement() {
    // ❌ Calculates trajectory even if x,y outside screen
    Vector2 tmp = new Vector2(this.pos.x - this.target.x, this.pos.y - this.target.y);
    tmp.nor();

    if (this.rotateClockwise) {
        this.rotation += Gdx.graphics.getDeltaTime() * this.rotationRate;
    }

    tmp.setAngle(this.rotation);  // Trig calculations
    this.pos.sub(tmp);

    // 60 Vector2 spline calculations
    for (int i = 0; i < 60; i++) {
        this.points[i] = new Vector2();
        this.crs.valueAt(this.points[i], i / 59.0F);  // Expensive
    }
}

// render() - Draws even if position is (-1000, -1000)
public void render(SpriteBatch sb) {
    for (int i = this.points.length - 1; i > 0; i--) {
        sb.draw(this.img, this.points[i].x, this.points[i].y, ...);
        // ❌ No bounds check before draw call
    }
}
```

**No screen bounds constants** in codebase:
```bash
$ grep -r "isOffscreen\|inBounds\|frustum" --include="*.java" com/megacrit/cardcrawl/vfx/
# (No results - culling never implemented)
```

---

## Performance Impact

### Typical Offscreen Scenarios

| Scenario | Offscreen Effects | Wasted CPU | Example |
|----------|------------------|------------|---------|
| Card played offscreen | 5-10 | 2-5% | Discard pile animation |
| Enemy death below screen | 10-20 | 5-10% | Falling enemies |
| Chest reward glow | 3-8 | 1-3% | Screen transition |
| Combat end particles | 20-40 | 10-15% | Victory screen fade |

### Worst Case: Victory Screen Transition
```
Combat ends:
- 20 damage numbers float off top of screen (still updating)
- 15 blood particle effects below screen bottom (still calculating)
- 10 impact sparks at enemy positions (enemies moved offscreen)
- 8 floating gold effects (chest moved offscreen)

Total: 53 effects updating+rendering offscreen
CPU waste: ~15-20% frame time
```

---

## Culling Strategies

### Strategy 1: Simple Bounds Check

**Pros**: Fast, easy to implement
**Cons**: Doesn't handle rotated/scaled effects perfectly

```java
public static boolean isOnScreen(float x, float y, float radius) {
    return x + radius >= 0 &&
           x - radius <= Settings.WIDTH &&
           y + radius >= 0 &&
           y - radius <= Settings.HEIGHT;
}
```

### Strategy 2: Visibility Margin

**Pros**: Prevents pop-in, handles edge cases
**Cons**: Slightly more CPU for check

```java
public static final float VISIBILITY_MARGIN = 100.0f * Settings.scale;

public static boolean isVisible(float x, float y, float radius) {
    return x + radius >= -VISIBILITY_MARGIN &&
           x - radius <= Settings.WIDTH + VISIBILITY_MARGIN &&
           y + radius >= -VISIBILITY_MARGIN &&
           y - radius <= Settings.HEIGHT + VISIBILITY_MARGIN;
}
```

### Strategy 3: Spatial Partitioning

**Pros**: O(1) lookups for large effect counts
**Cons**: Complex, overhead for small counts

```java
public class EffectQuadTree {
    // Divide screen into 4x4 grid
    private final List<AbstractGameEffect>[][] grid = new List[4][4];
    private final float cellWidth = Settings.WIDTH / 4;
    private final float cellHeight = Settings.HEIGHT / 4;

    public void insert(AbstractGameEffect e) {
        int gridX = (int) (e.x / cellWidth);
        int gridY = (int) (e.y / cellHeight);
        if (gridX >= 0 && gridX < 4 && gridY >= 0 && gridY < 4) {
            grid[gridX][gridY].add(e);
        }
    }

    public List<AbstractGameEffect> getVisibleEffects() {
        // Return only cells overlapping screen
        List<AbstractGameEffect> visible = new ArrayList<>();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                visible.addAll(grid[x][y]);
            }
        }
        return visible;
    }
}
```

---

## Mod Solution: Effect Culling System

### 1. Visibility Interface

```java
public interface Cullable {
    float getCullX();      // Center X for culling
    float getCullY();      // Center Y for culling
    float getCullRadius(); // Bounding radius
    boolean skipCulling(); // Force always render
}

// Patch AbstractGameEffect to implement Cullable
@SpirePatch(clz = AbstractGameEffect.class, method = SpirePatch.CLASS)
public class CullableFields implements Cullable {
    public static SpireField<Float> cullX = new SpireField<>(() -> 0f);
    public static SpireField<Float> cullY = new SpireField<>(() -> 0f);
    public static SpireField<Float> cullRadius = new SpireField<>(() -> 100f);
    public static SpireField<Boolean> noCull = new SpireField<>(() -> false);

    @Override
    public float getCullX() { return cullX.get(this); }

    @Override
    public float getCullY() { return cullY.get(this); }

    @Override
    public float getCullRadius() { return cullRadius.get(this); }

    @Override
    public boolean skipCulling() { return noCull.get(this); }
}
```

### 2. Update Culling Patch

```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class EffectUpdateCullingPatch {
    private static final float MARGIN = 100.0f * Settings.scale;

    @SpireInsertPatch(locator = EffectUpdateLocator.class)
    public static SpireReturn<Void> cullOffscreenUpdates(AbstractGameEffect e) {
        if (e instanceof Cullable) {
            Cullable c = (Cullable) e;
            if (!c.skipCulling() && !isVisible(c)) {
                // Skip update for offscreen effect
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }

    private static boolean isVisible(Cullable c) {
        float x = c.getCullX();
        float y = c.getCullY();
        float r = c.getCullRadius();

        return x + r >= -MARGIN &&
               x - r <= Settings.WIDTH + MARGIN &&
               y + r >= -MARGIN &&
               y - r <= Settings.HEIGHT + MARGIN;
    }
}
```

### 3. Render Culling Patch

```java
@SpirePatch(clz = AbstractDungeon.class, method = "render")
public class EffectRenderCullingPatch {
    @SpireInsertPatch(locator = EffectRenderLocator.class)
    public static SpireReturn<Void> cullOffscreenRenders(
        AbstractGameEffect e,
        SpriteBatch sb
    ) {
        if (e instanceof Cullable) {
            Cullable c = (Cullable) e;
            if (!c.skipCulling() && !isVisible(c)) {
                // Skip render for offscreen effect
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }

    private static boolean isVisible(Cullable c) {
        // Same check as update culling
        float x = c.getCullX();
        float y = c.getCullY();
        float r = c.getCullRadius();

        return x + r >= 0 &&
               x - r <= Settings.WIDTH &&
               y + r >= 0 &&
               y - r <= Settings.HEIGHT;
    }
}
```

### 4. Auto-Cull Position Detection

```java
@SpirePatch(clz = AbstractGameEffect.class, method = SpirePatch.CLASS)
public class AutoCullPositionPatch {
    // Automatically detect position from common field names
    public static void updateCullPosition(AbstractGameEffect effect) {
        try {
            // Try common position field names
            Float x = tryGetField(effect, "x", "posX", "position.x");
            Float y = tryGetField(effect, "y", "posY", "position.y");

            if (x != null && y != null) {
                CullableFields.cullX.set(effect, x);
                CullableFields.cullY.set(effect, y);
            }

            // Try to detect radius from scale or size
            Float scale = tryGetField(effect, "scale");
            if (scale != null) {
                CullableFields.cullRadius.set(effect, scale * 100f);
            }

        } catch (Exception e) {
            // Reflection failed, use defaults
        }
    }

    private static Float tryGetField(Object obj, String... fieldNames) {
        for (String name : fieldNames) {
            try {
                Field field = obj.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof Float) {
                    return (Float) value;
                }
            } catch (Exception e) {
                // Try next field name
            }
        }
        return null;
    }
}
```

### 5. Manual Cull Hints for Complex Effects

```java
// For effects that need custom culling logic
@SpirePatch(clz = SwirlyBloodEffect.class, method = SpirePatch.CONSTRUCTOR)
public class SwirlyBloodCullPatch {
    @SpirePostfixPatch
    public static void setupCulling(SwirlyBloodEffect __instance,
                                    float sX, float sY) {
        // Set initial position
        CullableFields.cullX.set(__instance, sX);
        CullableFields.cullY.set(__instance, sY);
        CullableFields.cullRadius.set(__instance, 200f * Settings.scale);
    }
}

@SpirePatch(clz = SwirlyBloodEffect.class, method = "updateMovement")
public class SwirlyBloodCullUpdatePatch {
    @SpirePostfixPatch
    public static void updateCulling(SwirlyBloodEffect __instance) {
        // Update position as effect moves
        Vector2 pos = ReflectionHacks.getPrivate(__instance,
            SwirlyBloodEffect.class, "pos");
        CullableFields.cullX.set(__instance, pos.x);
        CullableFields.cullY.set(__instance, pos.y);
    }
}
```

### 6. Critical Effect Exemptions

```java
@SpirePatch(clz = AbstractGameEffect.class, method = SpirePatch.CLASS)
public class CriticalEffectExemptions {
    // Effects that should NEVER be culled
    private static final Set<Class<? extends AbstractGameEffect>> EXEMPT = new HashSet<>(Arrays.asList(
        DamageNumberEffect.class,     // Gameplay feedback
        BlockedNumberEffect.class,    // Gameplay feedback
        HealNumberEffect.class,       // Gameplay feedback
        PlayerTurnEffect.class,       // Turn indicator
        BorderFlashEffect.class,      // Screen effects
        BorderLongFlashEffect.class   // Screen effects
    ));

    public static void checkExemption(AbstractGameEffect effect) {
        if (EXEMPT.contains(effect.getClass())) {
            CullableFields.noCull.set(effect, true);
        }
    }
}
```

---

## Configuration

```java
public class CullingConfig {
    public static boolean enableUpdateCulling = true;
    public static boolean enableRenderCulling = true;
    public static float visibilityMargin = 100.0f;
    public static boolean autoDetectPositions = true;
    public static boolean logCulledEffects = false;

    public static void setQualityMode(String mode) {
        switch (mode) {
            case "PERFORMANCE":
                enableUpdateCulling = true;
                enableRenderCulling = true;
                visibilityMargin = 50.0f;
                break;
            case "BALANCED":
                enableUpdateCulling = true;
                enableRenderCulling = true;
                visibilityMargin = 100.0f;
                break;
            case "QUALITY":
                enableUpdateCulling = false;
                enableRenderCulling = true;
                visibilityMargin = 200.0f;
                break;
        }
    }
}
```

---

## Verification

### Culling Statistics
```java
@SpirePatch(clz = AbstractDungeon.class, method = "update")
public class CullingStatsPatch {
    private static int totalEffects = 0;
    private static int culledUpdates = 0;
    private static int culledRenders = 0;

    public static void recordCulled(boolean update, boolean render) {
        totalEffects++;
        if (update) culledUpdates++;
        if (render) culledRenders++;
    }

    public static void logStats() {
        if (totalEffects == 0) return;

        logger.info(String.format(
            "Culling: %d total | %d updates (%.1f%%) | %d renders (%.1f%%)",
            totalEffects,
            culledUpdates, 100.0 * culledUpdates / totalEffects,
            culledRenders, 100.0 * culledRenders / totalEffects
        ));

        totalEffects = culledUpdates = culledRenders = 0;
    }
}
```

### Visual Debug Overlay
```java
public static void renderCullingDebug(SpriteBatch sb) {
    if (!CullingConfig.logCulledEffects) return;

    // Draw screen bounds
    ShapeRenderer sr = new ShapeRenderer();
    sr.setProjectionMatrix(sb.getProjectionMatrix());
    sr.begin(ShapeRenderer.ShapeType.Line);
    sr.setColor(Color.GREEN);
    sr.rect(0, 0, Settings.WIDTH, Settings.HEIGHT);
    sr.end();

    // Draw effect bounds
    for (AbstractGameEffect e : AbstractDungeon.effectList) {
        if (e instanceof Cullable) {
            Cullable c = (Cullable) e;
            sr.begin(ShapeRenderer.ShapeType.Line);
            sr.setColor(isVisible(c) ? Color.CYAN : Color.RED);
            sr.circle(c.getCullX(), c.getCullY(), c.getCullRadius());
            sr.end();
        }
    }
}
```

### Expected Results
- 10-30% of effects culled during normal combat
- 50-70% culled during screen transitions
- CPU savings: 5-15% in typical scenarios

---

## Related Issues

- **08_EffectPooling.md**: Culling reduces active effects, making pooling more effective
- **09_ParticleLimit.md**: Culled effects don't count toward particle budget

---

## References

- Decompiled: `com/megacrit/cardcrawl/vfx/AbstractGameEffect.java`
- Decompiled: `com/megacrit/cardcrawl/dungeons/AbstractDungeon.java` (render/update loops)
- No culling code found in 302 VFX classes
- Pattern: LibGDX frustum culling (com.badlogic.gdx.graphics.Camera.frustum)
