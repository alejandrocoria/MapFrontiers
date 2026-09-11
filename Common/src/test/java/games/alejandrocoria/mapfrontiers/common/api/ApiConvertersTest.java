package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.model.UserRef;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiConvertersTest {
    @Test
    void frontierViewUsesResolvedNamesForOwnerAndSharedUsers() {
        PlayerId owner = player(1L);
        PlayerId sharedUser = player(2L);
        PlayerNameRepository playerNames = new PlayerNameRepository();
        playerNames.observe(owner, "CurrentOwner", PlayerNameSource.CONNECTED_PROFILE);
        playerNames.observe(sharedUser, "CurrentSharedUser", PlayerNameSource.SERVER_SYNC);

        FrontierData frontier = new FrontierData(owner);
        frontier.setPersonal(true);
        frontier.setDimension(ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld")));
        frontier.addUserAccess(new FrontierUserAccess(sharedUser, false));

        var view = ApiConverters.fromFrontier(frontier, playerNames);

        assertEquals(owner.uuid(), view.owner().id());
        assertEquals("CurrentOwner", view.owner().name());
        assertEquals(sharedUser.uuid(), view.sharedUsers().getFirst().user().id());
        assertEquals("CurrentSharedUser", view.sharedUsers().getFirst().user().name());

        var collectionView = ApiConverters.fromCollection(new CollectionData(owner), playerNames);
        assertEquals(owner.uuid(), collectionView.owner().id());
        assertEquals("CurrentOwner", collectionView.owner().name());
    }

    @Test
    void apiInputRegistersOnlyALowPriorityNameHint() {
        PlayerId user = player(3L);
        PlayerNameRepository playerNames = new PlayerNameRepository();

        assertEquals(user, ApiConverters.toPlayerId(new UserRef(user.uuid(), "HintName"), playerNames));
        assertEquals("HintName", playerNames.resolveName(user));

        playerNames.observe(user, "ConnectedName", PlayerNameSource.CONNECTED_PROFILE);
        ApiConverters.toPlayerId(new UserRef(user.uuid(), "StaleHint"), playerNames);
        assertEquals("ConnectedName", playerNames.resolveName(user));

        PlayerId unknownUser = player(4L);
        var unknownRef = ApiConverters.fromUser(unknownUser, playerNames);
        assertEquals(unknownUser.uuid(), unknownRef.id());
        assertNull(unknownRef.name());
        assertEquals(unknownUser, ApiConverters.toPlayerId(new UserRef(unknownUser.uuid(), null), playerNames));
        assertNull(playerNames.resolveName(unknownUser));
    }

    private static PlayerId player(long value) {
        return new PlayerId(new UUID(0L, value));
    }
}
