package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketSettingsProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.math.BlockPos;
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
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        frontiersManager = null;
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (frontiersManager == null) {
            return;
        }

        frontiersManager.ensureOwners();

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllFrontiers().values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), (EntityPlayerMP) event.player);
            }
        }

        PacketHandler.INSTANCE.sendTo(new PacketSettingsProfile(frontiersManager.getSettings().getProfile(event.player)),
                (EntityPlayerMP) event.player);
    }

    public BlockPos snapVertex(BlockPos vertex, int snapDistance, FrontierData owner) {
        float snapDistanceSq = snapDistance * snapDistance;
        BlockPos closest = new BlockPos(vertex.getX(), 70, vertex.getZ());
        double closestDistance = Double.MAX_VALUE;
        for (FrontierData frontier : frontiersManager.getAllFrontiers(owner.getDimension())) {
            if (frontier == owner) {
                continue;
            }

            for (int i = 0; i < frontier.getVertexCount(); ++i) {
                BlockPos v = frontier.getVertex(i);
                BlockPos v2 = new BlockPos(v.getX(), 70, v.getZ());
                double distance = v2.distanceSq(closest);
                if (distance < snapDistanceSq && distance < closestDistance) {
                    closestDistance = distance;
                    closest = v2;
                }
            }
        }

        return closest;
    }

    public boolean isOPorHost(EntityPlayer player) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        UserListOpsEntry opEntry = server.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());

        if (opEntry != null) {
            return true;
        }

        if (server.isSinglePlayer()) {
            return server.getServerOwner() == player.getName();
        }

        return false;
    }
}