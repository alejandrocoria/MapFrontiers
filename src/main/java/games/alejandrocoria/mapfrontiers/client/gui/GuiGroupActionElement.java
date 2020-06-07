package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiGroupActionElement extends GuiScrollBox.ScrollElement {
    private final FontRenderer fontRenderer;
    private SettingsGroup group;
    private GroupActionResponder responder;
    private boolean ownersGroup;
    private boolean createFrontier;
    private boolean deleteFrontier;
    private boolean updateFrontier;
    private boolean updateSettings;
    private boolean personalFrontier;

    public GuiGroupActionElement(FontRenderer fontRenderer, SettingsGroup group, GroupActionResponder responder) {
        this(fontRenderer, group, false, responder);
    }

    public GuiGroupActionElement(FontRenderer fontRenderer, SettingsGroup group, boolean ownersGroup,
            GroupActionResponder responder) {
        super(430, 16);
        this.fontRenderer = fontRenderer;
        this.group = group;
        this.responder = responder;
        this.ownersGroup = ownersGroup;
        createFrontier = group.hasAction(FrontierSettings.Action.CreateFrontier);
        deleteFrontier = group.hasAction(FrontierSettings.Action.DeleteFrontier);
        updateFrontier = group.hasAction(FrontierSettings.Action.UpdateFrontier);
        updateSettings = group.hasAction(FrontierSettings.Action.UpdateSettings);
        personalFrontier = group.hasAction(FrontierSettings.Action.PersonalFrontier);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY, boolean selected) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            if (hovered) {
                Gui.drawRect(x, y, x + width, y + height, GuiColors.SETTINGS_ELEMENT_HOVERED);
            }

            String text = group.getName();
            if (text.isEmpty()) {
                text = I18n.format("mapfrontiers.unnamed", TextFormatting.ITALIC);
            }

            fontRenderer.drawString(text, x + 4, y + 4, GuiColors.SETTINGS_TEXT_HIGHLIGHT);

            if (!ownersGroup) {
                drawBox(x + 154, y + 2, createFrontier);
            }

            drawBox(x + 214, y + 2, deleteFrontier);
            drawBox(x + 274, y + 2, updateFrontier);

            if (!ownersGroup) {
                drawBox(x + 334, y + 2, updateSettings);
                drawBox(x + 394, y + 2, personalFrontier);
            }
        } else {
            hovered = false;
        }
    }

    @Override
    public GuiScrollBox.ScrollElement.Action mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (visible && hovered && responder != null) {
            if (!ownersGroup && mouseX >= x + 130 && mouseX <= x + 190) {
                createFrontier = !createFrontier;
                responder.actionChanged(group, FrontierSettings.Action.CreateFrontier, createFrontier);
            } else if (mouseX >= x + 190 && mouseX <= x + 250) {
                deleteFrontier = !deleteFrontier;
                responder.actionChanged(group, FrontierSettings.Action.DeleteFrontier, deleteFrontier);
            } else if (mouseX >= x + 250 && mouseX <= x + 310) {
                updateFrontier = !updateFrontier;
                responder.actionChanged(group, FrontierSettings.Action.UpdateFrontier, updateFrontier);
            } else if (!ownersGroup && mouseX >= x + 310 && mouseX <= x + 370) {
                updateSettings = !updateSettings;
                responder.actionChanged(group, FrontierSettings.Action.UpdateSettings, updateSettings);
            } else if (!ownersGroup && mouseX >= x + 370 && mouseX <= x + 430) {
                personalFrontier = !personalFrontier;
                responder.actionChanged(group, FrontierSettings.Action.PersonalFrontier, personalFrontier);
            }
        }

        return GuiScrollBox.ScrollElement.Action.None;
    }

    private void drawBox(int x, int y, boolean checked) {
        Gui.drawRect(x, y, x + 12, y + 12, GuiColors.SETTINGS_CHECKBOX_BORDER);
        Gui.drawRect(x + 1, y + 1, x + 11, y + 11, GuiColors.SETTINGS_CHECKBOX_BG);
        if (checked) {
            Gui.drawRect(x + 2, y + 2, x + 10, y + 10, GuiColors.SETTINGS_CHECKBOX_CHECK);
        }
    }

    @SideOnly(Side.CLIENT)
    public interface GroupActionResponder {
        public void actionChanged(SettingsGroup group, FrontierSettings.Action action, boolean checked);
    }
}
