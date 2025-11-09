# Slay the Spire 모바일 모딩 가이드

**⚠️ 경고: 이 문서는 교육 목적으로만 제공됩니다.**
- 개인 사용 목적으로만 사용하세요
- 수정된 APK/IPA를 배포하지 마세요
- 상업적 이용은 저작권 침해입니다
- 게임 개발사의 EULA를 확인하세요

---

## Android APK 수정 방법

### 1. 준비 단계

#### 필요한 도구 설치

```bash
# 1. APKTool 설치
# https://ibotpeaches.github.io/Apktool/
# apktool_2.x.x.jar 다운로드

# 2. jadx 설치 (소스 코드 확인용)
# https://github.com/skylot/jadx
# jadx-gui-x.x.x.zip 다운로드

# 3. uber-apk-signer 설치 (APK 서명)
# https://github.com/patrickfav/uber-apk-signer
# uber-apk-signer-x.x.x.jar 다운로드

# 4. Java JDK 설치 (이미 있을 것)
java -version  # 확인
```

#### 원본 APK 추출

**방법 A: Google Play에서 다운로드** (구매 필요)
```bash
# APK 추출 앱 사용 (루팅 불필요)
# 예: APK Extractor
# Play Store에서 Slay the Spire 설치 후 추출
```

**방법 B: 기기에서 추출**
```bash
# ADB 사용
adb shell pm list packages | grep slay
# com.humble.SlayTheSpire

adb shell pm path com.humble.SlayTheSpire
# package:/data/app/com.humble.SlayTheSpire-xxx/base.apk

adb pull /data/app/com.humble.SlayTheSpire-xxx/base.apk SlayTheSpire.apk
```

---

### 2. APK 디컴파일

```bash
# APK 디컴파일 (smali 코드 + 리소스)
java -jar apktool_2.9.3.jar d SlayTheSpire.apk -o SlayTheSpire_decompiled

# 결과 폴더 구조
SlayTheSpire_decompiled/
├── AndroidManifest.xml
├── apktool.yml
├── assets/              # 게임 리소스
├── lib/                 # 네이티브 라이브러리
├── res/                 # Android 리소스
├── smali/              # Dalvik 바이트코드 (메인 코드)
├── smali_classes2/     # 추가 코드
└── smali_classes3/     # 추가 코드
```

---

### 3. 소스 코드 확인 (참고용)

```bash
# jadx-gui로 APK 열기
# Java 소스로 디컴파일해서 확인 (읽기 전용)

# 찾을 내용:
# - AbstractMonster.class 위치
# - AbstractDungeon.class 위치
# - 승천 레벨 로직
```

**jadx에서 찾은 정보 예시**:
```
com.megacrit.cardcrawl.monsters.AbstractMonster
→ smali/com/megacrit/cardcrawl/monsters/AbstractMonster.smali

com.megacrit.cardcrawl.dungeons.AbstractDungeon
→ smali/com/megacrit/cardcrawl/dungeons/AbstractDungeon.smali
```

---

### 4. Smali 코드 수정

**Smali란?**
- Android Dalvik/ART VM의 어셈블리 언어
- Java 바이트코드와 유사하지만 다른 문법
- 직접 수정은 매우 어려움

**예시: Level 21 엘리트 체력 10% 증가**

#### Java 원본 로직 (참고)
```java
// AbstractMonster.init()
if (AbstractDungeon.isAscensionMode && AbstractDungeon.ascensionLevel >= 21) {
    if (this.type == EnemyType.ELITE) {
        this.maxHealth = (int) Math.ceil(this.maxHealth * 1.1f);
    }
}
```

#### Smali 수정 위치 찾기

```bash
# AbstractMonster.smali 파일 열기
# init() 메서드 찾기

# 검색할 패턴:
.method public init()V
    # 메서드 내용
.end method
```

#### Smali 수정 예시

**주의**: Smali 직접 수정은 **매우 어렵고 위험**합니다!

```smali
# SlayTheSpire_decompiled/smali/com/megacrit/cardcrawl/monsters/AbstractMonster.smali

.method public init()V
    .locals 8

    # ... 기존 코드 ...

    # 승천 모드 체크
    invoke-static {}, Lcom/megacrit/cardcrawl/dungeons/AbstractDungeon;->isAscensionMode()Z
    move-result v0
    if-nez v0, :cond_skip_ascension

    # 승천 레벨 체크
    sget v0, Lcom/megacrit/cardcrawl/dungeons/AbstractDungeon;->ascensionLevel:I
    const/16 v1, 0x15  # 21 in hex
    if-lt v0, v1, :cond_skip_ascension

    # 엘리트 체크
    iget-object v0, p0, Lcom/megacrit/cardcrawl/monsters/AbstractMonster;->type:Lcom/megacrit/cardcrawl/monsters/AbstractMonster$EnemyType;
    sget-object v1, Lcom/megacrit/cardcrawl/monsters/AbstractMonster$EnemyType;->ELITE:Lcom/megacrit/cardcrawl/monsters/AbstractMonster$EnemyType;
    if-ne v0, v1, :cond_skip_ascension

    # 체력 증가: maxHealth *= 1.1
    iget v0, p0, Lcom/megacrit/cardcrawl/monsters/AbstractMonster;->maxHealth:I
    int-to-float v0, v0
    const v1, 0x3f8ccccd  # 1.1f
    mul-float/2addr v0, v1
    float-to-double v0, v0
    invoke-static {v0, v1}, Ljava/lang/Math;->ceil(D)D
    move-result-wide v0
    double-to-int v0, v0
    iput v0, p0, Lcom/megacrit/cardcrawl/monsters/AbstractMonster;->maxHealth:I

    :cond_skip_ascension
    # ... 기존 코드 계속 ...

    return-void
.end method
```

**Smali 문법 기초**:
```smali
# 레지스터
v0, v1, v2      # 로컬 변수
p0, p1, p2      # 파라미터 (p0 = this)

# 명령어
const v0, 0x15                    # v0 = 21
iget v0, p0, Class;->field:I     # v0 = this.field
iput v0, p0, Class;->field:I     # this.field = v0
if-lt v0, v1, :label              # if (v0 < v1) goto label
invoke-static {}, Class;->method()V  # Class.method()
return-void                       # return
```

---

### 5. 더 쉬운 방법: 상수 값만 수정

Smali 직접 수정은 너무 어려우므로, **간단한 값만 수정**하는 것을 권장합니다.

**예시 1: 시작 골드 증가**

```bash
# 1. jadx-gui로 PlayerStartingGold 찾기
# com.megacrit.cardcrawl.characters.AbstractPlayer
# 초기 골드 = 99

# 2. smali 파일에서 99 찾기
grep -r "const.*99" SlayTheSpire_decompiled/smali/ | grep gold

# 3. 해당 파일 수정
# const/16 v0, 0x63  # 99
# const/16 v0, 0x3e8 # 1000 (변경)
```

**예시 2: 최대 체력 증가**

```bash
# AbstractPlayer.maxHealth 초기값 찾기
# Ironclad: 80
# Silent: 70
# Defect: 75

# smali에서 수정
# const/16 v0, 0x50  # 80 (Ironclad)
# const/16 v0, 0xc8  # 200 (변경)
```

---

### 6. APK 리빌드

```bash
# 수정한 smali 코드로 APK 재생성
java -jar apktool_2.9.3.jar b SlayTheSpire_decompiled -o SlayTheSpire_modded.apk

# 생성된 APK는 서명되지 않음
```

---

### 7. APK 서명

```bash
# uber-apk-signer로 자동 서명
java -jar uber-apk-signer-1.3.0.jar --apks SlayTheSpire_modded.apk

# 서명된 파일 생성
# SlayTheSpire_modded-aligned-debugSigned.apk

# 파일명 변경
mv SlayTheSpire_modded-aligned-debugSigned.apk SlayTheSpire_final.apk
```

---

### 8. 설치 및 테스트

```bash
# 기존 앱 제거 (데이터 백업 필수!)
adb uninstall com.humble.SlayTheSpire

# 수정된 APK 설치
adb install SlayTheSpire_final.apk

# 실행 및 테스트
# 게임이 정상 작동하는지 확인
```

---

## 더 쉬운 대안: 메모리 편집

Smali 수정보다 훨씬 쉬운 방법입니다.

### GameGuardian 사용 (루팅 필요)

```
1. GameGuardian 설치
   - https://gameguardian.net/

2. 게임 실행 후 GameGuardian 연결

3. 값 검색 및 수정
   - 현재 HP 검색 → 수정
   - 골드 검색 → 수정
   - 레벨 값 검색 → 수정

4. 메모리 수정으로 즉시 반영
```

**장점**:
- APK 수정 불필요
- 즉시 테스트 가능
- 원본 앱 유지

**단점**:
- 루팅 필요
- 앱 재시작 시 초기화
- 일부 값만 수정 가능

---

## Frida를 이용한 런타임 후킹 (고급)

**루팅 또는 재패키징 필요**

### Frida 설치

```bash
# PC에 Frida 설치
pip install frida-tools

# Android 기기에 frida-server 설치
# https://github.com/frida/frida/releases
# frida-server-android-arm64 다운로드

adb push frida-server /data/local/tmp/
adb shell "chmod 755 /data/local/tmp/frida-server"
adb shell "/data/local/tmp/frida-server &"
```

### Frida 스크립트 작성

```javascript
// sts_mod.js
Java.perform(function() {
    // AbstractMonster 클래스 찾기
    var AbstractMonster = Java.use("com.megacrit.cardcrawl.monsters.AbstractMonster");

    // init() 메서드 후킹
    AbstractMonster.init.implementation = function() {
        // 원본 메서드 실행
        this.init();

        // 승천 모드이고 레벨 21 이상일 때
        var AbstractDungeon = Java.use("com.megacrit.cardcrawl.dungeons.AbstractDungeon");

        if (AbstractDungeon.isAscensionMode.value &&
            AbstractDungeon.ascensionLevel.value >= 21) {

            // 엘리트 몬스터 체크
            var EnemyType = Java.use("com.megacrit.cardcrawl.monsters.AbstractMonster$EnemyType");
            if (this.type.value.equals(EnemyType.ELITE.value)) {
                // 체력 10% 증가
                this.maxHealth.value = Math.ceil(this.maxHealth.value * 1.1);
                console.log("Elite HP increased: " + this.maxHealth.value);
            }
        }
    };

    console.log("Slay the Spire mod loaded!");
});
```

### 스크립트 실행

```bash
# 게임 실행 후
frida -U -f com.humble.SlayTheSpire -l sts_mod.js

# 또는 실행 중인 앱에 연결
frida -U "Slay the Spire" -l sts_mod.js
```

**장점**:
- APK 수정 불필요
- JavaScript로 작성 (Smali보다 쉬움)
- 동적으로 테스트 가능
- 여러 모드 쉽게 전환

**단점**:
- 루팅 또는 디버깅 가능한 APK 필요
- 앱 실행 시마다 Frida 실행 필요
- PC 연결 필요 (USB 디버깅)

---

## iOS 수정 (IPA 패치)

**매우 어렵고 탈옥 필요**

### 요구사항

```
❌ 탈옥된 iOS 기기
❌ Mac (Xcode 필요)
❌ Apple Developer 계정 (선택)
```

### 과정 요약

```bash
# 1. IPA 추출 (App Store 앱)
# iPhone Backup Explorer 등 사용

# 2. IPA 압축 해제
unzip SlayTheSpire.ipa -d SlayTheSpire_ipa

# 3. Payload/SlayTheSpire.app 수정
# Mach-O 바이너리 분석 (Hopper, IDA Pro)
# ARM64 어셈블리 수정 (매우 어려움!)

# 4. 재서명
codesign -f -s "iPhone Developer" SlayTheSpire.app

# 5. IPA 재패키징
zip -r SlayTheSpire_modded.ipa Payload/

# 6. 설치
# Cydia Impactor 또는 AltStore 사용
```

**현실적으로**: iOS는 **너무 어렵고 비추천**입니다.

---

## 권장 워크플로우

### 1단계: 간단한 값 수정부터 시작

```bash
# 시작 골드, 체력 등 상수 값만 수정
# Smali에서 const 명령어 찾아서 수정
# 성공 후 APK 빌드 → 설치 → 테스트
```

### 2단계: 메모리 편집 도구 사용

```bash
# GameGuardian으로 실시간 값 수정
# 원하는 기능 테스트
# 나중에 APK에 반영
```

### 3단계: Frida로 로직 추가

```bash
# JavaScript로 게임 로직 후킹
# 복잡한 모드 로직 테스트
# 잘 작동하면 Smali로 변환
```

### 4단계: Smali 직접 수정 (고급)

```bash
# Java → Smali 변환 도구 사용
# 수동으로 Smali 코드 작성
# 복잡한 로직 구현
```

---

## 도구 모음

### 필수 도구

| 도구 | 용도 | 링크 |
|------|------|------|
| APKTool | APK 디컴파일/리빌드 | https://ibotpeaches.github.io/Apktool/ |
| jadx | Java 소스 확인 | https://github.com/skylot/jadx |
| uber-apk-signer | APK 서명 | https://github.com/patrickfav/uber-apk-signer |

### 선택 도구

| 도구 | 용도 | 링크 |
|------|------|------|
| GameGuardian | 메모리 편집 | https://gameguardian.net/ |
| Frida | 런타임 후킹 | https://frida.re/ |
| MT Manager | 올인원 APK 편집 | Play Store |
| Virtual Xposed | 루팅 없이 후킹 | https://github.com/android-hacker/VirtualXposed |

---

## 실전 예제: Level 28 골드 증가 10% 구현

### 방법 1: Smali 수정

```bash
# 1. jadx로 RewardItem 클래스 찾기
# com.megacrit.cardcrawl.rewards.RewardItem
# 생성자에서 goldAmt 설정

# 2. Smali 파일 찾기
# smali/com/megacrit/cardcrawl/rewards/RewardItem.smali

# 3. 생성자 찾기
.method public constructor <init>(I)V
    # I = int goldAmount 파라미터

    # goldAmt 필드 설정 부분 찾기
    iput p1, p0, Lcom/megacrit/cardcrawl/rewards/RewardItem;->goldAmt:I

    # 이 바로 앞에 승천 체크 + 골드 증가 로직 추가

    # 승천 모드 체크
    invoke-static {}, Lcom/megacrit/cardcrawl/dungeons/AbstractDungeon;->isAscensionMode()Z
    move-result v0
    if-nez v0, :skip_gold_mod

    # 승천 레벨 체크
    sget v0, Lcom/megacrit/cardcrawl/dungeons/AbstractDungeon;->ascensionLevel:I
    const/16 v1, 0x1c  # 28
    if-lt v0, v1, :skip_gold_mod

    # 골드 10% 감소: goldAmount * 0.9
    move v0, p1           # v0 = goldAmount
    int-to-float v0, v0
    const v1, 0x3f666666  # 0.9f
    mul-float/2addr v0, v1
    invoke-static {v0}, Ljava/lang/Math;->floor(F)F
    move-result v0
    float-to-int p1, v0  # p1 = (int)floor(goldAmount * 0.9)

    :skip_gold_mod
    iput p1, p0, Lcom/megacrit/cardcrawl/rewards/RewardItem;->goldAmt:I
.end method
```

### 방법 2: Frida 스크립트

```javascript
Java.perform(function() {
    var RewardItem = Java.use("com.megacrit.cardcrawl.rewards.RewardItem");
    var AbstractDungeon = Java.use("com.megacrit.cardcrawl.dungeons.AbstractDungeon");

    // 생성자 후킹
    RewardItem.$init.overload('int').implementation = function(goldAmount) {
        // 승천 모드이고 레벨 28 이상
        if (AbstractDungeon.isAscensionMode.value &&
            AbstractDungeon.ascensionLevel.value >= 28) {
            // 골드 10% 감소
            goldAmount = Math.floor(goldAmount * 0.9);
            console.log("Level 28: Gold reduced to " + goldAmount);
        }

        // 원본 생성자 호출
        return this.$init(goldAmount);
    };
});
```

---

## 문제 해결

### Q: APK 설치 시 "앱이 설치되지 않음"

**원인**: 서명 불일치 또는 원본 앱이 남아있음

**해결**:
```bash
# 원본 앱 완전 삭제
adb uninstall com.humble.SlayTheSpire

# 수정된 APK 재서명
java -jar uber-apk-signer-1.3.0.jar --apks SlayTheSpire_modded.apk

# 다시 설치
adb install SlayTheSpire_modded-aligned-debugSigned.apk
```

### Q: 게임이 실행되자마자 크래시

**원인**: Smali 코드 오류

**해결**:
```bash
# logcat으로 크래시 로그 확인
adb logcat | grep -i "androidruntime"

# 수정한 부분 되돌리기
# 다시 디컴파일 → 수정 → 리빌드
```

### Q: 온라인 기능이 작동하지 않음

**원인**: 서명 불일치로 서버가 수정된 앱 거부

**해결**:
- 오프라인 모드로만 플레이
- 원본 앱의 서명 키 사용 (불가능)
- 온라인 기능 포기

### Q: 업데이트 후 모드가 작동하지 않음

**원인**: 게임 업데이트로 코드 구조 변경

**해결**:
- 새 버전 APK로 다시 작업
- 수정 위치 재확인
- Frida 스크립트 수정

---

## 최종 권장사항

### Android 사용자

1. **가장 쉬운 방법**: GameGuardian (메모리 편집)
   - 루팅 필요
   - 즉시 테스트 가능
   - 간단한 값만 수정

2. **중간 난이도**: Frida (런타임 후킹)
   - JavaScript 사용
   - 복잡한 로직 가능
   - 디버깅 쉬움

3. **고급 방법**: APK 수정 (Smali)
   - 영구적 수정
   - 앱 독립적
   - 배우는 데 시간 소요

### iOS 사용자

**현실적으로 불가능합니다.**
- 탈옥 필요
- ARM64 어셈블리 지식 필요
- 도구 부족
- **PC에서 하는 것을 강력 권장**

---

## 마무리

**가장 현실적인 방법**:

```
1. Android 기기 사용
2. GameGuardian으로 간단한 값 수정
3. 원하는 효과 테스트
4. 마음에 들면 APK 수정 시도
5. Frida로 복잡한 로직 구현
```

**학습 순서**:
1. APKTool 기본 사용법
2. Smali 문법 기초
3. jadx로 코드 분석
4. 간단한 값 수정
5. Frida 스크립팅
6. 복잡한 로직 구현

더 구체적인 도움이 필요하면 말씀해주세요! 🎮
