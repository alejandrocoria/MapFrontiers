package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class MapFrontiers implements ModInitializer {
    public static final String MODID = "mapfrontiers";
    public static final String VERSION = "1.18.2-2.2.0";
    public static Logger LOGGER;

    private static FrontiersManager frontiersManager;

    public MapFrontiers() {
        LOGGER = LogManager.getLogger("MapFrontiers");
    }

    @Override
    public void onInitialize() {
        PacketHandler.registerServerReceivers();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            CommandAccept.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            frontiersManager = new FrontiersManager();
            frontiersManager.loadOrCreateData(server);
            LOGGER.info("SERVER_STARTED done");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            frontiersManager = null;
            LOGGER.info("SERVER_STOPPING done");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (frontiersManager == null) {
                return;
            }

            frontiersManager.ensureOwners(server);

            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
                for (FrontierData frontier : frontiers) {
                    PacketHandler.sendTo(PacketFrontier.class, new PacketFrontier(frontier), handler.player);
                }
            }

            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(handler.player)).values()) {
                for (FrontierData frontier : frontiers) {
                    PacketHandler.sendTo(PacketFrontier.class, new PacketFrontier(frontier), handler.player);
                }
            }

            PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(frontiersManager.getSettings().getProfile(handler.player)), handler.player);
        });

        LOGGER.info("onInitialize done");
    }

    public static FrontiersManager getFrontiersManager() {
        return frontiersManager;
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ServerOpListEntry opEntry = server.getPlayerList().getOps().get(player.getGameProfile());
        if (opEntry != null) {
            return true;
        }

        if (server.isSingleplayer()) {
            return server.isSingleplayerOwner(player.getGameProfile()) || player.isCreative();
        }

        return false;
    }
}
