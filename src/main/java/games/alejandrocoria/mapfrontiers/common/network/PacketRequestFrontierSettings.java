package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

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

    public static void handle(PacketRequestFrontierSettings message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            FrontierSettings settings = FrontiersManager.instance.getSettings();

            if (settings.checkAction(FrontierSettings.Action.UpdateSettings, new SettingsUser(player),
                    MapFrontiers.isOPorHost(player), null) && settings.getChangeCounter() > message.changeCounter) {
                PacketHandler.sendTo(new PacketFrontierSettings(settings), player);
            } else {
                PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)),
                        player);
            }
        });
        context.setPacketHandled(true);
    }
}
