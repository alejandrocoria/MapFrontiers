package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.common.Config;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

// TODO remove
@ParametersAreNonnullByDefault
public class SimpleLabel extends AbstractWidgetNoNarration {
    public enum Align {
        Left, Center, Right
    }

    private final Font font;
    private float scale = 1.f;
    private int color;
    private final Align align;
    private List<String> texts;
    private List<Integer> widths;
    private Config.Point topLeft;
    private Config.Point bottomRight;

    public SimpleLabel(Font font, int x, int y, Align align, Component text, int color) {
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

        topLeft = new Config.Point();
        bottomRight = new Config.Point();
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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.pose().scale(scale, scale, 1.0F);

        if (align == Align.Left) {
            for (int i = 0; i < texts.size(); ++i) {
                graphics.drawString(font, texts.get(i), Mth.floor(getX() / scale), Mth.floor((getY() + i * 12) / scale), color);
            }
        } else if (align == Align.Center) {
            for (int i = 0; i < texts.size(); ++i) {
                graphics.drawString(font, texts.get(i), Mth.floor(getX() / scale - (widths.get(i) - 1) / 2.f), Mth.floor((getY() + i * 12) / scale), color);
            }
        } else {
            for (int i = 0; i < texts.size(); ++i) {
                graphics.drawString(font, texts.get(i), Mth.floor(getX() / scale - widths.get(i)), Mth.floor((getY() + i * 12) / scale), color);
            }
        }

        isHovered = (mouseX >= topLeft.x * scale + getX() && mouseY >= topLeft.y * scale + getY() && mouseX < bottomRight.x * scale + getX()
                && mouseY < bottomRight.y * scale + getY());

        graphics.pose().scale(1.0F / scale, 1.0F / scale, 1.0F);
    }
}
