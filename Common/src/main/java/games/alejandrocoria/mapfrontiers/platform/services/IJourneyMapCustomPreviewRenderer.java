package games.alejandrocoria.mapfrontiers.platform.services;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import net.minecraft.client.gui.GuiGraphics;

public interface IJourneyMapCustomPreviewRenderer {
    void draw(GuiGraphics graphics, FrontierOverlay frontierOverlay);
}
