package net.inertiamc.anythinginabucket.item;

import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.config.Settings;
import net.inertiamc.anythinginabucket.util.Compat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The Capture Bucket's shaped recipe, built from {@code capture-bucket.recipe}. */
public final class CaptureRecipe {

    //? if >=1.21.2 {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> KEY =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("anythinginabucket", "capture_bucket"));
    //?} else
    /*public static final Identifier KEY = Identifier.fromNamespaceAndPath("anythinginabucket", "capture_bucket");*/

    private CaptureRecipe() {}

    public static void unlock(ServerPlayer player) {
        if (Compat.server(player).getRecipeManager().byKey(KEY).isPresent()) player.awardRecipesByKey(List.of(KEY));
    }

    /** Null when the recipe is disabled or catch-with is normal-bucket. */
    public static RecipeHolder<?> build() {
        Settings settings = AnythingInABucket.settings();
        Settings.Recipe recipe = settings.recipe();
        if (!recipe.enabled() || !settings.catchWith().capture()) return null;
        try {
            Map<Character, Ingredient> key = new HashMap<>();
            for (Map.Entry<Character, String> entry : recipe.ingredients().entrySet()) {
                Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(entry.getValue())).orElse(null);
                if (item == null) {
                    AnythingInABucket.LOGGER.warn("config.yml: capture-bucket.recipe.ingredients.{}: '{}' is not an item; the recipe is off",
                            entry.getKey(), entry.getValue());
                    return null;
                }
                key.put(entry.getKey(), Ingredient.of(item));
            }
            int width = recipe.shape().stream().mapToInt(String::length).max().orElse(1);
            List<String> rows = new ArrayList<>();
            for (String row : recipe.shape()) rows.add(String.format("%-" + width + "s", row));
            key.keySet().removeIf(letter -> rows.stream().noneMatch(row -> row.indexOf(letter) >= 0));
            ShapedRecipePattern pattern = ShapedRecipePattern.of(key, rows);
            //? if >=26.1 {
            // Item components aren't bound yet while 26.x loads recipes, so the result must be a template.
            ShapedRecipe shaped = new ShapedRecipe(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                    new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(CraftingBookCategory.EQUIPMENT, ""), pattern,
                    new net.minecraft.world.item.ItemStackTemplate(net.minecraft.world.item.Items.BUCKET, BucketItems.captureBucketData()));
            //?} else
            /*ShapedRecipe shaped = new ShapedRecipe("", CraftingBookCategory.EQUIPMENT, pattern, BucketItems.captureBucket(1), true);*/
            return new RecipeHolder<>(KEY, shaped);
        } catch (RuntimeException e) {
            AnythingInABucket.LOGGER.warn("config.yml: the Capture Bucket recipe could not be made ({}); it is off", e.getMessage());
            return null;
        }
    }
}
