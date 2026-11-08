package net.inertiamc.anythinginabucket.catching;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The key the bucket resource pack uses for a mob's look: {@code villager/desert/librarian}, {@code sheep/red},
 * {@code tropical_fish/kob/orange/white}, or just the type ({@code creeper}). From 1.21.4 a filled bucket carries it as
 * its first custom model data string and the type as the second, so a look the pack doesn't know falls back to the
 * mob's default art.
 *
 * <p>{@link #of} mirrors the plugin's {@code HeadKey.of} and takes the plugin's getter names. {@link #fromSave} feeds it
 * from the mob's saved NBT, whose variant ids are stable across versions even where the classes move. Keep this, the
 * plugin and the pack generator in sync. No Minecraft classes, so it is unit tested.
 */
public final class HeadKey {

    private static final Map<String, String> HORSE_COLOURS = Map.of("dark_brown", "darkbrown");
    private static final Map<String, String> HORSE_MARKINGS = Map.of("whitefield", "white_field");
    private static final Map<String, String> RABBITS = Map.of(
            "black_and_white", "white_splotched", "salt_and_pepper", "salt", "the_killer_bunny", "caerbannog");
    private static final Map<String, String> PARROTS = Map.of("red", "red_blue", "cyan", "yellow_blue", "gray", "grey");

    // The game's saved ids, in id order.
    static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    static final List<String> HORSE_COATS = List.of("white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown");
    static final List<String> MARKINGS = List.of("none", "white", "white_field", "white_dots", "black_dots");
    static final List<String> LLAMAS = List.of("creamy", "white", "brown", "gray");
    static final List<String> PARROT_IDS = List.of("red_blue", "blue", "green", "yellow_blue", "gray");
    static final List<String> AXOLOTLS = List.of("lucy", "wild", "gold", "cyan", "blue");
    static final Map<Integer, String> RABBIT_IDS = Map.of(0, "brown", 1, "white", 2, "black", 3, "black_and_white",
            4, "gold", 5, "salt_and_pepper", 99, "the_killer_bunny");
    static final List<String> SMALL_FISH = List.of("kob", "sunstreak", "snooper", "dasher", "brinely", "spotty");
    static final List<String> LARGE_FISH = List.of("flopper", "stripey", "glitter", "blockfish", "betty", "clayfish");
    static final Set<String> RECESSIVE_GENES = Set.of("brown", "weak");

    private HeadKey() {}

    /**
     * @param type  the entity type id's path ({@code villager})
     * @param value a getter's value as a lower-case key ({@code getColor} to {@code red}), or null when there is none
     * @param name  the mob's custom name, plain, or null
     */
    public static String of(String type, Function<String, String> value, String name) {
        String variant = switch (type) {
            case "villager", "zombie_villager" -> {
                String biome = value.apply("getVillagerType");
                String job = value.apply("getProfession");
                if (biome == null) yield null;
                yield job == null || job.equals("none") ? biome : biome + "/" + job;
            }
            case "horse" -> {
                String coat = value.apply("getColor");
                String marks = value.apply("getStyle");
                if (coat == null) yield null;
                coat = HORSE_COLOURS.getOrDefault(coat, coat);
                marks = marks == null ? "none" : HORSE_MARKINGS.getOrDefault(marks, marks);
                yield marks.equals("none") ? coat : coat + "/" + marks;
            }
            case "sheep", "shulker" -> notDefault(value.apply("getColor"), type.equals("sheep") ? "white" : null);
            case "llama", "trader_llama" -> notDefault(value.apply("getColor"), "creamy");
            case "cat" -> value.apply("getCatType");
            case "wolf", "frog", "pig", "cow", "chicken", "zombie_nautilus" -> value.apply("getVariant");
            case "rabbit" -> {
                if ("Toast".equals(name)) yield "toast";
                String kind = value.apply("getRabbitType");
                yield notDefault(kind == null ? null : RABBITS.getOrDefault(kind, kind), "brown");
            }
            case "fox" -> notDefault(value.apply("getFoxType"), "red");
            case "parrot" -> {
                String colour = value.apply("getVariant");
                yield notDefault(colour == null ? null : PARROTS.getOrDefault(colour, colour), "red_blue");
            }
            case "panda" -> notDefault(value.apply("panda"), "normal");
            case "axolotl" -> notDefault(value.apply("getVariant"), "lucy");
            case "mooshroom" -> notDefault(value.apply("getVariant"), "red");
            case "copper_golem" -> notDefault(value.apply("copper"), "unaffected");
            case "tropical_fish" -> {
                String pattern = value.apply("getPattern");
                String body = value.apply("getBodyColor");
                String colour = value.apply("getPatternColor");
                yield pattern == null || body == null || colour == null ? null : pattern + "/" + body + "/" + colour;
            }
            default -> null;
        };
        return variant == null ? type : type + "/" + variant;
    }

    private static String notDefault(String value, String fallback) {
        return value == null || value.equals(fallback) ? null : value;
    }

    /**
     * The look of a mob from its save ({@link MobCodec#save}). {@code ints} and {@code strings} return a field or null;
     * {@code name} is the plain custom name.
     */
    public static String fromSave(String type, Function<String, Integer> ints, Function<String, String> strings, String name) {
        return of(type, getter -> value(type, ints, strings, getter), name);
    }

    private static int number(Function<String, Integer> ints, String key, int fallback) {
        Integer value = ints.apply(key);
        return value == null ? fallback : value;
    }

    private static String value(String type, Function<String, Integer> ints, Function<String, String> strings, String getter) {
        int variant = number(ints, "Variant", Integer.MIN_VALUE);
        return switch (getter) {
            case "getVillagerType" -> path(strings.apply("VillagerData.type"));
            case "getProfession" -> path(strings.apply("VillagerData.profession"));
            case "getColor" -> switch (type) {
                case "horse" -> variant == Integer.MIN_VALUE ? null : pick(HORSE_COATS, variant & 0xFF);
                case "llama", "trader_llama" -> pick(LLAMAS, variant);
                default -> pick(DYES, number(ints, "Color", -1));
            };
            case "getStyle" -> variant == Integer.MIN_VALUE ? null : pick(MARKINGS, (variant >> 8) & 0xFF);
            case "getCatType" -> path(strings.apply("variant"));
            case "getVariant" -> switch (type) {
                case "parrot" -> pick(PARROT_IDS, variant);
                case "axolotl" -> pick(AXOLOTLS, variant);
                case "mooshroom" -> strings.apply("Type");
                default -> path(strings.apply("variant"));
            };
            case "getRabbitType" -> RABBIT_IDS.get(number(ints, "RabbitType", -1));
            case "getFoxType" -> strings.apply("Type");
            case "panda" -> panda(strings.apply("MainGene"), strings.apply("HiddenGene"));
            case "copper" -> strings.apply("weather_state");
            case "getPattern" -> variant == Integer.MIN_VALUE ? null
                    : pick((variant & 0xFF) == 0 ? SMALL_FISH : (variant & 0xFF) == 1 ? LARGE_FISH : List.of(), (variant >> 8) & 0xFF);
            case "getBodyColor" -> variant == Integer.MIN_VALUE ? null : pick(DYES, (variant >> 16) & 0xFF);
            case "getPatternColor" -> variant == Integer.MIN_VALUE ? null : pick(DYES, (variant >>> 24) & 0xFF);
            default -> null;
        };
    }

    /** The visible gene: the main gene, unless it is recessive and the hidden gene differs. */
    static String panda(String main, String hidden) {
        if (main == null) return null;
        return RECESSIVE_GENES.contains(main) && !main.equals(hidden) ? "normal" : main;
    }

    private static String path(String id) {
        return id == null ? null : id.substring(id.indexOf(':') + 1);
    }

    private static String pick(List<String> names, int id) {
        return id >= 0 && id < names.size() ? names.get(id) : null;
    }

    /** The model strings: {@code [look, type]}, or {@code [type]} for a mob with one look. */
    public static List<String> strings(String type, String look) {
        return look.equals(type) ? List.of(type) : List.of(look, type);
    }
}
