package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.MiniMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiHUDSettings extends Screen implements TextIntBox.TextIntBoxResponder {
    private GuiHUDWidget guiHUDWidget;
    private GuiOptionButton buttonSlot1;
    private GuiOptionButton buttonSlot2;
    private GuiOptionButton buttonSlot3;
    private TextIntBox textBannerSize;
    private GuiOptionButton buttonAnchor;
    private TextIntBox textPositionX;
    private TextIntBox textPositionY;
    private GuiOptionButton buttonAutoAdjustAnchor;
    private GuiOptionButton buttonSnapToBorder;
    private GuiSettingsButton buttonDone;
    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<Component>> labelTooltips;
    private MiniMap minimap;
    private final GuiHUD guiHUD;
    private int anchorLineColor = GuiColors.SETTINGS_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    public GuiHUDSettings() {
        super(CommonComponents.EMPTY);
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
        guiHUD = GuiHUD.asPreview();
    }

    @Override
    public void init() {
        if (UIManager.INSTANCE.isMiniMapEnabled()) {
            minimap = UIManager.INSTANCE.getMiniMap();
        }

        guiHUD.configUpdated(minecraft.getWindow());
        guiHUDWidget = new GuiHUDWidget(guiHUD, minimap, (widget) -> HUDUpdated());

        buttonSlot1 = new GuiOptionButton(font, width / 2 - 104, height / 2 - 32, 64, this::buttonPressed);
        buttonSlot1.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.None));
        buttonSlot1.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Name));
        buttonSlot1.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Owner));
        buttonSlot1.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Banner));
        buttonSlot1.setSelected(ConfigData.hudSlot1.ordinal());

        buttonSlot2 = new GuiOptionButton(font, width / 2 - 104, height / 2 - 16, 64, this::buttonPressed);
        buttonSlot2.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.None));
        buttonSlot2.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Name));
        buttonSlot2.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Owner));
        buttonSlot2.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Banner));
        buttonSlot2.setSelected(ConfigData.hudSlot2.ordinal());

        buttonSlot3 = new GuiOptionButton(font, width / 2 - 104, height / 2, 64, this::buttonPressed);
        buttonSlot3.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.None));
        buttonSlot3.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Name));
        buttonSlot3.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Owner));
        buttonSlot3.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDSlot.Banner));
        buttonSlot3.setSelected(ConfigData.hudSlot3.ordinal());

        textBannerSize = new TextIntBox(3, 1, 8, font, width / 2 - 104, height / 2 + 16, 64);
        textBannerSize.setValue(String.valueOf(ConfigData.hudBannerSize));
        textBannerSize.setMaxLength(1);
        textBannerSize.setResponder(this);

        buttonAnchor = new GuiOptionButton(font, width / 2 + 96, height / 2 - 32, 134, this::buttonPressed);
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenTop));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenTopRight));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenRight));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenBottomRight));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenBottom));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenBottomLeft));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenLeft));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.ScreenTopLeft));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.Minimap));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.MinimapHorizontal));
        buttonAnchor.addOption(ConfigData.getTranslatedEnum(ConfigData.HUDAnchor.MinimapVertical));
        buttonAnchor.setSelected(ConfigData.hudAnchor.ordinal());

        textPositionX = new TextIntBox(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 96, height / 2 - 16, 61);
        textPositionX.setValue(String.valueOf(ConfigData.hudXPosition));
        textPositionX.setMaxLength(5);
        textPositionX.setResponder(this);

        textPositionY = new TextIntBox(0, Integer.MIN_VALUE, Integer.MAX_VALUE, font, width / 2 + 168, height / 2 - 16, 62);
        textPositionY.setValue(String.valueOf(ConfigData.hudYPosition));
        textPositionY.setMaxLength(5);
        textPositionY.setResponder(this);

        buttonAutoAdjustAnchor = new GuiOptionButton(font, width / 2 + 96, height / 2, 134, this::buttonPressed);
        buttonAutoAdjustAnchor.addOption(Component.translatable("options.on"));
        buttonAutoAdjustAnchor.addOption(Component.translatable("options.off"));
        buttonAutoAdjustAnchor.setSelected(ConfigData.hudAutoAdjustAnchor ? 0 : 1);

        buttonSnapToBorder = new GuiOptionButton(font, width / 2 + 96, height / 2 + 16, 134, this::buttonPressed);
        buttonSnapToBorder.addOption(Component.translatable("options.on"));
        buttonSnapToBorder.addOption(Component.translatable("options.off"));
        buttonSnapToBorder.setSelected(ConfigData.hudSnapToBorder ? 0 : 1);

        buttonDone = new GuiSettingsButton(font, width / 2 - 50, height / 2 + 36, 100,
                Component.translatable("mapfrontiers.done"), this::buttonPressed);

        addRenderableWidget(guiHUDWidget);
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
            if (anchorLineColor == GuiColors.SETTINGS_ANCHOR_LIGHT) {
                anchorLineColor = GuiColors.SETTINGS_ANCHOR_DARK;
            } else {
                anchorLineColor = GuiColors.SETTINGS_ANCHOR_LIGHT;
            }
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (minimap != null) {
            minimap.drawMap(matrixStack, true);
        }

        drawAnchor(matrixStack, minecraft.getWindow());

        fill(matrixStack, width / 2 - 238, height / 2 - 40, width / 2 + 238, height / 2 + 60, GuiColors.SETTINGS_BG);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        for (GuiSimpleLabel label : labels) {
            if (label.isHoveredOrFocused()) {
                List<Component> tooltip = labelTooltips.get(label);
                if (tooltip != null) {
                    renderTooltip(matrixStack, tooltip, Optional.empty(), mouseX, mouseY);
                }

                break;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof EditBox)) {
            ForgeHooksClient.popGuiLayer(minecraft);
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    private void drawAnchor(PoseStack matrixStack, Window mainWindow) {
        float factor = (float) mainWindow.getGuiScale();
        matrixStack.pushPose();
        matrixStack.scale(1.f / factor, 1.f / factor, 1.f);

        int directionX = 0;
        int directionY = 0;
        int length = 25;
        ConfigData.Point anchor = ConfigData.getHUDAnchor(ConfigData.hudAnchor);

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

        if (ConfigData.hudAnchor == ConfigData.HUDAnchor.Minimap) {
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
            hLine(matrixStack, anchor.x - length, anchor.x + length, anchor.y, anchorLineColor);
        } else {
            hLine(matrixStack, anchor.x, anchor.x + length * directionX, anchor.y, anchorLineColor);
        }

        if (directionY == 0) {
            vLine(matrixStack, anchor.x, anchor.y - length, anchor.y + length, anchorLineColor);
        } else {
            vLine(matrixStack, anchor.x, anchor.y, anchor.y + length * directionY, anchorLineColor);
        }

        matrixStack.popPose();
    }

    protected void buttonPressed(Button button) {
        if (button == buttonSlot1) {
            updateSlots();
        } else if (button == buttonSlot2) {
            updateSlots();
        } else if (button == buttonSlot3) {
            updateSlots();
        } else if (button == buttonAnchor) {
            ConfigData.hudAnchor = ConfigData.HUDAnchor.values()[buttonAnchor.getSelected()];
            guiHUD.configUpdated(minecraft.getWindow());
            updatePosition();
        } else if (button == buttonAutoAdjustAnchor) {
            ConfigData.hudAutoAdjustAnchor = buttonAutoAdjustAnchor.getSelected() == 0;
            guiHUD.configUpdated(minecraft.getWindow());
        } else if (button == buttonSnapToBorder) {
            ConfigData.hudSnapToBorder = buttonSnapToBorder.getSelected() == 0;
            guiHUD.configUpdated(minecraft.getWindow());
        } else if (button == buttonDone) {
            onClose();
        }
    }

    private void updateSlots() {
        updateSlotsValidity();
        boolean updated = false;
        if (buttonSlot1.getColor() == GuiColors.SETTINGS_TEXT || buttonSlot1.getColor() == GuiColors.SETTINGS_TEXT_HIGHLIGHT) {
            ConfigData.hudSlot1 = ConfigData.HUDSlot.values()[buttonSlot1.getSelected()];
            updated = true;
        }

        if (buttonSlot2.getColor() == GuiColors.SETTINGS_TEXT || buttonSlot2.getColor() == GuiColors.SETTINGS_TEXT_HIGHLIGHT) {
            ConfigData.hudSlot2 = ConfigData.HUDSlot.values()[buttonSlot2.getSelected()];
            updated = true;
        }

        if (buttonSlot3.getColor() == GuiColors.SETTINGS_TEXT || buttonSlot3.getColor() == GuiColors.SETTINGS_TEXT_HIGHLIGHT) {
            ConfigData.hudSlot3 = ConfigData.HUDSlot.values()[buttonSlot3.getSelected()];
            updated = true;
        }

        if (updated) {
            guiHUD.configUpdated(minecraft.getWindow());
            updatePosition();
        }
    }

    private void updateSlotsValidity() {
        ConfigData.HUDSlot slot1 = ConfigData.HUDSlot.values()[buttonSlot1.getSelected()];
        ConfigData.HUDSlot slot2 = ConfigData.HUDSlot.values()[buttonSlot2.getSelected()];
        ConfigData.HUDSlot slot3 = ConfigData.HUDSlot.values()[buttonSlot3.getSelected()];

        buttonSlot1.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        buttonSlot2.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        buttonSlot3.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);

        if (slot1 != ConfigData.HUDSlot.None && slot1 == slot2) {
            buttonSlot1.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
            buttonSlot2.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
        }

        if (slot1 != ConfigData.HUDSlot.None && slot1 == slot3) {
            buttonSlot1.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
        }

        if (slot2 != ConfigData.HUDSlot.None && slot2 == slot3) {
            buttonSlot2.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
            buttonSlot3.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
        }
    }

    @Override
    public void removed() {
        ClientProxy.configUpdated();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(new GuiFrontierSettings());
    }

    private void resetLabels() {
        for (GuiSimpleLabel label : labels) {
            removeWidget(label);
        }

        labels.clear();
        labelTooltips.clear();

        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 230, height / 2 - 30, GuiSimpleLabel.Align.Left,
                ConfigData.getTranslatedName("hud.slot1"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot1"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 230, height / 2 - 14, GuiSimpleLabel.Align.Left,
                ConfigData.getTranslatedName("hud.slot2"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot2"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 230, height / 2 + 2, GuiSimpleLabel.Align.Left,
                ConfigData.getTranslatedName("hud.slot3"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot3"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 230, height / 2 + 18, GuiSimpleLabel.Align.Left,
                ConfigData.getTranslatedName("hud.bannerSize"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.bannerSize"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 30, height / 2 - 30, GuiSimpleLabel.Align.Left,
                ConfigData.getTranslatedName("hud.anchor"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.anchor"));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 - 14, GuiSimpleLabel.Align.Left,
                        Component.translatable("mapfrontiers.config.hud.position"), GuiColors.SETTINGS_TEXT),
                Collections.singletonList(Component.literal("HUD position relative to anchor.")));
        labels.add(new GuiSimpleLabel(font, width / 2 + 162, height / 2 - 14, GuiSimpleLabel.Align.Center,
                Component.literal("x"), GuiColors.SETTINGS_TEXT_DARK));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 + 2, GuiSimpleLabel.Align.Left,
                        ConfigData.getTranslatedName("hud.autoAdjustAnchor"), GuiColors.SETTINGS_TEXT),
                ConfigData.getTooltip("hud.autoAdjustAnchor"));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 + 18, GuiSimpleLabel.Align.Left,
                        ConfigData.getTranslatedName("hud.snapToBorder"), GuiColors.SETTINGS_TEXT),
                ConfigData.getTooltip("hud.snapToBorder"));

        for (GuiSimpleLabel label : labels) {
            addRenderableOnly(label);
        }
    }

    private void addLabelWithTooltip(GuiSimpleLabel label, List<Component> tooltip) {
        labels.add(label);
        labelTooltips.put(label, tooltip);
    }

    @Override
    public void updatedValue(TextIntBox textIntBox, int value) {
        if (textBannerSize == textIntBox) {
            ConfigData.hudBannerSize = value;
            guiHUD.configUpdated(minecraft.getWindow());
            updatePosition();
        } else if (textPositionX == textIntBox) {
            ConfigData.hudXPosition = value;
            guiHUD.configUpdated(minecraft.getWindow());
        } else if (textPositionY == textIntBox) {
            ConfigData.hudYPosition = value;
            guiHUD.configUpdated(minecraft.getWindow());
        }
    }

    private void updatePosition() {
        ConfigData.Point anchorPoint = ConfigData.getHUDAnchor(ConfigData.hudAnchor);
        ConfigData.Point originPoint = ConfigData.getHUDOrigin(ConfigData.hudAnchor, guiHUD.getWidth(), guiHUD.getHeight());
        ConfigData.Point positionPoint = new ConfigData.Point();
        positionPoint.x = ConfigData.hudXPosition + anchorPoint.x - originPoint.x;
        positionPoint.y = ConfigData.hudYPosition + anchorPoint.y - originPoint.y;
        guiHUDWidget.setPositionHUD(positionPoint);
    }

    private void HUDUpdated() {
        buttonAnchor.setSelected(ConfigData.hudAnchor.ordinal());
        textPositionX.setValue(String.valueOf(ConfigData.hudXPosition));
        textPositionY.setValue(String.valueOf(ConfigData.hudYPosition));
    }
}
