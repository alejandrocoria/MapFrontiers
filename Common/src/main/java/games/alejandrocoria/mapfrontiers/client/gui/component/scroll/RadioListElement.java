package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RadioListElement<T> extends ScrollBox.ScrollElement {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/radio_buttons.png");
    private static final int TEXTURE_WIDTH = 22;
    private static final int TEXTURE_HEIGHT = 11;

    private final StringWidget label;
    private final T value;

    public RadioListElement(Font font, Component text, T value) {
        super(200, 15);
        this.label = new StringWidget(text, font).setColor(ColorConstants.SIMPLE_BUTTON_TEXT);
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        label.setX(x + 15);
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        label.setY(y + 2);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        drawRadio(graphics, x + 2, y + 2, selected);

        label.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            return Action.Clicked;
        }

        return Action.None;
    }

    private void drawRadio(GuiGraphicsExtractor graphics, int x, int y, boolean checked) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, checked ? 11 : 0, 0, 11, 11, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
