package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;

import static com.prupe.mcpatcher.BinaryRegex.backReference;
import static com.prupe.mcpatcher.BinaryRegex.build;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Matches WorldRenderer class.
 */
public class WorldRendererMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef posX = new FieldRef(getDeobfClass(), "posX", "I");
    protected final FieldRef posY = new FieldRef(getDeobfClass(), "posY", "I");
    protected final FieldRef posZ = new FieldRef(getDeobfClass(), "posZ", "I");
    protected final FieldRef[] pos = new FieldRef[]{posX, posY, posZ};
    protected final MethodRef updateRenderer;

    public WorldRendererMod(Mod mod) {
        super(mod);

        final MethodRef glNewList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glNewList", "(II)V");
        final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");

        updateRenderer = new MethodRef(getDeobfClass(), "updateRenderer",
            Mod.getMinecraftVersion().compareTo("13w41a") < 0 ? "()V" : "(LEntityLivingBase;)V"
        );

        addClassSignature(new ConstSignature(glNewList));
        addClassSignature(new ConstSignature(glTranslatef));
        addClassSignature(new ConstSignature(1.000001f));

        if (PositionMod.havePositionClass()) {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateRenderer);
                    addXref(1, posX);
                    addXref(2, posY);
                    addXref(3, posZ);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // position0 = new Position(this.posX, this.posY, this.posZ);
                        anyReference(NEW),
                        DUP,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        anyReference(INVOKESPECIAL),
                        anyASTORE,

                        // position1 = position0.offsetBy(16, 16, 16);
                        anyALOAD,
                        push(16),
                        push(16),
                        push(16),
                        anyReference(INVOKEVIRTUAL),
                        anyASTORE
                    );
                }
            });
        } else {
            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateRenderer);
                    for (int i = 0; i < pos.length; i++) {
                        addXref(i + 1, pos[i]);
                    }
                }

                @Override
                public String getMatchExpression() {
                    String exp0 = "";
                    String exp1 = "";
                    for (int i = 0; i < 3; i++) {
                        exp0 += build(
                            // i0/j0/k0 = this.posX/Y/Z;
                            ALOAD_0,
                            captureReference(GETFIELD),
                            anyISTORE
                        );
                        exp1 += build(
                            // i1/j1/k1 = this.posX/Y/Z + 16;
                            ALOAD_0,
                            backReference(i + 1),
                            push(16),
                            IADD,
                            anyISTORE
                        );
                    }
                    return buildExpression(
                        exp0,
                        exp1
                    );
                }
            });
        }
    }
}
