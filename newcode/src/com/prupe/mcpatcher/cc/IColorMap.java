package com.prupe.mcpatcher.cc;

import net.minecraft.src.ResourceLocation;

import java.util.Collection;

interface IColorMap {
    boolean isHeightDependent();

    int getColorMultiplier();

    int getColorMultiplier(int i, int j, int k);

    float[] getColorMultiplierF(int i, int j, int k);

    void claimResources(Collection<ResourceLocation> resources);
}
