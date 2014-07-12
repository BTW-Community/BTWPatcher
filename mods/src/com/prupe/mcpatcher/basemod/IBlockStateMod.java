package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

public class IBlockStateMod extends ClassMod {
    public static boolean haveClass() {
        return !IconMod.haveClass();
    }

    public IBlockStateMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            new InterfaceMethodRef("IBlockState", "getProperties", "()Ljava/util/Collection;"),
            new InterfaceMethodRef("IBlockState", "getProperty", "(LIBlockStateProperty;)Ljava/lang/Comparable;"),
            new InterfaceMethodRef("IBlockState", "setProperty", "(LIBlockStateProperty;Ljava/lang/Comparable;)LIBlockState;"),
            new InterfaceMethodRef("IBlockState", "nextState", "(LIBlockStateProperty;)LIBlockState;"),
            new InterfaceMethodRef("IBlockState", "getPropertyMap", "()Lcom/google/common/collect/ImmutableMap;"),
            new InterfaceMethodRef("IBlockState", "getBlock", "()LBlock;")
        ).setInterfaceOnly(true));
    }
}
