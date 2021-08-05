package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
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
    private final List<AbstractWidget> widgets = new ArrayList<>();

    public GuiBookmark(int x, int y, int height, int activeHeight, Component text, ResourceLocation texture, int textureSize,
            Button.OnPress pressedAction) {
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

    public void addWidget(AbstractWidget widget) {
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
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (y != targetPosition) {
            int factor = Math.abs(targetPosition - y) / 4 + 1;
            if (y > targetPosition) {
                factor = -factor;
            }

            y += factor;
            for (AbstractWidget widget : widgets) {
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
        Minecraft mc = Minecraft.getInstance();

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        int textureX = 362;
        int textureY = 1;

        if (isHovered) {
            textureX += 52;
        }

        blit(matrixStack, x, y, textureX, textureY, width, height, textureSize, textureSize);
        drawCenteredLabel(matrixStack, mc.font, getMessage().getString(), x + width / 2, y + 9, GuiColors.BOOKMARK_TEXT);
    }

    private void drawCenteredLabel(PoseStack matrixStack, Font font, String label, int x, int y, int color) {
        int labelWidth = font.width(label);
        x -= labelWidth / 2;
        font.draw(matrixStack, label, x, y, color);
    }
}
