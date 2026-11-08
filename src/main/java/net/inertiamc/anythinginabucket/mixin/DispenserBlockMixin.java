package net.inertiamc.anythinginabucket.mixin;

import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.catching.BucketManager;
import net.inertiamc.anythinginabucket.item.BucketItems;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dispensers release a filled bucket's mob (dispensers-release) instead of using the spawn egg, and do nothing with a
 * Capture Bucket. Droppers override this method and still just drop the item.
 */
@Mixin(DispenserBlock.class)
abstract class DispenserBlockMixin {

    private static final DispenseItemBehavior ANYTHINGINABUCKET$NOTHING = (source, stack) -> stack;
    private static final DispenseItemBehavior ANYTHINGINABUCKET$RELEASE = BucketManager::dispense;

    @Inject(method = "getDispenseMethod", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$dispense(Level level, ItemStack itemStack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (!BucketItems.ours(itemStack)) return;
        cir.setReturnValue(BucketItems.filledOf(itemStack) != null && AnythingInABucket.settings().dispensersRelease()
                ? ANYTHINGINABUCKET$RELEASE : ANYTHINGINABUCKET$NOTHING);
    }
}
