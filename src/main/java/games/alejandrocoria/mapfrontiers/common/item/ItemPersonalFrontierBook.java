package games.alejandrocoria.mapfrontiers.common.item;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

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

        ClientProxy.openGUIFrontierBook();
        return new InteractionResultHolder<>(InteractionResult.PASS, itemStack);
    }
}
