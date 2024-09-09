package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.screen.ModSettings;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_settings");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierSettings> STREAM_CODEC = StreamCodec.ofMember(PacketFrontierSettings::encode, PacketFrontierSettings::new);

    private final FrontierSettings settings;

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketFrontierSettings(FriendlyByteBuf buf) {
        this.settings = new FrontierSettings();

        try {
            if (buf.readableBytes() > 1) {
                this.settings.fromBytes(buf);
                this.settings.setChangeCounter(buf.readInt());
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketFrontierSettings: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            settings.toBytes(buf);
            buf.writeInt(settings.getChangeCounter());
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketFrontierSettings: %s", t));
        }
    }

    public static void handle(PacketContext<PacketFrontierSettings> ctx) {
        PacketFrontierSettings message = ctx.message();
        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateSettings,
                    new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                FrontiersManager.instance.setSettings(message.settings);

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(p)), p);
                }
            } else {
                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        } else if (Side.CLIENT.equals(ctx.side())) {
            if (Minecraft.getInstance().screen instanceof ModSettings) {
                ((ModSettings) Minecraft.getInstance().screen).setFrontierSettings(message.settings);
            }
        }
    }
}
