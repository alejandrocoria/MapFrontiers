package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SectionHeaderListElement extends ScrollBox.ScrollElement {
    private static final int HEIGHT = 13;

    private final Font font;
    private final int color;
    private final String title;
    private final int titleX;

    public SectionHeaderListElement(Font font, String title, int width, int color) {
        super(width, HEIGHT);
        this.font = font;
        this.color = color;
        this.title = title;
        this.titleX = (width - font.width(title)) / 2;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        graphics.text(font, title, x + titleX, y + 3, color);
    }

    @Override
    protected boolean isKeyboardFocusable() {
        return false;
    }
}
