package games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SectionHeaderListElement extends TerritoryListRowElement implements ScrollBox.KeyedFocusNavigation {
    private static final int HEIGHT = 12;
    private static final int TITLE_Y = 2;
    private static final int TITLE_BUTTON_GAP = 3;

    private final Font font;
    private final int color;
    private final String title;
    private final IconButton addButton;
    private final boolean addEnabled;
    private final List<GuiEventListener> children;
    private boolean addRequested;

    public SectionHeaderListElement(String rowId, Font font, String title, int width, int color, boolean addEnabled,
                                    @Nullable Tooltip addTooltip) {
        super(rowId, width, HEIGHT);
        this.font = font;
        this.color = color;
        this.title = title;
        this.addEnabled = addEnabled;
        this.addButton = new IconButton(IconButton.Type.Add, (button) -> requestAdd());
        this.addButton.setTooltip(addTooltip);
        this.children = addEnabled ? List.of(addButton) : List.of();
    }

    public boolean consumeAddRequested() {
        boolean requested = addRequested;
        addRequested = false;
        return requested;
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        addButton.setX(getAddLeft());
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        addButton.setY(y);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        int titleX = getTitleLeft();
        graphics.drawString(font, title, titleX, y + TITLE_Y, color);
        if (addEnabled) {
            addButton.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void drawFocusOutline(GuiGraphics graphics) {
    }

    @Override
    protected boolean isKeyboardFocusable() {
        return addEnabled;
    }

    @Override
    protected boolean isPageNavigationTarget() {
        return true;
    }

    @Override
    public List<GuiEventListener> children() {
        return children;
    }

    @Override
    public @Nullable Object getFocusedNavigationKey() {
        return addEnabled ? TerritoryListFocusKey.MAIN : null;
    }

    @Override
    public @Nullable Object getDefaultNavigationKey() {
        return addEnabled ? TerritoryListFocusKey.MAIN : null;
    }

    @Override
    public @Nullable Object getEdgeNavigationKey(boolean forward) {
        return addEnabled ? TerritoryListFocusKey.MAIN : null;
    }

    @Override
    public @Nullable ComponentPath getFocusPathForKey(Object key) {
        if (!addEnabled || key != TerritoryListFocusKey.MAIN) {
            return null;
        }

        return ComponentPath.path(this, ComponentPath.leaf(addButton));
    }

    @Override
    public boolean isPrimaryActionFocused() {
        return false;
    }

    @Override
    public @Nullable ComponentPath focusNavigationKey(FocusNavigationEvent navigationEvent, Object key) {
        return getFocusPathForKey(key);
    }

    @Override
    public @Nullable ComponentPath focusRelativeNavigationKey(FocusNavigationEvent navigationEvent, int delta) {
        return null;
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY, int button) {
        if (!visible || !isHovered) {
            return ScrollBox.ScrollElement.Action.None;
        }

        if (addEnabled && addButton.isMouseOver(mouseX, mouseY)) {
            requestAdd();
            return ScrollBox.ScrollElement.Action.Handled;
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    private int getTitleLeft() {
        return x + Math.max(0, (width - font.width(title)) / 2);
    }

    private int getAddLeft() {
        return getTitleLeft() + font.width(title) + TITLE_BUTTON_GAP;
    }

    private void requestAdd() {
        addRequested = true;
    }
}
