package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLocalPersistenceCoordinatorTest {
    private static final PlayerId CURRENT_PLAYER = player(1L);

    @Test
    void ownedPersistentFrontierReferencesTriggerHintPersistence() {
        PlayerId sharedPlayer = player(2L);
        PlayerId copiedFromPlayer = player(3L);
        FrontierData frontier = ownedPersistentFrontier();
        frontier.addUserAccess(new FrontierUserAccess(sharedPlayer, false));
        frontier.setCopiedFrom(UUID.randomUUID(), copiedFromPlayer);

        assertTrue(references(frontier, List.of(), CURRENT_PLAYER));
        assertTrue(references(frontier, List.of(), sharedPlayer));
        assertTrue(references(frontier, List.of(), copiedFromPlayer));
        assertFalse(references(frontier, List.of(), player(4L)));
    }

    @Test
    void ownedPersistentCollectionReferencesTriggerHintPersistence() {
        PlayerId copiedFromPlayer = player(2L);
        CollectionData collection = ownedPersistentCollection();
        collection.setCopiedFrom(UUID.randomUUID(), copiedFromPlayer);

        assertTrue(references(List.of(), collection, CURRENT_PLAYER));
        assertTrue(references(List.of(), collection, copiedFromPlayer));
        assertFalse(references(List.of(), collection, player(3L)));
    }

    @Test
    void nonPersistedOrNonOwnedDataDoesNotTriggerHintPersistence() {
        FrontierData global = ownedPersistentFrontier();
        global.setPersonal(false);

        FrontierData sessionOnly = ownedPersistentFrontier();
        sessionOnly.setLifetime(TerritoryLifetime.SESSION_ONLY);

        FrontierData otherOwner = new FrontierData(player(2L));
        otherOwner.setPersonal(true);

        CollectionData globalCollection = ownedPersistentCollection();
        globalCollection.setPersonal(false);

        CollectionData sessionOnlyCollection = ownedPersistentCollection();
        sessionOnlyCollection.setLifetime(TerritoryLifetime.SESSION_ONLY);

        CollectionData otherOwnerCollection = new CollectionData(player(2L));
        otherOwnerCollection.setPersonal(true);

        assertFalse(ClientLocalPersistenceCoordinator.referencesPlayerInOwnedPersistentData(
                List.of(global, sessionOnly, otherOwner),
                List.of(globalCollection, sessionOnlyCollection, otherOwnerCollection), CURRENT_PLAYER, CURRENT_PLAYER));
    }

    private static boolean references(FrontierData frontier, List<CollectionData> collections, PlayerId playerId) {
        return ClientLocalPersistenceCoordinator.referencesPlayerInOwnedPersistentData(
                List.of(frontier), collections, CURRENT_PLAYER, playerId);
    }

    private static boolean references(List<FrontierData> frontiers, CollectionData collection, PlayerId playerId) {
        return ClientLocalPersistenceCoordinator.referencesPlayerInOwnedPersistentData(
                frontiers, List.of(collection), CURRENT_PLAYER, playerId);
    }

    private static FrontierData ownedPersistentFrontier() {
        FrontierData frontier = new FrontierData(CURRENT_PLAYER);
        frontier.setPersonal(true);
        return frontier;
    }

    private static CollectionData ownedPersistentCollection() {
        CollectionData collection = new CollectionData(CURRENT_PLAYER);
        collection.setPersonal(true);
        return collection;
    }

    private static PlayerId player(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
