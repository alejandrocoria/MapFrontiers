package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Objects;

public class CollectionVisibilityData {
    public static final int COLLECTION_VIEW_DISABLED_ZOOM = 0;
    private static final List<Integer> ZOOM_LEVELS = List.of(
            COLLECTION_VIEW_DISABLED_ZOOM, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    );

    private boolean visible = false;
    private int fullscreenZoom = 256;
    private int minimapZoom = 256;
    private int webmapZoom = 256;
    private boolean fullscreenName = true;
    private boolean fullscreenOwner = false;
    private boolean fullscreenBanner = true;
    private boolean minimapName = true;
    private boolean minimapOwner = false;
    private boolean minimapBanner = true;
    private boolean webmapName = true;
    private boolean webmapOwner = false;
    private boolean webmapBanner = true;

    public CollectionVisibilityData() {
    }

    public CollectionVisibilityData(CollectionVisibilityData other) {
        visible = other.visible;
        fullscreenZoom = other.fullscreenZoom;
        minimapZoom = other.minimapZoom;
        webmapZoom = other.webmapZoom;
        fullscreenName = other.fullscreenName;
        fullscreenOwner = other.fullscreenOwner;
        fullscreenBanner = other.fullscreenBanner;
        minimapName = other.minimapName;
        minimapOwner = other.minimapOwner;
        minimapBanner = other.minimapBanner;
        webmapName = other.webmapName;
        webmapOwner = other.webmapOwner;
        webmapBanner = other.webmapBanner;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CollectionVisibilityData otherVisibilityData)) {
            return false;
        }

        return visible == otherVisibilityData.visible
                && fullscreenZoom == otherVisibilityData.fullscreenZoom
                && minimapZoom == otherVisibilityData.minimapZoom
                && webmapZoom == otherVisibilityData.webmapZoom
                && fullscreenName == otherVisibilityData.fullscreenName
                && fullscreenOwner == otherVisibilityData.fullscreenOwner
                && fullscreenBanner == otherVisibilityData.fullscreenBanner
                && minimapName == otherVisibilityData.minimapName
                && minimapOwner == otherVisibilityData.minimapOwner
                && minimapBanner == otherVisibilityData.minimapBanner
                && webmapName == otherVisibilityData.webmapName
                && webmapOwner == otherVisibilityData.webmapOwner
                && webmapBanner == otherVisibilityData.webmapBanner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, fullscreenZoom, minimapZoom, webmapZoom,
                fullscreenName, fullscreenOwner, fullscreenBanner,
                minimapName, minimapOwner, minimapBanner,
                webmapName, webmapOwner, webmapBanner);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getFullscreenZoom() {
        return fullscreenZoom;
    }

    public void setFullscreenZoom(int fullscreenZoom) {
        this.fullscreenZoom = normalizeZoom(fullscreenZoom);
    }

    public int getMinimapZoom() {
        return minimapZoom;
    }

    public void setMinimapZoom(int minimapZoom) {
        this.minimapZoom = normalizeZoom(minimapZoom);
    }

    public int getWebmapZoom() {
        return webmapZoom;
    }

    public void setWebmapZoom(int webmapZoom) {
        this.webmapZoom = normalizeZoom(webmapZoom);
    }

    public boolean getFullscreenName() {
        return fullscreenName;
    }

    public void setFullscreenName(boolean fullscreenName) {
        this.fullscreenName = fullscreenName;
    }

    public boolean getFullscreenOwner() {
        return fullscreenOwner;
    }

    public void setFullscreenOwner(boolean fullscreenOwner) {
        this.fullscreenOwner = fullscreenOwner;
    }

    public boolean getFullscreenBanner() {
        return fullscreenBanner;
    }

    public void setFullscreenBanner(boolean fullscreenBanner) {
        this.fullscreenBanner = fullscreenBanner;
    }

    public boolean getMinimapName() {
        return minimapName;
    }

    public void setMinimapName(boolean minimapName) {
        this.minimapName = minimapName;
    }

    public boolean getMinimapOwner() {
        return minimapOwner;
    }

    public void setMinimapOwner(boolean minimapOwner) {
        this.minimapOwner = minimapOwner;
    }

    public boolean getMinimapBanner() {
        return minimapBanner;
    }

    public void setMinimapBanner(boolean minimapBanner) {
        this.minimapBanner = minimapBanner;
    }

    public boolean getWebmapName() {
        return webmapName;
    }

    public void setWebmapName(boolean webmapName) {
        this.webmapName = webmapName;
    }

    public boolean getWebmapOwner() {
        return webmapOwner;
    }

    public void setWebmapOwner(boolean webmapOwner) {
        this.webmapOwner = webmapOwner;
    }

    public boolean getWebmapBanner() {
        return webmapBanner;
    }

    public void setWebmapBanner(boolean webmapBanner) {
        this.webmapBanner = webmapBanner;
    }

    public void readFromNBT(CompoundTag nbt) {
        visible = nbt.getBooleanOr("visible", false);
        fullscreenZoom = normalizeZoom(nbt.getIntOr("fullscreenZoom", 256));
        minimapZoom = normalizeZoom(nbt.getIntOr("minimapZoom", 256));
        webmapZoom = normalizeZoom(nbt.getIntOr("webmapZoom", 256));
        fullscreenName = nbt.getBooleanOr("fullscreenName", true);
        fullscreenOwner = nbt.getBooleanOr("fullscreenOwner", false);
        fullscreenBanner = nbt.getBooleanOr("fullscreenBanner", true);
        minimapName = nbt.getBooleanOr("minimapName", true);
        minimapOwner = nbt.getBooleanOr("minimapOwner", false);
        minimapBanner = nbt.getBooleanOr("minimapBanner", true);
        webmapName = nbt.getBooleanOr("webmapName", true);
        webmapOwner = nbt.getBooleanOr("webmapOwner", false);
        webmapBanner = nbt.getBooleanOr("webmapBanner", true);
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("visible", visible);
        nbt.putInt("fullscreenZoom", fullscreenZoom);
        nbt.putInt("minimapZoom", minimapZoom);
        nbt.putInt("webmapZoom", webmapZoom);
        nbt.putBoolean("fullscreenName", fullscreenName);
        nbt.putBoolean("fullscreenOwner", fullscreenOwner);
        nbt.putBoolean("fullscreenBanner", fullscreenBanner);
        nbt.putBoolean("minimapName", minimapName);
        nbt.putBoolean("minimapOwner", minimapOwner);
        nbt.putBoolean("minimapBanner", minimapBanner);
        nbt.putBoolean("webmapName", webmapName);
        nbt.putBoolean("webmapOwner", webmapOwner);
        nbt.putBoolean("webmapBanner", webmapBanner);
    }

    public void fromBytes(FriendlyByteBuf buf) {
        visible = buf.readBoolean();
        fullscreenZoom = normalizeZoom(buf.readInt());
        minimapZoom = normalizeZoom(buf.readInt());
        webmapZoom = normalizeZoom(buf.readInt());
        fullscreenName = buf.readBoolean();
        fullscreenOwner = buf.readBoolean();
        fullscreenBanner = buf.readBoolean();
        minimapName = buf.readBoolean();
        minimapOwner = buf.readBoolean();
        minimapBanner = buf.readBoolean();
        webmapName = buf.readBoolean();
        webmapOwner = buf.readBoolean();
        webmapBanner = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(visible);
        buf.writeInt(fullscreenZoom);
        buf.writeInt(minimapZoom);
        buf.writeInt(webmapZoom);
        buf.writeBoolean(fullscreenName);
        buf.writeBoolean(fullscreenOwner);
        buf.writeBoolean(fullscreenBanner);
        buf.writeBoolean(minimapName);
        buf.writeBoolean(minimapOwner);
        buf.writeBoolean(minimapBanner);
        buf.writeBoolean(webmapName);
        buf.writeBoolean(webmapOwner);
        buf.writeBoolean(webmapBanner);
    }

    public static List<Integer> getZoomLevels() {
        return ZOOM_LEVELS;
    }

    public static boolean isZoomEnabled(int zoom) {
        return zoom > COLLECTION_VIEW_DISABLED_ZOOM;
    }

    public static int getZoomTransitionMinZoom(int zoom) {
        int currentIndex = ZOOM_LEVELS.indexOf(zoom);
        if (currentIndex < 0 || currentIndex >= ZOOM_LEVELS.size() - 1) {
            return 32768;
        }

        return ZOOM_LEVELS.get(currentIndex + 1);
    }

    public static int normalizeZoom(int zoom) {
        return ZOOM_LEVELS.contains(zoom) ? zoom : COLLECTION_VIEW_DISABLED_ZOOM;
    }
}
