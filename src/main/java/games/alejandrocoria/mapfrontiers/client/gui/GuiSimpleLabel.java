package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.ConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiSimpleLabel extends Gui {
    public enum Align {
        Left, Center, Right
    };

    private final FontRenderer fontRenderer;
    private int x;
    private int y;
    private int scale = 1;
    private int color;
    private boolean hovered;
    private final Align align;
    private List<String> texts;
    private List<Integer> widths;
    private ConfigData.Point topLeft;
    private ConfigData.Point bottomRight;

    public GuiSimpleLabel(FontRenderer fontRenderer, int x, int y, Align align, String text) {
        this(fontRenderer, x, y, align, text, 0);
    }

    public GuiSimpleLabel(FontRenderer fontRenderer, int x, int y, Align align, String text, int color) {
        this.fontRenderer = fontRenderer;
        this.x = x;
        this.y = y;
        this.color = color;
        this.align = align;

        setText(text);
    }

    public void setText(String text) {
        texts = new ArrayList<String>();
        widths = new ArrayList<Integer>();

        for (String t : text.split("\\R")) {
            texts.add(t);
            widths.add(Integer.valueOf(fontRenderer.getStringWidth(t)));
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getText(int line) {
        return texts.get(line);
    }

    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        GlStateManager.scale(scale, scale, 1.0);

        if (align == Align.Left) {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x / scale, (y + i * 12) / scale, color);
            }
        } else if (align == Align.Center) {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x / scale - (widths.get(i) - 1) / 2, (y + i * 12) / scale, color);
            }
        } else {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x / scale - widths.get(i), (y + i * 12) / scale, color);
            }
        }

        hovered = (mouseX >= topLeft.x * scale + x && mouseY >= topLeft.y * scale + y && mouseX < bottomRight.x * scale + x
                && mouseY < bottomRight.y * scale + y);

        GlStateManager.scale(1.0 / scale, 1.0 / scale, 1.0);
    }

    public boolean isMouseOver() {
        return hovered;
    }
}

