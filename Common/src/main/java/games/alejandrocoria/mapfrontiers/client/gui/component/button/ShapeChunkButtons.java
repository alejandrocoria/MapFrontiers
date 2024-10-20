package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private int size;
    private final SimpleLabel labelShapes;
    private final SimpleLabel labelChunks;
    private final Consumer<ShapeChunkButtons> callbackShapeUpdated;

    public ShapeChunkButtons(Font font, int selected, Consumer<ShapeChunkButtons> callbackShapeUpdated) {
        super(0, 0, 214, 140, Component.empty());
        this.selected = selected;
        labelShapes = new SimpleLabel(font, 0, 0, SimpleLabel.Align.Center, Component.translatable("mapfrontiers.initial_shape"), ColorConstants.WHITE);
        labelChunks = new SimpleLabel(font, 0, 0, SimpleLabel.Align.Center, Component.empty(), ColorConstants.WHITE);
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
    public void setX(int x) {
        super.setX(x);
        labelShapes.setX(x + 107);
        labelChunks.setX(x + 107);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        labelShapes.setY(y + 2);
        labelChunks.setY(y + 129);
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
        double row = (mouseY - getY() - 15) / 55.0;
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        int col = 0;
        int row = 0;
        for (int i = 0; i < 8; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            graphics.blit(texture, getX() + col * 55, getY() + row * 55 + 18, texX + 588, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 4) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(graphics, mouseX, mouseY, partialTicks);
        labelChunks.render(graphics, mouseX, mouseY, partialTicks);
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
