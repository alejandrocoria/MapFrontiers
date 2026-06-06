package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleSlider;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityField;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class CollectionVisibilityDialog extends PanelDialog {
    private static final Component GENERAL_LABEL = Component.translatable("mapfrontiers.general");
    private static final Component SHOW_COLLECTION_LABEL = Component.translatable("mapfrontiers.show_collection");
    private static final Component FULLSCREEN_LABEL = Component.translatable("mapfrontiers.fullscreen");
    private static final Component MINIMAP_LABEL = Component.translatable("mapfrontiers.minimap");
    private static final Component WEBMAP_LABEL = Component.translatable("mapfrontiers.webmap");
    private static final Component SHOW_NAME_LABEL = Component.translatable("mapfrontiers.show_name");
    private static final Component SHOW_OWNER_LABEL = Component.translatable("mapfrontiers.show_owner");
    private static final Component SHOW_BANNER_LABEL = Component.translatable("mapfrontiers.show_banner");
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component NOT_VISIBLE_LABEL = Component.translatable("mapfrontiers.not_visible");
    private static final int COLUMN_SPACING = 6;
    private static final int VISIBILITY_ZOOM_SLIDER_WIDTH = 92;

    private final CollectionVisibilityData visibilityData;
    private final @Nullable CollectionVisibilityMask visibilityMask;
    private final SaveCallback saveCallback;

    public CollectionVisibilityDialog(CollectionVisibilityData visibilityData,
                                      SaveCallback saveCallback) {
        super();
        this.visibilityData = new CollectionVisibilityData(visibilityData);
        visibilityMask = null;
        this.saveCallback = saveCallback;
    }

    public CollectionVisibilityDialog(CollectionVisibilityData visibilityData, CollectionVisibilityMask visibilityMask,
                                      SaveCallback saveCallback) {
        super();
        this.visibilityData = new CollectionVisibilityData(visibilityData);
        this.visibilityMask = new CollectionVisibilityMask(visibilityMask);
        this.saveCallback = saveCallback;
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout mainColumns = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_LARGE);
        mainLayout.addChild(mainColumns);

        LinearLayout generalColumn = createColumn(mainColumns, GENERAL_LABEL);
        GridLayout generalGrid = createGrid(generalColumn);
        createBooleanWidgets(generalGrid, 1, SHOW_COLLECTION_LABEL,
                visibilityData::isVisible, visibilityData::setVisible,
                createMaskBinding(CollectionVisibilityField.Visible));

        LinearLayout fullscreenColumn = createColumn(mainColumns, FULLSCREEN_LABEL);
        createZoomRow(fullscreenColumn,
                visibilityData::getFullscreenZoom, visibilityData::setFullscreenZoom,
                createMaskBinding(CollectionVisibilityField.FullscreenZoom));
        GridLayout fullscreenGrid = createGrid(fullscreenColumn);
        int row = 1;
        createBooleanWidgets(fullscreenGrid, row++, SHOW_NAME_LABEL,
                visibilityData::getFullscreenName, visibilityData::setFullscreenName,
                createMaskBinding(CollectionVisibilityField.FullscreenName));
        createBooleanWidgets(fullscreenGrid, row++, SHOW_OWNER_LABEL,
                visibilityData::getFullscreenOwner, visibilityData::setFullscreenOwner,
                createMaskBinding(CollectionVisibilityField.FullscreenOwner));
        createBooleanWidgets(fullscreenGrid, row, SHOW_BANNER_LABEL,
                visibilityData::getFullscreenBanner, visibilityData::setFullscreenBanner,
                createMaskBinding(CollectionVisibilityField.FullscreenBanner));

        LinearLayout minimapColumn = createColumn(mainColumns, MINIMAP_LABEL);
        createZoomRow(minimapColumn,
                visibilityData::getMinimapZoom, visibilityData::setMinimapZoom,
                createMaskBinding(CollectionVisibilityField.MinimapZoom));
        GridLayout minimapGrid = createGrid(minimapColumn);
        row = 1;
        createBooleanWidgets(minimapGrid, row++, SHOW_NAME_LABEL,
                visibilityData::getMinimapName, visibilityData::setMinimapName,
                createMaskBinding(CollectionVisibilityField.MinimapName));
        createBooleanWidgets(minimapGrid, row++, SHOW_OWNER_LABEL,
                visibilityData::getMinimapOwner, visibilityData::setMinimapOwner,
                createMaskBinding(CollectionVisibilityField.MinimapOwner));
        createBooleanWidgets(minimapGrid, row, SHOW_BANNER_LABEL,
                visibilityData::getMinimapBanner, visibilityData::setMinimapBanner,
                createMaskBinding(CollectionVisibilityField.MinimapBanner));

        LinearLayout webmapColumn = createColumn(mainColumns, WEBMAP_LABEL);
        createZoomRow(webmapColumn,
                visibilityData::getWebmapZoom, visibilityData::setWebmapZoom,
                createMaskBinding(CollectionVisibilityField.WebmapZoom));
        GridLayout webmapGrid = createGrid(webmapColumn);
        row = 1;
        createBooleanWidgets(webmapGrid, row++, SHOW_NAME_LABEL,
                visibilityData::getWebmapName, visibilityData::setWebmapName,
                createMaskBinding(CollectionVisibilityField.WebmapName));
        createBooleanWidgets(webmapGrid, row++, SHOW_OWNER_LABEL,
                visibilityData::getWebmapOwner, visibilityData::setWebmapOwner,
                createMaskBinding(CollectionVisibilityField.WebmapOwner));
        createBooleanWidgets(webmapGrid, row, SHOW_BANNER_LABEL,
                visibilityData::getWebmapBanner, visibilityData::setWebmapBanner,
                createMaskBinding(CollectionVisibilityField.WebmapBanner));

        addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
    }

    private LinearLayout createColumn(LinearLayout mainColumns, Component title) {
        LinearLayout column = LinearLayout.vertical().spacing(COLUMN_SPACING);
        column.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(column);
        column.addChild(new StringWidget(title.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));
        return column;
    }

    private GridLayout createGrid(LinearLayout column) {
        GridLayout grid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        grid.defaultCellSetting().alignVerticallyMiddle();
        column.addChild(grid);
        return grid;
    }

    private void createZoomRow(LinearLayout column,
                               IntSupplier getter, IntConsumer setter,
                               @Nullable BooleanMaskBinding maskBinding) {
        LinearLayout zoomContainer = LinearLayout.vertical();
        column.addChild(zoomContainer);
        zoomContainer.addChild(SpacerElement.height(LayoutConstants.SPACING_SMALL));

        LinearLayout zoomRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        zoomRow.defaultCellSetting().alignVerticallyMiddle();
        zoomContainer.addChild(zoomRow);

        SimpleSlider slider = new SimpleSlider(font, VISIBILITY_ZOOM_SLIDER_WIDTH, "mapfrontiers.zoom",
                CollectionVisibilityData.getZoomLevels(), getter.getAsInt(), (zoom, dragging) -> {
            setter.accept(zoom);
        }, CollectionVisibilityDialog::formatZoomLabel);

        if (maskBinding == null) {
            zoomRow.addChild(slider);
        } else {
            CheckBoxButton checkBox = new CheckBoxButton(maskBinding.getter().get(), b -> {
                maskBinding.setter().accept(b.isChecked());
                slider.active = b.isChecked();
            });
            zoomRow.addChild(checkBox);
            zoomRow.addChild(slider);
            slider.active = checkBox.isChecked();
        }
    }

    private void createBooleanWidgets(GridLayout layout, int row, Component label,
                                      Supplier<Boolean> getter, Consumer<Boolean> setter,
                                      @Nullable BooleanMaskBinding maskBinding) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, b -> setter.accept(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(getter.get() ? 0 : 1);
        layout.addChild(button, row, 2);

        bindMask(layout, row, maskBinding, button);
    }

    private void bindMask(GridLayout layout, int row, @Nullable BooleanMaskBinding maskBinding,
                          net.minecraft.client.gui.components.AbstractWidget widget) {
        if (maskBinding == null) {
            return;
        }

        CheckBoxButton checkBox = new CheckBoxButton(maskBinding.getter().get(), b -> {
            maskBinding.setter().accept(b.isChecked());
            widget.active = b.isChecked();
        });
        layout.addChild(checkBox, row, 1);
        widget.active = checkBox.isChecked();
    }

    private @Nullable BooleanMaskBinding createMaskBinding(CollectionVisibilityField field) {
        if (visibilityMask == null) {
            return null;
        }

        return new BooleanMaskBinding(() -> visibilityMask.has(field), enabled -> visibilityMask.set(field, enabled));
    }

    private void saveAndClose() {
        super.onClose();
        saveCallback.accept(visibilityData, visibilityMask);
    }

    private static Component formatZoomLabel(int zoom) {
        if (!CollectionVisibilityData.isZoomEnabled(zoom)) {
            return NOT_VISIBLE_LABEL;
        }

        return Component.literal(Integer.toString(zoom));
    }

    private record BooleanMaskBinding(Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    @FunctionalInterface
    public interface SaveCallback {
        void accept(CollectionVisibilityData visibilityData, @Nullable CollectionVisibilityMask visibilityMask);
    }
}
