# 미니언 (Minion)

## 기본 정보

**클래스명**: `MinionPower`
**전체 경로**: `com.megacrit.cardcrawl.powers.MinionPower`
**ID**: `"Minion"`
**타입**: BUFF
**적용 대상**: 보스의 부하 몬스터

---

## 효과

**기본 효과**: 주인(보스)이 죽으면 도망침
**수치 설명**: `amount` 필드 없음 (패시브 효과)
**적용 시점**: 보스가 죽었을 때 (AbstractMonster.die() 호출 시)

**특수 메커니즘**:
- **주인-부하 관계**: 보스가 미니언에게 이 파워를 부여
- **자동 도망**: 보스가 죽으면 미니언들이 자동으로 SuicideAction + HideHealthBar 실행
- **전투 종료 가속화**: 보스 처치 후 불필요한 전투 연장 방지

---

## 코드 분석

### 생성자
```java
public MinionPower(AbstractCreature owner) {
    this.name = NAME;
    this.ID = "Minion";
    this.owner = owner;  // 미니언 자신
    updateDescription();
    loadRegion("minion");
    this.type = AbstractPower.PowerType.BUFF;  // 미니언 입장에서 BUFF (패시브 표시)
}
```

**주요 파라미터**:
- `owner`: 미니언 자신 (이 파워를 받는 대상)
- 생성 시 주인(보스) 정보는 파워 자체에 저장되지 않음

**중요**: `MinionPower` 자체에는 `amount` 필드가 없음 (단순 마커 역할)

### 핵심 메서드

**updateDescription()**: 설명 텍스트

```java
public void updateDescription() {
    this.description = DESCRIPTIONS[0];
    // "주인이 죽으면 도망칩니다."
}
```

**실제 도망 로직**: 보스의 `die()` 메서드에서 구현

```java
// 예시: Reptomancer.die()
public void die() {
    super.die();
    for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
        if (!m.isDead && !m.isDying) {
            // 미니언들을 즉시 제거
            AbstractDungeon.actionManager.addToTop(
                new HideHealthBarAction(m)
            );
            AbstractDungeon.actionManager.addToTop(
                new SuicideAction(m)
            );
        }
    }
}
```

### 적용 예시 (Reptomancer)

**usePreBattleAction()**: 전투 시작 시 미니언에게 파워 부여

```java
public void usePreBattleAction() {
    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
        if (!m.id.equals(this.id)) {  // 자신 제외
            // 미니언에게 MinionPower 부여
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(m, m, new MinionPower(m))
            );
        }
    }
}
```

---

## 수정 방법

### 미니언 HP 비례 보상

**예시: 보스 죽을 때 미니언 남은 HP만큼 회복**

```java
@SpirePatch(
    clz = Reptomancer.class,  // 또는 다른 보스 클래스
    method = "die"
)
public static class MinionHPRewardPatch {
    @SpirePrefixPatch
    public static void Prefix(Reptomancer __instance) {
        int totalMinionHP = 0;

        // 살아있는 미니언의 HP 합산
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!m.isDead && !m.isDying && m.hasPower("Minion")) {
                totalMinionHP += m.currentHealth;
            }
        }

        // 플레이어 HP 회복
        if (totalMinionHP > 0) {
            AbstractDungeon.player.heal(totalMinionHP);
        }
    }
}
```

### 미니언 도망 지연

**예시: 2턴 후에 도망치도록 변경**

```java
@SpirePatch(
    clz = Reptomancer.class,
    method = "die"
)
public static class DelayedEscapePatch {
    @SpirePostfixPatch
    public static void Postfix(Reptomancer __instance) {
        // 기존 즉시 제거 액션 제거
        AbstractDungeon.actionManager.actions.removeIf(a ->
            a instanceof SuicideAction || a instanceof HideHealthBarAction
        );

        // 2턴 후 도망 예약
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!m.isDead && !m.isDying && m.hasPower("Minion")) {
                m.escapeNext();  // 다음 턴에 도망
            }
        }
    }
}
```

### 미니언 강화 메커니즘

**예시: 보스 죽으면 미니언 강화 (도망 대신)**

```java
@SpirePatch(
    clz = MinionPower.class
)
public static class EnragedMinionPatch {
    @SpirePatch(
        clz = Reptomancer.class,
        method = "die"
    )
    public static class BossDeathPatch {
        @SpirePrefixPatch
        public static SpireReturn<Void> Prefix(Reptomancer __instance) {
            // 미니언들을 제거하지 않고 강화
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!m.isDead && !m.isDying && m.hasPower("Minion")) {
                    // Strength +5, HP 회복
                    AbstractDungeon.actionManager.addToTop(
                        new ApplyPowerAction(m, m,
                            new StrengthPower(m, 5), 5)
                    );
                    m.heal(m.maxHealth / 2);

                    // MinionPower 제거 (더 이상 도망가지 않음)
                    AbstractDungeon.actionManager.addToTop(
                        new RemoveSpecificPowerAction(m, m, "Minion")
                    );
                }
            }

            // 원본 die() 로직 실행
            return SpireReturn.Continue();
        }
    }
}
```

### 특정 조건에서만 도망

**예시: 미니언 HP가 50% 이상이면 도망 안 침**

```java
@SpirePatch(
    clz = Reptomancer.class,
    method = "die"
)
public static class ConditionalEscapePatch {
    @SpirePostfixPatch
    public static void Postfix(Reptomancer __instance) {
        // 기존 제거 액션 제거
        AbstractDungeon.actionManager.actions.removeIf(a ->
            a instanceof SuicideAction || a instanceof HideHealthBarAction
        );

        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!m.isDead && !m.isDying && m.hasPower("Minion")) {
                // HP 50% 미만만 도망
                if (m.currentHealth < m.maxHealth / 2) {
                    AbstractDungeon.actionManager.addToTop(
                        new HideHealthBarAction(m)
                    );
                    AbstractDungeon.actionManager.addToTop(
                        new SuicideAction(m)
                    );
                } else {
                    // MinionPower 제거하고 계속 싸움
                    AbstractDungeon.actionManager.addToTop(
                        new RemoveSpecificPowerAction(m, m, "Minion")
                    );
                }
            }
        }
    }
}
```

---

## 관련 파일

**적용하는 몬스터**:
- **Reptomancer** (파충술사): Snake Dagger들에게 부여
  - Act 3 엘리트 몬스터
  - 2마리의 Snake Dagger 소환
  - 보스 죽으면 둘 다 즉시 도망

- **Gremlin Leader** (그렘린 리더): Gremlin 무리에게 부여
  - Act 2 엘리트 몬스터
  - 3마리의 Gremlin 부하 소환
  - 리더 죽으면 모두 도망

**도망 메커니즘**:
1. 보스의 `die()` 메서드 호출
2. 모든 몬스터 순회하며 살아있는 미니언 확인
3. `HideHealthBarAction` + `SuicideAction` 실행
4. 미니언이 화면에서 사라짐 (도망 애니메이션 없음)

**관련 액션**:
- **SuicideAction**: 몬스터 즉시 제거 (보상 없음)
- **HideHealthBarAction**: HP 바 숨김
- **EscapeAction**: 실제 도망 애니메이션 (MinionPower는 SuicideAction 사용)

---

## 참고사항

1. **amount 없음**: 단순 마커 파워로 수치 없음
2. **도망 vs 자살**:
   - 실제로는 `SuicideAction` 사용 (즉시 제거)
   - `escape()` 메서드는 사용하지 않음 (도망 애니메이션 없음)
3. **보스 die() 오버라이드 필수**: MinionPower 자체에는 도망 로직 없음
   - 보스가 `die()` 메서드에서 미니언 제거 로직 구현 필요
4. **제거 가능**: Artifact나 Orange Pellets로 제거 가능 (도망 방지)
5. **전투 가속화**: 보스 처치 후 불필요한 미니언 처리 생략
6. **보상 없음**: SuicideAction이므로 골드/경험치 보상 없음
7. **주인 정보 없음**: 파워 자체에 보스 참조 저장 안 함 (보스가 직접 관리)
8. **우선순위 없음**: `priority` 필드 설정 안 함 (패시브 효과)
9. **시각 효과**: "minion" 아이콘 표시 (작은 해골 모양)
10. **상호작용 없음**: `onDeath()` 같은 트리거 메서드 없음 (보스가 die()에서 처리)
