package games.alejandrocoria.mapfrontiers.common.network;

import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

@ParametersAreNonnullByDefault
public class PacketNewFrontier {
    private RegistryKey<World> dimension = World.OVERWORLD;
    private boolean personal = false;
    private BlockPos vertex;

    public PacketNewFrontier() {
    }

    public PacketNewFrontier(RegistryKey<World> dimension, boolean personal, @Nullable BlockPos vertex) {
        this.dimension = dimension;
        this.personal = personal;
        this.vertex = vertex;
    }

    public static PacketNewFrontier fromBytes(PacketBuffer buf) {
        PacketNewFrontier packet = new PacketNewFrontier();
        packet.dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        packet.personal = buf.readBoolean();

        boolean hasVertex = buf.readBoolean();
        if (hasVertex) {
            packet.vertex = BlockPos.of(buf.readLong());
        }

        return packet;
    }

    public static void toBytes(PacketNewFrontier packet, PacketBuffer buf) {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeBoolean(packet.personal);

        buf.writeBoolean(packet.vertex != null);
        if (packet.vertex != null) {
            buf.writeLong(packet.vertex.asLong());
        }
    }

    public static void handle(PacketNewFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) {
                return;
            }

            FrontierData frontier;

            if (message.personal) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = MapFrontiers.getFrontiersManager().createNewPersonalFrontier(message.dimension, player,
                            message.vertex);
                    PacketHandler.sendToUsersWithAccess(new PacketFrontier(frontier, player.getId()), frontier);

                    return;
                }
            } else {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.CreateFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = MapFrontiers.getFrontiersManager().createNewGlobalFrontier(message.dimension, player,
                            message.vertex);
                    PacketHandler.sendToAll(new PacketFrontier(frontier, player.getId()));

                    return;
                }
            }
            PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
        });
        context.setPacketHandled(true);
    }
}
