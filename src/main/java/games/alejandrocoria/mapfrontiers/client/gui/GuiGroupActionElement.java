package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiGroupActionElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsGroup group;
    private GroupActionResponder responder;
    private boolean ownersGroup;
    private boolean createFrontier;
    private boolean deleteFrontier;
    private boolean updateFrontier;
    private boolean updateSettings;

    public GuiGroupActionElement(FontRenderer fontRenderer, SettingsGroup group, GroupActionResponder responder) {
        this(fontRenderer, group, false, responder);
    }

    public GuiGroupActionElement(FontRenderer fontRenderer, SettingsGroup group, boolean ownersGroup,
            GroupActionResponder responder) {
        super(400, 16);
        this.fontRenderer = fontRenderer;
        this.group = group;
        this.responder = responder;
        this.ownersGroup = ownersGroup;
        createFrontier = group.hasAction(FrontierSettings.Action.CreateFrontier);
        deleteFrontier = group.hasAction(FrontierSettings.Action.DeleteFrontier);
        updateFrontier = group.hasAction(FrontierSettings.Action.UpdateFrontier);
        updateSettings = group.hasAction(FrontierSettings.Action.UpdateSettings);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int color = 0xffffff;

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, 0xff222222);
            }

            fontRenderer.drawString(group.getName(), x + 4, y + 4, color);

            if (!ownersGroup) {
                drawBox(x + 300, y + 1, createFrontier);
            }

            drawBox(x + 320, y + 1, deleteFrontier);
            drawBox(x + 340, y + 1, updateFrontier);

            if (!ownersGroup) {
                drawBox(x + 360, y + 1, updateSettings);
            }
        } else {
            hovered = false;
        }
    }

    @Override
    public void mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible && hovered && responder != null) {
            if (!ownersGroup && mouseX >= x + 300 && mouseX <= x + 320) {
                createFrontier = !createFrontier;
                responder.actionChanged(group, FrontierSettings.Action.CreateFrontier, createFrontier);
            } else if (mouseX >= x + 320 && mouseX <= x + 340) {
                deleteFrontier = !deleteFrontier;
                responder.actionChanged(group, FrontierSettings.Action.DeleteFrontier, deleteFrontier);
            } else if (mouseX >= x + 340 && mouseX <= x + 360) {
                updateFrontier = !updateFrontier;
                responder.actionChanged(group, FrontierSettings.Action.UpdateFrontier, updateFrontier);
            } else if (!ownersGroup && mouseX >= x + 360 && mouseX <= x + 380) {
                updateSettings = !updateSettings;
                responder.actionChanged(group, FrontierSettings.Action.UpdateSettings, updateSettings);
            }
        }
    }

    private void drawBox(int x, int y, boolean checked) {
        Gui.drawRect(x, y, x + 10, y + 10, 0xff444444);
        Gui.drawRect(x + 1, y + 1, x + 8, y + 8, 0xff000000);
        if (checked) {
            Gui.drawRect(x + 2, y + 2, x + 6, y + 6, 0xff555555);
        }
    }

    @SideOnly(Side.CLIENT)
    public interface GroupActionResponder {
        public void actionChanged(SettingsGroup group, FrontierSettings.Action action, boolean checked);
    }
}
