# 뒤틀린 덩어리 (Writhing Mass)

## 기본 정보

**클래스명**: `WrithingMass`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.WrithingMass`
**ID**: `"WrithingMass"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|---------|
| 기본 (A0-A6) | 160 |
| A7+ | 175 |

**코드 위치**: 33-35줄, 51-55줄

```java
private static final int HP = 160;
private static final int A_2_HP = 175;

if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(175);
} else {
    setHp(160);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public WrithingMass()
```

**파라미터**: 없음

**특징**:
- 크기: 450.0F x 310.0F
- 위치: 0.0F, 15.0F
- 히트박스 오프셋: 5.0F, -26.0F

**애니메이션 설정**:
```java
loadAnimation("images/monsters/theForest/spaghetti/skeleton.atlas",
              "images/monsters/theForest/spaghetti/skeleton.json", 1.0F);

AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
e.setTime(e.getEndTime() * MathUtils.random());
this.stateData.setMix("Hit", "Idle", 0.1F);
```

---

## 특수 파워 (Pre-Battle)

### usePreBattleAction

**코드 위치**: 73-76줄

```java
public void usePreBattleAction() {
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
            new ReactivePower((AbstractCreature)this))
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction((AbstractCreature)this, (AbstractCreature)this,
            new MalleablePower((AbstractCreature)this))
    );
}
```

**시작 파워**:
1. **Reactive**: 공격받을 때 마다 다음 턴 힘 증가
2. **Malleable**: 공격받을 때 방어도 획득

**특징**:
- 전투 시작 전 자동으로 두 파워 부여
- 방어적인 성향의 몬스터

---

## 패턴 정보

### 패턴 0: 큰 공격 (Big Hit)

**바이트 값**: `0`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름 (10% 확률)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 32 |
| A2+ | 38 |

**코드 위치**: 58-64줄, 81-86줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.damage.add(new DamageInfo((AbstractCreature)this, 38));
} else {
    this.damage.add(new DamageInfo((AbstractCreature)this, 32));
}

// takeTurn 실행
case 0:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.4F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.SLASH_HEAVY
        )
    );
    break;
```

**특징**:
- 가장 강력한 공격
- 상태 변경 후 0.4초 대기
- SLASH_HEAVY 효과

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.add` 수정
- 발동 확률 변경: `getMove()` 메서드

---

### 패턴 1: 다중 공격 (Multi Hit)

**바이트 값**: `1`
**의도**: `ATTACK` (다중)
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 | 횟수 |
|--------|--------|------|
| 기본 (A0-A1) | 7 | 3 |
| A2+ | 9 | 3 |

**코드 위치**: 59-60줄, 65-66줄, 38줄, 87-93줄

```java
// 상수 정의
private static final int HIT_COUNT = 3;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.damage.add(new DamageInfo((AbstractCreature)this, 9));
} else {
    this.damage.add(new DamageInfo((AbstractCreature)this, 7));
}

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    for (int i = 0; i < 3; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                (AbstractCreature)AbstractDungeon.player,
                this.damage.get(1),
                AbstractGameAction.AttackEffect.BLUNT_LIGHT
            )
        );
    }
    break;
```

**특징**:
- 3회 연속 공격
- BLUNT_LIGHT 효과
- 총 데미지: 21 (A0-A1) 또는 27 (A2+)

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.add` 수정
- 공격 횟수 변경: `HIT_COUNT` 상수 수정

---

### 패턴 2: 공격 + 방어 (Attack Block)

**바이트 값**: `2`
**의도**: `ATTACK_DEFEND`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 데미지
- 자신에게 데미지만큼 방어도 획득

**데미지 & 방어도**:
| 난이도 | 데미지 | 방어도 |
|--------|--------|--------|
| 기본 (A0-A1) | 15 | 15 |
| A2+ | 16 | 16 |

**코드 위치**: 60-61줄, 66-67줄, 94-99줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.damage.add(new DamageInfo((AbstractCreature)this, 16));
} else {
    this.damage.add(new DamageInfo((AbstractCreature)this, 15));
}

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(2),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction(
            (AbstractCreature)this,
            (AbstractCreature)this,
            ((DamageInfo)this.damage.get(2)).base
        )
    );
    break;
```

**특징**:
- 공격과 방어를 동시에 수행
- 방어도는 데미지와 동일한 수치
- BLUNT_HEAVY 효과

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.add` 수정
- 방어도는 자동으로 데미지와 동일하게 설정됨

---

### 패턴 3: 공격 + 디버프 (Attack Debuff)

**바이트 값**: `3`
**의도**: `ATTACK_DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 데미지
- 플레이어에게 **약화(Weak)** 부여
- 플레이어에게 **취약(Vulnerable)** 부여

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 10 |
| A2+ | 12 |

**디버프 수치**:
| 난이도 | 약화 & 취약 |
|--------|------------|
| 모든 난이도 | 2 |

**코드 위치**: 61-62줄, 67-68줄, 62줄, 68줄, 100-116줄

```java
// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.damage.add(new DamageInfo((AbstractCreature)this, 12));
    this.normalDebuffAmt = 2;
} else {
    this.damage.add(new DamageInfo((AbstractCreature)this, 10));
    this.normalDebuffAmt = 2;
}

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(3),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new WeakPower(
                (AbstractCreature)AbstractDungeon.player,
                this.normalDebuffAmt, true
            ),
            this.normalDebuffAmt
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player,
            (AbstractCreature)this,
            new VulnerablePower(
                (AbstractCreature)AbstractDungeon.player,
                this.normalDebuffAmt, true
            ),
            this.normalDebuffAmt
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    break;
```

**특징**:
- 약화와 취약을 동시에 부여
- 데미지 후 디버프 적용

**수정 포인트**:
- 데미지 변경: 생성자의 `damage.add` 수정
- 디버프 수치 변경: `normalDebuffAmt` 필드

---

### 패턴 4: 메가 디버프 (Mega Debuff)

**바이트 값**: `4`
**의도**: `STRONG_DEBUFF`
**발동 조건**: 한 번만 사용 가능, AI 로직에 따름

**효과**:
- 플레이어 덱에 **Parasite** 카드 추가

**코드 위치**: 37줄, 117-123줄

```java
// 필드 정의
private boolean usedMegaDebuff = false;

// takeTurn 실행
case 4:
    this.usedMegaDebuff = true;
    AbstractDungeon.actionManager.addToBottom(
        new FastShakeAction((AbstractCreature)this, 0.5F, 0.2F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new AddCardToDeckAction(
            CardLibrary.getCard("Parasite").makeCopy()
        )
    );
    break;
```

**Parasite 카드**:
- 저주 카드
- 유형: 상태이상
- 플레이어 덱에 영구적으로 추가됨

**특징**:
- 한 번만 사용 가능 (`usedMegaDebuff` 플래그)
- 화면 흔들림 효과
- 매우 강력한 디버프

**수정 포인트**:
- 사용 횟수 제한 변경: `usedMegaDebuff` 플래그
- 추가할 카드 변경: `CardLibrary.getCard()` 수정

---

## AI 로직 (getMove)

**코드 위치**: 153-210줄

```java
protected void getMove(int num) {
    // 첫 번째 턴: 랜덤 패턴 (Multi Hit, Attack Block, Attack Debuff 중 선택)
    if (this.firstMove) {
        this.firstMove = false;
        if (num < 33) {
            setMove((byte)1, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(1)).base, 3, true);
        } else if (num < 66) {
            setMove((byte)2, AbstractMonster.Intent.ATTACK_DEFEND,
                ((DamageInfo)this.damage.get(2)).base);
        } else {
            setMove((byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(3)).base);
        }
        return;
    }

    // num < 10 (10% 확률): Big Hit
    if (num < 10) {
        if (!lastMove((byte)0)) {
            setMove((byte)0, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base);
        } else {
            getMove(AbstractDungeon.aiRng.random(10, 99));
        }
    }
    // num 10-19 (10% 확률): Mega Debuff 또는 재시도
    else if (num < 20) {
        if (!this.usedMegaDebuff && !lastMove((byte)4)) {
            setMove((byte)4, AbstractMonster.Intent.STRONG_DEBUFF);
        }
        else if (AbstractDungeon.aiRng.randomBoolean(0.1F)) {
            setMove((byte)0, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(0)).base);
        } else {
            getMove(AbstractDungeon.aiRng.random(20, 99));
        }
    }
    // num 20-39 (20% 확률): Attack Debuff
    else if (num < 40) {
        if (!lastMove((byte)3)) {
            setMove((byte)3, AbstractMonster.Intent.ATTACK_DEBUFF,
                ((DamageInfo)this.damage.get(3)).base);
        }
        else if (AbstractDungeon.aiRng.randomBoolean(0.4F)) {
            getMove(AbstractDungeon.aiRng.random(19));
        } else {
            getMove(AbstractDungeon.aiRng.random(40, 99));
        }
    }
    // num 40-69 (30% 확률): Multi Hit
    else if (num < 70) {
        if (!lastMove((byte)1)) {
            setMove((byte)1, AbstractMonster.Intent.ATTACK,
                ((DamageInfo)this.damage.get(1)).base, 3, true);
        }
        else if (AbstractDungeon.aiRng.randomBoolean(0.3F)) {
            setMove((byte)2, AbstractMonster.Intent.ATTACK_DEFEND,
                ((DamageInfo)this.damage.get(2)).base);
        } else {
            getMove(AbstractDungeon.aiRng.random(39));
        }
    }
    // num 70-99 (30% 확률): Attack Block
    else {
        if (!lastMove((byte)2)) {
            setMove((byte)2, AbstractMonster.Intent.ATTACK_DEFEND,
                ((DamageInfo)this.damage.get(2)).base);
        } else {
            getMove(AbstractDungeon.aiRng.random(69));
        }
    }

    createIntent();
}
```

### AI 우선순위 (첫 턴)

첫 번째 턴에서 랜덤으로 선택:
- **33% 확률**: Multi Hit (패턴 1)
- **33% 확률**: Attack Block (패턴 2)
- **34% 확률**: Attack Debuff (패턴 3)

### AI 우선순위 (이후 턴)

**확률 구간별 분석**:

1. **0-9 (10%)**: Big Hit
   - 직전에 Big Hit 안 했으면 → Big Hit
   - 했으면 → 10-99 범위에서 재시도

2. **10-19 (10%)**: Mega Debuff
   - Mega Debuff 아직 안 썼고 직전에 안 했으면 → Mega Debuff
   - 조건 불충족 시:
     - 10% 확률 → Big Hit
     - 90% 확률 → 20-99 범위에서 재시도

3. **20-39 (20%)**: Attack Debuff
   - 직전에 Attack Debuff 안 했으면 → Attack Debuff
   - 했으면:
     - 40% 확률 → 0-19 범위에서 재시도
     - 60% 확률 → 40-99 범위에서 재시도

4. **40-69 (30%)**: Multi Hit
   - 직전에 Multi Hit 안 했으면 → Multi Hit
   - 했으면:
     - 30% 확률 → Attack Block
     - 70% 확률 → 0-39 범위에서 재시도

5. **70-99 (30%)**: Attack Block
   - 직전에 Attack Block 안 했으면 → Attack Block
   - 했으면 → 0-69 범위에서 재시도

**특징**:
- 매우 복잡한 재귀적 AI
- 같은 패턴 연속 사용 방지
- Mega Debuff는 한 번만 사용
- 확률적 균형 유지

**수정 포인트**:
- 확률 구간 변경 (num 비교 값)
- 재시도 로직 변경
- Mega Debuff 사용 제한 변경

---

## 특수 동작

### 데미지 애니메이션

**코드 위치**: 131-138줄

```java
public void damage(DamageInfo info) {
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
    super.damage(info);
}
```

**특징**:
- 데미지를 받으면 "Hit" 애니메이션 재생
- 가시 데미지는 애니메이션 없음

### 공격 애니메이션

**코드 위치**: 141-148줄

```java
public void changeState(String key) {
    switch (key) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

---

## 수정 예시

### 1. Reactive/Malleable 파워 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "usePreBattleAction"
)
public static class WrithingMassPowerPatch {
    @SpirePostfixPatch
    public static void Postfix(WrithingMass __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 추가 파워 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new StrengthPower(__instance, 2), 2)
            );
        }
    }
}
```

### 2. Multi Hit 횟수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "takeTurn"
)
public static class WrithingMassMultiHitPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(WrithingMass __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 &&
            __instance.nextMove == 1) {
            // 5회 공격으로 변경
            AbstractDungeon.actionManager.addToBottom(
                new AnimateSlowAttackAction(__instance)
            );
            for (int i = 0; i < 5; i++) {
                AbstractDungeon.actionManager.addToBottom(
                    new DamageAction(AbstractDungeon.player,
                        __instance.damage.get(1),
                        AbstractGameAction.AttackEffect.BLUNT_LIGHT)
                );
            }
            AbstractDungeon.actionManager.addToBottom(
                new RollMoveAction(__instance)
            );
            return SpireReturn.Return();
        }
        return SpireReturn.Continue();
    }
}
```

### 3. Parasite 추가 카드 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.WrithingMass",
    method = "takeTurn"
)
public static class WrithingMassParasitePatch {
    @SpirePostfixPatch
    public static void Postfix(WrithingMass __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 &&
            __instance.nextMove == 4) {
            // Parasite 추가로 한 장 더
            AbstractDungeon.actionManager.addToBottom(
                new AddCardToDeckAction(
                    CardLibrary.getCard("Parasite").makeCopy()
                )
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `usedMegaDebuff` | boolean | Mega Debuff 사용 여부 |
| `normalDebuffAmt` | int | 약화/취약 수치 (2) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/WrithingMass.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.ReactivePower`
  - `com.megacrit.cardcrawl.powers.MalleablePower`
  - `com.megacrit.cardcrawl.powers.WeakPower`
  - `com.megacrit.cardcrawl.powers.VulnerablePower`
- **카드**: `com.megacrit.cardcrawl.cards.status.Parasite`
- **액션**:
  - `AnimateFastAttackAction`
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `GainBlockAction`
  - `ChangeStateAction`
  - `AddCardToDeckAction`
  - `FastShakeAction`
  - `RollMoveAction`

---

## 참고사항

1. **방어적 파워**: Reactive와 Malleable로 공격받을 때마다 강화
2. **복잡한 AI**: 재귀적 확률 기반 AI로 예측 어려움
3. **Mega Debuff**: 한 번만 사용 가능, Parasite 카드 추가
4. **첫 턴 랜덤**: 첫 턴부터 3가지 중 랜덤 선택
5. **화면 흔들림**: Mega Debuff 사용 시 시각 효과
