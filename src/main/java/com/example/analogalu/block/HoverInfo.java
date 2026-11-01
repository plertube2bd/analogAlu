package com.example.analogalu.block;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** 조준(마우스를 가져다 댐)했을 때 화면에 툴팁 창으로 보여줄 정보를 제공하는 블록 엔티티 */
public interface HoverInfo {
    /** 첫 줄은 제목, 나머지는 상세 줄. 클라이언트에서 호출되므로 동기화된 값만 사용한다. */
    List<Component> hoverInfo(Level level);
}
