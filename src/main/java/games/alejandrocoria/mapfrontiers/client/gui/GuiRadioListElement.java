package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiRadioListElement extends GuiScrollBox.ScrollElement {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/radio_buttons.png");
    private static final int textureSizeX = 24;
    private static final int textureSizeY = 12;

    private GuiSimpleLabel label;
    int id;

    public GuiRadioListElement(FontRenderer font, TextComponent text, int id) {
        super(200, 16);
        this.label = new GuiSimpleLabel(font, x + 20, y + 4, GuiSimpleLabel.Align.Left, text, GuiColors.SETTINGS_BUTTON_TEXT);
        this.id = id;
    }

    @Override
    public void delete() {
    }

    public int getId() {
        return id;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        label.x = x + 20;
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        label.y = y + 4;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
        if (isHovered) {
            fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
        }

        drawRadio(matrixStack, x + 2, y + 2, selected);

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered) {
            return Action.Clicked;
        }

        return Action.None;
    }

    private void drawRadio(MatrixStack matrixStack, int x, int y, boolean checked) {
        RenderSystem.color4f(1.f, 1.f, 1.f, 1f);
        Minecraft.getInstance().getTextureManager().bind(texture);

        blit(matrixStack, x, y, checked ? 12 : 0, 0, 12, 12, textureSizeX, textureSizeY);
    }
}
