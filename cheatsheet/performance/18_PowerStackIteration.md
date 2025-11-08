# 18. Power Stack Iteration Overhead

## 문제 설명

전투 중 파워 스택을 순회하는 작업이 매우 빈번하게 발생하지만, 대부분의 파워는 특정 이벤트에만 반응합니다. 모든 파워를 매번 확인하는 것은 비효율적입니다.

## 성능 영향

**비용**: 파워당 메서드 호출 + 조건 확인
**빈도**: 카드 사용, 데미지, 턴 시작/종료마다
**예상 절감**: 파워 10개 이상일 때 ~50% 감소

## 코드 위치

```
AbstractCreature.java:610-630 (applyStartOfTurnPowers)
AbstractMonster.java:219-221 (update - updateParticles)
AbstractMonster.java:1304-1331 (calculateDamage - 4회 순회)
```

## 현재 구현

```java
// AbstractCreature.java - 턴 시작 시 파워 업데이트
public void applyStartOfTurnPowers() {
    for (AbstractPower p : this.powers) {
        p.atStartOfTurn();  // 모든 파워 호출 (대부분 빈 메서드)
    }

    // ... 파워 제거 로직 ...

    for (AbstractPower p : this.powers) {
        p.duringTurn();     // 다시 모든 파워 호출
    }
}

// AbstractMonster.java - 매 프레임 파워 파티클 업데이트
public void update() {
    for (AbstractPower p : this.powers) {
        p.updateParticles();  // 대부분의 파워는 파티클 없음
    }
    // ...
}

// AbstractPower.java - 기본 구현 (대부분 빈 메서드)
public void atStartOfTurn() {}
public void duringTurn() {}
public void updateParticles() {}
public void atEndOfTurn(boolean isPlayer) {}
public void atEndOfRound() {}
public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {}
// ... 수십 개의 빈 메서드들 ...
```

## 문제 분석

1. **빈 메서드 호출**: 대부분의 파워는 특정 이벤트만 처리하지만 모든 메서드가 호출됨
2. **불필요한 순회**: 파워가 많을수록 빈 메서드 호출이 선형 증가
3. **가상 메서드 오버헤드**: 인터페이스/추상 메서드 호출로 인한 디스패치 비용
4. **캐싱 부재**: 어떤 파워가 어떤 이벤트를 처리하는지 미리 분류하지 않음

## 예시 시나리오

```
플레이어: [Strength, Dexterity, Vulnerable, Weak, Artifact, Flight, Blur, Buffer, Vigor, Intangible]
         = 10 파워

턴 시작 시:
- atStartOfTurn() → 10회 호출, 실제 동작 2-3개
- duringTurn() → 10회 호출, 실제 동작 1-2개

카드 1장 사용 시:
- onUseCard() → 10회 호출, 실제 동작 0-1개
- onPlayCard() → 10회 호출, 실제 동작 0-1개

데미지 계산 시:
- atDamageGive() → 10회 호출, 실제 동작 1-2개
- atDamageFinalGive() → 10회 호출, 실제 동작 0-1개
```

## 최적화 전략

### 방법 1: 이벤트 리스너 분류

```java
// AbstractCreature.java - 파워를 이벤트별로 분류
public class AbstractCreature {
    public ArrayList<AbstractPower> powers = new ArrayList<>();

    // 이벤트별 활성 파워 리스트
    private ArrayList<AbstractPower> startOfTurnPowers = new ArrayList<>();
    private ArrayList<AbstractPower> duringTurnPowers = new ArrayList<>();
    private ArrayList<AbstractPower> attackPowers = new ArrayList<>();
    private ArrayList<AbstractPower> damagedPowers = new ArrayList<>();
    private ArrayList<AbstractPower> particlePowers = new ArrayList<>();
    private ArrayList<AbstractPower> cardPlayPowers = new ArrayList<>();

    public void addPower(AbstractPower power) {
        this.powers.add(power);

        // 파워 기능에 따라 분류
        if (power.hasStartOfTurnEffect()) {
            startOfTurnPowers.add(power);
        }
        if (power.hasDuringTurnEffect()) {
            duringTurnPowers.add(power);
        }
        if (power.hasAttackEffect()) {
            attackPowers.add(power);
        }
        if (power.hasDamagedEffect()) {
            damagedPowers.add(power);
        }
        if (power.hasParticles()) {
            particlePowers.add(power);
        }
        if (power.hasCardPlayEffect()) {
            cardPlayPowers.add(power);
        }
    }

    public void removePower(AbstractPower power) {
        this.powers.remove(power);

        // 모든 분류 리스트에서 제거
        startOfTurnPowers.remove(power);
        duringTurnPowers.remove(power);
        attackPowers.remove(power);
        damagedPowers.remove(power);
        particlePowers.remove(power);
        cardPlayPowers.remove(power);
    }

    // 최적화된 턴 시작 처리
    public void applyStartOfTurnPowers() {
        // 필요한 파워만 순회
        for (AbstractPower p : startOfTurnPowers) {
            p.atStartOfTurn();
        }

        // 파워 제거 로직...

        for (AbstractPower p : duringTurnPowers) {
            p.duringTurn();
        }
    }
}

// AbstractPower.java - 파워 기능 식별 메서드
public abstract class AbstractPower {
    // 각 파워가 어떤 이벤트를 처리하는지 선언
    public boolean hasStartOfTurnEffect() { return false; }
    public boolean hasDuringTurnEffect() { return false; }
    public boolean hasAttackEffect() { return false; }
    public boolean hasDamagedEffect() { return false; }
    public boolean hasParticles() { return false; }
    public boolean hasCardPlayEffect() { return false; }

    // ... 기존 메서드들 ...
}

// 구체적 파워 구현 예시
public class StrengthPower extends AbstractPower {
    @Override
    public boolean hasAttackEffect() { return true; }  // 공격 데미지에만 영향

    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        return (type == DamageInfo.DamageType.NORMAL) ?
               damage + this.amount : damage;
    }
}

public class DrawCardPower extends AbstractPower {
    @Override
    public boolean hasStartOfTurnEffect() { return true; }  // 턴 시작에만 동작

    @Override
    public void atStartOfTurn() {
        flash();
        AbstractDungeon.actionManager.addToBottom(
            new DrawCardAction(AbstractDungeon.player, this.amount)
        );
    }
}
```

### 방법 2: 비트마스크 기반 필터링

```java
// AbstractPower.java - 이벤트 플래그
public abstract class AbstractPower {
    // 이벤트 타입 비트마스크
    public static final int EVENT_START_OF_TURN = 1 << 0;    // 0x01
    public static final int EVENT_DURING_TURN = 1 << 1;      // 0x02
    public static final int EVENT_END_OF_TURN = 1 << 2;      // 0x04
    public static final int EVENT_ATTACK = 1 << 3;           // 0x08
    public static final int EVENT_DAMAGED = 1 << 4;          // 0x10
    public static final int EVENT_CARD_PLAY = 1 << 5;        // 0x20
    public static final int EVENT_PARTICLES = 1 << 6;        // 0x40
    public static final int EVENT_HEAL = 1 << 7;             // 0x80

    protected int eventMask = 0;  // 이 파워가 처리하는 이벤트들

    public boolean hasEvent(int eventType) {
        return (eventMask & eventType) != 0;
    }
}

// 구체적 파워 구현
public class StrengthPower extends AbstractPower {
    public StrengthPower(AbstractCreature owner, int amount) {
        // ...
        this.eventMask = EVENT_ATTACK;  // 공격 이벤트만 처리
    }

    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        return damage + this.amount;
    }
}

public class DrawCardPower extends AbstractPower {
    public DrawCardPower(AbstractCreature owner, int amount) {
        // ...
        this.eventMask = EVENT_START_OF_TURN | EVENT_DURING_TURN;
    }
}

// AbstractCreature.java - 최적화된 순회
public void applyStartOfTurnPowers() {
    for (AbstractPower p : this.powers) {
        if (p.hasEvent(AbstractPower.EVENT_START_OF_TURN)) {
            p.atStartOfTurn();
        }
    }

    // ... 제거 로직 ...

    for (AbstractPower p : this.powers) {
        if (p.hasEvent(AbstractPower.EVENT_DURING_TURN)) {
            p.duringTurn();
        }
    }
}

// AbstractMonster.java - 최적화된 파티클 업데이트
public void update() {
    for (AbstractPower p : this.powers) {
        if (p.hasEvent(AbstractPower.EVENT_PARTICLES)) {
            p.updateParticles();
        }
    }
    // ...
}
```

### 방법 3: 인덱싱된 파워 맵

```java
// AbstractCreature.java - 이벤트 타입별 파워 맵
public class AbstractCreature {
    public ArrayList<AbstractPower> powers = new ArrayList<>();

    // 이벤트 타입을 키로 하는 맵
    private EnumMap<PowerEventType, ArrayList<AbstractPower>> powersByEvent;

    public enum PowerEventType {
        START_OF_TURN,
        DURING_TURN,
        END_OF_TURN,
        ON_ATTACK,
        ON_DAMAGED,
        ON_CARD_PLAY,
        UPDATE_PARTICLES,
        ON_HEAL
    }

    public AbstractCreature() {
        powersByEvent = new EnumMap<>(PowerEventType.class);
        for (PowerEventType type : PowerEventType.values()) {
            powersByEvent.put(type, new ArrayList<>());
        }
    }

    public void addPower(AbstractPower power) {
        this.powers.add(power);

        // 파워가 처리하는 이벤트 타입들을 조회
        for (PowerEventType type : power.getHandledEvents()) {
            powersByEvent.get(type).add(power);
        }
    }

    public void removePower(AbstractPower power) {
        this.powers.remove(power);

        // 모든 이벤트 맵에서 제거
        for (ArrayList<AbstractPower> list : powersByEvent.values()) {
            list.remove(power);
        }
    }

    // 최적화된 이벤트 처리
    public void applyStartOfTurnPowers() {
        ArrayList<AbstractPower> activePowers =
            powersByEvent.get(PowerEventType.START_OF_TURN);

        for (AbstractPower p : activePowers) {
            p.atStartOfTurn();
        }

        // 제거 로직...

        activePowers = powersByEvent.get(PowerEventType.DURING_TURN);
        for (AbstractPower p : activePowers) {
            p.duringTurn();
        }
    }
}

// AbstractPower.java - 처리하는 이벤트 선언
public abstract class AbstractPower {
    public EnumSet<PowerEventType> getHandledEvents() {
        return EnumSet.noneOf(PowerEventType.class);  // 기본: 아무것도 처리 안 함
    }
}

// 구체적 파워 구현
public class StrengthPower extends AbstractPower {
    @Override
    public EnumSet<PowerEventType> getHandledEvents() {
        return EnumSet.of(PowerEventType.ON_ATTACK);
    }
}

public class DrawCardPower extends AbstractPower {
    @Override
    public EnumSet<PowerEventType> getHandledEvents() {
        return EnumSet.of(
            PowerEventType.START_OF_TURN,
            PowerEventType.DURING_TURN
        );
    }
}
```

## 구현 가이드

### 단계 1: AbstractPower에 이벤트 마스크 추가

```java
// AbstractPower.java
public static final int EVENT_START_OF_TURN = 1;
public static final int EVENT_ATTACK = 2;
public static final int EVENT_PARTICLES = 4;
// ...

protected int eventMask = 0;

public boolean hasEvent(int eventType) {
    return (eventMask & eventType) != 0;
}
```

### 단계 2: 각 파워 클래스에서 이벤트 설정

```java
public class StrengthPower extends AbstractPower {
    public StrengthPower(AbstractCreature owner, int amount) {
        // ...
        this.eventMask = EVENT_ATTACK;
    }
}
```

### 단계 3: 순회 로직 최적화

```java
// 기존
for (AbstractPower p : this.powers) {
    p.atStartOfTurn();
}

// 최적화
for (AbstractPower p : this.powers) {
    if (p.hasEvent(AbstractPower.EVENT_START_OF_TURN)) {
        p.atStartOfTurn();
    }
}
```

## 측정 방법

```java
// 성능 측정
ArrayList<AbstractPower> powers = new ArrayList<>();
// 10개의 파워 추가 (실제 동작하는 것은 2개만)

long start = System.nanoTime();
for (int i = 0; i < 10000; i++) {
    for (AbstractPower p : powers) {
        p.atStartOfTurn();
    }
}
long baseline = System.nanoTime() - start;

start = System.nanoTime();
for (int i = 0; i < 10000; i++) {
    for (AbstractPower p : powers) {
        if (p.hasEvent(AbstractPower.EVENT_START_OF_TURN)) {
            p.atStartOfTurn();
        }
    }
}
long optimized = System.nanoTime() - start;

System.out.println("Baseline: " + baseline / 1000 + "μs");
System.out.println("Optimized: " + optimized / 1000 + "μs");
System.out.println("Improvement: " + (baseline - optimized) * 100 / baseline + "%");
```

## 예상 결과

**시나리오**: 10개 파워, 그 중 2개만 이벤트 처리

- **Before**: 10회 가상 메서드 호출 + 10회 빈 메서드 실행
- **After (비트마스크)**: 10회 비트 연산 + 2회 메서드 호출
- **After (분류 리스트)**: 2회 메서드 호출만
- **절감**: ~60-80% 감소

## 주의사항

1. **파워 추가/제거 오버헤드**: 분류 시스템 사용 시 파워 추가/제거 시 추가 작업 필요
2. **메모리 사용량**: 분류 리스트 방식은 메모리 사용량 증가
3. **동기화**: 파워 리스트와 분류 리스트 간 동기화 유지 필요
4. **기존 코드 호환성**: 모든 파워 클래스에 이벤트 마스크 추가 필요

## 관련 이슈

- [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md): 몬스터 업데이트 최적화
- [17_IntentRecalculation.md](17_IntentRecalculation.md): 인텐트 재계산 중복

## 참고 자료

- `AbstractPower.java` - 파워 기본 클래스
- `AbstractCreature.java:610-630` - 턴 시작 파워 처리
- `AbstractCreature.java:800-818` - 파워 업데이트
- `AbstractMonster.java:219-221` - 파워 파티클 업데이트
