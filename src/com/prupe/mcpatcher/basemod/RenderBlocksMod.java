package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.MinecraftVersion;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

/**
 * Matches RenderBlocks class.
 */
public class RenderBlocksMod extends com.prupe.mcpatcher.ClassMod {
    private static final MinecraftVersion MIN_VERSION_SUBCLASS = MinecraftVersion.parseVersion("14w04a");

    protected final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;" + PositionMod.getDescriptor() + "FFF)Z");
    protected final FieldRef renderAllFaces = new FieldRef(getDeobfClass(), "renderAllFaces", "Z");
    protected final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
    protected final MethodRef getRenderType = new MethodRef("Block", "getRenderType", "()I");
    protected final MethodRef shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")Z");
    protected final MethodRef renderBlockByRenderType = new MethodRef(getDeobfClass(), "renderBlockByRenderType", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
    protected final MethodRef renderStandardBlock = new MethodRef(getDeobfClass(), "renderStandardBlock", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
    protected final MethodRef renderBlock = new MethodRef(getDeobfClass(), "renderBlock", "(LBlock;" + PositionMod.getDescriptor() + ")Z");
    protected final MethodRef isAmbientOcclusionEnabled = new MethodRef("Minecraft", "isAmbientOcclusionEnabled", "()Z");
    protected final FieldRef lightValue = new FieldRef("Block", "lightValue", "[I");
    protected final MethodRef getLightValue = new MethodRef("Block", "getLightValue", "()I");
    protected final MethodRef hasOverrideBlockTexture = new MethodRef(getDeobfClass(), "hasOverrideBlockTexture", "()Z");
    protected final FieldRef overrideBlockTexture = new FieldRef(getDeobfClass(), "overrideBlockTexture", "LIcon;");

    protected final com.prupe.mcpatcher.BytecodeSignature grassTopSignature;
    protected int useColorRegister;

    public static boolean haveSubclasses() {
        return Mod.getMinecraftVersion().compareTo(MIN_VERSION_SUBCLASS) >= 0;
    }

    public RenderBlocksMod(Mod mod) {
        super(mod);

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

        grassTopSignature = new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // if (this.hasOverrideTexture()) {
                    ALOAD_0,
                    anyReference(INVOKEVIRTUAL),
                    IFEQ, any(2),

                    // useColor = false;
                    push(0),
                    capture(anyISTORE)

                    // }
                );
            }

            @Override
            public boolean afterMatch() {
                useColorRegister = extractRegisterNum(getCaptureGroup(1));
                return true;
            }
        };
    }

    public RenderBlocksMod mapRenderStandardBlock() {
        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // Minecraft.isAmbientOcclusionEnabled() && ... == 0
                    captureReference(INVOKESTATIC),
                    IFEQ, any(2),
                    Mod.getMinecraftVersion().compareTo("13w36a") < 0 ? getSubExpression1() : getSubExpression2(),
                    IFEQ_or_IFNE, any(2)
                );
            }

            private String getSubExpression1() {
                addXref(2, lightValue);
                return build(
                    // 1.6: Block.lightValue[block.blockId]
                    captureReference(GETSTATIC),
                    ALOAD_1,
                    anyReference(GETFIELD),
                    IALOAD
                );
            }

            private String getSubExpression2() {
                addXref(2, getLightValue);
                return build(
                    // 1.7+: block.getLightValue()
                    ALOAD_1,
                    captureReference(INVOKEVIRTUAL)
                );
            }
        }
            .setMethod(renderStandardBlock)
            .addXref(1, isAmbientOcclusionEnabled)
        );

        if (haveSubclasses()) {
            addMemberMapper(new MethodMapper(renderBlock));
        }
        return this;
    }

    public RenderBlocksMod mapHasOverrideTexture() {
        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // return this.overrideBlockTexture != null;
                    begin(),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IFNULL_or_IFNONNULL, any(2),
                    any(4, 8),
                    IRETURN,
                    end()
                );
            }
        }
            .setMethod(hasOverrideBlockTexture)
            .addXref(1, overrideBlockTexture)
        );
        return this;
    }

    public RenderBlocksMod mapRenderType(final int type, MethodRef method) {
        if (haveSubclasses()) {
            throw new IllegalStateException("mapRenderType cannot be used in " + MIN_VERSION_SUBCLASS.toString() + "+");
        }

        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                int register = 2 + PositionMod.getDescriptorLength();
                return buildExpression(
                    // renderType = block.getRenderType();
                    ALOAD_1,
                    captureReference(INVOKEVIRTUAL),
                    registerLoadStore(ISTORE, register),

                    // ...
                    any(0, 1000),

                    // renderType == type ? this.method((optional cast) block, i, j, k) : ...
                    registerLoadStore(ILOAD, register),
                    push(type),
                    IF_ICMPNE, any(2),
                    ALOAD_0,
                    ALOAD_1,
                    optional(anyReference(CHECKCAST)),
                    PositionMod.passArguments(2),
                    capture(build(
                        subset(new int[]{INVOKEVIRTUAL, INVOKESPECIAL}, true),
                        any(2)
                    )),
                    subset(new int[]{GOTO, IRETURN}, true)
                );
            }
        }
            .setMethod(renderBlockByRenderType)
            .addXref(1, getRenderType)
            .addXref(2, method)
        );
        return this;
    }
}
