package games.alejandrocoria.mapfrontiers.common.network;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontierShared implements IMessage {
    private int shareMessageID;
    private SettingsUser playerSharing;
    private SettingsUser owner;
    private String name1;
    private String name2;

    public PacketPersonalFrontierShared() {
        shareMessageID = -1;
        playerSharing = new SettingsUser();
        owner = new SettingsUser();
        name1 = "";
        name2 = "";
    }

    public PacketPersonalFrontierShared(int shareMessageID, SettingsUser playerSharing, SettingsUser owner, String name1,
            String name2) {
        this.shareMessageID = shareMessageID;
        this.playerSharing = playerSharing;
        this.owner = owner;
        this.name1 = name1;
        this.name2 = name2;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        shareMessageID = buf.readInt();
        playerSharing.readFromNBT(ByteBufUtils.readTag(buf));
        owner.readFromNBT(ByteBufUtils.readTag(buf));
        name1 = ByteBufUtils.readUTF8String(buf);
        name2 = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(shareMessageID);

        NBTTagCompound nbt = new NBTTagCompound();
        playerSharing.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);

        nbt = new NBTTagCompound();
        owner.writeToNBT(nbt);
        ByteBufUtils.writeTag(buf, nbt);

        ByteBufUtils.writeUTF8String(buf, name1);
        ByteBufUtils.writeUTF8String(buf, name2);
    }

    public static class Handler implements IMessageHandler<PacketPersonalFrontierShared, IMessage> {
        @Override
        public IMessage onMessage(PacketPersonalFrontierShared message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    String frontierName = "";
                    if (message.name1.isEmpty() && message.name2.isEmpty()) {
                        frontierName = "Unnamed Frontier";
                    } else if (message.name1.isEmpty()) {
                        frontierName = message.name2;
                    } else if (message.name2.isEmpty()) {
                        frontierName = message.name1;
                    } else {
                        frontierName = message.name1 + " " + message.name2;
                    }

                    TextComponentString button = new TextComponentString(frontierName);
                    Style style = new Style();
                    style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("Click to accept or use command /mfaccept " + message.shareMessageID)));
                    style.setBold(true);
                    style.setClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mapfrontiersaccept " + message.shareMessageID));
                    button.setStyle(style);

                    TextComponentString text = new TextComponentString(userToString(message.playerSharing) + " ");
                    if (message.playerSharing.equals(message.owner)) {
                        text.appendText("want to share a frontier with you: ");
                    } else {
                        text.appendText("want to share a frontier from " + userToString(message.owner) + " with you: ");
                    }

                    text.appendSibling(button);

                    EntityPlayerSP player = Minecraft.getMinecraft().player;
                    player.sendMessage(text);
                });
            }

            return null;
        }

        private static String userToString(SettingsUser user) {
            String string;
            if (!StringUtils.isBlank(user.username)) {
                string = user.username;
            } else if (user.uuid != null) {
                string = user.uuid.toString();
            } else {
                string = "User not found";
            }

            return string;
        }
    }
}
