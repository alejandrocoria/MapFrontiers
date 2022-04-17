package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

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

    public static void handle(PacketSettingsProfile message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSettingsProfile message, NetworkEvent.Context ctx) {
        SettingsProfile currentProfile = ClientProxy.getSettingsProfile();
        if (currentProfile == null || !currentProfile.equals(message.profile)) {
            MinecraftForge.EVENT_BUS.post(new UpdatedSettingsProfileEvent(message.profile));
        }

        ctx.setPacketHandled(true);
    }
}
