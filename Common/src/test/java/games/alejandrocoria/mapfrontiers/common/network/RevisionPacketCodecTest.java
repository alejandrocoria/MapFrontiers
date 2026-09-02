package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RevisionPacketCodecTest {
    @Test
    void collectionRequestAndResponseCarryRevisionRequestAndResolution() {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setCollectionRevision(3L);

        FriendlyByteBuf request = new FriendlyByteBuf(Unpooled.buffer());
        new PacketUpdateCollection(collection, 3L, 41L).encode(request);
        CollectionData requestedCollection = new CollectionData();
        requestedCollection.fromBytes(request);
        assertEquals(3L, requestedCollection.getCollectionRevision());
        assertEquals(3L, request.readLong());
        assertEquals(41L, request.readLong());
        request.release();

        FriendlyByteBuf response = new FriendlyByteBuf(Unpooled.buffer());
        new PacketCollectionUpdated(collection, 12, 41L, OperationResolution.Rejected).encode(response);
        CollectionData responseCollection = new CollectionData();
        responseCollection.fromBytes(response);
        assertEquals(3L, responseCollection.getCollectionRevision());
        assertEquals(12, response.readInt());
        assertEquals(41L, response.readLong());
        assertEquals(OperationResolution.Rejected, OperationResolution.VALUES[response.readInt()]);
        response.release();
    }

    @Test
    void sharingResponseCarriesRevisionActorRequestAndResolution() {
        FrontierSharingChange change = new FrontierSharingChange();
        change.setSharingRevision(5L);
        UUID frontierId = UUID.randomUUID();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld"));

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        new PacketFrontierSharingUpdated(frontierId, dimension, change, 14, 43L,
                OperationResolution.Accepted).encode(encoded);

        assertEquals(frontierId, encoded.readUUID());
        assertEquals(dimension.identifier(), encoded.readIdentifier());
        FrontierSharingChange decodedChange = new FrontierSharingChange(encoded);
        assertEquals(5L, decodedChange.getSharingRevision());
        assertEquals(14, encoded.readInt());
        assertEquals(43L, encoded.readLong());
        assertEquals(OperationResolution.Accepted, OperationResolution.VALUES[encoded.readInt()]);
        encoded.release();
    }

    @Test
    void settingsPacketsCarryServerRevisionRequestAndResolution() {
        FrontierSettings settings = new FrontierSettings();

        FriendlyByteBuf update = new FriendlyByteBuf(Unpooled.buffer());
        new PacketUpdateFrontierSettings(settings, 7L, 47L).encode(update);
        FrontierSettings updatedSettings = new FrontierSettings();
        updatedSettings.fromBytes(update);
        assertEquals(7L, update.readLong());
        assertEquals(47L, update.readLong());
        update.release();

        FriendlyByteBuf response = new FriendlyByteBuf(Unpooled.buffer());
        new PacketFrontierSettings(settings, 8L, 47L, OperationResolution.Accepted).encode(response);
        FrontierSettings responseSettings = new FrontierSettings();
        responseSettings.fromBytes(response);
        assertEquals(8L, response.readLong());
        assertEquals(47L, response.readLong());
        assertEquals(OperationResolution.Accepted, OperationResolution.VALUES[response.readInt()]);
        response.release();

        FriendlyByteBuf poll = new FriendlyByteBuf(Unpooled.buffer());
        new PacketRequestFrontierSettings(8L).encode(poll);
        assertEquals(8L, poll.readLong());
        poll.release();
    }
}
