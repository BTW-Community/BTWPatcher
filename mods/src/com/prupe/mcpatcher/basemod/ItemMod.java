package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

/**
 * Matches Item class.
 */
public class ItemMod extends com.prupe.mcpatcher.ClassMod {
    protected final MethodRef getItemName = new MethodRef(getDeobfClass(), "getItemName", "()Ljava/lang/String;");

    public static boolean haveItemRegistry() {
        return BlockMod.haveBlockRegistry();
    }

    public ItemMod(Mod mod) {
        super(mod);

        if (haveItemRegistry()) {
            addClassSignature(new ConstSignature("iron_shovel"));
            addClassSignature(new ConstSignature("iron_pickaxe"));
            addClassSignature(new ConstSignature("iron_axe"));
            addClassSignature(new ConstSignature(".name"));
        } else {
            addClassSignature(new ConstSignature("CONFLICT @ "));
            addClassSignature(new ConstSignature("coal"));
        }

        addMemberMapper(new MethodMapper(getItemName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );
    }
}
