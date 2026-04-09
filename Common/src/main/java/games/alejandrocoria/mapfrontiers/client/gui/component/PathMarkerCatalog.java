package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public final class PathMarkerCatalog {
    public record Entry(Identifier id, @Nullable Identifier texture, int width, int height, boolean directional, Component label) {
    }

    public static final List<Entry> BUILT_INS = List.of(
            entry(FrontierData.PathStyle.NONE, null, 12, 12, false, "none"),
            entry(FrontierData.PathStyle.BIG_DOT, "big_dot", 12, 12, false, "big_dot"),
            entry(FrontierData.PathStyle.SMALL_DOT, "small_dot", 8, 8, false, "small_dot"),
            entry(FrontierData.PathStyle.RING, "ring", 12, 12, false, "ring"),
            entry(FrontierData.PathStyle.SQUARE, "square", 12, 12, false, "square"),
            entry(FrontierData.PathStyle.DIAMOND, "diamond", 12, 12, false, "diamond"),
            entry(FrontierData.PathStyle.ARROW, "arrow", 12, 12, true, "arrow"),
            entry(FrontierData.PathStyle.DOUBLE_ARROW, "double_arrow", 12, 12, true, "double_arrow"),
            entry(FrontierData.PathStyle.CHEVRON, "chevron", 12, 12, true, "chevron")
    );

    private PathMarkerCatalog() {
    }

    public static @Nullable Entry get(Identifier id) {
        for (Entry entry : BUILT_INS) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }

        return null;
    }

    private static Entry entry(Identifier id, @Nullable String textureName, int width, int height, boolean directional, String translationKey) {
        Identifier texture = textureName == null ? null
                : Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/markers/path/" + textureName + ".png");
        return new Entry(id, texture, width, height, directional, Component.translatable("mapfrontiers.path_marker." + translationKey));
    }
}
