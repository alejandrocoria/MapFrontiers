package games.alejandrocoria.mapfrontiers.common.network;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.client.gui.GuiShareSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

@ParametersAreNonnullByDefault
public class PacketSettingsProfile {
    private final SettingsProfile profile;

    public PacketSettingsProfile() {
        profile = new SettingsProfile();
    }

    public PacketSettingsProfile(SettingsProfile profile) {
        this.profile = profile;
    }

    public static PacketSettingsProfile fromBytes(PacketBuffer buf) {
        PacketSettingsProfile packet = new PacketSettingsProfile();
        packet.profile.fromBytes(buf);
        return packet;
    }

    public static void toBytes(PacketSettingsProfile packet, PacketBuffer buf) {
        packet.profile.toBytes(buf);
    }

    public static void handle(PacketSettingsProfile message, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketSettingsProfile message, NetworkEvent.Context ctx) {
        SettingsProfile currentProfile = ClientProxy.getSettingsProfile();
        if (currentProfile == null || !currentProfile.equals(message.profile)) {
            ClientProxy.setSettingsProfile(message.profile);

            if (Minecraft.getInstance().screen instanceof GuiFrontierSettings) {
                ((GuiFrontierSettings) Minecraft.getInstance().screen).updateSettingsProfile(message.profile);
            } else if (Minecraft.getInstance().screen instanceof GuiFrontierBook) {
                ((GuiFrontierBook) Minecraft.getInstance().screen).reloadPage(false);
            } else if (Minecraft.getInstance().screen instanceof GuiShareSettings) {
                ((GuiShareSettings) Minecraft.getInstance().screen).reloadPage(false);
            }
        }
        ctx.setPacketHandled(true);
    }
}
