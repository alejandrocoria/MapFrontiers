package games.alejandrocoria.mapfrontiers.client.territory.collection;

import com.mojang.blaze3d.platform.NativeImage;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.BannerRenderer;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierLabelPlacementSolver;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionVisibilityData;
import it.unimi.dsi.fastutil.Pair;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class CollectionOverlay {
    private static final int OVERLAY_Y = 70;
    private static final int BANNER_BASE_WIDTH_PX = 20;
    private static final int BANNER_BASE_HEIGHT_PX = 40;
    private static final int TEXT_LINE_HEIGHT_PX = 9;
    private static final int LABEL_CONTENT_PADDING_PX = 6;
    private static final int BANNER_SINGLE_LINE_TEXT_OFFSET_Y = 5;
    private static final double LABEL_SOLVER_PRECISION = 2.0;
    private static final int HIGHLIGHT_STROKE_WIDTH = 4;
    private static final Context.MapType[] ALL_MAP_TYPES = {
            Context.MapType.Day,
            Context.MapType.Night,
            Context.MapType.Underground,
            Context.MapType.Topo,
            Context.MapType.Biome
    };
    private static final MapImage transparentLabelMarker = createTransparentLabelMarker();

    private final CollectionOverlayKey key;
    private final @Nullable IClientAPI jmAPI;
    private @Nullable CollectionData collection;
    private List<FrontierOverlay> memberFrontiers;
    private CollectionVisibilityData effectiveVisibilityData = new CollectionVisibilityData();
    private CollectionVisibilityData visibilityOverrideData = new CollectionVisibilityData();
    private CollectionVisibilityMask visibilityOverrideMask = new CollectionVisibilityMask();
    private boolean needUpdateOverlay = true;
    private boolean membershipDirty = true;
    private boolean geometryDirty = true;
    private boolean labelsDirty = true;
    private boolean labelVisibilityDirty = true;
    private boolean highlighted = false;
    private boolean highlightStructureDirty = true;
    private boolean highlightVisibilityDirty = true;
    private List<CollectionVisibilityVariant> visibleVariants = List.of();
    private final List<MarkerOverlay> labelOverlays = new ArrayList<>();
    private final List<PolygonOverlay> highlightPolygonOverlays = new ArrayList<>();
    private List<CollectionHighlightRenderGeometry> highlightRenderGeometries = List.of();
    private final Map<CollectionLabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache = new HashMap<>();
    private final BannerRenderer bannerRenderer = new BannerRenderer();
    private @Nullable Runnable dirtyOverlayListener;

    public CollectionOverlay(CollectionOverlayKey key, @Nullable IClientAPI jmAPI, CollectionData collection, List<FrontierOverlay> members) {
        this.key = key;
        this.jmAPI = jmAPI;
        this.collection = collection;
        memberFrontiers = List.copyOf(members);
        refreshEffectiveVisibility();
        refreshBannerRenderer();
    }

    public CollectionOverlayKey getKey() {
        return key;
    }

    public List<MarkerOverlay> getLabelOverlays() {
        return labelOverlays;
    }

    public void refreshMembersAndCollection(CollectionData collection, List<FrontierOverlay> members) {
        boolean dirty = false;

        if (this.collection != collection) {
            this.collection = collection;
            refreshEffectiveVisibility();
            refreshBannerRenderer();
            geometryDirty = true;
            labelsDirty = true;
            labelVisibilityDirty = true;
            dirty = true;
        }

        List<FrontierOverlay> updatedMembers = List.copyOf(members);
        if (!memberFrontiers.equals(updatedMembers)) {
            memberFrontiers = updatedMembers;
            membershipDirty = true;
            geometryDirty = true;
            labelsDirty = true;
            labelVisibilityDirty = true;
            highlightStructureDirty = true;
            dirty = true;
        }

        if (dirty) {
            invalidateOverlayRefresh();
        }
    }

    public void setVisibilityOverride(Pair<CollectionVisibilityData, CollectionVisibilityMask> visibilityOverride) {
        CollectionVisibilityData newOverrideData = new CollectionVisibilityData(visibilityOverride.first());
        CollectionVisibilityMask newOverrideMask = new CollectionVisibilityMask(visibilityOverride.second());
        if (visibilityOverrideData.equals(newOverrideData) && visibilityOverrideMask.equals(newOverrideMask)) {
            return;
        }

        visibilityOverrideData = newOverrideData;
        visibilityOverrideMask = newOverrideMask;
        refreshEffectiveVisibility();
        refreshBannerRenderer();
        geometryDirty = true;
        labelsDirty = true;
        labelVisibilityDirty = true;
        invalidateOverlayRefresh();
    }

    public void processDirtyOverlay() {
        if (needUpdateOverlay) {
            refreshOverlay();
        }
    }

    public void rebuildOverlayNow() {
        membershipDirty = true;
        geometryDirty = true;
        labelsDirty = true;
        labelVisibilityDirty = true;
        highlightStructureDirty = true;
        highlightVisibilityDirty = true;
        refreshBannerRenderer();
        refreshOverlay();
    }

    void markGeometryDirty() {
        geometryDirty = true;
        labelsDirty = true;
        highlightStructureDirty = true;
        invalidateOverlayRefresh();
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        invalidateHighlightVisibility();
    }

    public void deleted() {
        hideMarkerOverlays(labelOverlays);
        hidePolygonOverlays(highlightPolygonOverlays);
        labelOverlays.clear();
        highlightPolygonOverlays.clear();
        highlightRenderGeometries = List.of();
        placementCache.clear();
        visibleVariants = List.of();
        bannerRenderer.releaseTexture();
        dirtyOverlayListener = null;
    }

    void setDirtyOverlayListener(@Nullable Runnable dirtyOverlayListener) {
        this.dirtyOverlayListener = dirtyOverlayListener;
    }

    private void refreshOverlay() {
        needUpdateOverlay = false;
        if (membershipDirty || geometryDirty) {
            rebuildVisibleVariants();
        }

        if (labelsDirty || labelVisibilityDirty) {
            rebuildLabelOverlays();
        }

        if (highlighted && highlightStructureDirty) {
            rebuildHighlightOverlays();
            highlightStructureDirty = false;
            highlightVisibilityDirty = false;
        } else if (highlightVisibilityDirty) {
            refreshHighlightVisibility();
            highlightVisibilityDirty = false;
        }

        membershipDirty = false;
        geometryDirty = false;
        labelsDirty = false;
        labelVisibilityDirty = false;
    }

    private void rebuildVisibleVariants() {
        placementCache.clear();
        if (collection == null || memberFrontiers.isEmpty()) {
            visibleVariants = List.of();
            labelsDirty = true;
            return;
        }

        List<CollectionVisibilityVariant> rebuiltVariants = new ArrayList<>();
        for (Context.UI ui : getSupportedUis()) {
            if (!resolveVisibility(ui)) {
                continue;
            }

            int collectionMaxZoom = resolveZoom(ui);
            if (!CollectionVisibilityData.isZoomEnabled(collectionMaxZoom)) {
                continue;
            }

            List<VisibleVariantBuilder> variantBuilders = new ArrayList<>();
            for (Context.MapType mapType : getMapTypesForUi(ui)) {
                List<VisibleMemberGeometry> visibleMembers = resolveVisibleMemberGeometries(ui, mapType, collectionMaxZoom);
                if (visibleMembers.isEmpty()) {
                    continue;
                }

                List<FrontierOverlay> visibleFrontiers = visibleMembers.stream()
                        .map(VisibleMemberGeometry::frontier)
                        .toList();
                VisibleVariantBuilder existingVariant = findVariantBuilder(variantBuilders, ui, visibleFrontiers);
                if (existingVariant == null) {
                    variantBuilders.add(new VisibleVariantBuilder(ui, List.copyOf(visibleFrontiers), List.copyOf(visibleMembers), mapType));
                } else {
                    existingVariant.addMapType(mapType);
                }
            }

            for (VisibleVariantBuilder builder : variantBuilders) {
                CollectionVisibilityVariant variant = buildVisibleVariant(builder);
                if (variant != null) {
                    rebuiltVariants.add(variant);
                }
            }
        }

        visibleVariants = List.copyOf(rebuiltVariants);
        labelsDirty = true;
    }

    private void rebuildHighlightOverlays() {
        hidePolygonOverlays(highlightPolygonOverlays);
        highlightPolygonOverlays.clear();
        highlightRenderGeometries = rebuildHighlightRenderGeometries();

        if (!highlighted) {
            return;
        }

        ShapeProperties highlightShapeProperties = createHighlightShapeProperties();
        for (CollectionHighlightRenderGeometry geometry : highlightRenderGeometries) {
            PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, key.dimension(),
                    highlightShapeProperties, geometry.polygon(), geometry.holes());
            overlay.setActiveUIs(Context.UI.Fullscreen);
            overlay.setActiveMapTypes(ALL_MAP_TYPES);
            if (geometry.minZoom() > 0) {
                overlay.setMinZoom(geometry.minZoom());
            }
            highlightPolygonOverlays.add(overlay);
        }

        showPolygonOverlaysQuietly(highlightPolygonOverlays);
    }

    private void rebuildLabelOverlays() {
        hideMarkerOverlays(labelOverlays);
        labelOverlays.clear();

        if (collection == null || visibleVariants.isEmpty()) {
            return;
        }

        for (CollectionVisibilityVariant variant : visibleVariants) {
            CollectionLabelContentMetrics metrics = buildLabelContentMetrics(variant.getUi());
            if (metrics.isEmpty()) {
                continue;
            }

            int collectionMaxZoom = resolveZoom(variant.getUi());
            for (CollectionGeometryIsland island : variant.getIslands()) {
                CollectionLabelPlacementKey placementKey = new CollectionLabelPlacementKey(island, metrics.contentWidthPx(), metrics.contentHeightPx());
                FrontierLabelPlacementSolver.LabelPlacement placement = placementCache.computeIfAbsent(placementKey,
                        ignored -> FrontierLabelPlacementSolver.solve(island.copyEffectiveArea(),
                                metrics.contentWidthPx(),
                                metrics.contentHeightPx(),
                                LABEL_SOLVER_PRECISION));
                if (placement.availableWidthBlocks() <= 0.0 || placement.availableHeightBlocks() <= 0.0) {
                    continue;
                }

                addLabelOverlay(variant.getUi(), variant.getMapTypes().toArray(Context.MapType[]::new), metrics, island, placement, collectionMaxZoom);
            }
        }
        showMarkerOverlaysQuietly(labelOverlays);
    }

    private void addLabelOverlay(Context.UI ui, Context.MapType[] mapTypes, CollectionLabelContentMetrics metrics, CollectionGeometryIsland island,
                                 FrontierLabelPlacementSolver.LabelPlacement placement,
                                 int collectionMaxZoom) {
        TextProperties textProperties = createBaseTextProperties().setOffsetY(metrics.textOffsetY());
        int minZoom = Math.max(2, island.getMinZoom());
        BlockPos anchor = BlockPos.containing(placement.centerX(), OVERLAY_Y, placement.centerZ());
        MarkerOverlay labelOverlay = new MarkerOverlay(MapFrontiers.MODID, anchor, createLabelAnchorIcon(metrics));
        labelOverlay.setActiveUIs(ui);
        labelOverlay.setActiveMapTypes(mapTypes);
        labelOverlay.setDimension(key.dimension());
        labelOverlay.setMinZoom(minZoom);
        labelOverlay.setMaxZoom(collectionMaxZoom);
        labelOverlay.setOverlayGroupName("collection");
        labelOverlay.setTextProperties(textProperties.setMinZoom(minZoom).setMaxZoom(collectionMaxZoom)).setLabel(metrics.label());
        labelOverlays.add(labelOverlay);
    }

    private @Nullable String getEffectiveCollectionName() {
        if (collection == null) {
            return null;
        }

        String name = collection.getName().trim();
        return name.isEmpty() ? null : name;
    }

    private CollectionLabelContentMetrics buildLabelContentMetrics(Context.UI ui) {
        boolean nameVisible = resolveNameVisibility(ui);
        boolean ownerVisible = resolveOwnerVisibility(ui);
        boolean bannerVisible = resolveBannerVisibility(ui);
        String effectiveName = nameVisible ? getEffectiveCollectionName() : null;
        String effectiveOwner = ownerVisible && collection != null ? SettingsUserFormatter.getDisplayName(collection.getOwner(), "") : "";
        boolean hasName = effectiveName != null;
        boolean hasOwner = !effectiveOwner.isEmpty();
        boolean hasBanner = bannerVisible && bannerRenderer.hasBanner();
        if (!hasName && !hasOwner && !hasBanner) {
            return CollectionLabelContentMetrics.empty();
        }

        StringBuilder labelBuilder = new StringBuilder();
        int lines = 0;
        int textWidthPx = 0;

        if (hasName) {
            ++lines;
            labelBuilder.append(ChatFormatting.BOLD).append(effectiveName).append(ChatFormatting.RESET);
            textWidthPx = Math.max(textWidthPx,
                    Minecraft.getInstance().font.width(Component.literal(effectiveName).withStyle(ChatFormatting.BOLD)));
        }
        if (hasOwner) {
            ++lines;
            if (!labelBuilder.isEmpty()) {
                labelBuilder.append('\n');
            }
            labelBuilder.append(ChatFormatting.ITALIC).append(effectiveOwner);
            textWidthPx = Math.max(textWidthPx, Minecraft.getInstance().font.width(effectiveOwner));
        }

        int textSize = ClientConfig.COLLECTION_TEXT_SIZE.get();
        int bannerSize = getBannerSize();
        textWidthPx *= textSize;
        int textHeightPx = lines * TEXT_LINE_HEIGHT_PX * textSize;
        int bannerWidthPx = hasBanner ? BANNER_BASE_WIDTH_PX * bannerSize : 0;
        int bannerHeightPx = hasBanner ? BANNER_BASE_HEIGHT_PX * bannerSize : 0;
        int bannerPlacementWidthPx = hasBanner ? bannerHeightPx : 0;
        int rawContentWidthPx = Math.max(textWidthPx, bannerPlacementWidthPx);
        int rawContentHeightPx = textHeightPx + bannerHeightPx;
        int textOffsetY;
        int bannerOffsetY = 0;

        if (hasBanner) {
            int topOffset = rawContentHeightPx / 2;
            textOffsetY = topOffset - textHeightPx / 2;
            bannerOffsetY = topOffset - textHeightPx;
            if (lines == 1) {
                textOffsetY += BANNER_SINGLE_LINE_TEXT_OFFSET_Y;
            }
        } else {
            textOffsetY = lines > 1 ? -(TEXT_LINE_HEIGHT_PX * textSize) / 2 : 0;
        }

        return new CollectionLabelContentMetrics(labelBuilder.toString(), hasBanner,
                bannerWidthPx, bannerHeightPx,
                rawContentWidthPx + LABEL_CONTENT_PADDING_PX,
                rawContentHeightPx + LABEL_CONTENT_PADDING_PX,
                textOffsetY, bannerOffsetY);
    }

    private TextProperties createBaseTextProperties() {
        TextProperties textProperties = new TextProperties()
                .setOpacity(ClientConfig.COLLECTION_TEXT_OPACITY.get().floatValue())
                .setScale(ClientConfig.COLLECTION_TEXT_SIZE.get())
                .setBackgroundOpacity(0.f);
        int collectionColor = collection == null ? ColorConstants.WHITE : collection.getColor();
        switch (ClientConfig.COLLECTION_TEXT_COLOR.get()) {
            case TextColor.FrontierColor -> textProperties.setColor(collectionColor);
            case TextColor.FrontierColorBright -> textProperties.setColor(colorMaxBrightness(collectionColor));
            case TextColor.White -> textProperties.setColor(ColorConstants.WHITE);
        }
        return textProperties;
    }

    private MapImage createLabelAnchorIcon(CollectionLabelContentMetrics metrics) {
        if (!metrics.hasBanner()) {
            return transparentLabelMarker;
        }

        MapImage bannerIcon = bannerRenderer.createJourneyMapImage(
                metrics.bannerWidthPx() / 2.0,
                metrics.bannerOffsetY(),
                metrics.bannerWidthPx(),
                metrics.bannerHeightPx(),
                ClientConfig.COLLECTION_BANNER_OPACITY.get().floatValue());
        return bannerIcon == null ? transparentLabelMarker : bannerIcon;
    }

    private int getBannerSize() {
        return ClientConfig.COLLECTION_BANNER_SIZE.get();
    }

    private void refreshBannerRenderer() {
        bannerRenderer.releaseTexture();
        if (hasVisibleBannerOnAnyUi() && collection != null && collection.getBannerData() != null) {
            bannerRenderer.createTexture(collection.getId(), collection.getBannerData());
        }
    }

    private static ShapeProperties createHighlightShapeProperties() {
        return new ShapeProperties()
                .setStrokeWidth(HIGHLIGHT_STROKE_WIDTH)
                .setStrokeColor(ColorConstants.WHITE)
                .setStrokeOpacity(1.f)
                .setStrokePosition(ShapeProperties.StrokePosition.OUTSIDE)
                .setFillOpacity(0.f);
    }

    private void refreshEffectiveVisibility() {
        CollectionVisibilityData collectionVisibility = collection == null ? new CollectionVisibilityData() : collection.getVisibilityData();
        CollectionVisibilityData updatedVisibility = new CollectionVisibilityData(collectionVisibility);
        if (visibilityOverrideMask.isVisible()) {
            updatedVisibility.setVisible(visibilityOverrideData.isVisible());
        }
        if (visibilityOverrideMask.getFullscreenZoom()) {
            updatedVisibility.setFullscreenZoom(visibilityOverrideData.getFullscreenZoom());
        }
        if (visibilityOverrideMask.getMinimapZoom()) {
            updatedVisibility.setMinimapZoom(visibilityOverrideData.getMinimapZoom());
        }
        if (visibilityOverrideMask.getWebmapZoom()) {
            updatedVisibility.setWebmapZoom(visibilityOverrideData.getWebmapZoom());
        }
        if (visibilityOverrideMask.getFullscreenName()) {
            updatedVisibility.setFullscreenName(visibilityOverrideData.getFullscreenName());
        }
        if (visibilityOverrideMask.getFullscreenOwner()) {
            updatedVisibility.setFullscreenOwner(visibilityOverrideData.getFullscreenOwner());
        }
        if (visibilityOverrideMask.getFullscreenBanner()) {
            updatedVisibility.setFullscreenBanner(visibilityOverrideData.getFullscreenBanner());
        }
        if (visibilityOverrideMask.getMinimapName()) {
            updatedVisibility.setMinimapName(visibilityOverrideData.getMinimapName());
        }
        if (visibilityOverrideMask.getMinimapOwner()) {
            updatedVisibility.setMinimapOwner(visibilityOverrideData.getMinimapOwner());
        }
        if (visibilityOverrideMask.getMinimapBanner()) {
            updatedVisibility.setMinimapBanner(visibilityOverrideData.getMinimapBanner());
        }
        if (visibilityOverrideMask.getWebmapName()) {
            updatedVisibility.setWebmapName(visibilityOverrideData.getWebmapName());
        }
        if (visibilityOverrideMask.getWebmapOwner()) {
            updatedVisibility.setWebmapOwner(visibilityOverrideData.getWebmapOwner());
        }
        if (visibilityOverrideMask.getWebmapBanner()) {
            updatedVisibility.setWebmapBanner(visibilityOverrideData.getWebmapBanner());
        }
        effectiveVisibilityData = updatedVisibility;
    }

    private boolean resolveVisibility(Context.UI ui) {
        if (jmAPI == null) {
            return effectiveVisibilityData.isVisible();
        }
        return ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_VISIBILITY.get(), effectiveVisibilityData.isVisible());
    }

    private int resolveZoom(Context.UI ui) {
        if (jmAPI == null) {
            return getZoom(ui);
        }

        return switch (ui) {
            case Fullscreen -> ClientConfig.COLLECTION_FULLSCREEN_ZOOM_FORCED.get()
                    ? CollectionVisibilityData.normalizeZoom(ClientConfig.COLLECTION_FULLSCREEN_ZOOM.get())
                    : effectiveVisibilityData.getFullscreenZoom();
            case Minimap -> ClientConfig.COLLECTION_MINIMAP_ZOOM_FORCED.get()
                    ? CollectionVisibilityData.normalizeZoom(ClientConfig.COLLECTION_MINIMAP_ZOOM.get())
                    : effectiveVisibilityData.getMinimapZoom();
            case Webmap -> ClientConfig.COLLECTION_WEBMAP_ZOOM_FORCED.get()
                    ? CollectionVisibilityData.normalizeZoom(ClientConfig.COLLECTION_WEBMAP_ZOOM.get())
                    : effectiveVisibilityData.getWebmapZoom();
            default -> CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM;
        };
    }

    private boolean resolveNameVisibility(Context.UI ui) {
        if (jmAPI == null) {
            return getNameVisibility(ui);
        }

        return switch (ui) {
            case Fullscreen -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_FULLSCREEN_NAME_VISIBILITY.get(), effectiveVisibilityData.getFullscreenName());
            case Minimap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_MINIMAP_NAME_VISIBILITY.get(), effectiveVisibilityData.getMinimapName());
            case Webmap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_WEBMAP_NAME_VISIBILITY.get(), effectiveVisibilityData.getWebmapName());
            default -> false;
        };
    }

    private boolean resolveOwnerVisibility(Context.UI ui) {
        if (jmAPI == null) {
            return getOwnerVisibility(ui);
        }

        return switch (ui) {
            case Fullscreen -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_FULLSCREEN_OWNER_VISIBILITY.get(), effectiveVisibilityData.getFullscreenOwner());
            case Minimap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_MINIMAP_OWNER_VISIBILITY.get(), effectiveVisibilityData.getMinimapOwner());
            case Webmap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_WEBMAP_OWNER_VISIBILITY.get(), effectiveVisibilityData.getWebmapOwner());
            default -> false;
        };
    }

    private boolean resolveBannerVisibility(Context.UI ui) {
        if (jmAPI == null) {
            return getBannerVisibility(ui);
        }

        return switch (ui) {
            case Fullscreen -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_FULLSCREEN_BANNER_VISIBILITY.get(), effectiveVisibilityData.getFullscreenBanner());
            case Minimap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_MINIMAP_BANNER_VISIBILITY.get(), effectiveVisibilityData.getMinimapBanner());
            case Webmap -> ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_WEBMAP_BANNER_VISIBILITY.get(), effectiveVisibilityData.getWebmapBanner());
            default -> false;
        };
    }

    private int getZoom(Context.UI ui) {
        return switch (ui) {
            case Fullscreen -> effectiveVisibilityData.getFullscreenZoom();
            case Minimap -> effectiveVisibilityData.getMinimapZoom();
            case Webmap -> effectiveVisibilityData.getWebmapZoom();
            default -> CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM;
        };
    }

    private boolean getNameVisibility(Context.UI ui) {
        return switch (ui) {
            case Fullscreen -> effectiveVisibilityData.getFullscreenName();
            case Minimap -> effectiveVisibilityData.getMinimapName();
            case Webmap -> effectiveVisibilityData.getWebmapName();
            default -> false;
        };
    }

    private boolean getOwnerVisibility(Context.UI ui) {
        return switch (ui) {
            case Fullscreen -> effectiveVisibilityData.getFullscreenOwner();
            case Minimap -> effectiveVisibilityData.getMinimapOwner();
            case Webmap -> effectiveVisibilityData.getWebmapOwner();
            default -> false;
        };
    }

    private boolean getBannerVisibility(Context.UI ui) {
        return switch (ui) {
            case Fullscreen -> effectiveVisibilityData.getFullscreenBanner();
            case Minimap -> effectiveVisibilityData.getMinimapBanner();
            case Webmap -> effectiveVisibilityData.getWebmapBanner();
            default -> false;
        };
    }

    private boolean hasVisibleBannerOnAnyUi() {
        for (Context.UI ui : getSupportedUis()) {
            if (resolveVisibility(ui) && resolveBannerVisibility(ui)) {
                return true;
            }
        }

        return false;
    }

    private List<Context.MapType> getMapTypesForUi(Context.UI ui) {
        List<Context.MapType> mapTypes = new ArrayList<>(ALL_MAP_TYPES.length);
        for (Context.MapType mapType : ALL_MAP_TYPES) {
            if (isMapTypeEnabledOnUi(ui, mapType)) {
                mapTypes.add(mapType);
            }
        }

        return List.copyOf(mapTypes);
    }

    private boolean isMapTypeEnabledOnUi(Context.UI ui, Context.MapType mapType) {
        if (jmAPI == null) {
            return true;
        }

        return switch (ui) {
            case Fullscreen -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), true);
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), true);
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(), true);
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), true);
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), true);
            };
            case Minimap -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_DAY_VISIBILITY.get(), true);
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_NIGHT_VISIBILITY.get(), true);
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get(), true);
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_TOPO_VISIBILITY.get(), true);
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_BIOME_VISIBILITY.get(), true);
            };
            case Webmap -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_DAY_VISIBILITY.get(), true);
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_NIGHT_VISIBILITY.get(), true);
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get(), true);
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_TOPO_VISIBILITY.get(), true);
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_BIOME_VISIBILITY.get(), true);
            };
            default -> false;
        };
    }

    private Context.UI[] getSupportedUis() {
        if (jmAPI == null) {
            return new Context.UI[]{Context.UI.Fullscreen};
        }

        return new Context.UI[]{Context.UI.Fullscreen, Context.UI.Minimap, Context.UI.Webmap};
    }

    private void hideMarkerOverlays(List<MarkerOverlay> overlays) {
        for (MarkerOverlay marker : overlays) {
            removeMarkerOverlay(marker);
        }
    }

    private void showMarkerOverlaysQuietly(List<MarkerOverlay> overlays) {
        try {
            showMarkerOverlays(overlays);
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Error showing collection label overlays", e);
        }
    }

    private void showMarkerOverlays(List<MarkerOverlay> overlays) throws Exception {
        if (jmAPI == null) {
            return;
        }

        for (MarkerOverlay marker : overlays) {
            jmAPI.show(marker);
        }
    }

    private void hidePolygonOverlays(List<PolygonOverlay> overlays) {
        for (PolygonOverlay polygon : overlays) {
            removePolygonOverlay(polygon);
        }
    }

    private void showPolygonOverlaysQuietly(List<PolygonOverlay> overlays) {
        try {
            showPolygonOverlays(overlays);
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Error showing collection highlight overlays", e);
        }
    }

    private void showPolygonOverlays(List<PolygonOverlay> overlays) throws Exception {
        if (jmAPI == null) {
            return;
        }

        for (PolygonOverlay polygon : overlays) {
            jmAPI.show(polygon);
        }
    }

    private void removePolygonOverlay(PolygonOverlay polygon) {
        if (jmAPI == null) {
            return;
        }

        try {
            jmAPI.remove(polygon);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to remove collection highlight overlay for {}", key, t);
        }
    }

    private void removeMarkerOverlay(MarkerOverlay marker) {
        if (jmAPI == null) {
            return;
        }

        try {
            jmAPI.remove(marker);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to remove collection label overlay for {}", key, t);
        }
    }

    private static int colorMaxBrightness(int color) {
        float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        return Color.HSBtoRGB(hsv[0], hsv[1], 1.f);
    }

    private static MapImage createTransparentLabelMarker() {
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixel(0, 0, 0);
        MapImage mapImage = new MapImage(image);
        mapImage.setAnchorX(0.5);
        mapImage.setAnchorY(0.5);
        mapImage.setDisplayWidth(1);
        mapImage.setDisplayHeight(1);
        mapImage.setOpacity(0.f);
        return mapImage;
    }

    private List<VisibleMemberGeometry> resolveVisibleMemberGeometries(Context.UI ui, Context.MapType mapType, int collectionMaxZoom) {
        List<VisibleMemberGeometry> visibleMembers = new ArrayList<>();

        for (FrontierOverlay frontier : memberFrontiers) {
            if (!frontier.isVisibleOnMap(ui, mapType)) {
                continue;
            }

            FrontierOverlay.CollectionGeometrySnapshot snapshot = frontier.getCollectionGeometrySnapshot();
            if (snapshot == null || snapshot.isEmpty()) {
                continue;
            }

            List<FrontierOverlay.CollectionGeometryIslandSnapshot> visibleIslands = snapshot.getIslands().stream()
                    .filter(island -> island.getMinZoom() <= collectionMaxZoom)
                    .toList();
            if (visibleIslands.isEmpty()) {
                continue;
            }

            visibleMembers.add(new VisibleMemberGeometry(frontier, visibleIslands));
        }

        return List.copyOf(visibleMembers);
    }

    private @Nullable CollectionVisibilityVariant buildVisibleVariant(VisibleVariantBuilder builder) {
        Area unionArea = new Area();
        List<CollectionSourceIsland> sourceIslands = new ArrayList<>();

        for (VisibleMemberGeometry memberGeometry : builder.visibleMemberGeometries()) {
            for (FrontierOverlay.CollectionGeometryIslandSnapshot islandSnapshot : memberGeometry.islands()) {
                Area islandArea = islandSnapshot.copyEffectiveArea();
                if (islandArea.isEmpty()) {
                    continue;
                }

                unionArea.add(new Area(islandArea));
                sourceIslands.add(new CollectionSourceIsland(islandArea, islandSnapshot.getMinZoom()));
            }
        }

        if (unionArea.isEmpty() || sourceIslands.isEmpty()) {
            return null;
        }

        List<CollectionGeometryIsland> islands = extractGeometryIslands(unionArea, sourceIslands);
        return new CollectionVisibilityVariant(builder.ui(), builder.mapTypes(), islands);
    }

    private List<CollectionHighlightRenderGeometry> rebuildHighlightRenderGeometries() {
        if (collection == null || memberFrontiers.isEmpty()) {
            return List.of();
        }

        Area unionArea = new Area();
        List<CollectionSourceIsland> sourceIslands = new ArrayList<>();

        for (FrontierOverlay frontier : memberFrontiers) {
            FrontierOverlay.CollectionGeometrySnapshot snapshot = frontier.getCollectionGeometrySnapshot();
            if (snapshot == null || snapshot.isEmpty()) {
                continue;
            }

            for (FrontierOverlay.CollectionGeometryIslandSnapshot islandSnapshot : snapshot.getIslands()) {
                Area islandArea = islandSnapshot.copyEffectiveArea();
                if (islandArea.isEmpty()) {
                    continue;
                }

                unionArea.add(new Area(islandArea));
                sourceIslands.add(new CollectionSourceIsland(islandArea, islandSnapshot.getMinZoom()));
            }
        }

        if (unionArea.isEmpty() || sourceIslands.isEmpty()) {
            return List.of();
        }

        List<CollectionGeometryIsland> islands = extractGeometryIslands(unionArea, sourceIslands);
        List<CollectionHighlightRenderGeometry> geometries = new ArrayList<>(islands.size());
        for (CollectionGeometryIsland island : islands) {
            CollectionHighlightRenderGeometry geometry = buildHighlightRenderGeometry(island);
            if (geometry != null) {
                geometries.add(geometry);
            }
        }

        return List.copyOf(geometries);
    }

    private static VisibleVariantBuilder findVariantBuilder(List<VisibleVariantBuilder> builders, Context.UI ui, List<FrontierOverlay> visibleFrontiers) {
        for (VisibleVariantBuilder builder : builders) {
            if (builder.ui() == ui && builder.visibleFrontiers().equals(visibleFrontiers)) {
                return builder;
            }
        }

        return null;
    }

    private static List<CollectionGeometryIsland> extractGeometryIslands(Area unionArea, List<CollectionSourceIsland> sourceIslands) {
        List<Area> extractedAreas = extractRegionAreas(unionArea);
        List<CollectionGeometryIsland> islands = new ArrayList<>();

        for (Area extractedArea : extractedAreas) {
            if (extractedArea.isEmpty()) {
                continue;
            }

            int minZoom = Integer.MAX_VALUE;
            for (CollectionSourceIsland sourceIsland : sourceIslands) {
                if (intersects(extractedArea, sourceIsland.effectiveArea())) {
                    minZoom = Math.min(minZoom, sourceIsland.minZoom());
                }
            }

            if (minZoom != Integer.MAX_VALUE) {
                islands.add(new CollectionGeometryIsland(extractedArea, minZoom));
            }
        }

        return List.copyOf(islands);
    }

    private static List<Area> extractRegionAreas(Area area) {
        List<RingPath> rings = extractRingPaths(area);
        if (rings.isEmpty()) {
            return List.of();
        }

        double outerSign = resolveOuterRingSign(rings);
        List<Area> islandAreas = new ArrayList<>();

        for (RingPath ring : rings) {
            if (!hasSameSign(ring.signedArea(), outerSign)) {
                continue;
            }

            Area islandArea = new Area(ring.path());
            Rectangle2D outerBounds = islandArea.getBounds2D();
            for (RingPath holeCandidate : rings) {
                if (hasSameSign(holeCandidate.signedArea(), outerSign)) {
                    continue;
                }

                Rectangle2D holeBounds = holeCandidate.area().getBounds2D();
                if (outerBounds.contains(holeBounds)) {
                    islandArea.subtract(new Area(holeCandidate.path()));
                }
            }

            if (!islandArea.isEmpty()) {
                islandAreas.add(islandArea);
            }
        }

        return List.copyOf(islandAreas);
    }

    private static @Nullable CollectionHighlightRenderGeometry buildHighlightRenderGeometry(CollectionGeometryIsland island) {
        List<RingPath> rings = extractRingPaths(island.copyEffectiveArea());
        if (rings.isEmpty()) {
            return null;
        }

        double outerSign = resolveOuterRingSign(rings);
        RingPath outerRing = null;
        List<MapPolygon> holes = new ArrayList<>();

        for (RingPath ring : rings) {
            if (hasSameSign(ring.signedArea(), outerSign)) {
                if (outerRing == null || Math.abs(ring.signedArea()) > Math.abs(outerRing.signedArea())) {
                    outerRing = ring;
                }
            } else {
                MapPolygon holePolygon = toMapPolygon(ring.points());
                if (holePolygon != null) {
                    holes.add(holePolygon);
                }
            }
        }

        if (outerRing == null) {
            return null;
        }

        MapPolygon outerPolygon = toMapPolygon(outerRing.points());
        if (outerPolygon == null) {
            return null;
        }

        return new CollectionHighlightRenderGeometry(outerPolygon, holes.isEmpty() ? null : List.copyOf(holes), island.getMinZoom());
    }

    private static List<RingPath> extractRingPaths(Area area) {
        List<RingPath> rings = new ArrayList<>();
        PathIterator pathIterator = area.getPathIterator(null);
        double[] coords = new double[6];
        Path2D.Double currentPath = null;
        List<Point2D.Double> currentPoints = null;

        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    if (currentPath != null && currentPoints != null) {
                        addRingPath(rings, currentPath, currentPoints);
                    }
                    currentPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
                    currentPath.moveTo(coords[0], coords[1]);
                    currentPoints = new ArrayList<>();
                    currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                }
                case PathIterator.SEG_LINETO -> {
                    if (currentPath == null) {
                        currentPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
                        currentPath.moveTo(coords[0], coords[1]);
                        currentPoints = new ArrayList<>();
                        currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                    } else {
                        currentPath.lineTo(coords[0], coords[1]);
                        if (currentPoints != null) {
                            currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                        }
                    }
                }
                case PathIterator.SEG_CLOSE -> {
                    if (currentPath != null && currentPoints != null) {
                        currentPath.closePath();
                        addRingPath(rings, currentPath, currentPoints);
                        currentPath = null;
                        currentPoints = null;
                    }
                }
            }

            pathIterator.next();
        }

        if (currentPath != null && currentPoints != null) {
            currentPath.closePath();
            addRingPath(rings, currentPath, currentPoints);
        }

        return List.copyOf(rings);
    }

    private static void addRingPath(List<RingPath> rings, Path2D.Double path, List<Point2D.Double> points) {
        if (points.size() < 3) {
            return;
        }

        rings.add(new RingPath((Path2D.Double) path.clone(), new Area(path), List.copyOf(points), computeSignedArea(points)));
    }

    private static double resolveOuterRingSign(List<RingPath> rings) {
        RingPath outerRing = rings.getFirst();
        double maxAreaMagnitude = Math.abs(outerRing.signedArea());

        for (RingPath ring : rings) {
            double areaMagnitude = Math.abs(ring.signedArea());
            if (areaMagnitude > maxAreaMagnitude) {
                outerRing = ring;
                maxAreaMagnitude = areaMagnitude;
            }
        }

        return outerRing.signedArea();
    }

    private static boolean hasSameSign(double value, double reference) {
        return value == 0.0 || reference == 0.0 || value > 0.0 == reference > 0.0;
    }

    private static double computeSignedArea(List<Point2D.Double> points) {
        double area = 0.0;
        for (int i = 0; i < points.size(); ++i) {
            Point2D.Double current = points.get(i);
            Point2D.Double next = points.get((i + 1) % points.size());
            area += current.x * next.y - next.x * current.y;
        }

        return area * 0.5;
    }

    private static boolean intersects(Area first, Area second) {
        Area intersection = new Area(first);
        intersection.intersect(second);
        return !intersection.isEmpty();
    }

    private void refreshHighlightVisibility() {
        hidePolygonOverlays(highlightPolygonOverlays);
        if (!highlighted) {
            return;
        }

        showPolygonOverlaysQuietly(highlightPolygonOverlays);
    }

    private void invalidateHighlightVisibility() {
        highlightVisibilityDirty = true;
        invalidateOverlayRefresh();
    }

    private static @Nullable MapPolygon toMapPolygon(List<Point2D.Double> points) {
        if (points.size() < 3) {
            return null;
        }

        return new MapPolygon(points.stream()
                .map(point -> new BlockPos((int) Math.round(point.x), OVERLAY_Y, (int) Math.round(point.y)))
                .toList());
    }

    private void invalidateOverlayRefresh() {
        if (!needUpdateOverlay && dirtyOverlayListener != null) {
            dirtyOverlayListener.run();
        }
        needUpdateOverlay = true;
    }

    private static final class CollectionVisibilityVariant {
        private final Context.UI ui;
        private final List<Context.MapType> mapTypes;
        private final List<CollectionGeometryIsland> islands;

        public CollectionVisibilityVariant(Context.UI ui, List<Context.MapType> mapTypes, List<CollectionGeometryIsland> islands) {
            this.ui = ui;
            this.mapTypes = List.copyOf(mapTypes);
            this.islands = List.copyOf(islands);
        }

        public Context.UI getUi() {
            return ui;
        }

        public List<Context.MapType> getMapTypes() {
            return mapTypes;
        }

        public List<CollectionGeometryIsland> getIslands() {
            return islands;
        }
    }

    private static final class CollectionGeometryIsland {
        private final Area effectiveArea;
        private final int minZoom;

        public CollectionGeometryIsland(Area effectiveArea, int minZoom) {
            this.effectiveArea = new Area(effectiveArea);
            this.minZoom = minZoom;
        }

        public Area copyEffectiveArea() {
            return new Area(effectiveArea);
        }

        public int getMinZoom() {
            return minZoom;
        }
    }

    private record VisibleMemberGeometry(FrontierOverlay frontier, List<FrontierOverlay.CollectionGeometryIslandSnapshot> islands) {
    }

    private record CollectionSourceIsland(Area effectiveArea, int minZoom) {
    }

    private record RingPath(Path2D.Double path, Area area, List<Point2D.Double> points, double signedArea) {
    }

    private record CollectionHighlightRenderGeometry(MapPolygon polygon,
                                                     @Nullable List<MapPolygon> holes,
                                                     int minZoom) {
    }

    private record CollectionLabelPlacementKey(CollectionGeometryIsland island,
                                               int contentWidthPx,
                                               int contentHeightPx) {
    }

    private record CollectionLabelContentMetrics(String label,
                                                 boolean hasBanner,
                                                 int bannerWidthPx,
                                                 int bannerHeightPx,
                                                 int contentWidthPx,
                                                 int contentHeightPx,
                                                 int textOffsetY,
                                                 int bannerOffsetY) {
        private static CollectionLabelContentMetrics empty() {
            return new CollectionLabelContentMetrics("", false, 0, 0, 0, 0, 0, 0);
        }

        private boolean isEmpty() {
            return label.isEmpty() && !hasBanner;
        }
    }

    private static final class VisibleVariantBuilder {
        private final Context.UI ui;
        private final List<FrontierOverlay> visibleFrontiers;
        private final List<VisibleMemberGeometry> visibleMemberGeometries;
        private final List<Context.MapType> mapTypes = new ArrayList<>();

        private VisibleVariantBuilder(Context.UI ui, List<FrontierOverlay> visibleFrontiers, List<VisibleMemberGeometry> visibleMemberGeometries,
                                      Context.MapType initialMapType) {
            this.ui = ui;
            this.visibleFrontiers = visibleFrontiers;
            this.visibleMemberGeometries = visibleMemberGeometries;
            mapTypes.add(initialMapType);
        }

        public Context.UI ui() {
            return ui;
        }

        public List<FrontierOverlay> visibleFrontiers() {
            return visibleFrontiers;
        }

        public List<VisibleMemberGeometry> visibleMemberGeometries() {
            return visibleMemberGeometries;
        }

        public List<Context.MapType> mapTypes() {
            return List.copyOf(mapTypes);
        }

        public void addMapType(Context.MapType mapType) {
            mapTypes.add(mapType);
        }
    }
}
