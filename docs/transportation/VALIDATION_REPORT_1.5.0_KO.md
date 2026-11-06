# 1.5.0 정적 검증 보고서

검증 시점: 2026-10-31

## 통과한 검사
- `tools/validate_resources.py`: 67개 JSON 파일, 11개 제작법 및 로컬 모델/텍스처 참조 검사 통과
- ALU 전차 제어기 레시피에 `fabric:all_mods_loaded` 조건이 있어 `analog_alu` 미설치 시 레시피 리소스가 로드되지 않도록 구성
- 소스 Java에 `com.example.analogalu.*` 직접 참조가 없어 선택적 의존성 유지
- 실제 Analog ALU 0.1.0 JAR의 블록 ID와 신호 API를 확인하고 해당 0~15 출력에 맞춰 연결
- AND/OR/XOR 설명을 실제 4비트 비트 연산에 맞게 정정

## 아직 수행하지 못한 검사
- 현재 빌드 컨테이너에는 Java 21만 설치되어 있어 Java 25 / Gradle Loom 컴파일은 실행하지 못함
- Minecraft 26.3 클라이언트에서 실제 ALU 회로, 전차 주행, 자동 정거장 출발을 실행 검증하지 못함

최종 빌드 및 게임 테스트는 Windows JDK 25 + Gradle 9.8.1 환경에서 `BUILD_WINDOWS.bat` 실행 후 확인해야 합니다.
