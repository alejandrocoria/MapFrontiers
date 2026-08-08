package games.alejandrocoria.mapfrontiers.client.territory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannerRendererTest {
    @Test
    void setRotation_changedValue_advancesTextureRevision() {
        BannerRenderer renderer = new BannerRenderer();

        renderer.setRotation(90);

        assertEquals(90, renderer.getRotation());
        assertEquals(1, renderer.getTextureRevision());
    }

    @Test
    void setRotation_sameValue_keepsTextureRevision() {
        BannerRenderer renderer = new BannerRenderer();
        renderer.setRotation(90);
        long revision = renderer.getTextureRevision();

        renderer.setRotation(90);

        assertEquals(revision, renderer.getTextureRevision());
    }
}
