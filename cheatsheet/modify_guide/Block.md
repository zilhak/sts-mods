# 방어도 (Block) 수정 가이드

## 1. 방어도 시스템 개요

### 핵심 개념
```java
public abstract class AbstractCreature {
    public int currentBlock;  // 현재 방어도
}
```

### 방어도 작동 원리
- **획득:** `addBlock(int blockAmount)` 메서드로 방어도 증가
- **사용:** 데미지를 받을 때 HP 대신 방어도 감소
- **소멸:** 턴 종료 시 자동 제거 (Barricade/Blur 파워 예외)
- **상한:** 최대 999

## 2. 방어도가 획득되는 시점

### 2.1 스킬 카드
**예시: Defend 카드**
```java
public void use(AbstractPlayer p, AbstractMonster m) {
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(p, p, this.block)
    );
}
```

**GainBlockAction 실행 과정:**
```java
public class GainBlockAction extends AbstractGameAction {
    public void update() {
        if (this.duration == this.startDuration) {
            // 시각 효과
            AbstractDungeon.effectList.add(
                new FlashAtkImgEffect(
                    this.target.hb.cX,
                    this.target.hb.cY,
                    AttackEffect.SHIELD
                )
            );

            // 실제 방어도 추가
            this.target.addBlock(this.amount);

            // 카드 데미지 재계산
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                c.applyPowers();
            }
        }
        tickDuration();
    }
}
```

### 2.2 파워
**Metallicize 파워:**
```java
@Override
public void atStartOfTurn() {
    flash();
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(this.owner, this.owner, this.amount)
    );
}
```

**Plated Armor 파워:**
```java
@Override
public void atEndOfTurn(boolean isPlayer) {
    flash();
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(this.owner, this.owner, this.amount)
    );
}
```

### 2.3 유물
**Oddly Smooth Stone:**
```java
@Override
public void atBattleStart() {
    flash();
    AbstractDungeon.actionManager.addToTop(
        new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, 9)
    );
}
```

### 2.4 포션
**Block Potion:**
```java
@Override
public void use(AbstractCreature target) {
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(target, target, this.potency)
    );
}
```

## 3. 방어도가 감소하는 시점

### 3.1 데미지 흡수
**메서드: `AbstractCreature.decrementBlock(DamageInfo info, int damageAmount)`**

```java
protected int decrementBlock(DamageInfo info, int damageAmount) {
    // HP_LOSS 타입은 Block 무시
    if (info.type != DamageInfo.DamageType.HP_LOSS && this.currentBlock > 0) {
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);

        // 케이스 1: 데미지 > Block (Block 완전 소진)
        if (damageAmount > this.currentBlock) {
            damageAmount -= this.currentBlock;
            if (Settings.SHOW_DMG_BLOCK) {
                // Block 수치 표시
                AbstractDungeon.effectList.add(
                    new BlockedNumberEffect(this.hb.cX, this.hb.cY + this.hb.height / 2.0f,
                        Integer.toString(this.currentBlock))
                );
            }
            loseBlock();        // Block을 0으로
            brokeBlock();       // Block 파괴 효과

        // 케이스 2: 데미지 == Block (정확히 소진)
        } else if (damageAmount == this.currentBlock) {
            damageAmount = 0;
            loseBlock();        // Block을 0으로
            brokeBlock();       // Block 파괴 효과
            AbstractDungeon.effectList.add(
                new BlockedWordEffect(this, this.hb.cX, this.hb.cY, TEXT[1])  // "BLOCKED!"
            );

        // 케이스 3: 데미지 < Block (Block 일부만 소진)
        } else {
            CardCrawlGame.sound.play("BLOCK_ATTACK");
            loseBlock(damageAmount);  // Block 일부 감소
            for (int i = 0; i < 18; i++) {
                // 방어 성공 이펙트
                AbstractDungeon.effectList.add(
                    new BlockImpactLineEffect(this.hb.cX, this.hb.cY)
                );
            }
            if (Settings.SHOW_DMG_BLOCK) {
                AbstractDungeon.effectList.add(
                    new BlockedNumberEffect(this.hb.cX, this.hb.cY + this.hb.height / 2.0f,
                        Integer.toString(damageAmount))
                );
            }
            damageAmount = 0;  // 데미지 완전 차단
        }
    }
    return damageAmount;  // 남은 데미지 (HP에서 차감)
}
```

### 3.2 턴 종료
**메서드: `AbstractPlayer.applyEndOfTurnTriggers()`**

```java
// 기본적으로 모든 Block 제거
this.loseBlock();

// 예외: Barricade 파워
if (this.hasPower("Barricade")) {
    // Block 유지
    return;
}
```

### 3.3 특정 파워/유물
**Frail 파워:** Block 획득 시 25% 감소
```java
@Override
public float modifyBlock(float blockAmount) {
    return blockAmount * 0.75f;
}
```

## 4. 방어도 수정 방법

### 4.1 전역 방어도 증가
**목표:** 모든 Block 획득 2배

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class DoubleBlock {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, @ByRef int[] blockAmount) {
        // 원래 blockAmount의 2배로 변경
        blockAmount[0] = blockAmount[0] * 2;
    }
}
```

**더 정확한 방법 - float 처리:**
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class DoubleBlockPrecise {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(AbstractCreature __instance, @ByRef float[] tmp) {
        // 유물/파워 적용 후, 최종 추가 전에 2배
        tmp[0] = tmp[0] * 2;
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractCreature.class, "currentBlock"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 4.2 특정 카드 Block 증가
**목표:** Defend 카드 Block +3

```java
@SpirePatch(
    clz = Defend_Red.class,  // Ironclad Defend
    method = "use",
    paramtypez = {AbstractPlayer.class, AbstractMonster.class}
)
public static class BuffDefend {
    @SpirePrefixPatch
    public static void Prefix(AbstractCard __instance, AbstractPlayer p, AbstractMonster m) {
        // 카드의 기본 Block 증가
        __instance.baseBlock += 3;
        __instance.applyPowers();
    }
}
```

**모든 Defend 카드 강화:**
```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "applyPowers"
)
public static class BuffAllDefends {
    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance) {
        // COMMON_SKILL 타입 또는 이름에 "Defend" 포함
        if (__instance.hasTag(CardTags.STARTER_DEFEND)) {
            __instance.block += 3;
            __instance.isBlockModified = true;
        }
    }
}
```

### 4.3 턴 종료 시 Block 유지
**목표:** Barricade 효과를 항상 적용

```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyEndOfTurnTriggers"
)
public static class KeepBlockAlways {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractPlayer __instance) {
        // Block 제거를 건너뜀
        // 다른 턴 종료 효과는 정상 실행

        // 원본 메서드에서 loseBlock() 부분만 제거하려면
        // 더 복잡한 Locator 필요
        return SpireReturn.Continue();
    }
}
```

**더 나은 방법 - Barricade 파워 자동 부여:**
```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "applyStartOfCombatLogic"
)
public static class AutoBarricade {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        // 전투 시작 시 Barricade 파워 부여
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                __instance,
                __instance,
                new BarricadePower(__instance)
            )
        );
    }
}
```

### 4.4 최대 Block 제한
**목표:** Block 상한을 999에서 9999로 증가

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class IncreaseBlockCap {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(AbstractCreature __instance) {
        // 999 체크를 9999로 변경
        if (__instance.currentBlock > 9999) {
            __instance.currentBlock = 9999;
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractCreature.class, "currentBlock"
            );
            int[] lines = LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            // 999 체크 라인 찾기 (addBlock 메서드 내)
            return new int[]{lines[lines.length - 2]};
        }
    }
}
```

## 5. 방어도 수정자

### Dexterity 파워
```java
@Override
public float modifyBlock(float blockAmount, AbstractCard card) {
    // Dexterity만큼 Block 증가/감소
    return blockAmount + (float)this.amount;
}
```

### Frail 파워
```java
@Override
public float modifyBlock(float blockAmount) {
    // Block 25% 감소
    return blockAmount * 0.75f;
}
```

### 유물
**Orichalcum:** 턴 종료 시 Block이 0이면 6 획득
```java
@Override
public void onPlayerEndTurn() {
    if (AbstractDungeon.player.currentBlock == 0) {
        flash();
        AbstractDungeon.actionManager.addToTop(
            new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, 6)
        );
    }
}
```

## 6. 실용적인 수정 예시

### 예시 1: 모든 Block 1.5배
**목표:** 획득하는 모든 Block을 50% 증가

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class BlockBoost {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, @ByRef int[] blockAmount) {
        // 1.5배 (반올림)
        blockAmount[0] = (int)(blockAmount[0] * 1.5f);
    }
}
```

### 예시 2: Block 획득 시 추가 +3
**목표:** Block을 얻을 때마다 +3 보너스

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class BonusBlock {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(AbstractCreature __instance, @ByRef float[] tmp) {
        // 플레이어만 보너스
        if (__instance.isPlayer && tmp[0] > 0) {
            tmp[0] += 3;
        }
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractCreature.class, "currentBlock"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 예시 3: Block 상한 9999
**목표:** Block 최대치를 999에서 9999로 변경

```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "addBlock"
)
public static class IncreaseBlockLimit {
    @SpirePostfixPatch
    public static void Postfix(AbstractCreature __instance) {
        // 999 체크 이후 실행되므로 다시 조정
        if (__instance.currentBlock == 999) {
            // 999로 제한된 경우 무시하고 계속 증가 가능
        }
        // 새로운 상한 설정
        if (__instance.currentBlock > 9999) {
            __instance.currentBlock = 9999;
        }
    }
}
```

## 7. 관련 클래스

### GainBlockAction
- **생성자:**
  ```java
  GainBlockAction(AbstractCreature target, int amount)
  GainBlockAction(AbstractCreature target, AbstractCreature source, int amount)
  GainBlockAction(AbstractCreature target, int amount, boolean superFast)
  ```
- **실행:** `update()` 메서드에서 `target.addBlock(amount)` 호출

### AbstractCreature
- **필드:**
  - `int currentBlock`: 현재 방어도
- **메서드:**
  - `addBlock(int blockAmount)`: 방어도 획득
  - `loseBlock()`: 모든 방어도 제거
  - `loseBlock(int amount)`: 일부 방어도 제거
  - `loseBlock(int amount, boolean noAnimation)`: 애니메이션 선택 가능

### AbstractCard
- **필드:**
  - `int block`: 카드의 기본 방어도
  - `int baseBlock`: 업그레이드/유물 수정 전 기본 방어도
  - `boolean isBlockModified`: 방어도가 수정되었는지 여부

### AbstractPower
- **메서드:**
  - `modifyBlock(float blockAmount)`: Block 수정
  - `modifyBlock(float blockAmount, AbstractCard card)`: 카드별 Block 수정
  - `onGainedBlock(float blockAmount)`: Block 획득 시 트리거

## 8. 주의사항

### 8.1 Frail 등 감소 효과 고려
```java
// addBlock 메서드 내부에서 Frail 처리
for (AbstractPower p : this.powers) {
    tmp = p.modifyBlock(tmp);
}
```
Prefix 패치로 수정하면 Frail 적용 전, Postfix는 적용 후

### 8.2 Barricade 유지 메커니즘
**Barricade 파워:**
```java
@Override
public void atEndOfTurn(boolean isPlayer) {
    // 아무것도 하지 않음 (Block이 자동으로 유지됨)
}
```

**구현 원리:**
```java
// AbstractPlayer.applyEndOfTurnTriggers()
if (!this.hasPower("Barricade")) {
    this.loseBlock();  // Barricade 없으면 Block 제거
}
```

### 8.3 Block 상한 999
```java
// AbstractCreature.addBlock()
if (this.currentBlock > 999) {
    this.currentBlock = 999;
}

// 999 도달 시 업적
if (this.currentBlock == 999) {
    UnlockTracker.unlockAchievement("BARRICADED");
}
```

상한을 변경하려면 이 체크를 수정해야 함

### 8.4 Block 획득 애니메이션
```java
private void gainBlockAnimation() {
    this.blockAnimTimer = 0.7F;
    this.blockOffset = 0.0F;
    this.blockScale = 1.0F;
    // 시각 효과 설정
}
```

`GainBlockAction`이 자동으로 처리하므로 직접 `addBlock()`을 호출할 때만 주의

### 8.5 유물 상호작용
**Orichalcum 예시:**
```java
@Override
public void onPlayerEndTurn() {
    if (AbstractDungeon.player.currentBlock == 0) {
        // Block이 0일 때만 발동
        flash();
        AbstractDungeon.actionManager.addToTop(
            new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, 6)
        );
    }
}
```

Block을 항상 유지하면 Orichalcum이 발동하지 않음

### 8.6 음수 Block 방지
```java
public void loseBlock(int amount, boolean noAnimation) {
    this.currentBlock -= amount;
    if (this.currentBlock < 0) {
        this.currentBlock = 0;  // 음수 방지
    }
}
```

Block 감소 패치 시 음수 체크 필수

### 8.7 Block vs Damage 타입
**NORMAL/THORNS:** Block 차감
```java
if (info.type != DamageInfo.DamageType.HP_LOSS && this.currentBlock > 0) {
    damageAmount = decrementBlock(info, damageAmount);
}
```

**HP_LOSS:** Block 무시
```java
// HP_LOSS는 decrementBlock을 건너뜀
if (info.type == DamageInfo.DamageType.HP_LOSS) {
    // Block 무시하고 바로 HP 감소
}
```

### 8.8 카드 Block 표시 vs 실제 Block
- **AbstractCard.block**: 카드에 표시되는 Block
- **GainBlockAction.amount**: 실제 추가되는 Block

```java
// 카드 표시 수정
@SpirePatch(clz = AbstractCard.class, method = "applyPowers")

// 실제 Block 수정
@SpirePatch(clz = AbstractCreature.class, method = "addBlock")
```

### 8.9 Dexterity 적용 시점
```java
// AbstractCard.applyPowers()
if (this.baseBlock > -1) {
    this.block = this.baseBlock;

    // Dexterity 적용
    for (AbstractPower p : AbstractDungeon.player.powers) {
        this.block = (int)p.modifyBlock((float)this.block, this);
    }

    // 음수 방지
    if (this.block < 0) {
        this.block = 0;
    }
}
```

카드 Block 수정은 Dexterity 이전/이후를 고려

### 8.10 패치 위치 선택
- **AbstractCreature.addBlock()**: Block 획득 시 수정
- **GainBlockAction.update()**: Action 실행 시 수정
- **AbstractCard.applyPowers()**: 카드 표시 Block 수정
- **AbstractPower.modifyBlock()**: 파워를 통한 수정

각 위치마다 적용 범위가 다르므로 목적에 맞게 선택:
- 전역 수정 → `addBlock()`
- 카드 전용 → `applyPowers()`
- 특정 조건 → Custom Power

### 8.11 Block 파괴 이벤트
```java
private void brokeBlock() {
    if (this instanceof AbstractMonster) {
        // 플레이어 유물 트리거
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onBlockBroken(this);  // 적 Block 파괴 시
        }
    }

    // 시각 효과
    AbstractDungeon.effectList.add(new HbBlockBrokenEffect(...));
    CardCrawlGame.sound.play("BLOCK_BREAK");
}
```

Block이 완전히 소진될 때만 발동
