package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketNewFrontier implements IMessage {
    private int dimension = 0;
    private boolean addVertex = false;
    private int snapDistance = 0;

    public PacketNewFrontier() {
    }

    public PacketNewFrontier(int dimension, boolean addVertex, int snapDistance) {
        this.dimension = dimension;
        this.addVertex = addVertex;
        this.snapDistance = snapDistance;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        addVertex = buf.readBoolean();
        snapDistance = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeBoolean(addVertex);
        buf.writeInt(snapDistance);
    }

    public static class Handler implements IMessageHandler<PacketNewFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketNewFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    FrontierData frontier = FrontiersManager.instance.createNewfrontier(message.dimension,
                            ctx.getServerHandler().player, message.addVertex, message.snapDistance);

                    PacketHandler.INSTANCE.sendToAll(new PacketFrontier(frontier, ctx.getServerHandler().player.getEntityId()));
                });
            }

            return null;
        }
    }
}
