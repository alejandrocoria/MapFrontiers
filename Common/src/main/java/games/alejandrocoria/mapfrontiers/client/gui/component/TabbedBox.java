package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class TabbedBox implements Layout {
    private final Font font;
    private final IntConsumer tabChanged;
    private int width;
    private int height;
    private final List<Tab> tabs = new ArrayList<>();
    private final List<FrameLayout> contents = new ArrayList<>();
    private final LinearLayout mainLayout = LinearLayout.vertical().spacing(16);
    private final LinearLayout tabLayouts = LinearLayout.horizontal();
    private final FrameLayout contentLayouts = new FrameLayout();
    private int selected;

    public TabbedBox(Font font, int width, int height, IntConsumer tabChanged) {
        super();
        this.font = font;
        this.tabChanged = tabChanged;
        selected = -1;
        this.width = width;
        this.height = height;
        contentLayouts.setMinDimensions(width, height - 32);
        mainLayout.addChild(tabLayouts, LayoutSettings.defaults().alignHorizontallyCenter());
        mainLayout.addChild(contentLayouts, LayoutSettings.defaults().alignHorizontallyCenter());
    }

    public void addTab(Component text, boolean enabled) {
        tabs.add(new Tab(font, text, tabs.size(), enabled, this::setTabSelected));
        tabLayouts.addChild(tabs.getLast());

        contents.add(new FrameLayout(width, height - 32));
        contentLayouts.addChild(contents.getLast());

        if (selected == -1) {
            selected = 0;
        }
    }

    public void setTabSelected(int tab) {
        if (selected != -1) {
            tabs.get(selected).setSelected(false);
        }
        selected = tab;
        tabs.get(selected).setSelected(true);

        for (int i = 0; i < contents.size(); ++i) {
            if (i == selected) {
                contents.get(i).visitWidgets((widget) -> widget.visible = true);
            } else {
                contents.get(i).visitWidgets((widget) -> widget.visible = false);
            }
        }

        tabChanged.accept(selected);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        contentLayouts.setMinDimensions(width, height - 32);
        for (FrameLayout content : contents) {
            content.setMinDimensions(width, height - 32);
        }
        arrangeElements();
    }

    public <T extends LayoutElement> T addChild(T layoutElement, int tab) {
        contents.get(tab).addChild(layoutElement);
        return layoutElement;
    }

    public <T extends LayoutElement> T addChild(T layoutElement, int tab, LayoutSettings layoutSettings) {
        contents.get(tab).addChild(layoutElement, layoutSettings);
        return layoutElement;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> visitor) {
        mainLayout.visitChildren(visitor);
    }

    @Override
    public void arrangeElements() {
        mainLayout.arrangeElements();
    }

    @Override
    public int getWidth() {
        return mainLayout.getWidth();
    }

    @Override
    public int getHeight() {
        return mainLayout.getHeight();
    }

    @Override
    public void setX(int x) {
        mainLayout.setX(x);
    }

    @Override
    public void setY(int y) {
        mainLayout.setY(y);
    }

    @Override
    public int getX() {
        return mainLayout.getX();
    }

    @Override
    public int getY() {
        return mainLayout.getY();
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY() + 16, getX() + width, getY() + height, ColorConstants.SCREEN_BG);

        if (selected == -1) {
            graphics.hLine(getX(), getX() + width, getY() + 16, ColorConstants.TAB_BORDER);
        } else {
            Tab tab = tabs.get(selected);
            graphics.hLine(getX(), tab.getX(), getY() + 16, ColorConstants.TAB_BORDER);
            graphics.hLine(tab.getX() + tab.getWidth(), getX() + width, getY() + 16, ColorConstants.TAB_BORDER);
        }

        graphics.hLine(getX(), getX() + width, getY() + height, ColorConstants.TAB_BORDER);
        graphics.vLine(getX(), getY() + 16, getY() + height, ColorConstants.TAB_BORDER);
        graphics.vLine(getX() + width, getY() + 16, getY() + height, ColorConstants.TAB_BORDER);
    }

    private static class Tab extends Button {
        private final Font font;
        private boolean selected = false;

        public Tab(Font font, Component text, int index, boolean enabled, Consumer<Integer> onPress) {
            super(0, 0, 70, 16, text, (b) -> onPress.accept(index), Button.DEFAULT_NARRATION);
            this.font = font;
            this.active = enabled;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorConstants.SCREEN_BG);

            graphics.hLine(getX(), getX() + getWidth(), getY(), ColorConstants.TAB_BORDER);
            graphics.vLine(getX(), getY(), getY() + getHeight(), ColorConstants.TAB_BORDER);
            graphics.vLine(getX() + getWidth(), getY(), getY() + getHeight(), ColorConstants.TAB_BORDER);

            int labelColor = ColorConstants.TAB_TEXT;
            if (!active) {
                labelColor = ColorConstants.TAB_TEXT_DISABLED;
            } else if (selected || isHovered) {
                labelColor = ColorConstants.TAB_TEXT_HIGHLIGHT;
            }

            graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5, labelColor);
        }
    }
}
