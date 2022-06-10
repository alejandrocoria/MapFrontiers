package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PacketRequestFrontierSettings {
    private final int changeCounter;

    public PacketRequestFrontierSettings() {
        changeCounter = 0;
    }

    public PacketRequestFrontierSettings(int changeNonce) {
        this.changeCounter = changeNonce;
    }

    public static PacketRequestFrontierSettings fromBytes(FriendlyByteBuf buf) {
        return new PacketRequestFrontierSettings(buf.readInt());
    }

    public static void toBytes(PacketRequestFrontierSettings packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.changeCounter);
    }

    public static void handle(PacketRequestFrontierSettings message, MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            FrontierSettings settings = FrontiersManager.instance.getSettings();

            if (settings.checkAction(FrontierSettings.Action.UpdateSettings, new SettingsUser(player),
                    MapFrontiers.isOPorHost(player), null) && settings.getChangeCounter() > message.changeCounter) {
                PacketHandler.sendTo(PacketFrontierSettings.class, new PacketFrontierSettings(settings), player);
            } else {
                PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
            }
        });
    }
}
