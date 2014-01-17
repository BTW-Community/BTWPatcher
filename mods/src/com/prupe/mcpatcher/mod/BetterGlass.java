package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterGlass extends Mod {
    private static final int EXTRA_PASSES = 2;

    private static final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef glDisable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDisable", "(I)V");
    private static final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");
    private static final MethodRef glShadeModel = new MethodRef(MCPatcherUtils.GL11_CLASS, "glShadeModel", "(I)V");
    private static final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
    private static final MethodRef glAlphaFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glAlphaFunc", "(IF)V");
    private static final MethodRef enableLightmap = new MethodRef("EntityRenderer", "enableLightmap", "(D)V");
    private static final MethodRef disableLightmap = new MethodRef("EntityRenderer", "disableLightmap", "(D)V");

    public BetterGlass() {
        name = MCPatcherUtils.BETTER_GLASS;
        author = "MCPatcher";
        description = "Enables partial transparency for glass blocks.";
        version = "2.4";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.CONNECTED_TEXTURES);

        addClassMod(new MinecraftMod(this));
        addClassMod(new ResourceLocationMod(this));
        addClassMod(new BlockMod(this));
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new WorldRendererMod());
        addClassMod(new EntityRendererMod());
        addClassMod(new RenderGlobalMod());
        addClassMod(new RenderBlocksMod());
        if (RenderPassEnumMod.haveRenderPassEnum()) {
            addClassMod(new RenderPassEnumMod());
        }

        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS);
        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS + "$1");
        addClassFile(MCPatcherUtils.RENDER_PASS_CLASS + "$2");
    }

    private class WorldRendererMod extends com.prupe.mcpatcher.basemod.WorldRendererMod {
        private final FieldRef skipRenderPass = new FieldRef(getDeobfClass(), "skipRenderPass", "[Z");
        private int loopRegister;

        WorldRendererMod() {
            super(BetterGlass.this);

            final MethodRef getRenderBlockPass = new MethodRef("Block", "getRenderBlockPass", "()" + RenderPassEnumMod.getDescriptor());
            final MethodRef startPass = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "start", "(I)V");
            final MethodRef canRenderInPass1 = new MethodRef("forge/ForgeHooksClient", "canRenderInPass", "(LBlock;I)Z");
            final MethodRef canRenderInPass2 = new MethodRef("Block", "canRenderInPass", "(I)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // j3 = block.getRenderBlockPass();
                        anyALOAD,
                        captureReference(INVOKEVIRTUAL),
                        RenderPassEnumMod.getStoreOpcode(), capture(any()),

                        // ...
                        any(0, 30),

                        // if (j3 != i2)
                        // -or-
                        // if (j3 > i2)
                        RenderPassEnumMod.getLoadOpcode(), backReference(2),
                        RenderPassEnumMod.getOrdinalExpr(),
                        RenderPassEnumMod.getLoadOpcode(), any(),
                        RenderPassEnumMod.getOrdinalExpr(),
                        subset(new int[]{IF_ICMPEQ, IF_ICMPLE}, true), any(2),

                        // flag = true;
                        push(1),
                        ISTORE, any()
                    );
                }
            }
                .setMethod(updateRenderer)
                .addXref(1, getRenderBlockPass)
            );

            addMemberMapper(new FieldMapper(skipRenderPass));

            if (!RenderPassEnumMod.haveRenderPassEnum()) {
                setupPre18();
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "pre render pass";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
                        ISTORE, capture(any()),
                        push(0),
                        ISTORE, any(),
                        push(0),
                        ISTORE, any()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    loopRegister = (getCaptureGroup(1)[0] & 0xff) - 1;
                    Logger.log(Logger.LOG_CONST, "loop register %d", loopRegister);
                    return buildCode(
                        ILOAD, loopRegister,
                        reference(INVOKESTATIC, startPass)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(updateRenderer)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "prevent early loop exit";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (!var12) {
                        ILOAD, loopRegister + 1,
                        IFNE, any(2),

                        // break;
                        GOTO, any(2)

                        // }
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up extra render pass";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, getRenderBlockPass)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "getBlockRenderPass", "(LBlock;)I"))
                    );
                }
            }.targetMethod(updateRenderer));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up extra render pass (forge)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(reference(INVOKESTATIC, canRenderInPass1)),
                        build(reference(INVOKEVIRTUAL, canRenderInPass2))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        DUP2,
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "canRenderInPass", "(LBlock;IZ)Z"))
                    );
                }
            }.targetMethod(updateRenderer));
        }

        private void setupPre18() {
            final FieldRef glRenderList = new FieldRef(getDeobfClass(), "glRenderList", "I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(2),
                        IADD,
                        reference(INVOKESTATIC, glCallList)
                    );
                }
            }.addXref(1, glRenderList));

            addPatch(new RenderPassPatch("init") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        ALOAD_0
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        NEWARRAY, T_BOOLEAN,
                        reference(PUTFIELD, skipRenderPass)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new RenderPassPatch("loop") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        anyILOAD
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        IF_ICMPLT_or_IF_ICMPGE, any(2)
                    );
                }
            });

            addPatch(new RenderPassPatch("occlusion") {
                @Override
                protected String getPrefix() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, glRenderList)
                    );
                }

                @Override
                protected String getSuffix() {
                    return buildExpression(
                        IADD
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "increase render passes from 2 to " + (2 + EXTRA_PASSES) + " (&&)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // return skipRenderPass[0] && skipRenderPass[1];
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        push(0),
                        BALOAD,
                        IFEQ, any(2),
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        push(1),
                        BALOAD,
                        IFEQ, any(2),
                        push(1),
                        or(
                            build(IRETURN),
                            build(
                                GOTO, any(2)
                            )
                        ),
                        push(0),
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // return RenderPass.skipAllRenderPasses(skipRenderPass);
                        ALOAD_0,
                        reference(GETFIELD, skipRenderPass),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "skipAllRenderPasses", "([Z)Z")),
                        IRETURN
                    );
                }
            });
        }

        abstract private class RenderPassPatch extends BytecodePatch {
            private final String tag;

            RenderPassPatch(String tag) {
                this.tag = tag;
            }

            @Override
            public String getDescription() {
                return "increase render passes from 2 to " + (2 + EXTRA_PASSES) + " (" + tag + ")";
            }

            @Override
            public final String getMatchExpression() {
                return buildExpression(
                    lookBehind(getPrefix(), true),
                    push(2),
                    lookAhead(getSuffix(), true)
                );
            }

            @Override
            public final byte[] getReplacementBytes() {
                return buildCode(
                    push(2 + EXTRA_PASSES)
                );
            }

            abstract protected String getPrefix();

            abstract protected String getSuffix();
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            final MethodRef renderWorld = new MethodRef(getDeobfClass(), "renderWorld", "(FJ)V");
            final MethodRef renderRainSnow = new MethodRef(getDeobfClass(), "renderRainSnow", "(F)V");
            final MethodRef sortAndRender = new MethodRef("RenderGlobal", "sortAndRender", "(LEntityLivingBase;" + RenderPassEnumMod.getDescriptor() + "D)I");
            final MethodRef doRenderPass = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "doRenderPass", "(LRenderGlobal;LEntityLivingBase;ID)V");

            addClassSignature(new ConstSignature("textures/environment/snow.png"));
            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // (var =) renderGlobal.sortAndRender(camera, 1, (double) partialTick);
                        ALOAD, 5,
                        ALOAD, 4,
                        RenderPassEnumMod.getPassExpr(this, 1),
                        FLOAD_1,
                        F2D,
                        captureReference(INVOKEVIRTUAL),
                        or(
                            build(ISTORE, any()), // pre-13w41a
                            build(POP) // 13w41a+
                        )
                    );
                }
            }
                .setMethod(renderWorld)
                .addXref(1, sortAndRender)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D,
                        reference(INVOKESTATIC, glDisable)
                    );
                }
            }.setMethod(disableLightmap));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(3553), // GL_TEXTURE_2D,
                        reference(INVOKESTATIC, glEnable)
                    );
                }
            }.setMethod(enableLightmap));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.rainXCoords = new float[1024];
                        ALOAD_0,
                        push(1024),
                        NEWARRAY, T_FLOAT,
                        anyReference(PUTFIELD)
                    );
                }
            }.setMethod(renderRainSnow));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set gl shade model";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (...)
                        IFEQ, any(2),

                        // GL11.glShadeModel(GL11.GL_SMOOTH);
                        push(7425),
                        reference(INVOKESTATIC, glShadeModel)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (RenderPass.setAmbientOcclusion(...))
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "setAmbientOcclusion", "(Z)Z"))
                    );
                }
            }.setInsertBefore(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "do extra render pass 2";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD, 5,
                        ALOAD, 4,
                        push(0),
                        FLOAD_1,
                        F2D,
                        reference(INVOKEVIRTUAL, sortAndRender),
                        POP
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // RenderPass.doRenderPass(renderGlobal, camera, 2, par1);
                        ALOAD, 5,
                        ALOAD, 4,
                        push(2),
                        FLOAD_1,
                        F2D,
                        reference(INVOKESTATIC, doRenderPass)
                    );
                }
            }.setInsertAfter(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "do extra render pass 3";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glDepthMask(true);
                        push(1),
                        reference(INVOKESTATIC, glDepthMask),

                        // GL11.glEnable(GL11.GL_CULL_FACE);
                        push(2884), // GL_CULL_FACE
                        reference(INVOKESTATIC, glEnable),

                        // GL11.glDisable(GL11.GL_BLEND);
                        push(3042), // GL_BLEND
                        reference(INVOKESTATIC, glDisable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // renderRainSnow(par1);
                        ALOAD_0,
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, renderRainSnow),

                        // RenderPass.doRenderPass(renderGlobal, camera, 3, par1);
                        ALOAD, 5,
                        ALOAD, 4,
                        push(3),
                        FLOAD_1,
                        F2D,
                        reference(INVOKESTATIC, doRenderPass)
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderWorld)
            );
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final FieldRef glRenderListBase = new FieldRef(getDeobfClass(), "glRenderListBase", "I");
            final MethodRef loadRenderers = new MethodRef(getDeobfClass(), "loadRenderers", "()V");
            final MethodRef renderAllRenderLists = new MethodRef(getDeobfClass(), "renderAllRenderLists", "(" + (RenderPassEnumMod.haveRenderPassEnum() ? "" : "I") + "D)V");
            final MethodRef generateDisplayLists = new MethodRef("GLAllocation", "generateDisplayLists", "(I)I");

            addClassSignature(new ConstSignature("smoke"));
            addClassSignature(new ConstSignature("textures/environment/clouds.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.glRenderListBase = GLAlloctation.generateDisplayLists(... * 3);
                        RenderPassEnumMod.haveRenderPassEnum() ? build(push(2), IADD) : push(3),
                        IMUL,
                        captureReference(INVOKESTATIC),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, generateDisplayLists)
                .addXref(2, glRenderListBase)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.worldRenderers = new WorldRenderer[this.renderChunksWide * this.renderChunksTall * this.renderChunksDeep];
                        ALOAD_0,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        IMUL,
                        ALOAD_0,
                        anyReference(GETFIELD),
                        IMUL,
                        anyReference(ANEWARRAY),
                        anyReference(PUTFIELD)
                    );
                }
            }.setMethod(loadRenderers));

            addClassSignature(new OrSignature(
                // 1.4.3
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // var4 = this.allRenderLists;
                            ALOAD_0,
                            anyReference(GETFIELD),
                            ASTORE, capture(any()),

                            // var5 = var4.length;
                            ALOAD, backReference(1),
                            ARRAYLENGTH,
                            build(ISTORE, any())
                        );
                    }
                }.setMethod(renderAllRenderLists),
                // 1.4.4+
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // for (int var4 = 0; var4 < this.allRenderLists.length; var4++)
                            push(0),
                            capture(anyISTORE),
                            capture(anyILOAD),

                            ALOAD_0,
                            captureReference(GETFIELD),
                            ARRAYLENGTH,
                            IF_ICMPGE_or_IF_ICMPLT, any(2),

                            // this.allRenderLists[var4]...
                            ALOAD_0,
                            backReference(3),
                            backReference(2),
                            AALOAD
                        );
                    }

                    @Override
                    public boolean afterMatch() {
                        return extractRegisterNum(getCaptureGroup(1)) == extractRegisterNum(getCaptureGroup(2));
                    }
                }.setMethod(renderAllRenderLists)
            ));

            if (RenderPassEnumMod.haveRenderPassEnum()) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "increase gl render lists per chunk";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // RenderPassEnum.TRANSLUCENT.ordinal() + 2
                            reference(GETSTATIC, RenderPassEnumMod.TRANSLUCENT),
                            reference(INVOKEVIRTUAL, RenderPassEnumMod.ordinal),
                            push(2),
                            IADD
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // RenderPassEnum.values().length + 1
                            reference(INVOKESTATIC, RenderPassEnumMod.values),
                            ARRAYLENGTH,
                            push(1),
                            IADD
                        );
                    }
                });
            } else {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "increase gl render lists per chunk from 3 to " + (3 + EXTRA_PASSES) + " (init)";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(3),
                            lookAhead(build(
                                IMUL,
                                reference(INVOKESTATIC, generateDisplayLists)
                            ), true)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            push(3 + EXTRA_PASSES)
                        );
                    }
                }.matchConstructorOnly(true));

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "increase gl render lists per chunk from 3 to " + (3 + EXTRA_PASSES) + " (loop)";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            IINC, capture(any()), 3
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            IINC, getCaptureGroup(1), 3 + EXTRA_PASSES
                        );
                    }
                }.targetMethod(loadRenderers));
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up lightmap for extra render passes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // mc.entityRenderer.enableLightmap(par2);
                        lookBehind(build(
                            ALOAD_0,
                            anyReference(GETFIELD),
                            anyReference(GETFIELD),
                            DLOAD_2
                        ), true),
                        reference(INVOKEVIRTUAL, enableLightmap)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ILOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "enableDisableLightmap", "(LEntityRenderer;DI)V"))
                    );
                }
            }.targetMethod(renderAllRenderLists));
        }
    }

    private class RenderBlocksMod extends com.prupe.mcpatcher.basemod.RenderBlocksMod {
        RenderBlocksMod() {
            super(BetterGlass.this);

            final MethodRef newGetAOBaseMultiplier = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "getAOBaseMultiplier", "(F)F");
            final MethodRef newShouldSideBeRendered = new MethodRef(MCPatcherUtils.RENDER_PASS_CLASS, "shouldSideBeRendered", "(LBlock;LIBlockAccess;" + PositionMod.getDescriptor() + DirectionMod.getDescriptor() + ")Z");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override AO block brightness for extra render passes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(push(0.5f)),
                        build(push(0.6f)),
                        build(push(0.8f))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, newGetAOBaseMultiplier)
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderStandardBlockWithAmbientOcclusion)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render all sides of adjacent blocks";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, shouldSideBeRendered)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, newShouldSideBeRendered)
                    );
                }
            });
        }
    }

    private class RenderPassEnumMod extends com.prupe.mcpatcher.basemod.RenderPassEnumMod {
        RenderPassEnumMod() {
            super(BetterGlass.this);

            addPatch(new BytecodePatch() {
                private final ClassRef classRef = new ClassRef(getDeobfClass());
                private final MethodRef arraycopy = new MethodRef("java/lang/System", "arraycopy", "([Ljava/lang/Object;I[Ljava/lang/Object;II)V");
                private final MethodRef constructor = new MethodRef(getDeobfClass(), "<init>", "(Ljava/lang/String;I)V");
                private int tmp1;
                private int tmp2;

                @Override
                public String getDescription() {
                    return "add new render passes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // RenderPassEnum.values = new RenderPassEnum[]{...};
                        AASTORE,
                        reference(PUTSTATIC, valuesArray)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    tmp1 = getMethodInfo().getCodeAttribute().getMaxLocals();
                    tmp2 = tmp1 + 1;

                    return buildCode(
                        // RenderPassEnum[] tmp1 = new RenderPassEnum[]{...};
                        AASTORE,
                        registerLoadStore(ASTORE, tmp1),

                        // RenderPassEnum[] tmp2 = new RenderPassEnum[tmp1.length + 2];
                        registerLoadStore(ALOAD, tmp1),
                        ARRAYLENGTH,
                        push(2),
                        IADD,
                        reference(ANEWARRAY, classRef),
                        registerLoadStore(ASTORE, tmp2),

                        // System.arraycopy(tmp1, 0, tmp2, 0, tmp1.length);
                        registerLoadStore(ALOAD, tmp1),
                        push(0),
                        registerLoadStore(ALOAD, tmp2),
                        push(0),
                        registerLoadStore(ALOAD, tmp1),
                        ARRAYLENGTH,
                        reference(INVOKESTATIC, arraycopy),

                        // tmp2[tmp1.length] = new RenderPassEnum("BACKFACE", tmp1.length);
                        newEnum("BACKFACE", 0),

                        // tmp2[tmp1.length + 1] = new RenderPassEnum("OVERLAY", tmp1.length + 1)
                        newEnum("OVERLAY", 1),

                        // RenderPassEnum.values = tmp2;
                        registerLoadStore(ALOAD, tmp2),
                        reference(PUTSTATIC, valuesArray)
                    );
                }

                private byte[] newEnum(String name, int offset) {
                    return buildCode(
                        // tmp2[tmp1.length + offset] = new RenderPassEnum(name, tmp1.length + offset)
                        registerLoadStore(ALOAD, tmp2),
                        registerLoadStore(ALOAD, tmp1),
                        ARRAYLENGTH,
                        push(offset),
                        IADD,
                        reference(NEW, classRef),
                        DUP,
                        push(name),
                        registerLoadStore(ALOAD, tmp1),
                        ARRAYLENGTH,
                        push(offset),
                        IADD,
                        reference(INVOKESPECIAL, constructor),
                        AASTORE
                    );
                }
            }.matchStaticInitializerOnly(true));
        }
    }
}
