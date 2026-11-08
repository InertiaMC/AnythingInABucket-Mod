package net.inertiamc.anythinginabucket.loader;

//? if neoforge {
/*import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.catching.Interactions;
import net.inertiamc.anythinginabucket.command.BucketCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

/^*
 * Entity clicks are handled at HIGHEST, before claim mods (respect-claims off), and at LOW, after them (respect-claims
 * on). Cancelled clicks are not received, so a claim mod wins.
 ^/
@Mod(AnythingInABucket.MOD_ID)
public final class NeoForgeEntry {

    public NeoForgeEntry(IEventBus modBus) {
        AnythingInABucket.init(FMLPaths.CONFIGDIR.get());
        // Optional, so vanilla clients can still join.
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1").optional()
                .playToClient(ArtChannel.TYPE, ArtChannel.CODEC, (payload, context) -> {}));
        AnythingInABucket.artOnClient(player -> player.connection.hasChannel(ArtChannel.TYPE));
        clicks(EventPriority.HIGHEST, true);
        clicks(EventPriority.LOW, false);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.RightClickBlock.class, event -> {
            InteractionResult result = Interactions.blockBeforeClaims(event.getEntity(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> BucketCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onJoin(player);
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) AnythingInABucket.onQuit(player);
        });
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> {
            if (event.getPlayer() == null) AnythingInABucket.onDataPackReload(event.getPlayerList().getServer());
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> AnythingInABucket.onStopped());
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> event.addNodes(LoaderPermissions.nodes()));
    }

    private static void clicks(EventPriority priority, boolean early) {
        //? if <26.1 {
        /^NeoForge.EVENT_BUS.addListener(priority, false, PlayerInteractEvent.EntityInteractSpecific.class, event -> {
            InteractionResult result = Interactions.entityClick(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
        ^///?}
        NeoForge.EVENT_BUS.addListener(priority, false, PlayerInteractEvent.EntityInteract.class, event -> {
            InteractionResult result = Interactions.entityClick(early, event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }
}
*///?}
