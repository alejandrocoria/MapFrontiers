package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ShapeChunkButtons extends AbstractWidgetNoNarration {
    public enum ShapeMeasure {
        None, Width, Length
    }

    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private int size;
    private final StringWidget labelShapes;
    private final Consumer<ShapeChunkButtons> callbackShapeUpdated;

    public ShapeChunkButtons(Font font, int selected, Consumer<ShapeChunkButtons> callbackShapeUpdated) {
        super(0, 0, 214, 122, Component.empty());
        this.selected = selected;
        labelShapes = new StringWidget(Component.translatable("mapfrontiers.initial_shape"), font, StringWidget.Align.Center).setColor(ColorConstants.WHITE);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(size, 1), 32);
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

    public int getChunkCount() {
        int chunks;
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
            default -> chunks = 0;
        }
        return chunks;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        labelShapes.setX(x + 107);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        labelShapes.setY(y + 2);
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double col = (event.x() - getX() + 3) / 55.0;
        double row = (event.y() - getY() - 15) / 55.0;
        if (col >= 0.0 && col < 4.0 && row >= 0.0 && row < 2.0) {
            selected = (int) col + (int) row * 4;
            callbackShapeUpdated.accept(this);
        }

        return true;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int col = 0;
        int row = 0;
        for (int i = 0; i < 8; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + col * 55, getY() + row * 55 + 18, texX + 588, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 4) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(graphics, mouseX, mouseY, partialTicks);
    }
}
