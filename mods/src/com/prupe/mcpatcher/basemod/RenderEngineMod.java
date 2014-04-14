package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BinaryRegex.backReference;
import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BinaryRegex.end;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Maps RenderEngine class (1.5 only).
 */
public class RenderEngineMod extends ClassMod {
    public static final FieldRef textureMapBlocks = new FieldRef("RenderEngine", "textureMapBlocks", "LTextureMap;");
    public static final FieldRef textureMapItems = new FieldRef("RenderEngine", "textureMapItems", "LTextureMap;");
    public static final MethodRef updateDynamicTextures = new MethodRef("RenderEngine", "updateDynamicTextures", "()V");
    public static final MethodRef refreshTextureMaps = new MethodRef("RenderEngine", "refreshTextureMaps", "()V");
    public static final MethodRef refreshTextures = new MethodRef("RenderEngine", "refreshTextures", "()V");
    public static final MethodRef allocateAndSetupTexture = new MethodRef("RenderEngine", "allocateAndSetupTexture", "(Ljava/awt/image/BufferedImage;)I");
    public static final FieldRef imageData = new FieldRef("RenderEngine", "imageData", "Ljava/nio/IntBuffer;");

    private String updateAnimationsMapped;

    public RenderEngineMod(Mod mod) {
        super(mod);

        final MethodRef glTexSubImage2DByte = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
        final MethodRef glTexSubImage2DInt = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");

        addClassSignature(new ConstSignature("%clamp%"));
        addClassSignature(new ConstSignature("%blur%"));
        addClassSignature(new OrSignature(
            new ConstSignature(glTexSubImage2DByte),
            new ConstSignature(glTexSubImage2DInt)
        ));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push("%blur%")
                );
            }
        }.setMethod(refreshTextures));

        // updateAnimations and refreshTextureMaps are identical up to obfuscation:
        // public void xxx() {
        //   this.terrain.yyy();
        //   this.items.yyy();
        // }
        // They're even called from similar methods, runTick() and startGame() in Minecraft.java.
        // Normal descriptor and bytecode matching is insufficient here, so we rely on the fact
        // that updateAnimations is defined first.
        addClassSignature(new VoidSignature(updateDynamicTextures, "updateAnimations") {
            @Override
            public boolean afterMatch() {
                updateAnimationsMapped = getMethodInfo().getName();
                return true;
            }
        });

        addClassSignature(new VoidSignature(refreshTextureMaps, "refreshTextures") {
            @Override
            public boolean filterMethod() {
                return updateAnimationsMapped != null && getMethodInfo().getName().compareTo(updateAnimationsMapped) > 0;
            }
        });

        addMemberMapper(new FieldMapper(imageData));
        addMemberMapper(new MethodMapper(allocateAndSetupTexture));
    }

    private class VoidSignature extends BytecodeSignature {
        VoidSignature(MethodRef method, String textureMethod) {
            setMethod(method);
            addXref(1, textureMapBlocks);
            addXref(2, new MethodRef("TextureMap", textureMethod, "()V"));
            addXref(3, textureMapItems);
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                begin(),
                ALOAD_0,
                captureReference(GETFIELD),
                captureReference(INVOKEVIRTUAL),
                ALOAD_0,
                captureReference(GETFIELD),
                backReference(2),
                RETURN,
                end()
            );
        }
    }
}
