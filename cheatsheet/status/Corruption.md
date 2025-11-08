# 부패 (Corruption)

## 기본 정보

**클래스명**: `CorruptionPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.CorruptionPower`
**ID**: `"Corruption"`
**타입**: 버프
**적용 대상**: 플레이어 전용

---

## 효과

**기본 효과**: 모든 스킬 카드의 코스트가 0이 되고, 사용 시 소진됨
**수치 설명**: `amount = -1` (영구 지속, 수치 표시 없음)
**적용 시점**:
- **카드 드로우 시**: 코스트를 0으로 변경 (onCardDraw)
- **카드 사용 시**: 소진 표시 추가 (onUseCard)

**중요**:
- 스킬 카드만 영향을 받음 (공격/파워 제외)
- 영구 지속 파워 (제거 불가)
- 코스트 0 + 소진의 양날의 검

---

## 코드 분석

### 생성자
```java
public CorruptionPower(AbstractCreature owner) {
    this.name = NAME;
    this.ID = "Corruption";
    this.owner = owner;
    this.amount = -1;  // 영구 지속
    this.description = DESCRIPTIONS[0];
    loadRegion("corruption");
}
```

**주요 특징**:
- `amount = -1`: 영구 지속 파워 (턴 종료 시 감소하지 않음)
- 별도의 생성자 파라미터 없음 (항상 영구)

### 핵심 메서드

**onCardDraw()**: 카드 드로우 시 코스트 변경

```java
public void onCardDraw(AbstractCard card) {
    if (card.type == AbstractCard.CardType.SKILL) {
        card.setCostForTurn(-9);  // 코스트를 0으로 설정
    }
}
```

**중요**:
- `setCostForTurn(-9)`: 게임 내부적으로 코스트 0을 의미하는 특수 값
- **스킬만 체크**: `card.type == AbstractCard.CardType.SKILL`
- 드로우되는 순간 즉시 적용됨

**onUseCard()**: 카드 사용 시 소진 설정

```java
public void onUseCard(AbstractCard card, UseCardAction action) {
    if (card.type == AbstractCard.CardType.SKILL) {
        flash();  // 시각 효과
        action.exhaustCard = true;  // 소진 표시
    }
}
```

**중요**:
- `action.exhaustCard = true`: 카드를 버리는 대신 소진 더미로 이동
- 스킬만 영향을 받음

### 설명 업데이트

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[1];  // "스킬의 코스트가 0이 됩니다. 스킬 사용 시 소진됩니다."
}
```

---

## 수정 방법

### 소진 제거 (코스트 0만 적용)

**변경 대상**: onUseCard 메서드
**목적**: 스킬을 소진하지 않고 코스트만 0으로

```java
@SpirePatch(
    clz = CorruptionPower.class,
    method = "onUseCard"
)
public static class NoExhaustCorruptionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card, UseCardAction action) {
        // 소진 로직을 제거: 아무것도 하지 않음
        return SpireReturn.Return(null);
    }
}
```

### 모든 카드 타입에 적용

**변경 대상**: 카드 타입 체크
**목적**: 공격/파워 카드도 코스트 0 + 소진

```java
@SpirePatch(
    clz = CorruptionPower.class,
    method = "onCardDraw"
)
public static class AllCardsCorruptionDrawPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card) {
        // 모든 카드 타입 적용
        card.setCostForTurn(-9);
        return SpireReturn.Return(null);
    }
}

@SpirePatch(
    clz = CorruptionPower.class,
    method = "onUseCard"
)
public static class AllCardsCorruptionUsePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card, UseCardAction action) {
        // 모든 카드 타입 소진
        __instance.flash();
        action.exhaustCard = true;
        return SpireReturn.Return(null);
    }
}
```

### 코스트 감소 대신 고정 값

**목적**: 코스트 0 대신 1로 변경

```java
@SpirePatch(
    clz = CorruptionPower.class,
    method = "onCardDraw"
)
public static class CostOneCorruptionPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card) {
        if (card.type == AbstractCard.CardType.SKILL) {
            // -9 대신 1로 설정
            card.setCostForTurn(1);
        }
        return SpireReturn.Return(null);
    }
}
```

### 제한적 Corruption (N개 카드만)

**목적**: 영구가 아닌 N개 스킬만 적용

```java
@SpirePatch(
    clz = CorruptionPower.class,
    method = SpirePatch.CLASS
)
public static class LimitedCorruptionFields {
    public static SpireField<Integer> cardsAffected = new SpireField<>(() -> 0);
    public static SpireField<Integer> maxCards = new SpireField<>(() -> 5);  // 최대 5개
}

@SpirePatch(
    clz = CorruptionPower.class,
    method = "onCardDraw"
)
public static class LimitedCorruptionDrawPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card) {
        int affected = LimitedCorruptionFields.cardsAffected.get(__instance);
        int max = LimitedCorruptionFields.maxCards.get(__instance);

        if (card.type == AbstractCard.CardType.SKILL && affected < max) {
            card.setCostForTurn(-9);
            LimitedCorruptionFields.cardsAffected.set(__instance, affected + 1);

            // 최대 개수 도달 시 파워 제거
            if (affected + 1 >= max) {
                addToBot(new RemoveSpecificPowerAction(__instance.owner, __instance.owner, "Corruption"));
            }
        }

        return SpireReturn.Return(null);
    }
}
```

### 조건부 소진 (고코스트만)

**목적**: 코스트 2 이상 스킬만 소진

```java
@SpirePatch(
    clz = CorruptionPower.class,
    method = "onUseCard"
)
public static class ConditionalExhaustPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CorruptionPower __instance, AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.SKILL) {
            __instance.flash();

            // 원래 코스트가 2 이상일 때만 소진
            if (card.costForTurn >= 2 || card.cost >= 2) {
                action.exhaustCard = true;
            }
        }

        return SpireReturn.Return(null);
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Corruption (아이언클래드 - 파워 카드, 코스트 3→2)

**시너지 카드 (스킬)**:
- **고코스트 스킬**: Impervious, Limit Break (원래 비싼 스킬을 무료로)
- **파워 스킬**: Inflame, Demon Form (0코스트 파워 획득)
- **방어 스킬**: Defend, Shrug It Off (무료 방어)
- **유틸 스킬**: Battle Trance, Seeing Red (무료 드로우/에너지)

**시너지 유물**:
- **Dead Branch**: 소진 시 랜덤 카드 생성 (무한 카드 생성 콤보)
- **Feel No Pain**: 소진 시 Block 획득 (소진마다 방어)
- **Dark Embrace**: 소진 시 카드 드로우 (소진마다 드로우)
- **Strange Spoon**: 50% 확률로 소진 무효 (스킬 재사용 가능)

**안티 시너지**:
- **Runic Pyramid**: 턴 종료 시 카드 버리지 않음 (스킬이 손에 계속 쌓임)
- 적은 스킬 덱: 스킬이 적으면 효과가 미미함

---

## 참고사항

1. **amount = -1**: 영구 지속 파워의 표준 표시 방법
2. **setCostForTurn(-9)**:
   - 게임 내부적으로 코스트 0을 의미
   - 임시 코스트 변경 (원래 코스트는 유지)
3. **스킬만 적용**: `card.type == AbstractCard.CardType.SKILL` 체크 필수
4. **양날의 검**:
   - 장점: 모든 스킬을 무료로 사용
   - 단점: 사용한 스킬은 영구히 소진됨
5. **Dead Branch 콤보**:
   - Corruption으로 스킬 무료 사용 + 소진
   - Dead Branch로 소진마다 랜덤 카드 생성
   - 무한 카드 플레이 가능
6. **Feel No Pain 콤보**:
   - 소진마다 Block 획득
   - 스킬 플레이 = 무료 + Block 획득 + 효과
7. **제거 불가**: 일반적인 방법으로는 Corruption을 제거할 수 없음 (영구 버프)
8. **onCardDraw vs onUseCard**:
   - onCardDraw: 드로우 시 코스트 변경 (시각적으로 0 표시)
   - onUseCard: 사용 시 소진 설정 (실제 소진 처리)
9. **아이언클래드 전용**: 주로 아이언클래드가 사용하는 파워 카드
10. **고위험 고수익**: 덱을 영구히 약화시키는 대신 단기적으로 강력함
