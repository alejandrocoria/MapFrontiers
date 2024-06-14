package games.alejandrocoria.mapfrontiers.client.gui.screen;

import com.mojang.blaze3d.platform.Window;
import games.alejandrocoria.mapfrontiers.client.event.ClientEventHandler;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.OptionButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.textbox.TextBoxInt;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUD;
import games.alejandrocoria.mapfrontiers.client.gui.hud.HUDWidget;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class HUDSettings extends Screen {
    private final Screen previousScreen;
    private final boolean showKeyHint;

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
    private final List<SimpleLabel> labels;
    private final Map<SimpleLabel, List<Component>> labelTooltips;
    private final HUD hud;
    private int anchorLineColor = ColorConstants.HUD_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    public HUDSettings(@Nullable Screen previousScreen, boolean showKeyHint) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.showKeyHint = showKeyHint;
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
        hud = HUD.asPreview();
    }

    @Override
    public void init() {
        ClientEventHandler.postUpdatedConfigEvent();
        HUDWidget = new HUDWidget(hud, Services.JOURNEYMAP.isMinimapEnabled(), (widget) -> HUDUpdated());

        buttonSlot1 = new OptionButton(font, width / 2 - 104, height / 2 - 32, 64, this::buttonPressed);
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot1.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot1.setSelected(Config.hudSlot1.ordinal());

        buttonSlot2 = new OptionButton(font, width / 2 - 104, height / 2 - 16, 64, this::buttonPressed);
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot2.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot2.setSelected(Config.hudSlot2.ordinal());

        buttonSlot3 = new OptionButton(font, width / 2 - 104, height / 2, 64, this::buttonPressed);
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.None));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Name));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Owner));
        buttonSlot3.addOption(Config.getTranslatedEnum(Config.HUDSlot.Banner));
        buttonSlot3.setSelected(Config.hudSlot3.ordinal());

        textBannerSize = new TextBoxInt(3, 1, 8, font, width / 2 - 104, height / 2 + 16, 64);
        textBannerSize.setValue(String.valueOf(Config.hudBannerSize));
        textBannerSize.setMaxLength(1);
        textBannerSize.setValueChangedCallback(value -> {
            Config.hudBannerSize = value;
            ClientEventHandler.postUpdatedConfigEvent();
            updatePosition();
        });

        buttonAnchor = new OptionButton(font, width / 2 + 96, height / 2 - 32, 134, this::buttonPressed);
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

        textPositionX = new TextBoxInt(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 96, height / 2 - 16, 61);
        textPositionX.setValue(String.valueOf(Config.hudXPosition));
        textPositionX.setMaxLength(5);
        textPositionX.setValueChangedCallback(value -> {
            Config.hudXPosition = value;
            ClientEventHandler.postUpdatedConfigEvent();
        });

        textPositionY = new TextBoxInt(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 168, height / 2 - 16, 62);
        textPositionY.setValue(String.valueOf(Config.hudYPosition));
        textPositionY.setMaxLength(5);
        textPositionY.setValueChangedCallback(value -> {
            Config.hudYPosition = value;
            ClientEventHandler.postUpdatedConfigEvent();
        });

        buttonAutoAdjustAnchor = new OptionButton(font, width / 2 + 96, height / 2, 134, this::buttonPressed);
        buttonAutoAdjustAnchor.addOption(Component.translatable("options.on"));
        buttonAutoAdjustAnchor.addOption(Component.translatable("options.off"));
        buttonAutoAdjustAnchor.setSelected(Config.hudAutoAdjustAnchor ? 0 : 1);

        buttonSnapToBorder = new OptionButton(font, width / 2 + 96, height / 2 + 16, 134, this::buttonPressed);
        buttonSnapToBorder.addOption(Component.translatable("options.on"));
        buttonSnapToBorder.addOption(Component.translatable("options.off"));
        buttonSnapToBorder.setSelected(Config.hudSnapToBorder ? 0 : 1);

        buttonDone = new SimpleButton(font, width / 2 - 50, height / 2 + 36, 100,
                Component.translatable("mapfrontiers.done"), this::buttonPressed);

        addRenderableWidget(HUDWidget);
        addRenderableWidget(buttonSlot1);
        addRenderableWidget(buttonSlot2);
        addRenderableWidget(buttonSlot3);
        addRenderableWidget(textBannerSize);
        addRenderableWidget(buttonAnchor);
        addRenderableWidget(textPositionX);
        addRenderableWidget(textPositionY);
        addRenderableWidget(buttonAutoAdjustAnchor);
        addRenderableWidget(buttonSnapToBorder);
        addRenderableWidget(buttonDone);

        resetLabels();
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (Services.JOURNEYMAP.isMinimapEnabled()) {
            Services.JOURNEYMAP.drawMinimapPreview(graphics);
        }

        drawAnchor(graphics, minecraft.getWindow());

        int x1 = width / 2 - 238;
        int x2 = width / 2 + 238;
        int y1 = height / 2 - 40;
        int y2 = height / 2 + 60;
        graphics.fill(x1, y1, x2, y2, ColorConstants.SCREEN_BG);
        graphics.hLine(x1, x2, y1, ColorConstants.TAB_BORDER);
        graphics.hLine(x1, x2, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x1, y1, y2, ColorConstants.TAB_BORDER);
        graphics.vLine(x2, y1, y2, ColorConstants.TAB_BORDER);

        // Rendering manually so the background is not drawn.
        for(GuiEventListener child : children()) {
            if (child instanceof Renderable renderable)
                renderable.render(graphics, mouseX, mouseY, partialTicks);
        }

        for (SimpleLabel label : labels) {
            if (label.visible) {
                label.render(graphics, mouseX, mouseY, partialTicks);
            }
        }

        for (SimpleLabel label : labels) {
            if (label.visible && label.isHovered()) {
                List<Component> tooltip = labelTooltips.get(label);
                if (tooltip != null) {
                    graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
                }

                break;
            }
        }
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

    protected void buttonPressed(Button button) {
        if (button == buttonSlot1) {
            updateSlots();
        } else if (button == buttonSlot2) {
            updateSlots();
        } else if (button == buttonSlot3) {
            updateSlots();
        } else if (button == buttonAnchor) {
            Config.hudAnchor = Config.HUDAnchor.values()[buttonAnchor.getSelected()];
            ClientEventHandler.postUpdatedConfigEvent();
            updatePosition();
        } else if (button == buttonAutoAdjustAnchor) {
            Config.hudAutoAdjustAnchor = buttonAutoAdjustAnchor.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonSnapToBorder) {
            Config.hudSnapToBorder = buttonSnapToBorder.getSelected() == 0;
            ClientEventHandler.postUpdatedConfigEvent();
        } else if (button == buttonDone) {
            onClose();
        }
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
    public void removed() {
        ClientEventHandler.postUpdatedConfigEvent();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new ModSettings(previousScreen, showKeyHint));
    }

    private void resetLabels() {
        labels.clear();
        labelTooltips.clear();

        addLabelWithTooltip(new SimpleLabel(font, width / 2 - 230, height / 2 - 30, SimpleLabel.Align.Left,
                Config.getTranslatedName("hud.slot1"), ColorConstants.TEXT), Config.getTooltip("hud.slot1"));
        addLabelWithTooltip(new SimpleLabel(font, width / 2 - 230, height / 2 - 14, SimpleLabel.Align.Left,
                Config.getTranslatedName("hud.slot2"), ColorConstants.TEXT), Config.getTooltip("hud.slot2"));
        addLabelWithTooltip(new SimpleLabel(font, width / 2 - 230, height / 2 + 2, SimpleLabel.Align.Left,
                Config.getTranslatedName("hud.slot3"), ColorConstants.TEXT), Config.getTooltip("hud.slot3"));
        addLabelWithTooltip(new SimpleLabel(font, width / 2 - 230, height / 2 + 18, SimpleLabel.Align.Left,
                Config.getTranslatedName("hud.bannerSize"), ColorConstants.TEXT), Config.getTooltip("hud.bannerSize"));
        addLabelWithTooltip(new SimpleLabel(font, width / 2 - 30, height / 2 - 30, SimpleLabel.Align.Left,
                Config.getTranslatedName("hud.anchor"), ColorConstants.TEXT), Config.getTooltip("hud.anchor"));
        addLabelWithTooltip(
                new SimpleLabel(font, width / 2 - 30, height / 2 - 14, SimpleLabel.Align.Left,
                        Component.translatable("mapfrontiers.config.hud.position"), ColorConstants.TEXT),
                Collections.singletonList(Component.literal("HUD position relative to anchor.")));
        labels.add(new SimpleLabel(font, width / 2 + 162, height / 2 - 14, SimpleLabel.Align.Center,
                Component.literal("x"), ColorConstants.TEXT_DARK));
        addLabelWithTooltip(
                new SimpleLabel(font, width / 2 - 30, height / 2 + 2, SimpleLabel.Align.Left,
                        Config.getTranslatedName("hud.autoAdjustAnchor"), ColorConstants.TEXT),
                Config.getTooltip("hud.autoAdjustAnchor"));
        addLabelWithTooltip(
                new SimpleLabel(font, width / 2 - 30, height / 2 + 18, SimpleLabel.Align.Left,
                        Config.getTranslatedName("hud.snapToBorder"), ColorConstants.TEXT),
                Config.getTooltip("hud.snapToBorder"));
    }

    private void addLabelWithTooltip(SimpleLabel label, List<Component> tooltip) {
        labels.add(label);
        labelTooltips.put(label, tooltip);
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
