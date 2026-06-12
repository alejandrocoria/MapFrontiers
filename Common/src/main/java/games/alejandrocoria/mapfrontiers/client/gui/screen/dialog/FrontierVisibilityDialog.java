package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityMask;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;

@ParametersAreNonnullByDefault
public class FrontierVisibilityDialog extends PanelDialog {
    private static final Component GENERAL_LABEL = Component.translatable("mapfrontiers.general");
    private static final Component SHOW_FRONTIER_LABEL = Component.translatable("mapfrontiers.show_frontier");
    private static final Component ANNOUNCE_IN_CHAT_LABEL = Component.translatable("mapfrontiers.announce_in_chat");
    private static final Component ANNOUNCE_IN_TITLE_LABEL = Component.translatable("mapfrontiers.announce_in_title");
    private static final Component MENTION_COLLECTION_LABEL = Component.translatable("mapfrontiers.mention_collection");
    private static final Component FULLSCREEN_LABEL = Component.translatable("mapfrontiers.fullscreen");
    private static final Component SHOW_NAME_LABEL = Component.translatable("mapfrontiers.show_name");
    private static final Component SHOW_COLLECTION_LABEL = Component.translatable("mapfrontiers.show_collection");
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
    private static final Component DEFAULT_VISIBILITY_LABEL = Component.translatable("mapfrontiers.replace_with_default_visibility");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final int COLUMN_SPACING = 6;
    private static final int BUTTON_HORIZONTAL_PADDING = 16;

    private final FrontierVisibilityData workingVisibilityData;
    @Nullable
    private final FrontierVisibilityData defaultVisibilityData;
    @Nullable
    private final FrontierVisibilityMask visibilityMask;
    private final SaveCallback saveCallback;
    private final EnumMap<FrontierVisibility, OptionButton> visibilityButtons = new EnumMap<>(FrontierVisibility.class);

    public FrontierVisibilityDialog(FrontierVisibilityData visibilityData, SaveCallback saveCallback) {
        this(visibilityData, null, null, saveCallback);
    }

    public FrontierVisibilityDialog(FrontierVisibilityData visibilityData, FrontierVisibilityData defaultVisibilityData,
                                    SaveCallback saveCallback) {
        this(visibilityData, defaultVisibilityData, null, saveCallback);
    }

    public FrontierVisibilityDialog(FrontierVisibilityData visibilityData, FrontierVisibilityMask visibilityDataMask,
                                    SaveCallback saveCallback) {
        this(visibilityData, null, visibilityDataMask, saveCallback);
    }

    private FrontierVisibilityDialog(FrontierVisibilityData visibilityData, @Nullable FrontierVisibilityData defaultVisibilityData,
                                     @Nullable FrontierVisibilityMask visibilityDataMask, SaveCallback saveCallback) {
        super();
        this.workingVisibilityData = new FrontierVisibilityData(visibilityData);
        this.defaultVisibilityData = defaultVisibilityData == null ? null : new FrontierVisibilityData(defaultVisibilityData);
        this.visibilityMask = visibilityDataMask == null ? null : new FrontierVisibilityMask(visibilityDataMask);
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
        createWidgets(generalGrid, row++, SHOW_FRONTIER_LABEL, FrontierVisibility.Frontier);
        generalGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(generalGrid, row++, ANNOUNCE_IN_CHAT_LABEL, FrontierVisibility.AnnounceInChat);
        createWidgets(generalGrid, row++, ANNOUNCE_IN_TITLE_LABEL, FrontierVisibility.AnnounceInTitle);
        createWidgets(generalGrid, row++, MENTION_COLLECTION_LABEL, FrontierVisibility.MentionCollection);

        LinearLayout fullscreenColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        fullscreenColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(fullscreenColumn);
        fullscreenColumn.addChild(new StringWidget(FULLSCREEN_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout fullscreenGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        fullscreenGrid.defaultCellSetting().alignVerticallyMiddle();
        fullscreenColumn.addChild(fullscreenGrid);
        row = 1;
        createWidgets(fullscreenGrid, row++, SHOW_FRONTIER_LABEL, FrontierVisibility.Fullscreen);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, SHOW_NAME_LABEL, FrontierVisibility.FullscreenName);
        createWidgets(fullscreenGrid, row++, SHOW_COLLECTION_LABEL, FrontierVisibility.FullscreenCollection);
        createWidgets(fullscreenGrid, row++, SHOW_OWNER_LABEL, FrontierVisibility.FullscreenOwner);
        createWidgets(fullscreenGrid, row++, SHOW_BANNER_LABEL, FrontierVisibility.FullscreenBanner);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, DAY_LABEL, FrontierVisibility.FullscreenDay);
        createWidgets(fullscreenGrid, row++, NIGHT_LABEL, FrontierVisibility.FullscreenNight);
        createWidgets(fullscreenGrid, row++, UNDERGROUND_LABEL, FrontierVisibility.FullscreenUnderground);
        createWidgets(fullscreenGrid, row++, TOPO_LABEL, FrontierVisibility.FullscreenTopo);
        createWidgets(fullscreenGrid, row++, BIOME_LABEL, FrontierVisibility.FullscreenBiome);

        LinearLayout minimapColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        minimapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(minimapColumn);
        minimapColumn.addChild(new StringWidget(MINIMAP_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout minimapGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        minimapGrid.defaultCellSetting().alignVerticallyMiddle();
        minimapColumn.addChild(minimapGrid);
        row = 1;
        createWidgets(minimapGrid, row++, SHOW_FRONTIER_LABEL, FrontierVisibility.Minimap);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, SHOW_NAME_LABEL, FrontierVisibility.MinimapName);
        createWidgets(minimapGrid, row++, SHOW_COLLECTION_LABEL, FrontierVisibility.MinimapCollection);
        createWidgets(minimapGrid, row++, SHOW_OWNER_LABEL, FrontierVisibility.MinimapOwner);
        createWidgets(minimapGrid, row++, SHOW_BANNER_LABEL, FrontierVisibility.MinimapBanner);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, DAY_LABEL, FrontierVisibility.MinimapDay);
        createWidgets(minimapGrid, row++, NIGHT_LABEL, FrontierVisibility.MinimapNight);
        createWidgets(minimapGrid, row++, UNDERGROUND_LABEL, FrontierVisibility.MinimapUnderground);
        createWidgets(minimapGrid, row++, TOPO_LABEL, FrontierVisibility.MinimapTopo);
        createWidgets(minimapGrid, row++, BIOME_LABEL, FrontierVisibility.MinimapBiome);

        LinearLayout webmapColumn = LinearLayout.vertical().spacing(COLUMN_SPACING);
        webmapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(webmapColumn);
        webmapColumn.addChild(new StringWidget(WEBMAP_LABEL.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout webmapGrid = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        webmapGrid.defaultCellSetting().alignVerticallyMiddle();
        webmapColumn.addChild(webmapGrid);
        row = 1;
        createWidgets(webmapGrid, row++, SHOW_FRONTIER_LABEL, FrontierVisibility.Webmap);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, SHOW_NAME_LABEL, FrontierVisibility.WebmapName);
        createWidgets(webmapGrid, row++, SHOW_COLLECTION_LABEL, FrontierVisibility.WebmapCollection);
        createWidgets(webmapGrid, row++, SHOW_OWNER_LABEL, FrontierVisibility.WebmapOwner);
        createWidgets(webmapGrid, row++, SHOW_BANNER_LABEL, FrontierVisibility.WebmapBanner);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, DAY_LABEL, FrontierVisibility.WebmapDay);
        createWidgets(webmapGrid, row++, NIGHT_LABEL, FrontierVisibility.WebmapNight);
        createWidgets(webmapGrid, row++, UNDERGROUND_LABEL, FrontierVisibility.WebmapUnderground);
        createWidgets(webmapGrid, row++, TOPO_LABEL, FrontierVisibility.WebmapTopo);
        createWidgets(webmapGrid, row++, BIOME_LABEL, FrontierVisibility.WebmapBiome);

        if (defaultVisibilityData != null) {
            LinearLayout defaultActionRow = LinearLayout.horizontal();
            defaultActionRow.addChild(new SimpleButton(font, font.width(DEFAULT_VISIBILITY_LABEL) + BUTTON_HORIZONTAL_PADDING,
                    DEFAULT_VISIBILITY_LABEL, b -> replaceWithDefaultVisibility()));
            mainLayout.addChild(defaultActionRow, LayoutSettings.defaults().alignHorizontallyCenter());
        }

        addConfirmButton(SAVE_LABEL, (b) -> saveAndClose());
        addCancelButton();
    }

    private void createWidgets(GridLayout layout, int row, Component label, FrontierVisibility visibility) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, (b) -> {
            workingVisibilityData.set(visibility, b.getSelected() == 0);
        });
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(workingVisibilityData.get(visibility) ? 0 : 1);
        layout.addChild(button, row, 2);
        visibilityButtons.put(visibility, button);

        if (visibilityMask != null) {
            CheckBoxButton checkBox = new CheckBoxButton(visibilityMask.has(visibility), (b) -> {
                visibilityMask.set(visibility, b.isChecked());
                button.active = b.isChecked();
            });
            layout.addChild(checkBox, row, 1);
            button.active = checkBox.isChecked();
        }
    }

    private void replaceWithDefaultVisibility() {
        if (defaultVisibilityData == null) {
            return;
        }

        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            workingVisibilityData.set(visibility, defaultVisibilityData.get(visibility));
        }
        syncWidgetsFromVisibility();
    }

    private void syncWidgetsFromVisibility() {
        for (FrontierVisibility visibility : FrontierVisibility.VALUES) {
            OptionButton button = visibilityButtons.get(visibility);
            if (button != null) {
                button.setSelected(workingVisibilityData.get(visibility) ? 0 : 1);
            }
        }
    }

    private void saveAndClose() {
        super.onClose();
        saveCallback.accept(new FrontierVisibilityData(workingVisibilityData),
                visibilityMask == null ? null : new FrontierVisibilityMask(visibilityMask));
    }

    @FunctionalInterface
    public interface SaveCallback {
        void accept(FrontierVisibilityData visibilityData, @Nullable FrontierVisibilityMask visibilityMask);
    }
}
