package games.alejandrocoria.mapfrontiers.common.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;

import javax.annotation.Nullable;

public final class BannerPatternNbtCompatibility {
    private BannerPatternNbtCompatibility() {
    }

    public static NormalizationResult normalize(ListTag patterns) {
        ListTag normalizedPatterns = new ListTag();
        boolean changed = false;

        for (int i = 0; i < patterns.size(); ++i) {
            if (!(patterns.get(i) instanceof CompoundTag patternTag)) {
                return NormalizationResult.sanitized("Banner pattern entry at index " + i + " is not a compound tag.");
            }

            if (!patternTag.contains("Pattern") || !patternTag.contains("Color")) {
                return NormalizationResult.sanitized("Banner pattern entry at index " + i + " is not a Minecraft 1.20.1 pattern.");
            }

            String legacyPatternHash = patternTag.getString("Pattern");
            if (BannerPattern.byHash(legacyPatternHash) == null) {
                return NormalizationResult.sanitized("Unknown legacy banner pattern hash \"" + legacyPatternHash + "\" at index " + i + ".");
            }

            int colorId = patternTag.getInt("Color");
            if (colorId < 0 || colorId >= DyeColor.values().length) {
                return NormalizationResult.sanitized("Invalid legacy banner dye color id " + colorId + " at index " + i + ".");
            }

            normalizedPatterns.add(patternTag.copy());
        }

        return new NormalizationResult(normalizedPatterns, changed, false, null);
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
