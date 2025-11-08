# 유령 (Transient)

## 기본 정보

**클래스명**: `Transient`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Transient`
**ID**: `"Transient"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|----|
| 모든 난이도 | 999 (고정) |

**코드 위치**: 28줄, 37줄

```java
private static final int HP = 999;

public Transient() {
    super(NAME, "Transient", 999, 0.0F, -15.0F, 370.0F, 340.0F, null, 0.0F, 20.0F);
}
```

**특징**:
- 사실상 무한 HP (Fading 파워로 자동 사망)
- 직접 HP를 0으로 만들면 업적 해금

---

## 생성자 정보

### 주요 생성자
```java
public Transient()
```

**파라미터**: 없음 (고정 위치)

**특징**:
- 히트박스: 0.0F, -15.0F, 370.0F, 340.0F
- 애니메이션: 랜덤 시작 시간
- 금화: 1골드 (최소값)

---

## 패턴 정보

### 패턴 1: 공격 (Attack)

**바이트 값**: `1`
**의도**: `ATTACK`
**발동 조건**: 유일한 패턴 (항상 사용)

**데미지**:
| 난이도 | 초기 데미지 | 증가량 |
|--------|------------|--------|
| 기본 (A0-A1) | 30 | +10/턴 |
| A2+ | 40 | +10/턴 |

**데미지 증가 계산**:
- 턴 N: 초기 데미지 + (N * 10)
- 1턴: 30 (또는 40)
- 2턴: 40 (또는 50)
- 3턴: 50 (또는 60)
- 4턴: 60 (또는 70)
- ...

**코드 위치**: 30-33줄, 50-63줄, 78-86줄, 118줄

```java
// 데미지 상수
private static final int DEATH_DMG = 30;
private static final int INCREMENT_DMG = 10;
private static final int A_2_DEATH_DMG = 40;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.startingDeathDmg = 40;
} else {
    this.startingDeathDmg = 30;
}

// 7개의 데미지 인덱스 미리 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 10));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 20));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 30));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 40));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 50));
this.damage.add(new DamageInfo((AbstractCreature)this, this.startingDeathDmg + 60));

// takeTurn 실행
case 1:
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "ATTACK")
    );
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(
            (AbstractCreature)AbstractDungeon.player,
            this.damage.get(this.count),  // count번째 데미지
            AbstractGameAction.AttackEffect.BLUNT_HEAVY
        )
    );
    this.count++;  // 카운터 증가
    // 다음 턴 데미지 설정
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        this.startingDeathDmg + this.count * 10);
    break;
```

**특징**:
- 턴마다 데미지 10씩 증가
- count 변수로 현재 턴 추적
- ATTACK 애니메이션 상태로 전환
- takeTurn()에서 다음 턴 의도 설정

**수정 포인트**:
- 초기 데미지 변경: `startingDeathDmg` 필드
- 증가량 변경: `INCREMENT_DMG` 상수 또는 `count * 10` 공식
- 최대 데미지 제한: count 상한 추가

---

## 특수 동작

### Fading 메커니즘 (usePreBattleAction)

**코드 위치**: 66-73줄

```java
public void usePreBattleAction() {
    if (AbstractDungeon.ascensionLevel >= 17) {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this, (AbstractCreature)this,
                new FadingPower((AbstractCreature)this, 6)
            )
        );
    } else {
        AbstractDungeon.actionManager.addToBottom(
            new ApplyPowerAction(
                (AbstractCreature)this, (AbstractCreature)this,
                new FadingPower((AbstractCreature)this, 5)
            )
        );
    }
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new ShiftingPower((AbstractCreature)this)
        )
    );
}
```

**Fading 파워**:
| 난이도 | 지속 시간 |
|--------|----------|
| 기본 (A0-A16) | 5턴 |
| A17+ | 6턴 |

**Fading 효과**:
- 매 턴 종료 시 카운터 감소
- 카운터가 0이 되면 자동 사망
- 즉, 5턴 (또는 6턴) 안에 처치해야 함

**Shifting 파워**:
- 반 유형 전환 (Intangible 유사)
- 받는 데미지를 50% 감소시킴

**특징**:
- 높은 HP와 데미지 감소로 시간 제한 전투
- Fading으로 자동 사망하지 않고 직접 처치 시 업적 해금

---

### 데미지 받을 때 (damage)

**코드 위치**: 90-96줄

```java
public void damage(DamageInfo info) {
    super.damage(info);
    if (info.owner != null &&
        info.type != DamageInfo.DamageType.THORNS &&
        info.output > 0) {
        this.state.setAnimation(0, "Hurt", false);
        this.state.addAnimation(0, "Idle", true, 0.0F);
    }
}
```

**특징**:
- 데미지를 받으면 Hurt 애니메이션
- 가시 데미지는 애니메이션 없음

---

### 상태 전환 (changeState)

**코드 위치**: 99-106줄

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

**특징**:
- ATTACK 상태: 공격 애니메이션 재생

---

### 사망 시 동작 (die)

**코드 위치**: 111-114줄

```java
public void die() {
    super.die();
    UnlockTracker.unlockAchievement("TRANSIENT");
}
```

**특징**:
- 사망 시 업적 "TRANSIENT" 해금
- Fading으로 사망하면 업적 해금 안 됨
- 직접 HP를 0으로 만들어야 업적 해금

---

## AI 로직 (getMove)

**단순한 고정 패턴**

**코드 위치**: 117-119줄

```java
protected void getMove(int num) {
    setMove((byte)1, AbstractMonster.Intent.ATTACK,
        this.startingDeathDmg + this.count * 10);
}
```

**특징**:
- 항상 공격 패턴만 사용
- 데미지는 count에 따라 자동 증가
- num 파라미터 사용하지 않음

**수정 포인트**:
- 다른 패턴 추가
- 조건부 패턴 변경

---

## 수정 예시

### 1. Fading 지속 시간 감소 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Transient",
    method = "usePreBattleAction"
)
public static class TransientFadingPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Transient __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Fading 지속 시간을 4턴으로 감소
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance, __instance,
                    new FadingPower(__instance, 4)
                )
            );
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    __instance, __instance,
                    new ShiftingPower(__instance)
                )
            );
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 2. 초기 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Transient",
    method = SpirePatch.CONSTRUCTOR
)
public static class TransientDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Transient __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // 모든 데미지 인덱스 증가
            for (DamageInfo dmg : __instance.damage) {
                dmg.base += 5;
            }
        }
    }
}
```

### 3. Shifting 파워 제거

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Transient",
    method = "usePreBattleAction"
)
public static class RemoveShiftingPatch {
    @SpirePostfixPatch
    public static void Postfix(Transient __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Shifting 파워 제거
            __instance.powers.removeIf(p -> p instanceof ShiftingPower);
        }
    }
}
```

### 4. 데미지 증가량 변경

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Transient",
    method = "takeTurn"
)
public static class IncrementDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Transient __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // count 필드 접근 필요 (private)
            // 데미지 증가량을 15로 변경
            try {
                Field countField = Transient.class.getDeclaredField("count");
                countField.setAccessible(true);
                int count = countField.getInt(__instance);

                Field startingDmgField = Transient.class.getDeclaredField("startingDeathDmg");
                startingDmgField.setAccessible(true);
                int startingDmg = startingDmgField.getInt(__instance);

                // 다음 턴 의도 재설정 (15씩 증가)
                __instance.setMove((byte)1, AbstractMonster.Intent.ATTACK,
                    startingDmg + count * 15);
                __instance.createIntent();
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
| `count` | int | 현재 턴 카운터 (초기값 0) |
| `startingDeathDmg` | int | 초기 데미지 (30 또는 40) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Transient.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.FadingPower` (시간 제한)
  - `com.megacrit.cardcrawl.powers.ShiftingPower` (데미지 감소)
- **액션**:
  - `ChangeStateAction` (애니메이션 상태 변경)
  - `DamageAction` (데미지)
  - `ApplyPowerAction` (파워 부여)
  - `WaitAction` (대기)
- **업적**:
  - `UnlockTracker.unlockAchievement("TRANSIENT")` (업적 해금)

---

## 참고사항

1. **무한 HP**: 999 HP로 사실상 직접 처치 불가능
2. **Fading 메커니즘**: 5-6턴 후 자동 사망
3. **Shifting 파워**: 받는 데미지 50% 감소
4. **업적 조건**: Fading으로 사망하지 않고 직접 HP를 0으로 만들어야 함
5. **데미지 증가**: 매 턴 10씩 증가
6. **고정 패턴**: 공격만 사용
7. **시간 제한 전투**: 5-6턴 안에 처치하거나 도망쳐야 함
8. **단일 등장**: 혼자서만 등장
9. **낮은 보상**: 금화 1개 (최소값)
10. **미리 계산된 데미지**: 7개의 데미지 인덱스가 미리 등록됨 (최대 7턴까지)

---

## 전략 포인트

### 일반 전투 (도망)
- Fading으로 자동 사망할 때까지 방어
- 5-6턴 버티면 승리
- Shifting으로 데미지 50% 감소

### 업적 전투 (직접 처치)
- 999 HP를 5-6턴 안에 처치해야 함
- 턴당 약 200 데미지 필요
- Shifting으로 실제 필요 데미지는 약 400
- 매우 높은 DPS가 필요
- 무한 콤보나 강력한 버프 덱 필요

### 난이도별 차이
- A2+: 초기 데미지 10 증가 (30→40)
- A17+: Fading 지속 시간 1턴 증가 (5→6)
- A17+는 시간 여유가 있어 약간 쉬움

### 주의사항
- 증가하는 데미지에 대비해야 함
- 3-4턴부터 데미지가 매우 높아짐
- 초반에 버프를 쌓는 것이 중요
- Shifting으로 단일 큰 타격보다 다중 소타격이 유리
