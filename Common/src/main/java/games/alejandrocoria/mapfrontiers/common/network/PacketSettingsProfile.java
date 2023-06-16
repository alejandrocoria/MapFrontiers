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
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_settings_profile");

    private final SettingsProfile profile;

    public PacketSettingsProfile() {
        profile = new SettingsProfile();
    }

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static PacketSettingsProfile decode(FriendlyByteBuf buf) {
        PacketSettingsProfile packet = new PacketSettingsProfile();
        packet.profile.fromBytes(buf);
        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        profile.toBytes(buf);
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
