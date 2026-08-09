package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.common.Context;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps optional collection label slots isolated by UI and visibility-variant ordinal.
 */
public final class CollectionLabelOverlayLayer {
    private static final Context.UI[] UI_ORDER = {
            Context.UI.Fullscreen,
            Context.UI.Minimap,
            Context.UI.Webmap
    };

    private final String modId;
    private final String layerName;
    private final OverlayPublisher publisher;
    private final Map<Context.UI, List<MarkerOverlayLayer>> variantLayers = new EnumMap<>(Context.UI.class);
    private final int[] variantCursors = new int[Context.UI.values().length];
    private boolean reconciling;
    private MarkerOverlayLayer activeVariantLayer;

    public CollectionLabelOverlayLayer(String modId, String layerName, OverlayPublisher publisher) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.layerName = Objects.requireNonNull(layerName, "layerName");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    public void beginReconcile() {
        if (reconciling) {
            throw new IllegalStateException("Collection label layer is already being reconciled");
        }
        reconciling = true;
        Arrays.fill(variantCursors, 0);
    }

    public void beginVariant(Context.UI ui) {
        requireReconcile();
        if (activeVariantLayer != null) {
            throw new IllegalStateException("Previous collection label variant is still being reconciled");
        }

        requireSupportedUi(ui);
        List<MarkerOverlayLayer> uiLayers = variantLayers.computeIfAbsent(ui, ignored -> new ArrayList<>());
        int variantOrdinal = variantCursors[ui.ordinal()]++;
        if (variantOrdinal == uiLayers.size()) {
            uiLayers.add(new MarkerOverlayLayer(modId,
                    layerName + "-" + ui.name().toLowerCase(Locale.ROOT) + "-" + variantOrdinal, publisher));
        }

        activeVariantLayer = uiLayers.get(variantOrdinal);
        activeVariantLayer.beginReconcile();
    }

    public void reconcileNext(@Nullable MarkerOverlayState state, OverlayRefreshResult result) {
        if (activeVariantLayer == null) {
            throw new IllegalStateException("Collection label variant is not being reconciled");
        }
        activeVariantLayer.reconcileNext(state, true, Objects.requireNonNull(result, "result"));
    }

    public void finishVariant(OverlayRefreshResult result) {
        if (activeVariantLayer == null) {
            throw new IllegalStateException("Collection label variant is not being reconciled");
        }
        activeVariantLayer.finishReconcile(Objects.requireNonNull(result, "result"));
        activeVariantLayer = null;
    }

    public void finishReconcile(OverlayRefreshResult result) {
        requireReconcile();
        Objects.requireNonNull(result, "result");
        if (activeVariantLayer != null) {
            throw new IllegalStateException("Collection label variant is still being reconciled");
        }

        for (Context.UI ui : UI_ORDER) {
            List<MarkerOverlayLayer> uiLayers = variantLayers.get(ui);
            if (uiLayers == null) {
                continue;
            }

            int retainedVariants = variantCursors[ui.ordinal()];
            for (int index = retainedVariants; index < uiLayers.size(); index++) {
                uiLayers.get(index).clear(result);
            }
            if (retainedVariants < uiLayers.size()) {
                uiLayers.subList(retainedVariants, uiLayers.size()).clear();
            }
            if (uiLayers.isEmpty()) {
                variantLayers.remove(ui);
            }
        }

        reconciling = false;
        Arrays.fill(variantCursors, 0);
    }

    public void clear(OverlayRefreshResult result) {
        if (reconciling) {
            throw new IllegalStateException("Collection label layer cannot be cleared during reconciliation");
        }
        Objects.requireNonNull(result, "result");
        for (List<MarkerOverlayLayer> uiLayers : variantLayers.values()) {
            for (MarkerOverlayLayer layer : uiLayers) {
                layer.clear(result);
            }
        }
        variantLayers.clear();
    }

    public List<MarkerOverlay> getOverlays() {
        List<MarkerOverlay> overlays = new ArrayList<>();
        for (Context.UI ui : UI_ORDER) {
            List<MarkerOverlayLayer> uiLayers = variantLayers.get(ui);
            if (uiLayers == null) {
                continue;
            }
            for (MarkerOverlayLayer layer : uiLayers) {
                overlays.addAll(layer.getOverlays());
            }
        }
        return overlays;
    }

    private void requireReconcile() {
        if (!reconciling) {
            throw new IllegalStateException("Collection label layer is not being reconciled");
        }
    }

    private static void requireSupportedUi(Context.UI ui) {
        Objects.requireNonNull(ui, "ui");
        if (ui != Context.UI.Fullscreen && ui != Context.UI.Minimap && ui != Context.UI.Webmap) {
            throw new IllegalArgumentException("Unsupported collection label UI: " + ui);
        }
    }
}
