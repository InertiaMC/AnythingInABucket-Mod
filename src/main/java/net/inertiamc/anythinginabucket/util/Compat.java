package net.inertiamc.anythinginabucket.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Calls that differ between Minecraft versions, kept in one place so the rest of the code is version-independent.
 */
public final class Compat {

    private Compat() {}

    // ---------------------------------------------------------------- players

    public static ServerLevel level(ServerPlayer player) {
        //? if >=1.21.6 {
        return player.level();
        //?} else
        /*return player.serverLevel();*/
    }

    public static MinecraftServer server(ServerPlayer player) {
        return level(player).getServer();
    }

    public static String name(Player player) {
        //? if >=1.21.9 {
        return player.getPlainTextName();
        //?} else
        /*return player.getGameProfile().getName();*/
    }

    /** Op level 2, the default for staff-only actions when no permissions mod decides. */
    public static boolean gamemaster(ServerPlayer player) {
        //? if >=1.21.11 {
        return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //?} else
        /*return player.hasPermissions(2);*/
    }

    public static Optional<String> knownName(MinecraftServer server, UUID id) {
        //? if >=1.21.9 {
        return server.services().nameToIdCache().get(id).map(net.minecraft.server.players.NameAndId::name);
        //?} else
        /*return server.getProfileCache() == null ? Optional.empty() : server.getProfileCache().get(id).map(com.mojang.authlib.GameProfile::getName);*/
    }

    public static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        //? if >=26.3 {
        player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
        //?} else
        /*player.getInventory().placeItemBackInInventory(stack);*/
    }

    // ---------------------------------------------------------------- mobs

    public static UUID owner(OwnableEntity ownable) {
        //? if >=1.21.5 {
        return ownable.getOwnerReference() == null ? null : ownable.getOwnerReference().getUUID();
        //?} else
        /*return ownable.getOwnerUUID();*/
    }

    public static void dropLeash(Leashable leashable) {
        //? if >=1.21.4 {
        leashable.dropLeash();
        //?} else
        /*leashable.dropLeash(true, true);*/
    }

    public static void place(Entity entity, double x, double y, double z, float yaw) {
        //? if >=1.21.5 {
        entity.snapTo(x, y, z, yaw, 0);
        //?} else
        /*entity.moveTo(x, y, z, yaw, 0);*/
    }

    public static Item spawnEgg(EntityType<?> type) {
        //? if >=26.1 {
        return SpawnEggItem.byId(type).map(net.minecraft.core.Holder::value).orElse(null);
        //?} else
        /*return SpawnEggItem.byId(type);*/
    }

    // ---------------------------------------------------------------- ids and registries

    public static String itemPath(ItemStack stack) {
        //? if >=26.1 {
        return stack.typeHolder().unwrapKey().map(k -> key(k).getPath()).orElse("?");
        //?} else
        /*return stack.getItemHolder().unwrapKey().map(k -> key(k).getPath()).orElse("?");*/
    }

    public static Identifier key(net.minecraft.resources.ResourceKey<?> key) {
        //? if >=1.21.11 {
        return key.identifier();
        //?} else
        /*return key.location();*/
    }

    public static String dimension(Level level) {
        return key(level.dimension()).toString();
    }

    public static SoundEvent sound(Identifier id) {
        //? if >=1.21.2 {
        return BuiltInRegistries.SOUND_EVENT.getValue(id);
        //?} else
        /*return BuiltInRegistries.SOUND_EVENT.get(id);*/
    }

    public static int dataVersion() {
        //? if >=1.21.6 {
        return net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
        //?} else
        /*return net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion();*/
    }

    // ---------------------------------------------------------------- NBT (getters return Optional from 1.21.5)

    public static String string(CompoundTag tag, String key, String fallback) {
        //? if >=1.21.5 {
        return tag.getStringOr(key, fallback);
        //?} else
        /*return tag.contains(key) ? tag.getString(key) : fallback;*/
    }

    public static long longValue(CompoundTag tag, String key, long fallback) {
        //? if >=1.21.5 {
        return tag.getLongOr(key, fallback);
        //?} else
        /*return tag.contains(key) ? tag.getLong(key) : fallback;*/
    }

    public static int intValue(CompoundTag tag, String key, int fallback) {
        //? if >=1.21.5 {
        return tag.getIntOr(key, fallback);
        //?} else
        /*return tag.contains(key) ? tag.getInt(key) : fallback;*/
    }

    public static CompoundTag compound(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        return tag.getCompound(key).orElse(null);
        //?} else
        /*return tag.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound(key) : null;*/
    }

    public static void putUuid(CompoundTag tag, String key, UUID id) {
        //? if >=1.21.5 {
        tag.store(key, net.minecraft.core.UUIDUtil.CODEC, id);
        //?} else
        /*tag.putUUID(key, id);*/
    }

    public static UUID uuid(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        return tag.read(key, net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        //?} else
        /*return tag.hasUUID(key) ? tag.getUUID(key) : null;*/
    }
}
