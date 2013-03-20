package net.minecraft.src;

public class ItemStack {
    public int itemID;
    public int stackSize;
    public NBTTagCompound stackTagCompound;

    public ItemStack copy() {
        return null;
    }

    public int getItemDamage() {
        return 0;
    }
}