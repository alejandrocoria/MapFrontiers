package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class FrontierListRowElement extends ScrollBox.ScrollElement {
    private final String rowId;

    protected FrontierListRowElement(String rowId, int width, int height) {
        super(width, height);
        this.rowId = rowId;
    }

    public String getRowId() {
        return rowId;
    }
}
