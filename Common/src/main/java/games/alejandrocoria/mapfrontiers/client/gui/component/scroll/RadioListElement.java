package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RadioListElement extends ScrollBox.ScrollElement {
    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/radio_buttons.png");
    private static final int textureSizeX = 24;
    private static final int textureSizeY = 12;

    private final StringWidget label;
    private final int id;

    public RadioListElement(Font font, Component text, int id) {
        super(200, 16);
        this.label = new StringWidget(text, font).setColor(ColorConstants.SIMPLE_BUTTON_TEXT);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        label.setX(x + 20);
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        label.setY(y + 4);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        drawRadio(graphics, x + 2, y + 2, selected);

        label.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            return Action.Clicked;
        }

        return Action.None;
    }

    private void drawRadio(GuiGraphics graphics, int x, int y, boolean checked) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, checked ? 12 : 0, 0, 12, 12, textureSizeX, textureSizeY);
    }
}
