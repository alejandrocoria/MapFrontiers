package games.alejandrocoria.mapfrontiers.common.network;

import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;

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

    public static void handle(PacketFrontierSettings message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getNetworkManager().getDirection() == PacketFlow.CLIENTBOUND) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(message, ctx.get()));
        } else {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.UpdateSettings,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    FrontiersManager.instance.setSettings(message.settings);

                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(p)), p);
                    }
                } else {
                    PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)),
                            player);
                }
            });
            context.setPacketHandled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PacketFrontierSettings message, NetworkEvent.Context ctx) {
        if (Minecraft.getInstance().screen instanceof GuiFrontierSettings) {
            ((GuiFrontierSettings) Minecraft.getInstance().screen).setFrontierSettings(message.settings);
        }
        ctx.setPacketHandled(true);
    }
}
