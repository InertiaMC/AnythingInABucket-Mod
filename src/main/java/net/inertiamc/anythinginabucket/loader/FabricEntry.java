package net.inertiamc.anythinginabucket.loader;

//? if fabric {
import net.inertiamc.anythinginabucket.AnythingInABucket;
import net.inertiamc.anythinginabucket.catching.Interactions;
import net.inertiamc.anythinginabucket.command.BucketCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class FabricEntry implements ModInitializer {

    /** Runs before claim mods. Open Parties and Claims uses its own phase ahead of the default one. */
    private static final Identifier BEFORE_CLAIMS = Identifier.fromNamespaceAndPath(AnythingInABucket.MOD_ID, "before_claims");
    private static final Identifier OPEN_PARTIES = Identifier.fromNamespaceAndPath("openpartiesandclaims", "protection");

    @Override
    public void onInitialize() {
        AnythingInABucket.init(FabricLoader.getInstance().getConfigDir());
        // Clients with the mod register a receiver for this (FabricClientEntry).
        //? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(ArtChannel.TYPE, ArtChannel.CODEC);
        //?} else
        /*PayloadTypeRegistry.playS2C().register(ArtChannel.TYPE, ArtChannel.CODEC);*/
        AnythingInABucket.artOnClient(player -> ServerPlayNetworking.canSend(player, ArtChannel.TYPE));
        before(UseEntityCallback.EVENT);
        before(UseBlockCallback.EVENT);
        UseEntityCallback.EVENT.register(BEFORE_CLAIMS, (player, level, hand, entity, hit) -> Interactions.entityClick(true, player, level, hand, entity));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> Interactions.entityClick(false, player, level, hand, entity));
        UseBlockCallback.EVENT.register(BEFORE_CLAIMS, (player, level, hand, hit) -> Interactions.blockBeforeClaims(player, hand, hit));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> BucketCommand.register(dispatcher));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> AnythingInABucket.onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AnythingInABucket.onQuit(handler.getPlayer()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> AnythingInABucket.onDataPackReload(server));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> AnythingInABucket.onStopped());
    }

    private static void before(Event<?> event) {
        event.addPhaseOrdering(BEFORE_CLAIMS, Event.DEFAULT_PHASE);
        event.addPhaseOrdering(BEFORE_CLAIMS, OPEN_PARTIES);
    }
}
//?}
