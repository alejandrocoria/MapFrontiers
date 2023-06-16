package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ShapeChunkButtons extends AbstractWidgetNoNarration {
    public enum ShapeMeasure {
        None, Width, Length
    }

    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private int size;
    private final SimpleLabel labelShapes;
    private final SimpleLabel labelChunks;
    private final Consumer<ShapeChunkButtons> callbackShapeUpdated;

    public ShapeChunkButtons(Font font, int x, int y, int selected, Consumer<ShapeChunkButtons> callbackShapeUpdated) {
        super(x, y, 214, 120, Component.empty());
        this.selected = selected;
        labelShapes = new SimpleLabel(font, x + 107, y, SimpleLabel.Align.Center, Component.translatable("mapfrontiers.initial_shape"), ColorConstants.WHITE);
        labelChunks = new SimpleLabel(font, x + 107, y + 126, SimpleLabel.Align.Center, Component.literal(""), ColorConstants.WHITE);
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

        return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        double col = (mouseX - getX() + 3) / 55.0;
        double row = (mouseY - getY() - 13) / 55.0;
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
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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

            blit(matrixStack, getX() + col * 55, getY() + row * 55 + 16, texX + 588, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 4) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(matrixStack, mouseX, mouseY, partialTicks);
        labelChunks.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void updateChunksLabel() {
        int chunks = 0;
        switch (selected) {
            case 1 -> chunks = 1;
            case 2 -> chunks = size * size;
            case 3 -> chunks = Math.max(1, (size - 1) * 4);
            case 4 -> {
                chunks = (size * size + 1) / 2;
                if (size % 2 == 0) {
                    chunks += size;
                }
            }
            case 5, 6 -> chunks = size;
            case 7 -> chunks = 1024;
        }

        labelChunks.setText(Component.translatable("mapfrontiers.chunks", chunks));
    }
}
