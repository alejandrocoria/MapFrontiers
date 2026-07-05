package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TerritoryListSorting;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.ButtonBase;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.layout.MFLinearLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

public class SortToolbar extends MFLinearLayout {
    private SortButton selected;
    private Runnable onChange;

    public SortToolbar(Font font, Runnable onChange) {
        super(0, 0, Orientation.HORIZONTAL);
        spacing(8);

        this.onChange = onChange;

        for (TerritoryListSorting sort : TerritoryListSorting.VALUES) {
            int index = ClientConfig.getTerritoryListSortingValues().indexOf(sort);
            if (index == -1) {
                continue;
            }
            SortButton button = addChild(new SortButton(font, sort, ClientConfig.getTerritoryListSortingDirectionValues().get(index), this::buttonPressed));
            if (index == 0) {
                selected = button;
                selected.setSelected(true);
            }
        }
    }

    private void buttonPressed(Button button) {
        if (button instanceof SortButton sortButton) {
            java.util.List<TerritoryListSorting> sorting = new java.util.ArrayList<>(ClientConfig.getTerritoryListSortingValues());
            java.util.List<Boolean> direction = new java.util.ArrayList<>(ClientConfig.getTerritoryListSortingDirectionValues());
            if (sortButton == selected) {
                sortButton.changeDirection();
                direction.set(0, !direction.get(0));
            } else {
                selected.setSelected(false);

                int index = sorting.indexOf(sortButton.getSorting());
                sorting.add(0, sorting.remove(index));
                direction.add(0, direction.remove(index));

                selected = sortButton;
                selected.setSelected(true);
            }

            ClientConfig.setTerritoryListSortingValues(sorting);
            ClientConfig.setTerritoryListSortingDirectionValues(direction);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }

        onChange.run();
    }


    @ParametersAreNonnullByDefault
    public static class SortButton extends ButtonBase {
        private final MFLinearLayout layout = MFLinearLayout.horizontal();
        private boolean selected = false;
        private final TerritoryListSorting sorting;
        private boolean direction;
        private final StringWidget label;
        private final IconButton iconButton;

        public SortButton(Font font, TerritoryListSorting sorting, boolean direction, OnPress onPress) {
            super(0, 0, 0, 0, Component.empty(), onPress, DEFAULT_NARRATION);
            this.sorting = sorting;
            this.direction = direction;

            layout.defaultCellSetting().alignVerticallyMiddle();

            Component text = ClientConfig.getTranslatedEnum(sorting);
            this.label = layout.addChild(new StringWidget(text, font, 12));
            iconButton = layout.addChild(new IconButton(direction ? IconButton.Type.SortUp : IconButton.Type.SortDown, (b) -> {}));

            layout.arrangeElements();
            width = layout.getWidth();
            height = layout.getHeight();
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            iconButton.setAlpha(selected ? 1 : 0);
        }

        public void changeDirection() {
            direction = !direction;

            iconButton.setType(direction ? IconButton.Type.SortUp : IconButton.Type.SortDown);
        }

        public TerritoryListSorting getSorting() {
            return sorting;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (isHoveredOrKeyboardFocused()) {
                label.setColor(ColorConstants.SORT_TOOLBAR_TEXT_HIGHLIGHT);
                iconButton.setFocused(true);
            } else {
                label.setColor(ColorConstants.SORT_TOOLBAR_TEXT_NORMAL);
                iconButton.setFocused(false);
            }

            label.render(graphics, mouseX, mouseY, partialTicks);
            if (selected) {
                iconButton.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            layout.setX(x);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            layout.setY(y);
        }
    }
}
