package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ButtonBase;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
public class TabbedBox implements Layout {
    private static final int TAB_HEIGHT = 16;
    private static final int DEFAULT_CONTENT_TOP_SPACING = 16;
    private static final int CONTENT_TOP_BORDER_HEIGHT = 1;

    private final Font font;
    private final IntConsumer tabChanged;
    private boolean sizeToContent = true;
    private int width;
    private int height;
    private int contentTopSpacing = DEFAULT_CONTENT_TOP_SPACING;
    private boolean interactive = true;
    private final List<Tab> tabs = new ArrayList<>();
    private final List<FrameLayout> contents = new ArrayList<>();
    private final LinearLayout mainLayout = LinearLayout.vertical().spacing(DEFAULT_CONTENT_TOP_SPACING);
    private final LinearLayout tabLayouts = LinearLayout.horizontal();
    private final FrameLayout contentLayouts = new FrameLayout();
    private int selected;

    public TabbedBox(Font font, IntConsumer tabChanged) {
        super();
        this.font = font;
        this.tabChanged = tabChanged;
        selected = -1;
        mainLayout.addChild(tabLayouts, LayoutSettings.defaults().alignHorizontallyCenter());
        mainLayout.addChild(contentLayouts, LayoutSettings.defaults().alignHorizontallyCenter());
    }

    public void addTab(Component text, boolean enabled) {
        addTab(text, enabled, 70);
    }

    public void addTab(Component text, boolean enabled, int width) {
        tabs.add(new Tab(this, font, text, tabs.size(), width, enabled, this::setTabSelected));
        tabLayouts.addChild(tabs.getLast());

        FrameLayout content = new FrameLayout();
        if (!sizeToContent) {
            content.setMinDimensions(width, contentHeight());
        }
        contents.add(content);
        contentLayouts.addChild(content);

        if (selected == -1) {
            selected = 0;
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public void setContentTopSpacing(int contentTopSpacing) {
        this.contentTopSpacing = contentTopSpacing;
        mainLayout.spacing(contentTopSpacing + CONTENT_TOP_BORDER_HEIGHT);
        if (!sizeToContent) {
            contentLayouts.setMinDimensions(width, contentHeight());
            for (FrameLayout content : contents) {
                content.setMinDimensions(width, contentHeight());
            }
        }
        arrangeElements();
    }

    public void setTabSelected(int tab) {
        if (tab < 0 || tab >= tabs.size() || !tabs.get(tab).isEnabled()) {
            return;
        }

        if (selected != -1) {
            tabs.get(selected).setSelected(false);
        }
        selected = tab;
        tabs.get(selected).setSelected(true);
        updateContentVisibility();

        tabChanged.accept(selected);
    }

    public void setTabEnabled(int tab, boolean enabled) {
        if (tab < 0 || tab >= tabs.size()) {
            return;
        }

        tabs.get(tab).setEnabled(enabled);

        if (!enabled && selected == tab) {
            tabs.get(tab).setSelected(false);
            selected = findFirstEnabledTab();
            if (selected != -1) {
                tabs.get(selected).setSelected(true);
                updateContentVisibility();
                tabChanged.accept(selected);
            } else {
                updateContentVisibility();
            }
        }
    }

    public boolean isTabEnabled(int tab) {
        if (tab < 0 || tab >= tabs.size()) {
            return false;
        }

        return tabs.get(tab).isEnabled();
    }

    public boolean isInteractive() {
        return interactive;
    }

    private int findFirstEnabledTab() {
        for (int i = 0; i < tabs.size(); ++i) {
            if (tabs.get(i).isEnabled()) {
                return i;
            }
        }

        return -1;
    }

    private void updateContentVisibility() {
        for (int i = 0; i < contents.size(); ++i) {
            final boolean visible = i == selected;
            contents.get(i).visitWidgets((widget) -> widget.visible = visible);
        }
    }

    public void setSize(int width, int height) {
        sizeToContent = false;
        this.width = width;
        this.height = height;
        contentLayouts.setMinDimensions(width, contentHeight());
        for (FrameLayout content : contents) {
            content.setMinDimensions(width, contentHeight());
        }
        arrangeElements();
    }

    public void setSizeToContent() {
        sizeToContent = true;
        contentLayouts.setMinDimensions(0, 0);
        for (FrameLayout content : contents) {
            content.setMinDimensions(0, 0);
        }
        arrangeElements();
    }

    private int contentHeight() {
        return Math.max(0, height - (TAB_HEIGHT + contentTopSpacing + CONTENT_TOP_BORDER_HEIGHT));
    }

    public <T extends LayoutElement> T addChild(T layoutElement, int tab) {
        return addChild(layoutElement, tab, LayoutSettings.defaults().alignHorizontallyCenter().alignVerticallyTop());
    }

    public <T extends LayoutElement> T addChild(T layoutElement, int tab, LayoutSettings layoutSettings) {
        return contents.get(tab).addChild(layoutElement, layoutSettings);
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
        return sizeToContent ? mainLayout.getWidth() : width;
    }

    @Override
    public int getHeight() {
        return sizeToContent ? mainLayout.getHeight() : height;
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
        int backgroundWidth = getWidth();
        int backgroundHeight = getHeight();
        graphics.fill(getX(), getY() + TAB_HEIGHT, getX() + backgroundWidth - 1, getY() + backgroundHeight - 1, ColorConstants.SCREEN_FRAME_BG);

        if (selected == -1) {
            graphics.hLine(getX(), getX() + backgroundWidth - 1, getY() + TAB_HEIGHT, ColorConstants.TAB_BORDER_NORMAL);
        } else {
            Tab tab = tabs.get(selected);
            graphics.hLine(getX(), tab.getX(), getY() + TAB_HEIGHT, ColorConstants.TAB_BORDER_NORMAL);
            graphics.hLine(tab.getX() + tab.getWidth(), getX() + backgroundWidth - 1, getY() + TAB_HEIGHT, ColorConstants.TAB_BORDER_NORMAL);
        }

        graphics.hLine(getX(), getX() + backgroundWidth - 1, getY() + backgroundHeight - 1, ColorConstants.TAB_BORDER_NORMAL);
        graphics.hLine(getX(), getY() + TAB_HEIGHT, getY() + backgroundHeight - 1, ColorConstants.TAB_BORDER_NORMAL);
        graphics.hLine(getX() + backgroundWidth - 1, getY() + TAB_HEIGHT, getY() + backgroundHeight - 1, ColorConstants.TAB_BORDER_NORMAL);
    }

    private int findNextEnabledTab(int startIndex, boolean forward) {
        if (forward) {
            for (int i = startIndex + 1; i < tabs.size(); ++i) {
                if (tabs.get(i).isEnabled()) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex - 1; i >= 0; --i) {
                if (tabs.get(i).isEnabled()) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean hasFocusedTab() {
        for (Tab tab : tabs) {
            if (tab.isFocused()) {
                return true;
            }
        }

        return false;
    }

    private static class Tab extends ButtonBase {
        private final Font font;
        private final TabbedBox parent;
        private final int index;
        private boolean selected = false;
        private boolean enabled;

        public Tab(TabbedBox parent, Font font, Component text, int index, int width, boolean enabled, Consumer<Integer> onPress) {
            super(0, 0, width, TAB_HEIGHT, text, (b) -> onPress.accept(index), Button.DEFAULT_NARRATION);
            this.parent = parent;
            this.index = index;
            this.font = font;
            this.enabled = enabled;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        private boolean isInteractive() {
            return enabled && parent.isInteractive();
        }

        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            if (!visible || !isInteractive()) {
                return null;
            }

            if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
                if (parent.hasFocusedTab()) {
                    return null;
                }

                return selected ? ComponentPath.leaf(this) : null;
            }

            if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation
                    && arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL) {
                if (!isFocused()) {
                    return selected ? ComponentPath.leaf(this) : null;
                }

                int nextIndex = parent.findNextEnabledTab(index, arrowNavigation.direction().isPositive());
                if (nextIndex == -1) {
                    return null;
                }

                return ComponentPath.leaf(parent.tabs.get(nextIndex));
            }

            if (!isFocused()) {
                return selected ? ComponentPath.leaf(this) : null;
            }

            return null;
        }

        @Override
        public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorConstants.SCREEN_FRAME_BG);

            graphics.hLine(getX(), getX() + getWidth(), getY(), isKeyboardFocused() ? ColorConstants.TAB_BORDER_FOCUSED : ColorConstants.TAB_BORDER_NORMAL);
            graphics.vLine(getX(), getY(), getY() + getHeight(), ColorConstants.TAB_BORDER_NORMAL);
            graphics.vLine(getX() + getWidth(), getY(), getY() + getHeight(), ColorConstants.TAB_BORDER_NORMAL);

            int labelColor = ColorConstants.TAB_TEXT_NORMAL;
            if (!isInteractive()) {
                labelColor = ColorConstants.TAB_TEXT_DISABLED;
            } else if (selected || isHoveredOrKeyboardFocused()) {
                labelColor = ColorConstants.TAB_TEXT_HIGHLIGHT;
            }

            graphics.drawCenteredString(font, getMessage(), getX() + (getWidth() + 1) / 2, getY() + 5, labelColor);
        }

        @Override
        public boolean isActive() {
            return isInteractive();
        }
    }
}
