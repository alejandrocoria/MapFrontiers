package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiTabbedBox extends AbstractWidget {
    private final Font font;
    private final TabbedBoxResponder responder;
    private final int width;
    private final int height;
    private final List<Tab> tabs;
    private int selected;

    public GuiTabbedBox(Font font, int x, int y, int width, int height, TabbedBoxResponder responder) {
        super(x, y, width, 16, Component.empty());
        this.font = font;
        this.responder = responder;
        tabs = new ArrayList<>();
        selected = -1;
        this.width = width;
        this.height = height;
    }

    public void addTab(Component text, boolean enabled) {
        tabs.add(new Tab(font, text, enabled));
        updateTabPositions();

        if (selected == -1) {
            selected = 0;
        }
    }

    public void setTabSelected(int tab) {
        selected = tab;
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < tabs.size(); ++i) {
            tabs.get(i).render(matrixStack, mouseX, mouseY, partialTicks, i == selected);
        }

        if (selected == -1) {
            hLine(matrixStack, getX(), getX() + width, getY() + 16, GuiColors.SETTINGS_TAB_BORDER);
        } else {
            int selectedX = tabs.get(selected).x;
            hLine(matrixStack, getX(), selectedX, getY() + 16, GuiColors.SETTINGS_TAB_BORDER);
            hLine(matrixStack, selectedX + 70, getX() + width, getY() + 16, GuiColors.SETTINGS_TAB_BORDER);
        }

        hLine(matrixStack, getX(), getX() + width, getY() + height, GuiColors.SETTINGS_TAB_BORDER);
        vLine(matrixStack, getX(), getY() + 16, getY() + height, GuiColors.SETTINGS_TAB_BORDER);
        vLine(matrixStack, getX() + width, getY() + 16, getY() + height, GuiColors.SETTINGS_TAB_BORDER);
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
        int tabX = getX() + width / 2 - tabs.size() * 35;

        for (Tab tab : tabs) {
            tab.x = tabX;
            tab.y = getY();
            tabX += 70;
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    @Environment(EnvType.CLIENT)
    private static class Tab extends GuiComponent {
        private int x = 0;
        private int y = 0;
        private boolean isHovered = false;
        private final boolean active;
        private final GuiSimpleLabel label;

        public Tab(Font font, Component text, boolean active) {
            this.active = active;
            this.label = new GuiSimpleLabel(font, 0, 0, GuiSimpleLabel.Align.Center, text, GuiColors.SETTINGS_TAB_TEXT);
        }

        public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks, boolean selected) {
            if (active) {
                isHovered = mouseX >= x && mouseY >= y && mouseX < x + 71 && mouseY < y + 16;
            } else {
                isHovered = false;
            }

            hLine(matrixStack, x, x + 70, y, GuiColors.SETTINGS_TAB_BORDER);
            vLine(matrixStack, x, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);
            vLine(matrixStack, x + 70, y, y + 16, GuiColors.SETTINGS_TAB_BORDER);

            if (active) {
                if (selected || isHovered) {
                    label.setColor(GuiColors.SETTINGS_TAB_TEXT_HIGHLIGHT);
                } else {
                    label.setColor(GuiColors.SETTINGS_TAB_TEXT);
                }
            } else {
                label.setColor(GuiColors.SETTINGS_TAB_TEXT_DISABLED);
            }

            label.setX(x + 35);
            label.setY(y + 5);
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        public boolean clicked(double mouseX, double mouseY) {
            return isHovered;
        }
    }

    @Environment(EnvType.CLIENT)
    public interface TabbedBoxResponder {
        void tabChanged(int tab);
    }
}
