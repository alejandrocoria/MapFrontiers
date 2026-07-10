package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

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
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class NewCollectionDefaultsDialog extends PanelDialog {
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component DEFAULT_NAME_LABEL = Component.translatable("mapfrontiers.default_name");
    private static final Component DEFAULT_VISIBILITY_LABEL = Component.translatable("mapfrontiers.default_visibility");
    private static final Component USE_RANDOM_COLOR_LABEL = Component.translatable("mapfrontiers.use_random_color");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");
    private static final Component ON_LABEL = Component.translatable("options.on");
    private static final Component OFF_LABEL = Component.translatable("options.off");
    private static final int SECTION_WIDTH = 146;
    private static final int NAME_SECTION_WIDTH = SECTION_WIDTH * 2 + LayoutConstants.SPACING_MEDIUM;
    private static final int DEFAULT_TEXTBOX_HEIGHT = 17;

    private TextBox textName;
    private SimpleButton buttonVisibility;
    private OptionButton buttonRandomColor;
    private ColorInputTabsWidget colorInputs;
    private ColorPicker colorPicker;
    private ColorPaletteWidget colorPalette;
    private CollectionVisibilityData visibilityData;
    private int fixedColor;
    private boolean randomColorEnabled;
    private DefaultValueBinding<Boolean> randomColorBinding;
    private String initialName;
    private int initialFixedColor;
    private boolean initialRandomColorEnabled;
    private SimpleButton saveButton;
    private boolean syncingWidgets = false;

    @Override
    protected void initScreen() {
        visibilityData = ClientConfig.getDefaultCollectionVisibility();
        fixedColor = ClientConfig.COLLECTION_DEFAULT_COLOR.get() | 0xFF000000;
        randomColorEnabled = ClientConfig.COLLECTION_DEFAULT_RANDOM_COLOR.get();
        initialName = ClientConfig.COLLECTION_DEFAULT_NAME.get();
        initialFixedColor = fixedColor;
        initialRandomColorEnabled = randomColorEnabled;

        LinearLayout layout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
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
        LinearLayout overviewColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
        overviewColumn.defaultCellSetting().alignHorizontallyLeft();
        mainLayout.addChild(overviewColumn, 0, 0, 1, 2);

        overviewColumn.addChild(new StringWidget(DEFAULT_NAME_LABEL, font).setColor(ColorConstants.FRONTIER_INFO_TEXT));

        textName = new TextBox(font, NAME_SECTION_WIDTH);
        textName.setMaxLength(CollectionData.MAX_NAME_CHARACTERS);
        textName.setHeight(DEFAULT_TEXTBOX_HEIGHT);
        textName.setValue(ClientConfig.COLLECTION_DEFAULT_NAME.get());
        textName.setValueChangedCallback(value -> refreshSaveButton());
        overviewColumn.addChild(textName);

        LinearLayout visibilityRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        visibilityRow.defaultCellSetting().alignVerticallyMiddle();
        overviewColumn.addChild(visibilityRow);

        buttonVisibility = new SimpleButton(font, SECTION_WIDTH, DEFAULT_VISIBILITY_LABEL, b -> onVisibilityPressed());
        visibilityRow.addChild(buttonVisibility);
    }

    private void buildColorSection(GridLayout mainLayout) {
        LinearLayout randomColorRow = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_SMALL);
        randomColorRow.defaultCellSetting().alignVerticallyMiddle();
        mainLayout.addChild(randomColorRow, 1, 0, 1, 2, LayoutSettings.defaults().alignHorizontallyLeft());

        randomColorRow.addChild(new StringWidget(USE_RANDOM_COLOR_LABEL, font).setColor(ColorConstants.TEXT));
        buttonRandomColor = createOnOffOptionButton(randomColorEnabled, this::setRandomColorEnabled);
        randomColorRow.addChild(buttonRandomColor);
        randomColorBinding = DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, ClientConfig.COLLECTION_DEFAULT_RANDOM_COLOR,
                () -> randomColorEnabled, this::setRandomColorEnabled, this::syncRandomColorWidgets);
        randomColorRow.addChild(randomColorBinding.button());

        colorPicker = new ColorPicker(fixedColor, this::onColorPicked);
        mainLayout.addChild(colorPicker, 2, 0, LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter());

        LinearLayout colorColumn = LinearLayout.vertical().spacing(LayoutConstants.SPACING_SMALL);
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
        textName.setFocused(false);
        colorInputs.clearFocus();
    }

    private void onVisibilityPressed() {
        CollectionVisibilityDialog.forClientDefaults(visibilityData, (newVisibilityData, newVisibilityMask) -> {
            visibilityData = new CollectionVisibilityData(newVisibilityData);
            ClientConfig.setDefaultCollectionVisibility(visibilityData);
            ClientGlobalEvents.postUpdatedConfigEvent();
        }).display();
    }

    private void saveAndClose() {
        clearTextBoxFocus();

        ClientConfig.COLLECTION_DEFAULT_NAME.set(limitName(textName.getValue(), CollectionData.MAX_NAME_CHARACTERS));
        ClientConfig.COLLECTION_DEFAULT_RANDOM_COLOR.set(randomColorEnabled);
        ClientConfig.COLLECTION_DEFAULT_COLOR.set(fixedColor);

        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private static String limitName(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private boolean hasChanges() {
        return !initialName.equals(textName.getValue())
                || initialFixedColor != fixedColor
                || initialRandomColorEnabled != randomColorEnabled;
    }

    private void refreshSaveButton() {
        if (saveButton != null) {
            saveButton.active = hasChanges();
        }
    }
}
