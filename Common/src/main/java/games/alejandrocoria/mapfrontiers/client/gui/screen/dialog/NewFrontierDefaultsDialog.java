package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorInputTabsWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPaletteWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.ColorPicker;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBox;
import games.alejandrocoria.mapfrontiers.client.gui.layout.MFLinearLayout;
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
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
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;

    private TextBox textName1;
    private TextBox textName2;
    private SimpleButton buttonVisibility;
    private SimpleButton buttonPathStyle;
    private OptionButton buttonRandomColor;
    private ColorInputTabsWidget colorInputs;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private FrontierVisibilityData visibilityData;
    private FrontierData.PathStyle pathStyle;
    private int fixedColor;
    private boolean randomColorEnabled;
    private DefaultValueBinding<Boolean> randomColorBinding;
    private String initialName1;
    private String initialName2;
    private int initialFixedColor;
    private boolean initialRandomColorEnabled;
    private SimpleButton saveButton;
    private boolean syncingWidgets = false;

    @Override
    protected void initScreen() {
        visibilityData = ClientConfig.getDefaultFrontierVisibility();
        pathStyle = ClientConfig.getDefaultFrontierPathStyle();
        fixedColor = ClientConfig.FRONTIER_DEFAULT_COLOR.get() | 0xFF000000;
        randomColorEnabled = ClientConfig.FRONTIER_DEFAULT_RANDOM_COLOR.get();
        initialName1 = ClientConfig.FRONTIER_DEFAULT_NAME_1.get();
        initialName2 = ClientConfig.FRONTIER_DEFAULT_NAME_2.get();
        initialFixedColor = fixedColor;
        initialRandomColorEnabled = randomColorEnabled;

        MFLinearLayout layout = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(layout);

        GridLayout mainLayout = new GridLayout().spacing(LayoutConstants.SPACING_MEDIUM);
        layout.addChild(mainLayout);

        buildOverviewSection(mainLayout);
        buildColorSection(mainLayout);

        saveButton = addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
        refreshSaveButton();
    }

    private void buildOverviewSection(GridLayout mainLayout) {
        MFLinearLayout overviewColumn = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 0, 1, 2);

        overviewColumn.addChild(new StringWidget(DEFAULT_NAME_LABEL, font).setColor(ColorConstants.FRONTIER_INFO_TEXT));

        textName1 = createNameTextBox(ClientConfig.FRONTIER_DEFAULT_NAME_1.get());
        textName1.setValueChangedCallback(value -> refreshSaveButton());
        overviewColumn.addChild(textName1);

        textName2 = createNameTextBox(ClientConfig.FRONTIER_DEFAULT_NAME_2.get());
        textName2.setValueChangedCallback(value -> refreshSaveButton());
        overviewColumn.addChild(textName2);

        MFLinearLayout visibilityRow = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, DEFAULT_VISIBILITY_LABEL, b -> onVisibilityPressed());
        visibilityRow.addChild(buttonVisibility);
        visibilityRow.addChild(SpacerElement.width(SECTION_WIDTH));

        MFLinearLayout pathStyleRow = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        pathStyleRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(pathStyleRow);

        buttonPathStyle = new SimpleButton(font, SECTION_WIDTH, DEFAULT_PATH_STYLE_LABEL, b -> onPathStylePressed());
        buttonPathStyle.active = areJourneyMapPreviewActionsAvailable();
        pathStyleRow.addChild(buttonPathStyle);
    }

    private void buildColorSection(GridLayout mainLayout) {
        MFLinearLayout randomColorRow = MFLinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        randomColorRow.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(randomColorRow, 1, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyLeft());

        randomColorRow.addChild(new StringWidget(USE_RANDOM_COLOR_LABEL, font).setColor(ColorConstants.TEXT));
        buttonRandomColor = createOnOffOptionButton(randomColorEnabled, this::setRandomColorEnabled);
        randomColorRow.addChild(buttonRandomColor);
        randomColorBinding = DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, ClientConfig.FRONTIER_DEFAULT_RANDOM_COLOR,
                () -> randomColorEnabled, this::setRandomColorEnabled, this::syncRandomColorWidgets);
        randomColorRow.addChild(randomColorBinding.button());

        colorPicker = new ColorPicker(fixedColor, this::onColorPicked);
        mainLayout.addChild(colorPicker, 2, 0, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        MFLinearLayout colorColumn = MFLinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        colorColumn.defaultCellSetting().alignHorizontallyCenter();
        mainLayout.addChild(colorColumn, 2, 1, LayoutSettings.defaults().alignVerticallyBottom());

        colorInputs = new ColorInputTabsWidget(font, fixedColor, this::applyColorChange);
        colorColumn.addChild(colorInputs);

        colorPalette = new ColorPaletteWidget(fixedColor, this::applyColorChange);
        colorColumn.addChild(colorPalette);

        syncColorWidgets(fixedColor);
        refreshManualColorWidgets();
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderScaledBackgroundScreen(graphics, mouseX, mouseY, partialTicks);
        if (colorInputs != null) {
            colorInputs.renderTabbedBoxBackground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private TextBox createNameTextBox(String value) {
        TextBox textBox = new TextBox(font, NAME_SECTION_WIDTH);
        textBox.setMaxLength(FrontierData.MAX_NAME_CHARACTERS);
        textBox.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textBox.setValue(value);
        return textBox;
    }

    private OptionButton createOnOffOptionButton(boolean value, java.util.function.Consumer<Boolean> consumer) {
        OptionButton button = new OptionButton(font, LayoutConstants.COMPACT_ON_OFF_BUTTON_WIDTH, b -> consumer.accept(b.getSelected() == 0));
        button.addOption(ON_LABEL);
        button.addOption(OFF_LABEL);
        button.setSelected(value ? 0 : 1);
        return button;
    }

    private void onColorPicked(int color, boolean dragging) {
        if (dragging) {
            fixedColor = color | 0xFF000000;
            syncColorWidgets(fixedColor);
            refreshSaveButton();
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
        refreshSaveButton();
    }

    private void setRandomColorEnabled(boolean enabled) {
        randomColorEnabled = enabled;
        refreshManualColorWidgets();
        if (randomColorBinding != null) {
            randomColorBinding.refresh();
        }
        refreshSaveButton();
    }

    private void refreshManualColorWidgets() {
        boolean manualEnabled = !randomColorEnabled;
        colorInputs.setEditable(manualEnabled);
        colorPicker.active = manualEnabled;
        colorPalette.active = manualEnabled;
    }

    private void syncRandomColorWidgets() {
        buttonRandomColor.setSelected(randomColorEnabled ? 0 : 1);
        refreshManualColorWidgets();
        if (randomColorBinding != null) {
            randomColorBinding.refresh();
        }
    }

    private void syncColorWidgets(int color) {
        syncingWidgets = true;
        try {
            colorInputs.setColor(color);
            colorPalette.setColor(color);
        } finally {
            syncingWidgets = false;
        }
    }

    private void clearTextBoxFocus() {
        textName1.setFocused(false);
        textName2.setFocused(false);
        colorInputs.clearFocus();
    }

    private void onVisibilityPressed() {
        FrontierVisibilityDialog.forClientDefaults(visibilityData, (newVisibilityData, newVisibilityMask) -> {
            visibilityData = new FrontierVisibilityData(newVisibilityData);
            ClientConfig.setDefaultFrontierVisibility(visibilityData);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }).display();
    }

    private void onPathStylePressed() {
        PathStyleDialog.forClientDefaults(pathStyle, newPathStyle -> {
            pathStyle = new FrontierData.PathStyle(newPathStyle);
            ClientConfig.setDefaultFrontierPathStyle(pathStyle);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }).display();
    }

    private void saveAndClose() {
        clearTextBoxFocus();

        ClientConfig.FRONTIER_DEFAULT_NAME_1.set(limitName(textName1.getValue(), FrontierData.MAX_NAME_CHARACTERS));
        ClientConfig.FRONTIER_DEFAULT_NAME_2.set(limitName(textName2.getValue(), FrontierData.MAX_NAME_CHARACTERS));
        ClientConfig.FRONTIER_DEFAULT_RANDOM_COLOR.set(randomColorEnabled);
        ClientConfig.FRONTIER_DEFAULT_COLOR.set(fixedColor);

        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
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

    private boolean hasChanges() {
        return !initialName1.equals(textName1.getValue())
                || !initialName2.equals(textName2.getValue())
                || initialFixedColor != fixedColor
                || initialRandomColorEnabled != randomColorEnabled;
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }
}
