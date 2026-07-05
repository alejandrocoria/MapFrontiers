package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.UUIDHelper;
import games.alejandrocoria.mapfrontiers.server.territory.ServerTerritoryOperationResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class PacketSharePersonalFrontier {
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_share_personal_frontier");

    private UUID frontierID;
    private final SettingsUserShared userShared;

    public PacketSharePersonalFrontier() {
        userShared = new SettingsUserShared();
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUser user) {
        this(frontierID, createSharedUser(user));
    }

    public PacketSharePersonalFrontier(UUID frontierID, SettingsUserShared userShared) {
        this.frontierID = frontierID;
        this.userShared = userShared;
    }

    public PacketSharePersonalFrontier(FriendlyByteBuf buf) {
        this.userShared = new SettingsUserShared();
        if (buf.readableBytes() > 1) {
            this.frontierID = UUIDHelper.fromBytes(buf);
            this.userShared.fromBytes(buf);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        UUIDHelper.toBytes(buf, frontierID);
        userShared.toBytes(buf);
    }

    public static void handle(PacketContext<PacketSharePersonalFrontier> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            PacketSharePersonalFrontier message = ctx.message();
            ServerPlayer player = ctx.sender();
            if (player == null) {
                return;
            }
            if (MapFrontiers.getServerRuntime() == null) {
                return;
            }

            ServerTerritoryOperationResult result = MapFrontiers.getServerRuntime().getShareService()
                    .sharePersonalFrontier(player, message.frontierID, message.userShared);
            result.dispatchNetworkActions();
        }
    }

    private static SettingsUserShared createSharedUser(SettingsUser user) {
        SettingsUserShared sharedUser = new SettingsUserShared(user, false);
        sharedUser.setActions(EnumSet.noneOf(SettingsUserShared.Action.class));
        return sharedUser;
    }
}
