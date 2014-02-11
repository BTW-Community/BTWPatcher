package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

/**
 * Matches Block class and maps blockID and blockList fields.
 */
public class BlockMod extends com.prupe.mcpatcher.ClassMod {
    protected final boolean haveBlockRegistry;

    public BlockMod(Mod mod) {
        super(mod);
        haveBlockRegistry = Mod.getMinecraftVersion().compareTo("13w36a") >= 0;

        final MethodRef getShortName = new MethodRef(getDeobfClass(), "getShortName", "()Ljava/lang/String;");

        if (haveBlockRegistry) {
            addClassSignature(new ConstSignature("stone"));
            addClassSignature(new ConstSignature("grass"));
            addClassSignature(new ConstSignature("dirt"));
            addClassSignature(new ConstSignature(".name"));
        } else {
            addClassSignature(new ConstSignature(" is already occupied by "));

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blockID", "I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blocksList", "[LBlock;"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
                .accessFlag(AccessFlag.FINAL, true)
            );
        }

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push("tile.")
                );
            }
        }.setMethod(getShortName));
    }
}
