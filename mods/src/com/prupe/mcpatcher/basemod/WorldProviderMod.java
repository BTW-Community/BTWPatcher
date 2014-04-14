package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BytecodeMatcher.anyFLOAD;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

public class WorldProviderMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef worldType = new FieldRef("WorldProvider", "worldType", "I");
    public static final MethodRef getWorldType = new MethodRef("WorldProvider", "getWorldType", "()I");

    public static boolean haveGetWorldType() {
        return Mod.getMinecraftVersion().compareTo("14w02a") >= 0;
    }

    public static int getWorldTypeOpcode() {
        return haveGetWorldType() ? INVOKEVIRTUAL : GETFIELD;
    }

    public static JavaRef getWorldTypeRef() {
        return haveGetWorldType() ? getWorldType : worldType;
    }

    public WorldProviderMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature(24000.0f));
        addClassSignature(new ConstSignature(0.5f));
        addClassSignature(new ConstSignature(0.25f));
        addClassSignature(new ConstSignature(15.0f));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // f * 3.0f + 1.0f
                    anyFLOAD,
                    push(3.0f),
                    FMUL,
                    push(1.0f),
                    FADD
                );
            }
        }.setMethod(new MethodRef(getDeobfClass(), "generateLightBrightnessTable", "()V")));

        addClassSignature(new ClassSignature() {
            @Override
            public boolean match(String filename, ClassFile classFile, ClassMap tempClassMap) {
                return classFile.isAbstract() && !classFile.isInterface();
            }
        });

        if (haveGetWorldType()) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return this.worldType;
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IRETURN
                    );
                }
            }
                .setMethod(getWorldType)
                .addXref(1, worldType)
            );
        } else {
            addMemberMapper(new FieldMapper(worldType)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            addPatch(new AddMethodPatch(getWorldType) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        // return this.worldType;
                        ALOAD_0,
                        reference(GETFIELD, worldType),
                        IRETURN
                    );
                }
            }.allowDuplicate(true));
        }
    }
}
