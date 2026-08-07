package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OverlayActivationTest {
    @Test
    void of_reorderedAndDuplicateValues_reusesCanonicalArrays() {
        OverlayActivation first = OverlayActivation.of(
                new Context.UI[]{Context.UI.Fullscreen, Context.UI.Webmap},
                new Context.MapType[]{Context.MapType.Day, Context.MapType.Night});
        OverlayActivation second = OverlayActivation.of(
                new Context.UI[]{Context.UI.Webmap, Context.UI.Fullscreen, Context.UI.Webmap},
                new Context.MapType[]{Context.MapType.Night, Context.MapType.Day, Context.MapType.Night});

        assertEquals(first, second);
        assertSame(first.getUis(), second.getUis());
        assertSame(first.getMapTypes(), second.getMapTypes());
    }
}
