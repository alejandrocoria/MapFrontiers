package games.alejandrocoria.mapfrontiers.client.territory.collection;

import com.mojang.blaze3d.platform.NativeImage;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.BannerRenderer;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierLabelPlacementSolver;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.CollectionBorderOverlayLayer;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.CollectionLabelOverlayLayer;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.MarkerOverlayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayActivation;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayDisplayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayPublisher;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayPublishers;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRefreshResult;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRetryLimiter;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.PolygonOverlayLayer;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.PolygonOverlayState;
import games.alejandrocoria.mapfrontiers.client.util.PlayerNameFormatter;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import it.unimi.dsi.fastutil.Pair;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.common.Context;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final CollectionBorderOverlayLayer collectionBorderLayer;
    private final CollectionLabelOverlayLayer collectionLabelLayer;
    private final PolygonOverlayLayer collectionHighlightLayer;
    private final OverlayRetryLimiter overlayRetryLimiter = new OverlayRetryLimiter();
    private @Nullable CollectionData collection;
    private int collectionColorSnapshot;
    private CollectionLabelContentKey collectionLabelContentSnapshot;
    private List<FrontierOverlay> memberFrontiers;
    private CollectionVisibilityData effectiveVisibilityData = new CollectionVisibilityData();
    private CollectionVisibilityData visibilityOverrideData = new CollectionVisibilityData();
    private CollectionVisibilityMask visibilityOverrideMask = new CollectionVisibilityMask();
    private boolean needUpdateOverlay = true;
    private boolean membershipDirty = true;
    private boolean geometryDirty = true;
    private boolean bordersDirty = true;
    private boolean labelsDirty = true;
    private boolean highlighted = false;
    private boolean highlightStructureDirty = true;
    private boolean highlightLayerDirty;
    private boolean highlightVisibilityDirty = true;
    private List<CollectionVisibilityVariant> visibleVariants = List.of();
    private long collectionGeometryRevision;
    private long highlightGeometryRevision;
    private List<CollectionHighlightRenderGeometry> highlightRenderGeometries = List.of();
    private final Map<CollectionLabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache = new HashMap<>();
    private List<CollectionLabelLayoutKey> labelLayoutFingerprint = List.of();
    private final BannerRenderer bannerRenderer = new BannerRenderer();
    private @Nullable BannerData renderedBannerData;
    private @Nullable String previewOwnerDisplayName;
    private @Nullable Runnable dirtyOverlayListener;

    public CollectionOverlay(CollectionOverlayKey key, @Nullable IClientAPI jmAPI, CollectionData collection, List<FrontierOverlay> members) {
        this.key = key;
        this.jmAPI = jmAPI;
        OverlayPublisher overlayPublisher = OverlayPublishers.create(jmAPI);
        collectionBorderLayer = new CollectionBorderOverlayLayer(MapFrontiers.MODID, "collection-border", overlayPublisher);
        collectionLabelLayer = new CollectionLabelOverlayLayer(MapFrontiers.MODID, "collection-label", overlayPublisher);
        collectionHighlightLayer = new PolygonOverlayLayer(MapFrontiers.MODID, "collection-highlight", overlayPublisher);
        this.collection = collection;
        collectionColorSnapshot = collection.getColor();
        collectionLabelContentSnapshot = resolveLabelContentKey(collection);
        memberFrontiers = List.copyOf(members);
        refreshEffectiveVisibility();
    }

    public CollectionOverlayKey getKey() {
        return key;
    }

    public void setPreviewOwnerDisplayName(String ownerDisplayName) {
        previewOwnerDisplayName = ownerDisplayName;
        markOwnerNameDirty();
    }

    public void markOwnerNameDirty(PlayerId playerId) {
        if (collection != null && playerId.equals(collection.getOwner())) {
            markOwnerNameDirty();
        }
    }

    private void markOwnerNameDirty() {
        CollectionLabelContentKey updatedLabelContent = resolveLabelContentKey(collection);
        if (!collectionLabelContentSnapshot.equals(updatedLabelContent)) {
            collectionLabelContentSnapshot = updatedLabelContent;
            labelsDirty = true;
            invalidateOverlayRefresh();
        }
    }

    public List<MarkerOverlay> getLabelOverlays() {
        return collectionLabelLayer.getOverlays();
    }

    public List<PolygonOverlay> getBorderPolygonOverlays() {
        return collectionBorderLayer.getOverlays();
    }

    public void refreshMembersAndCollection(CollectionData collection, List<FrontierOverlay> members) {
        boolean dirty = false;

        CollectionVariantVisibilityKey previousVariantVisibility = resolveVariantVisibilityKey();
        CollectionLabelVisibilityKey previousLabelVisibility = resolveLabelVisibilityKey();
        CollectionLabelContentKey previousLabelContent = collectionLabelContentSnapshot;
        CollectionLabelContentKey updatedLabelContent = resolveLabelContentKey(collection);
        boolean colorChanged = collectionColorSnapshot != collection.getColor();

        this.collection = collection;
        collectionColorSnapshot = collection.getColor();
        collectionLabelContentSnapshot = updatedLabelContent;
        refreshEffectiveVisibility();
        CollectionVariantVisibilityKey updatedVariantVisibility = resolveVariantVisibilityKey();
        CollectionLabelVisibilityKey updatedLabelVisibility = resolveLabelVisibilityKey();

        if (!previousVariantVisibility.equals(updatedVariantVisibility)) {
            geometryDirty = true;
            dirty = true;
        }
        if (colorChanged) {
            bordersDirty = true;
            labelsDirty = true;
            dirty = true;
        }
        if (!previousLabelContent.equals(updatedLabelContent)
                || !previousLabelVisibility.equals(updatedLabelVisibility)) {
            labelsDirty = true;
            dirty = true;
        }

        List<FrontierOverlay> updatedMembers = List.copyOf(members);
        if (!memberFrontiers.equals(updatedMembers)) {
            memberFrontiers = updatedMembers;
            membershipDirty = true;
            geometryDirty = true;
            labelsDirty = true;
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

        CollectionVariantVisibilityKey previousVariantVisibility = resolveVariantVisibilityKey();
        CollectionLabelVisibilityKey previousLabelVisibility = resolveLabelVisibilityKey();

        visibilityOverrideData = newOverrideData;
        visibilityOverrideMask = newOverrideMask;
        refreshEffectiveVisibility();
        CollectionVariantVisibilityKey updatedVariantVisibility = resolveVariantVisibilityKey();
        CollectionLabelVisibilityKey updatedLabelVisibility = resolveLabelVisibilityKey();
        boolean dirty = false;

        if (!previousVariantVisibility.equals(updatedVariantVisibility)) {
            geometryDirty = true;
            dirty = true;
        }
        if (!previousLabelVisibility.equals(updatedLabelVisibility)) {
            labelsDirty = true;
            dirty = true;
        }
        if (dirty) {
            invalidateOverlayRefresh();
        }
    }

    public void processDirtyOverlay() {
        if (needUpdateOverlay) {
            refreshOverlay();
        }
    }

    public void rebuildOverlayNow() {
        membershipDirty = true;
        geometryDirty = true;
        bordersDirty = true;
        labelsDirty = true;
        highlightStructureDirty = true;
        highlightVisibilityDirty = true;
        overlayRetryLimiter.resetForFunctionalInvalidation();
        refreshOverlay();
    }

    void markGeometryDirty() {
        geometryDirty = true;
        labelsDirty = true;
        highlightStructureDirty = true;
        invalidateOverlayRefresh();
    }

    public void setHighlighted(boolean highlighted) {
        if (this.highlighted == highlighted) {
            return;
        }
        this.highlighted = highlighted;
        invalidateHighlightVisibility();
    }

    public void deleted() {
        OverlayRefreshResult result = new OverlayRefreshResult();
        collectionBorderLayer.clear(result);
        collectionLabelLayer.clear(result);
        collectionHighlightLayer.clear(result);
        highlightRenderGeometries = List.of();
        placementCache.clear();
        labelLayoutFingerprint = List.of();
        visibleVariants = List.of();
        bannerRenderer.releaseTexture();
        renderedBannerData = null;
        dirtyOverlayListener = null;
        if (result.hasFailures()) {
            MapFrontiers.LOGGER.error("Failed to remove JourneyMap collection overlays for {}: {}",
                    key, result.getFailureSummaries(), result.getFirstFailure());
        }
    }

    void setDirtyOverlayListener(@Nullable Runnable dirtyOverlayListener) {
        this.dirtyOverlayListener = dirtyOverlayListener;
        if (dirtyOverlayListener != null && needUpdateOverlay) {
            dirtyOverlayListener.run();
        }
    }

    private void refreshOverlay() {
        needUpdateOverlay = false;
        OverlayRefreshResult refreshResult = new OverlayRefreshResult();
        if (membershipDirty || geometryDirty) {
            rebuildVisibleVariants();
            bordersDirty = true;
        }

        if (bordersDirty) {
            rebuildBorderOverlays(refreshResult);
        }

        if (labelsDirty) {
            rebuildLabelOverlays(refreshResult);
            labelsDirty = false;
        }

        if (highlighted && highlightStructureDirty) {
            highlightRenderGeometries = rebuildHighlightRenderGeometries();
            highlightGeometryRevision++;
            highlightStructureDirty = false;
            highlightLayerDirty = true;
        }
        if (highlighted && highlightLayerDirty) {
            rebuildHighlightOverlays(refreshResult);
            highlightLayerDirty = false;
            highlightVisibilityDirty = false;
        } else if (highlightVisibilityDirty) {
            refreshHighlightVisibility(refreshResult);
            highlightVisibilityDirty = false;
        }

        membershipDirty = false;
        geometryDirty = false;
        bordersDirty = false;
        finishOverlayRefresh(refreshResult);
    }

    private void rebuildVisibleVariants() {
        placementCache.clear();
        if (collection == null || memberFrontiers.isEmpty()) {
            visibleVariants = List.of();
            collectionGeometryRevision++;
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
                CollectionVisibilityVariant variant = buildVisibleVariant(builder, collectionMaxZoom);
                if (variant != null) {
                    rebuiltVariants.add(variant);
                }
            }
        }

        visibleVariants = List.copyOf(rebuiltVariants);
        collectionGeometryRevision++;
        labelsDirty = true;
    }

    private void rebuildBorderOverlays(OverlayRefreshResult result) {
        CollectionBorderStyleKey styleKey = resolveCollectionBorderStyleKey();
        ShapeProperties borderShapeProperties = createCollectionBorderShapeProperties(styleKey);
        collectionBorderLayer.beginReconcile();
        for (CollectionVisibilityVariant variant : visibleVariants) {
            collectionBorderLayer.beginVariant(variant.getUi());
            List<CollectionGeometryIsland> islands = variant.getIslands();
            for (int islandOrdinal = 0; islandOrdinal < islands.size(); islandOrdinal++) {
                CollectionGeometryIsland island = islands.get(islandOrdinal);
                CollectionIslandRenderGeometry geometry = island.getRenderGeometry();
                OverlayDisplayState displayState = new OverlayDisplayState(
                        key.dimension(), variant.getActivation(), island.getMinZoom(), variant.getMaxZoom(),
                        0, null, null, null, null, null, null);
                PolygonOverlayState state = new PolygonOverlayState(
                        geometry.polygon(), geometry.holes(), borderShapeProperties,
                        new CollectionGeometryKey(collectionGeometryRevision, islandOrdinal), styleKey, displayState);
                collectionBorderLayer.reconcileNext(state, result);
            }
            collectionBorderLayer.finishVariant(result);
        }
        collectionBorderLayer.finishReconcile(result);
    }

    private void rebuildHighlightOverlays(OverlayRefreshResult result) {
        CollectionHighlightStyleKey styleKey = new CollectionHighlightStyleKey(
                HIGHLIGHT_STROKE_WIDTH, ColorConstants.WHITE, 1.f,
                ShapeProperties.StrokePosition.OUTSIDE, 0.f);
        ShapeProperties highlightShapeProperties = createHighlightShapeProperties(styleKey);
        OverlayActivation activation = OverlayActivation.of(Context.UI.Fullscreen, ALL_MAP_TYPES);
        collectionHighlightLayer.beginReconcile();
        for (int geometryOrdinal = 0; geometryOrdinal < highlightRenderGeometries.size(); geometryOrdinal++) {
            CollectionHighlightRenderGeometry geometry = highlightRenderGeometries.get(geometryOrdinal);
            OverlayDisplayState displayState = new OverlayDisplayState(
                    key.dimension(), activation, geometry.minZoom(), 0,
                    0, null, null, null, null, null, null);
            PolygonOverlayState state = new PolygonOverlayState(
                    geometry.polygon(), geometry.holes(), highlightShapeProperties,
                    new CollectionHighlightGeometryKey(highlightGeometryRevision, geometryOrdinal),
                    styleKey, displayState);
            collectionHighlightLayer.reconcileNext(state, true, result);
        }
        collectionHighlightLayer.finishReconcile(result);
    }

    private void rebuildLabelOverlays(OverlayRefreshResult result) {
        refreshEffectiveBannerRenderer();
        Map<Context.UI, CollectionLabelContentMetrics> metricsByUi = buildLabelMetricsByUi();
        List<CollectionLabelLayoutKey> updatedLayoutFingerprint = buildLabelLayoutFingerprint(metricsByUi);
        if (!labelLayoutFingerprint.equals(updatedLayoutFingerprint)) {
            placementCache.clear();
            labelLayoutFingerprint = updatedLayoutFingerprint;
        }

        Map<CollectionBannerIconKey, MapImage> bannerIconCache = new HashMap<>();
        collectionLabelLayer.beginReconcile();
        for (CollectionVisibilityVariant variant : visibleVariants) {
            CollectionLabelContentMetrics metrics = metricsByUi.get(variant.getUi());
            collectionLabelLayer.beginVariant(variant.getUi());
            for (CollectionGeometryIsland island : variant.getIslands()) {
                collectionLabelLayer.reconcileNext(
                        createLabelState(variant, metrics, island, bannerIconCache), result);
            }
            collectionLabelLayer.finishVariant(result);
        }
        collectionLabelLayer.finishReconcile(result);
    }

    private Map<Context.UI, CollectionLabelContentMetrics> buildLabelMetricsByUi() {
        Map<Context.UI, CollectionLabelContentMetrics> metricsByUi = new EnumMap<>(Context.UI.class);
        for (CollectionVisibilityVariant variant : visibleVariants) {
            metricsByUi.computeIfAbsent(variant.getUi(), this::buildLabelContentMetrics);
        }
        return metricsByUi;
    }

    private List<CollectionLabelLayoutKey> buildLabelLayoutFingerprint(
            Map<Context.UI, CollectionLabelContentMetrics> metricsByUi) {
        List<CollectionLabelLayoutKey> fingerprint = new ArrayList<>(metricsByUi.size());
        for (Context.UI ui : getSupportedUis()) {
            CollectionLabelContentMetrics metrics = metricsByUi.get(ui);
            if (metrics != null) {
                fingerprint.add(new CollectionLabelLayoutKey(
                        ui, metrics.contentWidthPx(), metrics.contentHeightPx()));
            }
        }
        return List.copyOf(fingerprint);
    }

    private @Nullable MarkerOverlayState createLabelState(
            CollectionVisibilityVariant variant,
            CollectionLabelContentMetrics metrics,
            CollectionGeometryIsland island,
            Map<CollectionBannerIconKey, MapImage> bannerIconCache) {
        if (metrics.isEmpty()) {
            return null;
        }

        CollectionLabelPlacementKey placementKey = new CollectionLabelPlacementKey(
                island, metrics.contentWidthPx(), metrics.contentHeightPx());
        FrontierLabelPlacementSolver.LabelPlacement placement = placementCache.computeIfAbsent(placementKey,
                ignored -> {
                    Area effectiveArea = island.copyEffectiveArea();
                    double adaptiveLabelSolverPrecision =
                            FrontierLabelPlacementSolver.getAdaptiveChunkOrCollectionPrecision(
                                    effectiveArea, LABEL_SOLVER_PRECISION);
                    return FrontierLabelPlacementSolver.solve(effectiveArea,
                            metrics.contentWidthPx(), metrics.contentHeightPx(), adaptiveLabelSolverPrecision);
                });
        if (placement.availableWidthBlocks() <= 0.0 || placement.availableHeightBlocks() <= 0.0) {
            return null;
        }

        int minZoom = Math.max(2, island.getMinZoom());
        TextProperties textProperties = createBaseTextProperties()
                .setOffsetY(metrics.textOffsetY())
                .setMinZoom(minZoom)
                .setMaxZoom(variant.getMaxZoom());
        CollectionLabelIconState iconState = createLabelIcon(metrics, bannerIconCache);
        OverlayDisplayState displayState = new OverlayDisplayState(
                key.dimension(), variant.getActivation(), minZoom, variant.getMaxZoom(),
                0, "collection", null, metrics.label(), textProperties,
                CollectionLabelTextKey.from(textProperties), null);
        BlockPos anchor = BlockPos.containing(placement.centerX(), OVERLAY_Y, placement.centerZ());
        return new MarkerOverlayState(anchor, iconState.image(), iconState.visualKey(), displayState);
    }

    private CollectionLabelContentMetrics buildLabelContentMetrics(Context.UI ui) {
        boolean nameVisible = resolveNameVisibility(ui);
        boolean ownerVisible = resolveOwnerVisibility(ui);
        boolean bannerVisible = resolveBannerVisibility(ui);
        String effectiveName = nameVisible ? collectionLabelContentSnapshot.name() : null;
        String effectiveOwner = ownerVisible ? collectionLabelContentSnapshot.owner() : "";
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
        int collectionColor = collectionColorSnapshot;
        switch (ClientConfig.COLLECTION_TEXT_COLOR.get()) {
            case TextColor.FrontierColor -> textProperties.setColor(collectionColor);
            case TextColor.FrontierColorBright -> textProperties.setColor(colorMaxBrightness(collectionColor));
            case TextColor.White -> textProperties.setColor(ColorConstants.WHITE);
        }
        return textProperties;
    }

    private CollectionLabelIconState createLabelIcon(
            CollectionLabelContentMetrics metrics,
            Map<CollectionBannerIconKey, MapImage> bannerIconCache) {
        if (!metrics.hasBanner() || !bannerRenderer.hasBanner()) {
            return new CollectionLabelIconState(transparentLabelMarker, CollectionLabelIconType.TRANSPARENT);
        }

        double anchorX = metrics.bannerWidthPx() / 2.0;
        double anchorY = metrics.bannerOffsetY();
        float opacity = ClientConfig.COLLECTION_BANNER_OPACITY.get().floatValue();
        CollectionBannerIconKey visualKey = new CollectionBannerIconKey(
                bannerRenderer.getTextureRevision(), bannerRenderer.getRotation(), opacity,
                anchorX, anchorY, metrics.bannerWidthPx(), metrics.bannerHeightPx());
        MapImage bannerIcon = bannerIconCache.get(visualKey);
        if (bannerIcon == null) {
            bannerIcon = bannerRenderer.createJourneyMapImage(
                    anchorX, anchorY, metrics.bannerWidthPx(), metrics.bannerHeightPx(), opacity);
            if (bannerIcon == null) {
                return new CollectionLabelIconState(
                        transparentLabelMarker, CollectionLabelIconType.TRANSPARENT);
            }
            bannerIconCache.put(visualKey, bannerIcon);
        }
        return new CollectionLabelIconState(bannerIcon, visualKey);
    }

    private int getBannerSize() {
        return ClientConfig.COLLECTION_BANNER_SIZE.get();
    }

    private void refreshEffectiveBannerRenderer() {
        BannerData effectiveBanner = hasBannerEligibleVariant()
                ? collectionLabelContentSnapshot.banner()
                : null;
        boolean converged = effectiveBanner == null
                ? !bannerRenderer.hasBanner()
                : bannerRenderer.hasBanner() && effectiveBanner.equals(renderedBannerData);
        if (converged) {
            if (effectiveBanner == null) {
                renderedBannerData = null;
            }
            return;
        }

        if (effectiveBanner == null) {
            bannerRenderer.releaseTexture();
            renderedBannerData = null;
        } else {
            bannerRenderer.createTexture(key.collectionId(), effectiveBanner);
            renderedBannerData = bannerRenderer.hasBanner() ? new BannerData(effectiveBanner) : null;
        }
    }

    private boolean hasBannerEligibleVariant() {
        for (CollectionVisibilityVariant variant : visibleVariants) {
            if (resolveBannerVisibility(variant.getUi())) {
                return true;
            }
        }
        return false;
    }

    private static ShapeProperties createHighlightShapeProperties(CollectionHighlightStyleKey styleKey) {
        return new ShapeProperties()
                .setStrokeWidth(styleKey.strokeWidth())
                .setStrokeColor(styleKey.strokeColor())
                .setStrokeOpacity(styleKey.strokeOpacity())
                .setStrokePosition(styleKey.strokePosition())
                .setFillOpacity(styleKey.fillOpacity());
    }

    private CollectionBorderStyleKey resolveCollectionBorderStyleKey() {
        int collectionColor = collection == null ? ColorConstants.WHITE : collection.getColor();
        return new CollectionBorderStyleKey(
                collectionColor,
                ClientConfig.COLLECTION_BORDER_WIDTH.get() / 2.f,
                ClientConfig.COLLECTION_BORDER_OPACITY.get().floatValue(),
                ShapeProperties.StrokePosition.OUTSIDE);
    }

    private static ShapeProperties createCollectionBorderShapeProperties(CollectionBorderStyleKey styleKey) {
        return new ShapeProperties()
                .setStrokeWidth(styleKey.strokeWidth())
                .setStrokeColor(styleKey.strokeColor())
                .setStrokeOpacity(styleKey.strokeOpacity())
                .setStrokePosition(styleKey.strokePosition())
                .setFillOpacity(0.f);
    }

    private void refreshEffectiveVisibility() {
        CollectionVisibilityData collectionVisibility = collection == null ? new CollectionVisibilityData() : collection.getVisibilityData();
        CollectionVisibilityData updatedVisibility = new CollectionVisibilityData(collectionVisibility);
        updatedVisibility.applyOverride(visibilityOverrideData, visibilityOverrideMask);
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
                    ? ClientConfig.getNormalizedCollectionFullscreenZoom()
                    : effectiveVisibilityData.getFullscreenZoom();
            case Minimap -> ClientConfig.COLLECTION_MINIMAP_ZOOM_FORCED.get()
                    ? ClientConfig.getNormalizedCollectionMinimapZoom()
                    : effectiveVisibilityData.getMinimapZoom();
            case Webmap -> ClientConfig.COLLECTION_WEBMAP_ZOOM_FORCED.get()
                    ? ClientConfig.getNormalizedCollectionWebmapZoom()
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

    private CollectionVariantVisibilityKey resolveVariantVisibilityKey() {
        boolean visible = resolveVisibility(Context.UI.Fullscreen);
        if (!visible) {
            return new CollectionVariantVisibilityKey(false, 0, 0, 0);
        }
        return new CollectionVariantVisibilityKey(
                true,
                resolveZoom(Context.UI.Fullscreen),
                jmAPI == null ? CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM : resolveZoom(Context.UI.Minimap),
                jmAPI == null ? CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM : resolveZoom(Context.UI.Webmap));
    }

    private CollectionLabelVisibilityKey resolveLabelVisibilityKey() {
        boolean visible = resolveVisibility(Context.UI.Fullscreen);
        boolean fullscreenEnabled = visible && CollectionVisibilityData.isZoomEnabled(resolveZoom(Context.UI.Fullscreen));
        boolean minimapEnabled = visible && jmAPI != null
                && CollectionVisibilityData.isZoomEnabled(resolveZoom(Context.UI.Minimap));
        boolean webmapEnabled = visible && jmAPI != null
                && CollectionVisibilityData.isZoomEnabled(resolveZoom(Context.UI.Webmap));
        return new CollectionLabelVisibilityKey(
                fullscreenEnabled && resolveNameVisibility(Context.UI.Fullscreen),
                fullscreenEnabled && resolveOwnerVisibility(Context.UI.Fullscreen),
                fullscreenEnabled && resolveBannerVisibility(Context.UI.Fullscreen),
                minimapEnabled && resolveNameVisibility(Context.UI.Minimap),
                minimapEnabled && resolveOwnerVisibility(Context.UI.Minimap),
                minimapEnabled && resolveBannerVisibility(Context.UI.Minimap),
                webmapEnabled && resolveNameVisibility(Context.UI.Webmap),
                webmapEnabled && resolveOwnerVisibility(Context.UI.Webmap),
                webmapEnabled && resolveBannerVisibility(Context.UI.Webmap));
    }

    private CollectionLabelContentKey resolveLabelContentKey(@Nullable CollectionData collection) {
        if (collection == null) {
            return new CollectionLabelContentKey(null, "", null);
        }
        String name = collection.getName().trim();
        BannerData banner = collection.getBannerData();
        // BannerData is mutable, so the semantic baseline must not retain the caller's instance.
        return new CollectionLabelContentKey(name.isEmpty() ? null : name,
                previewOwnerDisplayName == null
                        ? PlayerNameFormatter.getDisplayName(collection.getOwner(), "")
                        : previewOwnerDisplayName,
                banner == null ? null : new BannerData(banner));
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

    private @Nullable CollectionVisibilityVariant buildVisibleVariant(VisibleVariantBuilder builder, int collectionMaxZoom) {
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
        Context.MapType[] mapTypes = builder.mapTypes().toArray(Context.MapType[]::new);
        return new CollectionVisibilityVariant(builder.ui(), mapTypes,
                OverlayActivation.of(builder.ui(), mapTypes), collectionMaxZoom, islands);
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
            geometries.add(buildHighlightRenderGeometry(island));
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

    static List<CollectionGeometryIsland> extractGeometryIslands(Area unionArea, List<CollectionSourceIsland> sourceIslands) {
        List<CollectionRegionGeometry> regions = extractRegionGeometries(unionArea);
        List<Rectangle2D> sourceBounds = new ArrayList<>(sourceIslands.size());
        for (CollectionSourceIsland sourceIsland : sourceIslands) {
            sourceBounds.add(sourceIsland.effectiveArea().getBounds2D());
        }

        List<CollectionGeometryIsland> islands = new ArrayList<>(regions.size());
        for (CollectionRegionGeometry region : regions) {
            Rectangle2D regionBounds = region.effectiveArea().getBounds2D();
            int minZoom = Integer.MAX_VALUE;
            for (int sourceIndex = 0; sourceIndex < sourceIslands.size(); ++sourceIndex) {
                if (!regionBounds.intersects(sourceBounds.get(sourceIndex))) {
                    continue;
                }

                CollectionSourceIsland sourceIsland = sourceIslands.get(sourceIndex);
                if (intersects(region.effectiveArea(), sourceIsland.effectiveArea())) {
                    minZoom = Math.min(minZoom, sourceIsland.minZoom());
                }
            }

            if (minZoom != Integer.MAX_VALUE) {
                islands.add(new CollectionGeometryIsland(region.effectiveArea(), minZoom, region.renderGeometry()));
            }
        }

        return List.copyOf(islands);
    }

    private static List<CollectionRegionGeometry> extractRegionGeometries(Area area) {
        List<RingPath> rings = extractRingPaths(area);
        if (rings.isEmpty()) {
            return List.of();
        }

        double outerSign = resolveOuterRingSign(rings);
        List<RingPath> outerRings = new ArrayList<>();
        List<RingPath> holeRings = new ArrayList<>();
        for (RingPath ring : rings) {
            if (hasSameSign(ring.signedArea(), outerSign)) {
                outerRings.add(ring);
            } else {
                holeRings.add(ring);
            }
        }

        List<RingPath> mergedOuterRings = mergeTouchingRings(outerRings);
        List<RingPath> mergedHoleRings = mergeTouchingRings(holeRings);
        List<CollectionRegionGeometry> regions = new ArrayList<>(mergedOuterRings.size());
        for (RingPath outerRing : mergedOuterRings) {
            Area effectiveArea = new Area(outerRing.path());
            Rectangle2D outerBounds = effectiveArea.getBounds2D();
            List<MapPolygon> holes = new ArrayList<>();
            for (RingPath holeRing : mergedHoleRings) {
                Rectangle2D holeBounds = holeRing.area().getBounds2D();
                if (!outerBounds.contains(holeBounds)) {
                    continue;
                }

                Area holeArea = new Area(holeRing.path());
                Area outsideArea = new Area(holeArea);
                outsideArea.subtract(effectiveArea);
                if (!outsideArea.isEmpty()) {
                    continue;
                }

                effectiveArea.subtract(holeArea);
                MapPolygon holePolygon = toMapPolygon(holeRing.points());
                if (holePolygon != null) {
                    holes.add(holePolygon);
                }
            }

            MapPolygon polygon = toMapPolygon(outerRing.points());
            if (polygon != null && !effectiveArea.isEmpty()) {
                CollectionIslandRenderGeometry renderGeometry = new CollectionIslandRenderGeometry(
                        polygon, holes.isEmpty() ? null : List.copyOf(holes));
                regions.add(new CollectionRegionGeometry(effectiveArea, renderGeometry));
            }
        }

        return List.copyOf(regions);
    }

    static List<RingPath> mergeTouchingRings(List<RingPath> rings) {
        // Area may represent one connected region as non-overlapping subpaths with shared outlines.
        // Removing opposite edge pairs restores the actual boundary without changing its coordinates.
        Set<DirectedEdge> boundaryEdges = new LinkedHashSet<>();
        Map<DirectedEdge, Integer> edgeOwners = new HashMap<>();
        for (int ringIndex = 0; ringIndex < rings.size(); ++ringIndex) {
            RingPath ring = rings.get(ringIndex);
            List<Point2D.Double> points = ring.points();
            for (int i = 0; i < points.size(); ++i) {
                DirectedEdge edge = new DirectedEdge(points.get(i), points.get((i + 1) % points.size()));
                DirectedEdge reverse = edge.reverse();
                if (boundaryEdges.remove(reverse)) {
                    edgeOwners.remove(reverse);
                } else {
                    if (!boundaryEdges.add(edge)) {
                        return List.copyOf(rings);
                    }
                    edgeOwners.put(edge, ringIndex);
                }
            }
        }

        Map<Point2D.Double, List<DirectedEdge>> outgoingEdges = new HashMap<>();
        for (DirectedEdge edge : boundaryEdges) {
            outgoingEdges.computeIfAbsent(edge.from(), ignored -> new ArrayList<>()).add(edge);
        }

        Set<DirectedEdge> remainingEdges = new LinkedHashSet<>(boundaryEdges);
        List<RingPath> mergedRings = new ArrayList<>();
        while (!remainingEdges.isEmpty()) {
            DirectedEdge firstEdge = remainingEdges.iterator().next();
            List<Point2D.Double> points = new ArrayList<>();
            DirectedEdge edge = firstEdge;
            boolean closed = false;
            while (edge != null && remainingEdges.remove(edge)) {
                points.add(edge.from());
                if (edge.to().equals(firstEdge.from())) {
                    closed = true;
                    break;
                }
                edge = nextBoundaryEdge(outgoingEdges.get(edge.to()), remainingEdges,
                        edgeOwners.get(edge), edgeOwners);
            }

            if (!closed) {
                return List.copyOf(rings);
            }
            RingPath mergedRing = createRingPath(points);
            if (mergedRing != null) {
                mergedRings.add(mergedRing);
            }
        }

        return List.copyOf(mergedRings);
    }

    private static @Nullable DirectedEdge nextBoundaryEdge(@Nullable List<DirectedEdge> candidates,
                                                            Set<DirectedEdge> remainingEdges,
                                                            int preferredOwner,
                                                            Map<DirectedEdge, Integer> edgeOwners) {
        if (candidates == null) {
            return null;
        }

        for (DirectedEdge candidate : candidates) {
            if (remainingEdges.contains(candidate) && edgeOwners.get(candidate) == preferredOwner) {
                return candidate;
            }
        }
        for (DirectedEdge candidate : candidates) {
            if (remainingEdges.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static @Nullable RingPath createRingPath(List<Point2D.Double> points) {
        if (points.size() < 3) {
            return null;
        }

        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        Point2D.Double first = points.getFirst();
        path.moveTo(first.x, first.y);
        for (int i = 1; i < points.size(); ++i) {
            Point2D.Double point = points.get(i);
            path.lineTo(point.x, point.y);
        }
        path.closePath();
        return new RingPath(path, new Area(path), List.copyOf(points), computeSignedArea(points));
    }

    private static CollectionHighlightRenderGeometry buildHighlightRenderGeometry(CollectionGeometryIsland island) {
        CollectionIslandRenderGeometry geometry = island.getRenderGeometry();

        return new CollectionHighlightRenderGeometry(geometry.polygon(), geometry.holes(), island.getMinZoom());
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
        double totalSignedArea = 0.0;
        for (RingPath ring : rings) {
            totalSignedArea += ring.signedArea();
        }
        return totalSignedArea != 0.0 ? totalSignedArea : rings.getFirst().signedArea();
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

    private void refreshHighlightVisibility(OverlayRefreshResult result) {
        collectionHighlightLayer.setVisible(highlighted, result);
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
        overlayRetryLimiter.resetForFunctionalInvalidation();
        if (!needUpdateOverlay && dirtyOverlayListener != null) {
            dirtyOverlayListener.run();
        }
        needUpdateOverlay = true;
    }

    private void finishOverlayRefresh(OverlayRefreshResult result) {
        if (!result.hasFailures()) {
            return;
        }

        MapFrontiers.LOGGER.error("Failed to refresh JourneyMap overlays for collection {}: {}",
                key, result.getFailureSummaries(), result.getFirstFailure());
        if (overlayRetryLimiter.consumeRetry(result)) {
            scheduleOverlayRetry();
        }
    }

    private void scheduleOverlayRetry() {
        bordersDirty = true;
        labelsDirty = true;
        highlightLayerDirty = true;
        highlightVisibilityDirty = true;
        needUpdateOverlay = true;
        if (dirtyOverlayListener != null) {
            dirtyOverlayListener.run();
        }
    }

    private static final class CollectionVisibilityVariant {
        private final Context.UI ui;
        private final Context.MapType[] mapTypes;
        private final OverlayActivation activation;
        private final int maxZoom;
        private final List<CollectionGeometryIsland> islands;

        public CollectionVisibilityVariant(Context.UI ui, Context.MapType[] mapTypes,
                                           OverlayActivation activation, int maxZoom,
                                           List<CollectionGeometryIsland> islands) {
            this.ui = ui;
            this.mapTypes = mapTypes;
            this.activation = activation;
            this.maxZoom = maxZoom;
            this.islands = List.copyOf(islands);
        }

        public Context.UI getUi() {
            return ui;
        }

        public Context.MapType[] getMapTypes() {
            return mapTypes;
        }

        public OverlayActivation getActivation() {
            return activation;
        }

        public int getMaxZoom() {
            return maxZoom;
        }

        public List<CollectionGeometryIsland> getIslands() {
            return islands;
        }
    }

    static final class CollectionGeometryIsland {
        private final Area effectiveArea;
        private final int minZoom;
        private final CollectionIslandRenderGeometry renderGeometry;

        public CollectionGeometryIsland(Area effectiveArea, int minZoom,
                                        CollectionIslandRenderGeometry renderGeometry) {
            this.effectiveArea = new Area(effectiveArea);
            this.minZoom = minZoom;
            this.renderGeometry = renderGeometry;
        }

        public Area copyEffectiveArea() {
            return new Area(effectiveArea);
        }

        public int getMinZoom() {
            return minZoom;
        }

        public CollectionIslandRenderGeometry getRenderGeometry() {
            return renderGeometry;
        }
    }

    private record VisibleMemberGeometry(FrontierOverlay frontier, List<FrontierOverlay.CollectionGeometryIslandSnapshot> islands) {
    }

    record CollectionSourceIsland(Area effectiveArea, int minZoom) {
    }

    record RingPath(Path2D.Double path, Area area, List<Point2D.Double> points, double signedArea) {
    }

    private record DirectedEdge(Point2D.Double from, Point2D.Double to) {
        public DirectedEdge reverse() {
            return new DirectedEdge(to, from);
        }
    }

    private record CollectionRegionGeometry(Area effectiveArea, CollectionIslandRenderGeometry renderGeometry) {
    }

    private record CollectionHighlightRenderGeometry(MapPolygon polygon,
                                                     @Nullable List<MapPolygon> holes,
                                                     int minZoom) {
    }

    private record CollectionGeometryKey(long revision, int islandOrdinal) {
    }

    private record CollectionHighlightGeometryKey(long revision, int geometryOrdinal) {
    }

    private record CollectionBorderStyleKey(int strokeColor,
                                            float strokeWidth,
                                            float strokeOpacity,
                                            ShapeProperties.StrokePosition strokePosition) {
    }

    private record CollectionHighlightStyleKey(float strokeWidth,
                                               int strokeColor,
                                               float strokeOpacity,
                                               ShapeProperties.StrokePosition strokePosition,
                                               float fillOpacity) {
    }

    private record CollectionVariantVisibilityKey(boolean visible,
                                                  int fullscreenZoom,
                                                  int minimapZoom,
                                                  int webmapZoom) {
    }

    private record CollectionLabelVisibilityKey(boolean fullscreenName,
                                                boolean fullscreenOwner,
                                                boolean fullscreenBanner,
                                                boolean minimapName,
                                                boolean minimapOwner,
                                                boolean minimapBanner,
                                                boolean webmapName,
                                                boolean webmapOwner,
                                                boolean webmapBanner) {
    }

    private record CollectionLabelContentKey(@Nullable String name,
                                             String owner,
                                             @Nullable BannerData banner) {
    }

    record CollectionIslandRenderGeometry(MapPolygon polygon,
                                          @Nullable List<MapPolygon> holes) {
    }

    private record CollectionLabelPlacementKey(CollectionGeometryIsland island,
                                               int contentWidthPx,
                                               int contentHeightPx) {
    }

    private record CollectionLabelLayoutKey(Context.UI ui,
                                            int contentWidthPx,
                                            int contentHeightPx) {
    }

    private record CollectionLabelTextKey(float scale,
                                          int color,
                                          int backgroundColor,
                                          float opacity,
                                          float backgroundOpacity,
                                          boolean fontShadow,
                                          int minZoom,
                                          int maxZoom,
                                          int offsetX,
                                          int offsetY) {
        private static CollectionLabelTextKey from(TextProperties properties) {
            return new CollectionLabelTextKey(properties.getScale(), properties.getColor(),
                    properties.getBackgroundColor(), properties.getOpacity(), properties.getBackgroundOpacity(),
                    properties.hasFontShadow(), properties.getMinZoom(), properties.getMaxZoom(),
                    properties.getOffsetX(), properties.getOffsetY());
        }
    }

    private enum CollectionLabelIconType {
        TRANSPARENT
    }

    private record CollectionBannerIconKey(long textureRevision,
                                           int rotation,
                                           float opacity,
                                           double anchorX,
                                           double anchorY,
                                           int displayWidth,
                                           int displayHeight) {
    }

    private record CollectionLabelIconState(MapImage image, Object visualKey) {
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
