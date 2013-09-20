package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static javassist.bytecode.Opcode.*;

class Shared {
    static class RegistryBaseMod extends com.prupe.mcpatcher.ClassMod {
        RegistryBaseMod(Mod mod) {
            super(mod);

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "newHashMap", "()Ljava/util/HashMap;"),
                new MethodRef(getDeobfClass(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getKeys", "()Ljava/util/Set;"),
                new MethodRef(getDeobfClass(), "containsKey", "(Ljava/lang/Object;)Z")
            ).setInterfaceOnly(false));
        }
    }

    static class RegistryMod extends com.prupe.mcpatcher.ClassMod {
        RegistryMod(Mod mod) {
            super(mod);
            setParentClass("RegistryBase");

            final MethodRef getFullName = new MethodRef(getDeobfClass(), "getFullName", "(Ljava/lang/String;)Ljava/lang/String;");
            final MethodRef indexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");

            addClassSignature(new ConstSignature("minecraft:"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(58),
                        reference(INVOKEVIRTUAL, indexOf)
                    );
                }
            }.setMethod(getFullName));

            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "register", "(ILjava/lang/String;Ljava/lang/Object;)V"),
                getFullName,
                new MethodRef(getDeobfClass(), "get", "(Ljava/lang/String;)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "containsKey", "(Ljava/lang/String;)Z"),
                new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
                new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "iterator", "()Ljava/util/Iterator;")
            ).setInterfaceOnly(false));
        }
    }
}
