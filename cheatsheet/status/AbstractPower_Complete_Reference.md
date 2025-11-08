# AbstractPower 완전 레퍼런스

**파일**: `com.megacrit.cardcrawl.powers.AbstractPower`

## 개요

모든 파워(Power)의 기반이 되는 추상 클래스입니다. **50개 이상의 훅(Hook) 메서드**를 제공하여, 게임의 거의 모든 상황에 반응할 수 있습니다.

### 훅(Hook) 메서드란?

게임 엔진이 특정 시점에 자동으로 호출하는 메서드입니다. 파워는 이 훅들을 오버라이드하여 원하는 효과를 구현합니다.

**예시**:
```java
public class MyCustomPower extends AbstractPower {
    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        // 이 메서드는 "데미지를 줄 때" 자동으로 호출됨
        return damage * 2.0F;  // 데미지 2배
    }
}
```

---

## 📚 전체 훅 메서드 목록

### 데미지 관련 훅 (9개)

#### 1. atDamageGive()
```java
public float atDamageGive(float damage, DamageInfo.DamageType type)
```

**호출 시점**: 데미지를 **줄 때** (공격자의 파워)
**실행 순서**: 2번째 (calculateCardDamage 다음)
**파라미터**:
- `damage`: 현재 데미지 값
- `type`: 데미지 타입 (NORMAL, THORNS, HP_LOSS)

**반환**: 수정된 데미지 값
**사용 예**:
- `StrengthPower`: `return damage + this.amount;` (가산)
- `WeakPower`: `return damage * 0.75F;` (곱셈)

**주의사항**:
- 여러 파워가 있으면 순차적으로 적용됨
- Priority가 낮을수록 먼저 실행 (Weak는 priority=99로 매우 먼저 실행)

---

#### 2. atDamageFinalGive()
```java
public float atDamageFinalGive(float damage, DamageInfo.DamageType type)
```

**호출 시점**: **최종** 데미지 계산 (공격자 파워, 모든 atDamageGive 이후)
**실행 순서**: 4번째
**파라미터**: atDamageGive와 동일
**반환**: 최종 수정된 데미지
**사용 예**: 드물게 사용됨 (특수 효과용)

**차이점**:
- `atDamageGive`: 여러 파워가 순차적으로 적용
- `atDamageFinalGive`: 모든 처리 후 마지막 조정

---

#### 3. atDamageReceive()
```java
public float atDamageReceive(float damage, DamageInfo.DamageType damageType)
```

**호출 시점**: 데미지를 **받을 때** (피격자의 파워)
**실행 순서**: 5번째
**파라미터**: 동일
**반환**: 수정된 받는 데미지
**사용 예**:
- `VulnerablePower`: `return damage * 1.5F;` (50% 증가)

---

#### 4. atDamageFinalReceive()
```java
public float atDamageFinalReceive(float damage, DamageInfo.DamageType type)
```

**호출 시점**: **최종** 받는 데미지 계산 (모든 atDamageReceive 이후)
**실행 순서**: 6번째 (마지막)
**파라미터**: 동일
**반환**: 최종 받는 데미지
**사용 예**:
- `IntangiblePower`: `return 1.0F;` (데미지를 1로 고정)

**중요**: 가장 마지막에 실행되므로, 다른 모든 데미지 수정을 무시하고 강제 적용 가능

---

#### 5. onAttacked()
```java
public int onAttacked(DamageInfo info, int damageAmount)
```

**호출 시점**: 공격을 받은 **직후**
**파라미터**:
- `info`: 데미지 정보 (공격자, 타입 등)
- `damageAmount`: 실제로 받은 데미지

**반환**: 수정된 데미지 (대부분 그대로 반환)
**사용 예**:
- 공격받을 때 반격 효과
- 공격받을 때 파워 스택 증가

---

#### 6. onAttack()
```java
public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target)
```

**호출 시점**: 공격한 **직후**
**파라미터**:
- `info`: 데미지 정보
- `damageAmount`: 준 데미지
- `target`: 공격 대상

**반환**: 없음 (void)
**사용 예**:
- 공격 시 추가 효과 (독 부여, 버프 등)

---

#### 7. onAttackedToChangeDamage()
```java
public int onAttackedToChangeDamage(DamageInfo info, int damageAmount)
```

**호출 시점**: 공격받기 전 (데미지 변경용)
**파라미터**: onAttacked와 동일
**반환**: 변경된 데미지
**사용 예**: 특수 데미지 감소 효과

---

#### 8. onAttackToChangeDamage()
```java
public int onAttackToChangeDamage(DamageInfo info, int damageAmount)
```

**호출 시점**: 공격하기 전 (데미지 변경용)
**파라미터**: onAttack과 동일
**반환**: 변경된 데미지
**사용 예**: 특수 데미지 증가 효과

---

#### 9. onInflictDamage()
```java
public void onInflictDamage(DamageInfo info, int damageAmount, AbstractCreature target)
```

**호출 시점**: 데미지를 입힌 직후
**파라미터**: onAttack과 동일
**반환**: 없음
**사용 예**: 데미지 입힐 때 추가 효과

---

### 블록 관련 훅 (4개)

#### 10. modifyBlock()
```java
public float modifyBlock(float blockAmount)
```

**호출 시점**: 블록 획득 **직전**
**실행 순서**: Priority 낮은 순 (곱셈 효과가 먼저)
**파라미터**: `blockAmount` - 원래 블록량
**반환**: 수정된 블록량
**사용 예**:
- `FrailPower`: `return blockAmount * 0.75F;` (25% 감소)
- `DexterityPower`: `return blockAmount + this.amount;` (가산)

**중요**:
- **곱셈 효과** (Frail)는 Priority 높게 (먼저 실행)
- **가산 효과** (Dexterity)는 Priority 낮게 (나중 실행)

---

#### 11. modifyBlockLast()
```java
public float modifyBlockLast(float blockAmount)
```

**호출 시점**: modifyBlock() 이후, 최종 블록 조정
**파라미터**: 동일
**반환**: 최종 블록량
**사용 예**: 특수 블록 수정 (드물게 사용)

---

#### 12. onGainedBlock()
```java
public void onGainedBlock(float blockAmount)
```

**호출 시점**: 블록 획득 **후**
**파라미터**: 획득한 블록량
**반환**: 없음
**사용 예**: 블록 획득 시 추가 효과 (버프 등)

---

#### 13. onPlayerGainedBlock()
```java
public int onPlayerGainedBlock(int blockAmount)
```

**호출 시점**: 플레이어가 블록 획득 **후**
**파라미터**: 획득한 블록량
**반환**: 변경된 블록량 (드물게 사용)
**사용 예**: 플레이어 전용 블록 효과

---

### 턴 관련 훅 (5개)

#### 14. atStartOfTurn()
```java
public void atStartOfTurn()
```

**호출 시점**: 턴 **시작** 시 (카드 드로우 전)
**파라미터**: 없음
**반환**: 없음
**사용 예**:
- `PoisonPower`: 독 데미지 적용
- `RegeneratePower`: 체력 회복

---

#### 15. atStartOfTurnPostDraw()
```java
public void atStartOfTurnPostDraw()
```

**호출 시점**: 턴 시작 시 (카드 드로우 **후**)
**파라미터**: 없음
**반환**: 없음
**사용 예**: 카드 드로우 후 효과

---

#### 16. atEndOfTurn()
```java
public void atEndOfTurn(boolean isPlayer)
```

**호출 시점**: 턴 **종료** 시
**파라미터**: `isPlayer` - 플레이어 턴인지 여부
**반환**: 없음
**사용 예**: 몬스터 파워에서 턴 종료 효과

**주의**: 플레이어 파워는 대부분 `atEndOfRound()` 사용

---

#### 17. atEndOfTurnPreEndTurnCards()
```java
public void atEndOfTurnPreEndTurnCards(boolean isPlayer)
```

**호출 시점**: 턴 종료 시 (Ethereal 카드 제거 **전**)
**파라미터**: 동일
**반환**: 없음
**사용 예**: 특수 턴 종료 효과

---

#### 18. atEndOfRound()
```java
public void atEndOfRound()
```

**호출 시점**: 라운드 종료 시 (플레이어 + 몬스터 턴 모두 끝)
**파라미터**: 없음
**반환**: 없음
**사용 예**:
- `WeakPower`, `VulnerablePower`: 지속 시간 감소
- 플레이어 파워의 턴 종료 처리

**차이점**:
- `atEndOfTurn()`: 각 턴마다 (플레이어턴, 적턴 따로)
- `atEndOfRound()`: 라운드 종료 시 (둘 다 끝난 후 1번)

---

### 카드 관련 훅 (7개)

#### 19. onPlayCard()
```java
public void onPlayCard(AbstractCard card, AbstractMonster m)
```

**호출 시점**: 카드 **사용 시작**
**파라미터**:
- `card`: 사용한 카드
- `m`: 대상 몬스터 (없으면 null)

**반환**: 없음
**사용 예**: 카드 사용 시 추가 효과

---

#### 20. onUseCard()
```java
public void onUseCard(AbstractCard card, UseCardAction action)
```

**호출 시점**: 카드 사용 **중**
**파라미터**:
- `card`: 사용한 카드
- `action`: 카드 사용 액션

**반환**: 없음
**사용 예**: 카드 사용 중 효과 (Echo 등)

---

#### 21. onAfterUseCard()
```java
public void onAfterUseCard(AbstractCard card, UseCardAction action)
```

**호출 시점**: 카드 사용 **완료 후**
**파라미터**: onUseCard와 동일
**반환**: 없음
**사용 예**:
- `TimeWarpPower`: 카드 카운터 증가
- `CuriosityPower`: 카드 사용 시 힘 증가

**중요**: 카드 효과가 모두 적용된 후 호출됨

---

#### 22. onCardDraw()
```java
public void onCardDraw(AbstractCard card)
```

**호출 시점**: 카드 **드로우 시**
**파라미터**: 드로운 카드
**반환**: 없음
**사용 예**: 카드 드로우 시 효과 (비용 감소 등)

---

#### 23. onExhaust()
```java
public void onExhaust(AbstractCard card)
```

**호출 시점**: 카드 **소진(Exhaust) 시**
**파라미터**: 소진된 카드
**반환**: 없음
**사용 예**: 소진 시 효과 (에너지 획득 등)

---

#### 24. onAfterCardPlayed()
```java
public void onAfterCardPlayed(AbstractCard usedCard)
```

**호출 시점**: 카드 사용 후 (onAfterUseCard와 유사)
**파라미터**: 사용한 카드
**반환**: 없음
**사용 예**: 카드 사용 후 추가 효과

---

#### 25. onDamageAllEnemies()
```java
public void onDamageAllEnemies(int[] damage)
```

**호출 시점**: 모든 적에게 데미지를 줄 때
**파라미터**: `damage` - 각 적에게 줄 데미지 배열
**반환**: 없음
**사용 예**: 전체 공격 시 추가 효과

---

### 체력/치유 관련 훅 (3개)

#### 26. onHeal()
```java
public int onHeal(int healAmount)
```

**호출 시점**: 체력 **회복 시**
**파라미터**: 회복량
**반환**: 수정된 회복량
**사용 예**: 회복량 증가/감소 효과

**주의**: `return healAmount;`로 그대로 반환해야 회복됨

---

#### 27. onLoseHp()
```java
public int onLoseHp(int damageAmount)
```

**호출 시점**: HP 손실 시 (DamageType.HP_LOSS)
**파라미터**: 손실량
**반환**: 수정된 손실량
**사용 예**: HP 손실 방지/증가 효과

---

#### 28. onDeath()
```java
public void onDeath()
```

**호출 시점**: 소유자 **사망 시**
**파라미터**: 없음
**반환**: 없음
**사용 예**: 사망 시 효과 (부활, 폭발 등)

---

### 오브 관련 훅 (디펙트 전용) (2개)

#### 29. onEvokeOrb()
```java
public void onEvokeOrb(AbstractOrb orb)
```

**호출 시점**: 오브 **발동(Evoke) 시**
**파라미터**: 발동한 오브
**반환**: 없음
**사용 예**: 오브 발동 시 추가 효과

---

#### 30. onChannel()
```java
public void onChannel(AbstractOrb orb)
```

**호출 시점**: 오브 **생성(Channel) 시**
**파라미터**: 생성한 오브
**반환**: 없음
**사용 예**: 오브 생성 시 효과

---

### 에너지 관련 훅 (2개)

#### 31. atEnergyGain()
```java
public void atEnergyGain()
```

**호출 시점**: 에너지 획득 시
**파라미터**: 없음
**반환**: 없음
**사용 예**: 에너지 획득 시 추가 효과

---

#### 32. onEnergyRecharge()
```java
public void onEnergyRecharge()
```

**호출 시점**: 에너지 충전 시 (턴 시작)
**파라미터**: 없음
**반환**: 없음
**사용 예**: 에너지 충전 시 효과

---

### 스탠스 관련 훅 (와쳐 전용) (1개)

#### 33. onChangeStance()
```java
public void onChangeStance(AbstractStance oldStance, AbstractStance newStance)
```

**호출 시점**: 스탠스 변경 시
**파라미터**:
- `oldStance`: 이전 스탠스
- `newStance`: 새 스탠스

**반환**: 없음
**사용 예**: 스탠스 변경 시 효과

---

### 기타 훅 (12개)

#### 34. onRemove()
```java
public void onRemove()
```

**호출 시점**: 파워 **제거 시**
**파라미터**: 없음
**반환**: 없음
**사용 예**: 파워 제거 시 효과 (정리 작업 등)

---

#### 35. onDrawOrDiscard()
```java
public void onDrawOrDiscard()
```

**호출 시점**: 카드 드로우 또는 버리기 시
**파라미터**: 없음
**반환**: 없음
**사용 예**: 덱 변화 시 효과

---

#### 36. onInitialApplication()
```java
public void onInitialApplication()
```

**호출 시점**: 파워 **최초 적용 시**
**파라미터**: 없음
**반환**: 없음
**사용 예**: 파워 부여 시 1회 효과

**주의**: `stackPower()`로 추가 시에는 호출 안 됨

---

#### 37. onApplyPower()
```java
public void onApplyPower(AbstractPower power, AbstractCreature target, AbstractCreature source)
```

**호출 시점**: 다른 파워 적용 시
**파라미터**:
- `power`: 적용되는 파워
- `target`: 대상
- `source`: 시전자

**반환**: 없음
**사용 예**: 파워 부여 시 추가 효과

---

#### 38. onVictory()
```java
public void onVictory()
```

**호출 시점**: 전투 **승리 시**
**파라미터**: 없음
**반환**: 없음
**사용 예**: 승리 시 정리 작업

---

#### 39. onScry()
```java
public void onScry()
```

**호출 시점**: Scry(카드 예견) 사용 시
**파라미터**: 없음
**반환**: 없음
**사용 예**: Scry 시 추가 효과

---

#### 40. onSpecificTrigger()
```java
public void onSpecificTrigger()
```

**호출 시점**: 특수 트리거 시 (몬스터별 커스텀)
**파라미터**: 없음
**반환**: 없음
**사용 예**: 특정 몬스터의 고유 효과

---

#### 41. onGainCharge()
```java
public void onGainCharge(int chargeAmount)
```

**호출 시점**: 차지 획득 시 (디펙트)
**파라미터**: 차지량
**반환**: 없음
**사용 예**: 차지 획득 시 효과

---

## 🔄 훅 실행 순서

### 데미지 계산 플로우

```
카드 사용
  ↓
[1] calculateCardDamage()
    카드 자체 데미지 계산
  ↓
[2] atDamageGive() - 공격자 파워들
    StrengthPower: +amount (가산)
    WeakPower: ×0.75 (곱셈)
    Priority 낮은 순서로 실행
  ↓
[3] atDamageFinalGive() - 공격자 파워들
    최종 조정
  ↓
[4] atDamageReceive() - 피격자 파워들
    VulnerablePower: ×1.5 (곱셈)
  ↓
[5] atDamageFinalReceive() - 피격자 파워들
    IntangiblePower: =1 (강제)
  ↓
[6] 데미지 적용
```

### 블록 계산 플로우

```
카드 사용 (Block 카드)
  ↓
[1] 카드 기본 블록 계산
  ↓
[2] modifyBlock() - Priority 낮은 순
    FrailPower (Priority 10): ×0.75 (곱셈 먼저)
    DexterityPower (Priority -1): +amount (가산 나중)
  ↓
[3] modifyBlockLast()
    최종 조정
  ↓
[4] 블록 적용
  ↓
[5] onGainedBlock(), onPlayerGainedBlock()
    블록 획득 후 효과
```

### 턴 관리 플로우

```
[플레이어 턴 시작]
  ↓
[1] atStartOfTurn() - 모든 파워
    PoisonPower: 독 데미지
  ↓
[2] 카드 드로우 (5장)
  ↓
[3] atStartOfTurnPostDraw()
    드로우 후 효과
  ↓
[플레이어 행동]
  ↓
[4] atEndOfTurn(true) - 몬스터 파워
  ↓
[적 턴 시작/행동/종료]
  ↓
[5] atEndOfTurn(false) - 플레이어 파워
  ↓
[6] atEndOfRound() - 모든 파워
    WeakPower: amount--
    VulnerablePower: amount--
```

### 카드 사용 플로우

```
카드 사용
  ↓
[1] onPlayCard(card, monster)
    카드 사용 시작
  ↓
[2] onUseCard(card, action)
    카드 사용 중
  ↓
[3] 카드 효과 실행
  ↓
[4] onAfterUseCard(card, action)
    카드 사용 완료
    TimeWarpPower: 카운터++
  ↓
[5] onAfterCardPlayed(card)
    추가 효과
```

---

## ⚙️ Priority 시스템

Priority 값이 **낮을수록** 먼저 실행됩니다.

### 표준 Priority 값

| Priority | 파워 | 의미 |
|----------|------|------|
| **-1** | DexterityPower | 가장 나중 (가산 효과) |
| **5** | 기본값 | 대부분의 파워 |
| **10** | FrailPower | 중간 |
| **50** | (드물게 사용) | - |
| **99** | WeakPower | 가장 먼저 (곱셈 효과) |

### 왜 중요한가?

**블록 계산 예시**:
```
기본 블록: 10

[Priority 10] FrailPower: 10 × 0.75 = 7.5
[Priority -1] Dexterity+3: 7.5 + 3 = 10.5

최종 블록: 10
```

만약 순서가 반대라면:
```
기본 블록: 10

[Dexterity+3]: 10 + 3 = 13
[FrailPower]: 13 × 0.75 = 9.75

최종 블록: 9 (다른 결과!)
```

**규칙**:
- **곱셈 효과**: Priority 높게 (먼저 실행)
- **가산 효과**: Priority 낮게 (나중 실행)

---

## 🎨 DamageInfo.DamageType

### 전체 타입 목록

| 타입 | 의미 | Strength 적용 | Weak 적용 | Vulnerable 적용 |
|------|------|--------------|----------|----------------|
| **NORMAL** | 일반 공격 | ✅ | ✅ | ✅ |
| **THORNS** | 가시 데미지 | ❌ | ❌ | ❌ |
| **HP_LOSS** | 직접 HP 손실 | ❌ | ❌ | ❌ |

### 사용 예시

```java
// NORMAL - 일반 공격
new DamageInfo(player, 10, DamageType.NORMAL);
// Strength +3 → 13
// Weak → 13 × 0.75 = 9

// THORNS - 가시 데미지
new DamageInfo(monster, 3, DamageType.THORNS);
// Strength, Weak 무시 → 3

// HP_LOSS - 직접 손실
new DamageInfo(null, 5, DamageType.HP_LOSS);
// 모든 파워 무시 → 5
```

---

## 🛠️ 기본 메서드

### stackPower()
```java
public void stackPower(int stackAmount)
```

**기능**: 파워 스택 추가
**파라미터**: 추가할 스택량
**사용 예**:
```java
// Poison 3이 이미 있는데 Poison 2 추가 적용
// → Poison 5가 됨
```

**기본 구현**:
```java
public void stackPower(int stackAmount) {
    this.fontScale = 8.0F;  // 시각 효과
    this.amount += stackAmount;
}
```

---

### reducePower()
```java
public void reducePower(int reduceAmount)
```

**기능**: 파워 스택 감소
**파라미터**: 감소할 스택량
**사용 예**:
```java
// Weak 3에서 1 감소
// → Weak 2
```

**기본 구현**:
```java
public void reducePower(int reduceAmount) {
    this.fontScale = 8.0F;
    this.amount -= reduceAmount;
    if (this.amount <= 0) {
        this.amount = 0;
    }
}
```

---

### updateDescription()
```java
public void updateDescription()
```

**기능**: 파워 설명 업데이트
**사용 예**:
```java
@Override
public void updateDescription() {
    if (this.amount == 1) {
        this.description = "1턴 동안 효과";
    } else {
        this.description = this.amount + "턴 동안 효과";
    }
}
```

---

### flash() / flashWithoutSound()
```java
public void flash()
public void flashWithoutSound()
```

**기능**: 파워 아이콘 깜빡임 효과
**사용 예**:
```java
@Override
public void atStartOfTurn() {
    flash();  // 파워 발동 시각 효과
    // 독 데미지 적용...
}
```

---

## 💡 실전 활용 예시

### 예시 1: Weak를 방어력 증가로 변경

```java
@SpirePatch(
    clz = WeakPower.class,
    method = SpirePatch.CLASS
)
public static class WeakFields {
    public static SpireField<Boolean> modified = new SpireField<>(() -> false);
}

// 기존 atDamageGive 비활성화
@SpirePatch(
    clz = WeakPower.class,
    method = "atDamageGive"
)
public static class DisableOriginalEffect {
    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(WeakPower __instance, float damage, DamageInfo.DamageType type) {
        if (WeakFields.modified.get(__instance)) {
            return SpireReturn.Return(damage);  // 원래 효과 무효화
        }
        return SpireReturn.Continue();
    }
}

// modifyBlock 훅 추가
@SpirePatch(
    clz = WeakPower.class,
    method = "modifyBlock"
)
public static class AddBlockModification {
    @SpirePrefixPatch
    public static SpireReturn<Float> Prefix(WeakPower __instance, float blockAmount) {
        if (WeakFields.modified.get(__instance)) {
            // 방어력 25% 증가
            return SpireReturn.Return(blockAmount * 1.25F);
        }
        return SpireReturn.Continue();
    }
}

// 생성자에서 플래그 설정
@SpirePatch(
    clz = WeakPower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class ConstructorPatch {
    @SpirePostfixPatch
    public static void Postfix(WeakPower __instance) {
        WeakFields.modified.set(__instance, true);
    }
}
```

**결과**: Weak가 이제 공격력 감소가 아닌 **방어력 증가** 효과로 동작합니다!

---

### 예시 2: 새로운 파워 - 턴 시작 시 카드 드로우

```java
public class DrawCardPower extends AbstractPower {
    public static final String POWER_ID = "MyMod:DrawCard";
    public static final String NAME = "카드 드로우";

    private int cardsToDraw;

    public DrawCardPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.cardsToDraw = amount;

        updateDescription();
        loadRegion("draw");  // 아이콘

        this.type = PowerType.BUFF;
        this.isTurnBased = false;  // 턴마다 감소하지 않음
    }

    @Override
    public void atStartOfTurnPostDraw() {
        // 카드 드로우 후에 추가 드로우
        flash();
        addToBot(new DrawCardAction(this.owner, this.cardsToDraw));
    }

    @Override
    public void updateDescription() {
        this.description = "턴 시작 시 카드를 " + this.cardsToDraw + "장 드로우합니다.";
    }
}
```

---

### 예시 3: 공격 시 독 부여

```java
public class VenomPower extends AbstractPower {
    public static final String POWER_ID = "MyMod:Venom";
    public static final String NAME = "맹독";

    private int poisonAmount;

    public VenomPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.poisonAmount = amount;

        updateDescription();
        loadRegion("poison");

        this.type = PowerType.BUFF;
    }

    @Override
    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
        // 데미지를 줬을 때만
        if (damageAmount > 0 && target != this.owner) {
            flash();
            // 대상에게 독 부여
            addToBot(new ApplyPowerAction(
                target,
                this.owner,
                new PoisonPower(target, this.owner, this.poisonAmount),
                this.poisonAmount
            ));
        }
    }

    @Override
    public void updateDescription() {
        this.description = "공격 시 적에게 독 " + this.poisonAmount + "을 부여합니다.";
    }
}
```

---

## ⚠️ 주의사항

### 1. 반환값 잊지 말기

**잘못된 예**:
```java
@Override
public float atDamageGive(float damage, DamageInfo.DamageType type) {
    // 반환 없음 → 컴파일 에러!
}
```

**올바른 예**:
```java
@Override
public float atDamageGive(float damage, DamageInfo.DamageType type) {
    return damage * 2.0F;  // 반드시 반환
}
```

### 2. DamageType 체크

```java
@Override
public float atDamageGive(float damage, DamageInfo.DamageType type) {
    if (type == DamageInfo.DamageType.NORMAL) {
        return damage + this.amount;
    }
    return damage;  // NORMAL이 아니면 그대로
}
```

### 3. Priority 설정

```java
public MyPower(AbstractCreature owner, int amount) {
    // ...
    this.priority = 99;  // 곱셈 효과는 높은 Priority
    // this.priority = -1;  // 가산 효과는 낮은 Priority
}
```

### 4. addToBot() vs addToTop()

```java
// 일반적으로 addToBot 사용
addToBot(new ApplyPowerAction(...));

// 즉시 실행 필요 시 addToTop
addToTop(new RemoveSpecificPowerAction(...));
```

---

## 📚 관련 문서

- **Weak.md** - atDamageGive 사용 예시
- **Vulnerable.md** - atDamageReceive 사용 예시
- **Frail.md** - modifyBlock 사용 예시
- **Poison.md** - atStartOfTurn 사용 예시
- **Intangible.md** - atDamageFinalReceive 사용 예시
- **Strength.md** - stackPower, reducePower 사용 예시

---

## 🎯 체크리스트

파워를 만들거나 수정할 때 확인하세요:

- [ ] 어떤 훅 메서드를 사용할 것인가?
- [ ] 반환 타입이 맞는가?
- [ ] DamageType 체크를 했는가? (데미지 훅)
- [ ] Priority를 올바르게 설정했는가?
- [ ] updateDescription()을 구현했는가?
- [ ] 적절한 PowerType (BUFF/DEBUFF)인가?
- [ ] stackPower() 동작을 고려했는가?
- [ ] 턴 감소가 필요하면 isTurnBased = true인가?

---

이 문서로 **모든 파워를 완전히 다른 기능으로 변경**할 수 있습니다! 🎉
