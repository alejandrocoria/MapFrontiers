package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class NewFrontierDefaultsDialog extends PanelDialog {
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component DEFAULT_NAME_LABEL = Component.translatable("mapfrontiers.default_name");
    private static final Component DEFAULT_VISIBILITY_LABEL = Component.translatable("mapfrontiers.default_visibility");
    private static final Component DEFAULT_PATH_STYLE_LABEL = Component.translatable("mapfrontiers.default_path_style");
    private static final Component USE_RANDOM_COLOR_LABEL = Component.translatable("mapfrontiers.use_random_color");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final Component R_LABEL = Component.literal("R");
    private static final Component G_LABEL = Component.literal("G");
    private static final Component B_LABEL = Component.literal("B");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;
    private static final int RGB_LABEL_HEIGHT = 8;
    private static final int RGB_TEXTBOX_WIDTH = 33;
    private static final int RGB_ROW_SPACER_WIDTH = 4;
    private static final int RGB_INLINE_SPACING = 3;

    private TextBox textName1;
    private TextBox textName2;
    private SimpleButton buttonVisibility;
    private SimpleButton buttonPathStyle;
    private OptionButton buttonRandomColor;
    private TextBoxInt textRed;
    private TextBoxInt textGreen;
    private TextBoxInt textBlue;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private FrontierVisibilityData visibilityData;
    private FrontierData.PathStyle pathStyle;
    private int fixedColor;
    private boolean syncingWidgets = false;

    @Override
    protected void initScreen() {
        visibilityData = ClientConfig.getDefaultFrontierVisibility();
        pathStyle = ClientConfig.getDefaultFrontierPathStyle();
        fixedColor = ClientConfig.FRONTIER_DEFAULT_COLOR.get() | 0xFF000000;

        LinearLayout layout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(layout);

        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.addChild(mainLayout);

        buildOverviewSection(mainLayout);
        buildColorSection(mainLayout);

        addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();

        refreshManualColorWidgets();
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        LinearLayout overviewColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 0, 1, 2);

        overviewColumn.addChild(new StringWidget(DEFAULT_NAME_LABEL, font).setColor(ColorConstants.TEXT));

        textName1 = createNameTextBox(ClientConfig.FRONTIER_DEFAULT_NAME_1.get());
        overviewColumn.addChild(textName1);

        textName2 = createNameTextBox(ClientConfig.FRONTIER_DEFAULT_NAME_2.get());
        overviewColumn.addChild(textName2);

        LinearLayout visibilityRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, DEFAULT_VISIBILITY_LABEL, b -> onVisibilityPressed());
        visibilityRow.addChild(buttonVisibility);
        visibilityRow.addChild(SpacerElement.width(SECTION_WIDTH));

        LinearLayout pathStyleRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        pathStyleRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(pathStyleRow);

        buttonPathStyle = new SimpleButton(font, SECTION_WIDTH, DEFAULT_PATH_STYLE_LABEL, b -> onPathStylePressed());
        buttonPathStyle.active = areJourneyMapPreviewActionsAvailable();
        pathStyleRow.addChild(buttonPathStyle);
        pathStyleRow.addChild(SpacerElement.width(SECTION_WIDTH));
    }

    private void buildColorSection(GridLayout mainLayout) {
        colorPicker = new ColorPicker(fixedColor, this::onColorPicked);
        mainLayout.addChild(colorPicker, 1, 0, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 1, 1, LayoutSettings.defaults().alignVerticallyBottom());

        LinearLayout rgbRow = LinearLayout.horizontal().spacing(RGB_INLINE_SPACING);
        rgbRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(rgbRow);

        rgbRow.addChild(new StringWidget(R_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_R));
        textRed = createRgbTextBox(value -> (fixedColor & 0xFF00FFFF) | (value << 16));
        rgbRow.addChild(textRed);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(G_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_G));
        textGreen = createRgbTextBox(value -> (fixedColor & 0xFFFF00FF) | (value << 8));
        rgbRow.addChild(textGreen);
        rgbRow.addChild(SpacerElement.width(RGB_ROW_SPACER_WIDTH));

        rgbRow.addChild(new StringWidget(B_LABEL, font, RGB_LABEL_HEIGHT).setColor(ColorConstants.LABEL_B));
        textBlue = createRgbTextBox(value -> (fixedColor & 0xFFFFFF00) | value);
        rgbRow.addChild(textBlue);

        LinearLayout randomColorRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        randomColorRow.defaultCellSetting().alignVerticallyMiddle();
        colorColumn.addChild(randomColorRow);

        randomColorRow.addChild(new StringWidget(USE_RANDOM_COLOR_LABEL, font).setColor(ColorConstants.TEXT));
        buttonRandomColor = createOnOffOptionButton(ClientConfig.FRONTIER_DEFAULT_RANDOM_COLOR.get(), this::onRandomColorChanged);
        randomColorRow.addChild(buttonRandomColor);

        colorPalette = new ColorPaletteWidget(fixedColor, color -> {
            colorPicker.setColor(color);
            applyColorChange(color);
        });
        colorColumn.addChild(colorPalette);

        syncColorWidgets(fixedColor);
    }

    private TextBox createNameTextBox(String value) {
        TextBox textBox = new TextBox(font, NAME_SECTION_WIDTH);
        textBox.setMaxLength(FrontierData.MAX_NAME_CHARACTERS);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValue(value);
        return textBox;
    }

    private TextBoxInt createRgbTextBox(java.util.function.IntUnaryOperator colorComposer) {
        TextBoxInt textBox = new TextBoxInt(0, 0, 255, font, RGB_TEXTBOX_WIDTH);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValueChangedCallback(value -> applyColorChange(colorComposer.applyAsInt(value)));
        return textBox;
    }

    private OptionButton createOnOffOptionButton(boolean value, java.util.function.Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, LayoutConstants.SETTING_CONTROL_WIDTH, b -> consumer.accept(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(value ? 0 : 1);
        return button;
    }

    private void onColorPicked(int color, boolean dragging) {
        if (dragging) {
            fixedColor = color | 0xFF000000;
            syncColorWidgets(fixedColor);
            return;
        }

        applyColorChange(color);
    }

    private void applyColorChange(int color) {
        if (syncingWidgets) {
            return;
        }

        fixedColor = color | 0xFF000000;
        colorPicker.setColor(fixedColor);
        syncColorWidgets(fixedColor);
    }

    private void onRandomColorChanged(boolean enabled) {
        refreshManualColorWidgets();
    }

    private void refreshManualColorWidgets() {
        boolean manualEnabled = buttonRandomColor.getSelected() != 0;
        textRed.setEditable(manualEnabled);
        textGreen.setEditable(manualEnabled);
        textBlue.setEditable(manualEnabled);
        colorPicker.active = manualEnabled;
        colorPalette.active = manualEnabled;
    }

    private void syncColorWidgets(int color) {
        syncingWidgets = true;
        try {
            textRed.setValue((color & 0xFF0000) >> 16);
            textGreen.setValue((color & 0x00FF00) >> 8);
            textBlue.setValue(color & 0x0000FF);
            colorPalette.setColor(color);
        } finally {
            syncingWidgets = false;
        }
    }

    private void clearTextBoxFocus() {
        textName1.setFocused(false);
        textName2.setFocused(false);
        textRed.setFocused(false);
        textGreen.setFocused(false);
        textBlue.setFocused(false);
    }

    private void onVisibilityPressed() {
        new FrontierVisibilityDialog(visibilityData, (newVisibilityData, newVisibilityMask) -> {
            visibilityData = new FrontierVisibilityData(newVisibilityData);
            ClientConfig.setDefaultFrontierVisibility(visibilityData);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }).display();
    }

    private void onPathStylePressed() {
        new PathStyleDialog(pathStyle, newPathStyle -> {
            pathStyle = new FrontierData.PathStyle(newPathStyle);
            ClientConfig.setDefaultFrontierPathStyle(pathStyle);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }).display();
    }

    private void saveAndClose() {
        clearTextBoxFocus();

        fixedColor = composeOpaqueColor();
        ClientConfig.FRONTIER_DEFAULT_NAME_1.set(limitName(textName1.getValue(), FrontierData.MAX_NAME_CHARACTERS));
        ClientConfig.FRONTIER_DEFAULT_NAME_2.set(limitName(textName2.getValue(), FrontierData.MAX_NAME_CHARACTERS));
        ClientConfig.FRONTIER_DEFAULT_RANDOM_COLOR.set(buttonRandomColor.getSelected() == 0);
        ClientConfig.FRONTIER_DEFAULT_COLOR.set(fixedColor);

        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private int composeOpaqueColor() {
        return 0xFF000000 | (textRed.clamped() << 16) | (textGreen.clamped() << 8) | textBlue.clamped();
    }

    private static String limitName(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private boolean areJourneyMapPreviewActionsAvailable() {
        return minecraft.player != null && MapFrontiersClient.isJourneyMapPluginAvailable();
    }
}
