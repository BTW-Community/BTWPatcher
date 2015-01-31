package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.ResourceLocationMod;
import com.prupe.mcpatcher.basemod.ResourceLocationSignature;
import com.prupe.mcpatcher.basemod.TessellatorMod;
import com.prupe.mcpatcher.basemod.WorldClientMod;
import com.prupe.mcpatcher.basemod.ext18.IBlockStateMod;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;
import com.prupe.mcpatcher.basemod.ext18.RenderUtilsMod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static com.prupe.mcpatcher.mod.cc.CustomColors.getMinecraftVersion;
import static com.prupe.mcpatcher.mod.cc.CustomColors.setColor;
import static javassist.bytecode.Opcode.*;

class CC_World {
    static void setup(Mod mod) {
        mod.addClassMod(new WorldMod(mod));
        mod.addClassMod(new WorldClientMod(mod));
        mod.addClassMod(new WorldProviderMod(mod));
        mod.addClassMod(new WorldProviderHellMod(mod));
        mod.addClassMod(new WorldProviderEndMod(mod));
        mod.addClassMod(new WorldChunkManagerMod(mod));
        mod.addClassMod(new RenderGlobalMod(mod));
        mod.addClassMod(new FontRendererMod(mod));
        mod.addClassMod(new TileEntitySignRendererMod(mod));
        mod.addClassMod(new EntityRendererMod(mod));
    }

    private static class WorldMod extends com.prupe.mcpatcher.basemod.WorldMod {
        WorldMod(Mod mod) {
            super(mod);
            setInterfaces("IBlockAccess");
            mapLightningFlash();

            final MethodRef getWorldChunkManager = new MethodRef(getDeobfClass(), "getWorldChunkManager", "()LWorldChunkManager;");
            final MethodRef computeSkyColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeSkyColor", "(LWorld;F)Z");
            final MethodRef setupForFog = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "setupForFog", "(LEntity;)V");

            addMemberMapper(new MethodMapper(getWorldChunkManager));

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // f8 = (f4 * 0.3f + f5 * 0.59f + f6 * 0.11f) * 0.6f;
                                FLOAD, any(),
                                push(0.3f),
                                FMUL,
                                FLOAD, any(),
                                push(0.59f),
                                FMUL,
                                FADD,
                                FLOAD, any(),
                                push(0.11f),
                                FMUL,
                                FADD,
                                push(0.6f),
                                FMUL,
                                FSTORE, any()
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f4 = (float) (k >> 16 & 0xff) / 255.0f;
                        ILOAD, capture(any()),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f5 = (float) (k >> 8 & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f6 = (float) (k & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.setupForFog(entity);
                        ALOAD_1,
                        reference(INVOKESTATIC, setupForFog),

                        // if (ColorizeWorld.computeSkyColor(this, f)) {
                        ALOAD_0,
                        FLOAD_2,
                        reference(INVOKESTATIC, computeSkyColor),
                        IFEQ, branch("A"),

                        // f4 = Colorizer.setColor[0];
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(2),

                        // f5 = Colorizer.setColor[1];
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),

                        // f5 = Colorizer.setColor[2];
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        FSTORE, getCaptureGroup(4),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ... original code ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            });
        }
    }

    private static class WorldProviderMod extends com.prupe.mcpatcher.basemod.WorldProviderMod {
        WorldProviderMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature(0.06f));
            addClassSignature(new ConstSignature(0.09f));
            addClassSignature(new ConstSignature(0.91f));
            addClassSignature(new ConstSignature(0.94f));

            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorld;");
            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final MethodRef computeFogColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeFogColor", "(LWorldProvider;F)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, capture(any()),
                        FLOAD_3,
                        push(0.94f),
                        FMUL,
                        push(0.06f),
                        FADD,
                        FMUL,
                        FSTORE, backReference(1)
                    );
                }
            }.setMethod(getFogColor));

            addMemberMapper(new FieldMapper(worldObj));

            addPatch(new MakeMemberPublicPatch(worldObj));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(getFogColor);
                }

                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // r = 0.7529412f;
                        // g = 0.84705883f;
                        // b = 1.0F;
                        anyLDC,
                        capture(anyFSTORE),
                        anyLDC,
                        capture(anyFSTORE),
                        push(1.0f),
                        capture(anyFSTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeFogColor(this, partialTick)) {
                        ALOAD_0,
                        FLOAD_1,
                        reference(INVOKESTATIC, computeFogColor),
                        IFEQ, branch("A"),

                        // r = Colorizer.setColor[0];
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(1),

                        // g = Colorizer.setColor[1];
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(2),

                        // b = Colorizer.setColor[2];
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(3),

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private static class WorldProviderHellMod extends ClassMod {
        private static final double MAGIC1 = 0.20000000298023224;
        private static final double MAGIC2 = 0.029999999329447746;

        WorldProviderHellMod(Mod mod) {
            super(mod);
            setParentClass("WorldProvider");

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final FieldRef netherFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "netherFogColor", "[F");

            addClassSignature(new ConstSignature(MAGIC1));
            addClassSignature(new ConstSignature(MAGIC2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }
            }.setMethod(getFogColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override nether fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.netherFogColor[0], ColorizeWorld.netherFogColor[1], ColorizeWorld.netherFogColor[2]
                        reference(GETSTATIC, netherFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private static class WorldProviderEndMod extends ClassMod {
        WorldProviderEndMod(Mod mod) {
            super(mod);
            setParentClass("WorldProvider");

            addClassSignature(new OrSignature(
                new ConstSignature(0x8080a0), // pre 12w23a
                new ConstSignature(0xa080a0)  // 12w23a+
            ));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }
            });

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final FieldRef endFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endFogColor", "[F");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, endFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private static class WorldChunkManagerMod extends ClassMod {
        WorldChunkManagerMod(Mod mod) {
            super(mod);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        ILOAD, 5,
                        IMUL,
                        NEWARRAY, T_FLOAT,
                        ASTORE_1
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        optional(anyReference(INVOKESTATIC)),
                        ILOAD_1,
                        ILOAD_3,
                        ISUB,
                        ICONST_2,
                        ISHR,
                        ISTORE, 6
                    );
                }
            });

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(" + PositionMod.getDescriptorIKOnly() + ")LBiomeGenBase;")));
        }
    }

    private static class RenderGlobalMod extends ClassMod {
        RenderGlobalMod(Mod mod) {
            super(mod);

            final FieldRef clouds;
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef gameSettings = new FieldRef("Minecraft", "gameSettings", "LGameSettings;");
            final JavaRef fancyGraphics;
            final int fancyGraphicsOp;
            final String fancyGraphicsIf;
            final MethodRef drawFancyClouds;
            if (getMinecraftVersion().compareTo("1.8.1-pre4") < 0) {
                fancyGraphics = new FieldRef("GameSettings", "fancyGraphics", "Z");
                fancyGraphicsOp = GETFIELD;
                fancyGraphicsIf = buildExpression(IFEQ, any(2));
                drawFancyClouds = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "drawFancyClouds", "(Z)Z");
            } else {
                fancyGraphics = new MethodRef("GameSettings", "fancyGraphics", "()I");
                fancyGraphicsOp = INVOKEVIRTUAL;
                fancyGraphicsIf = buildExpression(ICONST_2, IF_ICMPNE_or_IF_ICMPEQ, any(2));
                drawFancyClouds = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "drawFancyClouds", "(I)I");
            }
            final boolean intParam = getMinecraftVersion().compareTo("14w25a") >= 0;
            final MethodRef renderClouds = new MethodRef(getDeobfClass(), "renderClouds", "(F" + (intParam ? "I" : "") + ")V");
            final MethodRef renderCloudsFancy = new MethodRef(getDeobfClass(), "renderCloudsFancy", renderClouds.getType());
            final FieldRef endSkyColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endSkyColor", "I");

            RenderUtilsMod.setup(this);

            if (ResourceLocationMod.haveClass()) {
                clouds = new FieldRef(getDeobfClass(), "clouds", "LResourceLocation;");
                addClassSignature(new ResourceLocationSignature(this, clouds, "textures/environment/clouds.png"));
            } else {
                addClassSignature(new ConstSignature("/environment/clouds.png"));
                clouds = null;
            }

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderClouds);
                    addXref(1, mc);
                    addXref(2, gameSettings);
                    addXref(3, fancyGraphics);
                    addXref(4, renderCloudsFancy);
                    if (clouds != null) {
                        addXref(5, clouds);
                    }
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 1.8.1-pre4+: if (mc.gameSettings.fancyGraphics() == 2) {
                        // earlier: if (mc.gameSettings.fancyGraphics) {
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(fancyGraphicsOp),
                        fancyGraphicsIf,

                        // this.renderCloudsFancy(...);
                        ALOAD_0,
                        FLOAD_1,
                        intParam ? build(ILOAD_2) : "",
                        captureReference(INVOKESPECIAL, INVOKEVIRTUAL),
                        or(build(GOTO, any(2)), build(RETURN)),

                        // ...
                        any(0, 150),

                        // ...(RenderGlobal.clouds);
                        // ...("/environment/clouds.png");
                        clouds == null ? push("/environment/clouds.png") : captureReference(GETSTATIC)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override cloud type";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(build(
                            ALOAD_0,
                            reference(GETFIELD, mc),
                            reference(GETFIELD, gameSettings),
                            reference(fancyGraphicsOp, fancyGraphics)
                        )),
                        capture(fancyGraphicsIf)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, drawFancyClouds),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(renderClouds));

            if (TessellatorMod.haveVertexFormatClass()) {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override end sky color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // 40, 40, 40, 255
                            push(40),
                            push(40),
                            push(40),
                            push(255)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // (ColorizeWorld.endSkyColor >> 16) & 0xff
                            reference(GETSTATIC, endSkyColor),
                            push(16),
                            ISHR,
                            push(0xff),
                            IAND,

                            // (ColorizeWorld.endSkyColor >> 8) & 0xff
                            reference(GETSTATIC, endSkyColor),
                            push(8),
                            ISHR,
                            push(0xff),
                            IAND,

                            // ColorizeWorld.endSkyColor & 0xff
                            reference(GETSTATIC, endSkyColor),
                            push(0xff),
                            IAND,

                            // 255
                            push(255)
                        );
                    }
                });
            } else {
                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "override end sky color";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(or(
                            build(push(0x181818)), // pre-12w23a
                            build(push(0x282828))  // 12w23a+
                        ));
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            reference(GETSTATIC, endSkyColor)
                        );
                    }
                });
            }
        }
    }

    private static class FontRendererMod extends com.prupe.mcpatcher.basemod.FontRendererMod {
        FontRendererMod(Mod mod) {
            super(mod);
            RenderUtilsMod.setup(this);

            final String renderStringArgs = IBlockStateMod.haveClass() ? "FF" : "II";
            final MethodRef renderString = new MethodRef(getDeobfClass(), "renderString", "(Ljava/lang/String;" + renderStringArgs + "IZ)I");
            final FieldRef colorCode = new FieldRef(getDeobfClass(), "colorCode", "[I");
            final MethodRef colorizeText1 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(I)I");
            final MethodRef colorizeText2 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(II)I");

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, colorCode);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(32),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xff000000),
                        any(0, 100),
                        RenderUtilsMod.glColor4f(this)
                    );
                }
            }.setMethod(renderString));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderString);
                }

                @Override
                public String getDescription() {
                    return "override text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        push(0xfc000000),
                        IAND
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ILOAD, 4,
                        reference(INVOKESTATIC, colorizeText1),
                        ISTORE, 4
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override text color codes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, colorCode),
                        capture(anyILOAD),
                        IALOAD
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeText2)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private static class TileEntitySignRendererMod extends ClassMod {
        TileEntitySignRendererMod(Mod mod) {
            super(mod);
            RenderUtilsMod.setup(this);

            final FieldRef sign;
            final MethodRef colorizeSignText = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeSignText", "()I");

            if (ResourceLocationMod.haveClass()) {
                sign = new FieldRef(getDeobfClass(), "sign", "LResourceLocation;");
                addClassSignature(new ResourceLocationSignature(this, sign, "textures/entity/sign.png"));
            } else {
                sign = null;
                addClassSignature(new ConstSignature("/item/sign.png"));
            }

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                sign == null ? push("/item/sign.png") : reference(GETSTATIC, sign)
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sign text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
                        RenderUtilsMod.glDepthMask(this),
                        push(0),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(0),
                        RenderUtilsMod.glDepthMask(this),
                        reference(INVOKESTATIC, colorizeSignText),
                        getCaptureGroup(1)
                    );
                }
            });
        }
    }

    private static class EntityRendererMod extends ClassMod {
        EntityRendererMod(Mod mod) {
            super(mod);

            final MethodRef updateLightmap = new MethodRef(getDeobfClass(), "updateLightmap", "(F)V");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef updateFogColor = new MethodRef(getDeobfClass(), "updateFogColor", "(F)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");
            final FieldRef lightmapColors = new FieldRef(getDeobfClass(), "lightmapColors", "[I");
            final FieldRef lightmapTexture = new FieldRef(getDeobfClass(), "lightmapTexture", ResourceLocationMod.select("I", "LDynamicTexture;"));
            final FieldRef needLightmapUpdate = new FieldRef(getDeobfClass(), "needLightmapUpdate", "Z");
            final FieldRef renderEngine = new FieldRef("Minecraft", "renderEngine", "LRenderEngine;");
            final MethodRef createTextureFromBytes = new MethodRef("RenderEngine", "createTextureFromBytes", "([IIII)V");
            final FieldRef thePlayer = new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;");
            final FieldRef nightVision = new FieldRef("Potion", "nightVision", "LPotion;");
            final MethodRef isPotionActive = new MethodRef("EntityClientPlayerMP", "isPotionActive", "(LPotion;)Z");
            final String nvEntity = getMinecraftVersion().compareTo("14w06a") >= 0 ? "LEntityLivingBase;" : "LEntityPlayer;";
            final MethodRef getNightVisionStrength1 = new MethodRef(getDeobfClass(), "getNightVisionStrength1", "(" + nvEntity + "F)F");
            final MethodRef getNightVisionStrength = new MethodRef(getDeobfClass(), "getNightVisionStrength", "(F)F");
            final MethodRef reloadTexture = new MethodRef("DynamicTexture", "reload", "()V");
            final MethodRef computeLightmap = new MethodRef(MCPatcherUtils.LIGHTMAP_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;[IF)Z");
            final MethodRef computeUnderwaterColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeUnderwaterColor", "()Z");
            final MethodRef computeUnderlavaColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeUnderlavaColor", "()Z");

            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateLightmap);
                    addXref(1, new MethodRef("World", "getSunAngle", "(F)F"));
                    addXref(3, new FieldRef("World", "worldProvider", "LWorldProvider;"));
                    addXref(5, new FieldRef(getDeobfClass(), "torchFlickerX", "F"));
                    addXref(6, com.prupe.mcpatcher.basemod.WorldMod.getLightningFlashRef());
                    addXref(7, com.prupe.mcpatcher.basemod.WorldProviderMod.getWorldTypeRef());
                    addXref(8, mc);
                    addXref(9, new FieldRef("Minecraft", "gameSettings", "LGameSettings;"));
                    addXref(10, new FieldRef("GameSettings", "gammaSetting", "F"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sun = world.getSunAngle(1.0f) * 0.95f + 0.05f;
                        ALOAD_2,
                        push(1.0f),
                        captureReference(INVOKEVIRTUAL),
                        optional(build(anyFSTORE, anyFLOAD)), // 1.8.1-pre1
                        push(0.95f),
                        FMUL,
                        push(0.05f),
                        FADD,
                        FSTORE, capture(any()),

                        // ... (1.8.1-pre1+)
                        any(0, 20),

                        // older: lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        // 14w02a+: lightsun = world.worldProvider.getLightBrightnessTable()[i / 16] * sun;
                        ALOAD_2,
                        captureReference(GETFIELD),
                        anyReference(GETFIELD, INVOKEVIRTUAL),
                        capture(anyILOAD),
                        push(16),
                        IDIV,
                        FALOAD,
                        FLOAD, backReference(2),
                        FMUL,
                        anyFSTORE,

                        // older: lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1f + 1.5f);
                        // 14w02a+: lighttorch = world.worldProvider.getLightBrightnessTable()[i % 16] * (torchFlickerX * 0.1f + 1.5f);
                        any(0, 20),
                        backReference(4),
                        push(16),
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),

                        // ...
                        any(0, 30),

                        // older: if (world.lightningFlash > 0)
                        // 14w02a+: if (world.getLightningFlash() > 0)
                        ALOAD_2,
                        captureReference(com.prupe.mcpatcher.basemod.WorldMod.getLightningFlashOpcode()),
                        IFLE, any(2),

                        // ...
                        any(0, 300),

                        // older: if (world.worldProvider.worldType == 1) {
                        // 14w02a+: if (world.worldProvider.getWorldType() == 1) {
                        ALOAD_2,
                        backReference(3),
                        captureReference(com.prupe.mcpatcher.basemod.WorldProviderMod.getWorldTypeOpcode()),
                        push(1),
                        IF_ICMPNE, any(2),

                        // ...
                        any(0, 200),

                        // gamma = mc.gameSettings.gammaSetting;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        anyFSTORE,

                        // ...
                        any(0, 300),

                        ResourceLocationMod.haveClass() ? getSubExpression16(10) : getSubExpression15(10),
                        RETURN
                    );
                }

                private String getSubExpression15(int xref) {
                    addXref(xref + 1, renderEngine);
                    addXref(xref + 2, lightmapColors);
                    addXref(xref + 3, lightmapTexture);
                    addXref(xref + 4, createTextureFromBytes);
                    return buildExpression(
                        // this.mc.renderEngine.createTextureFromBytes(this.lightmapColors, 16, 16, this.lightmapTexture);
                        ALOAD_0,
                        backReference(xref - 2),
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(16),
                        push(16),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL)
                    );
                }

                private String getSubExpression16(int xref) {
                    addXref(xref + 1, lightmapColors);
                    addXref(xref + 2, lightmapTexture);
                    addXref(xref + 3, reloadTexture);
                    addXref(xref + 4, needLightmapUpdate);
                    return buildExpression(
                        // this.lightmapColors[i] = ...;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(0, 50),
                        IASTORE,

                        // ...
                        any(0, 20),

                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        push(0),
                        captureReference(PUTFIELD),

                        // ...
                        any(0, 20)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // fogColorRed = 0.02f;
                            ALOAD_0,
                            push(0.02f),
                            capture(optional(build( // 13w16a+
                                anyFLOAD,
                                FADD
                            ))),
                            captureReference(PUTFIELD),

                            // fogColorGreen = 0.02f;
                            ALOAD_0,
                            push(0.02f),
                            backReference(1),
                            captureReference(PUTFIELD),

                            // fogColorBlue = 0.2f;
                            ALOAD_0,
                            push(0.2f),
                            backReference(1),
                            captureReference(PUTFIELD)
                        );
                    }
                }
                    .setMethod(updateFogColor)
                    .addXref(2, fogColorRed)
                    .addXref(3, fogColorGreen)
                    .addXref(4, fogColorBlue)
            );

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
                            capture(build(
                                ALOAD_0,
                                captureReference(GETFIELD),
                                captureReference(GETFIELD),
                                captureReference(GETSTATIC),
                                captureReference(INVOKEVIRTUAL)
                            )),
                            IFEQ, any(2),

                            // var16 = getNightVisionStrength1(mc.thePlayer, var1);
                            capture(build(
                                ALOAD_0,
                                ALOAD_0,
                                backReference(2),
                                backReference(3),
                                FLOAD_1,
                                captureReference(INVOKESPECIAL)
                            )),
                            FSTORE, any()
                        );
                    }
                }
                    .setMethod(updateLightmap)
                    .addXref(2, mc)
                    .addXref(3, thePlayer)
                    .addXref(4, nightVision)
                    .addXref(5, isPotionActive)
                    .addXref(7, getNightVisionStrength1)
            );

            addPatch(new AddMethodPatch(getNightVisionStrength) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, thePlayer),
                        reference(GETSTATIC, nightVision),
                        reference(INVOKEVIRTUAL, isPotionActive),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, thePlayer),
                        FLOAD_1,
                        reference(INVOKESPECIAL, getNightVisionStrength1),
                        FRETURN,

                        label("A"),
                        push(0.0f),
                        FRETURN
                    );
                }
            });

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updateLightmap);
                }

                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ASTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Lightmap.computeLightmap(this, world, this.lightmapColors, partialTick)) {
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_0,
                        reference(GETFIELD, lightmapColors),
                        FLOAD_1,
                        reference(INVOKESTATIC, computeLightmap),
                        IFEQ, branch("A"),

                        ResourceLocationMod.haveClass() ? loadTexture16() : loadTexture15(),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }

                private byte[] loadTexture15() {
                    return buildCode(
                        // this.mc.renderEngine.createTextureFromBytes(this.lightmapColors, 16, 16, this.lightmapTexture);
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, renderEngine),
                        ALOAD_0,
                        reference(GETFIELD, lightmapColors),
                        push(16),
                        push(16),
                        ALOAD_0,
                        reference(GETFIELD, lightmapTexture),
                        reference(INVOKEVIRTUAL, createTextureFromBytes)
                    );
                }

                private byte[] loadTexture16() {
                    return buildCode(
                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        reference(GETFIELD, lightmapTexture),
                        reference(INVOKEVIRTUAL, reloadTexture),
                        ALOAD_0,
                        push(0),
                        reference(PUTFIELD, needLightmapUpdate)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updateFogColor);
                }

                @Override
                public String getDescription() {
                    return "override underwater ambient color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        capture(optional(build( // 13w16a+
                            anyFLOAD,
                            FADD
                        ))),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        backReference(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        backReference(1),
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeUnderwaterColor()) {
                        reference(INVOKESTATIC, computeUnderwaterColor),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updateFogColor);
                }

                @Override
                public String getDescription() {
                    return "override underlava ambient color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.fogColorRed = 0.6f;
                        ALOAD_0,
                        push(0.6f),
                        reference(PUTFIELD, fogColorRed),

                        // this.fogColorGreen = 0.1f;
                        ALOAD_0,
                        push(0.1f),
                        reference(PUTFIELD, fogColorGreen),

                        // this.fogColorBlue = 0.0f;
                        ALOAD_0,
                        push(0.0f),
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeUnderlavaColor()) {
                        reference(INVOKESTATIC, computeUnderlavaColor),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            });
        }
    }
}
