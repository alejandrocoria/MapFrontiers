package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.PolygonOverlay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonOverlaySlotTest {
    private static final String LAYER = "polygons";

    @Test
    void reconcile_semanticallyIdenticalState_preservesIdentityWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlaySlot slot = new PolygonOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.polygon(0, 1, 2), true, new OverlayRefreshResult(), LAYER);
        PolygonOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.polygon(0, 1, 2), true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_changedGeometryAndStyle_reappliesToSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlaySlot slot = new PolygonOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.polygon(0, 1, 2), true, new OverlayRefreshResult(), LAYER);
        PolygonOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.polygon(10, 2, 3), true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertEquals(10, overlay.getOuterArea().getPoints().get(0).getX());
        assertEquals(1, overlay.getHoles().size());
        assertEquals(3, overlay.getShapeProperties().getStrokeColor() & 0xFFFFFF);
        assertEquals(1, publisher.operations().size());
        assertSame(overlay, publisher.operations().get(0).overlay());
    }
}
