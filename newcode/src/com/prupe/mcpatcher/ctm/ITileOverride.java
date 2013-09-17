package com.prupe.mcpatcher.ctm;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Icon;

import java.util.Set;

interface ITileOverride extends Comparable<ITileOverride> {
    boolean isDisabled();

    void registerIcons();

    Set<Block> getMatchingBlocks();

    Set<String> getMatchingTiles();

    int getRenderPass();

    int getWeight();

    Icon getTile(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face);

    Icon getTile(Block block, Icon origIcon, int face, int metadata);
}
