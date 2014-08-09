package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

public class BlockRegistryMod extends ClassMod {
    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") >= 0;
    }

    public BlockRegistryMod(Mod mod) {
        super(mod);
        setParentClass("Registry");
        addPrerequisiteClass("Registry");

        final MethodRef notNull = new MethodRef("org/apache/commons/lang3/Validate", "notNull", "(Ljava/lang/Object;)Ljava/lang/Object;");

        addClassSignature(new ConstSignature(notNull));
    }
}
