# Redstone Transportation 1.5.0 변경 사항

- Analog Redstone ALU 0.1.0 선택 연동을 추가했습니다.
- ALU 전차 제어기: 뒤=속도 0~15, 왼쪽=운행 허가, 오른쪽=후진, 위=비상 제동, 앞=인접 모터 출력.
- Copper Wire와 ALU 논리/산술/메모리 블록의 출력 신호를 그대로 읽습니다.
- 가산기/감산기 Carry/Borrow, AND/OR/XOR, NOT, Shift, CRAM, Extender를 전차 운행 회로에 활용할 수 있습니다.
- 정거장 입력 신호 상승 에지로 자동 출발할 수 있습니다.
- Analog ALU의 Java 클래스를 직접 참조하지 않아 선택 의존성을 유지합니다. ALU 전용 제작법은 Fabric load condition으로 모드 설치 시에만 로드됩니다.
- ALU 연동이 없어도 기본 전차와 일반 레드스톤 제어는 유지됩니다.
- ALU 제어기의 속도/후진/제동 출력 상태가 바뀌면 연결된 모터를 즉시 깨우도록 알림 처리를 보강했습니다.
