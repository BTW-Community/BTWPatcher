package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

public class PotionMod extends ClassMod {
    public static final FieldRef potionTypes = new FieldRef("Potion", "potionTypes", "[LPotion;");
    public static final MethodRef getName = new MethodRef("Potion", "getName", "()Ljava/lang/String;");

    public PotionMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("potion.moveSpeed"));
        addClassSignature(new ConstSignature("potion.moveSlowdown"));

        addMemberMapper(new FieldMapper(potionTypes)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
        );

        addMemberMapper(new MethodMapper(getName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
        );
    }
}
