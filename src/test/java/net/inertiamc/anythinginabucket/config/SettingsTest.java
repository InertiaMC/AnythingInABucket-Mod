package net.inertiamc.anythinginabucket.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    private static Map<String, Object> yaml(InputStream in) throws java.io.IOException {
        return FlatYaml.parse(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void shippedConfigReadsWithoutProblems() throws Exception {
        try (InputStream in = Settings.class.getResourceAsStream("/assets/anythinginabucket/config.yml")) {
            Settings s = Settings.read(yaml(in));
            assertTrue(s.problems().isEmpty(), s.problems().toString());
            assertEquals(Settings.CatchWith.CAPTURE_BUCKET, s.catchWith());
            assertTrue(s.blacklist().contains("minecraft:ender_dragon"));
            assertEquals(1000, s.cooldownMillis());
            assertTrue(s.recipe().enabled());
            assertEquals("minecraft:ender_pearl", s.recipe().ingredients().get('e'));
            assertTrue(s.copyProtection() && s.petsOwnerOnly() && s.respectClaims());
        }
    }

    /** The plugin's config.yml must load unchanged, since the settings are shared. */
    @Test
    void pluginConfigReadsTheSame() throws Exception {
        // Tests run in versions/<node>/, two levels below the repo root.
        Path plugin = Path.of("../../../../plugins/AnythingInABucket/src/main/resources/config.yml");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(plugin), "the plugin is not beside this repo");
        try (InputStream in = Files.newInputStream(plugin); InputStream ours = Settings.class.getResourceAsStream("/assets/anythinginabucket/config.yml")) {
            Settings theirs = Settings.read(yaml(in));
            Settings mine = Settings.read(yaml(ours));
            assertTrue(theirs.problems().isEmpty(), theirs.problems().toString());
            assertEquals(mine.catchWith(), theirs.catchWith());
            assertEquals(mine.giveBack(), theirs.giveBack());
            assertEquals(mine.dispensersRelease(), theirs.dispensersRelease());
            assertEquals(mine.blacklist(), theirs.blacklist());
            assertEquals(mine.cooldownMillis(), theirs.cooldownMillis());
            assertEquals(mine.recipe(), theirs.recipe());
            assertEquals(mine.sounds(), theirs.sounds());
            assertEquals(mine.copyProtection(), theirs.copyProtection());
            assertEquals(mine.petsOwnerOnly(), theirs.petsOwnerOnly());
            assertEquals(mine.respectClaims(), theirs.respectClaims());
            assertEquals(mine.resourcePack(), theirs.resourcePack());
        }
    }

    @Test
    void pluginWorldNamesMapToDimensions() {
        Settings s = Settings.read(Map.of("disabled-worlds", java.util.List.of("world_nether", "minecraft:the_end", "custom:mine")));
        assertEquals(java.util.Set.of("minecraft:the_nether", "minecraft:the_end", "custom:mine"), s.disabledWorlds());
    }

    @Test
    void badValuesKeepDefaultsAndAreNamed() {
        Settings s = Settings.read(Map.of("catch-with", "spoon", "cooldown", "soon", "give-bucket-back", "maybe"));
        assertEquals(Settings.CatchWith.CAPTURE_BUCKET, s.catchWith());
        assertEquals(1000, s.cooldownMillis());
        assertTrue(s.giveBack());
        assertEquals(3, s.problems().size());
    }

    @Test
    void onOffSettingsAreOnWhenMissingAndNamedWhenBad() {
        Settings missing = Settings.read(Map.of());
        assertTrue(missing.copyProtection() && missing.petsOwnerOnly() && missing.respectClaims());
        Settings off = Settings.read(Map.of("copy-protection", false, "pets-owner-only", false, "respect-claims", false));
        assertTrue(!off.copyProtection() && !off.petsOwnerOnly() && !off.respectClaims());
        assertTrue(off.problems().isEmpty(), off.problems().toString());
        Settings bad = Settings.read(Map.of("respect-claims", "nope"));
        assertTrue(bad.respectClaims());
        assertEquals(1, bad.problems().size());
    }

    @Test
    void durations() {
        assertEquals(1000L, Settings.duration("1s"));
        assertEquals(500L, Settings.duration("500ms"));
        assertEquals(120_000L, Settings.duration("2m"));
        assertEquals(30_000L, Settings.duration("30"));
        assertEquals(0L, Settings.duration("0s"));
    }

    @Test
    void recipeWithAMissingLetterIsOff() {
        Settings s = Settings.read(Map.of("capture-bucket", Map.of("recipe", Map.of(
                "shape", java.util.List.of("xbx"), "ingredients", Map.of("b", "bucket")))));
        assertTrue(!s.recipe().enabled());
    }
}
