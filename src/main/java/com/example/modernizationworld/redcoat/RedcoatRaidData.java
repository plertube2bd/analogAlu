package com.example.modernizationworld.redcoat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent state for one active British raid in a dimension. */
public final class RedcoatRaidData extends SavedData {
    private record Snapshot(boolean active, int x, int y, int z, int wave, int waveCount,
                            int elapsed, List<String> soldiers) {
        static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("active").forGetter(Snapshot::active),
                Codec.INT.fieldOf("x").forGetter(Snapshot::x),
                Codec.INT.fieldOf("y").forGetter(Snapshot::y),
                Codec.INT.fieldOf("z").forGetter(Snapshot::z),
                Codec.INT.fieldOf("wave").forGetter(Snapshot::wave),
                Codec.INT.fieldOf("wave_count").forGetter(Snapshot::waveCount),
                Codec.INT.fieldOf("elapsed").forGetter(Snapshot::elapsed),
                Codec.STRING.listOf().fieldOf("soldiers").forGetter(Snapshot::soldiers)
        ).apply(instance, Snapshot::new));
    }

    private static final Codec<RedcoatRaidData> CODEC = Snapshot.CODEC.xmap(RedcoatRaidData::new, RedcoatRaidData::snapshot);
    public static final SavedDataType<RedcoatRaidData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("modernization_world", "redcoat_raid"),
            RedcoatRaidData::new, CODEC, null);

    private boolean active;
    private int x, y, z, wave, waveCount, elapsed;
    private final List<String> soldiers = new ArrayList<>();

    public RedcoatRaidData() {}

    private RedcoatRaidData(Snapshot snapshot) {
        active = snapshot.active();
        x = snapshot.x(); y = snapshot.y(); z = snapshot.z();
        wave = snapshot.wave(); waveCount = snapshot.waveCount(); elapsed = snapshot.elapsed();
        soldiers.addAll(snapshot.soldiers());
    }

    private Snapshot snapshot() {
        return new Snapshot(active, x, y, z, wave, waveCount, elapsed, List.copyOf(soldiers));
    }

    public static RedcoatRaidData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean active() { return active; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public int wave() { return wave; }
    public int waveCount() { return waveCount; }
    public int elapsed() { return elapsed; }
    public List<String> soldiers() { return List.copyOf(soldiers); }

    public void start(int x, int y, int z, int waveCount) {
        this.active = true; this.x = x; this.y = y; this.z = z;
        this.wave = 0; this.waveCount = waveCount; this.elapsed = 0;
        this.soldiers.clear(); setDirty();
    }

    public void nextWave() {
        wave++; elapsed = 0; soldiers.clear(); setDirty();
    }

    public void tick() { elapsed++; if ((elapsed % 20) == 0) setDirty(); }
    public void resetElapsed() { if (elapsed != 0) { elapsed = 0; setDirty(); } }

    public void addSoldier(UUID id) { soldiers.add(id.toString()); setDirty(); }

    public void finish() {
        active = false; soldiers.clear(); setDirty();
    }
}
