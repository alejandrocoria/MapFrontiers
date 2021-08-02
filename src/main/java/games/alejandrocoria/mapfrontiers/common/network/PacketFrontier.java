package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiShareSettings;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketFrontier
{
    private final FrontierData frontier;
    private int playerID = -1;

    public PacketFrontier()
    {
        frontier = new FrontierData();
    }

    public PacketFrontier(FrontierData frontier)
    {
        this.frontier = frontier;
    }

    public PacketFrontier(FrontierData frontier, int playerID)
    {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    public static PacketFrontier fromBytes(FriendlyByteBuf buf)
    {
        PacketFrontier packet = new PacketFrontier();
        packet.frontier.fromBytes(buf);
        packet.playerID = buf.readInt();
        return packet;
    }

    public static void toBytes(PacketFrontier packet, FriendlyByteBuf buf)
    {
        packet.frontier.toBytes(buf, false);
        buf.writeInt(packet.playerID);
    }

    public static void handle(PacketFrontier message, Supplier<NetworkEvent.Context> ctx)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFrontier message, NetworkEvent.Context ctx)
    {
        FrontierOverlay frontierOverlay = ClientProxy.getFrontiersOverlayManager(message.frontier.getPersonal())
                .addFrontier(message.frontier);

        ClientProxy.frontierChanged();

        if (frontierOverlay != null)
        {
            if (Minecraft.getInstance().screen instanceof GuiFrontierBook)
            {
                ((GuiFrontierBook) Minecraft.getInstance().screen).newFrontierMessage(frontierOverlay, message.playerID);
            }
            else if (Minecraft.getInstance().screen instanceof GuiShareSettings)
            {
                ((GuiShareSettings) Minecraft.getInstance().screen).newFrontierMessage(frontierOverlay, message.playerID);
            }
        }
        ctx.setPacketHandled(true);
    }
}
