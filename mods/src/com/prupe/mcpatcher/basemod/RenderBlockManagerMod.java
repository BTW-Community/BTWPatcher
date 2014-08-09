package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.ClassMod;
import com.prupe.mcpatcher.ClassRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Matches RenderBlockManager class (14w04a+).
 */
public class RenderBlockManagerMod extends ClassMod {
    public static final MethodRef registerRenderType = new MethodRef("RenderBlockManager", "registerRenderType", "(ILRenderBlocks;)V");
    public static MethodRef renderBlockAsItem;

    public RenderBlockManagerMod(Mod mod) {
        super(mod);

        final ClassRef renderBlocksClass = new ClassRef("RenderBlocks");
        final MethodRef renderBlockByRenderType = new MethodRef("RenderBlockManager", "renderBlockByRenderType", "(LBlock;" + PositionMod.getDescriptor() + (Mod.getMinecraftVersion().compareTo("14w06a") >= 0 ? "LIBlockAccess;" : "") + ")Z");
        renderBlockAsItem = new MethodRef("RenderBlockManager", "renderBlockAsItem", Mod.getMinecraftVersion().compareTo("14w32a") >= 0 ? "(LIBlockState;F)V" : "(LBlock;IF)V");

        if (RenderBlockCustomMod.haveCustomModels()) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (renderPass == 43)
                        anyILOAD,
                        push(43),
                        IF_ICMPEQ_or_IF_ICMPNE
                    );
                }
            });
        }

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, renderBlocksClass);
                addXref(2, registerRenderType);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.registerRenderType(0, new RenderBlocks());
                    ALOAD_0,
                    push(0),
                    captureReference(NEW),
                    DUP,
                    anyReference(INVOKESPECIAL),
                    captureReference(INVOKEVIRTUAL),

                    repeat(build(
                        // this.registerRenderType(renderType, new RenderBlocksSubclass());
                        // x10 or more
                        ALOAD_0,
                        any(1, 3), // ICONST_*, BIPUSH x, SIPUSH x x
                        anyReference(NEW),
                        DUP,
                        anyReference(INVOKESPECIAL),
                        backReference(2)
                    ), 10)
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(renderBlockByRenderType);
                addXref(1, BlockMod.getRenderType);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // renderType = block.getRenderType();
                    begin(),
                    ALOAD_1,
                    captureReference(INVOKEVIRTUAL),
                    anyISTORE,

                    // return renderType >= 0 ? ...;
                    anyILOAD,
                    or(
                        build(IFGE_or_IFLT, any(2)), // older
                        build(push(-1), IF_ICMPEQ_or_IF_ICMPNE, any(2)) // 14w06a+
                    )
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(renderBlockAsItem);
                addXref(1, RenderBlocksMod.renderBlockAsItem);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // renderer.renderBlockAsItem(block, metadata, brightness);
                    ALOAD_1,
                    ILOAD_2,
                    FLOAD_3,
                    captureReference(INVOKEVIRTUAL)
                );
            }
        });
    }

    public RenderBlockManagerMod mapRenderType(final int type, final String className) {
        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, new ClassRef(className));
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.registerRenderType(renderType, new RenderBlocksSubclass());
                    ALOAD_0,
                    push(type),
                    captureReference(NEW),
                    DUP,
                    anyReference(INVOKESPECIAL),
                    anyReference(INVOKEVIRTUAL)
                );
            }
        });
        return this;
    }
}
