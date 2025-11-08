# 20. Monster getMove() Redundant Calls

## 문제 설명

몬스터의 `getMove()` 메서드가 필요 이상으로 호출됩니다. 이 메서드는 다음 행동을 결정하는 AI 로직을 포함하고 있어 비용이 크지만, 턴마다 한 번만 호출되어야 하는데 중복 호출되는 경우가 있습니다.

## 성능 영향

**비용**: 몬스터당 복잡한 AI 로직 (조건 분기, 랜덤 생성 등)
**빈도**: 턴당 1회 (정상) vs 여러 번 (비정상)
**예상 절감**: 중복 호출 제거 시 ~40% 감소

## 코드 위치

```
AbstractMonster.java:563-565 (rollMove)
AbstractMonster.java:852 (init)
AbstractMonster.java:1521 (getMove - abstract)
```

## 현재 구현

```java
// AbstractMonster.java - rollMove는 단순 래퍼
public void rollMove() {
    getMove(AbstractDungeon.aiRng.random(99));  // 0-99 랜덤
}

// init()에서 초기 호출
public void init() {
    rollMove();
    healthBarUpdatedEvent();
}

// 구체적인 몬스터 구현 예시
public class Cultist extends AbstractMonster {
    private static final byte INCANTATION = 1;
    private static final byte DARK_STRIKE = 2;

    protected void getMove(int num) {
        // 복잡한 조건 분기
        if (this.firstMove) {
            this.firstMove = false;
            setMove((byte)1, Intent.BUFF);  // 첫 턴은 항상 버프
            return;
        }

        // 이전 행동 확인
        if (lastMove((byte)1)) {
            setMove((byte)2, Intent.ATTACK, this.damage.get(0).base);
        } else {
            // 50% 확률로 선택
            if (num < 50) {
                setMove((byte)1, Intent.BUFF);
            } else {
                setMove((byte)2, Intent.ATTACK, this.damage.get(0).base);
            }
        }
    }
}

// 더 복잡한 예시: TheGuardian
public class TheGuardian extends AbstractMonster {
    protected void getMove(int num) {
        // 여러 단계의 조건 확인
        if (this.form1) {
            if (this.moveCount % 4 == 0) {
                setMove((byte)1, Intent.ATTACK_DEFEND, this.damage.get(0).base);
            } else if (this.moveCount % 4 == 1) {
                setMove((byte)2, Intent.ATTACK_BUFF, this.damage.get(1).base);
            } else if (this.moveCount % 4 == 2) {
                setMove((byte)3, Intent.ATTACK, this.damage.get(2).base);
            } else {
                setMove((byte)4, Intent.DEFEND_BUFF);
            }
            this.moveCount++;
        } else {
            // Mode 2 로직...
        }
    }
}
```

## 문제 분석

1. **중복 호출 위치**:
   - `MonsterGroup.showIntent()`: 전투 시작 시 호출
   - 특정 몬스터가 상태 변경 시 `getMove()` 재호출
   - 일부 몬스터가 `takeTurn()` 내부에서 다시 호출

2. **불필요한 재계산**:
   ```java
   // ShelledParasite.java - changeState 내부에서 재호출
   public void changeState(String stateName) {
       if (stateName.equals("CLOSED")) {
           // ...
           getMove(AbstractDungeon.aiRng.random(20, 99));  // 중복 호출
       }
   }
   ```

3. **랜덤 상태 낭비**:
   - 매 호출마다 새로운 난수 생성
   - 동일한 결과를 위해 RNG 상태 소모

## 최적화 전략

### 방법 1: 캐싱 + 무효화 플래그

```java
// AbstractMonster.java
private boolean moveCalculated = false;  // 이번 턴에 이미 계산됨
private int cachedMoveNum = -1;          // 캐시된 난수

public void rollMove() {
    if (!moveCalculated) {
        cachedMoveNum = AbstractDungeon.aiRng.random(99);
        getMove(cachedMoveNum);
        moveCalculated = true;
    }
    // 이미 계산되었으면 스킵
}

// 턴 종료 시 플래그 초기화
public void applyEndOfTurnTriggers() {
    super.applyEndOfTurnTriggers();
    moveCalculated = false;  // 다음 턴을 위해 리셋
}

// 강제 재계산이 필요한 경우
public void forceRollMove() {
    moveCalculated = false;
    rollMove();
}
```

### 방법 2: 상태 변경 추적

```java
// AbstractMonster.java
private byte currentMove = -1;
private int moveVersion = 0;       // 행동이 변경될 때마다 증가
private int calculatedVersion = -1; // 마지막 계산 시점의 버전

public void rollMove() {
    // 버전이 다르면 재계산 필요
    if (calculatedVersion != moveVersion) {
        getMove(AbstractDungeon.aiRng.random(99));
        calculatedVersion = moveVersion;
    }
}

// 상태 변경 시 버전 증가
public void changeState(String stateName) {
    // ... 상태 변경 로직 ...
    moveVersion++;  // 행동 재계산 필요 표시
}

// setMove()에서도 현재 행동 추적
public void setMove(byte nextMove, Intent intent, int baseDamage,
                   int multiplier, boolean isMultiDamage) {
    this.currentMove = nextMove;
    // ... 기존 로직 ...
}
```

### 방법 3: Lazy Evaluation

```java
// AbstractMonster.java
private boolean moveNeedsUpdate = true;
private Supplier<Integer> moveNumSupplier;  // 난수 생성 지연

public void rollMove() {
    if (moveNeedsUpdate) {
        // 난수 생성을 지연시킴
        int num = (moveNumSupplier != null) ?
                  moveNumSupplier.get() :
                  AbstractDungeon.aiRng.random(99);

        getMove(num);
        moveNeedsUpdate = false;
    }
}

// 인텐트 표시 시점에만 실제 계산
public void createIntent() {
    if (moveNeedsUpdate) {
        rollMove();  // 이 시점에 처음 계산
    }

    this.intent = this.move.intent;
    // ... 기존 로직 ...
}

// 턴 종료 시 플래그만 설정
public void applyEndOfTurnTriggers() {
    super.applyEndOfTurnTriggers();
    moveNeedsUpdate = true;  // 다음 계산 시 갱신 필요
}
```

### 방법 4: 전략 패턴으로 복잡도 감소

```java
// MonsterMoveStrategy - 행동 결정 로직을 분리
public interface MonsterMoveStrategy {
    Move calculateMove(int randomNum, AbstractMonster monster);
}

// 간단한 패턴: 순환
public class CyclicMoveStrategy implements MonsterMoveStrategy {
    private byte[] movePattern;
    private int currentIndex = 0;

    public CyclicMoveStrategy(byte[] pattern) {
        this.movePattern = pattern;
    }

    @Override
    public Move calculateMove(int randomNum, AbstractMonster monster) {
        // 조건 분기 없이 인덱스만 증가
        byte move = movePattern[currentIndex];
        currentIndex = (currentIndex + 1) % movePattern.length;
        return new Move(move, /* intent */, /* damage */);
    }
}

// 확률 기반 패턴
public class WeightedMoveStrategy implements MonsterMoveStrategy {
    private List<WeightedMove> moves;

    public WeightedMoveStrategy() {
        this.moves = new ArrayList<>();
    }

    public void addMove(byte move, int weight, Intent intent) {
        moves.add(new WeightedMove(move, weight, intent));
    }

    @Override
    public Move calculateMove(int randomNum, AbstractMonster monster) {
        // 미리 계산된 가중치 테이블 사용
        int totalWeight = 100;
        int roll = randomNum;

        for (WeightedMove wm : moves) {
            if (roll < wm.weight) {
                return new Move(wm.move, wm.intent, wm.damage);
            }
            roll -= wm.weight;
        }

        return moves.get(0).toMove();  // fallback
    }
}

// AbstractMonster에서 사용
public abstract class AbstractMonster {
    protected MonsterMoveStrategy moveStrategy;

    protected void getMove(int num) {
        if (moveStrategy != null) {
            Move move = moveStrategy.calculateMove(num, this);
            setMove(move.nextMove, move.intent, move.damage);
        } else {
            // 기존 방식
            getMoveLegacy(num);
        }
    }

    protected abstract void getMoveLegacy(int num);
}

// Cultist 예시
public class Cultist extends AbstractMonster {
    public Cultist() {
        super(/* ... */);

        // 간단한 전략 설정
        WeightedMoveStrategy strategy = new WeightedMoveStrategy();
        strategy.addMove((byte)1, 50, Intent.BUFF);
        strategy.addMove((byte)2, 50, Intent.ATTACK);
        this.moveStrategy = strategy;
    }
}
```

## 구현 가이드

### 단계 1: 캐싱 플래그 추가

```java
// AbstractMonster.java
private boolean moveCalculated = false;
```

### 단계 2: rollMove() 수정

```java
public void rollMove() {
    if (!moveCalculated) {
        getMove(AbstractDungeon.aiRng.random(99));
        moveCalculated = true;
    }
}
```

### 단계 3: 턴 종료 시 리셋

```java
public void applyEndOfTurnTriggers() {
    super.applyEndOfTurnTriggers();
    moveCalculated = false;
}
```

### 단계 4: 특수 케이스 처리

```java
// 상태 변경 등으로 강제 재계산 필요한 경우
public void changeState(String stateName) {
    // ... 상태 변경 ...
    moveCalculated = false;  // 재계산 필요
    rollMove();
}
```

## 측정 방법

```java
// 성능 측정
int callCount = 0;

// AbstractMonster.getMove()에 카운터 추가
protected void getMove(int num) {
    callCount++;
    // ... 기존 로직 ...
}

// 전투 1턴 실행 후
System.out.println("getMove calls in 1 turn: " + callCount);
// 예상: 몬스터 3개 × 1회 = 3회
// 실제: 몬스터 3개 × 2-3회 = 6-9회 (중복)
```

## 예상 결과

**시나리오**: 3 몬스터, 10턴 전투

- **Before**:
  - 평균 2.5회/몬스터/턴 (중복 호출 포함)
  - 총 호출: 3 × 2.5 × 10 = 75회

- **After** (캐싱):
  - 1회/몬스터/턴 (정확히 필요한 만큼만)
  - 총 호출: 3 × 1 × 10 = 30회
  - 절감: ~60% 감소

## 주의사항

1. **상태 변경 동기화**: 몬스터 상태가 바뀔 때 `moveCalculated = false` 필수
2. **RNG 일관성**: 캐싱으로 인해 동일한 난수를 재사용하므로 결정론적 동작 보장
3. **특수 몬스터**: 일부 몬스터는 턴 중에 행동을 바꿀 수 있음 (예: 형태 변환)
4. **멀티플레이어**: 동기화 필요 시 캐싱 비활성화 고려

## 관련 코드 위치

```java
// 중복 호출이 발견된 위치들
1. MonsterGroup.showIntent() - 전투 시작
2. AbstractMonster.init() - 초기화
3. ShelledParasite.changeState() - 상태 변경
4. GremlinLeader.getMove() - 내부에서 재귀 호출
5. SlimeBoss.changeState() - 형태 변환
```

## 관련 이슈

- [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md): 몬스터 업데이트 최적화
- [17_IntentRecalculation.md](17_IntentRecalculation.md): 인텐트 재계산 중복

## 참고 자료

- `AbstractMonster.java:563-565` - rollMove() 메서드
- `MonsterGroup.java:67-71` - showIntent() 메서드
- `ShelledParasite.java:155-190` - 상태 변경 시 getMove 재호출
- `GremlinLeader.java:154-188` - 복잡한 getMove 로직
