package games.alejandrocoria.mapfrontiers.api.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable snapshot view of frontier data exposed by the API.
 * <p>
 * For now, MapFrontiers limits {@code name1} and {@code name2} to 17 characters each.
 */
public record FrontierDataView(FrontierId id,
                               FrontierType type,
                               DimensionId dimension,
                               int color,
                               String name1,
                               String name2,
                               FrontierShape shape,
                               Set<FrontierVisibilityFlag> visibility,
                               FrontierBanner banner,
                               Optional<String> sourcePluginId,
                               UserRef owner,
                               List<SharedUserAccess> sharedUsers) {
    public FrontierDataView {
        // Defensive copies ensure API callers cannot mutate internal state by reference.
        visibility = visibility == null ? Set.of() : Set.copyOf(visibility);
        sourcePluginId = sourcePluginId == null ? Optional.empty() : sourcePluginId;
        sharedUsers = sharedUsers == null ? List.of() : List.copyOf(sharedUsers);
    }
}
