# Modernization World 1.0.0

Fabric · Minecraft 26.3 · Java 25 · Fabric API 필요

**Analog ALU**(아날로그 레드스톤 컴퓨터, 전기 회로, 석유, 반도체)와 **Redstone Transportation**(자동 철도, 전차, 공장 기계, 직접 짓는 엘리베이터·배·비행선)을 하나로 합친 모드입니다. 1.0.0에서 총기와 흑색화약이 추가됐고, 전원 직렬·병렬 연결, 더 잘 보이는 +/− 표시, 더 희귀해진 석유가 들어갔습니다.

![리볼버](docs/previews/gun_revolver.png)

## 설치

1. `mods` 폴더에 `modernization-world-1.0.0.jar`와 Fabric API를 넣습니다.
2. 예전의 `analog-alu-*.jar`, `redstone-transportation-*.jar`는 **빼세요.** 두 모드가 이 jar 하나에 들어 있습니다.

기존 월드는 그대로 이어서 할 수 있습니다. 블록과 아이템 ID(`analog_alu:…`, `redstone_transportation:…`)를 바꾸지 않았고, 이 모드가 두 모드 ID를 대신 제공합니다.

## 1.0.0에서 바뀐 것

| 기능 | 내용 | 설명서 |
|---|---|---|
| 총기 | 리볼버(6발, 매번 R, 25 피해, 32블록), 머스킷(30 피해, 48블록, 3초 간격), 샷건(2발, 산탄 40알, 16블록) | [docs/GUNS_KO.md](docs/GUNS_KO.md) |
| 흑색화약 | 화약 + 숯. 9mm 총알, 머스킷 총알, 45게이지 산탄의 재료 | [docs/GUNS_KO.md](docs/GUNS_KO.md) |
| 석유 | 매장지가 예전의 5%(구역당 3.75%)로 줄고, 그중 90%가 지표에 드러남 | [README.TXT](README.TXT) 7장 |
| 전원 직렬·병렬 | 건전지·발전기를 여러 개 이으면 직렬은 전압이 더해지고 병렬은 전류를 나눔 | [전기 가이드](docs/transportation/ELECTRIC_GUIDE_KO.md) |
| +/− 표시 | 모든 전원의 + 면과 − 면에 크게 표시 | [전기 가이드](docs/transportation/ELECTRIC_GUIDE_KO.md) |
| 두 모드 합치기 | Analog ALU + Redstone Transportation → Modernization World | 아래 |

자세한 내용과 검증 결과는 [CHANGELOG_1.0.0_KO.md](CHANGELOG_1.0.0_KO.md)에 있습니다.

![+ 쪽에서 본 전원들](docs/previews/power_marks_positive.png)

## 문서

- [README.TXT](README.TXT): Analog ALU 쪽 블록 설명 (영문) — 논리 블록, 전기 회로, 석유, 반도체, 웨이퍼 프로그램, 총기
- [docs/GUNS_KO.md](docs/GUNS_KO.md): 총기와 흑색화약
- [docs/transportation/README_KO.md](docs/transportation/README_KO.md): 운송·공장·구조물 전체 설명
  - [AUTOMATION_GUIDE_KO.md](docs/transportation/AUTOMATION_GUIDE_KO.md): 자동화 기계, 평지화 기계, 강화 화로
  - [RAIL_GUIDE_KO.md](docs/transportation/RAIL_GUIDE_KO.md): 동력 선로, 선로 부설기, 정거장
  - [STRUCTURE_GUIDE_KO.md](docs/transportation/STRUCTURE_GUIDE_KO.md): 엘리베이터, 배, 비행선
  - [ELECTRIC_GUIDE_KO.md](docs/transportation/ELECTRIC_GUIDE_KO.md): 전기로 움직이는 블록, 직렬·병렬, +/− 표시

운송 쪽 문서는 합치기 전에 쓴 것이라 "Analog ALU를 함께 설치하세요" 같은 문장이 남아 있습니다. 이제 ALU가 늘 들어 있으므로 동력 선로·모터·기계·컨베이어는 항상 전기로 움직입니다.

## 빌드와 테스트

JDK 25와 Gradle 9.8.1이 필요합니다.

```bash
gradle build
```

`build/libs/modernization-world-1.0.0.jar`가 만들어집니다.

```bash
gradle runGameTest
```

서버 게임 테스트 192개(운송 172, 전원 5, 총기 12, 석유 3)를 돌립니다.

```bash
gradle runClientGameTest
```

실제 클라이언트를 띄워 클릭과 키 입력으로 확인하고, `docs/previews`의 사진을 찍습니다.

리소스 생성 스크립트는 `tools/`에 있습니다(표준 라이브러리만 사용).

| 스크립트 | 만드는 것 |
|---|---|
| `gen_gun_assets.py` | 총기·탄약·흑색화약 텍스처, 모델, 레시피, 이름, 총알 피해 종류 |
| `gen_terminal_marks.py` | 전원의 +/− 표시 |
| `generate_assets.py` | 운송 쪽 블록·아이템 리소스 |
| `gen_*.py` (그 밖) | ALU 쪽 리소스 |

## 소스 구조

- `src/main/java/com/example/analogalu/` — ALU: 논리 블록, 전기 회로(`block/CircuitSolver.java`), 석유(`worldgen/OilDeposits.java`), 반도체, 총기(`gun/`)
- `src/main/java/com/example/redstonetransportation/` — 운송: 철도, 전차, 공장 기계, 평지화 기계, 움직이는 구조물
- `src/gametest/` — 게임 테스트
