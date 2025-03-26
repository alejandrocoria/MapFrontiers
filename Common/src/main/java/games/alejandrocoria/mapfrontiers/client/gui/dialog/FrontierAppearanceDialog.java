package games.alejandrocoria.mapfrontiers.client.gui.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PreviewFrontiersWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxDouble;
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
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private StringWidget labelHideNamesThatDontFit;
    private StringWidget labelPolygonsOpacity;
    private OptionButton buttonHideNamesThatDontFit;
    private TextBoxDouble textPolygonsOpacity;
    private PreviewFrontiersWidget previewFrontiers;

    private SimpleButton doneButton;

    public FrontierAppearanceDialog() {
        super(Component.empty(), 500, 350);
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
        buttonHideNamesThatDontFit = settingsLayout.addChild(new OptionButton(font, 40, (b) -> {
            Config.hideNamesThatDontFit = b.getSelected() == 0;
            previewFrontiers.configUpdated();
        }), row++, 1);
        buttonHideNamesThatDontFit.addOption(onLabel);
        buttonHideNamesThatDontFit.addOption(offLabel);
        buttonHideNamesThatDontFit.setSelected(Config.hideNamesThatDontFit ? 0 : 1);

        labelPolygonsOpacity = settingsLayout.addChild(new StringWidget(polygonsOpacityLabel, font).setColor(ColorConstants.TEXT), row, 0);
        labelPolygonsOpacity.setTooltip(polygonsOpacityTooltip);
        textPolygonsOpacity = settingsLayout.addChild(new TextBoxDouble(0.4, 0.0, 1.0, font, 40), row++, 1);
        textPolygonsOpacity.setValue(String.valueOf(Config.polygonsOpacity));
        textPolygonsOpacity.setMaxLength(6);
        textPolygonsOpacity.setValueChangedCallback(value -> {
            Config.polygonsOpacity = value;
            previewFrontiers.configUpdated();
        });

        previewFrontiers = columnsLayout.addChild(new PreviewFrontiersWidget());

        doneButton = mainLayout.addChild(new SimpleButton(font, 100, doneLabel, (b) -> onClose()));
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
        previewFrontiers.setScaleFactor(scaleFactor);
    }
}
