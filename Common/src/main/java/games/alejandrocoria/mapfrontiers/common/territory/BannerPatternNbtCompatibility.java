package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;

import javax.annotation.Nullable;
import java.util.Map;

public final class BannerPatternNbtCompatibility {
    private static final Map<String, ResourceKey<BannerPattern>> LEGACY_HASH_TO_PATTERN_KEY = Map.ofEntries(
            Map.entry("bs", BannerPatterns.STRIPE_BOTTOM),
            Map.entry("ts", BannerPatterns.STRIPE_TOP),
            Map.entry("ls", BannerPatterns.STRIPE_LEFT),
            Map.entry("rs", BannerPatterns.STRIPE_RIGHT),
            Map.entry("cs", BannerPatterns.STRIPE_CENTER),
            Map.entry("ms", BannerPatterns.STRIPE_MIDDLE),
            Map.entry("drs", BannerPatterns.STRIPE_DOWNRIGHT),
            Map.entry("dls", BannerPatterns.STRIPE_DOWNLEFT),
            Map.entry("ss", BannerPatterns.STRIPE_SMALL),
            Map.entry("cr", BannerPatterns.CROSS),
            Map.entry("sc", BannerPatterns.STRAIGHT_CROSS),
            Map.entry("bt", BannerPatterns.TRIANGLE_BOTTOM),
            Map.entry("tt", BannerPatterns.TRIANGLE_TOP),
            Map.entry("bts", BannerPatterns.TRIANGLES_BOTTOM),
            Map.entry("tts", BannerPatterns.TRIANGLES_TOP),
            Map.entry("ld", BannerPatterns.DIAGONAL_LEFT),
            Map.entry("rd", BannerPatterns.DIAGONAL_RIGHT),
            Map.entry("lud", BannerPatterns.DIAGONAL_LEFT_MIRROR),
            Map.entry("rud", BannerPatterns.DIAGONAL_RIGHT_MIRROR),
            Map.entry("mc", BannerPatterns.CIRCLE_MIDDLE),
            Map.entry("mr", BannerPatterns.RHOMBUS_MIDDLE),
            Map.entry("vh", BannerPatterns.HALF_VERTICAL),
            Map.entry("hh", BannerPatterns.HALF_HORIZONTAL),
            Map.entry("vhr", BannerPatterns.HALF_VERTICAL_MIRROR),
            Map.entry("hhb", BannerPatterns.HALF_HORIZONTAL_MIRROR),
            Map.entry("bo", BannerPatterns.BORDER),
            Map.entry("cbo", BannerPatterns.CURLY_BORDER),
            Map.entry("gra", BannerPatterns.GRADIENT),
            Map.entry("gru", BannerPatterns.GRADIENT_UP),
            Map.entry("bri", BannerPatterns.BRICKS),
            Map.entry("glb", BannerPatterns.GLOBE),
            Map.entry("cre", BannerPatterns.CREEPER),
            Map.entry("sku", BannerPatterns.SKULL),
            Map.entry("flo", BannerPatterns.FLOWER),
            Map.entry("moj", BannerPatterns.MOJANG),
            Map.entry("pig", BannerPatterns.PIGLIN)
    );

    private BannerPatternNbtCompatibility() {
    }

    public static NormalizationResult normalize(ListTag patterns) {
        ListTag normalizedPatterns = new ListTag();
        boolean changed = false;

        for (int i = 0; i < patterns.size(); ++i) {
            if (!(patterns.get(i) instanceof CompoundTag patternTag)) {
                return NormalizationResult.sanitized("Banner pattern entry at index " + i + " is not a compound tag.");
            }

            boolean hasCurrentPatternId = patternTag.contains("pattern");
            boolean hasCurrentColor = patternTag.contains("color");
            if (hasCurrentPatternId && hasCurrentColor) {
                String currentPatternId = patternTag.getString("pattern");
                String currentColor = patternTag.getString("color");
                try {
                    ResourceLocation.parse(currentPatternId);
                } catch (RuntimeException e) {
                    return NormalizationResult.sanitized("Invalid banner pattern id \"" + currentPatternId + "\" at index " + i + ".");
                }

                if (!isValidDyeColorName(currentColor)) {
                    return NormalizationResult.sanitized("Invalid banner dye color \"" + currentColor + "\" at index " + i + ".");
                }

                normalizedPatterns.add(patternTag.copy());
                continue;
            }

            if (!patternTag.contains("Pattern") || !patternTag.contains("Color")) {
                return NormalizationResult.sanitized("Banner pattern entry at index " + i + " does not match the current or legacy format.");
            }

            String legacyPatternHash = patternTag.getString("Pattern");
            ResourceKey<BannerPattern> patternKey = LEGACY_HASH_TO_PATTERN_KEY.get(legacyPatternHash);
            if (patternKey == null) {
                return NormalizationResult.sanitized("Unknown legacy banner pattern hash \"" + legacyPatternHash + "\" at index " + i + ".");
            }

            int colorId = patternTag.getInt("Color");
            if (colorId < 0 || colorId >= DyeColor.values().length) {
                return NormalizationResult.sanitized("Invalid legacy banner dye color id " + colorId + " at index " + i + ".");
            }

            CompoundTag migratedPattern = new CompoundTag();
            migratedPattern.putString("pattern", patternKey.location().toString());
            migratedPattern.putString("color", DyeColor.byId(colorId).getName());
            normalizedPatterns.add(migratedPattern);
            changed = true;
        }

        return new NormalizationResult(normalizedPatterns, changed, false, null);
    }

    private static boolean isValidDyeColorName(String colorName) {
        for (DyeColor dyeColor : DyeColor.values()) {
            if (dyeColor.getName().equals(colorName)) {
                return true;
            }
        }

        return false;
    }

    public record NormalizationResult(@Nullable ListTag patterns, boolean changed, boolean sanitizedInvalid, @Nullable String warningMessage) {
        public boolean hasWarning() {
            return warningMessage != null;
        }

        private static NormalizationResult sanitized(String warningMessage) {
            return new NormalizationResult(null, true, true, warningMessage);
        }
    }
}
