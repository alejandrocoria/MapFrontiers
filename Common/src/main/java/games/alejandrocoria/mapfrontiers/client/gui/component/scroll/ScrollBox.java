package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScrollBox extends AbstractContainerWidget {
    private static final int SCROLLBAR_AREA_WIDTH = 12;
    private static final int SCROLLBAR_WIDTH = 8;

    private final int scrollStep;
    private int scrollOffset = 0;
    private int scrollBarPos = 0;
    private int scrollBarHeight = 0;
    private boolean scrollBarHovered = false;
    private boolean scrollBarGrabbed = false;
    private int scrollBarGrabbedYPos = 0;
    private final List<ScrollElement> elements;
    private int selected;
    private int focused;
    private Consumer<ScrollElement> elementClickedCallback;
    private Consumer<ScrollElement> elementDeletedCallback;
    private Consumer<ScrollElement> elementDeletePressedCallback;

    public ScrollBox(int viewportHeight, int elementWidth, int scrollStep) {
        super(0, 0, elementWidth + SCROLLBAR_AREA_WIDTH, Math.max(1, viewportHeight),
                Component.empty(), AbstractScrollArea.defaultSettings(SCROLLBAR_WIDTH));
        elements = new ArrayList<>();
        selected = -1;
        focused = -1;
        this.scrollStep = Math.max(1, scrollStep);
        this.height = Math.max(1, viewportHeight);
    }

    public static int rowsToHeight(int rows, int rowHeight) {
        return Math.max(1, rows) * Math.max(1, rowHeight);
    }

    public void setElementClickedCallback(Consumer<ScrollElement> callback) {
        elementClickedCallback = callback;
    }

    public void setElementDeletedCallback(Consumer<ScrollElement> callback) {
        elementDeletedCallback = callback;
    }

    public void setElementDeletePressedCallback(Consumer<ScrollElement> callback) {
        elementDeletePressedCallback = callback;
    }

    public List<ScrollElement> getElements() {
        return elements;
    }

    public void addElement(ScrollElement element) {
        element.setX(getX());
        elements.add(element);
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    public void selectElement(ScrollElement element) {
        selected = elements.indexOf(element);
        focused = selected;
    }

    @Nullable
    public ScrollElement getSelectedElement() {
        if (selected >= 0 && selected < elements.size()) {
            return elements.get(selected);
        }

        return null;
    }

    public void selectIndex(int index) {
        selected = Math.min(Math.max(index, 0), elements.size() - 1);
        focused = selected;
    }

    public int getSelectedIndex() {
        return selected;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
        clampScrollOffset();
        updateScrollWindow();
        updateScrollBar();
    }

    public void scrollSelectedElementIntoView() {
        scrollElementIntoView(selected);
    }

    public void selectElementIf(Predicate<ScrollElement> pred) {
        ScrollElement element = elements.stream()
                .filter(pred)
                .findFirst()
                .orElse(null);

        if (element == null) {
            deselectElement();
        } else {
            selectElement(element);
        }
    }

    public void deselectElement() {
        selected = -1;
        focused = -1;
    }

    public void removeElement(ScrollElement element) {
        ListIterator<ScrollElement> it = elements.listIterator();
        while (it.hasNext()) {
            if (it.next() == element) {
                removeElement(element, it);
                return;
            }
        }
    }

    private void removeElement(ScrollElement element, ListIterator<ScrollElement> it) {
        it.remove();

        if (selected == elements.size()) {
            selected = elements.size() - 1;
        }

        for (int i = 0; i < elements.size(); ++i) {
            elements.get(i).setY(getElementTop(i) - scrollOffset + getY());
        }

        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();

        if (elementDeletedCallback != null) {
            elementDeletedCallback.accept(element);
        }

        if (selected >= 0 && elementClickedCallback != null) {
            elementClickedCallback.accept(getSelectedElement());
        }

        focused = selected;
    }

    public void removeAll() {
        elements.clear();
        selected = -1;
        focused = -1;
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active || elements.isEmpty()) {
            return null;
        }

        int focusedChild = -1;
        List<GuiEventListener> children = focused == -1 ? null : elements.get(focused).children();
        if (children != null) {
            if (children.isEmpty()) {
                children = null;
            } else {
                for (int i = 0; i < children.size(); ++i) {
                    if (children.get(i).isFocused()) {
                        focusedChild = i;
                        break;
                    }
                }
            }
        }

        if (isFocused()) {
            boolean forward = true;
            if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
                forward = arrowNavigation.direction().isPositive();
                if (arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL) {
                    if (children != null) {
                        focusedChild += forward ? 1 : -1;
                        if (focusedChild < 0 || focusedChild >= children.size()) {
                            return null;
                        } else {
                            return ComponentPath.path(this, elements.get(focused).focusPathAtIndex(navigationEvent, focusedChild));
                        }
                    }
                    return null;
                }
            } else if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
                forward = tabNavigation.forward();
            }

            int nextFocused = findNextFocusableIndex(focused, forward);
            if (nextFocused == -1) {
                return null;
            }
            focused = nextFocused;
        } else {
            boolean forward = navigationEvent.getVerticalDirectionForInitialFocus().isPositive();
            if (forward) {
                focused = findNextFocusableIndex(-1, true);
            } else {
                focused = findNextFocusableIndex(elements.size(), false);
            }

            if (focused == -1) {
                return null;
            }
        }

        scrollElementIntoView(focused);

        if (!elements.get(focused).children().isEmpty()) {
            if (focusedChild == -1) {
                focusedChild = 0;
            }
            return ComponentPath.path(this, elements.get(focused).focusPathAtIndex(navigationEvent, focusedChild));
        }

        return ComponentPath.path(this, ComponentPath.leaf(elements.get(focused)));
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        for (ScrollElement element : elements) {
            element.setX(x);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateScrollWindow();
    }

    @Override
    public void setSize(int elementWidth, int height) {
        super.setSize(elementWidth + SCROLLBAR_AREA_WIDTH, height);
        setHeight(height);
    }

    @Override
    public void setWidth(int elementWidth) {
        super.setWidth(elementWidth + SCROLLBAR_AREA_WIDTH);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.height = Math.max(1, this.height);
        clampScrollOffset();
        updateScrollWindow();
        updateScrollBar();
    }

    public void setViewportHeight(int viewportHeight) {
        setHeight(viewportHeight);
    }

    public void setViewportSize(int elementWidth, int viewportHeight) {
        setSize(elementWidth, viewportHeight);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return elements;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && (isHovered || scrollBarHovered) && !scrollBarGrabbed) {
            int amount = (int) -vDelta;
            if (amount < 0 && scrollOffset == 0) {
                return false;
            } else if (amount > 0 && scrollOffset >= getMaxScrollOffset()) {
                return false;
            }

            scrollOffset += amount * scrollStep;
            clampScrollOffset();
            updateScrollWindow();
            updateScrollBar();
            return true;
        }

        return false;
    }

    @Override
    protected int contentHeight() {
        return getContentHeight();
    }

    @Override
    protected double scrollRate() {
        return scrollStep;
    }

    public void scrollBottom() {
        scrollOffset = getMaxScrollOffset();
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int clipLeft = getX();
        int clipTop = getY();
        int clipRight = getX() + width - SCROLLBAR_AREA_WIDTH;
        int clipBottom = getY() + height;

        if (!elements.isEmpty()) {
            graphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            try {
                for (int i = 0; i < elements.size(); ++i) {
                    boolean isFocused = focused == i && isKeyboardFocused();
                    elements.get(i).render(graphics, mouseX, mouseY, partialTicks, selected == i, isFocused,
                            clipLeft, clipTop, clipRight, clipBottom);
                }
            } finally {
                graphics.disableScissor();
            }
        }

        if (scrollBarHeight > 0) {
            scrollBarHovered = mouseX >= getX() + width - SCROLLBAR_WIDTH
                            && mouseY >= getY()
                            && mouseX < getX() + width
                            && mouseY < getY() + height;

            int barColor = ColorConstants.SCROLLBAR;
            if (scrollBarGrabbed) {
                barColor = ColorConstants.SCROLLBAR_GRABBED;
            } else if (scrollBarHovered) {
                barColor = ColorConstants.SCROLLBAR_HOVERED;
            }

            graphics.fill(getX() + width - SCROLLBAR_WIDTH, getY(), getX() + width, getY() + height,
                    ColorConstants.SCROLLBAR_BG);
            graphics.fill(getX() + width - SCROLLBAR_WIDTH, getY() + scrollBarPos, getX() + width,
                    getY() + scrollBarPos + scrollBarHeight, barColor);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (active && visible && isValidClickButton(event.buttonInfo())) {
            if (scrollBarHeight > 0 && event.x() >= getX() + width - SCROLLBAR_WIDTH && event.y() >= getY()
                    && event.x() < getX() + width && event.y() < getY() + height) {
                if (event.y() < getY() + scrollBarPos) {
                    mouseScrolled(event.x(), event.y(), 0, 1);
                } else if (event.y() > getY() + scrollBarPos + scrollBarHeight) {
                    mouseScrolled(event.x(), event.y(), 0, -1);
                } else {
                    scrollBarGrabbed = true;
                    scrollBarGrabbedYPos = (int) event.y() - getY() - scrollBarPos;
                }

                return true;
            }

            if (isHovered && !scrollBarGrabbed) {
                ListIterator<ScrollElement> it = elements.listIterator();
                while (it.hasNext()) {
                    ScrollElement element = it.next();
                    ScrollElement.Action action = element.mousePressed(event, doubleClick);
                    if (action == ScrollElement.Action.Deleted) {
                        if (elementDeletePressedCallback != null) {
                            elementDeletePressedCallback.accept(element);
                        } else {
                            removeElement(element, it);
                        }
                        return true;
                    } else if (action == ScrollElement.Action.Handled) {
                        if (elementClickedCallback != null) {
                            elementClickedCallback.accept(element);
                        }
                        return true;
                    } else if (action == ScrollElement.Action.Clicked) {
                        if (getSelectedElement() != element) {
                            selectElement(element);
                        }
                        if (elementClickedCallback != null) {
                            elementClickedCallback.accept(element);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Custom mouseReleased to be called from the Screen.
    public void mouseReleased() {
        if (visible && scrollBarHeight > 0 && scrollBarGrabbed) {
            scrollBarGrabbed = false;
            updateScrollBar();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.active && this.visible) {
            if (event.isSelection()) {
                if (focused == -1 || !elements.get(focused).isKeyboardFocusable()) {
                    return true;
                }
                selectIndex(focused);
                if (selected != -1 && elements.get(selected).isKeyboardFocusable()) {
                    ScrollElement focusedElement = elements.get(focused);
                    if (!focusedElement.children().isEmpty()) {
                        focusedElement.keyPressed(event);
                    } else if (elementClickedCallback != null) {
                        elementClickedCallback.accept(getSelectedElement());
                    }
                }
                return true;
            }

            if (event.input() == GLFW.GLFW_KEY_DELETE && focused != -1) {
                ScrollElement element = elements.get(focused);
                if (element.isKeyboardFocusable() && element.canBeDeleted()) {
                    if (elementDeletePressedCallback != null) {
                        elementDeletePressedCallback.accept(element);
                    } else {
                        removeElement(element);
                    }
                    return true;
                }
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollBarHeight > 0 && scrollBarGrabbed) {
            int delta = (int) event.y() - getY() - scrollBarPos - scrollBarGrabbedYPos;

            if (delta == 0) {
                return;
            }

            scrollBarPos += delta;
            if (scrollBarPos < 0) {
                scrollBarPos = 0;
            } else if (scrollBarPos + scrollBarHeight > height) {
                scrollBarPos = height - scrollBarHeight;
            }

            if (height == scrollBarHeight) {
                scrollOffset = 0;
            } else {
                scrollOffset = Math.round(((float) scrollBarPos) / (height - scrollBarHeight) * getMaxScrollOffset());
            }
            clampScrollOffset();
            updateScrollWindow();
        }
    }

    protected boolean isHoveredOrKeyboardFocused() {
        return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
    }

    protected boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }

    private void updateScrollWindow() {
        clampScrollOffset();

        int currentY = getY() - scrollOffset;
        int viewportBottom = getY() + height;
        for (ScrollElement element : elements) {
            int elementBottom = currentY + element.getHeight();
            element.visible = elementBottom > getY() && currentY < viewportBottom;
            element.setY(currentY);
            currentY = elementBottom;
        }
    }

    private void updateScrollBar() {
        int contentHeight = getContentHeight();
        if (contentHeight <= height) {
            scrollBarHeight = 0;
            scrollBarHovered = false;
            scrollBarGrabbed = false;
            return;
        }

        scrollBarHeight = Math.max(10, Math.round(((float) height) / contentHeight * height));
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset == 0) {
            scrollBarPos = 0;
        } else {
            scrollBarPos = Math.round(((float) scrollOffset) / maxScrollOffset * (height - scrollBarHeight));
        }
        if (scrollBarPos + scrollBarHeight > height) {
            scrollBarPos = height - scrollBarHeight;
        }
    }

    private void scrollElementIntoView(int index) {
        if (index < 0 || index >= elements.size()) {
            return;
        }

        int elementTop = getElementTop(index);
        int elementBottom = elementTop + elements.get(index).getHeight();

        if (elementTop < scrollOffset) {
            scrollOffset = elementTop;
        } else if (elementBottom > scrollOffset + height) {
            scrollOffset = elementBottom - height;
        }

        clampScrollOffset();
        updateScrollWindow();
        updateScrollBar();
    }

    private int getElementTop(int index) {
        int top = 0;
        for (int i = 0; i < index; ++i) {
            top += elements.get(i).getHeight();
        }
        return top;
    }

    private int getContentHeight() {
        if (elements.isEmpty()) {
            return 0;
        }

        int contentHeight = 0;
        for (ScrollElement element : elements) {
            contentHeight += element.getHeight();
        }
        return Math.max(0, contentHeight);
    }

    private int getMaxScrollOffset() {
        return Math.max(0, getContentHeight() - height);
    }

    private void clampScrollOffset() {
        if (scrollOffset < 0) {
            scrollOffset = 0;
        } else {
            scrollOffset = Math.min(scrollOffset, getMaxScrollOffset());
        }
    }

    private int findNextFocusableIndex(int startExclusive, boolean forward) {
        if (forward) {
            for (int i = Math.max(startExclusive + 1, 0); i < elements.size(); ++i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    return i;
                }
            }
        } else {
            for (int i = Math.min(startExclusive - 1, elements.size() - 1); i >= 0; --i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static class ScrollElement implements ContainerEventHandler {
        public enum Action {
            None, Clicked, Deleted, Handled
        }

        protected boolean visible = true;
        protected int x = 0;
        protected int y = 0;
        protected boolean isHovered = false;
        protected GuiEventListener focused;
        protected boolean dragging;
        protected final int height;
        protected final int width;

        protected ScrollElement(int width, int height) {
            this.width = width;
            this.height = height;
        }

        protected void setX(int x) {
            this.x = x;
        }

        protected void setY(int y) {
            this.y = y;
        }

        public int getHeight() {
            return height;
        }

        protected void render(GuiGraphicsExtractor graphics,
                              int mouseX,
                              int mouseY,
                              float partialTicks,
                              boolean selected,
                              boolean focused,
                              int clipLeft,
                              int clipTop,
                              int clipRight,
                              int clipBottom) {
            if (visible) {
                int hoverLeft = Math.max(x, clipLeft);
                int hoverTop = Math.max(y, clipTop);
                int hoverRight = Math.min(x + width, clipRight);
                int hoverBottom = Math.min(y + height, clipBottom);
                isHovered = mouseX >= hoverLeft && mouseY >= hoverTop && mouseX < hoverRight && mouseY < hoverBottom;
                extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks, selected, focused);
                if (focused) {
                    drawFocusOutline(graphics);
                }
            } else {
                isHovered = false;
            }
        }

        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        }

        protected void drawFocusOutline(GuiGraphicsExtractor graphics) {
            int right = x + width - 1;
            int bottom = y + height - 1;
            graphics.horizontalLine(x, right, y, ColorConstants.WHITE);
            graphics.horizontalLine(x, right, bottom, ColorConstants.WHITE);
            graphics.verticalLine(x, y, bottom, ColorConstants.WHITE);
            graphics.verticalLine(right, y, bottom, ColorConstants.WHITE);
        }

        protected Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
            return Action.None;
        }

        protected boolean canBeDeleted() {
            return false;
        }

        protected boolean isKeyboardFocusable() {
            return true;
        }

        public List<GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public boolean isDragging() {
            return dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        @Override
        public GuiEventListener getFocused() {
            return focused;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener guiEventListener) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }

            this.focused = guiEventListener;
        }

        @Nullable
        public ComponentPath focusPathAtIndex(FocusNavigationEvent navigationEvent, int index) {
            if (this.children().isEmpty()) {
                return null;
            } else {
                ComponentPath path = this.children().get(Math.min(index, this.children().size() - 1)).nextFocusPath(navigationEvent);
                for (int i = index; i < this.children().size() && path == null; ++i) {
                    if (this.children().get(i).isFocused()) {
                        break;
                    }
                    path = this.children().get(i).nextFocusPath(navigationEvent);
                }
                for (int i = index - 1; i > 0 && path == null; --i) {
                    if (this.children().get(i).isFocused()) {
                        break;
                    }
                    path = this.children().get(i).nextFocusPath(navigationEvent);
                }

                return ComponentPath.path(this, path);
            }
        }

        @Override
        public ComponentPath getCurrentFocusPath() {
            if (this.children().isEmpty()) {
                return ComponentPath.leaf(this);
            } else {
                return this.getFocused() != null ? ComponentPath.path(this, this.getFocused().getCurrentFocusPath()) : ComponentPath.leaf(this);
            }
        }
    }
}
