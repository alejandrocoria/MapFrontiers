package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiChunkShapeButtons extends AbstractWidget {
    public enum ShapeMeasure {
        None, Width, Length
    }

    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private int size;
    private GuiSimpleLabel labelShapes;
    private GuiSimpleLabel labelChunks;
    private final Consumer<GuiChunkShapeButtons> callbackShapeUpdated;

    public GuiChunkShapeButtons(Font font, int x, int y, int selected, Consumer<GuiChunkShapeButtons> callbackShapeUpdated) {
        super(x, y, 214, 120, TextComponent.EMPTY);
        this.selected = selected;
        labelShapes = new GuiSimpleLabel(font, x + 107, y, GuiSimpleLabel.Align.Center, new TranslatableComponent("mapfrontiers.initial_shape"), GuiColors.WHITE);
        labelChunks = new GuiSimpleLabel(font, x + 107, y + 126, GuiSimpleLabel.Align.Center, new TextComponent(""), GuiColors.WHITE);
        this.callbackShapeUpdated = callbackShapeUpdated;

        updateChunksLabel();
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 1), 32);
        updateChunksLabel();
    }

    public int getSelected() {
        return selected;
    }

    public ShapeMeasure getShapeMeasure() {
        if (selected < 2 || selected == 7) {
            return ShapeMeasure.None;
        } else if (selected < 5) {
            return ShapeMeasure.Width;
        } else {
            return ShapeMeasure.Length;
        }
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        if (!active || !visible) {
            return false;
        }

        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        double col = (mouseX - x + 3) / 55.0;
        double row = (mouseY - y - 13) / 55.0;
        if (col >= 0.0 && col < 4.0 && row >= 0.0 && row < 2.0) {
            selected = (int) col + (int) row * 4;
            updateChunksLabel();
            callbackShapeUpdated.accept(this);
        }
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        int col = 0;
        int row = 0;
        for (int i = 0; i < 8; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            blit(matrixStack, x + col * 55, y + row * 55 + 16, texX + 588, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 4) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(matrixStack, mouseX, mouseY, partialTicks);
        labelChunks.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }

    private void updateChunksLabel() {
        int chunks = 0;
        switch (selected) {
            case 1:
                chunks = 1;
                break;
            case 2:
                chunks = size * size;
                break;
            case 3:
                chunks = Math.max(1, (size - 1) * 4);
                break;
            case 4:
                chunks = (size * size + 1) / 2;
                if (size % 2 == 0) {
                    chunks += size;
                }
                break;
            case 5:
            case 6:
                chunks = size;
                break;
            case 7:
                chunks = 1024;
                break;
        }

        labelChunks.setText(new TranslatableComponent("mapfrontiers.chunks", chunks));
    }
}
