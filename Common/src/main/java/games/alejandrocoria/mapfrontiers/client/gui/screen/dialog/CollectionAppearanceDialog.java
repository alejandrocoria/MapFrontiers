package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewCollectionWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.DoubleConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CollectionAppearanceDialog extends PanelDialog {
    private static final Component FILL_OPACITY_LABEL = ClientConfig.COLLECTION_FILL_OPACITY.translatedName();
    private static final Tooltip FILL_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_FILL_OPACITY);
    private static final Component BORDER_WIDTH_LABEL = ClientConfig.COLLECTION_BORDER_WIDTH.translatedName();
    private static final Tooltip BORDER_WIDTH_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_BORDER_WIDTH);
    private static final Component BORDER_OPACITY_LABEL = ClientConfig.COLLECTION_BORDER_OPACITY.translatedName();
    private static final Tooltip BORDER_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_BORDER_OPACITY);
    private static final Component TEXT_SIZE_LABEL = ClientConfig.COLLECTION_TEXT_SIZE.translatedName();
    private static final Tooltip TEXT_SIZE_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_TEXT_SIZE);
    private static final Component TEXT_OPACITY_LABEL = ClientConfig.COLLECTION_TEXT_OPACITY.translatedName();
    private static final Tooltip TEXT_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_TEXT_OPACITY);
    private static final Component TEXT_COLOR_LABEL = ClientConfig.COLLECTION_TEXT_COLOR.translatedName();
    private static final Tooltip TEXT_COLOR_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_TEXT_COLOR);
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component COLLECTION_COLOR_LABEL = Component.translatable("mapfrontiers.collection");

    private final AppearanceSnapshot initialSnapshot;
    private PreviewCollectionWidget previewWidget;
    private boolean saved = false;

    public CollectionAppearanceDialog() {
        super();
        initialSnapshot = AppearanceSnapshot.capture();
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout columnsLayout = LinearLayout.horizontal().spacing(LayoutConstants.SPACING_MEDIUM);
        mainLayout.addChild(columnsLayout);

        GridLayout settingsLayout = new GridLayout().spacing(LayoutConstants.SPACING_SMALL);
        settingsLayout.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        columnsLayout.addChild(settingsLayout);
        int row = 0;

        StringWidget labelFillOpacity = settingsLayout.addChild(new StringWidget(FILL_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelFillOpacity.setTooltip(FILL_OPACITY_TOOLTIP);
        TextBoxDouble textFillOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_FILL_OPACITY), row++, 1);
        textFillOpacity.setMaxLength(6);
        textFillOpacity.setValueChangedCallback(value -> {
            ClientConfig.COLLECTION_FILL_OPACITY.set(value);
            previewWidget.configUpdated();
        });

        addSectionSpacing(settingsLayout, row++);

        StringWidget labelBorderWidth = settingsLayout.addChild(new StringWidget(BORDER_WIDTH_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(BORDER_WIDTH_TOOLTIP);
        TextBoxInt textBorderWidth = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.COLLECTION_BORDER_WIDTH), row++, 1);
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            ClientConfig.COLLECTION_BORDER_WIDTH.set(value);
            previewWidget.configUpdated();
        });

        StringWidget labelBorderOpacity = settingsLayout.addChild(new StringWidget(BORDER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(BORDER_OPACITY_TOOLTIP);
        TextBoxDouble textBorderOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_BORDER_OPACITY), row++, 1);
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            ClientConfig.COLLECTION_BORDER_OPACITY.set(value);
            previewWidget.configUpdated();
        });

        addSectionSpacing(settingsLayout, row++);

        StringWidget labelTextSize = settingsLayout.addChild(new StringWidget(TEXT_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(TEXT_SIZE_TOOLTIP);
        TextBoxInt textTextSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.COLLECTION_TEXT_SIZE), row++, 1);
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            ClientConfig.COLLECTION_TEXT_SIZE.set(value);
            previewWidget.configUpdated();
        });

        StringWidget labelTextOpacity = settingsLayout.addChild(new StringWidget(TEXT_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(TEXT_OPACITY_TOOLTIP);
        TextBoxDouble textTextOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_TEXT_OPACITY), row++, 1);
        textTextOpacity.setMaxLength(6);
        textTextOpacity.setValueChangedCallback(value -> {
            ClientConfig.COLLECTION_TEXT_OPACITY.set(value);
            previewWidget.configUpdated();
        });

        StringWidget labelTextColor = settingsLayout.addChild(new StringWidget(TEXT_COLOR_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextColor.setTooltip(TEXT_COLOR_TOOLTIP);
        OptionButton buttonTextColor = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            ClientConfig.COLLECTION_TEXT_COLOR.set(TextColor.values()[b.getSelected()]);
            previewWidget.configUpdated();
        }), row++, 1);
        buttonTextColor.addOption(COLLECTION_COLOR_LABEL);
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.FrontierColorBright));
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.White));
        buttonTextColor.setSelected(ClientConfig.COLLECTION_TEXT_COLOR.get().ordinal());

        addSectionSpacing(settingsLayout, row);

        previewWidget = columnsLayout.addChild(new PreviewCollectionWidget());
        previewWidget.configUpdated();

        addConfirmButton(SAVE_LABEL, b -> saveAndClose());
        addCancelButton();
    }

    @Override
    protected void resetContentToMinimumSize() {
        previewWidget.setScaleFactor(1.f);
    }

    @Override
    protected void resizeContentToAvailableSpace() {
        previewWidget.setScaleFactor(scaleFactor);
    }

    @Override
    public void onClose() {
        if (!saved) {
            initialSnapshot.apply();
        }
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private void saveAndClose() {
        saved = true;
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private static void addSectionSpacing(GridLayout layout, int row) {
        layout.addChild(SpacerElement.height(4), row, 0, 1, 2);
    }

    private TextBoxInt createIntConfigTextBox(IntConfigEntry entry) {
        TextBoxInt textBox = new TextBoxInt(entry, font, 60);
        textBox.setValue(String.valueOf(entry.get()));
        return textBox;
    }

    private TextBoxDouble createDoubleConfigTextBox(DoubleConfigEntry entry) {
        TextBoxDouble textBox = new TextBoxDouble(entry, font, 60);
        textBox.setValue(String.valueOf(entry.get()));
        return textBox;
    }

    private record AppearanceSnapshot(double fillOpacity,
                                      int borderWidth,
                                      double borderOpacity,
                                      int textSize,
                                      double textOpacity,
                                      TextColor textColor) {
        private static AppearanceSnapshot capture() {
            return new AppearanceSnapshot(
                    ClientConfig.COLLECTION_FILL_OPACITY.get(),
                    ClientConfig.COLLECTION_BORDER_WIDTH.get(),
                    ClientConfig.COLLECTION_BORDER_OPACITY.get(),
                    ClientConfig.COLLECTION_TEXT_SIZE.get(),
                    ClientConfig.COLLECTION_TEXT_OPACITY.get(),
                    ClientConfig.COLLECTION_TEXT_COLOR.get()
            );
        }

        private void apply() {
            ClientConfig.COLLECTION_FILL_OPACITY.set(fillOpacity);
            ClientConfig.COLLECTION_BORDER_WIDTH.set(borderWidth);
            ClientConfig.COLLECTION_BORDER_OPACITY.set(borderOpacity);
            ClientConfig.COLLECTION_TEXT_SIZE.set(textSize);
            ClientConfig.COLLECTION_TEXT_OPACITY.set(textOpacity);
            ClientConfig.COLLECTION_TEXT_COLOR.set(textColor);
        }
    }
}
