package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static javassist.bytecode.Opcode.INVOKEVIRTUAL;

public class RegistryMod extends com.prupe.mcpatcher.ClassMod {
    public RegistryMod(Mod mod) {
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
            new MethodRef(getDeobfClass(), "newMap", "Ljava/util/Map;"),
            new MethodRef(getDeobfClass(), "getValue", "(Ljava/lang/String;)Ljava/lang/Object;"),
            new MethodRef(getDeobfClass(), "getKey", "(Ljava/lang/Object;)Ljava/lang/String;"),
            new MethodRef(getDeobfClass(), "containsKey", "(Ljava/lang/String;)Z"),
            new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
            new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
            new MethodRef(getDeobfClass(), "iterator", "()Ljava/util/Iterator;"),
            Mod.getMinecraftVersion().compareTo("14w03b") >= 0 ? null : new MethodRef(getDeobfClass(), "containsId", "(I)Z"),
            getFullName
        ).setInterfaceOnly(false));
    }
}