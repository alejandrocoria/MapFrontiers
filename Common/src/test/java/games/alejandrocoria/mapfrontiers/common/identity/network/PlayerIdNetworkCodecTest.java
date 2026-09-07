package games.alejandrocoria.mapfrontiers.common.identity.network;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerIdNetworkCodecTest {
    @Test
    void roundTripWritesOnlyTheRequiredUuid() {
        PlayerId playerId = new PlayerId(UUID.randomUUID());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        PlayerIdNetworkCodec.write(buffer, playerId);

        assertEquals(16, buffer.readableBytes());
        assertEquals(playerId, PlayerIdNetworkCodec.read(buffer));
        assertEquals(0, buffer.readableBytes());
        buffer.release();
    }
}
