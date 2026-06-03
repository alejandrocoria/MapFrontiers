package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;

public class CollectionVisibilityMask {
    private final EnumSet<CollectionVisibilityField> values;

    public CollectionVisibilityMask() {
        values = EnumSet.noneOf(CollectionVisibilityField.class);
    }

    public CollectionVisibilityMask(CollectionVisibilityMask other) {
        values = other.values.clone();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CollectionVisibilityMask otherMask)) {
            return false;
        }

        return values.equals(otherMask.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    public boolean has(CollectionVisibilityField field) {
        return values.contains(field);
    }

    public void set(CollectionVisibilityField field, boolean enabled) {
        if (enabled) {
            values.add(field);
        } else {
            values.remove(field);
        }
    }

    public void clear() {
        values.clear();
    }

    public boolean hasAny() {
        return !values.isEmpty();
    }

    public boolean isVisible() {
        return has(CollectionVisibilityField.Visible);
    }

    public void setVisible(boolean visible) {
        set(CollectionVisibilityField.Visible, visible);
    }

    public boolean getFullscreenZoom() {
        return has(CollectionVisibilityField.FullscreenZoom);
    }

    public void setFullscreenZoom(boolean fullscreenZoom) {
        set(CollectionVisibilityField.FullscreenZoom, fullscreenZoom);
    }

    public boolean getMinimapZoom() {
        return has(CollectionVisibilityField.MinimapZoom);
    }

    public void setMinimapZoom(boolean minimapZoom) {
        set(CollectionVisibilityField.MinimapZoom, minimapZoom);
    }

    public boolean getWebmapZoom() {
        return has(CollectionVisibilityField.WebmapZoom);
    }

    public void setWebmapZoom(boolean webmapZoom) {
        set(CollectionVisibilityField.WebmapZoom, webmapZoom);
    }

    public boolean getFullscreenName() {
        return has(CollectionVisibilityField.FullscreenName);
    }

    public void setFullscreenName(boolean fullscreenName) {
        set(CollectionVisibilityField.FullscreenName, fullscreenName);
    }

    public boolean getFullscreenOwner() {
        return has(CollectionVisibilityField.FullscreenOwner);
    }

    public void setFullscreenOwner(boolean fullscreenOwner) {
        set(CollectionVisibilityField.FullscreenOwner, fullscreenOwner);
    }

    public boolean getFullscreenBanner() {
        return has(CollectionVisibilityField.FullscreenBanner);
    }

    public void setFullscreenBanner(boolean fullscreenBanner) {
        set(CollectionVisibilityField.FullscreenBanner, fullscreenBanner);
    }

    public boolean getMinimapName() {
        return has(CollectionVisibilityField.MinimapName);
    }

    public void setMinimapName(boolean minimapName) {
        set(CollectionVisibilityField.MinimapName, minimapName);
    }

    public boolean getMinimapOwner() {
        return has(CollectionVisibilityField.MinimapOwner);
    }

    public void setMinimapOwner(boolean minimapOwner) {
        set(CollectionVisibilityField.MinimapOwner, minimapOwner);
    }

    public boolean getMinimapBanner() {
        return has(CollectionVisibilityField.MinimapBanner);
    }

    public void setMinimapBanner(boolean minimapBanner) {
        set(CollectionVisibilityField.MinimapBanner, minimapBanner);
    }

    public boolean getWebmapName() {
        return has(CollectionVisibilityField.WebmapName);
    }

    public void setWebmapName(boolean webmapName) {
        set(CollectionVisibilityField.WebmapName, webmapName);
    }

    public boolean getWebmapOwner() {
        return has(CollectionVisibilityField.WebmapOwner);
    }

    public void setWebmapOwner(boolean webmapOwner) {
        set(CollectionVisibilityField.WebmapOwner, webmapOwner);
    }

    public boolean getWebmapBanner() {
        return has(CollectionVisibilityField.WebmapBanner);
    }

    public void setWebmapBanner(boolean webmapBanner) {
        set(CollectionVisibilityField.WebmapBanner, webmapBanner);
    }

    public boolean hasSome() {
        return hasAny();
    }

    public void readFromNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            set(field, nbt.getBooleanOr(field.getNbtKey(), false));
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            nbt.putBoolean(field.getNbtKey(), has(field));
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            set(field, buf.readBoolean());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            buf.writeBoolean(has(field));
        }
    }
}
