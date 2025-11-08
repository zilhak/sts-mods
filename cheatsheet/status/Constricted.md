# 구속 (Constricted)

## 기본 정보

**클래스명**: `ConstrictedPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.ConstrictedPower`
**ID**: `"Constricted"`
**타입**: 디버프 (DEBUFF)
**우선순위**: 105

---

## 생성자 정보

```java
public ConstrictedPower(AbstractCreature target, AbstractCreature source, int fadeAmt)
```

**파라미터**:
- `target`: 구속 파워를 받는 대상 (플레이어 또는 적)
- `source`: 구속을 부여한 대상
- `fadeAmt`: 구속 데미지 수치

**코드 위치**: 17-29줄

```java
public ConstrictedPower(AbstractCreature target, AbstractCreature source, int fadeAmt) {
    this.name = NAME;
    this.ID = "Constricted";
    this.owner = target;
    this.source = source;
    this.amount = fadeAmt;
    updateDescription();
    loadRegion("constricted");
    this.type = AbstractPower.PowerType.DEBUFF;

    this.priority = 105;
}
```

**특징**:
- `priority = 105`: 높은 우선순위 (턴 종료 시 먼저 발동)
- 스택 제한 없음

---

## 메커니즘

### 턴 종료 시 발동 (atEndOfTurn)

**코드 위치**: 42-46줄

```java
public void atEndOfTurn(boolean isPlayer) {
    flashWithoutSound();
    playApplyPowerSfx();
    addToBot((AbstractGameAction)new DamageAction(
        this.owner,
        new DamageInfo(this.source, this.amount, DamageInfo.DamageType.THORNS)));
}
```

**동작 순서**:
1. 파워 플래시 효과
2. 사운드 재생
3. `DamageAction` 실행
   - 데미지 수치: `this.amount` (구속 수치)
   - 데미지 타입: `THORNS` (가시 데미지)
   - **중요**: 구속 수치는 감소하지 않음 (지속 데미지)

---

## 데미지 타입: THORNS

**특징**:
- 방어도 무시
- 버퍼(Buffer) 무시
- 취약(Vulnerable) 영향 없음
- 힘(Strength) 영향 없음

**Poison과의 차이점**:

| 항목 | Poison | Constricted |
|------|--------|-------------|
| 발동 타이밍 | 턴 시작 | 턴 종료 |
| 데미지 타입 | HP_LOSS | THORNS |
| 수치 감소 | 매 턴 -1 | 감소 없음 (지속) |
| 스택 제한 | 9999 | 없음 |

---

## 설명문 (Description)

**코드 위치**: 37-39줄

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
}
```

**다국어 처리**:
- `DESCRIPTIONS[0]`: 설명 시작 부분 (예: "턴 종료 시 ")
- `this.amount`: 데미지 수치
- `DESCRIPTIONS[1]`: 설명 끝 부분 (예: " 피해를 받습니다.")

---

## 사운드 효과

**코드 위치**: 32-34줄

```java
public void playApplyPowerSfx() {
    CardCrawlGame.sound.play("POWER_CONSTRICTED", 0.05F);
}
```

**사운드**: `"POWER_CONSTRICTED"` (볼륨 0.05)

---

## 관련 몬스터

### Spire Growth (가시 성장체)

**클래스**: `com.megacrit.cardcrawl.monsters.beyond.SpireGrowth`

**구속 부여 패턴**:
- 특정 공격 시 플레이어에게 Constricted 파워 부여
- 고정된 수치로 부여 (난이도별 다를 수 있음)

**등장 위치**: 3막 (Beyond)

---

## 제거 방법

### 직접 제거 카드

1. **Orange Pellets** (유물):
   - 한 턴에 공격/스킬/파워 각 1장씩 사용 시 디버프 제거
   - Constricted 포함 모든 디버프 제거

2. **Artifact** (파워):
   - 1스택당 디버프 1개 무효화
   - Constricted 부여 시 Artifact로 차단 가능

3. **Panacea** (포션):
   - 모든 디버프 제거
   - Constricted 포함

4. **시간 경과**:
   - Constricted는 자동 감소하지 않음
   - 제거 수단 없으면 전투 내내 지속

---

## 수정 포인트

### 1. 구속 데미지 증가

```java
@SpirePatch(
    clz = ConstrictedPower.class,
    method = "atEndOfTurn"
)
public static class ConstrictedDamagePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(ConstrictedPower __instance, boolean isPlayer) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // 구속 데미지 1.5배
            int boostedDamage = (int)(__instance.amount * 1.5f);
            __instance.flashWithoutSound();
            __instance.playApplyPowerSfx();
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(__instance.owner,
                    new DamageInfo(__instance.source, boostedDamage,
                        DamageInfo.DamageType.THORNS))
            );
            return SpireReturn.Return(null);
        }
        return SpireReturn.Continue();
    }
}
```

### 2. 구속 수치 자동 감소 추가

**기본 동작**: 구속 수치는 감소하지 않음

**변경 방법**: 턴 종료 시 수치 감소 로직 추가

```java
@SpirePatch(
    clz = ConstrictedPower.class,
    method = "atEndOfTurn"
)
public static class ConstrictedDecayPatch {
    @SpirePostfixPatch
    public static void Postfix(ConstrictedPower __instance, boolean isPlayer) {
        // 구속 수치 -1 감소
        __instance.amount--;
        if (__instance.amount <= 0) {
            __instance.owner.powers.remove(__instance);
        } else {
            __instance.updateDescription();
        }
    }
}
```

### 3. Spire Growth 구속 부여량 조정

```java
@SpirePatch(
    clz = "com.megacrit.cardcrawl.monsters.beyond.SpireGrowth",
    method = "takeTurn"
)
public static class SpireGrowthConstrictPatch {
    @SpireInsertPatch(
        locator = ConstrictApplicationLocator.class
    )
    public static void Insert(Object __instance) {
        if (AbstractDungeon.ascensionLevel >= 20) {
            // 구속 부여량 증가
            int baseAmount = 5; // 기본값 확인 필요
            int increasedAmount = baseAmount + 3;

            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(AbstractDungeon.player, (AbstractMonster)__instance,
                    new ConstrictedPower(AbstractDungeon.player,
                        (AbstractMonster)__instance, increasedAmount))
            );
        }
    }
}
```

### 4. 우선순위 변경

**기본값**: 105 (높은 우선순위)

```java
@SpirePatch(
    clz = ConstrictedPower.class,
    method = SpirePatch.CONSTRUCTOR
)
public static class ConstrictedPriorityPatch {
    @SpirePostfixPatch
    public static void Postfix(ConstrictedPower __instance, AbstractCreature target,
                              AbstractCreature source, int fadeAmt) {
        // 우선순위를 50으로 낮춤 (일반 디버프 수준)
        __instance.priority = 50;
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `amount` | int | 턴 종료 시 데미지 수치 |
| `source` | AbstractCreature | 구속을 부여한 대상 |
| `priority` | int | 파워 우선순위 (105) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/powers/ConstrictedPower.java`
- **액션**: `com/megacrit/cardcrawl/actions/common/DamageAction.java`
- **관련 몬스터**:
  - `com/megacrit/cardcrawl/monsters/beyond/SpireGrowth.java`

---

## 참고사항

1. **데미지 타입**: `THORNS`이므로 방어도 무시, 버퍼 무시
2. **발동 타이밍**: 턴 종료 시 (`atEndOfTurn`)
3. **수치 감소**: 없음 - 전투 중 계속 지속
4. **우선순위**: 105로 높음 (다른 턴 종료 효과보다 먼저 발동)
5. **제거 방법**:
   - Orange Pellets (유물)
   - Artifact (파워)
   - Panacea (포션)
6. **스택**: 무제한 (Poison과 달리 9999 제한 없음)
7. **주요 출처**: Spire Growth (3막 적)
8. **Poison과의 비교**:
   - Poison: 턴 시작, HP_LOSS, 자동 감소
   - Constricted: 턴 종료, THORNS, 지속
