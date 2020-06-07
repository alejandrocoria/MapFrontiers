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
public class PacketUpdateFrontier implements IMessage {
    private FrontierData frontier;

    public PacketUpdateFrontier() {
        frontier = new FrontierData();
    }

    public PacketUpdateFrontier(FrontierData frontier) {
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

    public static class Handler implements IMessageHandler<PacketUpdateFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;

                    FrontierData currentFrontier = null;
                    if (message.frontier.getPersonal()) {
                        currentFrontier = FrontiersManager.instance.getPersonalFrontierFromID(message.frontier.getOwner(),
                                message.frontier.getDimension(), message.frontier.getId());
                    } else {
                        currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getDimension(),
                                message.frontier.getId());
                    }

                    if (currentFrontier != null) {
                        if (!currentFrontier.getOwner().isEmpty()) {
                            message.frontier.setOwner(currentFrontier.getOwner());
                        }

                        if (message.frontier.getPersonal()) {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                    new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player),
                                    message.frontier.getOwner())) {
                                boolean updated = FrontiersManager.instance.updatePersonalFrontier(message.frontier);
                                if (updated) {
                                    // @Incomplete: send to all players with access to this personal frontier
                                    PacketHandler.INSTANCE.sendTo(new PacketFrontierUpdated(message.frontier,
                                            ctx.getServerHandler().player.getEntityId()), player);
                                }

                                return;
                            }
                        } else {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateFrontier,
                                    new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player),
                                    message.frontier.getOwner())) {
                                boolean updated = FrontiersManager.instance.updateFrontier(message.frontier);
                                if (updated) {
                                    PacketHandler.INSTANCE.sendToAll(new PacketFrontierUpdated(message.frontier,
                                            ctx.getServerHandler().player.getEntityId()));
                                }

                                return;
                            }
                        }

                        PacketHandler.INSTANCE.sendTo(
                                new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
                    }
                });
            }

            return null;
        }
    }
}
