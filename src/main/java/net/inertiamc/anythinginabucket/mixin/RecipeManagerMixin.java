package net.inertiamc.anythinginabucket.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import net.inertiamc.anythinginabucket.item.CaptureRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//? if >=1.21.2 {
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the Capture Bucket recipe on every data pack load. It is a plain shaped recipe, so vanilla clients show it in
 * the recipe book.
 */
@Mixin(RecipeMap.class)
abstract class RecipeManagerMixin {

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private static void anythinginabucket$addCaptureBucket(CallbackInfoReturnable<RecipeMap> cir) {
        RecipeHolder<?> ours = CaptureRecipe.build();
        if (ours == null) return;
        RecipeMapAccessor loaded = (RecipeMapAccessor) cir.getReturnValue();
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
        byType.putAll(loaded.anythinginabucket$byType());
        byType.put(ours.value().getType(), ours);
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
        byKey.putAll(loaded.anythinginabucket$byKey());
        byKey.put(ours.id(), ours);
        cir.setReturnValue(RecipeMapAccessor.anythinginabucket$create(byType.build(), byKey.buildKeepingLast()));
    }
}
//?} else {
/*import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^* Before 1.21.2 the recipes live in RecipeManager itself. ^/
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    private void anythinginabucket$addCaptureBucket(CallbackInfo ci) {
        RecipeHolder<?> ours = CaptureRecipe.build();
        if (ours == null) return;
        RecipeMapAccessor self = (RecipeMapAccessor) this;
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
        byType.putAll(self.anythinginabucket$byType());
        byType.put(ours.value().getType(), ours);
        ImmutableMap.Builder<Identifier, RecipeHolder<?>> byName = ImmutableMap.builder();
        byName.putAll(self.anythinginabucket$byName());
        byName.put(ours.id(), ours);
        self.anythinginabucket$setByType(byType.build());
        self.anythinginabucket$setByName(byName.buildKeepingLast());
    }
}
*///?}
