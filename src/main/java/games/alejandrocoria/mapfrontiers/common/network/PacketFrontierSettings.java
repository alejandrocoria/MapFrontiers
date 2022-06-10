package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketFrontierSettings {
    private final FrontierSettings settings;

    public PacketFrontierSettings() {
        settings = new FrontierSettings();
    }

    public PacketFrontierSettings(FrontierSettings settings) {
        this.settings = settings;
    }

    public static PacketFrontierSettings fromBytes(FriendlyByteBuf buf) {
        PacketFrontierSettings packet = new PacketFrontierSettings();
        packet.settings.fromBytes(buf);
        packet.settings.setChangeCounter(buf.readInt());
        return packet;
    }

    public static void toBytes(PacketFrontierSettings packet, FriendlyByteBuf buf) {
        packet.settings.toBytes(buf);
        buf.writeInt(packet.settings.getChangeCounter());
    }

    public static void handle(PacketFrontierSettings message, MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateSettings,
                    new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                FrontiersManager.instance.setSettings(message.settings);

                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(p)), p);
                }
            } else {
                PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public static void handle(PacketFrontierSettings message, Minecraft client) {
        client.execute(() -> {
            if (Minecraft.getInstance().screen instanceof GuiFrontierSettings) {
                ((GuiFrontierSettings) Minecraft.getInstance().screen).setFrontierSettings(message.settings);
            }
        });
    }
}
