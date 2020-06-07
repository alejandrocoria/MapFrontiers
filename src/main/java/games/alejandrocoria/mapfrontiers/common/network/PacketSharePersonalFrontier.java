package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketSharePersonalFrontier implements IMessage {
    private FrontierData frontier;
    private SettingsUser targetUser;

    public PacketSharePersonalFrontier() {
        frontier = new FrontierData();
        targetUser = new SettingsUser();
    }

    public PacketSharePersonalFrontier(FrontierData frontier, SettingsUser user) {
        this.frontier = frontier;
        targetUser = user;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontier.readFromNBT(ByteBufUtils.readTag(buf));
        frontier.setId(buf.readInt());
        frontier.setDimension(buf.readInt());
        frontier.setPersonal(buf.readBoolean());

        targetUser.readFromNBT(ByteBufUtils.readTag(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        frontier.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(frontier.getId());
        buf.writeInt(frontier.getDimension());
        buf.writeBoolean(frontier.getPersonal());

        nbt = new NBTTagCompound();
        targetUser.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
    }

    public static class Handler implements IMessageHandler<PacketSharePersonalFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketSharePersonalFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    SettingsUser playerUser = new SettingsUser(player);

                    message.targetUser.fillMissingInfo(false);
                    if (message.targetUser.uuid == null) {
                        return;
                    }

                    Entity entity = FMLCommonHandler.instance().getMinecraftServerInstance()
                            .getEntityFromUuid(message.targetUser.uuid);
                    EntityPlayerMP entityPlayerTarget = null;
                    if (entity != null && entity instanceof EntityPlayerMP) {
                        entityPlayerTarget = (EntityPlayerMP) entity;
                    }

                    if (entityPlayerTarget == null) {
                        return;
                    }

                    FrontierData currentFrontier = FrontiersManager.instance.getPersonalFrontierFromID(
                            message.frontier.getOwner(), message.frontier.getDimension(), message.frontier.getId());

                    if (currentFrontier != null) {
                        if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                playerUser, MapFrontiers.proxy.isOPorHost(player), currentFrontier.getOwner())) {
                            int shareMessageID = FrontiersManager.instance.addShareMessage(playerUser, currentFrontier.getOwner(),
                                    message.targetUser, currentFrontier.getDimension(), currentFrontier.getId());

                            PacketHandler.INSTANCE.sendTo(new PacketPersonalFrontierShared(shareMessageID, playerUser,
                                    currentFrontier.getOwner(), currentFrontier.getName1(), currentFrontier.getName2()),
                                    entityPlayerTarget);
                        } else {
                            PacketHandler.INSTANCE.sendTo(
                                    new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)),
                                    player);
                        }
                    }
                });
            }

            return null;
        }
    }
}
