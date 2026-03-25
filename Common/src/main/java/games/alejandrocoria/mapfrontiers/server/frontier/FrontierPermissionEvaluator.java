package games.alejandrocoria.mapfrontiers.server.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FrontierPermissionEvaluator {
    private final FrontiersManager frontiersManager;

    public FrontierPermissionEvaluator(FrontiersManager frontiersManager) {
        this.frontiersManager = frontiersManager;
    }

    public FrontierSettings getSettings() {
        return frontiersManager.getSettings();
    }

    public SettingsUser getPlayerUser(ServerPlayer player) {
        return new SettingsUser(player);
    }

    public boolean canCreateGlobalFrontier(ServerPlayer player) {
        return getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier, getPlayerUser(player), MapFrontiers.isOPorHost(player), null);
    }

    public boolean canUpdateGlobalFrontier(ServerPlayer player, FrontierData frontier) {
        return getSettings().checkAction(FrontierSettings.Action.UpdateGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), frontier.getOwner());
    }

    public boolean canDeleteGlobalFrontier(ServerPlayer player, FrontierData frontier) {
        return getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), frontier.getOwner());
    }

    public boolean canSharePersonalFrontier(ServerPlayer player, FrontierData frontier) {
        return getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), frontier.getOwner());
    }

    public boolean canUpdateSettings(ServerPlayer player) {
        return getSettings().checkAction(FrontierSettings.Action.UpdateSettings, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), null);
    }

    public boolean canUpdatePersonalFrontier(ServerPlayer player, FrontierData frontier) {
        return frontier.checkActionUserShared(getPlayerUser(player), SettingsUserShared.Action.UpdateFrontier);
    }

    public boolean canManagePersonalShareSettings(ServerPlayer player, FrontierData frontier) {
        return frontier.checkActionUserShared(getPlayerUser(player), SettingsUserShared.Action.UpdateSettings);
    }

    public boolean canSendCommandAcceptFrontier(ServerPlayer player) {
        return getSettings().checkAction(FrontierSettings.Action.SharePersonalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), null);
    }

    public SettingsProfile getProfile(ServerPlayer player) {
        return getSettings().getProfile(player);
    }

    public PacketSettingsProfile createProfilePacket(ServerPlayer player) {
        return new PacketSettingsProfile(getProfile(player));
    }
}
