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
import net.minecraft.world.phys.Vec2;

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

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private final SimpleLabel labelShapes;
    private final SimpleLabel labelVertices;
    private final Consumer<ShapeVertexButtons> callbackShapeUpdated;

    public ShapeVertexButtons(Font font, int x, int y, int selected, Consumer<ShapeVertexButtons> callbackShapeUpdated) {
        super(x, y, 324, 120, Component.empty());
        this.selected = selected;
        labelShapes = new SimpleLabel(font, x + 162, y, SimpleLabel.Align.Center, Component.translatable("mapfrontiers.initial_shape"), ColorConstants.WHITE);
        labelVertices = new SimpleLabel(font, x + 162, y + 127, SimpleLabel.Align.Center, Component.literal(""), ColorConstants.WHITE);
        this.callbackShapeUpdated = callbackShapeUpdated;

        updateVertexLabel();
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

    public List<Vec2> getVertices() {
        if (vertexCount[selected] == 0) {
            return null;
        }

        List<Vec2> vertices = new ArrayList<>();

        if (vertexCount[selected] == 1) {
            vertices.add(Vec2.ZERO);
            return vertices;
        }

        double angle = vertexAngle[selected] / 180.0 * Math.PI;

        for (int i = 0; i < vertexCount[selected]; ++i) {
            double vertexAngle = Math.PI * 2 / vertexCount[selected] * i + angle;
            vertices.add(new Vec2((float) Math.cos(vertexAngle), (float) Math.sin(vertexAngle)));
        }

        return vertices;
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
        if (col >= 0.0 && col < 6.0 && row >= 0.0 && row < 2.0) {
            selected = (int) col + (int) row * 6;
            updateVertexLabel();
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
        for (int i = 0; i < 12; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            graphics.blit(texture, getX() + col * 55, getY() + row * 55 + 16, texX, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 6) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(graphics, mouseX, mouseY, partialTicks);
        labelVertices.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void updateVertexLabel() {
        labelVertices.setText(Component.translatable("mapfrontiers.vertices", vertexCount[selected]));
    }
}
