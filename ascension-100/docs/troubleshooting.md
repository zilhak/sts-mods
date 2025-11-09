# Ascension 100 에러 해결 가이드

ModTheSpire 패치 작업 중 발생한 주요 에러와 해결 방법을 정리한 문서입니다.

---

## 1. 오버로드 메서드 패치 에러

### 문제: getPotency() 패치 시 NullPointerException

**에러 메시지**:
```
java.lang.NullPointerException
    at com.evacipated.cardcrawl.modthespire.patcher.ParamInfo.findName(ParamInfo.java:21)
```

**발생 상황**:
- `AbstractPotion.getPotency(int ascensionLevel)` 메서드를 패치할 때
- `paramtypez = {int.class}` 사용 시 파라미터 이름 해석 실패

**원인**:
ModTheSpire가 오버로드된 메서드를 패치할 때, Postfix 메서드의 파라미터 이름을 바이트코드에서 찾으려고 시도합니다. 하지만 게임 JAR에는 디버그 정보(파라미터 이름)가 포함되어 있지 않아 NullPointerException이 발생합니다.

**잘못된 시도들**:

❌ **시도 1: 파라미터 제거**
```java
@SpirePatch(
    clz = AbstractPotion.class,
    method = "getPotency",
    paramtypez = {int.class}
)
public static int Postfix(int __result) {
    // NullPointerException 발생!
}
```

❌ **시도 2: 파라미터 추가 (구체적 이름)**
```java
@SpirePatch(
    clz = AbstractPotion.class,
    method = "getPotency",
    paramtypez = {int.class}
)
public static int Postfix(int __result, AbstractPotion __instance, int ascensionLevel) {
    // 여전히 NullPointerException - 'ascensionLevel' 이름을 찾을 수 없음
}
```

❌ **시도 3: 파라미터 추가 (간단한 이름)**
```java
@SpirePatch(
    clz = AbstractPotion.class,
    method = "getPotency",
    paramtypez = {int.class}
)
public static int Postfix(int __result, AbstractPotion __instance, int p) {
    // 여전히 NullPointerException - 파라미터 이름 해석 시도 자체가 문제
}
```

### 해결책: 파라미터 없는 오버로드 버전 패치

✅ **최종 해결 방법**:
```java
@SpirePatch(
    clz = AbstractPotion.class,
    method = "getPotency",
    paramtypez = {}  // 빈 배열로 파라미터 없는 버전 명시
)
public static class PotionPotencyReduction {
    @SpirePostfixPatch
    public static int Postfix(int __result) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 37) {
            return __result;
        }

        // 포션 효과 20% 감소
        return (int) Math.ceil(__result * 0.8f);
    }
}
```

**작동 원리**:
```java
// AbstractPotion 내부 구조
public int getPotency() {
    int potency = getPotency(AbstractDungeon.ascensionLevel);

    // Sacred Bark 유물 처리 등
    if (AbstractDungeon.player.hasRelic("SacredBark")) {
        potency *= 2;
    }
    return potency;
}

public abstract int getPotency(int ascensionLevel);
```

- `getPotency()` (파라미터 없음)는 wrapper 메서드
- 내부에서 `getPotency(int)`를 호출하고 결과를 반환
- **Postfix로 최종 결과를 수정**하므로 동일한 효과
- `paramtypez = {}`로 오버로드 명시 → 파라미터 이름 해석 불필요

**핵심 포인트**:
1. `paramtypez` 없음 → "Has overloads" 에러
2. `paramtypez = {int.class}` → "NullPointerException" (파라미터 이름 해석 실패)
3. **`paramtypez = {}`** → 파라미터 없는 버전 명시 ✅

---

## 2. 게임 초기화 중 NullPointerException

### 문제: AbstractDungeon.getCurrRoom() NPE

**에러 메시지**:
```
java.lang.NullPointerException
    at com.megacrit.cardcrawl.dungeons.AbstractDungeon.getCurrRoom(AbstractDungeon.java:809)
    at com.stsmod.ascension100.patches.levels.Level38$BossHealReduction.Prefix(Level38.java:44)
    at com.megacrit.cardcrawl.core.AbstractCreature.heal(AbstractCreature.java)
    at com.megacrit.cardcrawl.dungeons.AbstractDungeon.dungeonTransitionSetup(AbstractDungeon.java:3140)
```

**발생 상황**:
- 게임 시작 시 던전 초기화 중
- `heal()` 메서드가 호출되는데 던전이 아직 완전히 초기화되지 않음
- `AbstractDungeon.getCurrRoom()` 호출 시 NPE 발생

**원인**:
게임 초기화 순서:
1. `AbstractDungeon` 생성자 호출
2. `dungeonTransitionSetup()` 실행
3. 플레이어 체력 초기화 중 `heal()` 호출
4. **이 시점에 `currRoom` 등 필드가 아직 null**
5. `getCurrRoom()` 내부에서 NPE 발생

**잘못된 시도**:

❌ **시도 1: null 체크**
```java
if (AbstractDungeon.getCurrRoom() != null &&
    AbstractDungeon.getCurrRoom() instanceof VictoryRoom) {
    // getCurrRoom() 호출 자체에서 NPE 발생!
}
```

이 방법은 작동하지 않습니다. **null 체크를 하기 전에 메서드 호출 시점에 이미 에러가 발생**하기 때문입니다.

### 해결책: try-catch로 안전하게 처리

✅ **최종 해결 방법**:
```java
@SpirePatch(
    clz = AbstractCreature.class,
    method = "heal",
    paramtypez = {int.class, boolean.class}
)
public static class BossHealReduction {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, @ByRef int[] healAmount, boolean showEffect) {
        if (!AbstractDungeon.isAscensionMode || AbstractDungeon.ascensionLevel < 38) {
            return;
        }

        // Only apply to player
        if (!(__instance instanceof AbstractPlayer)) {
            return;
        }

        // try-catch로 안전하게 처리
        try {
            if (AbstractDungeon.getCurrRoom() instanceof VictoryRoom) {
                int originalAmount = healAmount[0];
                healAmount[0] = MathUtils.floor(healAmount[0] * 0.9f);

                logger.info(String.format(
                    "Ascension 38: Boss victory heal reduced from %d to %d (-10%%)",
                    originalAmount, healAmount[0]
                ));
            }
        } catch (NullPointerException e) {
            // 게임 초기화 중 NPE는 조용히 무시
            // 초기화 중에는 VictoryRoom이 아니므로 로직 실행 불필요
        }
    }
}
```

**작동 원리**:
- 게임 초기화 중: `getCurrRoom()` NPE 발생 → catch로 잡아서 무시
- 실제 게임 플레이 중: `getCurrRoom()` 정상 작동 → VictoryRoom 체크 성공
- 보스 승리 시: 회복량 감소 로직 정상 실행

**왜 이 방법이 안전한가?**
1. 게임 초기화 중에는 어차피 VictoryRoom이 아님
2. NPE를 잡아도 게임 로직에 영향 없음
3. 실제 보스 승리 시에는 정상 작동
4. 게임 크래시 방지

---

## 3. heal() 메서드 패치 대상 클래스 에러

### 문제: AbstractPlayer.heal() 메서드를 찾을 수 없음

**에러 메시지**:
```
java.lang.NoSuchMethodException: No method [heal(int, boolean)] found on class
[com.megacrit.cardcrawl.characters.AbstractPlayer]
```

**발생 상황**:
- 보스 승리 후 회복량을 감소시키려고 `AbstractPlayer.heal()` 패치 시도

**원인**:
`heal()` 메서드는 **AbstractPlayer가 아닌 AbstractCreature에 정의**되어 있습니다.

```java
// 클래스 계층 구조
AbstractCreature
  └─ AbstractPlayer extends AbstractCreature
  └─ AbstractMonster extends AbstractCreature
```

`heal()` 메서드는 AbstractCreature에 있고, AbstractPlayer와 AbstractMonster가 상속받아 사용합니다.

### 해결책: AbstractCreature 패치 + instanceof 체크

✅ **올바른 구현**:
```java
@SpirePatch(
    clz = AbstractCreature.class,  // AbstractPlayer 아님!
    method = "heal",
    paramtypez = {int.class, boolean.class}
)
public static class BossHealReduction {
    @SpirePrefixPatch
    public static void Prefix(AbstractCreature __instance, @ByRef int[] healAmount, boolean showEffect) {
        // 플레이어에게만 적용
        if (!(__instance instanceof AbstractPlayer)) {
            return;
        }

        // 보스 승리 회복량 감소 로직
        try {
            if (AbstractDungeon.getCurrRoom() instanceof VictoryRoom) {
                healAmount[0] = MathUtils.floor(healAmount[0] * 0.9f);
            }
        } catch (NullPointerException e) {
            // 초기화 중 NPE 무시
        }
    }
}
```

**핵심 포인트**:
1. **AbstractCreature를 패치 대상으로 지정**
2. `instanceof AbstractPlayer`로 플레이어만 필터링
3. 몬스터 회복에는 영향 없음

---

## 4. 중복 구현으로 인한 "no method body" 에러

### 문제: initializeLevelSpecificChances() 패치 실패

**에러 메시지**:
```
javassist.CannotCompileException: no method body
```

**발생 상황**:
- `AbstractDungeon.initializeLevelSpecificChances()` 패치 시도
- Level31.java, Level39.java에서 각각 시작 HP 감소 구현

**원인**:
`initializeLevelSpecificChances()` 메서드가 **abstract이거나 빈 구현**입니다. ModTheSpire는 실제 구현이 있는 메서드만 패치할 수 있습니다.

### 해결책: AbstractPlayer.initializeClass() 패치 + 중복 제거

✅ **올바른 구현**:

**PlayerStartingHPPatch.java** (통합 파일):
```java
@SpirePatch(
    clz = AbstractPlayer.class,
    method = "initializeClass"  // 올바른 메서드
)
public static class GameStartHPReduction {
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance) {
        if (!AbstractDungeon.isAscensionMode) {
            return;
        }

        int level = AbstractDungeon.ascensionLevel;

        // Level 31: -5 current HP (damage taken at start)
        if (level >= 31) {
            int reduction = 5;
            __instance.currentHealth = Math.max(1, __instance.currentHealth - reduction);

            logger.info(String.format(
                "Ascension 31: Starting current HP reduced by %d (current: %d/%d)",
                reduction, __instance.currentHealth, __instance.maxHealth
            ));
        }

        // Level 39: -5 max HP (permanent reduction)
        if (level >= 39) {
            int reduction = 5;
            __instance.decreaseMaxHealth(reduction);

            logger.info(String.format(
                "Ascension 39: Starting max HP reduced by %d (max: %d)",
                reduction, __instance.maxHealth
            ));
        }
    }
}
```

**핵심 포인트**:
1. ❌ `AbstractDungeon.initializeLevelSpecificChances()` - abstract/empty 메서드
2. ✅ `AbstractPlayer.initializeClass()` - 실제 구현이 있는 메서드
3. Level31.java, Level39.java 삭제 → PlayerStartingHPPatch.java로 통합
4. **중복 구현 제거**로 충돌 방지

**올바른 메서드 찾는 방법**:
- cheatsheet의 MaxHP.md 참고
- 기존 Ascension 패치 구현 확인 (Ascension 14 HP 감소)
- 디컴파일된 소스에서 실제 구현 확인

---

## 5. 빌드는 성공했는데 게임에서 에러가 계속 나는 경우

### 문제: 코드를 수정하고 빌드했는데 같은 에러 발생

**증상**:
```bash
./gradlew clean build
# BUILD SUCCESSFUL

# 하지만 게임 실행 시 같은 에러 발생
```

**원인**:
ModTheSpire는 **mods 폴더의 JAR 파일**을 로드합니다. 빌드한 JAR을 복사하지 않으면 이전 버전이 계속 실행됩니다.

### 해결책: 새 JAR 파일 복사

✅ **수동 복사**:
```bash
# 1. ModTheSpire mods 폴더로 이동
cd %LOCALAPPDATA%\ModTheSpire\mods

# 2. 기존 파일 완전히 삭제
del Ascension100.jar

# 3. 새 파일 복사
copy E:\workspace\sts-mods\build\libs\Ascension100.jar .
```

**또는 파일 탐색기에서**:
1. `C:\Users\[사용자명]\AppData\Local\ModTheSpire\mods\` 열기
2. 기존 `Ascension100.jar` **완전히 삭제**
3. `E:\workspace\sts-mods\build\libs\Ascension100.jar` 복사

**중요 사항**:
- 반드시 **기존 파일을 먼저 삭제**해야 함
- 덮어쓰기만 하면 파일 잠금 문제 발생 가능
- ModTheSpire를 재시작해야 새 JAR 로드됨

✅ **자동화 스크립트** (선택사항):
```batch
@echo off
REM deploy.bat
echo Deploying Ascension 100 mod...

REM Build
call gradlew clean build
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

REM Copy to mods folder
set MODS_DIR=%LOCALAPPDATA%\ModTheSpire\mods
echo Copying to %MODS_DIR%...

if exist "%MODS_DIR%\Ascension100.jar" (
    del "%MODS_DIR%\Ascension100.jar"
)

copy "build\libs\Ascension100.jar" "%MODS_DIR%\"

echo Deployment complete!
echo Please restart ModTheSpire to load the new version.
```

---

## 일반적인 디버깅 팁

### 1. 로그 확인하기

ModTheSpire 로그 위치:
```
%LOCALAPPDATA%\ModTheSpire\logs\
```

유용한 로그 파일:
- `mts.log` - ModTheSpire 메인 로그
- `errors.log` - 에러 전용 로그

### 2. 패치 검증하기

**체크리스트**:
- [ ] 패치 대상 클래스가 올바른가?
- [ ] 패치 대상 메서드가 실제로 존재하는가?
- [ ] 메서드 시그니처(파라미터 타입)가 정확한가?
- [ ] 오버로드된 메서드인 경우 `paramtypez` 지정했는가?
- [ ] Prefix/Postfix 메서드 시그니처가 올바른가?

### 3. cheatsheet 활용하기

`E:\workspace\sts-mods\cheatsheet\modify_guide\` 폴더의 문서들:
- `CurrentHP.md` - heal() 메서드 정보
- `MaxHP.md` - 최대 체력 관련 메서드
- `Potions.md` - 포션 관련 메서드
- 기타 게임 시스템별 가이드

### 4. 단계적 테스트

1. **최소한의 패치부터 시작**
   ```java
   @SpirePostfixPatch
   public static void Postfix() {
       System.out.println("Patch is working!");
   }
   ```

2. **조건 추가**
   ```java
   @SpirePostfixPatch
   public static void Postfix() {
       if (AbstractDungeon.isAscensionMode) {
           System.out.println("Ascension mode detected!");
       }
   }
   ```

3. **실제 로직 구현**
   ```java
   @SpirePostfixPatch
   public static void Postfix(AbstractPlayer __instance) {
       // 실제 게임 로직
   }
   ```

### 5. 일반적인 실수들

❌ **상대 경로 사용**
```java
// 잘못됨
clz = AbstractPlayer.class  // import 누락 시 컴파일 에러
```

✅ **전체 경로 또는 import**
```java
import com.megacrit.cardcrawl.characters.AbstractPlayer;
// 또는
clz = com.megacrit.cardcrawl.characters.AbstractPlayer.class
```

❌ **잘못된 파라미터 타입**
```java
paramtypez = {Integer.class}  // 잘못됨
```

✅ **기본 타입 사용**
```java
paramtypez = {int.class}  // 올바름
```

❌ **잘못된 Postfix 시그니처**
```java
public static void Postfix(int result)  // __result 아님
```

✅ **올바른 시그니처**
```java
public static int Postfix(int __result)  // 반환값이 있으면 return 필요
```

---

## 요약

| 문제 | 원인 | 해결책 |
|------|------|--------|
| getPotency() NPE | 파라미터 이름 해석 실패 | `paramtypez = {}` 사용 |
| getCurrRoom() NPE | 게임 초기화 중 호출 | try-catch로 안전하게 처리 |
| heal() NoSuchMethod | 잘못된 패치 대상 클래스 | AbstractCreature 패치 + instanceof |
| no method body | abstract/empty 메서드 패치 | 실제 구현이 있는 메서드 찾기 |
| 코드 수정 후 에러 지속 | 이전 JAR 로드 | 새 JAR 복사 후 재시작 |

**핵심 원칙**:
1. **cheatsheet 먼저 확인** - 올바른 메서드와 패턴 파악
2. **단계적 테스트** - 최소 구현부터 시작
3. **안전한 코드** - NPE 방지를 위한 방어적 프로그래밍
4. **로그 활용** - 디버깅 정보 충분히 남기기
5. **JAR 복사 확인** - 빌드 후 반드시 새 JAR 배포
