package games.alejandrocoria.mapfrontiers.common.item;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.plugin.MapFrontiersPlugin;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFrontierBook extends Item {

    protected String name;

    public ItemFrontierBook() {
        setUnlocalizedName(MapFrontiers.MODID + "." + "frontier_book");
        setRegistryName("frontier_book");
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemStack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, itemStack);
        }

        NBTTagCompound nbt;
        if (itemStack.hasTagCompound()) {
            nbt = itemStack.getTagCompound();
        } else {
            nbt = new NBTTagCompound();
        }

        int dimension = playerIn.dimension;
        if (nbt.hasKey("Dimension")) {
            dimension = nbt.getInteger("Dimension");
        } else {
            nbt.setInteger("Dimension", dimension);

            NBTTagCompound nbtDisplay = nbt.getCompoundTag("display");
            if (!nbtDisplay.hasKey("Lore")) {
                nbtDisplay.setTag("Lore", new NBTTagList());
            }

            NBTTagList nbtTagList = (NBTTagList) nbtDisplay.getTag("Lore");
            nbtTagList.appendTag(new NBTTagString("Dimension " + String.valueOf(dimension)));
            nbtDisplay.setTag("Lore", nbtTagList);
            nbt.setTag("display", nbtDisplay);

            itemStack.setTagCompound(nbt);
        }

        MapFrontiersPlugin.instance.openGUIFrontierBook(dimension);
        return new ActionResult<ItemStack>(EnumActionResult.PASS, itemStack);
    }
}
