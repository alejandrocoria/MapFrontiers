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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public class PacketPlayerNameMappings {
    public record Entry(PlayerId playerId, String username) {
        public Entry {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(username, "username");
        }
    }

    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final ResourceLocation CHANNEL = new ResourceLocation(MapFrontiers.MODID, "packet_player_name_mappings");

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
                    buf.readUtf(MAX_PLAYER_NAME_LENGTH)));
        }
        entries = List.copyOf(decodedEntries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void applyTo(PlayerNameRepository repository) {
        Objects.requireNonNull(repository, "repository");
        for (Entry entry : entries) {
            repository.observe(entry.playerId(), entry.username(), PlayerNameSource.SERVER_SYNC);
        }
    }

    void applyHintsTo(PlayerNameRepository repository, Set<PlayerId> allowedPlayerIds) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(allowedPlayerIds, "allowedPlayerIds");

        for (Entry entry : entries) {
            if (allowedPlayerIds.contains(entry.playerId())) {
                repository.observe(entry.playerId(), entry.username(), PlayerNameSource.HINT);
            }
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            PlayerIdNetworkCodec.write(buf, entry.playerId());
            buf.writeUtf(entry.username(), MAX_PLAYER_NAME_LENGTH);
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
        return !StringUtils.isBlank(username) && username.length() <= MAX_PLAYER_NAME_LENGTH;
    }
}
