package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.PatchComponent;
import javassist.bytecode.AccessFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static com.prupe.mcpatcher.BytecodeMatcher.registerLoadStore;
import static javassist.bytecode.Opcode.*;

public class PositionMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef i = new FieldRef("Position", "i", "I");
    public static final FieldRef j = new FieldRef("Position", "j", "I");
    public static final FieldRef k = new FieldRef("Position", "k", "I");
    public static final MethodRef getI = new MethodRef("Position", "getI", "()I");
    public static final MethodRef getJ = new MethodRef("Position", "getJ", "()I");
    public static final MethodRef getK = new MethodRef("Position", "getK", "()I");

    public static boolean havePositionClass() {
        return Mod.getMinecraftVersion().compareTo("14w02a") >= 0;
    }

    public static String getDescriptor() {
        return havePositionClass() ? "LPosition;" : "III";
    }

    public static int getDescriptorLength() {
        return havePositionClass() ? 1 : 3;
    }

    public static String getDescriptorIKOnly() {
        return havePositionClass() ? "LPosition;" : "II";
    }

    public static int getDescriptorLengthIKOnly() {
        return havePositionClass() ? 1 : 2;
    }

    public static Object unpackArguments(PatchComponent patchComponent, int register) {
        if (havePositionClass()) {
            // position.getI(), position.getJ(), position.getK()
            return new Object[]{
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getI),
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getJ),
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getK)
            };
        } else {
            // i, j, k
            return passArguments(register);
        }
    }

    public static byte[] passArguments(int register) {
        if (havePositionClass()) {
            // position
            return registerLoadStore(ALOAD, register);
        } else {
            // i, j, k
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(registerLoadStore(ILOAD, register));
                output.write(registerLoadStore(ILOAD, register + 1));
                output.write(registerLoadStore(ILOAD, register + 2));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }
    }

    public PositionMod(Mod mod) {
        super(mod);

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // (this.j + this.k * 31) * 31 + this.i
                    ALOAD_0,
                    captureReference(GETFIELD),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    push(31),
                    IMUL,
                    IADD,
                    push(31),
                    IMUL,
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IADD
                );
            }
        }
            .setMethod(new MethodRef(getDeobfClass(), "hashCode", "()I"))
            .addXref(1, j)
            .addXref(2, k)
            .addXref(3, i)
        );

        addMemberMapper(new MethodMapper(null, getI, getJ, getK)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );
    }
}