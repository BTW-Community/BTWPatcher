package net.minecraft.src;

public class ItemStack {
    public int stackSize;
    public NBTTagCompound stackTagCompound;

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
}
