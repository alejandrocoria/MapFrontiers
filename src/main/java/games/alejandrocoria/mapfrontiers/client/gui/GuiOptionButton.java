package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiOptionButton extends GuiButton {
    protected final FontRenderer fontRenderer;
    private List<String> options;
    private int selected = 0;

    public GuiOptionButton(int componentId, FontRenderer fontRenderer, int x, int y, int width) {
        super(componentId, x, y, width, 12, "");
        this.fontRenderer = fontRenderer;
        options = new ArrayList<String>();
    }

    public void addOption(String text) {
        options.add(text);
    }

    public void setSelected(int selected) {
        if (selected < 0) {
            selected = 0;
        } else if (selected >= options.size()) {
            selected = options.size() - 1;
        }

        this.selected = selected;
    }

    public int getSelected() {
        return selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);

            int color = 0xbbbbbb;
            if (hovered) {
                color = 0xffffff;
            }

            Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0xff444444);
            Gui.drawRect(x, y, x + width, y + height, 0xff000000);

            fontRenderer.drawString(options.get(selected), x + 4, y + 2, color);
        } else {
            hovered = false;
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (enabled && visible && hovered) {
            ++selected;
            if (selected >= options.size()) {
                selected = 0;
            }
            return true;
        }

        return false;
    }
}