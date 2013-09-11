package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

class Shared {
    static class RegistryBaseMod extends com.prupe.mcpatcher.ClassMod {
        RegistryBaseMod(Mod mod) {
            super(mod);

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "newHashMap", "()Ljava/util/HashMap;"),
                new MethodRef(getDeobfClass(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getKeys", "()Ljava/util/Set;")
            ).setInterfaceOnly(false));
        }
    }

    static class RegistryMod extends com.prupe.mcpatcher.ClassMod {
        RegistryMod(Mod mod) {
            super(mod);
            setParentClass("RegistryBase");

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "register", "(ILjava/lang/String;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
                new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "getAll", "()Ljava/util/List;")
            ).setInterfaceOnly(false));
        }
    }
}
