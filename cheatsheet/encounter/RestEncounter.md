# Rest Encounter (휴식처 조우 시스템)

Slay the Spire의 휴식처(Rest Site / Campfire) 시스템에 대한 완전한 분석 문서.

## 목차

1. [시스템 개요](#시스템-개요)
2. [호출 흐름](#호출-흐름)
3. [휴식처 옵션 시스템](#휴식처-옵션-시스템)
4. [휴식 (Rest) 시스템](#휴식-rest-시스템)
5. [대장간 (Smith) 시스템](#대장간-smith-시스템)
6. [회상 (Recall) 시스템](#회상-recall-시스템)
7. [유물 전용 옵션](#유물-전용-옵션)
8. [유물 효과](#유물-효과)
9. [수정 방법](#수정-방법)
10. [관련 클래스](#관련-클래스)

---

## 시스템 개요

### 휴식처 기본 구조

휴식처는 `RestRoom` 클래스로 표현되며, 플레이어가 다양한 행동을 선택할 수 있는 안전한 공간입니다.

**기본 설정:**
- **맵 심볼**: Fire icon
- **BGM**: 무음 (모닥불 소리만 재생)
- **Sound Effect**: `"REST_FIRE_WET"` (모닥불 소리, 루프 재생)
- **Room Phase**: 플레이어 행동 선택 대기

### 휴식처 옵션 구성

모든 휴식처는 기본적으로 다음 옵션들을 제공합니다:

**1. 기본 옵션 (항상 제공):**
- **휴식 (Rest)**: HP 회복
- **대장간 (Smith)**: 카드 1장 업그레이드

**2. 조건부 옵션:**
- **회상 (Recall)**: 4막 진입 가능 상태 + Ruby Key 미획득 시
- **유물 전용 옵션**: 특정 유물 소지 시 추가

**3. 유물 전용 옵션 (최대 2개까지):**
- **Girya (기리야)**: Lift (근력 단련) - 힘 +1 (최대 3회)
- **Peace Pipe (평화 파이프)**: Toke (명상) - 카드 1장 영구 제거
- **Shovel (삽)**: Dig (발굴) - 랜덤 유물 획득

### 옵션 개수 제한

```java
// Girya.java:54-56
private boolean campfireRelicCount() {
    int count = (AbstractDungeon.player.hasRelic("Girya") ? 1 : 0) +
                (AbstractDungeon.player.hasRelic("Peace Pipe") ? 1 : 0) +
                (AbstractDungeon.player.hasRelic("Shovel") ? 1 : 0);

    return (campfireRelicCount < 2);
}
```

**중요:** 유물 전용 옵션은 최대 2개까지만 표시됩니다. 3개의 유물을 모두 소지한 경우, 획득 순서에 따라 먼저 획득한 2개만 옵션으로 표시됩니다.

---

## 호출 흐름

### 1단계: RestRoom 진입

플레이어가 휴식처 노드에 진입하면 `onPlayerEntry()`가 호출됩니다.

```java
// RestRoom.java:25-36
public void onPlayerEntry() {
    // 4막이 아니면 BGM 정지
    if (!AbstractDungeon.id.equals("TheEnding")) {
        CardCrawlGame.music.silenceBGM();
    }

    // 모닥불 소리 재생 (루프)
    this.fireSoundId = CardCrawlGame.sound.playAndLoop("REST_FIRE_WET");
    lastFireSoundId = this.fireSoundId;

    // CampfireUI 생성
    this.campfireUI = new CampfireUI();

    // 모든 유물의 onEnterRestRoom() 호출
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onEnterRestRoom();
    }
}
```

### 2단계: CampfireUI 초기화

CampfireUI 생성자가 호출되어 옵션들을 초기화합니다.

```java
// CampfireUI.java:74-87
public CampfireUI() {
    this.scrollBar = new ScrollBar(this);
    hidden = false;

    // 옵션 버튼 초기화
    initializeButtons();

    // 버블 수 결정 (옵션 3개 이상이면 60개, 2개면 40개)
    if (this.buttons.size() > 2) {
        this.bubbleAmt = 60;
    } else {
        this.bubbleAmt = 40;
    }

    // 랜덤 메시지 선택
    this.bubbleMsg = getCampMessage();
}
```

### 3단계: 옵션 버튼 생성

initializeButtons()에서 모든 옵션을 생성하고 사용 가능 여부를 판단합니다.

```java
// CampfireUI.java:91-129
private void initializeButtons() {
    // 1. 기본 옵션 추가
    // 휴식 옵션 (항상 추가, 활성화 상태)
    this.buttons.add(new RestOption(true));

    // 대장간 옵션 (업그레이드 가능한 카드가 있고, Midas 모드가 아닐 때 활성화)
    this.buttons.add(new SmithOption(
        (AbstractDungeon.player.masterDeck.getUpgradableCards().size() > 0
         && !ModHelper.isModEnabled("Midas"))
    ));

    // 2. 유물 전용 옵션 추가
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.addCampfireOption(this.buttons);
    }

    // 3. 각 옵션에 대해 유물의 canUseCampfireOption() 검사
    for (AbstractCampfireOption co : this.buttons) {
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            if (!r.canUseCampfireOption(co)) {
                co.usable = false;  // 사용 불가 처리
            }
        }
    }

    // 4. 회상 옵션 추가 (4막 진입 가능 + Ruby Key 미획득)
    if (Settings.isFinalActAvailable && !Settings.hasRubyKey) {
        this.buttons.add(new RecallOption());
    }

    // 5. 모든 옵션이 사용 불가능하면 즉시 완료 처리
    boolean cannotProceed = true;
    for (AbstractCampfireOption opt : this.buttons) {
        if (opt.usable) {
            cannotProceed = false;
            break;
        }
    }
    if (cannotProceed) {
        AbstractRoom.waitTimer = 0.0F;
        (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMPLETE;
    }
}
```

### 4단계: 플레이어 선택

플레이어가 옵션을 선택하면 해당 옵션의 `useOption()`이 호출됩니다.

**선택 처리 흐름:**
1. 옵션 클릭/선택
2. `useOption()` 호출
3. 해당 효과 실행 (Effect 클래스 추가)
4. CampfireUI 숨김 처리 (`hidden = true`)
5. Room Phase를 COMPLETE로 변경

---

## 휴식처 옵션 시스템

### 옵션 배치 시스템

휴식처 옵션은 최대 6개까지 화면에 표시되며, 초과 시 스크롤바가 나타납니다.

**배치 규칙:**

```java
// CampfireUI.java:54-58
private static final float BUTTON_START_X = Settings.WIDTH * 0.416F;
private static final float BUTTON_SPACING_X = 300.0F * Settings.xScale;
private static final float BUTTON_START_Y = Settings.HEIGHT / 2.0F + 180.0F * Settings.scale;
private static final float BUTTON_SPACING_Y = -200.0F * Settings.scale;
private static final float BUTTON_EXTRA_SPACING_Y = -70.0F * Settings.scale;
```

**배치 패턴:**
- 2x3 그리드 형태
- 좌측 열, 우측 열로 배치
- 홀수 개수일 경우 마지막 버튼은 중앙에 배치

### 스크롤 시스템

옵션이 6개를 초과하면 스크롤바가 활성화됩니다:

```java
// CampfireUI.java:60, 498-500
private static final int MAX_BUTTONS_BEFORE_SCROLL = 6;

private boolean shouldShowScrollBar() {
    return (this.buttons.size() > 6);
}
```

### 옵션 사용 불가 시스템

특정 조건에서 옵션이 비활성화됩니다:

**사용 불가 조건:**

1. **휴식 (Rest)**:
   - Coffee Dripper 유물 소지 시

2. **대장간 (Smith)**:
   - 업그레이드 가능한 카드가 없을 때
   - Fusion Hammer 유물 소지 시
   - Midas 모드 활성화 시

3. **유물 전용 옵션**:
   - Girya: 이미 3회 사용 완료 시
   - Peace Pipe: 제거 가능한 카드가 없을 때

### 랜덤 메시지 시스템

휴식처 진입 시 랜덤 메시지가 표시됩니다:

```java
// CampfireUI.java:406-423
private String getCampMessage() {
    ArrayList<String> msgs = new ArrayList<>();

    // 기본 메시지 4개
    msgs.add(TEXT[0]);
    msgs.add(TEXT[1]);
    msgs.add(TEXT[2]);
    msgs.add(TEXT[3]);

    // 옵션이 3개 이상이면 추가 메시지
    if (this.buttons.size() > 2) {
        msgs.add(TEXT[4]);
    }

    // HP가 50% 미만이면 추가 메시지 2개
    if (AbstractDungeon.player.currentHealth < AbstractDungeon.player.maxHealth / 2) {
        msgs.add(TEXT[5]);
        msgs.add(TEXT[6]);
    }

    // 랜덤 선택
    return msgs.get(MathUtils.random(msgs.size() - 1));
}
```

---

## 휴식 (Rest) 시스템

### 기본 회복량

휴식을 선택하면 HP를 회복합니다.

**회복량 계산:**

```java
// RestOption.java:19-49
public RestOption(boolean active) {
    int healAmt;

    this.label = TEXT[0];  // "Rest"
    this.usable = active;

    // Night Terrors 모드: 최대 HP의 100% 회복
    if (ModHelper.isModEnabled("Night Terrors")) {
        healAmt = (int)(AbstractDungeon.player.maxHealth * 1.0F);
    }
    // 일반: 최대 HP의 30% 회복
    else {
        healAmt = (int)(AbstractDungeon.player.maxHealth * 0.3F);
    }

    // Endless 모드 + Full Belly 주박: 회복량 절반
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("FullBelly")) {
        healAmt /= 2;
    }

    // 설명 텍스트 생성
    this.description = TEXT[3] + healAmt + ")" + LocalizedStrings.PERIOD;

    // Regal Pillow 유물 소지 시 +15 회복
    if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
        this.description += "\n+15" + TEXT[2] +
                           (AbstractDungeon.player.getRelic("Regal Pillow")).name +
                           LocalizedStrings.PERIOD;
    }

    updateUsability(active);
}
```

**회복량 공식:**

| 조건 | 회복량 |
|------|--------|
| 기본 | 최대 HP × 30% |
| Night Terrors 모드 | 최대 HP × 100% |
| Full Belly 주박 (Endless) | 위 회복량 ÷ 2 |
| Regal Pillow 유물 | 위 회복량 + 15 |

**예시 (최대 HP 80 기준):**
- 일반: 24 HP
- Regal Pillow: 24 + 15 = 39 HP
- Full Belly: 24 ÷ 2 = 12 HP
- Full Belly + Regal Pillow: 12 + 15 = 27 HP
- Night Terrors: 80 HP
- Night Terrors + Regal Pillow: 80 + 15 = 95 HP

### 휴식 실행 과정

휴식을 선택하면 `CampfireSleepEffect`가 실행됩니다:

```java
// RestOption.java:60-69
public void useOption() {
    // 담요 소리 재생
    CardCrawlGame.sound.play("SLEEP_BLANKET");

    // 휴식 효과 추가
    AbstractDungeon.effectList.add(new CampfireSleepEffect());

    // 화면 덮기 효과 30개 추가
    for (int i = 0; i < 30; i++) {
        AbstractDungeon.topLevelEffects.add(new CampfireSleepScreenCoverEffect());
    }

    // 메트릭 기록
    CardCrawlGame.metricData.campfire_rested++;
    CardCrawlGame.metricData.addCampfireChoiceData("REST");
}
```

### CampfireSleepEffect 상세

휴식 효과는 다음과 같은 단계로 진행됩니다:

```java
// CampfireSleepEffect.java:45-105
public CampfireSleepEffect() {
    // 지속 시간 설정 (Fast Mode: 1.5초, 일반: 3초)
    if (Settings.FAST_MODE) {
        this.startingDuration = 1.5F;
    } else {
        this.startingDuration = 3.0F;
    }
    this.duration = this.startingDuration;

    this.screenColor.a = 0.0F;

    // 모닥불 소리 페이드아웃
    ((RestRoom)AbstractDungeon.getCurrRoom()).cutFireSound();
    AbstractDungeon.overlayMenu.proceedButton.hide();

    // 회복량 계산
    if (ModHelper.isModEnabled("Night Terrors")) {
        this.healAmount = (int)(AbstractDungeon.player.maxHealth * 1.0F);
        AbstractDungeon.player.decreaseMaxHealth(5);  // 최대 HP -5
    } else {
        this.healAmount = (int)(AbstractDungeon.player.maxHealth * 0.3F);
    }

    // Regal Pillow 유물 효과
    if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
        this.healAmount += 15;
    }
}

public void update() {
    this.duration -= Gdx.graphics.getDeltaTime();
    updateBlackScreenColor();  // 화면 페이드 인/아웃

    // 0.5초 후 회복 실행
    if (this.duration < this.startingDuration - 0.5F && !this.hasHealed) {
        playSleepJingle();  // 막별 수면 효과음 재생
        this.hasHealed = true;

        // Regal Pillow 플래시
        if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
            AbstractDungeon.player.getRelic("Regal Pillow").flash();
        }

        // HP 회복
        AbstractDungeon.player.heal(this.healAmount, false);

        // 모든 유물의 onRest() 호출
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onRest();
        }
    }

    // 절반 지난 후 Dream Catcher 효과
    if (this.duration < this.startingDuration / 2.0F) {
        if (AbstractDungeon.player.hasRelic("Dream Catcher")) {
            AbstractDungeon.player.getRelic("Dream Catcher").flash();
            ArrayList<AbstractCard> rewardCards = AbstractDungeon.getRewardCards();
            if (rewardCards != null && !rewardCards.isEmpty()) {
                AbstractDungeon.cardRewardScreen.open(rewardCards, null, TEXT[0]);
            }
        }

        this.isDone = true;
        ((RestRoom)AbstractDungeon.getCurrRoom()).fadeIn();
        AbstractRoom.waitTimer = 0.0F;
        (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMPLETE;
    }
}
```

### 수면 효과음 (Sleep Jingle)

막별로 다른 수면 효과음이 재생됩니다:

```java
// CampfireSleepEffect.java:107-138
private void playSleepJingle() {
    int roll = MathUtils.random(0, 2);  // 0~2 중 랜덤

    switch (AbstractDungeon.id) {
        case "Exordium":  // 1막
            if (roll == 0) {
                CardCrawlGame.sound.play("SLEEP_1-1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("SLEEP_1-2");
            } else {
                CardCrawlGame.sound.play("SLEEP_1-3");
            }
            break;

        case "TheCity":  // 2막
            if (roll == 0) {
                CardCrawlGame.sound.play("SLEEP_2-1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("SLEEP_2-2");
            } else {
                CardCrawlGame.sound.play("SLEEP_2-3");
            }
            break;

        case "TheBeyond":  // 3막
            if (roll == 0) {
                CardCrawlGame.sound.play("SLEEP_3-1");
            } else if (roll == 1) {
                CardCrawlGame.sound.play("SLEEP_3-2");
            } else {
                CardCrawlGame.sound.play("SLEEP_3-3");
            }
            break;
    }
}
```

**효과음 목록:**
- 1막: `SLEEP_1-1`, `SLEEP_1-2`, `SLEEP_1-3`
- 2막: `SLEEP_2-1`, `SLEEP_2-2`, `SLEEP_2-3`
- 3막: `SLEEP_3-1`, `SLEEP_3-2`, `SLEEP_3-3`

---

## 대장간 (Smith) 시스템

### 업그레이드 가능 조건

대장간 옵션은 다음 조건을 만족할 때 활성화됩니다:

```java
// CampfireUI.java:93-95
this.buttons.add(new SmithOption(
    (AbstractDungeon.player.masterDeck.getUpgradableCards().size() > 0
     && !ModHelper.isModEnabled("Midas"))
));
```

**활성화 조건:**
1. 업그레이드 가능한 카드가 1장 이상 존재
2. Midas 모드가 비활성화 상태

**업그레이드 불가능한 카드:**
- 이미 최대 업그레이드된 카드
- 업그레이드 불가능한 특수 카드

### 대장간 실행 과정

대장간을 선택하면 `CampfireSmithEffect`가 실행됩니다:

```java
// SmithOption.java:25-28
public void useOption() {
    if (this.usable) {
        AbstractDungeon.effectList.add(new CampfireSmithEffect());
    }
}
```

### CampfireSmithEffect 상세

대장간 효과는 다음과 같은 단계로 진행됩니다:

```java
// CampfireSmithEffect.java:37-92
public CampfireSmithEffect() {
    this.duration = 1.5F;  // 고정 1.5초
    this.screenColor.a = 0.0F;
    AbstractDungeon.overlayMenu.proceedButton.hide();
}

public void update() {
    if (!AbstractDungeon.isScreenUp) {
        this.duration -= Gdx.graphics.getDeltaTime();
        updateBlackScreenColor();  // 화면 페이드 인/아웃
    }

    // 카드 선택 완료 시 업그레이드 실행
    if (!AbstractDungeon.isScreenUp &&
        !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty() &&
        AbstractDungeon.gridSelectScreen.forUpgrade) {

        for (AbstractCard c : AbstractDungeon.gridSelectScreen.selectedCards) {
            // 업그레이드 효과 표시
            AbstractDungeon.effectsQueue.add(new UpgradeShineEffect(
                Settings.WIDTH / 2.0F,
                Settings.HEIGHT / 2.0F
            ));

            // 메트릭 기록
            CardCrawlGame.metricData.campfire_upgraded++;
            CardCrawlGame.metricData.addCampfireChoiceData("SMITH", c.getMetricID());

            // 카드 업그레이드
            c.upgrade();

            // Bottled 유물 체크
            AbstractDungeon.player.bottledCardUpgradeCheck(c);

            // 업그레이드된 카드 표시
            AbstractDungeon.effectsQueue.add(new ShowCardBrieflyEffect(
                c.makeStatEquivalentCopy()
            ));
        }

        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        ((RestRoom)AbstractDungeon.getCurrRoom()).fadeIn();
    }

    // 1초 후 카드 선택 화면 표시
    if (this.duration < 1.0F && !this.openedScreen) {
        this.openedScreen = true;

        // 업그레이드 가능한 카드 목록으로 GridSelectScreen 열기
        AbstractDungeon.gridSelectScreen.open(
            AbstractDungeon.player.masterDeck.getUpgradableCards(),
            1,                    // 1장만 선택 가능
            TEXT[0],              // "Upgrade a card"
            true,                 // 업그레이드용
            false,                // 변환 불가
            true,                 // 정렬 가능
            false                 // 취소 불가
        );

        // 모든 유물의 onSmith() 호출
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onSmith();
        }
    }

    // 효과 종료
    if (this.duration < 0.0F) {
        this.isDone = true;
        if (CampfireUI.hidden) {
            AbstractRoom.waitTimer = 0.0F;
            (AbstractDungeon.getCurrRoom()).phase = AbstractRoom.RoomPhase.COMPLETE;
            ((RestRoom)AbstractDungeon.getCurrRoom()).cutFireSound();
        }
    }
}
```

### Bottled 유물 체크

카드 업그레이드 시 Bottled 유물(Bottled Flame, Bottled Lightning, Bottled Tornado)을 소지한 경우, 해당 카드가 병에 담긴 카드인지 확인하고 업그레이드합니다:

```java
// 업그레이드 후 Bottled 유물의 카드 참조 업데이트
AbstractDungeon.player.bottledCardUpgradeCheck(c);
```

---

## 회상 (Recall) 시스템

### 회상 옵션 표시 조건

회상 옵션은 다음 조건을 만족할 때만 표시됩니다:

```java
// CampfireUI.java:112-114
if (Settings.isFinalActAvailable && !Settings.hasRubyKey) {
    this.buttons.add(new RecallOption());
}
```

**표시 조건:**
1. `Settings.isFinalActAvailable` = true (Act 4 해금 상태)
2. `Settings.hasRubyKey` = false (Ruby Key 미획득)

**Ruby Key 획득 조건:**
- 3개의 특수 보스(Guardian, Hexaghost, Slime Boss)를 모두 격파해야 함
- 각 보스 격파 시 Emerald Key, Sapphire Key, Ruby Key 중 하나를 획득
- 3개 모두 획득하면 4막 진입 가능

### 회상 실행

회상을 선택하면 `CampfireRecallEffect`가 실행됩니다:

```java
// RecallOption.java:20-22
public void useOption() {
    AbstractDungeon.effectList.add(new CampfireRecallEffect());
}
```

**CampfireRecallEffect 기능:**
- 이전에 패배한 엘리트/보스 중 하나를 선택하여 재도전 가능
- Ruby Key 획득 기회 제공
- 추가 보상 획득 가능

---

## 유물 전용 옵션

### Girya (기리야) - Lift

**효과:** 힘 (Strength) +1 영구 증가

**사용 제한:** 최대 3회

```java
// Girya.java:59-61
public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
    // counter < 3일 때만 활성화
    options.add(new LiftOption((this.counter < 3)));
}
```

**LiftOption 실행:**
- 힘 +1 증가
- counter += 1
- counter가 3이 되면 더 이상 옵션이 표시되지 않음

### Peace Pipe (평화 파이프) - Toke

**효과:** 카드 1장 영구 제거

**사용 조건:** 제거 가능한 카드가 1장 이상 존재

```java
// PeacePipe.java:40-44
public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
    options.add(new TokeOption(
        !CardGroup.getGroupWithoutBottledCards(
            AbstractDungeon.player.masterDeck.getPurgeableCards()
        ).isEmpty()
    ));
}
```

**제거 불가능한 카드:**
- Bottled 유물에 담긴 카드
- Curse (저주) 카드 중 일부 (제거 불가 속성)

### Shovel (삽) - Dig

**효과:** 랜덤 유물 1개 획득

**사용 제한:** 없음 (무제한 사용 가능)

```java
// Shovel.java:40-42
public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
    options.add(new DigOption());
}
```

**DigOption 실행:**
- `AbstractDungeon.getCurrRoom().addRelicToRewards()` 호출
- 랜덤 유물 보상 추가
- 유물 등급은 일반 유물 풀에서 랜덤 선택

---

## 유물 효과

### 휴식처 진입 시 (onEnterRestRoom)

모든 유물의 `onEnterRestRoom()`이 호출됩니다:

```java
// RestRoom.java:33-35
for (AbstractRelic r : AbstractDungeon.player.relics) {
    r.onEnterRestRoom();
}
```

**관련 유물:**
- 대부분의 유물은 이 메서드를 오버라이드하지 않음
- 일부 유물은 특수 효과 발동 (예: 메시지 표시, 카운터 증가 등)

### 휴식 시 (onRest)

휴식 선택 시 모든 유물의 `onRest()`가 호출됩니다:

```java
// CampfireSleepEffect.java:84-86
for (AbstractRelic r : AbstractDungeon.player.relics) {
    r.onRest();
}
```

**관련 유물:**
- **Dream Catcher**: 카드 보상 화면 표시 (카드 1장 선택 가능)
- **Regal Pillow**: 회복량 +15
- 기타 다양한 유물들이 이 시점에 효과 발동

### 대장간 사용 시 (onSmith)

대장간 선택 시 모든 유물의 `onSmith()`가 호출됩니다:

```java
// CampfireSmithEffect.java:78-80
for (AbstractRelic r : AbstractDungeon.player.relics) {
    r.onSmith();
}
```

**관련 유물:**
- 일부 유물은 대장간 사용 시 특수 효과 발동
- 메트릭 기록용으로 주로 사용

### 옵션 추가 (addCampfireOption)

유물이 휴식처에 전용 옵션을 추가합니다:

```java
// CampfireUI.java:97-99
for (AbstractRelic r : AbstractDungeon.player.relics) {
    r.addCampfireOption(this.buttons);
}
```

**옵션 추가 유물:**
- **Girya**: Lift 옵션
- **Peace Pipe**: Toke 옵션
- **Shovel**: Dig 옵션

### 옵션 사용 가능 여부 (canUseCampfireOption)

유물이 특정 옵션의 사용을 제한합니다:

```java
// CampfireUI.java:102-108
for (AbstractCampfireOption co : this.buttons) {
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        if (!r.canUseCampfireOption(co)) {
            co.usable = false;
        }
    }
}
```

**옵션 제한 유물:**

**1. Coffee Dripper (커피 물방울)**:

```java
// CoffeeDripper.java:49-54
public boolean canUseCampfireOption(AbstractCampfireOption option) {
    if (option instanceof RestOption &&
        option.getClass().getName().equals(RestOption.class.getName())) {
        ((RestOption)option).updateUsability(false);
        return false;
    }
    return true;
}
```

- **효과**: 휴식 옵션 사용 불가
- **대신**: 매 전투 시작 시 에너지 +1

**2. Fusion Hammer (융합 망치)**:

```java
// FusionHammer.java:49-54
public boolean canUseCampfireOption(AbstractCampfireOption option) {
    if (option instanceof SmithOption &&
        option.getClass().getName().equals(SmithOption.class.getName())) {
        ((SmithOption)option).updateUsability(false);
        return false;
    }
    return true;
}
```

- **효과**: 대장간 옵션 사용 불가
- **대신**: 매 전투 시작 시 에너지 +1

### 유물 효과 우선순위

여러 유물이 동시에 영향을 미치는 경우:

**회복량 계산 순서:**
1. 기본 회복량 계산 (최대 HP × 30%)
2. Night Terrors 모드 체크 (최대 HP × 100%)
3. Full Belly 주박 체크 (÷ 2)
4. Regal Pillow 체크 (+ 15)

**옵션 제한:**
- Coffee Dripper가 있으면 휴식 불가 (다른 유물 무관)
- Fusion Hammer가 있으면 대장간 불가 (다른 유물 무관)

---

## 수정 방법

### 1. 회복량 변경

기본 회복량을 변경하려면 `RestOption` 생성자를 패치합니다:

```java
@SpirePatch(
    clz = RestOption.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class CustomHealAmountPatch {
    @SpireInsertPatch(
        locator = HealAmountLocator.class
    )
    public static SpireReturn<Void> Insert(RestOption __instance, boolean active) {
        // 회복량을 최대 HP의 50%로 변경
        int healAmt = (int)(AbstractDungeon.player.maxHealth * 0.5F);

        if (Settings.isEndless && AbstractDungeon.player.hasBlight("FullBelly")) {
            healAmt /= 2;
        }

        if (AbstractDungeon.player.hasRelic("Regal Pillow")) {
            healAmt += 15;
        }

        // description 필드 설정
        ReflectionHacks.setPrivate(__instance, RestOption.class,
                                   "description",
                                   "Heal " + healAmt + " HP.");

        return SpireReturn.Continue();
    }

    private static class HealAmountLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractDungeon.class,
                "player"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 2. 업그레이드 개수 변경

대장간에서 여러 장 업그레이드 가능하게 하려면 `CampfireSmithEffect`를 패치합니다:

```java
@SpirePatch(
    clz = CampfireSmithEffect.class,
    method = "update"
)
public static class MultipleUpgradesPatch {
    @SpireInsertPatch(
        locator = GridScreenLocator.class
    )
    public static SpireReturn<Void> Insert(CampfireSmithEffect __instance) {
        if (/* 조건 체크 */) {
            // 3장 업그레이드 가능
            AbstractDungeon.gridSelectScreen.open(
                AbstractDungeon.player.masterDeck.getUpgradableCards(),
                3,                    // 3장 선택 가능
                "Upgrade up to 3 cards",
                true,
                false,
                true,
                false
            );

            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onSmith();
            }

            // openedScreen 플래그 설정
            ReflectionHacks.setPrivate(__instance, CampfireSmithEffect.class,
                                       "openedScreen", true);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class GridScreenLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                GridCardSelectScreen.class,
                "open"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 3. 커스텀 휴식처 옵션 추가

새로운 유물로 커스텀 옵션을 추가하려면:

```java
public class CustomRelic extends AbstractRelic {
    public CustomRelic() {
        super("CustomRelic", "customRelic.png",
              AbstractRelic.RelicTier.RARE,
              AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
        // 커스텀 옵션 추가
        options.add(new CustomCampfireOption());
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CustomRelic();
    }
}
```

**커스텀 옵션 클래스:**

```java
public class CustomCampfireOption extends AbstractCampfireOption {
    public CustomCampfireOption() {
        this.label = "Custom Action";
        this.description = "Do something custom";
        this.img = ImageMaster.CAMPFIRE_REST_BUTTON;  // 버튼 이미지
    }

    @Override
    public void useOption() {
        // 커스텀 효과 구현
        AbstractDungeon.effectList.add(new CustomCampfireEffect());
    }
}
```

### 4. 휴식처 옵션 제한 변경

유물 전용 옵션 최대 개수 제한을 변경하려면:

```java
@SpirePatch(
    clz = Girya.class,
    method = "campfireRelicCount"
)
public static class IncreaseCampfireRelicLimitPatch {
    @SpirePrefixPatch
    public static SpireReturn<Boolean> Prefix() {
        int count = 0;
        if (AbstractDungeon.player.hasRelic("Girya")) count++;
        if (AbstractDungeon.player.hasRelic("Peace Pipe")) count++;
        if (AbstractDungeon.player.hasRelic("Shovel")) count++;

        // 제한을 3개로 변경
        return SpireReturn.Return(count < 3);
    }
}
```

### 5. 휴식 시 추가 효과

휴식 선택 시 추가 효과를 주려면 `onRest()`를 오버라이드합니다:

```java
public class CustomRestRelic extends AbstractRelic {
    public CustomRestRelic() {
        super("CustomRestRelic", "customRestRelic.png",
              AbstractRelic.RelicTier.UNCOMMON,
              AbstractRelic.LandingSound.MAGICAL);
    }

    @Override
    public void onRest() {
        // 휴식 시 골드 +50
        AbstractDungeon.player.gainGold(50);
        flash();

        // 메시지 표시
        AbstractDungeon.effectList.add(
            new FlashPowerEffect(this)
        );
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CustomRestRelic();
    }
}
```

### 6. 대장간 시 추가 효과

대장간 사용 시 추가 효과를 주려면 `onSmith()`를 오버라이드합니다:

```java
public class CustomSmithRelic extends AbstractRelic {
    public CustomSmithRelic() {
        super("CustomSmithRelic", "customSmithRelic.png",
              AbstractRelic.RelicTier.RARE,
              AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public void onSmith() {
        // 대장간 사용 시 랜덤 포션 획득
        AbstractDungeon.player.obtainPotion(
            AbstractDungeon.returnRandomPotion()
        );
        flash();
    }

    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CustomSmithRelic();
    }
}
```

### 7. 휴식처 옵션 개수 제한 해제

최대 6개 스크롤 제한을 변경하려면:

```java
@SpirePatch(
    clz = CampfireUI.class,
    method = SpirePatch.CLASS
)
public static class IncreaseMaxButtonsPatch {
    @SpireStaticPatch
    public static void Patch(CtBehavior ctBehavior) throws Exception {
        // MAX_BUTTONS_BEFORE_SCROLL 필드 값 변경
        CtField field = ctBehavior.getDeclaringClass()
                                  .getField("MAX_BUTTONS_BEFORE_SCROLL");
        ctBehavior.getDeclaringClass().removeField(field);

        CtField newField = new CtField(
            CtClass.intType,
            "MAX_BUTTONS_BEFORE_SCROLL",
            ctBehavior.getDeclaringClass()
        );
        newField.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
        ctBehavior.getDeclaringClass().addField(newField, "10");  // 10개로 변경
    }
}
```

### 8. 모든 카드 업그레이드 허용

대장간에서 이미 업그레이드된 카드도 선택 가능하게 하려면:

```java
@SpirePatch(
    clz = CampfireSmithEffect.class,
    method = "update"
)
public static class UpgradeAllCardsPatch {
    @SpireInsertPatch(
        locator = GridScreenLocator.class
    )
    public static SpireReturn<Void> Insert(CampfireSmithEffect __instance) {
        if (/* 조건 체크 */) {
            // 모든 카드 선택 가능
            AbstractDungeon.gridSelectScreen.open(
                AbstractDungeon.player.masterDeck.group,  // 모든 카드
                1,
                "Upgrade a card",
                true,
                false,
                true,
                false
            );

            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onSmith();
            }

            ReflectionHacks.setPrivate(__instance, CampfireSmithEffect.class,
                                       "openedScreen", true);

            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }

    private static class GridScreenLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                GridCardSelectScreen.class,
                "open"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 9. 휴식처 진입 시 자동 행동

휴식처 진입 시 자동으로 특정 행동을 수행하려면:

```java
@SpirePatch(
    clz = RestRoom.class,
    method = "onPlayerEntry"
)
public static class AutoRestPatch {
    @SpirePostfixPatch
    public static void Postfix(RestRoom __instance) {
        // 특정 조건에서 자동 휴식
        if (AbstractDungeon.player.currentHealth < AbstractDungeon.player.maxHealth / 2) {
            // 자동으로 휴식 선택
            AbstractDungeon.effectList.add(new CampfireSleepEffect());

            // CampfireUI 숨김
            CampfireUI.hidden = true;

            // Room Phase 변경
            AbstractRoom.waitTimer = 0.0F;
            __instance.phase = AbstractRoom.RoomPhase.COMPLETE;
        }
    }
}
```

### 10. 휴식처 메시지 커스터마이징

휴식처 메시지를 추가하거나 변경하려면:

```java
@SpirePatch(
    clz = CampfireUI.class,
    method = "getCampMessage"
)
public static class CustomMessagesPatch {
    @SpirePrefixPatch
    public static SpireReturn<String> Prefix(CampfireUI __instance) {
        ArrayList<String> msgs = new ArrayList<>();

        // 커스텀 메시지 추가
        msgs.add("Welcome to my custom campfire!");
        msgs.add("Time to rest and recover!");
        msgs.add("The fire crackles warmly...");
        msgs.add("You feel safe here.");

        // 조건부 메시지
        if (AbstractDungeon.player.currentHealth < AbstractDungeon.player.maxHealth / 2) {
            msgs.add("You desperately need rest!");
            msgs.add("The journey has been harsh...");
        }

        // 랜덤 선택
        return SpireReturn.Return(
            msgs.get(MathUtils.random(msgs.size() - 1))
        );
    }
}
```

---

## 관련 클래스

### 핵심 클래스

- **`RestRoom`** (`com.megacrit.cardcrawl.rooms.RestRoom`)
  - 휴식처 룸 클래스
  - CampfireUI 생성 및 관리
  - 모닥불 사운드 재생
  - 유물의 onEnterRestRoom() 호출

- **`CampfireUI`** (`com.megacrit.cardcrawl.rooms.CampfireUI`)
  - 휴식처 UI 및 로직 클래스
  - 옵션 버튼 생성 및 배치
  - 스크롤 시스템
  - 랜덤 메시지 표시

### 옵션 클래스

- **`AbstractCampfireOption`** (`com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption`)
  - 모든 휴식처 옵션의 기본 클래스
  - 버튼 위치, 이미지, 텍스트 관리
  - useOption() 메서드 (추상)

- **`RestOption`** (`com.megacrit.cardcrawl.ui.campfire.RestOption`)
  - 휴식 옵션 클래스
  - 회복량 계산
  - CampfireSleepEffect 호출

- **`SmithOption`** (`com.megacrit.cardcrawl.ui.campfire.SmithOption`)
  - 대장간 옵션 클래스
  - CampfireSmithEffect 호출

- **`RecallOption`** (`com.megacrit.cardcrawl.ui.campfire.RecallOption`)
  - 회상 옵션 클래스
  - CampfireRecallEffect 호출

- **`LiftOption`** (`com.megacrit.cardcrawl.ui.campfire.LiftOption`)
  - Girya 유물 옵션
  - 힘 +1 증가

- **`TokeOption`** (`com.megacrit.cardcrawl.ui.campfire.TokeOption`)
  - Peace Pipe 유물 옵션
  - 카드 제거

- **`DigOption`** (`com.megacrit.cardcrawl.ui.campfire.DigOption`)
  - Shovel 유물 옵션
  - 랜덤 유물 획득

### 효과 클래스

- **`CampfireSleepEffect`** (`com.megacrit.cardcrawl.vfx.campfire.CampfireSleepEffect`)
  - 휴식 효과 클래스
  - HP 회복 실행
  - 유물의 onRest() 호출
  - Dream Catcher 효과 처리

- **`CampfireSmithEffect`** (`com.megacrit.cardcrawl.vfx.campfire.CampfireSmithEffect`)
  - 대장간 효과 클래스
  - 카드 선택 화면 표시
  - 카드 업그레이드 실행
  - 유물의 onSmith() 호출

- **`CampfireRecallEffect`** (`com.megacrit.cardcrawl.vfx.campfire.CampfireRecallEffect`)
  - 회상 효과 클래스
  - 이전 보스/엘리트 재도전

- **`CampfireSleepScreenCoverEffect`** (`com.megacrit.cardcrawl.vfx.campfire.CampfireSleepScreenCoverEffect`)
  - 휴식 화면 덮기 효과

### 유물 클래스

- **`AbstractRelic`** (`com.megacrit.cardcrawl.relics.AbstractRelic`)
  - `onEnterRestRoom()` - 휴식처 진입 시 호출
  - `addCampfireOption(ArrayList<AbstractCampfireOption>)` - 옵션 추가
  - `canUseCampfireOption(AbstractCampfireOption)` - 옵션 사용 가능 여부
  - `onRest()` - 휴식 시 호출
  - `onSmith()` - 대장간 사용 시 호출

- **`RegalPillow`** (`com.megacrit.cardcrawl.relics.RegalPillow`)
  - 휴식 시 회복량 +15

- **`DreamCatcher`** (`com.megacrit.cardcrawl.relics.DreamCatcher`)
  - 휴식 후 카드 보상 화면 표시

- **`CoffeeDripper`** (`com.megacrit.cardcrawl.relics.CoffeeDripper`)
  - 휴식 옵션 사용 불가
  - 매 전투 시작 시 에너지 +1

- **`FusionHammer`** (`com.megacrit.cardcrawl.relics.FusionHammer`)
  - 대장간 옵션 사용 불가
  - 매 전투 시작 시 에너지 +1

- **`Girya`** (`com.megacrit.cardcrawl.relics.Girya`)
  - Lift 옵션 추가
  - 힘 +1 (최대 3회)

- **`PeacePipe`** (`com.megacrit.cardcrawl.relics.PeacePipe`)
  - Toke 옵션 추가
  - 카드 1장 영구 제거

- **`Shovel`** (`com.megacrit.cardcrawl.relics.Shovel`)
  - Dig 옵션 추가
  - 랜덤 유물 획득

### UI 클래스

- **`GridCardSelectScreen`** (`com.megacrit.cardcrawl.screens.select.GridCardSelectScreen`)
  - 카드 선택 화면
  - 대장간에서 업그레이드할 카드 선택 시 사용

- **`ScrollBar`** (`com.megacrit.cardcrawl.screens.mainMenu.ScrollBar`)
  - 스크롤바 클래스
  - 옵션이 6개 초과 시 표시
