package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
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
    private GuiSettingsButton buttonDone;
    private List<GuiSimpleLabel> labels;
    private int id = 0;

    public GuiHUDSettings(GuiFrontierSettings parent) {
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/gui.png");
        labels = new ArrayList<GuiSimpleLabel>();
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonSlot1 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 140, height / 2 - 24, 60);
        buttonSlot1.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot1.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot1.setSelected(ConfigData.hud.slot1.ordinal());

        buttonSlot2 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 140, height / 2 - 8, 60);
        buttonSlot2.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot2.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot2.setSelected(ConfigData.hud.slot2.ordinal());

        buttonSlot3 = new GuiOptionButton(++id, mc.fontRenderer, width / 2 - 140, height / 2 + 8, 60);
        buttonSlot3.addOption(ConfigData.HUDSlot.None.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Name.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Owner.name());
        buttonSlot3.addOption(ConfigData.HUDSlot.Banner.name());
        buttonSlot3.setSelected(ConfigData.hud.slot3.ordinal());

        textBannerSize = new TextBox(++id, mc.fontRenderer, width / 2 + 70, height / 2 - 32, 100, "");
        textBannerSize.setText(String.valueOf(ConfigData.hud.bannerSize));
        textBannerSize.setMaxStringLength(1);
        textBannerSize.setResponder(this);
        textBannerSize.setCentered(false);
        textBannerSize.setColor(0xffbbbbbb, 0xffffffff);
        textBannerSize.setFrame(true);

        buttonAnchor = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 70, height / 2 - 16, 100);
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

        textPositionX = new TextBox(++id, mc.fontRenderer, width / 2 + 70, height / 2, 44, "");
        textPositionX.setText(String.valueOf(ConfigData.hud.position.x));
        textPositionX.setMaxStringLength(5);
        textPositionX.setResponder(this);
        textPositionX.setCentered(false);
        textPositionX.setColor(0xffbbbbbb, 0xffffffff);
        textPositionX.setFrame(true);

        textPositionY = new TextBox(++id, mc.fontRenderer, width / 2 + 125, height / 2, 45, "");
        textPositionY.setText(String.valueOf(ConfigData.hud.position.y));
        textPositionY.setMaxStringLength(5);
        textPositionY.setResponder(this);
        textPositionY.setCentered(false);
        textPositionY.setColor(0xffbbbbbb, 0xffffffff);
        textPositionY.setFrame(true);

        buttonAutoAdjustAnchor = new GuiOptionButton(++id, mc.fontRenderer, width / 2 + 70, height / 2 + 16, 100);
        buttonAutoAdjustAnchor.addOption("true");
        buttonAutoAdjustAnchor.addOption("false");
        buttonAutoAdjustAnchor.setSelected(ConfigData.hud.autoAdjustAnchor ? 0 : 1);

        buttonDone = new GuiSettingsButton(++id, mc.fontRenderer, width / 2 - 50, height / 2 + 36, 100, "Done");

        buttonList.add(buttonSlot1);
        buttonList.add(buttonSlot2);
        buttonList.add(buttonSlot3);
        buttonList.add(buttonAnchor);
        buttonList.add(buttonAutoAdjustAnchor);
        buttonList.add(buttonDone);

        resetLabels();
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(width / 2 - 178, height / 2 - 40, width / 2 + 178, height / 2 + 60, 0xc7101010);

        textBannerSize.drawTextBox(mouseX, mouseY);
        textPositionX.drawTextBox(mouseX, mouseY);
        textPositionY.drawTextBox(mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        if (btn == 0) {
            textBannerSize.mouseClicked(x, y, btn);
            textPositionX.mouseClicked(x, y, btn);
            textPositionY.mouseClicked(x, y, btn);
        }

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void mouseReleased(int x, int y, int state) {
        super.mouseReleased(x, y, state);
    }

    @Override
    protected void mouseClickMove(int x, int y, int btn, long timeSinceLastClick) {
        super.mouseClickMove(x, y, btn, timeSinceLastClick);
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
            ((ClientProxy) MapFrontiers.proxy).configUpdated();
        } else if (button == buttonSlot2) {
            ConfigData.hud.slot2 = ConfigData.HUDSlot.values()[buttonSlot2.getSelected()];
            ((ClientProxy) MapFrontiers.proxy).configUpdated();
        } else if (button == buttonSlot3) {
            ConfigData.hud.slot3 = ConfigData.HUDSlot.values()[buttonSlot3.getSelected()];
            ((ClientProxy) MapFrontiers.proxy).configUpdated();
        } else if (button == buttonAnchor) {
            ConfigData.hud.anchor = ConfigData.HUDAnchor.values()[buttonAnchor.getSelected()];
            ((ClientProxy) MapFrontiers.proxy).configUpdated();
        } else if (button == buttonAutoAdjustAnchor) {
            ConfigData.hud.autoAdjustAnchor = buttonAutoAdjustAnchor.getSelected() == 0;
            ((ClientProxy) MapFrontiers.proxy).configUpdated();
        } else if (button == buttonDone) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
        }
    }

    @Override
    public void onGuiClosed() {

    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void resetLabels() {
        labels.clear();

        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 - 22, GuiSimpleLabel.Align.Left, "slot1",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 - 6, GuiSimpleLabel.Align.Left, "slot2",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 170, height / 2 + 10, GuiSimpleLabel.Align.Left, "slot3",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 - 30, GuiSimpleLabel.Align.Left, "bannerSize",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 - 14, GuiSimpleLabel.Align.Left, "anchor",
                0xffdddddd));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 + 2, GuiSimpleLabel.Align.Left, "position",
                0xffdddddd));
        labels.add(
                new GuiSimpleLabel(fontRenderer, width / 2 + 120, height / 2 + 2, GuiSimpleLabel.Align.Center, "x", 0xff777777));
        labels.add(new GuiSimpleLabel(fontRenderer, width / 2 - 30, height / 2 + 18, GuiSimpleLabel.Align.Left,
                "autoAdjustAnchor", 0xffdddddd));
    }

    @Override
    public void updatedValue(int id, String value) {
    }

    @Override
    public void lostFocus(int id, String value) {
        if (textBannerSize.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textBannerSize.setText(ConfigData.getDefault("hud.bannerSize"));
                ((ClientProxy) MapFrontiers.proxy).configUpdated();
            } else {
                try {
                    Integer size = Integer.valueOf(value);
                    if (ConfigData.isInRange("hud.bannerSize", size)) {
                        ConfigData.hud.bannerSize = size;
                        ((ClientProxy) MapFrontiers.proxy).configUpdated();
                    }
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        } else if (textPositionX.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textPositionX.setText(ConfigData.getDefault("hud.position.x"));
                ((ClientProxy) MapFrontiers.proxy).configUpdated();
            } else {
                try {
                    Integer x = Integer.valueOf(value);
                    ConfigData.hud.position.x = x;
                    ((ClientProxy) MapFrontiers.proxy).configUpdated();
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        } else if (textPositionY.getId() == id) {
            if (StringUtils.isBlank(value)) {
                textPositionY.setText(ConfigData.getDefault("hud.position.y"));
                ((ClientProxy) MapFrontiers.proxy).configUpdated();
            } else {
                try {
                    Integer y = Integer.valueOf(value);
                    ConfigData.hud.position.y = y;
                    ((ClientProxy) MapFrontiers.proxy).configUpdated();
                } catch (Exception e) {
                    MapFrontiers.LOGGER.warn(e.getMessage(), e);
                }
            }
        }
    }
}
