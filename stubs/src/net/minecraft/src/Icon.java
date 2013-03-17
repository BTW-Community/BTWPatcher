package net.minecraft.src;

public interface Icon {
    int getOriginX();

    int getOriginY();

    float getMinU();

    float getMaxU();

    float getInterpolatedU(double u);

    float getMinV();

    float getMaxV();

    float getInterpolatedV(double v);

    String getIconName();

    int getSheetWidth();

    int getSheetHeight();
}
