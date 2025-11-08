# 17. Intent Damage Recalculation Redundancy

## 문제 설명

몬스터의 인텐트 데미지가 `applyPowers()` 호출 시마다 완전히 재계산되지만, 실제로는 파워 변경이 있을 때만 재계산이 필요합니다. 매 턴마다 불필요한 중복 계산이 발생합니다.

## 성능 영향

**비용**: 몬스터당 파워 스택 × 2회 순회 (평균 4-8 파워)
**빈도**: 카드 사용 시, 파워 적용 시마다
**예상 절감**: 파워가 많은 전투에서 ~60% 감소

## 코드 위치

```
AbstractMonster.java:1343-1363 (applyPowers)
AbstractMonster.java:1293-1341 (calculateDamage)
```

## 현재 구현

```java
// applyPowers() - 매번 모든 몬스터의 데미지 재계산
public void applyPowers() {
    boolean applyBackAttack = applyBackAttack();

    if (applyBackAttack && !hasPower("BackAttack")) {
        AbstractDungeon.actionManager.addToTop(
            new ApplyPowerAction(this, null, new BackAttackPower(this))
        );
    }

    // 모든 데미지 정보 재계산
    for (DamageInfo dmg : this.damage) {
        dmg.applyPowers(this, AbstractDungeon.player);
        if (applyBackAttack) {
            dmg.output = (int)(dmg.output * 1.5F);
        }
    }

    // 인텐트 데미지 재계산
    if (this.move.baseDamage > -1) {
        calculateDamage(this.move.baseDamage);  // ← 중복 계산
    }

    this.intentImg = getIntentImg();
    updateIntentTip();
}

// calculateDamage() - 파워 스택 2회 순회
private void calculateDamage(int dmg) {
    AbstractPlayer target = AbstractDungeon.player;
    float tmp = dmg;

    // Blight 모디파이어
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("DeadlyEnemies")) {
        float mod = AbstractDungeon.player.getBlight("DeadlyEnemies").effectFloat();
        tmp *= mod;
    }

    // 1차 순회: 몬스터 파워 (atDamageGive)
    for (AbstractPower p : this.powers) {
        tmp = p.atDamageGive(tmp, DamageInfo.DamageType.NORMAL);
    }

    // 2차 순회: 플레이어 파워 (atDamageReceive)
    for (AbstractPower p : target.powers) {
        tmp = p.atDamageReceive(tmp, DamageInfo.DamageType.NORMAL);
    }

    tmp = AbstractDungeon.player.stance.atDamageReceive(tmp, DamageInfo.DamageType.NORMAL);

    if (applyBackAttack()) {
        tmp = (int)(tmp * 1.5F);
    }

    // 3차 순회: 몬스터 파워 (atDamageFinalGive)
    for (AbstractPower p : this.powers) {
        tmp = p.atDamageFinalGive(tmp, DamageInfo.DamageType.NORMAL);
    }

    // 4차 순회: 플레이어 파워 (atDamageFinalReceive)
    for (AbstractPower p : target.powers) {
        tmp = p.atDamageFinalReceive(tmp, DamageInfo.DamageType.NORMAL);
    }

    dmg = MathUtils.floor(tmp);
    if (dmg < 0) {
        dmg = 0;
    }

    this.intentDmg = dmg;
}
```

## 문제 분석

1. **불필요한 호출**: 파워 변경이 없어도 `applyPowers()` 호출 시 매번 재계산
2. **중복 순회**: 4번의 파워 스택 순회 (몬스터 × 2, 플레이어 × 2)
3. **캐싱 부재**: 이전 계산 결과를 재사용하지 않음
4. **조기 최적화 없음**: 파워가 없어도 동일한 루프 실행

## 최적화 전략

### 방법 1: 파워 버전 추적

```java
// AbstractMonster.java
private int lastPowerVersion = -1;  // 마지막 계산 시점의 파워 버전
private int cachedIntentDmg = -1;   // 캐시된 인텐트 데미지

// AbstractCreature.java에 추가
private int powerVersion = 0;        // 파워 변경 시마다 증가

public void addPower(AbstractPower power) {
    // ... 기존 코드 ...
    this.powers.add(power);
    powerVersion++;  // 버전 증가
}

public void removePower(AbstractPower power) {
    // ... 기존 코드 ...
    this.powers.remove(power);
    powerVersion++;  // 버전 증가
}

// AbstractMonster.java - 최적화된 applyPowers
public void applyPowers() {
    boolean applyBackAttack = applyBackAttack();

    if (applyBackAttack && !hasPower("BackAttack")) {
        AbstractDungeon.actionManager.addToTop(
            new ApplyPowerAction(this, null, new BackAttackPower(this))
        );
    }

    // DamageInfo 배열 업데이트
    for (DamageInfo dmg : this.damage) {
        dmg.applyPowers(this, AbstractDungeon.player);
        if (applyBackAttack) {
            dmg.output = (int)(dmg.output * 1.5F);
        }
    }

    // 인텐트 데미지: 파워 변경 시만 재계산
    if (this.move.baseDamage > -1) {
        int currentVersion = this.powerVersion +
                            AbstractDungeon.player.powerVersion;

        if (currentVersion != lastPowerVersion) {
            calculateDamage(this.move.baseDamage);
            lastPowerVersion = currentVersion;
            cachedIntentDmg = this.intentDmg;
        } else {
            // 캐시된 값 사용
            this.intentDmg = cachedIntentDmg;
        }
    }

    this.intentImg = getIntentImg();
    updateIntentTip();
}
```

### 방법 2: 더티 플래그 + 조기 종료

```java
private boolean powersDirty = true;  // 파워 변경 여부

public void applyPowers() {
    // 파워 변경이 없으면 스킵
    if (!powersDirty && this.move.baseDamage > -1) {
        // 인텐트 이미지만 업데이트 (파워 아이콘 변경 가능)
        this.intentImg = getIntentImg();
        return;
    }

    boolean applyBackAttack = applyBackAttack();

    if (applyBackAttack && !hasPower("BackAttack")) {
        AbstractDungeon.actionManager.addToTop(
            new ApplyPowerAction(this, null, new BackAttackPower(this))
        );
    }

    for (DamageInfo dmg : this.damage) {
        dmg.applyPowers(this, AbstractDungeon.player);
        if (applyBackAttack) {
            dmg.output = (int)(dmg.output * 1.5F);
        }
    }

    if (this.move.baseDamage > -1) {
        calculateDamage(this.move.baseDamage);
    }

    this.intentImg = getIntentImg();
    updateIntentTip();
    powersDirty = false;
}

// 파워 변경 시 플래그 설정
public void addPower(AbstractPower power) {
    // ... 기존 코드 ...
    this.powers.add(power);
    powersDirty = true;
}
```

### 방법 3: 파워 스택 순회 최적화

```java
private void calculateDamage(int dmg) {
    AbstractPlayer target = AbstractDungeon.player;
    float tmp = dmg;

    // Blight 모디파이어
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("DeadlyEnemies")) {
        float mod = AbstractDungeon.player.getBlight("DeadlyEnemies").effectFloat();
        tmp *= mod;
    }

    // 조기 종료 최적화
    boolean hasMonsterPowers = !this.powers.isEmpty();
    boolean hasPlayerPowers = !target.powers.isEmpty();

    if (!hasMonsterPowers && !hasPlayerPowers) {
        // 파워가 없으면 즉시 반환
        this.intentDmg = MathUtils.floor(tmp);
        return;
    }

    // 단일 순회로 통합 (가능한 경우)
    if (hasMonsterPowers) {
        for (AbstractPower p : this.powers) {
            tmp = p.atDamageGive(tmp, DamageInfo.DamageType.NORMAL);
        }
    }

    if (hasPlayerPowers) {
        for (AbstractPower p : target.powers) {
            tmp = p.atDamageReceive(tmp, DamageInfo.DamageType.NORMAL);
        }
    }

    tmp = AbstractDungeon.player.stance.atDamageReceive(tmp, DamageInfo.DamageType.NORMAL);

    if (applyBackAttack()) {
        tmp = (int)(tmp * 1.5F);
    }

    // Final 모디파이어
    if (hasMonsterPowers) {
        for (AbstractPower p : this.powers) {
            tmp = p.atDamageFinalGive(tmp, DamageInfo.DamageType.NORMAL);
        }
    }

    if (hasPlayerPowers) {
        for (AbstractPower p : target.powers) {
            tmp = p.atDamageFinalReceive(tmp, DamageInfo.DamageType.NORMAL);
        }
    }

    dmg = MathUtils.floor(tmp);
    if (dmg < 0) {
        dmg = 0;
    }

    this.intentDmg = dmg;
}
```

## 구현 가이드

### 단계 1: AbstractCreature에 파워 버전 추가

```java
// AbstractCreature.java
protected int powerVersion = 0;

public int getPowerVersion() {
    return powerVersion;
}

// addPower, removePower에서 버전 증가
```

### 단계 2: AbstractMonster에 캐싱 필드 추가

```java
// AbstractMonster.java
private int lastPowerVersion = -1;
private int cachedIntentDmg = -1;
```

### 단계 3: applyPowers() 최적화

```java
public void applyPowers() {
    // ... 기존 코드 ...

    if (this.move.baseDamage > -1) {
        int currentVersion = this.powerVersion +
                            AbstractDungeon.player.getPowerVersion();

        if (currentVersion != lastPowerVersion) {
            calculateDamage(this.move.baseDamage);
            lastPowerVersion = currentVersion;
            cachedIntentDmg = this.intentDmg;
        } else {
            this.intentDmg = cachedIntentDmg;
        }
    }

    // ... 기존 코드 ...
}
```

### 단계 4: calculateDamage() 조기 종료 추가

```java
private void calculateDamage(int dmg) {
    // 조기 종료 체크 추가
    if (this.powers.isEmpty() &&
        AbstractDungeon.player.powers.isEmpty() &&
        !Settings.isEndless) {
        this.intentDmg = dmg;
        return;
    }

    // ... 기존 로직 ...
}
```

## 측정 방법

```java
// 성능 측정 코드
int iterations = 1000;
long start = System.nanoTime();

for (int i = 0; i < iterations; i++) {
    for (AbstractMonster m : monsters) {
        m.applyPowers();
    }
}

long elapsed = System.nanoTime() - start;
System.out.println("applyPowers avg: " + (elapsed / iterations / 1000) + "μs");
```

## 예상 결과

**시나리오**: 3 몬스터, 각 5개 파워, 플레이어 5개 파워

- **Before**:
  - 파워 순회: 3 × (5 + 5) × 4 = 120 iterations/call
  - 카드 10장 사용 = 1,200 iterations

- **After** (캐싱):
  - 첫 계산: 120 iterations
  - 이후: 0 iterations (캐시 히트)
  - 10장 중 파워 변경 3번 = 360 iterations

- **절감**: ~70% 감소

## 주의사항

1. **버전 동기화**: 플레이어와 몬스터의 파워 버전 모두 확인 필요
2. **Stance 변경**: Stance 변경 시에도 재계산 필요 (stance 버전 추가 고려)
3. **Blight 효과**: Settings.isEndless 변경 시 재계산 필요
4. **BackAttack**: 위치 변경 시 재계산 필요

## 관련 이슈

- [16_MonsterAIThrottle.md](16_MonsterAIThrottle.md): 몬스터 AI 업데이트 최적화
- [18_PowerStackIteration.md](18_PowerStackIteration.md): 파워 스택 순회 최적화

## 참고 자료

- `AbstractMonster.java:1343-1363` - applyPowers() 메서드
- `AbstractMonster.java:1293-1341` - calculateDamage() 메서드
- `DamageInfo.java:applyPowers()` - 데미지 계산 로직
