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
import net.minecraft.world.phys.Vec2;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiVertexShapeButtons extends AbstractWidget {
    private static final int[] vertexCount = {
            0, 1, 3, 3, 3, 3, 4, 4, 6, 6, 8, 16
    };

    private static final double[] vertexAngle = {
            0.0, 0.0, -90.0, 0.0, 90.0, 180.0, 45.0, 0.0, 30.0, 0.0, 22.5, 0.0
    };

    public enum ShapeMeasure {
        None, Width, Radius
    }

    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/shape_buttons.png");
    private static final int textureSizeX = 980;
    private static final int textureSizeY = 98;

    private int selected;
    private GuiSimpleLabel labelShapes;
    private GuiSimpleLabel labelVertices;
    private final Consumer<GuiVertexShapeButtons> callbackShapeUpdated;

    public GuiVertexShapeButtons(Font font, int x, int y, int selected, Consumer<GuiVertexShapeButtons> callbackShapeUpdated) {
        super(x, y, 324, 120, TextComponent.EMPTY);
        this.selected = selected;
        labelShapes = new GuiSimpleLabel(font, x + 162, y, GuiSimpleLabel.Align.Center, new TranslatableComponent("mapfrontiers.initial_shape"), GuiColors.WHITE);
        labelVertices = new GuiSimpleLabel(font, x + 162, y + 126, GuiSimpleLabel.Align.Center, new TextComponent(""), GuiColors.WHITE);
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
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        int col = 0;
        int row = 0;
        for (int i = 0; i < 12; ++i) {
            int texX = i * 49;
            int texY = 0;
            if (i == selected) {
                texY = 49;
            }

            blit(matrixStack, x + col * 55, y + row * 55 + 16, texX, texY, 49, 49, textureSizeX, textureSizeY);

            ++col;
            if (col == 6) {
                col = 0;
                ++row;
            }
        }

        labelShapes.render(matrixStack, mouseX, mouseY, partialTicks);
        labelVertices.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {

    }

    private void updateVertexLabel() {
        labelVertices.setText(new TranslatableComponent("mapfrontiers.vertices", vertexCount[selected]));
    }
}
