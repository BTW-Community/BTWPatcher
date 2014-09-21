package net.minecraft.src;

public class ItemStack {
    public int stackSize;

    public ItemStack copy() {
        return null;
    }

    public Item getItem() {
        return null;
    }

    public int getItemDamage() {
        return 0;
    }

    public int getMaxDamage() {
        return 0;
    }

    public boolean hasEffectVanilla() {
        return false;
    }

    public NBTTagCompound getTagCompound() {
        return null;
    }
}
