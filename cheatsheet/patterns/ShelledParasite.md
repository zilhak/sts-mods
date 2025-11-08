# 껍질 기생충 (Shelled Parasite)

## 기본 정보

**클래스명**: `ShelledParasite`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.ShelledParasite`
**ID**: `"ShelledParasite"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 68-72 |
| A7+ | 70-74 |

**특징**: 중간 HP, Plated Armor (갑옷) 능력

---

## 고유 메커니즘

### Plated Armor (판금 갑옷)

**시작 Plated Armor**:
| 난이도 | Armor |
|--------|-------|
| 기본 (A0-A16) | 14 |
| A17+ | 18 |

**효과**:
- 피격 시 Armor 먼저 감소
- Armor > 0: 데미지 차단
- Armor = 0: 일반 데미지
- 턴 종료 시 Armor 1 감소

**코드 특징**:
```java
// 생성자에서 Plated Armor 부여
int armorAmount = (AbstractDungeon.ascensionLevel >= 17) ? 18 : 14;
addToBot(new ApplyPowerAction(this, this,
    new PlatedArmorPower(this, armorAmount)));
```

**메커니즘**:
```java
@Override
public int onAttacked(DamageInfo info, int damageAmount) {
    if (info.type == DamageType.NORMAL && damageAmount > 0) {
        if (this.amount > 0) {
            // Armor 감소
            this.amount -= 1;
            damageAmount = 0;  // 데미지 차단
        }
    }
    return damageAmount;
}

@Override
public void atEndOfTurn(boolean isPlayer) {
    if (!isPlayer && this.amount > 0) {
        // 매 턴 Armor 1 감소
        reducePower(1);
    }
}
```

**특징**:
- **타격당 Armor 1 감소** (데미지 양 무관)
- 다중 공격에 취약 (타수당 Armor 감소)
- 턴 종료 시 추가 1 감소

---

## 패턴 정보

### 패턴 1: 이중 공격 (Double Strike)

**의도**: `ATTACK`
**데미지**: 6 x 2회

**발동 확률**: 60%

**효과**:
- 플레이어에게 **6 데미지 x 2회** 공격
- 총 12 데미지

**코드 특징**:
```java
// 2타 연속 공격
multiDamage = 6
attackCount = 2
```

**수정 포인트**:
- 타수 변경: 2 → 3
- 데미지 변경: 6 → 조정값

---

### 패턴 2: 쪼아먹기 (Fell)

**의도**: `ATTACK`
**데미지**: 18

**발동 확률**: 40%

**효과**:
- 플레이어에게 **18 데미지** 단타 공격

**코드 특징**:
```java
// 단순 단타 공격
baseDamage = 18
```

**수정 포인트**:
- 데미지 변경: 18 → 조정값

---

## AI 로직 (getMove)

**패턴 선택**:
```java
int roll = random(100);
if (roll < 60) {
    // Double Strike (60%)
    setMove(ATTACK, 6, 2);
} else {
    // Fell (40%)
    setMove(ATTACK, 18);
}
```

**로직 설명**:
1. Double Strike 60% / Fell 40%
2. 패턴 랜덤 선택
3. 같은 패턴 연속 사용 방지

**수정 포인트**:
- 확률 조정
- 패턴 추가 (방어, 버프 등)

---

## 특수 동작

### Plated Armor 상세

**피격 메커니즘**:
```java
// 예시: 플레이어가 20 데미지 공격
Armor 14 → 13 (데미지 0)
Armor 13 → 12 (데미지 0)
...
Armor 1 → 0 (데미지 0)
Armor 0 → 일반 데미지
```

**다중 공격 효율**:
- **단타**: Armor 1 감소 per 공격
- **다중**: Armor 타수만큼 감소
- **예시**: Pummel (5타) → Armor 5 감소

**턴 종료 감소**:
- 플레이어 턴 종료 시 Armor 1 감소
- 0 이하로 감소 안 함

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **다중 공격**: Armor 빠르게 제거
2. **지속 데미지**: Poison으로 Armor 우회
3. **Piercing**: Armor 무시 관통 데미지

**위험 요소**:
- 높은 Armor (14-18) → 초반 데미지 차단
- Double Strike → 지속 데미지
- Fell 18 → 높은 단타 공격

**카운터 전략**:
- **다중 공격**: Pummel, Sword Boomerang, Blade Dance
- **Poison**: Catalyst로 Armor 우회
- **Heavy Blade**: Armor 제거 후 고데미지
- **Shockwave**: Vulnerable로 데미지 증가

**효과적인 카드**:
- Pummel (5타): Armor 5 감소
- Sword Boomerang (3타): Armor 3 감소
- Blade Dance (4타): Armor 4 감소
- Bouncing Flask: Poison으로 우회

**피해야 할 카드**:
- 단타 공격 (초반): Armor에 막힘
- Bludgeon, Heavy Blade (초반): Armor 제거 후 사용

---

## 수정 예시

### 1. Plated Armor 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.ShelledParasite",
    method = SpirePatch.CONSTRUCTOR
)
public static class ShelledParasiteArmorPatch {
    @SpirePostfixPatch
    public static void Postfix(ShelledParasite __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Armor 14 → 20 (A17: 18 → 24)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new PlatedArmorPower(__instance, 6))
            );
        }
    }
}
```

### 2. Double Strike 타수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.ShelledParasite",
    method = SpirePatch.CONSTRUCTOR
)
public static class ShelledParasiteDoublePatch {
    @SpirePostfixPatch
    public static void Postfix(ShelledParasite __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Double Strike 2타 → 3타
            // multiAttack 필드 수정
        }
    }
}
```

### 3. Armor 재생 패턴 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.ShelledParasite",
    method = "getMove"
)
public static class ShelledParasiteRegeneratePatch {
    @SpirePrefixPatch
    public static void Prefix(ShelledParasite __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Armor 0이면 재생 패턴 추가
            PlatedArmorPower armor = (PlatedArmorPower)
                __instance.getPower(PlatedArmorPower.POWER_ID);
            if (armor == null || armor.amount == 0) {
                // Plated Armor 5 재부여
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `armorAmount` | int | Plated Armor 수치 (14 또는 18) |
| `doubleStrikeDamage` | int | Double Strike 데미지 (6) |
| `fellDamage` | int | Fell 데미지 (18) |
| `lastMove` | byte | 이전 사용 패턴 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/ShelledParasite.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.PlatedArmorPower`
- **액션**:
  - `DamageAction`
  - `ApplyPowerAction`

---

## 관련 파워 상세: PlatedArmorPower

**클래스**: `com.megacrit.cardcrawl.powers.PlatedArmorPower`

**효과**:
```java
public int onAttacked(DamageInfo info, int damageAmount) {
    if (info.type != DamageType.THORNS &&
        info.type != DamageType.HP_LOSS &&
        damageAmount > 0 &&
        this.amount > 0) {
        flash();
        reducePower(1);  // Armor 1 감소
        return 0;        // 데미지 차단
    }
    return damageAmount;
}

public void atEndOfTurn(boolean isPlayer) {
    if (!isPlayer && this.amount > 0) {
        reducePower(1);  // 매 턴 Armor 1 감소
    }
}
```

**특징**:
- 타격당 Armor 1 감소 (데미지 양 무관)
- 턴 종료 시 추가 1 감소
- 0 이하로 감소 안 함

---

## 참고사항

1. **Plated Armor 메커니즘**: 타격당 1 감소, 데미지 양 무관
2. **다중 공격 유리**: Pummel, Sword Boomerang 등 효과적
3. **Poison 우회**: Catalyst로 Armor 무시 가능
4. **턴 종료 감소**: 매 턴 Armor 자동 1 감소
5. **A17+ 강화**: Armor 14 → 18로 생존력 증가
6. **초반 방해**: 높은 Armor로 초반 공격 차단
