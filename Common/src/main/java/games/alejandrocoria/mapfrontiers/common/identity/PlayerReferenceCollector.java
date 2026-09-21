package games.alejandrocoria.mapfrontiers.common.identity;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierUserAccess;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class PlayerReferenceCollector {
    private PlayerReferenceCollector() {
    }

    public static LinkedHashSet<PlayerId> collect(FrontierData frontier) {
        LinkedHashSet<PlayerId> playerIds = new LinkedHashSet<>();
        add(playerIds, frontier);
        return playerIds;
    }

    public static LinkedHashSet<PlayerId> collect(CollectionData collection) {
        LinkedHashSet<PlayerId> playerIds = new LinkedHashSet<>();
        add(playerIds, collection);
        return playerIds;
    }

    public static LinkedHashSet<PlayerId> collect(FrontierSharingChange sharingChange) {
        LinkedHashSet<PlayerId> playerIds = new LinkedHashSet<>();
        add(playerIds, sharingChange);
        return playerIds;
    }

    public static LinkedHashSet<PlayerId> collect(FrontierSettings settings) {
        LinkedHashSet<PlayerId> playerIds = new LinkedHashSet<>();
        add(playerIds, settings);
        return playerIds;
    }

    public static void add(Set<PlayerId> playerIds, FrontierData frontier) {
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(frontier, "frontier");

        playerIds.add(frontier.getOwner());
        List<FrontierUserAccess> userAccesses = frontier.getUserAccesses();
        if (userAccesses != null) {
            for (FrontierUserAccess userAccess : userAccesses) {
                playerIds.add(userAccess.getPlayerId());
            }
        }
        PlayerId copiedFromUser = frontier.getCopiedFromUser();
        if (frontier.wasCopied() && copiedFromUser != null) {
            playerIds.add(copiedFromUser);
        }
    }

    public static void add(Set<PlayerId> playerIds, CollectionData collection) {
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(collection, "collection");

        playerIds.add(collection.getOwner());
        PlayerId copiedFromUser = collection.getCopiedFromUser();
        if (collection.wasCopied() && copiedFromUser != null) {
            playerIds.add(copiedFromUser);
        }
    }

    public static void add(Set<PlayerId> playerIds, FrontierSharingChange sharingChange) {
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(sharingChange, "sharingChange");

        List<FrontierUserAccess> userAccesses = sharingChange.getUserAccesses();
        if (userAccesses != null) {
            for (FrontierUserAccess userAccess : userAccesses) {
                playerIds.add(userAccess.getPlayerId());
            }
        }
    }

    public static void add(Set<PlayerId> playerIds, FrontierSettings settings) {
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(settings, "settings");

        for (SettingsGroup group : settings.getCustomGroups()) {
            playerIds.addAll(group.getUsers());
        }
    }
}
