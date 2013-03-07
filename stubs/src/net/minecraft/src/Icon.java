package net.minecraft.src;

public interface Icon {
    int getX0();

    int getY0();

    float getNormalizedX0();

    float getNormalizedX1();

    float interpolateX(double var1);

    float getNormalizedY0();

    float getNormalizedY1();

    float interpolateY(double var1);

    String getName();

    int getTextureWidth();

    int getTextureHeight();
}
