package net.inertiamc.anythinginabucket.catching;

import com.mojang.serialization.Codec;
import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Codes of filled buckets not yet released, saved with the world. Catching issues one and releasing spends it, so of
 * two copies of a bucket only the first releases.
 *
 * <p>The format is {@code {codes: [...]}} on every version so a world keeps its codes across updates. 1.21.x stores it
 * in {@code data/anythinginabucket_codes.dat}, 26.x in {@code data/anythinginabucket/codes.dat}, importing the 1.21.x
 * file once when the new one doesn't exist yet.
 */
public final class Codes extends SavedData {

    private static final String LEGACY_NAME = "anythinginabucket_codes";
    private static final Codec<Codes> CODEC = UUIDUtil.STRING_CODEC.listOf().fieldOf("codes").codec()
            .xmap(Codes::new, c -> List.copyOf(c.live));

    //? if >=26.1 {
    private static final net.minecraft.world.level.saveddata.SavedDataType<Codes> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("anythinginabucket", "codes"), Codes::new, CODEC,
            net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    //?} else if >=1.21.5 {
    /*private static final net.minecraft.world.level.saveddata.SavedDataType<Codes> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
            LEGACY_NAME, Codes::new, CODEC, net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    *///?} else {
    /*private static final SavedData.Factory<Codes> FACTORY = new SavedData.Factory<>(Codes::new,
            (tag, registries) -> CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElseGet(Codes::new),
            net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this).result()
                .ifPresent(encoded -> tag.merge((net.minecraft.nbt.CompoundTag) encoded));
        return tag;
    }
    *///?}

    private final Set<UUID> live = new HashSet<>();

    public Codes() {}

    private Codes(List<UUID> codes) {
        live.addAll(codes);
    }

    public static Codes of(MinecraftServer server) {
        //? if >=26.1 {
        Codes codes = server.getDataStorage().get(TYPE);
        if (codes == null) {
            codes = new Codes(legacy(server));
            server.getDataStorage().set(TYPE, codes);
        }
        return codes;
        //?} else if >=1.21.5 {
        /*return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        *///?} else {
        /*return server.overworld().getDataStorage().computeIfAbsent(FACTORY, LEGACY_NAME);
        *///?}
    }

    //? if >=26.1 {
    /** Codes saved by a 1.21.x version of the mod in this world. */
    private static List<UUID> legacy(MinecraftServer server) {
        java.nio.file.Path file = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve(LEGACY_NAME + ".dat");
        if (java.nio.file.Files.notExists(file)) return new ArrayList<>();
        try {
            net.minecraft.nbt.CompoundTag root = net.minecraft.nbt.NbtIo.readCompressed(file, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            List<UUID> codes = root.getCompound("data")
                    .flatMap(data -> CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, data).result())
                    .map(c -> List.copyOf(c.live)).orElse(List.of());
            AnythingInABucket.LOGGER.info("Read {} filled bucket codes saved by an older version of the mod.", codes.size());
            return new ArrayList<>(codes);
        } catch (java.io.IOException | RuntimeException e) {
            AnythingInABucket.LOGGER.warn("Could not read {}: {}", file, e.toString());
            return new ArrayList<>();
        }
    }
    //?}

    public boolean live(UUID code) {
        return live.contains(code);
    }

    public UUID issue() {
        UUID code;
        do code = UUID.randomUUID(); while (!live.add(code));
        setDirty();
        return code;
    }

    /** True when the code was live and is now spent. */
    public boolean spend(UUID code) {
        boolean was = live.remove(code);
        if (was) setDirty();
        return was;
    }

    /** Undoes a spend when the mob could not be released. */
    public void restore(UUID code) {
        if (live.add(code)) setDirty();
    }

    public int size() {
        return live.size();
    }
}
