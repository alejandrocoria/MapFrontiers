package games.alejandrocoria.mapfrontiers.server.settings;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerReferenceCollector;
import games.alejandrocoria.mapfrontiers.common.network.OperationResolution;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketPlayerNameMappings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoriesManager;
import games.alejandrocoria.mapfrontiers.server.territory.TerritoryPermissionEvaluator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ServerSettingsOperationService {
    private final MinecraftServer server;
    private final TerritoriesManager territoriesManager;
    private final TerritoryPermissionEvaluator permissionEvaluator;
    private final PlayerNameResolver playerNameResolver;

    public ServerSettingsOperationService(MinecraftServer server, TerritoriesManager territoriesManager,
                                          TerritoryPermissionEvaluator permissionEvaluator,
                                          PlayerNameResolver playerNameResolver) {
        this.server = server;
        this.territoriesManager = territoriesManager;
        this.permissionEvaluator = permissionEvaluator;
        this.playerNameResolver = playerNameResolver;
    }

    public ServerSettingsOperationResult requestSettings(ServerPlayer player, long clientRevision) {
        FrontierSettings settings = territoriesManager.getSettings();
        if (!permissionEvaluator.canUpdateSettings(player)) {
            return rejectedWithProfileRefresh(player);
        }

        if (clientRevision == territoriesManager.getSettingsRevision()) {
            return ServerSettingsOperationResult.ignored();
        }

        ServerSettingsOperationResult result = ServerSettingsOperationResult.success();
        PacketFrontierSettings settingsPacket = new PacketFrontierSettings(settings,
                territoriesManager.getSettingsRevision(), 0L, OperationResolution.Accepted);
        result.addNetworkAction(() -> sendSettings(player, settingsPacket, settings));
        return result;
    }

    public ServerSettingsOperationResult updateSettings(ServerPlayer player, FrontierSettings settings,
                                                        long baseRevision, long requestId) {
        if (!permissionEvaluator.canUpdateSettings(player)) {
            return rejectedUpdateWithProfileRefresh(player, requestId);
        }

        long currentRevision = territoriesManager.getSettingsRevision();
        if (baseRevision != currentRevision) {
            return rejectedUpdate(player, requestId);
        }

        if (territoriesManager.getSettings().hasSameFunctionalState(settings)) {
            ServerSettingsOperationResult result = ServerSettingsOperationResult.success();
            enqueueSettingsResponse(result, player, requestId, OperationResolution.Accepted);
            return result;
        }

        territoriesManager.setSettings(settings, currentRevision + 1L);

        ServerSettingsOperationResult result = ServerSettingsOperationResult.success();
        enqueueSettingsResponse(result, player, requestId, OperationResolution.Accepted);
        result.addNetworkAction(() -> {
            for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                PacketHandler.sendTo(permissionEvaluator.createProfilePacket(otherPlayer), otherPlayer);
            }
        });
        return result;
    }

    private ServerSettingsOperationResult rejectedUpdate(ServerPlayer player, long requestId) {
        ServerSettingsOperationResult result = ServerSettingsOperationResult.rejected();
        enqueueSettingsResponse(result, player, requestId, OperationResolution.Rejected);
        return result;
    }

    private ServerSettingsOperationResult rejectedUpdateWithProfileRefresh(ServerPlayer player, long requestId) {
        ServerSettingsOperationResult result = rejectedUpdate(player, requestId);
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }

    private void enqueueSettingsResponse(ServerSettingsOperationResult result, ServerPlayer player, long requestId,
                                         OperationResolution resolution) {
        PacketFrontierSettings response = new PacketFrontierSettings(territoriesManager.getSettings(),
                territoriesManager.getSettingsRevision(), requestId, resolution);
        result.addNetworkAction(() -> sendSettings(player, response, territoriesManager.getSettings()));
    }

    private ServerSettingsOperationResult rejectedWithProfileRefresh(ServerPlayer player) {
        ServerSettingsOperationResult result = ServerSettingsOperationResult.rejected();
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }

    private void sendSettings(ServerPlayer player, PacketFrontierSettings settingsPacket, FrontierSettings settings) {
        PacketPlayerNameMappings mappings = new PacketPlayerNameMappings(PlayerReferenceCollector.collect(settings), playerNameResolver);
        if (!mappings.isEmpty()) {
            PacketHandler.sendTo(mappings, player);
        }
        PacketHandler.sendTo(settingsPacket, player);
    }
}
