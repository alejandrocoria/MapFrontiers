package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import com.mojang.blaze3d.platform.NativeImage;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
public final class PathMarkerCatalog {
    public record Entry(Identifier id, @Nullable Identifier texture, boolean directional, double segmentSpacingMultiplier) {
    }

    public static final List<Entry> BUILT_INS = List.of(
            entry(FrontierData.PathStyle.NONE, null, false, 1.0),
            entry(FrontierData.PathStyle.BIG_DOT, "big_dot", false, 2.0),
            entry(FrontierData.PathStyle.SMALL_DOT, "small_dot", false, 1.0),
            entry(FrontierData.PathStyle.RING, "ring", false, 1.5),
            entry(FrontierData.PathStyle.BIG_SQUARE, "big_square", false, 2.0),
            entry(FrontierData.PathStyle.SMALL_SQUARE, "small_square", false, 1.0),
            entry(FrontierData.PathStyle.DIAMOND, "diamond", false, 1.5),
            entry(FrontierData.PathStyle.X_CROSS, "x_cross", false, 1.5),
            entry(FrontierData.PathStyle.TRIANGLE, "triangle", true, 1.5),
            entry(FrontierData.PathStyle.ARROW, "arrow", true, 1.5),
            entry(FrontierData.PathStyle.CHEVRON, "chevron", true, 1.5)
    );
    private static final HighlightTextureCache HIGHLIGHT_TEXTURE_CACHE = new HighlightTextureCache();

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

    public static @Nullable Identifier getHighlightTexture(Identifier id) {
        Entry entry = get(id);
        return entry == null ? null : HIGHLIGHT_TEXTURE_CACHE.get(entry.texture());
    }

    private static Entry entry(Identifier id, @Nullable String textureName, boolean directional, double segmentSpacingMultiplier) {
        Identifier texture = textureName == null ? null
                : Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/markers/path/" + textureName + ".png");
        return new Entry(id, texture, directional, segmentSpacingMultiplier);
    }

    private static class HighlightTextureCache {
        private final Map<Identifier, Optional<Identifier>> textures = new HashMap<>();

        private @Nullable Identifier get(@Nullable Identifier sourceTexture) {
            if (sourceTexture == null) {
                return null;
            }

            return textures.computeIfAbsent(sourceTexture, this::createHighlightTexture).orElse(null);
        }

        private Optional<Identifier> createHighlightTexture(Identifier sourceTexture) {
            try {
                Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(sourceTexture);
                if (resource.isEmpty()) {
                    MapFrontiers.LOGGER.warn("Path marker texture not found: {}", sourceTexture);
                    return Optional.empty();
                }

                try (InputStream stream = resource.get().open(); NativeImage sourceImage = NativeImage.read(stream)) {
                    NativeImage highlightImage = createHighlightImage(sourceImage);
                    Identifier highlightTexture = getHighlightTextureId(sourceTexture);
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> highlightTexture.toString(), highlightImage);
                    Minecraft.getInstance().getTextureManager().register(highlightTexture, dynamicTexture);
                    return Optional.of(highlightTexture);
                }
            } catch (Throwable t) {
                MapFrontiers.LOGGER.error("Error creating path marker highlight texture for {}", sourceTexture, t);
                return Optional.empty();
            }
        }

        private static Identifier getHighlightTextureId(Identifier sourceTexture) {
            return Identifier.fromNamespaceAndPath(MapFrontiers.MODID,
                    "dynamic/path_marker_highlights/" + sourceTexture.getNamespace() + "/" + sourceTexture.getPath());
        }

        private static NativeImage createHighlightImage(NativeImage sourceImage) {
            NativeImage highlightImage = new NativeImage(sourceImage.getWidth(), sourceImage.getHeight(), false);
            for (int y = 0; y < sourceImage.getHeight(); ++y) {
                for (int x = 0; x < sourceImage.getWidth(); ++x) {
                    highlightImage.setPixel(x, y, createHighlightPixel(sourceImage.getPixel(x, y)));
                }
            }
            return highlightImage;
        }

        private static int createHighlightPixel(int pixel) {
            int alpha = ARGB.alpha(pixel);
            int brightness = Math.max(ARGB.red(pixel), Math.max(ARGB.green(pixel), ARGB.blue(pixel)));
            int highlightAlpha = alpha * (255 - brightness) / 255;
            return highlightAlpha == 0 ? 0 : ARGB.color(highlightAlpha, 255, 255, 255);
        }
    }
}
