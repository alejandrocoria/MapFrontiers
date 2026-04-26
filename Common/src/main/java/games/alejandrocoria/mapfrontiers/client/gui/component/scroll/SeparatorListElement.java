package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SeparatorListElement extends ScrollBox.ScrollElement {
    private static final int HEIGHT = 9;
    private static final int LINE_Y = 4;

    public SeparatorListElement(int width) {
        super(width, HEIGHT);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        graphics.horizontalLine(x, x + width, y + LINE_Y, ColorConstants.SCROLL_ELEMENT_SEPARATOR);
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return ScrollBox.ScrollElement.Action.None;
    }
}
