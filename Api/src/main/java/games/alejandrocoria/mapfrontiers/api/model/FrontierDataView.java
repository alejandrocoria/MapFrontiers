package games.alejandrocoria.mapfrontiers.api.model;

import java.util.List;
import java.util.Set;

public record FrontierDataView(FrontierId id,
                               FrontierType type,
                               DimensionId dimension,
                               int color,
                               String name1,
                               String name2,
                               FrontierShape shape,
                               Set<FrontierVisibilityFlag> visibility,
                               FrontierBanner banner,
                               UserRef owner,
                               List<SharedUserAccess> sharedUsers) {
}
