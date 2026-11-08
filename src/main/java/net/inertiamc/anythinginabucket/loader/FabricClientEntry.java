package net.inertiamc.anythinginabucket.loader;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Registers the art channel on the client so servers know it has the bucket art. */
public final class FabricClientEntry implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ArtChannel.TYPE, (payload, context) -> {});
    }
}
//?}
