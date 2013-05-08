package net.minecraft.src;

public interface Icon {
    int getWidth();

    int getHeight();

    float getMinU();

    float getMaxU();

    float getInterpolatedU(double u);

    float getMinV();

    float getMaxV();

    float getInterpolatedV(double v);

    String getIconName();
}
