# 포션 (Potions) 수정 가이드

## 1. 포션 저장 위치

### 1.1 AbstractPlayer의 포션 필드
```java
public int potionSlots = 3;                      // 포션 슬롯 개수 (기본 3개)
public ArrayList<AbstractPotion> potions = new ArrayList<>();
```

### 1.2 포션 슬롯 시스템
- 기본 슬롯: 3개
- 최대 슬롯: 제한 없음 (UI는 5개까지 표시)
- 빈 슬롯은 `PotionSlot` 객체로 채워짐
- 포션 위치: `TopPanel.potionX + slot * Settings.POTION_W`

### 1.3 포션 저장 구조
```java
// potions ArrayList의 각 요소
public abstract class AbstractPotion {
    public String ID;
    public String name;
    public String description;
    public int slot = -1;           // 슬롯 번호 (-1: 미배치)
    public PotionRarity rarity;     // COMMON, UNCOMMON, RARE
    public PotionSize size;         // 포션 모양
    public PotionColor color;       // 포션 색상
    protected int potency = 0;      // 효과 강도
    public boolean isObtained = false;
    public boolean discarded = false;
    public boolean targetRequired = false; // 대상 지정 필요 여부
}
```

## 2. 포션이 추가되는 시점

### 2.1 전투 승리 (확률적 드롭)
```java
// PotionHelper.getPotion() - 전투 종료 시 호출
// 기본 드롭 확률: 40%
// Ascension 11+: 30%
```

**드롭 확률 계산**:
```java
public static int potionChance = 40; // 기본값

// 유물 효과로 확률 증가
if (AbstractDungeon.player.hasRelic("White Beast Statue")) {
    potionChance += 15; // +15%
}
```

**포션 드롭 확률 100%로 변경**:
```java
@SpirePatch(clz = PotionHelper.class, method = "getPotion")
public static class AlwaysDropPotion {
    @SpirePrefixPatch
    public static SpireReturn<AbstractPotion> Prefix(String name) {
        // 항상 포션 드롭
        AbstractPotion potion = PotionHelper.getRandomPotion();
        return SpireReturn.Return(potion);
    }
}
```

### 2.2 상점 구매
```java
// ShopScreen - 상점에서 포션 구매
```

**포션 가격 (레어리티별)**:
```java
public int getPrice() {
    switch (this.rarity) {
        case COMMON:   return 50;
        case UNCOMMON: return 75;
        case RARE:     return 100;
    }
    return 999;
}
```

**포션 가격 변경**:
```java
@SpirePatch(clz = AbstractPotion.class, method = "getPrice")
public static class FreePotions {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(AbstractPotion __instance) {
        return SpireReturn.Return(0); // 모든 포션 무료
    }
}
```

### 2.3 이벤트
**포션 획득 이벤트**:
- `Neow's Blessing`: 게임 시작 시 포션 선택
- `The Cleric`: 포션 5개 획득 (체력 회복 대가)
- `Duplicator`: 포션 복제
- `Fountain of Cleansing`: Curse 제거 + 포션
- `Lab`: 포션 3개 교환
- `Purifier`: Curse 제거 + 포션
- `Shining Light`: 카드 업그레이드 + 포션
- `World of Goop`: 포션 획득

**이벤트에서 포션 획득**:
```java
AbstractPotion potion = PotionHelper.getPotion("Fire Potion");
AbstractDungeon.player.obtainPotion(potion);
```

### 2.4 유물 효과
**포션 생성 유물**:
- `Potion Belt`: 포션 슬롯 +2
- `Sozu`: 포션 드롭 안 됨 (에너지 +1)
- `White Beast Statue`: 포션 드롭 확률 +15%
- `Entropic Brew`: 전투 시작 시 무작위 포션
- `Toy Ornithopter`: 전투 승리 시 5회 체력 회복 후 포션 획득

```java
// Entropic Brew 예시
@Override
public void atBattleStart() {
    this.flash();
    AbstractPotion potion = AbstractDungeon.returnRandomPotion(true);
    AbstractDungeon.player.obtainPotion(potion);
}
```

### 2.5 보물 상자
```java
// 보물상자에서 골드/카드/포션 획득
// AbstractChest.open()
```

### 2.6 카드 효과
- `Alchemize`: 무작위 포션 생성
- `Distillation`: 포션 생성 (소멸)

```java
// Alchemize 효과
AbstractPotion potion = AbstractDungeon.returnRandomPotion(true);
AbstractDungeon.player.obtainPotion(potion);
```

## 3. 포션이 사라지는 시점

### 3.1 사용
```java
// AbstractPotion.use() - 포션 사용
public abstract void use(AbstractCreature target);

// 사용 후 포션 제거
potion.use(target);
AbstractDungeon.topPanel.destroyPotion(potion.slot);
```

### 3.2 버림 (Discard)
```java
// TopPanel.destroyPotion(slot)
// 플레이어가 수동으로 포션 버리기
```

**포션 버리기 제한**:
```java
public boolean canDiscard() {
    // We Meet Again! 이벤트 중엔 버리기 불가
    if ((AbstractDungeon.getCurrRoom()).event != null &&
        (AbstractDungeon.getCurrRoom()).event instanceof WeMeetAgain) {
        return false;
    }
    return true;
}
```

### 3.3 이벤트
**포션 제거 이벤트**:
- `Augmenter`: 포션 제거 후 유물 획득
- `Duplicator`: 포션 1개 제거하고 2개로 복제
- `Lab`: 포션 3개 교환
- `We Meet Again!`: 포션 제거 불가능 (이벤트 중)

### 3.4 Fairy in a Bottle 자동 소모
```java
// Fairy in a Bottle - 사망 시 자동 사용
@Override
public boolean onPlayerDeath() {
    this.flash();
    AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth * 30 / 100);
    AbstractDungeon.topPanel.destroyPotion(this.slot);
    return true; // 부활
}
```

## 4. 포션 수정 방법

### 4.1 포션 슬롯 개수 증가
```java
@SpirePatch(clz = AbstractPlayer.class, method = SpirePatch.CONSTRUCTOR)
public static class IncreasePotionSlots {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        __instance.potionSlots = 10; // 10개 슬롯
    }
}
```

**유물로 포션 슬롯 증가**:
```java
public class ExtraPotionSlotRelic extends CustomRelic {
    @Override
    public void onEquip() {
        AbstractDungeon.player.potionSlots += 3;
    }

    @Override
    public void onUnequip() {
        AbstractDungeon.player.potionSlots -= 3;
    }
}
```

### 4.2 포션 드롭 확률 증가
```java
@SpirePatch(clz = PotionHelper.class, method = SpirePatch.STATIC_INITIALIZER)
public static class IncreasePotionDropRate {
    @SpirePostfixPatch
    public static void Postfix() {
        PotionHelper.potionChance = 100; // 100% 확률
    }
}
```

### 4.3 게임 시작 시 포션 지급
```java
@SpirePatch(clz = AbstractPlayer.class, method = "initializeStarterDeck")
public static class StartingPotions {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 시작 시 포션 5개 지급
        String[] potions = {
            "Fire Potion",
            "Block Potion",
            "Strength Potion",
            "Energy Potion",
            "Explosive Potion"
        };

        for (String potionID : potions) {
            AbstractPotion potion = PotionHelper.getPotion(potionID);
            __instance.obtainPotion(potion);
        }
    }
}
```

### 4.4 포션 효과 증가
```java
// 포션 효과량 2배
@SpirePatch(clz = AbstractPotion.class, method = "getPotency",
            paramtypez = {int.class})
public static class DoublePotionEffects {
    @SpirePostfixPatch
    public static int Postfix(int __result, AbstractPotion __instance, int ascensionLevel) {
        return __result * 2; // 효과 2배
    }
}
```

### 4.5 무제한 포션 사용
```java
// 포션 사용 후 제거하지 않음
@SpirePatch(clz = TopPanel.class, method = "destroyPotion")
public static class InfinitePotions {
    @SpirePrefixPatch
    public static SpireReturn Prefix(int slot) {
        // 포션 제거하지 않음
        return SpireReturn.Return(null);
    }
}
```

### 4.6 자동 포션 사용
```java
// 전투 시작 시 모든 포션 자동 사용
@SpirePatch(clz = AbstractPlayer.class, method = "applyStartOfCombatLogic")
public static class AutoUsePotions {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        ArrayList<AbstractPotion> potionsToUse = new ArrayList<>(__instance.potions);

        for (AbstractPotion potion : potionsToUse) {
            if (potion instanceof PotionSlot) continue;

            if (potion.targetRequired) {
                AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster();
                potion.use(target);
            } else {
                potion.use(__instance);
            }
        }
    }
}
```

## 5. 실용적인 수정 예시

### 5.1 매 턴 무작위 포션 생성
```java
public class PotionGeneratorRelic extends CustomRelic {
    @Override
    public void atTurnStart() {
        AbstractPotion potion = AbstractDungeon.returnRandomPotion(true);
        AbstractDungeon.player.obtainPotion(potion);
        this.flash();
    }
}
```

### 5.2 포션 슬롯 무제한
```java
@SpirePatch(clz = AbstractPlayer.class, method = "obtainPotion")
public static class UnlimitedPotionSlots {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, AbstractPotion potion) {
        // 슬롯 부족 시 자동 확장
        if (__instance.potions.size() >= __instance.potionSlots) {
            __instance.potionSlots = __instance.potions.size() + 1;
        }
    }
}
```

### 5.3 특정 포션만 드롭
```java
@SpirePatch(clz = PotionHelper.class, method = "getRandomPotion")
public static class OnlyFairyPotions {
    @SpirePrefixPatch
    public static SpireReturn<AbstractPotion> Prefix() {
        // 항상 Fairy in a Bottle만 드롭
        return SpireReturn.Return(new FairyPotion());
    }
}
```

### 5.4 포션 사용 시 복제
```java
@SpirePatch(clz = AbstractPotion.class, method = "use")
public static class DuplicatePotionOnUse {
    @SpirePostfixPatch
    public static void Postfix(AbstractPotion __instance) {
        // 포션 사용 시 같은 포션 1개 더 획득
        AbstractPotion copy = __instance.makeCopy();
        AbstractDungeon.player.obtainPotion(copy);
    }
}
```

### 5.5 포션 효과 전체 적용
```java
public class AoEPotionRelic extends CustomRelic {
    @Override
    public void onUsePotion() {
        // 포션 사용 시 모든 적에게 효과 적용
        ArrayList<AbstractMonster> monsters = AbstractDungeon.getMonsters().monsters;
        for (AbstractMonster m : monsters) {
            if (!m.isDeadOrEscaped()) {
                // 추가 효과 적용
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(m, AbstractDungeon.player,
                        new WeakPower(m, 2, false), 2)
                );
            }
        }
        this.flash();
    }
}
```

### 5.6 포션 가격 무료화
```java
@SpirePatch(clz = AbstractPotion.class, method = "getPrice")
public static class FreePotionsInShop {
    @SpirePrefixPatch
    public static SpireReturn<Integer> Prefix(AbstractPotion __instance) {
        return SpireReturn.Return(0);
    }
}
```

### 5.7 전투 종료 시 포션 복구
```java
@SpirePatch(clz = AbstractPlayer.class, method = "onVictory")
public static class RefillPotionsAfterCombat {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 빈 슬롯을 무작위 포션으로 채움
        while (__instance.potions.size() < __instance.potionSlots) {
            AbstractPotion potion = AbstractDungeon.returnRandomPotion(true);
            __instance.obtainPotion(potion);
        }
    }
}
```

## 6. 관련 클래스

### 6.1 AbstractPotion
```java
public abstract class AbstractPotion {
    public String ID;
    public String name;
    public String description;
    public int slot = -1;
    public PotionRarity rarity;
    public PotionSize size;
    public PotionColor color;
    protected int potency = 0;
    public boolean targetRequired = false;

    // 주요 메서드
    public abstract void use(AbstractCreature target);
    public abstract int getPotency(int ascensionLevel);
    public boolean canUse();
    public boolean canDiscard();
    public int getPrice();
    public void initializeData();
    public boolean onPlayerDeath(); // Fairy Potion 등

    // 유틸리티
    public void setAsObtained(int potionSlot);
    public void adjustPosition(int slot);
    public abstract AbstractPotion makeCopy();
}
```

### 6.2 PotionHelper
```java
public class PotionHelper {
    public static int potionChance = 40; // 드롭 확률
    public static ArrayList<String> potions; // 포션 풀

    public static AbstractPotion getPotion(String name);
    public static AbstractPotion getRandomPotion();
    public static AbstractPotion getRandomPotion(boolean anyRarity);
}
```

### 6.3 PotionRarity
```java
public enum PotionRarity {
    PLACEHOLDER,  // 빈 슬롯
    COMMON,       // 일반 (40%)
    UNCOMMON,     // 고급 (40%)
    RARE          // 레어 (20%)
}
```

### 6.4 PotionSize (포션 모양)
```java
public enum PotionSize {
    T,      // 작은 원뿔형
    S,      // 작은 둥근형
    M,      // 중간 크기
    SPHERE, // 구형
    H,      // 높은 원통형
    BOTTLE, // 병 모양
    HEART,  // 하트 모양
    SNECKO, // 스네코 모양
    FAIRY,  // 요정 모양
    GHOST,  // 유령 모양
    JAR,    // 항아리
    BOLT,   // 번개 모양
    CARD,   // 카드 모양
    MOON,   // 달 모양
    SPIKY,  // 가시 모양
    EYE,    // 눈 모양
    ANVIL   // 모루 모양
}
```

### 6.5 PotionColor
```java
public enum PotionColor {
    POISON,     // 독 (녹색)
    BLUE,       // 파란색
    FIRE,       // 불 (빨간색)
    GREEN,      // 초록색
    EXPLOSIVE,  // 폭발 (주황색)
    WEAK,       // 약화 (회색)
    FEAR,       // 공포 (검은색)
    STRENGTH,   // 힘 (빨간색)
    WHITE,      // 흰색
    FAIRY,      // 요정 (분홍색)
    ANCIENT,    // 고대 (보라색)
    ELIXIR,     // 엘릭서 (금색)
    NONE,       // 없음
    ENERGY,     // 에너지 (청록색)
    SWIFT,      // 신속 (주황색)
    FRUIT,      // 과일 (초록색)
    SNECKO,     // 스네코 (파란색)
    SMOKE,      // 연기 (회색)
    STEROID,    // 스테로이드 (빨간색)
    SKILL,      // 스킬 (초록색)
    ATTACK,     // 공격 (빨간색)
    POWER       // 파워 (파란색)
}
```

## 7. 주의사항

### 7.1 포션 슬롯과 ArrayList 관리
```java
// 포션 슬롯 개수와 potions ArrayList 크기는 독립적
// potionSlots: UI 슬롯 개수
// potions.size(): 실제 포션 개수

// 빈 슬롯은 PotionSlot 객체로 채워짐
if (potion instanceof PotionSlot) {
    // 빈 슬롯
}
```

### 7.2 포션 획득 방법
```java
// 올바른 포션 획득
AbstractPlayer p = AbstractDungeon.player;
AbstractPotion potion = new FirePotion();
p.obtainPotion(potion); // 자동으로 빈 슬롯에 배치

// 잘못된 방법 (슬롯 관리 안 됨)
p.potions.add(potion); // ❌
```

### 7.3 대상 지정 포션
```java
public boolean targetRequired = false;

// 대상 지정 필요 (Fire Potion 등)
if (potion.targetRequired) {
    AbstractMonster target = AbstractDungeon.getMonsters().getRandomMonster();
    potion.use(target);
} else {
    // 자가 사용 (Block Potion 등)
    potion.use(AbstractDungeon.player);
}
```

### 7.4 포션 효과량 (potency)
```java
public int getPotency() {
    int potency = getPotency(AbstractDungeon.ascensionLevel);

    // Sacred Bark 유물: 포션 효과 2배
    if (AbstractDungeon.player != null &&
        AbstractDungeon.player.hasRelic("SacredBark")) {
        potency *= 2;
    }
    return potency;
}
```

### 7.5 Ascension 난이도 영향
```java
// 포션 효과량은 Ascension 레벨에 따라 변함
@Override
public int getPotency(int ascensionLevel) {
    if (ascensionLevel >= 11) {
        return 10; // A11+ 효과 감소
    }
    return 15; // 기본 효과
}
```

### 7.6 포션 사용 조건
```java
@Override
public boolean canUse() {
    // 전투 중에만 사용 가능
    if (AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
        return false;
    }

    // 적이 모두 죽었으면 사용 불가
    if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
        return false;
    }

    // 턴이 종료되었으면 사용 불가
    if (AbstractDungeon.actionManager.turnHasEnded) {
        return false;
    }

    return true;
}
```

### 7.7 포션 드롭 확률
```java
// 기본 확률: 40%
// Ascension 11+: 30%

// 확률 증가 유물
// White Beast Statue: +15%
// Bottled Flame/Lightning/Tornado: 특정 카드 보관 (포션 슬롯 -1)
```

### 7.8 Sozu 유물 충돌
```java
// Sozu 유물: 포션 드롭 안 됨, 에너지 +1
if (AbstractDungeon.player.hasRelic("Sozu")) {
    // 포션 드롭하지 않음
    return null;
}
```

### 7.9 포션 복제
```java
// 올바른 포션 복사
AbstractPotion copy = potion.makeCopy();

// 잘못된 방법 (참조만 복사)
AbstractPotion wrong = potion; // ❌
```

### 7.10 커스텀 포션 생성
```java
public class MyCustomPotion extends CustomPotion {
    public static final String ID = "MyMod:MyPotion";
    public static final String NAME = "My Potion";

    public MyCustomPotion() {
        super(
            NAME,
            ID,
            PotionRarity.RARE,
            PotionSize.BOTTLE,
            PotionColor.FIRE
        );
        this.potency = getPotency();
    }

    @Override
    public void use(AbstractCreature target) {
        // 포션 효과 구현
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                target,
                new DamageInfo(AbstractDungeon.player, this.potency, DamageInfo.DamageType.THORNS),
                AbstractGameAction.AttackEffect.FIRE
            )
        );
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 25; // 효과량
    }

    @Override
    public AbstractPotion makeCopy() {
        return new MyCustomPotion();
    }

    @Override
    public void initializeData() {
        this.description = "Deal " + this.potency + " damage.";
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }
}
```

### 7.11 포션 레어리티 확률
```java
// 무작위 포션 획득 시 레어리티 확률
// COMMON: 40%
// UNCOMMON: 40%
// RARE: 20%

// 변경 방법
@SpirePatch(clz = AbstractDungeon.class, method = "returnRandomPotion",
            paramtypez = {PotionRarity.class, boolean.class})
public static class AlwaysRarePotions {
    @SpirePrefixPatch
    public static SpireReturn<AbstractPotion> Prefix(PotionRarity rarity, boolean limited) {
        // 항상 레어 포션
        return SpireReturn.Return(
            PotionHelper.getRandomPotion(PotionRarity.RARE)
        );
    }
}
```
