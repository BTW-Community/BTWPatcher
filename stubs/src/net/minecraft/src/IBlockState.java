package net.minecraft.src;

import java.util.Collection;

// 14w25a+
public interface IBlockState {
    Collection<IBlockStateProperty> getProperties();

    Comparable getProperty(IBlockStateProperty property);

    IBlockState setProperty(IBlockStateProperty property, Comparable value);

    IBlockState nextState(IBlockStateProperty property);

    // ImmutableMap getPropertyMap();

    Block getBlock();
}
