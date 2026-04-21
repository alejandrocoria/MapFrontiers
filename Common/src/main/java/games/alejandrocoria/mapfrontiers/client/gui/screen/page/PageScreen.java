package games.alejandrocoria.mapfrontiers.client.gui.screen.page;

import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class PageScreen extends AutoScaledScreen {
    public PageScreen(Component title, int minWidth, int minHeight) {
        super(title, minWidth, minHeight, BottomButtonsMode.Floating);
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics);
    }
}
