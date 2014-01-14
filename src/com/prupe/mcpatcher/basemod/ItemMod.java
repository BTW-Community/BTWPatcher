package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

/**
 * Matches Item class.
 */
public class ItemMod extends com.prupe.mcpatcher.ClassMod {
    protected final boolean haveItemRegistry;

    protected final MethodRef getItemName = new MethodRef(getDeobfClass(), "getItemName", "()Ljava/lang/String;");

    public ItemMod(Mod mod) {
        super(mod);
        haveItemRegistry = Mod.getMinecraftVersion().compareTo("13w36a") >= 0;

        if (haveItemRegistry) {
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
