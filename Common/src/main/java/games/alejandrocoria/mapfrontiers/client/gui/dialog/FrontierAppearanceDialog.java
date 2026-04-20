package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.event.ClientGlobalEvents;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewFrontiersWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.DoubleConfigEntry;
import games.alejandrocoria.mapfrontiers.common.config.IntConfigEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FrontierAppearanceDialog extends AutoScaledScreen {
    private static final Component hideNamesThatDontFitLabel = ClientConfig.HIDE_NAMES_THAT_DONT_FIT.translatedName();
    private static final Tooltip hideNamesThatDontFitTooltip = tooltip(ClientConfig.HIDE_NAMES_THAT_DONT_FIT);
    private static final Component polygonsOpacityLabel = ClientConfig.POLYGONS_OPACITY.translatedName();
    private static final Tooltip polygonsOpacityTooltip = tooltip(ClientConfig.POLYGONS_OPACITY);
    private static final Component borderWidthLabel = ClientConfig.BORDER_WIDTH.translatedName();
    private static final Tooltip borderWidthTooltip = tooltip(ClientConfig.BORDER_WIDTH);
    private static final Component borderOpacityLabel = ClientConfig.BORDER_OPACITY.translatedName();
    private static final Tooltip borderOpacityTooltip = tooltip(ClientConfig.BORDER_OPACITY);
    private static final Component pathMarkerSizeLabel = ClientConfig.PATH_MARKER_SIZE.translatedName();
    private static final Tooltip pathMarkerSizeTooltip = tooltip(ClientConfig.PATH_MARKER_SIZE);
    private static final Component pathMarkerOpacityLabel = ClientConfig.PATH_MARKER_OPACITY.translatedName();
    private static final Tooltip pathMarkerOpacityTooltip = tooltip(ClientConfig.PATH_MARKER_OPACITY);
    private static final Component textSizeLabel = ClientConfig.TEXT_SIZE.translatedName();
    private static final Tooltip textSizeTooltip = tooltip(ClientConfig.TEXT_SIZE);
    private static final Component textOpacityLabel = ClientConfig.TEXT_OPACITY.translatedName();
    private static final Tooltip textOpacityTooltip = tooltip(ClientConfig.TEXT_OPACITY);
    private static final Component textColorLabel = ClientConfig.TEXT_COLOR.translatedName();
    private static final Tooltip textColorTooltip = tooltip(ClientConfig.TEXT_COLOR);
    private static final Component bannerSizeLabel = ClientConfig.BANNER_SIZE.translatedName();
    private static final Tooltip bannerSizeTooltip = tooltip(ClientConfig.BANNER_SIZE);
    private static final Component bannerOpacityLabel = ClientConfig.BANNER_OPACITY.translatedName();
    private static final Tooltip bannerOpacityTooltip = tooltip(ClientConfig.BANNER_OPACITY);
    private static final Component saveLabel = Component.translatable("mapfrontiers.save");
    private static final Component cancelLabel = Component.translatable("gui.cancel");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private final AppearanceSnapshot initialSnapshot;
    private StringWidget labelHideNamesThatDontFit;
    private StringWidget labelPolygonsOpacity;
    private StringWidget labelBorderWidth;
    private StringWidget labelBorderOpacity;
    private StringWidget labelPathMarkerSize;
    private StringWidget labelPathMarkerOpacity;
    private StringWidget labelTextSize;
    private StringWidget labelTextOpacity;
    private StringWidget labelTextUsesCustomColor;
    private StringWidget labelBannerSize;
    private StringWidget labelBannerOpacity;
    private OptionButton buttonHideNamesThatDontFit;
    private TextBoxDouble textPolygonsOpacity;
    private TextBoxInt textBorderWidth;
    private TextBoxDouble textBorderOpacity;
    private TextBoxInt textPathMarkerSize;
    private TextBoxDouble textPathMarkerOpacity;
    private TextBoxInt textTextSize;
    private TextBoxDouble textTextOpacity;
    private OptionButton buttonTextUsesCustomColor;
    private TextBoxInt textBannerSize;
    private TextBoxDouble textBannerOpacity;
    private PreviewFrontiersWidget previewFrontiers;
    private boolean saved = false;

    private SimpleButton saveButton;
    private SimpleButton cancelButton;

    public FrontierAppearanceDialog() {
        super(Component.empty(), 455, 255);
        initialSnapshot = AppearanceSnapshot.capture();
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout columnsLayout = LinearLayout.horizontal().spacing(8);
        mainLayout.addChild(columnsLayout);

        GridLayout settingsLayout = new GridLayout().spacing(4);
        settingsLayout.defaultCellSetting().alignHorizontallyLeft().alignVerticallyMiddle();
        columnsLayout.addChild(settingsLayout);
        int row = 0;

        labelHideNamesThatDontFit = settingsLayout.addChild(new StringWidget(hideNamesThatDontFitLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelHideNamesThatDontFit.setTooltip(hideNamesThatDontFitTooltip);
        buttonHideNamesThatDontFit = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            ClientConfig.HIDE_NAMES_THAT_DONT_FIT.set(b.getSelected() == 0);
            previewFrontiers.configUpdated();
        }), row++, 1);
        buttonHideNamesThatDontFit.addOption(onLabel);
        buttonHideNamesThatDontFit.addOption(offLabel);
        buttonHideNamesThatDontFit.setSelected(ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get() ? 0 : 1);

        addSectionSpacing(settingsLayout, row++);

        labelPolygonsOpacity = settingsLayout.addChild(new StringWidget(polygonsOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPolygonsOpacity.setTooltip(polygonsOpacityTooltip);
        textPolygonsOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.POLYGONS_OPACITY), row++, 1);
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setValueChangedCallback(value -> {
            ClientConfig.POLYGONS_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        labelBorderWidth = settingsLayout.addChild(new StringWidget(borderWidthLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(borderWidthTooltip);
        textBorderWidth = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.BORDER_WIDTH), row++, 1);
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            ClientConfig.BORDER_WIDTH.set(value);
            previewFrontiers.configUpdated();
        });

        labelBorderOpacity = settingsLayout.addChild(new StringWidget(borderOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(borderOpacityTooltip);
        textBorderOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.BORDER_OPACITY), row++, 1);
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            ClientConfig.BORDER_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        addSectionSpacing(settingsLayout, row++);

        labelPathMarkerSize = settingsLayout.addChild(new StringWidget(pathMarkerSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPathMarkerSize.setTooltip(pathMarkerSizeTooltip);
        textPathMarkerSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.PATH_MARKER_SIZE), row++, 1);
        textPathMarkerSize.setMaxLength(1);
        textPathMarkerSize.setValueChangedCallback(value -> {
            ClientConfig.PATH_MARKER_SIZE.set(value);
            previewFrontiers.configUpdated();
        });

        labelPathMarkerOpacity = settingsLayout.addChild(new StringWidget(pathMarkerOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPathMarkerOpacity.setTooltip(pathMarkerOpacityTooltip);
        textPathMarkerOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.PATH_MARKER_OPACITY), row++, 1);
        textPathMarkerOpacity.setMaxLength(6);
        textPathMarkerOpacity.setValueChangedCallback(value -> {
            ClientConfig.PATH_MARKER_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        addSectionSpacing(settingsLayout, row++);

        labelTextSize = settingsLayout.addChild(new StringWidget(textSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(textSizeTooltip);
        textTextSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.TEXT_SIZE), row++, 1);
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            ClientConfig.TEXT_SIZE.set(value);
            previewFrontiers.configUpdated();
        });

        labelTextOpacity = settingsLayout.addChild(new StringWidget(textOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(textOpacityTooltip);
        textTextOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.TEXT_OPACITY), row++, 1);
        textTextOpacity.setMaxLength(6);
        textTextOpacity.setValueChangedCallback(value -> {
            ClientConfig.TEXT_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        labelTextUsesCustomColor = settingsLayout.addChild(new StringWidget(textColorLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextUsesCustomColor.setTooltip(textColorTooltip);
        buttonTextUsesCustomColor = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            ClientConfig.TEXT_COLOR.set(ClientConfig.TextColor.values()[b.getSelected()]);
            previewFrontiers.configUpdated();
        }), row++, 1);
        buttonTextUsesCustomColor.addOption(ClientConfig.getTranslatedEnum(ClientConfig.TextColor.FrontierColor));
        buttonTextUsesCustomColor.addOption(ClientConfig.getTranslatedEnum(ClientConfig.TextColor.FrontierColorBright));
        buttonTextUsesCustomColor.addOption(ClientConfig.getTranslatedEnum(ClientConfig.TextColor.White));
        buttonTextUsesCustomColor.setSelected(ClientConfig.TEXT_COLOR.get().ordinal());

        addSectionSpacing(settingsLayout, row++);

        labelBannerSize = settingsLayout.addChild(new StringWidget(bannerSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerSize.setTooltip(bannerSizeTooltip);
        textBannerSize = settingsLayout.addChild(createIntConfigTextBox(ClientConfig.BANNER_SIZE), row++, 1);
        textBannerSize.setMaxLength(2);
        textBannerSize.setValueChangedCallback(value -> {
            ClientConfig.BANNER_SIZE.set(value);
            previewFrontiers.configUpdated();
        });

        labelBannerOpacity = settingsLayout.addChild(new StringWidget(bannerOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerOpacity.setTooltip(bannerOpacityTooltip);
        textBannerOpacity = settingsLayout.addChild(createDoubleConfigTextBox(ClientConfig.BANNER_OPACITY), row++, 1);
        textBannerOpacity.setMaxLength(6);
        textBannerOpacity.setValueChangedCallback(value -> {
            ClientConfig.BANNER_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        previewFrontiers = columnsLayout.addChild(new PreviewFrontiersWidget());

        LinearLayout buttons = LinearLayout.horizontal().spacing(7);
        saveButton = buttons.addChild(new SimpleButton(font, 100, saveLabel, (b) -> saveAndClose()));
        saveButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM, ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_HIGHLIGHT);
        cancelButton = buttons.addChild(new SimpleButton(font, 100, cancelLabel, (b) -> onClose()));
        mainLayout.addChild(buttons);
    }

    @Override
    public void repositionElements() {
        previewFrontiers.setScaleFactor(scaleFactor);
        super.repositionElements();
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
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

    private static Tooltip tooltip(ConfigEntry<?, ?> entry) {
        Component component = entry.tooltip();
        return component == null ? null : Tooltip.create(component);
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

    private record AppearanceSnapshot(boolean hideNamesThatDontFit,
                                      double polygonsOpacity,
                                      int borderWidth,
                                      double borderOpacity,
                                      int pathMarkerSize,
                                      double pathMarkerOpacity,
                                      int textSize,
                                      double textOpacity,
                                      ClientConfig.TextColor textColor,
                                      int bannerSize,
                                      double bannerOpacity) {
        private static AppearanceSnapshot capture() {
            return new AppearanceSnapshot(
                    ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get(),
                    ClientConfig.POLYGONS_OPACITY.get(),
                    ClientConfig.BORDER_WIDTH.get(),
                    ClientConfig.BORDER_OPACITY.get(),
                    ClientConfig.PATH_MARKER_SIZE.get(),
                    ClientConfig.PATH_MARKER_OPACITY.get(),
                    ClientConfig.TEXT_SIZE.get(),
                    ClientConfig.TEXT_OPACITY.get(),
                    ClientConfig.TEXT_COLOR.get(),
                    ClientConfig.BANNER_SIZE.get(),
                    ClientConfig.BANNER_OPACITY.get()
            );
        }

        private void apply() {
            ClientConfig.HIDE_NAMES_THAT_DONT_FIT.set(hideNamesThatDontFit);
            ClientConfig.POLYGONS_OPACITY.set(polygonsOpacity);
            ClientConfig.BORDER_WIDTH.set(borderWidth);
            ClientConfig.BORDER_OPACITY.set(borderOpacity);
            ClientConfig.PATH_MARKER_SIZE.set(pathMarkerSize);
            ClientConfig.PATH_MARKER_OPACITY.set(pathMarkerOpacity);
            ClientConfig.TEXT_SIZE.set(textSize);
            ClientConfig.TEXT_OPACITY.set(textOpacity);
            ClientConfig.TEXT_COLOR.set(textColor);
            ClientConfig.BANNER_SIZE.set(bannerSize);
            ClientConfig.BANNER_OPACITY.set(bannerOpacity);
        }
    }
}
