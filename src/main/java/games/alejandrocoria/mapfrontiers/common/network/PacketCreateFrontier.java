package games.alejandrocoria.mapfrontiers.common.network;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketCreateFrontier {
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean personal = false;
    private List<BlockPos> vertices;
    private List<ChunkPos> chunks;

    public PacketCreateFrontier() {
    }

    public PacketCreateFrontier(ResourceKey<Level> dimension, boolean personal, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        this.dimension = dimension;
        this.personal = personal;
        this.vertices = vertices;
        this.chunks = chunks;
    }

    public static PacketCreateFrontier fromBytes(FriendlyByteBuf buf) {
        PacketCreateFrontier packet = new PacketCreateFrontier();
        packet.dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
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

    public static void toBytes(PacketCreateFrontier packet, FriendlyByteBuf buf) {
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

    public static void handle(PacketCreateFrontier message, MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            FrontierData frontier;

            if (message.personal) {
                frontier = FrontiersManager.instance.createNewPersonalFrontier(message.dimension, player, message.vertices, message.chunks);
                PacketHandler.sendToUsersWithAccess(PacketFrontierCreated.class, new PacketFrontierCreated(frontier, player.getId()), frontier, server);

                return;
            } else {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = FrontiersManager.instance.createNewGlobalFrontier(message.dimension, player, message.vertices, message.chunks);
                    PacketHandler.sendToAll(PacketFrontierCreated.class, new PacketFrontierCreated(frontier, player.getId()), server);

                    return;
                }
            }
            PacketHandler.sendTo(PacketSettingsProfile.class, new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
        });
    }
}
