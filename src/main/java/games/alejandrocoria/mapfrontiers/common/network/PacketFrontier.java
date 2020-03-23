package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.common.FrontierData;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PacketFrontier implements IMessage {
    private FrontierData frontier;

    public PacketFrontier() {
        frontier = new FrontierData();
    }

    public PacketFrontier(FrontierData frontier) {
        this.frontier = frontier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        frontier.readFromNBT(ByteBufUtils.readTag(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();
        frontier.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);
    }

    public static class Handler implements IMessageHandler<PacketFrontier, IMessage> {
        @Override
        public IMessage onMessage(PacketFrontier message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                // @Incomplete
            }

            return null;
        }
    }
}
