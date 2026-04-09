package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.AbstractWidgetNoNarration;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
public class ShapePathButtons extends AbstractWidgetNoNarration {
    public enum ShapeMeasure {
        None, Length
    }

    private static final int[] pointCount = {
            0, 1, 2, 2, 2, 2
    };

    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_buttons.png");
    private static final int textureSizeX = 1274;
    private static final int textureSizeY = 98;
    private static final int textureOffsetX = 980;

    private int selected;
    private final StringWidget labelShapes;
    private final Consumer<ShapePathButtons> callbackShapeUpdated;

    public ShapePathButtons(Font font, int selected, Consumer<ShapePathButtons> callbackShapeUpdated) {
        super(0, 0, 324, 122, Component.empty());
        this.selected = selected;
        labelShapes = new StringWidget(Component.translatable("mapfrontiers.initial_shape"), font, StringWidget.Align.Center).setColor(ColorConstants.WHITE);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public int getSelected() {
        return selected;
    }

    public ShapeMeasure getShapeMeasure() {
        return selected < 2 ? ShapeMeasure.None : ShapeMeasure.Length;
    }

    public int getPointCount() {
        return pointCount[selected];
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        labelShapes.setX(x + 162);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        labelShapes.setY(y + 2);
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double col = (event.x() - getX() + 3) / 55.0;
        double row = (event.y() - getY() - 15) / 55.0;
        if (col >= 0.0 && col < 6.0 && row >= 0.0 && row < 1.0) {
            selected = (int) col;
            callbackShapeUpdated.accept(this);
        }

        return true;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < 6; ++i) {
            int texX = textureOffsetX + i * 49;
            int texY = i == selected ? 49 : 0;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + i * 55, getY() + 18, texX, texY, 49, 49, textureSizeX, textureSizeY);
        }

        labelShapes.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }
}
