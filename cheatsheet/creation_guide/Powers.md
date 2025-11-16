# 새로운 파워(Power) 만들기 가이드

기존 파워를 기반으로 새로운 커스텀 파워를 만드는 완전한 가이드입니다.

## 📋 목차

1. [시스템 개요](#시스템-개요)
2. [파워 구조 분석](#파워-구조-분석)
3. [예제: 약화된 Time Warp 만들기](#예제-약화된-time-warp-만들기)
4. [단계별 구현](#단계별-구현)
5. [파워 적용 방법](#파워-적용-방법)
6. [고급 기법](#고급-기법)
7. [문제 해결](#문제-해결)

---

## 시스템 개요

### 파워(Power)란?

파워는 전투 중 캐릭터나 적에게 적용되는 **지속 효과**입니다.

**파워의 종류:**
- **버프 (Buff)**: 긍정적 효과 (힘, 민첩, 방어도 등)
- **디버프 (Debuff)**: 부정적 효과 (약화, 취약, 속박 등)
- **중립 (Neutral)**: 특수 효과 (Time Warp, Barricade 등)

### 핵심 클래스

```java
// 모든 파워의 부모 클래스
com.megacrit.cardcrawl.powers.AbstractPower

// 파워 타입
public enum PowerType {
    BUFF,    // 초록색
    DEBUFF,  // 빨간색
    NEUTRAL  // 노란색
}
```

---

## 파워 구조 분석

### 원본 Time Warp Power 분석

**파일 위치:** `com.megacrit.cardcrawl.powers.TimeWarpPower`

**핵심 메커니즘:**
```java
public class TimeWarpPower extends AbstractPower {
    private static final int COUNTDOWN_AMT = 12;  // 카드 12개 제한
    private static final int STR_AMT = 2;         // 힘 +2

    // 카드 사용 시마다 호출
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        this.amount++;  // 카운터 증가

        if (this.amount == 12) {  // 12개 도달 시
            // 턴 강제 종료
            AbstractDungeon.actionManager.callEndTurnEarlySequence();

            // 모든 적에게 힘 +2
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                addToBot(new ApplyPowerAction(m, m, new StrengthPower(m, 2), 2));
            }

            this.amount = 0;  // 카운터 리셋
        }
    }
}
```

**주요 구성 요소:**

| 요소 | 역할 |
|------|------|
| `COUNTDOWN_AMT` | 카드 사용 제한 (12개) |
| `STR_AMT` | 힘 증가량 (+2) |
| `amount` | 현재 카드 사용 횟수 |
| `onAfterUseCard()` | 카드 사용 시 트리거 |

---

## 예제: 약화된 Time Warp 만들기

### 목표

시간 포식자의 Time Warp를 **67% 약화**시킨 버전 제작:
- **카드 제한:** 12개 → **20개**
- **힘 부여:** +2 (동일)

### 1단계: 프로젝트 구조 설정

```
your-mod/
├── src/main/java/
│   └── com/yourmod/
│       └── powers/
│           └── WeakenedTimeWarpPower.java  ← 여기에 생성
└── src/main/resources/
    └── yourmodResources/
        └── localization/eng/
            └── PowerStrings.json  ← 설명 텍스트
```

### 2단계: 파워 클래스 작성

**파일:** `WeakenedTimeWarpPower.java`

```java
package com.yourmod.powers;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.TimeWarpTurnEndEffect;

public class WeakenedTimeWarpPower extends AbstractPower {
    public static final String POWER_ID = "YourMod:WeakenedTimeWarp";
    private static final PowerStrings powerStrings =
        CardCrawlGame.languagePack.getPowerStrings("Time Warp");

    private static final int COUNTDOWN_AMT = 20;  // 12 → 20으로 변경
    private static final int STR_AMT = 2;

    public WeakenedTimeWarpPower(AbstractCreature owner) {
        this.name = "Weakened Time Warp";
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 0;

        // 파워 아이콘 로드 (원본 Time Warp 아이콘 재사용)
        loadRegion("time");

        this.type = PowerType.BUFF;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        // 설명: "20장의 카드를 사용하면 턴이 종료됩니다."
        this.description = "카드를 " + COUNTDOWN_AMT + "장 사용하면 턴이 종료됩니다. NL " +
                          "현재: " + this.amount + "/" + COUNTDOWN_AMT + " NL " +
                          "턴 종료 시 모든 적은 힘을 " + STR_AMT + " 얻습니다.";
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        flashWithoutSound();  // 파워 아이콘 번쩍임
        this.amount++;

        if (this.amount >= COUNTDOWN_AMT) {
            this.amount = 0;

            // 사운드 재생
            CardCrawlGame.sound.play("POWER_TIME_WARP", 0.05F);

            // 턴 강제 종료
            AbstractDungeon.actionManager.callEndTurnEarlySequence();

            // 이펙트
            AbstractDungeon.effectsQueue.add(
                new BorderFlashEffect(Color.GOLD, true)
            );
            AbstractDungeon.topLevelEffectsQueue.add(
                new TimeWarpTurnEndEffect()
            );

            // 모든 적에게 힘 +2
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                if (!m.isDead && !m.isDying) {
                    addToBot(new ApplyPowerAction(
                        m, m,
                        new StrengthPower(m, STR_AMT),
                        STR_AMT
                    ));
                }
            }
        }

        updateDescription();  // 설명 업데이트
    }
}
```

### 3단계: 몬스터에게 파워 적용

**방법 1: 패치를 통한 적용**

```java
package com.yourmod.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.beyond.TimeEater;
import com.yourmod.powers.WeakenedTimeWarpPower;

@SpirePatch(
    clz = TimeEater.class,
    method = "usePreBattleAction"
)
public class ApplyWeakenedTimeWarp {
    @SpirePostfixPatch
    public static void Postfix(TimeEater __instance) {
        // 원본 Time Warp 제거
        __instance.powers.removeIf(p -> p.ID.equals("Time Warp"));

        // 약화 버전 적용
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                __instance, __instance,
                new WeakenedTimeWarpPower(__instance)
            )
        );
    }
}
```

**방법 2: 조건부 적용 (Ascension 레벨별)**

```java
@SpirePostfixPatch
public static void Postfix(TimeEater __instance) {
    int asc = AbstractDungeon.ascensionLevel;

    // Ascension 50 이상에서만 약화
    if (asc >= 50) {
        __instance.powers.removeIf(p -> p.ID.equals("Time Warp"));

        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                __instance, __instance,
                new WeakenedTimeWarpPower(__instance)
            )
        );
    }
    // 그 외에는 원본 Time Warp 사용
}
```

---

## 단계별 구현

### Step 1: AbstractPower 상속

```java
public class YourPower extends AbstractPower {
    // 필수 구현 사항
}
```

### Step 2: 필수 필드 설정

```java
public YourPower(AbstractCreature owner) {
    this.name = "Your Power Name";
    this.ID = "YourMod:YourPower";
    this.owner = owner;
    this.amount = 0;  // 파워 스택 수 (선택)
    this.type = PowerType.BUFF;  // BUFF, DEBUFF, NEUTRAL

    // 아이콘 로드
    loadRegion("powerIconName");  // 또는 기존 아이콘 재사용

    updateDescription();
}
```

### Step 3: 설명 구현

```java
@Override
public void updateDescription() {
    this.description = "Your power description here.";

    // amount 값 포함
    if (this.amount > 0) {
        this.description = "Effect increases by " + this.amount + ".";
    }
}
```

### Step 4: 트리거 메서드 구현

**주요 트리거 메서드:**

```java
// 카드 사용 후
@Override
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    // Time Warp처럼 카드 카운팅
}

// 턴 시작
@Override
public void atStartOfTurn() {
    // 매 턴 시작 시 효과
}

// 턴 종료
@Override
public void atEndOfTurn(boolean isPlayer) {
    // 매 턴 종료 시 효과
}

// 데미지 받을 때
@Override
public int onAttacked(DamageInfo info, int damageAmount) {
    // 데미지 감소/증가
    return damageAmount;
}

// 공격 시
@Override
public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
    // 공격 시 효과
}

// 카드 드로우 시
@Override
public void onCardDraw(AbstractCard card) {
    // 카드 드로우 시 효과
}

// 전투 시작
@Override
public void atStartOfTurnPostDraw() {
    // 카드 드로우 후 턴 시작
}
```

### Step 5: 파워 제거 조건

```java
@Override
public void atEndOfRound() {
    // 라운드 종료 시 자동 제거
    if (this.amount == 0) {
        addToTop(new RemoveSpecificPowerAction(this.owner, this.owner, this));
    }
}
```

---

## 파워 적용 방법

### 1. 전투 시작 시 적용

```java
@SpirePatch(
    clz = YourMonster.class,
    method = "usePreBattleAction"
)
public static class ApplyPowerPatch {
    @SpirePostfixPatch
    public static void Postfix(YourMonster __instance) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                __instance, __instance,
                new YourPower(__instance),
                1  // 스택 수
            )
        );
    }
}
```

### 2. 특정 행동 시 적용

```java
// 몬스터가 특정 기술 사용 시
@Override
public void takeTurn() {
    switch (this.nextMove) {
        case BUFF_MOVE:
            addToBot(new ApplyPowerAction(
                this, this,
                new YourPower(this)
            ));
            break;
    }
}
```

### 3. 플레이어에게 적용

```java
AbstractDungeon.actionManager.addToBottom(
    new ApplyPowerAction(
        AbstractDungeon.player,  // 플레이어에게
        monster,                 // 몬스터가 부여
        new YourPower(AbstractDungeon.player)
    )
);
```

---

## 고급 기법

### 1. 커스터마이징 가능한 파워

```java
public class ConfigurablePower extends AbstractPower {
    private final int threshold;
    private final int effectAmount;

    // 생성자 오버로딩
    public ConfigurablePower(AbstractCreature owner) {
        this(owner, 10, 2);  // 기본값
    }

    public ConfigurablePower(AbstractCreature owner, int threshold, int effect) {
        this.threshold = threshold;
        this.effectAmount = effect;
        // ... 초기화
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        this.amount++;
        if (this.amount >= this.threshold) {
            // 트리거
        }
    }
}
```

**사용:**
```java
// 기본값 (10, 2)
new ConfigurablePower(monster);

// 커스텀 (20, 3)
new ConfigurablePower(monster, 20, 3);
```

### 2. 카드 타입별 차별화

```java
@Override
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    // 공격 카드만 카운팅
    if (card.type == AbstractCard.CardType.ATTACK) {
        this.amount++;
    }

    // 스킬 카드는 2배 카운팅
    if (card.type == AbstractCard.CardType.SKILL) {
        this.amount += 2;
    }

    // 파워 카드는 무시
}
```

### 3. 조건부 효과

```java
@Override
public void atEndOfTurn(boolean isPlayer) {
    // 플레이어 턴 종료 시에만
    if (isPlayer) {
        // 효과 발동
    }

    // HP가 50% 이하일 때만
    if (this.owner.currentHealth < this.owner.maxHealth / 2) {
        // 효과 발동
    }

    // 특정 유물 소지 시
    if (AbstractDungeon.player.hasRelic("YourRelic")) {
        // 효과 증폭
    }
}
```

### 4. 스택 감소/증가

```java
@Override
public void stackPower(int stackAmount) {
    this.amount += stackAmount;

    // 최대 스택 제한
    if (this.amount > 99) {
        this.amount = 99;
    }
}

@Override
public void reducePower(int reduceAmount) {
    this.amount -= reduceAmount;

    // 0 이하면 제거
    if (this.amount <= 0) {
        addToTop(new RemoveSpecificPowerAction(
            this.owner, this.owner, this
        ));
    }
}
```

### 5. 복잡한 트리거 조건

```java
private int consecutiveAttacks = 0;

@Override
public void onAfterUseCard(AbstractCard card, UseCardAction action) {
    if (card.type == AbstractCard.CardType.ATTACK) {
        consecutiveAttacks++;

        // 연속 3회 공격 시 트리거
        if (consecutiveAttacks >= 3) {
            // 효과 발동
            consecutiveAttacks = 0;
        }
    } else {
        // 공격 외 카드 사용 시 카운터 리셋
        consecutiveAttacks = 0;
    }
}
```

---

## 파워 아이콘 설정

### Option 1: 기존 아이콘 재사용

```java
// 원본 Time Warp 아이콘
loadRegion("time");

// 다른 기존 아이콘들
loadRegion("strength");    // 힘
loadRegion("dexterity");   // 민첩
loadRegion("artifact");    // 아티팩트
loadRegion("vulnerable");  // 취약
loadRegion("weak");        // 약화
loadRegion("frail");       // 나약
```

### Option 2: 커스텀 아이콘 (고급)

```java
// 1. 이미지 준비 (84x84 PNG)
// yourmodResources/images/powers/yourpower84.png
// yourmodResources/images/powers/yourpower32.png (작은 버전)

// 2. 아이콘 로드
this.region128 = new TextureAtlas.AtlasRegion(
    ImageMaster.loadImage("yourmodResources/images/powers/yourpower84.png"),
    0, 0, 84, 84
);
this.region48 = new TextureAtlas.AtlasRegion(
    ImageMaster.loadImage("yourmodResources/images/powers/yourpower32.png"),
    0, 0, 32, 32
);
```

---

## 예제 모음

### 예제 1: 턴당 에너지 +1 파워

```java
public class BonusEnergyPower extends AbstractPower {
    public static final String POWER_ID = "YourMod:BonusEnergy";

    public BonusEnergyPower(AbstractCreature owner, int amount) {
        this.name = "Bonus Energy";
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.BUFF;
        loadRegion("energized");
        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = "매 턴 시작 시 에너지를 " + this.amount + " 얻습니다.";
    }

    @Override
    public void atStartOfTurnPostDraw() {
        flash();
        addToBot(new GainEnergyAction(this.amount));
    }
}
```

### 예제 2: X턴 후 자동 제거

```java
public class TemporaryPower extends AbstractPower {
    private int turnsRemaining;

    public TemporaryPower(AbstractCreature owner, int duration) {
        this.name = "Temporary Effect";
        this.ID = "YourMod:Temporary";
        this.owner = owner;
        this.turnsRemaining = duration;
        this.type = PowerType.BUFF;
        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = turnsRemaining + "턴 후 사라집니다.";
    }

    @Override
    public void atEndOfRound() {
        turnsRemaining--;

        if (turnsRemaining <= 0) {
            addToTop(new RemoveSpecificPowerAction(
                this.owner, this.owner, this
            ));
        }

        updateDescription();
    }
}
```

### 예제 3: 데미지 감소 파워

```java
public class DamageReductionPower extends AbstractPower {
    private final int reduction;

    public DamageReductionPower(AbstractCreature owner, int reduction) {
        this.name = "Damage Reduction";
        this.ID = "YourMod:DamageReduction";
        this.owner = owner;
        this.reduction = reduction;
        this.type = PowerType.BUFF;
        loadRegion("platedArmor");
        updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = "받는 모든 공격 데미지가 " + reduction + " 감소합니다.";
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.type == DamageInfo.DamageType.NORMAL) {
            return Math.max(0, damageAmount - reduction);
        }
        return damageAmount;
    }
}
```

---

## 문제 해결

### Q: 파워가 적용되지 않아요

**A:** 다음을 확인하세요:

1. **ApplyPowerAction 사용:**
```java
// 올바른 방법
addToBot(new ApplyPowerAction(target, source, power));

// 잘못된 방법 (작동 안 함)
target.powers.add(power);
```

2. **owner 설정:**
```java
// 생성자에서 반드시 설정
this.owner = owner;
```

3. **ID 설정:**
```java
// 고유한 ID 필요
this.ID = "YourMod:YourPower";
```

### Q: 파워 아이콘이 안 보여요

**A:** loadRegion() 호출 확인:

```java
// 생성자에서 호출
loadRegion("powerName");  // 기존 아이콘
// 또는
this.region128 = ...;  // 커스텀 아이콘
```

### Q: 설명이 업데이트되지 않아요

**A:** updateDescription() 호출:

```java
@Override
public void onAfterUseCard(...) {
    this.amount++;
    updateDescription();  // 이 줄 추가
}
```

### Q: 파워가 중복 적용돼요

**A:** 기존 파워 제거:

```java
// 패치에서 기존 파워 제거
__instance.powers.removeIf(p -> p.ID.equals("OriginalPower"));

// 새 파워 적용
addToBot(new ApplyPowerAction(...));
```

### Q: 턴 종료가 제대로 작동하지 않아요

**A:** 정확한 메서드 사용:

```java
// 턴 강제 종료
AbstractDungeon.actionManager.callEndTurnEarlySequence();

// 일반 턴 종료 효과
@Override
public void atEndOfTurn(boolean isPlayer) {
    // ...
}
```

---

## 참고 자료

### 원본 파워 파일 위치

- **Time Warp:** `com.megacrit.cardcrawl.powers.TimeWarpPower`
- **Strength:** `com.megacrit.cardcrawl.powers.StrengthPower`
- **Vulnerable:** `com.megacrit.cardcrawl.powers.VulnerablePower`
- **Abstract Power:** `com.megacrit.cardcrawl.powers.AbstractPower`

### 디컴파일 소스

```
E:\workspace\sts-decompile\com\megacrit\cardcrawl\powers\
```

### 주요 메서드 전체 목록

```java
// 턴 관련
atStartOfTurn()
atStartOfTurnPostDraw()
atEndOfTurn(boolean isPlayer)
atEndOfRound()

// 카드 관련
onAfterUseCard(AbstractCard card, UseCardAction action)
onUseCard(AbstractCard card, UseCardAction action)
onCardDraw(AbstractCard card)
onPlayCard(AbstractCard card, AbstractMonster m)

// 전투 관련
onAttack(DamageInfo info, int damageAmount, AbstractCreature target)
onAttacked(DamageInfo info, int damageAmount)
onInflictDamage(DamageInfo info, int damageAmount, AbstractCreature target)
wasHPLost(DamageInfo info, int damageAmount)

// 기타
onDeath()
onRemove()
onInitialApplication()
stackPower(int stackAmount)
reducePower(int reduceAmount)
```

---

## 변경 이력

- **2025-01-15:** 초기 버전 생성
  - Time Warp 기반 예제
  - 단계별 구현 가이드
  - 고급 기법 및 문제 해결
