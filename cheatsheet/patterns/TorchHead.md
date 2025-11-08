# 횃불 머리 (Torch Head)

## 기본 정보

**클래스명**: `TorchHead`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.TorchHead`
**ID**: `"TorchHead"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A8) | 38-40 |
| A9+ | 40-45 |

**특징**: 매우 낮은 HP, 단순한 공격 패턴

---

## 패턴 정보

### 패턴 1: 태클 (Tackle) - 유일한 패턴

**의도**: `ATTACK`
**데미지**: 7

**효과**:
- 플레이어에게 **7 데미지** 공격
- **매 턴 반복**

**코드 특징**:
```java
// 생성자에서 설정
setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
damage.add(new DamageInfo(this, 7));
```

**수정 포인트**:
- 데미지 변경: 7 → 조정값
- 새로운 패턴 추가 가능

---

## AI 로직 (getMove)

**실제 소스코드**:
```java
protected void getMove(int num) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK, 7);
}
```

**로직 설명**:
- **확률 없음**: 매 턴 100% Tackle
- **변화 없음**: 조건 분기 없음
- **단순 반복**: 같은 패턴만 사용

**특징**:
- 가장 단순한 AI 구조
- 패턴 변화 없음
- 확률 로직 없음

---

## 특수 동작

### 시각 효과

**불 효과**:
```java
// update() 메서드에서 0.04초마다 실행
if (!isDying) {
    fireTimer -= Gdx.graphics.getDeltaTime();
    if (fireTimer < 0.0F) {
        fireTimer = 0.04F;
        // 화염 이펙트 생성
        AbstractDungeon.effectList.add(
            new TorchHeadFireEffect(...)
        );
    }
}
```

**효과**:
- 순수 시각적 효과
- 게임플레이 영향 없음
- 머리 부분에서 불 파티클 발생

---

## 전투 전략

### 플레이어 대응

**추천 전략**:
1. **속공**: 매우 낮은 HP (38-45) 이용해 빠른 처치
2. **단순 대응**: 예측 가능한 7 데미지에만 대비
3. **AOE 활용**: 멀티 인카운터 대비

**위험 요소**:
- **개별적으로는 위협 낮음**
- **멀티 인카운터 시**: 3마리 × 7 = 21 데미지

---

## 멀티 인카운터

### 3 Torch Heads

**조합 정보**:
- Torch Head 3마리 동시 등장
- 각자 독립적으로 7 데미지 공격
- 총 21 데미지/턴

**전략 변화**:
- **AOE 필수**: Whirlwind, Immolate, Cleave
- **우선순위**: 낮은 HP로 빠른 각개격파
- **방어**: 21 데미지 대비 충분한 Block 확보

**위험도**:
- 3마리 모두 생존 → 21 데미지/턴
- 빠른 처치 실패 시 누적 데미지 증가
- AOE 없으면 단타로 하나씩 처리 필요

---

## 수정 예시

### 1. 데미지 증가 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TorchHead",
    method = SpirePatch.CONSTRUCTOR
)
public static class TorchHeadDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(TorchHead __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 데미지 7 → 9
            ReflectionHacks.setPrivateInherited(
                __instance,
                AbstractMonster.class,
                "damage",
                new ArrayList<DamageInfo>() {{
                    add(new DamageInfo(__instance, 9));
                }}
            );
        }
    }
}
```

### 2. 새로운 패턴 추가 (화염 공격)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TorchHead",
    method = "getMove"
)
public static class TorchHeadNewPatternPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(TorchHead __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 30% 확률로 화염 공격 (12 데미지 + Burn 1장)
            if (num < 30) {
                ReflectionHacks.privateMethod(
                    AbstractMonster.class,
                    "setMove",
                    byte.class,
                    Intent.class,
                    int.class
                ).invoke(__instance, (byte)2, Intent.ATTACK_DEBUFF, 12);
                return SpireReturn.Return(null);
            }
        }
        return SpireReturn.Continue();
    }
}

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TorchHead",
    method = "takeTurn"
)
public static class TorchHeadNewPatternTurnPatch {
    @SpirePostfixPatch
    public static void Postfix(TorchHead __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            int nextMove = ReflectionHacks.getPrivate(
                __instance,
                AbstractMonster.class,
                "nextMove"
            );

            if (nextMove == 2) {
                // 화염 공격 실행
                AbstractDungeon.actionManager.addToBottom(
                    new AnimateSlowAttackAction(__instance)
                );
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(
                        AbstractDungeon.player,
                        new DamageInfo(__instance, 12),
                        AttackEffect.FIRE
                    )
                );
                AbstractDungeon.actionManager.addToBottom(
                    new MakeTempCardInDiscardAction(new Burn(), 1)
                );
            }
        }
    }
}
```

### 3. HP 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.TorchHead",
    method = SpirePatch.CONSTRUCTOR
)
public static class TorchHeadHPPatch {
    @SpirePostfixPatch
    public static void Postfix(TorchHead __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            if (AbstractDungeon.ascensionLevel >= 9) {
                // A9+: 40-45 → 50-55
                __instance.setHp(50, 55);
            } else {
                // A0-A8: 38-40 → 48-50
                __instance.setHp(48, 50);
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `damage` | ArrayList<DamageInfo> | 데미지 정보 (7) |
| `nextMove` | byte | 항상 1 (Tackle) |
| `fireTimer` | float | 화염 이펙트 타이머 (0.04초) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/TorchHead.java`
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.TorchHeadFireEffect`
- **액션**:
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `SetMoveAction`

---

## 참고사항

1. **가장 단순한 몬스터**: Act 2 중 가장 단순한 AI 구조
2. **단일 패턴**: 확률 분기 없이 매 턴 Tackle만 사용
3. **낮은 HP**: 38-45로 빠른 처치 가능
4. **멀티 인카운터**: 3마리 동시 등장 시 총 21 데미지/턴
5. **시각 효과만**: 불 파티클은 순수 장식
6. **수정 용이**: 단순한 구조로 패턴 추가가 쉬움
