package net.inertiamc.anythinginabucket.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Permission nodes ({@code aiab.catch} and so on), checked through each loader's permission API. Without a permissions
 * mod, everyone can catch and release and the rest needs op level 2. The console can do everything.
 */
public enum Perms {
    CATCH("catch", false),
    RELEASE("release", false),
    BYPASS_OWNER("bypass.owner", true),
    COMMAND_HELP("command.help", false),
    COMMAND_GIVE("command.give", true),
    COMMAND_ADMIN("command.admin", true),
    COMMAND_ADMIN_INFO("command.admin.info", true),
    COMMAND_ADMIN_RELOAD("command.admin.reload", true);

    private final String path;
    private final boolean staff;

    Perms(String path, boolean staff) {
        this.path = path;
        this.staff = staff;
    }

    public String node() {
        return "aiab." + path;
    }

    /** Whether a player has it when no permissions mod says otherwise. */
    public boolean byDefault(ServerPlayer player) {
        return !staff || Compat.gamemaster(player);
    }

    public boolean has(CommandSourceStack source) {
        return !(source.getEntity() instanceof ServerPlayer player) || has(player);
    }

    public boolean has(ServerPlayer player) {
        //? if fabric {
        return me.lucko.fabric.api.permissions.v0.Permissions.check(player, node(), byDefault(player));
        //?} else
        /*return net.inertiamc.anythinginabucket.loader.LoaderPermissions.check(player, this);*/
    }

    /** {@code aiab.catch.<mob>}: true by default. */
    public static boolean mayCatch(ServerPlayer player, String mob) {
        //? if fabric {
        return me.lucko.fabric.api.permissions.v0.Permissions.check(player, "aiab.catch." + mob, true);
        //?} else
        /*return net.inertiamc.anythinginabucket.loader.LoaderPermissions.mayCatch(player, mob);*/
    }

    public String path() {
        return path;
    }

    public static List<Perms> all() {
        return Arrays.asList(values());
    }
}
