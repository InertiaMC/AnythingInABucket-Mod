package net.inertiamc.anythinginabucket.item;

import net.inertiamc.anythinginabucket.catching.MobFacts;
import net.inertiamc.anythinginabucket.util.Compat;
import net.inertiamc.anythinginabucket.util.Messages;
import net.inertiamc.anythinginabucket.util.Text;
import net.inertiamc.anythinginabucket.util.Words;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Capture Bucket and filled buckets. Both are vanilla items, so vanilla clients display them. All data lives under
 * one {@code anythinginabucket} tag in custom data, including a filled bucket's whole mob.
 *
 * <p>Don't rename these keys: existing buckets would stop working.
 */
public final class BucketItems {

    public static final String ROOT = "anythinginabucket";
    public static final String KIND = "kind";
    public static final String CAPTURE = "capture";
    public static final String FILLED = "filled";
    public static final String CODE = "code";
    public static final String MOB = "mob";
    public static final String BUCKET = "bucket";
    public static final String PLAIN = "plain";
    public static final String CAUGHT_BY = "caught_by";
    public static final String CAUGHT_AT = "caught_at";
    public static final String DATA_VERSION = "data_version";
    public static final String ENTITY = "entity";

    private BucketItems() {}

    public record Filled(UUID code, String mob, boolean plain, String caughtBy, long caughtAt, int dataVersion, CompoundTag entity) {}

    public static ItemStack captureBucket(int amount) {
        return new ItemStack(Items.BUCKET.builtInRegistryHolder(), Math.max(1, Math.min(16, amount)), captureBucketData());
    }

    /** Components that turn a plain bucket into a Capture Bucket. Also used by the recipe, before components are bound. */
    public static DataComponentPatch captureBucketData() {
        CompoundTag root = new CompoundTag();
        root.putString(KIND, CAPTURE);
        return DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Text.of(Messages.BUCKET_NAME))
                .set(DataComponents.LORE, lore(Messages.BUCKET_LORE))
                .set(DataComponents.CUSTOM_DATA, CustomData.of(wrap(root)))
                .build();
    }

    public static ItemStack filled(EntityType<?> type, Component name, String catcher, List<MobFacts.Fact> facts,
                                   CompoundTag entity, int dataVersion, UUID code, boolean plain, String look) {
        Item egg = Compat.spawnEgg(type);
        ItemStack item = new ItemStack(egg == null ? Items.BUCKET : egg);
        if (egg == null) item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        String mob = Words.pretty(EntityType.getKey(type).getPath());
        item.set(DataComponents.CUSTOM_NAME, name == null
                ? Messages.line(Messages.FILLED_NAME, "mob", mob)
                : Messages.line(Messages.FILLED_NAME_NAMED, "mob", mob, "name", name.getString()));
        List<String> lines = new ArrayList<>();
        lines.add(Messages.fill(Messages.FILLED_CAUGHT_BY, "player", catcher));
        lines.add("");
        lines.add(Messages.FILLED_HEADER);
        for (MobFacts.Fact fact : facts) lines.add(Messages.fill(Messages.FILLED_FACT, "label", fact.label(), "value", fact.value()));
        lines.add("");
        lines.add(Messages.FILLED_RELEASE);
        item.set(DataComponents.LORE, lore(lines));
        item.set(DataComponents.MAX_STACK_SIZE, 1);
        artMarks(item, EntityType.getKey(type).getPath(), look);
        //? if >=1.21.5 {
        item.set(DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT.withHidden(DataComponents.ENTITY_DATA, true));
        //?} else
        /*item.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);*/

        CompoundTag root = new CompoundTag();
        root.putString(KIND, FILLED);
        Compat.putUuid(root, CODE, code);
        root.putString(MOB, EntityType.getKey(type).toString());
        root.putString(BUCKET, plain ? PLAIN : CAPTURE);
        root.putString(CAUGHT_BY, catcher);
        root.putLong(CAUGHT_AT, System.currentTimeMillis());
        root.putInt(DATA_VERSION, dataVersion);
        root.put(ENTITY, entity);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(wrap(root)));
        return item;
    }

    /**
     * Custom model data for the bucket art, matching the plugin: from 1.21.4 the strings {@code [look, type]} plus the
     * look's number as the first float; before 1.21.4 just the number.
     */
    private static void artMarks(ItemStack item, String typePath, String look) {
        Integer number = net.inertiamc.anythinginabucket.catching.BucketNumbers.of(look, typePath);
        //? if >=1.21.4 {
        item.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(
                number == null ? List.of() : List.of(number.floatValue()), List.of(),
                net.inertiamc.anythinginabucket.catching.HeadKey.strings(typePath, look), List.of()));
        //?} else {
        /*if (number != null) item.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(number));
        *///?}
    }

    public static boolean isCaptureBucket(ItemStack item) {
        CompoundTag root = root(item);
        return root != null && CAPTURE.equals(Compat.string(root, KIND, "")) && item.is(Items.BUCKET);
    }

    public static boolean isFilled(ItemStack item) {
        CompoundTag root = root(item);
        return root != null && FILLED.equals(Compat.string(root, KIND, ""));
    }

    public static boolean ours(ItemStack item) {
        return root(item) != null;
    }

    public static Filled filledOf(ItemStack item) {
        CompoundTag root = root(item);
        if (root == null || !FILLED.equals(Compat.string(root, KIND, ""))) return null;
        UUID code = Compat.uuid(root, CODE);
        CompoundTag entity = Compat.compound(root, ENTITY);
        if (code == null || entity == null) return null;
        return new Filled(code, Compat.string(root, MOB, "minecraft:pig"), PLAIN.equals(Compat.string(root, BUCKET, CAPTURE)),
                Compat.string(root, CAUGHT_BY, "?"), Compat.longValue(root, CAUGHT_AT, 0), Compat.intValue(root, DATA_VERSION, 0), entity);
    }

    private static CompoundTag root(ItemStack item) {
        if (item == null || item.isEmpty()) return null;
        CustomData data = item.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        return Compat.compound(data.copyTag(), ROOT);
    }

    private static CompoundTag wrap(CompoundTag root) {
        CompoundTag tag = new CompoundTag();
        tag.put(ROOT, root);
        return tag;
    }

    private static ItemLore lore(List<String> lines) {
        return new ItemLore(lines.stream().map(line -> (Component) Text.of(line)).toList());
    }
}
