package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier implements IMessage {
    private FrontierData frontier;

    public PacketDeleteFrontier() {
        frontier = new FrontierData();
    }

    public PacketDeleteFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontier.readFromNBT(ByteBufUtils.readTag(buf));
        frontier.setId(buf.readInt());
        frontier.setDimension(buf.readInt());
        frontier.setPersonal(buf.readBoolean());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        frontier.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(frontier.getId());
        buf.writeInt(frontier.getDimension());
        buf.writeBoolean(frontier.getPersonal());
    }

    public static class Handler implements IMessageHandler<PacketDeleteFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketDeleteFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;

                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.DeleteFrontier,
                            new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player), message.frontier.getOwner())) {
                        if (message.frontier.getPersonal()) {
                            boolean deleted = FrontiersManager.instance.deletePersonalFrontier(message.frontier.getOwner(),
                                    message.frontier.getDimension(), message.frontier.getId());
                            if (deleted) {
                                // @Incomplete: send to all players with access to this personal frontier
                                PacketHandler.INSTANCE.sendTo(new PacketFrontierDeleted(message.frontier.getDimension(),
                                        message.frontier.getId(), message.frontier.getPersonal(), player.getEntityId()), player);
                            }
                        } else {
                            boolean deleted = FrontiersManager.instance.deleteFrontier(message.frontier.getDimension(),
                                    message.frontier.getId());
                            if (deleted) {
                                PacketHandler.INSTANCE.sendToAll(new PacketFrontierDeleted(message.frontier.getDimension(),
                                        message.frontier.getId(), message.frontier.getPersonal(), player.getEntityId()));
                            }
                        }
                    } else {
                        PacketHandler.INSTANCE.sendTo(
                                new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                    }
                });
            }

            return null;
        }
    }
}
