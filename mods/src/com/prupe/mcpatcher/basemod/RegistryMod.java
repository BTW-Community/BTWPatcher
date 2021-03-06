package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.ext18.IBlockStateMod;

import static javassist.bytecode.Opcode.INVOKEVIRTUAL;

public class RegistryMod extends com.prupe.mcpatcher.ClassMod {
    public RegistryMod(Mod mod) {
        super(mod);
        setParentClass("RegistryBase");

        final InterfaceMethodRef biMapInverse = new InterfaceMethodRef("com.google.common.collect.BiMap", "inverse", "()Lcom/google/common/collect/BiMap;");
        final MethodRef indexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");
        final MethodRef getFullName;

        if (IBlockStateMod.haveClass()) {
            addClassSignature(new ConstSignature(biMapInverse));
            getFullName = null;
        } else {
            getFullName = new MethodRef(getDeobfClass(), "getFullName", "(Ljava/lang/String;)Ljava/lang/String;");
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
        }

        String keyType = IBlockStateMod.haveClass() ? "Ljava/lang/Object;" : "Ljava/lang/String;";
        String methodSuffix = IBlockStateMod.haveClass() ? "Object" : "";
        addClassSignature(new InterfaceSignature(
            new MethodRef(getDeobfClass(), "<init>", "()V"),
            new MethodRef(getDeobfClass(), "register" + methodSuffix, "(I" + keyType + "Ljava/lang/Object;)V"),
            new MethodRef(getDeobfClass(), "newMap", "()Ljava/util/Map;"),
            new MethodRef(getDeobfClass(), "getValue" + methodSuffix, "(" + keyType + ")Ljava/lang/Object;"),
            new MethodRef(getDeobfClass(), "getKey" + methodSuffix, "(Ljava/lang/Object;)" + keyType),
            new MethodRef(getDeobfClass(), "containsKey" + methodSuffix, "(" + keyType + ")Z"),
            new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
            new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
            new MethodRef(getDeobfClass(), "iterator", "()Ljava/util/Iterator;"),
            Mod.getMinecraftVersion().compareTo("14w03b") >= 0 ? null : new MethodRef(getDeobfClass(), "containsId", "(I)Z"),
            getFullName
        ).setInterfaceOnly(false));
    }
}
