package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.event.EventHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class MapFrontiers {
    public static final String MODID = "mapfrontiers";
    public static final Logger LOGGER = LogManager.getLogger("MapFrontiers");

    private static FrontiersManager frontiersManager;

    public MapFrontiers() {

    }

    protected static void init() {
        PacketHandler.init();

        EventHandler.subscribeCommandRegistrationEvent(MapFrontiers.class, CommandAccept::register);

        EventHandler.subscribeServerStartingEvent(MapFrontiers.class, server -> {
            frontiersManager = new FrontiersManager();
            frontiersManager.loadOrCreateData(server);

            LOGGER.info("ServerStartingEvent done");
        });

        EventHandler.subscribeServerStoppingEvent(MapFrontiers.class, server -> {
            if (frontiersManager != null) {
                frontiersManager.close();
            }
            frontiersManager = null;

            LOGGER.info("ServerStoppingEvent done");
        });

        EventHandler.subscribePlayerJoinedEvent(MapFrontiers.class, (server, player) -> {
            if (frontiersManager == null) {
                return;
            }

            frontiersManager.ensureOwners(server);

            PacketHandler.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(player)), player);

            PacketFrontiers packetFrontiers = new PacketFrontiers();
            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
                packetFrontiers.addGlobalFrontiers(frontiers);
            }

            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(player)).values()) {
                packetFrontiers.addPersonalFrontiers(frontiers);
            }

            PacketHandler.sendTo(packetFrontiers, player);

            LOGGER.info("PlayerJoinedEvent done (" + player.getStringUUID() + ")");
        });
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        return server.getPlayerList().isOp(player.getGameProfile());
    }
}
