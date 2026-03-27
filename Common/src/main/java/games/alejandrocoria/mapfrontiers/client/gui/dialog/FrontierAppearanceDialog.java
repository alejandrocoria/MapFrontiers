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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
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
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private StringWidget labelHideNamesThatDontFit;
    private StringWidget labelPolygonsOpacity;
    private StringWidget labelBorderWidth;
    private StringWidget labelBorderOpacity;
    private StringWidget labelTextSize;
    private StringWidget labelTextOpacity;
    private StringWidget labelTextUsesCustomColor;
    private StringWidget labelBannerSize;
    private StringWidget labelBannerOpacity;
    private OptionButton buttonHideNamesThatDontFit;
    private TextBoxDouble textPolygonsOpacity;
    private TextBoxInt textBorderWidth;
    private TextBoxDouble textBorderOpacity;
    private TextBoxInt textTextSize;
    private TextBoxDouble textTextOpacity;
    private OptionButton buttonTextUsesCustomColor;
    private TextBoxInt textBannerSize;
    private TextBoxDouble textBannerOpacity;
    private PreviewFrontiersWidget previewFrontiers;

    private SimpleButton doneButton;

    public FrontierAppearanceDialog() {
        super(Component.empty(), 455, 255);
    }

    @Override
    protected void initScreen() {
        LinearLayout mainLayout = LinearLayout.vertical().spacing(8);
        mainLayout.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(mainLayout);

        LinearLayout columnsLayout = LinearLayout.horizontal().spacing(8);
        mainLayout.addChild(columnsLayout);

        GridLayout settingsLayout = new GridLayout().spacing(4);
        settingsLayout.defaultCellSetting().alignHorizontallyLeft();
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

        labelPolygonsOpacity = settingsLayout.addChild(new StringWidget(polygonsOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPolygonsOpacity.setTooltip(polygonsOpacityTooltip);
        textPolygonsOpacity = settingsLayout.addChild(new TextBoxDouble(0.4, 0.0, 1.0, font, 60), row++, 1);
        textPolygonsOpacity.setValue(String.valueOf(ClientConfig.POLYGONS_OPACITY.get()));
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setValueChangedCallback(value -> {
            ClientConfig.POLYGONS_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        labelBorderWidth = settingsLayout.addChild(new StringWidget(borderWidthLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(borderWidthTooltip);
        textBorderWidth = settingsLayout.addChild(new TextBoxInt(0, 0, 64, font, 60), row++, 1);
        textBorderWidth.setValue(String.valueOf(ClientConfig.BORDER_WIDTH.get()));
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            ClientConfig.BORDER_WIDTH.set(value);
            previewFrontiers.configUpdated();
        });

        labelBorderOpacity = settingsLayout.addChild(new StringWidget(borderOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(borderOpacityTooltip);
        textBorderOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textBorderOpacity.setValue(String.valueOf(ClientConfig.BORDER_OPACITY.get()));
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            ClientConfig.BORDER_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        labelTextSize = settingsLayout.addChild(new StringWidget(textSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(textSizeTooltip);
        textTextSize = settingsLayout.addChild(new TextBoxInt(2, 1, 5, font, 60), row++, 1);
        textTextSize.setValue(String.valueOf(ClientConfig.TEXT_SIZE.get()));
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            ClientConfig.TEXT_SIZE.set(value);
            previewFrontiers.configUpdated();
        });

        labelTextOpacity = settingsLayout.addChild(new StringWidget(textOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(textOpacityTooltip);
        textTextOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textTextOpacity.setValue(String.valueOf(ClientConfig.TEXT_OPACITY.get()));
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

        labelBannerSize = settingsLayout.addChild(new StringWidget(bannerSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerSize.setTooltip(bannerSizeTooltip);
        textBannerSize = settingsLayout.addChild(new TextBoxInt(2, 1, 5, font, 60), row++, 1);
        textBannerSize.setValue(String.valueOf(ClientConfig.BANNER_SIZE.get()));
        textBannerSize.setMaxLength(2);
        textBannerSize.setValueChangedCallback(value -> {
            ClientConfig.BANNER_SIZE.set(value);
            previewFrontiers.configUpdated();
        });

        labelBannerOpacity = settingsLayout.addChild(new StringWidget(bannerOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerOpacity.setTooltip(bannerOpacityTooltip);
        textBannerOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textBannerOpacity.setValue(String.valueOf(ClientConfig.BANNER_OPACITY.get()));
        textBannerOpacity.setMaxLength(6);
        textBannerOpacity.setValueChangedCallback(value -> {
            ClientConfig.BANNER_OPACITY.set(value);
            previewFrontiers.configUpdated();
        });

        previewFrontiers = columnsLayout.addChild(new PreviewFrontiersWidget());

        doneButton = mainLayout.addChild(new SimpleButton(font, 100, doneLabel, (b) -> onClose()));
    }

    @Override
    public void repositionElements() {
        previewFrontiers.setScaleFactor(scaleFactor);
        super.repositionElements();
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public void onClose() {
        ClientGlobalEvents.postUpdatedConfigEvent();
        super.onClose();
    }

    private static Tooltip tooltip(ConfigEntry<?, ?> entry) {
        Component component = entry.tooltip();
        return component == null ? null : Tooltip.create(component);
    }
}
