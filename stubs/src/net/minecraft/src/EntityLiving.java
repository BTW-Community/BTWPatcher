package net.minecraft.src;

public class EntityLiving extends Entity {
    protected int health;

    public EntityLiving(World worldObj) {
        super(worldObj);
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound var1) {
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound var1) {
    }

    public ItemStack getCurrentArmor(int slot) { // moved to EntityLivingSub in 13w16a
        return null;
    }
}
