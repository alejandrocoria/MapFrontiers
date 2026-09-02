package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketFrontierDeleted implements CustomPacketPayload {
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_frontier_deleted");
    public static final CustomPacketPayload.Type<PacketFrontierDeleted> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFrontierDeleted> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketFrontierDeleted::encode, PacketFrontierDeleted::new);

    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private UUID frontierID;
    private boolean personal;
    private int playerID = -1;

    public PacketFrontierDeleted(ResourceKey<Level> dimension, UUID frontierID, boolean personal, int playerID) {
        this.dimension = dimension;
        this.frontierID = frontierID;
        this.personal = personal;
        this.playerID = playerID;
    }

    @Override
    public CustomPacketPayload.Type<PacketFrontierDeleted> type() {
        return TYPE;
    }

    public PacketFrontierDeleted(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readIdentifier());
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.personal = buf.readBoolean();
            this.playerID = buf.readInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeIdentifier(dimension.identifier());
        UUIDHelper.toBytes(buf, frontierID);
        buf.writeBoolean(personal);
        buf.writeInt(playerID);
    }

    public static void handle(PacketContext<PacketFrontierDeleted> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierDeleted message = ctx.message();
            ClientPacketDelivery.submit(() -> MapFrontiersClient.getOperationService()
                    .applyFrontierDeleted(message.frontierID, message.personal));
        }
    }
}
