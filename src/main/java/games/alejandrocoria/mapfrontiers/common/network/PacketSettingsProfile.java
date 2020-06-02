package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketSettingsProfile implements IMessage {
    private SettingsProfile profile;

    public PacketSettingsProfile() {
        profile = new SettingsProfile();
    }

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        profile.readFromNBT(ByteBufUtils.readTag(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        profile.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
    }

    public static class Handler implements IMessageHandler<PacketSettingsProfile, IMessage> {
        @Override
        public IMessage onMessage(PacketSettingsProfile message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    SettingsProfile currentProfile = ((ClientProxy) MapFrontiers.proxy).getSettingsProfile();
                    if (currentProfile == null || !currentProfile.equals(message.profile)) {
                        ((ClientProxy) MapFrontiers.proxy).setSettingsProfile(message.profile);

                        if (Minecraft.getMinecraft().currentScreen instanceof GuiFrontierSettings) {
                            ((GuiFrontierSettings) Minecraft.getMinecraft().currentScreen).updateSettingsProfile(message.profile);
                        } else if (Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
                            ((GuiFrontierBook) Minecraft.getMinecraft().currentScreen).reloadPage(false);
                        }
                    }
                });
            }

            return null;
        }
    }
}
