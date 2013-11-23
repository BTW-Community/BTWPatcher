package net.minecraft.src;

public class BiomeGenBase {
    public static BiomeGenBase[] biomeList;

    public int biomeID;
    public String biomeName;
    public int color;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;

    // 1.2 - 1.6.2
    public float getTemperaturef() {
        return 0.0f;
    }

    public int getGrassColor() {
        return 0;
    }

    public int getFoliageColor() {
        return 0;
    }

    // 13w36a+
    public float getTemperaturef(int i, int j, int k) {
        return 0.0f;
    }

    public int getGrassColor(int i, int j, int k) {
        return 0;
    }

    public int getFoliageColor(int i, int j, int k) {
        return 0;
    }

    public float getRainfallf() { // 1.2 and up
        return 0.0f;
    }
}
