# Hexaghost (육각령) - Act 1 Boss

## 기본 정보

**ID**: `Hexaghost`
**이름**: Hexaghost
**타입**: BOSS
**출현**: Act 1 보스
**체력**: 250 (A9+: 264)

## 핵심 메커니즘

### 1. 오브 시스템 (Orb System)
Hexaghost는 6개의 오브(구체)를 가지고 있으며, 전투 중 하나씩 활성화됩니다.

#### 오브 배치 (육각형)
```
        [0]         [1]
    (-90, 380)  (90, 380)

[5]                      [2]
(-160, 250)          (160, 250)

    [4]         [3]
    (-90, 120)  (90, 120)
```

#### 오브 활성화 순서
```
첫 턴: 모든 오브 동시 활성화 (ACTIVATE)
이후: 매 턴마다 1개씩 활성화
0 → 1 → 2 → 3 → 4 → 5 (순차)
```

#### 오브 카운트 (orbActiveCount)
- **0개**: SEAR 공격
- **1개**: TACKLE 공격 (오브 +1)
- **2개**: SEAR 공격 (오브 +1)
- **3개**: INFLAME 버프 (오브 +1)
- **4개**: TACKLE 공격 (오브 +1)
- **5개**: SEAR 공격 (오브 +1)
- **6개**: INFERNO 공격 (모든 오브 비활성화)

### 2. Burn 카드 업그레이드
- **초기**: 일반 Burn 카드 추가
- **INFERNO 이후**: Burn+ (업그레이드) 카드 추가
- **플래그**: `burnUpgraded`

### 3. 특수 구성 요소
- **HexaghostBody**: 별도의 몸체 렌더링
- **HexaghostOrb**: 6개의 독립 오브 객체
- **위치 동기화**: 오브가 본체 위치 추적

## 공격 패턴

### 전체 순환 패턴
```
Turn 1: ACTIVATE (6개 오브 동시 활성화)
→ Turn 2: DIVIDER (6회 연속 공격)
→ Turn 3: SEAR (0개)
→ Turn 4: TACKLE (1개)
→ Turn 5: SEAR (2개)
→ Turn 6: INFLAME (3개)
→ Turn 7: TACKLE (4개)
→ Turn 8: SEAR (5개)
→ Turn 9: INFERNO (6개, 모든 오브 비활성화)
→ Turn 10: SEAR (0개)
... 반복
```

**중요**: 첫 턴 이후는 오브 카운트에 따라 자동 결정

## 행동 상세

### Phase 1: 활성화 (첫 턴)

#### 1. ACTIVATE (활성화) - byte 5
**Intent**: UNKNOWN
**효과**:
```java
1. changeState("Activate")
   - 배경음악 전환: "BOSS_BOTTOM"
   - 모든 오브(6개) 동시 활성화
   - orbActiveCount = 6
   - body.targetRotationSpeed = 120.0F (회전 시작)

2. DIVIDER 데미지 계산
   - damage = (플레이어 현재 체력 / 12) + 1
   - 최소 1 데미지 보장

3. setMove(DIVIDER, 6회 공격)
```

**특징**:
- 전투 시작 신호
- 플레이어 체력 비례 데미지
- 음악/시각 효과 극대화

### Phase 2: 연속 공격

#### 2. DIVIDER (연속 공격) - byte 1
**Intent**: ATTACK (다단 공격)
**데미지**: (플레이어 체력/12 + 1) × 6회
**효과**:
```java
for (6회) {
    GhostIgniteEffect (랜덤 위치)
    SFXAction - "GHOST_ORB_IGNITE_1" or "2"
    DamageAction - 계산된 데미지
}
changeState("Deactivate") - 모든 오브 비활성화
orbActiveCount = 0
```

**다음 행동**: getMove()로 자동 결정 (SEAR)

### Phase 3: 오브 순차 활성화 (2~9턴)

#### 3. SEAR (화염구) - byte 4
**Intent**: ATTACK_DEBUFF
**데미지**: 6
**발생 시점**: 오브 0, 2, 5개
**효과**:
- 6 데미지 공격 (FireballEffect)
- Burn 카드 추가:
  - `burnUpgraded == false`: Burn × 1-2장
  - `burnUpgraded == true`: Burn+ × 1-2장
- 오브 1개 활성화 (`changeState("Activate Orb")`)

**다음 행동**: 오브 카운트 기반 자동 결정

#### 4. TACKLE (돌진 공격) - byte 2
**Intent**: ATTACK (다단 공격)
**데미지**: 5 (A4+: 6) × 2회
**발생 시점**: 오브 1, 4개
**효과**:
- 2번 연속 공격 (녹색 번쩍임)
- 오브 1개 활성화

**다음 행동**: 오브 카운트 기반 자동 결정

#### 5. INFLAME (분노) - byte 3
**Intent**: DEFEND_BUFF
**발생 시점**: 오브 3개
**효과**:
- 12 방어도 획득
- 힘 +2 (A19+: +3)
- 오브 1개 활성화
- InflameEffect 시각 효과

**다음 행동**: 오브 카운트 기반 자동 결정

### Phase 4: 대폭발 (9턴)

#### 6. INFERNO (지옥불) - byte 6
**Intent**: ATTACK_DEBUFF (다단 공격)
**데미지**: 2 (A4+: 3) × 6회
**발생 시점**: 오브 6개 (모두 활성화)
**효과**:
```java
ScreenOnFireEffect - 화면 전체 불타는 효과
for (6회) {
    DamageAction - 2-3 데미지
}
BurnIncreaseAction - 덱의 모든 Burn 데미지 +2
if (!burnUpgraded) {
    burnUpgraded = true; // 이후 Burn+ 추가
}
changeState("Deactivate") - 모든 오브 비활성화
orbActiveCount = 0
```

**다음 행동**: SEAR (오브 0개 상태)

**중요**:
- Burn 카드 영구 업그레이드
- 모든 오브 초기화 (사이클 재시작)

## 승천 난이도별 변화

| 난이도 | 변경사항 |
|-------|---------|
| **A4+** | Tackle 5→6, Inferno 2→3 |
| **A9+** | HP 250→264 |
| **A19+** | Inflame 힘 +2→+3, Sear Burn 1→2장 |

## 오브 상태 관리

### 활성화 (Activate)
```java
// "Activate" - 모든 오브 동시
for (all orbs) {
    orb.activate(drawX, drawY);
}
orbActiveCount = 6;
body.targetRotationSpeed = 120.0F;
```

### 순차 활성화 (Activate Orb)
```java
// "Activate Orb" - 다음 1개
for (orb in orbs) {
    if (!orb.activated) {
        orb.activate(drawX, drawY);
        break;
    }
}
orbActiveCount++;
```

### 비활성화 (Deactivate)
```java
// "Deactivate" - 모든 오브
for (all orbs) {
    orb.deactivate();
}
SFXAction - "CARD_EXHAUST" (2회)
orbActiveCount = 0;
```

## 전투 단계별 전략

### Stage 1: 활성화 단계 (턴 1-2)
```
Turn 1: ACTIVATE
→ 음악 변경, 6개 오브 활성화
Turn 2: DIVIDER
→ (체력/12 + 1) × 6회 공격
```

**전략**:
- 체력이 높을수록 DIVIDER 데미지 증가
- 예시: 80HP → (80/12+1) × 6 = 42 데미지
- 높은 방어력 준비 필수

### Stage 2: 오브 증가 단계 (턴 3-8)
```
Turn 3: SEAR (6 dmg, Burn 1-2장) - 오브 0→1
Turn 4: TACKLE (5-6×2 dmg) - 오브 1→2
Turn 5: SEAR (6 dmg, Burn 1-2장) - 오브 2→3
Turn 6: INFLAME (12방어, 힘+2-3) - 오브 3→4
Turn 7: TACKLE (5-6×2 dmg) - 오브 4→5
Turn 8: SEAR (6 dmg, Burn 1-2장) - 오브 5→6
```

**전략**:
- **SEAR (3회)**: Burn 누적 방지 (정화/버리기)
- **TACKLE (2회)**: 총 20-24 데미지
- **INFLAME (1회)**: 버프 제거 or 빠른 처치

### Stage 3: 폭발 단계 (턴 9)
```
Turn 9: INFERNO (2-3×6 dmg, Burn 업그레이드)
```

**전략**:
- 총 12-18 데미지 (다단 공격)
- **중요**: 모든 Burn 카드 영구 강화
- 이후 Burn+ (2 → 4 데미지)

### Stage 4: 재순환 (턴 10+)
```
Turn 10: SEAR (오브 0개)
... Stage 2 반복 ...
Turn 17: INFERNO (두 번째 폭발)
```

**전략**:
- 장기전 시 Burn 축적 심각
- 정화 카드/유물 필수
- 오브 6개 도달 전 처치 권장

## 위험 요소 분석

### 높음
- **DIVIDER**: 플레이어 체력 비례 (최대 15+ × 6)
- **INFERNO**: Burn 카드 영구 업그레이드
- **Burn 누적**: 3회 SEAR로 6-12장 (A19: 12장)

### 중간
- **TACKLE**: 총 10-12 데미지 (2회)
- **INFLAME**: 힘 +2-3 (누적 시 위험)

### 낮음
- **SEAR**: 6 데미지 (단일 공격)
- **ACTIVATE**: 데미지 없음

## 특수 상황

### Burn 카드 관리
```java
// SEAR 행동
Burn c = new Burn();
if (burnUpgraded) {
    c.upgrade(); // Burn+ (2 데미지 → 4 데미지)
}
MakeTempCardInDiscardAction(c, 1-2장);
```

**누적 예시**:
- Turn 3: Burn 1-2장
- Turn 5: Burn 1-2장
- Turn 8: Burn 1-2장
- Turn 9: INFERNO (모든 Burn → Burn+)
- Turn 10: Burn+ 1-2장
- ...

### 체력 기반 데미지 (DIVIDER)
```java
int d = AbstractDungeon.player.currentHealth / 12 + 1;
damage.get(2).base = d;
```

| 플레이어 체력 | DIVIDER 1회 | 총 데미지 (×6) |
|------------|------------|--------------|
| 12-23 HP | 2 | 12 |
| 24-35 HP | 3 | 18 |
| 36-47 HP | 4 | 24 |
| 60-71 HP | 6 | 36 |
| 84-95 HP | 8 | 48 |

### 오브 렌더링
```java
update() {
    super.update();
    body.update();
    for (orb in orbs) {
        orb.update(drawX + animX, drawY + animY);
    }
}

render(sb) {
    body.render(sb); // 몸체 먼저
    super.render(sb); // 코어
    // 오브는 별도 렌더링
}
```

## 데이터 구조

### 데미지 인덱스
```java
damage.get(0) = fireTackleDmg (5-6)
damage.get(1) = searDmg (6)
damage.get(2) = dividerDmg (동적 계산)
damage.get(3) = infernoDmg (2-3)
```

### 바이트 코드
```java
DIVIDER = 1
TACKLE = 2
INFLAME = 3
SEAR = 4
ACTIVATE = 5
INFERNO = 6
```

### 상태 플래그
```java
activated: boolean - 첫 활성화 여부
burnUpgraded: boolean - Burn 업그레이드 여부
orbActiveCount: int - 활성화된 오브 개수 (0-6)
```

### 오브 시스템
```java
orbs: ArrayList<HexaghostOrb> - 6개 오브 객체
body: HexaghostBody - 별도 몸체 객체
```

## AI 로직 요약

```java
usePreBattleAction() {
    UnlockTracker.markBossAsSeen("GHOST");
    CardCrawlGame.music.precacheTempBgm("BOSS_BOTTOM");
}

constructor() {
    body = new HexaghostBody(this);
    createOrbs(); // 6개 오브 생성 (육각형 배치)
}

takeTurn() {
    switch (nextMove) {
        case ACTIVATE(5):
            - changeState("Activate") - 6개 오브 활성화
            - DIVIDER 데미지 = (체력/12) + 1
            - 다음: DIVIDER

        case DIVIDER(1):
            - 6회 연속 공격
            - changeState("Deactivate") - 모든 오브 비활성화
            - 다음: getMove() (SEAR)

        case SEAR(4):
            - 6 데미지
            - Burn (or Burn+) 1-2장
            - changeState("Activate Orb") - 오브 +1
            - 다음: getMove()

        case TACKLE(2):
            - 5-6 데미지 × 2회
            - changeState("Activate Orb") - 오브 +1
            - 다음: getMove()

        case INFLAME(3):
            - 12 방어도
            - 힘 +2-3
            - changeState("Activate Orb") - 오브 +1
            - 다음: getMove()

        case INFERNO(6):
            - 2-3 데미지 × 6회
            - BurnIncreaseAction (모든 Burn +2)
            - burnUpgraded = true
            - changeState("Deactivate") - 모든 오브 비활성화
            - 다음: getMove() (SEAR)
    }
}

getMove(num) {
    if (!activated) {
        activated = true;
        setMove(ACTIVATE);
    } else {
        switch (orbActiveCount) {
            case 0: setMove(SEAR);
            case 1: setMove(TACKLE, 2회);
            case 2: setMove(SEAR);
            case 3: setMove(INFLAME);
            case 4: setMove(TACKLE, 2회);
            case 5: setMove(SEAR);
            case 6: setMove(INFERNO, 6회);
        }
    }
}

changeState(stateName) {
    switch (stateName) {
        case "Activate":
            - 배경음악 전환
            - 모든 오브 활성화
            - orbActiveCount = 6
            - body.targetRotationSpeed = 120.0F

        case "Activate Orb":
            - 다음 비활성화 오브 1개 활성화
            - orbActiveCount++
            - if (orbActiveCount == 6) setMove(INFERNO)

        case "Deactivate":
            - 모든 오브 비활성화
            - SFX: "CARD_EXHAUST" (2회)
            - orbActiveCount = 0
    }
}

die() {
    for (all orbs) {
        orb.hide();
    }
    onBossVictoryLogic();
    UnlockTracker.hardUnlockOverride("GHOST");
    UnlockTracker.unlockAchievement("GHOST_GUARDIAN");
}
```

## 전투 흐름 다이어그램

```
전투 시작
   ↓
[Turn 1] ACTIVATE
   - 6개 오브 활성화
   - 음악 전환
   - DIVIDER 데미지 계산
   ↓
[Turn 2] DIVIDER
   - (체력/12+1) × 6회 공격
   - 모든 오브 비활성화
   - orbActiveCount = 0
   ↓
[Turn 3] SEAR (0→1)
   - 6 dmg, Burn 1-2장
   ↓
[Turn 4] TACKLE (1→2)
   - 5-6 × 2 dmg
   ↓
[Turn 5] SEAR (2→3)
   - 6 dmg, Burn 1-2장
   ↓
[Turn 6] INFLAME (3→4)
   - 12 방어, 힘 +2-3
   ↓
[Turn 7] TACKLE (4→5)
   - 5-6 × 2 dmg
   ↓
[Turn 8] SEAR (5→6)
   - 6 dmg, Burn 1-2장
   ↓
[Turn 9] INFERNO (6→0)
   - 2-3 × 6 dmg
   - 모든 Burn → Burn+
   - 모든 오브 비활성화
   ↓
[Turn 10+] 사이클 반복
   - SEAR (Burn+ 추가)
   - ...
```

## 카운터 전략

### 초반 대처 (턴 1-2)
1. **체력 관리**: DIVIDER 데미지 감소
2. **높은 방어력**: 42+ 데미지 대비
3. **회복 유물**: 체력 회복으로 데미지 감소

### 중반 관리 (턴 3-8)
1. **Burn 제거**: 정화 카드/유물
2. **버프 제거**: INFLAME 힘 버프 해제
3. **안정적 방어**: 15-20 방어력 유지

### INFERNO 대비 (턴 9)
1. **덱 정리**: Burn 카드 최소화
2. **방어 준비**: 12-18 데미지 대비
3. **빠른 처치**: 오브 6개 전 승리

### 장기전 전략
1. **Burn 관리**: Burn+ (4 데미지) 누적 위험
2. **힘 버프**: 여러 INFERNO 후 누적
3. **정화 필수**: 지속적인 Burn 제거

## 사운드 효과

| 행동 | 사운드 |
|-----|--------|
| ACTIVATE | "BOSS_BOTTOM" (음악) |
| DIVIDER | "GHOST_ORB_IGNITE_1/2" (각 타격) |
| Orb Activate | (없음) |
| Deactivate | "CARD_EXHAUST" (2회) |

## 업적 (Achievements)

```java
// 전투 시작 시
UnlockTracker.markBossAsSeen("GHOST");

// 승리 시
UnlockTracker.hardUnlockOverride("GHOST");
UnlockTracker.unlockAchievement("GHOST_GUARDIAN");
```

## 디버그 정보

### 이미지 경로
```java
IMAGE = "images/monsters/theBottom/boss/ghost/core.png"
```

### 렌더링 순서
```
1. HexaghostBody (몸체)
2. Hexaghost (코어)
3. HexaghostOrb × 6 (오브들)
```

### 위치 동기화
```java
update() {
    body.update();
    for (orb : orbs) {
        orb.update(drawX + animX, drawY + animY);
    }
}
```

### 로그 확인
```java
logger.info("ERROR: Default Take Turn was called on " + name);
```

## 모드 제작 시 주의사항

1. **오브 시스템**
   - `HexaghostOrb` 클래스 별도 관리
   - 위치 동기화 필수 (`drawX + animX`, `drawY + animY`)
   - 6개 육각형 배치

2. **오브 카운트**
   - `orbActiveCount` 정확히 추적 (0-6)
   - `getMove()`에서 자동 패턴 결정
   - INFERNO 후 0으로 초기화

3. **Burn 업그레이드**
   - `burnUpgraded` 플래그 관리
   - INFERNO 후 true로 설정
   - 이후 모든 Burn은 Burn+

4. **상태 전환**
   - `changeState()` 메서드 사용
   - "Activate", "Activate Orb", "Deactivate"
   - 음악/회전 효과 관리

5. **동적 데미지**
   - DIVIDER는 플레이어 체력 기반
   - `applyPowers()` 후 재계산
   - `damage.get(2).base` 업데이트

6. **렌더링**
   - `body` 렌더링 우선
   - `super.render()` 후 오브
   - `disposables.add(body)` 메모리 관리

7. **사망 처리**
   - 모든 오브 `hide()` 호출
   - 메모리 누수 방지
   - 업적 해제

## 핵심 전략 요약

### 승리 전략
1. **빠른 처치**: 첫 INFERNO 전 (9턴 이내)
2. **Burn 관리**: 정화 카드/유물 확보
3. **체력 유지**: DIVIDER 데미지 최소화

### 회피 전략
1. **장기전 회피**: Burn+ 누적 위험
2. **힘 버프 제거**: INFLAME 무력화
3. **오브 6개 방지**: 8턴 이내 처치
