package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.platform.Services;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Shared panel for previews that render real frontier overlays through JourneyMap.
 * Widgets are responsible for building the example frontiers; this class owns the
 * common preview pipeline and panel decoration.
 */
@ParametersAreNonnullByDefault
public class FrontierPreviewPanel {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_preview_bg.png");
    private static final int BACKGROUND_TEXTURE_SIZE = 420;

    private final IJourneyMapHelper.ICustomPreviewRenderer customPreviewRenderer;
    private List<FrontierOverlay> frontiers = List.of();

    public FrontierPreviewPanel() {
        customPreviewRenderer = Services.JOURNEYMAP.createCustomPreviewRenderer();
    }

    public void recalculateAndSetFrontiers(List<FrontierOverlay> frontiers) {
        for (FrontierOverlay frontier : frontiers) {
            frontier.recalculateOverlays();
        }

        setFrontiers(frontiers);
    }

    public void refreshRenderer() {
        customPreviewRenderer.setFrontiers(frontiers);
    }

    public void drawPanelBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int sourceSize) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, x, y, 0, 0, width, height, sourceSize,
                sourceSize, BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
    }

    public void drawPanelBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.horizontalLine(x, x + width - 1, y, ColorConstants.OPTION_BORDER);
        graphics.horizontalLine(x, x + width - 1, y + height - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(x, y, y + height - 1, ColorConstants.OPTION_BORDER);
        graphics.verticalLine(x + width - 1, y, y + height - 1, ColorConstants.OPTION_BORDER);
    }

    public void drawPreview(GuiGraphicsExtractor graphics, int x, int y, int size, float scaleFactor) {
        customPreviewRenderer.draw(graphics, Minecraft.getInstance().renderBuffers().bufferSource(), x, y, size, scaleFactor);
    }

    private void setFrontiers(List<FrontierOverlay> frontiers) {
        this.frontiers = List.copyOf(frontiers);
        customPreviewRenderer.setFrontiers(this.frontiers);
    }
}
