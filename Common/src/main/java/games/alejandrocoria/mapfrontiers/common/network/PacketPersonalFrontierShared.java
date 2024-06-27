package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontierShared {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_frontier_shared");

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

    public static PacketPersonalFrontierShared decode(FriendlyByteBuf buf) {
        PacketPersonalFrontierShared packet = new PacketPersonalFrontierShared();

        try {
            if (buf.readableBytes() > 1) {
                packet.shareMessageID = buf.readInt();
                packet.playerSharing.fromBytes(buf);
                packet.owner.fromBytes(buf);
                packet.name1 = buf.readUtf(17);
                packet.name2 = buf.readUtf(17);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketPersonalFrontierShared: %s", t));
        }

        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeInt(shareMessageID);
            playerSharing.toBytes(buf);
            owner.toBytes(buf);
            buf.writeUtf(name1);
            buf.writeUtf(name2);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketPersonalFrontierShared: %s", t));
        }
    }

    public static void handle(PacketContext<PacketPersonalFrontierShared> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            PacketPersonalFrontierShared message = ctx.message();
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

            MutableComponent button = Component.literal(frontierName);
            button.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to accept or use command /mfaccept " + message.shareMessageID))));
            button.withStyle(style -> style.withBold(true));
            button.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mapfrontiersaccept " + message.shareMessageID)));

            MutableComponent text = Component.literal(userToString(message.playerSharing) + " ");
            if (message.playerSharing.equals(message.owner)) {
                text.append("want to share a frontier with you: ");
            } else {
                text.append("want to share a frontier from " + userToString(message.owner) + " with you: ");
            }

            text.append(button);
            player.sendSystemMessage(text);
        }
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
