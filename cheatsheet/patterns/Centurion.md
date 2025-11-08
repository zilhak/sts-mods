# 백부장 (Centurion)

## 기본 정보

**클래스명**: `Centurion`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Centurion`
**ID**: `"Centurion"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 76-80 |
| A7+ | 80-84 |

**특징**: 중간 HP, 방어 중심 적

---

## 패턴 정보

### 패턴 1: 일격 (Slash)

**의도**: `ATTACK`
**데미지**: 12

**발동 확률**: 60%

**효과**:
- 플레이어에게 **12 데미지** 공격

**코드 특징**:
```java
// 기본 공격
baseDamage = 12
```

**수정 포인트**:
- 데미지 변경: 12 → 조정값

---

### 패턴 2: 방어 태세 (Defend)

**의도**: `DEFEND`
**효과**: 자신에게 Block 15 (A17: 20)

**발동 확률**: 40%

**효과**:
- 자신에게 **Block 15** 부여 (A17+: 20)
- 다음 공격 피해 감소

**코드 특징**:
```java
// Block 획득
int blockAmount = (AbstractDungeon.ascensionLevel >= 17) ? 20 : 15;
GainBlockAction(this, this, blockAmount);
```

**수정 포인트**:
- Block 수치 변경
- A17 임계값 조정

---

### 패턴 3: 격노 (Fury)

**의도**: `BUFF`
**효과**: 자신에게 Strength 2 (A17: 3)

**발동 조건**: 첫 턴에만 사용 (40% 확률)

**효과**:
- 자신에게 **Strength 2** 부여 (A17+: 3)
- 이후 모든 공격 데미지 증가

**코드 특징**:
```java
// 첫 턴에만
if (firstMove) {
    int roll = random(100);
    if (roll < 40) {
        int strAmount = (AbstractDungeon.ascensionLevel >= 17) ? 3 : 2;
        ApplyPowerAction(this, this, new StrengthPower(this, strAmount));
    }
}
```

**수정 포인트**:
- Strength 수치 변경
- 발동 확률 조정 (40%)
- 발동 조건 변경

---

## AI 로직 (getMove)

**첫 턴**:
```java
int roll = random(100);
if (roll < 40) {
    // Fury (40%)
    setMove(BUFF);
} else if (roll < 70) {
    // Slash (30%)
    setMove(ATTACK, 12);
} else {
    // Defend (30%)
    setMove(DEFEND);
}
```

**이후 턴**:
```java
// Fury는 첫 턴 이후 사용 안 함
int roll = random(100);
if (roll < 60) {
    // Slash (60%)
    setMove(ATTACK, 12);
} else {
    // Defend (40%)
    setMove(DEFEND);
}
```

**로직 설명**:
1. 첫 턴: Fury 40% / Slash 30% / Defend 30%
2. 이후: Slash 60% / Defend 40%
3. Fury는 첫 턴에만 사용 가능

**특수 규칙**:
- 같은 패턴 연속 사용 방지
- Defend 후 Slash 확률 증가

**수정 포인트**:
- 확률 조정
- Fury 사용 조건 변경 (반복 사용 등)
- 패턴 추가

---

## 특수 동작

### 방어 전문가

**특징**:
- 40% 확률로 방어 태세
- Block 15-20 획득
- 장기전 유도

**전략적 의미**:
- 빠른 처치가 어려움
- 데미지 효율 저하
- Strength 버프와 시너지

### Strength 버프

**버프 적용 시**:
- Slash: 12 → 14 (A17: 15)
- 영구 지속
- 스택 가능

**위험도**:
- 첫 턴 버프 성공 시 장기전 불리
- Defend와 조합 시 높은 생존력

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **방어 무시**: Defend 사용 턴에도 지속 데미지
2. **버프 차단**: Disarm, Flex Potion 등으로 Strength 제거
3. **방어 관통**: Heavy Blade, Perfected Strike 등 고데미지 공격

**위험 요소**:
- Strength 버프 + Defend → 높은 생존력
- Slash 12 데미지 → 방어 필요
- 장기전 → 피로 누적

**카운터 전략**:
- **Power Through**: 방어 무시하고 속공
- **Weak**: 공격력 감소
- **Disarm**: Strength 제거
- **Heavy Blade**: 방어 관통

---

## 멀티 인카운터 (Centurion and Healer)

### 특수 조합

**조합 정보**:
- Centurion + Mystic 동시 등장
- Mystic이 Centurion 회복

**전략 변화**:
- **우선순위**: Mystic 먼저 처치
- **이유**: Centurion 회복 차단
- **난이도**: 크게 상승

---

## 수정 예시

### 1. Fury 버프 강화 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
    method = "takeTurn"
)
public static class CenturionFuryPatch {
    @SpirePrefixPatch
    public static void Prefix(Centurion __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Strength 2 → 4 (A17: 3 → 5)
            // buffAmount 필드 수정
        }
    }
}
```

### 2. Defend Block 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
    method = "takeTurn"
)
public static class CenturionDefendPatch {
    @SpirePostfixPatch
    public static void Postfix(Centurion __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Block 15 → 20 (A17: 20 → 25)
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(__instance, __instance, 5)
            );
        }
    }
}
```

### 3. Fury 반복 사용 허용

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Centurion",
    method = "getMove"
)
public static class CenturionFuryRepeatPatch {
    @SpirePrefixPatch
    public static void Prefix(Centurion __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // firstMove 체크 제거
            // Fury를 매 3턴마다 사용 가능하게 변경
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `buffAmount` | int | Strength 수치 (2 또는 3) |
| `blockAmount` | int | Block 수치 (15 또는 20) |
| `lastMove` | byte | 이전 사용 패턴 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Centurion.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.StrengthPower`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`

---

## 참고사항

1. **방어 전문가**: Block 획득 빈도가 높아 장기전 유리
2. **Strength 버프**: 첫 턴에만 사용, 영구 지속
3. **Mystic 조합**: Healer와 함께 등장 시 우선순위 변경
4. **패턴 반복 방지**: 같은 패턴 연속 사용 안 함
5. **속공 유리**: Strength 버프 전 처치 추천
