package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

public class IBlockStateMod extends ClassMod {
    public static final InterfaceMethodRef getProperties = new InterfaceMethodRef("IBlockState", "getProperties", "()Ljava/util/Collection;");
    public static final InterfaceMethodRef getProperty = new InterfaceMethodRef("IBlockState", "getProperty", "(LIBlockStateProperty;)Ljava/lang/Comparable;");
    public static final InterfaceMethodRef setProperty = new InterfaceMethodRef("IBlockState", "setProperty", "(LIBlockStateProperty;Ljava/lang/Comparable;)LIBlockState;");
    public static final InterfaceMethodRef nextState = new InterfaceMethodRef("IBlockState", "nextState", "(LIBlockStateProperty;)LIBlockState;");
    public static final InterfaceMethodRef getPropertyMap = new InterfaceMethodRef("IBlockState", "getPropertyMap", "()Lcom/google/common/collect/ImmutableMap;");
    public static final InterfaceMethodRef getBlock = new InterfaceMethodRef("IBlockState", "getBlock", "()LBlock;");

    public static boolean haveClass() {
        return !IconMod.haveClass();
    }

    public IBlockStateMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            getProperties,
            getProperty,
            setProperty,
            nextState,
            getPropertyMap,
            getBlock
        ).setInterfaceOnly(true));
    }
}
