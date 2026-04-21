package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class VisibilityDialog extends PanelDialog {
    private static final Component GENERAL_LABEL = Component.translatable("mapfrontiers.general");
    private static final Component SHOW_FRONTIER_LABEL = Component.translatable("mapfrontiers.show_frontier");
    private static final Component ANNOUNCE_IN_CHAT_LABEL = Component.translatable("mapfrontiers.announce_in_chat");
    private static final Component ANNOUNCE_IN_TITLE_LABEL = Component.translatable("mapfrontiers.announce_in_title");
    private static final Component FULLSCREEN_LABEL = Component.translatable("mapfrontiers.fullscreen");
    private static final Component SHOW_NAME_LABEL = Component.translatable("mapfrontiers.show_name");
    private static final Component SHOW_OWNER_LABEL = Component.translatable("mapfrontiers.show_owner");
    private static final Component SHOW_BANNER_LABEL = Component.translatable("mapfrontiers.show_banner");
    private static final Component MINIMAP_LABEL = Component.translatable("mapfrontiers.minimap");
    private static final Component WEBMAP_LABEL = Component.translatable("mapfrontiers.webmap");
    private static final Component DAY_LABEL = Component.translatable("mapfrontiers.day");
    private static final Component NIGHT_LABEL = Component.translatable("mapfrontiers.night");
    private static final Component UNDERGROUND_LABEL = Component.translatable("mapfrontiers.underground");
    private static final Component TOPO_LABEL = Component.translatable("mapfrontiers.topo");
    private static final Component BIOME_LABEL = Component.translatable("mapfrontiers.biome");
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final int COLUMN_SPACING = 6;

    private final FrontierData.VisibilityData visibilityData;
    @Nullable
    private final FrontierData.VisibilityData visibilityMask;
    private final BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> saveCallback;

    public VisibilityDialog(FrontierData.VisibilityData visibilityData, BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> saveCallback) {
        super(554, 191);
        this.visibilityData = new FrontierData.VisibilityData(visibilityData);
        this.visibilityMask = null;
        this.saveCallback = saveCallback;
    }

    public VisibilityDialog(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask, BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> saveCallback) {
        super(554, 191);
        this.visibilityData = new FrontierData.VisibilityData(visibilityData);
        this.visibilityMask = new FrontierData.VisibilityData(visibilityDataMask);
        this.saveCallback = saveCallback;
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout mainColumns = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_LARGE);
        mainLayout.addChild(mainColumns);

        LinearLayout generalColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        generalColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(generalColumn);
        generalColumn.addChild(new StringWidget(GENERAL_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout generalGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        generalColumn.addChild(generalGrid);
        int row = 1;
        createWidgets(generalGrid, row++, SHOW_FRONTIER_LABEL, FrontierData.VisibilityData.Visibility.Frontier);
        generalGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(generalGrid, row++, ANNOUNCE_IN_CHAT_LABEL, FrontierData.VisibilityData.Visibility.AnnounceInChat);
        createWidgets(generalGrid, row++, ANNOUNCE_IN_TITLE_LABEL, FrontierData.VisibilityData.Visibility.AnnounceInTitle);

        LinearLayout fullscreenColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        fullscreenColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(fullscreenColumn);
        fullscreenColumn.addChild(new StringWidget(FULLSCREEN_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout fullscreenGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        fullscreenGrid.defaultCellSetting().alignVerticallyMiddle();
        fullscreenColumn.addChild(fullscreenGrid);
        row = 1;
        createWidgets(fullscreenGrid, row++, SHOW_FRONTIER_LABEL, FrontierData.VisibilityData.Visibility.Fullscreen);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, SHOW_NAME_LABEL, FrontierData.VisibilityData.Visibility.FullscreenName);
        createWidgets(fullscreenGrid, row++, SHOW_OWNER_LABEL, FrontierData.VisibilityData.Visibility.FullscreenOwner);
        createWidgets(fullscreenGrid, row++, SHOW_BANNER_LABEL, FrontierData.VisibilityData.Visibility.FullscreenBanner);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, DAY_LABEL, FrontierData.VisibilityData.Visibility.FullscreenDay);
        createWidgets(fullscreenGrid, row++, NIGHT_LABEL, FrontierData.VisibilityData.Visibility.FullscreenNight);
        createWidgets(fullscreenGrid, row++, UNDERGROUND_LABEL, FrontierData.VisibilityData.Visibility.FullscreenUnderground);
        createWidgets(fullscreenGrid, row++, TOPO_LABEL, FrontierData.VisibilityData.Visibility.FullscreenTopo);
        createWidgets(fullscreenGrid, row++, BIOME_LABEL, FrontierData.VisibilityData.Visibility.FullscreenBiome);

        LinearLayout minimapColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        minimapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(minimapColumn);
        minimapColumn.addChild(new StringWidget(MINIMAP_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout minimapGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        minimapGrid.defaultCellSetting().alignVerticallyMiddle();
        minimapColumn.addChild(minimapGrid);
        row = 1;
        createWidgets(minimapGrid, row++, SHOW_FRONTIER_LABEL, FrontierData.VisibilityData.Visibility.Minimap);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, SHOW_NAME_LABEL, FrontierData.VisibilityData.Visibility.MinimapName);
        createWidgets(minimapGrid, row++, SHOW_OWNER_LABEL, FrontierData.VisibilityData.Visibility.MinimapOwner);
        createWidgets(minimapGrid, row++, SHOW_BANNER_LABEL, FrontierData.VisibilityData.Visibility.MinimapBanner);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, DAY_LABEL, FrontierData.VisibilityData.Visibility.MinimapDay);
        createWidgets(minimapGrid, row++, NIGHT_LABEL, FrontierData.VisibilityData.Visibility.MinimapNight);
        createWidgets(minimapGrid, row++, UNDERGROUND_LABEL, FrontierData.VisibilityData.Visibility.MinimapUnderground);
        createWidgets(minimapGrid, row++, TOPO_LABEL, FrontierData.VisibilityData.Visibility.MinimapTopo);
        createWidgets(minimapGrid, row++, BIOME_LABEL, FrontierData.VisibilityData.Visibility.MinimapBiome);

        LinearLayout webmapColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        webmapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(webmapColumn);
        webmapColumn.addChild(new StringWidget(WEBMAP_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout webmapGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        webmapGrid.defaultCellSetting().alignVerticallyMiddle();
        webmapColumn.addChild(webmapGrid);
        row = 1;
        createWidgets(webmapGrid, row++, SHOW_FRONTIER_LABEL, FrontierData.VisibilityData.Visibility.Webmap);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, SHOW_NAME_LABEL, FrontierData.VisibilityData.Visibility.WebmapName);
        createWidgets(webmapGrid, row++, SHOW_OWNER_LABEL, FrontierData.VisibilityData.Visibility.WebmapOwner);
        createWidgets(webmapGrid, row++, SHOW_BANNER_LABEL, FrontierData.VisibilityData.Visibility.WebmapBanner);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, DAY_LABEL, FrontierData.VisibilityData.Visibility.WebmapDay);
        createWidgets(webmapGrid, row++, NIGHT_LABEL, FrontierData.VisibilityData.Visibility.WebmapNight);
        createWidgets(webmapGrid, row++, UNDERGROUND_LABEL, FrontierData.VisibilityData.Visibility.WebmapUnderground);
        createWidgets(webmapGrid, row++, TOPO_LABEL, FrontierData.VisibilityData.Visibility.WebmapTopo);
        createWidgets(webmapGrid, row++, BIOME_LABEL, FrontierData.VisibilityData.Visibility.WebmapBiome);

        addConfirmButton(SAVE_LABEL, (b) -> saveAndClose());
        addCancelButton();
    }

    private void createWidgets(GridLayout layout, int row, Component label, FrontierData.VisibilityData.Visibility visibility) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, (b) -> {
            visibilityData.setValue(visibility, b.getSelected() == 0);
        });
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(visibilityData.getValue(visibility) ? 0 : 1);
        layout.addChild(button, row, 2);

        if (visibilityMask != null) {
            CheckBoxButton checkBox = new CheckBoxButton(visibilityMask.getValue(visibility), (b) -> {
                visibilityMask.setValue(visibility, b.isChecked());
                button.active = b.isChecked();
            });
            layout.addChild(checkBox, row, 1);
            button.active = checkBox.isChecked();
        }
    }

    private void saveAndClose() {
        super.onClose();
        saveCallback.accept(visibilityData, visibilityMask);
    }
}
