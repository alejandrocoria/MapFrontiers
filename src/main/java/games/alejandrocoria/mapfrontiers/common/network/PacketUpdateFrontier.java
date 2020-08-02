package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
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
        frontier.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        frontier.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<PacketUpdateFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateFrontier message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    SettingsUser playerUser = new SettingsUser(player);

                    FrontierData currentFrontier = FrontiersManager.instance.getFrontierFromID(message.frontier.getId());

                    if (currentFrontier != null) {
                        message.frontier.setPersonal(currentFrontier.getPersonal());
                        if (!currentFrontier.getOwner().isEmpty()) {
                            message.frontier.setOwner(currentFrontier.getOwner());
                        }

                        message.frontier.setUsersShared(currentFrontier.getUsersShared());
                        message.frontier.removeChange(FrontierData.Change.Shared);

                        if (message.frontier.getPersonal()) {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                                    playerUser, MapFrontiers.proxy.isOPorHost(player), message.frontier.getOwner())) {
                                if (currentFrontier.checkActionUserShared(playerUser, SettingsUserShared.Action.UpdateFrontier)) {
                                    boolean updated = FrontiersManager.instance
                                            .updatePersonalFrontier(message.frontier.getOwner(), message.frontier);
                                    if (updated) {
                                        if (message.frontier.getUsersShared() != null) {
                                            for (SettingsUserShared userShared : message.frontier.getUsersShared()) {
                                                FrontiersManager.instance.updatePersonalFrontier(userShared.getUser(),
                                                        message.frontier);
                                            }
                                        }
                                        PacketHandler.sendToUsersWithAccess(new PacketFrontierUpdated(message.frontier,
                                                ctx.getServerHandler().player.getEntityId()), message.frontier);
                                    }
                                }

                                return;
                            }
                        } else {
                            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateFrontier,
                                    playerUser, MapFrontiers.proxy.isOPorHost(player), message.frontier.getOwner())) {
                                boolean updated = FrontiersManager.instance.updateGlobalFrontier(message.frontier);
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
