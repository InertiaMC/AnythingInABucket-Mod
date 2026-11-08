package net.inertiamc.anythinginabucket.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

//? if >=1.21.2 {
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeMap.class)
public interface RecipeMapAccessor {

    @Invoker("<init>")
    static RecipeMap anythinginabucket$create(Multimap<RecipeType<?>, RecipeHolder<?>> byType, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey) {
        throw new AssertionError();
    }

    @Accessor("byType")
    Multimap<RecipeType<?>, RecipeHolder<?>> anythinginabucket$byType();

    @Accessor("byKey")
    Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> anythinginabucket$byKey();
}
//?} else {
/*import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;

@Mixin(RecipeManager.class)
public interface RecipeMapAccessor {

    @Accessor("byType")
    Multimap<RecipeType<?>, RecipeHolder<?>> anythinginabucket$byType();

    @Accessor("byType")
    void anythinginabucket$setByType(Multimap<RecipeType<?>, RecipeHolder<?>> byType);

    @Accessor("byName")
    Map<Identifier, RecipeHolder<?>> anythinginabucket$byName();

    @Accessor("byName")
    void anythinginabucket$setByName(Map<Identifier, RecipeHolder<?>> byName);
}
*///?}
