package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class PacketNewFrontier {
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean personal = false;
    private List<BlockPos> vertices;
    private List<ChunkPos> chunks;

    public PacketNewFrontier() {
    }

    public PacketNewFrontier(ResourceKey<Level> dimension, boolean personal, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        this.dimension = dimension;
        this.personal = personal;
        this.vertices = vertices;
        this.chunks = chunks;
    }

    public static PacketNewFrontier fromBytes(FriendlyByteBuf buf) {
        PacketNewFrontier packet = new PacketNewFrontier();
        packet.dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
        packet.personal = buf.readBoolean();

        boolean hasVertex = buf.readBoolean();
        if (hasVertex) {
            packet.vertices = new ArrayList<>();
            int vertexCount = buf.readInt();
            for (int i = 0; i < vertexCount; ++i) {
                BlockPos vertex = BlockPos.of(buf.readLong());
                packet.vertices.add(vertex);
            }
        }

        boolean hasChunks = buf.readBoolean();
        if (hasChunks) {
            packet.chunks = new ArrayList<>();
            int chunksCount = buf.readInt();
            for (int i = 0; i < chunksCount; ++i) {
                ChunkPos chunk = new ChunkPos(buf.readLong());
                packet.chunks.add(chunk);
            }
        }

        return packet;
    }

    public static void toBytes(PacketNewFrontier packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension.location());
        buf.writeBoolean(packet.personal);

        buf.writeBoolean(packet.vertices != null);
        if (packet.vertices != null) {
            buf.writeInt(packet.vertices.size());
            for (BlockPos pos : packet.vertices) {
                buf.writeLong(pos.asLong());
            }
        }

        buf.writeBoolean(packet.chunks != null);
        if (packet.chunks != null) {
            buf.writeInt(packet.chunks.size());
            for (ChunkPos pos : packet.chunks) {
                buf.writeLong(pos.toLong());
            }
        }
    }

    public static void handle(PacketNewFrontier message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            FrontierData frontier;

            if (message.personal) {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.PersonalFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = MapFrontiers.getFrontiersManager().createNewPersonalFrontier(message.dimension, player, message.vertices, message.chunks);
                    PacketHandler.sendToUsersWithAccess(new PacketFrontier(frontier, player.getId()), frontier);

                    return;
                }
            } else {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.CreateFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = MapFrontiers.getFrontiersManager().createNewGlobalFrontier(message.dimension, player, message.vertices, message.chunks);
                    PacketHandler.sendToAll(new PacketFrontier(frontier, player.getId()));

                    return;
                }
            }
            PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
        });
        context.setPacketHandled(true);
    }
}
