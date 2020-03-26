package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier implements IMessage {
    private int dimension;
    private int frontierID = -1;

    public PacketDeleteFrontier() {
    }

    public PacketDeleteFrontier(int dimension, int frontierID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        frontierID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(frontierID);
    }

    public static class Handler implements IMessageHandler<PacketDeleteFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketDeleteFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    FrontiersManager.instance.deleteFrontier(message.dimension, message.frontierID);

                    PacketHandler.INSTANCE.sendToAll(new PacketFrontierDeleted(message.dimension, message.frontierID,
                            ctx.getServerHandler().player.getEntityId()));
                });
            }

            return null;
        }
    }
}
