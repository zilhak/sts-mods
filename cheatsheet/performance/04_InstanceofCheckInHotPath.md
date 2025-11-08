# instanceof 체크를 매 프레임 실행

## 🔍 문제 발견 위치
- 파일: AbstractDungeon.java
- 메서드: update(), render()
- 라인: 2628 (update), 2690 (render)
- 호출 빈도: 매 프레임 / 초당 60회

## 📋 문제 설명
effectList의 모든 이펙트를 순회하면서 `instanceof` 타입 체크를 수행합니다. 또한 render() 메서드에서도 현재 방의 타입을 `instanceof`로 체크합니다. instanceof는 빠른 연산이지만, hot path(매 프레임 실행)에서는 누적 비용이 상당합니다.

## 🔬 원인 분석

### update() 메서드의 instanceof
```java
// AbstractDungeon.java:2624-2635
for (i = effectList.iterator(); i.hasNext(); ) {
    AbstractGameEffect e = i.next();

    e.update();
    if (e instanceof com.megacrit.cardcrawl.vfx.PlayerTurnEffect) {  // 문제!
        turnPhaseEffectActive = true;
    }

    if (e.isDone) {
        i.remove();
    }
}
```

### render() 메서드의 instanceof
```java
// AbstractDungeon.java:2688-2692
AbstractRoom room = getCurrRoom();

if (room instanceof EventRoom || room instanceof NeowRoom || room instanceof VictoryRoom) {  // 문제!
    room.renderEventTexts(sb);
}
```

### 실행 빈도 및 영향
- **프레임당 instanceof 호출**: effectList.size() × 1 + 3
- **평균 effectList 크기**: 10-50개 (전투 중 50-200개)
- **프레임당 총 instanceof 연산**: 13-203회
- **CPU 비용**: instanceof는 빠르지만 (1-5 사이클), 누적 시 무시 못함
- **캐시 미스**: 클래스 메타데이터 접근 시 L1/L2 캐시 미스 가능

## ✅ 해결 방법

### 방법 1: boolean 플래그 추가 (권장)
```java
@SpirePatch(
    clz = AbstractGameEffect.class,
    method = SpirePatch.CLASS
)
public static class EffectTypeFlagPatch {
    public static SpireField<Boolean> isPlayerTurnEffect =
        new SpireField<>(() -> false);
}

@SpirePatch(
    clz = PlayerTurnEffect.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class SetPlayerTurnEffectFlagPatch {
    @SpirePostfixPatch
    public static void Postfix(PlayerTurnEffect __instance) {
        EffectTypeFlagPatch.isPlayerTurnEffect.set(__instance, true);
    }
}

@SpirePatch(
    clz = AbstractDungeon.class,
    method = "update"
)
public static class OptimizedInstanceofPatch {
    @SpireInsertPatch(
        locator = InstanceofCheckLocator.class
    )
    public static SpireReturn<Void> Insert(AbstractDungeon __instance) {
        Iterator<AbstractGameEffect> i = AbstractDungeon.effectList.iterator();
        AbstractDungeon.turnPhaseEffectActive = false;

        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            e.update();

            // instanceof 대신 boolean 플래그 체크
            if (EffectTypeFlagPatch.isPlayerTurnEffect.get(e)) {
                AbstractDungeon.turnPhaseEffectActive = true;
            }

            if (e.isDone) {
                i.remove();
            }
        }

        return SpireReturn.Return(null);
    }

    private static class InstanceofCheckLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.InstanceOfMatcher(
                PlayerTurnEffect.class
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 방법 2: AbstractRoom에 타입 플래그 추가
```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = SpirePatch.CLASS
)
public static class RoomTypeFlagPatch {
    public static SpireField<Boolean> hasEventTexts =
        new SpireField<>(() -> false);
}

@SpirePatch(
    clz = EventRoom.class,
    method = SpirePatch.CONSTRUCTOR
)
@SpirePatch(
    clz = NeowRoom.class,
    method = SpirePatch.CONSTRUCTOR
)
@SpirePatch(
    clz = VictoryRoom.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class SetEventTextsRoomFlagPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        RoomTypeFlagPatch.hasEventTexts.set(__instance, true);
    }
}

@SpirePatch(
    clz = AbstractDungeon.class,
    method = "render"
)
public static class OptimizedRoomInstanceofPatch {
    @SpireInsertPatch(
        locator = RoomInstanceofLocator.class
    )
    public static SpireReturn<Void> Insert(SpriteBatch sb) {
        AbstractRoom room = AbstractDungeon.getCurrRoom();

        // instanceof 대신 boolean 플래그
        if (RoomTypeFlagPatch.hasEventTexts.get(room)) {
            room.renderEventTexts(sb);
        }

        return SpireReturn.Continue();
    }

    private static class RoomInstanceofLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.InstanceOfMatcher(
                EventRoom.class
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 방법 3: 비트마스크 사용 (고급)
```java
@SpirePatch(
    clz = AbstractGameEffect.class,
    method = SpirePatch.CLASS
)
public static class EffectTypeBitmaskPatch {
    // 비트마스크 플래그
    public static final int TYPE_PLAYER_TURN_EFFECT = 1 << 0;
    public static final int TYPE_CARD_EFFECT = 1 << 1;
    public static final int TYPE_RENDER_BEHIND = 1 << 2;
    // ... 최대 32개 타입

    public static SpireField<Integer> typeFlags =
        new SpireField<>(() -> 0);
}

@SpirePatch(
    clz = PlayerTurnEffect.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class SetPlayerTurnEffectBitPatch {
    @SpirePostfixPatch
    public static void Postfix(PlayerTurnEffect __instance) {
        int flags = EffectTypeBitmaskPatch.typeFlags.get(__instance);
        flags |= EffectTypeBitmaskPatch.TYPE_PLAYER_TURN_EFFECT;
        EffectTypeBitmaskPatch.typeFlags.set(__instance, flags);
    }
}

// 사용
if ((EffectTypeBitmaskPatch.typeFlags.get(e) & EffectTypeBitmaskPatch.TYPE_PLAYER_TURN_EFFECT) != 0) {
    turnPhaseEffectActive = true;
}
```

## 📊 성능 개선 효과

### 방법 1: boolean 플래그
- **예상 FPS 향상**: 2-5% (전투 중 이펙트가 많을 때)
- **연산 비용**: instanceof (1-5 사이클) → boolean 필드 접근 (1 사이클)
- **메모리 사용**: 객체당 1바이트 (boolean)
- **캐시 효율**: 개선 (필드 접근은 객체와 함께 캐시됨)

### 방법 2: AbstractRoom 타입 플래그
- **예상 FPS 향상**: 0.5-1%
- **연산 비용**: 3× instanceof → 1× boolean 체크
- **메모리 사용**: 객체당 1바이트

### 방법 3: 비트마스크
- **예상 FPS 향상**: 3-7% (여러 타입 체크 시)
- **연산 비용**: instanceof → 비트 AND 연산 (1 사이클)
- **메모리 사용**: 객체당 4바이트 (int)
- **확장성**: 최대 32개 타입 플래그 저장 가능

## ⚠️ 주의사항

### 방법 1
- **장점**: 구현 간단, 가독성 좋음
- **단점**: 타입마다 별도 필드 필요
- **호환성**: 완벽함

### 방법 2
- **장점**: AbstractRoom 체크 최적화
- **단점**: 새로운 이벤트 방 추가 시 패치 필요
- **호환성**: 다른 모드가 새 방 타입 추가 시 문제 가능

### 방법 3
- **장점**: 메모리 효율, 다중 타입 체크 빠름
- **단점**: 구현 복잡도 높음
- **호환성**: 비트마스크 충돌 가능 (다른 모드와 조율 필요)

## 💡 성능 측정

### instanceof 비용 분석
```java
// 벤치마크 코드
long start = System.nanoTime();
for (int i = 0; i < 1000000; i++) {
    if (effect instanceof PlayerTurnEffect) {
        // do something
    }
}
long end = System.nanoTime();
System.out.println("instanceof: " + (end - start) / 1000000.0 + "ms");

// vs

long start = System.nanoTime();
for (int i = 0; i < 1000000; i++) {
    if (isPlayerTurnEffect) {
        // do something
    }
}
long end = System.nanoTime();
System.out.println("boolean: " + (end - start) / 1000000.0 + "ms");
```

### 예상 결과
- instanceof: 5-15ms (1백만 회)
- boolean: 1-3ms (1백만 회)
- **개선율**: 60-80%

## 🔗 관련 문제
- 01_EffectListDoubleIteration.md - 리스트 순회 최적화
- 05_VirtualMethodCall.md - 가상 메서드 호출 오버헤드
- 06_PolymorphicDispatch.md - 다형성 디스패치 비용
