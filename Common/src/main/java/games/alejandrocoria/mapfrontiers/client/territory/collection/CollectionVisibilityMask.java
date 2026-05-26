package games.alejandrocoria.mapfrontiers.client.territory.collection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;

public class CollectionVisibilityMask {
    private boolean visible;
    private boolean fullscreenZoom;
    private boolean minimapZoom;
    private boolean webmapZoom;
    private boolean fullscreenName;
    private boolean fullscreenOwner;
    private boolean fullscreenBanner;
    private boolean minimapName;
    private boolean minimapOwner;
    private boolean minimapBanner;
    private boolean webmapName;
    private boolean webmapOwner;
    private boolean webmapBanner;

    public CollectionVisibilityMask() {
    }

    public CollectionVisibilityMask(CollectionVisibilityMask other) {
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

        if (!(other instanceof CollectionVisibilityMask otherMask)) {
            return false;
        }

        return visible == otherMask.visible
                && fullscreenZoom == otherMask.fullscreenZoom
                && minimapZoom == otherMask.minimapZoom
                && webmapZoom == otherMask.webmapZoom
                && fullscreenName == otherMask.fullscreenName
                && fullscreenOwner == otherMask.fullscreenOwner
                && fullscreenBanner == otherMask.fullscreenBanner
                && minimapName == otherMask.minimapName
                && minimapOwner == otherMask.minimapOwner
                && minimapBanner == otherMask.minimapBanner
                && webmapName == otherMask.webmapName
                && webmapOwner == otherMask.webmapOwner
                && webmapBanner == otherMask.webmapBanner;
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

    public boolean getFullscreenZoom() {
        return fullscreenZoom;
    }

    public void setFullscreenZoom(boolean fullscreenZoom) {
        this.fullscreenZoom = fullscreenZoom;
    }

    public boolean getMinimapZoom() {
        return minimapZoom;
    }

    public void setMinimapZoom(boolean minimapZoom) {
        this.minimapZoom = minimapZoom;
    }

    public boolean getWebmapZoom() {
        return webmapZoom;
    }

    public void setWebmapZoom(boolean webmapZoom) {
        this.webmapZoom = webmapZoom;
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

    public boolean hasSome() {
        return visible
                || fullscreenZoom
                || minimapZoom
                || webmapZoom
                || fullscreenName
                || fullscreenOwner
                || fullscreenBanner
                || minimapName
                || minimapOwner
                || minimapBanner
                || webmapName
                || webmapOwner
                || webmapBanner;
    }

    public void readFromNBT(CompoundTag nbt) {
        visible = nbt.getBooleanOr("visible", false);
        fullscreenZoom = nbt.getBooleanOr("fullscreenZoom", false);
        minimapZoom = nbt.getBooleanOr("minimapZoom", false);
        webmapZoom = nbt.getBooleanOr("webmapZoom", false);
        fullscreenName = nbt.getBooleanOr("fullscreenName", false);
        fullscreenOwner = nbt.getBooleanOr("fullscreenOwner", false);
        fullscreenBanner = nbt.getBooleanOr("fullscreenBanner", false);
        minimapName = nbt.getBooleanOr("minimapName", false);
        minimapOwner = nbt.getBooleanOr("minimapOwner", false);
        minimapBanner = nbt.getBooleanOr("minimapBanner", false);
        webmapName = nbt.getBooleanOr("webmapName", false);
        webmapOwner = nbt.getBooleanOr("webmapOwner", false);
        webmapBanner = nbt.getBooleanOr("webmapBanner", false);
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("visible", visible);
        nbt.putBoolean("fullscreenZoom", fullscreenZoom);
        nbt.putBoolean("minimapZoom", minimapZoom);
        nbt.putBoolean("webmapZoom", webmapZoom);
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
        fullscreenZoom = buf.readBoolean();
        minimapZoom = buf.readBoolean();
        webmapZoom = buf.readBoolean();
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
        buf.writeBoolean(fullscreenZoom);
        buf.writeBoolean(minimapZoom);
        buf.writeBoolean(webmapZoom);
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
}
