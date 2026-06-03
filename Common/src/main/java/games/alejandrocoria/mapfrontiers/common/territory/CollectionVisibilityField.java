package games.alejandrocoria.mapfrontiers.common.territory;

public enum CollectionVisibilityField {
    Visible("visible", Type.Boolean, false),
    FullscreenZoom("fullscreenZoom", Type.Zoom, false),
    MinimapZoom("minimapZoom", Type.Zoom, false),
    WebmapZoom("webmapZoom", Type.Zoom, false),
    FullscreenName("fullscreenName", Type.Boolean, true),
    FullscreenOwner("fullscreenOwner", Type.Boolean, false),
    FullscreenBanner("fullscreenBanner", Type.Boolean, true),
    MinimapName("minimapName", Type.Boolean, true),
    MinimapOwner("minimapOwner", Type.Boolean, false),
    MinimapBanner("minimapBanner", Type.Boolean, true),
    WebmapName("webmapName", Type.Boolean, true),
    WebmapOwner("webmapOwner", Type.Boolean, false),
    WebmapBanner("webmapBanner", Type.Boolean, true);

    public static final CollectionVisibilityField[] VALUES = values();
    public static final CollectionVisibilityField[] BOOLEAN_VALUES = {
            Visible,
            FullscreenName,
            FullscreenOwner,
            FullscreenBanner,
            MinimapName,
            MinimapOwner,
            MinimapBanner,
            WebmapName,
            WebmapOwner,
            WebmapBanner
    };
    public static final CollectionVisibilityField[] ZOOM_VALUES = {
            FullscreenZoom,
            MinimapZoom,
            WebmapZoom
    };

    private final String nbtKey;
    private final Type type;
    private final boolean defaultBooleanValue;

    CollectionVisibilityField(String nbtKey, Type type, boolean defaultBooleanValue) {
        this.nbtKey = nbtKey;
        this.type = type;
        this.defaultBooleanValue = defaultBooleanValue;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public boolean isBoolean() {
        return type == Type.Boolean;
    }

    public boolean isZoom() {
        return type == Type.Zoom;
    }

    public boolean getDefaultBooleanValue() {
        if (!isBoolean()) {
            throw new IllegalStateException("Field " + this + " does not have a boolean default");
        }

        return defaultBooleanValue;
    }

    private enum Type {
        Boolean,
        Zoom
    }
}
