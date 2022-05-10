package games.alejandrocoria.mapfrontiers.common.item;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemFrontierBook extends Item {
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

        ClientProxy.openGUIFrontierBook();
        return new ActionResult<>(ActionResultType.PASS, itemStack);
    }
}
