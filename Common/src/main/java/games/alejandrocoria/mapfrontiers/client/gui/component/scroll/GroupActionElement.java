package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.common.settings.FrontierSettings;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GroupActionElement extends ScrollBox.ScrollElement {
    private final Font font;
    private final SettingsGroup group;
    private final CheckBoxButton createFrontier;
    private final CheckBoxButton deleteFrontier;
    private final CheckBoxButton updateFrontier;
    private final CheckBoxButton updateSettings;
    private final CheckBoxButton personalFrontier;
    private final List<GuiEventListener> children;

    public GroupActionElement(Font font, SettingsGroup group, ActionChangedConsumer actionChangedCallback) {
        this(font, group, false, actionChangedCallback);
    }

    public GroupActionElement(Font font, SettingsGroup group, boolean ownersGroup, ActionChangedConsumer actionChangedCallback) {
        super(430, 15);
        this.font = font;
        this.group = group;
        createFrontier = new CheckBoxButton(group.hasAction(FrontierSettings.Action.CreateGlobalFrontier),
                (b) -> actionChangedCallback.accept(group, FrontierSettings.Action.CreateGlobalFrontier, b.isChecked()));
        createFrontier.active = !ownersGroup;
        deleteFrontier = new CheckBoxButton(group.hasAction(FrontierSettings.Action.DeleteGlobalFrontier),
                (b) -> actionChangedCallback.accept(group, FrontierSettings.Action.DeleteGlobalFrontier, b.isChecked()));
        updateFrontier = new CheckBoxButton(group.hasAction(FrontierSettings.Action.UpdateGlobalFrontier),
                (b) -> actionChangedCallback.accept(group, FrontierSettings.Action.UpdateGlobalFrontier, b.isChecked()));
        updateSettings = new CheckBoxButton(group.hasAction(FrontierSettings.Action.UpdateSettings),
                (b) -> actionChangedCallback.accept(group, FrontierSettings.Action.UpdateSettings, b.isChecked()));
        updateSettings.active = !ownersGroup;
        personalFrontier = new CheckBoxButton(group.hasAction(FrontierSettings.Action.SharePersonalFrontier),
                (b) -> actionChangedCallback.accept(group, FrontierSettings.Action.SharePersonalFrontier, b.isChecked()));
        personalFrontier.active = !ownersGroup;

        children = List.of(createFrontier, deleteFrontier, updateFrontier, updateSettings, personalFrontier);
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        createFrontier.setX(x + 154);
        deleteFrontier.setX(x + 214);
        updateFrontier.setX(x + 274);
        updateSettings.setX(x + 334);
        personalFrontier.setX(x + 394);
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        createFrontier.setY(y + 2);
        deleteFrontier.setY(y + 2);
        updateFrontier.setY(y + 2);
        updateSettings.setY(y + 2);
        personalFrontier.setY(y + 2);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        String text = group.getName();
        if (text.isEmpty()) {
            text = I18n.get("mapfrontiers.unnamed", ChatFormatting.ITALIC);
        }

        graphics.text(font, text, x + 4, y + 4, ColorConstants.TEXT_HIGHLIGHT);

        createFrontier.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        deleteFrontier.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        updateFrontier.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        updateSettings.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        personalFrontier.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (visible && isHovered) {
            for (GuiEventListener checkBox : children) {
                if (checkBox.mouseClicked(event, doubleClick)) {
                    return ScrollBox.ScrollElement.Action.Handled;
                }
            }
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    @Override
    public List<GuiEventListener> children() {
        return children;
    }

    @FunctionalInterface
    public interface ActionChangedConsumer {
        void accept(SettingsGroup group, FrontierSettings.Action action, boolean checked);
    }
}
