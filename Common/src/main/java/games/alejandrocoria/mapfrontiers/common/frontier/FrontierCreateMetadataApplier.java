package games.alejandrocoria.mapfrontiers.common.frontier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class FrontierCreateMetadataApplier {
    private FrontierCreateMetadataApplier() {
    }

    /**
     * Create applies a fully resolved payload. Unlike mutation flows, there is no merge semantics here.
     */
    public static void applyInitialMetadata(FrontierData frontier, FrontierCreateSpec spec) {
        frontier.setName1(spec.getName1());
        frontier.setName2(spec.getName2());
        frontier.setColor(spec.getColor());
        frontier.setVisibilityData(spec.getVisibility());
        frontier.setBannerData(spec.getBanner());
        frontier.setPathStyle(spec.getPathStyle());
        frontier.setCollectionId(spec.getCollectionId());
        frontier.setSourcePluginId(spec.getSourcePluginId());
    }
}
