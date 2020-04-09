package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierSettings implements IMessage {
    private int changeNonce;

    public PacketRequestFrontierSettings() {
        changeNonce = 0;
    }

    public PacketRequestFrontierSettings(int changeNonce) {
        this.changeNonce = changeNonce;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        changeNonce = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(changeNonce);
    }

    public static class Handler implements IMessageHandler<PacketRequestFrontierSettings, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestFrontierSettings message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    FrontierSettings settings = FrontiersManager.instance.getSettings();

                    if (settings.checkAction(FrontierSettings.Action.UpdateSettings, new SettingsUser(player),
                            MapFrontiers.proxy.isOPorHost(player), null) && settings.getChangeNonce() > message.changeNonce) {
                        PacketHandler.INSTANCE.sendToAll(new PacketFrontierSettings(settings));
                    }
                });
            }

            return null;
        }
    }
}
