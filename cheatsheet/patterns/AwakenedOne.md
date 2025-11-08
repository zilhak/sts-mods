# 깨어난 자 (The Awakened One)

## 기본 정보

**클래스명**: `AwakenedOne`
**전체 경로**: `com.megacrit.cardcrawl.monsters.beyond.AwakenedOne`
**ID**: `"AwakenedOne"`
**타입**: 보스 (BOSS)
**등장 지역**: 3막 (The Beyond)

---

## HP 정보

### Phase 1 (Cultist Form)

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 300 |
| A9+ | 320 |

### Phase 2 (Awakened Form)

| 난이도 | HP |
|--------|-----|
| 기본 (A0-A8) | 300 |
| A9+ | 320 |

**특징**:
- 2페이즈 보스 (Phase 1 HP가 0이 되면 Phase 2로 부활)
- 총 HP = 600 (A9+: 640)
- Phase 1에서는 `cannotLose = true`로 죽지 않음

---

## 생성자 정보

```java
public AwakenedOne(float x, float y) {
    super(NAME, "AwakenedOne", 300, 40.0F, -30.0F, 460.0F, 250.0F, null, x, y);

    if (AbstractDungeon.ascensionLevel >= 9) {
        setHp(320);  // Phase 1 HP
    } else {
        setHp(300);
    }

    // 애니메이션 초기화
    loadAnimation("images/monsters/theForest/awakenedOne/skeleton.atlas",
                  "images/monsters/theForest/awakenedOne/skeleton.json", 1.0F);
    state.setAnimation(0, "Idle_1", true);  // Phase 1 애니메이션

    // 데미지 정보 초기화 (5개)
    damage.add(new DamageInfo(this, 20));  // [0] Slash
    damage.add(new DamageInfo(this, 6));   // [1] Soul Strike (4타)
    damage.add(new DamageInfo(this, 40));  // [2] Dark Echo
    damage.add(new DamageInfo(this, 18));  // [3] Sludge
    damage.add(new DamageInfo(this, 10));  // [4] Tackle (3타)
}
```

---

## Phase 1: Cultist Form (형상 1)

### 시작 파워

```java
public void usePreBattleAction() {
    // BGM 변경
    AbstractDungeon.getCurrRoom().playBgmInstantly("BOSS_BEYOND");

    // Phase 1에서 죽지 않도록 설정
    (AbstractDungeon.getCurrRoom()).cannotLose = true;

    // Regenerate 파워
    if (AbstractDungeon.ascensionLevel >= 19) {
        // A19+: Regenerate 15
        ApplyPowerAction(this, this, new RegenerateMonsterPower(this, 15));
        // Curiosity 2 (카드 사용 시 힘 +2)
        ApplyPowerAction(this, this, new CuriosityPower(this, 2));
    } else {
        // 기본: Regenerate 10
        ApplyPowerAction(this, this, new RegenerateMonsterPower(this, 10));
        // Curiosity 1 (카드 사용 시 힘 +1)
        ApplyPowerAction(this, this, new CuriosityPower(this, 1));
    }

    // Unawakened 파워 (Phase 1 전용 마커)
    ApplyPowerAction(this, this, new UnawakenedPower(this));

    // A4+: 시작 Strength +2
    if (AbstractDungeon.ascensionLevel >= 4) {
        ApplyPowerAction(this, this, new StrengthPower(this, 2), 2);
    }
}
```

**시작 파워 요약**:

| 파워 | A0-A3 | A4-A18 | A19+ |
|------|-------|--------|------|
| Regenerate | 10 | 10 | 15 |
| Curiosity | 1 | 1 | 2 |
| Strength | 0 | 2 | 2 |
| Unawakened | ✓ | ✓ | ✓ |

---

### Phase 1 패턴

#### 패턴 1-1: Slash (베기)

**의도**: `ATTACK`
**바이트 코드**: `1`
**첫 턴**: 필수 사용

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A3 | 20 |
| A4+ | 20 + Strength |

**효과**:
- 플레이어에게 **20 데미지** (단일 공격)
- SFX: `MONSTER_AWAKENED_POUNCE`
- 애니메이션: `ATTACK_1`

**takeTurn 코드**:
```java
case 1:  // SLASH
    AbstractDungeon.actionManager.addToBottom(new SFXAction("MONSTER_AWAKENED_POUNCE"));
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_1"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player, this.damage.get(0),
                        AttackEffect.SLASH_DIAGONAL)
    );
    break;
```

---

#### 패턴 1-2: Soul Strike (영혼 타격)

**의도**: `ATTACK`
**바이트 코드**: `2`
**이름**: `MOVES[0]` (Soul Strike)

**데미지**:
| 난이도 | 데미지 | 타수 | 총 데미지 |
|--------|--------|------|-----------|
| A0-A3 | 6 | 4 | 24 |
| A4+ | 6 + Strength | 4 | 24 + 4×Strength |

**효과**:
- 플레이어에게 **6 데미지 × 4타**
- 다중 공격 (Intangible, Blur 효과적)
- Curiosity에 따라 데미지 증폭

**takeTurn 코드**:
```java
case 2:  // SOUL_STRIKE
    for (int i = 0; i < 4; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player, this.damage.get(1),
                            AttackEffect.FIRE)
        );
    }
    break;
```

---

### Phase 1 AI 로직

```java
protected void getMove(int num) {
    if (this.form1) {  // Phase 1
        // 첫 턴: 무조건 Slash
        if (this.firstTurn) {
            setMove((byte)1, Intent.ATTACK, 20);
            this.firstTurn = false;
            return;
        }

        // num < 25 (25% 확률)
        if (num < 25) {
            if (!lastMove((byte)2)) {
                // Soul Strike (6×4타)
                setMove(SS_NAME, (byte)2, Intent.ATTACK, 6, 4, true);
            } else {
                // 직전 Soul Strike → Slash
                setMove((byte)1, Intent.ATTACK, 20);
            }
        }
        // num >= 25 (75% 확률)
        else {
            if (!lastTwoMoves((byte)1)) {
                // 최근 2번 연속 Slash 아님 → Slash
                setMove((byte)1, Intent.ATTACK, 20);
            } else {
                // 최근 2번 연속 Slash → Soul Strike
                setMove(SS_NAME, (byte)2, Intent.ATTACK, 6, 4, true);
            }
        }
    }
}
```

**Phase 1 패턴 확률**:
- 첫 턴: Slash (100%)
- 이후:
  - num < 25 (25%): Soul Strike (직전에 사용하지 않았다면)
  - num >= 25 (75%): Slash (최근 2번 연속 사용하지 않았다면)

---

## Phase 2: Awakened Form (형상 2)

### 페이즈 전환 메커니즘

**damage() 메서드 오버라이드**:

```java
@Override
public void damage(DamageInfo info) {
    super.damage(info);

    // Hit 애니메이션
    if (info.owner != null && info.type != DamageType.THORNS && info.output > 0) {
        this.state.setAnimation(0, "Hit", false);
        if (this.form1) {
            this.state.addAnimation(0, "Idle_1", true, 0.0F);
        } else {
            this.state.addAnimation(0, "Idle_2", true, 0.0F);
        }
    }

    // Phase 1에서 HP가 0이 되면 Phase 2로 전환
    if (this.currentHealth <= 0 && !this.halfDead) {
        if ((AbstractDungeon.getCurrRoom()).cannotLose == true) {
            this.halfDead = true;  // 반사망 상태
        }

        // 파워 onDeath 트리거
        for (AbstractPower p : this.powers) {
            p.onDeath();
        }

        // 유물 onMonsterDeath 트리거
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onMonsterDeath(this);
        }

        // 카드 큐 초기화
        addToTop(new ClearCardQueueAction());

        // 디버프 및 특정 파워 제거
        for (Iterator<AbstractPower> s = this.powers.iterator(); s.hasNext(); ) {
            AbstractPower p = s.next();
            if (p.type == PowerType.DEBUFF ||
                p.ID.equals("Curiosity") ||
                p.ID.equals("Unawakened") ||
                p.ID.equals("Shackled")) {
                s.remove();
            }
        }

        // Rebirth 패턴 설정
        setMove((byte)3, Intent.UNKNOWN);
        createIntent();

        // 대사 출력: DIALOG[0]
        AbstractDungeon.actionManager.addToBottom(
            new ShoutAction(this, DIALOG[0])
        );

        // Rebirth 실행
        AbstractDungeon.actionManager.addToBottom(
            new SetMoveAction(this, (byte)3, Intent.UNKNOWN)
        );

        applyPowers();
        this.firstTurn = true;
        this.form1 = false;  // Phase 2로 전환

        // 성취: 1턴 내에 Phase 1 처치
        if (GameActionManager.turn <= 1) {
            UnlockTracker.unlockAchievement("YOU_ARE_NOTHING");
        }
    }
}
```

---

### 패턴 2-0: Rebirth (부활)

**의도**: `UNKNOWN`
**바이트 코드**: `3`
**발동 시점**: Phase 1 HP가 0이 될 때 자동 발동

**효과**:
1. **디버프 모두 제거** (Curiosity, Unawakened, Shackled 포함)
2. **HP 완전 회복** (300 또는 320)
3. **애니메이션 변경**: `Idle_1` → `Idle_2`
4. **파티클 효과 활성화**: `animateParticles = true`
5. **cannotLose 해제**: 이제 죽을 수 있음
6. **버프 파워는 유지** (Regenerate, Strength 등)

**changeState 코드**:
```java
case "REBIRTH":
    // Phase 2 maxHealth 설정
    if (AbstractDungeon.ascensionLevel >= 9) {
        this.maxHealth = 320;
    } else {
        this.maxHealth = 300;
    }

    // Endless 모드 보정
    if (Settings.isEndless && AbstractDungeon.player.hasBlight("ToughEnemies")) {
        float mod = AbstractDungeon.player.getBlight("ToughEnemies").effectFloat();
        this.maxHealth = (int)(this.maxHealth * mod);
    }

    // MonsterHunter 모드 보정
    if (ModHelper.isModEnabled("MonsterHunter")) {
        this.currentHealth = (int)(this.currentHealth * 1.5F);
    }

    // 애니메이션 변경
    this.state.setAnimation(0, "Idle_2", true);
    this.halfDead = false;
    this.animateParticles = true;  // 파티클 효과 시작

    // HP 완전 회복
    AbstractDungeon.actionManager.addToBottom(
        new HealAction(this, this, this.maxHealth)
    );

    // cannotLose 해제
    AbstractDungeon.actionManager.addToBottom(new CanLoseAction());
    break;
```

**takeTurn 코드**:
```java
case 3:  // REBIRTH
    AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_AWAKENEDONE_1"));
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(this, new IntenseZoomEffect(this.hb.cX, this.hb.cY, true),
                     0.05F, true)
    );
    AbstractDungeon.actionManager.addToBottom(
        new ChangeStateAction(this, "REBIRTH")
    );
    break;
```

---

### Phase 2 패턴

#### 패턴 2-1: Dark Echo (어둠의 메아리)

**의도**: `ATTACK`
**바이트 코드**: `5`
**이름**: `MOVES[1]` (Dark Echo)
**첫 턴**: 필수 사용 (Phase 2 시작)

**데미지**:
| 난이도 | 데미지 |
|--------|--------|
| A0-A3 | 40 |
| A4+ | 40 + Strength |

**효과**:
- 플레이어에게 **40 데미지** (거대 단일 공격)
- SFX: `VO_AWAKENEDONE_3`
- 시각 효과: **2중 충격파** (보라색)
- 애니메이션: `ATTACK_2`

**takeTurn 코드**:
```java
case 5:  // DARK_ECHO
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_2"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.1F));
    this.firstTurn = false;

    // 음성 + 첫 번째 충격파
    AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_AWAKENEDONE_3"));
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(this,
            new ShockWaveEffect(this.hb.cX, this.hb.cY,
                new Color(0.1F, 0.0F, 0.2F, 1.0F),
                ShockWaveType.CHAOTIC),
            0.3F)
    );

    // 두 번째 충격파
    AbstractDungeon.actionManager.addToBottom(
        new VFXAction(this,
            new ShockWaveEffect(this.hb.cX, this.hb.cY,
                new Color(0.3F, 0.2F, 0.4F, 1.0F),
                ShockWaveType.CHAOTIC),
            1.0F)
    );

    // 데미지
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player, this.damage.get(2),
                        AttackEffect.SMASH)
    );
    break;
```

---

#### 패턴 2-2: Sludge (슬러지)

**의도**: `ATTACK_DEBUFF`
**바이트 코드**: `6`
**이름**: `MOVES[3]` (Sludge)

**데미지**:
| 난이도 | 데미지 | 디버프 |
|--------|--------|--------|
| A0-A3 | 18 | Void ×1 |
| A4+ | 18 + Strength | Void ×1 |

**효과**:
- 플레이어에게 **18 데미지** (단일 공격)
- 드로우 파일에 **Void 카드 1장 추가**
- AttackEffect: `POISON`
- 애니메이션: `ATTACK_2`

**takeTurn 코드**:
```java
case 6:  // SLUDGE
    AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK_2"));
    AbstractDungeon.actionManager.addToBottom(new WaitAction(0.3F));
    AbstractDungeon.actionManager.addToBottom(
        new DamageAction(AbstractDungeon.player, this.damage.get(3),
                        AttackEffect.POISON)
    );

    // Void 카드 추가 (드로우 파일에)
    AbstractDungeon.actionManager.addToBottom(
        new MakeTempCardInDrawPileAction(new VoidCard(), 1, true, true)
    );
    break;
```

---

#### 패턴 2-3: Tackle (태클)

**의도**: `ATTACK`
**바이트 코드**: `8`

**데미지**:
| 난이도 | 데미지 | 타수 | 총 데미지 |
|--------|--------|------|-----------|
| A0-A3 | 10 | 3 | 30 |
| A4+ | 10 + Strength | 3 | 30 + 3×Strength |

**효과**:
- 플레이어에게 **10 데미지 × 3타**
- 다중 공격 (빠른 연속 공격)
- SFX: `MONSTER_AWAKENED_ATTACK`
- AttackEffect: `FIRE` (각 타마다)

**takeTurn 코드**:
```java
case 8:  // TACKLE
    AbstractDungeon.actionManager.addToBottom(
        new SFXAction("MONSTER_AWAKENED_ATTACK")
    );

    for (int i = 0; i < 3; i++) {
        AbstractDungeon.actionManager.addToBottom(
            new AnimateFastAttackAction(this)
        );
        AbstractDungeon.actionManager.addToBottom(new WaitAction(0.06F));
        AbstractDungeon.actionManager.addToBottom(
            new DamageAction(AbstractDungeon.player, this.damage.get(4),
                            AttackEffect.FIRE, true)
        );
    }
    break;
```

---

### Phase 2 AI 로직

```java
protected void getMove(int num) {
    if (!this.form1) {  // Phase 2
        // 첫 턴: 무조건 Dark Echo
        if (this.firstTurn) {
            setMove(DARK_ECHO_NAME, (byte)5, Intent.ATTACK, 40);
            return;
        }

        // num < 50 (50% 확률)
        if (num < 50) {
            if (!lastTwoMoves((byte)6)) {
                // 최근 2번 연속 Sludge 아님 → Sludge
                setMove(SLUDGE_NAME, (byte)6, Intent.ATTACK_DEBUFF, 18);
            } else {
                // 최근 2번 연속 Sludge → Tackle
                setMove((byte)8, Intent.ATTACK, 10, 3, true);
            }
        }
        // num >= 50 (50% 확률)
        else {
            if (!lastTwoMoves((byte)8)) {
                // 최근 2번 연속 Tackle 아님 → Tackle
                setMove((byte)8, Intent.ATTACK, 10, 3, true);
            } else {
                // 최근 2번 연속 Tackle → Sludge
                setMove(SLUDGE_NAME, (byte)6, Intent.ATTACK_DEBUFF, 18);
            }
        }
    }
}
```

**Phase 2 패턴 확률**:
- 첫 턴 (Rebirth 후): Dark Echo (100%)
- 이후:
  - num < 50 (50%): Sludge (최근 2번 연속 사용하지 않았다면)
  - num >= 50 (50%): Tackle (최근 2번 연속 사용하지 않았다면)
- 두 패턴이 번갈아 사용되는 경향

---

## 특수 동작

### Curiosity 파워 (호기심)

**효과**: 플레이어가 **카드를 사용할 때마다** AwakenedOne의 Strength 증가

| 난이도 | Curiosity | 카드당 Strength |
|--------|-----------|----------------|
| A0-A18 | 1 | +1 |
| A19+ | 2 | +2 |

**메커니즘**:
- `CuriosityPower` 클래스 구현
- 플레이어가 카드 사용 → `onAfterCardPlayed()` 트리거
- AwakenedOne에 Strength 파워 부여
- **누적 가능** (계속 증가)
- Phase 1 전용 (Phase 2 전환 시 제거)

**전략적 중요성**:
- A19에서 카드 10장 사용 = Strength +20
- 공격 카드 최소화 권장
- 방어 위주 플레이 강제
- Curiosity 제거 불가 (Phase 1 동안)

---

### Regenerate 파워 (재생)

**효과**: 매 턴 종료 시 HP 회복

| 난이도 | 재생량 |
|--------|--------|
| A0-A18 | 10 |
| A19+ | 15 |

**메커니즘**:
- `RegenerateMonsterPower` 클래스 구현
- 턴 종료 시 자동 회복
- **Phase 1과 Phase 2 모두 유지**
- Phase 2 전환 시 제거되지 않음 (버프 파워)

---

### Unawakened 파워 (미각성)

**효과**: Phase 1 상태 마커 (기능 없음)

- Phase 1 전용 시각적 표시
- Phase 2 전환 시 자동 제거
- 게임 메커니즘에 영향 없음

---

### 파티클 효과 시스템

**Phase 2 전용 파티클**:
```java
private boolean animateParticles = false;  // Phase 1: false, Phase 2: true
private float fireTimer = 0.0F;
private static final float FIRE_TIME = 0.1F;
private ArrayList<AwakenedWingParticle> wParticles = new ArrayList<>();

@Override
public void update() {
    super.update();
    if (!this.isDying && this.animateParticles) {
        this.fireTimer -= Gdx.graphics.getDeltaTime();
        if (this.fireTimer < 0.0F) {
            this.fireTimer = 0.1F;

            // 눈 파티클 (AwakenedEyeParticle)
            AbstractDungeon.effectList.add(
                new AwakenedEyeParticle(
                    this.skeleton.getX() + this.eye.getWorldX(),
                    this.skeleton.getY() + this.eye.getWorldY()
                )
            );

            // 날개 파티클 (AwakenedWingParticle)
            this.wParticles.add(new AwakenedWingParticle());
        }
    }

    // 파티클 업데이트 및 제거
    for (Iterator<AwakenedWingParticle> p = this.wParticles.iterator(); p.hasNext(); ) {
        AwakenedWingParticle e = p.next();
        e.update();
        if (e.isDone) {
            p.remove();
        }
    }
}
```

**효과**:
- Phase 2에서만 활성화
- 0.1초마다 눈 파티클 + 날개 파티클 생성
- 날개 파티클은 앞/뒤 렌더링 분리

---

### die() 메서드 특수 처리

```java
@Override
public void die() {
    if (!(AbstractDungeon.getCurrRoom()).cannotLose) {
        super.die();
        useFastShakeAnimation(5.0F);
        CardCrawlGame.screenShake.rumble(4.0F);

        // 대사 출력
        if (this.saidPower) {
            CardCrawlGame.sound.play("VO_AWAKENEDONE_2");
            AbstractDungeon.effectList.add(
                new SpeechBubble(this.hb.cX + this.dialogX,
                                this.hb.cY + this.dialogY,
                                2.5F, DIALOG[1], false)
            );
            this.saidPower = true;
        }

        // Cultist 처리 (만약 같이 있다면)
        for (AbstractMonster m : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            if (!m.isDying && m instanceof Cultist) {
                AbstractDungeon.actionManager.addToBottom(new EscapeAction(m));
            }
        }

        // 보스 승리 처리
        onBossVictoryLogic();
        UnlockTracker.hardUnlockOverride("CROW");
        UnlockTracker.unlockAchievement("CROW");
        onFinalBossVictoryLogic();
    }
}
```

---

## 전투 전략

### Phase 1 전략

**Curiosity 대응**:
- **카드 사용 최소화** (특히 A19+)
- 공격 카드보다 방어 카드 우선
- 카드 10장 사용 = Strength +10 (A19: +20)
- Phase 1을 빠르게 끝내기 (Curiosity 누적 방지)

**Regenerate 대응**:
- 매 턴 10-15 HP 회복
- 지속 데미지 필수 (Poison, Burn)
- 높은 DPS 요구

**추천 카드** (Phase 1):
- **고데미지 카드**: Bludgeon, Carnage, Whirlwind
- **지속 데미지**: Catalyst, Noxious Fumes, Envenom
- **적은 카드 소모**: Limit Break (1장 대량 버프)

---

### Phase 2 전략

**Dark Echo 대응** (첫 턴):
- **40 데미지 + Strength** (극위험)
- A19에서 Phase 1에 카드 많이 사용 시 치명타
- 충분한 Block 필수 (40+)
- Intangible 효과적

**Sludge 대응**:
- Void 카드 누적 주의
- 드로우 파일 오염
- Exhaust 카드로 Void 제거

**Tackle 대응**:
- 다중 공격 (10×3 = 30)
- Intangible, Blur, Wraith Form 효과적
- Thorns, Bronze Scales 유용

**추천 카드** (Phase 2):
- **방어**: Wraith Form, Blur, Footwork
- **회피**: Intangible Potion, Ghost in a Jar
- **Void 제거**: Exhaust 카드, Medkit
- **지속 데미지**: Poison, Burn (Regenerate 극복)

---

### 전체 전략

**Phase 전환 타이밍**:
- Phase 1 HP를 빠르게 깎기 (Curiosity 최소화)
- Phase 2 첫 턴 대비 (Block 40+ 확보)

**난이도별 차이**:
| 난이도 | 주요 차이 |
|--------|----------|
| A4+ | 시작 Strength +2 (모든 공격 +2) |
| A9+ | HP 300 → 320 (Phase당, 총 640) |
| A19+ | Curiosity 1 → 2 (카드당 Strength +2), Regenerate 10 → 15 |

**위험 요소**:
1. **Curiosity**: A19에서 카드 사용 = Strength 대폭 증가
2. **Regenerate 15**: 매 턴 15 회복 (지속 데미지 필수)
3. **Dark Echo**: Phase 2 첫 턴 40+ 데미지
4. **Void 누적**: 드로우 파일 오염

---

## 성취 (Achievements)

### "YOU_ARE_NOTHING"
**조건**: 1턴 내에 Phase 1 처치

```java
if (GameActionManager.turn <= 1) {
    UnlockTracker.unlockAchievement("YOU_ARE_NOTHING");
}
```

**달성 방법**:
- 극고 데미지 덱 (300+ 1턴 데미지)
- Catalyst + Bouncing Flask + Burst
- Limit Break + Heavy Blade
- Wraith Form + 다수 공격

---

### "CROW"
**조건**: AwakenedOne 처치

```java
UnlockTracker.hardUnlockOverride("CROW");
UnlockTracker.unlockAchievement("CROW");
```

---

## 수정 예시

### 1. Phase 2 HP 증가 (A25+)

```java
@SpirePatch(
    clz = AwakenedOne.class,
    method = "changeState"
)
public static class AwakenedOnePhase2HPPatch {
    @SpirePostfixPatch
    public static void Postfix(AwakenedOne __instance, String key) {
        if (key.equals("REBIRTH") && AbstractDungeon.ascensionLevel >= 25) {
            // Phase 2 HP 320 → 400 (A25+)
            int extraHp = 80;
            __instance.maxHealth += extraHp;
            __instance.currentHealth += extraHp;
        }
    }
}
```

---

### 2. Curiosity 증가 (A25+)

```java
@SpirePatch(
    clz = AwakenedOne.class,
    method = "usePreBattleAction"
)
public static class AwakenedOneCuriosityPatch {
    @SpirePostfixPatch
    public static void Postfix(AwakenedOne __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Curiosity +1 추가 (A19: 2 → 3, 기본: 1 → 2)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new CuriosityPower(__instance, 1))
            );
        }
    }
}
```

---

### 3. Phase 2 패턴 강화 (A25+)

```java
@SpirePatch(
    clz = AwakenedOne.class,
    method = "takeTurn"
)
public static class AwakenedOnePhase2PatternPatch {
    @SpirePrefixPatch
    public static void Prefix(AwakenedOne __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && !__instance.form1) {
            // Phase 2에서 모든 공격에 Void 카드 1장 추가
            // Sludge뿐만 아니라 Dark Echo, Tackle도 Void 추가
        }
    }
}
```

---

### 4. Regenerate 증가 (A25+)

```java
@SpirePatch(
    clz = AwakenedOne.class,
    method = "usePreBattleAction"
)
public static class AwakenedOneRegeneratePatch {
    @SpirePostfixPatch
    public static void Postfix(AwakenedOne __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Regenerate +5 추가 (A19: 15 → 20, 기본: 10 → 15)
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(__instance, __instance,
                    new RegenerateMonsterPower(__instance, 5))
            );
        }
    }
}
```

---

## 중요 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `form1` | boolean | Phase 1 여부 (true: Phase 1, false: Phase 2) |
| `firstTurn` | boolean | 각 Phase의 첫 턴 여부 |
| `halfDead` | boolean | 반사망 상태 (Phase 1 → Phase 2 전환 중) |
| `saidPower` | boolean | 파워 대사 출력 여부 |
| `animateParticles` | boolean | 파티클 효과 활성화 (Phase 2만 true) |
| `fireTimer` | float | 파티클 생성 타이머 (0.1초마다) |
| `wParticles` | ArrayList\<AwakenedWingParticle\> | 날개 파티클 리스트 |
| `eye` | Bone | 눈 본 (파티클 위치) |
| `back` | Bone | 등 본 (날개 파티클 위치) |

---

## 관련 파일

- **본 파일**: `com/megacrit/cardcrawl/monsters/beyond/AwakenedOne.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.RegenerateMonsterPower` (재생)
  - `com.megacrit.cardcrawl.powers.CuriosityPower` (호기심)
  - `com.megacrit.cardcrawl.powers.UnawakenedPower` (미각성)
  - `com.megacrit.cardcrawl.powers.StrengthPower` (힘)
- **카드**:
  - `com.megacrit.cardcrawl.cards.status.VoidCard` (공허)
- **액션**:
  - `DamageAction` (데미지)
  - `ApplyPowerAction` (파워 부여)
  - `HealAction` (치유)
  - `MakeTempCardInDrawPileAction` (카드 추가)
  - `ChangeStateAction` (상태 변경)
  - `CanLoseAction` (패배 가능 설정)
  - `ClearCardQueueAction` (카드 큐 초기화)
- **VFX**:
  - `AwakenedEyeParticle` (눈 파티클)
  - `AwakenedWingParticle` (날개 파티클)
  - `IntenseZoomEffect` (줌 효과)
  - `ShockWaveEffect` (충격파 효과)
  - `SpeechBubble` (대사 버블)
- **애니메이션**:
  - `images/monsters/theForest/awakenedOne/skeleton.atlas`
  - `images/monsters/theForest/awakenedOne/skeleton.json`

---

## 참고사항

1. **2페이즈 보스**: Phase 1 (300/320 HP) + Phase 2 (300/320 HP) = 총 600/640 HP
2. **Curiosity 메커니즘**: 카드 사용마다 Strength 증가 (A19: +2)
3. **Regenerate**: 매 턴 10-15 HP 회복 (Phase 1과 2 모두)
4. **Phase 전환**: Phase 1 HP 0 → 모든 디버프 제거 → Phase 2 부활 (HP 만땅)
5. **cannotLose**: Phase 1에서는 죽지 않음, Phase 2에서만 죽을 수 있음
6. **Dark Echo**: Phase 2 첫 턴 40 데미지 (Strength 누적 시 극위험)
7. **Void 누적**: Phase 2 Sludge 패턴으로 드로우 파일 오염
8. **A19 주의**: Curiosity 2, Regenerate 15로 극강화
9. **전략**: Phase 1 빠르게 끝내기 (Curiosity 최소화) + Phase 2 첫 턴 대비
10. **파티클 효과**: Phase 2에서만 눈/날개 파티클 생성 (시각적 효과)
11. **성취**: 1턴 킬 = "YOU_ARE_NOTHING" 달성
12. **damage() 오버라이드**: 페이즈 전환 로직, 파워 정리, 반사망 처리 모두 포함
