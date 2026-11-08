package net.inertiamc.anythinginabucket.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Player-facing messages, worded as in the plugin's messages.yml. {@code {x}} placeholders are filled from pairs. */
public final class Messages {

    public static final String PREFIX = "&#E07A5F&lBUCKET &8» ";

    public static final String NO_PERMISSION = "&cYou do not have permission to do that.";
    public static final String RELOAD_ERROR = "&c{error}";
    public static final String RELOAD_PROBLEMS = "&e{count} settings were left out or changed; the console lists them.";
    public static final String RELOAD_SUCCESS = "&fConfiguration &#E07A5Freloaded&f.";
    public static final String RELOAD_RECIPE = "&7The Capture Bucket recipe changed, so data packs were reloaded too.";

    public static final String CATCH_NO_PERMISSION = "&cYou can't catch mobs.";
    public static final String CATCH_NO_PERMISSION_MOB = "&cYou can't catch a &e{mob}&c.";
    public static final String CATCH_WORLD = "&cMobs can't be caught or released in this world.";
    public static final String CATCH_BLACKLISTED = "&cA &e{mob}&c can't be caught.";
    public static final String CATCH_CAPTURE_OFF = "&cCapture Buckets don't catch on this server. &7Sneak and right-click with a normal bucket.";
    public static final String CATCH_NOT_YOURS = "&cThis &e{mob}&c belongs to &e{owner}&c.";
    public static final String CATCH_RIDDEN = "&cSomeone is riding this &e{mob}&c.";
    public static final String CATCH_PROTECTED = "&cYou can't catch mobs here.";
    public static final String CATCH_COOLDOWN = "&cWait &e{time}&c before catching again.";
    public static final String CATCH_TOO_BIG = "&cThis &e{mob}&c carries too much to fit in a bucket.";
    public static final String CATCH_FAILED = "&cThis mob could not be caught; the console has the details.";
    public static final String CATCH_CAUGHT = "&fYou caught the &#E07A5F{mob}&f.";

    public static final String RELEASE_NO_PERMISSION = "&cYou can't release mobs.";
    public static final String RELEASE_BLOCKED = "&cSomething is in the way there.";
    public static final String RELEASE_PROTECTED = "&cYou can't release mobs here.";
    public static final String RELEASE_COPY = "&cThis bucket is a copy, so it can't be released.";
    public static final String RELEASE_FAILED = "&cThis mob could not be released; the console has the details.";
    public static final String RELEASE_RELEASED = "&fYou let the &#E07A5F{mob}&f out.";

    public static final String RESOURCE_PACK_PROMPT = "&fShows each caught mob peeking out of its bucket. &7You can say no.";

    public static final String GIVE_RECEIVED = "&fYou got &#E07A5F{amount} Capture Buckets&f.";
    public static final String GIVE_RECEIVED_ONE = "&fYou got a &#E07A5FCapture Bucket&f.";
    public static final String GIVE_GIVEN = "&fGave &#E07A5F{amount} Capture Buckets&f to &#E07A5F{player}&f.";
    public static final String GIVE_GIVEN_ONE = "&fGave a &#E07A5FCapture Bucket&f to &#E07A5F{player}&f.";
    public static final String GIVE_OFF = "&cCapture Buckets are off: catch-with is normal-bucket.";

    public static final String BUCKET_NAME = "&#E07A5F&lCapture Bucket";
    public static final List<String> BUCKET_LORE = List.of(
            "&#A8B4C4Catches a mob to carry, store or trade,",
            "&#A8B4C4and lets it out anywhere later.",
            "",
            "&#F0A08A➔ Right-click a mob to CATCH it");

    public static final String FILLED_NAME = "&#E07A5F&l{mob} in a Bucket";
    public static final String FILLED_NAME_NAMED = "&#E07A5F&l{name} &7({mob} in a Bucket)";
    public static final String FILLED_CAUGHT_BY = "&#A8B4C4Caught by {player}.";
    public static final String FILLED_HEADER = "&#E07A5F&lBUCKET";
    public static final String FILLED_FACT = " &#F2B7A5◆ &7{label}: &f{value}";
    public static final String FILLED_RELEASE = "&#F0A08A➔ Right-click a block to RELEASE";

    public static final String INFO_NONE = "&cHold a filled bucket in your main hand first.";
    public static final String INFO_WORKS = "&#4ADE80works";
    public static final String INFO_COPY = "&#FF6B6Ba copy, or already used";
    public static final List<String> INFO_BOX = List.of(
            "&8&m                                                  ",
            "&#E07A5F&lBUCKET &8» &f{mob}",
            "",
            "  &#E07A5F» &7Caught by: &f{player}",
            "  &#E07A5F» &7Caught: &f{caught}",
            "  &#E07A5F» &7Code: &f{code} &8(&7{state}&8)",
            "  &#E07A5F» &7Stored as: &fdata version {version}, {size}",
            "&8&m                                                  ");

    public static final List<String> HELP = List.of(
            "&8&m                                                  ",
            "&#E07A5F&lANYTHING IN A BUCKET &8» &7Commands",
            "",
            "&#E07A5F/bucket give <player> [amount] &8- &7Give Capture Buckets",
            "&#E07A5F/bucket admin info &8- &7The filled bucket in your hand",
            "&#E07A5F/bucket admin reload &8- &7Reload config.yml",
            "&8&m                                                  ");

    private Messages() {}

    public static String fill(String line, String... pairs) {
        String out = line;
        for (int i = 0; i + 1 < pairs.length; i += 2) out = out.replace("{" + pairs[i] + "}", pairs[i + 1]);
        return out;
    }

    public static Component line(String line, String... pairs) {
        return Text.of(fill(line, pairs));
    }

    public static Component prefixed(String line, String... pairs) {
        return Text.of(PREFIX + fill(line, pairs));
    }

    public static void send(ServerPlayer player, String line, String... pairs) {
        player.sendSystemMessage(prefixed(line, pairs));
    }
}
