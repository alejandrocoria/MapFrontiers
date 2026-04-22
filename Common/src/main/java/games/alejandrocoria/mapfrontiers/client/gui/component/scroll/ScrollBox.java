package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
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
    private static final int SCROLLBAR_AREA_WIDTH = 15;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int ELEMENT_GAP = 1;

    private final int elementHeight;
    private int scrollStart = 0;
    private int scrollHeight;
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

    public ScrollBox(int height, int elementWidth, int elementHeight) {
        super(0, 0, elementWidth + SCROLLBAR_AREA_WIDTH, Math.max(height, elementHeight + ELEMENT_GAP), Component.empty());
        elements = new ArrayList<>();
        selected = -1;
        focused = -1;
        this.elementHeight = elementHeight + ELEMENT_GAP;
        scrollHeight = this.height / this.elementHeight;
        this.height = scrollHeight * this.elementHeight;
    }

    public static ScrollBox withRows(int rows, int elementWidth, int elementHeight) {
        return new ScrollBox(heightForRows(rows, elementHeight), elementWidth, elementHeight);
    }

    public static int heightForRows(int rows, int elementHeight) {
        return Math.max(1, rows) * (elementHeight + ELEMENT_GAP);
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
        element.setY(getY() + elements.size() * elementHeight);
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

    public void selectElementIf(Predicate<ScrollElement> pred) {
        ScrollElement element = elements.stream()
                .filter(pred)
                .findFirst()
                .orElse(null);

        if (element == null) {
            selected = -1;
            focused = -1;
        } else {
            selectElement(element);
        }
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
            elements.get(i).setY(getY() + i * elementHeight);
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

            if (forward) {
                if (focused == elements.size() - 1) {
                    return null;
                } else {
                    ++focused;
                }
            } else {
                if (focused == 0) {
                    return null;
                } else if (focused == -1) {
                    focused = elements.size() - 1;
                } else {
                    --focused;
                }
            }
        } else {
            if (navigationEvent.getVerticalDirectionForInitialFocus().isPositive()) {
                focused = 0;
            } else {
                focused = elements.size() - 1;
            }
        }

        if (focused < scrollStart || focused >= scrollStart + scrollHeight) {
            if (focused < scrollStart) {
                scrollStart = focused;
            } else {
                scrollStart = focused - scrollHeight + 1;
            }
            updateScrollWindow();
            updateScrollBar();
        }

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
        for (int i = 0; i < elements.size(); ++i) {
            elements.get(i).setY(y + i * elementHeight);
        }
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
        scrollHeight = this.height / elementHeight;
        this.height = scrollHeight * elementHeight;
        updateScrollWindow();
        updateScrollBar();
    }

    public void setVisibleRows(int rows) {
        setHeight(Math.max(1, rows) * elementHeight);
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
            if (amount < 0 && scrollStart == 0) {
                return false;
            } else if (amount > 0 && scrollStart + scrollHeight >= elements.size()) {
                return false;
            }

            scrollStart += amount;
            updateScrollWindow();
            updateScrollBar();
            return true;
        }

        return false;
    }

    @Override
    protected int contentHeight() {
        return elementHeight;
    }

    @Override
    protected double scrollRate() {
        return elementHeight;
    }

    public void scrollBottom() {
        scrollStart = elements.size() - scrollHeight;
        scrollBarGrabbed = false;
        updateScrollWindow();
        updateScrollBar();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < elements.size(); ++i) {
            boolean isFocused = focused == i && isKeyboardFocused();
            elements.get(i).render(graphics, mouseX, mouseY, partialTicks, selected == i, isFocused);
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
                    } else if (action == ScrollElement.Action.Clicked) {
                        if (getSelectedElement() != element) {
                            selectElement(element);
                            if (elementClickedCallback != null) {
                                elementClickedCallback.accept(element);
                            }
                            return true;
                        }
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
                selectIndex(focused);
                if (selected != -1) {
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
                if (element.canBeDeleted()) {
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

            int newScrollStart = Math.round(((float) scrollBarPos) / height * elements.size());

            if (newScrollStart != scrollStart) {
                scrollStart = newScrollStart;
                updateScrollWindow();
            }
        }
    }

    protected boolean isHoveredOrKeyboardFocused() {
        return isHovered() || (isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard());
    }

    protected boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }

    private void updateScrollWindow() {
        if (elements.size() <= scrollHeight) {
            scrollStart = 0;
        } else {
            int bottomExtra = elements.size() - (scrollStart + scrollHeight);
            if (bottomExtra < 0) {
                scrollStart += bottomExtra;
            }

            if (scrollStart < 0) {
                scrollStart = 0;
            }
        }

        for (int i = 0; i < elements.size(); ++i) {
            if (i < scrollStart || i >= scrollStart + scrollHeight) {
                elements.get(i).visible = false;
            } else {
                elements.get(i).visible = true;
                elements.get(i).setY(getY() + (i - scrollStart) * elementHeight);
            }
        }
    }

    private void updateScrollBar() {
        if (elements.size() <= scrollHeight) {
            scrollBarHeight = 0;
            scrollBarHovered = false;
            scrollBarGrabbed = false;
            return;
        }

        scrollBarHeight = Math.round(((float) scrollHeight) / elements.size() * height);
        scrollBarPos = Math.round(((float) scrollStart) / elements.size() * height);
        if (scrollBarPos + scrollBarHeight > height) {
            scrollBarPos = height - scrollBarHeight;
        }
    }

    public static class ScrollElement implements ContainerEventHandler {
        public enum Action {
            None, Clicked, Deleted
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

        protected void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
            if (visible) {
                isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
                renderWidget(graphics, mouseX, mouseY, partialTicks, selected, focused);
                if (focused) {
                    graphics.hLine(x - 1, x + width, y - 1, ColorConstants.WHITE);
                    graphics.hLine(x - 1, x + width, y + height, ColorConstants.WHITE);
                    graphics.vLine(x - 1, y - 1, y + height, ColorConstants.WHITE);
                    graphics.vLine(x + width, y - 1, y + height, ColorConstants.WHITE);
                }
            } else {
                isHovered = false;
            }
        }

        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        }

        protected Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
            return Action.None;
        }

        protected boolean canBeDeleted() {
            return false;
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
