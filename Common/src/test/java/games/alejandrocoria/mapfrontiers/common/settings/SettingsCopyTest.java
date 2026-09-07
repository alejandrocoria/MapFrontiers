package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsCopyTest {
    @Test
    void legacyAdapterConvertsStrictlyToAndFromPlayerId() {
        PlayerId playerId = playerId(9L);
        SettingsUser adapter = new SettingsUser(playerId);

        assertEquals(playerId, adapter.toPlayerId());
        assertThrows(IllegalStateException.class, new SettingsUser()::toPlayerId);
    }

    @Test
    void frontierUserAccessCopiesActionsAndRetainsImmutableIdentity() {
        FrontierUserAccess original = sharedUser(1L);
        original.addAction(FrontierUserAccess.Action.UpdateFrontier);

        FrontierUserAccess copy = new FrontierUserAccess(original);
        original.addAction(FrontierUserAccess.Action.UpdateSettings);
        original.setPending(false);

        assertSame(original.getPlayerId(), copy.getPlayerId());
        assertEquals(EnumSet.of(FrontierUserAccess.Action.UpdateFrontier), copy.getActions());
        assertTrue(copy.isPending());
    }

    @Test
    void setActionsDoesNotRetainReceivedSet() {
        FrontierUserAccess sharedUser = sharedUser(2L);
        EnumSet<FrontierUserAccess.Action> actions = EnumSet.of(FrontierUserAccess.Action.UpdateFrontier);

        sharedUser.setActions(actions);
        actions.add(FrontierUserAccess.Action.UpdateSettings);

        assertEquals(EnumSet.of(FrontierUserAccess.Action.UpdateFrontier), sharedUser.getActions());
    }

    @Test
    void frontierSettingsCopyIsDeep() {
        FrontierSettings original = new FrontierSettings();
        original.getOPsGroup().addAction(FrontierSettings.Action.UpdateSettings);
        SettingsGroup originalGroup = original.createCustomGroup("group");
        PlayerId originalUser = playerId(3L);
        originalGroup.addUser(originalUser);
        originalGroup.addAction(FrontierSettings.Action.UpdateGlobalFrontier);

        FrontierSettings copy = new FrontierSettings(original);
        original.getOPsGroup().addAction(FrontierSettings.Action.CreateGlobalFrontier);
        originalGroup.setName("changed");
        originalGroup.addAction(FrontierSettings.Action.DeleteGlobalFrontier);

        assertEquals(EnumSet.of(FrontierSettings.Action.UpdateSettings), copy.getOPsGroup().getActions());
        SettingsGroup copiedGroup = copy.getCustomGroups().getFirst();
        assertEquals("group", copiedGroup.getName());
        assertEquals(originalUser, copiedGroup.getUsers().getFirst());
        assertEquals(EnumSet.of(FrontierSettings.Action.UpdateGlobalFrontier), copiedGroup.getActions());
        assertFalse(copy.hasSameFunctionalState(original));
    }

    @Test
    void frontierDataCopyAndUpdateCopySharedUsers() {
        FrontierData original = personalFrontier();
        FrontierUserAccess originalSharedUser = sharedUser(4L);
        originalSharedUser.addAction(FrontierUserAccess.Action.UpdateFrontier);
        original.addUserAccess(originalSharedUser);

        FrontierData constructedCopy = new FrontierData(original);
        FrontierData updatedCopy = personalFrontier();
        updatedCopy.updateFromData(original);
        originalSharedUser.addAction(FrontierUserAccess.Action.UpdateSettings);

        assertSharedUserWasCopied(constructedCopy);
        assertSharedUserWasCopied(updatedCopy);
    }

    private static void assertSharedUserWasCopied(FrontierData frontier) {
        FrontierUserAccess sharedUser = frontier.getUserAccesses().getFirst();
        assertEquals(playerId(4L), sharedUser.getPlayerId());
        assertTrue(sharedUser.hasAction(FrontierUserAccess.Action.UpdateFrontier));
        assertFalse(sharedUser.hasAction(FrontierUserAccess.Action.UpdateSettings));
    }

    private static FrontierData personalFrontier() {
        FrontierData frontier = new FrontierData(playerId(10L));
        frontier.setPersonal(true);
        return frontier;
    }

    private static FrontierUserAccess sharedUser(long uuidValue) {
        return new FrontierUserAccess(playerId(uuidValue), true);
    }

    private static PlayerId playerId(long uuidValue) {
        return new PlayerId(new UUID(0L, uuidValue));
    }
}
