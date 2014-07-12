package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

public class IBlockStatePropertyMod extends ClassMod {
    public IBlockStatePropertyMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            new InterfaceMethodRef("IBlockStateProperty", "getName", "()Ljava/lang/String;"),
            new InterfaceMethodRef("IBlockStateProperty", "getValues", "()Ljava/util/Collection;"),
            new InterfaceMethodRef("IBlockStateProperty", "getValueClass", "()Ljava/lang/Class;"),
            new InterfaceMethodRef("IBlockStateProperty", "getValueString", "(Ljava/lang/Comparable;)Ljava/lang/String;")
        ).setInterfaceOnly(true));
    }
}
