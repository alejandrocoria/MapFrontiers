package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GroupActionElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final SettingsGroup group;
    private final ActionChangedConsumer actionChangedCallback;
    private final boolean ownersGroup;
    private boolean createFrontier;
    private boolean deleteFrontier;
    private boolean updateFrontier;
    private boolean updateSettings;
    private boolean personalFrontier;

    public GroupActionElement(Font font, SettingsGroup group, ActionChangedConsumer actionChangedCallback) {
        this(font, group, false, actionChangedCallback);
    }

    public GroupActionElement(Font font, SettingsGroup group, boolean ownersGroup, ActionChangedConsumer actionChangedCallback) {
        super(430, 16);
        this.font = font;
        this.group = group;
        this.actionChangedCallback = actionChangedCallback;
        this.ownersGroup = ownersGroup;
        createFrontier = group.hasAction(FrontierSettings.Action.CreateGlobalFrontier);
        deleteFrontier = group.hasAction(FrontierSettings.Action.DeleteGlobalFrontier);
        updateFrontier = group.hasAction(FrontierSettings.Action.UpdateGlobalFrontier);
        updateSettings = group.hasAction(FrontierSettings.Action.UpdateSettings);
        personalFrontier = group.hasAction(FrontierSettings.Action.SharePersonalFrontier);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        String text = group.getName();
        if (text.isEmpty()) {
            text = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
        }

        graphics.drawString(font, text, x + 4, y + 4, ColorConstants.TEXT_HIGHLIGHT);

        if (!ownersGroup) {
            drawBox(graphics, x + 154, y + 2, createFrontier);
        }

        drawBox(graphics, x + 214, y + 2, deleteFrontier);
        drawBox(graphics, x + 274, y + 2, updateFrontier);

        if (!ownersGroup) {
            drawBox(graphics, x + 334, y + 2, updateSettings);
            drawBox(graphics, x + 394, y + 2, personalFrontier);
        }
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY) {
        if (visible && isHovered && actionChangedCallback != null) {
            if (!ownersGroup && mouseX >= x + 130 && mouseX <= x + 190) {
                createFrontier = !createFrontier;
                actionChangedCallback.accept(group, FrontierSettings.Action.CreateGlobalFrontier, createFrontier);
            } else if (mouseX >= x + 190 && mouseX <= x + 250) {
                deleteFrontier = !deleteFrontier;
                actionChangedCallback.accept(group, FrontierSettings.Action.DeleteGlobalFrontier, deleteFrontier);
            } else if (mouseX >= x + 250 && mouseX <= x + 310) {
                updateFrontier = !updateFrontier;
                actionChangedCallback.accept(group, FrontierSettings.Action.UpdateGlobalFrontier, updateFrontier);
            } else if (!ownersGroup && mouseX >= x + 310 && mouseX <= x + 370) {
                updateSettings = !updateSettings;
                actionChangedCallback.accept(group, FrontierSettings.Action.UpdateSettings, updateSettings);
            } else if (!ownersGroup && mouseX >= x + 370 && mouseX <= x + 430) {
                personalFrontier = !personalFrontier;
                actionChangedCallback.accept(group, FrontierSettings.Action.SharePersonalFrontier, personalFrontier);
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    private void drawBox(GuiGraphics graphics, int x, int y, boolean checked) {
        graphics.fill(x, y, x + 12, y + 12, ColorConstants.CHECKBOX_BORDER);
        graphics.fill(x + 1, y + 1, x + 11, y + 11, ColorConstants.CHECKBOX_BG);
        if (checked) {
            graphics.fill(x + 2, y + 2, x + 10, y + 10, ColorConstants.CHECKBOX_CHECK);
        }
    }

    @FunctionalInterface
    public interface ActionChangedConsumer {
        void accept(SettingsGroup group, FrontierSettings.Action action, boolean checked);
    }
}
