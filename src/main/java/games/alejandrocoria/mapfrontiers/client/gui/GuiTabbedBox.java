package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiTabbedBox extends Widget {
    private final FontRenderer font;
    private final TabbedBoxResponder responder;
    private final int width;
    private final int height;
    private final List<Tab> tabs;
    private int selected;

    public GuiTabbedBox(FontRenderer font, int x, int y, int width, int height, TabbedBoxResponder responder) {
        super(x, y, width, 16, StringTextComponent.EMPTY);
        this.font = font;
        this.responder = responder;
        tabs = new ArrayList<>();
        selected = -1;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void addTab(ITextComponent text) {
        tabs.add(new Tab(font, text));
        updateTabPositions();

        if (selected == -1) {
            selected = 0;
        }
    }

    public void setTabSelected(int tab) {
        selected = tab;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < tabs.size(); ++i) {
            tabs.get(i).render(matrixStack, mouseX, mouseY, partialTicks, i == selected);
        }

        if (selected == -1) {
            hLine(matrixStack, x, x + width, y + 16, GuiColors.SETTINGS_TAB_BORDER);
        } else {
            int selectedX = tabs.get(selected).x;
            hLine(matrixStack, x, selectedX, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            hLine(matrixStack, selectedX + 70, x + width, y + 16, GuiColors.SETTINGS_TAB_BORDER);
        }

        hLine(matrixStack, x, x + width, y + height, GuiColors.SETTINGS_TAB_BORDER);
        vLine(matrixStack, x, y + 16, y + height, GuiColors.SETTINGS_TAB_BORDER);
        vLine(matrixStack, x + width, y + 16, y + height, GuiColors.SETTINGS_TAB_BORDER);
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        for (int i = 0; i < tabs.size(); ++i) {
            if (tabs.get(i).clicked(mouseX, mouseY)) {
                if (selected != i) {
                    selected = i;
                    if (responder != null) {
                        responder.tabChanged(selected);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private void updateTabPositions() {
        int tabX = x + width / 2 - tabs.size() * 35;

        for (Tab tab : tabs) {
            tab.x = tabX;
            tab.y = y;
            tabX += 70;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class Tab extends AbstractGui {
        private int x = 0;
        private int y = 0;
        private boolean isHovered = false;
        private final GuiSimpleLabel label;

        public Tab(FontRenderer font, ITextComponent text) {
            this.label = new GuiSimpleLabel(font, 0, 0, GuiSimpleLabel.Align.Center, text, GuiColors.SETTINGS_TAB_TEXT);
        }

        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
            isHovered = mouseX >= x && mouseY >= y && mouseX < x + 71 && mouseY < y + 16;

            hLine(matrixStack, x, x + 70, y, GuiColors.SETTINGS_TAB_BORDER);
            vLine(matrixStack, x, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            vLine(matrixStack, x + 70, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);

            if (selected || isHovered) {
                label.setColor(GuiColors.SETTINGS_TAB_TEXT_HIGHLIGHT);
            } else {
                label.setColor(GuiColors.SETTINGS_TAB_TEXT);
            }

            label.x = x + 35;
            label.y = y + 5;
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        public boolean clicked(double mouseX, double mouseY) {
            return isHovered;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface TabbedBoxResponder {
        void tabChanged(int tab);
    }
}
