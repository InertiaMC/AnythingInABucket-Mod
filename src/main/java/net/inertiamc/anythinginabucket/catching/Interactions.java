package net.inertiamc.anythinginabucket.catching;

import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.config.Settings;
import net.inertiamc.anythinginabucket.item.BucketItems;
import net.inertiamc.anythinginabucket.util.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Click handling: entity clicks come from the loader's events, block and air clicks from the item mixins.
 *
 * <p>Each loader registers the entity handler twice, once before claim mods (early) and once after. The early one acts
 * only with respect-claims off, so a claim mod cancelling the click can't stop it; the late one acts only with it on,
 * so the claim mod wins.
 */
public final class Interactions {

    private Interactions() {}

    private static BucketManager buckets() {
        return AnythingInABucket.buckets();
    }

    /** Runs before the entity's own interaction (milking, breeding, spawn egg babies). */
    public static InteractionResult entityClick(boolean early, Player player, Level level, InteractionHand hand, Entity entity) {
        if (early == AnythingInABucket.settings().respectClaims()) return InteractionResult.PASS;
        return useOnEntity(player, level, hand, entity);
    }

    private static InteractionResult useOnEntity(Player player, Level level, InteractionHand hand, Entity entity) {
        ItemStack held = player.getItemInHand(hand);
        Settings.CatchWith catchWith = AnythingInABucket.settings().catchWith();
        boolean capture = BucketItems.isCaptureBucket(held);
        boolean filled = !capture && BucketItems.isFilled(held);
        boolean plain = !capture && !filled && held.is(Items.BUCKET) && held.getComponentsPatch().isEmpty()
                && player.isSecondaryUseActive() && catchWith.normal();
        if (!capture && !filled && !plain) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        BucketManager.resyncLater(serverPlayer);
        if (buckets().heldClick(player)) {
            BucketManager.resync(serverPlayer);
            return InteractionResult.FAIL;
        }
        Entity target = entity instanceof EnderDragonPart part ? part.parentMob : entity;
        if (filled || !(target instanceof Mob mob)) {
            BucketManager.resync(serverPlayer);
            return plain ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        if (capture && !catchWith.capture()) {
            BucketManager.deny(serverPlayer, Messages.CATCH_CAPTURE_OFF);
            return InteractionResult.FAIL;
        }
        buckets().tryCatch(serverPlayer, mob, hand, held, plain);
        return InteractionResult.SUCCESS;
    }

    /** One of this mod's items used on a block that didn't handle the click itself (or while sneaking). */
    public static InteractionResult useOnBlock(ItemStack held, UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (buckets().heldClick(player) || !BucketItems.isFilled(held)) {
            BucketManager.resync(player);
            return InteractionResult.FAIL;
        }
        buckets().tryRelease(player, context.getHand(), held, context.getClickedPos(), context.getClickedFace());
        return InteractionResult.SUCCESS;
    }

    /**
     * With respect-claims off, releases a filled bucket before claim mods see the click. Blocks that open or toggle
     * (containers, doors, buttons) are left to the normal path unless sneaking, so claims still protect them.
     */
    public static InteractionResult blockBeforeClaims(Player player, InteractionHand hand, BlockHitResult hit) {
        if (AnythingInABucket.settings().respectClaims() || !(player instanceof ServerPlayer)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!BucketItems.isFilled(held)) return InteractionResult.PASS;
        if (!player.isSecondaryUseActive() && opens(player.level(), hit.getBlockPos())) return InteractionResult.PASS;
        return useOnBlock(held, new UseOnContext(player, hand, hit));
    }

    private static boolean opens(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getMenuProvider(level, pos) != null || state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.BUTTONS) || state.getBlock() instanceof LeverBlock;
    }

    /** Used on air or a liquid: does nothing, but still tracks held clicks. */
    public static InteractionResult useInAir(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            buckets().heldClick(player);
            BucketManager.resync(serverPlayer);
        }
        return InteractionResult.FAIL;
    }
}
