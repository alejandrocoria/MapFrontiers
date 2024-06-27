package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketSettingsProfile {
    public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "packet_settings_profile");

    private final SettingsProfile profile;

    public PacketSettingsProfile() {
        profile = new SettingsProfile();
    }

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static PacketSettingsProfile decode(FriendlyByteBuf buf) {
        PacketSettingsProfile packet = new PacketSettingsProfile();

        try {
            if (buf.readableBytes() > 1) {
                packet.profile.fromBytes(buf);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketSettingsProfile: %s", t));
        }

        return packet;
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
