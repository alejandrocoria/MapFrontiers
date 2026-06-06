package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PluginSourceBadge extends AbstractWidgetNoNarration {
    private static final int DEFAULT_HEIGHT = 12;
    private final Font font;

    public PluginSourceBadge(Font font, @Nullable String sourcePluginId, boolean showLabel) {
        this(font, sourcePluginId, showLabel, DEFAULT_HEIGHT);
    }

    public PluginSourceBadge(Font font, @Nullable String sourcePluginId, boolean showLabel, int height) {
        super(0, 0, resolveWidth(font, sourcePluginId, showLabel), resolveHeight(sourcePluginId, height),
                resolveMessage(font, sourcePluginId, showLabel));

        this.font = font;
        this.visible = SourcePluginUiHelper.hasSourcePlugin(sourcePluginId);
        this.active = this.visible;

        if (!this.visible) {
            return;
        }

        Tooltip tooltip = SourcePluginUiHelper.createTooltip(sourcePluginId);
        if (tooltip != null) {
            setTooltip(tooltip);
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        int textY = getY() + (getHeight() - 8) / 2;
        graphics.text(font, getMessage(), getX(), textY, ColorConstants.WHITE);
    }

    private static int resolveWidth(Font font, @Nullable String sourcePluginId, boolean showLabel) {
        if (!SourcePluginUiHelper.hasSourcePlugin(sourcePluginId)) {
            return 0;
        }

        return font.width(resolveMessage(font, sourcePluginId, showLabel));
    }

    private static int resolveHeight(@Nullable String sourcePluginId, int height) {
        return SourcePluginUiHelper.hasSourcePlugin(sourcePluginId) ? height : 0;
    }

    private static Component resolveMessage(Font font, @Nullable String sourcePluginId, boolean showLabel) {
        if (!SourcePluginUiHelper.hasSourcePlugin(sourcePluginId)) {
            return Component.empty();
        }

        if (!showLabel) {
            return SourcePluginUiHelper.createSymbolComponent();
        }

        SourcePluginUiHelper.SourcePluginDisplay display = SourcePluginUiHelper.createDisplay(font, sourcePluginId, Integer.MAX_VALUE);
        if (display == null) {
            return Component.empty();
        }

        return display.text();
    }
}
