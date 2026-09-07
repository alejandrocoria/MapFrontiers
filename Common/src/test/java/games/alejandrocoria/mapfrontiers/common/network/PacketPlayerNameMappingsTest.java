package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketPlayerNameMappingsTest {
    @Test
    void serializesKnownNamesAndAppliesThemAsServerSync() {
        PlayerId first = playerId(1L);
        PlayerId second = playerId(2L);
        PlayerId unknown = playerId(3L);
        PacketPlayerNameMappings packet = new PacketPlayerNameMappings(List.of(first, unknown, second, first), playerId -> {
            if (playerId.equals(first)) {
                return "First";
            }
            if (playerId.equals(second)) {
                return "Second";
            }
            return null;
        });

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(encoded);
        PacketPlayerNameMappings decoded = new PacketPlayerNameMappings(encoded);
        encoded.release();

        assertEquals(List.of(new PacketPlayerNameMappings.Entry(first, "First"),
                new PacketPlayerNameMappings.Entry(second, "Second")), decoded.getEntries());

        PlayerNameRepository names = new PlayerNameRepository();
        names.observe(first, "OldName", PlayerNameSource.HINT);
        List<PlayerId> changes = new ArrayList<>();
        names.getEvents().subscribeChanged(this, changes::add);

        decoded.applyTo(names);
        assertEquals("First", names.resolveName(first));
        assertEquals("Second", names.resolveName(second));
        assertEquals(List.of(first, second), changes);

        decoded.applyTo(names);
        assertEquals(List.of(first, second), changes);
    }

    private static PlayerId playerId(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
