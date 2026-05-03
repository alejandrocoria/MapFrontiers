package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class PageScreen extends AutoScaledScreen {
    public PageScreen(Component title) {
        super(title, BottomButtonsMode.Floating);
    }

    @Override
    protected int getMinimumLayoutExtraWidth() {
        return (LayoutConstants.PAGE_MARGIN + LayoutConstants.BOX_PADDING) * 2;
    }

    @Override
    protected int getMinimumLayoutExtraHeight() {
        return (LayoutConstants.PAGE_MARGIN + LayoutConstants.BOX_PADDING) * 2;
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics);
    }
}
