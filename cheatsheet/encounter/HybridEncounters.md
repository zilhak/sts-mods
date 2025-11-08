# 하이브리드 조우 (Hybrid Encounters - Event + Combat)

## 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [1막 하이브리드 이벤트](#1막-하이브리드-이벤트)
4. [2막 하이브리드 이벤트](#2막-하이브리드-이벤트)
5. [3막 하이브리드 이벤트](#3막-하이브리드-이벤트)
6. [전투 시작 메커니즘](#전투-시작-메커니즘)
7. [보상 시스템](#보상-시스템)
8. [수정 방법](#수정-방법)
9. [관련 클래스](#관련-클래스)

---

## 시스템 개요

하이브리드 조우는 **이벤트와 전투가 결합된 특수한 만남**입니다. 플레이어는 이벤트에서 선택을 하고, 그 결과에 따라 전투가 시작되거나 전투 없이 이벤트가 종료됩니다.

### 핵심 특징

- **선택적 전투**: 플레이어의 선택에 따라 전투 발생 여부 결정
- **특수 보상**: 전투 승리 시 일반 전투보다 더 좋은 보상 제공
- **이벤트 + 전투 통합**: `enterCombat()` 또는 `enterCombatFromImage()` 사용
- **독립적인 보상 설정**: 일반 전투 보상 시스템을 무시하고 커스텀 보상 설정 가능

---

## 호출 흐름

### 이벤트 → 전투 전환 (AbstractEvent.java)

```
EventRoom.onPlayerEntry()                         // Line 19-24 (EventRoom.java)
    └─> AbstractDungeon.generateEvent()
        └─> HybridEvent Constructor
            ├─> monsters = MonsterHelper.getEncounter(...)
            ├─> Set rewards.clear() / rewardAllowed = false
            └─> Player choice → enterCombat()

enterCombat()                                     // AbstractEvent.java
    └─> (AbstractDungeon.getCurrRoom()).phase = COMBAT
        └─> Combat System starts

enterCombatFromImage()                            // AbstractImageEvent.java
    └─> (AbstractDungeon.getCurrRoom()).phase = COMBAT
        └─> Combat System starts
```

### 전투 종료 후 이벤트 복귀

```
Combat ends
    └─> AbstractRoom.endBattle()                  // AbstractRoom.java:537
        └─> combatRewardScreen.open()
            └─> Player collects rewards
                └─> event.reopen()                // Called from EventRoom
                    └─> Continue event or openMap()
```

---

## 1막 하이브리드 이벤트

### Mushrooms (버섯 방)

**파일**: `com.megacrit.cardcrawl.events.exordium.Mushrooms.java`

**이벤트 ID**: `"Mushrooms"`
**전투 ID**: `"The Mushroom Lair"`

#### 선택지

| 옵션 | 효과 | 보상 |
|------|------|------|
| 전투 (0번) | The Mushroom Lair 전투 시작 | 금화 (20-30G) + Odd Mushroom 유물 |
| 회복 (1번) | 최대 HP의 25% 회복 + Parasite 저주 획득 | HP 회복 |

#### 코드 분석 (Mushrooms.java:59-108)

```java
protected void buttonEffect(int buttonPressed) {
  switch (buttonPressed) {

    case 0:
      if (this.screenNum == 0) {
        (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter("The Mushroom Lair");

        this.roomEventText.updateBodyText(FIGHT_MSG);
        this.roomEventText.updateDialogOption(0, OPTIONS[3]);
        this.roomEventText.removeDialogOption(1);
        AbstractEvent.logMetric("Mushrooms", "Fought Mushrooms");
        this.screenNum += 2;
      } else if (this.screenNum == 1) {

        openMap();
      } else if (this.screenNum == 2) {
        if (Settings.isDailyRun) {
          AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(25));
        } else {
          AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(20, 30));
        }

        if (AbstractDungeon.player.hasRelic("Odd Mushroom")) {
          AbstractDungeon.getCurrRoom().addRelicToRewards((AbstractRelic)new Circlet());
        } else {
          AbstractDungeon.getCurrRoom().addRelicToRewards((AbstractRelic)new OddMushroom());
        }

        enterCombat();
        AbstractDungeon.lastCombatMetricKey = "The Mushroom Lair";
      }
      return;

    case 1:
      parasite = new Parasite();
      healAmt = (int)(AbstractDungeon.player.maxHealth * 0.25F);
      AbstractEvent.logMetricObtainCardAndHeal("Mushrooms", "Healed and dodged fight", (AbstractCard)parasite, healAmt);
      AbstractDungeon.player.heal(healAmt);
      AbstractDungeon.effectList.add(new ShowCardAndObtainEffect((AbstractCard)parasite, Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));

      this.roomEventText.updateBodyText(HEAL_MSG);
      this.roomEventText.updateDialogOption(0, OPTIONS[4]);
      this.roomEventText.removeDialogOption(1);
      this.screenNum = 1;
      return;
  }
}
```

#### 특징

- 회복 선택 시 **25% HP 회복**하지만 **Parasite 저주** 획득
- 전투 선택 시 **Odd Mushroom 유물** 보장 (이미 보유 시 Circlet)

### DeadAdventurer (죽은 모험가)

**파일**: `com.megacrit.cardcrawl.events.exordium.DeadAdventurer.java`

**이벤트 ID**: `"Dead Adventurer"`
**전투 ID**: `"3 Sentries"`, `"Gremlin Nob"`, `"Lagavulin Event"`

#### 선택지

| 옵션 | 효과 | 전투 확률 | 보상 |
|------|------|-----------|------|
| 수색 (0번) | 보상 획득 또는 전투 | 초기 25% (승천 15+: 35%), +25%씩 증가 | 금화 30G 또는 유물 또는 없음 (랜덤) |
| 떠나기 (1번) | 이벤트 종료 | 0% | 없음 |

#### 코드 분석 (DeadAdventurer.java:109-164)

```java
protected void buttonEffect(int buttonPressed) {
  switch (this.screen) {
    case INTRO:
      switch (buttonPressed) {
        case 0:
          if (AbstractDungeon.miscRng.random(0, 99) < this.encounterChance) {

            this.screen = CUR_SCREEN.FAIL;
            this.roomEventText.updateBodyText(FIGHT_MSG);
            this.roomEventText.updateDialogOption(0, OPTIONS[2]);
            this.roomEventText.removeDialogOption(1);
            if (Settings.isDailyRun) {
              AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(30));
            } else {
              AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(25, 35));
            }

            (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter(getMonster());
            (AbstractDungeon.getCurrRoom()).eliteTrigger = true;

            break;
          }
          randomReward();
          break;

        case 1:
          this.screen = CUR_SCREEN.ESCAPE;
          this.roomEventText.updateBodyText(ESCAPE_MSG);
          this.roomEventText.updateDialogOption(0, OPTIONS[1]);
          this.roomEventText.removeDialogOption(1);
          break;
      }
      return;
    case SUCCESS:
      openMap();
      return;
    case FAIL:
      for (String s : this.rewards) {
        if (s.equals("GOLD")) {
          AbstractDungeon.getCurrRoom().addGoldToRewards(30); continue;
        }  if (s.equals("RELIC")) {
          AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractDungeon.returnRandomRelicTier());
        }
      }
      enterCombat();
      AbstractDungeon.lastCombatMetricKey = getMonster();
      this.numRewards++;
      logMetric(this.numRewards);
      return;
    case ESCAPE:
      logMetric(this.numRewards);
      openMap();
      return;
  }
}

private String getMonster() {
  switch (this.enemy) {
    case 0:
      return "3 Sentries";
    case 1:
      return "Gremlin Nob";
  }
  return "Lagavulin Event";
}
```

#### 전투 확률 메커니즘 (DeadAdventurer.java:63-67, 180-181)

```java
// 초기화
if (AbstractDungeon.ascensionLevel >= 15) {
  this.encounterChance = 35;       // 승천 15+ : 35% 시작
} else {
  this.encounterChance = 25;       // 일반: 25% 시작
}

// 수색 성공 시 (randomReward)
this.encounterChance += 25;        // 다음 수색 시 +25% 증가
```

**확률 증가 패턴**:
- 1번째 수색: 25% (승천 15+: 35%)
- 2번째 수색: 50% (승천 15+: 60%)
- 3번째 수색: 75% (승천 15+: 85%)

#### 특징

- **3번 수색** 가능 (GOLD, NOTHING, RELIC 중 랜덤 순서)
- 전투 발생 시 **엘리트 전투 카운트** 증가 (`eliteTrigger = true`)
- 전투 보상: 금화 (25-35G) + 수색으로 얻은 보상들

---

## 2막 하이브리드 이벤트

### MaskedBandits (가면 도적단)

**파일**: `com.megacrit.cardcrawl.events.city.MaskedBandits.java`

**이벤트 ID**: `"Masked Bandits"`
**전투 ID**: `"Masked Bandits"`

#### 선택지

| 옵션 | 효과 | 보상 |
|------|------|------|
| 금화 지불 (0번) | 모든 금화 상실, 이벤트 종료 | 없음 (금화 -100%) |
| 전투 (1번) | Masked Bandits 전투 시작 | 금화 (25-35G) + Red Mask 유물 |

#### 코드 분석 (MaskedBandits.java:54-106)

```java
protected void buttonEffect(int buttonPressed) {
  switch (this.screen) {
    case INTRO:
      switch (buttonPressed) {
        case 0:
          stealGold();
          AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
          this.roomEventText.updateBodyText(PAID_MSG_1);
          this.roomEventText.updateDialogOption(0, OPTIONS[2]);
          this.roomEventText.clearRemainingOptions();
          this.screen = CurScreen.PAID_1;
          return;

        case 1:
          logMetric("Masked Bandits", "Fought Bandits");

          if (Settings.isDailyRun) {
            AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(30));
          } else {
            AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(25, 35));
          }
          if (AbstractDungeon.player.hasRelic("Red Mask")) {
            AbstractDungeon.getCurrRoom().addRelicToRewards((AbstractRelic)new Circlet());
          } else {
            AbstractDungeon.getCurrRoom().addRelicToRewards((AbstractRelic)new RedMask());
          }

          enterCombat();
          AbstractDungeon.lastCombatMetricKey = "Masked Bandits";
          return;
      }
      break;
    // ... (PAID screens omitted)
  }
}

private void stealGold() {
  AbstractPlayer abstractPlayer = AbstractDungeon.player;
  if (((AbstractCreature)abstractPlayer).gold == 0) {
    return;
  }

  logMetricLoseGold("Masked Bandits", "Paid Fearfully", ((AbstractCreature)abstractPlayer).gold);
  CardCrawlGame.sound.play("GOLD_JINGLE");

  for (int i = 0; i < ((AbstractCreature)abstractPlayer).gold; i++) {
    AbstractMonster abstractMonster = (AbstractDungeon.getCurrRoom()).monsters.getRandomMonster();
    AbstractDungeon.effectList.add(new GainPennyEffect((AbstractCreature)abstractMonster, ((AbstractCreature)abstractPlayer).hb.cX, ((AbstractCreature)abstractPlayer).hb.cY, ((AbstractCreature)abstractMonster).hb.cX, ((AbstractCreature)abstractMonster).hb.cY, false));
  }
}
```

#### 특징

- **Red Mask 유물** 보장 (이미 보유 시 Circlet)
- 금화 지불 시 **모든 금화 상실**하지만 전투 회피
- 금화 지불 시 **GainPennyEffect** 애니메이션 재생 (시각적 효과)

### Colosseum (콜로세움)

**파일**: `com.megacrit.cardcrawl.events.city.Colosseum.java`

**이벤트 ID**: `"Colosseum"`
**전투 ID**: `"Colosseum Slavers"`, `"Colosseum Nobs"`

#### 선택지 (2단계 전투)

| 단계 | 옵션 | 전투 | 보상 |
|------|------|------|------|
| 1단계 | 싸우기 (0번) | Colosseum Slavers | 없음 (rewardAllowed = false) |
| 2단계 | 계속 싸우기 (1번) | Colosseum Nobs | 금화 100G + Rare Relic + Uncommon Relic |
| 2단계 | 떠나기 (0번) | 전투 없음 | 없음 |

#### 코드 분석 (Colosseum.java:34-108)

```java
protected void buttonEffect(int buttonPressed) {
  switch (this.screen) {
    case INTRO:
      switch (buttonPressed) {
        case 0:
          this.imageEventText.updateBodyText(DESCRIPTIONS[1] + DESCRIPTIONS[2] + 'ၨ' + DESCRIPTIONS[3]);
          this.imageEventText.updateDialogOption(0, OPTIONS[1]);
          this.screen = CurScreen.FIGHT;
          break;
      }
      return;
    case FIGHT:
      switch (buttonPressed) {
        case 0:
          this.screen = CurScreen.POST_COMBAT;
          logMetric("Fight");
          (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter("Colosseum Slavers");

          (AbstractDungeon.getCurrRoom()).rewards.clear();
          (AbstractDungeon.getCurrRoom()).rewardAllowed = false;
          enterCombatFromImage();
          AbstractDungeon.lastCombatMetricKey = "Colosseum Slavers";
          break;
      }


      this.imageEventText.clearRemainingOptions();
      return;
    case POST_COMBAT:
      (AbstractDungeon.getCurrRoom()).rewardAllowed = true;
      switch (buttonPressed) {
        case 1:
          this.screen = CurScreen.LEAVE;
          logMetric("Fought Nobs");
          (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter("Colosseum Nobs");

          (AbstractDungeon.getCurrRoom()).rewards.clear();
          AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
          AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
          AbstractDungeon.getCurrRoom().addGoldToRewards(100);
          (AbstractDungeon.getCurrRoom()).eliteTrigger = true;
          enterCombatFromImage();
          AbstractDungeon.lastCombatMetricKey = "Colosseum Nobs";
          return;
      }
      logMetric("Fled From Nobs");
      openMap();
      return;


    case LEAVE:
      openMap();
      return;
  }
  openMap();
}

public void reopen() {
  if (this.screen != CurScreen.LEAVE) {
    AbstractDungeon.resetPlayer();
    AbstractDungeon.player.drawX = Settings.WIDTH * 0.25F;
    AbstractDungeon.player.preBattlePrep();
    enterImageFromCombat();
    this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
    this.imageEventText.updateDialogOption(0, OPTIONS[2]);
    this.imageEventText.setDialogOption(OPTIONS[3]);
  }
}
```

#### 특징

- **2단계 전투**: Slavers → Nobs (선택적)
- 1단계 전투는 **보상 없음** (`rewardAllowed = false`)
- 2단계 전투는 **Rare + Uncommon Relic + 100 Gold**
- 2단계 전투는 **엘리트 카운트** 증가 (`eliteTrigger = true`)
- `reopen()` 메서드로 전투 후 이벤트 재개

---

## 3막 하이브리드 이벤트

### MysteriousSphere (신비로운 구체)

**파일**: `com.megacrit.cardcrawl.events.beyond.MysteriousSphere.java`

**이벤트 ID**: `"Mysterious Sphere"`
**전투 ID**: `"2 Orb Walkers"`

#### 선택지

| 옵션 | 효과 | 보상 |
|------|------|------|
| 열기 (0번) | 2 Orb Walkers 전투 시작 | 금화 (45-55G) + Rare Relic (보장) |
| 떠나기 (1번) | 이벤트 종료 | 없음 |

#### 코드 분석 (MysteriousSphere.java:51-95)

```java
protected void buttonEffect(int buttonPressed) {
  switch (this.screen) {
    case INTRO:
      switch (buttonPressed) {
        case 0:
          this.screen = CurScreen.PRE_COMBAT;
          this.roomEventText.updateBodyText(DESCRIPTIONS[1]);
          this.roomEventText.updateDialogOption(0, OPTIONS[2]);
          this.roomEventText.clearRemainingOptions();
          logMetric("Mysterious Sphere", "Fight");
          return;
        case 1:
          this.screen = CurScreen.END;
          this.roomEventText.updateBodyText(DESCRIPTIONS[2]);
          this.roomEventText.updateDialogOption(0, OPTIONS[1]);
          this.roomEventText.clearRemainingOptions();
          logMetricIgnored("Mysterious Sphere");
          return;
      }
      break;
    case PRE_COMBAT:
      if (Settings.isDailyRun) {
        AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(50));
      } else {
        AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(45, 55));
      }
      AbstractDungeon.getCurrRoom().addRelicToRewards(
          AbstractDungeon.returnRandomScreenlessRelic(AbstractRelic.RelicTier.RARE));


      if (this.img != null) {
        this.img.dispose();
        this.img = null;
      }

      this.img = ImageMaster.loadImage("images/events/sphereOpen.png");

      enterCombat();
      AbstractDungeon.lastCombatMetricKey = "2 Orb Walkers";
      break;
    case END:
      openMap();
      break;
  }
}
```

#### 특징

- **Rare Relic 보장**
- 이미지 변경: `sphereClosed.png` → `sphereOpen.png`

### MindBloom (정신 개화)

**파일**: `com.megacrit.cardcrawl.events.beyond.MindBloom.java`

**이벤트 ID**: `"MindBloom"`
**전투 ID**: `"The Guardian"`, `"Hexaghost"`, `"Slime Boss"` 중 랜덤

#### 선택지 (3가지)

| 옵션 | 효과 | 전투 | 보상 |
|------|------|------|------|
| 전투 (0번) | 무작위 Act 1 보스 전투 | Guardian/Hexaghost/Slime Boss (랜덤) | 금화 (25G or 50G) + Rare Relic |
| 업그레이드 (1번) | 모든 카드 업그레이드 + Mark of the Bloom 획득 | 없음 | 전체 카드 업그레이드 + Mark of the Bloom |
| 금화 or 회복 (2번) | 40층 이하: 999 Gold + Normality×2 <br> 41층 이상: 완전 회복 + Doubt | 없음 | 금화 or HP 회복 + 저주 |

#### 코드 분석 (MindBloom.java:58-180)

```java
protected void buttonEffect(int buttonPressed) {
  switch (this.screen) {
    case INTRO:
      switch (buttonPressed) {

        case 0:
          this.imageEventText.updateBodyText(DIALOG_2);
          this.screen = CurScreen.FIGHT;
          logMetric("MindBloom", "Fight");
          CardCrawlGame.music.playTempBgmInstantly("MINDBLOOM", true);
          list = new ArrayList<>();
          list.add("The Guardian");
          list.add("Hexaghost");
          list.add("Slime Boss");
          Collections.shuffle(list, new Random(AbstractDungeon.miscRng.randomLong()));
          (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter(list.get(0));
          (AbstractDungeon.getCurrRoom()).rewards.clear();
          if (AbstractDungeon.ascensionLevel >= 13) {
            AbstractDungeon.getCurrRoom().addGoldToRewards(25);
          } else {
            AbstractDungeon.getCurrRoom().addGoldToRewards(50);
          }
          AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
          enterCombatFromImage();
          AbstractDungeon.lastCombatMetricKey = "Mind Bloom Boss Battle";
          break;

        case 1:
          this.imageEventText.updateBodyText(DIALOG_3);
          this.screen = CurScreen.LEAVE;
          effectCount = 0;
          upgradedCards = new ArrayList<>();
          obtainedRelic = new ArrayList<>();
          for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (c.canUpgrade()) {
              effectCount++;
              if (effectCount <= 20) {
                float x = MathUtils.random(0.1F, 0.9F) * Settings.WIDTH;
                float y = MathUtils.random(0.2F, 0.8F) * Settings.HEIGHT;

                AbstractDungeon.effectList.add(new ShowCardBrieflyEffect(c
                      .makeStatEquivalentCopy(), x, y));
                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(x, y));
              }
              upgradedCards.add(c.cardID);
              c.upgrade();
              AbstractDungeon.player.bottledCardUpgradeCheck(c);
            }
          }
          AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F,


              RelicLibrary.getRelic("Mark of the Bloom").makeCopy());
          obtainedRelic.add("Mark of the Bloom");
          logMetric("MindBloom", "Upgrade", null, null, null, upgradedCards, obtainedRelic, null, null, 0, 0, 0, 0, 0, 0);

          this.imageEventText.updateDialogOption(0, OPTIONS[4]);
          break;

        case 2:
          if (AbstractDungeon.floorNum % 50 <= 40) {
            this.imageEventText.updateBodyText(DIALOG_2);
            this.screen = CurScreen.LEAVE;
            List<String> cardsAdded = new ArrayList<>();
            cardsAdded.add("Normality");
            cardsAdded.add("Normality");
            logMetric("MindBloom", "Gold", cardsAdded, null, null, null, null, null, null, 0, 0, 0, 0, 999, 0);
            AbstractDungeon.effectList.add(new RainingGoldEffect(999));
            AbstractDungeon.player.gainGold(999);
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect((AbstractCard)new Normality(), Settings.WIDTH * 0.6F, Settings.HEIGHT / 2.0F));




            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect((AbstractCard)new Normality(), Settings.WIDTH * 0.3F, Settings.HEIGHT / 2.0F));




            this.imageEventText.updateDialogOption(0, OPTIONS[4]);
            break;
          }
          this.imageEventText.updateBodyText(DIALOG_2);
          this.screen = CurScreen.LEAVE;
          doubt = new Doubt();
          logMetricObtainCardAndHeal("MindBloom", "Heal", (AbstractCard)doubt, AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth);




          AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth);
          AbstractDungeon.effectList.add(new ShowCardAndObtainEffect((AbstractCard)doubt, Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));

          this.imageEventText.updateDialogOption(0, OPTIONS[4]);
          break;
      }


      this.imageEventText.clearRemainingOptions();
      return;
    case LEAVE:
      openMap();
      return;
  }
  openMap();
}
```

#### 특징

- **3가지 선택지** 모두 강력하지만 대가 존재
- 보스 전투: **MINDBLOOM 테마 BGM** 재생
- 보스 전투: **승천 13+에서 금화 반감** (50G → 25G)
- 업그레이드 선택: **모든 업그레이드 가능 카드** 업그레이드 (20개까지 애니메이션)
- 업그레이드 선택: **Mark of the Bloom** 획득 (치유 불가)
- 3번 선택: **층수에 따라 효과 변경** (40층 기준)
  - 40층 이하: **999 Gold + Normality×2** (턴당 드로우 3장으로 제한)
  - 41층 이상: **완전 회복 + Doubt** (턴당 에너지 1 감소)

---

## 전투 시작 메커니즘

### enterCombat() (AbstractEvent.java)

```java
protected void enterCombat() {
  AbstractDungeon.overlayMenu.proceedButton.hide();
  (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMBAT;
  (AbstractDungeon.getCurrRoom()).isBattleOver = false;
  AbstractDungeon.rs = AbstractDungeon.RenderScene.NORMAL;
}
```

**특징**:
- 일반 이벤트 (`AbstractEvent`)에서 사용
- 화면 전환 없이 전투 시작
- 배경 그대로 유지

### enterCombatFromImage() (AbstractImageEvent.java)

```java
protected void enterCombatFromImage() {
  AbstractDungeon.overlayMenu.proceedButton.hide();
  (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMBAT;
  (AbstractDungeon.getCurrRoom()).isBattleOver = false;
  AbstractDungeon.rs = AbstractDungeon.RenderScene.NORMAL;
}
```

**특징**:
- 이미지 이벤트 (`AbstractImageEvent`)에서 사용
- 기능은 `enterCombat()`과 동일

### enterImageFromCombat() (AbstractImageEvent.java)

```java
protected void enterImageFromCombat() {
  AbstractDungeon.rs = AbstractDungeon.RenderScene.EVENT;
}
```

**특징**:
- 전투 → 이벤트로 복귀 시 사용
- 렌더링 씬을 EVENT로 변경

---

## 보상 시스템

### 일반 전투 보상 무시

하이브리드 이벤트는 **커스텀 보상**을 설정할 수 있습니다:

```java
(AbstractDungeon.getCurrRoom()).rewards.clear();         // 기존 보상 제거
(AbstractDungeon.getCurrRoom()).rewardAllowed = false;  // 일반 보상 시스템 비활성화

// 커스텀 보상 추가
AbstractDungeon.getCurrRoom().addGoldToRewards(100);
AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
```

### 보상 추가 메서드

```java
// 금화 보상
AbstractDungeon.getCurrRoom().addGoldToRewards(int amount);

// 유물 보상 (등급별)
AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier tier);

// 유물 보상 (특정 유물)
AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic relic);

// 포션 보상
AbstractDungeon.getCurrRoom().addPotionToRewards();
AbstractDungeon.getCurrRoom().addPotionToRewards(AbstractPotion potion);

// 카드 보상
AbstractDungeon.getCurrRoom().addCardToRewards();
```

---

## 수정 방법

### 1. 기존 하이브리드 이벤트의 보상 변경

**예시: Mushrooms 전투 보상 증가**

```java
@SpirePatch(
    clz = Mushrooms.class,
    method = "buttonEffect",
    paramtypez = {int.class}
)
public static class MushroomsRewardPatch {
    @SpireInsertPatch(
        locator = MushroomsCombatLocator.class
    )
    public static void Insert(Mushrooms __instance, int buttonPressed) {
        if (buttonPressed == 0) {
            // 추가 보상: Uncommon Relic
            AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);

            // 금화 증가
            AbstractDungeon.getCurrRoom().addGoldToRewards(50);
        }
    }
}

private static class MushroomsCombatLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.MethodCallMatcher(
            Mushrooms.class, "enterCombat"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 2. 전투 확률 변경 (DeadAdventurer)

```java
@SpirePatch(
    clz = DeadAdventurer.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class DeadAdventurerChancePatch {
    @SpirePostfixPatch
    public static void Postfix(DeadAdventurer __instance) {
        // 초기 전투 확률 10%로 감소
        ReflectionHacks.setPrivate(__instance, DeadAdventurer.class, "encounterChance", 10);
    }
}

@SpirePatch(
    clz = DeadAdventurer.class,
    method = "randomReward"
)
public static class DeadAdventurerRampPatch {
    @SpirePostfixPatch
    public static void Postfix(DeadAdventurer __instance) {
        // 확률 증가량 10%로 감소 (기본 25%)
        int currentChance = ReflectionHacks.getPrivate(__instance, DeadAdventurer.class, "encounterChance");
        ReflectionHacks.setPrivate(__instance, DeadAdventurer.class, "encounterChance", currentChance - 15);
    }
}
```

### 3. MindBloom 선택지 효과 변경

```java
@SpirePatch(
    clz = MindBloom.class,
    method = "buttonEffect",
    paramtypez = {int.class}
)
public static class MindBloomUpgradePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(MindBloom __instance, int buttonPressed) {
        if (buttonPressed == 1) {
            // 모든 카드 업그레이드하지만 Mark of the Bloom 없이

            for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                if (c.canUpgrade()) {
                    c.upgrade();
                    AbstractDungeon.player.bottledCardUpgradeCheck(c);
                }
            }

            // 대신 Rare Relic 제공
            AbstractRelic r = AbstractDungeon.returnRandomScreenlessRelic(AbstractRelic.RelicTier.RARE);
            AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F, r);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 4. 커스텀 하이브리드 이벤트 생성

```java
public class CustomCombatEvent extends AbstractImageEvent {
    public static final String ID = "CustomCombatEvent";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString(ID);
    public static final String NAME = eventStrings.NAME;
    public static final String[] DESCRIPTIONS = eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = eventStrings.OPTIONS;

    private CurScreen screen = CurScreen.INTRO;

    private enum CurScreen {
        INTRO, COMBAT, END;
    }

    public CustomCombatEvent() {
        super(NAME, DESCRIPTIONS[0], "modResources/images/events/customEvent.png");

        // 선택지 추가
        this.imageEventText.setDialogOption(OPTIONS[0]);  // 전투
        this.imageEventText.setDialogOption(OPTIONS[1]);  // 회피
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO:
                switch (buttonPressed) {
                    case 0:  // 전투 선택
                        this.screen = CurScreen.COMBAT;
                        logMetric(ID, "Fight");

                        // 몬스터 설정
                        (AbstractDungeon.getCurrRoom()).monsters = MonsterHelper.getEncounter("Custom Monster Group");

                        // 보상 설정
                        (AbstractDungeon.getCurrRoom()).rewards.clear();
                        AbstractDungeon.getCurrRoom().addGoldToRewards(75);
                        AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);

                        // 전투 시작
                        enterCombatFromImage();
                        AbstractDungeon.lastCombatMetricKey = "Custom Combat Event";
                        break;

                    case 1:  // 회피 선택
                        this.screen = CurScreen.END;
                        logMetricIgnored(ID);
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                        this.imageEventText.setDialogOption(OPTIONS[2]);
                        break;
                }
                return;

            case COMBAT:
                // 전투 후 처리 (자동으로 호출됨)
                openMap();
                return;

            case END:
                openMap();
                return;
        }
    }
}
```

### 5. Colosseum 2단계 보상 변경

```java
@SpirePatch(
    clz = Colosseum.class,
    method = "buttonEffect",
    paramtypez = {int.class}
)
public static class ColosseumRewardPatch {
    @SpireInsertPatch(
        locator = ColosseumNobsLocator.class
    )
    public static void Insert(Colosseum __instance, int buttonPressed) {
        // 원래 보상: Rare + Uncommon + 100G
        // 추가 보상: Boss Relic
        AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.BOSS);
    }
}

private static class ColosseumNobsLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.FieldAccessMatcher(
            AbstractRoom.class, "eliteTrigger"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 6. MaskedBandits 금화 지불 페널티 감소

```java
@SpirePatch(
    clz = MaskedBandits.class,
    method = "buttonEffect",
    paramtypez = {int.class}
)
public static class MaskedBanditsPenaltyPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(MaskedBandits __instance, int buttonPressed) {
        CurScreen screen = ReflectionHacks.getPrivate(__instance, MaskedBandits.class, "screen");

        if (screen == CurScreen.INTRO && buttonPressed == 0) {
            // 50% 금화만 상실하도록 변경
            int halfGold = AbstractDungeon.player.gold / 2;
            AbstractDungeon.player.loseGold(halfGold);

            // 나머지 처리
            ReflectionHacks.setPrivate(__instance, MaskedBandits.class, "screen", CurScreen.PAID_1);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 7. 전투 종료 후 이벤트 복귀 커스터마이징

```java
@SpirePatch(
    clz = Colosseum.class,
    method = "reopen"
)
public static class ColosseumReopenPatch {
    @SpirePostfixPatch
    public static void Postfix(Colosseum __instance) {
        // 전투 후 추가 효과
        CurScreen screen = ReflectionHacks.getPrivate(__instance, Colosseum.class, "screen");

        if (screen == CurScreen.POST_COMBAT) {
            // 플레이어에게 10 HP 회복
            AbstractDungeon.player.heal(10);
        }
    }
}
```

### 8. 커스텀 몬스터 그룹 등록 (하이브리드 이벤트용)

```java
@SpirePatch(
    clz = MonsterHelper.class,
    method = "getEncounter",
    paramtypez = {String.class}
)
public static class CustomMonsterGroupPatch {
    @SpirePostfixPatch
    public static MonsterGroup Postfix(MonsterGroup __result, String key) {
        if (key.equals("Custom Combat Event Monsters")) {
            // 커스텀 몬스터 그룹 생성
            return new MonsterGroup(
                new Monster1(0.0F, 0.0F),
                new Monster2(100.0F, 0.0F),
                new Monster3(200.0F, 0.0F)
            );
        }
        return __result;
    }
}
```

### 9. 전투 BGM 변경 (MindBloom 스타일)

```java
@SpirePatch(
    clz = CustomCombatEvent.class,
    method = "buttonEffect",
    paramtypez = {int.class}
)
public static class CustomBGMPatch {
    @SpireInsertPatch(
        locator = CombatStartLocator.class
    )
    public static void Insert(CustomCombatEvent __instance, int buttonPressed) {
        // 커스텀 BGM 재생
        CardCrawlGame.music.playTempBgmInstantly("BOSS_BOTTOM", true);
    }
}

private static class CombatStartLocator extends SpireInsertLocator {
    @Override
    public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
        Matcher matcher = new Matcher.MethodCallMatcher(
            AbstractImageEvent.class, "enterCombatFromImage"
        );
        return LineFinder.findInOrder(ctMethodToPatch, matcher);
    }
}
```

### 10. 하이브리드 이벤트 출현 확률 조정

이벤트 풀에 추가하는 방법은 EventEncounter.md 참조.

---

## 관련 클래스

### 핵심 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **AbstractEvent** | `com.megacrit.cardcrawl.events.AbstractEvent` | 모든 이벤트의 기본 클래스 |
| **AbstractImageEvent** | `com.megacrit.cardcrawl.events.AbstractImageEvent` | 이미지 이벤트 기본 클래스 |
| **EventRoom** | `com.megacrit.cardcrawl.rooms.EventRoom` | 이벤트 방 |

### 1막 하이브리드 이벤트

| 클래스 | 경로 | 전투 ID | 특징 |
|--------|------|---------|------|
| **Mushrooms** | `com.megacrit.cardcrawl.events.exordium.Mushrooms` | `The Mushroom Lair` | 회복 or 전투 (Odd Mushroom) |
| **DeadAdventurer** | `com.megacrit.cardcrawl.events.exordium.DeadAdventurer` | `3 Sentries`, `Gremlin Nob`, `Lagavulin Event` | 확률적 전투 (3회 수색) |

### 2막 하이브리드 이벤트

| 클래스 | 경로 | 전투 ID | 특징 |
|--------|------|---------|------|
| **MaskedBandits** | `com.megacrit.cardcrawl.events.city.MaskedBandits` | `Masked Bandits` | 금화 지불 or 전투 (Red Mask) |
| **Colosseum** | `com.megacrit.cardcrawl.events.city.Colosseum` | `Colosseum Slavers`, `Colosseum Nobs` | 2단계 전투 (Slavers → Nobs) |

### 3막 하이브리드 이벤트

| 클래스 | 경로 | 전투 ID | 특징 |
|--------|------|---------|------|
| **MysteriousSphere** | `com.megacrit.cardcrawl.events.beyond.MysteriousSphere` | `2 Orb Walkers` | Rare Relic 보장 |
| **MindBloom** | `com.megacrit.cardcrawl.events.beyond.MindBloom` | `The Guardian`, `Hexaghost`, `Slime Boss` | 3가지 강력한 선택지 |

### 유틸리티 클래스

| 클래스 | 경로 | 설명 |
|--------|------|------|
| **MonsterHelper** | `com.megacrit.cardcrawl.helpers.MonsterHelper` | getEncounter() - 몬스터 그룹 생성 |
| **AbstractRoom** | `com.megacrit.cardcrawl.rooms.AbstractRoom` | phase, rewards, rewardAllowed 관리 |

---

## 추가 참고사항

### 전투 메트릭 기록

```java
// 전투 시작 전
logMetric("Event ID", "Action Taken");

// 전투 키 설정
AbstractDungeon.lastCombatMetricKey = "Combat Name";
```

### eliteTrigger 플래그

```java
(AbstractDungeon.getCurrRoom()).eliteTrigger = true;
```

- 엘리트 전투 카운터 증가
- DeadAdventurer, Colosseum (Nobs)에서 사용

### rewardAllowed 플래그

```java
(AbstractDungeon.getCurrRoom()).rewardAllowed = false;  // 일반 보상 시스템 비활성화
(AbstractDungeon.getCurrRoom()).rewardAllowed = true;   // 일반 보상 시스템 활성화
```

- Colosseum 1단계에서 `false` 설정
- Colosseum 2단계에서 `true` 재설정

### 승천 난이도에 따른 분기

```java
// 승천 13+ : 금화 감소
if (AbstractDungeon.ascensionLevel >= 13) {
    AbstractDungeon.getCurrRoom().addGoldToRewards(25);
} else {
    AbstractDungeon.getCurrRoom().addGoldToRewards(50);
}

// 승천 15+ : 초기 확률 증가
if (AbstractDungeon.ascensionLevel >= 15) {
    this.encounterChance = 35;
} else {
    this.encounterChance = 25;
}
```

### 층수 기반 분기 (MindBloom)

```java
if (AbstractDungeon.floorNum % 50 <= 40) {
    // 40층 이하: 999 Gold + Normality×2
} else {
    // 41층 이상: 완전 회복 + Doubt
}
```

### 이미지 변경 (MysteriousSphere)

```java
if (this.img != null) {
    this.img.dispose();
    this.img = null;
}

this.img = ImageMaster.loadImage("images/events/sphereOpen.png");
```

- 이벤트 진행 중 이미지 동적 변경 가능
- 메모리 누수 방지를 위해 기존 이미지 dispose() 필수
