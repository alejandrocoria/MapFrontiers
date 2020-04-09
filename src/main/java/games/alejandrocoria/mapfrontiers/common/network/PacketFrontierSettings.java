package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
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
        settings.setChangeNonce(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        settings.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
        buf.writeInt(settings.getChangeNonce());
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
            }

            return null;
        }
    }
}
