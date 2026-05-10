package games.alejandrocoria.mapfrontiers.server.territory;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.FrontierData;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TerritoryPermissionEvaluator {
    private final TerritoriesManager territoriesManager;

    public TerritoryPermissionEvaluator(TerritoriesManager territoriesManager) {
        this.territoriesManager = territoriesManager;
    }

    public FrontierSettings getSettings() {
        return territoriesManager.getSettings();
    }

    public SettingsUser getPlayerUser(ServerPlayer player) {
        return new SettingsUser(player);
    }

    public boolean canCreateGlobalFrontier(ServerPlayer player) {
        return getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier, getPlayerUser(player), MapFrontiers.isOPorHost(player), null);
    }

    public boolean canCreateGlobalCollection(ServerPlayer player) {
        return canCreateGlobalFrontier(player);
    }

    public boolean canUpdateGlobalFrontier(ServerPlayer player, FrontierData frontier) {
        return getSettings().checkAction(FrontierSettings.Action.UpdateGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), frontier.getOwner());
    }

    public boolean canUpdateGlobalCollection(ServerPlayer player, CollectionData collection) {
        return getSettings().checkAction(FrontierSettings.Action.UpdateGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), collection.getOwner());
    }

    public boolean canDeleteGlobalFrontier(ServerPlayer player, FrontierData frontier) {
        return getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), frontier.getOwner());
    }

    public boolean canDeleteGlobalCollection(ServerPlayer player, CollectionData collection) {
        return getSettings().checkAction(FrontierSettings.Action.DeleteGlobalFrontier, getPlayerUser(player),
                MapFrontiers.isOPorHost(player), collection.getOwner());
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

    public boolean canUpdatePersonalCollection(ServerPlayer player, CollectionData collection) {
        return collection.getOwner().equals(getPlayerUser(player));
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
