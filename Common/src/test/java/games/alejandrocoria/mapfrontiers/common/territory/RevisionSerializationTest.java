package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.nbt.PlayerReferenceNbtReadContext;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RevisionSerializationTest {
    @Test
    void collectionRevisionRoundTripsThroughNetworkButNotNbt() {
        CollectionData collection = collection();
        collection.setCollectionRevision(17L);
        assertEquals(17L, new CollectionData(collection).getCollectionRevision());
        CollectionData updatedCollection = new CollectionData(collection.getOwner());
        updatedCollection.updateFromData(collection);
        assertEquals(17L, updatedCollection.getCollectionRevision());

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        collection.toBytes(encoded);
        CollectionData decoded = CollectionData.fromBytes(encoded);
        assertEquals(17L, decoded.getCollectionRevision());
        encoded.release();

        PlayerNameRepository names = new PlayerNameRepository();
        CompoundTag nbt = new CompoundTag();
        collection.writeToNBT(nbt, ignored -> "owner");
        assertFalse(nbt.contains("collectionRevision"));
        decoded = CollectionData.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(names)).collection();
        assertEquals(0L, decoded.getCollectionRevision());
    }

    @Test
    void sharingRevisionRoundTripsThroughFrontierAndSharingCodecsButNotNbt() {
        FrontierData frontier = frontier();
        long syncHash = frontier.computeSyncHash();
        frontier.setSharingRevision(23L);
        assertEquals(syncHash, frontier.computeSyncHash());
        assertEquals(23L, new FrontierData(frontier).getSharingRevision());
        FrontierData updatedFrontier = new FrontierData(frontier.getOwner());
        updatedFrontier.updateFromData(frontier);
        assertEquals(23L, updatedFrontier.getSharingRevision());

        FriendlyByteBuf encodedFrontier = new FriendlyByteBuf(Unpooled.buffer());
        frontier.toBytes(encodedFrontier);
        FrontierData decodedFrontier = FrontierData.fromBytes(encodedFrontier);
        assertEquals(23L, decodedFrontier.getSharingRevision());
        encodedFrontier.release();

        FrontierSharingChange change = FrontierSharingChange.fromFrontierData(frontier);
        FriendlyByteBuf encodedChange = new FriendlyByteBuf(Unpooled.buffer());
        change.toBytes(encodedChange);
        FrontierSharingChange decodedChange = new FrontierSharingChange(encodedChange);
        assertEquals(23L, decodedChange.getSharingRevision());
        encodedChange.release();

        PlayerNameRepository names = new PlayerNameRepository();
        CompoundTag nbt = new CompoundTag();
        frontier.writeToNBT(nbt, ignored -> "owner");
        assertFalse(nbt.contains("sharingRevision"));
        decodedFrontier = FrontierData.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION,
                PlayerReferenceNbtReadContext.uuidOnly(names)).frontier();
        assertEquals(0L, decodedFrontier.getSharingRevision());
    }

    private static CollectionData collection() {
        CollectionData collection = new CollectionData(playerId(1L));
        collection.setId(UUID.randomUUID());
        return collection;
    }

    private static FrontierData frontier() {
        FrontierData frontier = new FrontierData(playerId(2L));
        frontier.setId(UUID.randomUUID());
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        return frontier;
    }

    private static PlayerId playerId(long uuidValue) {
        return new PlayerId(new UUID(0L, uuidValue));
    }
}
