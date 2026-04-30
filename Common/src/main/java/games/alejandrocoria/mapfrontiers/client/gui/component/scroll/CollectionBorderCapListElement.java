package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectionBorderCapListElement extends ScrollBox.ScrollElement {
    private static final int HEIGHT = 2;

    private final int color;

    public CollectionBorderCapListElement(int width, int color) {
        super(width, HEIGHT);
        this.color = color;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    @Override
    protected boolean isKeyboardFocusable() {
        return false;
    }
}
