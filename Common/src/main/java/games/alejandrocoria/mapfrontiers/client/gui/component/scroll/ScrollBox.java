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
import net.minecraft.client.gui.navigation.ScreenRectangle;
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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScrollBox extends AbstractContainerWidget {
    public enum HorizontalEdgeNavigation {
        EXIT_SCROLLBOX,
        KEEP_FOCUS,
        WRAP_WITHIN_ROW
    }

    public record FocusSnapshot(@Nullable Object rowKey,
                                @Nullable Object navigationKey,
                                int childIndex,
                                boolean restoreActiveFocus) {
    }

    private record NavigationTarget(int focusIndex, int rangeEndIndex) {
    }

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
    private HorizontalEdgeNavigation horizontalEdgeNavigation = HorizontalEdgeNavigation.EXIT_SCROLLBOX;
    private boolean navigationTargetsDirty = true;
    private @Nullable NavigationTarget homeTarget = null;
    private @Nullable NavigationTarget endTarget = null;
    private List<NavigationTarget> pageNavigationTargets = List.of();

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

    public void setHorizontalEdgeNavigation(HorizontalEdgeNavigation horizontalEdgeNavigation) {
        this.horizontalEdgeNavigation = horizontalEdgeNavigation;
    }

    public List<ScrollElement> getElements() {
        return elements;
    }

    public void addElement(ScrollElement element) {
        element.setX(getX());
        elements.add(element);
        invalidateNavigationTargets();
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    public void setSelectedElement(ScrollElement element) {
        selected = elements.indexOf(element);
    }

    @Nullable
    public ScrollElement getSelectedElement() {
        if (selected >= 0 && selected < elements.size()) {
            return elements.get(selected);
        }

        return null;
    }

    public int getSelectedIndex() {
        return selected;
    }

    @Nullable
    public ScrollElement getFocusedElement() {
        if (focused >= 0 && focused < elements.size()) {
            return elements.get(focused);
        }

        return null;
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

    public void setSelectedElementIf(Predicate<ScrollElement> pred) {
        ScrollElement element = elements.stream()
                .filter(pred)
                .findFirst()
                .orElse(null);

        if (element == null) {
            clearSelection();
        } else {
            setSelectedElement(element);
        }
    }

    public void clearSelection() {
        selected = -1;
    }

    public @Nullable FocusSnapshot captureFocusSnapshot() {
        ScrollElement focusedElement = getFocusedElement();
        if (focusedElement == null) {
            return null;
        }

        Object rowKey = focusedElement.getFocusRestoreKey();
        Object navigationKey = null;
        int childIndex = -1;

        if (focusedElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
            navigationKey = keyedFocusNavigation.getFocusedNavigationKey();
        } else {
            GuiEventListener focusedChild = focusedElement.getFocused();
            if (focusedChild != null) {
                int index = focusedElement.children().indexOf(focusedChild);
                if (index >= 0) {
                    childIndex = index;
                }
            }
        }

        return new FocusSnapshot(rowKey, navigationKey, childIndex, isFocused());
    }

    public @Nullable ComponentPath restoreFocusSnapshot(@Nullable FocusSnapshot focusSnapshot) {
        if (focusSnapshot == null) {
            return null;
        }

        int index = findIndexByFocusRestoreKey(focusSnapshot.rowKey());
        if (index == -1) {
            return null;
        }

        focused = index;
        scrollElementIntoView(index);

        if (!focusSnapshot.restoreActiveFocus()) {
            return null;
        }

        ScrollElement element = elements.get(index);
        if (element instanceof KeyedFocusNavigation) {
            return focusIndex(index, focusSnapshot.navigationKey());
        }

        if (focusSnapshot.childIndex() >= 0) {
            return focusIndexChild(index, focusSnapshot.childIndex());
        }

        return focusRowPath(index);
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
        invalidateNavigationTargets();

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
        invalidateNavigationTargets();
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active || elements.isEmpty()) {
            return null;
        }

        Object focusedNavigationKey = null;
        int focusedChild = -1;
        ScrollElement focusedElement = focused == -1 ? null : elements.get(focused);
        if (focusedElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
            focusedNavigationKey = keyedFocusNavigation.getFocusedNavigationKey();
        }

        List<GuiEventListener> children = focusedElement == null ? null : focusedElement.children();
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
                    if (focusedElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
                        ComponentPath keyedPath = keyedFocusNavigation.focusRelativeNavigationKey(navigationEvent, forward ? 1 : -1);
                        if (keyedPath != null) {
                            return ComponentPath.path(this, keyedPath);
                        }
                        return handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                    }

                    if (children != null) {
                        if (focusedChild == -1) {
                            if (!forward) {
                                return handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                            }
                            ComponentPath childPath = focusChildPath(navigationEvent, focused, 0, true);
                            return childPath != null ? childPath
                                    : handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                        }

                        int nextChild = focusedChild + (forward ? 1 : -1);
                        if (nextChild < 0) {
                            return handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                        }
                        if (nextChild >= children.size()) {
                            return handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                        }

                        ComponentPath childPath = focusChildPath(navigationEvent, focused, nextChild, forward);
                        return childPath != null ? childPath
                                : handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                    }
                    return handleHorizontalEdgeNavigation(navigationEvent, forward, focusedElement, children);
                }
            } else if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
                return null;
            }

            int nextFocused = findNextFocusableIndex(focused, forward);
            if (nextFocused == -1) {
                return null;
            }
            focused = nextFocused;
        } else {
            if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
                boolean forward = tabNavigation.forward();
                if (focused == -1 || focused >= elements.size() || !elements.get(focused).isKeyboardFocusable()) {
                    focused = forward ? findNextFocusableIndex(-1, true)
                            : findNextFocusableIndex(elements.size(), false);
                }
                if (focused == -1) {
                    return null;
                }

                scrollFocusedElementIntoView();

                ScrollElement targetElement = elements.get(focused);
                if (targetElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
                    Object defaultNavigationKey = keyedFocusNavigation.getDefaultNavigationKey();
                    if (defaultNavigationKey != null) {
                        ComponentPath keyedPath = keyedFocusNavigation.focusNavigationKey(navigationEvent, defaultNavigationKey);
                        if (keyedPath != null) {
                            return ComponentPath.path(this, keyedPath);
                        }
                    }
                }

                List<GuiEventListener> targetChildren = targetElement.children();
                if (!targetChildren.isEmpty()) {
                    int childIndex = forward ? 0 : targetChildren.size() - 1;
                    ComponentPath childPath = targetElement.focusPathAtIndex(navigationEvent, childIndex);
                    if (childPath != null) {
                        return ComponentPath.path(this, childPath);
                    }
                }

                return focusRowPath(focused);
            }

            if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation
                    && arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL) {
                boolean forward = arrowNavigation.direction().isPositive();
                if (focused == -1 || focused >= elements.size() || !elements.get(focused).isKeyboardFocusable()) {
                    focused = forward ? findNextFocusableIndex(-1, true)
                            : findNextFocusableIndex(elements.size(), false);
                }
                if (focused == -1) {
                    return null;
                }

                scrollFocusedElementIntoView();

                ScrollElement targetElement = elements.get(focused);
                if (targetElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
                    Object edgeNavigationKey = keyedFocusNavigation.getEdgeNavigationKey(forward);
                    if (edgeNavigationKey != null) {
                        ComponentPath keyedPath = keyedFocusNavigation.focusNavigationKey(navigationEvent, edgeNavigationKey);
                        if (keyedPath != null) {
                            return ComponentPath.path(this, keyedPath);
                        }
                    }
                }

                List<GuiEventListener> targetChildren = targetElement.children();
                if (!targetChildren.isEmpty()) {
                    int childIndex = forward ? 0 : targetChildren.size() - 1;
                    ComponentPath childPath = targetElement.focusPathAtIndex(navigationEvent, childIndex);
                    if (childPath != null) {
                        return ComponentPath.path(this, childPath);
                    }
                }

                return focusRowPath(focused);
            }

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

        scrollFocusedElementIntoView();

        ScrollElement targetElement = elements.get(focused);
        if (targetElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
            if (focusedNavigationKey != null) {
                ComponentPath keyedPath = keyedFocusNavigation.focusNavigationKey(navigationEvent, focusedNavigationKey);
                if (keyedPath != null) {
                    return ComponentPath.path(this, keyedPath);
                }
            }

            Object defaultNavigationKey = keyedFocusNavigation.getDefaultNavigationKey();
            if (defaultNavigationKey != null) {
                ComponentPath keyedPath = keyedFocusNavigation.focusNavigationKey(navigationEvent, defaultNavigationKey);
                if (keyedPath != null) {
                    return ComponentPath.path(this, keyedPath);
                }
            }
        }

        if (!targetElement.children().isEmpty()) {
            if (focusedChild == -1) {
                focusedChild = 0;
            }
            ComponentPath childPath = targetElement.focusPathAtIndex(navigationEvent, focusedChild);
            if (childPath != null) {
                return ComponentPath.path(this, childPath);
            }
        }

        return focusRowPath(focused);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(getX(), getY(), width, height);
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
                    int elementIndex = it.nextIndex();
                    ScrollElement element = it.next();
                    ScrollElement.Action action = element.mousePressed(event, doubleClick);
                    if (action == ScrollElement.Action.Deleted) {
                        setFocusedIndex(elementIndex);
                        if (elementDeletePressedCallback != null) {
                            elementDeletePressedCallback.accept(element);
                        } else {
                            removeElement(element, it);
                        }
                        return true;
                    } else if (action == ScrollElement.Action.Handled) {
                        setFocusedIndex(elementIndex);
                        if (elementClickedCallback != null) {
                            elementClickedCallback.accept(element);
                        }
                        return true;
                    } else if (action == ScrollElement.Action.Clicked) {
                        setFocusedIndex(elementIndex);
                        if (getSelectedElement() != element) {
                            setSelectedElement(element);
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
            if ((event.input() == GLFW.GLFW_KEY_HOME || event.input() == GLFW.GLFW_KEY_END) && isFocused()) {
                ComponentPath focusPath = focusBoundaryElement(event.input() == GLFW.GLFW_KEY_END);
                if (focusPath != null) {
                    focusPath.applyFocus(true);
                }
                return true;
            }

            if ((event.input() == GLFW.GLFW_KEY_PAGE_UP || event.input() == GLFW.GLFW_KEY_PAGE_DOWN) && isFocused()) {
                ComponentPath focusPath = focusPageNavigationTarget(event.input() == GLFW.GLFW_KEY_PAGE_DOWN);
                if (focusPath != null) {
                    focusPath.applyFocus(true);
                }
                return true;
            }

            if (event.isSelection()) {
                if (focused == -1 || !elements.get(focused).isKeyboardFocusable()) {
                    return true;
                }
                ScrollElement focusedElement = elements.get(focused);
                if (focusedElement.getFocused() != null) {
                    boolean handled = focusedElement.keyPressed(event);
                    if (handled && elementClickedCallback != null) {
                        elementClickedCallback.accept(focusedElement);
                    } else if (!handled && focusedElement instanceof KeyedFocusNavigation keyedFocusNavigation
                            && keyedFocusNavigation.isPrimaryActionFocused()) {
                        selected = focused;
                        if (elementClickedCallback != null) {
                            elementClickedCallback.accept(focusedElement);
                        }
                    }
                } else {
                    selected = focused;
                    if (selected != -1 && elements.get(selected).isKeyboardFocusable() && elementClickedCallback != null) {
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
            if (startExclusive + 1 >= elements.size()) {
                return -1;
            }
            for (int i = startExclusive + 1; i < elements.size(); ++i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    return i;
                }
            }
        } else {
            if (startExclusive - 1 < 0) {
                return -1;
            }
            for (int i = startExclusive - 1; i >= 0; --i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findIndexByFocusRestoreKey(@Nullable Object rowKey) {
        for (int i = 0; i < elements.size(); ++i) {
            if (Objects.equals(elements.get(i).getFocusRestoreKey(), rowKey)) {
                return i;
            }
        }

        return -1;
    }

    // Navigation targets used by Home/End/PageUp/PageDown.
    private void invalidateNavigationTargets() {
        navigationTargetsDirty = true;
    }

    private void ensureNavigationTargets() {
        if (!navigationTargetsDirty) {
            return;
        }

        homeTarget = null;
        endTarget = null;
        pageNavigationTargets = List.of();

        if (elements.isEmpty()) {
            navigationTargetsDirty = false;
            return;
        }

        int firstFocusableIndex = findNextFocusableIndex(-1, true);
        int lastFocusableIndex = findNextFocusableIndex(elements.size(), false);

        if (firstFocusableIndex != -1) {
            int rangeEndIndex = findContiguousNonFocusableBoundaryIndex(firstFocusableIndex, false);
            homeTarget = new NavigationTarget(firstFocusableIndex,
                    rangeEndIndex == firstFocusableIndex ? -1 : rangeEndIndex);
        }
        if (lastFocusableIndex != -1) {
            int rangeEndIndex = findContiguousNonFocusableBoundaryIndex(lastFocusableIndex, true);
            endTarget = new NavigationTarget(lastFocusableIndex,
                    rangeEndIndex == lastFocusableIndex ? -1 : rangeEndIndex);
        }

        List<NavigationTarget> navigationTargets = new ArrayList<>();
        for (int i = 0; i < elements.size(); ++i) {
            ScrollElement element = elements.get(i);
            if (!element.isPageNavigationTarget()) {
                continue;
            }

            int focusIndex = element.isKeyboardFocusable() ? i : findNextFocusableIndex(i, true);
            if (focusIndex == -1) {
                continue;
            }

            int rangeEndIndex = -1;
            Class<? extends ScrollElement> rangeEndType = element.getPageNavigationRangeEndType();
            if (rangeEndType != null) {
                rangeEndIndex = findNextPageNavigationRangeEnd(i, rangeEndType);
                if (rangeEndIndex < focusIndex) {
                    rangeEndIndex = -1;
                }
            }

            navigationTargets.add(new NavigationTarget(focusIndex, rangeEndIndex));
        }

        if (!navigationTargets.isEmpty()) {
            List<NavigationTarget> deduplicatedTargets = new ArrayList<>();
            for (NavigationTarget target : navigationTargets) {
                NavigationTarget previous = deduplicatedTargets.isEmpty() ? null
                        : deduplicatedTargets.get(deduplicatedTargets.size() - 1);
                if (previous != null && previous.focusIndex() == target.focusIndex()) {
                    deduplicatedTargets.set(deduplicatedTargets.size() - 1, new NavigationTarget(previous.focusIndex(),
                            Math.max(previous.rangeEndIndex(), target.rangeEndIndex())));
                } else {
                    deduplicatedTargets.add(target);
                }
            }
            pageNavigationTargets = List.copyOf(deduplicatedTargets);
        }

        navigationTargetsDirty = false;
    }

    private int findContiguousNonFocusableBoundaryIndex(int startIndex, boolean forward) {
        int boundaryIndex = startIndex;
        if (forward) {
            for (int i = startIndex + 1; i < elements.size(); ++i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    break;
                }
                boundaryIndex = i;
            }
        } else {
            for (int i = startIndex - 1; i >= 0; --i) {
                if (elements.get(i).isKeyboardFocusable()) {
                    break;
                }
                boundaryIndex = i;
            }
        }

        return boundaryIndex;
    }

    private int findNextPageNavigationRangeEnd(int startExclusive, Class<? extends ScrollElement> rangeEndType) {
        for (int i = startExclusive + 1; i < elements.size(); ++i) {
            if (rangeEndType.isInstance(elements.get(i))) {
                return i;
            }
        }

        return -1;
    }

    private int findPageNavigationTargetListIndex(boolean forward) {
        if (forward) {
            for (int i = 0; i < pageNavigationTargets.size(); ++i) {
                if (pageNavigationTargets.get(i).focusIndex() > focused) {
                    return i;
                }
            }
            return -1;
        }

        int currentOrPrevious = -1;
        for (int i = 0; i < pageNavigationTargets.size(); ++i) {
            int targetFocusIndex = pageNavigationTargets.get(i).focusIndex();
            if (targetFocusIndex > focused) {
                break;
            }
            currentOrPrevious = i;
        }

        if (currentOrPrevious == -1) {
            return -1;
        }
        if (pageNavigationTargets.get(currentOrPrevious).focusIndex() == focused) {
            return currentOrPrevious - 1;
        }
        return currentOrPrevious;
    }

    private int getFocusedChildIndex() {
        ScrollElement currentFocusedElement = getFocusedElement();
        if (currentFocusedElement == null) {
            return -1;
        }

        List<GuiEventListener> currentChildren = currentFocusedElement.children();
        for (int i = 0; i < currentChildren.size(); ++i) {
            if (currentChildren.get(i).isFocused()) {
                return i;
            }
        }

        return -1;
    }

    private @Nullable ComponentPath focusBoundaryElement(boolean end) {
        ensureNavigationTargets();
        NavigationTarget target = end ? endTarget : homeTarget;
        if (target == null) {
            return null;
        }

        return focusNavigationTarget(target, getFocusedChildIndex());
    }

    private @Nullable ComponentPath focusPageNavigationTarget(boolean forward) {
        if (focused == -1) {
            return null;
        }

        ensureNavigationTargets();
        if (pageNavigationTargets.isEmpty()) {
            return null;
        }

        int targetListIndex = findPageNavigationTargetListIndex(forward);
        if (targetListIndex == -1) {
            return null;
        }

        return focusNavigationTarget(pageNavigationTargets.get(targetListIndex), getFocusedChildIndex());
    }

    private @Nullable ComponentPath focusNavigationTarget(NavigationTarget target, int focusedChildIndex) {
        ComponentPath path = focusElementAtIndex(target.focusIndex(), focusedChildIndex);
        if (path == null) {
            return null;
        }

        scrollNavigationTargetIntoView(target);

        return path;
    }

    private void scrollFocusedElementIntoView() {
        if (focused == -1) {
            return;
        }

        scrollElementIntoView(focused);
        scrollEndTargetIntoViewIfFocused();
    }

    private void scrollEndTargetIntoViewIfFocused() {
        ensureNavigationTargets();
        if (endTarget != null && endTarget.focusIndex() == focused) {
            scrollNavigationTargetIntoView(endTarget);
        }
    }

    private void scrollNavigationTargetIntoView(NavigationTarget target) {
        if (target.rangeEndIndex() != -1) {
            scrollElementIntoView(target.rangeEndIndex());
            scrollElementIntoView(target.focusIndex());
        }
    }

    private @Nullable ComponentPath focusElementAtIndex(int index, int focusedChildIndex) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }

        ScrollElement element = elements.get(index);
        if (element instanceof KeyedFocusNavigation keyedFocusNavigation) {
            Object key = keyedFocusNavigation.getDefaultNavigationKey();
            return focusIndex(index, key);
        }

        List<GuiEventListener> children = element.children();
        if (!children.isEmpty()) {
            if (focusedChildIndex >= 0) {
                return focusIndexChild(index, focusedChildIndex);
            }
            return focusIndexChild(index, 0);
        }

        focused = index;
        scrollFocusedElementIntoView();
        return focusRowPath(index);
    }

    private void setFocusedIndex(int index) {
        focused = index >= 0 && index < elements.size() ? index : -1;
    }

    private @Nullable ComponentPath handleHorizontalEdgeNavigation(FocusNavigationEvent navigationEvent,
                                                                   boolean forward,
                                                                   @Nullable ScrollElement focusedElement,
                                                                   @Nullable List<GuiEventListener> children) {
        if (focusedElement == null) {
            return null;
        }

        return switch (horizontalEdgeNavigation) {
            case EXIT_SCROLLBOX -> null;
            case KEEP_FOCUS -> getCurrentFocusPath();
            case WRAP_WITHIN_ROW -> wrapHorizontalFocus(navigationEvent, forward, focusedElement, children);
        };
    }

    private @Nullable ComponentPath wrapHorizontalFocus(FocusNavigationEvent navigationEvent,
                                                        boolean forward,
                                                        ScrollElement focusedElement,
                                                        @Nullable List<GuiEventListener> children) {
        if (focusedElement instanceof KeyedFocusNavigation keyedFocusNavigation) {
            Object edgeNavigationKey = keyedFocusNavigation.getEdgeNavigationKey(!forward);
            if (edgeNavigationKey == null) {
                return ComponentPath.path(this, focusedElement.getCurrentFocusPath());
            }

            ComponentPath keyedPath = keyedFocusNavigation.focusNavigationKey(navigationEvent, edgeNavigationKey);
            return keyedPath != null ? ComponentPath.path(this, keyedPath)
                    : ComponentPath.path(this, focusedElement.getCurrentFocusPath());
        }

        if (children != null && !children.isEmpty()) {
            ComponentPath wrappedPath = focusChildPath(navigationEvent, focused,
                    forward ? 0 : children.size() - 1, forward);
            if (wrappedPath == null) {
                wrappedPath = focusChildPath(navigationEvent, focused, forward ? children.size() - 1 : 0, !forward);
            }
            return wrappedPath != null ? wrappedPath : ComponentPath.path(this, focusedElement.getCurrentFocusPath());
        }

        return ComponentPath.path(this, focusedElement.getCurrentFocusPath());
    }

    private ComponentPath focusRowPath(int rowIndex) {
        return ComponentPath.path(this, ComponentPath.leaf(elements.get(rowIndex)));
    }

    private @Nullable ComponentPath focusChildPath(FocusNavigationEvent navigationEvent, int rowIndex, int childIndex,
                                                   boolean forwardOnly) {
        ScrollElement element = elements.get(rowIndex);
        ComponentPath childPath = element.focusPathAtIndexDirectional(navigationEvent, childIndex, forwardOnly ? 1 : -1);
        if (childPath == null) {
            return null;
        }
        return ComponentPath.path(this, ComponentPath.path(element, childPath));
    }

    @Nullable
    private ComponentPath focusIndex(int index, @Nullable Object navigationKey) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }

        focused = index;
        scrollElementIntoView(index);

        ScrollElement element = elements.get(index);
        if (element instanceof KeyedFocusNavigation keyedFocusNavigation) {
            if (navigationKey != null) {
                ComponentPath path = keyedFocusNavigation.getFocusPathForKey(navigationKey);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }
            }

            Object defaultNavigationKey = keyedFocusNavigation.getDefaultNavigationKey();
            if (defaultNavigationKey != null) {
                ComponentPath path = keyedFocusNavigation.getFocusPathForKey(defaultNavigationKey);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }
            }
        }

        return focusRowPath(index);
    }

    @Nullable
    private ComponentPath focusIndexChild(int index, int childIndex) {
        if (index < 0 || index >= elements.size()) {
            return null;
        }

        focused = index;
        scrollElementIntoView(index);

        ScrollElement element = elements.get(index);
        List<GuiEventListener> children = element.children();
        if (children.isEmpty()) {
            return focusRowPath(index);
        }

        ComponentPath childPath = element.focusPathAtIndex(new FocusNavigationEvent.InitialFocus(), childIndex);
        if (childPath == null) {
            return null;
        }

        return ComponentPath.path(this, childPath);
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
            graphics.outline(x, y, width, height, ColorConstants.WHITE);
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

        protected boolean isPageNavigationTarget() {
            return false;
        }

        protected @Nullable Class<? extends ScrollElement> getPageNavigationRangeEndType() {
            return null;
        }

        public @Nullable Object getFocusRestoreKey() {
            return this;
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
            if (this.focused != null) {
                this.focused.setFocused(true);
            }
        }

        @Nullable
        public ComponentPath focusPathAtIndex(FocusNavigationEvent navigationEvent, int index) {
            if (this.children().isEmpty()) {
                return null;
            } else {
                ComponentPath path = focusPathAtIndexDirectional(navigationEvent, index, 1);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }

                path = focusPathAtIndexDirectional(navigationEvent, index - 1, -1);
                if (path != null) {
                    return ComponentPath.path(this, path);
                }

                return null;
            }
        }

        @Nullable
        private ComponentPath focusPathAtIndexDirectional(FocusNavigationEvent navigationEvent, int index, int delta) {
            List<? extends GuiEventListener> children = this.children();
            if (children.isEmpty()) {
                return null;
            }

            int clampedStart = Math.min(Math.max(index, 0), children.size() - 1);
            for (int i = clampedStart; i >= 0 && i < children.size(); i += delta) {
                ComponentPath path = children.get(i).nextFocusPath(navigationEvent);
                if (path != null) {
                    return path;
                }
            }

            return null;
        }

        @Override
        public ComponentPath getCurrentFocusPath() {
            if (this.children().isEmpty()) {
                return ComponentPath.leaf(this);
            } else {
                return this.getFocused() != null ? ComponentPath.path(this, this.getFocused().getCurrentFocusPath()) : ComponentPath.leaf(this);
            }
        }

        @Override
        public ScreenRectangle getRectangle() {
            return new ScreenRectangle(x, y, width, height);
        }
    }

    public interface KeyedFocusNavigation {
        @Nullable Object getFocusedNavigationKey();

        @Nullable Object getDefaultNavigationKey();

        @Nullable Object getEdgeNavigationKey(boolean forward);

        @Nullable ComponentPath getFocusPathForKey(Object key);

        boolean isPrimaryActionFocused();

        @Nullable ComponentPath focusNavigationKey(FocusNavigationEvent navigationEvent, Object key);

        @Nullable ComponentPath focusRelativeNavigationKey(FocusNavigationEvent navigationEvent, int delta);
    }
}
