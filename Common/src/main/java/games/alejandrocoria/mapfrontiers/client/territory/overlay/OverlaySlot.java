package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Displayable;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class OverlaySlot<O extends Displayable> {
    private final OverlayPublisher publisher;
    private @Nullable O overlay;
    private PublicationState publicationState = PublicationState.UNPUBLISHED;

    protected OverlaySlot(OverlayPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    public @Nullable O getOverlay() {
        return overlay;
    }

    protected final void initializeOverlay(O overlay) {
        if (this.overlay != null) {
            throw new IllegalStateException("Overlay slot is already initialized");
        }
        this.overlay = Objects.requireNonNull(overlay, "overlay");
    }

    protected final void reconcilePublication(boolean visible, boolean stateChanged,
                                              OverlayRefreshResult result, String layer) {
        O currentOverlay = requireOverlay();
        if (!publisher.isAvailable()) {
            return;
        }

        if (visible) {
            if (stateChanged || publicationState != PublicationState.PUBLISHED) {
                show(currentOverlay, result, layer);
            }
        } else if (publicationState != PublicationState.UNPUBLISHED) {
            removeRetained(currentOverlay, result, layer);
        }
    }

    public final void reconcileVisibility(boolean visible, OverlayRefreshResult result, String layer) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(layer, "layer");
        if (overlay != null) {
            reconcilePublication(visible, false, result, layer);
        }
    }

    public final void retire(OverlayRefreshResult result, String layer) {
        if (overlay == null) {
            return;
        }

        if (publisher.isAvailable() && publicationState != PublicationState.UNPUBLISHED) {
            try {
                publisher.remove(overlay);
            } catch (Exception failure) {
                result.recordFailure(OverlayRefreshResult.Operation.REMOVE, layer, failure, false);
            }
        }

        overlay = null;
        publicationState = PublicationState.UNPUBLISHED;
        clearDesiredState();
    }

    protected abstract void clearDesiredState();

    PublicationState getPublicationState() {
        return publicationState;
    }

    private void show(O currentOverlay, OverlayRefreshResult result, String layer) {
        try {
            publisher.show(currentOverlay);
            publicationState = PublicationState.PUBLISHED;
        } catch (Exception showFailure) {
            result.recordFailure(OverlayRefreshResult.Operation.SHOW, layer, showFailure, true);
            // A failed update may have partially registered the displayable, so remove the same identity before retrying.
            try {
                publisher.remove(currentOverlay);
                publicationState = PublicationState.UNPUBLISHED;
            } catch (Exception cleanupFailure) {
                publicationState = PublicationState.UNKNOWN;
                result.recordFailure(OverlayRefreshResult.Operation.REMOVE, layer, cleanupFailure, true);
            }
        }
    }

    private void removeRetained(O currentOverlay, OverlayRefreshResult result, String layer) {
        try {
            publisher.remove(currentOverlay);
            publicationState = PublicationState.UNPUBLISHED;
        } catch (Exception failure) {
            publicationState = PublicationState.UNKNOWN;
            result.recordFailure(OverlayRefreshResult.Operation.REMOVE, layer, failure, true);
        }
    }

    private O requireOverlay() {
        if (overlay == null) {
            throw new IllegalStateException("Overlay slot has not been initialized");
        }
        return overlay;
    }
}
