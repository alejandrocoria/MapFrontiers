package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketPersonalFrontierShared implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_personal_frontier_shared");
    public static final CustomPacketPayload.Type<PacketPersonalFrontierShared> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPersonalFrontierShared> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketPersonalFrontierShared::encode, PacketPersonalFrontierShared::new);

    private int shareMessageID;
    private final PlayerId playerSharing;
    private final PlayerId owner;
    private String name1;
    private String name2;

    public PacketPersonalFrontierShared(int shareMessageID, PlayerId playerSharing, PlayerId owner, String name1,
            String name2) {
        this.shareMessageID = shareMessageID;
        this.playerSharing = playerSharing;
        this.owner = owner;
        this.name1 = name1;
        this.name2 = name2;
    }

    @Override
    public CustomPacketPayload.Type<PacketPersonalFrontierShared> type() {
        return TYPE;
    }

    public PacketPersonalFrontierShared(FriendlyByteBuf buf) {
        this.shareMessageID = buf.readInt();
        this.playerSharing = PlayerIdNetworkCodec.read(buf);
        this.owner = PlayerIdNetworkCodec.read(buf);
        this.name1 = buf.readUtf(FrontierData.MAX_NAME_CHARACTERS);
        this.name2 = buf.readUtf(FrontierData.MAX_NAME_CHARACTERS);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(shareMessageID);
        PlayerIdNetworkCodec.write(buf, playerSharing);
        PlayerIdNetworkCodec.write(buf, owner);
        buf.writeUtf(name1, FrontierData.MAX_NAME_CHARACTERS);
        buf.writeUtf(name2, FrontierData.MAX_NAME_CHARACTERS);
    }

    public static void handle(PacketContext<PacketPersonalFrontierShared> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketPersonalFrontierShared message = ctx.message();
            ClientPacketDelivery.submit(() -> applyClient(message));
        }
    }

    private static void applyClient(PacketPersonalFrontierShared message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

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
        button.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept or use command /mfaccept " + message.shareMessageID))));
        button.withStyle(style -> style.withBold(true));
        button.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/mapfrontiersaccept " + message.shareMessageID)));

        MutableComponent text = Component.literal(SettingsUserFormatter.getDisplayName(message.playerSharing, "User not found") + " ");
        if (message.playerSharing.equals(message.owner)) {
            text.append("want to share a frontier with you: ");
        } else {
            text.append("want to share a frontier of " + SettingsUserFormatter.getDisplayName(message.owner, "User not found") + " with you: ");
        }

        text.append(button);
        player.sendSystemMessage(text);
    }
}
