package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.client.territory.overlay.MarkerOverlayLayer;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.MarkerOverlayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayPublisher;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRefreshResult;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.common.Context;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Keeps one stable marker slot for every path-label role in every supported UI.
 */
final class PathLabelOverlayLayer {
    private static final List<Context.UI> UI_ORDER = List.of(
            Context.UI.Fullscreen,
            Context.UI.Minimap,
            Context.UI.Webmap);

    enum Role {
        SINGLE,
        START,
        MIDDLE,
        END
    }

    private static final List<Role> ROLE_ORDER = List.of(
            Role.SINGLE,
            Role.START,
            Role.MIDDLE,
            Role.END);

    record UiState(Context.UI ui,
                   boolean enabled,
                   @Nullable MarkerOverlayState single,
                   @Nullable MarkerOverlayState start,
                   @Nullable MarkerOverlayState middle,
                   @Nullable MarkerOverlayState end) {
        UiState {
            Objects.requireNonNull(ui, "ui");
        }

        private @Nullable MarkerOverlayState get(Role role) {
            return switch (role) {
                case SINGLE -> single;
                case START -> start;
                case MIDDLE -> middle;
                case END -> end;
            };
        }
    }

    private final MarkerOverlayLayer markers;

    PathLabelOverlayLayer(String modId, String layerName, OverlayPublisher publisher) {
        markers = new MarkerOverlayLayer(modId, layerName, publisher);
    }

    void reconcile(List<UiState> uiStates, boolean visible, OverlayRefreshResult result) {
        Objects.requireNonNull(uiStates, "uiStates");
        Objects.requireNonNull(result, "result");

        markers.beginReconcile();
        for (Context.UI ui : UI_ORDER) {
            UiState uiState = findUiState(uiStates, ui);
            for (Role role : ROLE_ORDER) {
                MarkerOverlayState state = uiState == null || !uiState.enabled() ? null : uiState.get(role);
                markers.reconcileNext(state, visible, result);
            }
        }
        markers.finishReconcile(result);
    }

    void clear(OverlayRefreshResult result) {
        markers.clear(result);
    }

    List<MarkerOverlay> getOverlays() {
        return markers.getOverlays();
    }

    private static @Nullable UiState findUiState(List<UiState> uiStates, Context.UI ui) {
        for (UiState uiState : uiStates) {
            if (uiState.ui() == ui) {
                return uiState;
            }
        }
        return null;
    }
}
