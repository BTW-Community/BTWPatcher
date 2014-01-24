package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.PatchComponent;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static com.prupe.mcpatcher.BytecodeMatcher.registerLoadStore;
import static javassist.bytecode.Opcode.*;

public class DirectionMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef id = new FieldRef("Direction", "id", "I");
    public static final MethodRef getID = new MethodRef("Direction", "getID", "()I");

    public static final FieldRef DOWN = new FieldRef("Direction", "DOWN", "LDirection;");
    public static final FieldRef UP = new FieldRef("Direction", "UP", "LDirection;");
    public static final FieldRef NORTH = new FieldRef("Direction", "NORTH", "LDirection;");
    public static final FieldRef SOUTH = new FieldRef("Direction", "SOUTH", "LDirection;");
    public static final FieldRef WEST = new FieldRef("Direction", "WEST", "LDirection;");
    public static final FieldRef EAST = new FieldRef("Direction", "EAST", "LDirection;");

    public static final FieldRef ALL = new FieldRef("Direction", "ALL", "[LDirection;");
    public static final FieldRef SIDES = new FieldRef("Direction", "SIDES", "[LDirection;");

    private static final FieldRef[] ALL_FIELDS = new FieldRef[]{DOWN, UP, NORTH, SOUTH, WEST, EAST};

    public static boolean haveDirectionClass() {
        return PositionMod.havePositionClass();
    }

    public static String getDescriptor() {
        return haveDirectionClass() ? "LDirection;" : "I";
    }

    public static Object unpackArguments(PatchComponent patchComponent, int register) {
        if (haveDirectionClass()) {
            // direction.getID()
            return new Object[]{
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getID),
            };
        } else {
            // direction
            return registerLoadStore(ILOAD, register);
        }
    }

    public static byte[] passArguments(int register) {
        if (haveDirectionClass()) {
            return registerLoadStore(ALOAD, register);
        } else {
            return registerLoadStore(ILOAD, register);
        }
    }

    public static int getStoreOpcode() {
        return haveDirectionClass() ? ASTORE : ISTORE;
    }

    public static Object getFixedDirection(PatchComponent patchComponent, int direction) {
        if (haveDirectionClass()) {
            return patchComponent.reference(GETSTATIC, ALL_FIELDS[direction]);
        } else {
            return patchComponent.push(direction);
        }
    }

    public static Object getFixedDirection(PatchComponent patchComponent, FieldRef direction) {
        for (int i = 0; i < ALL_FIELDS.length; i++) {
            if (ALL_FIELDS[i].equals(direction)) {
                return getFixedDirection(patchComponent, i);
            }
        }
        return null;
    }

    protected DirectionMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("DOWN"));
        addClassSignature(new ConstSignature("UP"));
        addClassSignature(new ConstSignature("NORTH"));
        addClassSignature(new ConstSignature("SOUTH"));
        addClassSignature(new ConstSignature("WEST"));
        addClassSignature(new ConstSignature("EAST"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.id = id;
                    ALOAD_0,
                    ILOAD_3,
                    captureReference(PUTFIELD)
                );
            }
        }
            .matchConstructorOnly(true)
            .addXref(1, id)
        );

        addMemberMapper(new MethodMapper(getID)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );

        addMemberMapper(new FieldMapper(ALL_FIELDS)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, true)
        );

        addMemberMapper(new FieldMapper(ALL, SIDES)
            .accessFlag(AccessFlag.PRIVATE, true)
            .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
