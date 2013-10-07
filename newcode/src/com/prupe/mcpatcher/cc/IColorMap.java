package com.prupe.mcpatcher.cc;

interface IColorMap {
    boolean isHeightDependent();

    int getColorMultiplier();

    int getColorMultiplier(int i, int j, int k);

    float[] getColorMultiplierF(int i, int j, int k);
}
