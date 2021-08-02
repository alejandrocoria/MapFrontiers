package games.alejandrocoria.mapfrontiers.common.item;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemPersonalFrontierBook extends Item {
    public ItemPersonalFrontierBook() {
        super(new Properties().stacksTo(1).tab(CreativeModeTab.TAB_TOOLS));
        setRegistryName(new ResourceLocation(MapFrontiers.MODID, "personal_frontier_book"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemStack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide()) {
            return new InteractionResultHolder<>(InteractionResult.PASS, itemStack);
        }

        CompoundTag nbt = itemStack.getTag();
        if (nbt == null) {
            nbt = new CompoundTag();
        }

        ResourceKey<Level> dimension = playerIn.level.dimension();
        if (!nbt.contains("Dimension")) {
            nbt.putString("Dimension", dimension.location().toString());
            itemStack.setTag(nbt);
        }

        ClientProxy.openGUIFrontierBook(dimension, true);
        return new InteractionResultHolder<>(InteractionResult.PASS, itemStack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack itemStack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemStack, world, list, flag);

        CompoundTag nbt = itemStack.getTag();
        if (nbt == null) {
            return;
        }

        if (nbt.contains("Dimension")) {
            ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY,
                    new ResourceLocation(nbt.getString("Dimension")));
            list.add(new TextComponent(dimension.location().toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
