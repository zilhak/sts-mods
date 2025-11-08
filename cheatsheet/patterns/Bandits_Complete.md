# 도적단 (Bandits) - 완전 가이드

> **검증 완료** (2025-01-08): 디컴파일 소스 대조 검증 완료
> **주요 수정**:
> - BanditBear: 50/50 랜덤 패턴 → 고정 순환 패턴 (Bear Hug → Lunge ↔ Maul)
> - BanditLeader: Strength 버퍼 → Weak 디버퍼 (Mock은 대사만, 버프 없음)
> - BanditPointy: HP 값 수정 (30/34), 데미지 수정 (5-6×2)

## 개요

**등장 지역**: 2막 (The City)
**인카운터 타입**: 멀티 몬스터 (3마리 동시 등장)
**구성원**:
- Bear (곰 도적): 디버퍼 + 딜러, Dexterity 감소
- Pointy (꼬챙이 도적): 딜러, 다중 공격
- Leader (우두머리): 디버퍼 + 딜러, Weak 부여

**특징**: 디버프 중심, 고정 패턴, 처치 순서 중요

---

# 1. 곰 도적 (Bear)

## 기본 정보

**클래스명**: `BanditBear`
**ID**: `"BanditBear"`
**타입**: 하이브리드 (디버퍼 + 딜러)
**역할**: Dexterity 디버프, 순환 공격

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 38-42 |
| A7+ | 40-44 |

---

## 패턴 정보

### 패턴 1: 곰 포옹 (Bear Hug) - 첫 턴 전용

**의도**: `STRONG_DEBUFF`
**효과**: Dexterity 감소

**발동 확률**: 첫 턴 무조건 사용

**Dexterity 감소량**:
| 난이도 | Dexterity 감소 |
|--------|----------------|
| 기본 (A0-A16) | -2 |
| A17+ | -4 |

**효과**:
- 플레이어의 **Dexterity -2** (A17+: -4)
- Block 카드 효율 감소
- 전투 중 1회만 사용

---

### 패턴 2: 돌진 (Lunge)

**의도**: `ATTACK_DEFEND`
**데미지**: 9 (A2+: 10)
**Block**: 9

**발동 확률**: Bear Hug 이후 순환

**효과**:
- 플레이어에게 **9 데미지** 공격 (A2+: 10)
- 자신에게 **Block 9** 부여

---

### 패턴 3: 망치질 (Maul)

**의도**: `ATTACK`
**데미지**: 18 (A2+: 20)

**발동 확률**: Lunge 이후 순환

**효과**:
- 플레이어에게 **18 데미지** 공격 (A2+: 20)
- 강력한 단일 타격

---

## AI 로직

**패턴 선택**:
```java
protected void getMove(int num) {
    // 첫 턴: Bear Hug (STRONG_DEBUFF)
    setMove((byte)2, Intent.STRONG_DEBUFF);
}

// takeTurn()에서 순환 설정:
// Bear Hug → Lunge → Maul → Lunge → Maul → ...
case 2: // Bear Hug
    applyDexDebuff();
    setMove((byte)3); // 다음: Lunge
    break;
case 3: // Lunge
    attack + gainBlock();
    setMove((byte)1); // 다음: Maul
    break;
case 1: // Maul
    heavyAttack();
    setMove((byte)3); // 다음: Lunge
    break;
```

**패턴 순서**:
1. **턴 1**: Bear Hug (Dexterity -2/-4)
2. **턴 2**: Lunge (9-10 dmg + 9 block)
3. **턴 3**: Maul (18-20 dmg)
4. **턴 4**: Lunge (9-10 dmg + 9 block)
5. **턴 5**: Maul (18-20 dmg)
6. 2-3-4-5 반복...

---

## 특징

- **디버퍼 역할**: Dexterity 감소로 방어 효율 저하
- **고정 순환**: Lunge ↔ Maul 교대
- **자체 생존**: Block 9로 내구력 확보
- **우선순위**: 중간 (Leader 후, Pointy 전)

---

# 2. 꼬챙이 도적 (Pointy)

## 기본 정보

**클래스명**: `BanditPointy`
**ID**: `"BanditPointy"`
**타입**: 딜러
**역할**: 높은 공격력, 다중 타격

---

## HP 정보

| 난이도 | HP |
|--------|---------|
| 기본 (A0-A6) | 30 |
| A7+ | 34 |

---

## 패턴 정보

### 패턴 1: 찌르기 (Stab)

**의도**: `ATTACK`
**데미지**: 5 x 2회 (A2+: 6 x 2회)

**발동 확률**: 100%

**효과**:
- 플레이어에게 **5 데미지 x 2회** 공격 (A2+: 6 x 2회)
- 총 10 데미지 (A2+: 12)
- **매 턴 반복**

**코드 특징**:
```java
// 2타 연속 공격
multiDamage = 5 (A2+: 6)
attackCount = 2
// 단일 패턴만 사용
```

---

## AI 로직

**패턴 선택**:
```java
// 항상 Stab 사용
protected void getMove(int num) {
    setMove((byte)1, Intent.ATTACK, damage.get(0).base, 2, true);
}
```

---

## 특징

- **딜러 역할**: 지속적인 공격
- **다중 공격**: Plated Armor, Thorns 취약
- **낮은 HP**: 가장 빠르게 처치 가능
- **우선순위**: 낮음 (Bear 후 처치)

---

# 3. 우두머리 (Leader)

## 기본 정보

**클래스명**: `BanditLeader`
**ID**: `"BanditLeader"`
**타입**: 디버퍼 + 딜러
**역할**: 도발 + Weak 부여 + 공격

---

## HP 정보

| 난이도 | HP 범위 |
|--------|---------|
| 기본 (A0-A6) | 35-39 |
| A7+ | 37-41 |

---

## 패턴 정보

### 패턴 1: 조롱 (Mock) - 첫 턴 전용

**의도**: `UNKNOWN`
**효과**: 대사만 출력

**발동 확률**: 첫 턴 무조건 사용

**효과**:
- **아무 효과 없음** (대사만 출력)
- Bear 생존 여부에 따라 대사 변경
- **전투 중 1회만 사용**

**코드 특징**:
```java
case 2: // MOCK
    // Bear 생존 여부 확인
    if (bearAlive) {
        TalkAction(DIALOG[0]); // "얘들아, 저놈 잡아라!"
    } else {
        TalkAction(DIALOG[1]); // "이런 제기랄!"
    }
    setMove((byte)3); // 다음: Agonizing Slash
    break;
```

---

### 패턴 2: 고통스러운 베기 (Agonizing Slash)

**의도**: `ATTACK_DEBUFF`
**데미지**: 10 (A2+: 12)
**Weak**: 2 (A17+: 3)

**발동 확률**: Mock 이후 순환

**Weak 부여량**:
| 난이도 | Weak |
|--------|------|
| 기본 (A0-A16) | 2 |
| A17+ | 3 |

**효과**:
- 플레이어에게 **10 데미지** 공격 (A2+: 12)
- 플레이어에게 **Weak 2** 부여 (A17+: 3)

---

### 패턴 3: 교차 베기 (Cross Slash)

**의도**: `ATTACK`
**데미지**: 15 (A2+: 17)

**발동 확률**: Agonizing Slash 이후 순환

**효과**:
- 플레이어에게 **15 데미지** 공격 (A2+: 17)
- 강력한 단일 타격

---

## AI 로직

**패턴 선택**:
```java
protected void getMove(int num) {
    // 첫 턴: Mock (UNKNOWN)
    setMove((byte)2, Intent.UNKNOWN);
}

// takeTurn()에서 순환 설정:
// Mock → Agonizing Slash → Cross Slash → ...
case 2: // Mock
    dialogue();
    setMove((byte)3); // 다음: Agonizing Slash
    break;
case 3: // Agonizing Slash
    attack + applyWeak();
    setMove((byte)1); // 다음: Cross Slash
    break;
case 1: // Cross Slash
    attack();
    // A17+: Cross Slash 2연속 가능
    if (A17+ && !lastTwoMoves((byte)1)) {
        setMove((byte)1); // Cross Slash 반복
    } else {
        setMove((byte)3); // Agonizing Slash
    }
    break;
```

**패턴 순서 (A0-A16)**:
1. **턴 1**: Mock (대사만)
2. **턴 2**: Agonizing Slash (10-12 dmg + Weak 2)
3. **턴 3**: Cross Slash (15-17 dmg)
4. **턴 4**: Agonizing Slash (10-12 dmg + Weak 2)
5. 2-3-4 반복...

**패턴 순서 (A17+)**:
1. **턴 1**: Mock (대사만)
2. **턴 2**: Agonizing Slash (12 dmg + Weak 3)
3. **턴 3**: Cross Slash (17 dmg)
4. **턴 4**: Cross Slash (17 dmg) - 연속 가능
5. **턴 5**: Agonizing Slash (12 dmg + Weak 3)
6. 2-5 반복...

---

## 특징

- **디버퍼 역할**: Weak 부여로 공격력 감소
- **공격 집중**: Mock 이후 지속 공격
- **A17+ 강화**: Weak 증가 + Cross Slash 연속 사용
- **우선순위**: 중간 (Weak 관리 필요)

---

# 멀티 인카운터 전략

## 구성 및 역할

```
Leader (디버퍼) → Bear (디버퍼) → Pointy (딜러)
  Weak 2-3       Dex -2/-4      5-6x2 공격
```

## 디버프 효과

### 첫 턴 (턴 1)

**Bear**: Dexterity -2 (A17+: -4)
- Block 카드 효율 감소
- 방어력 크게 저하

**Leader**: Mock (대사만, 효과 없음)
- 시간 벌기

**Pointy**: Stab 5-6 x2
- 즉시 데미지 시작

---

### 두 번째 턴부터

**Bear + Pointy 조합**:
- Lunge (9-10 dmg) + Stab (5-6 x2) = 19-22 dmg
- 또는 Maul (18-20 dmg) + Stab (5-6 x2) = 28-32 dmg

**Leader의 Weak 추가 시**:
- 플레이어 공격력 25% 감소
- 처치 속도 저하

---

## 처치 순서 전략

### 전략 1: Bear 우선 (추천)

**순서**: Bear → Pointy → Leader

**이유**:
1. Dexterity 디버프 조기 차단
2. 방어 효율 회복
3. Maul(18-20) 고데미지 차단

**장점**:
- Block 카드 정상 작동
- 안정적 방어 확보
- 고데미지 패턴 제거

**단점**:
- Weak 디버프는 지속
- Leader 처치 지연

---

### 전략 2: Pointy 우선 (속공)

**순서**: Pointy → Bear → Leader

**이유**:
1. 가장 낮은 HP (30-34)
2. 지속 데미지 감소
3. AOE 효율

**장점**:
- 빠른 처치로 데미지 감소
- AOE 카드 효율적
- 전투 단순화

**단점**:
- Dexterity 디버프 지속
- Bear Maul 위협 남음

---

### 전략 3: Leader 우선 (Weak 차단)

**순서**: Leader → Bear → Pointy

**이유**:
1. Weak 디버프 차단
2. 공격력 유지
3. 빠른 처치

**장점**:
- 공격 효율 유지
- Weak 관리 불필요

**단점**:
- Dexterity 디버프 지속
- Bear + Pointy 데미지 견뎌야 함

---

## 전투 전략

### 추천 카드

**AOE 카드**:
- Whirlwind
- Immolate
- Cleave

**단타 고데미지**:
- Heavy Blade (Bear Maul 전 처치)
- Bludgeon
- Perfected Strike

**디버프 대응**:
- Artifact (디버프 무효화)
- Orange Pellets (디버프 제거)
- Flex (Dexterity 회복)

---

### 위험 요소

**첫 턴**:
- Bear: Dexterity -2/-4 (방어 저하)
- Pointy: 10-12 데미지
- Leader: 대사만

**이후 턴**:
- Bear Maul: 18-20 데미지 (최대 위협)
- Bear Lunge: 9-10 데미지 + Block 9
- Pointy Stab: 10-12 데미지
- Leader 공격: 10-17 데미지
- Leader Weak: 공격력 25% 감소

**최악의 경우** (턴 3):
- Bear Maul: 18-20
- Pointy Stab: 10-12
- Leader Cross Slash: 15-17
- **총 43-49 데미지/턴**

---

### 카운터 전략

**Artifact / Orange Pellets**:
- Dexterity, Weak 디버프 차단/제거
- 전투 효율 유지

**AOE + 집중 공격**:
- Whirlwind로 전체 타격
- Bear 우선 집중 처치

**방어 우선**:
- 첫 턴 Block 충분히 확보
- Dexterity 디버프 대비
- 43-49 데미지 대비 준비

---

## 수정 예시

### 1. Bear Dexterity 디버프 강화 (A25+)

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.BanditBear",
    method = "takeTurn"
)
public static class BanditBearDebuffPatch {
    @SpirePostfixPatch
    public static void Postfix(BanditBear __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Dexterity -2 → -3 (A17: -4 → -6)
            int extraDebuff = (AbstractDungeon.ascensionLevel >= 17) ? -2 : -1;
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player, __instance,
                    new DexterityPower(AbstractDungeon.player, extraDebuff),
                    extraDebuff
                )
            );
        }
    }
}
```

### 2. Leader Weak 강화

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.BanditLeader",
    method = "takeTurn"
)
public static class BanditLeaderWeakPatch {
    @SpirePostfixPatch
    public static void Postfix(BanditLeader __instance) {
        if (AbstractDungeon.ascensionLevel >= 25 && __instance.nextMove == 3) {
            // Weak 2 → 3 (A17: 3 → 4)
            int extraWeak = 1;
            AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(
                    AbstractDungeon.player, __instance,
                    new WeakPower(AbstractDungeon.player, extraWeak, true),
                    extraWeak
                )
            );
        }
    }
}
```

### 3. Pointy 타수 증가

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.city.BanditPointy",
    method = "takeTurn"
)
public static class BanditPointyStabPatch {
    @SpirePostfixPatch
    public static void Postfix(BanditPointy __instance) {
        if (AbstractDungeon.ascensionLevel >= 25) {
            // Stab 2타 → 3타 추가
            AbstractDungeon.actionManager.addToBottom(
                new DamageAction(
                    AbstractDungeon.player,
                    __instance.damage.get(0),
                    AbstractGameAction.AttackEffect.SLASH_DIAGONAL
                )
            );
        }
    }
}
```

---

## 중요 필드

### BanditBear

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `maulDmg` | int | Maul 데미지 (18 또는 20) |
| `lungeDmg` | int | Lunge 데미지 (9 또는 10) |
| `con_reduction` | int | Dexterity 감소량 (-2 또는 -4) |

### BanditPointy

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `attackDmg` | int | Stab 데미지 (5 또는 6) |
| (고정) | int | Stab 타수 (2) |

### BanditLeader

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `slashDmg` | int | Cross Slash 데미지 (15 또는 17) |
| `agonizeDmg` | int | Agonizing Slash 데미지 (10 또는 12) |
| `weakAmount` | int | Weak 수치 (2 또는 3) |

---

## 관련 파일

- **Bear**: `com/megacrit/cardcrawl/monsters/city/BanditBear.java`
- **Pointy**: `com/megacrit/cardcrawl/monsters/city/BanditPointy.java`
- **Leader**: `com/megacrit/cardcrawl/monsters/city/BanditLeader.java`
- **파워**:
  - `com.megacrit.cardcrawl.powers.StrengthPower`
- **액션**:
  - `DamageAction`
  - `GainBlockAction`
  - `ApplyPowerAction`

---

## 참고사항

1. **Bear 우선**: Dexterity 디버프 차단이 전투의 핵심
2. **처치 순서**: Bear → Pointy → Leader (추천)
3. **AOE 유리**: Whirlwind, Immolate로 효율적 타격
4. **디버프 대응**: Artifact, Orange Pellets로 Dex/Weak 제거
5. **Bear 위협**: Maul 18-20 고데미지, Lunge Block 9
6. **Pointy 다중 공격**: 지속 데미지원, 낮은 HP
7. **Leader Mock**: 첫 턴은 대사만, 실제 위협 없음
8. **A17+ 강화**: Dex -4, Weak 3, Leader Cross Slash 연속 사용
