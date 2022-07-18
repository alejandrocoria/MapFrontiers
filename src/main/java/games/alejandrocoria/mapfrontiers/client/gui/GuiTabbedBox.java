package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiTabbedBox extends AbstractWidget {
    private final Font font;
    private final TabbedBoxResponder responder;
    private final int width;
    private final int height;
    private final List<Tab> tabs;
    private int selected;

    public GuiTabbedBox(Font font, int x, int y, int width, int height, TabbedBoxResponder responder) {
        super(x, y, width, 16, TextComponent.EMPTY);
        this.font = font;
        this.responder = responder;
        tabs = new ArrayList<>();
        selected = -1;
        this.x = x;
        this.y = y;
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
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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

    @Override
    public void updateNarration(NarrationElementOutput p_169152_)
    {

    }

    @OnlyIn(Dist.CLIENT)
    private static class Tab extends GuiComponent {
        private int x = 0;
        private int y = 0;
        private boolean isHovered = false;
        private boolean active;
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
