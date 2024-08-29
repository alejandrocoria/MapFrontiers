package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RadioListElement extends ScrollBox.ScrollElement {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/radio_buttons.png");
    private static final int textureSizeX = 24;
    private static final int textureSizeY = 12;

    private final SimpleLabel label;
    private final int id;

    public RadioListElement(Font font, Component text, int id) {
        super(200, 16);
        this.label = new SimpleLabel(font, x + 20, y + 4, SimpleLabel.Align.Left, text, ColorConstants.SIMPLE_BUTTON_TEXT);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        label.setX(x + 20);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        label.setY(y + 4);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        drawRadio(graphics, x + 2, y + 2, selected);

        label.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            return Action.Clicked;
        }

        return Action.None;
    }

    private void drawRadio(GuiGraphics graphics, int x, int y, boolean checked) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        graphics.blit(texture, x, y, checked ? 12 : 0, 0, 12, 12, textureSizeX, textureSizeY);
    }
}
