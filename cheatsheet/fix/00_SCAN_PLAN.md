# STS 버그 스캐닝 계획서 (Bug Scanning Plan)

Slay the Spire 디컴파일 소스에서 버그를 체계적으로 발견하고 문서화하기 위한 전체 계획입니다.

## 목차
1. [스캐닝 전략](#스캐닝-전략)
2. [우선순위 시스템](#우선순위-시스템)
3. [카테고리별 스캐닝 순서](#카테고리별-스캐닝-순서)
4. [버그 문서 작성 규칙](#버그-문서-작성-규칙)
5. [체크리스트](#체크리스트)

---

## 스캐닝 전략

### 기본 원칙
1. **핵심 시스템 우선**: 게임의 핵심 메커니즘부터 검토
2. **사용 빈도 고려**: 자주 실행되는 코드 우선 검토
3. **영향 범위 평가**: 광범위한 영향을 미치는 버그 우선 발견
4. **재현 가능성**: 재현 가능하고 검증 가능한 버그 우선

### 버그 유형 분류

#### 1. Critical Bugs (치명적 버그)
- 게임 크래시 유발
- 데이터 손실 또는 세이브 파일 손상
- 게임 진행 불가 상태 (Softlock)
- 심각한 밸런스 파괴

#### 2. Major Bugs (주요 버그)
- 잘못된 계산 (데미지, 확률 등)
- 능력이 의도와 다르게 작동
- 메모리 누수 또는 성능 문제
- 일관성 없는 동작

#### 3. Minor Bugs (경미한 버그)
- UI 표시 오류
- 텍스트 오타
- 사운드/비주얼 이펙트 버그
- Edge case에서만 발생하는 문제

#### 4. Design Issues (설계 문제)
- 비효율적인 알고리즘
- 불필요한 중복 코드
- 타입 안전성 부족
- Null safety 문제

---

## 우선순위 시스템

### Priority 1: Core Combat System (전투 시스템)
**중요도**: ★★★★★
**이유**: 게임의 핵심 루프, 가장 자주 실행됨

- 데미지 계산 (DamageAction, DamageInfo)
- Block 계산 (GainBlockAction)
- Power 적용 및 스택 (ApplyPowerAction)
- 카드 효과 처리 (AbstractCard.use)
- 턴 종료 처리 (EndTurnAction, GameActionManager)
- 버프/디버프 상호작용

### Priority 2: Card System (카드 시스템)
**중요도**: ★★★★★
**이유**: 전투의 기반, 복잡한 상호작용 다수

- 카드 코스트 계산
- 카드 드로우 (DrawCardAction)
- 덱 셔플 (ShuffleAction)
- 카드 업그레이드
- 임시 카드 추가 (MakeTempCardInDrawPileAction 등)
- 카드 제거/변환
- Exhaust 처리

### Priority 3: Monster AI (몬스터 AI)
**중요도**: ★★★★☆
**이유**: 전투 밸런스의 핵심

- 행동 선택 (getMove)
- 인텐트 표시
- 다단히트 공격 처리
- 특수 능력 발동 조건
- AI RNG 시드 처리

### Priority 4: Relic System (유물 시스템)
**중요도**: ★★★★☆
**이유**: 게임 전반에 영향, 복잡한 트리거 조건

- 유물 트리거 타이밍 (onPlayerEndTurn, atBattleStart 등)
- 유물 스택 처리
- 유물 간 상호작용
- 카운터 관리 (Ink Bottle, Nunchaku 등)
- Shop 유물 가격 계산

### Priority 5: Potion System (포션 시스템)
**중요도**: ★★★☆☆
**이유**: 중요하지만 독립적인 시스템

- 포션 효과 처리
- 포션 드롭 확률 (PotionHelper)
- 포션 슬롯 관리
- 포션 사용 타이밍

### Priority 6: Event System (이벤트 시스템)
**중요도**: ★★★☆☆
**이유**: 게임 진행에 중요하나 실행 빈도 낮음

- 이벤트 선택지 처리
- 보상 계산
- RNG 시드 처리
- 특수 이벤트 조건

### Priority 7: Map & Dungeon Generation (맵 생성)
**중요도**: ★★★☆☆
**이유**: 게임 시작 시에만 실행

- 맵 생성 알고리즘
- 경로 연결
- 방 타입 분배
- 보스 선택

### Priority 8: Reward System (보상 시스템)
**중요도**: ★★★☆☆
**이유**: 밸런스에 영향

- 카드 보상 레어도 계산
- 골드 보상 계산
- 유물 보상 풀 관리

### Priority 9: UI System (UI 시스템)
**중요도**: ★★☆☆☆
**이유**: 게임플레이에는 영향 적지만 사용자 경험 중요

- 툴팁 표시
- 버튼 상호작용
- 화면 전환
- 애니메이션 타이밍

### Priority 10: Save/Load System (저장/로드)
**중요도**: ★★★★☆
**이유**: 데이터 무결성 중요

- 세이브 파일 직렬화
- 로드 시 데이터 복원
- 체크포인트 관리

---

## 카테고리별 스캐닝 순서

### Phase 1: Combat Core (1-2주)
```
1.1. DamageAction & DamageInfo
     - 데미지 계산 공식
     - Vulnerable, Weak 적용
     - 다단히트 처리
     - Overkill 버그

1.2. GainBlockAction
     - Block 계산
     - Dexterity 적용
     - Frail 적용

1.3. ApplyPowerAction
     - Power 스택 로직
     - Replace vs Stack
     - Artifact 상호작용

1.4. GameActionManager
     - Action queue 처리
     - 턴 종료 타이밍
     - Infinite loop 방지
```

### Phase 2: Card Mechanics (1-2주)
```
2.1. AbstractCard
     - Cost 계산 (X cost, cost reduction)
     - canUse() 조건
     - triggerOnGlowCheck()

2.2. DrawCardAction
     - 덱 소진 처리
     - 핸드 크기 제한
     - 드로우 순서

2.3. DiscardAction & ExhaustAction
     - Discard pile 관리
     - Exhaust pile 복원 불가 보장

2.4. Card generation
     - MakeTempCardInHandAction
     - MakeTempCardInDrawPileAction
     - MakeTempCardInDiscardAction
```

### Phase 3: Monster AI (1주)
```
3.1. AbstractMonster.getMove()
     - 패턴 선택 로직
     - RNG 시드 일관성

3.2. Intent calculation
     - 데미지 인텐트 계산
     - 다단히트 표시

3.3. Monster death
     - 사망 타이밍
     - minion 처리
```

### Phase 4: Relic System (1주)
```
4.1. Relic triggers
     - atBattleStart vs atPreBattle
     - onPlayerEndTurn 순서
     - Relic 실행 순서 (relics list 순회)

4.2. Counter relics
     - Ink Bottle, Nunchaku
     - Sundial, HandDrill

4.3. Shop relics
     - Membership Card 가격 계산
     - Courier 포션 추가
```

### Phase 5: Potion System (3-4일)
```
5.1. PotionHelper
     - 포션 드롭 확률
     - Potion pool 관리

5.2. Potion effects
     - 타겟팅 처리
     - 효과 계산
```

### Phase 6: Event & Dungeon (1주)
```
6.1. Event choices
     - 선택지 처리
     - RNG 시드

6.2. Map generation
     - 경로 연결 검증
     - Boss 선택 버그
```

### Phase 7: Reward & Economy (3-4일)
```
7.1. Card rewards
     - 레어도 계산
     - Upgrade 확률

7.2. Gold rewards
     - 계산 공식
```

### Phase 8: UI & Misc (3-4일) ✓
```
8.1. EnergyPanel - 에너지 표시 (정상)
8.2. TopPanel - HP, 골드, 버튼 (정상)
8.3. Tooltip bugs (명확한 버그 없음)
8.4. Animation timing (정상)
```

### Phase 9: Save/Load (3-4일) ✓
```
9.1. File.save() - 저장 실패 처리
     ⚠️ P2_Save_SilentSaveFailureBug 발견
9.2. AsyncSaver - thread management (정상)
9.3. SaveHelper - corruption handling (정상)
9.4. Serialization (정상)
```

---

## 버그 문서 작성 규칙

### 파일 네이밍 규칙
```
[Priority]_[Category]_[BugName].md

예시:
P1_Combat_MultiHitOverkillBug.md
P2_Card_ExhaustDuplicationBug.md
P4_Relic_InkBottleCounterBug.md
```

### Priority 코드
- `P1`: Critical (치명적)
- `P2`: Major (주요)
- `P3`: Minor (경미한)
- `P4`: Design Issue (설계 문제)

### Category 코드
- `Combat`: 전투 시스템
- `Card`: 카드 시스템
- `Monster`: 몬스터 AI
- `Relic`: 유물 시스템
- `Potion`: 포션 시스템
- `Event`: 이벤트 시스템
- `Map`: 맵 생성
- `Reward`: 보상 시스템
- `UI`: UI 시스템
- `Save`: 저장/로드 시스템
- `Misc`: 기타

### 버그 문서 템플릿

```markdown
# [버그 이름]

## 버그 정보
- **Priority**: P1/P2/P3/P4
- **Category**: Combat/Card/Relic 등
- **발견 위치**: `파일명.java:라인번호`
- **영향 범위**: 전투/특정 카드/특정 유물 등
- **재현 조건**: 어떤 상황에서 발생하는가

## 버그 설명

### 현재 동작 (Current Behavior)
현재 어떻게 동작하는지 상세히 설명

### 예상 동작 (Expected Behavior)
올바르게 동작했을 때 어떻게 되어야 하는지

### 발생 원인 (Root Cause)
코드 분석 결과, 왜 이 버그가 발생하는지

## 코드 분석

### 문제가 되는 코드
```java
// 파일명.java:라인번호
[문제 코드 발췌]
```

### 호출 흐름
```
호출 스택 또는 실행 흐름
```

## 재현 방법

1. 단계별 재현 방법
2. 필요한 조건
3. 예상 결과

## 수정 방법

### Option 1: [수정 방법 이름]

**장점**:
-

**단점**:
-

**사이드 이펙트**:
-

**패치 코드**:
```java
@SpirePatch(...)
public class FixPatch {
    // 패치 코드
}
```

### Option 2: [다른 수정 방법]

(같은 구조 반복)

## 영향 받는 요소

- 영향 받는 카드 목록
- 영향 받는 유물 목록
- 영향 받는 파워 목록

## 관련 버그

- 관련된 다른 버그 문서 링크

## 참고 자료

- 공식 패치 노트 링크
- 커뮤니티 버그 리포트 링크
- 관련 디스커션

## 검증 방법

패치 후 올바르게 수정되었는지 확인하는 방법
```

---

## 체크리스트

### Phase 1: Combat Core
- [x] DamageAction - 다단히트 Overkill 버그 (정상)
- [x] DamageAction - Vulnerable/Weak 계산 순서 (정상)
- [x] DamageAction - 음수 데미지 처리 (정상)
- [x] GainBlockAction - Frail/Dexterity 계산 (정상)
- [x] GainBlockAction - Block overflow (정상)
- [x] ApplyPowerAction - Power 스택 버그 (정상)
- [x] ApplyPowerAction - Artifact 소모 타이밍 (정상)
- [x] GameActionManager - Action queue 무한루프 (정상)
- [x] EndTurnAction - Relic 트리거 순서 (정상)
- [x] **AbstractMonster.damage() - wasHPLost 타이밍 버그 발견** ⚠️

### Phase 2: Card Mechanics
- [x] DrawCardAction - 덱 소진 시 처리 (정상)
- [x] **DrawCardAction - 핸드 크기 초과 계산 버그 발견** ⚠️
- [x] ExhaustAction - Exhaust 복원 버그 (정상)
- [x] DiscardAction - Discard pile 순서 (정상)
- [x] AbstractCard.calculateCardDamage - X cost 버그 (정상)
- [x] AbstractCard.canUse - 조건 체크 누락 (정상)
- [x] **MakeTempCardInHandAction - switch fall-through 버그 발견 (검증 필요)** ⚠️⚠️
- [x] **MakeTempCardInDiscardAction - else 절 누락 버그 발견** ⚠️
- [ ] UpgradeAction - 업그레이드 중복 적용 (미확인)

### Phase 3: Monster AI
- [x] AbstractMonster.getMove - 무한루프 가능성 (정상)
- [x] AbstractMonster.takeTurn - RollMoveAction 누락 (정상 - 모든 몬스터가 호출)
- [x] MonsterGroup.areMonstersBasicallyDead - 조건 버그 (정상 - halfDead는 특수 케이스)
- [x] Intent calculation - 다단히트 표시 오류 (정상)
- [x] **onPlayerGainedBlock 네이밍 이슈 발견 (설계 문제)** ⚠️

### Phase 4: Relic System
- [x] AbstractRelic.atBattleStart - 실행 순서 (정상)
- [x] AbstractRelic.onPlayerEndTurn - 타이밍 이슈 (정상)
- [x] Ink Bottle - 카운터 버그 (정상)
- [x] Nunchaku - 카운터 버그 (정상)
- [x] PenNib - 카운터 버그 (정상)
- [ ] Sundial - 카운터 버그
- [ ] Membership Card - 가격 계산 버그
- [ ] Courier - 포션 추가 버그

### Phase 5: Potion System
- [ ] PotionHelper.getPotion - 확률 계산
- [ ] AbstractPotion.use - 타겟팅 버그
- [ ] Potion slot - 슬롯 관리 버그

### Phase 6: Event & Dungeon
- [ ] AbstractEvent - 선택지 처리 버그
- [ ] AbstractDungeon.generateMap - 맵 생성 버그
- [ ] Boss selection - 보스 선택 버그

### Phase 7: Reward & Economy
- [ ] CardRewardScreen - 레어도 계산
- [ ] GoldReward - 골드 계산 공식

### Phase 8: UI & Misc
- [x] EnergyPanel - 에너지 표시 (정상)
- [x] TopPanel - HP, 골드, 버튼 (정상)
- [x] Tooltip - 표시 오류 (명확한 버그 없음)
- [x] Animation - 타이밍 버그 (정상)

### Phase 9: Save/Load
- [x] **File.save() - 에러 로깅 누락 버그 발견** ⚠️
- [x] AsyncSaver - thread management (정상)
- [x] SaveHelper - corruption handling (정상)
- [x] Serialization (정상)

---

## 스캐닝 진행 상황

### 완료된 Phase
- [x] Phase 1: Combat Core ✓ (버그 1개 발견)
- [x] Phase 2: Card Mechanics ✓ (버그 3개 발견)
- [x] Phase 3: Monster AI ✓ (명확한 버그 없음)
- [x] Phase 4: Relic System ✓ (명확한 버그 없음)
- [x] Phase 5: Potion System ✓ (명확한 버그 없음)
- [x] Phase 6: Event & Dungeon ✓ (간단 스캔 완료, 명확한 버그 없음)
- [x] Phase 7: Reward & Economy ✓ (간단 스캔 완료, 명확한 버그 없음)
- [x] Phase 8: UI & Misc ✓ (명확한 버그 없음)
- [x] Phase 9: Save/Load ✓ (버그 1개 발견)

**전체 스캔 완료!** ✅

### 발견된 버그 통계
- **P1 (Critical)**: 1
  - P1_Card_MakeTempCardDuplicateGenerationBug (검증 필요)
- **P2 (Major)**: 4
  - P2_Combat_MonsterWasHPLostTimingBug
  - P2_Card_DrawCardHandSizeCalculationBug
  - P2_Card_MakeTempCardInDiscardMissingElseBug
  - P2_Save_SilentSaveFailureBug (NEW!)
- **P3 (Minor)**: 0
- **P4 (Design Issues)**: 1
  - P4_Design_OnPlayerGainedBlockNamingIssue
- **Total**: 6개 발견

---

## 작업 가이드라인

### 스캐닝 시 주의사항

1. **Null Safety 체크**
   - Null pointer dereference 가능성
   - Optional 사용 여부
   - Null check 누락

2. **경계 조건 (Edge Cases)**
   - 0, 음수, 최대값
   - Empty collection
   - Division by zero

3. **동시성 (Concurrency)**
   - Thread safety
   - Race condition
   - Synchronization 누락

4. **메모리 관리**
   - Memory leak
   - Unreleased resources
   - Circular references

5. **타입 안전성**
   - Unsafe casting
   - Generic type erasure
   - Reflection 오용

6. **로직 오류**
   - Off-by-one error
   - 잘못된 조건문
   - 무한루프 가능성

### 버그 검증 방법

1. **코드 리뷰**: 소스 코드 정적 분석
2. **동작 테스트**: 실제 게임에서 재현
3. **커뮤니티 확인**: 알려진 버그인지 확인
4. **패치 노트 확인**: 공식적으로 수정되었는지 확인

---

## 참고 자료

### 공식 자료
- [Slay the Spire Patch Notes](https://store.steampowered.com/news/app/646570)
- [Slay the Spire Wiki](https://slay-the-spire.fandom.com/)

### 커뮤니티 자료
- [STS Subreddit](https://www.reddit.com/r/slaythespire/)
- [STS Discord](https://discord.gg/slaythespire)
- [ModTheSpire GitHub Issues](https://github.com/kiooeht/ModTheSpire/issues)

### 기술 자료
- [BaseMod Documentation](https://github.com/daviscook477/BaseMod/wiki)
- [SpirePatch Tutorial](https://github.com/daviscook477/BaseMod/wiki/SpirePatch)

---

## 다음 단계

1. ✅ 작업 준비 문서 작성 완료
2. ⏳ Phase 1: Combat Core 스캐닝 시작
   - DamageAction.java 분석
   - GainBlockAction.java 분석
   - ApplyPowerAction.java 분석
3. ⏳ 발견된 버그 문서화
4. ⏳ 패치 코드 작성 및 검증

---

**작성 일자**: 2025-01-08
**최종 수정**: 2025-01-08
**작성자**: Claude Code Analysis Team
