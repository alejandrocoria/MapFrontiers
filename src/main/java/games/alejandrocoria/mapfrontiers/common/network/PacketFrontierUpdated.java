package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

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
public class PacketFrontierUpdated implements IMessage {
    private FrontierData frontier;
    private int playerID = -1;

    public PacketFrontierUpdated() {
        frontier = new FrontierData();
    }

    public PacketFrontierUpdated(FrontierData frontier) {
        this.frontier = frontier;
    }

    public PacketFrontierUpdated(FrontierData frontier, int playerID) {
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

    public static class Handler implements IMessageHandler<PacketFrontierUpdated, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontierUpdated message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (message.playerID != Minecraft.getMinecraft().player.getEntityId()) {
                        FrontierOverlay frontierOverlay = FrontiersOverlayManager.instance.updateFrontier(message.frontier);

                        if (frontierOverlay != null && Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
                            ((GuiFrontierBook) Minecraft.getMinecraft().currentScreen).updateFrontierMessage(frontierOverlay);
                        }
                    }
                });
            }

            return null;
        }
    }
}
