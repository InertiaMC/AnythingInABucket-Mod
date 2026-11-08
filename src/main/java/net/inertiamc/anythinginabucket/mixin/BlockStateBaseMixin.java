package net.inertiamc.anythinginabucket.mixin;

import net.inertiamc.anythinginabucket.item.BucketItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks treat this mod's items as an empty hand: chests and doors still open, but a cauldron won't fill a Capture
 * Bucket and a spawner won't take a filled bucket's egg.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {

    //? if >=1.21.2 {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$useItemOn(ItemStack itemStack, Level level, Player player, InteractionHand hand,
                                             BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (BucketItems.ours(itemStack)) cir.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
    }
    //?} else {
    /*@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void anythinginabucket$useItemOn(ItemStack itemStack, Level level, Player player, InteractionHand hand,
                                             BlockHitResult hitResult, CallbackInfoReturnable<net.minecraft.world.ItemInteractionResult> cir) {
        if (BucketItems.ours(itemStack)) cir.setReturnValue(net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }
    *///?}
}
