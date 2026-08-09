package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerOverlaySlotTest {
    private static final String LAYER = "markers";

    @Test
    void reconcile_newVisibleSlot_createsAndPublishes() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);

        MarkerOverlay overlay = slot.getOverlay();
        assertNotNull(overlay);
        assertSame(overlay, publisher.operations().getFirst().overlay());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
        assertEquals(PublicationState.PUBLISHED, slot.getPublicationState());
    }

    @Test
    void reconcile_identicalState_preservesIdentityWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay firstOverlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);

        assertSame(firstOverlay, slot.getOverlay());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_changedState_reappliesAndPublishesSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay firstOverlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(7, "second"), true, new OverlayRefreshResult(), LAYER);

        assertSame(firstOverlay, slot.getOverlay());
        assertEquals(7, firstOverlay.getPoint().getX());
        assertEquals("marker-7", firstOverlay.getLabel());
        assertEquals(1, publisher.operations().size());
        assertSame(firstOverlay, publisher.operations().getFirst().overlay());
    }

    @Test
    void reconcile_stateClearsOptionalValues_resetsPreviousPresentation() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        TextProperties labeledText = slot.getOverlay().getTextProperties();

        MarkerOverlayState bareState = OverlayTestStates.bareMarker(2, "bare");
        slot.reconcile(bareState, true, new OverlayRefreshResult(), LAYER);

        MarkerOverlay overlay = slot.getOverlay();
        assertEquals(2, overlay.getMinZoom());
        assertEquals(16384, overlay.getMaxZoom());
        assertNull(overlay.getOverlayGroupName());
        assertNull(overlay.getTitle());
        assertNull(overlay.getLabel());
        TextProperties neutralText = overlay.getTextProperties();
        assertNotNull(neutralText);
        assertNotSame(labeledText, neutralText);
        assertEquals(1.f, neutralText.getScale());

        MarkerOverlayState movedState = new MarkerOverlayState(new BlockPos(3, 70, 3),
                bareState.getIcon(), bareState.getVisualKey(), bareState.getDisplayState());
        slot.reconcile(movedState, true, new OverlayRefreshResult(), LAYER);
        assertEquals(3, overlay.getPoint().getX());
        assertSame(neutralText, overlay.getTextProperties());
    }

    @Test
    void reconcile_hideThenShow_preservesSlotAndRepublishes() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        MarkerOverlayState state = OverlayTestStates.marker(1, "first");
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(state, false, new OverlayRefreshResult(), LAYER);
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertEquals(2, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.REMOVE, publisher.operations().get(0).type());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().get(1).type());
    }

    @Test
    void reconcile_changedWhileHidden_appliesLocallyAndShowsOnlyFinalState() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        slot.reconcile(OverlayTestStates.marker(2, "second"), false, new OverlayRefreshResult(), LAYER);
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(4, "final"), false, new OverlayRefreshResult(), LAYER);
        assertEquals(4, slot.getOverlay().getPoint().getX());
        assertTrue(publisher.operations().isEmpty());

        slot.reconcile(OverlayTestStates.marker(4, "final"), true, new OverlayRefreshResult(), LAYER);
        assertEquals(1, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
    }

    @Test
    void reconcile_unavailablePublisher_updatesWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.setAvailable(false);
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);

        slot.reconcile(OverlayTestStates.marker(3, "preview"), true, new OverlayRefreshResult(), LAYER);

        assertEquals(3, slot.getOverlay().getPoint().getX());
        assertEquals(PublicationState.UNPUBLISHED, slot.getPublicationState());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_showAndCleanupFail_marksUnknownAndConvergesLater() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.failNextShow();
        publisher.failNextRemove();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, failedResult, LAYER);

        assertEquals(PublicationState.UNKNOWN, slot.getPublicationState());
        assertTrue(failedResult.isRetryNeeded());
        assertEquals(1, failedResult.getFailureCount(OverlayRefreshResult.Operation.SHOW, LAYER));
        assertEquals(1, failedResult.getFailureCount(OverlayRefreshResult.Operation.REMOVE, LAYER));

        publisher.clearOperations();
        slot.reconcileVisibility(true, new OverlayRefreshResult(), LAYER);
        assertEquals(PublicationState.PUBLISHED, slot.getPublicationState());
        assertEquals(1, publisher.operations().size());
    }

    @Test
    void reconcile_removeFails_retriesRetainedHide() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        MarkerOverlayState state = OverlayTestStates.marker(1, "first");
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        slot.reconcileVisibility(false, failedResult, LAYER);
        assertEquals(PublicationState.UNKNOWN, slot.getPublicationState());
        assertTrue(failedResult.isRetryNeeded());

        slot.reconcileVisibility(false, new OverlayRefreshResult(), LAYER);
        assertEquals(PublicationState.UNPUBLISHED, slot.getPublicationState());
        assertEquals(2, publisher.operations().size());
    }

    @Test
    void reconcileVisibility_uninitializedSlot_doesNotPublish() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);

        slot.reconcileVisibility(true, new OverlayRefreshResult(), LAYER);
        slot.reconcileVisibility(false, new OverlayRefreshResult(), LAYER);

        assertNull(slot.getOverlay());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcileVisibility_publishedSlot_hidesAndShowsSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcileVisibility(false, new OverlayRefreshResult(), LAYER);
        slot.reconcileVisibility(true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertEquals(List.of(FakeOverlayPublisher.OperationType.REMOVE, FakeOverlayPublisher.OperationType.SHOW),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void reconcile_semanticKeysChange_replacesCompleteLabelPresentation() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("mapfrontiers", "textures/test/shared.png");
        MapImage firstIcon = new MapImage(texture, 16, 16);
        TextProperties firstText = new TextProperties().setScale(2).setColor(0x112233).setOffsetY(3);
        slot.reconcile(OverlayTestStates.labelMarker(1, firstIcon, "icon-1", "first", firstText, "text-1"),
                true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        MapImage secondIcon = new MapImage(texture, 16, 16);
        TextProperties secondText = new TextProperties().setScale(3).setColor(0x445566).setOffsetY(7);
        slot.reconcile(OverlayTestStates.labelMarker(1, secondIcon, "icon-2", "", secondText, "text-2"),
                true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertSame(secondIcon, overlay.getIcon());
        assertSame(secondText, overlay.getTextProperties());
        assertEquals("", overlay.getLabel());
        assertEquals(1, publisher.operations().size());

        publisher.clearOperations();
        MapImage equivalentIcon = new MapImage(texture, 16, 16);
        TextProperties equivalentText = new TextProperties().setScale(3).setColor(0x445566).setOffsetY(7);
        slot.reconcile(OverlayTestStates.labelMarker(1, equivalentIcon, "icon-2", "", equivalentText, "text-2"),
                true, new OverlayRefreshResult(), LAYER);

        assertSame(secondIcon, overlay.getIcon());
        assertSame(secondText, overlay.getTextProperties());
        assertTrue(publisher.operations().isEmpty());

        publisher.clearOperations();
        MapImage transparentIcon = new MapImage(
                ResourceLocation.fromNamespaceAndPath("mapfrontiers", "textures/test/transparent.png"), 1, 1);
        slot.reconcile(OverlayTestStates.labelMarker(1, transparentIcon, "transparent", "label", firstText, "text-1"),
                true, new OverlayRefreshResult(), LAYER);

        assertSame(transparentIcon, overlay.getIcon());
        assertSame(firstText, overlay.getTextProperties());
        assertEquals("label", overlay.getLabel());
        assertEquals(1, publisher.operations().size());
    }
}
