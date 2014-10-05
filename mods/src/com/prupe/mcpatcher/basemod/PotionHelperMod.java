package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

public class PotionHelperMod extends ClassMod {
    public static final MethodRef getMundaneName = new MethodRef("PotionHelper", "getMundaneName", "(I)Ljava/lang/String;");

    public PotionHelperMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("potion.prefix.mundane"));
        addClassSignature(new ConstSignature("potion.prefix.uninteresting"));

        addMemberMapper(new MethodMapper(getMundaneName)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
