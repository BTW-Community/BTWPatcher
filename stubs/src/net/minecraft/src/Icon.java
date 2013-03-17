package net.minecraft.src;

public interface Icon {
    int getOriginX();

    int getOriginY();

    float getMinU();

    float getMaxU();

    float getInterpolatedU(double var1);

    float getMinV();

    float getMaxV();

    float getInterpolatedV(double var1);

    String getIconName();

    int getSheetWidth();

    int getSheetHeight();
}
