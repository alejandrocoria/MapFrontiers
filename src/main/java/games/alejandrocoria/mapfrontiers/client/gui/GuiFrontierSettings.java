package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierSettings extends GuiScreen
        implements GuiGroupElement.GroupResponder, GuiGroupActionElement.GroupActionResponder {
    private FrontierSettings settings;
    private GuiScrollBox groups;
    private GuiScrollBox groupsActions;
    private List<GuiSimpleLabel> labels;

    public GuiFrontierSettings() {
        labels = new ArrayList<GuiSimpleLabel>();
        groups = new GuiScrollBox(50, 50, 200, 400, 16);
        groupsActions = new GuiScrollBox(300, 50, 370, 400, 16);
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
        groupsActions.drawBox(mc, mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        groups.mousePressed(mc, x, y);
        groupsActions.mousePressed(mc, x, y);

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

        groupsActions.removeAll();
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOPsGroup(), this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOwnersGroup(), true, this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getEveryoneGroup(), this));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupActionElement(fontRenderer, group, this));
        }

        resetLabels();
    }

    private void resetLabels() {
        labels.clear();

        labels.add(new GuiSimpleLabel(fontRenderer, 460, 28, GuiSimpleLabel.Align.Center, "Create\nfrontier", 0xffffffff));
        labels.add(new GuiSimpleLabel(fontRenderer, 520, 28, GuiSimpleLabel.Align.Center, "Delete\nfrontier", 0xffffffff));
        labels.add(new GuiSimpleLabel(fontRenderer, 580, 28, GuiSimpleLabel.Align.Center, "Update\nfrontier", 0xffffffff));
        labels.add(new GuiSimpleLabel(fontRenderer, 640, 28, GuiSimpleLabel.Align.Center, "Update\nsettings", 0xffffffff));
    }

    private void updateButtonsVisibility() {

    }

    @Override
    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
    }

    @Override
    public void actionChanged(SettingsGroup group, Action action, boolean checked) {
        if (checked) {
            group.addAction(action);
        } else {
            group.removeAction(action);
        }

        settings.advanceChangeCounter();
        PacketHandler.INSTANCE.sendToServer(new PacketFrontierSettings(settings));
    }
}
