package net.minecraft.src;

public class BiomeGenBase {
    public static BiomeGenBase[] biomeList;

    public int biomeID;
    public String biomeName;
    public int color;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;

    public float getTemperaturef() { // 1.2 - 1.6.2
        return 0.0f;
    }

    public float getTemperaturef(int i, int j, int k) { // 13w36a+
        return 0.0f;
    }

    public float getRainfallf() { // 1.2 and up
        return 0.0f;
    }
}
