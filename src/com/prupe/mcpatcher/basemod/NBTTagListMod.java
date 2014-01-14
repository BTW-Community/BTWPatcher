package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;

import static javassist.bytecode.Opcode.*;

/**
 * Matches NBTTagList class.
 */
public class NBTTagListMod extends com.prupe.mcpatcher.ClassMod {
    public NBTTagListMod(Mod mod) {
        super(mod);
        setParentClass("NBTBase");

        final boolean haveTagAt = Mod.getMinecraftVersion().compareTo("13w36a") < 0;

        final FieldRef data = new FieldRef(getDeobfClass(), "data", "Ljava/util/List;");
        final MethodRef tagCount = new MethodRef(getDeobfClass(), "tagCount", "()I");
        final MethodRef removeTag = new MethodRef(getDeobfClass(), "removeTag", "(I)LNBTBase;");
        final MethodRef tagAt = new MethodRef(getDeobfClass(), "tagAt", "(I)LNBTBase;");
        final InterfaceMethodRef listSize = new InterfaceMethodRef("java/util/List", "size", "()I");
        final InterfaceMethodRef listRemove = new InterfaceMethodRef("java/util/List", "remove", "(I)Ljava/lang/Object;");
        final InterfaceMethodRef listGet = new InterfaceMethodRef("java/util/List", "get", "(I)Ljava/lang/Object;");

        if (haveTagAt) {
            addClassSignature(new ConstSignature(" entries of type "));
        } else {
            addClassSignature(new ConstSignature("["));
            addClassSignature(new ConstSignature("]"));
        }

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    reference(INVOKEINTERFACE, listSize)
                );
            }
        }.setMethod(tagCount));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    reference(INVOKEINTERFACE, listRemove)
                );
            }
        }.setMethod(removeTag));

        if (haveTagAt) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEINTERFACE, listGet)
                    );
                }
            }.setMethod(tagAt));
        } else {
            addMemberMapper(new FieldMapper(data));

            addPatch(new AddMethodPatch(tagAt) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, data),
                        ILOAD_1,
                        reference(INVOKEINTERFACE, listGet),
                        reference(CHECKCAST, new ClassRef("NBTBase")),
                        ARETURN
                    );
                }
            }.allowDuplicate(true));
        }
    }
}
