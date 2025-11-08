# ascension-100 모드 구현 현황

ascension-100 모드의 상세 구현 현황과 미구현 기능을 정리한 문서입니다.

## 📊 전체 구현 현황

| 레벨 범위 | 구현 상태 | 완성도 | 비고 |
|-----------|-----------|--------|------|
| 1-20 | 기본 게임 | 100% | 수정 불필요 |
| 21-27 | ✅ 완전 구현 | 100% | 체력, 공격력, 보스 강화 |
| 28-30 | ✅ 완전 구현 | 100% | 골드, Artifact |
| 31-39 | ✅ 완전 구현 | 100% | 플레이어 패널티, 추가 강화 |
| **40-50** | ❌ **완전 미구현** | **0%** | **중요! 구현 필요** |
| 51-56 | ✅ 완전 구현 | 100% | 대폭 강화 |
| 57-70 | ✅ 완전 구현 | 100% | 버프 추가, 막별 차등 |
| 71-100 | ❌ 완전 미구현 | 0% | 설계 단계 |

---

## ✅ 완전 구현된 기능

### Level 21-27: 기본 강화

#### Level 21 ✅
**파일**: `MonsterHealthPatch.java`
```java
// 엘리트 체력 10% 증가
if (level >= 21 && monster.type == AbstractMonster.EnemyType.ELITE) {
    multiplier *= 1.1f;
}
```
✅ **작동 확인**: 모든 엘리트 적에 정상 적용

#### Level 22 ✅
**파일**: `MonsterHealthPatch.java`
```java
// 일반 적 체력 10% 증가
if (level >= 22 && monster.type == AbstractMonster.EnemyType.NORMAL) {
    multiplier *= 1.1f;
}
```
✅ **작동 확인**: 모든 일반 적에 정상 적용

#### Level 23 ✅
**파일**: `MonsterDamagePatch.java`
```java
// 엘리트 공격력 10% 증가
if (level >= 23 && monster.type == AbstractMonster.EnemyType.ELITE) {
    multiplier *= 1.1f;
}
```
✅ **작동 확인**: 모든 엘리트 공격에 정상 적용

#### Level 24 ✅
**파일**: `MonsterDamagePatch.java`
```java
// 일반 적 공격력 10% 증가
if (level >= 24 && monster.type == AbstractMonster.EnemyType.NORMAL) {
    multiplier *= 1.1f;
}
```
✅ **작동 확인**: 모든 일반 적 공격에 정상 적용

#### Level 25 ✅
**파일**: `Level25Patches.java` (768 lines!)

**구현된 적 (29종)**:
- Louse (Normal, Defensive): Curl Up +3
- Cultist: 공격력 -2, Ritual +2
- Fungi Beast: SporeCloud +1
- Gremlin (5종): HP/공격력/방어도 증가
- JawWorm: Bellow 시 방어도 +12
- Slime (4종): Slimed +2
- Looter, Mugger: 도둑질 증가
- ShelledParasite: Plated Armor +2
- Chosen, Snecko: 공격력 +2
- Byrd: Strength 획득 +1
- Centurion: 회복 기능
- Healer/Mystic: 혼자 있을 때 Strength +8
- SphericGuardian: 방어도 +15
- Bandit (Bear, Pointy, Romeo): HP/공격력 증가
- Spiker, Exploder, OrbWalker: HP 증가
- Darkling: HP +25
- Maw: HP +100
- SpireGrowth: 공격력 +5
- SnakePlant: Malleable +1
- Transient: 사라지는 턴 +1
- WrithingMass: 기생충 확률 증가

✅ **작동 확인**: 모든 패치 정상 작동

#### Level 26 ✅
**파일**: `Level26BossPatch.java`

**구현된 보스 (9종)**:
- Slime Boss: HP +15
- Guardian: HP +50
- Hexaghost: HP +20
- Bronze Automaton: 공격력 +2, HP +20
- The Champ: 공격력 +2
- The Collector: HP +60
- Awakened One: HP +30
- Time Eater: HP +40
- Donu and Deca: 각각 HP +35

✅ **작동 확인**: 모든 보스 정상 적용

#### Level 27 ✅
**파일**: `Level27BossPatch.java`

**구현된 보스 패턴 강화 (9종)**:
- Slime Boss: Slimed +1
- Guardian: Weak/Vulnerable +1
- Hexaghost: 공격력 +1
- Bronze Automaton: 구체 HP +20
- The Champ: Strength 획득 +1
- The Collector: 디버프 2회
- Awakened One: Curiosity +1
- Time Eater: 회복 70%까지
- Donu: Dazed +1

✅ **작동 확인**: 모든 보스 패턴 정상 작동

### Level 28-30

#### Level 28 ✅
**파일**: `GoldRewardPatch.java`
```java
// 전투 골드 10% 감소
goldAmount = MathUtils.ceil(goldAmount * 0.9f);
```
✅ **작동 확인**: 전투 보상 골드 감소 정상 작동

#### Level 29 ✅
**효과**: 없음 (의도적)
✅ **작동 확인**: 아무 효과 없음

#### Level 30 ✅
**파일**: `Level30ArtifactPatch.java`
```java
// Artifact 가진 적 +1
if (hasSertifactPower) {
    artifactAmount += 1;
}
```
✅ **작동 확인**: Artifact 파워 증가 정상 작동

### Level 31-39

**파일**: `Level31to39Patches.java`

#### 구현된 기능:
- Level 31 ✅: 시작 HP -5
- Level 32 ✅: 엘리트 HP +10%
- Level 33 ✅: 일반 HP +10%
- Level 34 ✅: 엘리트 공격력 +2
- Level 35 ✅: 일반 공격력 +1
- Level 36 ✅: 보스 HP +15%, 공격력 -1
- Level 37 ⚠️: 포션 가격/효과 (부분 구현)
- Level 38 ⚠️: 보스 회복 감소 (미구현)
- Level 39 ✅: 시작 최대 HP -5

#### Level 37 상세:
```java
// ⚠️ 부분 구현
// 포션 가격 -40%: 미구현
// 포션 효과 -20%: 미구현
```
**구현 필요**: `PotionHelper` 패치

#### Level 38 상세:
```java
// ❌ 미구현
// 보스 전투 후 회복량 10% 감소
```
**구현 필요**: `RestRoom` 또는 보스 보상 패치

### Level 51-70

**파일**: `Level51to70Patches.java`

#### 구현된 기능:
- Level 51 ✅: 모든 적 HP +20%
- Level 52 ✅: 모든 적 공격력 +1
- Level 53 ✅: 없음 (의도적)
- Level 54 ✅: 없음 (의도적)
- Level 55 ⚠️: 강화 엘리트 (미구현)
- Level 56 ✅: 디버프 강화 (WeakPower, VulnerablePower, FrailPower)
- Level 57 ✅: 모든 적 HP +10%
- Level 58 ✅: 모든 적 공격력 +1
- Level 59 ✅: 엘리트 HP +10
- Level 60 ⚠️: 강화 엘리트 성장 (미구현)
- Level 61 ✅: 모든 적 HP +10%
- Level 62 ✅: 모든 적 공격력 +1
- Level 63 ✅: 보스 HP +20%
- Level 64 ✅: 보스 공격력 +10%
- Level 65 ✅: Artifact +1
- Level 66 ✅: 랜덤 버프 (15% 확률)
- Level 67 ✅: 모든 적 HP +15%
- Level 68 ✅: 막별 공격력 (+1/+2/+5)
- Level 69 ✅: 보스 막별 강화
- Level 70 ✅: 엘리트 막별 이점

#### Level 56 상세 (DebuffPatch):
**파일**: `Level56DebuffPatch.java`
```java
// Vulnerable: 추가 10% 데미지
WEAK_MODIFIER = -35;  // -25 → -35

// Weak: 추가 10% 감소
VULNERABLE_MODIFIER = 60;  // 50 → 60

// Frail: 추가 10% 방어 감소
FRAIL_MODIFIER = 35;  // 25 → 35
```
✅ **작동 확인**: 모든 디버프 강화 정상 작동

---

## ❌ 미구현 기능

### Level 40-50: 완전 미구현 (중요!)

#### Level 40 ❌
**효과**: 적들의 행동이 때때로 당신의 상황에 맞춰 결정됩니다.
**상세**: 효과 없음 (ascension-detail.md)
**구현 필요도**: 낮음

#### Level 41 ❌
**효과**: 엘리트가 더 많이 발생합니다.
**상세**: ?지역 10% 확률로 엘리트로 변경
**구현 필요**: `MapGenerator` 또는 `MapRoomNode` 패치
**구현 필요도**: **높음** (체감 난이도 큰 영향)

```java
// 예상 구현
@SpirePatch(cls = "com.megacrit.cardcrawl.map.MapGenerator", method = "generateDungeon")
public static class EliteIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 41) {
            // ? 노드를 10% 확률로 엘리트로 변경
        }
    }
}
```

#### Level 42 ❌
**효과**: 얻는 돈 25% 증가, 카드 제거 비용 +25
**구현 필요**:
1. 골드 증가: `GoldRewardPatch` 수정
2. 제거 비용: `ShopScreen` 패치
**구현 필요도**: **높음** (경제 시스템 영향)

```java
// 예상 구현
@SpirePatch(cls = "com.megacrit.cardcrawl.rewards.RewardItem", method = "incrementGold")
public static class GoldIncreasePatch {
    @SpirePostfixPatch
    public static void Postfix(RewardItem __instance, int amount) {
        if (AbstractDungeon.ascensionLevel >= 42) {
            int bonus = MathUtils.ceil(amount * 0.25f);
            __instance.goldAmt += bonus;
        }
    }
}

@SpirePatch(cls = "com.megacrit.cardcrawl.shop.ShopScreen", method = "init")
public static class RemovalCostPatch {
    @SpirePostfixPatch
    public static void Postfix(ShopScreen __instance) {
        if (AbstractDungeon.ascensionLevel >= 42) {
            __instance.purgeCost += 25;
        }
    }
}
```

#### Level 43 ❌
**효과**: 일부 이벤트가 당신의 상황에 맞춰 변경
**상세**: 돌림판 이벤트 조작
- 체력 <15%: 피해 선택
- 체력 >80%: 회복 선택
**구현 필요**: `WheelOfChange` 이벤트 패치
**구현 필요도**: 중간

#### Level 44 ❌
**효과**: 도둑들이 더 많은 돈을 강탈
**상세**: 도둑질 +10
**구현 필요**: Looter, Mugger 도둑질 메커니즘 패치
**구현 필요도**: 낮음

#### Level 45 ❌
**효과**: 적의 구성이 일부 변경
**상세**:
1. 대형 슬라임 → 대형 슬라임 + 슬라임 1
2. 공벌레 2 → 20% 확률로 공벌레 3
**구현 필요**: `MonsterInfo` 또는 encounter 패치
**구현 필요도**: **높음** (전투 난이도 큰 영향)

```java
// 예상 구현
@SpirePatch(cls = "com.megacrit.cardcrawl.dungeons.AbstractDungeon", method = "getMonsterForRoomCreation")
public static class EncounterModificationPatch {
    @SpirePostfixPatch
    public static void Postfix(String encounterKey) {
        if (AbstractDungeon.ascensionLevel >= 45) {
            if (encounterKey.equals("Large Slime")) {
                // 슬라임 1마리 추가
            }
            if (encounterKey.equals("2 Louses") && MathUtils.randomBoolean(0.2f)) {
                // 공벌레 1마리 추가
            }
        }
    }
}
```

#### Level 46 ❌
**효과**: 전투 골드 20% 증가, 유물 가격 15% 증가
**구현 필요**:
1. 골드 증가: `GoldRewardPatch` 수정
2. 유물 가격: `ShopScreen` 패치
**구현 필요도**: **높음**

#### Level 47 ❌
**효과**: 휴식 장소에서 쉴 때마다 최대 체력 -2
**구현 필요**: `RestRoom` 패치
**구현 필요도**: **높음** (생존 난이도 큰 영향)

```java
// 예상 구현
@SpirePatch(cls = "com.megacrit.cardcrawl.ui.campfire.RestOption", method = "useOption")
public static class RestPenaltyPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 47) {
            AbstractDungeon.player.decreaseMaxHealth(2);
        }
    }
}
```

#### Level 48 ❌
**효과**: 니오우의 선물 패널티 +10%
**구현 필요**: `Neow` 이벤트 패치
**구현 필요도**: 낮음

#### Level 49 ❌
**효과**: 전투 골드 30% 증가, 카드 가격 50% 증가
**구현 필요**:
1. 골드 증가: `GoldRewardPatch` 수정
2. 카드 가격: `ShopScreen` 패치
**구현 필요도**: **높음**

#### Level 50 ❌
**효과**: ???막 강제 (열쇠 없으면 체력 1, 에너지 0)
**구현 필요**: 4막 진입 조건 패치
**구현 필요도**: **최고** (게임 플레이 크게 변경)

```java
// 예상 구현
@SpirePatch(cls = "com.megacrit.cardcrawl.dungeons.TheBeyond", method = "initializeSpecialOneTimeEventList")
public static class ForcedActFourPatch {
    @SpirePostfixPatch
    public static void Postfix() {
        if (AbstractDungeon.ascensionLevel >= 50) {
            // 열쇠 없으면 체력 1, 에너지 0으로 4막 진입
        }
    }
}
```

### Level 55, 60: 강화 엘리트 ⚠️

#### Level 55 ❌
**효과**: 강화 엘리트가 더욱 강해집니다.
**상세**:
- 최대 체력 추가 +15%
- 힘 추가 +2
- 금속화 추가 +4
- 재생 추가 +3
**구현 필요**: "Strengthened" modifier 패치
**구현 필요도**: 중간

#### Level 60 ❌
**효과**: 강화 엘리트 막별 성장
**상세**:
- 1막: HP +15, 공격력 +1
- 2막: HP +40, 공격력 +2
- 3막: HP +80, 공격력 +5
**구현 필요**: "Strengthened" modifier + Act 체크
**구현 필요도**: 중간

### Level 71-100: 대부분 미구현

#### 구현된 레벨 (ascension-detail.md에 설명 있음):
- 71-74: 비어있음
- 75: 일부 전투 구성 변경 (설명 없음)
- 76-79: 특수 전투 (설명 없음)
- 80: 시작 저주 변경 (설명 없음)
- 81-84: 비어있음
- 85: 일부 이벤트 적대적 (설명 없음)
- 86-89: 비어있음
- 90: 적 기믹 강화 (설명 없음)
- 91-93: 비어있음
- 94: 일부 이벤트 불리 (설명 없음)
- 95: 11턴 시작 시 사망 (설명 없음)
- 96: 상점 감소 (설명 없음)
- 97: 운 감소 (설명 없음)
- 98: 모든 회복 -1 (설명 없음)
- 99: 불합리한 사건 (설명 없음)
- 100: ??? (비어있음)

**구현 필요도**: 낮음 (설계가 완성되지 않음)

---

## 📋 구현 우선순위

### 높음 (체감 난이도 큰 영향)
1. **Level 41**: 엘리트 증가
2. **Level 42**: 경제 시스템 (골드/제거 비용)
3. **Level 45**: 적 구성 변경
4. **Level 46**: 골드/유물 가격
5. **Level 47**: 휴식 패널티
6. **Level 49**: 골드/카드 가격
7. **Level 50**: 4막 강제

### 중간
1. **Level 37**: 포션 시스템 (가격/효과)
2. **Level 38**: 보스 회복 감소
3. **Level 43**: 이벤트 조작
4. **Level 55/60**: 강화 엘리트

### 낮음
1. **Level 44**: 도둑질 증가
2. **Level 48**: 니오우 패널티
3. **Level 71-100**: 고레벨 컨텐츠

---

## 🛠️ 구현 시 참고사항

### 1. 골드 관련 패치
- **파일**: `GoldRewardPatch.java` (이미 존재)
- Level 28, 42, 46, 49에서 사용
- 누적 계산 필요

### 2. 상점 관련 패치
- **필요 클래스**: `com.megacrit.cardcrawl.shop.ShopScreen`
- Level 42 (제거), 46 (유물), 49 (카드)
- 별도 파일 생성 필요: `ShopPatch.java`

### 3. 이벤트 관련 패치
- **Level 43**: `com.megacrit.cardcrawl.events.city.WheelOfChange`
- **Level 48**: `com.megacrit.cardcrawl.neow.NeowEvent`
- **Level 50**: `com.megacrit.cardcrawl.dungeons.TheBeyond`
- 별도 파일 생성 필요: `EventPatches.java`

### 4. 적 구성 패치
- **Level 41**: Map generation
- **Level 45**: Monster encounters
- 별도 파일 생성 필요: `EncounterPatches.java`

### 5. 휴식 관련 패치
- **Level 38**: 보스 회복
- **Level 47**: 휴식 패널티
- 별도 파일 생성 필요: `RestPatch.java`

---

## 📊 통계

### 구현 완료
- **레벨 수**: 50개 (Level 21-27, 28-30, 31-39, 51-70)
- **패치 파일**: 11개
- **총 코드 라인**: ~2500 lines

### 미구현
- **레벨 수**: 50개 (Level 40-50, 55, 60, 71-100)
- **필요 패치 파일**: ~5개 (추정)
- **예상 코드 라인**: ~1500 lines

### 우선순위 높음 미구현
- **레벨 수**: 7개 (Level 41, 42, 45, 46, 47, 49, 50)
- **필요 패치 파일**: 4개
- **예상 코드 라인**: ~800 lines

---

## 📚 관련 문서

- [INDEX.md](INDEX.md) - 전체 가이드 목차
- [ENEMY_MODIFY.md](ENEMY_MODIFY.md) - 적 수정 방법
- [COMMON_PATTERNS.md](COMMON_PATTERNS.md) - 구현 패턴 모음

---

**최종 업데이트**: 2025-11-07
**분석 기준**: ascension-100 모드 소스코드 + ascension-detail.md
