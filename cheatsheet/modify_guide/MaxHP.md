# 최대 체력 (Max HP) 수정 가이드

## 1. 최대 체력 저장 위치

### 1.1 AbstractCreature.java
```java
public int maxHealth;  // 현재 최대 체력
```

### 1.2 AbstractPlayer.java
```java
public int startingMaxHP;  // 시작 최대 체력 (게임 시작 시 기록)
```

**초기화 시점:**
```java
// AbstractPlayer.initializeClass() - Line 385~387
this.maxHealth = info.maxHp;           // CharSelectInfo에서 최대 체력 설정
this.startingMaxHP = this.maxHealth;    // 시작 값 기록
this.currentHealth = info.currentHp;    // 현재 체력 설정
```

### 1.3 캐릭터별 초기 최대 체력
- **Ironclad (전사)**: 80 HP
- **Silent (암살자)**: 70 HP
- **Defect (결함자)**: 75 HP
- **Watcher (관찰자)**: 72 HP

---

## 2. 최대 체력이 증가하는 시점

### 2.1 보스 처치 보상 (캠프파이어 아님!)
**위치:** 보스 처치 후 보상 선택 화면
**증가량:** 기본 +5~8 HP (난이도/이벤트에 따라 다름)

### 2.2 이벤트
- **신전 이벤트 (Shrine)**: 최대 체력 +3~10
- **업그레이드 신전**: 최대 체력 +10
- **기타 이벤트**: 다양한 최대 체력 증가 선택지

### 2.3 유물
- **Tiny Chest**: 최대 체력 +10
- **Bloody Idol**: 최대 체력 +5
- **Magic Flower**: 최대 체력 +5
- **Waffle**: 최대 체력 +7
- **Mango**: 최대 체력 +14

### 2.4 카드
- **Feed (전사)**: 적 처치 시 최대 체력 +3
- **Reaper (전사)**: 업그레이드 시 최대 체력 +1

### 2.5 포션
- **Fruit Juice**: 최대 체력 +5
- **Entropic Brew**: 최대 체력 +5

---

## 3. 최대 체력이 감소하는 시점

### 3.1 이벤트 (체력 희생)
- **악마와의 거래 (Devil)**: 최대 체력 -10% ~ -20%
- **기타 이벤트**: 선택지에 따라 최대 체력 감소

### 3.2 특정 유물
- **Mark of Pain**: 최대 체력 -1 (에너지 +1)
- **Runic Dome**: 최대 체력 변동 없음 (정보 차단)

### 3.3 난이도 패널티
- **Ascension 14+**: 시작 최대 체력 -10%
- **Ascension 25**: 체력 관련 추가 페널티

---

## 4. 최대 체력 수정 방법

### 4.1 시작 최대 체력 변경

**CharSelectInfo 패치:**
```java
@SpirePatch(
    clz = CharSelectInfo.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class StartingMaxHPPatch {
    @SpireInsertPatch(rloc = 0)
    public static void Insert(CharSelectInfo __instance,
                              String name, String flavorText,
                              int maxHp, int currentHp,
                              int maxOrbs, int startingGold,
                              int cardDraw, AbstractPlayer.PlayerClass newClass) {
        // 시작 최대 체력 2배
        ReflectionHacks.setPrivate(__instance, CharSelectInfo.class, "maxHp", maxHp * 2);
        ReflectionHacks.setPrivate(__instance, CharSelectInfo.class, "currentHp", currentHp * 2);
    }
}
```

### 4.2 보스 보상 최대 체력 증가량 변경

**BossChestReward 패치:**
```java
@SpirePatch(
    clz = BossChest.class,
    method = "open"
)
public static class BossMaxHPRewardPatch {
    @SpirePostfixPatch
    public static void Postfix(BossChest __instance, boolean bossChest) {
        if (bossChest) {
            // 보스 보상 체력 증가량 +5 추가
            AbstractDungeon.player.increaseMaxHp(5, true);
        }
    }
}
```

### 4.3 특정 난이도에서 시작 최대 체력 감소

**Ascension 패치 (A14+ 체력 감소):**
```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "initializeClass"
)
public static class AscensionMaxHPPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance,
                                String imgUrl, String shoulder2ImgUrl,
                                String shouldImgUrl, String corpseImgUrl,
                                CharSelectInfo info, float hb_x, float hb_y,
                                float hb_w, float hb_h, EnergyManager energy) {
        if (AbstractDungeon.ascensionLevel >= 14) {
            // A14+에서 최대 체력 -10
            __instance.decreaseMaxHealth(10);
        }
    }
}
```

### 4.4 최대 체력 증가 전역 배율

**increaseMaxHp 패치:**
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "increaseMaxHp"
)
public static class MaxHPIncreaseMultiplierPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance,
                                            int amount, boolean showEffect) {
        if (__instance instanceof AbstractPlayer) {
            // 모든 최대 체력 증가량 2배
            int newAmount = amount * 2;

            // 원본 로직 실행
            if (!Settings.isEndless || !AbstractDungeon.player.hasBlight("FullBelly")) {
                __instance.maxHealth += newAmount;
                AbstractDungeon.effectsQueue.add(
                    new TextAboveCreatureEffect(
                        __instance.hb.cX - __instance.animX,
                        __instance.hb.cY,
                        AbstractCreature.TEXT[2] + Integer.toString(newAmount),
                        Settings.GREEN_TEXT_COLOR
                    )
                );
                __instance.heal(newAmount, true);
                __instance.healthBarUpdatedEvent();
            }

            return SpireReturn.Return();  // 원본 메서드 실행 안 함
        }
        return SpireReturn.Continue();  // 몬스터는 원본 메서드 실행
    }
}
```

### 4.5 최대 체력 상한/하한 설정

**최대 체력 상한 설정:**
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "increaseMaxHp"
)
public static class MaxHPCapPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance,
                                int amount, boolean showEffect) {
        if (__instance instanceof AbstractPlayer) {
            // 최대 체력 상한 999
            if (__instance.maxHealth > 999) {
                __instance.maxHealth = 999;
                if (__instance.currentHealth > 999) {
                    __instance.currentHealth = 999;
                }
                __instance.healthBarUpdatedEvent();
            }
        }
    }
}
```

**최대 체력 하한 설정:**
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "decreaseMaxHealth"
)
public static class MaxHPMinimumPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance instanceof AbstractPlayer) {
            // 최대 체력 하한 10
            if (__instance.maxHealth < 10) {
                __instance.maxHealth = 10;
                if (__instance.currentHealth < 10) {
                    __instance.currentHealth = 10;
                }
                __instance.healthBarUpdatedEvent();
            }
        }
    }
}
```

---

## 5. 관련 메서드

### 5.1 increaseMaxHp(int amount, boolean showEffect)
**위치:** AbstractCreature.java Line 202~220

**동작:**
1. `maxHealth += amount` (최대 체력 증가)
2. 화면에 "+X HP" 효과 표시 (showEffect = true)
3. `heal(amount, true)` 호출 → **현재 체력도 함께 증가**
4. `healthBarUpdatedEvent()` 호출 → 체력바 갱신

**중요:** `increaseMaxHp()`는 **현재 체력도 함께 증가**시킵니다!

### 5.2 decreaseMaxHealth(int amount)
**위치:** AbstractCreature.java Line 222~235

**동작:**
1. `maxHealth -= amount` (최대 체력 감소)
2. 최소값 검증: `if (maxHealth <= 1) maxHealth = 1`
3. 현재 체력 조정: `if (currentHealth > maxHealth) currentHealth = maxHealth`
4. `healthBarUpdatedEvent()` 호출 → 체력바 갱신

**중요:** 최대 체력이 감소하면 **현재 체력도 자동 조정**됩니다!

---

## 6. 실용적인 수정 예시

### 예시 1: 시작 체력 2배
```java
@SpirePatch(clz = AbstractPlayer.class, method = "initializeClass")
public static class DoubleStartingHPPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, CharSelectInfo info) {
        int extraHP = __instance.startingMaxHP;  // 현재 최대 체력만큼 추가
        __instance.increaseMaxHp(extraHP, false);
    }
}
```

### 예시 2: 보스 보상 체력 증가량 +5
```java
@SpirePatch(clz = BossChest.class, method = "open")
public static class BetterBossHPReward {
    @SpirePostfixPatch
    public static void Postfix(BossChest __instance, boolean bossChest) {
        if (bossChest) {
            AbstractDungeon.player.increaseMaxHp(5, true);
        }
    }
}
```

### 예시 3: A25+ 시작 체력 -10
```java
@SpirePatch(clz = AbstractPlayer.class, method = "initializeClass")
public static class A25HPPenalty {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            __instance.decreaseMaxHealth(10);
        }
    }
}
```

### 예시 4: 전투 승리 시 최대 체력 +1
```java
@SpirePatch(clz = AbstractRoom.class, method = "endBattle")
public static class BattleVictoryMaxHP {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        if (__instance instanceof MonsterRoom && !AbstractDungeon.player.isDead) {
            AbstractDungeon.player.increaseMaxHp(1, true);
        }
    }
}
```

---

## 7. CurrentHP와의 연동

### 7.1 최대 체력 증가 시 현재 체력도 증가
```java
// increaseMaxHp() 내부 동작 (Line 217)
heal(amount, true);  // 현재 체력 += amount
```
**결과:** 최대 체력 +10 → 현재 체력 +10

### 7.2 최대 체력 감소 시 현재 체력 조정
```java
// decreaseMaxHealth() 내부 동작 (Line 231~233)
if (currentHealth > maxHealth) {
    currentHealth = maxHealth;
}
```
**결과:** 최대 체력 80→70, 현재 체력 80 → 현재 체력 70으로 조정

### 7.3 최대 체력만 변경하고 현재 체력은 유지하기
```java
// 직접 maxHealth 필드 수정 (heal() 호출 안 함)
@SpirePatch(clz = AbstractPlayer.class, method = "preBattlePrep")
public static class MaxHPOnlyIncrease {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 현재 체력은 유지하고 최대 체력만 +10
        __instance.maxHealth += 10;
        __instance.healthBarUpdatedEvent();
    }
}
```

---

## 8. 주의사항

### 8.1 FullBelly Blight (무한 모드)
```java
// increaseMaxHp() Line 203~205
if (!Settings.isEndless || !AbstractDungeon.player.hasBlight("FullBelly")) {
    // 최대 체력 증가 로직
}
```
**무한 모드 + FullBelly**: 최대 체력 증가 **완전 차단**

### 8.2 최소/최대 제한
```java
// decreaseMaxHealth() Line 228~230
if (maxHealth <= 1) {
    maxHealth = 1;  // 최소 1
}
```
**최소값:** 최대 체력은 최소 1 이상 유지
**최대값:** 게임 내부 제한 없음 (UI는 999까지 표시)

### 8.3 체력바 갱신 필수
```java
// 최대 체력 직접 수정 시 반드시 호출
__instance.healthBarUpdatedEvent();
```
**필수:** 최대 체력 변경 후 `healthBarUpdatedEvent()` 호출 필수!

### 8.4 startingMaxHP vs maxHealth
- **startingMaxHP**: 게임 시작 시 초기 최대 체력 (변경 안 됨)
- **maxHealth**: 현재 최대 체력 (게임 중 계속 변화)

**사용 예:**
```java
// 시작 체력 대비 증가율 계산
float hpGrowth = (float) player.maxHealth / player.startingMaxHP;
```

### 8.5 Ascension 난이도별 최대 체력 패널티
```java
// getAscensionMaxHPLoss() - 각 캐릭터별 구현
// Ironclad: return 10;  (A14+에서 -10)
// Silent: return 7;      (A14+에서 -7)
// Defect: return 8;      (A14+에서 -8)
// Watcher: return 8;     (A14+에서 -8)
```

---

## 9. 추가 팁

### 9.1 최대 체력 증가 이벤트 감지
```java
@SpirePatch(clz = AbstractCreature.class, method = "increaseMaxHp")
public static class MaxHPIncreaseListener {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance instanceof AbstractPlayer) {
            System.out.println("최대 체력 +" + amount + " 증가!");
            System.out.println("현재 최대 체력: " + __instance.maxHealth);
        }
    }
}
```

### 9.2 최대 체력 증가량 누적 추적
```java
public static int totalMaxHPGained = 0;

@SpirePatch(clz = AbstractCreature.class, method = "increaseMaxHp")
public static class TrackMaxHPGains {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance instanceof AbstractPlayer) {
            totalMaxHPGained += amount;
            System.out.println("총 최대 체력 증가량: " + totalMaxHPGained);
        }
    }
}
```

### 9.3 최대 체력 증가 시 추가 효과
```java
@SpirePatch(clz = AbstractCreature.class, method = "increaseMaxHp")
public static class MaxHPIncreaseBonus {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int amount) {
        if (__instance instanceof AbstractPlayer && amount > 0) {
            // 최대 체력 증가 시 골드 +10
            AbstractDungeon.player.gainGold(10);

            // 최대 체력 증가 시 카드 1장 드로우
            AbstractDungeon.actionManager.addToBottom(
                new DrawCardAction(__instance, 1)
            );
        }
    }
}
```
