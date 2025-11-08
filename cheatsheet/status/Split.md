# 분열 (Split)

## 기본 정보

**클래스명**: `SplitPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.SplitPower`
**ID**: `"Split"`
**타입**: BUFF (패시브 마커)
**적용 대상**: 슬라임 계열 몬스터

---

## 효과

**기본 효과**: 죽으면 2마리의 작은 슬라임으로 분열
**수치 설명**: `amount = -1` (수치 없음, 단순 마커)
**적용 시점**: 몬스터가 죽을 때 (die() 호출 시)

**특수 메커니즘**:
- **HP 비례 분열**: 죽을 때 남은 HP를 작은 슬라임들이 나눠 가짐
- **보스 전용 특수 로직**: SlimeBoss는 HP 50% 이하일 때 자동 분열
- **분열 타이밍**: CannotLoseAction으로 감싸서 분열 완료 전까지 전투 종료 방지

---

## 코드 분석

### 생성자
```java
public SplitPower(AbstractCreature owner) {
    this.name = NAME;
    this.ID = "Split";
    this.owner = owner;  // 슬라임 자신
    this.amount = -1;    // 수치 표시 없음
    updateDescription();
    loadRegion("split");
    // type 설정 없음 (기본값 BUFF)
}
```

**주요 파라미터**:
- `owner`: 슬라임 자신
- `amount`: -1 (시각적 수치 표시 안 함)

**중요**: 파워 자체에는 분열 로직 없음 (몬스터 클래스에서 구현)

### 핵심 메서드

**updateDescription()**: 설명 텍스트

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0] +
                      FontHelper.colorString(this.owner.name, "y") +
                      DESCRIPTIONS[1];
    // "죽으면 [슬라임 이름](으)로 분열합니다."
}
```

### 분열 로직 예시 (SlimeBoss)

**HP 50% 이하일 때 자동 분열**:

```java
public void damage(DamageInfo info) {
    super.damage(info);

    // HP 50% 이하이고 분열 중이 아니면
    if (!this.isDying &&
        this.currentHealth <= this.maxHealth / 2.0F &&
        this.nextMove != 3) {  // 3 = SPLIT

        // 분열 패턴으로 강제 전환
        setMove(SPLIT_NAME, (byte)3, Intent.UNKNOWN);
        createIntent();

        AbstractDungeon.actionManager.addToBottom(
            new TextAboveCreatureAction(this, TextAboveCreatureAction.TextType.INTERRUPTED)
        );
        AbstractDungeon.actionManager.addToBottom(
            new SetMoveAction(this, SPLIT_NAME, (byte)3, Intent.UNKNOWN)
        );
    }
}
```

**실제 분열 실행** (takeTurn() 내부):

```java
case SPLIT:  // 분열 패턴
    // 전투 종료 방지
    AbstractDungeon.actionManager.addToBottom(new CannotLoseAction());

    // 분열 애니메이션
    AbstractDungeon.actionManager.addToBottom(
        new AnimateShakeAction(this, 1.0F, 0.1F)
    );

    // HP 바 숨기기
    AbstractDungeon.actionManager.addToBottom(
        new HideHealthBarAction(this)
    );

    // 보스 자살 (보상 없음)
    AbstractDungeon.actionManager.addToBottom(
        new SuicideAction(this, false)
    );

    AbstractDungeon.actionManager.addToBottom(new WaitAction(1.0F));
    AbstractDungeon.actionManager.addToBottom(new SFXAction("SLIME_SPLIT"));

    // 2마리 소환 (현재 HP를 나눠 가짐)
    AbstractDungeon.actionManager.addToBottom(
        new SpawnMonsterAction(
            new SpikeSlime_L(-385.0F, 20.0F, 0, this.currentHealth),
            false
        )
    );

    AbstractDungeon.actionManager.addToBottom(
        new SpawnMonsterAction(
            new AcidSlime_L(120.0F, -8.0F, 0, this.currentHealth),
            false
        )
    );

    // 전투 종료 허용
    AbstractDungeon.actionManager.addToBottom(new CanLoseAction());
    break;
```

### 일반 슬라임 분열 (죽을 때 트리거)

일반 Medium/Large 슬라임은 `die()` 메서드를 오버라이드하지 않고, `onDeath()` 트리거를 파워에서 처리하지 않음. 대신 각 슬라임 클래스의 `die()` 메서드에서 직접 구현:

```java
// 예시: AcidSlime_M.die()
public void die() {
    super.die();

    if (AbstractDungeon.getCurrRoom().cannotLose) {
        return;  // 무한 분열 방지
    }

    // 2마리의 Small 슬라임 소환
    AbstractDungeon.actionManager.addToBottom(
        new SpawnMonsterAction(
            new AcidSlime_S(-200.0F, 0.0F),
            true
        )
    );

    AbstractDungeon.actionManager.addToBottom(
        new SpawnMonsterAction(
            new AcidSlime_S(100.0F, 0.0F),
            true
        )
    );
}
```

---

## 수정 방법

### 분열 몬스터 종류 변경

**예시: 다른 종류의 몬스터로 분열**

```java
@SpirePatch(
    clz = SlimeBoss.class,
    method = "takeTurn"
)
public static class DifferentSplitPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(SlimeBoss __instance) {
        // 슬라임 대신 그렘린으로 분열
        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new GremlinWarrior(-385.0F, 20.0F),
                false
            )
        );

        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new GremlinWarrior(120.0F, -8.0F),
                false
            )
        );
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.MethodCallMatcher(
                SpawnMonsterAction.class, "<init>"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 분열 HP 비율 조정

**예시: 분열 슬라임이 더 많은 HP를 받도록**

```java
@SpirePatch(
    clz = SlimeBoss.class,
    method = "takeTurn"
)
public static class HigherHPSplitPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(SlimeBoss __instance) {
        // 현재 HP의 150%로 분열 (기본은 100%)
        int splitHP = (int)(__instance.currentHealth * 1.5f);

        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new SpikeSlime_L(-385.0F, 20.0F, 0, splitHP),
                false
            )
        );

        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new AcidSlime_L(120.0F, -8.0F, 0, splitHP),
                false
            )
        );
    }
}
```

### 분열 횟수 증가

**예시: 3마리로 분열 (기본 2마리)**

```java
@SpirePatch(
    clz = SlimeBoss.class,
    method = "takeTurn"
)
public static class TripleSplitPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static void Insert(SlimeBoss __instance) {
        // HP를 3등분
        int splitHP = __instance.currentHealth / 3;

        // 3마리 소환
        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new SpikeSlime_L(-500.0F, 20.0F, 0, splitHP),
                false
            )
        );

        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new AcidSlime_L(0.0F, -8.0F, 0, splitHP),
                false
            )
        );

        AbstractDungeon.actionManager.addToBottom(
            new SpawnMonsterAction(
                new SpikeSlime_L(300.0F, 20.0F, 0, splitHP),
                false
            )
        );
    }
}
```

### 분열 조건 변경

**예시: HP 25% 이하일 때 분열 (기본 50%)**

```java
@SpirePatch(
    clz = SlimeBoss.class,
    method = "damage"
)
public static class LowerSplitThresholdPatch {
    @SpirePrefixPatch
    public static void Prefix(SlimeBoss __instance, DamageInfo info) {
        // HP 25% 이하이고 분열 중이 아니면 분열
        if (!__instance.isDying &&
            __instance.currentHealth <= __instance.maxHealth / 4.0F &&
            __instance.nextMove != 3) {

            __instance.setMove("분열", (byte)3, Intent.UNKNOWN);
            __instance.createIntent();
        }
    }
}
```

### 분열 시 버프 부여

**예시: 분열된 슬라임에게 Strength 부여**

```java
@SpirePatch(
    clz = SlimeBoss.class,
    method = "takeTurn"
)
public static class BuffedSplitPatch {
    @SpirePostfixPatch
    public static void Postfix(SlimeBoss __instance) {
        // 방금 소환된 슬라임들에게 Strength +3 부여
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!m.isDead && !m.isDying && m != __instance) {
                AbstractDungeon.actionManager.addToBottom(
                    new ApplyPowerAction(m, m,
                        new StrengthPower(m, 3), 3)
                );
            }
        }
    }
}
```

---

## 관련 파일

**적용하는 몬스터**:
- **SlimeBoss** (슬라임 보스):
  - Act 1 보스
  - HP 50% 이하일 때 자동 분열
  - SpikeSlime_L + AcidSlime_L로 분열

- **AcidSlime_L/M** (산성 슬라임 대/중):
  - 죽으면 2마리의 작은 슬라임으로 분열
  - Large → 2× Medium
  - Medium → 2× Small

- **SpikeSlime_L/M** (가시 슬라임 대/중):
  - 죽으면 2마리의 작은 슬라임으로 분열
  - Large → 2× Medium
  - Medium → 2× Small

**관련 액션**:
- **SpawnMonsterAction**: 새 몬스터 소환
- **CannotLoseAction**: 전투 종료 방지
- **CanLoseAction**: 전투 종료 허용 재개
- **SuicideAction**: 몬스터 즉시 제거
- **HideHealthBarAction**: HP 바 숨김

**분열 패턴**:
- **SlimeBoss**: HP 50% → 강제 분열 패턴 전환
- **Medium Slime**: 죽으면 즉시 2× Small 소환
- **Large Slime**: 죽으면 즉시 2× Medium 소환
- **Small Slime**: 분열 안 함 (SplitPower 없음)

---

## 참고사항

1. **amount = -1**: 수치 표시 없음 (단순 마커)
2. **파워 자체에 로직 없음**: 실제 분열은 각 몬스터 클래스에서 구현
3. **HP 상속**: 분열 시 부모 슬라임의 HP를 자식들이 나눠 가짐
4. **무한 분열 방지**: `cannotLose` 체크로 무한 분열 방지
5. **보스 특수 로직**: SlimeBoss는 HP 50% 자동 분열
6. **일반 슬라임**: 죽을 때만 분열 (HP 체크 없음)
7. **Small 슬라임**: SplitPower 없음 (더 이상 분열 안 함)
8. **CannotLoseAction**: 분열 중 전투 종료 방지
   - 분열 완료 전까지 플레이어 승리 불가
   - 분열 후 CanLoseAction으로 해제
9. **SuicideAction**: 보스는 보상 없이 제거됨 (자식 슬라임이 보스 역할)
10. **시각 효과**: "SLIME_SPLIT" 사운드 + 흔들림 애니메이션
11. **위치 지정**: SpawnMonsterAction에서 X, Y 좌표 지정 (화면 배치)
12. **타입별 차이**:
    - **AcidSlime**: 약화(Weak) 부여
    - **SpikeSlime**: Frail 부여
    - 분열 후에도 각 타입의 특성 유지
