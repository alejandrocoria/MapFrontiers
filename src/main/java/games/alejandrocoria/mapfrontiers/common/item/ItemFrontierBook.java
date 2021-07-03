package games.alejandrocoria.mapfrontiers.common.item;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemFrontierBook extends Item {
    protected String name;

    public ItemFrontierBook() {
        super(new Properties().stacksTo(1).tab(ItemGroup.TAB_TOOLS));
        setRegistryName(new ResourceLocation(MapFrontiers.MODID, "frontier_book"));
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemStack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide()) {
            return new ActionResult<>(ActionResultType.PASS, itemStack);
        }

        CompoundNBT nbt = itemStack.getTag();
        if (nbt == null) {
            nbt = new CompoundNBT();
        }

        RegistryKey<World> dimension = playerIn.level.dimension();
        if (!nbt.contains("Dimension")) {
            nbt.putString("Dimension", dimension.location().toString());
            itemStack.setTag(nbt);
        }

        ClientProxy.openGUIFrontierBook(dimension, false);
        return new ActionResult<>(ActionResultType.PASS, itemStack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack itemStack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        super.appendHoverText(itemStack, world, list, flag);

        CompoundNBT nbt = itemStack.getTag();
        if (nbt == null) {
            return;
        }

        if (nbt.contains("Dimension")) {
            RegistryKey<World> dimension = RegistryKey.create(Registry.DIMENSION_REGISTRY,
                    new ResourceLocation(nbt.getString("Dimension")));
            list.add(new StringTextComponent(dimension.location().toString()).withStyle(TextFormatting.GRAY));
        }
    }
}
