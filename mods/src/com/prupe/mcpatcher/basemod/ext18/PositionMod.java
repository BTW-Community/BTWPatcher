package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class PositionMod extends com.prupe.mcpatcher.ClassMod {
    private static final MinecraftVersion MIN_VERSION = MinecraftVersion.parseVersion("14w02a");
    private static final MinecraftVersion MIN_VERSION_SUBCLASS = MinecraftVersion.parseVersion("14w04a");

    private static MethodRef getI = new MethodRef("Position", "getI", "()I");
    private static MethodRef getJ = new MethodRef("Position", "getJ", "()I");
    private static MethodRef getK = new MethodRef("Position", "getK", "()I");

    public static boolean setup(Mod mod) {
        if (havePositionClass()) {
            mod.addClassMod(new PositionMod(mod));
            if (Mod.getMinecraftVersion().compareTo(MIN_VERSION_SUBCLASS) >= 0) {
                mod.addClassMod(new PositionBaseMod(mod));
            }
            mod.addClassMod(new DirectionMod(mod));
            return true;
        } else {
            return false;
        }
    }

    public static boolean havePositionClass() {
        return Mod.getMinecraftVersion().compareTo(MIN_VERSION) >= 0;
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

    protected PositionMod(Mod mod) {
        super(mod);

        if (Mod.getMinecraftVersion().compareTo(MIN_VERSION_SUBCLASS) >= 0) {
            setParentClass("PositionBase");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // h = 64 - f - g;
                        push(64),
                        anyReference(GETSTATIC),
                        ISUB,
                        anyReference(GETSTATIC),
                        ISUB,
                        anyReference(PUTSTATIC)
                    );
                }
            }.matchStaticInitializerOnly(true));
        } else {
            addBaseSignatures(this);
        }
    }

    private static class PositionBaseMod extends com.prupe.mcpatcher.ClassMod {
        public PositionBaseMod(Mod mod) {
            super(mod);

            addBaseSignatures(this);
        }
    }

    private static void addBaseSignatures(ClassMod classMod) {
        String deobfClass = classMod.getDeobfClass();
        final MethodRef hashCode = new MethodRef(deobfClass, "hashCode", "()I");
        final boolean useGetters = Mod.getMinecraftVersion().compareTo("14w25a") >= 0;

        final FieldRef i = new FieldRef(deobfClass, "i", "I");
        final FieldRef j = new FieldRef(deobfClass, "j", "I");
        final FieldRef k = new FieldRef(deobfClass, "k", "I");
        getI = new MethodRef(deobfClass, "getI", "()I");
        getJ = new MethodRef(deobfClass, "getJ", "()I");
        getK = new MethodRef(deobfClass, "getK", "()I");

        classMod.addClassSignature(new com.prupe.mcpatcher.BytecodeSignature(classMod) {
            private final int opcode;

            {
                setMethod(hashCode);
                if (useGetters) {
                    addXref(1, getJ);
                    addXref(2, getK);
                    addXref(3, getI);
                    opcode = INVOKEVIRTUAL;
                } else {
                    addXref(1, j);
                    addXref(2, k);
                    addXref(3, i);
                    opcode = GETFIELD;
                }
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // (this.j + this.k * 31) * 31 + this.i
                    // -or-
                    // (this.getJ() + this.getK() * 31) * 31 + this.getI()
                    ALOAD_0,
                    captureReference(opcode),
                    ALOAD_0,
                    captureReference(opcode),
                    push(31),
                    IMUL,
                    IADD,
                    push(31),
                    IMUL,
                    ALOAD_0,
                    captureReference(opcode),
                    IADD
                );
            }
        });

        if (!useGetters) {
            classMod.addMemberMapper(new com.prupe.mcpatcher.MethodMapper(classMod, null, getI, getJ, getK)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
            );
        }
    }
}