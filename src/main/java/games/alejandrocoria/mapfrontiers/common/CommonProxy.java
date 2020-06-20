package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.command.CommandAccept;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@ParametersAreNonnullByDefault
public class CommonProxy {
    private FrontiersManager frontiersManager;

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {

    }

    public void serverStarting(FMLServerStartingEvent event) {
        frontiersManager = new FrontiersManager();
        frontiersManager.loadOrCreateData();

        MinecraftForge.EVENT_BUS.register(frontiersManager);
        event.registerServerCommand(new CommandAccept());
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        MinecraftForge.EVENT_BUS.unregister(frontiersManager);
        frontiersManager = null;
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (frontiersManager == null) {
            return;
        }

        frontiersManager.ensureOwners();

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllGlobalFrontiers().values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), (EntityPlayerMP) event.player);
            }
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllPersonalFrontiers(new SettingsUser(event.player))
                .values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), (EntityPlayerMP) event.player);
            }
        }

        PacketHandler.INSTANCE.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(event.player)),
                (EntityPlayerMP) event.player);
    }

    public boolean isOPorHost(EntityPlayer player) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        UserListOpsEntry opEntry = server.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());

        if (opEntry != null) {
            return true;
        }

        if (server.isSinglePlayer()) {
            return server.getServerOwner().equals(player.getName());
        }

        return false;
    }

    public void configUpdated() {

    }

    public void frontierChanged() {

    }
}