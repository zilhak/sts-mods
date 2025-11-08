# Slay the Spire Modding Cheatsheet - INDEX

이 문서는 Slay the Spire 모딩을 위한 종합 가이드입니다. 디컴파일된 게임 소스와 구현된 모드를 비교 분석하여 작성되었습니다.

## 📚 문서 구조

### 핵심 가이드
- **[INDEX.md](INDEX.md)** - 이 문서 (메인 진입점)
- **[PATCH_BASICS.md](PATCH_BASICS.md)** - SpirePatch 기본 개념
- **[ENEMY_LIST.md](ENEMY_LIST.md)** - 게임 내 모든 적 목록과 클래스명
- **[ENEMY_MODIFY.md](ENEMY_MODIFY.md)** - 적 능력치/패턴 수정 방법

### 고급 가이드
- **[MONSTER_HEALTH.md](MONSTER_HEALTH.md)** - 적 체력 수정 패턴
- **[MONSTER_DAMAGE.md](MONSTER_DAMAGE.md)** - 적 공격력 수정 패턴
- **[MONSTER_BEHAVIOR.md](MONSTER_BEHAVIOR.md)** - 적 행동 패턴 수정
- **[BOSS_MODIFICATIONS.md](BOSS_MODIFICATIONS.md)** - 보스 수정 가이드
- **[PLAYER_MODIFICATIONS.md](PLAYER_MODIFICATIONS.md)** - 플레이어 수정 (체력, 골드 등)

### 참고 자료
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - ascension-100 모드 구현 현황
- **[COMMON_PATTERNS.md](COMMON_PATTERNS.md)** - 자주 사용되는 패턴 모음

## 🎯 빠른 시작

### 1. 기본 패치 구조 이해
모든 수정은 `@SpirePatch` 어노테이션을 사용합니다:

```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.AbstractMonster",  // 패치할 클래스
    method = "init"                                           // 패치할 메서드
)
public static class YourPatchName {
    @SpirePostfixPatch  // 메서드 실행 후 추가 코드 실행
    public static void Postfix(AbstractMonster __instance) {
        // 수정 코드
    }
}
```

### 2. 적 체력 수정하기
```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "init")
public static class HealthPatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 21) {
            // 엘리트 적의 체력 10% 증가
            if (__instance.type == AbstractMonster.EnemyType.ELITE) {
                __instance.maxHealth = MathUtils.ceil(__instance.maxHealth * 1.1f);
                __instance.currentHealth = __instance.maxHealth;
            }
        }
    }
}
```

### 3. 적 공격력 수정하기
```java
@SpirePatch(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "usePreBattleAction")
public static class DamagePatch {
    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (AbstractDungeon.ascensionLevel >= 23) {
            // 모든 공격력 +2
            for (DamageInfo dmg : __instance.damage) {
                if (dmg != null && dmg.base > 0) {
                    dmg.base += 2;
                }
            }
        }
    }
}
```

### 4. 특정 적만 수정하기
```java
@SpirePatch(
    cls = "com.megacrit.cardcrawl.monsters.exordium.Cultist",
    method = SpirePatch.CONSTRUCTOR,
    paramtypez = { float.class, float.class, boolean.class }
)
public static class CultistPatch {
    @SpirePostfixPatch
    public static void Postfix(Cultist __instance, float x, float y, boolean talk) {
        // Cultist만 수정
        __instance.maxHealth += 10;
        __instance.currentHealth += 10;
    }
}
```

## 🔍 주요 개념

### 패치 타입
- **@SpirePostfixPatch** - 메서드 실행 **후** 코드 추가
- **@SpirePrefixPatch** - 메서드 실행 **전** 코드 추가
- **@SpireInsertPatch** - 메서드 **중간**에 코드 삽입
- **@SpireReturn** - 메서드 반환값 변경 또는 조기 종료

### 주요 클래스
- **AbstractMonster** - 모든 적의 기본 클래스
- **AbstractDungeon** - 던전 상태 관리 (현재 Ascension 레벨, Act 번호 등)
- **DamageInfo** - 데미지 정보
- **AbstractPower** - 버프/디버프

### 적 타입
- **AbstractMonster.EnemyType.NORMAL** - 일반 적
- **AbstractMonster.EnemyType.ELITE** - 엘리트 적
- **AbstractMonster.EnemyType.BOSS** - 보스

### 중요 메서드
- **init()** - 적 초기화 (체력 설정 최적 시점)
- **usePreBattleAction()** - 전투 시작 직전 (공격력, 버프 설정)
- **takeTurn()** - 적의 턴 행동 (패턴 수정)
- **die()** - 적 사망 시

## 📖 상세 가이드 목차

### 기본 수정
1. [적 체력 변경하기](MONSTER_HEALTH.md#기본-체력-수정)
2. [적 공격력 변경하기](MONSTER_DAMAGE.md#기본-공격력-수정)
3. [적 타입별 수정](ENEMY_MODIFY.md#타입별-수정)

### 고급 수정
1. [적 행동 패턴 변경](MONSTER_BEHAVIOR.md#행동-패턴-수정)
2. [버프/디버프 추가](MONSTER_BEHAVIOR.md#버프-추가)
3. [보스 전용 수정](BOSS_MODIFICATIONS.md)

### 막별 차등 적용
1. [Act별 다른 효과 적용](COMMON_PATTERNS.md#act별-차등-적용)
2. [Ascension 레벨별 적용](COMMON_PATTERNS.md#ascension-레벨별-적용)

## 🐛 일반적인 문제 해결

### 1. 패치가 적용되지 않음
- **원인**: 클래스명이나 메서드명이 잘못됨
- **해결**: [ENEMY_LIST.md](ENEMY_LIST.md)에서 정확한 클래스명 확인

### 2. 게임 크래시
- **원인**: null 체크 없이 필드 접근
- **해결**: 항상 null 체크 추가
```java
if (__instance.damage != null && !__instance.damage.isEmpty()) {
    // 안전한 접근
}
```

### 3. 체력이 두 배로 늘어남
- **원인**: 여러 패치가 중복 적용됨
- **해결**: 레벨 범위 정확히 설정
```java
if (level == 21) {  // >= 21이 아닌 == 21
    // 한 번만 적용
}
```

## 📊 ascension-100 모드 구현 현황

### ✅ 완전 구현
- Level 21-27: 체력, 공격력, 보스 강화
- Level 28-39: 플레이어 패널티, 추가 체력/공격력 증가
- Level 51-70: 대폭 강화, 버프 추가

### ⚠️ 미구현 (중요!)
- **Level 40-50**: 완전 미구현
  - Level 40: 적 AI 조정
  - Level 41: 엘리트 증가
  - Level 42-49: 골드, 상점, 이벤트 수정
  - Level 50: 4막 강제

- **Level 71-100**: 대부분 미구현
  - 특수 전투, 이벤트 변경 등

자세한 내용은 [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) 참고

## 🎓 학습 순서 추천

1. **입문**: [PATCH_BASICS.md](PATCH_BASICS.md) - 패치 기본 개념
2. **초급**: [MONSTER_HEALTH.md](MONSTER_HEALTH.md), [MONSTER_DAMAGE.md](MONSTER_DAMAGE.md) - 간단한 수정
3. **중급**: [MONSTER_BEHAVIOR.md](MONSTER_BEHAVIOR.md) - 행동 패턴 수정
4. **고급**: [BOSS_MODIFICATIONS.md](BOSS_MODIFICATIONS.md) - 보스 전용 수정
5. **마스터**: 실제 구현 코드 분석 (`ascension-100/src/main/java/com/stsmod/ascension100/patches/`)

## 🔗 추가 리소스

- **디컴파일 소스**: `E:\workspace\sts-decompile\`
- **구현된 모드**: `E:\workspace\sts-mods\ascension-100\`
- **BaseMod Wiki**: https://github.com/daviscook477/BaseMod/wiki
- **ModTheSpire**: https://github.com/kiooeht/ModTheSpire

## 💡 팁

1. **디컴파일 소스 활용**: 막히면 `E:\workspace\sts-decompile\`에서 원본 구현 확인
2. **로거 사용**: 모든 패치에 로그 추가로 디버깅 용이
3. **점진적 개발**: 작은 기능부터 테스트하며 개발
4. **버전 관리**: 작동하는 버전은 반드시 커밋

---

**작성일**: 2025-11-07
**기반 모드**: ascension-100 v1.0
**게임 버전**: 01-23-2019
**ModTheSpire**: 3.29.3
**BaseMod**: 5.48.0
