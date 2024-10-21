package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDWidget;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HUDSettings extends AutoScaledScreen {
    private static final Component slot1Label = Config.getTranslatedName("hud.slot1");
    private static final Tooltip slot1Tooltip = Config.getTooltip("hud.slot1");
    private static final Component slot2Label = Config.getTranslatedName("hud.slot2");
    private static final Tooltip slot2Tooltip = Config.getTooltip("hud.slot2");
    private static final Component slot3Label = Config.getTranslatedName("hud.slot3");
    private static final Tooltip slot3Tooltip = Config.getTooltip("hud.slot3");
    private static final Component bannerSizeLabel = Config.getTranslatedName("hud.bannerSize");
    private static final Tooltip bannerSizeTooltip = Config.getTooltip("hud.bannerSize");
    private static final Component anchorLabel = Config.getTranslatedName("hud.anchor");
    private static final Tooltip anchorTooltip = Config.getTooltip("hud.anchor");
    private static final Component positionLabel = Component.translatable("mapfrontiers.config.hud.position");
    private static final Tooltip positionTooltip = Tooltip.create(Component.literal("HUD position relative to anchor."));
    private static final Component positionSeparatorLabel = Component.literal("x");
    private static final Component autoAdjustAnchorLabel = Config.getTranslatedName("hud.autoAdjustAnchor");
    private static final Tooltip autoAdjustAnchorTooltip = Config.getTooltip("hud.autoAdjustAnchor");
    private static final Component snapToBorderLabel = Config.getTranslatedName("hud.snapToBorder");
    private static final Tooltip snapToBorderTooltip = Config.getTooltip("hud.snapToBorder");
    private static final Component doneLabel = Component.translatable("gui.done");
    private static final Component onLabel = Component.translatable("options.on");
    private static final Component offLabel = Component.translatable("options.off");

    private HUDWidget HUDWidget;
    private OptionButton buttonSlot1;
    private OptionButton buttonSlot2;
    private OptionButton buttonSlot3;
    private TextBoxInt textBannerSize;
    private OptionButton buttonAnchor;
    private TextBoxInt textPositionX;
    private TextBoxInt textPositionY;
    private OptionButton buttonAutoAdjustAnchor;
    private OptionButton buttonSnapToBorder;
    private SimpleButton buttonDone;
    private final HUD hud;
    private int anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    private Screen previousScreen;

    public HUDSettings() {
        super(Component.empty());
        hud = HUD.asPreview();
    }

    @Override
    public void display() {
        super.display();
        previousScreen = backgroundScreen;
        backgroundScreen = null;
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        if (previousScreen != null) {
            previousScreen.resize(this.minecraft, this.width, this.height);
        }
    }

    @Override
    public void initScreen() {
        ClientEventHandler.postUpdatedConfigEvent();

        HUDWidget = addRenderableWidget(new HUDWidget(hud, Services.JOURNEYMAP.isMinimapEnabled(), (widget) -> HUDUpdated()));

        GridLayout mainLayout = new GridLayout().spacing(4);
        content.addChild(mainLayout);

        StringWidget labelSlot1 = mainLayout.addChild(new StringWidget(slot1Label, font).setColor(ColorConstants.TEXT), 0, 0);
        labelSlot1.setTooltip(slot1Tooltip);
        buttonSlot1 = new OptionButton(font, width / 2 - 104, height / 2 - 32, 64, (b) -> updateSlots());
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot1.setSelected(Config.hudSlot1.ordinal());
        mainLayout.addChild(buttonSlot1, 0, 1);

        StringWidget labelSlot2 = mainLayout.addChild(new StringWidget(slot2Label, font).setColor(ColorConstants.TEXT), 1, 0);
        labelSlot2.setTooltip(slot2Tooltip);
        buttonSlot2 = new OptionButton(font, width / 2 - 104, height / 2 - 16, 64, (b) -> updateSlots());
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot2.setSelected(Config.hudSlot2.ordinal());
        mainLayout.addChild(buttonSlot2, 1, 1);

        StringWidget labelSlot3 = mainLayout.addChild(new StringWidget(slot3Label, font).setColor(ColorConstants.TEXT), 2, 0);
        labelSlot3.setTooltip(slot3Tooltip);
        buttonSlot3 = new OptionButton(font, width / 2 - 104, height / 2, 64, (b) -> updateSlots());
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot3.setSelected(Config.hudSlot3.ordinal());
        mainLayout.addChild(buttonSlot3, 2, 1);

        StringWidget labelBannerSize = mainLayout.addChild(new StringWidget(bannerSizeLabel, font).setColor(ColorConstants.TEXT), 3, 0);
        labelBannerSize.setTooltip(bannerSizeTooltip);
        textBannerSize = new TextBoxInt(3, 1, 8, font, width / 2 - 104, height / 2 + 16, 64);
        textBannerSize.setValue(String.valueOf(Config.hudBannerSize));
        textBannerSize.setMaxLength(1);
        textBannerSize.setValueChangedCallback(value -> {
            Config.hudBannerSize = value;
            ClientEventHandler.postUpdatedConfigEvent();
            updatePosition();
        });
        mainLayout.addChild(textBannerSize, 3, 1);

        StringWidget labelAnchorLabel = mainLayout.addChild(new StringWidget(anchorLabel, font).setColor(ColorConstants.TEXT), 0, 3);
        labelAnchorLabel.setTooltip(anchorTooltip);
        buttonAnchor = new OptionButton(font, width / 2 + 96, height / 2 - 32, 134, (b) -> {
            Config.hudAnchor = Config.HUDAnchor.values()[b.getSelected()];
            ClientEventHandler.postUpdatedConfigEvent();
            updatePosition();
        });
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenTop));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenTopRight));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenRight));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenBottomRight));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenBottom));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenBottomLeft));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenLeft));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.ScreenTopLeft));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.Minimap));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.MinimapHorizontal));
        buttonAnchor.addOption(Config.getTranslatedEnum(Config.HUDAnchor.MinimapVertical));
        buttonAnchor.setSelected(Config.hudAnchor.ordinal());
        mainLayout.addChild(buttonAnchor, 0, 4);

        StringWidget labelPosition = mainLayout.addChild(new StringWidget(positionLabel, font).setColor(ColorConstants.TEXT), 1, 3);
        labelPosition.setTooltip(positionTooltip);
        LinearLayout positionLayout = LinearLayout.horizontal();
        mainLayout.addChild(positionLayout, 1, 4);

        textPositionX = new TextBoxInt(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 96, height / 2 - 16, 61);
        textPositionX.setValue(String.valueOf(Config.hudXPosition));
        textPositionX.setMaxLength(5);
        textPositionX.setValueChangedCallback(value -> {
            Config.hudXPosition = value;
            ClientEventHandler.postUpdatedConfigEvent();
        });
        positionLayout.addChild(textPositionX);

        positionLayout.addChild(SpacerElement.width(3));
        positionLayout.addChild(new StringWidget(positionSeparatorLabel, font).setColor(ColorConstants.TEXT_DARK));
        positionLayout.addChild(SpacerElement.width(2));

        textPositionY = new TextBoxInt(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 168, height / 2 - 16, 62);
        textPositionY.setValue(String.valueOf(Config.hudYPosition));
        textPositionY.setMaxLength(5);
        textPositionY.setValueChangedCallback(value -> {
            Config.hudYPosition = value;
            ClientEventHandler.postUpdatedConfigEvent();
        });
        positionLayout.addChild(textPositionY);

        StringWidget labelAutoAdjustAnchor = mainLayout.addChild(new StringWidget(autoAdjustAnchorLabel, font).setColor(ColorConstants.TEXT), 2, 3);
        labelAutoAdjustAnchor.setTooltip(autoAdjustAnchorTooltip);
        buttonAutoAdjustAnchor = new OptionButton(font, width / 2 + 96, height / 2, 134, (b) -> {
            Config.hudAutoAdjustAnchor = b.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        });
        buttonAutoAdjustAnchor.addOption(onLabel);
        buttonAutoAdjustAnchor.addOption(offLabel);
        buttonAutoAdjustAnchor.setSelected(Config.hudAutoAdjustAnchor ? 0 : 1);
        mainLayout.addChild(buttonAutoAdjustAnchor, 2, 4);

        StringWidget labelSnapToBorder = mainLayout.addChild(new StringWidget(snapToBorderLabel, font).setColor(ColorConstants.TEXT), 3, 3);
        labelSnapToBorder.setTooltip(snapToBorderTooltip);
        buttonSnapToBorder = new OptionButton(font, width / 2 + 96, height / 2 + 16, 134, (b) -> {
            Config.hudSnapToBorder = b.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        });
        buttonSnapToBorder.addOption(onLabel);
        buttonSnapToBorder.addOption(offLabel);
        buttonSnapToBorder.setSelected(Config.hudSnapToBorder ? 0 : 1);
        mainLayout.addChild(buttonSnapToBorder, 3, 4);

        buttonDone = new SimpleButton(font, width / 2 - 50, height / 2 + 36, 100, doneLabel, (b) -> onClose());
        mainLayout.addChild(buttonDone, 4, 0, 1, 5, LayoutSettings.defaults().alignHorizontallyCenter());

        updatePosition();
    }

    @Override
    public void tick() {
        ++anchorLineColorTick;

        if (anchorLineColorTick >= 3) {
            anchorLineColorTick = 0;
            if (anchorLineColor == ColorConstants.HUD_ANCHOR_LIGHT) {
                anchorLineColor = ColorConstants.HUD_ANCHOR_DARK;
            } else {
                anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
            }
        }
    }

    @Override
    public void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (Services.JOURNEYMAP.isMinimapEnabled()) {
            Services.JOURNEYMAP.drawMinimapPreview(graphics);
        }

        drawAnchor(graphics, minecraft.getWindow());
        drawCenteredBoxBackground(graphics, content.getWidth() + 20, content.getHeight() + 20);
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    private void drawAnchor(GuiGraphics graphics, Window mainWindow) {
        float factor = (float) mainWindow.getGuiScale();
        graphics.pose().pushPose();
        graphics.pose().scale(1.f / factor, 1.f / factor, 1.f);

        int directionX = 0;
        int directionY = 0;
        int length = 25;
        Config.Point anchor = Config.getHUDAnchor(Config.hudAnchor);

        int displayWidth = mainWindow.getWidth();
        int displayHeight = mainWindow.getHeight();

        if (anchor.x < displayWidth / 2) {
            directionX = 1;
        } else if (anchor.x > displayWidth / 2) {
            directionX = -1;
            --anchor.x;
        }

        if (anchor.y < displayHeight / 2) {
            directionY = 1;
        } else if (anchor.y > displayHeight / 2) {
            directionY = -1;
            --anchor.y;
        }

        if (Config.hudAnchor == Config.HUDAnchor.Minimap) {
            directionX = -directionX;
            directionY = -directionY;

            if (directionX == 1) {
                ++anchor.x;
            }
            if (directionY == 1) {
                ++anchor.y;
            }
        }

        if (directionX == 0) {
            graphics.hLine(anchor.x - length, anchor.x + length, anchor.y, anchorLineColor);
        } else {
            graphics.hLine(anchor.x, anchor.x + length * directionX, anchor.y, anchorLineColor);
        }

        if (directionY == 0) {
            graphics.vLine(anchor.x, anchor.y - length, anchor.y + length, anchorLineColor);
        } else {
            graphics.vLine(anchor.x, anchor.y, anchor.y + length * directionY, anchorLineColor);
        }

        graphics.pose().popPose();
    }

    private void updateSlots() {
        updateSlotsValidity();
        boolean updated = false;
        if (buttonSlot1.getColor() == ColorConstants.TEXT || buttonSlot1.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            Config.hudSlot1 = Config.HUDSlot.values()[buttonSlot1.getSelected()];
            updated = true;
        }

        if (buttonSlot2.getColor() == ColorConstants.TEXT || buttonSlot2.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            Config.hudSlot2 = Config.HUDSlot.values()[buttonSlot2.getSelected()];
            updated = true;
        }

        if (buttonSlot3.getColor() == ColorConstants.TEXT || buttonSlot3.getColor() == ColorConstants.TEXT_HIGHLIGHT) {
            Config.hudSlot3 = Config.HUDSlot.values()[buttonSlot3.getSelected()];
            updated = true;
        }

        if (updated) {
            ClientEventHandler.postUpdatedConfigEvent();
            updatePosition();
        }
    }

    private void updateSlotsValidity() {
        Config.HUDSlot slot1 = Config.HUDSlot.values()[buttonSlot1.getSelected()];
        Config.HUDSlot slot2 = Config.HUDSlot.values()[buttonSlot2.getSelected()];
        Config.HUDSlot slot3 = Config.HUDSlot.values()[buttonSlot3.getSelected()];

        buttonSlot1.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);
        buttonSlot2.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);
        buttonSlot3.setColor(ColorConstants.TEXT, ColorConstants.TEXT_HIGHLIGHT);

        if (slot1 != Config.HUDSlot.None && slot1 == slot2) {
            buttonSlot1.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot2.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }

        if (slot1 != Config.HUDSlot.None && slot1 == slot3) {
            buttonSlot1.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }

        if (slot2 != Config.HUDSlot.None && slot2 == slot3) {
            buttonSlot2.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(ColorConstants.TEXT_ERROR, ColorConstants.TEXT_ERROR_HIGHLIGHT);
        }
    }

    @Override
    public void onClose() {
        backgroundScreen = previousScreen;
        super.onClose();
    }

    @Override
    public void removed() {
        ClientEventHandler.postUpdatedConfigEvent();
    }

    private void updatePosition() {
        Config.Point anchorPoint = Config.getHUDAnchor(Config.hudAnchor);
        Config.Point originPoint = Config.getHUDOrigin(Config.hudAnchor, hud.getWidth(), hud.getHeight());
        Config.Point positionPoint = new Config.Point();
        positionPoint.x = Config.hudXPosition + anchorPoint.x - originPoint.x;
        positionPoint.y = Config.hudYPosition + anchorPoint.y - originPoint.y;
        HUDWidget.setPositionHUD(positionPoint);
    }

    private void HUDUpdated() {
        buttonAnchor.setSelected(Config.hudAnchor.ordinal());
        textPositionX.setValue(String.valueOf(Config.hudXPosition));
        textPositionY.setValue(String.valueOf(Config.hudYPosition));
    }
}
