package net.inertiamc.anythinginabucket.catching;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.config.Settings;
import net.inertiamc.anythinginabucket.util.Messages;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Offers the bucket resource pack to players as they join ({@code resource-pack.send}). With an empty {@code url} the
 * newest version on Modrinth is used, with the SHA-1 Modrinth reports; a custom URL is downloaded once to hash it. Both
 * lookups run off-thread at startup and on reload. If a custom URL can't be downloaded it is offered without a hash.
 */
public final class HeadPack {

    /** Fixed pack id (same as the plugin's), so a rejoin or reload replaces the pack instead of stacking it. */
    static final UUID PACK_ID = UUID.nameUUIDFromBytes("anythinginabucket:heads".getBytes(StandardCharsets.UTF_8));
    static final String MODRINTH_PROJECT = "anythinginabucket-resource-pack";
    private static final String MODRINTH_VERSIONS =
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT + "/version?include_changelog=false";
    private static final int MAX_BYTES = 50 * 1024 * 1024;

    /** The resolved URL and SHA-1 (empty for none), keyed by the url setting it was resolved from. */
    private record Found(String setting, String url, String sha1) {}

    private static volatile Found found;

    private HeadPack() {}

    public static void refresh(Settings.ResourcePack pack) {
        if (!pack.send()) return;
        String url = pack.url();
        Thread thread = new Thread(() -> {
            if (url.isBlank()) {
                try {
                    found = newestOnModrinth();
                    AnythingInABucket.LOGGER.info("Offering the bucket resource pack from Modrinth to players as they join.");
                } catch (IOException | RuntimeException e) {
                    found = null;
                    AnythingInABucket.LOGGER.warn("Could not find the bucket resource pack on Modrinth, so it is not offered "
                            + "until a reload or restart reaches it: {}", e.getMessage());
                }
                return;
            }
            try {
                found = new Found(url, url, HexFormat.of().formatHex(sha1(url)));
                AnythingInABucket.LOGGER.info("Offering the bucket resource pack to players as they join.");
            } catch (IOException | NoSuchAlgorithmException | RuntimeException e) {
                found = new Found(url, url, "");
                AnythingInABucket.LOGGER.warn("Could not download the bucket resource pack from {} to check it; players are offered "
                        + "it without a check. {}", url, e.getMessage());
            }
        }, "AnythingInABucket head pack");
        thread.setDaemon(true);
        thread.start();
    }

    public static void offer(ServerPlayer player) {
        Settings.ResourcePack pack = AnythingInABucket.settings().resourcePack();
        Found now = found;
        if (!pack.send() || now == null || !now.setting().equals(pack.url())) return;
        // Clients with the mod already have the art.
        if (AnythingInABucket.hasArt(player)) return;
        player.connection.send(new ClientboundResourcePackPushPacket(PACK_ID, now.url(), now.sha1(), false,
                Optional.of(Messages.line(Messages.RESOURCE_PACK_PROMPT))));
    }

    /** Modrinth lists versions newest first; take the first one's primary file. */
    private static Found newestOnModrinth() throws IOException {
        HttpURLConnection connection = open(MODRINTH_VERSIONS);
        if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
        JsonArray versions;
        try (InputStream in = connection.getInputStream()) {
            versions = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        if (versions.isEmpty()) throw new IOException("the project has no versions yet");
        JsonObject file = null;
        for (JsonElement f : versions.get(0).getAsJsonObject().getAsJsonArray("files")) {
            if (file == null || f.getAsJsonObject().get("primary").getAsBoolean()) file = f.getAsJsonObject();
        }
        if (file == null) throw new IOException("its newest version has no file");
        return new Found("", file.get("url").getAsString(), file.getAsJsonObject("hashes").get("sha1").getAsString());
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        // Modrinth requires a descriptive User-Agent.
        connection.setRequestProperty("User-Agent", "InertiaMC/AnythingInABucket (github.com/InertiaMC/AnythingInABucket-Mod)");
        return connection;
    }

    private static byte[] sha1(String url) throws IOException, NoSuchAlgorithmException {
        HttpURLConnection connection = open(url);
        if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            for (int n; (n = in.read(buffer)) > 0; ) {
                total += n;
                if (total > MAX_BYTES) throw new IOException("larger than 50 MB");
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }
}
