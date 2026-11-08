package net.inertiamc.anythinginabucket.mixin;

import net.inertiamc.anythinginabucket.catching.Interactions;
import net.inertiamc.anythinginabucket.item.BucketItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the vanilla use of both items (spawn egg, scooping water) with releasing on a block. */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (BucketItems.ours(self)) cir.setReturnValue(Interactions.useOnBlock(self, context));
    }

    //? if >=1.21.2 {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (BucketItems.ours(self)) cir.setReturnValue(Interactions.useInAir(player));
    }
    //?} else {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$use(Level level, Player player, InteractionHand hand,
                                       CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (BucketItems.ours(self)) {
            Interactions.useInAir(player);
            cir.setReturnValue(net.minecraft.world.InteractionResultHolder.fail(self));
        }
    }
    *///?}
}
