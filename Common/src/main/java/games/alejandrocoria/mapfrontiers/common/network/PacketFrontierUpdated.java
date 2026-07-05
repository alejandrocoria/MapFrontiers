package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketFrontierUpdated {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_frontier_updated");

    private UUID frontierId = new UUID(0, 0);
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private boolean personal;
    private FrontierChange change = new FrontierChange();
    private long authoritativeSyncHash;
    private int playerID = -1;

    public PacketFrontierUpdated(UUID frontierId, ResourceKey<Level> dimension, boolean personal, FrontierChange change, long authoritativeSyncHash,
                                 int playerID) {
        this.frontierId = frontierId;
        this.dimension = dimension;
        this.personal = personal;
        this.change = change;
        this.authoritativeSyncHash = authoritativeSyncHash;
        this.playerID = playerID;
    }

    public PacketFrontierUpdated(FriendlyByteBuf buf) {
        if (buf.readableBytes() > 1) {
            this.frontierId = buf.readUUID();
            this.dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
            this.personal = buf.readBoolean();
            this.change = new FrontierChange(buf);
            this.authoritativeSyncHash = buf.readLong();
            this.playerID = buf.readInt();
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(frontierId);
        buf.writeResourceLocation(dimension.location());
        buf.writeBoolean(personal);
        change.toBytes(buf);
        buf.writeLong(authoritativeSyncHash);
        buf.writeInt(playerID);
    }

    public static void handle(PacketContext<PacketFrontierUpdated> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            if (!MapFrontiersClient.isJourneyMapPluginAvailable()) {
                return;
            }
            PacketFrontierUpdated message = ctx.message();
            MapFrontiersClient.getOperationService().applyFrontierUpdated(message.dimension, message.frontierId, message.personal, message.change,
                    message.authoritativeSyncHash, message.playerID);
        }
    }
}
