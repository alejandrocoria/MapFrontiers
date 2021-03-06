package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.widget.TextFieldWidget;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.MiniMap;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.gui.GuiUtils;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiHUDSettings extends Screen implements TextBox.TextBoxResponder {
    private final GuiFrontierSettings parent;
    private GuiHUDWidget guiHUDWidget;
    private GuiOptionButton buttonSlot1;
    private GuiOptionButton buttonSlot2;
    private GuiOptionButton buttonSlot3;
    private TextBox textBannerSize;
    private GuiOptionButton buttonAnchor;
    private TextBox textPositionX;
    private TextBox textPositionY;
    private GuiOptionButton buttonAutoAdjustAnchor;
    private GuiOptionButton buttonSnapToBorder;
    private GuiSettingsButton buttonDone;
    private final List<GuiSimpleLabel> labels;
    private final Map<GuiSimpleLabel, List<ITextComponent>> labelTooltips;
    private MiniMap minimap;
    private final GuiHUD guiHUD;
    private int anchorLineColor = GuiColors.SETTINGS_ANCHOR_LIGHT;
    private int anchorLineColorTick = 0;

    public GuiHUDSettings(GuiFrontierSettings parent) {
        super(StringTextComponent.EMPTY);
        labels = new ArrayList<>();
        labelTooltips = new HashMap<>();
        this.parent = parent;
        guiHUD = GuiHUD.asPreview();
    }

    @Override
    public void init() {
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        if (UIManager.INSTANCE.isMiniMapEnabled()) {
            minimap = UIManager.INSTANCE.getMiniMap();
        }

        guiHUD.configUpdated(minecraft.getWindow());
        guiHUDWidget = new GuiHUDWidget(guiHUD, minimap, (widget) -> HUDUpdated());

        buttonSlot1 = new GuiOptionButton(font, width / 2 - 108, height / 2 - 32, 50, this::buttonPressed);
        buttonSlot1.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot1.setSelected(ConfigData.hudSlot1.ordinal());

        buttonSlot2 = new GuiOptionButton(font, width / 2 - 108, height / 2 - 16, 50, this::buttonPressed);
        buttonSlot2.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot2.setSelected(ConfigData.hudSlot2.ordinal());

        buttonSlot3 = new GuiOptionButton(font, width / 2 - 108, height / 2, 50, this::buttonPressed);
        buttonSlot3.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot3.setSelected(ConfigData.hudSlot3.ordinal());

        textBannerSize = new TextBox(font, width / 2 - 108, height / 2 + 16, 50, "");
        textBannerSize.setValue(String.valueOf(ConfigData.hudBannerSize));
        textBannerSize.setMaxLength(1);
        textBannerSize.setResponder(this);
        textBannerSize.setCentered(false);
        textBannerSize.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textBannerSize.setFrame(true);

        buttonAnchor = new GuiOptionButton(font, width / 2 + 70, height / 2 - 32, 100, this::buttonPressed);
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenTop.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenTopRight.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenRight.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenBottomRight.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenBottom.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenBottomLeft.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenLeft.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.ScreenTopLeft.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.Minimap.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.MinimapHorizontal.name());
        buttonAnchor.addOption(ConfigData.HUDAnchor.MinimapVertical.name());
        buttonAnchor.setSelected(ConfigData.hudAnchor.ordinal());

        textPositionX = new TextBox(font, width / 2 + 70, height / 2 - 16, 44, "");
        textPositionX.setValue(String.valueOf(ConfigData.hudXPosition));
        textPositionX.setMaxLength(5);
        textPositionX.setResponder(this);
        textPositionX.setCentered(false);
        textPositionX.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textPositionX.setFrame(true);

        textPositionY = new TextBox(font, width / 2 + 125, height / 2 - 16, 45, "");
        textPositionY.setValue(String.valueOf(ConfigData.hudYPosition));
        textPositionY.setMaxLength(5);
        textPositionY.setResponder(this);
        textPositionY.setCentered(false);
        textPositionY.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textPositionY.setFrame(true);

        buttonAutoAdjustAnchor = new GuiOptionButton(font, width / 2 + 70, height / 2, 100, this::buttonPressed);
        buttonAutoAdjustAnchor.addOption("true");
        buttonAutoAdjustAnchor.addOption("false");
        buttonAutoAdjustAnchor.setSelected(ConfigData.hudAutoAdjustAnchor ? 0 : 1);

        buttonSnapToBorder = new GuiOptionButton(font, width / 2 + 70, height / 2 + 16, 100, this::buttonPressed);
        buttonSnapToBorder.addOption("true");
        buttonSnapToBorder.addOption("false");
        buttonSnapToBorder.setSelected(ConfigData.hudSnapToBorder ? 0 : 1);

        buttonDone = new GuiSettingsButton(font, width / 2 - 50, height / 2 + 36, 100,
                new TranslationTextComponent("mapfrontiers.done"), this::buttonPressed);

        addButton(guiHUDWidget);
        addButton(buttonSlot1);
        addButton(buttonSlot2);
        addButton(buttonSlot3);
        addButton(textBannerSize);
        addButton(buttonAnchor);
        addButton(textPositionX);
        addButton(textPositionY);
        addButton(buttonAutoAdjustAnchor);
        addButton(buttonSnapToBorder);
        addButton(buttonDone);

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
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (minimap != null) {
            GlStateManager._matrixMode(GL11.GL_MODELVIEW);
            GlStateManager._pushMatrix();
            GlStateManager._matrixMode(GL11.GL_PROJECTION);
            GlStateManager._pushMatrix();
            minimap.drawMap(matrixStack, true);
            GlStateManager._matrixMode(GL11.GL_MODELVIEW);
            GlStateManager._popMatrix();
            GlStateManager._matrixMode(GL11.GL_PROJECTION);
            GlStateManager._popMatrix();
        }

        drawAnchor(matrixStack, minecraft.getWindow());

        fill(matrixStack, width / 2 - 178, height / 2 - 40, width / 2 + 178, height / 2 + 60, GuiColors.SETTINGS_BG);

        for (GuiSimpleLabel label : labels) {
            label.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        for (GuiSimpleLabel label : labels) {
            if (label.isHovered()) {
                List<ITextComponent> tooltip = labelTooltips.get(label);
                if (tooltip == null) {
                    continue;
                }

                GuiUtils.drawHoveringText(matrixStack, tooltip, mouseX, mouseY, width, height, 300, font);
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int value, int modifier) {
        if (key == GLFW.GLFW_KEY_E && !(getFocused() instanceof TextFieldWidget)) {
            onClose();
            return true;
        } else {
            return super.keyPressed(key, value, modifier);
        }
    }

    private void drawAnchor(MatrixStack matrixStack, MainWindow mainWindow) {
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
            minecraft.setScreen(parent);
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
    public void onClose() {
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
        ClientProxy.configUpdated();
        minecraft.setScreen(parent);
    }

    private void resetLabels() {
        labels.clear();

        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 170, height / 2 - 30, GuiSimpleLabel.Align.Left,
                new StringTextComponent("slot1"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot1"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 170, height / 2 - 14, GuiSimpleLabel.Align.Left,
                new StringTextComponent("slot2"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot2"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 170, height / 2 + 2, GuiSimpleLabel.Align.Left,
                new StringTextComponent("slot3"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.slot3"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 170, height / 2 + 18, GuiSimpleLabel.Align.Left,
                new StringTextComponent("bannerSize"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.bannerSize"));
        addLabelWithTooltip(new GuiSimpleLabel(font, width / 2 - 30, height / 2 - 30, GuiSimpleLabel.Align.Left,
                new StringTextComponent("anchor"), GuiColors.SETTINGS_TEXT), ConfigData.getTooltip("hud.anchor"));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 - 14, GuiSimpleLabel.Align.Left,
                        new StringTextComponent("position"), GuiColors.SETTINGS_TEXT),
                Collections.singletonList(new StringTextComponent("HUD position relative to anchor.")));
        labels.add(new GuiSimpleLabel(font, width / 2 + 119, height / 2 - 14, GuiSimpleLabel.Align.Center,
                new StringTextComponent("x"), GuiColors.SETTINGS_TEXT_DARK));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 + 2, GuiSimpleLabel.Align.Left,
                        new StringTextComponent("autoAdjustAnchor"), GuiColors.SETTINGS_TEXT),
                ConfigData.getTooltip("hud.autoAdjustAnchor"));
        addLabelWithTooltip(
                new GuiSimpleLabel(font, width / 2 - 30, height / 2 + 18, GuiSimpleLabel.Align.Left,
                        new StringTextComponent("snapToBorder"), GuiColors.SETTINGS_TEXT),
                ConfigData.getTooltip("hud.snapToBorder"));
    }

    private void addLabelWithTooltip(GuiSimpleLabel label, @Nullable List<ITextComponent> tooltip) {
        labels.add(label);
        if (tooltip != null) {
            labelTooltips.put(label, tooltip);
        }
    }

    @Override
    public void updatedValue(TextBox textBox, String value) {
    }

    @Override
    public void lostFocus(TextBox textBox, String value) {
        if (textBannerSize == textBox) {
            if (StringUtils.isBlank(value)) {
                textBannerSize.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textBannerSize.setValue(ConfigData.getDefault("hud.bannerSize"));
                ConfigData.hudBannerSize = Integer.parseInt(textBannerSize.getValue());
                guiHUD.configUpdated(minecraft.getWindow());
                updatePosition();
            } else {
                try {
                    textBannerSize.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    Integer size = Integer.valueOf(value);
                    if (ConfigData.isInRange("hud.bannerSize", size)) {
                        textBannerSize.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                        ConfigData.hudBannerSize = size;
                        guiHUD.configUpdated(minecraft.getWindow());
                        updatePosition();
                    }
                } catch (Exception ignored) {
                }
            }
        } else if (textPositionX == textBox) {
            if (StringUtils.isBlank(value)) {
                textPositionX.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textPositionX.setValue(ConfigData.getDefault("hud.position.x"));
                ConfigData.hudXPosition = Integer.parseInt(textPositionX.getValue());
                guiHUD.configUpdated(minecraft.getWindow());
                updatePosition();
            } else {
                try {
                    textPositionX.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    ConfigData.hudXPosition = Integer.parseInt(value);
                    guiHUD.configUpdated(minecraft.getWindow());
                    textPositionX.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                } catch (Exception ignored) {
                }
            }
        } else if (textPositionY == textBox) {
            if (StringUtils.isBlank(value)) {
                textPositionY.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                textPositionY.setValue(ConfigData.getDefault("hud.position.y"));
                ConfigData.hudYPosition = Integer.parseInt(textPositionY.getValue());
                guiHUD.configUpdated(minecraft.getWindow());
                updatePosition();
            } else {
                try {
                    textPositionY.setColor(GuiColors.SETTINGS_TEXT_ERROR, GuiColors.SETTINGS_TEXT_ERROR_HIGHLIGHT);
                    ConfigData.hudYPosition = Integer.parseInt(value);
                    guiHUD.configUpdated(minecraft.getWindow());
                    textPositionY.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
                } catch (Exception ignored) {
                }
            }
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
        textPositionX.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
        textPositionY.setColor(GuiColors.SETTINGS_TEXT, GuiColors.SETTINGS_TEXT_HIGHLIGHT);
    }
}
