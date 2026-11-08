package net.inertiamc.anythinginabucket.loader;

//? if forge {
/*import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.catching.Interactions;
import net.inertiamc.anythinginabucket.command.BucketCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
//? if >=1.21.6 {
import net.minecraftforge.eventbus.api.listener.Priority;
//?} else {
/^import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
^///?}

/^*
 * Entity clicks are handled at HIGHEST, before claim mods (respect-claims off), and at LOW, after them (respect-claims
 * on). Cancelled clicks are not received, so a claim mod wins.
 ^/
@Mod(AnythingInABucket.MOD_ID)
public final class ForgeEntry {

    /^* Optional, so vanilla clients can still join. ^/
    private static EventNetworkChannel art;

    public ForgeEntry() {
        AnythingInABucket.init(FMLPaths.CONFIGDIR.get());
        art = ChannelBuilder.named(ArtChannel.ID).optional().networkProtocolVersion(1).eventNetworkChannel();
        AnythingInABucket.artOnClient(player -> art.isRemotePresent(player.connection.getConnection()));
        //? if >=1.21.6 {
        for (boolean early : new boolean[] {true, false}) {
            byte priority = early ? Priority.HIGHEST : Priority.LOW;
            PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(priority,
                    (java.util.function.Predicate<PlayerInteractEvent.EntityInteractSpecific>) event -> click(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), event::setCancellationResult));
            //? if <26.1 {
            /^PlayerInteractEvent.EntityInteract.BUS.addListener(priority,
                    (java.util.function.Predicate<PlayerInteractEvent.EntityInteract>) event -> click(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), event::setCancellationResult));
            ^///?}
        }
        PlayerInteractEvent.RightClickBlock.BUS.addListener(Priority.HIGHEST,
                (java.util.function.Predicate<PlayerInteractEvent.RightClickBlock>) event -> block(event.getEntity(), event.getHand(), event.getHitVec(), event::setCancellationResult));
        RegisterCommandsEvent.BUS.addListener(event -> BucketCommand.register(event.getDispatcher()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onJoin(player);
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onQuit(player);
        });
        OnDatapackSyncEvent.BUS.addListener(event -> {
            if (event.getPlayer() == null) AnythingInABucket.onDataPackReload(event.getPlayerList().getServer());
        });
        ServerStoppedEvent.BUS.addListener(event -> AnythingInABucket.onStopped());
        PermissionGatherEvent.Nodes.BUS.addListener(event -> event.addNodes(LoaderPermissions.nodes()));
        //?} else {
        /^for (boolean early : new boolean[] {true, false}) {
            EventPriority priority = early ? EventPriority.HIGHEST : EventPriority.LOW;
            MinecraftForge.EVENT_BUS.addListener(priority, false, PlayerInteractEvent.EntityInteractSpecific.class, event -> {
                if (click(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), event::setCancellationResult)) event.setCanceled(true);
            });
            MinecraftForge.EVENT_BUS.addListener(priority, false, PlayerInteractEvent.EntityInteract.class, event -> {
                if (click(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget(), event::setCancellationResult)) event.setCanceled(true);
            });
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, event -> {
            if (block(event.getEntity(), event.getHand(), event.getHitVec(), event::setCancellationResult)) event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> BucketCommand.register(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onJoin(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onQuit(player);
        });
        MinecraftForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) AnythingInABucket.onDataPackReload(event.getPlayerList().getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> AnythingInABucket.onStopped());
        MinecraftForge.EVENT_BUS.addListener((PermissionGatherEvent.Nodes event) -> event.addNodes(LoaderPermissions.nodes()));
        ^///?}
    }

    /^* True when the mod handled the click; the event is then cancelled with {@code result}. ^/
    private static boolean click(boolean early, net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level level,
                                 net.minecraft.world.InteractionHand hand, net.minecraft.world.entity.Entity target,
                                 java.util.function.Consumer<InteractionResult> result) {
        return done(Interactions.entityClick(early, player, level, hand, target), result);
    }

    private static boolean block(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                                 net.minecraft.world.phys.BlockHitResult hit, java.util.function.Consumer<InteractionResult> result) {
        return done(Interactions.blockBeforeClaims(player, hand, hit), result);
    }

    private static boolean done(InteractionResult outcome, java.util.function.Consumer<InteractionResult> result) {
        if (outcome == InteractionResult.PASS) return false;
        result.accept(outcome);
        return true;
    }
}
*///?}
