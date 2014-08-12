package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BinaryRegex.end;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

public class RenderUtilsMod extends ClassMod {
    public static final String CLASS_NAME = "RenderUtils";

    private static MethodRef translatef;
    private static MethodRef rotatef;
    private static MethodRef color3f;
    private static MethodRef color4f;
    private static MethodRef blendFunc;
    private static MethodRef alphaFunc;
    private static MethodRef depthFunc;
    private static MethodRef depthMask;
    private static MethodRef pushMatrix;
    private static MethodRef popMatrix;
    private static MethodRef callList;
    private static MethodRef bindTexture;
    private static MethodRef viewport;
    private static MethodRef clearColor;

    private static final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
    private static final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");
    private static final MethodRef glColor3f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor3f", "(FFF)V");
    private static final MethodRef glColor4f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V");
    private static final MethodRef glBlendFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBlendFunc", "(II)V");
    private static final MethodRef glAlphaFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glAlphaFunc", "(IF)V");
    private static final MethodRef glDepthFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthFunc", "(I)V");
    private static final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");
    private static final MethodRef glPushMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPushMatrix", "()V");
    private static final MethodRef glPopMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPopMatrix", "()V");
    private static final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
    private static final MethodRef glBindTexture = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V");
    private static final MethodRef glViewport = new MethodRef(MCPatcherUtils.GL11_CLASS, "glViewport", "(IIII)V");
    private static final MethodRef glClearColor = new MethodRef(MCPatcherUtils.GL11_CLASS, "glClearColor", "(FFFF)V");

    public static boolean haveClass() {
        return Mod.getMinecraftVersion().compareTo("14w25a") >= 0;
    }

    public static <T> T select(T v1, T v2) {
        return haveClass() ? v2 : v1;
    }

    public static boolean setup(Mod mod) {
        RenderUtilsMod renderUtilsMod = new RenderUtilsMod(mod);
        if (haveClass()) {
            mod.addClassMod(renderUtilsMod);
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

    public static byte[] glColor3f(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, color3f);
    }

    public static byte[] glBlendFunc(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, blendFunc);
    }

    public static byte[] glAlphaFunc(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, alphaFunc);
    }

    public static byte[] glDepthFunc(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, depthFunc);
    }

    public static byte[] glDepthMask(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, depthMask);
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

    public static byte[] glViewport(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, viewport);
    }

    public static byte[] glClearColor(PatchComponent patchComponent) {
        return patchComponent.reference(INVOKESTATIC, clearColor);
    }

    private RenderUtilsMod(Mod mod) {
        super(mod);

        translatef = simpleWrapper(glTranslatef);
        rotatef = simpleWrapper(glRotatef);
        color4f = simpleWrapper(glColor4f);
        blendFunc = simpleWrapper(glBlendFunc);
        alphaFunc = simpleWrapper(glAlphaFunc);
        depthFunc = simpleWrapper(glDepthFunc);
        depthMask = simpleWrapper(glDepthMask);
        pushMatrix = simpleWrapper(glPushMatrix);
        popMatrix = simpleWrapper(glPopMatrix);
        callList = simpleWrapper(glCallList);
        bindTexture = simpleWrapper(glBindTexture, new MethodRef(CLASS_NAME, "glBindTexture", "(I)V"));
        viewport = simpleWrapper(glViewport);
        clearColor = simpleWrapper(glClearColor);

        if (haveClass()) {
            color3f = new MethodRef(CLASS_NAME, glColor3f.getName(), glColor3f.getType());
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(color3f);
                    addXref(1, color4f);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // color3f(r, g, b, 1.0f);
                        begin(),
                        FLOAD_0,
                        FLOAD_1,
                        FLOAD_2,
                        push(1.0f),
                        captureReference(INVOKESTATIC),
                        RETURN,
                        end()
                    );
                }
            });

            MethodRef glBlendFuncSeparate = new MethodRef(CLASS_NAME, "glBlendFuncSeparate", "(IIII)V");
            addMemberMapper(new MethodMapper(glBlendFuncSeparate));
        } else {
            color3f = glColor3f;
        }
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
