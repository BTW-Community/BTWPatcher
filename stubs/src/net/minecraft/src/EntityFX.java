package net.minecraft.src;

public class EntityFX extends Entity {
    public EntityFX(World worldObj, double x, double y, double z) {
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

    public int getFXLayer() {
        return 0;
    }
}
