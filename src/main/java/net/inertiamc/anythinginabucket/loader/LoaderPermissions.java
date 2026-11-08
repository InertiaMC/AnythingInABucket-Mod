package net.inertiamc.anythinginabucket.loader;

//? if neoforge || forge {
/*import net.inertiamc.anythinginabucket.util.Perms;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//? if neoforge {
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
//?} else {
/^import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
^///?}

/^* Permission nodes for NeoForge and Forge, registered when the server gathers them. ^/
public final class LoaderPermissions {

    private static final Map<Perms, PermissionNode<Boolean>> NODES = new EnumMap<>(Perms.class);

    static {
        for (Perms perm : Perms.values()) {
            NODES.put(perm, new PermissionNode<>("aiab", perm.path(), PermissionTypes.BOOLEAN,
                    (player, uuid, context) -> player != null && perm.byDefault(player)));
        }
    }

    /^* {@code catch.<mob>} for every living entity type, true by default, registered so permission mods can list them. ^/
    private static final Map<String, PermissionNode<Boolean>> MOBS = new LinkedHashMap<>();
    /^* Living entities that are never catchable. ^/
    private static final Set<String> NOT_MOBS = Set.of("player", "armor_stand", "mannequin");

    private LoaderPermissions() {}

    public static synchronized List<PermissionNode<?>> nodes() {
        if (MOBS.isEmpty()) {
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                String mob = EntityType.getKey(type).getPath();
                if (!DefaultAttributes.hasSupplier(type) || NOT_MOBS.contains(mob)) continue;
                MOBS.putIfAbsent(mob, new PermissionNode<>("aiab", "catch." + mob, PermissionTypes.BOOLEAN,
                        (player, uuid, context) -> true));
            }
        }
        List<PermissionNode<?>> all = new ArrayList<>(NODES.values());
        all.addAll(MOBS.values());
        return all;
    }

    /^* Mobs registered after the nodes were gathered have no node and are allowed. ^/
    public static boolean mayCatch(ServerPlayer player, String mob) {
        PermissionNode<Boolean> node = MOBS.get(mob);
        return node == null || PermissionAPI.getPermission(player, node);
    }

    public static boolean check(ServerPlayer player, Perms perm) {
        return PermissionAPI.getPermission(player, NODES.get(perm));
    }
}
*///?}
