# 2막 (The City) 몬스터 패턴 완전 가이드

## 개요

**지역명**: The City (도시)
**난이도**: 중급
**특징**: 다양한 메커니즘, 멀티 인카운터 증가, 복잡한 보스

---

## 📁 문서 구조

### 일반 몬스터 (Normal Monsters)

**단독 등장**:
1. [Byrd.md](./Byrd.md) - 버드 (낮은 HP, 다중 공격, Weak 디버프)
2. [Chosen.md](./Chosen.md) - 선택받은 자 (HP 50% 이하 Strength 버프)
3. [Centurion.md](./Centurion.md) - 백부장 (방어 중심, Strength 버프)
4. [Healer.md](./Healer.md) - 신비주의자 (아군 힐링, 최우선 처치 대상)
5. [Snecko.md](./Snecko.md) - 스네코 (카드 코스트 랜덤화)
6. [SnakePlant.md](./SnakePlant.md) - 뱀 식물 (Malleable, 다중 공격에 강함)
7. [SphericGuardian.md](./SphericGuardian.md) - 구형 수호자 (Artifact, Barricade, Thorns)
8. [Mugger.md](./Mugger.md) - 강도 (골드 훔치기, 도망)
9. [ShelledParasite.md](./ShelledParasite.md) - 껍질 기생충 (Plated Armor)
10. [TorchHead.md](./TorchHead.md) - 횃불 머리 (Burn 카드 추가)

**멀티 인카운터**:
- [Bandits_Complete.md](./Bandits_Complete.md) - 도적단 (Bear, Pointy, Leader)
  - Bear: 탱커 역할
  - Pointy: 딜러 역할
  - Leader: 버퍼 역할, 최우선 처치

---

### 엘리트 몬스터 (Elite Monsters)

[Act2_Elites_Complete.md](./Act2_Elites_Complete.md) - 2막 엘리트 완전 가이드
1. **Book of Stabbing** (찌르기의 서) - 타수 누적 (최대 6x5)
2. **Gremlin Leader** (그렘린 우두머리) - 하수인 소환 + 버프
3. **Taskmaster** (감독관) - 하수인당 데미지 증가

---

### 보스 몬스터 (Boss Monsters)

1. [BronzeAutomaton.md](./BronzeAutomaton.md) - 청동 자동인형
   - 2페이즈 시스템
   - 구체 소환 (1페이즈)
   - Hyper Beam (2페이즈, 50 데미지)

2. [Champ.md](./Champ.md) - 챔피언
   - 2페이즈 시스템
   - Metallicize (매 턴 Block)
   - 2페이즈: Strength +10-13

3. [TheCollector.md](./TheCollector.md) - 수집가
   - 하수인 소환 (최대 3마리)
   - Mega Debuff (Weak + Frail + Vulnerable)
   - Buff (본체 + 하수인 Strength)

---

## 🎯 핵심 메커니즘

### 카드 조작

**Snecko (Confusion)**:
- 카드 코스트 0-3 랜덤
- 전투 종료까지 지속
- Orange Pellets로 제거

**TorchHead (Burn)**:
- 언플레이어블 카드
- 버리기 시 2 데미지
- Fire Breathing으로 역이용

---

### 방어 메커니즘

**Plated Armor** (ShelledParasite):
- 타격당 Armor 1 감소
- 다중 공격에 취약
- 턴 종료 시 추가 1 감소

**Malleable** (SnakePlant):
- 타격당 Block 3-4 획득
- 다중 공격에 강함
- 단타 고데미지 유리

**Barricade** (SphericGuardian):
- Block 영구 보존
- 장기전 위험
- 속공 필수

**Metallicize** (Champ):
- 매 턴 Block 자동 획득
- 다중 공격으로 제거
- 피격 시 1 감소

---

### 버프 시스템

**Strength 버프**:
- Chosen: Strength +3-4 (HP 50% 이하, 1회)
- Centurion: Strength +2-3 (첫 턴 40%)
- Bandit Leader: Strength +2-3 (첫 턴, 아군 전체)
- Champ: Strength +10-13 (HP 50% 이하)
- The Collector: Strength +3-4 (아군 전체)

**Artifact**:
- SphericGuardian: 1-2 (디버프 무효화)

**Thorns**:
- SphericGuardian: 3-4 (HP 50% 이하)

---

### 하수인 시스템

**Gremlin Leader**:
- 최대 3마리
- 다양한 그렘린 소환
- Rally로 버프

**Taskmaster**:
- 시작 2-3마리
- 하수인당 +3 데미지
- 낮은 HP (54-60)

**The Collector**:
- 시작 TorchHead x2
- 3턴마다 소환
- 최대 3마리

**Bronze Automaton**:
- Bronze Orb 소환
- 최대 2-3개
- 2페이즈 전환 시 모두 제거

---

## 💡 전략 가이드

### AOE 필수 인카운터

**멀티 몬스터**:
- Bandits (3마리)
- Mugger (3마리)
- TorchHead (3마리)
- Gremlin Leader (1+3)
- Taskmaster (1+2-3)
- The Collector (1+2-3)
- Bronze Automaton (1+2-3)

**추천 AOE 카드**:
- Whirlwind
- Immolate
- Cleave
- Blade Dance

---

### 처치 순서 우선순위

**최우선**:
1. **Healer** (Mystic) - 아군 회복 차단
2. **Bandit Leader** - Strength 버프 차단
3. **Gremlin Leader** - 하수인 소환 차단

**중간**:
- Mugger - 골드 손실 방지
- TorchHead - Burn 누적 방지
- Bronze Orb - 지속 데미지 감소

**낮음**:
- Bear - 탱커, 마지막 처치
- Pointy - 낮은 HP, 빠른 처치 가능

---

### 디버프 관리

**Orange Pellets 필수**:
- Snecko (Confusion)
- Champ (Weak + Frail)
- The Collector (Mega Debuff)

**Artifact 소모**:
- SphericGuardian (Artifact 1-2)
- 디버프로 먼저 제거

**Disarm 활용**:
- Chosen (Strength +3-4)
- Champ (Strength +10-13)
- 버퍼들 (Strength 버프)

---

### 속공 vs 장기전

**속공 필수**:
- SphericGuardian (Barricade)
- Book of Stabbing (타수 누적)
- Champ (Metallicize)

**장기전 가능**:
- Snecko (Confusion 대비 필요)
- Bronze Automaton (구체 관리)
- The Collector (하수인 관리)

---

## 📊 난이도별 강화

### A7+ (HP 증가)

**전체**:
- 몬스터 HP 소폭 증가 (2-4)

---

### A17+ (메커니즘 강화)

**버프 강화**:
- Chosen: Strength 3 → 4
- Centurion: Strength 2 → 3, Block 15 → 20
- Bandit Leader: Strength 2 → 3
- Champ: Metallicize 5 → 6, 2페이즈 Strength 10 → 13
- The Collector: Buff Strength 3 → 4

**방어 강화**:
- SnakePlant: Malleable Block 3 → 4
- SphericGuardian: Artifact 1 → 2, Thorns 3 → 4
- Centurion: Defend Block 15 → 20
- Champ: Metallicize 5 → 6

**공격 강화**:
- Bronze Automaton: Orb 최대 2 → 3

**디버프 강화**:
- TorchHead: Burn 1 → 2
- Champ: Taunt 2턴 → 3턴
- The Collector: Mega Debuff 1턴 → 2턴

---

### A25+ (사용자 정의)

**예시 강화**:
- Book of Stabbing: 최대 5타 → 7타
- Gremlin Leader: 최대 하수인 3 → 5
- Bronze Automaton: Hyper Beam 50 → 60
- Champ: 2페이즈 Strength 13 → 18
- The Collector: 최대 하수인 3 → 5

---

## 🔧 수정 가이드

### 일반적인 수정 패턴

**HP 증가**:
```java
@SpirePatch(cls = "MonsterName", method = SpirePatch.CONSTRUCTOR)
public static class HPPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterName __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            __instance.maxHealth += 20;
            __instance.currentHealth += 20;
        }
    }
}
```

**데미지 증가**:
```java
@SpirePatch(cls = "MonsterName", method = "takeTurn")
public static class DamagePatch {
    @SpirePrefixPatch
    public static void Prefix(MonsterName __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // damage 필드 수정
        }
    }
}
```

**버프 강화**:
```java
@SpirePatch(cls = "MonsterName", method = "takeTurn")
public static class BuffPatch {
    @SpirePostfixPatch
    public static void Postfix(MonsterName __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 추가 Strength 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new StrengthPower(__instance, 2))
            );
        }
    }
}
```

---

## 📚 참고 자료

### 공통 파일

**파워**:
- `StrengthPower`: 공격력 증가
- `WeakPower`: 공격력 25% 감소
- `FrailPower`: Block 25% 감소
- `VulnerablePower`: 받는 데미지 50% 증가
- `PlatedArmorPower`: 타격당 Armor 1 감소
- `MalleablePower`: 타격당 Block 획득
- `MetallicizePower`: 매 턴 Block 획득
- `BarricadePower`: Block 영구 보존
- `ThornsPower`: 피격 시 반격
- `ArtifactPower`: 디버프 무효화
- `ConfusionPower`: 카드 코스트 랜덤화

**상태이상 카드**:
- `Burn`: 버리기 시 2 데미지
- `Dazed`: 언플레이어블
- `Wound`: 언플레이어블

**액션**:
- `DamageAction`: 데미지 공격
- `ApplyPowerAction`: 파워 부여
- `GainBlockAction`: Block 획득
- `HealAction`: HP 회복
- `MakeTempCardInDiscardAction`: 카드 추가
- `SpawnMonsterAction`: 몬스터 소환

---

## 🎮 플레이 팁

1. **AOE 카드 필수**: 멀티 인카운터가 많음
2. **Orange Pellets 유용**: 디버프 제거 기회 많음
3. **Fire Breathing**: Burn 카드 역이용
4. **Disarm**: Strength 버프 제거
5. **속공 전략**: SphericGuardian, Book of Stabbing
6. **하수인 우선**: Healer, Leader 먼저 처치
7. **다중 공격**: Plated Armor 제거에 유리
8. **단타 고데미지**: Malleable 상대에게 유리

---

## 📝 업데이트 로그

**2025-11-08**: 2막 전체 패턴 문서 작성 완료
- 일반 몬스터 10종 (개별 문서)
- 도적단 (통합 문서)
- 엘리트 3종 (통합 문서)
- 보스 3종 (개별 문서)

---

## 🔗 관련 문서

- [Act1_Normal_Monsters_Index.md](./Act1_Normal_Monsters_Index.md) - 1막 일반 몬스터
- [Gremlins_Complete.md](./Gremlins_Complete.md) - 그렘린 가이드
- [Slavers_Complete.md](./Slavers_Complete.md) - 노예상 가이드
