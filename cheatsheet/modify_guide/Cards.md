# 카드 (Cards) 수정 가이드

## 1. 카드 저장 위치

### 1.1 AbstractPlayer의 CardGroup 필드
```java
public CardGroup masterDeck;     // 전체 덱 (모든 카드 목록)
public CardGroup drawPile;       // 뽑기 더미 (전투 중)
public CardGroup hand;           // 손패 (전투 중)
public CardGroup discardPile;    // 버리기 더미 (전투 중)
public CardGroup exhaustPile;    // 소멸 더미 (전투 중)
public CardGroup limbo;          // 임시 저장소
```

### 1.2 CardGroup 구조
- `ArrayList<AbstractCard> group` - 카드 목록
- `CardGroupType type` - 덱 타입
- 전투 시작 시 `masterDeck`을 복사하여 `drawPile`에 넣고 섞음

## 2. 카드가 추가되는 시점

### 2.1 게임 시작 (시작 덱)
```java
// AbstractPlayer.initializeStarterDeck()
protected void initializeStarterDeck(PlayerClass chosenClass) {
    ArrayList<String> cards = getStartingDeck();
    for (String cardID : cards) {
        this.masterDeck.addToBottom(CardLibrary.getCard(cardID).makeCopy());
    }
}
```
**수정 방법**: `getStartingDeck()` 오버라이드 또는 `initializeStarterDeck` 후킹

### 2.2 전투 승리 보상
```java
// CombatRewardScreen - 카드 보상
// 기본 보상 개수: 3장
// 레어 카드 확률: Ascension에 따라 다름
```

**보상 카드 개수 수정**:
```java
@SpirePatch(clz = AbstractDungeon.class, method = "getRewardCards")
public static class IncreaseCardRewards {
    @SpirePrefixPatch
    public static SpireReturn<ArrayList<AbstractCard>> Prefix() {
        // 보상 카드 5장으로 증가
        ArrayList<AbstractCard> retVal = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AbstractCard card = AbstractDungeon.getCard(
                AbstractDungeon.rollRarity()
            );
            retVal.add(card.makeCopy());
        }
        return SpireReturn.Return(retVal);
    }
}
```

**레어 카드 확률 증가**:
```java
// AbstractRelic.changeRareCardRewardChance() 활용
public class CustomRelic extends AbstractRelic {
    @Override
    public int changeRareCardRewardChance(int rareCardChance) {
        return 20; // 20% 확률로 레어 카드 (기본: 3%)
    }
}
```

### 2.3 이벤트
- 특정 이벤트에서 카드 획득
- 예: `Neow's Blessing`, `Match and Keep`, `The Library` 등
```java
AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(
    card, Settings.WIDTH / 2.0f, Settings.HEIGHT / 2.0f
));
```

### 2.4 상점 구매
```java
// ShopScreen - 상점에서 카드 구매
// 가격: 레어리티에 따라 다름 (Common: 50, Uncommon: 75, Rare: 150)
```

### 2.5 유물 효과
- `Calling Bell`: 무작위 Curse 3장 추가
- `Astrolabe`: 시작 덱 변환
- `Pandora's Box`: 시작 카드 변환
- `Dolly's Mirror`: 카드 복제
- `Nilry's Codex`: 3턴마다 카드 선택

### 2.6 카드 생성 효과
- `Discovery`, `Dual Wield` 등 카드 생성 카드
```java
// 손패에 카드 추가
AbstractDungeon.player.hand.addToTop(card);
// 뽑기 더미에 카드 추가
AbstractDungeon.player.drawPile.addToBottom(card);
// 버리기 더미에 카드 추가
AbstractDungeon.player.discardPile.addToBottom(card);
```

## 3. 카드가 제거되는 시점

### 3.1 상점에서 카드 제거 서비스
```java
// ShopScreen - 카드 제거 가격: 75 골드
// Ascension 15+: 최초 제거 무료, 이후 75 골드
```

### 3.2 이벤트
- `The Bonfire`: 카드 제거 (5장까지)
- `Falling`: 카드 제거 (1장)
- `The Joust`: 카드 제거 (1장)
- `Tomb of Lord Red Mask`: 카드 제거 (1장)
- `We Meet Again!`: 카드 제거 (2장)

### 3.3 유물
- `Peace Pipe`: 휴식 시 카드 제거 옵션
```java
public void onRest() {
    AbstractDungeon.getCurrRoom().addCardRemoveOption();
}
```

### 3.4 Curse/Status 자동 소멸
- 일부 Curse는 특정 조건에서 자동 제거
- 예: `Writhe` (전투 종료 시 제거), `Injury` (버릴 때 소멸)

## 4. 카드 수정 방법

### 4.1 시작 덱 변경
```java
@SpirePatch(clz = Ironclad.class, method = "getStartingDeck")
public static class ModifyStartingDeck {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(ArrayList<String> __result) {
        // 기본 시작 덱에 카드 추가
        __result.add("Whirlwind");
        __result.add("Whirlwind");
        __result.add("Offering");
        return __result;
    }
}
```

### 4.2 카드 보상 개수 증가
```java
@SpirePatch(clz = AbstractDungeon.class, method = "getRewardCards")
public static class MoreCardRewards {
    @SpirePrefixPatch
    public static SpireReturn<ArrayList<AbstractCard>> Prefix() {
        int count = 5; // 5장으로 증가
        ArrayList<AbstractCard> retVal = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AbstractCard card = AbstractDungeon.getCard(
                AbstractDungeon.rollRarity()
            );
            retVal.add(card.makeCopy());
        }
        return SpireReturn.Return(retVal);
    }
}
```

### 4.3 카드 보상 레어리티 조정
```java
@SpirePatch(clz = AbstractDungeon.class, method = "rollRarity")
public static class AlwaysRareCards {
    @SpirePrefixPatch
    public static SpireReturn<AbstractCard.CardRarity> Prefix() {
        // 항상 레어 카드만 보상
        return SpireReturn.Return(AbstractCard.CardRarity.RARE);
    }
}
```

### 4.4 특정 카드 자동 추가
```java
// 전투 시작 시 특정 카드를 손패에 추가
@SpirePatch(clz = AbstractPlayer.class, method = "applyStartOfCombatLogic")
public static class AddCardAtCombatStart {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        AbstractCard offering = new Offering();
        offering.upgrade();
        __instance.hand.addToTop(offering);
    }
}
```

### 4.5 카드 업그레이드 자동화
```java
// 획득하는 모든 카드를 자동 업그레이드
@SpirePatch(clz = AbstractPlayer.class, method = "obtainCard")
public static class AutoUpgradeCards {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, AbstractCard card) {
        if (card.canUpgrade()) {
            card.upgrade();
        }
    }
}
```

### 4.6 전투 시작 시 드로우 증가
```java
@SpirePatch(clz = AbstractPlayer.class, method = "applyStartOfTurnRelics")
public static class DrawMoreCards {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 턴 시작 시 2장 추가 드로우
        AbstractDungeon.actionManager.addToBottom(
            new DrawCardAction(__instance, 2)
        );
    }
}
```

## 5. 카드 속성 수정

### 5.1 데미지 증가
```java
@SpirePatch(clz = Strike_Red.class, method = SpirePatch.CONSTRUCTOR)
public static class BuffStrike {
    @SpirePostfixPatch
    public static void Postfix(Strike_Red __instance) {
        __instance.baseDamage = 10; // 기본 6 → 10
    }
}
```

### 5.2 코스트 감소
```java
@SpirePatch(clz = Whirlwind.class, method = SpirePatch.CONSTRUCTOR)
public static class CheapWhirlwind {
    @SpirePostfixPatch
    public static void Postfix(Whirlwind __instance) {
        __instance.cost = 0; // X코스트를 0코스트로
        __instance.costForTurn = 0;
    }
}
```

### 5.3 드로우 증가
```java
@SpirePatch(clz = Pommel_Strike.class, method = SpirePatch.CONSTRUCTOR)
public static class BetterPommelStrike {
    @SpirePostfixPatch
    public static void Postfix(Pommel_Strike __instance) {
        __instance.magicNumber = 3; // 1장 드로우 → 3장 드로우
        __instance.baseMagicNumber = 3;
    }
}
```

## 6. 실용적인 수정 예시

### 6.1 무한 카드 선택 유물
```java
public class InfiniteCardSelectRelic extends CustomRelic {
    @Override
    public void atBattleStart() {
        AbstractDungeon.actionManager.addToBottom(
            new DiscoveryAction(AbstractCard.CardColor.RED, 999)
        );
    }
}
```

### 6.2 턴 시작 시 무작위 카드 생성
```java
@SpirePatch(clz = AbstractPlayer.class, method = "applyStartOfTurnCards")
public static class RandomCardEachTurn {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        AbstractCard card = AbstractDungeon.returnTrulyRandomCardInCombat();
        card.upgrade();
        card.setCostForTurn(0);
        __instance.hand.addToTop(card);
    }
}
```

### 6.3 모든 카드 0코스트
```java
@SpirePatch(clz = AbstractCard.class, method = "applyPowers")
public static class AllCardsFree {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance) {
        __instance.costForTurn = 0;
    }
}
```

### 6.4 덱에서 특정 카드 제거
```java
// masterDeck에서 모든 Strike 제거
public static void removeAllStrikes() {
    AbstractPlayer p = AbstractDungeon.player;
    ArrayList<AbstractCard> toRemove = new ArrayList<>();

    for (AbstractCard card : p.masterDeck.group) {
        if (card.hasTag(AbstractCard.CardTags.STARTER_STRIKE)) {
            toRemove.add(card);
        }
    }

    for (AbstractCard card : toRemove) {
        p.masterDeck.removeCard(card);
    }
}
```

### 6.5 카드 복제
```java
// 손패의 모든 카드 복제
public static void duplicateHand() {
    AbstractPlayer p = AbstractDungeon.player;
    ArrayList<AbstractCard> copies = new ArrayList<>();

    for (AbstractCard card : p.hand.group) {
        copies.add(card.makeStatEquivalentCopy());
    }

    for (AbstractCard copy : copies) {
        p.hand.addToTop(copy);
    }
}
```

## 7. 관련 클래스

### 7.1 CardGroup
```java
public class CardGroup {
    public ArrayList<AbstractCard> group;

    public void addToBottom(AbstractCard card);
    public void addToTop(AbstractCard card);
    public void removeCard(AbstractCard card);
    public void shuffle();
    public AbstractCard getTopCard();
    public AbstractCard getBottomCard();
    public void clear();
}
```

### 7.2 CardRewardScreen
```java
// 전투 보상 카드 선택 화면
public class CardRewardScreen {
    public ArrayList<AbstractCard> rewardGroup;
    public void open(ArrayList<AbstractCard> cards, RewardItem rItem, String header);
}
```

### 7.3 CardLibrary
```java
// 모든 카드의 중앙 저장소
public class CardLibrary {
    public static HashMap<String, AbstractCard> cards;

    public static AbstractCard getCard(String cardID);
    public static AbstractCard getCopy(String cardID);
}
```

### 7.4 AbstractCard
```java
public abstract class AbstractCard {
    public int baseDamage;
    public int baseBlock;
    public int baseMagicNumber;
    public int cost;
    public int costForTurn;
    public CardRarity rarity;
    public CardColor color;
    public CardType type;

    public void upgrade();
    public boolean canUpgrade();
    public AbstractCard makeCopy();
    public AbstractCard makeStatEquivalentCopy();
}
```

## 8. 주의사항

### 8.1 CardGroup vs MasterDeck
- `masterDeck`: 플레이어가 소유한 모든 카드 (영구적)
- `drawPile`, `hand`, `discardPile`, `exhaustPile`: 전투 중에만 존재
- 전투 시작 시 `masterDeck`이 `drawPile`로 복사됨

### 8.2 카드 복사
```java
// 잘못된 방법 (참조만 복사)
AbstractCard wrong = card;

// 올바른 방법 (새 인스턴스 생성)
AbstractCard correct = card.makeCopy();
AbstractCard upgraded = card.makeStatEquivalentCopy(); // 업그레이드 상태 보존
```

### 8.3 카드 추가 타이밍
- 전투 중: `hand`, `drawPile`, `discardPile` 사용
- 전투 외: `masterDeck` 사용
- 잘못된 타이밍에 추가하면 게임 크래시 발생

### 8.4 Curse와 Status 카드
- Curse: `masterDeck`에 영구 추가
- Status: 전투 중에만 존재, 전투 종료 시 자동 제거
- 일부 Curse는 특수 제거 조건 있음

### 8.5 카드 획득 애니메이션
```java
// 올바른 카드 획득 방법 (애니메이션 포함)
AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(
    card,
    Settings.WIDTH / 2.0f,
    Settings.HEIGHT / 2.0f
));

// 즉시 획득 (애니메이션 없음)
AbstractDungeon.player.masterDeck.addToTop(card);
```

### 8.6 Ascension 난이도 고려
- A15+: 카드 제거 비용 변경
- A10+: 카드 보상 레어리티 감소
- A1+: 엘리트 몬스터가 Curse 카드 추가

### 8.7 세이브 파일 호환성
- `masterDeck` 변경 시 기존 세이브와 호환 문제 발생 가능
- 카드 ID 변경 주의 (세이브 로드 시 null 참조 발생)
