# 속박 (Entangle)

## 기본 정보

**클래스명**: `EntanglePower`
**전체 경로**: `com.megacrit.cardcrawl.powers.EntanglePower`
**ID**: `"Entangled"`
**타입**: DEBUFF
**적용 대상**: 플레이어 / 몬스터 모두

---

## 효과

**기본 효과**: 공격 카드를 사용할 수 없음
**수치 설명**: `amount`는 항상 1 (스택되지 않음)
**적용 시점**: 카드 사용 가능 여부 확인 시 (AbstractCard.canPlay)

**특수 상호작용**:
- 공격(ATTACK) 타입 카드만 차단됨
- 스킬, 파워 카드는 정상적으로 사용 가능
- 턴 종료 시 자동으로 제거됨

---

## 코드 분석

### 생성자
```java
public EntanglePower(AbstractCreature owner) {
    this.name = powerStrings.NAME;
    this.ID = "Entangled";
    this.owner = owner;
    this.amount = 1;  // 항상 1
    updateDescription();
    loadRegion("entangle");
    this.isTurnBased = true;
    this.type = AbstractPower.PowerType.DEBUFF;
}
```

**주요 파라미터**:
- `owner`: 속박 상태가 적용된 대상
- `amount`: 항상 1로 고정 (스택되지 않음)

### 핵심 메커니즘

**공격 카드 차단**: AbstractCard.canPlay() 메서드에서 확인

```java
// AbstractCard.java 라인 1030-1033
if (AbstractDungeon.player.hasPower("Entangled") && this.type == CardType.ATTACK) {
    this.cantUseMessage = TEXT[10];  // "Cannot play Attack cards."
    return false;
}
```

**중요**:
- 파워 자체에는 `canPlay` 메서드가 없음
- AbstractCard 클래스에서 직접 "Entangled" 파워 존재 여부를 확인
- CardType.ATTACK만 차단, 스킬/파워는 허용

### 지속 시간 관리

**턴 종료 시 자동 제거**:

```java
public void atEndOfTurn(boolean isPlayer) {
    if (isPlayer)
        addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, "Entangled"));
}
```

**제거 메커니즘**:
1. 플레이어 턴 종료 시에만 제거됨
2. 몬스터에게 적용된 경우 몬스터 턴 종료 시 제거
3. 다음 턴까지 지속되지 않음 (1턴 효과)

---

## 수정 방법

### 차단 범위 변경

**변경 대상**: AbstractCard.canPlay() 메서드의 타입 체크
**코드 위치**: AbstractCard.java 라인 1030-1033

**예시 1: 모든 카드 차단**

```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "canPlay"
)
public static class EntangleAllCardsPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Boolean> Insert(AbstractCard __instance, AbstractCard ___c) {
        if (AbstractDungeon.player.hasPower("Entangled")) {
            __instance.cantUseMessage = "Entangled - Cannot play any cards.";
            return SpireReturn.Return(false);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                AbstractCard.class, "type"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

**예시 2: 스킬 카드도 차단**

```java
@SpirePatch(
    clz = AbstractCard.class,
    method = "canPlay"
)
public static class EntangleAttackSkillPatch {
    @SpireInsertPatch(
        locator = Locator.class
    )
    public static SpireReturn<Boolean> Insert(AbstractCard __instance) {
        if (AbstractDungeon.player.hasPower("Entangled") &&
            (__instance.type == CardType.ATTACK || __instance.type == CardType.SKILL)) {
            __instance.cantUseMessage = "Entangled - Cannot play Attack or Skill cards.";
            return SpireReturn.Return(false);
        }
        return SpireReturn.Continue();
    }

    private static class Locator extends SpireInsertLocator {
        @Override
        public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
            Matcher finalMatcher = new Matcher.FieldAccessMatcher(
                CardType.class, "ATTACK"
            );
            return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
        }
    }
}
```

### 지속 시간 변경

**방법 1: 여러 턴 지속**
```java
// EntanglePower를 상속받은 새로운 파워 생성
public class LongEntanglePower extends EntanglePower {
    private int turnsRemaining;

    public LongEntanglePower(AbstractCreature owner, int turns) {
        super(owner);
        this.turnsRemaining = turns;
        this.amount = turns;
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            this.turnsRemaining--;
            if (this.turnsRemaining <= 0) {
                addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, this.ID));
            } else {
                this.amount = this.turnsRemaining;
                updateDescription();
            }
        }
    }
}
```

**방법 2: 영구 지속 (제거 방지)**
```java
@SpirePatch(
    clz = EntanglePower.class,
    method = "atEndOfTurn"
)
public static class PermanentEntanglePatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(EntanglePower __instance, boolean isPlayer) {
        // 턴 종료 시 제거되지 않도록 차단
        return SpireReturn.Return(null);
    }
}
```

---

## 관련 파일

**적용하는 카드**:
- Caltrops (Silent - 파워 카드, 적 공격 시 속박 적용)
- Wraith Form (Silent - 디버프로 속박 부여하는 변형 가능)

**적용하는 몬스터**:
- Lagavulin (라가불린) - 잠에서 깨어나면 속박 적용
- Bronze Automaton (청동 자동인형) - Hyper Beam 사용 시 자신에게 속박

**관련 파워**:
- **NoBlockPower**: 블록 획득 차단 (유사한 카드 사용 제한)

---

## 참고사항

1. **스택되지 않음**: `amount`가 항상 1로 고정, 중복 적용 불가
2. **공격 카드만 차단**: CardType.ATTACK만 영향, 스킬/파워는 정상 사용
3. **1턴 효과**: 턴 종료 시 항상 제거됨 (지속되지 않음)
4. **직접 체크 방식**: AbstractCard.canPlay()에서 파워 존재를 직접 확인
   - 다른 파워처럼 콜백 메서드(canPlayCard 등) 사용하지 않음
5. **사운드 효과**: "POWER_ENTANGLED" 재생 (볼륨 0.05F)
6. **Artifact로 차단 가능**: Artifact 파워로 속박 적용 자체를 막을 수 있음
7. **플레이어/몬스터 구분**: `isPlayer` 파라미터로 턴 종료 시점 구분
   - 플레이어에게 적용: 플레이어 턴 종료 시 제거
   - 몬스터에게 적용: 몬스터 턴 종료 시 제거
