package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketFrontier implements IMessage {
    private FrontierData frontier;
    private int playerID = -1;

    public PacketFrontier() {
        frontier = new FrontierData();
    }

    public PacketFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontier(FrontierData frontier, int playerID) {
        this.frontier = frontier;
        this.playerID = playerID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontier.readFromNBT(ByteBufUtils.readTag(buf));
        frontier.setId(buf.readInt());
        frontier.setDimension(buf.readInt());
        playerID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        frontier.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(frontier.getId());
        buf.writeInt(frontier.getDimension());
        buf.writeInt(playerID);
    }

    public static class Handler implements IMessageHandler<PacketFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontier message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    FrontierOverlay frontierOverlay = FrontiersOverlayManager.instance.addFrontier(message.frontier);

                    ((ClientProxy) MapFrontiers.proxy).frontierChanged(null);

                    if (frontierOverlay != null && Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
                        ((GuiFrontierBook) Minecraft.getMinecraft().currentScreen).newFrontierMessage(frontierOverlay,
                                message.playerID);
                    }
                });
            }

            return null;
        }
    }
}
