package games.alejandrocoria.mapfrontiers.common.territory.collection;

import games.alejandrocoria.mapfrontiers.common.util.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class CollectionVisibilityData {
    public static final int COLLECTION_VIEW_DISABLED_ZOOM = 0;
    private static final int DEFAULT_ZOOM = 256;
    private static final List<Integer> ZOOM_LEVELS = List.of(
            COLLECTION_VIEW_DISABLED_ZOOM, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384
    );

    private final EnumSet<CollectionVisibilityField> booleanValues;
    private int fullscreenZoom = getDefaultFullscreenZoom();
    private int minimapZoom = getDefaultMinimapZoom();
    private int webmapZoom = getDefaultWebmapZoom();

    public CollectionVisibilityData() {
        booleanValues = EnumSet.noneOf(CollectionVisibilityField.class);
        for (CollectionVisibilityField field : CollectionVisibilityField.BOOLEAN_VALUES) {
            if (field.getDefaultBooleanValue()) {
                booleanValues.add(field);
            }
        }
    }

    public CollectionVisibilityData(CollectionVisibilityData other) {
        booleanValues = other.booleanValues.clone();
        fullscreenZoom = other.fullscreenZoom;
        minimapZoom = other.minimapZoom;
        webmapZoom = other.webmapZoom;
    }

    public CollectionVisibilityData normalized() {
        return new CollectionVisibilityData(this);
    }

    public CollectionVisibilityData normalized(CollectionVisibilityMask mask) {
        CollectionVisibilityData normalized = normalized();
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (mask.has(field)) {
                continue;
            }

            if (field.isBoolean()) {
                normalized.setBoolean(field, field.getDefaultBooleanValue());
            } else {
                normalized.setZoom(field, getDefaultZoom(field));
            }
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof CollectionVisibilityData otherVisibilityData)) {
            return false;
        }

        return booleanValues.equals(otherVisibilityData.booleanValues)
                && fullscreenZoom == otherVisibilityData.fullscreenZoom
                && minimapZoom == otherVisibilityData.minimapZoom
                && webmapZoom == otherVisibilityData.webmapZoom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(booleanValues, fullscreenZoom, minimapZoom, webmapZoom);
    }

    public void setBoolean(CollectionVisibilityField field, boolean enabled) {
        requireBooleanField(field);

        if (enabled) {
            booleanValues.add(field);
        } else {
            booleanValues.remove(field);
        }
    }

    public boolean getBoolean(CollectionVisibilityField field) {
        requireBooleanField(field);
        return booleanValues.contains(field);
    }

    public void setZoom(CollectionVisibilityField field, int zoom) {
        int normalizedZoom = normalizeZoom(zoom);
        switch (field) {
            case FullscreenZoom -> fullscreenZoom = normalizedZoom;
            case MinimapZoom -> minimapZoom = normalizedZoom;
            case WebmapZoom -> webmapZoom = normalizedZoom;
            default -> throw new IllegalArgumentException("Field " + field + " is not a zoom field");
        }
    }

    public int getZoom(CollectionVisibilityField field) {
        return switch (field) {
            case FullscreenZoom -> fullscreenZoom;
            case MinimapZoom -> minimapZoom;
            case WebmapZoom -> webmapZoom;
            default -> throw new IllegalArgumentException("Field " + field + " is not a zoom field");
        };
    }

    public boolean isVisible() {
        return getBoolean(CollectionVisibilityField.Visible);
    }

    public void setVisible(boolean visible) {
        setBoolean(CollectionVisibilityField.Visible, visible);
    }

    public int getFullscreenZoom() {
        return getZoom(CollectionVisibilityField.FullscreenZoom);
    }

    public void setFullscreenZoom(int fullscreenZoom) {
        setZoom(CollectionVisibilityField.FullscreenZoom, fullscreenZoom);
    }

    public int getMinimapZoom() {
        return getZoom(CollectionVisibilityField.MinimapZoom);
    }

    public void setMinimapZoom(int minimapZoom) {
        setZoom(CollectionVisibilityField.MinimapZoom, minimapZoom);
    }

    public int getWebmapZoom() {
        return getZoom(CollectionVisibilityField.WebmapZoom);
    }

    public void setWebmapZoom(int webmapZoom) {
        setZoom(CollectionVisibilityField.WebmapZoom, webmapZoom);
    }

    public boolean getFullscreenName() {
        return getBoolean(CollectionVisibilityField.FullscreenName);
    }

    public void setFullscreenName(boolean fullscreenName) {
        setBoolean(CollectionVisibilityField.FullscreenName, fullscreenName);
    }

    public boolean getFullscreenOwner() {
        return getBoolean(CollectionVisibilityField.FullscreenOwner);
    }

    public void setFullscreenOwner(boolean fullscreenOwner) {
        setBoolean(CollectionVisibilityField.FullscreenOwner, fullscreenOwner);
    }

    public boolean getFullscreenBanner() {
        return getBoolean(CollectionVisibilityField.FullscreenBanner);
    }

    public void setFullscreenBanner(boolean fullscreenBanner) {
        setBoolean(CollectionVisibilityField.FullscreenBanner, fullscreenBanner);
    }

    public boolean getMinimapName() {
        return getBoolean(CollectionVisibilityField.MinimapName);
    }

    public void setMinimapName(boolean minimapName) {
        setBoolean(CollectionVisibilityField.MinimapName, minimapName);
    }

    public boolean getMinimapOwner() {
        return getBoolean(CollectionVisibilityField.MinimapOwner);
    }

    public void setMinimapOwner(boolean minimapOwner) {
        setBoolean(CollectionVisibilityField.MinimapOwner, minimapOwner);
    }

    public boolean getMinimapBanner() {
        return getBoolean(CollectionVisibilityField.MinimapBanner);
    }

    public void setMinimapBanner(boolean minimapBanner) {
        setBoolean(CollectionVisibilityField.MinimapBanner, minimapBanner);
    }

    public boolean getWebmapName() {
        return getBoolean(CollectionVisibilityField.WebmapName);
    }

    public void setWebmapName(boolean webmapName) {
        setBoolean(CollectionVisibilityField.WebmapName, webmapName);
    }

    public boolean getWebmapOwner() {
        return getBoolean(CollectionVisibilityField.WebmapOwner);
    }

    public void setWebmapOwner(boolean webmapOwner) {
        setBoolean(CollectionVisibilityField.WebmapOwner, webmapOwner);
    }

    public boolean getWebmapBanner() {
        return getBoolean(CollectionVisibilityField.WebmapBanner);
    }

    public void setWebmapBanner(boolean webmapBanner) {
        setBoolean(CollectionVisibilityField.WebmapBanner, webmapBanner);
    }

    public void applyOverride(CollectionVisibilityData override, CollectionVisibilityMask mask) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (!mask.has(field)) {
                continue;
            }

            if (field.isBoolean()) {
                setBoolean(field, override.getBoolean(field));
            } else {
                setZoom(field, override.getZoom(field));
            }
        }
    }

    public void readSparseNbt(CompoundTag nbt, CollectionVisibilityMask mask) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (!nbt.contains(field.getNbtKey())) {
                continue;
            }

            if (field.isBoolean()) {
                setBoolean(field, NbtCompat.getBooleanOr(nbt, field.getNbtKey(), false));
            } else {
                setZoom(field, NbtCompat.getIntOr(nbt, field.getNbtKey(), COLLECTION_VIEW_DISABLED_ZOOM));
            }
            mask.set(field, true);
        }
    }

    public void writeSparseNbt(CompoundTag nbt, CollectionVisibilityMask mask) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (!mask.has(field)) {
                continue;
            }

            if (field.isBoolean()) {
                nbt.putBoolean(field.getNbtKey(), getBoolean(field));
            } else {
                nbt.putInt(field.getNbtKey(), getZoom(field));
            }
        }
    }

    public void readFromNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                setBoolean(field, NbtCompat.getBooleanOr(nbt, field.getNbtKey(), field.getDefaultBooleanValue()));
            } else {
                setZoom(field, NbtCompat.getIntOr(nbt, field.getNbtKey(), getDefaultZoom(field)));
            }
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                nbt.putBoolean(field.getNbtKey(), getBoolean(field));
            } else {
                nbt.putInt(field.getNbtKey(), getZoom(field));
            }
        }
    }

    public void fromBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                setBoolean(field, buf.readBoolean());
            } else {
                setZoom(field, buf.readInt());
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        for (CollectionVisibilityField field : CollectionVisibilityField.VALUES) {
            if (field.isBoolean()) {
                buf.writeBoolean(getBoolean(field));
            } else {
                buf.writeInt(getZoom(field));
            }
        }
    }

    public static List<Integer> getZoomLevels() {
        return ZOOM_LEVELS;
    }

    public static int getDefaultFullscreenZoom() {
        return DEFAULT_ZOOM;
    }

    public static int getDefaultMinimapZoom() {
        return DEFAULT_ZOOM;
    }

    public static int getDefaultWebmapZoom() {
        return DEFAULT_ZOOM;
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

    public static int normalizeZoomToNearest(int zoom) {
        if (ZOOM_LEVELS.contains(zoom)) {
            return zoom;
        }

        int nearestZoom = ZOOM_LEVELS.get(0);
        int nearestDistance = Math.abs(zoom - nearestZoom);
        for (int i = 1; i < ZOOM_LEVELS.size(); ++i) {
            int candidateZoom = ZOOM_LEVELS.get(i);
            int candidateDistance = Math.abs(zoom - candidateZoom);
            if (candidateDistance < nearestDistance) {
                nearestZoom = candidateZoom;
                nearestDistance = candidateDistance;
            }
        }

        return nearestZoom;
    }

    private static void requireBooleanField(CollectionVisibilityField field) {
        if (!field.isBoolean()) {
            throw new IllegalArgumentException("Field " + field + " is not a boolean field");
        }
    }

    private static int getDefaultZoom(CollectionVisibilityField field) {
        return switch (field) {
            case FullscreenZoom -> getDefaultFullscreenZoom();
            case MinimapZoom -> getDefaultMinimapZoom();
            case WebmapZoom -> getDefaultWebmapZoom();
            default -> throw new IllegalArgumentException("Field " + field + " is not a zoom field");
        };
    }
}
