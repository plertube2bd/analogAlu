package com.example.analogalu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 웨이퍼 편집기의 임시 저장(초안) 보관소. 저장하지 않고 창을 닫아도 다음에 다시 열면 이어서 쓸 수 있다.
 *
 * 초안은 "웨이퍼에 실제로 저장된 코드(base)"를 키로 보관한다. 그래서 같은 웨이퍼를 다시 열 때만 복원되고,
 * 다른 코드가 저장된 웨이퍼를 열면 그 웨이퍼의 코드가 그대로 보인다.
 * 최근 {@link #MAX_DRAFTS}개만 유지하며, 창을 닫을 때 config 폴더의 파일에도 기록해 게임을 껐다 켜도 남는다.
 * 저장(성공) 시에는 해당 초안을 지운다.
 */
@Environment(EnvType.CLIENT)
public final class WaferDraftStore {
    private static final int MAX_DRAFTS = 8;
    private static final int MAX_CODE_LENGTH = 4096;
    private static final String FILE_NAME = "analog_alu_wafer_drafts.json";

    /** base 코드 -> 초안. 오래된 항목이 앞쪽. */
    private static final Map<String, String> DRAFTS = new LinkedHashMap<>();
    private static boolean loaded;

    private WaferDraftStore() {}

    /** base 코드에 대한 초안이 있으면 돌려준다 (base와 같은 내용이면 초안으로 치지 않는다). */
    public static synchronized Optional<String> get(String base) {
        ensureLoaded();
        String draft = DRAFTS.get(base);
        return draft == null || draft.equals(base) ? Optional.empty() : Optional.of(draft);
    }

    /** 초안을 기록한다. base와 같아지면(= 수정사항 없음) 초안을 지운다. */
    public static synchronized void put(String base, String draft) {
        ensureLoaded();
        DRAFTS.remove(base);
        if (draft == null || draft.equals(base) || draft.length() > MAX_CODE_LENGTH) return;
        DRAFTS.put(base, draft);
        while (DRAFTS.size() > MAX_DRAFTS) {
            DRAFTS.remove(DRAFTS.keySet().iterator().next());
        }
    }

    public static synchronized void clear(String base) {
        ensureLoaded();
        DRAFTS.remove(base);
    }

    /** 현재 초안들을 파일에 기록한다. 실패해도 게임에는 영향이 없다. */
    public static synchronized void flush() {
        ensureLoaded();
        JsonArray array = new JsonArray();
        DRAFTS.forEach((base, draft) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("base", base);
            entry.addProperty("code", draft);
            array.add(entry);
        });
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("drafts", array);
        try {
            Path file = file();
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ignored) {
            // 임시 저장은 편의 기능이므로 기록 실패는 무시한다.
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path file = file();
            if (!Files.isRegularFile(file)) return;
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("drafts")) {
                JsonObject entry = element.getAsJsonObject();
                String base = entry.get("base").getAsString();
                String code = entry.get("code").getAsString();
                if (code.length() <= MAX_CODE_LENGTH && !code.equals(base)) DRAFTS.put(base, code);
            }
        } catch (IOException | RuntimeException ignored) {
            // 손상된 파일은 무시하고 빈 상태로 시작한다.
        }
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
