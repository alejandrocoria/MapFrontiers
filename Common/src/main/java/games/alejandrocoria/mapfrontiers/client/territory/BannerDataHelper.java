package games.alejandrocoria.mapfrontiers.client.territory;

import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerBlockEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class BannerDataHelper {
    private BannerDataHelper() {
    }

    public static @Nullable BannerData fromBannerItem(@Nullable ItemStack item) {
        if (item == null || !(item.getItem() instanceof BannerItem itemBanner)) {
            return null;
        }

        return fromBanner(itemBanner.getColor(), BannerBlockEntity.getItemPatterns(item));
    }

    public static BannerData fromBanner(DyeColor baseColor, @Nullable ListTag patterns) {
        return new BannerData(baseColor, patterns, 0);
    }
}
