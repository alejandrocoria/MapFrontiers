package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiRadioListElement extends GuiScrollBox.ScrollElement {
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/radio_buttons.png");
    private static final int textureSizeX = 24;
    private static final int textureSizeY = 12;

    private GuiSimpleLabel label;
    int id;

    public GuiRadioListElement(Font font, Component text, int id) {
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
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
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

    private void drawRadio(PoseStack matrixStack, int x, int y, boolean checked) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        blit(matrixStack, x, y, checked ? 12 : 0, 0, 12, 12, textureSizeX, textureSizeY);
    }
}
