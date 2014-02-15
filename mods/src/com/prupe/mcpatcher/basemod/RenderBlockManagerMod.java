package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
* Matches RenderBlockManager class (14w04a+).
*/
public class RenderBlockManagerMod extends ClassMod {
    public static final FieldRef instance = new FieldRef("RenderBlockManager", "instance", "LRenderBlockManager;");
    public final MethodRef renderBlockByRenderType = new MethodRef("RenderBlockManager", "renderBlockByRenderType", "(LBlock;" + PositionMod.getDescriptor() + (Mod.getMinecraftVersion().compareTo("14w06a") >= 0 ? "LIBlockAccess;" : "") + ")Z");
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
                        // x36 or more
                        ALOAD_0,
                        any(1, 3), // ICONST_*, BIPUSH x, SIPUSH x x
                        anyReference(NEW),
                        DUP,
                        anyReference(INVOKESPECIAL),
                        backReference(2)
                    ), 24)
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
                    anyISTORE,

                    // return renderType >= 0 ? ...;
                    anyILOAD,
                    or(
                        build(IFGE_or_IFLT, any(2)), // older
                        build(push(-1), IF_ICMPEQ_or_IF_ICMPNE, any(2)) // 14w06a+
                    )
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
                    // renderer.renderBlockAsItem(block, metadata, brightness);
                    ALOAD_1,
                    ILOAD_2,
                    FLOAD_3,
                    captureReference(INVOKEVIRTUAL)
                );
            }
        }
            .setMethod(renderBlockAsItem)
            .addXref(1, renderBlockAsItem1)
        );

        addMemberMapper(new FieldMapper(instance)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, true)
        );
    }

    public RenderBlockManagerMod mapRenderType(final int type, String className) {
        addClassSignature(new BytecodeSignature() {
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
        }
            .matchConstructorOnly(true)
            .addXref(1, new ClassRef(className))
        );
        return this;
    }
}
