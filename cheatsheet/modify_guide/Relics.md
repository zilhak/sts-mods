# 유물 (Relics) 수정 가이드

## 1. 유물 저장 위치

### 1.1 AbstractPlayer의 유물 필드
```java
public ArrayList<AbstractRelic> relics = new ArrayList<>();
```

### 1.2 유물 위치 시스템
- 화면 상단에 좌측부터 정렬
- 위치 계산: `START_X + slot * PAD_X`
- `PAD_X = 72.0F * Settings.scale` (유물 간 간격)
- 페이지별 최대 유물 개수: `MAX_RELICS_PER_PAGE`

## 2. 유물이 추가되는 시점

### 2.1 게임 시작 (시작 유물)
```java
// AbstractPlayer.initializeStarterRelics()
protected void initializeStarterRelics(PlayerClass chosenClass) {
    ArrayList<String> relics = getStartingRelics();
    for (String relicID : relics) {
        AbstractRelic relic = RelicLibrary.getRelic(relicID).makeCopy();
        relic.instantObtain(this, this.relics.size(), false);
    }
}
```

**캐릭터별 시작 유물**:
- Ironclad: `Burning Blood`
- Silent: `Ring of the Snake`
- Defect: `Cracked Core`
- Watcher: `Pure Water`

**수정 방법**:
```java
@SpirePatch(clz = Ironclad.class, method = "getStartingRelics")
public static class AddStartingRelics {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(ArrayList<String> __result) {
        __result.add("Akabeko");
        __result.add("Bag of Preparation");
        return __result;
    }
}
```

### 2.2 보스 처치 보상
```java
// BossRelicScreen - 보스 유물 3개 중 1개 선택
// 보스 유물 풀에서 무작위 3개 제시
```

**보스 유물 개수 증가**:
```java
@SpirePatch(clz = AbstractDungeon.class, method = "bossRelicScreen")
public static class MoreBossRelics {
    @SpirePrefixPatch
    public static void Prefix() {
        // 보스 유물 5개 제공
        ArrayList<AbstractRelic> relics = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AbstractRelic relic = AbstractDungeon.returnRandomRelic(
                AbstractRelic.RelicTier.BOSS
            );
            relics.add(relic);
        }
        // 화면에 표시
        AbstractDungeon.bossRelicScreen.open(relics);
    }
}
```

### 2.3 엘리트 처치 보상
```java
// 엘리트 몬스터 처치 시 유물 보상
// 확률: 100% (기본)
```

**레어리티별 확률**:
- Common: 50%
- Uncommon: 33%
- Rare: 17%

**모든 엘리트 보상을 레어 유물로**:
```java
@SpirePatch(clz = AbstractDungeon.class, method = "returnRandomRelic")
public static class AlwaysRareFromElite {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic> Prefix(AbstractRelic.RelicTier tier) {
        if (tier == AbstractRelic.RelicTier.UNCOMMON ||
            tier == AbstractRelic.RelicTier.COMMON) {
            AbstractRelic relic = AbstractDungeon.returnRandomRelic(
                AbstractRelic.RelicTier.RARE
            );
            return SpireReturn.Return(relic);
        }
        return SpireReturn.Continue();
    }
}
```

### 2.4 상점 구매
```java
// ShopScreen - 상점에서 유물 구매
```

**유물 가격 (RelicTier별)**:
```java
public int getPrice() {
    switch (this.tier) {
        case COMMON:    return 150;
        case UNCOMMON:  return 250;
        case RARE:      return 300;
        case SHOP:      return 150; // 상점 전용 유물
        case BOSS:      return 999; // 구매 불가
    }
    return -1;
}
```

**유물 가격 변경**:
```java
@SpirePatch(clz = AbstractRelic.class, method = "getPrice")
public static class FreeRelics {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(AbstractRelic __instance) {
        return SpireReturn.Return(0); // 모든 유물 무료
    }
}
```

### 2.5 이벤트
**유물 획득 이벤트**:
- `Neow's Blessing`: 게임 시작 시 유물 선택
- `Living Wall`: 일반/레어 유물 선택
- `Golden Shrine`: 골드 지불하고 유물 획득
- `Augmenter`: 유물 교환
- `Cursed Tome`: 유물 + Curse 획득
- `Old Beggar`: 골드로 유물 구매
- `The Nest`: 유물 + Curse 획득
- `The Joust`: 유물 획득
- `Knowing Skull`: 보스 유물 획득 (대가 지불)

**이벤트에서 유물 획득**:
```java
AbstractRelic relic = AbstractDungeon.returnRandomRelic(
    AbstractRelic.RelicTier.RARE
);
AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
    Settings.WIDTH / 2.0f,
    Settings.HEIGHT / 2.0f,
    relic
);
```

### 2.6 보물 상자
```java
// TreasureRoom - 보물방에서 유물 획득
// 일반 보물상자: COMMON/UNCOMMON/RARE
// 보스 보물상자: BOSS
```

**보물상자 레어리티 조작**:
```java
@SpirePatch(clz = AbstractChest.class, method = "open")
public static class BetterChests {
    @SpirePrefixPatch
    public static void Prefix(AbstractChest __instance) {
        // 보물상자에서 항상 레어 유물
        if (__instance instanceof SmallChest ||
            __instance instanceof MediumChest ||
            __instance instanceof LargeChest) {
            AbstractRelic relic = AbstractDungeon.returnRandomRelic(
                AbstractRelic.RelicTier.RARE
            );
            AbstractDungeon.getCurrRoom().addRelicToRewards(relic);
        }
    }
}
```

### 2.7 유물 효과로 유물 획득
- `Calling Bell`: 무작위 유물 1개 + Curse 3장
- `Prismatic Shard`: 다른 캐릭터 카드 허용
- `Tiny House`: 골드, 카드, 포션, 유물 획득
- `Matryoshka`: 일반 유물 2개 획득

## 3. 유물이 제거되는 시점

### 3.1 이벤트
**유물 제거 이벤트**:
- `Augmenter`: 유물 1개 제거 후 새 유물 획득
- `We Meet Again!`: 유물 제거 (Circlet 제외)
- `Secret Portal` (Act 4): 유물 버리기 선택

**유물 제거 코드**:
```java
// 특정 유물 제거
AbstractRelic relicToRemove = AbstractDungeon.player.getRelic("relicID");
if (relicToRemove != null) {
    relicToRemove.onUnequip();
    AbstractDungeon.player.relics.remove(relicToRemove);
}
```

### 3.2 보스 교환 유물
- `Black Star`, `Runic Dome` 등 일부 보스 유물 선택 시 시작 유물 교체
```java
// 보스 유물 획득 시 시작 유물 제거
if (AbstractDungeon.player.hasRelic("Burning Blood")) {
    AbstractRelic oldRelic = AbstractDungeon.player.getRelic("Burning Blood");
    oldRelic.onUnequip();
    AbstractDungeon.player.relics.remove(oldRelic);
}
```

## 4. 유물 수정 방법

### 4.1 시작 유물 추가
```java
@SpirePatch(clz = Ironclad.class, method = "getStartingRelics")
public static class ExtraStartingRelics {
    @SpirePostfixPatch
    public static ArrayList<String> Postfix(ArrayList<String> __result) {
        __result.add("Lantern");      // 에너지 +1
        __result.add("Bag of Marbles"); // 전투 시작 시 적 약화
        __result.add("Akabeko");       // 첫 공격 데미지 +8
        return __result;
    }
}
```

### 4.2 게임 시작 시 유물 지급
```java
// PostInitializeSubscriber 활용
@Override
public void receivePostInitialize() {
    BaseMod.subscribe(new AbstractGameActionListener() {
        @Override
        public void onGameStart() {
            AbstractPlayer p = AbstractDungeon.player;

            // 레어 유물 3개 지급
            for (int i = 0; i < 3; i++) {
                AbstractRelic relic = AbstractDungeon.returnRandomRelic(
                    AbstractRelic.RelicTier.RARE
                );
                p.relics.add(relic);
                relic.instantObtain(p, p.relics.size() - 1, false);
            }
        }
    });
}
```

### 4.3 보스 유물 보상 개수 증가
```java
@SpirePatch(clz = BossRelicSelectScreen.class, method = "open")
public static class MoreBossRelicChoices {
    @SpirePrefixPatch
    public static void Prefix(ArrayList<AbstractRelic> relics) {
        // 보스 유물 5개 중 선택
        relics.clear();
        for (int i = 0; i < 5; i++) {
            AbstractRelic relic = AbstractDungeon.returnRandomRelic(
                AbstractRelic.RelicTier.BOSS
            );
            relics.add(relic);
        }
    }
}
```

### 4.4 특정 유물 자동 획득
```java
// 전투 승리 시 자동으로 유물 획득
@SpirePatch(clz = AbstractPlayer.class, method = "onVictory")
public static class RelicOnVictory {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        AbstractRelic relic = AbstractDungeon.returnRandomRelic(
            AbstractRelic.RelicTier.COMMON
        );
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(
            Settings.WIDTH / 2.0f,
            Settings.HEIGHT / 2.0f,
            relic
        );
    }
}
```

### 4.5 유물 풀 조작
```java
// 특정 유물만 등장하도록 제한
@SpirePatch(clz = AbstractDungeon.class, method = "returnRandomRelic")
public static class LimitedRelicPool {
    @SpirePrefixPatch
    public static SpireReturn<AbstractRelic> Prefix(AbstractRelic.RelicTier tier) {
        if (tier == AbstractRelic.RelicTier.RARE) {
            // 레어 유물은 항상 Dead Branch로
            return SpireReturn.Return(new DeadBranch());
        }
        return SpireReturn.Continue();
    }
}
```

## 5. 유물 효과 수정

### 5.1 유물 카운터 증가
```java
// 카운터 기반 유물 효과 강화
@SpirePatch(clz = OrnamentalFan.class, method = "onUseCard")
public static class BuffOrnamentalFan {
    @SpirePostfixPatch
    public static void Postfix(OrnamentalFan __instance, AbstractCard card) {
        if (card.type == CardType.ATTACK) {
            // 기본 1번 → 3번 트리거
            for (int i = 0; i < 2; i++) {
                __instance.flash();
                AbstractDungeon.player.addBlock(
                    __instance.counter
                );
            }
        }
    }
}
```

### 5.2 유물 트리거 확률 조정
```java
// Gambling Chip: 확률 50% → 100%
@SpirePatch(clz = GamblingChip.class, method = "onPlayerEndTurn")
public static class GuaranteedGamblingChip {
    @SpirePrefixPatch
    public static SpireReturn Prefix(GamblingChip __instance) {
        // 항상 트리거 (확률 무시)
        __instance.flash();
        AbstractDungeon.player.gainEnergy(1);
        AbstractDungeon.actionManager.addToBottom(
            new DrawCardAction(AbstractDungeon.player, 1)
        );
        return SpireReturn.Return(null);
    }
}
```

### 5.3 유물 수치 증가
```java
// Kunai: 3번 공격 → 1번 공격으로 트리거
@SpirePatch(clz = Kunai.class, method = SpirePatch.CONSTRUCTOR)
public static class FasterKunai {
    @SpirePostfixPatch
    public static void Postfix(Kunai __instance) {
        __instance.counter = -1; // 카운터 초기화
    }
}

@SpirePatch(clz = Kunai.class, method = "onUseCard")
public static class KunaiTriggerFrequency {
    @SpirePostfixPatch
    public static void Postfix(Kunai __instance, AbstractCard card) {
        if (card.type == CardType.ATTACK) {
            // 1번 공격마다 민첩 1 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player,
                    AbstractDungeon.player,
                    new DexterityPower(AbstractDungeon.player, 1),
                    1
                )
            );
        }
    }
}
```

## 6. 실용적인 수정 예시

### 6.1 모든 유물 즉시 획득
```java
public static void obtainAllRelics() {
    AbstractPlayer p = AbstractDungeon.player;

    // 모든 유물 ID 가져오기
    ArrayList<String> allRelicIDs = new ArrayList<>(
        RelicLibrary.relicIDs
    );

    for (String relicID : allRelicIDs) {
        AbstractRelic relic = RelicLibrary.getRelic(relicID).makeCopy();
        relic.instantObtain(p, p.relics.size(), false);
    }
}
```

### 6.2 유물 효과 중첩
```java
// 같은 유물 여러 개 소지 가능
@SpirePatch(clz = AbstractRelic.class, method = "instantObtain")
public static class AllowDuplicateRelics {
    @SpirePrefixPatch
    public static void Prefix(AbstractRelic __instance) {
        // Circlet 체크 비활성화
        if (__instance.relicId.equals("Circlet")) {
            return;
        }

        // 중복 유물 허용
        AbstractPlayer p = AbstractDungeon.player;
        p.relics.add(__instance);
        __instance.onEquip();
    }
}
```

### 6.3 전투 시작 시 에너지 대량 증가
```java
public class EnergyRelic extends CustomRelic {
    @Override
    public void atBattleStart() {
        AbstractDungeon.player.gainEnergy(10);
        this.flash();
    }

    @Override
    public void atTurnStart() {
        AbstractDungeon.player.gainEnergy(5);
    }
}
```

### 6.4 모든 카드 코스트 감소
```java
public class CostReductionRelic extends CustomRelic {
    @Override
    public void atBattleStart() {
        for (AbstractCard card : AbstractDungeon.player.hand.group) {
            card.modifyCostForCombat(-1);
        }
    }

    @Override
    public void onCardDraw(AbstractCard drawnCard) {
        if (drawnCard.cost > 0) {
            drawnCard.modifyCostForCombat(-1);
        }
    }
}
```

### 6.5 자동 카드 업그레이드
```java
public class AutoUpgradeRelic extends CustomRelic {
    @Override
    public void onObtainCard(AbstractCard c) {
        if (c.canUpgrade()) {
            c.upgrade();
            this.flash();
        }
    }
}
```

### 6.6 체력 회복 증가
```java
@SpirePatch(clz = AbstractPlayer.class, method = "heal")
public static class DoubleHealing {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, int healAmount) {
        if (__instance.hasRelic("Magic Flower")) {
            // Magic Flower 효과: +50% → +200%
            __instance.heal(healAmount);
        }
    }
}
```

## 7. 관련 클래스

### 7.1 AbstractRelic
```java
public abstract class AbstractRelic {
    public String relicId;
    public String name;
    public String description;
    public RelicTier tier;
    public int counter = -1;

    // 주요 메서드
    public void onEquip();
    public void onUnequip();
    public void atBattleStart();
    public void atTurnStart();
    public void onPlayerEndTurn();
    public void onVictory();
    public void onPlayCard(AbstractCard c, AbstractMonster m);
    public void onUseCard(AbstractCard card, UseCardAction action);
    public void onObtainCard(AbstractCard c);
    public void onExhaust(AbstractCard card);
    public int onPlayerGainBlock(int blockAmount);
    public int onPlayerHeal(int healAmount);
    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target);
    public int onAttacked(DamageInfo info, int damageAmount);
    public void onCardDraw(AbstractCard drawnCard);
    public void onChestOpen(boolean bossChest);
    public void onEnterRoom(AbstractRoom room);
    public void onRest();
    public void onShuffle();

    // 보상 조작
    public int changeNumberOfCardsInReward(int numberOfCards);
    public int changeRareCardRewardChance(int rareCardChance);
    public int changeUncommonCardRewardChance(int uncommonCardChance);

    // 유틸리티
    public void flash();
    public void setCounter(int counter);
    public abstract AbstractRelic makeCopy();
}
```

### 7.2 RelicLibrary
```java
public class RelicLibrary {
    public static HashMap<String, AbstractRelic> relics;
    public static ArrayList<String> relicIDs;

    public static AbstractRelic getRelic(String relicID);
    public static void add(AbstractRelic relic);
}
```

### 7.3 RelicTier (레어리티)
```java
public enum RelicTier {
    STARTER,    // 시작 유물
    COMMON,     // 일반 (흰색)
    UNCOMMON,   // 고급 (파란색)
    RARE,       // 레어 (노란색)
    BOSS,       // 보스 (빨간색)
    SPECIAL,    // 특수 (주황색)
    SHOP,       // 상점 전용
    DEPRECATED  // 사용 안 함
}
```

## 8. 주의사항

### 8.1 Circlet 메커니즘
```java
// Circlet: 모든 유물 소지 시 자동 추가
if (relicId == "Circlet" && AbstractDungeon.player.hasRelic("Circlet")) {
    AbstractRelic circ = AbstractDungeon.player.getRelic("Circlet");
    circ.counter++; // 카운터만 증가, 새 유물 추가 안 함
    circ.flash();
}
```
- 같은 유물을 다시 얻으면 일반적으로 무시됨
- Circlet만 예외로 카운터 증가

### 8.2 유물 획득 타이밍
```java
// 즉시 획득 (애니메이션 없음)
relic.instantObtain(player, slot, callOnEquip);

// 애니메이션과 함께 획득
relic.obtain();

// 특정 위치에 스폰 후 획득
AbstractDungeon.getCurrRoom().spawnRelicAndObtain(x, y, relic);
```

### 8.3 onEquip vs atBattleStart
```java
// onEquip: 유물 획득 즉시 1회 실행
public void onEquip() {
    // 영구 효과, 최대 체력 증가 등
}

// atBattleStart: 모든 전투 시작 시 실행
public void atBattleStart() {
    // 전투별 버프, 임시 효과 등
}
```

### 8.4 유물 제거 시 정리
```java
@Override
public void onUnequip() {
    // 유물 제거 시 효과 제거
    // Power 제거, 카운터 초기화 등
}
```

### 8.5 유물 카운터 사용
```java
public int counter = -1; // -1이면 카운터 표시 안 됨

// 카운터 표시
public void setCounter(int counter) {
    this.counter = counter;
}

// 카운터 기반 로직
if (this.counter >= 0) {
    this.counter++;
}
```

### 8.6 유물 효과 스택
- 대부분 유물은 중복 소지 불가 (Circlet 제외)
- 유물 효과는 자동으로 스택되지 않음
- 중복 효과를 원하면 별도 로직 필요

### 8.7 보스 유물 교체
```java
// 일부 보스 유물은 시작 유물과 교체
// 예: Black Blood는 Burning Blood 대체
if (!this.relicId.equals("HolyWater") &&
    !this.relicId.equals("Black Blood") &&
    !this.relicId.equals("Ring of the Serpent") &&
    !this.relicId.equals("FrozenCore")) {
    obtain();
}
```

### 8.8 Ascension 난이도 영향
- A14+: 엘리트 유물 풀 변경
- A15+: 보스 유물 풀 변경
- 특정 유물이 특정 난이도에서만 등장

### 8.9 유물 save/load
```java
// 커스텀 유물은 반드시 makeCopy() 구현
@Override
public AbstractRelic makeCopy() {
    return new MyCustomRelic();
}
```
