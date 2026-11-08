package net.inertiamc.anythinginabucket.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed config.yml. An invalid value falls back to its default and is listed in {@link #problems()}. No Minecraft
 * classes, so it is unit tested.
 */
public record Settings(CatchWith catchWith, boolean giveBack, boolean dispensersRelease, Set<String> blacklist, long cooldownMillis,
                       Set<String> disabledWorlds, boolean copyProtection, boolean petsOwnerOnly, boolean respectClaims,
                       ResourcePack resourcePack, Recipe recipe, Map<String, Sound> sounds, List<String> problems) {

    public enum CatchWith {
        CAPTURE_BUCKET, NORMAL_BUCKET, BOTH;

        public boolean capture() {
            return this != NORMAL_BUCKET;
        }

        public boolean normal() {
            return this != CAPTURE_BUCKET;
        }
    }

    public record Recipe(boolean enabled, List<String> shape, Map<Character, String> ingredients) {}

    public record Sound(String name, float volume, float pitch) {}

    /** The bucket resource pack, offered to players as they join when {@code send} is on. */
    public record ResourcePack(boolean send, String url) {}

    /** Empty: the newest version on Modrinth. */
    public static final String DEFAULT_PACK_URL = "";

    private static final Pattern DURATION = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ms|s|m|h)?");

    /** The plugin's world folder names, so its config works unchanged. */
    private static final Map<String, String> PLUGIN_WORLDS = Map.of(
            "world", "minecraft:overworld",
            "world_nether", "minecraft:the_nether",
            "world_the_end", "minecraft:the_end");

    public static Settings read(Map<String, Object> yaml) {
        List<String> problems = new ArrayList<>();

        CatchWith catchWith = CatchWith.CAPTURE_BUCKET;
        Object rawCatch = yaml.get("catch-with");
        if (rawCatch != null) {
            try {
                catchWith = CatchWith.valueOf(rawCatch.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                problems.add("catch-with: '" + rawCatch + "' is not capture-bucket, normal-bucket or both");
            }
        }

        boolean giveBack = flag(yaml, "give-bucket-back", problems);
        boolean dispensersRelease = flag(yaml, "dispensers-release", problems);

        Set<String> blacklist = new LinkedHashSet<>();
        for (String mob : strings(yaml, "blacklist", List.of("wither", "ender_dragon", "warden", "elder_guardian"), problems)) {
            blacklist.add(id(mob));
        }

        long cooldown = 1000;
        Object rawCooldown = yaml.get("cooldown");
        if (rawCooldown != null) {
            Long parsed = duration(rawCooldown.toString());
            if (parsed == null) problems.add("cooldown: '" + rawCooldown + "' is not a time like 1s");
            else cooldown = parsed;
        }

        Set<String> worlds = new LinkedHashSet<>();
        for (String world : strings(yaml, "disabled-worlds", List.of(), problems)) {
            String key = world.trim().toLowerCase(Locale.ROOT);
            worlds.add(PLUGIN_WORLDS.getOrDefault(key, id(key)));
        }

        boolean copyProtection = flag(yaml, "copy-protection", problems);
        boolean petsOwnerOnly = flag(yaml, "pets-owner-only", problems);
        boolean respectClaims = flag(yaml, "respect-claims", problems);

        Map<String, Object> rawPack = section(yaml, "resource-pack", problems);
        boolean sendPack = false;
        if (rawPack.get("send") instanceof Boolean b) sendPack = b;
        else if (rawPack.get("send") != null) problems.add("resource-pack.send: '" + rawPack.get("send") + "' is not true or false");
        String packUrl = rawPack.get("url") == null ? DEFAULT_PACK_URL : rawPack.get("url").toString().trim();
        ResourcePack resourcePack = new ResourcePack(sendPack, packUrl);

        Recipe recipe = recipe(section(yaml, "capture-bucket", problems), problems);

        Map<String, Sound> sounds = new LinkedHashMap<>();
        sounds.put("catch", new Sound("item.bucket.fill_axolotl", 1, 1));
        sounds.put("release", new Sound("item.bucket.empty_axolotl", 1, 1));
        sounds.put("deny", new Sound("entity.villager.no", 1, 1));
        Map<String, Object> rawSounds = section(yaml, "sounds", problems);
        for (String key : List.copyOf(sounds.keySet())) {
            if (!rawSounds.containsKey(key)) continue;
            Object raw = rawSounds.get(key);
            if (raw == null || raw.toString().isBlank()) {
                sounds.put(key, null);
            } else if (raw instanceof Map<?, ?> map && map.get("name") != null) {
                sounds.put(key, new Sound(map.get("name").toString(), number(map.get("volume"), 1), number(map.get("pitch"), 1)));
            } else if (raw instanceof String name) {
                sounds.put(key, new Sound(name, 1, 1));
            } else {
                problems.add("sounds." + key + ": '" + raw + "' is not a sound key or { name, volume, pitch }");
            }
        }

        return new Settings(catchWith, giveBack, dispensersRelease, Set.copyOf(blacklist), cooldown, Set.copyOf(worlds), copyProtection,
                petsOwnerOnly, respectClaims, resourcePack, recipe,
                java.util.Collections.unmodifiableMap(sounds), List.copyOf(problems));
    }

    private static Recipe recipe(Map<String, Object> bucket, List<String> problems) {
        List<String> defaultShape = List.of(" e ", "ebe", " l ");
        Map<Character, String> defaultIngredients = Map.of('e', "minecraft:ender_pearl", 'b', "minecraft:bucket", 'l', "minecraft:lead");
        Object rawRecipe = bucket.get("recipe");
        if (rawRecipe == null) return new Recipe(true, defaultShape, defaultIngredients);
        if (!(rawRecipe instanceof Map<?, ?> map)) {
            problems.add("capture-bucket.recipe is not a section");
            return new Recipe(true, defaultShape, defaultIngredients);
        }
        boolean enabled = !(map.get("enabled") instanceof Boolean b) || b;

        List<String> shape = defaultShape;
        if (map.get("shape") instanceof List<?> rows && !rows.isEmpty() && rows.size() <= 3
                && rows.stream().allMatch(r -> r != null && !r.toString().isEmpty() && r.toString().length() <= 3)) {
            shape = rows.stream().map(Object::toString).toList();
        } else if (map.containsKey("shape")) {
            problems.add("capture-bucket.recipe.shape must be up to three rows of up to three letters");
        }

        Map<Character, String> ingredients = new LinkedHashMap<>();
        if (map.get("ingredients") instanceof Map<?, ?> raw) {
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String letter = String.valueOf(entry.getKey());
                if (letter.length() != 1 || entry.getValue() == null) {
                    problems.add("capture-bucket.recipe.ingredients." + letter + " must be one letter with an item");
                    continue;
                }
                ingredients.put(letter.charAt(0), id(entry.getValue().toString()));
            }
        } else {
            ingredients.putAll(defaultIngredients);
        }
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && !ingredients.containsKey(c)) {
                    problems.add("capture-bucket.recipe: the letter '" + c + "' in the shape has no ingredient; the recipe is off");
                    enabled = false;
                }
            }
        }
        return new Recipe(enabled, shape, Map.copyOf(ingredients));
    }

    /** "1s", "30s", "500ms", "2m", "1h", or a bare number of seconds, in milliseconds; null when unreadable. */
    public static Long duration(String text) {
        Matcher m = DURATION.matcher(text.trim().toLowerCase(Locale.ROOT));
        if (!m.matches()) return null;
        double value = Double.parseDouble(m.group(1));
        String unit = m.group(2) == null ? "s" : m.group(2);
        double millis = switch (unit) {
            case "ms" -> value;
            case "m" -> value * 60_000;
            case "h" -> value * 3_600_000;
            default -> value * 1000;
        };
        return Math.round(millis);
    }

    /** A game id with its namespace: {@code pig} is {@code minecraft:pig}. */
    public static String id(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        return key.contains(":") ? key : "minecraft:" + key;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> yaml, String key, List<String> problems) {
        Object raw = yaml.get(key);
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) return (Map<String, Object>) map;
        problems.add(key + " is not a section");
        return Map.of();
    }

    private static List<String> strings(Map<String, Object> yaml, String key, List<String> fallback, List<String> problems) {
        if (!yaml.containsKey(key)) return fallback;
        Object raw = yaml.get(key);
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) return list.stream().filter(java.util.Objects::nonNull).map(Object::toString).toList();
        problems.add(key + " is not a list");
        return fallback;
    }

    /** An on/off setting, on when missing. */
    private static boolean flag(Map<String, Object> yaml, String key, List<String> problems) {
        Object raw = yaml.get(key);
        if (raw instanceof Boolean b) return b;
        if (raw != null) problems.add(key + ": '" + raw + "' is not true or false");
        return true;
    }

    private static float number(Object raw, float fallback) {
        if (raw instanceof Number n) return n.floatValue();
        try {
            return raw == null ? fallback : Float.parseFloat(raw.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
