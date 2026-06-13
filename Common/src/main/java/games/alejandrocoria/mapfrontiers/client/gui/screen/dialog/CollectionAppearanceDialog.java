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
import games.alejandrocoria.mapfrontiers.client.gui.util.DefaultValueBinding;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
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
    private static final Component BANNER_SIZE_LABEL = ClientConfig.COLLECTION_BANNER_SIZE.translatedName();
    private static final Tooltip BANNER_SIZE_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_BANNER_SIZE);
    private static final Component BANNER_OPACITY_LABEL = ClientConfig.COLLECTION_BANNER_OPACITY.translatedName();
    private static final Tooltip BANNER_OPACITY_TOOLTIP = ScreenHelper.tooltip(ClientConfig.COLLECTION_BANNER_OPACITY);
    private static final Component SAVE_LABEL = Component.translatable("mapfrontiers.save");
    private static final Component COLLECTION_COLOR_LABEL = Component.translatable("mapfrontiers.collection");
    private static final Component RESTORE_DEFAULT_VALUE_LABEL = Component.translatable("mapfrontiers.restore_default_value");

    private final AppearanceSnapshot initialSnapshot;
    private DefaultValueBinding<Double> fillOpacityBinding;
    private DefaultValueBinding<Integer> borderWidthBinding;
    private DefaultValueBinding<Double> borderOpacityBinding;
    private DefaultValueBinding<Integer> textSizeBinding;
    private DefaultValueBinding<Double> textOpacityBinding;
    private DefaultValueBinding<TextColor> textColorBinding;
    private DefaultValueBinding<Integer> bannerSizeBinding;
    private DefaultValueBinding<Double> bannerOpacityBinding;
    private OptionButton buttonTextColor;
    private PreviewCollectionWidget previewWidget;
    private boolean saved = false;
    private boolean syncingWidgets;

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
            if (!syncingWidgets) {
                setFillOpacity(value);
            }
        });
        fillOpacityBinding = createConfigBinding(ClientConfig.COLLECTION_FILL_OPACITY,
                () -> syncTextBoxValue(textFillOpacity, ClientConfig.COLLECTION_FILL_OPACITY.get()));
        settingsLayout.addChild(fillOpacityBinding.button(), row - 1, 2);

        StringWidget labelBorderWidth = settingsLayout.addChild(new StringWidget(BORDER_WIDTH_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(BORDER_WIDTH_TOOLTIP);
        TextBoxInt textBorderWidth = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.COLLECTION_BORDER_WIDTH), row++, 1);
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBorderWidth(value);
            }
        });
        borderWidthBinding = createConfigBinding(ClientConfig.COLLECTION_BORDER_WIDTH,
                () -> syncTextBoxValue(textBorderWidth, ClientConfig.COLLECTION_BORDER_WIDTH.get()));
        settingsLayout.addChild(borderWidthBinding.button(), row - 1, 2);

        StringWidget labelBorderOpacity = settingsLayout.addChild(new StringWidget(BORDER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(BORDER_OPACITY_TOOLTIP);
        TextBoxDouble textBorderOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_BORDER_OPACITY), row++, 1);
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBorderOpacity(value);
            }
        });
        borderOpacityBinding = createConfigBinding(ClientConfig.COLLECTION_BORDER_OPACITY,
                () -> syncTextBoxValue(textBorderOpacity, ClientConfig.COLLECTION_BORDER_OPACITY.get()));
        settingsLayout.addChild(borderOpacityBinding.button(), row - 1, 2);

        addSectionSpacing(settingsLayout, row++);

        StringWidget labelTextSize = settingsLayout.addChild(new StringWidget(TEXT_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(TEXT_SIZE_TOOLTIP);
        TextBoxInt textTextSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.COLLECTION_TEXT_SIZE), row++, 1);
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setTextSize(value);
            }
        });
        textSizeBinding = createConfigBinding(ClientConfig.COLLECTION_TEXT_SIZE,
                () -> syncTextBoxValue(textTextSize, ClientConfig.COLLECTION_TEXT_SIZE.get()));
        settingsLayout.addChild(textSizeBinding.button(), row - 1, 2);

        StringWidget labelTextOpacity = settingsLayout.addChild(new StringWidget(TEXT_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(TEXT_OPACITY_TOOLTIP);
        TextBoxDouble textTextOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_TEXT_OPACITY), row++, 1);
        textTextOpacity.setMaxLength(6);
        textTextOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setTextOpacity(value);
            }
        });
        textOpacityBinding = createConfigBinding(ClientConfig.COLLECTION_TEXT_OPACITY,
                () -> syncTextBoxValue(textTextOpacity, ClientConfig.COLLECTION_TEXT_OPACITY.get()));
        settingsLayout.addChild(textOpacityBinding.button(), row - 1, 2);

        StringWidget labelTextColor = settingsLayout.addChild(new StringWidget(TEXT_COLOR_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextColor.setTooltip(TEXT_COLOR_TOOLTIP);
        buttonTextColor = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            if (!syncingWidgets) {
                setTextColor(TextColor.values()[b.getSelected()]);
            }
        }), row++, 1);
        buttonTextColor.addOption(COLLECTION_COLOR_LABEL);
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.FrontierColorBright));
        buttonTextColor.addOption(ClientConfig.getTranslatedEnum(TextColor.White));
        buttonTextColor.setSelected(ClientConfig.COLLECTION_TEXT_COLOR.get().ordinal());
        textColorBinding = createConfigBinding(ClientConfig.COLLECTION_TEXT_COLOR, this::syncTextColorWidgets);
        settingsLayout.addChild(textColorBinding.button(), row - 1, 2);

        addSectionSpacing(settingsLayout, row++);

        StringWidget labelBannerSize = settingsLayout.addChild(new StringWidget(BANNER_SIZE_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerSize.setTooltip(BANNER_SIZE_TOOLTIP);
        TextBoxInt textBannerSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.COLLECTION_BANNER_SIZE), row++, 1);
        textBannerSize.setMaxLength(2);
        textBannerSize.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBannerSize(value);
            }
        });
        bannerSizeBinding = createConfigBinding(ClientConfig.COLLECTION_BANNER_SIZE,
                () -> syncTextBoxValue(textBannerSize, ClientConfig.COLLECTION_BANNER_SIZE.get()));
        settingsLayout.addChild(bannerSizeBinding.button(), row - 1, 2);

        StringWidget labelBannerOpacity = settingsLayout.addChild(new StringWidget(BANNER_OPACITY_LABEL, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerOpacity.setTooltip(BANNER_OPACITY_TOOLTIP);
        TextBoxDouble textBannerOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.COLLECTION_BANNER_OPACITY), row++, 1);
        textBannerOpacity.setMaxLength(6);
        textBannerOpacity.setValueChangedCallback(value -> {
            if (!syncingWidgets) {
                setBannerOpacity(value);
            }
        });
        bannerOpacityBinding = createConfigBinding(ClientConfig.COLLECTION_BANNER_OPACITY,
                () -> syncTextBoxValue(textBannerOpacity, ClientConfig.COLLECTION_BANNER_OPACITY.get()));
        settingsLayout.addChild(bannerOpacityBinding.button(), row - 1, 2);

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
        layout.addChild(SpacerElement.height(4), row, 0, 1, 3);
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

    private <T> DefaultValueBinding<T> createConfigBinding(ConfigEntry<T, ?> entry, Runnable syncWidgetsFromState) {
        return DefaultValueBinding.forConfigEntry(RESTORE_DEFAULT_VALUE_LABEL, entry, entry::get,
                value -> setConfigValue(entry, value), syncWidgetsFromState);
    }

    private void setFillOpacity(double value) {
        setConfigValue(ClientConfig.COLLECTION_FILL_OPACITY, value);
        fillOpacityBinding.refresh();
    }

    private void setBorderWidth(int value) {
        setConfigValue(ClientConfig.COLLECTION_BORDER_WIDTH, value);
        borderWidthBinding.refresh();
    }

    private void setBorderOpacity(double value) {
        setConfigValue(ClientConfig.COLLECTION_BORDER_OPACITY, value);
        borderOpacityBinding.refresh();
    }

    private void setTextSize(int value) {
        setConfigValue(ClientConfig.COLLECTION_TEXT_SIZE, value);
        textSizeBinding.refresh();
    }

    private void setTextOpacity(double value) {
        setConfigValue(ClientConfig.COLLECTION_TEXT_OPACITY, value);
        textOpacityBinding.refresh();
    }

    private void setTextColor(TextColor value) {
        setConfigValue(ClientConfig.COLLECTION_TEXT_COLOR, value);
        textColorBinding.refresh();
    }

    private void setBannerSize(int value) {
        setConfigValue(ClientConfig.COLLECTION_BANNER_SIZE, value);
        bannerSizeBinding.refresh();
    }

    private void setBannerOpacity(double value) {
        setConfigValue(ClientConfig.COLLECTION_BANNER_OPACITY, value);
        bannerOpacityBinding.refresh();
    }

    private <T> void setConfigValue(ConfigEntry<T, ?> entry, T value) {
        entry.set(value);
        previewWidget.configUpdated();
    }

    private void syncTextBoxValue(TextBoxInt textBox, int value) {
        syncingWidgets = true;
        try {
            textBox.setValue(value);
        } finally {
            syncingWidgets = false;
        }
    }

    private void syncTextBoxValue(TextBoxDouble textBox, double value) {
        syncingWidgets = true;
        try {
            textBox.setValue(value);
        } finally {
            syncingWidgets = false;
        }
    }

    private void syncTextColorWidgets() {
        syncingWidgets = true;
        try {
            buttonTextColor.setSelected(ClientConfig.COLLECTION_TEXT_COLOR.get().ordinal());
        } finally {
            syncingWidgets = false;
        }
    }

    private record AppearanceSnapshot(double fillOpacity,
                                      int borderWidth,
                                      double borderOpacity,
                                      int textSize,
                                      double textOpacity,
                                      TextColor textColor,
                                      int bannerSize,
                                      double bannerOpacity) {
        private static AppearanceSnapshot capture() {
            return new AppearanceSnapshot(
                    ClientConfig.COLLECTION_FILL_OPACITY.get(),
                    ClientConfig.COLLECTION_BORDER_WIDTH.get(),
                    ClientConfig.COLLECTION_BORDER_OPACITY.get(),
                    ClientConfig.COLLECTION_TEXT_SIZE.get(),
                    ClientConfig.COLLECTION_TEXT_OPACITY.get(),
                    ClientConfig.COLLECTION_TEXT_COLOR.get(),
                    ClientConfig.COLLECTION_BANNER_SIZE.get(),
                    ClientConfig.COLLECTION_BANNER_OPACITY.get()
            );
        }

        private void apply() {
            ClientConfig.COLLECTION_FILL_OPACITY.set(fillOpacity);
            ClientConfig.COLLECTION_BORDER_WIDTH.set(borderWidth);
            ClientConfig.COLLECTION_BORDER_OPACITY.set(borderOpacity);
            ClientConfig.COLLECTION_TEXT_SIZE.set(textSize);
            ClientConfig.COLLECTION_TEXT_OPACITY.set(textOpacity);
            ClientConfig.COLLECTION_TEXT_COLOR.set(textColor);
            ClientConfig.COLLECTION_BANNER_SIZE.set(bannerSize);
            ClientConfig.COLLECTION_BANNER_OPACITY.set(bannerOpacity);
        }
    }
}
