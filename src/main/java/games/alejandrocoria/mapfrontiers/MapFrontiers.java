package games.alejandrocoria.mapfrontiers;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontiers;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

public class MapFrontiers implements ModInitializer {
    public static final String MODID = "mapfrontiers";
    public static String VERSION = "";
    public static Logger LOGGER;

    private static FrontiersManager frontiersManager;

    static {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MODID);
        modContainer.ifPresent(mod -> VERSION = mod.getMetadata().getVersion().getFriendlyString());
    }

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

            PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(frontiersManager.getSettings().getProfile(handler.player)), handler.player);

            PacketFrontiers packetFrontiers = new PacketFrontiers();
            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
                packetFrontiers.addGlobalFrontiers(frontiers);
            }

            for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(handler.player)).values()) {
                packetFrontiers.addPersonalFrontiers(frontiers);
            }

            PacketHandler.sendTo(PacketFrontiers.class, packetFrontiers, handler.player);
        });

        LOGGER.info("onInitialize done");
    }

    public static boolean isOPorHost(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        return server.getPlayerList().isOp(player.getGameProfile());
    }
}
