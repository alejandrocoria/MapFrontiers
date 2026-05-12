package games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class TerritoryListRowElement extends ScrollBox.ScrollElement {
    private final String rowId;

    protected TerritoryListRowElement(String rowId, int width, int height) {
        super(width, height);
        this.rowId = rowId;
    }

    public String getRowId() {
        return rowId;
    }

    @Override
    public Object getFocusRestoreKey() {
        return rowId;
    }

    protected static class FocusTarget implements GuiEventListener {
        private final Supplier<ScreenRectangle> rectangleSupplier;
        private boolean focused;

        protected FocusTarget(Supplier<ScreenRectangle> rectangleSupplier) {
            this.rectangleSupplier = rectangleSupplier;
        }

        @Override
        public boolean isFocused() {
            return focused;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            return ComponentPath.leaf(this);
        }

        @Override
        public ComponentPath getCurrentFocusPath() {
            return ComponentPath.leaf(this);
        }

        @Override
        public ScreenRectangle getRectangle() {
            return rectangleSupplier.get();
        }
    }
}
