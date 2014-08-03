package net.minecraft.src;

import java.util.Collection;

// 14w25a+
public interface IBlockStateProperty {
    String getName();

    Collection<Comparable> getValues();

    Class<? extends Comparable> getValueClass();

    String getValueString(Comparable value);
}
