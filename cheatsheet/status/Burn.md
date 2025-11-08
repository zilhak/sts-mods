# 화상 (Burn)

## 기본 정보

**클래스명**: `Burn`
**전체 경로**: `com.megacrit.cardcrawl.cards.status.Burn`
**ID**: `"Burn"`
**타입**: 상태 카드 (STATUS)
**색상**: 무색 (COLORLESS)
**희귀도**: 일반 (COMMON)
**코스트**: 언플레이어블 (-2)
**대상**: 없음 (NONE)

---

## 카드 정보

### 기본 카드

**데미지**: 2 (magicNumber)

**코드 위치**: 19-33줄

```java
public Burn() {
    super("Burn", cardStrings.NAME, "status/burn", -2, cardStrings.DESCRIPTION,
        AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS,
        AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);

    this.magicNumber = 2;
    this.baseMagicNumber = 2;
}
```

### 강화 카드 (Burn+)

**데미지**: 4 (2 + 2)

**코드 위치**: 59-66줄

```java
public void upgrade() {
    if (!this.upgraded) {
        upgradeName();
        upgradeMagicNumber(2);  // 2 → 4
        this.rawDescription = cardStrings.UPGRADE_DESCRIPTION;
        initializeDescription();
    }
}
```

---

## 메커니즘

### 1. 직접 사용 불가 (Unplayable)

**코스트**: -2 (언플레이어블)

- 손에서 직접 사용할 수 없음
- 턴 종료 시 자동 발동

### 2. 턴 종료 시 자동 발동 (triggerOnEndOfTurnForPlayingCard)

**코드 위치**: 47-50줄

```java
public void triggerOnEndOfTurnForPlayingCard() {
    this.dontTriggerOnUseCard = true;
    AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(this, true));
}
```

**동작 순서**:
1. 턴 종료 시 자동으로 카드 큐에 추가
2. `dontTriggerOnUseCard` 플래그 설정
3. `use()` 메서드 실행

### 3. 데미지 처리 (use)

**코드 위치**: 36-40줄

```java
public void use(AbstractPlayer p, AbstractMonster m) {
    if (this.dontTriggerOnUseCard) {
        addToBot((AbstractGameAction)new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            new DamageInfo((AbstractCreature)AbstractDungeon.player,
                this.magicNumber, DamageInfo.DamageType.THORNS),
            AbstractGameAction.AttackEffect.FIRE));
    }
}
```

**특징**:
- 플레이어가 자신에게 데미지 입힘
- 데미지 타입: `THORNS` (가시 데미지)
- 방어도 무시
- 공격 이펙트: `FIRE` (불꽃 효과)

---

## 데미지 타입: THORNS

**특징**:
- 방어도 무시
- 버퍼(Buffer) 무시
- 취약(Vulnerable) 영향 없음
- 힘(Strength) 영향 없음

**Poison/Constricted와의 비교**:

| 항목 | Burn | Poison | Constricted |
|------|------|--------|-------------|
| 타입 | 상태 카드 | 파워 | 파워 |
| 발동 타이밍 | 턴 종료 (자동 사용) | 턴 시작 | 턴 종료 |
| 데미지 타입 | THORNS | HP_LOSS | THORNS |
| 데미지 (기본) | 2 | 스택 수 | 스택 수 |
| 데미지 (강화) | 4 | - | - |
| 제거 방법 | 사용 시 자동 제거 | 매 턴 -1 | 지속 (제거 불가) |

---

## 관련 몬스터 및 시스템

### Hexaghost (헥사고스트) - 1막 보스

**클래스**: `com.megacrit.cardcrawl.monsters.exordium.Hexaghost`

**Burn 부여 패턴**:

1. **Sear (지지기)**:
   - Burn 카드 추가
   - A19 미만: 1장
   - A19+: 2장

2. **Inferno (지옥불)**:
   - 업그레이드된 Burn+ 카드 추가
   - `BurnIncreaseAction` 실행

**난이도별 수치**:

| 난이도 | Sear Burn 개수 |
|--------|---------------|
| A0-A18 | 1 |
| A19+ | 2 |

---

## BurnIncreaseAction (업그레이드 및 추가)

**클래스**: `com.megacrit.cardcrawl.actions.unique.BurnIncreaseAction`

**동작 순서**:

1. **기존 Burn 업그레이드** (21-31줄):
   ```java
   // 버림 더미의 모든 Burn 카드 업그레이드
   for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
       if (c instanceof Burn) {
           c.upgrade();  // 2 → 4 데미지
       }
   }

   // 뽑기 더미의 모든 Burn 카드 업그레이드
   for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
       if (c instanceof Burn) {
           c.upgrade();
       }
   }
   ```

2. **새로운 Burn+ 추가** (34-44줄):
   ```java
   // Burn+ 3장을 버림 더미에 추가
   if (this.duration < 1.5F && !this.gotBurned) {
       this.gotBurned = true;

       Burn b = new Burn();
       b.upgrade();
       AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(b));

       // 총 3장의 Burn+ 추가
       // (코드 반복 생략)
   }
   ```

**효과**:
- 덱에 있는 모든 Burn 카드를 Burn+로 업그레이드 (2→4 데미지)
- 버림 더미에 Burn+ 3장 추가

---

## 제거 및 대응 방법

### 1. 자동 제거
- Burn 카드는 턴 종료 시 자동으로 발동되어 제거됨
- 손에 남아있으면 다음 턴 종료 시 발동

### 2. 수동 제거 카드

**Second Wind** (아이언클래드):
- 비공격 카드 소진 시 방어도 획득
- Burn 카드를 소진하여 제거 가능

**True Grit** (아이언클래드):
- 손에서 랜덤 카드 1장 소진
- Burn 카드가 선택되면 데미지 없이 제거

**Gambling Chip** (유물):
- 버림 시 카드 뽑기
- Burn 카드를 버려서 대응 가능

### 3. 데미지 경감

**Burn 데미지는 THORNS 타입**이므로:
- 방어도로 막을 수 없음
- Buffer, Intangible로도 무효화 불가
- 취약/힘 영향 없음

**대응 방법**:
- HP 여유 확보
- 회복 효과 (포션, 유물)
- 빠른 전투 종료

---

## 수정 포인트

### 1. Burn 데미지 변경

```java
@SpirePatch(
    clz = Burn.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class BurnDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Burn __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // 기본 Burn 데미지 2 → 3
            __instance.magicNumber = 3;
            __instance.baseMagicNumber = 3;
        }
    }
}
```

### 2. Burn 업그레이드 데미지 변경

```java
@SpirePatch(
    clz = Burn.class,
    method = "upgrade"
)
public static class BurnUpgradePatch {
    @SpirePostfixPatch
    public static void Postfix(Burn __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // Burn+ 데미지 4 → 6
            __instance.magicNumber = 6;
            __instance.baseMagicNumber = 6;
        }
    }
}
```

### 3. Hexaghost Sear Burn 개수 증가

```java
@SpirePatch(
    clz = "com.megacrit.cardcrawl.monsters.exordium.Hexaghost",
    method = SpirePatch.CONSTRUCTOR
)
public static class HexaghostBurnCountPatch {
    @SpirePostfixPatch
    public static void Postfix(Object __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Sear Burn 개수 2 → 3
            ReflectionHacks.setPrivate(__instance, Hexaghost.class, "searBurnCount", 3);
        }
    }
}
```

### 4. BurnIncreaseAction 추가 개수 변경

```java
@SpirePatch(
    clz = BurnIncreaseAction.class,
    method = "update"
)
public static class BurnIncreaseCountPatch {
    @SpireInsertPatch(
        locator = BurnAddLocator.class
    )
    public static SpireReturn<Void> Insert(BurnIncreaseAction __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // Burn+ 추가 개수 3 → 5
            for (int i = 0; i < 5; i++) {
                Burn b = new Burn();
                b.upgrade();
                AbstractDungeon.effectList.add(
                    new ShowCardAndAddToDiscardEffect(b));
            }
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `magicNumber` | int | 데미지 수치 (기본 2, 강화 4) |
| `dontTriggerOnUseCard` | boolean | 턴 종료 발동 플래그 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/cards/status/Burn.java`
- **액션**:
  - `com/megacrit/cardcrawl/actions/common/DamageAction.java`
  - `com/megacrit/cardcrawl/actions/unique/BurnIncreaseAction.java`
- **관련 몬스터**:
  - `com/megacrit/cardcrawl/monsters/exordium/Hexaghost.java`
- **관련 카드**:
  - `com/megacrit/cardcrawl/cards/ironclad/SecondWind.java`
  - `com/megacrit/cardcrawl/cards/ironclad/TrueGrit.java`

---

## 참고사항

1. **카드 타입**: 상태 카드 (STATUS), 파워가 아님
2. **발동 타이밍**: 턴 종료 시 자동 발동 (`triggerOnEndOfTurnForPlayingCard`)
3. **데미지 타입**: `THORNS` (방어도 무시, 버퍼 무시)
4. **코스트**: -2 (언플레이어블, 직접 사용 불가)
5. **업그레이드**: 데미지 2 → 4
6. **자동 제거**: 발동 후 카드가 사라짐 (소진은 아님)
7. **주요 출처**: Hexaghost (1막 보스)
8. **특수 메커니즘**:
   - `dontTriggerOnUseCard` 플래그로 턴 종료 발동 구분
   - `BurnIncreaseAction`으로 모든 Burn 업그레이드 + 3장 추가
9. **대응 방법**:
   - Second Wind, True Grit (소진)
   - 회복 카드/유물 (데미지 감수)
   - 방어도는 효과 없음 (THORNS 타입)
10. **Poison/Constricted와의 차이**:
    - Burn: 상태 카드, 턴 종료 시 자동 사용 후 제거
    - Poison: 파워, 턴 시작 시 데미지 후 -1 감소
    - Constricted: 파워, 턴 종료 시 데미지 (감소 없음)
