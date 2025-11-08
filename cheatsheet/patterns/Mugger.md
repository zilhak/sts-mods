# 강도 (Mugger)

## 기본 정보

**클래스명**: `Mugger`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Mugger`
**ID**: `"Mugger"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 44-48 |
| A7+ | 46-50 |

**특징**: 낮은 HP, 골드 훔치기 능력

---

## 패턴 정보

### 패턴 1: 돌진 (Lunge)

**의도**: `ATTACK`
**데미지**: 10

**발동 확률**: 60%

**효과**:
- 플레이어에게 **10 데미지** 공격

**코드 특징**:
```java
// 기본 공격
baseDamage = 10
```

**수정 포인트**:
- 데미지 변경: 10 → 조정값

---

### 패턴 2: 연막탄 (Smoke Bomb)

**의도**: `ESCAPE`
**효과**: 골드 훔치고 도망

**골드 훔치기**:
| 난이도 | 골드 |
|--------|------|
| 기본 (A0-A16) | 10-15 |
| A17+ | 15-20 |

**발동 확률**: 40%

**효과**:
- 플레이어 골드 **10-15** 훔치기 (A17+: 15-20)
- 전투 종료 (도망)
- **중요**: 훔친 골드는 회수 불가

**코드 특징**:
```java
// 골드 감소
int stolenGold = (AbstractDungeon.ascensionLevel >= 17) ?
    MathUtils.random(15, 20) : MathUtils.random(10, 15);
AbstractDungeon.player.loseGold(stolenGold);

// 도망
this.escaped = true;
AbstractDungeon.getCurrRoom().smoked = true;
```

**수정 포인트**:
- 골드 범위 변경
- 도망 조건 추가 (HP 임계값 등)
- 추가 페널티 (카드 제거 등)

---

## AI 로직 (takeTurn 기반 자동 시퀀스)

**getMove()** (Line 184-186):
```java
protected void getMove(int num) {
  setMove((byte)1, Intent.ATTACK, damage[0].base);
}
```

**중요**: getMove()는 **단순히 MUG(1)만 설정**하며, 실제 패턴 로직은 **takeTurn()에서 처리**

**takeTurn() 자동 시퀀스** (Line 85-148):
```java
case 1: // MUG (골드 훔치기 + 공격)
  // 골드 훔치기 + 10 데미지
  slashCount++;

  if (slashCount == 2) {
    if (random(50%)) {
      setMove(SMOKE_BOMB, DEFEND);  // 50%
    } else {
      setMove(BIGSWIPE, ATTACK, 16-18);  // 50%
    }
  } else {
    setMove(MUG, ATTACK, 10-11);  // 반복
  }
  break;

case 4: // BIGSWIPE (강력 공격)
  // 18 데미지 + 골드 훔치기
  setMove(SMOKE_BOMB, DEFEND);
  break;

case 2: // SMOKE_BOMB (방어)
  // Block 11 (A17: 17)
  setMove(ESCAPE, ESCAPE);
  break;

case 3: // ESCAPE
  // 도망 (전투 종료)
  break;
```

**실제 패턴 시퀀스**:
1. **MUG** (골드 훔치기) → slashCount = 1
2. **MUG** (골드 훔치기) → slashCount = 2
3. **50% SMOKE_BOMB / 50% BIGSWIPE**
4. **SMOKE_BOMB** (방어) → 다음 턴 ESCAPE 확정
5. **ESCAPE** (도망, 전투 종료)

**특수 규칙**:
- MUG 2회 후 분기 (SMOKE_BOMB 또는 BIGSWIPE)
- SMOKE_BOMB은 방어 + 다음 턴 도망 예고
- ESCAPE 사용 시 전투 즉시 종료, 보상 없음

**수정 포인트**:
- slashCount 임계값 조정 (2 → 3으로 MUG 횟수 증가)
- SMOKE_BOMB 확률 조정 (50% → 70%)
- ESCAPE 시 추가 페널티 (카드 제거 등)

---

## 특수 동작

### 골드 훔치기 메커니즘

**골드 감소**:
```java
public void stealGold() {
    int stolenAmount = calculateStolenGold();
    AbstractDungeon.player.loseGold(stolenAmount);

    // 메시지 표시
    CardCrawlGame.sound.play("GOLD_JINGLE");
    AbstractDungeon.effectList.add(
        new GainGoldTextEffect(-stolenAmount)
    );
}
```

**특징**:
- 즉시 골드 감소
- 회수 불가능
- 음수 효과 표시

### 도망 메커니즘

**전투 종료**:
```java
public void escape() {
    this.escaped = true;
    AbstractDungeon.getCurrRoom().smoked = true;
    AbstractDungeon.getCurrRoom().endBattle();

    // 보상 없음
    AbstractDungeon.getCurrRoom().rewards.clear();
}
```

**효과**:
- 전투 즉시 종료
- 모든 보상 제거
- 골드도 손실

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **속공**: 낮은 HP (44-50) 이용해 빠른 처치
2. **골드 보호**: Smoke Bomb 전 처치
3. **고데미지**: 2-3턴 내 처치 목표

**위험 요소**:
- Smoke Bomb → 골드 손실 + 보상 없음
- 장기전 → Smoke Bomb 확률 증가
- 골드 부족 시 → 상점 이용 불가

**카운터 전략**:
- **속공**: 2턴 내 처치 목표
- **고데미지 카드**: Bludgeon, Heavy Blade
- **AOE**: 멀티 인카운터 시 동시 타격

**피해야 할 행동**:
- 장기전 → Smoke Bomb 위험 증가
- 방어 위주 플레이 → 처치 지연

---

## 멀티 인카운터

### 3 Muggers

**조합 정보**:
- Mugger 3마리 동시 등장
- 각자 독립적으로 Smoke Bomb 사용 가능
- 모두 도망 시 큰 골드 손실

**전략 변화**:
- **AOE 필수**: Whirlwind, Immolate
- **우선순위**: 낮은 HP 먼저 처치
- **골드 보호**: 빠른 처치 필수

**위험도**:
- 3마리 모두 Smoke Bomb → 30-60 골드 손실
- 보상 전멸 가능성

---

## 수정 예시

### 1. 골드 훔치기 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Mugger",
    method = "takeTurn"
)
public static class MuggerStealPatch {
    @SpirePrefixPatch
    public static void Prefix(Mugger __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 골드 10-15 → 20-30 (A17: 15-20 → 25-35)
            // stolenGold 계산 수정
        }
    }
}
```

### 2. Smoke Bomb 조건 추가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Mugger",
    method = "getMove"
)
public static class MuggerSmokeBombPatch {
    @SpirePrefixPatch
    public static void Prefix(Mugger __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // HP 50% 이하면 Smoke Bomb 확률 증가
            if (__instance.currentHealth <= __instance.maxHealth * 0.5f) {
                // Smoke Bomb 확률 40% → 70%
            }
        }
    }
}
```

### 3. 추가 페널티: 카드 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Mugger",
    method = "escape"
)
public static class MuggerCardRemovalPatch {
    @SpirePostfixPatch
    public static void Postfix(Mugger __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 골드 훔치기 + 랜덤 카드 1장 제거
            AbstractCard randomCard = AbstractDungeon.player.masterDeck
                .getRandomCard(AbstractDungeon.cardRandomRng);
            AbstractDungeon.player.masterDeck.removeCard(randomCard);
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `escaped` | boolean | 도망 여부 플래그 |
| `stolenGold` | int | 훔친 골드 수치 |
| `lungeDamage` | int | Lunge 데미지 (10) |
| `lastMove` | byte | 이전 사용 패턴 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Mugger.java`
- **액션**:
  - `DamageAction`
  - `LoseGoldAction`
  - `EscapeAction`

---

## 참고사항

1. **골드 손실**: Smoke Bomb으로 10-20 골드 손실, 회수 불가
2. **보상 없음**: 도망 시 모든 보상 제거
3. **속공 필수**: 2-3턴 내 처치로 골드 보호
4. **멀티 인카운터**: 3마리 등장 시 큰 손실 위험
5. **AOE 유리**: Whirlwind, Immolate 등으로 동시 타격
6. **골드 부족 위험**: 상점 이용 불가로 이어질 수 있음
