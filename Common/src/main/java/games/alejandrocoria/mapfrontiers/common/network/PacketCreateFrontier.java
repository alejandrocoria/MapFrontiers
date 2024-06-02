package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.FrontiersManager;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_create_frontier");

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

    public static PacketCreateFrontier decode(FriendlyByteBuf buf) {
        PacketCreateFrontier packet = new PacketCreateFrontier();

        try {
            if (buf.readableBytes() > 1) {
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
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketCreateFrontier: %s", t));
        }


        return packet;
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeResourceLocation(dimension.location());
            buf.writeBoolean(personal);

            buf.writeBoolean(vertices != null);
            if (vertices != null) {
                buf.writeInt(vertices.size());
                for (BlockPos pos : vertices) {
                    buf.writeLong(pos.asLong());
                }
            }

            buf.writeBoolean(chunks != null);
            if (chunks != null) {
                buf.writeInt(chunks.size());
                for (ChunkPos pos : chunks) {
                    buf.writeLong(pos.toLong());
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to write message for PacketCreateFrontier: %s", t));
        }
    }

    public static void handle(PacketContext<PacketCreateFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketCreateFrontier message = ctx.message();
            ServerPlayer player = (ServerPlayer) ctx.sender();
            if (player == null) {
                return;
            }
            MinecraftServer server = player.server;
            FrontierData frontier;

            if (message.personal) {
                frontier = FrontiersManager.instance.createNewPersonalFrontier(message.dimension, player, message.vertices, message.chunks);
                PacketHandler.sendToUsersWithAccess(new PacketFrontierCreated(frontier, player.getId()), frontier, server);

                return;
            } else {
                if (FrontiersManager.instance.getSettings().checkAction(FrontierSettings.Action.CreateGlobalFrontier,
                        new SettingsUser(player), MapFrontiers.isOPorHost(player), null)) {
                    frontier = FrontiersManager.instance.createNewGlobalFrontier(message.dimension, player, message.vertices, message.chunks);
                    PacketHandler.sendToAll(new PacketFrontierCreated(frontier, player.getId()), server);

                    return;
                }
            }
            PacketHandler.sendTo(new PacketSettingsProfile(FrontiersManager.instance.getSettings().getProfile(player)), player);
        }
    }
}
