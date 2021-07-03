package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiBookmark extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private final int activeHeight;
    private final List<Integer> yPositions = new ArrayList<>();
    private int targetPosition;
    private final List<Widget> widgets = new ArrayList<>();

    public GuiBookmark(int x, int y, int height, int activeHeight, ITextComponent text, ResourceLocation texture, int textureSize,
            Button.IPressable pressedAction) {
        super(x, y, 51, height, text, pressedAction);
        this.texture = texture;
        this.textureSize = textureSize;
        this.activeHeight = activeHeight;

        yPositions.add(y);
        targetPosition = y;
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
        yPositions.add(yPosition);
    }

    public void addWidget(Widget widget) {
        widgets.add(widget);
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return active && visible && mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + activeHeight;
    }

    @Override
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (y != targetPosition) {
            int factor = Math.abs(targetPosition - y) / 4 + 1;
            if (y > targetPosition) {
                factor = -factor;
            }

            y += factor;
            for (Widget widget : widgets) {
                widget.y += factor;
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

        isHovered = isMouseOver(mouseX, mouseY);
        RenderSystem.color3f(1.f, 1.f, 1.f);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(texture);

        int textureX = 362;
        int textureY = 1;

        if (isHovered) {
            textureX += 52;
        }

        blit(matrixStack, x, y, textureX, textureY, width, height, textureSize, textureSize);
        drawCenteredLabel(matrixStack, mc.font, getMessage().getString(), x + width / 2, y + 9, GuiColors.BOOKMARK_TEXT);
    }

    private void drawCenteredLabel(MatrixStack matrixStack, FontRenderer font, String label, int x, int y, int color) {
        int labelWidth = font.width(label);
        x -= labelWidth / 2;
        font.draw(matrixStack, label, x, y, color);
    }
}
