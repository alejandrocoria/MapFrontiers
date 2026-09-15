package games.alejandrocoria.mapfrontiers.common.settings;

import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsCopyTest {
    @Test
    void settingsUserSharedCopiesUserAndActions() {
        SettingsUserShared original = sharedUser("original", 1L);
        original.addAction(SettingsUserShared.Action.UpdateFrontier);

        SettingsUserShared copy = new SettingsUserShared(original);
        original.getUser().username = "changed";
        original.addAction(SettingsUserShared.Action.UpdateSettings);
        original.setPending(false);

        assertEquals("original", copy.getUser().username);
        assertEquals(EnumSet.of(SettingsUserShared.Action.UpdateFrontier), copy.getActions());
        assertTrue(copy.isPending());
        assertNotSame(original.getUser(), copy.getUser());
    }

    @Test
    void setActionsDoesNotRetainReceivedSet() {
        SettingsUserShared sharedUser = sharedUser("user", 2L);
        EnumSet<SettingsUserShared.Action> actions = EnumSet.of(SettingsUserShared.Action.UpdateFrontier);

        sharedUser.setActions(actions);
        actions.add(SettingsUserShared.Action.UpdateSettings);

        assertEquals(EnumSet.of(SettingsUserShared.Action.UpdateFrontier), sharedUser.getActions());
    }

    @Test
    void frontierSettingsCopyIsDeep() {
        FrontierSettings original = new FrontierSettings();
        original.getOPsGroup().addAction(FrontierSettings.Action.UpdateSettings);
        SettingsGroup originalGroup = original.createCustomGroup("group");
        SettingsUser originalUser = user("original", 3L);
        originalGroup.addUser(originalUser);
        originalGroup.addAction(FrontierSettings.Action.UpdateGlobalFrontier);

        FrontierSettings copy = new FrontierSettings(original);
        original.getOPsGroup().addAction(FrontierSettings.Action.CreateGlobalFrontier);
        originalGroup.setName("changed");
        originalUser.username = "changed";
        originalGroup.addAction(FrontierSettings.Action.DeleteGlobalFrontier);

        assertEquals(EnumSet.of(FrontierSettings.Action.UpdateSettings), copy.getOPsGroup().getActions());
        SettingsGroup copiedGroup = copy.getCustomGroups().getFirst();
        assertEquals("group", copiedGroup.getName());
        assertEquals("original", copiedGroup.getUsers().getFirst().username);
        assertEquals(EnumSet.of(FrontierSettings.Action.UpdateGlobalFrontier), copiedGroup.getActions());
        assertFalse(copy.hasSameFunctionalState(original));
    }

    @Test
    void frontierDataCopyAndUpdateCopySharedUsers() {
        FrontierData original = personalFrontier();
        SettingsUserShared originalSharedUser = sharedUser("original", 4L);
        originalSharedUser.addAction(SettingsUserShared.Action.UpdateFrontier);
        original.addUserShared(originalSharedUser);

        FrontierData constructedCopy = new FrontierData(original);
        FrontierData updatedCopy = personalFrontier();
        updatedCopy.updateFromData(original);

        originalSharedUser.getUser().username = "changed";
        originalSharedUser.addAction(SettingsUserShared.Action.UpdateSettings);

        assertSharedUserWasCopied(constructedCopy);
        assertSharedUserWasCopied(updatedCopy);
    }

    private static void assertSharedUserWasCopied(FrontierData frontier) {
        SettingsUserShared sharedUser = frontier.getUsersShared().getFirst();
        assertEquals("original", sharedUser.getUser().username);
        assertTrue(sharedUser.hasAction(SettingsUserShared.Action.UpdateFrontier));
        assertFalse(sharedUser.hasAction(SettingsUserShared.Action.UpdateSettings));
    }

    private static FrontierData personalFrontier() {
        FrontierData frontier = new FrontierData();
        frontier.setPersonal(true);
        frontier.setOwner(user("owner", 10L));
        return frontier;
    }

    private static SettingsUserShared sharedUser(String username, long uuidValue) {
        return new SettingsUserShared(user(username, uuidValue), true);
    }

    private static SettingsUser user(String username, long uuidValue) {
        SettingsUser user = new SettingsUser();
        user.username = username;
        user.uuid = new UUID(0L, uuidValue);
        return user;
    }
}
