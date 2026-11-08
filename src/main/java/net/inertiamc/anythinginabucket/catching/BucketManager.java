package net.inertiamc.anythinginabucket.catching;

import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.config.Settings;
import net.inertiamc.anythinginabucket.item.BucketItems;
import net.inertiamc.anythinginabucket.util.Compat;
import net.inertiamc.anythinginabucket.util.Messages;
import net.inertiamc.anythinginabucket.util.Perms;
import net.inertiamc.anythinginabucket.util.Settle;
import net.inertiamc.anythinginabucket.util.Words;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catching and releasing. A caught mob is saved onto the filled bucket and removed in the same tick. Releasing spends
 * the bucket's code first, so a copied bucket releases nothing. With copy-protection off, codes are still issued but
 * never spent, so turning it back on later keeps existing buckets working.
 */
public final class BucketManager {

    /** Well above a normal mob's save; stops e.g. a llama full of shulker boxes producing an item clients can't load. */
    public static final int MAX_SAVE_BYTES = 64 * 1024;

    private final Map<UUID, Long> lastCatch = new ConcurrentHashMap<>();
    /** Tick of each player's last bucket click on a mob, so the interact and interact-at packets count as one click. */
    private final Map<UUID, Long> lastClickTick = new ConcurrentHashMap<>();
    private final Settle settle = new Settle();

    private static Settings settings() {
        return AnythingInABucket.settings();
    }

    // ---------------------------------------------------------------- catching

    public void tryCatch(ServerPlayer player, Mob mob, InteractionHand hand, ItemStack held, boolean plain) {
        ServerLevel level = Compat.level(player);
        long tick = level.getServer().getTickCount();
        Long last = lastClickTick.put(player.getUUID(), tick);
        if (last != null && last == tick) return;

        Settings settings = settings();
        Identifier typeId = EntityType.getKey(mob.getType());
        String kind = Words.pretty(typeId.getPath());
        if (!Perms.CATCH.has(player)) {
            deny(player, Messages.CATCH_NO_PERMISSION);
            return;
        }
        if (!Perms.mayCatch(player, typeId.getPath())) {
            deny(player, Messages.CATCH_NO_PERMISSION_MOB, "mob", kind);
            return;
        }
        if (disabled(level)) {
            deny(player, Messages.CATCH_WORLD);
            return;
        }
        if (!mob.isAlive() || mob.isRemoved()) return;
        if (settings.blacklist().contains(typeId.toString())) {
            deny(player, Messages.CATCH_BLACKLISTED, "mob", kind);
            return;
        }
        UUID owner = mob instanceof OwnableEntity ownable ? Compat.owner(ownable) : null;
        if (settings.petsOwnerOnly() && owner != null && !owner.equals(player.getUUID()) && !Perms.BYPASS_OWNER.has(player)) {
            deny(player, Messages.CATCH_NOT_YOURS, "mob", kind, "owner", MobFacts.ownerName(owner, level.getServer()));
            return;
        }
        for (Entity passenger : mob.getPassengers()) {
            if (passenger instanceof Player) {
                deny(player, Messages.CATCH_RIDDEN, "mob", kind);
                return;
            }
        }
        if (mob.getVehicle() instanceof Player) {
            deny(player, Messages.CATCH_RIDDEN, "mob", kind);
            return;
        }
        if (!mayBuild(player, level, mob.blockPosition())) {
            deny(player, Messages.CATCH_PROTECTED);
            return;
        }
        long now = System.currentTimeMillis();
        Long lastAt = lastCatch.get(player.getUUID());
        long left = lastAt == null ? 0 : settings.cooldownMillis() - (now - lastAt);
        if (left > 0) {
            deny(player, Messages.CATCH_COOLDOWN, "time", Math.max(1, (left + 999) / 1000) + "s");
            return;
        }

        mob.ejectPassengers();
        mob.stopRiding();
        if (mob instanceof Leashable leashable && leashable.isLeashed()) Compat.dropLeash(leashable);
        CompoundTag saved;
        try {
            saved = MobCodec.save(mob, AnythingInABucket.LOGGER);
        } catch (RuntimeException e) {
            AnythingInABucket.LOGGER.error("Could not save a {} into a bucket for {}.", typeId, Compat.name(player), e);
            saved = null;
        }
        if (saved == null) {
            deny(player, Messages.CATCH_FAILED);
            return;
        }
        if (saved.sizeInBytes() > MAX_SAVE_BYTES) {
            deny(player, Messages.CATCH_TOO_BIG, "mob", kind);
            return;
        }
        CompoundTag save = saved;
        var facts = MobFacts.read(mob, level.getServer(), key -> savedInt(save, key), key -> savedString(save, key));

        UUID code = Codes.of(level.getServer()).issue();
        lastCatch.put(player.getUUID(), now);
        settle.acted(player.getUUID());
        mob.discard();

        String look = HeadKey.fromSave(typeId.getPath(), key -> savedInt(save, key), key -> savedString(save, key),
                mob.getCustomName() == null ? null : mob.getCustomName().getString());
        ItemStack filled = BucketItems.filled(mob.getType(), mob.getCustomName(), Compat.name(player), facts, saved,
                MobCodec.currentDataVersion(), code, plain, look);
        if (held.getCount() <= 1) {
            player.setItemInHand(hand, filled);
        } else {
            held.shrink(1);
            Compat.giveOrDrop(player, filled);
        }
        resync(player);
        sound(player, "catch");
        Messages.send(player, Messages.CATCH_CAUGHT, "mob", kind);
    }

    // ---------------------------------------------------------------- releasing

    public void tryRelease(ServerPlayer player, InteractionHand hand, ItemStack held, BlockPos clicked, Direction face) {
        ServerLevel level = Compat.level(player);
        BucketItems.Filled filled = BucketItems.filledOf(held);
        if (filled == null) return;
        if (!Perms.RELEASE.has(player)) {
            deny(player, Messages.RELEASE_NO_PERMISSION);
            return;
        }
        if (disabled(level)) {
            deny(player, Messages.CATCH_WORLD);
            return;
        }
        BlockPos spot = passable(level, clicked) ? clicked : clicked.relative(face);
        if (!passable(level, spot)) {
            deny(player, Messages.RELEASE_BLOCKED);
            return;
        }
        if (!mayBuild(player, level, spot)) {
            deny(player, Messages.RELEASE_PROTECTED);
            return;
        }
        Codes codes = Codes.of(level.getServer());
        boolean coded = settings().copyProtection();
        if (coded && !codes.spend(filled.code())) {
            AnythingInABucket.LOGGER.warn("{} tried to release a copied filled bucket ({}, code {}) at {} in {}.",
                    Compat.name(player), filled.mob(), filled.code(), spot.toShortString(), Compat.dimension(level));
            deny(player, Messages.RELEASE_COPY);
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(spot);
        Entity spawned;
        try {
            spawned = MobCodec.spawn(level, filled.entity(), filled.dataVersion(), at, player.getYRot() + 180, AnythingInABucket.LOGGER);
        } catch (RuntimeException e) {
            AnythingInABucket.LOGGER.error("Could not release a {} (data version {}) for {}.", filled.mob(), filled.dataVersion(),
                    Compat.name(player), e);
            spawned = null;
        }
        if (spawned == null) {
            if (coded) codes.restore(filled.code());
            deny(player, Messages.RELEASE_FAILED);
            return;
        }
        settle.acted(player.getUUID());
        ItemStack back = ItemStack.EMPTY;
        if (settings().giveBack()) {
            // A Capture Bucket only while it still catches; otherwise a normal bucket.
            back = filled.plain() || !settings().catchWith().capture() ? new ItemStack(Items.BUCKET) : BucketItems.captureBucket(1);
        }
        player.setItemInHand(hand, back);
        resync(player);
        sound(player, "release");
        Messages.send(player, Messages.RELEASE_RELEASED, "mob", Words.pretty(Identifier.parse(filled.mob()).getPath()));
    }

    /**
     * Dispenser behaviour for a filled bucket (dispensers-release). The mob is released into the block in front, facing
     * away, and the empty bucket takes its slot (give-bucket-back). On failure the bucket stays in the dispenser.
     */
    public static ItemStack dispense(BlockSource source, ItemStack stack) {
        ServerLevel level = source.level();
        BlockPos pos = source.pos();
        BucketItems.Filled filled = BucketItems.filledOf(stack);
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos spot = pos.relative(facing);
        if (filled == null || disabled(level) || !passable(level, spot) || !level.getWorldBorder().isWithinBounds(spot)) {
            level.levelEvent(1001, pos, 0);
            return stack;
        }
        Codes codes = Codes.of(level.getServer());
        boolean coded = settings().copyProtection();
        if (coded && !codes.spend(filled.code())) {
            AnythingInABucket.LOGGER.warn("A dispenser at {} in {} fired a copied filled bucket ({}, code {}).", pos.toShortString(),
                    Compat.dimension(level), filled.mob(), filled.code());
            level.levelEvent(1001, pos, 0);
            return stack;
        }
        float yaw = facing.getAxis().isHorizontal() ? facing.toYRot() : 0;
        Entity spawned;
        try {
            spawned = MobCodec.spawn(level, filled.entity(), filled.dataVersion(), Vec3.atBottomCenterOf(spot), yaw, AnythingInABucket.LOGGER);
        } catch (RuntimeException e) {
            AnythingInABucket.LOGGER.error("Could not release a {} (data version {}) from a dispenser at {}.", filled.mob(),
                    filled.dataVersion(), pos.toShortString(), e);
            spawned = null;
        }
        if (spawned == null) {
            if (coded) codes.restore(filled.code());
            level.levelEvent(1001, pos, 0);
            return stack;
        }
        level.levelEvent(1000, pos, 0);
        ItemStack back = ItemStack.EMPTY;
        if (settings().giveBack()) {
            back = filled.plain() || !settings().catchWith().capture() ? new ItemStack(Items.BUCKET) : BucketItems.captureBucket(1);
        }
        if (stack.getCount() <= 1) return back;
        // A stack of copies (copy-protection off): the empty bucket goes in a free slot, or is dropped in front.
        stack.shrink(1);
        if (back.isEmpty()) return stack;
        DispenserBlockEntity dispenser = source.blockEntity();
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
            if (dispenser.getItem(slot).isEmpty()) {
                dispenser.setItem(slot, back);
                return stack;
            }
        }
        Block.popResource(level, spot, back);
        return stack;
    }

    // ---------------------------------------------------------------- shared

    /** Called first for every right click with this mod's items. True for a held click still repeating. */
    public boolean heldClick(Player player) {
        return settle.repeat(player.getUUID());
    }

    public void quit(UUID player) {
        lastClickTick.remove(player);
        settle.clear(player);
    }

    public void clearAll() {
        lastCatch.clear();
        lastClickTick.clear();
        settle.clearAll();
    }

    private static Integer savedInt(CompoundTag save, String key) {
        int missing = Integer.MIN_VALUE;
        int value = Compat.intValue(save, key, missing);
        return value == missing ? null : value;
    }

    /** A dotted key such as {@code VillagerData.type} reads one level into a compound. */
    private static String savedString(CompoundTag save, String key) {
        int dot = key.indexOf('.');
        if (dot > 0) {
            CompoundTag inner = Compat.compound(save, key.substring(0, dot));
            return inner == null ? null : Compat.string(inner, key.substring(dot + 1), null);
        }
        return Compat.string(save, key, null);
    }

    static boolean passable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean disabled(ServerLevel level) {
        return settings().disabledWorlds().contains(Compat.dimension(level));
    }

    /**
     * Adventure mode, spawn protection and the world border, plus claim mods through the Common Protection API on Fabric
     * (unless respect-claims is off). Claim mods that cancel the click are handled in {@link Interactions}.
     */
    static boolean mayBuild(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return false;
        if (!settings().respectClaims()) return true;
        //? if fabric && >=26.1 {
        return eu.pb4.common.protection.api.CommonProtection.canPlaceBlock(level, pos, player.nameAndId(), player);
        //?} else if fabric {
        /*return eu.pb4.common.protection.api.CommonProtection.canPlaceBlock(level, pos, player.getGameProfile(), player);
        *///?} else
        /*return true;*/
    }

    /** Resends the inventory, undoing whatever the client predicted the bucket or egg would do. */
    public static void resync(ServerPlayer player) {
        player.containerMenu.sendAllDataToRemote();
        player.inventoryMenu.sendAllDataToRemote();
    }

    /**
     * Resends the inventory at the end of the tick. A creative client predicts the vanilla result of the click (milking a
     * cow, scooping a bucketable mob) and nothing else corrects it; this lands after both that and the catch.
     */
    public static void resyncLater(ServerPlayer player) {
        if (!RESYNC_PENDING.add(player.getUUID())) return;
        MinecraftServer server = Compat.level(player).getServer();
        TickTask task = new TickTask(server.getTickCount() + 1, () -> {
            RESYNC_PENDING.remove(player.getUUID());
            if (!player.isRemoved()) resync(player);
        });
        //? if >=1.21.2 {
        server.schedule(task);
        //?} else
        /*server.tell(task);*/
    }

    private static final Set<UUID> RESYNC_PENDING = ConcurrentHashMap.newKeySet();

    public static void deny(ServerPlayer player, String line, String... pairs) {
        Messages.send(player, line, pairs);
        sound(player, "deny");
        resync(player);
    }

    private static void sound(ServerPlayer player, String key) {
        Settings.Sound sound = settings().sounds().get(key);
        if (sound == null) return;
        Identifier id = Identifier.tryParse(sound.name().contains(":") ? sound.name() : "minecraft:" + sound.name());
        if (id == null) return;
        SoundEvent event = Compat.sound(id);
        if (event == null) event = SoundEvent.createVariableRangeEvent(id);
        Compat.level(player).playSound(null, player.getX(), player.getY(), player.getZ(), event, SoundSource.PLAYERS, sound.volume(), sound.pitch());
    }
}
