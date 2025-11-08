# SlimeBoss (대왕 슬라임) - Act 1 Boss

## 기본 정보

**ID**: `SlimeBoss`
**이름**: Slime Boss
**타입**: BOSS
**출현**: Act 1 보스
**체력**: 140 (A9+: 150)

## 핵심 메커니즘

### 1. 분열 시스템 (Split Mechanic)
- **트리거**: 체력이 최대 체력의 50% 이하로 떨어질 때
- **효과**:
  - 현재 진행 중인 행동 즉시 중단 (`INTERRUPTED` 텍스트 표시)
  - 분열 (SPLIT) 행동으로 강제 전환
  - 자신을 제거하고 두 개의 대형 슬라임 소환
- **소환물**:
  - `SpikeSlime_L` (-385.0F, 20.0F) - 현재 체력 상속
  - `AcidSlime_L` (120.0F, -8.0F) - 현재 체력 상속

**중요**: 분열은 한 번만 발생하며, `nextMove != 3` 조건으로 중복 방지

### 2. 고유 파워
- **Split Power**: 전투 시작 시 자동 부여, 분열 가능 상태 표시

## 공격 패턴

### 첫 턴 고정 패턴
```
Turn 1: STICKY (끈적끈적)
```
첫 턴은 항상 STICKY로 시작 (`firstTurn` 플래그)

### 순환 패턴 (첫 턴 이후)
```
STICKY → PREP_SLAM → SLAM → (SPLIT or STICKY)
```

**체력 50% 이하 시 언제든 SPLIT으로 중단**

## 행동 상세

### 1. STICKY (끈적끈적) - byte 4
**Intent**: STRONG_DEBUFF
**효과**:
- A19+: Slimed 카드 5장 버리기 더미에 추가
- 일반: Slimed 카드 3장 버리기 더미에 추가

**다음 행동**: PREP_SLAM

### 2. PREP_SLAM (준비) - byte 2
**Intent**: UNKNOWN
**효과**:
- 대사 출력: `DIALOG[0]`
- 화면 진동 (LOW 강도, LONG 지속)
- 시각적/청각적 경고

**다음 행동**: SLAM

### 3. SLAM (몸통박치기) - byte 1
**데미지**: 35 (A4+: 38)
**Intent**: ATTACK
**효과**:
- 점프 애니메이션
- 강력한 단일 공격 (WeightyImpactEffect)
- 0.8초 대기 후 피해 적용
- 공격 타입: POISON

**다음 행동**: STICKY

### 4. SPLIT (분열) - byte 3
**Intent**: UNKNOWN
**트리거**: 체력 50% 이하 도달 시 자동 발동
**효과**:
```java
1. CannotLoseAction - 죽지 않는 상태 활성화
2. AnimateShakeAction - 떨림 애니메이션 (1.0초)
3. HideHealthBarAction - 체력바 숨김
4. SuicideAction - 자살 (전투 패배 없음)
5. 1.0초 대기
6. SFXAction - "SLIME_SPLIT" 소리
7. SpawnMonsterAction - SpikeSlime_L 소환 (현재 체력)
8. SpawnMonsterAction - AcidSlime_L 소환 (현재 체력)
9. CanLoseAction - 죽을 수 있는 상태 복구
```

**중요 로직**:
```java
// damage() 메서드에서 체력 50% 체크
if (!isDying && currentHealth <= maxHealth / 2.0F && nextMove != 3) {
    // 즉시 분열로 전환
    setMove(SPLIT_NAME, (byte)3, Intent.UNKNOWN);
    createIntent();
    TextAboveCreatureAction - INTERRUPTED
    SetMoveAction - 다음 턴 분열 확정
}
```

## 승천 난이도별 변화

| 난이도 | 변경사항 |
|-------|---------|
| **A4+** | Tackle 9→10, Slam 35→38 |
| **A9+** | HP 140→150 |
| **A19+** | STICKY: Slimed 3장→5장 |

## 전투 단계별 전략

### Phase 1: 풀체력 (100%~51%)
```
Turn 1: STICKY (Slimed 3-5장)
Turn 2: PREP_SLAM (경고)
Turn 3: SLAM (35-38 데미지)
Turn 4: STICKY (Slimed 3-5장)
... 반복
```

**전략**:
- STICKY 턴에 방어력 무시하고 공격 집중
- PREP_SLAM 턴에 방어 태세 준비
- SLAM 직전 턴에 높은 방어력 확보

### Phase 2: 분열 (50% 이하)
```
임의의 턴: SPLIT (즉시 발동)
→ 전투 종료, 소환물 2마리와 재전투
```

**전략**:
- 50% 근처에서 체력 관리 중요
- 분열 후 소환물 2마리는 보스 체력 상속
- 한 마리씩 처리 추천 (Spike 우선 or Acid 우선)

## 위험 요소 분석

### 높음
- **SLAM**: 35-38의 강력한 단일 공격
- **분열 타이밍**: 준비 없이 50% 돌파 시 소환물 2마리 동시 상대

### 중간
- **STICKY**: Slimed 카드 누적
  - 카드 덱 오염
  - 드로우 방해
  - A19+에서 5장으로 증가

### 낮음
- **PREP_SLAM**: 데미지 없음, 경고 역할

## 특수 상황

### 분열 중단 (Interrupt)
```java
// 임의의 행동 중 체력 50% 이하 도달
현재 행동 중단 → SPLIT 즉시 실행
```

**예시**:
- SLAM 준비 중 50% 돌파 → SLAM 취소, SPLIT 실행
- 다음 턴 예고와 실제 행동이 다를 수 있음

### 소환물 전투
분열 후:
- **SpikeSlime_L**: 가시 공격 특화
- **AcidSlime_L**: 약화/취약 디버프

둘 다 보스의 남은 체력 상속 → 빠른 처리 필요

## 사운드 효과

| 행동 | 사운드 |
|-----|--------|
| STICKY | "MONSTER_SLIME_ATTACK" |
| PREP_SLAM | "VO_SLIMEBOSS_1A" or "VO_SLIMEBOSS_1B" |
| SLAM | 없음 (VFX만) |
| SPLIT | "SLIME_SPLIT" |
| 사망 | "VO_SLIMEBOSS_2A" |

## 데이터 구조

### 데미지 인덱스
```java
damage.get(0) = tackleDmg (9-10) - 미사용
damage.get(1) = slamDmg (35-38) - SLAM 공격
```

### 바이트 코드
```java
SLAM = 1
PREP_SLAM = 2
SPLIT = 3
STICKY = 4
```

### 상태 플래그
```java
firstTurn: boolean - 첫 턴 여부
nextMove: byte - 다음 행동 코드
```

## AI 로직 요약

```java
takeTurn() {
    switch (nextMove) {
        case STICKY(4):
            - Slimed 3-5장 추가
            - 다음: PREP_SLAM

        case PREP_SLAM(2):
            - 경고 대사 + 화면 진동
            - 다음: SLAM

        case SLAM(1):
            - 35-38 데미지
            - 다음: STICKY

        case SPLIT(3):
            - 자살 후 2마리 소환
            - 현재 체력 상속
    }
}

damage(info) {
    super.damage(info);

    // 체력 50% 이하 + 아직 분열 안함
    if (currentHealth <= maxHealth / 2 && nextMove != 3) {
        setMove(SPLIT); // 즉시 분열로 전환
        createIntent(); // 의도 업데이트
        INTERRUPTED 텍스트 표시
    }
}

getMove(num) {
    if (firstTurn) {
        firstTurn = false;
        setMove(STICKY); // 첫 턴 강제
    }
    // 이후는 takeTurn에서 순환 관리
}
```

## 전투 흐름 다이어그램

```
전투 시작
   ↓
[Turn 1] STICKY (3-5 Slimed)
   ↓
[Turn 2] PREP_SLAM (경고)
   ↓
[Turn 3] SLAM (35-38 dmg)
   ↓
[Turn 4] STICKY (3-5 Slimed)
   ↓
   ... 순환 ...
   ↓
체력 ≤ 50%? → [즉시] SPLIT
                  ↓
            SpikeSlime_L + AcidSlime_L
                  ↓
            소환물 처치 → 승리
```

## 승리 조건

1. **직접 처치**: 분열 전 체력을 0으로 만들기 (불가능)
2. **소환물 처치**: 분열 후 나온 2마리 슬라임 모두 처치

**중요**: 보스 자체는 50% 이하에서 자동 분열하므로 직접 처치 불가

## 업적 (Achievements)

```java
// 전투 시작 시
UnlockTracker.markBossAsSeen("SLIME");

// 승리 시
UnlockTracker.hardUnlockOverride("SLIME");
UnlockTracker.unlockAchievement("SLIME_BOSS");
```

## 디버그 정보

### 로그 확인
```java
logger.info("SPLIT"); // 분열 발동 시
```

### 애니메이션
- Skeleton: `images/monsters/theBottom/boss/slime/skeleton.atlas`
- JSON: `images/monsters/theBottom/boss/slime/skeleton.json`
- 기본: "idle" 무한 반복

## 모드 제작 시 주의사항

1. **분열 시스템**
   - `damage()` 메서드에서 체력 체크
   - `nextMove != 3` 조건 필수 (중복 방지)
   - 소환물 체력은 보스 현재 체력 상속

2. **상태 관리**
   - `firstTurn` 플래그로 첫 턴 고정
   - `setMove()` 호출로 다음 행동 결정
   - `createIntent()` 호출로 UI 업데이트

3. **소환물 처리**
   - `SpawnMonsterAction` 사용
   - 위치 좌표 정확히 지정
   - 현재 체력 전달

4. **사망 처리**
   - `die()` 메서드에서 `SpawnMonsterAction` 체크
   - 소환물 생성 중이면 조기 return
   - 승리 로직은 소환물까지 처치 후 실행
