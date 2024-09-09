package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketSettingsProfile {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_settings_profile");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSettingsProfile> STREAM_CODEC = StreamCodec.ofMember(PacketSettingsProfile::encode, PacketSettingsProfile::new);

    private final SettingsProfile profile;

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketSettingsProfile(FriendlyByteBuf buf) {
        this.profile = new SettingsProfile();

        try {
            if (buf.readableBytes() > 1) {
                this.profile.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketSettingsProfile: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            profile.toBytes(buf);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketSettingsProfile: %s", t));
        }
    }

    public static void handle(PacketContext<PacketSettingsProfile> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketSettingsProfile message = ctx.message();
            SettingsProfile currentProfile = MapFrontiersClient.getSettingsProfile();
            if (currentProfile == null || !currentProfile.equals(message.profile)) {
                ClientEventHandler.postUpdatedSettingsProfileEvent(message.profile);
            }
        }
    }
}
