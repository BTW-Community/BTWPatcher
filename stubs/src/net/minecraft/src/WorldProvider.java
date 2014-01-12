package net.minecraft.src;

public class WorldProvider {
    public float[] lightBrightnessTable;
    public World worldObj;

    public int getWorldType() { // new in 14w02a, added by MCPatcher in earlier versions
        return 0;
    }
}
