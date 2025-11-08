# 1막 몬스터 문서 검증 보고서

검증 날짜: 2025-11-08
검증 대상: E:\workspace\sts-mods\cheatsheet\patterns\ 내 1막 몬스터 문서
검증 방법: 디컴파일 소스(E:\workspace\sts-decompile\) 대조

---

## 요약

**검증 완료**: Cultist, JawWorm, LouseNormal, LouseDefensive
**더미 데이터 발견**: AcidSlime_S (부분), TorchHead (대량)

---

## 1. 검증 완료 - 정확한 문서 ✅

### Cultist.md
- **상태**: ✅ 정확
- **검증 항목**:
  - HP 수치: 정확
  - 패턴 (Incantation, Dark Strike): 정확
  - AI 로직 (firstMove 기반): 정확
  - Ritual 수치 (A2, A17 분기): 정확

### JawWorm.md
- **상태**: ✅ 정확
- **검증 항목**:
  - HP 수치: 정확
  - 패턴 3개 (Chomp, Bellow, Thrash): 정확
  - 복잡한 확률 기반 AI: 정확
  - 하드 모드 메커니즘: 정확

### LouseNormal.md
- **상태**: ✅ 정확
- **검증 항목**:
  - HP 수치: 정확
  - 패턴 (Bite, Strengthen): 정확
  - A17 vs A0-16 AI 차이: 정확
  - CurlUp 파워 값: 정확

### LouseDefensive.md
- **상태**: ✅ 정확
- **검증 항목**:
  - HP 수치: 정확
  - 패턴 (Bite, Weaken): 정확
  - WebEffect 시각 효과: 정확
  - AI 로직: 정확

---

## 2. 오류 발견 - 수정 필요 ⚠️

### AcidSlime_S (Slimes_Complete.md)

**파일 위치**: `E:/workspace/sts-mods/cheatsheet/patterns/Slimes_Complete.md` (Line 7-31)

**문제**: AI 로직 설명이 불완전함

**실제 코드 동작** (AcidSlime_S.java):
```java
// takeTurn() - Line 62-81
public void takeTurn() {
    switch (this.nextMove) {
        case 1: // TACKLE 실행 후
            // ... 공격 실행
            setMove((byte)2, Intent.DEBUFF); // 다음 패턴을 DEBUFF로 강제 설정
            break;
        case 2: // DEBUFF 실행 후
            // ... 약화 실행
            setMove((byte)1, Intent.ATTACK, ...); // 다음 패턴을 TACKLE로 강제 설정
            break;
    }
}

// getMove() - Line 84-97 (첫 턴에만 호출됨)
protected void getMove(int num) {
    if (AbstractDungeon.ascensionLevel >= 17) {
        if (lastTwoMoves((byte)1)) {
            setMove((byte)1, ...); // TACKLE
        } else {
            setMove((byte)2, ...); // DEBUFF
        }
    } else {
        if (AbstractDungeon.aiRng.randomBoolean()) {
            setMove((byte)1, ...); // TACKLE
        } else {
            setMove((byte)2, ...); // DEBUFF
        }
    }
}
```

**실제 패턴 순서**:
1. **첫 턴**: `getMove()` 호출
   - A17+: 보통 DEBUFF로 시작 (lastTwoMoves(TACKLE)이 false이므로)
   - A0-16: 50-50 랜덤
2. **두 번째 턴 이후**: `takeTurn()`의 `setMove()` 호출로 자동 교대
   - TACKLE 사용 → 자동으로 다음 패턴을 DEBUFF로 설정
   - DEBUFF 사용 → 자동으로 다음 패턴을 TACKLE로 설정
   - **결과**: 완벽한 교대 패턴 (TACKLE ↔ DEBUFF ↔ TACKLE ↔ DEBUFF ...)

**현재 문서의 오류**:
- `getMove()`의 AI 로직만 설명하고 있음
- `takeTurn()`에서 `setMove()`를 호출하여 다음 패턴을 강제하는 메커니즘을 언급하지 않음
- 첫 턴 이후로는 `getMove()`가 무시된다는 중요한 사실을 누락

**수정 필요 내용**:
```markdown
### AI 로직

**⚠️ 중요**: AcidSlime_S는 `takeTurn()` 내에서 `setMove()`를 호출하여 패턴을 자동 교대합니다. `getMove()`는 **첫 턴에만** 작동합니다.

**첫 턴 (getMove(), Line 84-96)**:
- **A17+**: lastTwoMoves(TACKLE)이면 TACKLE, 아니면 DEBUFF (보통 DEBUFF로 시작)
- **A0-16**: 50% TACKLE, 50% DEBUFF

**두 번째 턴 이후 (takeTurn()의 setMove(), Line 68, 78)**:
```java
case 1: // TACKLE 실행 후
    setMove((byte)2, Intent.DEBUFF); // 자동으로 DEBUFF 설정

case 2: // DEBUFF 실행 후
    setMove((byte)1, Intent.ATTACK, ...); // 자동으로 TACKLE 설정
```

**실제 패턴**:
```
Turn 1: getMove()로 결정
Turn 2~: TACKLE ↔ DEBUFF 완벽한 교대
```
```

---

## 3. 대량 더미 데이터 발견 - 전체 재작성 필요 🚨

### TorchHead.md

**파일 위치**: `E:/workspace/sts-mods/cheatsheet/patterns/TorchHead.md`

**심각도**: **CRITICAL** - 문서의 50% 이상이 허구

**실제 코드 (TorchHead.java)**:
```java
public class TorchHead extends AbstractMonster {
    public static final int ATTACK_DMG = 7;
    private static final byte TACKLE = 1;

    public TorchHead(float x, float y) {
        super(NAME, "TorchHead", ...);
        setMove((byte)1, Intent.ATTACK, 7);
        this.damage.add(new DamageInfo(this, 7));

        if (AbstractDungeon.ascensionLevel >= 9) {
            setHp(40, 45);
        } else {
            setHp(38, 40);
        }
    }

    public void takeTurn() {
        switch (this.nextMove) {
            case 1: // TACKLE만 존재
                // 7 데미지 공격
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(AbstractDungeon.player,
                        this.damage.get(0), AttackEffect.BLUNT_LIGHT));
                AbstractDungeon.actionManager.addToBottom(
                    new SetMoveAction(this, (byte)1, Intent.ATTACK, 7));
                break;
        }
    }

    protected void getMove(int num) {
        setMove((byte)1, Intent.ATTACK, 7);
    }
}
```

**실제 패턴**: **TACKLE (7 데미지)만 존재**

**문서의 허구 내용**:
1. ❌ **Pattern 2: Burning Attack (12 dmg + Burn)** - 완전히 존재하지 않음
2. ❌ **70% Tackle / 30% Burning Attack 확률** - 허구
3. ❌ **Burn 카드 추가 메커니즘** - 코드에 없음
4. ❌ **A17+ Burn 2장** - 허구
5. ❌ **복잡한 AI 로직** - 실제로는 항상 TACKLE만 사용
6. ❌ **Fire Breathing 시너지** - Burn이 없으므로 무의미
7. ❌ **멀티 인카운터에서 Burn 누적** - 허구

**실제 HP**:
- A0-A8: 38-40 (문서 A0-A6와 불일치)
- A9+: 40-45 (문서 A7+와 불일치)

**올바른 정보**:
```markdown
# 횃불 머리 (Torch Head)

## 기본 정보
- **ID**: `TorchHead`
- **등장 지역**: 2막 (The City)
- **타입**: 일반 적

## HP 정보
| 난이도 | HP 범위 |
|--------|---------|
| A0-A8 | 38-40 |
| A9+ | 40-45 |

## 패턴 정보

### 유일 패턴: Tackle
- **의도**: ATTACK
- **데미지**: 7 (고정, 모든 난이도 동일)
- **발동**: 매 턴

## AI 로직
```java
protected void getMove(int num) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
}
```
**설명**: 항상 TACKLE만 사용. 패턴 선택이나 확률 없음.

## 전투 전략
- 매우 단순한 적
- HP가 낮아 빠른 처치 가능
- 7 데미지를 매 턴 방어하면 됨
- 복잡한 메커니즘 없음

## 특수 효과
- 시각 효과: 머리의 횃불에서 불꽃 파티클 (TorchHeadFireEffect)
- 전투 메커니즘에는 영향 없음

## 참고사항
- 가장 단순한 적 중 하나
- 패턴이 1개뿐
- Burn 카드나 다른 디버프 없음
```

---

## 4. 검증 대기 중 📋

### 1막 일반 몬스터
- [ ] FungiBeast.md
- [ ] Looter.md
- [ ] Slimes_Complete.md (나머지 5종 검증 필요)

### 1막 그렘린
- [ ] Gremlins_Complete.md
  - GremlinFat
  - GremlinSneaky (GremlinThief)
  - GremlinTsundere
  - GremlinWarrior
  - GremlinWizard

### 1막 슬레이버
- [ ] Slavers_Complete.md
  - SlaverBlue
  - SlaverRed

### 1막 엘리트
- [ ] GremlinNob.md
- [ ] Lagavulin.md
- [ ] Sentry.md

### 1막 보스
- [ ] SlimeBoss.md
- [ ] TheGuardian.md
- [ ] Hexaghost.md

---

## 5. 검증 방법

### 필수 확인 사항
1. **getMove() 메서드**: 실제로 setMove()를 호출하는 패턴만 유효
2. **takeTurn() 메서드**: case문이 실제로 도달 가능한지 확인
3. **takeTurn() 내 setMove() 호출**: 다음 패턴을 강제하는지 확인
4. **HP 값**: 생성자의 setHp() 호출과 난이도 분기 확인
5. **데미지 값**: damage 리스트 초기화와 실제 사용 확인

### TorchHead 같은 더미 데이터 식별 방법
```
1. getMove()에서 setMove() 호출 확인
2. 호출되는 모든 바이트 값 추출
3. takeTurn()의 switch-case에서 해당 바이트 값 case 존재 확인
4. case 내부에 도달 불가능한 조건이 있는지 확인
5. takeTurn()에만 있고 getMove()에 없는 case → 더미 데이터
```

---

## 6. 다음 단계

1. AcidSlime_S 섹션 수정 (Slimes_Complete.md Line 18-30)
2. TorchHead.md 전체 재작성
3. 나머지 Slimes 검증 (AcidSlime_M, AcidSlime_L, SpikeSlime 3종)
4. Gremlins_Complete.md 검증 (5종)
5. 나머지 1막 몬스터 검증

---

## 7. 참고: 더미 데이터가 있는 다른 문서

grep 결과:
```
CorruptHeart.md
Donu_Deca.md
Nemesis.md
OrbWalker.md
Reptomancer.md
Sentry.md
SlimeBoss.md
SpireShield_SpireSpear.md
TheCollector.md
TimeEater.md
TorchHead.md (검증 완료 - 대량 더미)
```

이 중 1막 몬스터:
- Sentry.md
- SlimeBoss.md

---

## 결론

**정확도 평가** (1막 검증 완료 문서 기준):
- ✅ 완벽: Cultist, JawWorm, LouseNormal, LouseDefensive (4개)
- ⚠️ 부분 오류: AcidSlime_S (1개)
- 🚨 대량 허구: TorchHead (1개, 2막이지만 예시로 검증)

**1막 전체 검증률**: 4/20 (20%) - 검증 계속 필요
