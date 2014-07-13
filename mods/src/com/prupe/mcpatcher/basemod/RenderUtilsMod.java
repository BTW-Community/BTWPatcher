package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;

import static javassist.bytecode.Opcode.*;

public class RenderUtilsMod extends ClassMod {
    public static final String CLASS_NAME = "RenderUtils";

    private static MethodRef translatef;
    private static MethodRef rotatef;
    private static MethodRef color4f;
    private static MethodRef blendFunc;
    private static MethodRef pushMatrix;
    private static MethodRef popMatrix;
    private static MethodRef callList;
    private static MethodRef bindTexture;

    private static final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
    private static final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");
    private static final MethodRef glColor4f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V");
    private static final MethodRef glBlendFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBlendFunc", "(II)V");
    private static final MethodRef glPushMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPushMatrix", "()V");
    private static final MethodRef glPopMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPopMatrix", "()V");
    private static final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
    private static final MethodRef glBindTexture = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V");

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") >= 0;
    }

    public static <T> T select(T v1, T v2) {
        return haveClass() ? v2 : v1;
    }

    public static boolean setup(Mod mod) {
        if (haveClass()) {
            mod.addClassMod(new RenderUtilsMod(mod));
            return true;
        } else {
            return false;
        }
    }

    public static boolean setup(ClassMod classMod) {
        if (haveClass()) {
            classMod.addPrerequisiteClass(CLASS_NAME);
            return true;
        } else {
            return false;
        }
    }

    public static byte[] glTranslatef(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, translatef);
    }

    public static byte[] glRotatef(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, rotatef);
    }

    public static byte[] glColor4f(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, color4f);
    }

    public static byte[] glBlendFunc(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, blendFunc);
    }

    public static byte[] glPushMatrix(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, pushMatrix);
    }

    public static byte[] glPopMatrix(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, popMatrix);
    }

    public static byte[] glCallList(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, callList);
    }

    public static byte[] glBindTexture(PatchComponent patchComponent, byte[] args) {
        if (haveClass()) {
            return patchComponent.buildCode(
                args,
                patchComponent.reference(INVOKESTATIC, bindTexture)
            );
        } else {
            return patchComponent.buildCode(
                patchComponent.push(3553),
                args,
                patchComponent.reference(INVOKESTATIC, bindTexture)
            );
        }
    }

    private RenderUtilsMod(Mod mod) {
        super(mod);

        translatef = simpleWrapper(glTranslatef);
        rotatef = simpleWrapper(glRotatef);
        color4f = simpleWrapper(glColor4f);
        blendFunc = simpleWrapper(glBlendFunc);
        pushMatrix = simpleWrapper(glPushMatrix);
        popMatrix = simpleWrapper(glPopMatrix);
        callList = simpleWrapper(glCallList);
        bindTexture = simpleWrapper(glBindTexture, new MethodRef(CLASS_NAME, "glBindTexture", "(I)V"));
    }

    private MethodRef simpleWrapper(final MethodRef glMethod) {
        MethodRef wrapperMethod = new MethodRef(CLASS_NAME, glMethod.getName(), glMethod.getType());
        return simpleWrapper(glMethod, wrapperMethod);
    }

    private MethodRef simpleWrapper(final MethodRef glMethod, final MethodRef wrapperMethod) {
        if (haveClass()) {

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(wrapperMethod);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKESTATIC, glMethod)
                    );
                }
            });

            return wrapperMethod;
        } else {
            return glMethod;
        }
    }
}
