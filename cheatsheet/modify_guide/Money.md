# 골드 (Gold) 수정 가이드

## 1. 골드 저장 위치

**핵심 변수:**
- `AbstractDungeon.player.gold` (현재 골드)
- **클래스:** `com.megacrit.cardcrawl.characters.AbstractPlayer`
- **상속 원본:** `com.megacrit.cardcrawl.core.AbstractCreature` (Line 62-63)
  ```java
  public int gold;           // 현재 골드
  public int displayGold;    // UI 표시용 골드 (애니메이션)
  ```
- **초기값:** 99 (모든 캐릭터 공통)
  - Ironclad: Line 172
  - Silent: Line 136
  - Defect: Line 131
  - Watcher: Line 172

---

## 2. 골드가 증가하는 모든 시점

### 2.1 전투 승리 보상
**클래스:** `com.megacrit.cardcrawl.rewards.RewardItem`
**생성자:** `RewardItem(int gold)` (Line 103-107)

**기본 수치 계산:**
`com.megacrit.cardcrawl.rooms.AbstractRoom` 클래스에서 결정
- **일반 몬스터:** 10-20 골드 (Line 412-414)
  ```java
  addGoldToRewards(15);  // Floor 1
  addGoldToRewards(AbstractDungeon.treasureRng.random(10, 20));  // Other floors
  ```
- **엘리트 몬스터:** 25-35 골드 (Line 401-403)
  ```java
  addGoldToRewards(30);  // Floor 1
  addGoldToRewards(AbstractDungeon.treasureRng.random(25, 35));  // Other floors
  ```
- **보스:** 100 골드 (Line 360)
  ```java
  addGoldToRewards(100);
  ```

**골드 보너스 적용 (Line 115-142):**
1. **Golden Idol 유물**: +25% (0.25 배율, Line 123-125)
2. **Midas 모드**: +200% (2.0 배율, Line 127-129)
3. **MonsterHunter 모드**: +150% (1.5 배율, Line 131-133)
4. **보너스는 Treasure Room 제외**

### 2.2 이벤트
**클래스:** `com.megacrit.cardcrawl.events.*`

**골드 획득 이벤트 예시:**
- **Golden Wing** (`events/exordium/GoldenWing.java`)
- **Golden Idol Event** (`events/exordium/GoldenIdolEvent.java`)
- **기타 선택지 기반 이벤트들**

### 2.3 유물
**골드 획득 유물:**
1. **Golden Idol** (`relics/GoldenIdol.java`)
   - 효과: 전투 승리 시 골드 +25%
   - 배율: 0.25F (Line 6)

**관련 유물:**
- **Bloody Idol**: 엘리트 처치 시 골드 보너스
- **Old Coin**: 시작 골드 증가
- **Ectoplasm**: 골드 획득 불가 (전투 승리 시 골드 0으로 변경)

### 2.4 카드
**현재 기본 게임에 직접 골드 획득 카드 없음**
- 카드로 골드를 얻는 효과는 특수 이벤트나 모드에서만 존재

### 2.5 기타
- **도둑질 보상:** `RewardItem(int gold, boolean theft)` (Line 109-113)
  - RewardType.STOLEN_GOLD
- **상점 도둑질:** 일부 이벤트에서 가능
- **Treasure Room:** 보물 상자 (별도 계산)

---

## 3. 골드가 감소하는 모든 시점

### 3.1 상점 구매
**클래스:** `com.megacrit.cardcrawl.shop.ShopScreen`
- **카드:** 가격 변동 (희귀도별 차등)
- **유물:** 가격 변동 (희귀도별 차등)
- **포션:** 가격 변동
- **Card Removal (카드 제거):** 초기 75 골드, +25 씩 증가 (Line 101-102, 250-251)
  ```java
  public static int purgeCost = 75;
  public static int actualPurgeCost = 75;
  private static final int PURGE_COST_RAMP = 25;
  ```

### 3.2 이벤트 비용
- **Cleric** (`events/exordium/Cleric.java`): 힐링 비용
- **Designer** (`events/shrines/Designer.java`): 카드 업그레이드/변환 비용
- **WomanInBlue** (`events/shrines/WomanInBlue.java`): 포션 구매
- **기타 선택지 기반 이벤트들**

### 3.3 카드 제거
- **상점 카드 제거:** 75 → 100 → 125 → ... (25씩 증가)
- **특정 이벤트:** 고정 비용 또는 무료

### 3.4 기타
- **Neow Bonus 선택지:** 일부 보상 선택 시 골드 차감
- **랜덤 이벤트:** 선택지에 따라 골드 차감

---

## 4. 골드 수정 방법

### 4.1 전투 승리 골드 증가

**목표:** 전투 승리 시 골드 2배

```java
@SpirePatch(
    clz = RewardItem.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { int.class }
)
public static class GoldRewardPatch {
    @SpirePostfixPatch
    public static void Postfix(RewardItem __instance, int gold) {
        if (__instance.type == RewardItem.RewardType.GOLD) {
            __instance.goldAmt *= 2;
            // bonusGold도 함께 업데이트
            __instance.applyGoldBonus(false);
        }
    }
}
```

**주의:** `applyGoldBonus`는 private 메서드이므로 Reflection 사용 필요 또는 직접 계산 필요

**대안 - 골드 획득 시점 패치:**
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldGainMultiplierPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance, @ByRef int[] amount) {
        if (__instance.isPlayer) {
            amount[0] = amount[0] * 2;  // 2배 배율
        }
        return SpireReturn.Continue();
    }
}
```

### 4.2 특정 몬스터 처치 시 보너스 골드

**목표:** 엘리트 처치 시 추가 100 골드

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addGoldToRewards",
    paramtypez = { int.class }
)
public static class EliteGoldBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance, int gold) {
        if (__instance instanceof MonsterRoomElite) {
            // 추가 골드 보상 추가
            __instance.addGoldToRewards(100);
        }
    }
}
```

**더 나은 방법 - 보상 생성 후 수정:**
```java
@SpirePatch(
    clz = MonsterRoomElite.class,
    method = "dropReward"
)
public static class EliteGoldBonusPostDropPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomElite __instance) {
        // 이미 생성된 보상 중 골드 찾아서 증가
        for (RewardItem reward : AbstractDungeon.getCurrRoom().rewards) {
            if (reward.type == RewardItem.RewardType.GOLD) {
                reward.goldAmt += 100;
                reward.incrementGold(0);  // UI 갱신
            }
        }
    }
}
```

### 4.3 시작 골드 변경

**목표:** 시작 시 500 골드

**방법 1 - CharSelectInfo 수정:**
```java
@SpirePatch(
    clz = CharSelectInfo.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class StartingGoldPatch {
    @SpireInsertPatch(
        locator = GoldLocator.class
    )
    public static void Insert(@ByRef int[] gold) {
        gold[0] = 500;  // 시작 골드 500으로 변경
    }

    private static class GoldLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher matcher = new Matcher.FieldAccessMatcher(
                CharSelectInfo.class, "gold"
            );
            return LineFinder.findInOrder(ctMethodToPatch, matcher);
        }
    }
}
```

**방법 2 - 캐릭터별 getLoadout 수정:**
```java
@SpirePatch(
    clz = Ironclad.class,
    method = "getLoadout"
)
public static class IroncladStartGoldPatch {
    @SpirePostfixPatch
    public static CharSelectInfo Postfix(CharSelectInfo result) {
        result.gold = 500;
        return result;
    }
}

// Silent, Defect, Watcher에도 동일 패치 적용
```

### 4.4 골드 획득량 전역 배율

**목표:** 모든 골드 획득 1.5배

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldMultiplierPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance, @ByRef int[] amount) {
        if (__instance.isPlayer) {
            amount[0] = (int)(amount[0] * 1.5f);
        }
        return SpireReturn.Continue();
    }
}
```

**대안 - GainGoldAction 패치:**
```java
@SpirePatch(
    clz = GainGoldAction.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { int.class }
)
public static class GainGoldActionPatch {
    @SpirePostfixPatch
    public static void Postfix(GainGoldAction __instance, @ByRef int[] amount) {
        amount[0] = (int)(amount[0] * 1.5f);
        __instance.amount = amount[0];
    }
}
```

### 4.5 특정 난이도에서 골드 감소

**목표:** A25+ 에서 전투 골드 50% 감소

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "addGoldToRewards",
    paramtypez = { int.class }
)
public static class AscensionGoldNerfPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractRoom __instance, @ByRef int[] gold) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            gold[0] = (int)(gold[0] * 0.5f);
        }
        return SpireReturn.Continue();
    }
}
```

### 4.6 카드 제거 비용 수정

**목표:** 카드 제거 비용 무료

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "init"
)
public static class FreeCardRemovalPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        ShopScreen.purgeCost = 0;
        ShopScreen.actualPurgeCost = 0;
    }
}
```

**목표:** 카드 제거 비용 고정 (증가 없음)

```java
@SpirePatch(
    clz = ShopScreen.class,
    method = "purchasePurge"
)
public static class FixedPurgeCostPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        // 제거 후 비용을 다시 원래대로 되돌림
        ShopScreen.purgeCost = 75;
        ShopScreen.actualPurgeCost = 75;
    }
}
```

---

## 5. 관련 클래스 및 메서드

### 주요 클래스
- **`AbstractCreature`**: gold 필드 소유 (Line 62-63)
- **`AbstractPlayer`**: 플레이어 골드 관리
- **`RewardItem`**: 보상 골드 관리 (Line 59-60: goldAmt, bonusGold)
- **`AbstractRoom`**: 방 보상 생성 (Line 801: addGoldToRewards)
- **`GainGoldAction`**: 골드 획득 액션

### 주요 메서드

**AbstractCreature (Line 932-950):**
```java
public void loseGold(int goldAmount)  // Line 932
public void gainGold(int amount)      // Line 944
```

**AbstractRoom (Line 801):**
```java
public void addGoldToRewards(int gold)
```

**RewardItem:**
```java
public RewardItem(int gold)                    // Line 103 - 일반 골드 보상
public RewardItem(int gold, boolean theft)     // Line 109 - 도둑질 골드
public void incrementGold(int gold)            // Line 187 - 골드 증가
private void applyGoldBonus(boolean theft)     // Line 115 - 보너스 계산
public boolean claimReward()                   // Line 289 - 보상 획득 (Line 292-306)
```

**GainGoldAction:**
```java
public void update()  // Line 12-15 - AbstractDungeon.player.gainGold 호출
```

---

## 6. 실용적인 수정 예시

### 예시 1: 보스 처치 보너스 골드
**목표:** 보스 처치 시 추가 300 골드

```java
@SpirePatch(
    clz = MonsterRoomBoss.class,
    method = "dropReward"
)
public static class BossGoldBonusPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterRoomBoss __instance) {
        AbstractDungeon.getCurrRoom().addGoldToRewards(300);
    }
}
```

### 예시 2: 골드 드롭 확률 100%
**목표:** 모든 전투에서 항상 골드 드롭

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "dropReward"
)
public static class AlwaysDropGoldPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        boolean hasGold = false;
        for (RewardItem reward : __instance.rewards) {
            if (reward.type == RewardItem.RewardType.GOLD) {
                hasGold = true;
                break;
            }
        }

        if (!hasGold && __instance.monsters != null && !__instance.monsters.areMonstersBasicallyDead()) {
            // 골드 보상이 없으면 추가
            __instance.addGoldToRewards(AbstractDungeon.treasureRng.random(10, 20));
        }
    }
}
```

### 예시 3: 골드 상한/하한 설정
**목표:** 최소 0, 최대 999999

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldCapPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance) {
        if (__instance.isPlayer) {
            if (__instance.gold > 999999) {
                __instance.gold = 999999;
            }
            if (__instance.gold < 0) {
                __instance.gold = 0;
            }
        }
    }
}
```

### 예시 4: 골드 기반 난이도 조정
**목표:** 골드가 많을수록 적 강화

```java
@SpirePatch(
    clz = AbstractMonster.class,
    method = "applyStartOfTurnPowers"
)
public static class GoldBasedDifficultyPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractMonster __instance) {
        int playerGold = AbstractDungeon.player.gold;

        if (playerGold > 500) {
            // 500 골드 이상일 때 적 체력/공격력 강화
            float multiplier = 1.0f + ((playerGold - 500) / 1000.0f);
            multiplier = Math.min(multiplier, 2.0f);  // 최대 2배

            __instance.currentHealth = (int)(__instance.currentHealth * multiplier);
            __instance.maxHealth = (int)(__instance.maxHealth * multiplier);
        }
    }
}
```

### 예시 5: 골드 획득 시 UI 효과 추가
**목표:** 대량 골드 획득 시 특수 효과

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldEffectPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance.isPlayer && amount >= 100) {
            // 100 골드 이상 획득 시 특수 효과
            AbstractDungeon.effectList.add(
                new TextAboveCreatureEffect(
                    __instance.hb.cX,
                    __instance.hb.cY,
                    "BIG MONEY!",
                    Settings.GOLD_COLOR
                )
            );
            CardCrawlGame.sound.play("GOLD_JINGLE");
        }
    }
}
```

---

## 7. 주의사항

### 7.1 음수 골드 방지
- `loseGold()` 메서드는 자동으로 0 이하 방지 (AbstractCreature Line 935-937)
  ```java
  if (this.gold < 0) {
      this.gold = 0;
  }
  ```

### 7.2 저장/로드
- 골드는 `AbstractPlayer.gold` 필드에 저장되며 자동으로 세이브 파일에 기록됨
- 수정 시 저장/로드 호환성 유지 필요

### 7.3 UI 업데이트
- 골드 변경 시 `displayGold` 필드가 자동으로 애니메이션 처리
- 직접 `gold` 필드 수정 시 UI가 즉시 반영됨

### 7.4 이벤트 연계
- 일부 이벤트는 골드 보유량 기반으로 선택지 제공 (예: 골드가 충분하지 않으면 선택 불가)
- 골드 감소 로직 수정 시 이벤트 호환성 확인 필요

### 7.5 유물 상호작용
- **Golden Idol**: RewardItem 생성 시 bonusGold 계산 (Line 123-125)
- **Ectoplasm**: 전투 보상 골드를 0으로 만드는 유물 (별도 패치 필요)
- **Bloody Idol**: 엘리트 처치 시 추가 골드

### 7.6 Ascension 차이
- 기본 게임에서는 Ascension 레벨에 따른 골드 변화 없음
- 커스텀 Ascension 효과 추가 시 `AbstractDungeon.ascensionLevel` 체크 필요

---

## 8. 디버깅 팁

### 8.1 콘솔 로그로 골드 추적
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldDebugPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance.isPlayer) {
            System.out.println("[GOLD] Gained: " + amount + " | Total: " + __instance.gold);
        }
    }
}
```

### 8.2 골드 변화 시점 breakpoint
- `AbstractCreature.gainGold()` (Line 944)
- `AbstractCreature.loseGold()` (Line 932)
- `RewardItem.claimReward()` (Line 289-306)

### 8.3 RewardItem 생성 시점 확인
```java
@SpirePatch(
    clz = RewardItem.class,
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { int.class }
)
public static class RewardDebugPatch {
    @SpirePostfixPatch
    public static void Postfix(RewardItem __instance, int gold) {
        System.out.println("[REWARD] Gold Created: " + gold +
                           " | With Bonus: " + __instance.bonusGold);
    }
}
```

### 8.4 상점 골드 차감 확인
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "loseGold"
)
public static class LoseGoldDebugPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, int goldAmount) {
        if (__instance.isPlayer) {
            System.out.println("[GOLD] Losing: " + goldAmount +
                               " | Before: " + __instance.gold +
                               " | After: " + (__instance.gold - goldAmount));

            // 스택 트레이스로 호출 경로 확인
            Thread.dumpStack();
        }
    }
}
```

---

## 9. 고급 패치 기법

### 9.1 골드 배율 시스템 구현
```java
public class GoldMultiplierManager {
    public static float globalMultiplier = 1.0f;

    public static void setMultiplier(float mult) {
        globalMultiplier = Math.max(0.1f, Math.min(mult, 10.0f));
    }

    public static int applyMultiplier(int baseAmount) {
        return Math.round(baseAmount * globalMultiplier);
    }
}

@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class DynamicGoldMultiplierPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance, @ByRef int[] amount) {
        if (__instance.isPlayer) {
            amount[0] = GoldMultiplierManager.applyMultiplier(amount[0]);
        }
        return SpireReturn.Continue();
    }
}
```

### 9.2 골드 획득 이벤트 리스너
```java
public interface GoldGainListener {
    void onGoldGained(int amount, int totalGold);
}

public class GoldEventManager {
    private static List<GoldGainListener> listeners = new ArrayList<>();

    public static void registerListener(GoldGainListener listener) {
        listeners.add(listener);
    }

    public static void notifyGoldGained(int amount, int total) {
        for (GoldGainListener listener : listeners) {
            listener.onGoldGained(amount, total);
        }
    }
}

@SpirePatch(
    clz = AbstractCreature.class,
    method = "gainGold"
)
public static class GoldEventPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance.isPlayer) {
            GoldEventManager.notifyGoldGained(amount, __instance.gold);
        }
    }
}
```

### 9.3 골드 획득 통계 추적
```java
public class GoldStatistics {
    public static int totalGoldGained = 0;
    public static int totalGoldSpent = 0;
    public static Map<String, Integer> goldSourceMap = new HashMap<>();

    public static void recordGain(int amount, String source) {
        totalGoldGained += amount;
        goldSourceMap.put(source, goldSourceMap.getOrDefault(source, 0) + amount);
    }

    public static void recordSpend(int amount) {
        totalGoldSpent += amount;
    }

    public static void printReport() {
        System.out.println("=== Gold Report ===");
        System.out.println("Total Gained: " + totalGoldGained);
        System.out.println("Total Spent: " + totalGoldSpent);
        System.out.println("Net Gold: " + (totalGoldGained - totalGoldSpent));
        System.out.println("\nSources:");
        for (Map.Entry<String, Integer> entry : goldSourceMap.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
```

---

## 10. 참고 자료

### 주요 파일 위치
- `com/megacrit/cardcrawl/core/AbstractCreature.java` - Line 62-63 (gold 필드), Line 932-950 (gainGold/loseGold)
- `com/megacrit/cardcrawl/rewards/RewardItem.java` - Line 103-142 (골드 보상 생성)
- `com/megacrit/cardcrawl/rooms/AbstractRoom.java` - Line 360-414 (전투 골드 계산), Line 801 (addGoldToRewards)
- `com/megacrit/cardcrawl/actions/common/GainGoldAction.java` - Line 12-15 (골드 획득 액션)
- `com/megacrit/cardcrawl/relics/GoldenIdol.java` - Line 6 (25% 보너스)
- `com/megacrit/cardcrawl/daily/mods/Midas.java` - Line 11 (200% 보너스)
- `com/megacrit/cardcrawl/shop/ShopScreen.java` - Line 101-102 (카드 제거 비용)
- `com/megacrit/cardcrawl/screens/CharSelectInfo.java` - Line 38, 60 (시작 골드)

### 골드 관련 상수
- **시작 골드:** 99 (모든 캐릭터 공통)
- **카드 제거 비용:** 75 → 100 → 125 ... (+25씩)
- **일반 전투:** 10-20 골드
- **엘리트 전투:** 25-35 골드
- **보스 전투:** 100 골드
- **Golden Idol 배율:** 0.25 (25%)
- **Midas 모드 배율:** 2.0 (200%)
