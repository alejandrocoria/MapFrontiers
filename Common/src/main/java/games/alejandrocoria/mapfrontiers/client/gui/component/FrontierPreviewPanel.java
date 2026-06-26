package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.platform.Services;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Shared panel for previews that render real frontier overlays through JourneyMap.
 * Widgets are responsible for building the example frontiers; this class owns the
 * common preview pipeline and panel decoration.
 */
@ParametersAreNonnullByDefault
public class FrontierPreviewPanel {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_preview_bg.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 420;

    private final IJourneyMapHelper.ICustomPreviewRenderer customPreviewRenderer;
    private List<FrontierOverlay> frontiers = List.of();
    private List<CollectionOverlay> collections = List.of();

    public FrontierPreviewPanel() {
        customPreviewRenderer = Services.JOURNEYMAP.createCustomPreviewRenderer();
    }

    public void recalculateAndSetFrontiers(List<FrontierOverlay> frontiers) {
        recalculateAndSetTerritories(frontiers, List.of());
    }

    public void recalculateAndSetTerritories(List<FrontierOverlay> frontiers, List<CollectionOverlay> collections) {
        for (FrontierOverlay frontier : frontiers) {
            frontier.recalculateOverlays();
        }

        this.frontiers = List.copyOf(frontiers);
        this.collections = List.copyOf(collections);
        refreshRenderer();
    }

    public void refreshRenderer() {
        customPreviewRenderer.setTerritories(frontiers, collections);
    }

    public void drawPanelBackground(GuiGraphics graphics, int x, int y, int width, int height, int sourceSize) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0, 0, width, height, sourceSize,
                sourceSize, BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
    }

    public void drawPanelBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.submitOutline(x, y, width, height, ColorConstants.PREVIEW_PANEL_BORDER);
    }

    public void drawPreview(GuiGraphics graphics, int x, int y, int size, float scaleFactor) {
        customPreviewRenderer.draw(graphics, Minecraft.getInstance().renderBuffers().bufferSource(), x, y, size, scaleFactor);
    }
}
