# 중독 (Poison)

## 기본 정보

**클래스명**: `PoisonPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.PoisonPower`
**ID**: `"Poison"`
**타입**: 디버프 (DEBUFF)
**최대 스택**: 9999

---

## 생성자 정보

```java
public PoisonPower(AbstractCreature owner, AbstractCreature source, int poisonAmt)
```

**파라미터**:
- `owner`: 중독 파워를 받는 대상 (플레이어 또는 적)
- `source`: 중독을 부여한 대상
- `poisonAmt`: 중독 수치

**코드 위치**: 22-38줄

```java
public PoisonPower(AbstractCreature owner, AbstractCreature source, int poisonAmt) {
    this.name = NAME;
    this.ID = "Poison";
    this.owner = owner;
    this.source = source;
    this.amount = poisonAmt;

    if (this.amount >= 9999) {
        this.amount = 9999;
    }

    updateDescription();
    loadRegion("poison");
    this.type = AbstractPower.PowerType.DEBUFF;

    this.isTurnBased = true;
}
```

**특징**:
- 중독 수치가 9999를 초과하면 자동으로 9999로 제한
- `isTurnBased = true`: 턴 기반 파워

---

## 메커니즘

### 턴 시작 시 발동 (atStartOfTurn)

**코드 위치**: 64-70줄

```java
public void atStartOfTurn() {
    if ((AbstractDungeon.getCurrRoom()).phase == AbstractRoom.RoomPhase.COMBAT &&
        !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
        flashWithoutSound();
        addToBot((AbstractGameAction)new PoisonLoseHpAction(
            this.owner, this.source, this.amount, AbstractGameAction.AttackEffect.POISON));
    }
}
```

**동작 순서**:
1. 전투 페이즈 확인
2. 적이 모두 죽지 않았는지 확인
3. `PoisonLoseHpAction` 실행
   - 현재 중독 수치만큼 HP 감소
   - 데미지 타입: `HP_LOSS` (방어도 무시)
   - 중독 수치 -1 감소

---

## PoisonLoseHpAction 상세

**클래스**: `com.megacrit.cardcrawl.actions.unique.PoisonLoseHpAction`

**동작 순서**:

1. **데미지 처리** (48줄):
   ```java
   this.target.damage(new DamageInfo(this.source, this.amount, DamageInfo.DamageType.HP_LOSS));
   ```
   - `HP_LOSS` 타입: 방어도 무시, 버퍼/취약 영향 없음

2. **중독 수치 감소** (60-68줄):
   ```java
   AbstractPower p = this.target.getPower("Poison");
   if (p != null) {
       p.amount--;
       if (p.amount == 0) {
           this.target.powers.remove(p);
       } else {
           p.updateDescription();
       }
   }
   ```
   - 데미지 처리 후 중독 수치 1 감소
   - 중독 수치가 0이 되면 파워 제거

3. **시각 효과**:
   - 연두색(CHARTREUSE) 색조 효과
   - POISON 공격 이펙트

---

## 스택 처리 (stackPower)

**코드 위치**: 55-61줄

```java
public void stackPower(int stackAmount) {
    super.stackPower(stackAmount);

    if (this.amount > 98 && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
        UnlockTracker.unlockAchievement("CATALYST");
    }
}
```

**특수 기능**:
- 사일런트로 중독 수치 99 이상 달성 시 "CATALYST" 업적 해금

---

## 설명문 (Description)

**코드 위치**: 46-52줄

```java
public void updateDescription() {
    if (this.owner == null || this.owner.isPlayer) {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    } else {
        this.description = DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[1];
    }
}
```

**다국어 처리**:
- `DESCRIPTIONS[0]`: 플레이어용 설명 시작 부분
- `DESCRIPTIONS[2]`: 적용 설명 시작 부분
- `DESCRIPTIONS[1]`: 공통 설명 끝 부분

---

## 사운드 효과

**코드 위치**: 41-43줄

```java
public void playApplyPowerSfx() {
    CardCrawlGame.sound.play("POWER_POISON", 0.05F);
}
```

**사운드**: `"POWER_POISON"` (볼륨 0.05)

---

## 관련 카드 및 시스템

### 중독 부여 카드 (사일런트)

- **Poison Stab**: 중독 3 부여
- **Deadly Poison**: 중독 5 부여
- **Bouncing Flask**: 랜덤 적 3명에게 중독 3 부여
- **Noxious Fumes**: 매 턴 모든 적에게 중독 2 부여
- **Corpse Explosion**: 중독 피해 + 중독을 독으로 전환

### 중독 증폭 카드

**Catalyst**:
- 중독 수치를 2배 또는 3배로 증폭
- 코드에서 중독 99+ 업적과 연관

### 중독 관련 유물

- **Snecko Skull**: 중독 적 공격 시 추가 데미지
- **Kunai**: 공격 3회 시 민첩 1 (중독 도트와 별개)

---

## 중독 킬 카운트 (Achievement)

**코드 위치**: `PoisonLoseHpAction.java` 51-56줄

```java
if (this.target.isDying) {
    AbstractPlayer.poisonKillCount++;
    if (AbstractPlayer.poisonKillCount == 3 &&
        AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
        UnlockTracker.unlockAchievement("PLAGUE");
    }
}
```

**업적 조건**:
- 사일런트로 중독으로 적 3명 처치 시 "PLAGUE" 업적 해금

---

## 수정 포인트

### 1. 중독 데미지 증폭

```java
@SpirePatch(
    clz = PoisonPower.class,
    method = "atStartOfTurn"
)
public static class PoisonDamageMultiplierPatch {
    @SpireInsertPatch(
        locator = PoisonActionLocator.class
    )
    public static void Insert(PoisonPower __instance) {
        // 중독 데미지 2배
        int boostedAmount = __instance.amount * 2;
        AbstractDungeon.actionManager.addToBottom(
            new PoisonLoseHpAction(__instance.owner, __instance.source,
                boostedAmount, AbstractGameAction.AttackEffect.POISON)
        );
    }
}
```

### 2. 중독 감소 수치 변경

**기본 동작**: 데미지 후 중독 -1

**변경 방법**: `PoisonLoseHpAction`의 60-68줄 패치

```java
@SpirePatch(
    clz = PoisonLoseHpAction.class,
    method = "update"
)
public static class PoisonDecayPatch {
    @SpireInsertPatch(
        locator = PoisonDecayLocator.class
    )
    public static SpireReturn<Void> Insert(PoisonLoseHpAction __instance) {
        AbstractPower p = __instance.target.getPower("Poison");
        if (p != null) {
            // 중독 감소량을 2로 변경
            p.amount -= 2;
            if (p.amount <= 0) {
                __instance.target.powers.remove(p);
            } else {
                p.updateDescription();
            }
        }
        return SpireReturn.Continue();
    }
}
```

### 3. 중독 최대 스택 변경

**기본값**: 9999

```java
@SpirePatch(
    clz = PoisonPower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class PoisonCapPatch {
    @SpirePostfixPatch
    public static void Postfix(PoisonPower __instance, AbstractCreature owner,
                              AbstractCreature source, int poisonAmt) {
        // 최대 중독 9999 → 500으로 변경
        if (__instance.amount >= 500) {
            __instance.amount = 500;
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `amount` | int | 현재 중독 수치 (최대 9999) |
| `source` | AbstractCreature | 중독을 부여한 대상 |
| `isTurnBased` | boolean | 턴 기반 파워 (true) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/PoisonPower.java`
- **액션**: `com/megacrit/cardcrawl/actions/unique/PoisonLoseHpAction.java`
- **관련 카드**:
  - `com/megacrit/cardcrawl/cards/green/PoisonStab.java`
  - `com/megacrit/cardcrawl/cards/green/DeadlyPoison.java`
  - `com/megacrit/cardcrawl/cards/green/Catalyst.java`
  - `com/megacrit/cardcrawl/cards/green/NoxiousFumes.java`

---

## 참고사항

1. **데미지 타입**: `HP_LOSS`이므로 방어도, 버퍼, 취약 등 모든 수정자 무시
2. **감소 타이밍**: 데미지 처리 후 중독 수치 1 감소
3. **발동 타이밍**: 턴 시작 시 (`atStartOfTurn`)
4. **스택 제한**: 9999 고정 (생성자에서 제한)
5. **Catalyst**: 중독 수치를 2배/3배로 증폭하는 핵심 카드
6. **업적 시스템**:
   - 중독 99+: CATALYST
   - 중독으로 3킬: PLAGUE
7. **적 AI**: 일부 적(예: 슬라임 보스)이 중독 파워 부여 가능
