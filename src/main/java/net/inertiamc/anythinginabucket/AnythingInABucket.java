package net.inertiamc.anythinginabucket;

import net.inertiamc.anythinginabucket.catching.BucketManager;
import net.inertiamc.anythinginabucket.catching.HeadPack;
import net.inertiamc.anythinginabucket.config.ConfigFile;
import net.inertiamc.anythinginabucket.config.Settings;
import net.inertiamc.anythinginabucket.item.CaptureRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Loader-independent entry point. The entrypoints in {@code loader/} forward the game's events here.
 */
public final class AnythingInABucket {

    public static final String MOD_ID = "anythinginabucket";
    public static final Logger LOGGER = LoggerFactory.getLogger("AnythingInABucket");

    private static ConfigFile config;
    private static volatile Settings settings;
    private static final BucketManager BUCKETS = new BucketManager();
    /** Whether a player's client has this mod installed (and so the bucket art). Set by each loader. */
    private static Predicate<ServerPlayer> artOnClient = player -> false;

    private AnythingInABucket() {}

    public static Settings settings() {
        Settings current = settings;
        return current != null ? current : (settings = ConfigFile.builtInSettings());
    }

    public static BucketManager buckets() {
        return BUCKETS;
    }

    public static void init(Path configDir) {
        config = new ConfigFile(configDir.resolve(MOD_ID), LOGGER);
        settings = config.load(null).settings();
        HeadPack.refresh(settings.resourcePack());
    }

    public static void artOnClient(Predicate<ServerPlayer> test) {
        artOnClient = test;
    }

    public static boolean hasArt(ServerPlayer player) {
        try {
            return artOnClient.test(player);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void onJoin(ServerPlayer player) {
        unlockRecipe(player);
        HeadPack.offer(player);
    }

    public static void unlockRecipe(ServerPlayer player) {
        CaptureRecipe.unlock(player);
    }

    public static void onDataPackReload(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(AnythingInABucket::unlockRecipe);
    }

    public static void onQuit(ServerPlayer player) {
        BUCKETS.quit(player.getUUID());
    }

    public static void onStopped() {
        BUCKETS.clearAll();
    }

    public record Reload(String error, int problems, boolean recipeChanged) {}

    /** Reloads config.yml. A recipe change also reloads data packs so it takes effect. */
    public static Reload reload(MinecraftServer server) {
        Settings before = settings();
        ConfigFile.Loaded loaded = config.load(before);
        if (loaded.error() != null) return new Reload(loaded.error(), 0, false);
        settings = loaded.settings();
        HeadPack.refresh(settings.resourcePack());
        boolean recipeChanged = !Objects.equals(before.recipe(), settings.recipe())
                || before.catchWith().capture() != settings.catchWith().capture();
        if (recipeChanged) server.reloadResources(server.getPackRepository().getSelectedIds());
        // /bucket give is only available while catch-with allows Capture Buckets.
        server.getPlayerList().getPlayers().forEach(p -> server.getCommands().sendCommands(p));
        return new Reload(null, settings.problems().size(), recipeChanged);
    }
}
