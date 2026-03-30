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
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ShapeVertexButtons extends AbstractWidgetNoNarration {
    private static final int[] vertexCount = {
            0, 1, 3, 3, 3, 3, 4, 4, 6, 6, 8, 16
    };

    private static final double[] vertexAngle = {
            0.0, 0.0, -90.0, 0.0, 90.0, 180.0, 45.0, 0.0, 30.0, 0.0, 22.5, 0.0
    };

    public enum ShapeMeasure {
        None, Width, Radius
    }

    private static final Identifier texture = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private final StringWidget labelShapes;
    private final Consumer<ShapeVertexButtons> callbackShapeUpdated;

    public ShapeVertexButtons(Font font, int selected, Consumer<ShapeVertexButtons> callbackShapeUpdated) {
        super(0, 0, 324, 122, Component.empty());
        this.selected = selected;
        labelShapes = new StringWidget(Component.translatable("mapfrontiers.initial_shape"), font, StringWidget.Align.Center).setColor(ColorConstants.WHITE);
        this.callbackShapeUpdated = callbackShapeUpdated;
    }

    public int getSelected() {
        return selected;
    }

    public ShapeMeasure getShapeMeasure() {
        if (selected < 2) {
            return ShapeMeasure.None;
        } else if (selected < 7) {
            return ShapeMeasure.Width;
        } else {
            return ShapeMeasure.Radius;
        }
    }

    public int getVertexCount() {
        return vertexCount[selected];
    }

    public List<Vec2> getVertices() {
        return getVertices(vertexCount[selected], vertexAngle[selected] / 180.0 * Math.PI);
    }

    public List<Vec2> getVertices(int count) {
        return getVertices(count, 0.0);
    }

    private List<Vec2> getVertices(int count, double angleOffset) {
        if (count == 0) {
            return null;
        }

        List<Vec2> vertices = new ArrayList<>();

        if (count == 1) {
            vertices.add(Vec2.ZERO);
            return vertices;
        }

        for (int i = 0; i < count; ++i) {
            double vertexAngle = Math.PI * 2 / count * i + angleOffset;
            vertices.add(new Vec2((float) Math.cos(vertexAngle), (float) Math.sin(vertexAngle)));
        }

        return vertices;
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
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double col = (event.x() - getX() + 3) / 55.0;
        double row = (event.y() - getY() - 15) / 55.0;
        if (col >= 0.0 && col < 6.0 && row >= 0.0 && row < 2.0) {
            selected = (int) col + (int) row * 6;
            callbackShapeUpdated.accept(this);
        }

        return true;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int col = 0;
        int row = 0;
        for (int i = 0; i < 12; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, getX() + col * 55, getY() + row * 55 + 18, texX, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 6) {
                col = 0;
                ++row;
            }
        }

        labelShapes.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }
}
