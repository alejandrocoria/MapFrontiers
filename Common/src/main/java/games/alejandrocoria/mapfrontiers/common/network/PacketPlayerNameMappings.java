package games.alejandrocoria.mapfrontiers.common.network;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.network.ClientPacketDelivery;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.identity.network.PlayerIdNetworkCodec;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class PacketPlayerNameMappings implements CustomPacketPayload {
    public record Entry(PlayerId playerId, String username) {
        public Entry {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(username, "username");
        }
    }

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "packet_player_name_mappings");
    public static final CustomPacketPayload.Type<PacketPlayerNameMappings> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPlayerNameMappings> STREAM_CODEC = PacketCodecs.guarded(CHANNEL, PacketPlayerNameMappings::encode, PacketPlayerNameMappings::new);

    private final List<Entry> entries;

    public PacketPlayerNameMappings(Iterable<PlayerId> playerIds, PlayerNameResolver resolver) {
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(resolver, "resolver");

        Map<PlayerId, String> mappings = new LinkedHashMap<>();
        for (PlayerId playerId : playerIds) {
            String username = resolver.resolveName(playerId);
            if (isValidUsername(username)) {
                mappings.putIfAbsent(playerId, username);
            }
        }

        entries = mappings.entrySet().stream()
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public PacketPlayerNameMappings(PlayerId playerId, String username) {
        entries = isValidUsername(username) ? List.of(new Entry(playerId, username)) : List.of();
    }

    public PacketPlayerNameMappings(FriendlyByteBuf buf) {
        int size = buf.readInt();
        if (size < 0) {
            throw new IllegalArgumentException("Player name mapping count cannot be negative");
        }

        List<Entry> decodedEntries = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            decodedEntries.add(new Entry(PlayerIdNetworkCodec.read(buf),
                    buf.readUtf(SharedConstants.MAX_PLAYER_NAME_LENGTH)));
        }
        entries = List.copyOf(decodedEntries);
    }

    @Override
    public CustomPacketPayload.Type<PacketPlayerNameMappings> type() {
        return TYPE;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void applyTo(PlayerNameRepository repository) {
        for (Entry entry : entries) {
            repository.observe(entry.playerId(), entry.username(), PlayerNameSource.SERVER_SYNC);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            PlayerIdNetworkCodec.write(buf, entry.playerId());
            buf.writeUtf(entry.username(), SharedConstants.MAX_PLAYER_NAME_LENGTH);
        }
    }

    public static void handle(PacketContext<PacketPlayerNameMappings> ctx) {
        if (!Side.CLIENT.equals(ctx.side()) || !MapFrontiersClient.isJourneyMapPluginAvailable()) {
            return;
        }

        PacketPlayerNameMappings message = ctx.message();
        ClientPacketDelivery.submit(() -> MapFrontiersClient.applyPlayerNameMappings(message));
    }

    private static boolean isValidUsername(String username) {
        return !StringUtils.isBlank(username) && username.length() <= SharedConstants.MAX_PLAYER_NAME_LENGTH;
    }
}
