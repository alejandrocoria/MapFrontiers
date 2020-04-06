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

    public PacketUpdateFrontier(FrontierData frontier, int playerID) {
        this.frontier = frontier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontier.readFromNBT(ByteBufUtils.readTag(buf));
        frontier.setId(buf.readInt());
        frontier.setDimension(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        frontier.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(frontier.getId());
        buf.writeInt(frontier.getDimension());
    }

    public static class Handler implements IMessageHandler<PacketUpdateFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;

                    FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getDimension(),
                            message.frontier.getId());

                    if (currentFrontier.getOwnerUUID() != null) {
                        message.frontier.setOwner(currentFrontier.getOwnerUUID());
                    }
                    if ((message.frontier.getOwnerName() == null || message.frontier.getOwnerName().isEmpty())
                            && currentFrontier.getOwnerName() != null && !currentFrontier.getOwnerName().isEmpty()) {
                        message.frontier.setOwner(currentFrontier.getOwnerName());
                    }

                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateFrontier,
                            new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player),
                            new SettingsUser(message.frontier.getOwnerName(), message.frontier.getOwnerUUID()))) {
                        boolean updated = FrontiersManager.instance.updateFrontier(message.frontier);

                        if (updated) {
                            PacketHandler.INSTANCE.sendToAll(
                                    new PacketFrontierUpdated(message.frontier, ctx.getServerHandler().player.getEntityId()));
                        }
                    }
                });
            }

            return null;
        }
    }
}
