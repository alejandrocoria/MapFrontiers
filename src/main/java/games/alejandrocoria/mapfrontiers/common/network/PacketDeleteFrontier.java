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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketDeleteFrontier implements IMessage {
    private UUID frontierID;

    public PacketDeleteFrontier() {

    }

    public PacketDeleteFrontier(UUID frontierID) {
        this.frontierID = frontierID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontierID = UUIDHelper.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
    }

    public static class Handler implements IMessageHandler<PacketDeleteFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketDeleteFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    SettingsUser playerUser = new SettingsUser(player);

                    FrontierData frontier = FrontiersManager.instance.getFrontierFromID(message.frontierID);

                    if (frontier != null) {
                        if (frontier.getPersonal()) {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                    playerUser, MapFrontiers.proxy.isOPorHost(player), frontier.getOwner())) {
                                if (frontier.getOwner().equals(playerUser)) {
                                    boolean deleted = FrontiersManager.instance.deletePersonalFrontier(frontier.getOwner(),
                                            frontier.getDimension(), frontier.getId());
                                    if (deleted) {
                                        if (frontier.getUsersShared() != null) {
                                            for (SettingsUserShared userShared : frontier.getUsersShared()) {
                                                FrontiersManager.instance.deletePersonalFrontier(userShared.getUser(),
                                                        frontier.getDimension(), frontier.getId());
                                            }
                                        }
                                        PacketHandler.sendToUsersWithAccess(new PacketFrontierDeleted(frontier.getDimension(),
                                                frontier.getId(), frontier.getPersonal(), player.getEntityId()), frontier);
                                    }
                                } else {
                                    frontier.removeUserShared(playerUser);
                                    FrontiersManager.instance.deletePersonalFrontier(playerUser, frontier.getDimension(),
                                            frontier.getId());

                                    PacketHandler.INSTANCE.sendTo(new PacketFrontierDeleted(frontier.getDimension(),
                                            frontier.getId(), frontier.getPersonal(), player.getEntityId()), player);
                                    PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(frontier, player.getEntityId()),
                                            frontier);
                                }

                                return;
                            }
                        } else {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.DeleteFrontier,
                                    playerUser, MapFrontiers.proxy.isOPorHost(player), frontier.getOwner())) {
                                boolean deleted = FrontiersManager.instance.deleteGlobalFrontier(frontier.getDimension(),
                                        frontier.getId());
                                if (deleted) {
                                    PacketHandler.INSTANCE.sendToAll(new PacketFrontierDeleted(frontier.getDimension(),
                                            frontier.getId(), frontier.getPersonal(), player.getEntityId()));
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
