package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewFrontiersWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import games.alejandrocoria.mapfrontiers.common.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class FrontierAppearanceDialog extends AutoScaledScreen {
    private static final Component hideNamesThatDontFitLabel = Config.getTranslatedName("hideNamesThatDontFit");
    private static final Tooltip hideNamesThatDontFitTooltip = Config.getTooltip("hideNamesThatDontFit");
    private static final Component polygonsOpacityLabel = Config.getTranslatedName("polygonsOpacity");
    private static final Tooltip polygonsOpacityTooltip = Config.getTooltip("polygonsOpacity");
    private static final Component borderWidthLabel = Config.getTranslatedName("borderWidth");
    private static final Tooltip borderWidthTooltip = Config.getTooltip("borderWidth");
    private static final Component borderOpacityLabel = Config.getTranslatedName("borderOpacity");
    private static final Tooltip borderOpacityTooltip = Config.getTooltip("borderOpacity");
    private static final Component textSizeLabel = Config.getTranslatedName("textSize");
    private static final Tooltip textSizeTooltip = Config.getTooltip("textSize");
    private static final Component textOpacityLabel = Config.getTranslatedName("textOpacity");
    private static final Tooltip textOpacityTooltip = Config.getTooltip("textOpacity");
    private static final Component textColorLabel = Config.getTranslatedName("textColor");
    private static final Tooltip textColorTooltip = Config.getTooltip("textColor");
    private static final Component bannerSizeLabel = Config.getTranslatedName("bannerSize");
    private static final Tooltip bannerSizeTooltip = Config.getTooltip("bannerSize");
    private static final Component bannerOpacityLabel = Config.getTranslatedName("bannerOpacity");
    private static final Tooltip bannerOpacityTooltip = Config.getTooltip("bannerOpacity");
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
            Config.hideNamesThatDontFit = b.getSelected() == 0;
            previewFrontiers.configUpdated();
        }), row++, 1);
        buttonHideNamesThatDontFit.addOption(onLabel);
        buttonHideNamesThatDontFit.addOption(offLabel);
        buttonHideNamesThatDontFit.setSelected(Config.hideNamesThatDontFit ? 0 : 1);

        labelPolygonsOpacity = settingsLayout.addChild(new StringWidget(polygonsOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPolygonsOpacity.setTooltip(polygonsOpacityTooltip);
        textPolygonsOpacity = settingsLayout.addChild(new TextBoxDouble(0.4, 0.0, 1.0, font, 60), row++, 1);
        textPolygonsOpacity.setValue(String.valueOf(Config.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setValueChangedCallback(value -> {
            Config.polygonsOpacity = value;
            previewFrontiers.configUpdated();
        });

        labelBorderWidth = settingsLayout.addChild(new StringWidget(borderWidthLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderWidth.setTooltip(borderWidthTooltip);
        textBorderWidth = settingsLayout.addChild(new TextBoxInt(0, 0, 64, font, 60), row++, 1);
        textBorderWidth.setValue(String.valueOf(Config.borderWidth));
        textBorderWidth.setMaxLength(2);
        textBorderWidth.setValueChangedCallback(value -> {
            Config.borderWidth = value;
            previewFrontiers.configUpdated();
        });

        labelBorderOpacity = settingsLayout.addChild(new StringWidget(borderOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBorderOpacity.setTooltip(borderOpacityTooltip);
        textBorderOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textBorderOpacity.setValue(String.valueOf(Config.borderOpacity));
        textBorderOpacity.setMaxLength(6);
        textBorderOpacity.setValueChangedCallback(value -> {
            Config.borderOpacity = value;
            previewFrontiers.configUpdated();
        });

        labelTextSize = settingsLayout.addChild(new StringWidget(textSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextSize.setTooltip(textSizeTooltip);
        textTextSize = settingsLayout.addChild(new TextBoxInt(2, 1, 5, font, 60), row++, 1);
        textTextSize.setValue(String.valueOf(Config.textSize));
        textTextSize.setMaxLength(2);
        textTextSize.setValueChangedCallback(value -> {
            Config.textSize = value;
            previewFrontiers.configUpdated();
        });

        labelTextOpacity = settingsLayout.addChild(new StringWidget(textOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextOpacity.setTooltip(textOpacityTooltip);
        textTextOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textTextOpacity.setValue(String.valueOf(Config.textOpacity));
        textTextOpacity.setMaxLength(6);
        textTextOpacity.setValueChangedCallback(value -> {
            Config.textOpacity = value;
            previewFrontiers.configUpdated();
        });

        labelTextUsesCustomColor = settingsLayout.addChild(new StringWidget(textColorLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelTextUsesCustomColor.setTooltip(textColorTooltip);
        buttonTextUsesCustomColor = settingsLayout.addChild(new OptionButton(font, 60, (b) -> {
            Config.textColor = Config.TextColor.values()[b.getSelected()];
            previewFrontiers.configUpdated();
        }), row++, 1);
        buttonTextUsesCustomColor.addOption(Config.getTranslatedEnum(Config.TextColor.Frontier));
        buttonTextUsesCustomColor.addOption(Config.getTranslatedEnum(Config.TextColor.Bright));
        buttonTextUsesCustomColor.addOption(Config.getTranslatedEnum(Config.TextColor.White));
        buttonTextUsesCustomColor.setSelected(Config.textColor.ordinal());

        labelBannerSize = settingsLayout.addChild(new StringWidget(bannerSizeLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerSize.setTooltip(bannerSizeTooltip);
        textBannerSize = settingsLayout.addChild(new TextBoxInt(2, 1, 5, font, 60), row++, 1);
        textBannerSize.setValue(String.valueOf(Config.bannerSize));
        textBannerSize.setMaxLength(2);
        textBannerSize.setValueChangedCallback(value -> {
            Config.bannerSize = value;
            previewFrontiers.configUpdated();
        });

        labelBannerOpacity = settingsLayout.addChild(new StringWidget(bannerOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelBannerOpacity.setTooltip(bannerOpacityTooltip);
        textBannerOpacity = settingsLayout.addChild(new TextBoxDouble(1.0, 0.0, 1.0, font, 60), row++, 1);
        textBannerOpacity.setValue(String.valueOf(Config.bannerOpacity));
        textBannerOpacity.setMaxLength(6);
        textBannerOpacity.setValueChangedCallback(value -> {
            Config.bannerOpacity = value;
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
        ClientEventHandler.postUpdatedConfigEvent();
        super.onClose();
    }
}
