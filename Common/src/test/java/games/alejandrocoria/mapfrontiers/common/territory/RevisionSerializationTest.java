package games.alejandrocoria.mapfrontiers.common.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
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
        CollectionData updatedCollection = new CollectionData();
        updatedCollection.updateFromData(collection);
        assertEquals(17L, updatedCollection.getCollectionRevision());

        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        collection.toBytes(encoded);
        CollectionData decoded = new CollectionData();
        decoded.fromBytes(encoded);
        assertEquals(17L, decoded.getCollectionRevision());
        encoded.release();

        CompoundTag nbt = new CompoundTag();
        collection.writeToNBT(nbt);
        assertFalse(nbt.contains("collectionRevision"));
        decoded.setCollectionRevision(99L);
        decoded.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION);
        assertEquals(0L, decoded.getCollectionRevision());
    }

    @Test
    void sharingRevisionRoundTripsThroughFrontierAndSharingCodecsButNotNbt() {
        FrontierData frontier = frontier();
        long syncHash = frontier.computeSyncHash();
        frontier.setSharingRevision(23L);
        assertEquals(syncHash, frontier.computeSyncHash());
        assertEquals(23L, new FrontierData(frontier).getSharingRevision());
        FrontierData updatedFrontier = new FrontierData();
        updatedFrontier.updateFromData(frontier);
        assertEquals(23L, updatedFrontier.getSharingRevision());

        FriendlyByteBuf encodedFrontier = new FriendlyByteBuf(Unpooled.buffer());
        frontier.toBytes(encodedFrontier);
        FrontierData decodedFrontier = new FrontierData();
        decodedFrontier.fromBytes(encodedFrontier);
        assertEquals(23L, decodedFrontier.getSharingRevision());
        encodedFrontier.release();

        FrontierSharingChange change = FrontierSharingChange.fromFrontierData(frontier);
        FriendlyByteBuf encodedChange = new FriendlyByteBuf(Unpooled.buffer());
        change.toBytes(encodedChange);
        FrontierSharingChange decodedChange = new FrontierSharingChange(encodedChange);
        assertEquals(23L, decodedChange.getSharingRevision());
        encodedChange.release();

        CompoundTag nbt = new CompoundTag();
        frontier.writeToNBT(nbt);
        assertFalse(nbt.contains("sharingRevision"));
        decodedFrontier.setSharingRevision(99L);
        decodedFrontier.readFromNBT(nbt, MapFrontiers.FRONTIER_DATA_VERSION);
        assertEquals(0L, decodedFrontier.getSharingRevision());
    }

    private static CollectionData collection() {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        collection.setOwner(user("owner", 1L));
        return collection;
    }

    private static FrontierData frontier() {
        FrontierData frontier = new FrontierData();
        frontier.setId(UUID.randomUUID());
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION,
                Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        frontier.setOwner(user("owner", 2L));
        return frontier;
    }

    private static SettingsUser user(String username, long uuidValue) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = new UUID(0L, uuidValue);
        return user;
    }
}
