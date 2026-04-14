package games.alejandrocoria.mapfrontiers.client.frontier;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public final class PathMarkerCatalog {
    public record Entry(Identifier id, @Nullable Identifier texture, boolean directional) {
    }

    public static final List<Entry> BUILT_INS = List.of(
            entry(FrontierData.PathStyle.NONE, null, false),
            entry(FrontierData.PathStyle.BIG_DOT, "big_dot", false),
            entry(FrontierData.PathStyle.SMALL_DOT, "small_dot", false),
            entry(FrontierData.PathStyle.RING, "ring", false),
            entry(FrontierData.PathStyle.BIG_SQUARE, "big_square", false),
            entry(FrontierData.PathStyle.SMALL_SQUARE, "small_square", false),
            entry(FrontierData.PathStyle.DIAMOND, "diamond", false),
            entry(FrontierData.PathStyle.X_CROSS, "x_cross", false),
            entry(FrontierData.PathStyle.ARROW, "arrow", true),
            entry(FrontierData.PathStyle.CHEVRON, "chevron", true)
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

    private static Entry entry(Identifier id, @Nullable String textureName, boolean directional) {
        Identifier texture = textureName == null ? null
                : Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/markers/path/" + textureName + ".png");
        return new Entry(id, texture, directional);
    }
}
