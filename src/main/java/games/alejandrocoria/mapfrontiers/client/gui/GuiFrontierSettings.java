package games.alejandrocoria.mapfrontiers.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiScrollBox.ScrollElement;
import games.alejandrocoria.mapfrontiers.common.network.PacketFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import games.alejandrocoria.mapfrontiers.common.network.PacketRequestFrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings.Action;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiFrontierSettings extends GuiScreen implements GuiScrollBox.ScrollBoxResponder,
        GuiGroupActionElement.GroupActionResponder, GuiTabbedBox.TabbedBoxResponder, TextBox.TextBoxResponder {
    private static final int guiTextureSize = 512;

    private ResourceLocation guiTexture;
    private FrontierSettings settings;
    private GuiTabbedBox tabbedBox;
    private GuiScrollBox groups;
    private GuiScrollBox users;
    private GuiScrollBox groupsActions;
    private TextBox textNewGroupName;
    private GuiButtonIcon buttonNewGroup;
    private TextBox textGroupName;
    private List<GuiSimpleLabel> labels;
    private int tabSelected = 0;
    int id = 0;

    public GuiFrontierSettings() {
        guiTexture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/book.png");
        labels = new ArrayList<GuiSimpleLabel>();
    }

    @Override
    public void initGui() {
        PacketHandler.INSTANCE.sendToServer(new PacketRequestFrontierSettings());
        tabbedBox = new GuiTabbedBox(fontRenderer, 40, 24, width - 80, height - 64, this);
        tabbedBox.addTab(I18n.format("mapfrontiers.groups"));
        tabbedBox.addTab(I18n.format("mapfrontiers.actions"));
        tabbedBox.setTabSelected(tabSelected);
        groups = new GuiScrollBox(++id, 50, 50, 160, 400, 16, this);
        users = new GuiScrollBox(++id, 50, 80, 160, 400, 16, this);
        groupsActions = new GuiScrollBox(++id, width / 2 - 185, 80, 370, 400, 16, this);

        textNewGroupName = new TextBox(++id, fontRenderer, 50, 284, 140, "New group name");
        textNewGroupName.setMaxStringLength(22);
        textNewGroupName.setResponder(this);
        textNewGroupName.setCentered(false);
        textNewGroupName.setColor(0xffffffff);
        textNewGroupName.setFrame(true);

        buttonNewGroup = new GuiButtonIcon(++id, 191, 284, 13, 13, 494, 119, -23, guiTexture, guiTextureSize);

        textGroupName = new TextBox(++id, fontRenderer, 250, 50, 140, "Edit group name");
        textGroupName.setMaxStringLength(22);
        textGroupName.setResponder(this);
        textGroupName.setEnabled(false);
        textGroupName.setCentered(false);
        textGroupName.setColor(0xffffffff);

        buttonList.add(buttonNewGroup);
    }

    @Override
    public void updateScreen() {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        tabbedBox.drawBox(mc, mouseX, mouseY);
        groups.drawBox(mc, mouseX, mouseY);
        users.drawBox(mc, mouseX, mouseY);
        groupsActions.drawBox(mc, mouseX, mouseY);

        if (tabSelected == 0) {
            textNewGroupName.drawTextBox();
            if (groups.getSelectedElement() != null) {
                textGroupName.drawTextBox();
            }
        }

        for (GuiSimpleLabel label : labels) {
            label.drawLabel(mc, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int x, int y, int btn) throws IOException {
        tabbedBox.mousePressed(mc, x, y);
        groups.mousePressed(mc, x, y);
        users.mousePressed(mc, x, y);
        textNewGroupName.mouseClicked(x, y, btn);
        groupsActions.mousePressed(mc, x, y);
        textGroupName.mouseClicked(x, y, btn);

        super.mouseClicked(x, y, btn);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        textNewGroupName.textboxKeyTyped(typedChar, keyCode);
        textGroupName.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == buttonNewGroup) {
            SettingsGroup group = settings.createCustomGroup(textNewGroupName.getText());
            GuiGroupElement element = new GuiGroupElement(fontRenderer, buttonList, id, group, guiTexture, guiTextureSize);
            groups.addElement(element);

            groupClicked(element);
            textNewGroupName.setText("New group name");

            sendChangesToServer();
        }
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
        groups.addElement(new GuiGroupElement(fontRenderer, buttonList, id, settings.getOPsGroup(), guiTexture, guiTextureSize));
        groups.addElement(
                new GuiGroupElement(fontRenderer, buttonList, id, settings.getOwnersGroup(), guiTexture, guiTextureSize));
        groups.addElement(
                new GuiGroupElement(fontRenderer, buttonList, id, settings.getEveryoneGroup(), guiTexture, guiTextureSize));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groups.addElement(new GuiGroupElement(fontRenderer, buttonList, id, group, guiTexture, guiTextureSize));
        }

        groupsActions.removeAll();
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOPsGroup(), this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getOwnersGroup(), true, this));
        groupsActions.addElement(new GuiGroupActionElement(fontRenderer, settings.getEveryoneGroup(), this));

        for (SettingsGroup group : settings.getCustomGroups()) {
            groupsActions.addElement(new GuiGroupActionElement(fontRenderer, group, this));
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
        buttonNewGroup.visible = tabSelected == 0;
        groupsActions.visible = tabSelected == 1;
    }

    public void groupClicked(GuiGroupElement element) {
        groups.selectElement(element);
        textGroupName.setText(element.getGroup().getName());
        textGroupName.setEnabled(!element.getGroup().isSpecial());

        updateUsers();
    }

    @Override
    public void elementClicked(int id, ScrollElement element) {
        if (id == groups.getId()) {
            GuiGroupElement group = (GuiGroupElement) element;
            textGroupName.setText(group.getGroup().getName());
            textGroupName.setEnabled(!group.getGroup().isSpecial());
        }

        updateUsers();
    }

    @Override
    public void elementDelete(int id, ScrollElement element) {
        if (id == groups.getId()) {
            if (groups.getSelectedElement() != null) {
                groupClicked((GuiGroupElement) groups.getSelectedElement());
            }
            sendChangesToServer();
        } else if (id == users.getId()) {
            updateUsers();
            sendChangesToServer();
        }
    }

    @Override
    public void actionChanged(SettingsGroup group, Action action, boolean checked) {
        if (checked) {
            group.addAction(action);
        } else {
            group.removeAction(action);
        }

        sendChangesToServer();
    }

    @Override
    public void tabChanged(int tab) {
        tabSelected = tab;
        resetLabels();
        updateButtonsVisibility();
    }

    @Override
    public void updatedValue(int id, String value) {
    }

    @Override
    public void lostFocus(int id, String value) {
        if (textGroupName.getId() == id && tabSelected == 0) {
            GuiGroupElement groupElement = (GuiGroupElement) groups.getSelectedElement();
            if (groupElement != null) {
                groupElement.getGroup().setName(value);
                sendChangesToServer();
            }
        }
    }

    private void sendChangesToServer() {
        settings.advanceChangeCounter();
        PacketHandler.INSTANCE.sendToServer(new PacketFrontierSettings(settings));
    }

    private void updateUsers() {
        users.removeAll();
        GuiGroupElement element = (GuiGroupElement) groups.getSelectedElement();
        if (element != null && !element.getGroup().isSpecial()) {
            for (SettingsUser user : element.getGroup().getUsers()) {
                users.addElement(new GuiUserElement(fontRenderer, buttonList, id, user, guiTexture, guiTextureSize));
            }
        }
    }
}
