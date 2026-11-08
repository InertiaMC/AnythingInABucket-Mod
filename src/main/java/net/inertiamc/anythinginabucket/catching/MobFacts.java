package net.inertiamc.anythinginabucket.catching;

import net.inertiamc.anythinginabucket.util.Compat;
import net.inertiamc.anythinginabucket.util.Words;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.Set;
import java.util.UUID;

/** The lore lines of a filled bucket, read from the mob when it is caught. */
public final class MobFacts {

    public record Fact(String label, String value) {}

    private static final String[] LEVELS = {"Novice", "Apprentice", "Journeyman", "Expert", "Master"};
    /** Mobs that can wear a saddle. Not {@code canUseSlot(SADDLE)}, which is true for nearly every mob. */
    private static final Set<String> SADDLED = Set.of("horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel",
            "camel_husk", "pig", "strider", "nautilus", "zombie_nautilus");
    /** Babies that aren't AgeableMob. */
    private static final Set<String> BABIES = Set.of("zombie", "husk", "drowned", "zombie_villager", "zombified_piglin",
            "piglin", "zoglin");
    private static final Set<String> NO_AGE = Set.of("slime", "magma_cube", "piglin_brute");
    private static final Set<String> CUBES = Set.of("slime", "magma_cube", "sulfur_cube");
    /** Variant names that read better reworded; the rest keep the game's name. */
    private static final Map<String, Map<String, String>> RENAMED = Map.of(
            "parrot", Map.of("red_blue", "Red", "yellow_blue", "Cyan"),
            "rabbit", Map.of("salt", "Salt and pepper", "white_splotched", "Black and white", "evil", "Killer bunny"));

    private MobFacts() {}

    /** {@code ints} and {@code strings} read the mob's saved NBT, as in {@link HeadKey#fromSave}. */
    public static List<Fact> read(Mob mob, MinecraftServer server, Function<String, Integer> ints, Function<String, String> strings) {
        List<Fact> facts = new ArrayList<>();
        String type = EntityType.getKey(mob.getType()).getPath();
        facts.add(new Fact("Health", Words.number(mob.getHealth()) + "/" + Words.number(mob.getMaxHealth())));
        if (!NO_AGE.contains(type) && (mob instanceof AgeableMob || BABIES.contains(type))) {
            facts.add(new Fact("Age", mob.isBaby() ? "Baby" : "Adult"));
        }
        if (mob instanceof OwnableEntity ownable) {
            UUID owner = Compat.owner(ownable);
            if (owner != null) facts.add(new Fact("Owner", ownerName(owner, server)));
        }
        variants(mob, facts);
        renamed(type, facts);
        if (mob instanceof Horse horse) facts.add(new Fact("Markings", Words.pretty(horse.getMarkings().name())));
        saved(type, ints, strings, facts);
        if (mob instanceof Villager villager) {
            //? if >=1.21.5 {
            villager.getVillagerData().profession().unwrapKey()
                    .ifPresent(key -> facts.add(new Fact("Profession", Words.pretty(Compat.key(key).getPath()))));
            int level = villager.getVillagerData().level();
            //?} else {
            /*facts.add(new Fact("Profession", Words.pretty(net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION
                    .getKey(villager.getVillagerData().getProfession()).getPath())));
            int level = villager.getVillagerData().getLevel();
            *///?}
            facts.add(new Fact("Level", LEVELS[Math.max(1, Math.min(LEVELS.length, level)) - 1]));
        }
        if (mob instanceof AbstractVillager merchant) facts.add(new Fact("Trades", String.valueOf(merchant.getOffers().size())));
        if (mob instanceof Sheep sheep && sheep.isSheared()) facts.add(new Fact("Sheared", "Yes"));

        boolean horse = mob instanceof AbstractHorse && !(mob instanceof Llama);
        if (SADDLED.contains(type)) {
            //? if >=1.21.5 {
            boolean saddled = worn(mob, EquipmentSlot.SADDLE) != null;
            //?} else
            /*boolean saddled = mob instanceof net.minecraft.world.entity.Saddleable saddleable && saddleable.isSaddled();*/
            facts.add(new Fact("Saddle", saddled ? "Yes" : "No"));
        }
        ItemStack armour = worn(mob, EquipmentSlot.BODY);
        if (armour != null) facts.add(new Fact("Armour", Words.pretty(Compat.itemPath(armour))));
        if (mob instanceof AbstractChestedHorse chested && chested.hasChest()) {
            int items = 0;
            for (int slot = 0; slot < chested.getInventoryColumns() * 3; slot++) {
                if (!chested.getSlot(slot + 500).get().isEmpty()) items++;
            }
            facts.add(new Fact("Chest", items == 1 ? "1 item" : items + " items"));
        }
        if (horse) {
            facts.add(new Fact("Speed", Words.number(Words.blocksPerSecond(mob.getAttributeValue(Attributes.MOVEMENT_SPEED))) + " blocks/s"));
            if (!(mob instanceof Camel)) {
                facts.add(new Fact("Jump", Words.number(Words.jumpHeight(mob.getAttributeValue(Attributes.JUMP_STRENGTH))) + " blocks"));
            }
        }
        return facts;
    }

    /** Colour and variant: from data components on 1.21.5+, from the entity's getters before. */
    private static void variants(Mob mob, List<Fact> facts) {
        tropicalFish(mob, facts);
        //? if >=1.21.5 {
        Object[][] variants = {
                {"Colour", net.minecraft.core.component.DataComponents.SHEEP_COLOR},
                {"Colour", net.minecraft.core.component.DataComponents.HORSE_VARIANT},
                {"Colour", net.minecraft.core.component.DataComponents.LLAMA_VARIANT},
                {"Colour", net.minecraft.core.component.DataComponents.SHULKER_COLOR},
                {"Variant", net.minecraft.core.component.DataComponents.CAT_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.WOLF_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.FOX_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.RABBIT_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.PARROT_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.AXOLOTL_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.FROG_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.PIG_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.COW_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.CHICKEN_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.MOOSHROOM_VARIANT},
                {"Variant", net.minecraft.core.component.DataComponents.SALMON_SIZE},
                //? if >=1.21.11 {
                {"Variant", net.minecraft.core.component.DataComponents.ZOMBIE_NAUTILUS_VARIANT},
                //?}
                {"Variant", net.minecraft.core.component.DataComponents.VILLAGER_VARIANT},
        };
        for (Object[] variant : variants) {
            String value = words(mob.get((net.minecraft.core.component.DataComponentType<?>) variant[1]));
            String label = (String) variant[0];
            if (value != null && facts.stream().noneMatch(f -> f.label().equals(label))) facts.add(new Fact(label, value));
        }
        //?} else {
        /*Object colour = null;
        Object variant = null;
        if (mob instanceof Sheep sheep) colour = sheep.getColor();
        else if (mob instanceof Horse horse) colour = horse.getVariant();
        else if (mob instanceof Llama llama) colour = llama.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.Cat cat) variant = cat.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.Wolf wolf) variant = wolf.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.Fox fox) variant = fox.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.Rabbit rabbit) variant = rabbit.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.Parrot parrot) variant = parrot.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.axolotl.Axolotl axolotl) variant = axolotl.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.frog.Frog frog) variant = frog.getVariant();
        else if (mob instanceof net.minecraft.world.entity.animal.MushroomCow cow) variant = cow.getVariant();
        else if (mob instanceof net.minecraft.world.entity.npc.VillagerDataHolder villager) variant = net.minecraft.core.registries.BuiltInRegistries.VILLAGER_TYPE
                .getKey(villager.getVillagerData().getType()).getPath();
        String colourWords = words(colour);
        String variantWords = variant instanceof String text ? Words.pretty(text) : words(variant);
        if (colourWords != null) facts.add(new Fact("Colour", colourWords));
        if (variantWords != null) facts.add(new Fact("Variant", variantWords));
        *///?}
        //? if >=1.21.2 && <1.21.5 {
        /*if (mob instanceof net.minecraft.world.entity.animal.Salmon salmon) facts.add(new Fact("Variant", words(salmon.getVariant())));
        *///?}
    }

    private static void renamed(String type, List<Fact> facts) {
        Map<String, String> names = RENAMED.get(type);
        if (names == null) return;
        facts.replaceAll(fact -> {
            if (!fact.label().equals("Variant")) return fact;
            for (Map.Entry<String, String> name : names.entrySet()) {
                if (fact.value().equals(Words.pretty(name.getKey()))) return new Fact("Variant", name.getValue());
            }
            return fact;
        });
    }

    /** Facts only available from the save: panda personality, copper golem rust, slime size, zombie villager profession. */
    private static void saved(String type, Function<String, Integer> ints, Function<String, String> strings, List<Fact> facts) {
        if (type.equals("panda")) {
            String main = strings.apply("MainGene");
            if (main != null) {
                String hidden = strings.apply("HiddenGene");
                facts.add(new Fact("Personality", Words.pretty(HeadKey.panda(main, hidden == null ? main : hidden))));
            }
        }
        if (type.equals("copper_golem")) {
            String rust = strings.apply("weather_state");
            if (rust != null) facts.add(new Fact("Rust", rust.equals("unaffected") ? "None" : Words.pretty(rust)));
        }
        if (CUBES.contains(type)) {
            Integer saved = ints.apply("Size");
            if (saved != null) {
                int size = saved + 1;
                facts.add(new Fact("Size", size <= 1 ? "Small" : size == 2 ? "Medium" : "Big"));
            }
        }
        if (type.equals("zombie_villager")) {
            String job = strings.apply("VillagerData.profession");
            if (job != null) facts.add(new Fact("Profession", Words.pretty(job.substring(job.indexOf(':') + 1))));
        }
    }

    /** "Colour: Orange and white" (base, then pattern) or "Colour: Orange", then "Variant: Kob". */
    private static void tropicalFish(Mob mob, List<Fact> facts) {
        //? if >=1.21.5 {
        Object pattern = mob.get(net.minecraft.core.component.DataComponents.TROPICAL_FISH_PATTERN);
        Object base = mob.get(net.minecraft.core.component.DataComponents.TROPICAL_FISH_BASE_COLOR);
        Object over = mob.get(net.minecraft.core.component.DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        //?} else {
        /*if (!(mob instanceof net.minecraft.world.entity.animal.TropicalFish fish)) return;
        Object pattern = fish.getVariant();
        Object base = fish.getBaseColor();
        Object over = fish.getPatternColor();
        *///?}
        String baseWords = words(base);
        String overWords = words(over);
        if (baseWords != null) {
            facts.add(new Fact("Colour", overWords == null || base == over ? baseWords
                    : baseWords + " and " + overWords.toLowerCase(Locale.ROOT)));
        }
        String patternWords = words(pattern);
        if (patternWords != null) facts.add(new Fact("Variant", patternWords));
    }

    public static String ownerName(UUID owner, MinecraftServer server) {
        return Compat.knownName(server, owner).orElseGet(() -> {
            LivingEntity online = server.getPlayerList().getPlayer(owner);
            return online instanceof net.minecraft.world.entity.player.Player player ? Compat.name(player) : "Unknown";
        });
    }

    private static ItemStack worn(Mob mob, EquipmentSlot slot) {
        ItemStack item = mob.getItemBySlot(slot);
        return item.isEmpty() ? null : item;
    }

    private static String words(Object value) {
        if (value == null) return null;
        if (value instanceof Holder<?> holder) return holder.unwrapKey().map(k -> Words.pretty(Compat.key(k).getPath())).orElse(null);
        if (value instanceof StringRepresentable named) return Words.pretty(named.getSerializedName());
        if (value instanceof Enum<?> constant) return Words.pretty(constant.name());
        return null;
    }
}
