package games.alejandrocoria.mapfrontiers.common.network;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontierShared {
    private int shareMessageID;
    private final SettingsUser playerSharing;
    private final SettingsUser owner;
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

    public static PacketPersonalFrontierShared fromBytes(PacketBuffer buf) {
        PacketPersonalFrontierShared packet = new PacketPersonalFrontierShared();
        packet.shareMessageID = buf.readInt();
        packet.playerSharing.fromBytes(buf);
        packet.owner.fromBytes(buf);
        packet.name1 = buf.readUtf(17);
        packet.name2 = buf.readUtf(17);
        return packet;
    }

    public static void toBytes(PacketPersonalFrontierShared packet, PacketBuffer buf) {
        buf.writeInt(packet.shareMessageID);
        packet.playerSharing.toBytes(buf);
        packet.owner.toBytes(buf);
        buf.writeUtf(packet.name1);
        buf.writeUtf(packet.name2);
    }

    public static void handle(PacketPersonalFrontierShared message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketPersonalFrontierShared message, NetworkEvent.Context ctx) {
        String frontierName;
        if (message.name1.isEmpty() && message.name2.isEmpty()) {
            frontierName = "Unnamed Frontier";
        } else if (message.name1.isEmpty()) {
            frontierName = message.name2;
        } else if (message.name2.isEmpty()) {
            frontierName = message.name1;
        } else {
            frontierName = message.name1 + " " + message.name2;
        }

        StringTextComponent button = new StringTextComponent(frontierName);
        button.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new StringTextComponent("Click to accept or use command /mfaccept " + message.shareMessageID))));
        button.withStyle(style -> style.withBold(true));
        button.withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mapfrontiersaccept " + message.shareMessageID)));

        StringTextComponent text = new StringTextComponent(userToString(message.playerSharing) + " ");
        if (message.playerSharing.equals(message.owner)) {
            text.append("want to share a frontier with you: ");
        } else {
            text.append("want to share a frontier from " + userToString(message.owner) + " with you: ");
        }

        text.append(button);

        ClientPlayerEntity player = Minecraft.getInstance().player;
        player.sendMessage(text, message.owner.uuid);

        ctx.setPacketHandled(true);
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
