package net.inertiamc.anythinginabucket.loader;

import net.inertiamc.anythinginabucket.AnythingInABucket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * A channel clients with the mod register, so the server can skip the resource pack offer for them (they already have
 * the art). Nothing is ever sent on it.
 */
public record ArtChannel() implements CustomPacketPayload {

    public static final String ID = AnythingInABucket.MOD_ID + ":art";
    public static final ArtChannel INSTANCE = new ArtChannel();
    public static final Type<ArtChannel> TYPE = new Type<>(Identifier.fromNamespaceAndPath(AnythingInABucket.MOD_ID, "art"));
    public static final StreamCodec<ByteBuf, ArtChannel> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
