package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.mal.block.RenderBlockState;
import net.minecraft.src.Block;
import net.minecraft.src.Icon;

import java.util.Set;

interface ITileOverride extends Comparable<ITileOverride> {
    boolean isDisabled();

    void registerIcons();

    Set<Block> getMatchingBlocks();

    Set<String> getMatchingTiles();

    int getRenderPass();

    int getWeight();

    Icon getTileWorld(RenderBlockState renderBlockState, Icon origIcon);

    Icon getTileHeld(RenderBlockState renderBlockState, Icon origIcon);
}
