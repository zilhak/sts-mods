# Monster wasHPLost 호출 타이밍 버그

## 버그 정보
- **Priority**: P2 (Major)
- **Category**: Combat
- **발견 위치**:
  - `AbstractMonster.java:783-785` (wasHPLost 호출)
  - `AbstractPlayer.java:1797-1799` (비교 대상)
- **영향 범위**: Monster가 Power를 가지는 모든 상황 (특히 커스텀 적 모드)
- **재현 조건**: RupturePower 등 wasHPLost를 구현하는 Power를 가진 Monster

## 버그 설명

### 현재 동작 (Current Behavior)
AbstractMonster는 **HP 감소 전**에, 그리고 **damageAmount에 관계없이** wasHPLost를 호출합니다.

```java
// AbstractMonster.java:783-785
for (AbstractPower p : this.powers) {
    p.wasHPLost(info, damageAmount);  // HP 감소 전 호출!
}

// ...

// AbstractMonster.java:803-812
if (damageAmount > 0) {
    // ...
    this.currentHealth -= damageAmount;  // HP 감소는 나중에
}
```

### 예상 동작 (Expected Behavior)
AbstractPlayer처럼 **HP 감소 후**에, 그리고 **damageAmount > 0일 때만** wasHPLost를 호출해야 합니다.

```java
// AbstractPlayer.java:1788-1803
if (damageAmount > 0) {
    // ...
    for (AbstractPower p : this.powers) {
        damageAmount = p.onLoseHp(damageAmount);  // 먼저 onLoseHp
    }

    // ...

    this.currentHealth -= damageAmount;  // HP 감소

    // ...

    for (AbstractPower p : this.powers) {
        p.wasHPLost(info, damageAmount);  // HP 감소 후 wasHPLost
    }
}
```

### 발생 원인 (Root Cause)
Player와 Monster의 damage() 메서드 구현이 일관성이 없습니다. wasHPLost의 의도는 "HP를 잃었을 때" 트리거되는 것인데, Monster는 HP 감소 전에 호출하므로 의미론적으로 잘못되었습니다.

## 코드 분석

### 문제가 되는 코드

#### AbstractMonster.java:783-846
```java
// Line 783-785: wasHPLost를 HP 감소 전에 호출
for (AbstractPower p : this.powers) {
    p.wasHPLost(info, damageAmount);  // ❌ 잘못된 위치
}

// Line 788-796: 다른 트리거들
if (info.owner != null) {
    for (AbstractPower p : info.owner.powers) {
        p.onAttack(info, damageAmount, this);
    }
}

for (AbstractPower p : this.powers) {
    damageAmount = p.onAttacked(info, damageAmount);
}

this.lastDamageTaken = Math.min(damageAmount, this.currentHealth);

boolean probablyInstantKill = (this.currentHealth == 0);

// Line 803-812: HP 감소는 여기서
if (damageAmount > 0) {
    if (info.owner != this) {
        useStaggerAnimation();
    }

    if (damageAmount >= 99 && !CardCrawlGame.overkill) {
        CardCrawlGame.overkill = true;
    }

    this.currentHealth -= damageAmount;  // ❌ HP 감소가 wasHPLost 호출 후
    if (!probablyInstantKill) {
        AbstractDungeon.effectList.add(new StrikeEffect(this, this.hb.cX, this.hb.cY, damageAmount));
    }
    if (this.currentHealth < 0) {
        this.currentHealth = 0;
    }
    healthBarUpdatedEvent();
}
```

#### AbstractPlayer.java:1788-1803 (올바른 구현)
```java
// Line 1788: damageAmount > 0 체크
if (damageAmount > 0) {
    for (AbstractPower p : this.powers) {
        damageAmount = p.onLoseHp(damageAmount);
    }

    for (AbstractRelic r : this.relics) {
        r.onLoseHp(damageAmount);
    }

    // Line 1797-1799: HP 감소 후 wasHPLost
    for (AbstractPower p : this.powers) {
        p.wasHPLost(info, damageAmount);  // ✅ 올바른 위치
    }

    // ... (더 많은 트리거)

    // Line 1822: HP 감소
    this.currentHealth -= damageAmount;  // ✅ wasHPLost 호출 전
}
```

### 호출 흐름 비교

#### Player 호출 흐름 (올바름)
```
1. damageAmount < 0 체크
2. Intangible 체크
3. decrementBlock (Block 처리)
4. onAttackToChangeDamage (relics)
5. onAttackToChangeDamage (owner powers)
6. onAttackedToChangeDamage (relics)
7. onAttackedToChangeDamage (powers)
8. onAttack (relics)
9. onAttack (owner powers)
10. onAttacked (powers)
11. onAttacked (relics)
12. onLoseHpLast (relics)
13. lastDamageTaken 설정
14. if (damageAmount > 0):
    a. onLoseHp (powers)
    b. onLoseHp (relics)
    c. wasHPLost (powers) ✅
    d. wasHPLost (relics)
    e. onInflictDamage (owner powers)
    f. currentHealth -= damageAmount
```

#### Monster 호출 흐름 (잘못됨)
```
1. Intangible 체크 (info.output 직접 수정)
2. damageAmount = info.output
3. isDying || isEscaping 체크
4. damageAmount < 0 체크
5. decrementBlock (Block 처리)
6. onAttackToChangeDamage (player relics)
7. onAttackToChangeDamage (owner powers)
8. onAttackedToChangeDamage (powers)
9. onAttack (player relics)
10. wasHPLost (powers) ❌ (HP 감소 전, damageAmount 체크 없음)
11. onAttack (owner powers)
12. onAttacked (powers)
13. lastDamageTaken 설정
14. if (damageAmount > 0):
    a. currentHealth -= damageAmount ❌ (wasHPLost 후)
```

## 재현 방법

### 테스트 시나리오 1: Rupture Power

1. Monster에 RupturePower 부여 (커스텀 모드 사용)
2. Player가 Monster를 공격하되 Block으로 완전히 막힘 (damageAmount = 0)
3. **잘못된 동작**: Monster의 RupturePower가 발동하여 Strength 증가
4. **올바른 동작**: damageAmount = 0이므로 RupturePower 발동하지 않음

### 테스트 시나리오 2: 타이밍 민감 Power

1. Monster에 커스텀 Power 부여:
   ```java
   public void wasHPLost(DamageInfo info, int damageAmount) {
       logger.info("Current HP: " + this.owner.currentHealth);
       logger.info("Damage: " + damageAmount);
   }
   ```
2. Monster 공격
3. **잘못된 동작**: 로그에 HP 감소 전 값 출력
4. **올바른 동작**: 로그에 HP 감소 후 값 출력

## 영향 받는 요소

### 현재 바닐라 게임
- **영향 없음**: 바닐라 게임에서 Monster는 wasHPLost를 구현하는 Power를 가지지 않음
- RupturePower는 Player 전용 (Ironclad)
- PlatedArmorPower는 Monster가 가지지만 wasHPLost를 오버라이드하지 않음

### 커스텀 모드에서 영향
- **높은 영향**: Monster에게 Rupture, 커스텀 Power 부여하는 모드
- **모드 호환성 문제**: 서로 다른 모드가 Monster Power를 추가할 때 예상치 못한 동작
- **디버깅 어려움**: HP 감소 전/후 타이밍 차이로 인한 버그 재현 어려움

## 수정 방법

### Option 1: Player와 동일한 구조로 변경 (권장)

**장점**:
- Player와 일관성 유지
- wasHPLost의 의미론적 정확성 보장
- 향후 Power 추가 시 예측 가능한 동작

**단점**:
- 코드 변경 범위 큼
- 기존 모드 중 이 버그에 의존하는 경우 문제 발생 가능성 (낮음)

**사이드 이펙트**:
- wasHPLost가 HP 감소 후 호출되므로, HP 체크하는 Power의 동작이 변경될 수 있음
- damageAmount = 0일 때 wasHPLost가 호출되지 않으므로, 일부 Power가 발동하지 않을 수 있음

**패치 코드**:
```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "damage"
)
public class FixMonsterWasHPLostTimingPatch {
    @SpireInsertPatch(
        locator = WasHPLostLocator.class
    )
    public static SpireReturn<Void> RemoveWasHPLost(AbstractMonster __instance, DamageInfo info) {
        // 기존 wasHPLost 호출 제거
        return SpireReturn.Continue();
    }

    @SpireInsertPatch(
        locator = AfterHPLossLocator.class
    )
    public static void InsertWasHPLostAfterHPLoss(AbstractMonster __instance, DamageInfo info, @ByRef int[] damageAmount) {
        // HP 감소 후 wasHPLost 호출 (damageAmount > 0 체크 포함)
        if (damageAmount[0] > 0) {
            for (AbstractPower p : __instance.powers) {
                p.wasHPLost(info, damageAmount[0]);
            }
        }
    }

    private static class WasHPLostLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                AbstractPower.class, "wasHPLost"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }

    private static class AfterHPLossLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractMonster.class, "currentHealth"
            );
            int[] lines = LineFinder.findAllInOrder(ctMethodToPatch, finalMatcher);
            // currentHealth -= damageAmount 라인 찾기
            return new int[] { lines[lines.length - 1] };
        }
    }
}
```

### Option 2: wasHPLost 호출 전에 damageAmount > 0 체크 추가 (최소 수정)

**장점**:
- 최소한의 코드 변경
- Block으로 완전히 막았을 때 Power 발동 방지

**단점**:
- 여전히 HP 감소 전에 호출되므로 의미론적으로 부정확
- Player와 일관성 없음

**사이드 이펙트**:
- damageAmount = 0일 때 wasHPLost가 호출되지 않음

**패치 코드**:
```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "damage"
)
public class FixMonsterWasHPLostZeroCheckPatch {
    @SpirePrefixPatch
    public static void PrefixWasHPLost(AbstractMonster __instance, DamageInfo info) {
        // damageAmount 계산 (decrementBlock 후의 값)
        // 이 방법은 복잡하므로 Option 1 권장
    }
}
```

### Option 3: Prefix Patch로 완전히 대체 (가장 안전)

**장점**:
- 원본 메서드를 완전히 제어
- Player와 동일한 로직 구현 가능

**단점**:
- 긴 코드 복사 필요
- 다른 모드와 충돌 가능성

**사이드 이펙트**:
- 원본 메서드 실행 차단으로 다른 Prefix 패치와 충돌 가능

**패치 코드**:
```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "damage"
)
public class CompleteMonsterDamageRewritePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractMonster __instance, DamageInfo info) {
        // AbstractMonster.damage() 전체를 올바른 순서로 재구현
        // ... (생략: 매우 긴 코드)

        return SpireReturn.Return(null);  // 원본 메서드 실행 차단
    }
}
```

## 관련 버그

- [P2_Combat_MonsterMissingRelicTriggers.md] - Monster가 onLoseHpLast 등 Relic 트리거를 호출하지 않음
- [P3_Combat_PlayerMonsterInconsistency.md] - Player와 Monster의 damage() 메서드 불일치

## 참고 자료

### 관련 Powers
- `RupturePower.java:26-31` - wasHPLost 구현 (damageAmount > 0 && info.owner == this.owner 체크)
- `PlatedArmorPower.java` - wasHPLost 구현하지 않음 (onLoseHp 사용)

### 관련 Relics
- `TungstenRod.java:16-22` - onLoseHpLast 구현 (Monster는 호출하지 않음)

### 커뮤니티 논의
- (해당 버그에 대한 커뮤니티 리포트를 찾지 못함)

## 검증 방법

1. **테스트 모드 작성**:
   ```java
   public class TestMonsterRupture extends AbstractMonster {
       public TestMonsterRupture() {
           super("Test", "TestMonster", 100, 0.0F, 0.0F, 200.0F, 200.0F, null);
           // RupturePower 부여
           this.powers.add(new RupturePower(this, 2));
       }
   }
   ```

2. **시나리오 1**: Monster를 공격하되 Block으로 완전히 막음
   - **Before Fix**: RupturePower 발동, Strength 증가
   - **After Fix**: RupturePower 발동하지 않음

3. **시나리오 2**: Monster를 공격하여 HP 감소
   - **Before Fix**: wasHPLost가 HP 감소 전에 호출됨 (로그 확인)
   - **After Fix**: wasHPLost가 HP 감소 후에 호출됨 (로그 확인)

4. **시나리오 3**: Monster의 HP가 정확히 damageAmount와 같을 때
   - **Before Fix**: wasHPLost 호출 시 currentHealth = 원래 값
   - **After Fix**: wasHPLost 호출 시 currentHealth = 0

## 위험성 평가

### 바닐라 게임
- **Risk Level**: Low
- **이유**: 바닐라 Monster는 wasHPLost를 사용하는 Power가 없음

### 모드 환경
- **Risk Level**: Medium to High
- **이유**: 커스텀 Monster + Power 조합 시 예상치 못한 동작
- **특히 위험한 경우**:
  - Rupture를 Monster에게 부여하는 모드
  - wasHPLost에서 HP 값을 체크하는 커스텀 Power
  - 다단히트 공격과 wasHPLost 조합

### 모드 호환성 문제
1. **Power 추가 모드**: Monster에게 Player Power를 부여할 때 예상과 다른 동작
2. **밸런스 모드**: wasHPLost 타이밍에 의존하는 밸런스 조정이 깨질 수 있음
3. **디버그 모드**: wasHPLost에서 HP 로깅 시 잘못된 값 표시

## 결론

이 버그는 바닐라 게임에는 영향이 없지만, **모드 환경에서 Player와 Monster의 동작 불일치**를 야기합니다. 특히:

1. **의미론적 오류**: "HP를 잃었을 때"라는 의미와 달리 HP 감소 전에 호출
2. **damageAmount = 0 케이스**: Block으로 완전히 막았는데도 Power 발동
3. **모드 호환성**: 서로 다른 모드가 예상치 못한 상호작용

**권장 사항**: Option 1 (Player와 동일한 구조로 변경)을 적용하여 일관성 보장
