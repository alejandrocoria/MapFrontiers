package games.alejandrocoria.mapfrontiers.server.settings;

import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.server.frontier.FrontierPermissionEvaluator;
import games.alejandrocoria.mapfrontiers.server.frontier.FrontiersManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ServerSettingsOperationService {
    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;

    public ServerSettingsOperationService(MinecraftServer server, FrontiersManager frontiersManager,
                                          FrontierPermissionEvaluator permissionEvaluator) {
        this.server = server;
        this.frontiersManager = frontiersManager;
        this.permissionEvaluator = permissionEvaluator;
    }

    public ServerSettingsOperationResult requestSettings(ServerPlayer player, int clientChangeCounter) {
        FrontierSettings settings = frontiersManager.getSettings();
        if (permissionEvaluator.canUpdateSettings(player) && settings.getChangeCounter() > clientChangeCounter) {
            ServerSettingsOperationResult result = ServerSettingsOperationResult.success();
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierSettings(settings), player));
            return result;
        }

        return rejectedWithProfileRefresh(player);
    }

    public ServerSettingsOperationResult updateSettings(ServerPlayer player, FrontierSettings settings) {
        if (!permissionEvaluator.canUpdateSettings(player)) {
            return rejectedWithProfileRefresh(player);
        }

        frontiersManager.setSettings(settings);

        ServerSettingsOperationResult result = ServerSettingsOperationResult.success();
        result.addNetworkAction(() -> {
            for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                PacketHandler.sendTo(permissionEvaluator.createProfilePacket(otherPlayer), otherPlayer);
            }
        });
        return result;
    }

    private ServerSettingsOperationResult rejectedWithProfileRefresh(ServerPlayer player) {
        ServerSettingsOperationResult result = ServerSettingsOperationResult.rejected();
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }
}
