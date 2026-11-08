package net.inertiamc.anythinginabucket.catching;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every look the mod can produce exists in the plugin's pack, and saves decode to the plugin's names. */
class HeadKeyTest {

    private static final Path HEADS = Path.of("../../../../plugins/AnythingInABucket/resourcepack/heads.txt");
    private static Set<String> heads;

    @BeforeAll
    static void readThePacksHeads() throws IOException {
        Path file = Files.exists(HEADS) ? HEADS : Path.of("../../plugins/AnythingInABucket/resourcepack/heads.txt");
        heads = Files.exists(file) ? new HashSet<>(Files.readAllLines(file)) : null;
    }

    /** A mob's save as the fields HeadKey reads: numbers and strings by key. */
    private static final class Save {
        final Map<String, Integer> ints = new java.util.HashMap<>();
        final Map<String, String> strings = new java.util.HashMap<>();

        Save putInt(String key, int value) {
            ints.put(key, value);
            return this;
        }

        Save putString(String key, String value) {
            strings.put(key, value);
            return this;
        }
    }

    private static String save(String type, Save tag) {
        return HeadKey.fromSave(type, tag.ints::get, tag.strings::get, null);
    }

    private static Save variant(int value) {
        return new Save().putInt("Variant", value);
    }

    private static void inPack(List<String> keys) {
        org.junit.jupiter.api.Assumptions.assumeTrue(heads != null, "the plugin's resourcepack/heads.txt is not beside this repo");
        List<String> missing = keys.stream().filter(k -> !heads.contains(k)).toList();
        assertEquals(List.of(), missing, "looks the mod names that the pack has no head for");
    }

    @Test
    void savesDecodeToThePluginsNames() {
        Save villager = new Save().putString("VillagerData.type", "minecraft:desert")
                .putString("VillagerData.profession", "minecraft:librarian");
        assertEquals("villager/desert/librarian", save("villager", villager));

        assertEquals("horse/gray/white_field", save("horse", variant(5 | 2 << 8)));
        assertEquals("horse/darkbrown", save("horse", variant(6)));

        Save sheep = new Save();
        sheep.putInt("Color", 14);
        assertEquals("sheep/red", save("sheep", sheep));
        assertEquals("sheep", save("sheep", new Save()));

        Save cat = new Save();
        cat.putString("variant", "minecraft:all_black");
        assertEquals("cat/all_black", save("cat", cat));

        // Kob (small, 0), orange body (1), white pattern (0).
        assertEquals("tropical_fish/kob/orange/white", save("tropical_fish", variant(0 | 1 << 16)));
        assertEquals("tropical_fish/clayfish/black/red", save("tropical_fish", variant(1 | 5 << 8 | 15 << 16 | 14 << 24)));

        Save rabbit = new Save();
        rabbit.putInt("RabbitType", 99);
        assertEquals("rabbit/caerbannog", save("rabbit", rabbit));
        assertEquals("rabbit/toast", HeadKey.fromSave("rabbit", k -> null, k -> null, "Toast"));

        assertEquals("parrot/grey", save("parrot", variant(4)));
        assertEquals("parrot", save("parrot", variant(0)));
        assertEquals("creeper", save("creeper", new Save()));
        assertEquals(List.of("sheep/red", "sheep"), HeadKey.strings("sheep", "sheep/red"));
        assertEquals(List.of("creeper"), HeadKey.strings("creeper", "creeper"));
    }

    /** numbers.txt is bundled at build time; before 1.21.4 the art is picked by number. */
    @Test
    void everyLookInThePackHasItsOwnNumber() {
        org.junit.jupiter.api.Assumptions.assumeTrue(heads != null, "the plugin's resourcepack/heads.txt is not beside this repo");
        assertTrue(BucketNumbers.size() >= heads.size(), "numbers.txt is not bundled: " + BucketNumbers.size());
        List<String> missing = heads.stream().filter(k -> BucketNumbers.of(k, "?") == null).toList();
        assertEquals(List.of(), missing, "looks with no number");
        assertEquals(BucketNumbers.of("sheep", "sheep"), BucketNumbers.of("sheep/no_such_look", "sheep"));
    }

    @Test
    void pandasShowTheGeneTheyWear() {
        assertEquals("normal", HeadKey.panda("weak", "lazy"));
        assertEquals("weak", HeadKey.panda("weak", "weak"));
        assertEquals("lazy", HeadKey.panda("lazy", "normal"));
    }

    @Test
    void everyLookFromTheSavedIdsIsInThePack() {
        List<String> all = new ArrayList<>();
        for (String biome : List.of("desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga")) {
            for (String job : List.of("none", "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
                    "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith")) {
                for (String type : List.of("villager", "zombie_villager")) {
                    all.add(HeadKey.of(type, Map.of("getVillagerType", biome, "getProfession", job)::get, null));
                }
            }
        }
        for (int coat = 0; coat < HeadKey.HORSE_COATS.size(); coat++)
            for (int marks = 0; marks < HeadKey.MARKINGS.size(); marks++) all.add(save("horse", variant(coat | marks << 8)));
        for (int dye = 0; dye < 16; dye++) {
            Save tag = new Save();
            tag.putInt("Color", dye);
            all.add(save("sheep", tag));
            all.add(save("shulker", tag));
        }
        for (int i = 0; i < HeadKey.LLAMAS.size(); i++) {
            all.add(save("llama", variant(i)));
            all.add(save("trader_llama", variant(i)));
        }
        for (int i = 0; i < HeadKey.PARROT_IDS.size(); i++) all.add(save("parrot", variant(i)));
        for (int i = 0; i < HeadKey.AXOLOTLS.size(); i++) all.add(save("axolotl", variant(i)));
        for (int id : HeadKey.RABBIT_IDS.keySet()) {
            Save tag = new Save();
            tag.putInt("RabbitType", id);
            all.add(save("rabbit", tag));
        }
        for (String[] typed : new String[][] {{"fox", "red"}, {"fox", "snow"}, {"mooshroom", "red"}, {"mooshroom", "brown"}}) {
            Save tag = new Save();
            tag.putString("Type", typed[1]);
            all.add(save(typed[0], tag));
        }
        for (String state : List.of("unaffected", "exposed", "weathered", "oxidized")) {
            Save tag = new Save();
            tag.putString("weather_state", state);
            all.add(save("copper_golem", tag));
        }
        List<String> genes = List.of("normal", "lazy", "worried", "playful", "brown", "weak", "aggressive");
        for (String main : genes) {
            for (String hidden : genes) {
                Save tag = new Save();
                tag.putString("MainGene", main);
                tag.putString("HiddenGene", hidden);
                all.add(save("panda", tag));
            }
        }
        for (int size = 0; size < 2; size++)
            for (int pattern = 0; pattern < 6; pattern++)
                for (int body = 0; body < 16; body++)
                    for (int colour = 0; colour < 16; colour++)
                        all.add(save("tropical_fish", variant(size | pattern << 8 | body << 16 | colour << 24)));
        assertTrue(all.size() > 3000);
        inPack(all);
    }
}
