package com.prupe.mcpatcher.ctm;

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

    Icon getTileWorld(BlockOrientation blockOrientation, Icon origIcon);

    Icon getTileHeld(BlockOrientation blockOrientation, Icon origIcon);
}
