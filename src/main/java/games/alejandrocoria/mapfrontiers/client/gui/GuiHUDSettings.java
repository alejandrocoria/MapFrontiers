package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.MiniMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiHUDSettings extends GuiScreen implements TextBox.TextBoxResponder {
    private static final int guiTextureSize = 512;

    private ResourceLocation guiTexture;
    private GuiFrontierSettings parent;
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
    private List<GuiSimpleLabel> labels;
    private int id = 0;
    private MiniMap minimap;
    private GuiHUD guiHUD;
    private int anchorLineColor = 0xffdddddd;
    private int anchorLineColorTick = 0;
    private ConfigData.Point positionHUD = new ConfigData.Point();
    private ConfigData.Point grabOffset = new ConfigData.Point();
    private boolean grabbed = false;

    public GuiHUDSettings(GuiFrontierSettings parent) {
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<GuiSimpleLabel>();
        this.parent = parent;
        guiHUD = GuiHUD.asPreview();
        guiHUD.configUpdated();
        updatePosition();
    }

    @Override
    public void initGui() {
        buttonSlot1 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 108, height / 2 - 32, 50);
        buttonSlot1.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot1.setSelected(ConfigData.hud.slot1.ordinal());

        buttonSlot2 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 108, height / 2 - 16, 50);
        buttonSlot2.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot2.setSelected(ConfigData.hud.slot2.ordinal());

        buttonSlot3 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 108, height / 2, 50);
        buttonSlot3.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot3.setSelected(ConfigData.hud.slot3.ordinal());

        textBannerSize = new TextBox(++id, mc.fontRenderer, width / 2 - 108, height / 2 + 16, 50, "");
        textBannerSize.setText(String.valueOf(ConfigData.hud.bannerSize));
        textBannerSize.setMaxStringLength(1);
        textBannerSize.setResponder(this);
        textBannerSize.setCentered(false);
        textBannerSize.setColor(0xffbbbbbb, 0xffffffff);
        textBannerSize.setFrame(true);

        buttonAnchor = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 70, height / 2 - 32, 100);
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
        buttonAnchor.setSelected(ConfigData.hud.anchor.ordinal());

        textPositionX = new TextBox(++id, mc.fontRenderer, width / 2 + 70, height / 2 - 16, 44, "");
        textPositionX.setText(String.valueOf(ConfigData.hud.position.x));
        textPositionX.setMaxStringLength(5);
        textPositionX.setResponder(this);
        textPositionX.setCentered(false);
        textPositionX.setColor(0xffbbbbbb, 0xffffffff);
        textPositionX.setFrame(true);

        textPositionY = new TextBox(++id, mc.fontRenderer, width / 2 + 125, height / 2 - 16, 45, "");
        textPositionY.setText(String.valueOf(ConfigData.hud.position.y));
        textPositionY.setMaxStringLength(5);
        textPositionY.setResponder(this);
        textPositionY.setCentered(false);
        textPositionY.setColor(0xffbbbbbb, 0xffffffff);
        textPositionY.setFrame(true);

        buttonAutoAdjustAnchor = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 70, height / 2, 100);
        buttonAutoAdjustAnchor.addOption("true");
        buttonAutoAdjustAnchor.addOption("false");
        buttonAutoAdjustAnchor.setSelected(ConfigData.hud.autoAdjustAnchor ? 0 : 1);

        buttonSnapToBorder = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 70, height / 2 + 16, 100);
        buttonSnapToBorder.addOption("true");
        buttonSnapToBorder.addOption("false");
        buttonSnapToBorder.setSelected(ConfigData.hud.snapToBorder ? 0 : 1);

        buttonDone = new GuiSettingsButton(++id, mc.fontRenderer, width / 2 - 50, height / 2 + 36, 100, "Done");

        buttonList.add(buttonSlot1);
        buttonList.add(buttonSlot2);
        buttonList.add(buttonSlot3);
        buttonList.add(buttonAnchor);
        buttonList.add(buttonAutoAdjustAnchor);
        buttonList.add(buttonSnapToBorder);
        buttonList.add(buttonDone);

        if (UIManager.INSTANCE.isMiniMapEnabled()) {
            minimap = UIManager.INSTANCE.getMiniMap();
        }

        resetLabels();
    }

    @Override
    public void updateScreen() {
        ++anchorLineColorTick;

        if (anchorLineColorTick >= 3) {
            anchorLineColorTick = 0;
            if (anchorLineColor == 0xffdddddd) {
                anchorLineColor = 0xff222222;
            } else {
                anchorLineColor = 0xffdddddd;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (minimap != null) {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            minimap.drawMap(true);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
        }

        guiHUD.draw();
        drawAnchor();

        Gui.drawRect(width / 2 - 178, height / 2 - 40, width / 2 + 178, height / 2 + 60, 0xc7101010);

        textBannerSize.drawTextBox(mouseX, mouseY);
        textPositionX.drawTextBox(mouseX, mouseY);
        textPositionY.drawTextBox(mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawAnchor() {
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int factor = scaledresolution.getScaleFactor();
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0 / factor, 1.0 / factor, 1.0);

        int directionX = 0;
        int directionY = 0;
        int length = 25;
        ConfigData.Point anchor = ConfigData.getHUDAnchor(ConfigData.hud.anchor);

        if (anchor.x < mc.displayWidth / 2) {
            directionX = 1;
        } else if (anchor.x > mc.displayWidth / 2) {
            directionX = -1;
            --anchor.x;
        }

        if (anchor.y < mc.displayHeight / 2) {
            directionY = 1;
        } else if (anchor.y > mc.displayHeight / 2) {
            directionY = -1;
            --anchor.y;
        }

        if (ConfigData.hud.anchor == ConfigData.HUDAnchor.Minimap) {
            directionX = -directionX;
            directionY = -directionY;
        }

        if (directionX == 0) {
            drawHorizontalLine(anchor.x - length, anchor.x + length, anchor.y, anchorLineColor);
        } else {
            drawHorizontalLine(anchor.x, anchor.x + length * directionX, anchor.y, anchorLineColor);
        }

        if (directionY == 0) {
            drawVerticalLine(anchor.x, anchor.y - length, anchor.y + length, anchorLineColor);
        } else {
            drawVerticalLine(anchor.x, anchor.y, anchor.y + length * directionY, anchorLineColor);
        }

        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        if (btn == 0) {
            boolean textClicked = false;
            textClicked |= textBannerSize.mouseClicked(x, y, btn);
            textClicked |= textPositionX.mouseClicked(x, y, btn);
            textClicked |= textPositionY.mouseClicked(x, y, btn);

            if (!textClicked && guiHUD.isInside(x, y)) {
                grabbed = true;
                grabOffset.x = x - positionHUD.x;
                grabOffset.y = y - positionHUD.y;
            }
        }

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void mouseReleased(int x, int y, int state) {
        super.mouseReleased(x, y, state);
        grabbed = false;
    }

    @Override
    protected void mouseClickMove(int x, int y, int btn, long timeSinceLastClick) {
        super.mouseClickMove(x, y, btn, timeSinceLastClick);
        if (grabbed) {
            positionHUD.x = x - grabOffset.x;
            positionHUD.y = y - grabOffset.y;

            ConfigData.Point anchorPoint = ConfigData.getHUDAnchor(ConfigData.hud.anchor);
            ConfigData.Point originPoint = ConfigData.getHUDOrigin(ConfigData.hud.anchor, guiHUD.getWidth(), guiHUD.getHeight());

            ConfigData.Point snapOffset = new ConfigData.Point();
            if (ConfigData.hud.snapToBorder) {
                snapOffset.x = 9999;
                snapOffset.y = 9999;
                for (ConfigData.HUDAnchor anchor : ConfigData.HUDAnchor.values()) {
                    ConfigData.Point anchorP = ConfigData.getHUDAnchor(anchor);
                    ConfigData.Point originP = ConfigData.getHUDOrigin(anchor, guiHUD.getWidth(), guiHUD.getHeight());
                    int offsetX = positionHUD.x - anchorP.x + originP.x;
                    int offsetY = positionHUD.y - anchorP.y + originP.y;
                    if (Math.abs(offsetX) < 16 && Math.abs(offsetX) < Math.abs(snapOffset.x)) {
                        snapOffset.x = offsetX;
                    }
                    if (Math.abs(offsetY) < 16 && Math.abs(offsetY) < Math.abs(snapOffset.y)) {
                        snapOffset.y = offsetY;
                    }
                }

                if (snapOffset.x == 9999) {
                    snapOffset.x = 0;
                }
                if (snapOffset.y == 9999) {
                    snapOffset.y = 0;
                }
            }

            ConfigData.hud.position.x = positionHUD.x - anchorPoint.x + originPoint.x - snapOffset.x;
            ConfigData.hud.position.y = positionHUD.y - anchorPoint.y + originPoint.y - snapOffset.y;

            guiHUD.configUpdated();

            textPositionX.setText(String.valueOf(ConfigData.hud.position.x));
            textPositionY.setText(String.valueOf(ConfigData.hud.position.y));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        } else {
            super.keyTyped(typedChar, keyCode);
            textBannerSize.textboxKeyTyped(typedChar, keyCode);
            textPositionX.textboxKeyTyped(typedChar, keyCode);
            textPositionY.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonSlot1) {
            ConfigData.hud.slot1 = ConfigData.HUDSlot.values()[buttonSlot1.getSelected()];
            guiHUD.configUpdated();
            updatePosition();
        } else if (button == buttonSlot2) {
            ConfigData.hud.slot2 = ConfigData.HUDSlot.values()[buttonSlot2.getSelected()];
            guiHUD.configUpdated();
            updatePosition();
        } else if (button == buttonSlot3) {
            ConfigData.hud.slot3 = ConfigData.HUDSlot.values()[buttonSlot3.getSelected()];
            guiHUD.configUpdated();
            updatePosition();
        } else if (button == buttonAnchor) {
            ConfigData.hud.anchor = ConfigData.HUDAnchor.values()[buttonAnchor.getSelected()];
            guiHUD.configUpdated();
            updatePosition();
        } else if (button == buttonAutoAdjustAnchor) {
            ConfigData.hud.autoAdjustAnchor = buttonAutoAdjustAnchor.getSelected() == 0;
            guiHUD.configUpdated();
        } else if (button == buttonSnapToBorder) {
            ConfigData.hud.snapToBorder = buttonSnapToBorder.getSelected() == 0;
            guiHUD.configUpdated();
        } else if (button == buttonDone) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }
    }

    @Override
    public void onGuiClosed() {
        ((ClientProxy) MapFrontiers.proxy).configUpdated();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void resetLabels() {
        labels.clear();

        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 - 30, GuiSimpleLabel.Align.Left, "slot1",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 - 14, GuiSimpleLabel.Align.Left, "slot2",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 + 2, GuiSimpleLabel.Align.Left, "slot3",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 + 18, GuiSimpleLabel.Align.Left, "bannerSize",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 - 30, GuiSimpleLabel.Align.Left, "anchor",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 - 14, GuiSimpleLabel.Align.Left, "position",
                0xffdddddd));
        labels.add(
                new GuiSimpleLabel(fontRenderer, width / 2 + 120, height / 2 - 14, GuiSimpleLabel.Align.Center, "x", 0xff777777));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 + 2, GuiSimpleLabel.Align.Left, "autoAdjustAnchor",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 + 18, GuiSimpleLabel.Align.Left, "snapToBorder",
                0xffdddddd));
    }

    @Override
    public void updatedValue(int id, String value) {
    }

    @Override
    public void lostFocus(int id, String value) {
        if (textBannerSize.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textBannerSize.setText(ConfigData.getDefault("hud.bannerSize"));
                ConfigData.hud.bannerSize = Integer.parseInt(textBannerSize.getText());
                guiHUD.configUpdated();
                updatePosition();
            } else {
                try {
                    Integer size = Integer.valueOf(value);
                    if (ConfigData.isInRange("hud.bannerSize", size)) {
                        ConfigData.hud.bannerSize = size;
                        guiHUD.configUpdated();
                        updatePosition();
                    }
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        } else if (textPositionX.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textPositionX.setText(ConfigData.getDefault("hud.position.x"));
                ConfigData.hud.position.x = Integer.parseInt(textPositionX.getText());
                guiHUD.configUpdated();
                updatePosition();
            } else {
                try {
                    Integer x = Integer.valueOf(value);
                    ConfigData.hud.position.x = x;
                    guiHUD.configUpdated();
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        } else if (textPositionY.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textPositionY.setText(ConfigData.getDefault("hud.position.y"));
                ConfigData.hud.position.y = Integer.parseInt(textPositionY.getText());
                guiHUD.configUpdated();
                updatePosition();
            } else {
                try {
                    Integer y = Integer.valueOf(value);
                    ConfigData.hud.position.y = y;
                    guiHUD.configUpdated();
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }

    private void updatePosition() {
        ConfigData.Point anchorPoint = ConfigData.getHUDAnchor(ConfigData.hud.anchor);
        ConfigData.Point originPoint = ConfigData.getHUDOrigin(ConfigData.hud.anchor, guiHUD.getWidth(), guiHUD.getHeight());
        positionHUD.x = ConfigData.hud.position.x + anchorPoint.x - originPoint.x;
        positionHUD.y = ConfigData.hud.position.y + anchorPoint.y - originPoint.y;
    }
}
