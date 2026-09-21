package games.alejandrocoria.mapfrontiers.server.settings;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoriesManager;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoryPermissionEvaluator;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerSettingsRevisionTest {
    @Test
    void pollOnlyReturnsSnapshotWhenRevisionDiffers() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        ServerSettingsOperationService service = service(manager);

        ServerSettingsOperationResult equal = service.requestSettings(null, 0L);
        assertEquals(ServerSettingsOperationResult.Status.Ignored, equal.getStatus());
        assertEquals(0, equal.getNetworkActionCount());

        ServerSettingsOperationResult lower = service.requestSettings(null, -1L);
        assertEquals(ServerSettingsOperationResult.Status.Success, lower.getStatus());
        assertEquals(1, lower.getNetworkActionCount());

        ServerSettingsOperationResult greater = service.requestSettings(null, 1L);
        assertEquals(ServerSettingsOperationResult.Status.Success, greater.getStatus());
        assertEquals(1, greater.getNetworkActionCount());
    }

    @Test
    void commitAdvancesRevisionWhileNoOpAndRejectionPreserveIt() {
        TerritoriesManager manager = new TerritoriesManager(new PlayerNameRepository(), username -> null);
        ServerSettingsOperationService service = service(manager);
        FrontierSettings changed = new FrontierSettings(manager.getSettings());
        changed.getEveryoneGroup().addAction(FrontierSettings.Action.SharePersonalFrontier);

        ServerSettingsOperationResult committed = service.updateSettings(null, changed, 0L, 51L);
        assertEquals(ServerSettingsOperationResult.Status.Success, committed.getStatus());
        assertEquals(1L, manager.getSettingsRevision());
        assertEquals(2, committed.getNetworkActionCount());

        ServerSettingsOperationResult noOp = service.updateSettings(null, changed, 1L, 52L);
        assertEquals(ServerSettingsOperationResult.Status.Success, noOp.getStatus());
        assertEquals(1L, manager.getSettingsRevision());
        assertEquals(1, noOp.getNetworkActionCount());

        FrontierSettings staleChange = new FrontierSettings(changed);
        staleChange.getEveryoneGroup().addAction(FrontierSettings.Action.CreateGlobalFrontier);
        ServerSettingsOperationResult rejected = service.updateSettings(null, staleChange, 0L, 53L);
        assertEquals(ServerSettingsOperationResult.Status.Rejected, rejected.getStatus());
        assertEquals(1L, manager.getSettingsRevision());
        assertEquals(1, rejected.getNetworkActionCount());
    }

    private static ServerSettingsOperationService service(TerritoriesManager manager) {
        TerritoryPermissionEvaluator permissions = new TerritoryPermissionEvaluator(manager) {
            @Override
            public boolean canUpdateSettings(ServerPlayer player) {
                return true;
            }
        };
        return new ServerSettingsOperationService(null, manager, permissions, playerId -> null);
    }
}
