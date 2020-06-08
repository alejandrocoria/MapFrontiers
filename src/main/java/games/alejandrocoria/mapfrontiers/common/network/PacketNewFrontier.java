package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketNewFrontier implements IMessage {
    private int dimension = 0;
    private boolean personal = false;
    private BlockPos vertex;

    public PacketNewFrontier() {
    }

    public PacketNewFrontier(int dimension, boolean personal, @Nullable BlockPos vertex) {
        this.dimension = dimension;
        this.personal = personal;
        this.vertex = vertex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        personal = buf.readBoolean();

        boolean hasVertex = buf.readBoolean();
        if (hasVertex) {
            vertex = BlockPos.fromLong(buf.readLong());
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeBoolean(personal);

        buf.writeBoolean(vertex != null);
        if (vertex != null) {
            buf.writeLong(vertex.toLong());
        }
    }

    public static class Handler implements IMessageHandler<PacketNewFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketNewFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    FrontierData frontier = null;

                    if (message.personal) {
                        if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player), null)) {
                            frontier = FrontiersManager.instance.createNewPersonalFrontier(message.dimension, player,
                                    message.vertex);
                            // @Incomplete: send to all players with access to this personal frontier
                            PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier, player.getEntityId()), player);

                            return;
                        }
                    } else {
                        if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.CreateFrontier,
                                new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player), null)) {
                            frontier = FrontiersManager.instance.createNewFrontier(message.dimension, player, message.vertex);
                            PacketHandler.INSTANCE.sendToAll(new PacketFrontier(frontier, player.getEntityId()));

                            return;
                        }
                    }
                    PacketHandler.INSTANCE.sendTo(
                            new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                });
            }

            return null;
        }
    }
}
