# 뱀 식물 (Snake Plant)

## 기본 정보

**클래스명**: `SnakePlant`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.SnakePlant`
**ID**: `"SnakePlant"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 63-67 |
| A7+ | 66-70 |

**특징**: 중간 HP, Malleable (방어 증가) 능력

---

## 패턴 정보

### 패턴 1: 가시 공격 (Chomp)

**의도**: `ATTACK`
**데미지**: 7 x 3회

**발동 확률**: 50%

**효과**:
- 플레이어에게 **7 데미지 x 3회** 공격
- 총 21 데미지
- Frail 상태라면 더 치명적

**코드 특징**:
```java
// 3타 연속 공격
multiDamage = 7
attackCount = 3
```

**수정 포인트**:
- 타수 변경: 3 → 4
- 데미지 변경: 7 → 조정값

---

## 특수 파워

### Malleable (유연성)

**효과**: 피격 시 Block 획득

**Block 획득량**:
| 난이도 | Block/Hit |
|--------|-----------|
| 기본 (A0-A16) | 3 |
| A17+ | 4 |

**메커니즘**:
```java
@Override
public int onAttacked(DamageInfo info, int damageAmount) {
    if (info.type == DamageType.NORMAL && damageAmount > 0) {
        int blockAmount = (AbstractDungeon.ascensionLevel >= 17) ? 4 : 3;
        addToBot(new GainBlockAction(this, this, blockAmount));
    }
    return damageAmount;
}
```

**특징**:
- **모든 피격마다** Block 획득
- 다중 공격에 특히 강력 (타수당 트리거)
- NORMAL 타입 데미지만 대상
- 0 데미지 공격은 트리거 안 함

**수정 포인트**:
- Block 수치 변경
- 발동 조건 변경 (데미지 임계값 등)

---

## AI 로직 (getMove)

**패턴 선택**:
```java
// 단일 패턴만 사용
setMove(ATTACK, 7, 3);  // Chomp
```

**로직 설명**:
- **매 턴 Chomp만 사용**
- 패턴 변화 없음
- 단순 AI

**수정 포인트**:
- 패턴 추가 (방어, 버프 등)
- 조건부 패턴 (HP 기반 등)

---

## 특수 동작

### Malleable 파워 상세

**트리거 조건**:
- NORMAL 타입 데미지
- 실제 데미지 >0
- 타격당 1회 트리거

**다중 공격 시**:
```java
// 예: 플레이어가 3타 공격
3타 공격 → Block 9 (A17: 12) 획득
```

**전략적 의미**:
- 다중 공격 카드 불리 (Pummel, Sword Boomerang 등)
- 단타 고데미지 유리 (Bludgeon, Heavy Blade 등)
- 장기전 유리 (지속적 방어 증가)

### 약점

**취약점**:
- 단타 고데미지 공격 (Malleable 최소화)
- Power 카드 (데미지 없음)
- Poison, Burning (비공격 데미지)

**대응 전략**:
- Heavy Blade, Bludgeon 등 단타 고데미지
- Catalyst + Poison (Malleable 무시)
- Demon Form + 단타 공격

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **단타 공격**: Malleable 트리거 최소화
2. **Poison/Burn**: 비공격 데미지로 우회
3. **Piercing**: Malleable 무시 관통 데미지

**위험 요소**:
- 다중 공격 → Malleable 다중 트리거
- 장기전 → Block 누적
- Chomp 21 데미지 → 높은 공격력

**카운터 전략**:
- **단타 고데미지**: Heavy Blade (14-20), Bludgeon (32)
- **Poison**: Catalyst + Bouncing Flask
- **Piercing**: Shockwave, Corpse Explosion

**피해야 할 카드**:
- Pummel (5타): Block 15 획득
- Sword Boomerang (3타): Block 9 획득
- Blade Dance (4타): Block 12 획득

---

## 수정 예시

### 1. Malleable Block 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.powers.MalleablePower",
    method = "onAttacked"
)
public static class MalleableBlockPatch {
    @SpirePrefixPatch
    public static void Prefix(MalleablePower __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Block 3 → 5 (A17: 4 → 6)
            // amount 필드 수정
        }
    }
}
```

### 2. Chomp 타수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SnakePlant",
    method = SpirePatch.CONSTRUCTOR
)
public static class SnakePlantChompPatch {
    @SpirePostfixPatch
    public static void Postfix(SnakePlant __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Chomp 3타 → 4타
            // multiAttack 필드 수정
        }
    }
}
```

### 3. 패턴 추가: 재생 (Regenerate)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SnakePlant",
    method = "getMove"
)
public static class SnakePlantRegeneratePatch {
    @SpirePrefixPatch
    public static void Prefix(SnakePlant __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // HP 50% 이하 시 재생 패턴 추가
            if (__instance.currentHealth <= __instance.maxHealth * 0.5f) {
                // Regenerate: 자신에게 Regen 5 부여
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `chompCount` | int | Chomp 타수 (기본 3) |
| `chompDamage` | int | Chomp 1타 데미지 (7) |
| `malleableAmount` | int | Malleable Block 수치 (3 또는 4) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/SnakePlant.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.MalleablePower`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`

---

## 관련 파워 상세: MalleablePower

**클래스**: `com.megacrit.cardcrawl.powers.MalleablePower`

**효과**:
```java
public int onAttacked(DamageInfo info, int damageAmount) {
    if (info.type != DamageType.THORNS &&
        info.type != DamageType.HP_LOSS &&
        damageAmount > 0) {
        flash();
        addToTop(new GainBlockAction(owner, owner, amount));
    }
    return damageAmount;
}
```

**트리거 제외**:
- Thorns 데미지
- HP Loss (직접 체력 감소)
- 0 데미지 공격

---

## 참고사항

1. **Malleable 메커니즘**: 타격당 Block 획득, 다중 공격에 강력
2. **단일 패턴**: Chomp만 반복 사용
3. **다중 공격 불리**: Pummel, Sword Boomerang 등 비효율적
4. **단타 공격 유리**: Heavy Blade, Bludgeon 등 효과적
5. **Poison 우회**: Catalyst + Poison으로 Malleable 무시 가능
6. **A17+ 강화**: Malleable Block 3 → 4로 생존력 증가
