package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.IFGE_or_IFLT;
import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
* Matches RenderBlockManager class (14w04a+).
*/
public class RenderBlockManagerMod extends ClassMod {
    public static final FieldRef instance = new FieldRef("RenderBlockManager", "instance", "LRenderBlockManager;");
    public final MethodRef renderBlockByRenderType = new MethodRef("RenderBlockManager", "renderBlockByRenderType", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
    public static final MethodRef registerRenderType = new MethodRef("RenderBlockManager", "registerRenderType", "(ILRenderBlocks;)V");
    public static final MethodRef renderBlockAsItem = new MethodRef("RenderBlockManager", "renderBlockAsItem", "(LBlock;IF)V");

    public RenderBlockManagerMod(Mod mod) {
        super(mod);

        final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");
        final ClassRef renderBlocksClass = new ClassRef("RenderBlocks");
        final MethodRef renderBlockAsItem1 = new MethodRef("RenderBlocks", "renderBlockAsItem", "(LBlock;IF)V");
        final InterfaceMethodRef listGet = new InterfaceMethodRef("java/util/List", "get", "(I)Ljava/lang/Object;");

        addClassSignature(new BytecodeSignature() {
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
                        // x40 or more
                        ALOAD_0,
                        any(1, 3), // ICONST_*, BIPUSH x, SIPUSH x x
                        anyReference(NEW),
                        DUP,
                        anyReference(INVOKESPECIAL),
                        backReference(2)
                    ), 40)
                );
            }
        }
            .matchConstructorOnly(true)
            .addXref(1, renderBlocksClass)
            .addXref(2, registerRenderType)
        );

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // renderType = block.getRenderType();
                    begin(),
                    ALOAD_1,
                    captureReference(INVOKEVIRTUAL),
                    ISTORE_3,

                    // return renderType >= 0 ? ...;
                    ILOAD_3,
                    IFGE_or_IFLT, any(2)
                );
            }
        }
            .setMethod(renderBlockByRenderType)
            .addXref(1, getRenderType)
        );

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // this.renderers.get(renderType).renderBlockAsItem(block, metadata, brightness);
                    ALOAD_0,
                    anyReference(GETFIELD),
                    ILOAD, 4,
                    reference(INVOKEINTERFACE, listGet),
                    captureReference(CHECKCAST),

                    ALOAD_1,
                    ILOAD_2,
                    FLOAD_3,
                    captureReference(INVOKEVIRTUAL)
                );
            }
        }
            .setMethod(renderBlockAsItem)
            .addXref(1, renderBlocksClass)
            .addXref(2, renderBlockAsItem1)
        );

        addMemberMapper(new FieldMapper(instance)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
