# 턱 (Maw)

## 기본 정보

**클래스명**: `Maw`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.Maw`
**ID**: `"Maw"`
**타입**: 일반 적 (NORMAL)
**등장 지역**: 3막 (Beyond)

---

## HP 정보

| 난이도 | HP |
|--------|----|
| 모든 난이도 | 300 (고정) |

**코드 위치**: 29줄, 43줄

```java
private static final int HP = 300;

public Maw(float x, float y) {
    super(NAME, "Maw", 300, 0.0F, -40.0F, 430.0F, 360.0F, null, x, y);
}
```

---

## 생성자 정보

### 주요 생성자
```java
public Maw(float x, float y)
```

**파라미터**:
- `x`: X 좌표
- `y`: Y 좌표

**특징**:
- 히트박스: 0.0F, -40.0F, 430.0F, 360.0F (대형 몬스터)
- 애니메이션: 랜덤 시작 시간

---

## 패턴 정보

### 패턴 2: 포효 (Roar)

**바이트 값**: `2`
**의도**: `STRONG_DEBUFF`
**발동 조건**: 전투 시작 시 무조건 첫 턴에 사용 (roared = false)

**효과**:
- 플레이어에게 **약화(Weak)** 부여
- 플레이어에게 **취약(Frail)** 부여

**디버프 지속 시간**:
| 난이도 | 지속 시간 |
|--------|----------|
| 기본 (A0-A16) | 3턴 |
| A17+ | 5턴 |

**코드 위치**: 35-36줄, 55-61줄, 78-94줄

```java
// 상수
private static final byte ROAR = 2;

// 생성자에서 설정
this.strUp = 3;
this.terrifyDur = 3;

if (AbstractDungeon.ascensionLevel >= 17) {
    this.strUp += 2;  // 5로 증가 (Drool 패턴용)
    this.terrifyDur += 2;  // 5로 증가
}

// takeTurn 실행
case 2:
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MAW_DEATH", 0.1F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ShoutAction((AbstractCreature)this, DIALOG[0], 1.0F, 2.0F)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player, (AbstractCreature)this,
            new WeakPower((AbstractCreature)AbstractDungeon.player, this.terrifyDur, true),
            this.terrifyDur
        )
    );
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)AbstractDungeon.player, (AbstractCreature)this,
            new FrailPower((AbstractCreature)AbstractDungeon.player, this.terrifyDur, true),
            this.terrifyDur
        )
    );
    this.roared = true;
    break;
```

**특징**:
- 전투 시작 시 무조건 사용
- 대사 출력 (DIALOG[0])
- 사운드 효과 (MAW_DEATH)
- roared 플래그 설정

**수정 포인트**:
- 지속 시간 변경: `terrifyDur` 필드 수정
- 디버프 종류 변경: ApplyPowerAction 수정
- 발동 조건 변경: getMove() 로직 수정

---

### 패턴 3: 내리치기 (Slam)

**바이트 값**: `3`
**의도**: `ATTACK`
**발동 조건**: AI 로직에 따름

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| 기본 (A0-A1) | 25 |
| A2+ | 30 |

**코드 위치**: 29줄, 63-69줄, 71줄, 96-99줄

```java
// 데미지 상수
private static final int SLAM_DMG = 25;
private static final int A_2_SLAM_DMG = 30;

// 생성자에서 설정
if (AbstractDungeon.ascensionLevel >= 2) {
    this.slamDmg = 30;
    this.nomDmg = 5;
} else {
    this.slamDmg = 25;
    this.nomDmg = 5;
}

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.slamDmg));

// takeTurn 실행
case 3:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
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
- 단일 타격 공격
- 느린 공격 애니메이션

**수정 포인트**:
- 데미지 변경: `slamDmg` 필드 수정
- 공격 효과 변경: AttackEffect 수정

---

### 패턴 4: 침흘리기 (Drool)

**바이트 값**: `4`
**의도**: `BUFF`
**발동 조건**: AI 로직에 따름 (Slam 또는 NomNomNom 이후)

**효과**:
- 자신에게 **힘(Strength)** 부여

**힘 수치**:
| 난이도 | 힘 |
|--------|----|
| 기본 (A0-A16) | 3 |
| A17+ | 5 |

**코드 위치**: 37줄, 55-61줄, 100-102줄

```java
// 상수
private static final byte DROOL = 4;

// 생성자에서 설정
this.strUp = 3;
if (AbstractDungeon.ascensionLevel >= 17) {
    this.strUp += 2;  // 5로 증가
}

// takeTurn 실행
case 4:
    AbstractDungeon.actionManager.addToBottom(
        new ApplyPowerAction(
            (AbstractCreature)this, (AbstractCreature)this,
            new StrengthPower((AbstractCreature)this, this.strUp),
            this.strUp
        )
    );
    break;
```

**특징**:
- Slam 또는 NomNomNom 이후 높은 확률로 사용
- 누적되는 힘 버프

**수정 포인트**:
- 힘 수치 변경: `strUp` 필드 수정
- 발동 조건 변경: getMove() 로직 수정

---

### 패턴 5: 쩝쩝쩝 (NomNomNom)

**바이트 값**: `5`
**의도**: `ATTACK` (다중 공격)
**발동 조건**: AI 로직에 따름 (roared = true 이후)

**데미지**:
- 5 데미지 x (turnCount / 2)회

**공격 횟수**:
| 턴 | 공격 횟수 |
|----|---------|
| 2턴 (turnCount=2) | 1회 |
| 3턴 (turnCount=3) | 1회 |
| 4턴 (turnCount=4) | 2회 |
| 5턴 (turnCount=5) | 2회 |
| 6턴 (turnCount=6) | 3회 |

**코드 위치**: 29줄, 38줄, 40줄, 63-72줄, 104-118줄

```java
// 상수
private static final int NOM_DMG = 5;
private static final byte NOMNOMNOM = 5;

// 필드
private int turnCount = 1;

// 생성자에서 설정 (모든 난이도 동일)
this.nomDmg = 5;

// 데미지 등록
this.damage.add(new DamageInfo((AbstractCreature)this, this.nomDmg));

// takeTurn 실행
case 5:
    AbstractDungeon.actionManager.addToBottom(
        new AnimateSlowAttackAction((AbstractCreature)this)
    );

    // turnCount / 2 만큼 반복
    for (i = 0; i < this.turnCount / 2; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new VFXAction(
                new BiteEffect(
                    AbstractDungeon.player.hb.cX + MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                    AbstractDungeon.player.hb.cY + MathUtils.random(-50.0F, 50.0F) * Settings.scale,
                    Color.SKY.cpy()
                )
            )
        );

        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(
                (AbstractCreature)AbstractDungeon.player,
                this.damage.get(1),
                AbstractGameAction.AttackEffect.NONE
            )
        );
    }
    break;
```

**특징**:
- 턴이 지날수록 공격 횟수 증가
- BiteEffect 이펙트 (하늘색)
- 랜덤 위치에 공격 이펙트 표시
- 최소 1회 공격 (turnCount=2~3일 때)

**수정 포인트**:
- 데미지 변경: `nomDmg` 필드 수정
- 공격 횟수 계산 변경: `turnCount / 2` 공식 수정
- 이펙트 색상 변경: Color.SKY 수정

---

## AI 로직 (getMove)

**턴 카운터 기반 AI**

**코드 위치**: 125-146줄

### 턴 카운터 증가
```java
protected void getMove(int num) {
    this.turnCount++;
    // ...
}
```

**특징**:
- getMove()가 호출될 때마다 turnCount 증가
- 첫 번째 getMove() 호출 시 turnCount = 2

### 첫 번째 턴 (roared = false)
```java
if (!this.roared) {
    setMove((byte)2, AbstractMonster.Intent.STRONG_DEBUFF);
    return;
}
```

무조건 **Roar (패턴 2)** 사용

### 이후 턴 (num < 50, 50% 확률)
```java
if (num < 50 && !lastMove((byte)5)) {
    if (this.turnCount / 2 <= 1) {
        setMove((byte)5, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(1)).base);
    } else {
        setMove((byte)5, AbstractMonster.Intent.ATTACK,
            ((DamageInfo)this.damage.get(1)).base,
            this.turnCount / 2, true);
    }
    return;
}
```

**로직**:
- 이전 패턴이 NomNomNom(5)이 아니면
- 50% 확률로 NomNomNom (패턴 5)
- 공격 횟수는 turnCount / 2

### 이후 턴 (Slam 또는 NomNomNom 이후)
```java
if (lastMove((byte)3) || lastMove((byte)5)) {
    setMove((byte)4, AbstractMonster.Intent.BUFF);
    return;
}
```

**로직**:
- 이전 패턴이 Slam(3) 또는 NomNomNom(5)이면
- Drool (패턴 4) 사용

### 이후 턴 (나머지 경우)
```java
setMove((byte)3, AbstractMonster.Intent.ATTACK,
    ((DamageInfo)this.damage.get(0)).base);
```

**로직**:
- 그 외의 경우 Slam (패턴 3) 사용

**전체 패턴 흐름**:
1. 첫 턴: Roar (고정)
2. 이후: NomNomNom (50% 확률) OR Slam
3. NomNomNom/Slam 이후: Drool (거의 확정)
4. Drool 이후: NomNomNom (50% 확률) OR Slam
5. 반복...

**수정 포인트**:
- 패턴 순서 변경
- 확률 변경 (num < 50 조건)
- turnCount 증가 속도 변경
- 첫 턴 패턴 변경

---

## 특수 동작

### 사망 시 동작 (die)

**코드 위치**: 151-154줄

```java
public void die() {
    super.die();
    CardCrawlGame.sound.play("MAW_DEATH");
}
```

**특징**:
- 사망 시 "MAW_DEATH" 사운드 재생
- Roar 패턴에서도 같은 사운드 사용

---

## 수정 예시

### 1. NomNomNom 공격 횟수 증가 (Ascension 25)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Maw",
    method = "takeTurn"
)
public static class MawNomCountPatch {
    @SpireInsertPatch(
        locator = NomLoopLocator.class
    )
    public static void Insert(Maw __instance) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // turnCount를 증가시켜 공격 횟수 증가
            // 실제로는 for문 조건을 수정해야 함
        }
    }

    private static class NomLoopLocator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                Maw.class, "turnCount");
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 2. Roar 지속 시간 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Maw",
    method = SpirePatch.CONSTRUCTOR
)
public static class MawRoarPatch {
    @SpirePostfixPatch
    public static void Postfix(Maw __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // terrifyDur 필드 접근 필요 (private)
            // Reflection 사용 또는 별도 필드 추가
            try {
                Field terrifyField = Maw.class.getDeclaredField("terrifyDur");
                terrifyField.setAccessible(true);
                int currentDur = terrifyField.getInt(__instance);
                terrifyField.setInt(__instance, currentDur + 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 3. Slam 데미지 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.beyond.Maw",
    method = SpirePatch.CONSTRUCTOR
)
public static class MawSlamDamagePatch {
    @SpirePostfixPatch
    public static void Postfix(Maw __instance, float x, float y) {
        if (AbstractDungeon.isAscensionMode &&
            AbstractDungeon.ascensionLevel >= 25) {
            // Slam 데미지 증가 (damage.get(0))
            if (!__instance.damage.isEmpty()) {
                __instance.damage.get(0).base += 5;
            }
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `roared` | boolean | Roar 사용 여부 |
| `turnCount` | int | 턴 카운터 (초기값 1) |
| `slamDmg` | int | Slam 데미지 |
| `nomDmg` | int | NomNomNom 데미지 (개별) |
| `strUp` | int | Drool 힘 수치 |
| `terrifyDur` | int | Roar 디버프 지속 시간 |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/Maw.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.WeakPower` (약화)
  - `com.megacrit.cardcrawl.powers.FrailPower` (취약)
  - `com.megacrit.cardcrawl.powers.StrengthPower` (힘)
- **이펙트**:
  - `com.megacrit.cardcrawl.vfx.combat.BiteEffect` (물어뜯기 이펙트)
- **액션**:
  - `AnimateSlowAttackAction` (느린 공격 애니메이션)
  - `ShoutAction` (대사 출력)
  - `DamageAction` (데미지)
  - `ApplyPowerAction` (파워 부여)
  - `VFXAction` (이펙트)
  - `SFXAction` (사운드 효과)
  - `RollMoveAction` (다음 패턴 결정)

---

## 참고사항

1. **첫 턴 고정**: 무조건 Roar 사용
2. **턴 카운터**: getMove() 호출마다 증가 (초기값 1)
3. **공격 횟수 증가**: NomNomNom은 턴이 지날수록 강력해짐
4. **패턴 순환**: Slam/NomNomNom → Drool → Slam/NomNomNom 반복
5. **높은 HP**: 300으로 고정되어 장기전
6. **강력한 디버프**: 약화와 취약을 동시에 부여
7. **누적 힘**: Drool로 계속 힘이 증가함
8. **랜덤 이펙트**: NomNomNom의 BiteEffect 위치가 랜덤
