package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
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
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSettingsProfile> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketSettingsProfile::encode, PacketSettingsProfile::new);

    private final SettingsProfile profile;

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketSettingsProfile(FriendlyByteBuf buf) {
        this.profile = new SettingsProfile();
        if (buf.readableBytes() > 1) {
            this.profile.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        profile.toBytes(buf);
    }

    public static void handle(PacketContext<PacketSettingsProfile> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            MapFrontiersClient.receiveSettingsProfile(ctx.message().profile);
        }
    }
}
