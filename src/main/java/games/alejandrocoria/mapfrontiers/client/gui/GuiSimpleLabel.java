package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiSimpleLabel extends Gui {
    public enum Align {
        Left, Center, Right
    };

    private final FontRenderer fontRenderer;
    private final int x;
    private final int y;
    private final int color;
    private final Align align;
    private List<String> texts;
    private List<Integer> widths;

    public GuiSimpleLabel(FontRenderer fontRenderer, int x, int y, Align align, String text) {
        this(fontRenderer, x, y, align, text, 0);
    }

    public GuiSimpleLabel(FontRenderer fontRenderer, int x, int y, Align align, String text, int color) {
        this.fontRenderer = fontRenderer;
        this.x = x;
        this.y = y;
        this.color = color;
        this.align = align;
        texts = new ArrayList<String>();
        widths = new ArrayList<Integer>();

        for (String t : text.split("\\R")) {
            texts.add(t);
            widths.add(Integer.valueOf(fontRenderer.getStringWidth(t)));
        }
    }

    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        if (align == Align.Left) {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x, y + i * 12, color);
            }
        } else if (align == Align.Center) {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x - widths.get(i) / 2, y + i * 12, color);
            }
        } else {
            for (int i = 0; i < texts.size(); ++i) {
                fontRenderer.drawString(texts.get(i), x - widths.get(i), y + i * 12, color);
            }
        }
    }
}
