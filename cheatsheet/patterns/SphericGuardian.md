# 구형 수호자 (Spheric Guardian)

## 기본 정보

**클래스명**: `SphericGuardian`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.SphericGuardian`
**ID**: `"SphericGuardian"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 20-24 |
| A7+ | 22-25 |

**특징**: 매우 낮은 HP, 높은 방어막, 반격 능력

---

## 고유 메커니즘

### Artifact (아티팩트)

**시작 Artifact**:
| 난이도 | Artifact |
|--------|----------|
| 기본 (A0-A16) | 1 |
| A17+ | 2 |

**효과**:
- 다음 디버프 1회 무효화
- Vulnerable, Weak, Poison 등 차단
- Artifact 소모 시 재충전 안 됨

**코드 특징**:
```java
// 생성자에서 Artifact 부여
int artifactAmount = (AbstractDungeon.ascensionLevel >= 17) ? 2 : 1;
addToBot(new ApplyPowerAction(this, this, new ArtifactPower(this, artifactAmount)));
```

---

## 패턴 정보

### 패턴 1: 공격 (Slam)

**의도**: `ATTACK_DEFEND`
**데미지**: 10
**Block**: 9 (A17: 12)

**발동 확률**: 50%

**효과**:
- 플레이어에게 **10 데미지** 공격
- 자신에게 **Block 9** 부여 (A17+: 12)

**코드 특징**:
```java
// 공격 + 방어 동시 수행
DamageAction(player, damage, AttackEffect.BLUNT_HEAVY);
int blockAmount = (AbstractDungeon.ascensionLevel >= 17) ? 12 : 9;
GainBlockAction(this, this, blockAmount);
```

**수정 포인트**:
- 데미지 변경: 10 → 조정값
- Block 변경: 9/12 → 조정값

---

### 패턴 2: 방어 태세 (Harden)

**의도**: `DEFEND_BUFF`
**Block**: 15 (A17: 20)
**Barricade**: 영구 Block

**발동 확률**: 50%

**효과**:
- 자신에게 **Block 15** 부여 (A17+: 20)
- **Barricade** 파워 부여 (Block이 턴 종료 시 사라지지 않음)

**코드 특징**:
```java
int blockAmount = (AbstractDungeon.ascensionLevel >= 17) ? 20 : 15;
GainBlockAction(this, this, blockAmount);
ApplyPowerAction(this, this, new BarricadePower(this));
```

**중요**: Barricade 파워로 Block 영구 누적

**수정 포인트**:
- Block 수치 변경
- Barricade 발동 조건 추가

---

### 패턴 3: 가시 반격 (Activate)

**의도**: `BUFF`
**효과**: Thorns 3 (A17: 4)

**발동 조건**: HP 50% 이하에서 1회만 사용

**효과**:
- 자신에게 **Thorns 3** 부여 (A17+: 4)
- 피격 시 공격자에게 Thorns 수치만큼 데미지 반사
- **전투 중 1회만 사용 가능**

**코드 특징**:
```java
// HP 체크
if (currentHp <= maxHp * 0.5f && !usedActivate) {
    int thornsAmount = (AbstractDungeon.ascensionLevel >= 17) ? 4 : 3;
    ApplyPowerAction(this, this, new ThornsPower(this, thornsAmount));
    usedActivate = true;
}
```

**수정 포인트**:
- Thorns 수치 변경
- 발동 조건 변경 (HP 임계값)
- 사용 횟수 제한 변경

---

## AI 로직 (getMove)

**패턴 선택**:
```java
// HP 50% 이하면 Activate 사용 (1회만)
if (hp <= maxHp * 0.5 && !usedActivate) {
    setMove(BUFF);
} else {
    int roll = random(100);
    if (roll < 50) {
        // Slam (50%)
        setMove(ATTACK_DEFEND, 10);
    } else {
        // Harden (50%)
        setMove(DEFEND_BUFF);
    }
}
```

**로직 설명**:
1. HP 50% 이하: Activate (1회)
2. 일반: Slam 50% / Harden 50%
3. Harden으로 Block 누적

**특수 규칙**:
- 같은 패턴 연속 사용 방지
- Barricade로 Block 영구 보존

**수정 포인트**:
- 확률 조정
- HP 임계값 변경
- Activate 반복 사용 허용

---

## 특수 동작

### Artifact 메커니즘

**디버프 차단**:
- Weak, Vulnerable, Poison 등 모든 디버프
- Artifact 1당 디버프 1회 무효화
- A17+: Artifact 2로 디버프 2회 차단

**전략적 의미**:
- 초반 디버프 카드 무효화
- Poison 덱에 불리
- Weak/Vulnerable 전략 무력화

### Barricade 메커니즘

**Block 누적**:
```java
// 턴 종료 시 Block 유지
@Override
public void atEndOfTurn() {
    // Block 제거 안 함 (Barricade 효과)
}
```

**누적 효과**:
- Harden 사용마다 Block +15/20
- Slam 사용마다 Block +9/12
- 장기전 시 처치 불가능 수준으로 누적

### Thorns 반격

**피격 반사**:
- 공격 시 Thorns 수치만큼 반사
- 다중 공격 시 타수당 반사
- 플레이어에게 큰 피해

**예시**:
```java
// Thorns 4, 플레이어가 5타 공격
5타 공격 → 20 데미지 반사
```

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **속공**: 낮은 HP (20-25) 이용해 빠른 처치
2. **Artifact 소모**: 디버프로 Artifact 먼저 제거
3. **Thorns 대비**: HP 50% 근처에서 큰 데미지로 일격 처치

**위험 요소**:
- Barricade → Block 무한 누적
- Thorns → 다중 공격 시 자해 피해
- Artifact → 디버프 무효화
- 장기전 → 처치 불가능

**카운터 전략**:
- **속공**: 2-3턴 내 처치 목표
- **단타 고데미지**: Thorns 피해 최소화
- **Piercing**: Corpse Explosion, Dagger Throw

**피해야 할 행동**:
- 장기전 → Block 누적으로 처치 불가능
- 다중 공격 (Thorns 후) → 자해 피해 급증
- 디버프 의존 → Artifact로 차단

---

## 수정 예시

### 1. Artifact 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SphericGuardian",
    method = SpirePatch.CONSTRUCTOR
)
public static class SphericGuardianArtifactPatch {
    @SpirePostfixPatch
    public static void Postfix(SphericGuardian __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Artifact 1 → 2 (A17: 2 → 3)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new ArtifactPower(__instance, 1))
            );
        }
    }
}
```

### 2. Thorns 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SphericGuardian",
    method = "takeTurn"
)
public static class SphericGuardianThornsPatch {
    @SpirePostfixPatch
    public static void Postfix(SphericGuardian __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Thorns 3 → 6 (A17: 4 → 7)
            // thornsAmount 필드 수정
        }
    }
}
```

### 3. Harden Block 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.SphericGuardian",
    method = "takeTurn"
)
public static class SphericGuardianHardenPatch {
    @SpirePostfixPatch
    public static void Postfix(SphericGuardian __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Harden Block 15 → 25 (A17: 20 → 30)
            AbstractDungeon.actionManager.addToBottom(
                new GainBlockAction(__instance, __instance, 10)
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `usedActivate` | boolean | Activate 사용 여부 플래그 |
| `artifactAmount` | int | Artifact 수치 (1 또는 2) |
| `thornsAmount` | int | Thorns 수치 (3 또는 4) |
| `hardenBlock` | int | Harden Block 수치 (15 또는 20) |
| `lastMove` | byte | 이전 사용 패턴 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/SphericGuardian.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.ArtifactPower`
  - `com.megacrit.cardcrawl.powers.BarricadePower`
  - `com.megacrit.cardcrawl.powers.ThornsPower`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`

---

## 참고사항

1. **속공 필수**: 낮은 HP를 이용한 빠른 처치가 최선
2. **Artifact 차단**: 디버프 전략 무력화, 초반 소모 필요
3. **Barricade 위험**: Block 무한 누적, 장기전 절대 불가
4. **Thorns 반격**: HP 50% 이하에서 발동, 다중 공격 주의
5. **A17+ 강화**: Artifact 2, Thorns 4로 난이도 급상승
6. **처치 타이밍**: Thorns 발동 전 (HP 50% 이상) 처치 권장
