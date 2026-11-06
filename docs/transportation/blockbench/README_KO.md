# 안내: 이 문서는 1.5.0까지의 전차 모델 기준입니다

1.6.0 전차는 블록 모델이 아니라 전용 엔티티 모델을 씁니다. 이 폴더의 `.bbmodel`은 **이전 전차**의 원본이며 게임에 더 이상 쓰이지 않습니다.

새 전차와 화물 전차의 형태, 색상은 `tools/generate_assets.py`의 `tram_body()`, `cargo_body()`, `chassis()`에서 수정합니다. 실행하면 `client/TramGeometry.java`와 `textures/entity/*.png`가 함께 다시 만들어집니다. 자세한 내용은 `docs/CUSTOM_MODELS.md`를 참고하세요.

---

# Redstone Tram Blockbench 모델

## 포함 파일
- `RedstoneTram.bbmodel`: Blockbench 5.x Java Block/Item 형식의 편집 가능한 원본 프로젝트
- `textures/redstone_tram_atlas.png`: 64x64 픽셀 아트 아틀라스
- `RedstoneTram_Preview.png`: 모델 참고 미리보기
- 게임 리소스 `assets/redstone_transportation/models/block/redstone_tram_body.json`: 현재 엔티티 렌더러가 사용하는 Java 블록 모델

## Blockbench에서 열기
1. Blockbench를 실행합니다.
2. `File > Open Model`을 선택합니다.
3. `blockbench/RedstoneTram.bbmodel`을 엽니다.
4. 왼쪽 Outliner에서 `Underframe`, `Body`, `Cabin`, `PowerCore`, `Lights`, `Reactor` 그룹을 선택해 부품을 수정합니다.
5. UV/Texture 탭에서 64x64 atlas를 편집할 수 있습니다. 텍스처는 BBModel 파일 안에도 포함되어 있습니다.

## 게임에 수정 모델 적용
이 프로젝트는 `AbstractMinecartRenderer`의 custom display block 기능으로 `redstone_tram_body` 모델을 전차에 표시합니다. 따라서 이 리소스 경로를 유지해야 합니다.

1. Blockbench에서 수정 후 `File > Export > Export Block/Item Model`을 선택합니다.
2. 출력 JSON을 다음 경로에 덮어씁니다:
   `src/main/resources/assets/redstone_transportation/models/block/redstone_tram_body.json`
3. 텍스처를 PNG로 저장/내보내고 다음 경로를 유지합니다:
   `src/main/resources/assets/redstone_transportation/textures/block/redstone_tram_atlas.png`
4. 출력 JSON의 texture 참조가 다음과 같은지 확인합니다:
   `redstone_transportation:block/redstone_tram_atlas`
5. `gradle clean build`로 다시 빌드합니다.

## 모델 설계
- 모델의 기본 공간은 16x16x16 block-unit입니다.
- 전차 앞뒤가 대칭이어서 레일 방향으로 회전해도 형태가 유지됩니다.
- 부품은 6개 Outliner 그룹으로 나뉘어 있습니다.
- Minecraft 런타임은 `.bbmodel`을 직접 읽지 않습니다. 게임에서 사용하는 것은 함께 제공한 Java block model JSON과 PNG 텍스처입니다. `.bbmodel`은 수정 가능한 원본 파일입니다.

## 주의
Blockbench 프로젝트 파일 포맷은 Blockbench 버전에 따라 변경될 수 있습니다. 이 파일은 Blockbench 5.0 프로젝트 포맷을 기준으로 생성했습니다. 게임 실행 시 렌더링은 Fabric 모드의 custom display block을 통해 이 Java block model을 사용합니다.
