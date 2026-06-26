package games.alejandrocoria.mapfrontiers.common.territory.frontier;

public enum FrontierVisibility {
    Frontier("visible", true),
    AnnounceInChat("announceInChat", false),
    AnnounceInTitle("announceInTitle", false),
    MentionCollection("mentionCollection", true),
    Fullscreen("fullscreenVisible", true),
    FullscreenName("fullscreenNameVisible", true),
    FullscreenCollection("fullscreenCollectionVisible", false),
    FullscreenOwner("fullscreenOwnerVisible", false),
    FullscreenBanner("fullscreenBannerVisible", false),
    FullscreenDay("fullscreenDay", true),
    FullscreenNight("fullscreenNight", true),
    FullscreenUnderground("fullscreenUnderground", true),
    FullscreenTopo("fullscreenTopo", true),
    FullscreenBiome("fullscreenBiome", true),
    Minimap("minimapVisible", true),
    MinimapName("minimapNameVisible", true),
    MinimapCollection("minimapCollectionVisible", false),
    MinimapOwner("minimapOwnerVisible", false),
    MinimapBanner("minimapBannerVisible", false),
    MinimapDay("minimapDay", true),
    MinimapNight("minimapNight", true),
    MinimapUnderground("minimapUnderground", true),
    MinimapTopo("minimapTopo", true),
    MinimapBiome("minimapBiome", true),
    Webmap("webmapVisible", true),
    WebmapName("webmapNameVisible", true),
    WebmapCollection("webmapCollectionVisible", false),
    WebmapOwner("webmapOwnerVisible", false),
    WebmapBanner("webmapBannerVisible", false),
    WebmapDay("webmapDay", true),
    WebmapNight("webmapNight", true),
    WebmapUnderground("webmapUnderground", true),
    WebmapTopo("webmapTopo", true),
    WebmapBiome("webmapBiome", true);

    public static final FrontierVisibility[] VALUES = values();

    private final String nbtKey;
    private final boolean defaultValue;

    FrontierVisibility(String nbtKey, boolean defaultValue) {
        this.nbtKey = nbtKey;
        this.defaultValue = defaultValue;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }
}
