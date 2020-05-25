package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiTabbedBox extends Gui {
    private final FontRenderer fontRenderer;
    private TabbedBoxResponder responder;
    public boolean visible = true;
    private int x = 0;
    private int y = 0;
    private int width;
    private int height;
    private List<Tab> tabs;
    private int selected;

    public GuiTabbedBox(FontRenderer fontRenderer, int x, int y, int width, int height, TabbedBoxResponder responder) {
        this.fontRenderer = fontRenderer;
        this.responder = responder;
        tabs = new ArrayList<Tab>();
        selected = -1;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addTab(String label) {
        tabs.add(new Tab(fontRenderer, label));
        updateTabPositions();

        if (selected == -1) {
            selected = 0;
        }
    }

    public void setTabSelected(int tab) {
        selected = tab;
    }

    public void drawBox(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            for (int i = 0; i < tabs.size(); ++i) {
                tabs.get(i).draw(mc, mouseX, mouseY, i == selected);
            }

            if (selected == -1) {
                drawHorizontalLine(x, x + width, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            } else {
                int selectedX = tabs.get(selected).x;
                drawHorizontalLine(x, selectedX, y + 16, GuiColors.SETTINGS_TAB_BORDER);
                drawHorizontalLine(selectedX + 70, x + width, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            }

            drawHorizontalLine(x, x + width, y + height, GuiColors.SETTINGS_TAB_BORDER);
            drawVerticalLine(x, y + 16, y + height, GuiColors.SETTINGS_TAB_BORDER);
            drawVerticalLine(x + width, y + 16, y + height, GuiColors.SETTINGS_TAB_BORDER);
        }
    }

    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            for (int i = 0; i < tabs.size(); ++i) {
                if (tabs.get(i).mousePressed(mc, mouseX, mouseY)) {
                    if (selected != i) {
                        selected = i;
                        if (responder != null) {
                            responder.tabChanged(selected);
                        }
                        return;
                    }
                }
            }
        }
    }

    private void updateTabPositions() {
        int tabX = x + width / 2 - tabs.size() * 35;

        for (Tab tab : tabs) {
            tab.x = tabX;
            tab.y = y;
            tabX += 70;
        }
    }

    @SideOnly(Side.CLIENT)
    private static class Tab extends Gui {
        private int x = 0;
        private int y = 0;
        private boolean hovered = false;
        private GuiSimpleLabel label;

        public Tab(FontRenderer fontRenderer, String label) {
            this.label = new GuiSimpleLabel(fontRenderer, 0, 0, GuiSimpleLabel.Align.Center, label, GuiColors.SETTINGS_TAB_TEXT);
        }

        public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + 71 && mouseY < y + 16;

            drawHorizontalLine(x, x + 70, y, GuiColors.SETTINGS_TAB_BORDER);
            drawVerticalLine(x, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            drawVerticalLine(x + 70, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);

            if (selected || hovered) {
                label.setColor(GuiColors.SETTINGS_TAB_TEXT_HIGHLIGHT);
            } else {
                label.setColor(GuiColors.SETTINGS_TAB_TEXT);
            }

            label.setX(x + 35);
            label.setY(y + 5);
            label.drawLabel(mc, mouseX, mouseY);
        }

        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            return hovered;
        }
    }

    @SideOnly(Side.CLIENT)
    public interface TabbedBoxResponder {
        public void tabChanged(int tab);
    }
}
