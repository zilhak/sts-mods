# 스네코 (Snecko)

## 기본 정보

**클래스명**: `Snecko`
**전체 경로**: `com.megacrit.cardcrawl.monsters.city.Snecko`
**ID**: `"Snecko"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 2막 (The City)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 114-120 |
| A7+ | 120-125 |

**코드 위치**: 62-66줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(120, 125);
} else {
    setHp(114, 120);
}
```

---

## 패턴 정보

### 패턴 1: 혼란스런 시선 (Perplexing Gaze / GLARE)

**바이트 값**: `1`
**의도**: `STRONG_DEBUFF`
**발동 조건**: **첫 번째 턴 고정**

**효과**:
- 플레이어에게 **Confusion 파워** 부여
- 손패의 모든 카드 코스트를 **랜덤화 (0-3)**
- **전투 종료까지 지속** (매 턴 시작 시 손패 카드 코스트 재설정)

**코드 위치**: 33줄 (상수), 156-160줄 (첫 턴), 82-90줄 (실행)

```java
// 상수 정의
private static final byte GLARE = 1;

// 첫 턴 고정 패턴
if (this.firstTurn) {
    this.firstTurn = false;
    setMove(MOVES[0], (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_SNECKO_GLARE")
    );
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction((AbstractCreature)this,
            new IntimidateEffect(this.hb.cX, this.hb.cY), 0.5F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new FastShakeAction((AbstractCreature)AbstractDungeon.player, 1.0F, 1.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new ConfusionPower((AbstractCreature)AbstractDungeon.player))
    );
    break;
```

**특징**:
- **첫 턴에 반드시 사용** (firstTurn = true)
- IntimidateEffect 비주얼 효과
- 화면 흔들림 효과
- Confusion 파워로 카드 코스트 랜덤화

**수정 포인트**:
- 첫 턴 고정 제거: `firstTurn` 로직 수정
- Confusion 범위 변경: ConfusionPower 클래스 수정
- 추가 디버프 부여 (Weak, Vulnerable 등)

---

### 패턴 2: 물어뜯기 (Bite / BITE)

**바이트 값**: `2`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 15 |
| A2+ | 18 |

**코드 위치**: 34줄, 36줄, 68-70줄, 76줄, 92-104줄

```java
// 상수 정의
private static final byte BITE = 2;
private static final int BITE_DAMAGE = 15;
private static final int A_2_BITE_DAMAGE = 18;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.biteDmg = 18;
} else {
    this.biteDmg = 15;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.biteDmg));

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK_2")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3F));
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(
            new BiteEffect(
                AbstractDungeon.player.hb.cX + MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                AbstractDungeon.player.hb.cY + MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                Color.CHARTREUSE.cpy()
            ), 0.3F
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.NONE
        )
    );
    break;
```

**특징**:
- 초록색 물어뜯기 비주얼 효과 (BiteEffect)
- 위치 랜덤화 (±50 픽셀)
- 단일 공격

**수정 포인트**:
- 데미지 변경: `biteDmg` 필드 또는 생성자 로직
- 추가 효과 부여 (Weak, Poison 등)

---

### 패턴 3: 꼬리 후려치기 (Tail Whip / TAIL)

**바이트 값**: `3`
**의도**: `ATTACK_DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 **데미지**
- 플레이어에게 **Vulnerable 2턴** 부여
- **A17+**: 플레이어에게 **Weak 2턴** 추가 부여

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 8 |
| A2+ | 10 |

**디버프**:
| 난이도 | Vulnerable | Weak |
|--------|------------|------|
| 기본 (A0-A16) | 2턴 | - |
| A17+ | 2턴 | 2턴 |

**코드 위치**: 33줄, 35줄, 37줄, 40줄, 70-73줄, 77줄, 105-118줄

```java
// 상수 정의
private static final byte TAIL = 3;
private static final int TAIL_DAMAGE = 8;
private static final int A_2_TAIL_DAMAGE = 10;
private static final int VULNERABLE_AMT = 2;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.tailDmg = 10;
} else {
    this.tailDmg = 8;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.tailDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.SLASH_DIAGONAL
        )
    );
    // A17+: Weak 추가
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)AbstractDungeon.player,
                (AbstractCreature)this,
                new WeakPower((AbstractCreature)AbstractDungeon.player, 2, true),
                2
            )
        );
    }
    // 항상: Vulnerable 부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new VulnerablePower((AbstractCreature)AbstractDungeon.player, 2, true),
            2
        )
    );
    break;
```

**특징**:
- **단일 공격** (다단 히트가 아님)
- Vulnerable 항상 부여
- A17+에서 Weak 추가

**수정 포인트**:
- 데미지 변경: `tailDmg` 필드
- 디버프 수치 조정: VULNERABLE_AMT 상수
- 다단 히트로 변경: 공격 로직 전체 수정

---

## AI 로직 (getMove)

**코드 위치**: 155-174줄

### 첫 번째 턴 (고정)
```java
if (this.firstTurn) {
    this.firstTurn = false;
    setMove(MOVES[0], (byte)1, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}
```
**무조건 GLARE (Perplexing Gaze) 사용**

### 이후 턴 (num < 40, 40% 확률)
```java
if (num < 40) {
    setMove(MOVES[1], (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
    return;
}
```
**TAIL (Tail Whip) 사용**

### 이후 턴 (num >= 40, 60% 확률)
```java
if (lastTwoMoves((byte)2)) {
    setMove(MOVES[1], (byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
        ((DamageInfo)this.damage.get(1)).base);
} else {
    setMove(MOVES[2], (byte)2, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
}
```

**로직**:
- 이전 2턴이 모두 BITE(2)이면 → TAIL(3)
- 그 외 → BITE(2)

### 전체 패턴 흐름

1. **첫 턴**: GLARE (100%)
2. **이후 턴**:
   - 40% → TAIL
   - 60% → BITE/TAIL (이전 2턴이 모두 BITE면 TAIL, 아니면 BITE)

**수정 포인트**:
- 첫 턴 GLARE 제거
- 확률 조정
- 패턴 추가/변경

---

## 특수 동작

### Confusion 파워 메커니즘

**ConfusionPower 효과**:
- 전투 종료까지 영구 지속
- 매 턴 시작 시 손패의 모든 카드 코스트를 **0-3 랜덤**으로 재설정
- Orange Pellets 유물로 제거 가능

**전략적 영향**:
- 코스트 0 카드 → 강력한 콤보 가능
- 코스트 3 카드 → 사용 불가능
- 덱 구성 전략 무력화

**Snecko Eye 유물과의 관계**:
- Snecko Eye: 카드 드로우 +2, 카드 코스트 0-3 랜덤
- Confusion 파워: 동일한 코스트 랜덤화 효과
- 중복 시 효과 재적용 (유의미한 차이 없음)

---

## 애니메이션 상태

**코드 위치**: 131-142줄

```java
public void changeState(String stateName) {
    switch (stateName) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
        case "ATTACK_2":
            this.state.setAnimation(0, "Attack_2", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**애니메이션**:
- "Attack": GLARE 패턴용
- "Attack_2": BITE 패턴용
- "Idle": 기본 대기

---

## 수정 예시

### 1. GLARE 첫 턴 고정 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Snecko",
    method = "getMove"
)
public static class SneckoFirstTurnPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Snecko __instance, int num) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // firstTurn 체크 스킵 → 일반 패턴 로직 사용
            try {
                Field firstTurnField = Snecko.class.getDeclaredField("firstTurn");
                firstTurnField.setAccessible(true);
                firstTurnField.set(__instance, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SpireReturn.Continue();
    }
}
```

### 2. Tail Whip 강화 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Snecko",
    method = "takeTurn"
)
public static class SneckoTailWhipPatch {
    @SpirePostfixPatch
    public static void Postfix(Snecko __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && __instance.nextMove == 3) {
            // Tail Whip 사용 시 Frail 2턴 추가
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player,
                    __instance,
                    new FrailPower(AbstractDungeon.player, 2, true),
                    2
                )
            );
        }
    }
}
```

### 3. Bite 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.Snecko",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class }
)
public static class SneckoBiteDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Snecko __instance, float x, float y) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            try {
                Field biteDmgField = Snecko.class.getDeclaredField("biteDmg");
                biteDmgField.setAccessible(true);
                int currentDmg = biteDmgField.getInt(__instance);
                biteDmgField.setInt(__instance, currentDmg + 3); // +3 데미지

                // damage ArrayList도 업데이트
                __instance.damage.get(0).base = currentDmg + 3;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstTurn` | boolean | 첫 턴 여부 (GLARE 고정) |
| `biteDmg` | int | Bite 데미지 (15 또는 18) |
| `tailDmg` | int | Tail 데미지 (8 또는 10) |
| `damage` | ArrayList&lt;DamageInfo&gt; | [0]: biteDmg, [1]: tailDmg |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/city/Snecko.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.ConfusionPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
- **유물**:
  - `com.megacrit.cardcrawl.relics.SneckoEye` (유사 효과)
  - `com.megacrit.cardcrawl.relics.OrangePellets` (Confusion 제거)
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.combat.BiteEffect`
  - `com.megacrit.cardcrawl.vfx.combat.IntimidateEffect`
- **액션**:
  - `DamageAction`
  - `ApplyPowerAction`
  - `ChangeStateAction`
  - `VFXAction`
  - `FastShakeAction`
  - `AnimateFastAttackAction`

---

## 참고사항

1. **첫 턴 고정**: 첫 턴은 반드시 GLARE (Perplexing Gaze) 사용
2. **Confusion 파워**: 전투 종료까지 영구 지속, 매 턴 코스트 재설정
3. **Tail Whip**: 단일 공격 + Vulnerable (A17+: Weak 추가)
4. **AI 패턴**: 40% TAIL, 60% BITE/TAIL (lastTwoMoves 체크)
5. **Snecko Eye**: 유물과 동일한 코스트 랜덤화 효과
6. **데미지 인덱스**: damage.get(0) = Bite, damage.get(1) = Tail
7. **사망 음성**: "SNECKO_DEATH" 효과음
