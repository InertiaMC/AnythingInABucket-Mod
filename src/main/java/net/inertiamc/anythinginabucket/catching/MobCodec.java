package net.inertiamc.anythinginabucket.catching;

import com.mojang.serialization.Dynamic;
import net.inertiamc.anythinginabucket.util.Compat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Saves and restores a whole mob as NBT. A mob saved on an older Minecraft version goes through the game's data fixers
 * when it is released.
 */
public final class MobCodec {

    private MobCodec() {}

    public static int currentDataVersion() {
        return Compat.dataVersion();
    }

    /** The mob's NBT with its id, or null if the game won't save it. Dismount riders and vehicles first. */
    public static CompoundTag save(Mob mob, Logger logger) {
        //? if >=1.21.6 {
        try (net.minecraft.util.ProblemReporter.ScopedCollector reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(mob.problemPath(), logger)) {
            net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(reporter, mob.registryAccess());
            if (!mob.saveAsPassenger(output)) return null;
            CompoundTag tag = output.buildResult();
            tag.remove("Passengers");
            return tag;
        }
        //?} else {
        /*CompoundTag tag = new CompoundTag();
        if (!mob.saveAsPassenger(tag)) return null;
        tag.remove("Passengers");
        return tag;
        *///?}
    }

    /** Spawns the mob at {@code at} facing {@code yaw}, or returns null if it can't be created. */
    public static Entity spawn(ServerLevel level, CompoundTag saved, int dataVersion, Vec3 at, float yaw, Logger logger) {
        CompoundTag tag = saved.copy();
        int current = currentDataVersion();
        if (dataVersion > 0 && dataVersion < current) {
            tag = (CompoundTag) DataFixers.getDataFixer()
                    .update(References.ENTITY, new Dynamic<>(NbtOps.INSTANCE, tag), dataVersion, current).getValue();
        }
        Entity entity = create(level, tag, logger);
        if (entity == null) return null;
        // A copy that got past the code check would clash with the original's UUID.
        if (level.getEntity(entity.getUUID()) != null) entity.setUUID(UUID.randomUUID());
        Compat.place(entity, at.x, at.y, at.z, yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        return level.addFreshEntity(entity) ? entity : null;
    }

    private static Entity create(ServerLevel level, CompoundTag tag, Logger logger) {
        //? if >=26.2 {
        try (net.minecraft.util.ProblemReporter.ScopedCollector reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(logger)) {
            return EntityType.create(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), tag), level,
                    new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.BUCKET, true)).orElse(null);
        }
        //?} else if >=1.21.6 {
        /*try (net.minecraft.util.ProblemReporter.ScopedCollector reporter = new net.minecraft.util.ProblemReporter.ScopedCollector(logger)) {
            return EntityType.create(net.minecraft.world.level.storage.TagValueInput.create(reporter, level.registryAccess(), tag), level,
                    net.minecraft.world.entity.EntitySpawnReason.BUCKET).orElse(null);
        }
        *///?} else if >=1.21.2 {
        /*return EntityType.create(tag, level, net.minecraft.world.entity.EntitySpawnReason.BUCKET).orElse(null);
        *///?} else {
        /*return EntityType.create(tag, level).orElse(null);
        *///?}
    }
}
