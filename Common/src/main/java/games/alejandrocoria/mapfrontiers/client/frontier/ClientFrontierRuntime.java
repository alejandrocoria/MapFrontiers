package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.client.api.MapFrontiersClientAPIImpl;
import games.alejandrocoria.mapfrontiers.client.settings.ClientSettingsProfileEvents;
import journeymap.api.v2.client.IClientAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ClientFrontierRuntime {
    private final IClientAPI journeyMapApi;
    private FrontiersOverlayManager globalFrontiersOverlayManager;
    private FrontiersOverlayManager personalFrontiersOverlayManager;
    private ClientLocalPersonalFrontierStore localPersonalFrontierStore;
    private ClientFrontierEvents frontierEvents;
    private ClientSettingsProfileEvents settingsProfileEvents;
    private ClientFrontierOperationService operationService;
    private ClientFrontierSyncService syncService;
    private FrontierLocalOverrides localOverrides;
    private MapFrontiersClientAPIImpl clientApi;

    public ClientFrontierRuntime(IClientAPI journeyMapApi) {
        this.journeyMapApi = journeyMapApi;
    }

    public void ensureInitialized() {
        if (globalFrontiersOverlayManager == null) {
            globalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi);
        }

        if (personalFrontiersOverlayManager == null) {
            personalFrontiersOverlayManager = new FrontiersOverlayManager(journeyMapApi);
        }

        if (localPersonalFrontierStore == null) {
            localPersonalFrontierStore = new ClientLocalPersonalFrontierStore();
        }

        if (frontierEvents == null) {
            frontierEvents = new ClientFrontierEvents();
        }

        if (settingsProfileEvents == null) {
            settingsProfileEvents = new ClientSettingsProfileEvents();
        }

        if (operationService == null) {
            operationService = new ClientFrontierOperationService(globalFrontiersOverlayManager, personalFrontiersOverlayManager,
                    localPersonalFrontierStore, frontierEvents);
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

    public FrontiersOverlayManager getGlobalFrontiersOverlayManager() {
        ensureInitialized();
        return globalFrontiersOverlayManager;
    }

    public FrontiersOverlayManager getPersonalFrontiersOverlayManager() {
        ensureInitialized();
        return personalFrontiersOverlayManager;
    }

    public FrontierLocalOverrides getLocalOverrides() {
        ensureInitialized();
        return localOverrides;
    }

    public ClientFrontierOperationService getOperationService() {
        ensureInitialized();
        return operationService;
    }

    public ClientFrontierEvents getFrontierEvents() {
        ensureInitialized();
        return frontierEvents;
    }

    public ClientSettingsProfileEvents getSettingsProfileEvents() {
        ensureInitialized();
        return settingsProfileEvents;
    }

    public ClientFrontierSyncService getSyncService() {
        ensureInitialized();
        return syncService;
    }

    public MapFrontiersClientAPIImpl getOrCreateClientApi() {
        ensureInitialized();
        if (clientApi == null) {
            clientApi = new MapFrontiersClientAPIImpl(frontierEvents);
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

        if (frontierEvents != null) {
            frontierEvents.close();
            frontierEvents = null;
        }

        if (settingsProfileEvents != null) {
            settingsProfileEvents.close();
            settingsProfileEvents = null;
        }

        operationService = null;
        localPersonalFrontierStore = null;
        localOverrides = null;
    }
}
