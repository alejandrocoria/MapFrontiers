package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class PacketTerritoriesSnapshot {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_territories_snapshot");

    private final List<FrontierData> globalFrontiers;
    private final List<FrontierData> personalFrontiers;
    private final List<CollectionData> globalCollections;
    private final List<CollectionData> personalCollections;

    public PacketTerritoriesSnapshot() {
        globalFrontiers = new ArrayList<>();
        personalFrontiers = new ArrayList<>();
        globalCollections = new ArrayList<>();
        personalCollections = new ArrayList<>();
    }

    public void addGlobalFrontier(FrontierData frontier) {
        globalFrontiers.add(frontier);
    }

    public void addPersonalFrontier(FrontierData frontier) {
        personalFrontiers.add(frontier);
    }

    public void addGlobalCollection(CollectionData collection) {
        globalCollections.add(collection);
    }

    public void addPersonalCollection(CollectionData collection) {
        personalCollections.add(collection);
    }

    public void addGlobalFrontiers(List<FrontierData> frontiers) {
        globalFrontiers.addAll(frontiers);
    }

    public void addPersonalFrontiers(List<FrontierData> frontiers) {
        personalFrontiers.addAll(frontiers);
    }

    public void addGlobalCollections(List<CollectionData> collections) {
        globalCollections.addAll(collections);
    }

    public PacketTerritoriesSnapshot(FriendlyByteBuf buf) {
        globalFrontiers = new ArrayList<>();
        personalFrontiers = new ArrayList<>();
        globalCollections = new ArrayList<>();
        personalCollections = new ArrayList<>();
        if (buf.readableBytes() > 1) {
            int size = buf.readInt();
            for (int i = 0; i < size; ++i) {
                FrontierData frontier = new FrontierData();
                frontier.fromBytes(buf);
                this.addGlobalFrontier(frontier);
            }

            size = buf.readInt();
            for (int i = 0; i < size; ++i) {
                FrontierData frontier = new FrontierData();
                frontier.fromBytes(buf);
                this.addPersonalFrontier(frontier);
            }

            size = buf.readInt();
            for (int i = 0; i < size; ++i) {
                CollectionData collection = new CollectionData();
                collection.fromBytes(buf);
                this.addGlobalCollection(collection);
            }

            size = buf.readInt();
            for (int i = 0; i < size; ++i) {
                CollectionData collection = new CollectionData();
                collection.fromBytes(buf);
                this.addPersonalCollection(collection);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(globalFrontiers.size());
        for (FrontierData frontier : globalFrontiers) {
            frontier.toBytes(buf);
        }

        buf.writeInt(personalFrontiers.size());
        for (FrontierData frontier : personalFrontiers) {
            frontier.toBytes(buf);
        }

        buf.writeInt(globalCollections.size());
        for (CollectionData collection : globalCollections) {
            collection.toBytes(buf);
        }

        buf.writeInt(personalCollections.size());
        for (CollectionData collection : personalCollections) {
            collection.toBytes(buf);
        }
    }

    public static void handle(PacketContext<PacketTerritoriesSnapshot> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            PacketTerritoriesSnapshot message = ctx.message();
            MapFrontiers.LOGGER.debug("Handling PacketTerritoriesSnapshot. globalFrontiers={}, personalFrontiers={}, globalCollections={}, personalCollections={}",
                    message.globalFrontiers.size(), message.personalFrontiers.size(),
                    message.globalCollections.size(), message.personalCollections.size());
            MapFrontiersClient.applyTerritoriesSnapshot(message.globalFrontiers, message.personalFrontiers,
                    message.globalCollections, message.personalCollections);
        }
    }
}
