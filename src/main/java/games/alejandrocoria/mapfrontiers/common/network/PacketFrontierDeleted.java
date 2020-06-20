package games.alejandrocoria.mapfrontiers.common.network;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiShareSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted implements IMessage {
    private int dimension;
    private UUID frontierID;
    private boolean personal;
    private int playerID = -1;

    public PacketFrontierDeleted() {

    }

    public PacketFrontierDeleted(int dimension, UUID frontierID, boolean personal, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.personal = personal;
        this.playerID = playerID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        frontierID = new UUID(buf.readLong(), buf.readLong());
        personal = buf.readBoolean();
        playerID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeLong(frontierID.getMostSignificantBits());
        buf.writeLong(frontierID.getLeastSignificantBits());
        buf.writeBoolean(personal);
        buf.writeInt(playerID);
    }

    public static class Handler implements IMessageHandler<PacketFrontierDeleted, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontierDeleted message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    int frontierIndex = ((ClientProxy) MapFrontiers.proxy).getFrontiersOverlayManager(message.personal)
                            .deleteFrontier(message.dimension, message.frontierID);

                    MapFrontiers.proxy.frontierChanged();

                    if (frontierIndex != -1) {
                        if (Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
                            ((GuiFrontierBook) Minecraft.getMinecraft().currentScreen).deleteFrontierMessage(frontierIndex,
                                    message.dimension, message.personal, message.playerID);
                        } else if (Minecraft.getMinecraft().currentScreen instanceof GuiShareSettings) {
                            ((GuiShareSettings) Minecraft.getMinecraft().currentScreen).deleteFrontierMessage(frontierIndex,
                                    message.dimension, message.frontierID, message.personal, message.playerID);
                        }
                    }
                });
            }

            return null;
        }
    }
}
