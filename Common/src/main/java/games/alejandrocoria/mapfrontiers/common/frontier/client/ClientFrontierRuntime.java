package games.alejandrocoria.mapfrontiers.common.frontier.client;

import games.alejandrocoria.mapfrontiers.client.FrontierLocalOverrides;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.api.client.MapFrontiersClientAPIImpl;
import journeymap.api.v2.client.IClientAPI;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClientFrontierRuntime {
    private @Nullable IClientAPI journeyMapApi;
    private @Nullable FrontiersOverlayManager globalFrontiersOverlayManager;
    private @Nullable FrontiersOverlayManager personalFrontiersOverlayManager;
    private @Nullable ClientLocalPersonalFrontierStore localPersonalFrontierStore;
    private @Nullable ClientFrontierEventBridge frontierEventBridge;
    private @Nullable ClientSettingsProfileBridge settingsProfileBridge;
    private @Nullable ClientFrontierCommandService commandService;
    private @Nullable ClientFrontierSyncService syncService;
    private @Nullable FrontierLocalOverrides localOverrides;
    private @Nullable MapFrontiersClientAPIImpl clientApi;

    public ClientFrontierRuntime(@Nullable IClientAPI journeyMapApi) {
        this.journeyMapApi = journeyMapApi;
    }

    public void setJourneyMapApi(@Nullable IClientAPI journeyMapApi) {
        this.journeyMapApi = journeyMapApi;
    }

    public void ensureInitialized() {
        if (journeyMapApi == null) {
            return;
        }

        if (globalFrontiersOverlayManager == null) {
            globalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi, false);
        }

        if (personalFrontiersOverlayManager == null) {
            personalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi, true);
        }

        if (localPersonalFrontierStore == null) {
            localPersonalFrontierStore = new ClientLocalPersonalFrontierStore();
        }

        if (frontierEventBridge == null) {
            frontierEventBridge = new ClientFrontierEventBridge();
        }

        if (settingsProfileBridge == null) {
            settingsProfileBridge = new ClientSettingsProfileBridge();
        }

        if (commandService == null) {
            commandService = new ClientFrontierCommandService(globalFrontiersOverlayManager, personalFrontiersOverlayManager,
                    localPersonalFrontierStore, frontierEventBridge);
        }

        if (syncService == null) {
            syncService = new ClientFrontierSyncService(globalFrontiersOverlayManager, personalFrontiersOverlayManager, localPersonalFrontierStore);
            syncService.loadLocalPersonalFrontiers();
        }

        if (localOverrides == null) {
            localOverrides = new FrontierLocalOverrides();
        }
    }

    public boolean hasInitializedManagers() {
        return globalFrontiersOverlayManager != null && personalFrontiersOverlayManager != null;
    }

    @Nullable
    public FrontiersOverlayManager getGlobalFrontiersOverlayManager() {
        ensureInitialized();
        return globalFrontiersOverlayManager;
    }

    @Nullable
    public FrontiersOverlayManager getPersonalFrontiersOverlayManager() {
        ensureInitialized();
        return personalFrontiersOverlayManager;
    }

    @Nullable
    public FrontierLocalOverrides getLocalOverrides() {
        ensureInitialized();
        return localOverrides;
    }

    public ClientFrontierCommandService getCommandService() {
        ensureInitialized();
        return commandService;
    }

    public ClientFrontierEventBridge getFrontierEventBridge() {
        ensureInitialized();
        return frontierEventBridge;
    }

    public ClientSettingsProfileBridge getSettingsProfileBridge() {
        ensureInitialized();
        return settingsProfileBridge;
    }

    public ClientFrontierSyncService getSyncService() {
        ensureInitialized();
        return syncService;
    }

    public MapFrontiersClientAPIImpl getOrCreateClientApi() {
        ensureInitialized();
        if (clientApi == null) {
            clientApi = new MapFrontiersClientAPIImpl(frontierEventBridge);
        }

        return clientApi;
    }

    public void close() {
        if (globalFrontiersOverlayManager != null) {
            globalFrontiersOverlayManager.close();
            globalFrontiersOverlayManager = null;
        }

        if (personalFrontiersOverlayManager != null) {
            personalFrontiersOverlayManager.close();
            personalFrontiersOverlayManager = null;
        }

        if (syncService != null) {
            syncService.close();
            syncService = null;
        }

        if (clientApi != null) {
            clientApi.close();
            clientApi = null;
        }

        if (frontierEventBridge != null) {
            frontierEventBridge.close();
            frontierEventBridge = null;
        }

        if (settingsProfileBridge != null) {
            settingsProfileBridge.close();
            settingsProfileBridge = null;
        }

        commandService = null;
        localPersonalFrontierStore = null;
        localOverrides = null;
    }
}
