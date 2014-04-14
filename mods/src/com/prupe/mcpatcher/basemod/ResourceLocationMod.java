package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static javassist.bytecode.Opcode.INVOKESTATIC;
import static javassist.bytecode.Opcode.INVOKEVIRTUAL;

/**
 * Matches ResourceLocation class.
 */
public class ResourceLocationMod extends com.prupe.mcpatcher.ClassMod {
    private static final MinecraftVersion MIN_VERSION = MinecraftVersion.parseVersion("13w24a");

    public static final MethodRef getNamespace = new MethodRef("ResourceLocation", "getNamespace", "()Ljava/lang/String;");
    public static final MethodRef getPath = new MethodRef("ResourceLocation", "getPath", "()Ljava/lang/String;");

    // for 1.5 compatibility
    public static final MethodRef wrap = new MethodRef(MCPatcherUtils.FAKE_RESOURCE_LOCATION_CLASS, "wrap", "(Ljava/lang/String;)LResourceLocation;");
    public static final MethodRef unwrap = new MethodRef(MCPatcherUtils.FAKE_RESOURCE_LOCATION_CLASS, "unwrap", "(LResourceLocation;)Ljava/lang/String;");

    private static final byte[] EMPTY = new byte[0];

    public static boolean setup(Mod mod) {
        if (haveClass()) {
            mod.addClassMod(new ResourceLocationMod(mod));
            return true;
        } else {
            mod.getClassMap().addClassMap("ResourceLocation", MCPatcherUtils.FAKE_RESOURCE_LOCATION_CLASS);
            return false;
        }
    }

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo(MIN_VERSION) >= 0;
    }

    public static String getDescriptor() {
        return haveClass() ? "LResourceLocation;" : "Ljava/lang/String;";
    }

    public static byte[] pack(PatchComponent patchComponent) {
        return haveClass() ? EMPTY : patchComponent.reference(INVOKESTATIC, wrap);
    }

    public static byte[] unpack(PatchComponent patchComponent) {
        return haveClass() ? EMPTY : patchComponent.reference(INVOKESTATIC, unwrap);
    }

    public static <T> T select(T v1, T v2) {
        return haveClass() ? v2 : v1;
    }

    private ResourceLocationMod(Mod mod) {
        super(mod);

        final MethodRef indexOf = new MethodRef("java/lang/String", "indexOf", "(I)I");

        addClassSignature(new ConstSignature("minecraft"));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push(58),
                    reference(INVOKEVIRTUAL, indexOf)
                );
            }
        });

        addMemberMapper(new MethodMapper(getPath, getNamespace)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );
    }
}
