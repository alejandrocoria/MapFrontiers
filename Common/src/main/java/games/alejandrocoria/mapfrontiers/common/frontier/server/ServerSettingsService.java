package games.alejandrocoria.mapfrontiers.common.frontier.server;

import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ServerSettingsService {
    private final MinecraftServer server;
    private final FrontiersManager frontiersManager;
    private final FrontierPermissionEvaluator permissionEvaluator;

    public ServerSettingsService(MinecraftServer server, FrontiersManager frontiersManager,
                                 FrontierPermissionEvaluator permissionEvaluator) {
        this.server = server;
        this.frontiersManager = frontiersManager;
        this.permissionEvaluator = permissionEvaluator;
    }

    public ServerSettingsCommandResult requestSettings(ServerPlayer player, int clientChangeCounter) {
        FrontierSettings settings = frontiersManager.getSettings();
        if (permissionEvaluator.canUpdateSettings(player) && settings.getChangeCounter() > clientChangeCounter) {
            ServerSettingsCommandResult result = ServerSettingsCommandResult.success();
            result.addNetworkAction(() -> PacketHandler.sendTo(new PacketFrontierSettings(settings), player));
            return result;
        }

        return rejectedWithProfileRefresh(player);
    }

    public ServerSettingsCommandResult updateSettings(ServerPlayer player, FrontierSettings settings) {
        if (!permissionEvaluator.canUpdateSettings(player)) {
            return rejectedWithProfileRefresh(player);
        }

        frontiersManager.setSettings(settings);

        ServerSettingsCommandResult result = ServerSettingsCommandResult.success();
        result.addNetworkAction(() -> {
            for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
                PacketHandler.sendTo(permissionEvaluator.createProfilePacket(otherPlayer), otherPlayer);
            }
        });
        return result;
    }

    private ServerSettingsCommandResult rejectedWithProfileRefresh(ServerPlayer player) {
        ServerSettingsCommandResult result = ServerSettingsCommandResult.rejected();
        result.addNetworkAction(() -> PacketHandler.sendTo(permissionEvaluator.createProfilePacket(player), player));
        return result;
    }
}
