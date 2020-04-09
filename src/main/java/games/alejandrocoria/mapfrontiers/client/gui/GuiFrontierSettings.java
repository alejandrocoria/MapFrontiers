package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierSettings extends GuiScreen implements GuiGroupElement.GroupResponder {
    private FrontierSettings settings;
    private GuiScrollBox groups;
    private List<GuiSimpleLabel> labels;

    public GuiFrontierSettings() {
        labels = new ArrayList<GuiSimpleLabel>();
        groups = new GuiScrollBox(50, 50, 200, 400, 16);
    }

    @Override
    public void initGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        groups.drawBox(mc, mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        groups.mousePressed(mc, x, y);

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void actionPerformed(GuiButton button) {

    }

    @Override
    public void onGuiClosed() {

    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    public void setFrontierSettings(FrontierSettings settings) {
        this.settings = settings;

        groups.removeAll();
        groups.addElement(new GuiGroupElement(fontRenderer, settings.getOPsGroup(), this));
        groups.addElement(new GuiGroupElement(fontRenderer, settings.getOwnersGroup(), this));
        groups.addElement(new GuiGroupElement(fontRenderer, settings.getEveryoneGroup(), this));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupElement(fontRenderer, group, this));
        }
    }

    private void resetLabels() {
        labels.clear();
    }

    private void updateButtonsVisibility() {

    }

    @Override
    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
    }
}
