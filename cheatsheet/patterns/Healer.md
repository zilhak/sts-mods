# 신비주의자 (Mystic / Healer)

## 기본 정보

**클래스명**: `Healer` (내부명: Mystic)
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Healer`
**ID**: `"Healer"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 48-52 |
| A7+ | 50-54 |

**특징**: 낮은 HP, 아군 힐링 특화 지원형 적

---

## 패턴 정보

### 패턴 1: 치유 (Heal)

**의도**: `BUFF`
**효과**: 랜덤 아군 몬스터 HP 회복

**힐량**:
| 난이도 | 회복량 |
|--------|--------|
| 기본 (A0-A16) | 16 |
| A17+ | 20 |

**발동 확률**: 60%

**효과**:
- 랜덤 아군 몬스터 1마리 **16 HP** 회복 (A17+: 20)
- 자신도 대상에 포함
- **중요**: 죽은 몬스터는 대상 제외

**코드 특징**:
```java
// 생존한 몬스터 중 랜덤 선택
ArrayList<AbstractMonster> aliveMonsters = new ArrayList<>();
for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters) {
    if (!m.isDead && !m.isDying) {
        aliveMonsters.add(m);
    }
}

AbstractMonster target = aliveMonsters.get(random(aliveMonsters.size()));
int healAmount = (AbstractDungeon.ascensionLevel >= 17) ? 20 : 16;
HealAction(target, this, healAmount);
```

**수정 포인트**:
- 회복량 변경
- 대상 선택 로직 (최저 HP 우선 등)
- 발동 확률 조정

---

### 패턴 2: 수비 (Defend)

**의도**: `DEFEND`
**효과**: 자신에게 Block 8 (A17: 11)

**발동 확률**: 25%

**효과**:
- 자신에게 **Block 8** 부여 (A17+: 11)
- 생존력 증가

**코드 특징**:
```java
int blockAmount = (AbstractDungeon.ascensionLevel >= 17) ? 11 : 8;
GainBlockAction(this, this, blockAmount);
```

**수정 포인트**:
- Block 수치 변경
- A17 임계값 조정

---

### 패턴 3: 침묵 (Incantation)

**의도**: `ATTACK`
**데미지**: 8

**발동 확률**: 15%

**효과**:
- 플레이어에게 **8 데미지** 공격
- 약한 공격력

**코드 특징**:
```java
// 기본 공격
baseDamage = 8
```

**수정 포인트**:
- 데미지 변경: 8 → 조정값
- 추가 효과 (디버프 등)

---

## AI 로직 (getMove)

**패턴 선택**:
```java
// 아군이 있고 회복이 필요한 경우
if (hasAllies() && shouldHeal()) {
    int roll = random(100);
    if (roll < 60) {
        // Heal (60%)
    } else if (roll < 85) {
        // Defend (25%)
    } else {
        // Incantation (15%)
    }
} else {
    // 혼자거나 회복 불필요 시
    int roll = random(100);
    if (roll < 70) {
        // Defend (70%)
    } else {
        // Incantation (30%)
    }
}
```

**회복 필요 판단**:
```java
boolean shouldHeal() {
    for (AbstractMonster m : monsters) {
        if (!m.isDead && m.currentHp < m.maxHp) {
            return true;
        }
    }
    return false;
}
```

**로직 설명**:
1. 아군 있고 회복 필요: Heal 60% / Defend 25% / Incantation 15%
2. 아군 없거나 회복 불필요: Defend 70% / Incantation 30%
3. 아군 생존 시 Heal 우선

**수정 포인트**:
- 회복 우선순위 조정
- Incantation 확률 증가
- 조건부 패턴 추가

---

## 특수 동작

### 아군 힐링 메커니즘

**대상 선택**:
- 생존한 아군 중 랜덤
- 자신도 대상 포함
- 죽은 몬스터는 제외

**전략적 의미**:
- **최우선 처치 대상**
- 방치 시 장기전 불리
- 아군 생존력 크게 증가

### 멀티 인카운터 (Centurion and Healer)

**조합 정보**:
- Centurion + Healer 동시 등장
- Healer가 Centurion 회복
- **우선순위**: Healer 먼저 처치 필수

**시너지 효과**:
- Centurion의 높은 방어 + Healer 회복
- 장기전 시 Centurion 처치 불가능
- 난이도 급상승

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **최우선 처치**: Healer 먼저 제거
2. **AOE 공격**: Whirlwind, Immolate 등으로 동시 타격
3. **빠른 처치**: 낮은 HP (48-54) 이용

**위험 요소**:
- Healer 방치 → 아군 무한 회복
- Centurion + Healer → 처치 불가능
- 장기전 → 체력 소모전 패배

**카운터 전략**:
- **우선순위 타격**: Healer 집중 공격
- **AOE**: 동시 타격으로 효율 증가
- **Poison**: 회복 무시 지속 데미지

---

## 수정 예시

### 1. 회복량 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Healer",
    method = "takeTurn"
)
public static class HealerHealAmountPatch {
    @SpirePrefixPatch
    public static void Prefix(Healer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 회복량 16 → 25 (A17: 20 → 30)
            // healAmount 필드 수정
        }
    }
}
```

### 2. 회복 대상 확대

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Healer",
    method = "takeTurn"
)
public static class HealerMultiHealPatch {
    @SpirePostfixPatch
    public static void Postfix(Healer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 모든 아군 회복 (랜덤 1마리 → 전체)
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters) {
                if (!m.isDead && !m.isDying) {
                    AbstractDungeon.actionManager.addToBottom(
                        new HealAction(m, __instance, 10)
                    );
                }
            }
        }
    }
}
```

### 3. Incantation 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Healer",
    method = "takeTurn"
)
public static class HealerIncantationPatch {
    @SpirePostfixPatch
    public static void Postfix(Healer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Incantation 후 Weak 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player,
                    __instance,
                    new WeakPower(AbstractDungeon.player, 1, true)
                )
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `healAmount` | int | 회복량 (16 또는 20) |
| `blockAmount` | int | Block 수치 (8 또는 11) |
| `attackDamage` | int | Incantation 데미지 (8) |
| `lastMove` | byte | 이전 사용 패턴 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Healer.java`
- **액션**:
  - `HealAction`
  - `GainBlockAction`
  - `DamageAction`

---

## 참고사항

1. **최우선 처치 대상**: 멀티 인카운터 시 반드시 먼저 제거
2. **회복 메커니즘**: 랜덤 아군 회복, 자신 포함
3. **낮은 공격력**: Incantation 8 데미지로 위협도 낮음
4. **지원형 적**: 아군 생존력 크게 증가
5. **Centurion 조합**: 가장 위험한 조합 중 하나
6. **AOE 유리**: Whirlwind, Immolate 등으로 동시 타격 효율적
