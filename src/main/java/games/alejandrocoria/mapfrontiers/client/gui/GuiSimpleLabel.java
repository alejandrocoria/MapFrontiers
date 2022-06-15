package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiSimpleLabel extends AbstractWidget {
    public enum Align {
        Left, Center, Right
    }

    private final Font font;
    private int scale = 1;
    private int color;
    private final Align align;
    private List<String> texts;
    private List<Integer> widths;
    private ConfigData.Point topLeft;
    private ConfigData.Point bottomRight;

    public GuiSimpleLabel(Font font, int x, int y, Align align, Component text, int color) {
        super(x, y, 0, 0, text);
        this.font = font;
        this.color = color;
        this.align = align;

        setText(text);
    }

    public void setText(Component text) {
        texts = new ArrayList<>();
        widths = new ArrayList<>();

        for (String t : text.getString().split("\\R")) {
            texts.add(t);
            widths.add(font.width(t));
        }

        topLeft = new ConfigData.Point();
        bottomRight = new ConfigData.Point();
        bottomRight.y = texts.size() * 12;

        if (align == Align.Left) {
            for (int i = 0; i < texts.size(); ++i) {
                int width = widths.get(i);
                if (width > bottomRight.x) {
                    bottomRight.x = width;
                }
            }
        } else if (align == Align.Center) {
            for (int i = 0; i < texts.size(); ++i) {
                int halfWidth = widths.get(i) / 2;
                if (-halfWidth < topLeft.x) {
                    topLeft.x = -halfWidth;
                }
                if (halfWidth > bottomRight.x) {
                    bottomRight.x = halfWidth;
                }
            }
        } else {
            for (int i = 0; i < texts.size(); ++i) {
                int width = widths.get(i);
                if (-width < topLeft.x) {
                    topLeft.x = -width;
                }
            }
        }
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        matrixStack.scale(scale, scale, 1.0F);

        if (align == Align.Left) {
            for (int i = 0; i < texts.size(); ++i) {
                font.draw(matrixStack, texts.get(i), x / scale, (y + i * 12) / scale, color);
            }
        } else if (align == Align.Center) {
            for (int i = 0; i < texts.size(); ++i) {
                font.draw(matrixStack, texts.get(i), x / scale - (widths.get(i) - 1) / 2, (y + i * 12) / scale,
                        color);
            }
        } else {
            for (int i = 0; i < texts.size(); ++i) {
                font.draw(matrixStack, texts.get(i), x / scale - widths.get(i), (y + i * 12) / scale, color);
            }
        }

        isHovered = (mouseX >= topLeft.x * scale + x && mouseY >= topLeft.y * scale + y && mouseX < bottomRight.x * scale + x
                && mouseY < bottomRight.y * scale + y);

        matrixStack.scale(1.0F / scale, 1.0F / scale, 1.0F);
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_) {
    }
}
