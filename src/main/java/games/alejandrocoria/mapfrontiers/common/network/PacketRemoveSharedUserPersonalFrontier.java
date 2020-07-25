package games.alejandrocoria.mapfrontiers.common.network;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketRemoveSharedUserPersonalFrontier implements IMessage {
    private UUID frontierID;
    private SettingsUser targetUser;

    public PacketRemoveSharedUserPersonalFrontier() {
        targetUser = new SettingsUser();
    }

    public PacketRemoveSharedUserPersonalFrontier(UUID frontierID, SettingsUser user) {
        this.frontierID = frontierID;
        targetUser = user;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontierID = UUIDHelper.fromBytes(buf);
        targetUser.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        targetUser.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<PacketRemoveSharedUserPersonalFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketRemoveSharedUserPersonalFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    SettingsUser playerUser = new SettingsUser(player);

                    message.targetUser.fillMissingInfo(false);
                    if (message.targetUser.uuid == null) {
                        return;
                    }

                    FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

                    if (currentFrontier != null && currentFrontier.getPersonal()) {
                        if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                playerUser, MapFrontiers.proxy.isOPorHost(player), currentFrontier.getOwner())) {
                            SettingsUserShared userShared = currentFrontier.getUserShared(message.targetUser);

                            if (userShared == null || userShared.getUser().equals(playerUser)) {
                                return;
                            }

                            if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateSettings)) {
                                currentFrontier.removeUserShared(message.targetUser);

                                if (userShared.isPending()) {
                                    FrontiersManager.instance.removePendingShareFrontier(message.targetUser);
                                } else {
                                    FrontiersManager.instance.deletePersonalFrontier(message.targetUser,
                                            currentFrontier.getDimension(), message.frontierID);

                                    Entity entity = FMLCommonHandler.instance().getMinecraftServerInstance()
                                            .getEntityFromUuid(message.targetUser.uuid);
                                    if (entity != null && entity instanceof EntityPlayerMP) {
                                        PacketHandler.INSTANCE.sendTo(new PacketFrontierDeleted(currentFrontier.getDimension(),
                                                message.frontierID, true, -1), (EntityPlayerMP) entity);
                                    }
                                }

                                PacketHandler.sendToUsersWithAccess(
                                        new PacketFrontierUpdated(currentFrontier, ctx.getServerHandler().player.getEntityId()),
                                        currentFrontier);
                            }
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
