package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketPersonalImportHintsTest {
    @Test
    void roundTripsFrontierAndCollectionWithPortableHints() {
        PlayerId owner = playerId(1L);
        PlayerId copiedFrom = playerId(2L);
        FrontierData frontier = frontier(owner, copiedFrom);
        CollectionData collection = collection(owner, copiedFrom);

        PacketPersonalFrontier decodedFrontier = decodeFrontier(new PacketPersonalFrontier(frontier,
                playerId -> nameFor(playerId, owner, copiedFrom)));
        PacketPersonalCollection decodedCollection = decodeCollection(new PacketPersonalCollection(collection,
                playerId -> nameFor(playerId, owner, copiedFrom)));

        assertEquals(frontier.getId(), decodedFrontier.getFrontier().getId());
        assertEquals(owner, decodedFrontier.getFrontier().getOwner());
        assertEquals(copiedFrom, decodedFrontier.getFrontier().getCopiedFromUser());
        assertEquals(List.of(new PacketPlayerNameMappings.Entry(owner, "OwnerName"),
                new PacketPlayerNameMappings.Entry(copiedFrom, "CopiedFromName")),
                decodedFrontier.getPlayerNameMappings().getEntries());

        assertEquals(collection.getId(), decodedCollection.getCollection().getId());
        assertEquals(owner, decodedCollection.getCollection().getOwner());
        assertEquals(copiedFrom, decodedCollection.getCollection().getCopiedFromUser());
        assertEquals(List.of(new PacketPlayerNameMappings.Entry(owner, "OwnerName"),
                new PacketPlayerNameMappings.Entry(copiedFrom, "CopiedFromName")),
                decodedCollection.getPlayerNameMappings().getEntries());
    }

    @Test
    void roundTripsEmptyPortableHintBatches() {
        PlayerId owner = playerId(3L);
        FrontierData frontier = frontier(owner, null);
        CollectionData collection = collection(owner, null);

        PacketPersonalFrontier decodedFrontier = decodeFrontier(new PacketPersonalFrontier(frontier, playerId -> null));
        PacketPersonalCollection decodedCollection = decodeCollection(new PacketPersonalCollection(collection, playerId -> null));

        assertEquals(List.of(), decodedFrontier.getPlayerNameMappings().getEntries());
        assertEquals(List.of(), decodedCollection.getPlayerNameMappings().getEntries());
    }

    private static PacketPersonalFrontier decodeFrontier(PacketPersonalFrontier packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        PacketPersonalFrontier decoded = new PacketPersonalFrontier(buffer);
        assertEquals(0, buffer.readableBytes());
        buffer.release();
        return decoded;
    }

    private static PacketPersonalCollection decodeCollection(PacketPersonalCollection packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        PacketPersonalCollection decoded = new PacketPersonalCollection(buffer);
        assertEquals(0, buffer.readableBytes());
        buffer.release();
        return decoded;
    }

    private static FrontierData frontier(PlayerId owner, PlayerId copiedFrom) {
        FrontierData frontier = new FrontierData(owner);
        frontier.setId(new UUID(0L, 11L));
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        frontier.setPersonal(true);
        if (copiedFrom != null) {
            frontier.setCopiedFrom(new UUID(0L, 12L), copiedFrom);
        }
        return frontier;
    }

    private static CollectionData collection(PlayerId owner, PlayerId copiedFrom) {
        CollectionData collection = new CollectionData(owner);
        collection.setId(new UUID(0L, 13L));
        collection.setPersonal(true);
        if (copiedFrom != null) {
            collection.setCopiedFrom(new UUID(0L, 14L), copiedFrom);
        }
        return collection;
    }

    private static String nameFor(PlayerId playerId, PlayerId owner, PlayerId copiedFrom) {
        if (playerId.equals(owner)) {
            return "OwnerName";
        }
        return playerId.equals(copiedFrom) ? "CopiedFromName" : null;
    }

    private static PlayerId playerId(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
