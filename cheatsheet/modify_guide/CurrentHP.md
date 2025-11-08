# 현재 체력 (Current HP) 수정 가이드

## 1. 현재 체력 저장 위치

### 1.1 AbstractCreature.java
```java
public int currentHealth;  // 현재 체력
```

**초기화 시점:**
```java
// AbstractPlayer.initializeClass() - Line 387
this.currentHealth = info.currentHp;  // CharSelectInfo에서 현재 체력 설정
```

### 1.2 체력 관련 상태 변수
```java
public int lastDamageTaken = 0;      // 마지막 받은 데미지
public boolean isBloodied = false;   // 체력 50% 이하 여부
```

---

## 2. 현재 체력이 증가하는 시점

### 2.1 힐 (Heal)
**메서드:** `AbstractCreature.heal(int healAmount, boolean showEffect)`

**호출 시점:**
- HealAction 실행
- 최대 체력 증가 시 자동 호출
- 유물/카드/포션 효과

### 2.2 캠프파이어 휴식
**위치:** CampfireUI.rest()
**회복량:** 최대 체력의 30%

```java
// CampfireRestOption.useOption()
int healAmount = (int)(AbstractDungeon.player.maxHealth * 0.3F);
AbstractDungeon.player.heal(healAmount);
```

### 2.3 포션
- **Potion of Healing**: 현재 체력 +20
- **Ancient Potion**: 현재 체력 = 최대 체력 (완전 회복)
- **Fairy in a Bottle**: 사망 시 최대 체력의 30% 회복

### 2.4 유물
- **Singing Bowl**: 최대 체력의 2 회복
- **Meat on the Bone**: 전투 종료 시 체력 12 회복
- **Magic Flower**: 캠프파이어 휴식 시 추가 회복
- **Eternal Feather**: 층 시작 시 체력 회복
- **Self-Forming Clay**: 전투 종료 시 방어도만큼 체력 회복

### 2.5 카드
- **전사 (Ironclad):**
  - Feed: 적 처치 시 체력 3 회복
  - Reaper: 데미지만큼 체력 회복
  - Bloodletting: 체력 소모 (증가 아님)

- **암살자 (Silent):**
  - Neutralize: 약화 부여 후 체력 회복 (업그레이드)

- **결함자 (Defect):**
  - Self Repair: 전투 종료 시 체력 회복

- **관찰자 (Watcher):**
  - Pray: 체력 3 회복 + 통찰력 2

---

## 3. 현재 체력이 감소하는 시점

### 3.1 전투 데미지
**메서드:** `AbstractPlayer.damage(DamageInfo info)`

**위치:** AbstractPlayer.java Line 1786~1876

**동작 순서:**
1. 블록으로 데미지 감소 (`decrementBlock()`)
2. 파워 적용 (`DamageInfo.applyPowers()`)
3. 현재 체력 감소 (`currentHealth -= damageAmount`)
4. 사망 체크 및 이벤트 처리

### 3.2 이벤트
- **가면 상인 (Masked Bandits)**: 체력 손실
- **뱀 (Snakes)**: 체력 50% 감소
- **함정 (Spike Trap)**: 최대 체력의 10% 손실

### 3.3 자해 카드
- **Bloodletting (전사)**: 현재 체력 -3, 에너지 +2
- **Offering (전사)**: 현재 체력 -6, 카드 3장 드로우, 에너지 +2
- **Ritual Dagger (암살자)**: 체력 소모 없음 (데미지만)

### 3.4 유물
- **Mark of Pain**: 에너지 +1, 전투 시작 시 저주 카드 추가
- **Black Star**: 엘리트 처치 시 보상 증가 (체력 변동 없음)

---

## 4. 현재 체력 수정 방법

### 4.1 전투 시작 시 체력 회복

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "preBattlePrep"
)
public static class BattleStartHealPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 전투 시작 시 체력 10 회복
        __instance.heal(10);
    }
}
```

### 4.2 힐량 증가

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "heal"
)
public static class HealMultiplierPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractCreature __instance,
                                            int healAmount, boolean showEffect) {
        if (__instance instanceof AbstractPlayer) {
            // 모든 힐 2배
            int newHealAmount = healAmount * 2;

            // 원본 heal() 로직 실행
            originalHeal(__instance, newHealAmount, showEffect);

            return SpireReturn.Return();  // 원본 메서드 실행 안 함
        }
        return SpireReturn.Continue();
    }

    private static void originalHeal(AbstractCreature creature, int amount, boolean showEffect) {
        if (creature.isDying) return;

        // 파워 적용
        for (AbstractPower p : creature.powers) {
            amount = p.onHeal(amount);
        }

        // 체력 회복
        creature.currentHealth += amount;
        if (creature.currentHealth > creature.maxHealth) {
            creature.currentHealth = creature.maxHealth;
        }

        // 효과 표시
        if (amount > 0 && showEffect && creature.isPlayer) {
            AbstractDungeon.topPanel.panelHealEffect();
            AbstractDungeon.effectsQueue.add(
                new HealEffect(creature.hb.cX - creature.animX,
                               creature.hb.cY, amount)
            );
        }

        creature.healthBarUpdatedEvent();
    }
}
```

### 4.3 받는 데미지 감소

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "damage"
)
public static class DamageReductionPatch {
    @SpirePrefixPatch
    public static void Prefix(AbstractPlayer __instance, DamageInfo info) {
        // 받는 모든 데미지 50% 감소
        if (info.type != DamageInfo.DamageType.HP_LOSS) {
            info.output = (int)(info.output * 0.5f);
        }
    }
}
```

### 4.4 특정 난이도에서 시작 체력 감소

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "initializeClass"
)
public static class AscensionStartHPPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // A20+에서 시작 체력 -20
            __instance.currentHealth = Math.max(1, __instance.currentHealth - 20);
            __instance.healthBarUpdatedEvent();
        }
    }
}
```

### 4.5 전투 승리 시 체력 회복

```java
@SpirePatch(
    clz = AbstractRoom.class,
    method = "endBattle"
)
public static class BattleVictoryHealPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        if (__instance instanceof MonsterRoom && !AbstractDungeon.player.isDead) {
            // 전투 승리 시 최대 체력의 10% 회복
            int healAmount = (int)(AbstractDungeon.player.maxHealth * 0.1f);
            AbstractDungeon.player.heal(healAmount);
        }
    }
}
```

---

## 5. 관련 메서드

### 5.1 heal(int healAmount, boolean showEffect)
**위치:** AbstractCreature.java Line 428~476

**동작 순서:**
1. **FullBelly Blight 체크** (무한 모드)
   ```java
   if (Settings.isEndless && isPlayer && hasBlight("FullBelly")) {
       healAmount /= 2;  // 힐량 50% 감소
   }
   ```

2. **유물 처리** (`AbstractRelic.onPlayerHeal()`)
   ```java
   for (AbstractRelic r : AbstractDungeon.player.relics) {
       if (isPlayer) {
           healAmount = r.onPlayerHeal(healAmount);
       }
   }
   ```

3. **파워 처리** (`AbstractPower.onHeal()`)
   ```java
   for (AbstractPower p : powers) {
       healAmount = p.onHeal(healAmount);
   }
   ```

4. **체력 증가**
   ```java
   currentHealth += healAmount;
   if (currentHealth > maxHealth) {
       currentHealth = maxHealth;  // 최대 체력 초과 방지
   }
   ```

5. **Bloodied 상태 해제**
   ```java
   if (currentHealth > maxHealth / 2.0F && isBloodied) {
       isBloodied = false;
       for (AbstractRelic r : relics) {
           r.onNotBloodied();
       }
   }
   ```

6. **효과 표시** (showEffect = true)
   ```java
   if (healAmount > 0 && showEffect && isPlayer) {
       AbstractDungeon.topPanel.panelHealEffect();
       AbstractDungeon.effectsQueue.add(new HealEffect(...));
   }
   ```

### 5.2 damage(DamageInfo info)
**위치:** AbstractPlayer.java Line 1786~1876

**동작 순서:**
1. **데미지 기록**
   ```java
   lastDamageTaken = Math.min(damageAmount, currentHealth);
   ```

2. **블록 처리** (`decrementBlock()`)
   ```java
   damageAmount = decrementBlock(info, damageAmount);
   ```

3. **파워 적용** (data already in DamageInfo)

4. **체력 감소**
   ```java
   currentHealth -= damageAmount;
   if (currentHealth < 0) {
       currentHealth = 0;
   }
   ```

5. **Bloodied 상태 설정**
   ```java
   if (currentHealth < maxHealth / 4) {
       // 체력 25% 이하 시 업적 체크
   }
   ```

6. **사망 체크**
   ```java
   if (currentHealth < 1) {
       // Fairy in a Bottle, Lizard Tail 체크
       // 사망 처리
   }
   ```

---

## 6. 데미지 시스템 연동

### 6.1 DamageInfo 클래스
**위치:** DamageInfo.java

**필드:**
```java
public AbstractCreature owner;    // 데미지 소스 (공격자)
public DamageType type;           // 데미지 타입
public int base;                  // 기본 데미지
public int output;                // 최종 데미지 (파워 적용 후)
public boolean isModified;        // 파워로 변경되었는지 여부
```

### 6.2 DamageType (Line 199~201)
```java
public enum DamageType {
    NORMAL,   // 일반 데미지 (블록으로 막을 수 있음)
    THORNS,   // 가시 데미지 (반사 데미지)
    HP_LOSS   // 직접 체력 손실 (블록 무시)
}
```

### 6.3 applyPowers() 메서드
**동작:** 데미지에 파워 효과 적용

**플레이어 공격 시:**
1. `atDamageGive()` - 공격자 파워 (힘, 약화 등)
2. `stance.atDamageGive()` - 자세 효과
3. `atDamageReceive()` - 방어자 파워 (취약, 강화 등)
4. `atDamageFinalGive()` - 최종 공격 배율
5. `atDamageFinalReceive()` - 최종 방어 배율

**몬스터 공격 시:**
1. `atDamageGive()` - 몬스터 파워
2. `atDamageReceive()` - 플레이어 파워 (강화, 취약 등)
3. `stance.atDamageReceive()` - 플레이어 자세
4. 최종 계산

---

## 7. 사망 방지 메커니즘

### 7.1 Buffer 파워
```java
// BufferPower.onAttacked()
if (damageInfo.output > 0 && damageInfo.type != DamageType.HP_LOSS) {
    damageInfo.output = 0;  // 데미지 무효화
    flash();
    reducePower(1);  // 버퍼 1 감소
}
```

### 7.2 Fairy in a Bottle 유물
**위치:** AbstractPlayer.damage() Line 1850~1867

```java
if (currentHealth < 1) {
    if (hasRelic("Fairy in a Bottle")) {
        // 최대 체력의 30% 회복
        currentHealth = 0;  // 일단 0으로 설정
        getRelic("Fairy in a Bottle").onTrigger();

        // 실제 회복은 유물에서 처리
        // FairyInABottle.onTrigger():
        // player.heal((int)(player.maxHealth * 0.3F));
    }
}
```

### 7.3 Lizard Tail 유물
**동작:** 전투 중 1회 사망 시 최대 체력의 50% 회복

---

## 8. 실용적인 수정 예시

### 예시 1: 전투 승리 시 체력 10% 회복
```java
@SpirePatch(clz = AbstractRoom.class, method = "endBattle")
public static class VictoryHeal {
    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        if (__instance instanceof MonsterRoom && !AbstractDungeon.player.isDead) {
            int healAmount = (int)(AbstractDungeon.player.maxHealth * 0.1f);
            AbstractDungeon.player.heal(healAmount, true);
        }
    }
}
```

### 예시 2: 모든 힐 2배
```java
@SpirePatch(clz = AbstractCreature.class, method = "heal")
public static class DoubleHeal {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance,
                              @ByRef int[] healAmount, boolean showEffect) {
        if (__instance instanceof AbstractPlayer) {
            healAmount[0] *= 2;  // 배열로 참조 전달하여 수정
        }
    }
}
```

### 예시 3: 받는 데미지 50% 감소
```java
@SpirePatch(clz = DamageInfo.class, method = "applyPowers")
public static class DamageReduction {
    @SpirePostfixPatch
    public static void Postfix(DamageInfo __instance,
                                AbstractCreature owner, AbstractCreature target) {
        if (target instanceof AbstractPlayer &&
            __instance.type != DamageInfo.DamageType.HP_LOSS) {
            __instance.output = (int)(__instance.output * 0.5f);
            __instance.isModified = true;
        }
    }
}
```

### 예시 4: 전투 시작 시 완전 회복
```java
@SpirePatch(clz = AbstractPlayer.class, method = "preBattlePrep")
public static class FullHealOnBattle {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        __instance.currentHealth = __instance.maxHealth;
        __instance.healthBarUpdatedEvent();
    }
}
```

### 예시 5: 캠프파이어 휴식 회복량 증가
```java
@SpirePatch(clz = CampfireRestOption.class, method = "useOption")
public static class BetterRest {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(CampfireRestOption __instance) {
        // 기본 30% → 50% 회복
        int healAmount = (int)(AbstractDungeon.player.maxHealth * 0.5f);
        AbstractDungeon.player.heal(healAmount);

        // 원본 메서드 실행 안 함
        return SpireReturn.Return();
    }
}
```

---

## 9. 주의사항

### 9.1 체력 상한/하한
```java
// heal() 내부 (Line 452~454)
if (currentHealth > maxHealth) {
    currentHealth = maxHealth;  // 최대 체력 초과 방지
}

// damage() 내부 (Line 1831~1833)
if (currentHealth < 0) {
    currentHealth = 0;  // 음수 방지
}
```

### 9.2 FullBelly Blight (무한 모드)
```java
// heal() Line 429~434
if (Settings.isEndless && isPlayer && hasBlight("FullBelly")) {
    healAmount /= 2;  // 힐량 50% 감소
    if (healAmount < 1) {
        healAmount = 1;
    }
}
```

### 9.3 HP_LOSS 타입 데미지
```java
// HP_LOSS는 블록 무시
if (info.type != DamageInfo.DamageType.HP_LOSS && currentBlock > 0) {
    // 블록 처리
}
```
**예시:** Necronomicon, Offering 카드

### 9.4 Bloodied 상태
```java
// 체력 50% 이하일 때 활성화
isBloodied = (currentHealth <= maxHealth / 2);

// 관련 유물:
// - Blood Vial: Bloodied 상태에서 전투 시작 시 체력 회복
// - Red Skull: Bloodied 상태에서 힘 +3
```

### 9.5 체력바 갱신 필수
```java
// 현재 체력 직접 수정 시 반드시 호출
player.healthBarUpdatedEvent();
```

---

## 10. 추가 팁

### 10.1 체력 회복 이벤트 감지
```java
@SpirePatch(clz = AbstractCreature.class, method = "heal")
public static class HealListener {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance,
                                int healAmount, boolean showEffect) {
        if (__instance instanceof AbstractPlayer && healAmount > 0) {
            System.out.println("체력 +" + healAmount + " 회복!");
            System.out.println("현재 체력: " + __instance.currentHealth +
                             "/" + __instance.maxHealth);
        }
    }
}
```

### 10.2 데미지 받은 이벤트 감지
```java
@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public static class DamageListener {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
        if (__instance.lastDamageTaken > 0) {
            System.out.println("데미지 -" + __instance.lastDamageTaken + " 받음!");
            System.out.println("현재 체력: " + __instance.currentHealth +
                             "/" + __instance.maxHealth);
        }
    }
}
```

### 10.3 전투 중 받은 총 데미지 추적
```java
public static int totalDamageTaken = 0;

@SpirePatch(clz = AbstractPlayer.class, method = "preBattlePrep")
public static class ResetDamageCounter {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        totalDamageTaken = 0;
    }
}

@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public static class TrackDamage {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
        totalDamageTaken += __instance.lastDamageTaken;
        System.out.println("전투 중 받은 총 데미지: " + totalDamageTaken);
    }
}
```

### 10.4 체력 변화 시 추가 효과
```java
@SpirePatch(clz = AbstractCreature.class, method = "heal")
public static class HealBonus {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance, int healAmount) {
        if (__instance instanceof AbstractPlayer && healAmount > 0) {
            // 체력 회복 시 카드 1장 드로우
            AbstractDungeon.actionManager.addToBottom(
                new DrawCardAction(__instance, 1)
            );
        }
    }
}
```

### 10.5 체력 비율 기반 효과
```java
public static void applyHealthBasedEffect(AbstractPlayer player) {
    float healthPercent = (float)player.currentHealth / player.maxHealth;

    if (healthPercent <= 0.25f) {
        // 체력 25% 이하: 힘 +2
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(player, player,
                new StrengthPower(player, 2), 2)
        );
    } else if (healthPercent >= 0.9f) {
        // 체력 90% 이상: 방어도 +5
        player.addBlock(5);
    }
}
```
