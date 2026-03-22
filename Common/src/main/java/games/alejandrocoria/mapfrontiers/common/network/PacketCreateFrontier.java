package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.frontier.ServerFrontierOperationResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketCreateFrontier {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_create_frontier");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateFrontier> STREAM_CODEC = StreamCodec.ofMember(PacketCreateFrontier::encode, PacketCreateFrontier::new);

    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean personal = false;
    private UUID frontierId = new UUID(0L, 0L);
    private @Nullable String sourcePluginId;
    private List<BlockPos> vertices;
    private List<ChunkPos> chunks;

    public PacketCreateFrontier(UUID frontierId, ResourceKey<Level> dimension, boolean personal, @Nullable List<BlockPos> vertices, @Nullable List<ChunkPos> chunks) {
        this(frontierId, dimension, personal, null, vertices, chunks);
    }

    public PacketCreateFrontier(UUID frontierId,
                                ResourceKey<Level> dimension,
                                boolean personal,
                                @Nullable String sourcePluginId,
                                @Nullable List<BlockPos> vertices,
                                @Nullable List<ChunkPos> chunks) {
        this.frontierId = frontierId;
        this.dimension = dimension;
        this.personal = personal;
        this.sourcePluginId = sourcePluginId;
        this.vertices = vertices;
        this.chunks = chunks;
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }

    public PacketCreateFrontier(FriendlyByteBuf buf) {
        try {
            if (buf.readableBytes() > 1) {
                this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
                this.personal = buf.readBoolean();
                this.frontierId = UUIDHelper.fromBytes(buf);
                this.sourcePluginId = buf.readNullable(FriendlyByteBuf::readUtf);

                boolean hasVertex = buf.readBoolean();
                if (hasVertex) {
                    this.vertices = new ArrayList<>();
                    int vertexCount = buf.readInt();
                    for (int i = 0; i < vertexCount; ++i) {
                        BlockPos vertex = BlockPos.of(buf.readLong());
                        this.vertices.add(vertex);
                    }
                }

                boolean hasChunks = buf.readBoolean();
                if (hasChunks) {
                    this.chunks = new ArrayList<>();
                    int chunksCount = buf.readInt();
                    for (int i = 0; i < chunksCount; ++i) {
                        ChunkPos chunk = new ChunkPos(buf.readLong());
                        this.chunks.add(chunk);
                    }
                }
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(String.format("Failed to read message for PacketCreateFrontier: %s", t));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        try {
            buf.writeIdentifier(dimension.identifier());
            buf.writeBoolean(personal);
            UUIDHelper.toBytes(buf, frontierId);
            buf.writeNullable(sourcePluginId, FriendlyByteBuf::writeUtf);

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
            ServerPlayer player = ctx.sender();
            if (player == null) {
                MapFrontiers.LOGGER.warn("Ignoring PacketCreateFrontier because sender is null.");
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            MapFrontiers.LOGGER.debug(
                    "Handling PacketCreateFrontier from player={} frontierId={} personal={} sourcePluginId={}",
                    player.getGameProfile().name(), message.frontierId, message.personal, message.sourcePluginId
            );

            ServerFrontierOperationResult result = MapFrontiers.getServerRuntime().getOperationService().createFrontier(player, message.frontierId,
                    message.dimension, message.personal, message.sourcePluginId, message.vertices, message.chunks);
            if (!result.isSuccess()) {
                MapFrontiers.LOGGER.warn(
                        "Rejected PacketCreateFrontier from player={} frontierId={} personal={} sourcePluginId={}",
                        player.getGameProfile().name(), message.frontierId, message.personal, message.sourcePluginId
                );
            }
            result.dispatchNetworkActions();
        }
    }
}
