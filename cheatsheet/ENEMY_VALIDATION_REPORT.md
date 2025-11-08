# ENEMY_LIST.md 검증 리포트

생성 일시: 2025-11-08
검증 대상: E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\

---

## 검증 결과 요약

### 최종 결과: ✅ 모든 항목 검증 완료 (100% 일치)

- **총 검증 항목**: 68개
- **존재 확인**: 68개 (100%)
- **누락**: 0개
- **추가 항목**: 0개

---

## 상세 검증 결과

### 1막 (Exordium) - 28개

#### 일반 적 (14개)
- ✅ LouseNormal
- ✅ LouseDefensive
- ✅ Cultist
- ✅ JawWorm
- ✅ Looter
- ✅ FungiBeast
- ✅ AcidSlime_S
- ✅ AcidSlime_M
- ✅ AcidSlime_L
- ✅ SpikeSlime_S
- ✅ SpikeSlime_M
- ✅ SpikeSlime_L
- ✅ SlaverRed
- ✅ SlaverBlue

#### 그렘린 계열 (5개)
- ✅ GremlinTsundere (방패 그렘린)
- ✅ GremlinWizard (마법사 그렘린)
- ✅ GremlinWarrior (화난 그렘린)
- ✅ GremlinFat (뚱뚱한 그렘린)
- ✅ GremlinThief (교활한 그렘린)

#### 엘리트 (3개)
- ✅ GremlinNob
- ✅ Lagavulin
- ✅ Sentry

#### 보스 (5개)
- ✅ SlimeBoss
- ✅ TheGuardian
- ✅ Hexaghost
- ✅ HexaghostBody
- ✅ HexaghostOrb

#### 특수 (1개)
- ✅ ApologySlime

**결과**: 28/28 ✅

---

### 2막 (City) - 20개

#### 일반 적 (10개)
- ✅ Byrd
- ✅ Chosen
- ✅ Centurion
- ✅ Healer
- ✅ Snecko
- ✅ SnakePlant
- ✅ SphericGuardian
- ✅ Mugger
- ✅ ShelledParasite
- ✅ TorchHead

#### 붉은 가면 도적단 (3개)
- ✅ BanditBear
- ✅ BanditPointy
- ✅ BanditLeader

#### 엘리트 (3개)
- ✅ BookOfStabbing
- ✅ GremlinLeader
- ✅ Taskmaster

#### 보스 (4개)
- ✅ BronzeAutomaton
- ✅ BronzeOrb
- ✅ Champ
- ✅ TheCollector

**결과**: 20/20 ✅

---

### 3막 (Beyond) - 17개

#### 일반 적 (9개)
- ✅ Spiker
- ✅ Exploder
- ✅ OrbWalker
- ✅ Darkling
- ✅ Maw
- ✅ Transient
- ✅ SpireGrowth
- ✅ WrithingMass
- ✅ Repulsor

#### 엘리트 (4개)
- ✅ GiantHead
- ✅ Nemesis
- ✅ Reptomancer
- ✅ SnakeDagger

#### 보스 (4개)
- ✅ AwakenedOne
- ✅ TimeEater
- ✅ Donu
- ✅ Deca

**결과**: 17/17 ✅

---

### 4막 (Ending) - 3개

- ✅ CorruptHeart
- ✅ SpireShield
- ✅ SpireSpear

**결과**: 3/3 ✅

---

## 폴더별 추가 항목 검사

### EXORDIUM 폴더
**디스크 파일 28개** vs **ENEMY_LIST.md 항목 28개**
- 추가/누락 항목: 0개 ✅

### CITY 폴더
**디스크 파일 20개** vs **ENEMY_LIST.md 항목 20개**
- 추가/누락 항목: 0개 ✅

### BEYOND 폴더
**디스크 파일 17개** vs **ENEMY_LIST.md 항목 17개**
- 추가/누락 항목: 0개 ✅

### ENDING 폴더
**디스크 파일 3개** vs **ENEMY_LIST.md 항목 3개**
- 추가/누락 항목: 0개 ✅

---

## 검증 방법론

1. **소스 탐색**
   - 디스크 경로: `E:/workspace/sts-decompile/com/megacrit/cardcrawl/monsters/`
   - 파일 형식: `.java` (자바 소스 파일)

2. **리스트 비교**
   - ENEMY_LIST.md의 모든 항목을 파일 시스템의 실제 클래스와 1:1 비교
   - 완전 일치 확인 (클래스명 동일성)

3. **양방향 검증**
   - ENEMY_LIST.md에 있는 항목이 파일에 존재하는가? ✅
   - 파일에 있는 항목이 ENEMY_LIST.md에 나열되어 있는가? ✅

---

## 결론

**ENEMY_LIST.md는 완벽하게 최신 상태이며 모든 적 클래스를 정확하게 나열하고 있습니다.**

### 권장사항

1. **문서 신뢰도**: 높음 (100% 정확도)
2. **용도**: 안전하게 패치 개발 및 참조에 사용 가능
3. **유지보수**: 게임 업데이트 시에만 업데이트 필요

---

## 파일 경로 참조

| 작업 | 경로 |
|------|------|
| 검증된 리스트 | E:\workspace\sts-mods\cheatsheet\ENEMY_LIST.md |
| 1막 클래스 | E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\exordium\ |
| 2막 클래스 | E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\city\ |
| 3막 클래스 | E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\beyond\ |
| 4막 클래스 | E:\workspace\sts-decompile\com\megacrit\cardcrawl\monsters\ending\ |

