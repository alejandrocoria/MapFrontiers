package games.alejandrocoria.mapfrontiers.client.gui.util;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class SourcePluginUiHelper {
    private static final String CREATED_BY_PLUGIN_KEY = "mapfrontiers.created_by_plugin";
    private static final String PLUGIN_ID_KEY = "mapfrontiers.plugin_id";
    private static final String PLUGIN_NAME_KEY = "mapfrontiers.plugin_name";
    private static final String SOURCE_PLUGIN_SYMBOL = "\uD83E\uDDE9";

    public static @Nullable SourcePluginDisplay createDisplay(Font font, @Nullable String sourcePluginId, int maxWidth) {
        if (sourcePluginId == null) {
            return null;
        }

        String sourcePluginName = Services.PLATFORM.getModDisplayName(sourcePluginId);
        String sourcePluginDisplay = !StringUtil.isBlank(sourcePluginName) ? sourcePluginName : sourcePluginId;
        int symbolWidth = font.width(SOURCE_PLUGIN_SYMBOL);
        int separatorWidth = font.width(" ");
        Component text;

        if (maxWidth <= symbolWidth) {
            text = createSymbolComponent();
        } else {
            int visibleNameMaxWidth = Math.max(0, maxWidth - symbolWidth - separatorWidth);
            String visibleName = TextEllipsizeHelper.ellipsizeByWidth(font, sourcePluginDisplay, visibleNameMaxWidth);
            if (visibleName.isEmpty()) {
                text = createSymbolComponent();
            } else {
                text = Component.literal(visibleName + " ").withStyle(ColorConstants.WARNING).append(createSymbolComponent());
            }
        }

        return new SourcePluginDisplay(
                text,
                buildTooltip(sourcePluginId, sourcePluginName)
        );
    }

    public static @Nullable Tooltip createTooltip(@Nullable String sourcePluginId) {
        if (sourcePluginId == null) {
            return null;
        }

        return buildTooltip(sourcePluginId, Services.PLATFORM.getModDisplayName(sourcePluginId));
    }

    public static Component createSymbolComponent() {
        return Component.literal(SOURCE_PLUGIN_SYMBOL).withStyle(ColorConstants.WARNING);
    }

    private static Tooltip buildTooltip(String sourcePluginId, @Nullable String sourcePluginName) {
        MutableComponent tooltip = Component.empty()
                .append(Component.literal(SOURCE_PLUGIN_SYMBOL).withStyle(ColorConstants.WARNING))
                .append(Component.literal(" "))
                .append(Component.translatable(CREATED_BY_PLUGIN_KEY))
                .append(Component.literal("\n"))
                .append(Component.translatable(PLUGIN_ID_KEY, sourcePluginId));
        if (!StringUtil.isBlank(sourcePluginName)) {
            tooltip.append(Component.literal("\n"))
                    .append(Component.translatable(PLUGIN_NAME_KEY, sourcePluginName));
        }
        return Tooltip.create(tooltip);
    }

    public record SourcePluginDisplay(Component text, Tooltip tooltip) {
    }

    private SourcePluginUiHelper() {
    }
}
