package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiVertexShapeButtons extends Widget {
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

    public GuiVertexShapeButtons(FontRenderer font, int x, int y, int selected, Consumer<GuiVertexShapeButtons> callbackShapeUpdated) {
        super(x, y, 324, 120, StringTextComponent.EMPTY);
        this.selected = selected;
        labelShapes = new GuiSimpleLabel(font, x + 162, y, GuiSimpleLabel.Align.Center, new TranslationTextComponent("mapfrontiers.initial_shape"), GuiColors.WHITE);
        labelVertices = new GuiSimpleLabel(font, x + 162, y + 126, GuiSimpleLabel.Align.Center, new StringTextComponent(""), GuiColors.WHITE);
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

    public List<Vector3d> getVertices() {
        if (vertexCount[selected] == 0) {
            return null;
        }

        List<Vector3d> vertices = new ArrayList<>();

        if (vertexCount[selected] == 1) {
            vertices.add(Vector3d.ZERO);
            return vertices;
        }

        double angle = vertexAngle[selected] / 180.0 * Math.PI;

        for (int i = 0; i < vertexCount[selected]; ++i) {
            double vertexAngle = Math.PI * 2 / vertexCount[selected] * i + angle;
            vertices.add(new Vector3d((float) Math.cos(vertexAngle), 0.0, (float) Math.sin(vertexAngle)));
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
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.f, 1.f, 1.f, 1.f);
        Minecraft.getInstance().getTextureManager().bind(texture);

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

    private void updateVertexLabel() {
        labelVertices.setText(new TranslationTextComponent("mapfrontiers.vertices", vertexCount[selected]));
    }
}
