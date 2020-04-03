package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiBookTag extends GuiButton {
    private final ResourceLocation texture;
    private final int textureSize;
    private final boolean toLeft;

    public GuiBookTag(int id, int x, int y, int width, boolean toLeft, String text, ResourceLocation texture, int textureSize) {
        super(id, x, y, Math.max(Math.min(width, 127), 67), 15, text);
        this.texture = texture;
        this.textureSize = textureSize;
        this.toLeft = toLeft;
    }

    public void setText(String text) {
        displayString = text;
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            if (toLeft) {
                hovered = (mouseX >= x - width && mouseY >= y && mouseX < x && mouseY < y + height);
            } else {
                hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);
            }
            GlStateManager.color(1.f, 1.f, 1.f);
            mc.getTextureManager().bindTexture(texture);
            int textureX = 330;
            int textureY = 52;

            if (hovered) {
                textureX += 68;
            }

            if (toLeft) {
                textureY += 16;
                drawModalRectWithCustomSizedTexture(x - width, y, textureX, textureY, Math.min(67, width), height, textureSize,
                        textureSize);
                drawModalRectWithCustomSizedTexture(x - width + 67, y, textureX + 7, textureY, width - 67, height, textureSize,
                        textureSize);
                drawLeftLabel(mc.fontRenderer);
            } else {
                drawModalRectWithCustomSizedTexture(x, y, textureX, textureY, Math.min(60, width), height, textureSize,
                        textureSize);
                drawModalRectWithCustomSizedTexture(x + Math.min(60, width), y, textureX + 67 - width + 60, textureY, width - 60,
                        height, textureSize, textureSize);
                drawRightLabel(mc.fontRenderer);
            }
        } else {
            hovered = false;
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return enabled && visible && hovered;
    }

    private void drawLeftLabel(FontRenderer fontRenderer) {
        int labelWidth = fontRenderer.getStringWidth(displayString);
        fontRenderer.drawString(displayString, x - labelWidth - 4, y + 4, 0xeeeeee);
    }

    private void drawRightLabel(FontRenderer fontRenderer) {
        fontRenderer.drawString(displayString, x + 4, y + 4, 0xeeeeee);
    }
}