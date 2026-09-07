package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.api.model.FrontierMutation;
import games.alejandrocoria.mapfrontiers.api.model.Point2i;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.TerritoryLifetime;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTerritoryOperationServiceTest {
    @Test
    void sessionOnlyOperationsNeverUseAuthoritativeFlow() {
        FrontierData frontier = personalEntity(new FrontierData(user(10L)), TerritoryLifetime.SESSION_ONLY);
        CollectionData collection = personalEntity(new CollectionData(user(10L)), TerritoryLifetime.SESSION_ONLY);

        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCreateFlow(TerritoryLifetime.SESSION_ONLY, false));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCreateFlow(TerritoryLifetime.SESSION_ONLY, true));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeMutationFlow(frontier, false));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeMutationFlow(frontier, true));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCollectionMutationFlow(collection, false));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCollectionMutationFlow(collection, true));
    }

    @Test
    void persistentOperationsUseAuthoritativeFlowOnlyWithServerSupport() {
        FrontierData frontier = personalEntity(new FrontierData(user(10L)), TerritoryLifetime.PERSISTENT);
        CollectionData collection = personalEntity(new CollectionData(user(10L)), TerritoryLifetime.PERSISTENT);

        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCreateFlow(TerritoryLifetime.PERSISTENT, false));
        assertTrue(ClientTerritoryOperationService.usesAuthoritativeCreateFlow(TerritoryLifetime.PERSISTENT, true));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeMutationFlow(frontier, false));
        assertTrue(ClientTerritoryOperationService.usesAuthoritativeMutationFlow(frontier, true));
        assertFalse(ClientTerritoryOperationService.usesAuthoritativeCollectionMutationFlow(collection, false));
        assertTrue(ClientTerritoryOperationService.usesAuthoritativeCollectionMutationFlow(collection, true));
    }

    @Test
    void affectsCollectionVariants_presentationOnlyChange_returnsFalse() {
        FrontierChange change = new FrontierChange();
        change.setName("name", "owner");
        change.setColor(0x123456);
        change.setBanner(null, false);
        change.setPathStyle(new FrontierData.PathStyle());
        change.setModifiedTime(1L);

        assertFalse(ClientTerritoryOperationService.affectsCollectionMembership(change));
        assertFalse(ClientTerritoryOperationService.affectsCollectionVariants(change));
    }

    @Test
    void affectsCollectionVariants_visibilityChange_doesNotChangeMembership() {
        FrontierChange change = new FrontierChange();
        change.setVisibility(new FrontierVisibilityData());

        assertFalse(ClientTerritoryOperationService.affectsCollectionMembership(change));
        assertTrue(ClientTerritoryOperationService.affectsCollectionVariants(change));
    }

    @Test
    void affectsCollectionVariants_shapeOrCollectionChange_changesMembership() {
        FrontierChange shapeChange = new FrontierChange();
        shapeChange.setShape(List.of(
                new BlockPos(0, 70, 0),
                new BlockPos(16, 70, 0),
                new BlockPos(16, 70, 16)), Set.of(), List.of(), FrontierShape.Vertex);
        FrontierChange collectionChange = new FrontierChange();
        collectionChange.setCollectionId(new UUID(1L, 2L));

        assertTrue(ClientTerritoryOperationService.affectsCollectionMembership(shapeChange));
        assertTrue(ClientTerritoryOperationService.affectsCollectionVariants(shapeChange));
        assertTrue(ClientTerritoryOperationService.affectsCollectionMembership(collectionChange));
        assertTrue(ClientTerritoryOperationService.affectsCollectionVariants(collectionChange));
    }

    @Test
    void affectsCollectionVariants_incrementalGeometryChange_changesMembership() {
        FrontierChange change = FrontierChange.fromMutation(new FrontierData(user(10L)), FrontierMutation.builder()
                .insertPathPointAfterLast(new Point2i(16, 16))
                .build());

        assertTrue(ClientTerritoryOperationService.affectsCollectionMembership(change));
        assertTrue(ClientTerritoryOperationService.affectsCollectionVariants(change));
    }

    @Test
    void optimisticShareMutatesOnceAndPostsOneEvent() {
        FrontierData frontier = personalEntity(new FrontierData(user(10L)), TerritoryLifetime.PERSISTENT);
        PlayerId currentUser = user(1L);
        PlayerId targetUser = user(2L);
        frontier.setOwner(currentUser);
        AtomicInteger events = new AtomicInteger();

        assertTrue(ClientTerritoryOperationService.applyOptimisticShareLocally(
                frontier, targetUser, currentUser, events::incrementAndGet));

        assertEquals(1, frontier.getUserAccesses().size());
        assertTrue(frontier.getUserAccess(targetUser).isPending());
        assertEquals(1, events.get());
        assertFalse(ClientTerritoryOperationService.applyOptimisticShareLocally(
                frontier, targetUser, currentUser, events::incrementAndGet));
        assertEquals(1, frontier.getUserAccesses().size());
        assertEquals(1, events.get());
    }

    @Test
    void optimisticSharedUserUpdateMutatesOnceAndPostsOneEvent() {
        FrontierData frontier = personalEntity(new FrontierData(user(10L)), TerritoryLifetime.PERSISTENT);
        PlayerId currentUser = user(3L);
        PlayerId targetUser = user(4L);
        frontier.setOwner(currentUser);
        frontier.addUserAccess(new FrontierUserAccess(targetUser, false));
        FrontierUserAccess desiredUser = new FrontierUserAccess(frontier.getUserAccess(targetUser));
        desiredUser.setActions(EnumSet.of(FrontierUserAccess.Action.UpdateSettings));
        AtomicInteger events = new AtomicInteger();

        assertTrue(ClientTerritoryOperationService.applyOptimisticSharedUserUpdateLocally(
                frontier, desiredUser, currentUser, events::incrementAndGet));

        assertTrue(frontier.getUserAccess(targetUser).hasAction(FrontierUserAccess.Action.UpdateSettings));
        assertEquals(1, frontier.getUserAccesses().size());
        assertEquals(1, events.get());
        assertFalse(ClientTerritoryOperationService.applyOptimisticSharedUserUpdateLocally(
                frontier, desiredUser, currentUser, events::incrementAndGet));
        assertEquals(1, events.get());
    }

    @Test
    void optimisticSharedUserRemovalMutatesOnceAndPostsOneEvent() {
        FrontierData frontier = personalEntity(new FrontierData(user(10L)), TerritoryLifetime.PERSISTENT);
        PlayerId currentUser = user(5L);
        PlayerId targetUser = user(6L);
        frontier.setOwner(currentUser);
        frontier.addUserAccess(new FrontierUserAccess(targetUser, false));
        AtomicInteger events = new AtomicInteger();

        assertTrue(ClientTerritoryOperationService.applyOptimisticRemoveSharedUserLocally(
                frontier, targetUser, currentUser, events::incrementAndGet));

        assertFalse(frontier.hasUserAccess(targetUser));
        assertEquals(1, events.get());
        assertFalse(ClientTerritoryOperationService.applyOptimisticRemoveSharedUserLocally(
                frontier, targetUser, currentUser, events::incrementAndGet));
        assertEquals(1, events.get());
    }

    private static FrontierData personalEntity(FrontierData frontier, TerritoryLifetime lifetime) {
        frontier.setPersonal(true);
        frontier.setLifetime(lifetime);
        return frontier;
    }

    private static CollectionData personalEntity(CollectionData collection, TerritoryLifetime lifetime) {
        collection.setPersonal(true);
        collection.setLifetime(lifetime);
        return collection;
    }

    private static PlayerId user(long uuidValue) {
        return new PlayerId(new UUID(0L, uuidValue));
    }
}
