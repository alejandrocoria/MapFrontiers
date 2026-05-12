package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class PacketCodecs {
    public static <B extends FriendlyByteBuf, P> StreamCodec<B, P> guarded(Identifier channel, BiConsumer<P, B> encoder, Function<B, P> decoder) {
        return StreamCodec.ofMember(
                (packet, buf) -> {
                    try {
                        encoder.accept(packet, buf);
                    } catch (Exception e) {
                        MapFrontiers.LOGGER.error("Failed to write message for channel {}", channel, e);
                        throw e;
                    }
                },
                buf -> {
                    try {
                        return decoder.apply(buf);
                    } catch (Exception e) {
                        MapFrontiers.LOGGER.error("Failed to read message for channel {}", channel, e);
                        throw e;
                    }
                }
        );
    }

    private PacketCodecs() {
    }
}


