package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Matches RenderBlocks class.
 */
public class RenderBlocksMod extends com.prupe.mcpatcher.ClassMod {
    protected final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;" + PositionMod.getDescriptor() + "FFF)Z");
    protected final FieldRef renderAllFaces = new FieldRef(getDeobfClass(), "renderAllFaces", "Z");
    protected final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
    protected final MethodRef shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")Z");

    protected final com.prupe.mcpatcher.BytecodeSignature grassTopSignature;
    protected int useColorRegister;

    public RenderBlocksMod(Mod mod) {
        super(mod);

        final MethodRef strEquals = new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z");

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push(0x0f000f)
                );
            }
        }.setMethod(renderStandardBlockWithAmbientOcclusion));

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, i, j - 1, k, 0))
                    // - or -
                    // if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, position.down(), Direction.DOWN)
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IFNE, any(2),
                    ALOAD_1,
                    ALOAD_0,
                    captureReference(GETFIELD),

                    PositionMod.havePositionClass() ?
                        build(
                            ALOAD_2,
                            anyReference(INVOKEVIRTUAL),
                            anyReference(GETSTATIC)
                        ) :
                        build(
                            ILOAD_2,
                            ILOAD_3,
                            push(1),
                            ISUB,
                            ILOAD, 4,
                            push(0)
                        ),

                    captureReference(INVOKEVIRTUAL),
                    IFEQ, any(2)
                );
            }
        }
            .setMethod(renderStandardBlockWithAmbientOcclusion)
            .addXref(1, renderAllFaces)
            .addXref(2, blockAccess)
            .addXref(3, shouldSideBeRendered)
        );

        addClassSignature(new ConstSignature(0.1875));
        addClassSignature(new ConstSignature(0.01));

        grassTopSignature = new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push("grass_top"),
                    reference(INVOKEVIRTUAL, strEquals),
                    IFEQ, any(2),
                    // useColor = false;
                    push(0),
                    capture(anyISTORE),
                    GOTO, any(2)
                );
            }

            @Override
            public boolean afterMatch() {
                useColorRegister = extractRegisterNum(getCaptureGroup(1));
                return true;
            }
        };
    }
}