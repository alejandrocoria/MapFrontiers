package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public final class BannerDataHelper {
    private BannerDataHelper() {
    }

    public static @Nullable BannerData fromBannerItem(@Nullable ItemStack item) {
        if (item == null || !(item.getItem() instanceof BannerItem itemBanner)) {
            return null;
        }

        return fromBanner(itemBanner.getColor(), getBannerPatternLayers(item));
    }

    public static BannerData fromBanner(DyeColor baseColor, BannerPatternLayers bannerPatterns) {
        return new BannerData(baseColor, encodePatterns(bannerPatterns), 0);
    }

    private static BannerPatternLayers getBannerPatternLayers(ItemStack item) {
        if (item.getComponents().has(DataComponents.BANNER_PATTERNS)) {
            return item.getComponents().get(DataComponents.BANNER_PATTERNS);
        }

        return BannerPatternLayers.EMPTY;
    }

    private static @Nullable ListTag encodePatterns(BannerPatternLayers bannerPatterns) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        Optional<Tag> patternsOptional = BannerPatternLayers.CODEC.encodeStart(
                level.registryAccess().createSerializationContext(NbtOps.INSTANCE), bannerPatterns).result();
        if (patternsOptional.isPresent() && patternsOptional.get().getType().equals(ListTag.TYPE)) {
            return BannerData.normalizePatterns((ListTag) patternsOptional.get().copy());
        }

        return null;
    }
}
