package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings implements IMessage {
    private FrontierSettings settings;

    public PacketFrontierSettings() {
        settings = new FrontierSettings();
    }

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        settings.readFromNBT(ByteBufUtils.readTag(buf));
        settings.setChangeCounter(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        settings.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(settings.getChangeCounter());
    }

    public static class Handler implements IMessageHandler<PacketFrontierSettings, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontierSettings message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (Minecraft.getMinecraft().currentScreen instanceof GuiFrontierSettings) {
                        ((GuiFrontierSettings) Minecraft.getMinecraft().currentScreen).setFrontierSettings(message.settings);
                    }
                });
            } else {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                    EntityPlayerMP player = ctx.getServerHandler().player;
                    if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateSettings,
                            new SettingsUser(player), MapFrontiers.proxy.isOPorHost(player), null)) {
                        FrontiersManager.instance.setSettings(message.settings);

                        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                            PacketHandler.INSTANCE
                                    .sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(p)), p);
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
