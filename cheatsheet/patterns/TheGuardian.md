# TheGuardian (수호자) - Act 1 Boss

## 기본 정보

**ID**: `TheGuardian`
**이름**: The Guardian
**타입**: BOSS
**출현**: Act 1 보스
**체력**: 240 (A9+: 250)

## 핵심 메커니즘

### 1. 모드 전환 시스템 (Mode Shift)
Guardian은 두 가지 모드를 오가며 전투합니다:

#### Offensive Mode (공격 모드)
- **초기 상태**: 전투 시작 시
- **특징**:
  - 공격 위주 패턴
  - ModeShift 파워 보유 (데미지 임계값 추적)
  - 열린 자세 (더 큰 히트박스)

#### Defensive Mode (방어 모드)
- **진입 조건**: 공격 모드에서 누적 데미지 ≥ 임계값
- **특징**:
  - Sharp Hide 파워 획득 (가시 데미지)
  - 20 방어도 획득
  - 닫힌 자세 (작은 히트박스)
  - ModeShift 파워 제거

### 2. 데미지 임계값 (Damage Threshold)
**초기값**:
- 일반: 30
- A9+: 35
- A19+: 40

**임계값 증가**:
- 방어 모드 진입 시마다 +10
- 무한 증가 (30 → 40 → 50 → 60 ...)

**추적 메커니즘**:
```java
// 공격 모드에서만 추적
if (isOpen && !closeUpTriggered) {
    dmgTaken += damage_received;
    if (dmgTaken >= dmgThreshold) {
        → Defensive Mode 전환
        closeUpTriggered = true;
    }
}
```

### 3. Sharp Hide (가시 가죽)
**효과**: 근접 공격 시 반사 데미지
**데미지**: 3 (A19+: 4)
**획득 시점**: CLOSE_UP 행동 시
**제거 시점**: TWIN_SLAM 행동 시

## 공격 패턴

### 공격 모드 (Offensive Mode)
```
첫 턴: CHARGE_UP
→ FIERCE_BASH
→ VENT_STEAM
→ WHIRLWIND
→ CHARGE_UP
... (30-40 데미지 누적 시 방어 모드로 전환)
```

### 방어 모드 (Defensive Mode)
```
진입: CLOSE_UP (가시 가죽 획득)
→ ROLL_ATTACK
→ TWIN_SLAM (공격 모드로 복귀)
→ WHIRLWIND
→ CHARGE_UP
...
```

## 행동 상세

### 공격 모드 패턴

#### 1. CHARGE_UP (충전) - byte 6
**Intent**: DEFEND
**효과**:
- 9 방어도 획득
- 대사: `DIALOG[2]`
- 소리: "MONSTER_GUARDIAN_DESTROY"

**다음 행동**: FIERCE_BASH

#### 2. FIERCE_BASH (강타) - byte 2
**Intent**: ATTACK
**데미지**: 32 (A4+: 36)
**효과**:
- 강력한 단일 공격
- 느린 공격 애니메이션

**다음 행동**: VENT_STEAM

#### 3. VENT_STEAM (증기 분출) - byte 7
**Intent**: STRONG_DEBUFF
**효과**:
- 약화 2턴 부여
- 취약 2턴 부여

**다음 행동**: WHIRLWIND

#### 4. WHIRLWIND (회오리 공격) - byte 5
**Intent**: ATTACK (다단 공격)
**데미지**: 5 × 4회
**효과**:
- 4번의 연속 공격
- 소리: "ATTACK_WHIRLWIND", "ATTACK_HEAVY" (각 타격)
- CleaveEffect 시각 효과

**다음 행동**: CHARGE_UP

### 방어 모드 패턴

#### 5. CLOSE_UP (몸 웅크리기) - byte 1
**Intent**: BUFF
**효과**:
- Sharp Hide 파워 획득
  - 일반: 가시 데미지 3
  - A19+: 가시 데미지 4
- 대사: `DIALOG[1]`

**다음 행동**: ROLL_ATTACK

#### 6. ROLL_ATTACK (구르기 공격) - byte 3
**Intent**: ATTACK
**데미지**: 9 (A4+: 10)
**효과**:
- 단일 공격
- Sharp Hide 유지

**다음 행동**: TWIN_SLAM

#### 7. TWIN_SLAM (이중 강타) - byte 4
**Intent**: ATTACK_BUFF (다단 공격)
**데미지**: 8 × 2회
**효과**:
- 2번의 연속 공격
- Sharp Hide 제거
- 공격 모드로 전환 (changeState)

**다음 행동**: WHIRLWIND

## 모드 전환 메커니즘

### 공격 모드 → 방어 모드
**트리거**:
```java
// damage() 메서드에서
if (isOpen && !closeUpTriggered && damage >= threshold) {
    dmgTaken = 0;
    VFXAction - IntenseZoomEffect (화면 줌)
    ChangeStateAction - "Defensive Mode"
    closeUpTriggered = true;
}
```

**효과**:
```java
changeState("Defensive Mode") {
    1. RemoveSpecificPowerAction - "Mode Shift" 제거
    2. GainBlockAction - 20 방어도
    3. 애니메이션: "idle" → "transition" → "defensive"
    4. dmgThreshold += 10 (임계값 증가)
    5. setMove(CLOSE_UP)
    6. isOpen = false
    7. updateHitbox(440.0F, 250.0F) - 히트박스 축소
}
```

### 방어 모드 → 공격 모드
**트리거**: TWIN_SLAM 행동 시

**효과**:
```java
changeState("Offensive Mode") {
    1. ApplyPowerAction - ModeShift 파워 재부여
    2. ChangeStateAction - "Reset Threshold"
    3. LoseBlockAction - 모든 방어도 제거
    4. 애니메이션: "defensive" → "idle"
    5. isOpen = true
    6. closeUpTriggered = false
    7. updateHitbox(440.0F, 350.0F) - 히트박스 확대
}

changeState("Reset Threshold") {
    dmgTaken = 0; // 누적 데미지 초기화
}
```

## 승천 난이도별 변화

| 난이도 | 변경사항 |
|-------|---------|
| **A4+** | Fierce Bash 32→36, Roll Attack 9→10 |
| **A9+** | HP 240→250, 임계값 30→35 |
| **A19+** | 임계값 35→40, Sharp Hide 3→4 |

## 전투 단계별 전략

### Phase 1: 첫 공격 모드
```
Turn 1: CHARGE_UP (9 방어)
Turn 2: FIERCE_BASH (32-36 데미지)
Turn 3: VENT_STEAM (약화+취약 2턴)
Turn 4: WHIRLWIND (5×4 = 20 데미지)
```

**전략**:
- CHARGE_UP: 공격 집중
- FIERCE_BASH: 높은 방어력 필요 (32-36)
- VENT_STEAM: 디버프 해제 카드 준비
- WHIRLWIND: 방어력 20+ 권장

**데미지 관리**:
- 30-40 데미지 이내로 조절
- 임계값 근처에서 큰 공격 자제

### Phase 2: 첫 방어 모드
```
Turn X: CLOSE_UP (가시 3-4)
Turn X+1: ROLL_ATTACK (9-10 데미지)
Turn X+2: TWIN_SLAM (8×2 = 16 데미지)
```

**전략**:
- CLOSE_UP: 공격 중단, 파워/스킬 사용
- ROLL_ATTACK: 가시 데미지 주의 (3-4 반사)
- TWIN_SLAM: 총 16 데미지 대비

**가시 대처**:
- 공격 횟수 최소화
- 강한 단일 공격 우선
- 파워 카드 활용

### Phase 3+: 반복 사이클
```
공격 모드 (임계값 40+) → 방어 모드 → 공격 모드 (임계값 50+) ...
```

**전략**:
- 임계값이 계속 증가 (40 → 50 → 60 ...)
- 후반으로 갈수록 공격 모드 지속 시간 증가
- 장기전 대비 필수

## 위험 요소 분석

### 높음
- **FIERCE_BASH**: 32-36의 강력한 공격
- **WHIRLWIND**: 총 20 데미지 (다단 공격)
- **VENT_STEAM**: 약화+취약 동시 부여 (2턴)

### 중간
- **Sharp Hide**: 가시 3-4 반사 데미지
- **TWIN_SLAM**: 총 16 데미지
- **모드 전환**: 전략 변경 필요

### 낮음
- **CHARGE_UP**: 9 방어도만 획득
- **ROLL_ATTACK**: 9-10 데미지 (약함)

## 특수 상황

### 임계값 돌파
```java
// 한 번의 공격으로 임계값 초과
큰 데미지 → 방어 모드 즉시 전환
→ 현재 행동 완료 후 다음 턴에 CLOSE_UP
```

**중요**: `closeUpTriggered` 플래그로 턴당 1회만 전환

### 히트박스 변화
- **공격 모드**: 440.0F × 350.0F (크고 넓음)
- **방어 모드**: 440.0F × 250.0F (낮고 좁음)

→ 시각적 차이, 타겟팅 변화

### 방어도 관리
```java
// 공격 모드 전환 시
if (currentBlock != 0) {
    LoseBlockAction - 모든 방어도 제거
}
```

## 데이터 구조

### 데미지 인덱스
```java
damage.get(0) = fierceBashDamage (32-36)
damage.get(1) = rollDamage (9-10)
damage.get(2) = whirlwindDamage (5)
damage.get(3) = twinSlamDamage (8)
```

### 바이트 코드
```java
CLOSE_UP = 1
FIERCE_BASH = 2
ROLL_ATTACK = 3
TWIN_SLAM = 4
WHIRLWIND = 5
CHARGE_UP = 6
VENT_STEAM = 7
```

### 상태 플래그
```java
isOpen: boolean - 공격 모드 여부
closeUpTriggered: boolean - 방어 모드 전환 플래그
dmgTaken: int - 누적 데미지
dmgThreshold: int - 현재 임계값
dmgThresholdIncrease: int = 10 - 임계값 증가량
```

## AI 로직 요약

```java
usePreBattleAction() {
    ApplyPowerAction - ModeShift(dmgThreshold)
    ChangeStateAction - "Reset Threshold"
}

takeTurn() {
    switch (nextMove) {
        case CLOSE_UP(1):
            - Sharp Hide 부여 (3 or 4)
            - 다음: ROLL_ATTACK

        case FIERCE_BASH(2):
            - 32-36 데미지
            - 다음: VENT_STEAM

        case VENT_STEAM(7):
            - 약화 2턴
            - 취약 2턴
            - 다음: WHIRLWIND

        case ROLL_ATTACK(3):
            - 9-10 데미지
            - 다음: TWIN_SLAM

        case TWIN_SLAM(4):
            - 8×2 데미지
            - Sharp Hide 제거
            - 공격 모드 전환
            - 다음: WHIRLWIND

        case WHIRLWIND(5):
            - 5×4 데미지
            - 다음: CHARGE_UP

        case CHARGE_UP(6):
            - 9 방어도
            - 다음: FIERCE_BASH
    }
}

damage(info) {
    int tmpHealth = currentHealth;
    super.damage(info);

    if (isOpen && !closeUpTriggered && !isDying) {
        dmgTaken += (tmpHealth - currentHealth);

        // ModeShift 파워 업데이트
        if (hasPower("Mode Shift")) {
            power.amount -= damage;
            power.updateDescription();
        }

        // 임계값 도달 시
        if (dmgTaken >= dmgThreshold) {
            dmgTaken = 0;
            VFXAction - IntenseZoomEffect
            ChangeStateAction - "Defensive Mode"
            closeUpTriggered = true;
        }
    }
}

changeState(stateName) {
    switch (stateName) {
        case "Defensive Mode":
            - ModeShift 제거
            - 20 방어도 획득
            - 애니메이션 전환
            - dmgThreshold += 10
            - setMove(CLOSE_UP)
            - isOpen = false
            - 히트박스 축소

        case "Offensive Mode":
            - ModeShift 재부여
            - "Reset Threshold" 호출
            - 방어도 제거
            - 애니메이션 전환
            - isOpen = true
            - closeUpTriggered = false
            - 히트박스 확대

        case "Reset Threshold":
            - dmgTaken = 0
    }
}

getMove(num) {
    if (isOpen) {
        setMove(CHARGE_UP); // 공격 모드 시작
    } else {
        setMove(ROLL_ATTACK); // 방어 모드 시작
    }
}
```

## 전투 흐름 다이어그램

```
전투 시작
   ↓
[공격 모드] ModeShift 30-40 부여
   ↓
CHARGE_UP (9 방어)
   ↓
FIERCE_BASH (32-36 dmg)
   ↓
VENT_STEAM (약화+취약)
   ↓
WHIRLWIND (5×4 dmg)
   ↓
누적 데미지 ≥ 임계값?
   ↓ YES
[방어 모드] 임계값 +10
   ↓
CLOSE_UP (가시 3-4)
   ↓
ROLL_ATTACK (9-10 dmg)
   ↓
TWIN_SLAM (8×2 dmg)
   ↓
[공격 모드] ModeShift 재부여
   ↓
... 반복 (임계값 계속 증가) ...
```

## 카운터 전략

### 공격 모드 대처
1. **데미지 조절**: 임계값 근처에서 약한 공격
2. **버스트 타이밍**: 방어 모드 직전 큰 피해
3. **디버프 대비**: VENT_STEAM 후 정화 효과

### 방어 모드 대처
1. **가시 회피**: 파워/스킬 카드 사용
2. **강한 단일 공격**: 공격 횟수 최소화
3. **빠른 돌파**: TWIN_SLAM 전 처치 시도

### 장기전 준비
- 임계값이 60, 70, 80... 계속 증가
- 공격 모드 지속 시간 증가
- 방어 카드/유물 확보 필수

## 사운드 효과

| 행동 | 사운드 |
|-----|--------|
| CLOSE_UP | "GUARDIAN_ROLL_UP" |
| CHARGE_UP | "MONSTER_GUARDIAN_DESTROY" |
| WHIRLWIND | "ATTACK_WHIRLWIND", "ATTACK_HEAVY" |

## 업적 (Achievements)

```java
// 전투 시작 시
UnlockTracker.markBossAsSeen("GUARDIAN");

// 승리 시
UnlockTracker.hardUnlockOverride("GUARDIAN");
UnlockTracker.unlockAchievement("GUARDIAN");
```

## 디버그 정보

### 애니메이션
- Skeleton: `images/monsters/theBottom/boss/guardian/skeleton.atlas`
- JSON: `images/monsters/theBottom/boss/guardian/skeleton.json`
- Scale: 2.0F

### 애니메이션 상태
- **공격 모드**: "idle" (반복)
- **전환**: "transition" (1회) → "defensive"
- **방어 모드**: "defensive" (반복)

### 로그 확인
```java
logger.info("ERROR"); // takeTurn에서 잘못된 nextMove
```

## 모드 제작 시 주의사항

1. **모드 전환 시스템**
   - `isOpen` 플래그로 모드 구분
   - `closeUpTriggered` 플래그로 턴당 1회 전환
   - `changeState()` 메서드로 모든 전환 처리

2. **데미지 추적**
   - `damage()` 메서드 오버라이드 필수
   - `tmpHealth - currentHealth` 계산
   - ModeShift 파워 동기화

3. **임계값 관리**
   - 전투 시작: 30-40
   - 방어 모드 진입마다 +10
   - 무한 증가 가능

4. **히트박스 조정**
   - `updateHitbox()` 호출
   - `healthBarUpdatedEvent()` 호출
   - 모드별 크기 다름

5. **파워 관리**
   - ModeShift: 공격 모드에만 존재
   - Sharp Hide: CLOSE_UP에서 부여, TWIN_SLAM에서 제거
   - 정확한 타이밍 중요
