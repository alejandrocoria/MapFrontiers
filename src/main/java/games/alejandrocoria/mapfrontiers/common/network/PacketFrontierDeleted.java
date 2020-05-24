package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted implements IMessage {
    private int dimension;
    private int frontierID = -1;
    private int playerID = -1;

    public PacketFrontierDeleted() {
    }

    public PacketFrontierDeleted(int dimension, int frontierID, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.playerID = playerID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        frontierID = buf.readInt();
        playerID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(frontierID);
        buf.writeInt(playerID);
    }

    public static class Handler implements IMessageHandler<PacketFrontierDeleted, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontierDeleted message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    int frontierIndex = FrontiersOverlayManager.instance.deleteFrontier(message.dimension, message.frontierID);

                    MapFrontiers.proxy.frontierChanged();

                    if (frontierIndex != -1 && Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
                        ((GuiFrontierBook) Minecraft.getMinecraft().currentScreen).deleteFrontierMessage(frontierIndex,
                                message.dimension, message.playerID);
                    }
                });
            }

            return null;
        }
    }
}
