package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class VisibilityDialog extends AutoScaledScreen {
    private static final Component generalLabel = Component.translatable("mapfrontiers.general");
    private static final Component showFrontierLabel = Component.translatable("mapfrontiers.show_frontier");
    private static final Component announceInChatLabel = Component.translatable("mapfrontiers.announce_in_chat");
    private static final Component announceInTitleLabel = Component.translatable("mapfrontiers.announce_in_title");
    private static final Component fullscreenLabel = Component.translatable("mapfrontiers.fullscreen");
    private static final Component showNameLabel = Component.translatable("mapfrontiers.show_name");
    private static final Component showOwnerLabel = Component.translatable("mapfrontiers.show_owner");
    private static final Component showBannerLabel = Component.translatable("mapfrontiers.show_banner");
    private static final Component minimapLabel = Component.translatable("mapfrontiers.minimap");
    private static final Component webmapLabel = Component.translatable("mapfrontiers.webmap");
    private static final Component dayLabel = Component.translatable("mapfrontiers.day");
    private static final Component nightLabel = Component.translatable("mapfrontiers.night");
    private static final Component undergroundLabel = Component.translatable("mapfrontiers.underground");
    private static final Component topoLabel = Component.translatable("mapfrontiers.topo");
    private static final Component biomeLabel = Component.translatable("mapfrontiers.biome");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private final FrontierData.VisibilityData visibilityData;
    @Nullable
    private final FrontierData.VisibilityData visibilityMask;
    private final BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> afterDoneCallback;
    protected SimpleButton doneButton;

    public VisibilityDialog(FrontierData.VisibilityData visibilityData, BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> afterDoneCallback) {
        super(Component.empty(), 554, 191);
        this.visibilityData = new FrontierData.VisibilityData(visibilityData);
        this.visibilityMask = null;
        this.afterDoneCallback = afterDoneCallback;
    }

    public VisibilityDialog(FrontierData.VisibilityData visibilityData, FrontierData.VisibilityData visibilityDataMask, BiConsumer<FrontierData.VisibilityData, FrontierData.VisibilityData> afterDoneCallback) {
        super(Component.empty(), 554, 191);
        this.visibilityData = new FrontierData.VisibilityData(visibilityData);
        this.visibilityMask = new FrontierData.VisibilityData(visibilityDataMask);
        this.afterDoneCallback = afterDoneCallback;
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout mainColumns = LinearLayout.horizontal().spacing(16);
        mainLayout.addChild(mainColumns);

        LinearLayout generalColumn = LinearLayout.vertical().spacing(6);
        generalColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(generalColumn);
        generalColumn.addChild(new StringWidget(generalLabel.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout generalGrid = new GridLayout().spacing(4);
        generalColumn.addChild(generalGrid);
        int row = 1;
        createWidgets(generalGrid, row++, showFrontierLabel, FrontierData.VisibilityData.Visibility.Frontier);
        generalGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(generalGrid, row++, announceInChatLabel, FrontierData.VisibilityData.Visibility.AnnounceInChat);
        createWidgets(generalGrid, row++, announceInTitleLabel, FrontierData.VisibilityData.Visibility.AnnounceInTitle);

        LinearLayout fullscreenColumn = LinearLayout.vertical().spacing(6);
        fullscreenColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(fullscreenColumn);
        fullscreenColumn.addChild(new StringWidget(fullscreenLabel.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout fullscreenGrid = new GridLayout().spacing(4);
        fullscreenGrid.defaultCellSetting().alignVerticallyMiddle();
        fullscreenColumn.addChild(fullscreenGrid);
        row = 1;
        createWidgets(fullscreenGrid, row++, showFrontierLabel, FrontierData.VisibilityData.Visibility.Fullscreen);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, showNameLabel, FrontierData.VisibilityData.Visibility.FullscreenName);
        createWidgets(fullscreenGrid, row++, showOwnerLabel, FrontierData.VisibilityData.Visibility.FullscreenOwner);
        createWidgets(fullscreenGrid, row++, showBannerLabel, FrontierData.VisibilityData.Visibility.FullscreenBanner);
        fullscreenGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(fullscreenGrid, row++, dayLabel, FrontierData.VisibilityData.Visibility.FullscreenDay);
        createWidgets(fullscreenGrid, row++, nightLabel, FrontierData.VisibilityData.Visibility.FullscreenNight);
        createWidgets(fullscreenGrid, row++, undergroundLabel, FrontierData.VisibilityData.Visibility.FullscreenUnderground);
        createWidgets(fullscreenGrid, row++, topoLabel, FrontierData.VisibilityData.Visibility.FullscreenTopo);
        createWidgets(fullscreenGrid, row++, biomeLabel, FrontierData.VisibilityData.Visibility.FullscreenBiome);

        LinearLayout minimapColumn = LinearLayout.vertical().spacing(6);
        minimapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(minimapColumn);
        minimapColumn.addChild(new StringWidget(minimapLabel.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout minimapGrid = new GridLayout().spacing(4);
        minimapGrid.defaultCellSetting().alignVerticallyMiddle();
        minimapColumn.addChild(minimapGrid);
        row = 1;
        createWidgets(minimapGrid, row++, showFrontierLabel, FrontierData.VisibilityData.Visibility.Minimap);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, showNameLabel, FrontierData.VisibilityData.Visibility.MinimapName);
        createWidgets(minimapGrid, row++, showOwnerLabel, FrontierData.VisibilityData.Visibility.MinimapOwner);
        createWidgets(minimapGrid, row++, showBannerLabel, FrontierData.VisibilityData.Visibility.MinimapBanner);
        minimapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(minimapGrid, row++, dayLabel, FrontierData.VisibilityData.Visibility.MinimapDay);
        createWidgets(minimapGrid, row++, nightLabel, FrontierData.VisibilityData.Visibility.MinimapNight);
        createWidgets(minimapGrid, row++, undergroundLabel, FrontierData.VisibilityData.Visibility.MinimapUnderground);
        createWidgets(minimapGrid, row++, topoLabel, FrontierData.VisibilityData.Visibility.MinimapTopo);
        createWidgets(minimapGrid, row++, biomeLabel, FrontierData.VisibilityData.Visibility.MinimapBiome);

        LinearLayout webmapColumn = LinearLayout.vertical().spacing(6);
        webmapColumn.defaultCellSetting().alignHorizontallyCenter();
        mainColumns.addChild(webmapColumn);
        webmapColumn.addChild(new StringWidget(webmapLabel.copy().withStyle(Style.EMPTY.withBold(true)), font).setColor(ColorConstants.TEXT));

        GridLayout webmapGrid = new GridLayout().spacing(4);
        webmapGrid.defaultCellSetting().alignVerticallyMiddle();
        webmapColumn.addChild(webmapGrid);
        row = 1;
        createWidgets(webmapGrid, row++, showFrontierLabel, FrontierData.VisibilityData.Visibility.Webmap);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, showNameLabel, FrontierData.VisibilityData.Visibility.WebmapName);
        createWidgets(webmapGrid, row++, showOwnerLabel, FrontierData.VisibilityData.Visibility.WebmapOwner);
        createWidgets(webmapGrid, row++, showBannerLabel, FrontierData.VisibilityData.Visibility.WebmapBanner);
        webmapGrid.addChild(SpacerElement.height(2), row++, 0);
        createWidgets(webmapGrid, row++, dayLabel, FrontierData.VisibilityData.Visibility.WebmapDay);
        createWidgets(webmapGrid, row++, nightLabel, FrontierData.VisibilityData.Visibility.WebmapNight);
        createWidgets(webmapGrid, row++, undergroundLabel, FrontierData.VisibilityData.Visibility.WebmapUnderground);
        createWidgets(webmapGrid, row++, topoLabel, FrontierData.VisibilityData.Visibility.WebmapTopo);
        createWidgets(webmapGrid, row++, biomeLabel, FrontierData.VisibilityData.Visibility.WebmapBiome);

        doneButton = mainLayout.addChild(new SimpleButton(font, 100, doneLabel, (b) -> onClose()));
    }

    private void createWidgets(GridLayout layout, int row, Component label, FrontierData.VisibilityData.Visibility visibility) {
        layout.addChild(new StringWidget(label, font).setColor(ColorConstants.TEXT), row, 0);

        OptionButton button = new OptionButton(font, 28, (b) -> {
            visibilityData.setValue(visibility, b.getSelected() == 0);
        });
        button.addOption(onLabel);
        button.addOption(offLabel);
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

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    public void onClose() {
        super.onClose();
        afterDoneCallback.accept(visibilityData, visibilityMask);
    }
}
