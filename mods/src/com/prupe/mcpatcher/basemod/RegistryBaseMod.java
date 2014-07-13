package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

public class RegistryBaseMod extends com.prupe.mcpatcher.ClassMod {
    public RegistryBaseMod(Mod mod) {
        super(mod);

        addClassSignature(new InterfaceSignature(
            new MethodRef(getDeobfClass(), "<init>", "()V"),
            new MethodRef(getDeobfClass(), "newMap", "()Ljava/util/Map;"),
            new MethodRef(getDeobfClass(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
            new MethodRef(getDeobfClass(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
            new MethodRef(getDeobfClass(), "getKeys", "()Ljava/util/Set;"),
            new MethodRef(getDeobfClass(), "containsKey", "(Ljava/lang/Object;)Z"),
            IconMod.haveClass() ? null : new MethodRef(getDeobfClass(), "iterator", "()Ljava/util/Iterator;")
        ).setInterfaceOnly(false));
    }
}
