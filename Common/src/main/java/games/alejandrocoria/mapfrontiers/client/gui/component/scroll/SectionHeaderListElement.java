package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SectionHeaderListElement extends FrontierListRowElement {
    private static final int HEIGHT = 13;
    private static final int TITLE_Y = 3;
    private static final int TITLE_BUTTON_GAP = 2;

    private final Font font;
    private final int color;
    private final String title;
    private final IconButton addButton;
    private final boolean addEnabled;
    private boolean addRequested;

    public SectionHeaderListElement(String rowId, Font font, String title, int width, int color, boolean addEnabled,
                                    @Nullable Tooltip addTooltip) {
        super(rowId, width, HEIGHT);
        this.font = font;
        this.color = color;
        this.title = title;
        this.addEnabled = addEnabled;
        this.addButton = new IconButton(IconButton.Type.Add, (button) -> {});
        this.addButton.setTooltip(addTooltip);
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        int titleX = getTitleLeft();
        graphics.text(font, title, titleX, y + TITLE_Y, color);
        if (addEnabled) {
            addButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected boolean isKeyboardFocusable() {
        return false;
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (!visible || !isHovered) {
            return ScrollBox.ScrollElement.Action.None;
        }

        if (addEnabled && addButton.isMouseOver(event.x(), event.y())) {
            addRequested = true;
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
}
