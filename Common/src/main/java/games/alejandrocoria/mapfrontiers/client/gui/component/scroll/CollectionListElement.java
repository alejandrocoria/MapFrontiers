package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectionListElement extends FrontierListRowElement {
    private static final int CHEVRON_CLICK_WIDTH = 18;
    private static final int CONTENT_X = 14;
    private static final int TITLE_Y = 4;
    private static final int COUNTERS_GAP = 6;

    private final Font font;
    private final @Nullable CollectionData collection;
    private final boolean virtualRow;
    private final String title;
    private final String counters;
    private final boolean collapsed;
    private boolean collapseToggleRequested;

    public CollectionListElement(String rowId,
                                 Font font,
                                 @Nullable CollectionData collection,
                                 boolean virtualRow,
                                 String title,
                                 String counters,
                                 boolean collapsed,
                                 int width) {
        super(rowId, width, 15);
        this.font = font;
        this.collection = collection;
        this.virtualRow = virtualRow;
        this.title = title;
        this.counters = counters;
        this.collapsed = collapsed;
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public boolean isVirtualRow() {
        return virtualRow;
    }

    public boolean consumeCollapseToggleRequested() {
        boolean requested = collapseToggleRequested;
        collapseToggleRequested = false;
        return requested;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        int textColor = selected ? ColorConstants.TEXT_HIGHLIGHT : ColorConstants.TEXT;
        if (selected) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_SELECTED);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        graphics.text(font, collapsed ? ">" : "v", x + 2, y + TITLE_Y, textColor);
        int titleColor = virtualRow ? ColorConstants.TEXT_HIGHLIGHT : textColor;
        graphics.text(font, title, x + CONTENT_X, y + TITLE_Y, titleColor);

        int countersX = x + CONTENT_X + font.width(title) + COUNTERS_GAP;
        graphics.text(font, counters, countersX, y + TITLE_Y, ColorConstants.TEXT_DIMENSION);
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            if (event.x() >= x && event.x() < x + CHEVRON_CLICK_WIDTH) {
                collapseToggleRequested = true;
            }
            return ScrollBox.ScrollElement.Action.Clicked;
        }

        return ScrollBox.ScrollElement.Action.None;
    }
}
