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
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierSettings extends GuiScreen
        implements GuiGroupElement.GroupResponder, GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder {
    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiScrollBox groups;
    private GuiScrollBox groupsActions;
    private List<GuiSimpleLabel> labels;
    private int tabSelected = 1;

    public GuiFrontierSettings() {
        labels = new ArrayList<GuiSimpleLabel>();
    }

    @Override
    public void initGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
        tabbedBox = new GuiTabbedBox(fontRenderer, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(I18n.format("mapfrontiers.groups"));
        tabbedBox.addTab(I18n.format("mapfrontiers.actions"));
        tabbedBox.setTabSelected(tabSelected);
        groups = new GuiScrollBox(50, 50, 160, 400, 16);
        groupsActions = new GuiScrollBox(width / 2 - 185, 80, 370, 400, 16);
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        tabbedBox.drawBox(mc, mouseX, mouseY);
        groups.drawBox(mc, mouseX, mouseY);
        groupsActions.drawBox(mc, mouseX, mouseY);

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        tabbedBox.mousePressed(mc, x, y);
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
        updateButtonsVisibility();
    }

    private void resetLabels() {
        labels.clear();

        if (tabSelected == 1) {
            int x = width / 2 - 25;
            labels.add(new GuiSimpleLabel(fontRenderer, x, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.create_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 60, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.delete_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 120, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_frontier"), 0xffffffff));
            labels.add(new GuiSimpleLabel(fontRenderer, x + 180, 54, GuiSimpleLabel.Align.Center,
                    I18n.format("mapfrontiers.update_settings"), 0xffffffff));
        }
    }

    private void updateButtonsVisibility() {
        groups.visible = tabSelected == 0;
        groupsActions.visible = tabSelected == 1;
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

    @Override
    public void tabChanged(int tab) {
        tabSelected = tab;
        resetLabels();
        updateButtonsVisibility();
    }
}
