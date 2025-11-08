# 다클링 (Darkling)

## 기본 정보

**클래스명**: `Darkling`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Darkling`
**ID**: `"Darkling"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (Beyond)

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 48-56 |
| A7+ | 50-59 |

**코드 위치**: 62-66줄

```java
if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(50, 59);
} else {
    setHp(48, 56);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Darkling(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표 + 20.0F (조정됨)

**특징**:
- 히트박스: 0.0F, -20.0F, 260.0F, 200.0F
- 애니메이션: 랜덤 시작 시간과 타임스케일

---

## 패턴 정보

### 패턴 1: 할퀴기 (Chomp)

**바이트 값**: `1`
**의도**: `ATTACK` (2회 공격)
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 8 x 2 |
| A2+ | 9 x 2 |

**코드 위치**: 48-49줄, 68-74줄, 77줄, 91-97줄

```java
// 데미지 상수
private static final int BITE_DMG = 8;
private static final int A_2_BITE_DMG = 9;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.chompDmg = 9;
} else {
    this.chompDmg = 8;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.chompDmg));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    // 두 번째 공격
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(0),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    break;
```

**특징**:
- 2회 연속 공격
- ATTACK 애니메이션 상태 전환

**수정 포인트**:
- 데미지 변경: `chompDmg` 필드 또는 생성자 로직
- 공격 횟수 변경: DamageAction 추가/제거

---

### 패턴 2: 단단해지기 (Harden)

**바이트 값**: `2`
**의도**: `DEFEND` (A0-A16) 또는 `DEFEND_BUFF` (A17+)
**발동 조건**: AI 로직에 따름

**효과**:
- 자신에게 **12 방어도** 획득
- A17+: 자신에게 **힘 2** 부여

**코드 위치**: 80줄, 99-103줄

```java
// 상수
private static final int BLOCK_AMT = 12;
private static final int CHOMP_AMT = 2;

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new GainBlockAction((AbstractCreature)this, (AbstractCreature)this, 12)
    );
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this, (AbstractCreature)this,
                new StrengthPower((AbstractCreature)this, 2), 2
            )
        );
    }
    break;
```

**특징**:
- A17+에서 힘 버프 추가
- 의도 표시가 Ascension에 따라 변경됨

**수정 포인트**:
- 방어도 변경: `BLOCK_AMT` 상수 수정
- 힘 수치 변경: `CHOMP_AMT` 상수 수정
- Ascension 조건 변경

---

### 패턴 3: 할퀴기 (Nip)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 7-11 (랜덤) |
| A2+ | 9-13 (랜덤) |

**코드 위치**: 68-74줄, 78줄, 106-109줄

```java
// 생성자에서 랜덤 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.nipDmg = AbstractDungeon.monsterHpRng.random(9, 13);
} else {
    this.nipDmg = AbstractDungeon.monsterHpRng.random(7, 11);
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.nipDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.BLUNT_LIGHT
        )
    );
    break;
```

**특징**:
- 데미지가 생성 시 랜덤하게 결정됨
- 빠른 공격 애니메이션

**수정 포인트**:
- 데미지 범위 변경: 생성자의 `random()` 호출 수정

---

### 패턴 4: 카운트다운 (Count)

**바이트 값**: `4`
**의도**: `UNKNOWN`
**발동 조건**: halfDead 상태이고 다른 Darkling이 아직 살아있을 때

**효과**:
- 플레이어 위에 텍스트 표시 (DIALOG[0])
- 실제 행동 없음

**코드 위치**: 111-112줄, 219-223줄

```java
// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new TextAboveCreatureAction((AbstractCreature)this, DIALOG[0])
    );
    break;

// damage() 메서드에서 설정
if (!allDead) {
    if (this.nextMove != 4) {
        setMove((byte)4, AbstractMonster.Intent.UNKNOWN);
        createIntent();
        AbstractDungeon.actionManager.addToBottom(
            new SetMoveAction(this, (byte)4, AbstractMonster.Intent.UNKNOWN)
        );
    }
}
```

**특징**:
- halfDead 상태에서만 발동
- 다른 Darkling의 부활을 기다림

---

### 패턴 5: 부활 (Reincarnate)

**바이트 값**: `5`
**의도**: `BUFF`
**발동 조건**: getMove()에서 halfDead = true일 때

**효과**:
- 최대 HP의 50% 회복
- Regrow 파워 재부여
- REVIVE 상태로 전환
- 유물 onSpawnMonster 트리거

**코드 위치**: 114-127줄, 135-138줄

```java
// takeTurn 실행
case 5:
    // 랜덤 사운드
    if (MathUtils.randomBoolean()) {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("DARKLING_REGROW_2", MathUtils.random(-0.1F, 0.1F))
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new SFXAction("DARKLING_REGROW_1", MathUtils.random(-0.1F, 0.1F))
        );
    }
    // 회복
    AbstractDungeon.actionManager.addToBottom(
        new HealAction((AbstractCreature)this, (AbstractCreature)this, this.maxHealth / 2)
    );
    // 상태 변경
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "REVIVE")
    );
    // Regrow 파워 재부여
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new RegrowPower((AbstractCreature)this), 1
        )
    );
    // 유물 트리거
    for (AbstractRelic r : AbstractDungeon.player.relics) {
        r.onSpawnMonster(this);
    }
    break;

// getMove()에서 설정
if (this.halfDead) {
    setMove((byte)5, AbstractMonster.Intent.BUFF);
    return;
}
```

**특징**:
- 최대 HP의 절반 회복
- 부활 사운드 효과 (2가지 중 랜덤)
- 유물 효과 트리거

---

## 특수 동작

### Regrow 메커니즘 (usePreBattleAction & damage)

**코드 위치**: 82-85줄, 197-235줄

#### usePreBattleAction
```java
public void usePreBattleAction() {
    (AbstractDungeon.getCurrRoom()).cannotLose = true;
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new RegrowPower((AbstractCreature)this)
        )
    );
}
```

**특징**:
- 전투 시작 시 `cannotLose = true` 설정
- Regrow 파워 부여

#### damage() 메서드 오버라이드

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (this.currentHealth <= 0 && !this.halfDead) {
        this.halfDead = true;

        // 파워 onDeath 트리거
        for (AbstractPower p : this.powers) {
            p.onDeath();
        }

        // 유물 onMonsterDeath 트리거
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onMonsterDeath(this);
        }

        // 파워 제거
        this.powers.clear();

        logger.info("This monster is now half dead.");

        // 다른 Darkling 확인
        boolean allDead = true;
        for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
            if (m.id.equals("Darkling") && !m.halfDead) {
                allDead = false;
            }
        }

        logger.info("All dead: " + allDead);

        // 모두 halfDead가 아니면 카운트다운
        if (!allDead) {
            if (this.nextMove != 4) {
                setMove((byte)4, AbstractMonster.Intent.UNKNOWN);
                createIntent();
                AbstractDungeon.actionManager.addToBottom(
                    new SetMoveAction(this, (byte)4, AbstractMonster.Intent.UNKNOWN)
                );
            }
        } else {
            // 모두 halfDead면 진짜 사망
            (AbstractDungeon.getCurrRoom()).cannotLose = false;
            this.halfDead = false;
            for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                m.die();
            }
        }
    } else if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
        // 히트 애니메이션
        this.state.setAnimation(0, "Hit", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**부활 메커니즘**:
1. HP가 0이 되면 `halfDead = true`
2. 파워와 유물 효과 트리거
3. 다른 Darkling이 살아있으면:
   - `nextMove = 4` (카운트다운)
   - 다음 턴에 부활 (패턴 5)
4. 모든 Darkling이 halfDead면:
   - `cannotLose = false`
   - 모두 진짜 사망

---

### die() 메서드

**코드 위치**: 238-241줄

```java
public void die() {
    if (!(AbstractDungeon.getCurrRoom()).cannotLose)
        super.die();
}
```

**특징**:
- `cannotLose`가 false일 때만 진짜 사망
- 부활 중에는 die()가 호출되어도 무시됨

---

## AI 로직 (getMove)

**매우 복잡한 확률 기반 AI**

**코드 위치**: 134-178줄

### halfDead 상태
```java
if (this.halfDead) {
    setMove((byte)5, AbstractMonster.Intent.BUFF);
    return;
}
```
무조건 **부활 (패턴 5)** 사용

### 첫 번째 턴
```java
if (this.firstMove) {
    if (num < 50) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            setMove((byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        } else {
            setMove((byte)2, AbstractMonster.Intent.DEFEND);
        }
    } else {
        setMove((byte)3, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(1)).base);
    }

    this.firstMove = false;
    return;
}
```

**로직**:
- 50% 확률: Harden (패턴 2)
- 50% 확률: Nip (패턴 3)

### 이후 턴 (num < 40, 40% 확률)
```java
if (num < 40) {
    if (!lastMove((byte)1) &&
        (AbstractDungeon.getMonsters()).monsters.lastIndexOf(this) % 2 == 0) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base, 2, true);
    } else {
        getMove(AbstractDungeon.aiRng.random(40, 99));
    }
}
```

**로직**:
- 이전 패턴이 Chomp(1)가 아니고
- 몬스터 인덱스가 짝수이면 → Chomp (패턴 1)
- 그 외 → 재귀 호출 (40-99 범위)

### 이후 턴 (num 40-69, 30% 확률)
```java
else if (num < 70) {
    if (!lastMove((byte)2)) {
        if (AbstractDungeon.ascensionLevel >= 17) {
            setMove((byte)2, AbstractMonster.Intent.DEFEND_BUFF);
        } else {
            setMove((byte)2, AbstractMonster.Intent.DEFEND);
        }
    } else {
        setMove((byte)3, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(1)).base);
    }
}
```

**로직**:
- 이전 패턴이 Harden(2)가 아니면 → Harden (패턴 2)
- 그 외 → Nip (패턴 3)

### 이후 턴 (num 70-99, 30% 확률)
```java
else if (!lastTwoMoves((byte)3)) {
    setMove((byte)3, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(1)).base);
} else {
    getMove(AbstractDungeon.aiRng.random(0, 99));
}
```

**로직**:
- 이전 2턴이 모두 Nip(3)이 아니면 → Nip (패턴 3)
- 그 외 → 재귀 호출 (0-99 범위)

**수정 포인트**:
- 패턴 순서 변경
- 확률 변경
- 첫 턴 패턴 변경
- 몬스터 인덱스 조건 제거

---

## 수정 예시

### 1. 부활 회복량 감소 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = "takeTurn"
)
public static class DarklingHealPatch {
    @SpireInsertPatch(
        locator = HealLocator.class
    )
    public static void Insert(Darkling __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 회복량을 maxHealth / 3으로 감소
            int reducedHeal = __instance.maxHealth / 3;
            // HealAction의 amount를 수정하려면 별도 패치 필요
        }
    }

    private static class HealLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                HealAction.class, "<init>");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 2. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = SpirePatch.CONSTRUCTOR
)
public static class DarklingDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Darkling __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Chomp 데미지 증가 (damage.get(0))
            if (__instance.damage.size() > 0) {
                __instance.damage.get(0).base += 2;
            }
            // Nip 데미지 증가 (damage.get(1))
            if (__instance.damage.size() > 1) {
                __instance.damage.get(1).base += 2;
            }
        }
    }
}
```

### 3. 부활 횟수 제한

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = SpirePatch.CLASS
)
public static class ReviveCountField {
    public static SpireField<Integer> reviveCount = new SpireField<>(() -> 0);
}

@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Darkling",
    method = "damage"
)
public static class LimitRevivePatch {
    @SpireInsertPatch(
        locator = HalfDeadLocator.class
    )
    public static SpireReturn<Void> Insert(Darkling __instance, DamageInfo info) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            int reviveCount = ReviveCountField.reviveCount.get(__instance);
            if (reviveCount >= 1) {
                // 부활 1회만 허용
                __instance.halfDead = false;
                (AbstractDungeon.getCurrRoom()).cannotLose = false;
                __instance.die();
                return SpireReturn.Return(null);
            }
            ReviveCountField.reviveCount.set(__instance, reviveCount + 1);
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `firstMove` | boolean | 첫 턴 여부 |
| `chompDmg` | int | Chomp 데미지 |
| `nipDmg` | int | Nip 데미지 (랜덤) |
| `halfDead` | boolean | 부활 대기 상태 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Darkling.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.RegrowPower` (부활 능력)
  - `com.megacrit.cardcrawl.powers.StrengthPower` (A17+ Harden)
- **액션**:
  - `ChangeStateAction` (애니메이션 상태 변경)
  - `DamageAction` (데미지)
  - `GainBlockAction` (방어도)
  - `ApplyPowerAction` (파워 부여)
  - `HealAction` (회복)
  - `AnimateFastAttackAction` (빠른 공격 애니메이션)
  - `TextAboveCreatureAction` (텍스트 표시)
  - `SetMoveAction` (패턴 변경)
  - `SFXAction` (사운드 효과)
  - `WaitAction` (대기)

---

## 참고사항

1. **부활 메커니즘**: 모든 Darkling이 halfDead가 되어야 진짜 사망
2. **cannotLose**: 전투 시작 시 true, 모두 halfDead가 되면 false
3. **인덱스 기반 AI**: 몬스터 인덱스(짝수/홀수)에 따라 패턴이 달라짐
4. **멀티 인카운터**: 보통 3마리가 함께 등장
5. **파워 초기화**: halfDead가 되면 모든 파워가 제거됨
6. **랜덤 데미지**: Nip의 데미지는 생성 시 결정되어 고정됨
7. **부활 회복**: 최대 HP의 50% 회복
8. **유물 트리거**: 부활 시 onSpawnMonster 트리거
