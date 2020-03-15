package games.alejandrocoria.mapfrontiers.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiBookmark extends GuiButton {
    private final ResourceLocation texture;
    private final int textureSize;
    private final int activeHeight;
    private List<Integer> yPositions;
    private int targetPosition;
    private List<GuiLabel> labels;

    public GuiBookmark(int id, int x, int y, int height, int activeHeight, String text, ResourceLocation texture,
            int textureSize) {
        super(id, x, y, 51, height, text);
        this.texture = texture;
        this.textureSize = textureSize;
        this.activeHeight = activeHeight;

        yPositions = new ArrayList<Integer>();
        yPositions.add(Integer.valueOf(y));
        targetPosition = y;

        labels = new ArrayList<GuiLabel>();
    }

    public void changePosition(int indexPosition) {
        if (indexPosition >= yPositions.size()) {
            indexPosition = yPositions.size() - 1;
        } else if (indexPosition < 0) {
            indexPosition = 0;
        }

        targetPosition = yPositions.get(indexPosition);
    }

    public void addYPosition(int yPosition) {
        yPositions.add(Integer.valueOf(yPosition));
    }

    public void addlabel(GuiLabel label) {
        labels.add(label);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return enabled && visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + activeHeight;
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (y != targetPosition) {
            int factor = Math.abs(targetPosition - y) / 4 + 1;
            if (y > targetPosition) {
                factor = -factor;
            }

            y += factor;
            for (GuiLabel label : labels) {
                label.y += factor;
            }

            if (factor > 0) {
                if (y > targetPosition) {
                    y = targetPosition;
                }
            } else {
                if (y < targetPosition) {
                    y = targetPosition;
                }
            }
        }

        if (visible) {
            hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + activeHeight);
            GlStateManager.color(1.f, 1.f, 1.f);
            mc.getTextureManager().bindTexture(texture);
            int textureX = 362;
            int textureY = 1;

            if (hovered) {
                textureX += 52;
            }

            drawModalRectWithCustomSizedTexture(x, y, textureX, textureY, width, height, textureSize, textureSize);
            drawCenteredLabel(mc.fontRenderer, displayString, x + width / 2, y + 9, 0xffffff);

            for (GuiLabel label : labels) {
                label.drawLabel(mc, mouseX, mouseY);
            }
        }
    }

    private void drawCenteredLabel(FontRenderer fontRenderer, String label, int x, int y, int color) {
        int labelWidth = fontRenderer.getStringWidth(label);
        x -= labelWidth / 2;
        fontRenderer.drawString(label, x, y, color);
    }
}
