package com.prupe.mcpatcher.basemod.ext18;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.PatchComponent;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static javassist.bytecode.Opcode.*;

public class RenderPassEnumMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef SOLID = new FieldRef("RenderPassEnum", "SOLID", "LRenderPassEnum;");
    public static final FieldRef CUTOUT_MIPPED = new FieldRef("RenderPassEnum", "CUTOUT_MIPPED", "LRenderPassEnum;");
    public static final FieldRef CUTOUT = new FieldRef("RenderPassEnum", "CUTOUT", "LRenderPassEnum;");
    public static final FieldRef TRANSLUCENT = new FieldRef("RenderPassEnum", "TRANSLUCENT", "LRenderPassEnum;");
    public static final FieldRef valuesArray = new FieldRef("RenderPassEnum", "values", "[LRenderPassEnum;");
    public static final MethodRef values = new MethodRef("RenderPassEnum", "values", "()[LRenderPassEnum;");
    public static final MethodRef ordinal = new MethodRef("RenderPassEnum", "ordinal", "()I");

    public static boolean haveRenderPassEnum() {
        return Mod.getMinecraftVersion().compareTo("14w03a") >= 0;
    }

    public static String getDescriptor() {
        return haveRenderPassEnum() ? "LRenderPassEnum;" : "I";
    }

    public static int getLoadOpcode() {
        return haveRenderPassEnum() ? ALOAD : ILOAD;
    }

    public static int getStoreOpcode() {
        return haveRenderPassEnum() ? ASTORE : ISTORE;
    }

    public static String getOrdinalExpr() {
        return haveRenderPassEnum() ? anyReference(INVOKEVIRTUAL) : "";
    }

    public static Object getPassExpr(PatchComponent patchComponent, int pass) {
        return haveRenderPassEnum() ? anyReference(GETSTATIC) : patchComponent.push(pass);
    }

    public static Object getPassExpr(PatchComponent patchComponent, FieldRef pass, int passNum) {
        return haveRenderPassEnum() ? patchComponent.reference(GETSTATIC, pass) : patchComponent.push(passNum);
    }

    public static byte[] getPassByOrdinal(PatchComponent patchComponent, int pass) {
        if (haveRenderPassEnum()) {
            return patchComponent.buildCode(
                patchComponent.reference(INVOKESTATIC, values),
                patchComponent.push(pass),
                AALOAD
            );
        } else {
            return patchComponent.buildCode(
                patchComponent.push(pass)
            );
        }
    }

    public RenderPassEnumMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("SOLID"));
        addClassSignature(new ConstSignature("CUTOUT_MIPPED"));
        addClassSignature(new ConstSignature("CUTOUT"));
        addClassSignature(new ConstSignature("TRANSLUCENT"));

        addMemberMapper(new FieldMapper(SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
        );

        addMemberMapper(new FieldMapper(valuesArray)
                .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
