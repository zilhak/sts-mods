# Act 1 Normal Monsters - Complete Index

1막 일반 몬스터 패턴 문서 전체 목록 및 빠른 참조

---

## 📁 문서 목록

### 개별 몬스터 (4종)
1. **LouseNormal.md** - 공벌레 일반 (빨간색)
2. **LouseDefensive.md** - 공벌레 방어형 (녹색)
3. **FungiBeast.md** - 동물하초
4. **Looter.md** - 도적

### 통합 문서 (3종)
5. **Slimes_Complete.md** - 슬라임 6종 (Acid/Spike × S/M/L)
6. **Slavers_Complete.md** - 노예 상인 2종 (Red/Blue)
7. **Gremlins_Complete.md** - 그렘린 5종

---

## 🎯 빠른 참조

### HP 범위 (A0-6 기준)
| 몬스터 | HP | 위협도 |
|--------|----|----|
| LouseNormal | 10-15 | 낮음 |
| LouseDefensive | 11-17 | 낮음 |
| FungiBeast | 22-28 | 중간 |
| Looter | 44-48 | 높음 |
| AcidSlime_S | 8-12 | 낮음 |
| AcidSlime_M | 28-32 | 중간 |
| AcidSlime_L | 65-69 | 높음 |
| SpikeSlime_S | 10-14 | 낮음 |
| SpikeSlime_M | 28-32 | 중간 |
| SpikeSlime_L | 64-70 | 높음 |
| SlaverRed | 46-50 | 높음 |
| SlaverBlue | 46-50 | 중간 |
| GremlinTsundere | 12-15 | 중간 |
| GremlinWizard | 21-25 | 높음 |
| GremlinWarrior | 20-24 | 중간 |
| GremlinFat | 13-17 | 낮음 |
| GremlinThief | 10-14 | 중간 |

### 주요 메커니즘 분류

#### 버프형
- **LouseNormal**: 자신에게 힘 +3/4
- **FungiBeast**: 자신에게 힘 +3/4/5
- **GremlinTsundere**: 아군에게 블록 7-11

#### 디버프형
- **LouseDefensive**: 약화 2턴
- **AcidSlime_S/M/L**: 약화 1-2턴
- **SpikeSlime_M/L**: 나약 1-3턴
- **SlaverRed**: 취약 1-2턴, Entangle 1회
- **SlaverBlue**: 약화 1-2턴
- **GremlinFat**: 약화 + 나약(A17+) 1턴

#### 특수 메커니즘
- **FungiBeast**: SporeCloudPower (죽을 때 취약 부여)
- **Looter**: ThieveryPower (골드 도둑)
- **AcidSlime_M/L, SpikeSlime_M/L**: 점액 카드 부여
- **AcidSlime_L, SpikeSlime_L**: HP 50% 분열
- **GremlinWizard**: 차징 3회 후 강력한 마법
- **GremlinWarrior**: AngryPower (피격 시 힘 증가)
- **모든 그렘린**: 동료 사망 시 도주

---

## 📊 난이도별 주요 변경점

### Ascension 2 (A2+)
**데미지 증가**:
- LouseNormal/Defensive: BITE 6-8 (기존 5-7)
- FungiBeast: GROW +4 (기존 +3)
- Looter: MUG 11, LUNGE 14 (기존 10, 12)
- 모든 Slime_S: +1
- 모든 Slime_M/L: +2
- SlaverRed: STAB 14, SCRAPE 9 (기존 13, 8)
- SlaverBlue: STAB 13, RAKE 8 (기존 12, 7)
- 모든 Gremlin: +1

### Ascension 7 (A7+)
**HP 증가**:
- 모든 몬스터 HP 범위 상한 +1~2

**CurlUpPower 증가** (Louse 계열):
- 기존: 3-7
- A7+: 4-8

### Ascension 17 (A17+)
**주요 강화**:
- LouseNormal: STRENGTHEN +4 (기존 +3)
- FungiBeast: GROW +5 (기존 +4)
- Looter: goldAmt 20 (기존 15)
- AcidSlime 계열: AI 공격적으로 변경
- SpikeSlime_L: FRAIL 3턴 (기존 2턴)
- SlaverRed/Blue: 디버프 2턴 (기존 1턴)
- GremlinTsundere: 블록 11 (기존 8)
- GremlinWizard: DOPE_MAGIC 연속 사용 가능
- GremlinWarrior: AngryPower 2 (기존 1)
- GremlinFat: 나약 1턴 추가

**CurlUpPower** (Louse 계열):
- A17+: 9-12

---

## 🔧 공통 수정 포인트

### 1. HP 조정
모든 몬스터의 생성자에서 setHp() 호출:
```java
if (AbstractDungeon.ascensionLevel >= 7) {
  setHp(MIN, MAX); // 높은 난이도
} else {
  setHp(MIN, MAX); // 기본 난이도
}
```

### 2. 데미지 조정
DamageInfo 생성 시점:
```java
if (AbstractDungeon.ascensionLevel >= 2) {
  this.damage.add(new DamageInfo(this, HIGH_DMG));
} else {
  this.damage.add(new DamageInfo(this, LOW_DMG));
}
```

### 3. AI 로직 수정
getMove() 메서드 내부:
```java
protected void getMove(int num) {
  if (AbstractDungeon.ascensionLevel >= 17) {
    // 공격적인 AI
  } else {
    // 기본 AI
  }
}
```

### 4. 특수 효과 조정
usePreBattleAction() 또는 takeTurn() 내부:
```java
if (AbstractDungeon.ascensionLevel >= 17) {
  // 강화된 효과
} else {
  // 기본 효과
}
```

---

## 📖 문서 사용 가이드

### 각 문서 구조
1. **기본 정보**: ID, 경로, HP, 타입
2. **생성자 정보**: 매개변수, 애니메이션
3. **패턴 정보**: 모든 패턴 상세 (바이트값, 의도, 효과, 코드 위치)
4. **특수 동작**: usePreBattleAction, die, damage 등
5. **AI 로직**: getMove 상세 분석 (난이도별)
6. **수정 예시**: 실용적인 수정 코드
7. **중요 필드**: 필드명, 타입, 설명, 위치
8. **관련 파일**: Powers, Effects, Actions

### 추천 읽기 순서
1. **초보자**: LouseNormal → FungiBeast → Slimes_Complete
2. **중급자**: Looter → Slavers_Complete → Gremlins_Complete
3. **고급자**: 특정 메커니즘 중심으로 교차 참조

### 수정 작업 시
1. 해당 몬스터 문서 읽기
2. "수정 예시" 섹션 참조
3. "코드 위치" 정보로 정확한 라인 찾기
4. "관련 파일" 섹션에서 의존성 확인

---

## 🎮 전투 전략 요약

### 1막 초반 (Louse, Slime_S)
- 낮은 HP, 단순한 패턴
- CurlUp 파워 먼저 해제 (블록 부여)
- 약화/나약 디버프 주의

### 1막 중반 (FungiBeast, Slime_M, Gremlins)
- 버프/디버프 누적 주의
- 그렘린은 Wizard 최우선 처치
- 점액 카드로 덱 오염 관리

### 1막 후반 (Looter, Slime_L, Slavers)
- 높은 HP, 강력한 메커니즘
- Looter는 골드 보호 우선
- 슬라임_L은 HP 50% 전에 처치
- SlaverRed의 Entangle 대비

### 그룹 조우
- **3 Louse**: CurlUp 블록량 합산 주의
- **2 FungiBeast**: SporeCloud 2회 누적
- **Slime Boss**: 분열 메커니즘 이해 필수
- **Gremlin Gang**: Wizard 최우선, 도주 대비

---

## 📝 패치/밸런스 작업 체크리스트

### 1. HP 밸런스
- [ ] A0-6 HP 범위 적절한지 확인
- [ ] A7+ HP 증가폭 적절한지 확인
- [ ] 몬스터 간 HP 비율 일관성 확인

### 2. 데미지 밸런스
- [ ] A0-1 데미지 적절한지 확인
- [ ] A2+ 데미지 증가폭 적절한지 확인
- [ ] 다중 공격 vs 단일 공격 밸런스

### 3. AI 난이도
- [ ] A0-16 AI 패턴 예측 가능성
- [ ] A17+ AI 공격성 증가 적절한지
- [ ] RNG 요소 적절한지 확인

### 4. 특수 메커니즘
- [ ] Powers 효과 지속 시간 적절한지
- [ ] 분열/도주 등 특수 동작 밸런스
- [ ] 디버프 턴수 적절한지 확인

### 5. 플레이어 경험
- [ ] 1막 초보자 진입 장벽 적절한지
- [ ] 난이도 상승 곡선 자연스러운지
- [ ] 몬스터 다양성 충분한지

---

## 🔗 외부 참조

### 관련 클래스
- **AbstractMonster**: 모든 몬스터의 베이스 클래스
- **DamageInfo**: 데미지 정보 구조체
- **AbstractDungeon**: 난이도, RNG 등 전역 정보

### 관련 Powers (자주 사용)
- **CurlUpPower**: Louse 계열 (블록 해제 + 데미지)
- **StrengthPower**: 공격력 증가
- **WeakPower**: 주는 데미지 25% 감소
- **VulnerablePower**: 받는 데미지 50% 증가
- **FrailPower**: 받는 블록 25% 감소
- **PoisonPower**: 턴 시작 시 독 데미지 + 감소
- **SporeCloudPower**: 죽을 때 취약 부여
- **ThieveryPower**: 공격 시 골드 도둑
- **EntanglePower**: 다음 턴 공격 불가
- **AngryPower**: 피격 시 힘 증가
- **SplitPower**: 분열 가능 표시 (시각적)

### 관련 Actions (자주 사용)
- **DamageAction**: 데미지 처리
- **ApplyPowerAction**: Power 부여
- **GainBlockAction**: 블록 획득
- **EscapeAction**: 도주 처리
- **SpawnMonsterAction**: 몬스터 생성 (분열)
- **MakeTempCardInDiscardAction**: 카드 추가 (점액)

---

## 📌 주의사항

1. **코드 위치(Line Number)**는 디컴파일된 코드 기준이므로 원본 소스와 다를 수 있음
2. **난이도(Ascension)** 체크는 항상 >= 연산자 사용
3. **RNG**는 `AbstractDungeon.monsterHpRng` 또는 `AbstractDungeon.aiRng` 사용
4. **애니메이션 경로**는 대소문자 구분 주의
5. **바이트값(byte)**은 패턴 식별자로 중복 없이 사용

---

## 📅 작성 정보
- **작성일**: 2025-11-08
- **대상 버전**: Slay the Spire 01-23-2019 빌드
- **커버리지**: 1막 일반 몬스터 17종 전체
