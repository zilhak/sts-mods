# 첨탑 성장체 (Spire Growth)

## 기본 정보

**클래스명**: `SpireGrowth`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.SpireGrowth`
**ID**: `"Serpent"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (The Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|---------|
| 기본 (A0-A6) | 170 |
| A7+ | 190 |

**코드 위치**: 27-28줄, 41-45줄

```java
private static final int START_HP = 170;
private static final int A_2_START_HP = 190;

if (AbstractDungeon.ascensionLevel >= 7) {
    setHp(190);
} else {
    setHp(170);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public SpireGrowth()
```

**파라미터**: 없음

**특징**:
- 크기: 480.0F x 430.0F (매우 큼)
- 위치: 0.0F, 10.0F
- 히트박스 오프셋: -10.0F, -35.0F

**애니메이션 설정**:
```java
loadAnimation("images/monsters/theForest/spireGrowth/skeleton.atlas",
              "images/monsters/theForest/spireGrowth/skeleton.json", 1.0F);

AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
e.setTime(e.getEndTime() * MathUtils.random());
e.setTimeScale(1.3F);
```

---

## 패턴 정보

### 패턴 1: 빠른 돌진 (Quick Tackle)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 16 |
| A2+ | 18 |

**코드 위치**: 29-30줄, 47-53줄, 61줄, 67-72줄

```java
// 상수 정의
private int tackleDmg = 16, smashDmg = 22, constrictDmg = 10;
private int A_2_tackleDmg = 18; private int A_2_smashDmg = 25;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.tackleDmgActual = this.A_2_tackleDmg;
    this.smashDmgActual = this.A_2_smashDmg;
} else {
    this.tackleDmgActual = this.tackleDmg;
    this.smashDmgActual = this.smashDmg;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.tackleDmgActual));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateFastAttackAction((AbstractCreature)this)
    );
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
- 빠른 공격 애니메이션
- BLUNT_HEAVY 효과

**수정 포인트**:
- 데미지 변경: `tackleDmg`, `A_2_tackleDmg` 필드
- 애니메이션 속도 변경 가능

---

### 패턴 2: 조임 (Constrict)

**바이트 값**: `2`
**의도**: `STRONG_DEBUFF`
**발동 조건**: AI 로직에 따름

**효과**:
- 플레이어에게 **Constricted** 파워 부여

**Constricted 수치**:
| 난이도 | Constricted |
|--------|------------|
| 기본 (A0-A16) | 10 |
| A17+ | 12 |

**코드 위치**: 29줄, 73-83줄

```java
// 상수 정의
private int constrictDmg = 10;

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)AbstractDungeon.player,
                (AbstractCreature)this,
                new ConstrictedPower(
                    (AbstractCreature)AbstractDungeon.player,
                    (AbstractCreature)this,
                    this.constrictDmg + 2
                )
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)AbstractDungeon.player,
                (AbstractCreature)this,
                new ConstrictedPower(
                    (AbstractCreature)AbstractDungeon.player,
                    (AbstractCreature)this,
                    this.constrictDmg
                )
            )
        );
    }
    break;
```

**Constricted 효과**:
- 매 턴 종료 시 HP 손실
- 플레이어가 카드를 사용할 때마다 HP 손실

**특징**:
- A17+ 난이도에서 Constricted 12 적용
- 느린 공격 애니메이션

**수정 포인트**:
- Constricted 수치 변경: `constrictDmg` 필드
- A17+ 추가 수치 변경: `constrictDmg + 2`

---

### 패턴 3: 강타 (Smash)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 22 |
| A2+ | 25 |

**코드 위치**: 29-30줄, 47-53줄, 62줄, 89-94줄

```java
// 상수 정의
private int smashDmg = 22;
private int A_2_smashDmg = 25;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.smashDmgActual = this.A_2_smashDmg;
} else {
    this.smashDmgActual = this.smashDmg;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.smashDmgActual));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(
        new WaitAction(0.4F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(1),
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    break;
```

**특징**:
- 상태 변경 (ChangeStateAction)
- 0.4초 대기 후 공격
- BLUNT_HEAVY 효과

**애니메이션 시스템**:
```java
public void changeState(String key) {
    switch (key) {
        case "ATTACK":
            this.state.setAnimation(0, "Attack", false);
            this.state.setTimeScale(1.3F);
            this.state.addAnimation(0, "Idle", true, 0.0F);
            break;
    }
}
```

**수정 포인트**:
- 데미지 변경: `smashDmg`, `A_2_smashDmg` 필드
- 대기 시간 변경: `WaitAction` 수정

---

## AI 로직 (getMove)

**코드 위치**: 101-124줄

```java
protected void getMove(int num) {
    // A17+ 전용: Constricted 없고 직전에 Constrict 안 했으면 우선 사용
    if (AbstractDungeon.ascensionLevel >= 17 &&
        !AbstractDungeon.player.hasPower("Constricted") &&
        !lastMove((byte)2)) {
        setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
        return;
    }

    // 50% 확률, 직전 2턴에 Quick Tackle 안 했으면
    if (num < 50 && !lastTwoMoves((byte)1)) {
        setMove((byte)1, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(0)).base);
        return;
    }

    // Constricted 없고 직전에 Constrict 안 했으면
    if (!AbstractDungeon.player.hasPower("Constricted") &&
        !lastMove((byte)2)) {
        setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
        return;
    }

    // 직전 2턴에 Smash 안 했으면
    if (!lastTwoMoves((byte)3)) {
        setMove((byte)3, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(1)).base);
        return;
    }

    // 기본: Quick Tackle
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        ((DamageInfo)this.damage.get(0)).base);
}
```

### AI 우선순위

1. **A17+ 최우선**: 플레이어에게 Constricted가 없고, 직전에 Constrict 안 했으면 → **Constrict**

2. **50% 확률**: `num < 50` && 직전 2턴에 Quick Tackle 안 했으면 → **Quick Tackle**

3. **조건부**: 플레이어에게 Constricted가 없고, 직전에 Constrict 안 했으면 → **Constrict**

4. **조건부**: 직전 2턴에 Smash 안 했으면 → **Smash**

5. **기본**: → **Quick Tackle**

**특징**:
- A17+에서는 Constrict를 우선 사용
- Constricted 상태를 계속 유지하려고 함
- Quick Tackle과 Smash를 번갈아 사용하려는 패턴

**수정 포인트**:
- A17+ 우선순위 제거/변경
- 확률 조정 (num < 50)
- 패턴 순서 변경

---

## 특수 동작

### 데미지 애니메이션

**코드 위치**: 129-136줄

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hurt", false);
        this.state.setTimeScale(1.3F);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**특징**:
- 데미지를 받으면 "Hurt" 애니메이션 재생
- 가시 데미지는 애니메이션 없음

---

## 수정 예시

### 1. Constricted 수치 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.SpireGrowth",
    method = SpirePatch.CONSTRUCTOR
)
public static class SpireGrowthConstrictPatch {
    @SpirePostfixPatch
    public static void Postfix(SpireGrowth __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // constrictDmg 필드 접근 필요
            // Reflection으로 필드 변경
            try {
                Field constrictField = SpireGrowth.class.getDeclaredField("constrictDmg");
                constrictField.setAccessible(true);
                constrictField.setInt(__instance, 15);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 2. 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.SpireGrowth",
    method = SpirePatch.CONSTRUCTOR
)
public static class SpireGrowthDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(SpireGrowth __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Quick Tackle 데미지 증가 (damage.get(0))
            if (__instance.damage.size() > 0) {
                __instance.damage.get(0).base += 3;
            }
            // Smash 데미지 증가 (damage.get(1))
            if (__instance.damage.size() > 1) {
                __instance.damage.get(1).base += 5;
            }
        }
    }
}
```

### 3. A17+ Constrict 우선순위 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.SpireGrowth",
    method = "getMove"
)
public static class SpireGrowthAIPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(SpireGrowth __instance, int num) {
        // A17+ Constrict 우선순위를 제거하고 일반 AI 로직만 사용
        if (AbstractDungeon.ascensionLevel >= 25) {
            // 커스텀 AI 로직 구현
            // SpireReturn.Return()으로 원본 메서드 스킵
        }
        return SpireReturn.Continue();
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `tackleDmg` | int | Quick Tackle 기본 데미지 (16) |
| `A_2_tackleDmg` | int | A2+ Quick Tackle 데미지 (18) |
| `smashDmg` | int | Smash 기본 데미지 (22) |
| `A_2_smashDmg` | int | A2+ Smash 데미지 (25) |
| `constrictDmg` | int | Constrict 수치 (10) |
| `tackleDmgActual` | int | 실제 사용할 Quick Tackle 데미지 |
| `smashDmgActual` | int | 실제 사용할 Smash 데미지 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/SpireGrowth.java`
- **파워**: `com.megacrit.cardcrawl.powers.ConstrictedPower`
- **액션**:
  - `AnimateFastAttackAction`
  - `AnimateSlowAttackAction`
  - `DamageAction`
  - `ApplyPowerAction`
  - `ChangeStateAction`
  - `WaitAction`
  - `RollMoveAction`

---

## 참고사항

1. **ID 주의**: 클래스명은 `SpireGrowth`지만 ID는 `"Serpent"`
2. **A17+ 특수 AI**: Constricted를 우선적으로 부여하려는 로직
3. **큰 히트박스**: 480x430으로 매우 큰 크기
4. **애니메이션 속도**: 모든 애니메이션이 1.3배속
5. **상태 변경**: Smash 사용 시 ChangeStateAction 사용
