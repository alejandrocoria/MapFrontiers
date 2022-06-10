package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketSettingsProfile {
    private final SettingsProfile profile;

    public PacketSettingsProfile() {
        profile = new SettingsProfile();
    }

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static PacketSettingsProfile fromBytes(FriendlyByteBuf buf) {
        PacketSettingsProfile packet = new PacketSettingsProfile();
        packet.profile.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketSettingsProfile packet, FriendlyByteBuf buf) {
        packet.profile.toBytes(buf);
    }

    @Environment(EnvType.CLIENT)
    public static void handle(PacketSettingsProfile message, Minecraft client) {
        client.execute(() -> {
            SettingsProfile currentProfile = ClientProxy.getSettingsProfile();
            if (currentProfile == null || !currentProfile.equals(message.profile)) {
                ClientProxy.postUpdatedSettingsProfileEvent(message.profile);
            }
        });
    }
}
