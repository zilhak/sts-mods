# STS 성능 최적화 스캔 계획

## 목표
Slay the Spire 디컴파일 소스를 체계적으로 스캔하여 성능 문제가 될 수 있는 부분을 찾고, 모드로 해결할 수 있는 방법을 문서화

---

## 스캔 순서 및 우선순위

### Phase 1: Core Systems (핵심 시스템)
**우선순위**: 🔴 최고
**이유**: 게임 루프의 기반, 모든 곳에서 호출됨

1. **CardCrawlGame.java**
   - 메인 게임 루프 (update, render)
   - 전역 상태 관리
   - 프레임 타임 계산

2. **AbstractDungeon.java**
   - 던전 업데이트 루프
   - 전역 리스트 관리 (effectList, actionManager)
   - 화면 전환 로직

**스캔 항목**:
- [ ] 프레임당 반복되는 로직
- [ ] 불필요한 객체 생성
- [ ] 리스트 순회 최적화 가능 여부
- [ ] 캐싱 가능한 계산

---

### Phase 2: Rendering Pipeline (렌더링 파이프라인)
**우선순위**: 🔴 최고
**이유**: FPS에 직접적인 영향, 프레임당 실행

1. **SpriteBatch 사용 패턴**
   - begin/end 호출 빈도
   - 불필요한 텍스처 바인딩
   - 드로우콜 최적화

2. **render() 메서드들**
   - AbstractCard.render()
   - AbstractMonster.render()
   - AbstractRelic.render()
   - AbstractPotion.render()
   - UI 요소 render()

**스캔 항목**:
- [ ] 매 프레임 텍스처 로드 여부
- [ ] 불필요한 SpriteBatch begin/end
- [ ] 화면 밖 객체 렌더링
- [ ] 알파 블렌딩 최적화

---

### Phase 3: VFX & Effects (시각 효과)
**우선순위**: 🟡 중간
**이유**: 많이 생성되면 성능 저하

1. **AbstractGameEffect 및 서브클래스**
   - effectList 관리
   - 이펙트 생명주기
   - 파티클 시스템

2. **vfx 패키지**
   - com.megacrit.cardcrawl.vfx.*
   - 이펙트 풀링 가능 여부
   - 동시 이펙트 개수 제한

**스캔 항목**:
- [ ] 객체 풀링 미사용
- [ ] 과도한 파티클 생성
- [ ] 이펙트 정리 누락 (메모리 누수)
- [ ] 불필요한 이펙트 업데이트

---

### Phase 4: Card System (카드 시스템)
**우선순위**: 🟡 중간
**이유**: 전투 중 자주 실행

1. **AbstractCard.java**
   - update() 메서드
   - applyPowers() 메서드
   - calculateCardDamage() 메서드

2. **CardGroup.java**
   - 카드 리스트 순회
   - 카드 이동/추가/제거

**스캔 항목**:
- [ ] 매 프레임 데미지 재계산
- [ ] 불필요한 applyPowers 호출
- [ ] 카드 리스트 복사 비용
- [ ] 정렬 알고리즘 최적화

---

### Phase 5: Monster & Combat (몬스터 & 전투)
**우선순위**: 🟡 중간
**이유**: 전투 중 실행

1. **AbstractMonster.java**
   - update() 메서드
   - AI 로직 (getMove)
   - 애니메이션 업데이트

2. **GameActionManager.java**
   - 액션 큐 처리
   - 액션 실행 최적화

**스캔 항목**:
- [ ] 몬스터 AI 매 프레임 실행 여부
- [ ] 액션 큐 오버헤드
- [ ] 의도 계산 중복
- [ ] 애니메이션 업데이트 최적화

---

### Phase 6: Memory Management (메모리 관리)
**우선순위**: 🟢 낮음
**이유**: 장기 플레이 시 영향

1. **리소스 정리**
   - Texture dispose
   - scene dispose
   - 이벤트 리스너 제거

2. **리스트 관리**
   - ArrayList clear() vs new ArrayList()
   - 리스트 크기 사전 할당

**스캔 항목**:
- [ ] 메모리 누수 패턴
- [ ] 불필요한 객체 보유
- [ ] 리스트 재사용 가능 여부
- [ ] 캐시 무효화 전략

---

### Phase 7: UI Systems (UI 시스템)
**우선순위**: 🟢 낮음
**이유**: 상대적으로 부하 적음

1. **Hitbox 시스템**
   - 매 프레임 충돌 검사
   - 불필요한 hitbox 업데이트

2. **UI 화면들**
   - 화면 전환 시 정리
   - UI 요소 업데이트

**스캔 항목**:
- [ ] 불필요한 마우스 위치 체크
- [ ] UI 요소 중복 업데이트
- [ ] 화면 밖 UI 업데이트

---

### Phase 8: Helper Classes (헬퍼 클래스)
**우선순위**: 🟢 낮음
**이유**: 성능 핫스팟이 적음

1. **MonsterHelper.java**
   - 몬스터 생성 최적화

2. **CardHelper.java**
   - 카드 생성 최적화

3. **PotionHelper.java**
   - 포션 생성 최적화

**스캔 항목**:
- [ ] 불필요한 리플렉션 사용
- [ ] 캐싱 가능한 생성 로직
- [ ] 문자열 비교 최적화

---

## 스캔 방법론

### 1. 정적 분석
```bash
# 특정 패턴 검색
grep -r "new ArrayList" --include="*.java"
grep -r "for.*effectList" --include="*.java"
grep -r "\.render(" --include="*.java"
```

### 2. 핫스팟 식별
- update() 메서드 내부 로직
- render() 메서드 내부 로직
- 반복문 내부 객체 생성
- 매 프레임 실행되는 계산

### 3. 문서화 형식
각 발견된 문제마다 별도 파일 생성:
```
performance/
├── 01_EffectPooling.md       # 이펙트 객체 풀링
├── 02_CardDamageCache.md     # 카드 데미지 캐싱
├── 03_RenderCulling.md       # 화면 밖 렌더링 제거
├── 04_BatchOptimization.md   # SpriteBatch 최적화
└── ...
```

**파일 구조**:
```markdown
# [문제명]

## 문제 발견 위치
- 파일: xxx.java
- 메서드: yyy()
- 라인: zzz

## 문제 설명
왜 이것이 성능 문제인가?

## 원인 분석
코드 분석 및 실행 빈도

## 해결 방법
SpirePatch 코드 예제

## 성능 개선 효과
예상 효과 (FPS 향상, 메모리 절감 등)

## 주의사항
부작용 및 호환성 문제
```

---

## 예상 결과물

### 고성능 영향 (High Impact)
1. **EffectPooling.md** - 이펙트 객체 재사용
2. **RenderCulling.md** - 화면 밖 객체 렌더링 스킵
3. **BatchOptimization.md** - SpriteBatch 호출 최적화
4. **CardDamageCache.md** - 카드 데미지 계산 캐싱

### 중간 영향 (Medium Impact)
5. **MonsterAIThrottle.md** - 몬스터 AI 실행 빈도 감소
6. **TextureAtlas.md** - 텍스처 아틀라스 최적화
7. **ListPreallocation.md** - 리스트 사전 할당

### 낮은 영향 (Low Impact)
8. **StringOptimization.md** - 문자열 비교 최적화
9. **HitboxCulling.md** - 불필요한 Hitbox 업데이트 제거
10. **MemoryLeakFixes.md** - 메모리 누수 패치

---

## 다음 단계

이 계획에 동의하면:
1. Phase 1부터 순차적으로 스캔 시작
2. 발견된 문제마다 문서 생성
3. 모든 Phase 완료 후 INDEX.md 생성 (전체 요약)

**예상 소요 시간**: Phase별 30분~1시간
**예상 문서 수**: 10~20개

---

## 질문

1. 이 순서가 적절한가?
2. 특정 영역에 집중하고 싶은가? (예: VFX만, 렌더링만)
3. 추가로 스캔해야 할 영역이 있는가?
