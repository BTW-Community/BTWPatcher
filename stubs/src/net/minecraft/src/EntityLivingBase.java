package net.minecraft.src;

public class EntityLivingBase extends Entity {
    public EntityLivingBase(World worldObj) {
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

    public ItemStack getCurrentItemOrArmor(int slot) {
        return null;
    }
}
