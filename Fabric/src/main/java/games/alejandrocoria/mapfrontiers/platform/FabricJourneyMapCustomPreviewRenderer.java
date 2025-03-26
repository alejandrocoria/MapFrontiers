package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapCustomPreviewRenderer;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.client.model.MapState;
import journeymap.client.model.MapType;
import journeymap.client.render.draw.DrawMarkerStep;
import journeymap.client.render.draw.DrawPolygonStep;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.MapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class FabricJourneyMapCustomPreviewRenderer implements IJourneyMapCustomPreviewRenderer {
    private final MapRenderer mapRenderer;

    public FabricJourneyMapCustomPreviewRenderer() {

        mapRenderer = new MapRenderer(Context.UI.Fullscreen);
        mapRenderer.setZoom(512);
        mapRenderer.setViewPortBounds(null);
        MapState mapState = new MapState();
        mapState.setMapType(MapType.day(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"))));
        mapRenderer.setContext(mapState);
        mapRenderer.center(mapState.getWorldDir(), mapState.getMapType(), 0, 0, 512);
    }

    @Override
    public void draw(GuiGraphics graphics, FrontierOverlay frontierOverlay) {
        List<DrawStep> drawSteps = new ArrayList<>();
        for (PolygonOverlay polygon : frontierOverlay.getPolygonOverlays()) {
            drawSteps.add(new DrawPolygonStep(polygon));
        }
        for (MarkerOverlay banner : frontierOverlay.getBannerOverlays()) {
            drawSteps.add(new DrawMarkerStep(banner));
        }

        if (drawSteps.isEmpty()) {
            return;
        }

        int width = Minecraft.getInstance().getWindow().getScreenWidth();
        int height = Minecraft.getInstance().getWindow().getScreenHeight();
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        DrawUtil.sizeDisplay(width, height);

        for(DrawStep.Pass pass : DrawStep.Pass.values()) {
            int zLevel = 0;

            for (DrawStep drawStep : drawSteps) {
                ++zLevel;
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, zLevel);
                drawStep.draw(graphics, graphics.bufferSource(), pass, 0, 0, mapRenderer, 1, 0);
                graphics.pose().popPose();
            }
        }

        DrawUtil.sizeDisplay(width / guiScale, height / guiScale);
    }
}
