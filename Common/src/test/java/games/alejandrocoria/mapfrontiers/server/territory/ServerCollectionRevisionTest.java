package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.server.territory.collection.ServerCollectionEvents;
import games.alejandrocoria.mapfrontiers.server.territory.frontier.ServerFrontierEvents;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerCollectionRevisionTest {
    @Test
    void updateIncrementsRevisionOnceWhileNoOpAndTouchPreserveIt() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        CollectionData collection = collection();
        manager.addGlobalCollection(collection);
        ServerCollectionEvents collectionEvents = new ServerCollectionEvents();
        AtomicInteger updateEvents = new AtomicInteger();
        collectionEvents.subscribeUpdated(this, ignored -> updateEvents.incrementAndGet());
        ServerTerritoryOperationService service = new ServerTerritoryOperationService(
                null, manager, null, new ServerFrontierEvents(), collectionEvents);

        CollectionData changed = new CollectionData(collection);
        changed.setName("Changed");
        ServerTerritoryOperationResult committed = service.updateGlobalCollection(collection.getId(), changed);

        assertTrue(committed.isSuccess());
        assertEquals(1L, collection.getCollectionRevision());
        assertEquals(1, updateEvents.get());
        assertEquals(1, committed.getNetworkActionCount());

        ServerTerritoryOperationResult noOp = service.updateGlobalCollection(
                collection.getId(), new CollectionData(collection));
        assertTrue(noOp.isSuccess());
        assertEquals(1L, collection.getCollectionRevision());
        assertEquals(1, updateEvents.get());
        assertEquals(0, noOp.getNetworkActionCount());

        service.touchCollection(collection, new Date(collection.getModified().getTime() + 1L));
        assertEquals(1L, collection.getCollectionRevision());
    }

    private static CollectionData collection() {
        CollectionData collection = new CollectionData();
        collection.setId(UUID.randomUUID());
        SettingsUser owner = new SettingsUser();
        owner.username = "Owner";
        owner.uuid = UUID.randomUUID();
        collection.setOwner(owner);
        collection.setName("Original");
        return collection;
    }
}
