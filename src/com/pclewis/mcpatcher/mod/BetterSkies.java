package com.pclewis.mcpatcher.mod;

import com.pclewis.mcpatcher.*;

import java.io.IOException;

import static com.pclewis.mcpatcher.BinaryRegex.*;
import static com.pclewis.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterSkies extends Mod {
    private final boolean haveNewWorld;
    private final boolean haveNewWorldTime;
    private final boolean haveFireworks;
    private final String worldObjClass;

    public BetterSkies(MinecraftVersion minecraftVersion) {
        name = MCPatcherUtils.BETTER_SKIES;
        author = "MCPatcher";
        description = "Adds support for custom skyboxes.";
        version = "1.1";

        addDependency(BaseTexturePackMod.NAME);

        haveNewWorld = minecraftVersion.compareTo("12w18a") >= 0;
        haveNewWorldTime = minecraftVersion.compareTo("12w32a") >= 0;
        haveFireworks = minecraftVersion.compareTo("12w50a") >= 0;

        addClassMod(new BaseMod.MinecraftMod().addWorldGetter(minecraftVersion));
        addClassMod(new WorldMod());
        if (haveNewWorld) {
            addClassMod(new BaseMod.WorldServerMPMod(minecraftVersion));
            addClassMod(new BaseMod.WorldServerMod(minecraftVersion));
            worldObjClass = "WorldServerMP";
        } else {
            worldObjClass = "World";
        }
        addClassMod(new RenderGlobalMod());

        if (haveFireworks) {
            addClassMod(new EffectRendererMod());
            addClassMod(new EntityFireworkSparkFXMod());
        }

        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS);
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$1");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$WorldEntry");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$Layer");
        addClassFile(MCPatcherUtils.FIREWORKS_HELPER_CLASS);
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            final MethodRef getWorldTime = new MethodRef(getDeobfClass(), "getWorldTime", "()J");

            if (haveNewWorldTime) {
                addMemberMapper(new MethodMapper(null, null, getWorldTime));
            } else {
                addMemberMapper(new MethodMapper(null, getWorldTime));
            }
        }
    }

    private class RenderGlobalMod extends ClassMod {
        private final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

        RenderGlobalMod() {
            final MethodRef getTexture = new MethodRef("RenderEngine", "getTexture", "(Ljava/lang/String;)I");
            final MethodRef bindTexture = new MethodRef("RenderEngine", "bindTexture", "(I)V");
            final MethodRef getCelestialAngle = new MethodRef(worldObjClass, "getCelestialAngle", "(F)F");
            final MethodRef getRainStrength = new MethodRef("World", "getRainStrength", "(F)F");
            final MethodRef startDrawingQuads = new MethodRef("Tessellator", "startDrawingQuads", "()V");
            final MethodRef setColorOpaque_I = new MethodRef("Tessellator", "setColorOpaque_I", "(I)V");
            final MethodRef addVertexWithUV = new MethodRef("Tessellator", "addVertexWithUV", "(DDDDD)V");
            final MethodRef draw = new MethodRef("Tessellator", "draw", "()I");
            final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");
            final MethodRef glCallList = new MethodRef(MCPatcherUtils.GL11_CLASS, "glCallList", "(I)V");
            final FieldRef tessellator = new FieldRef("Tessellator", "instance", "LTessellator;");
            final FieldRef renderEngine = new FieldRef(getDeobfClass(), "renderEngine", "LRenderEngine;");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef worldProvider = new FieldRef("World", "worldProvider", "LWorldProvider;");
            final FieldRef worldType = new FieldRef("WorldProvider", "worldType", "I");
            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "L" + worldObjClass + ";");
            final FieldRef glSkyList = new FieldRef(getDeobfClass(), "glSkyList", "I");
            final FieldRef glSkyList2 = new FieldRef(getDeobfClass(), "glSkyList2", "I");
            final FieldRef glStarList = new FieldRef(getDeobfClass(), "glStarList", "I");
            final FieldRef active = new FieldRef(MCPatcherUtils.SKY_RENDERER_CLASS, "active", "Z");

            addClassSignature(new ConstSignature("smoke"));
            addClassSignature(new ConstSignature("/environment/clouds.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // mc.theWorld.worldProvider.worldType == 1
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(3),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        ICONST_1,

                        // ...
                        any(0, 100),

                        // renderEngine.bindTexture(renderEngine.getTexture("/misc/tunnel.png"));
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        backReference(4),
                        push("/misc/tunnel.png"),
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKEVIRTUAL),

                        // Tessellator tessellator = Tessellator.instance;
                        captureReference(GETSTATIC),
                        anyASTORE,

                        // ...
                        any(0, 1000),

                        // d = 1.0F - worldObj.getRainStrength(par1);
                        push(1.0f),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        FSUB,
                        or(
                            build(
                                anyFSTORE
                            ),
                            build(
                                F2D,
                                anyDSTORE
                            )
                        ),

                        // ..
                        any(0, 500),

                        // GL11.glRotatef(worldObj.getCelestialAngle(par1) * 360F, 1.0F, 0.0F, 0.0F);
                        ALOAD_0,
                        backReference(8),
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        push(360.0f),
                        FMUL,
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
                    );
                }
            }
                .setMethod(renderSky)
                .addXref(1, mc)
                .addXref(2, worldProvider)
                .addXref(3, worldType)
                .addXref(4, renderEngine)
                .addXref(5, getTexture)
                .addXref(6, bindTexture)
                .addXref(7, tessellator)
                .addXref(8, worldObj)
                .addXref(9, getRainStrength)
                .addXref(10, getCelestialAngle)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList),

                        nonGreedy(any(0, 1000)),

                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList),

                        nonGreedy(any(0, 1000)),

                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glCallList)
                    );
                }
            }
                .setMethod(renderSky)
                .addXref(1, glSkyList)
                .addXref(2, glStarList)
                .addXref(3, glSkyList2)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // tessellator.startDrawingQuads();
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.setColorOpaque_I(0x...);
                        ALOAD_2,
                        anyLDC,
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addVertexWithUV(-100D, -100D, -100D, 0.0D, 0.0D);
                        ALOAD_2,
                        push(-100.0),
                        push(-100.0),
                        push(-100.0),
                        push(0.0),
                        push(0.0),
                        captureReference(INVOKEVIRTUAL),

                        // tessellator.addVertexWithUV(-100D, -100D, 100D, 0.0D, 16D);
                        ALOAD_2,
                        push(-100.0),
                        push(-100.0),
                        push(100.0),
                        push(0.0),
                        push(16.0),
                        backReference(3),

                        // tessellator.addVertexWithUV(100D, -100D, 100D, 16D, 16D);
                        ALOAD_2,
                        push(100.0),
                        push(-100.0),
                        push(100.0),
                        push(16.0),
                        push(16.0),
                        backReference(3),

                        // tessellator.addVertexWithUV(100D, -100D, -100D, 16D, 0.0D);
                        ALOAD_2,
                        push(100.0),
                        push(-100.0),
                        push(-100.0),
                        push(16.0),
                        push(0.0),
                        backReference(3),

                        // tessellator.draw();
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),
                        POP
                    );
                }
            }
                .addXref(1, startDrawingQuads)
                .addXref(2, setColorOpaque_I)
                .addXref(3, addVertexWithUV)
                .addXref(4, draw)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "setup for sky rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        ALOAD_0,
                        reference(GETFIELD, renderEngine),
                        FLOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, getCelestialAngle),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setup", "(LWorld;LRenderEngine;FF)V"))
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch.InsertBefore() {
                @Override
                public String getDescription() {
                    return "render custom sky";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glRotatef(worldObj.getCelestialAngle(par1) * 360F, 1.0F, 0.0F, 0.0F);
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, getCelestialAngle),
                        push(360.0f),
                        FMUL,
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "renderAll", "()V"))
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "disable default stars";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, capture(any()),
                        FLOAD, backReference(1),
                        FLOAD, backReference(1),
                        FLOAD, backReference(1),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V")),

                        ALOAD_0,
                        reference(GETFIELD, glStarList),
                        reference(INVOKESTATIC, glCallList)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(GETSTATIC, active),
                        IFNE, branch("A"),
                        getMatch(),
                        label("A")
                    );
                }
            }.targetMethod(renderSky));

            addCelestialObjectPatch("sun", "/terrain/sun.png");
            addCelestialObjectPatch("moon", "/terrain/moon_phases.png");
        }

        private void addCelestialObjectPatch(final String objName, final String textureName) {
            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "override " + objName + " texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(textureName)
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setupCelestialObject", "(Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }.targetMethod(renderSky));
        }
    }

    private class EffectRendererMod extends ClassMod {
        private static final int ORIG_LAYERS = 4;
        private static final int EXTRA_LAYERS = 1;

        private int layerRegister;

        EffectRendererMod() {
            final ClassRef list = new ClassRef("java/util/List");
            final FieldRef fxLayers = new FieldRef(getDeobfClass(), "fxLayers", "[Ljava/util/List;");
            final FieldRef renderer = new FieldRef(getDeobfClass(), "renderer", "LRenderEngine;");
            final MethodRef renderParticles = new MethodRef(getDeobfClass(), "renderParticles", "(LEntity;F)V");
            final MethodRef addEffect = new MethodRef(getDeobfClass(), "addEffect", "(LEntityFX;)V");
            final MethodRef getFXLayer = new MethodRef("EntityFX", "getFXLayer", "()I");
            final MethodRef getTexture = new MethodRef("RenderEngine", "getTexture", "(Ljava/lang/String;)I");
            final MethodRef glBlendFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBlendFunc", "(II)V");

            addClassSignature(new ConstSignature("/particles.png"));
            addClassSignature(new ConstSignature("/gui/items.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(ORIG_LAYERS),
                        reference(ANEWARRAY, list),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, fxLayers)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push("/particles.png"),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(renderParticles)
                .addXref(1, renderer)
                .addXref(2, getTexture)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE_2

                    );
                }
            }
                .setMethod(addEffect)
                .addXref(1, getFXLayer)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return String.format("increase fx layers from %d to %d", ORIG_LAYERS, ORIG_LAYERS + EXTRA_LAYERS);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        or(
                            build(push(ORIG_LAYERS)),
                            build(push(ORIG_LAYERS - 1))
                        ),
                        lookAhead(or(
                            build(reference(ANEWARRAY, list)),
                            build(IF_ICMPLT_or_IF_ICMPGE, any(2))),
                        true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        push(ORIG_LAYERS + EXTRA_LAYERS)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override entity fx layer";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, getFXLayer)
                    );
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "getFXLayer", "(LEntityFX;)I"))
                    );
                }
            }.targetMethod(addEffect));

            addPatch(new BytecodePatch.InsertAfter() {
                @Override
                public String getDescription() {
                    return "render extra fx layers";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, fxLayers),
                        ILOAD, capture(any()),
                        AALOAD,
                        reference(INVOKEINTERFACE, new InterfaceMethodRef("java/util/List", "isEmpty", "()Z"))
                    );
                }

                @Override
                public byte[] getInsertBytes() throws IOException {
                    layerRegister = getCaptureGroup(1)[0] & 0xff;
                    return buildCode(
                        ILOAD, layerRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "skipThisLayer", "(ZI)Z"))
                    );
                }
            }.targetMethod(renderParticles));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "bind texture for extra fx layers";
                }

                @Override
                public String getMatchExpression() {
                    if (layerRegister > 0) {
                        return buildExpression(
                            // if (layer == 0) {
                            ILOAD, layerRegister,
                            IFNE, any(2),

                            // var9 = this.renderer.getTexture("/particles.png");
                            capture(build(
                                ALOAD_0,
                                reference(GETFIELD, renderer),
                                push("/particles.png"),
                                reference(INVOKEVIRTUAL, getTexture),
                                anyISTORE
                            ))

                            // }
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        // if (layer % 4 == 0) {
                        ILOAD, layerRegister,
                        push(ORIG_LAYERS),
                        IREM,
                        IFNE, branch("A"),

                        // ...
                        getCaptureGroup(1),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderParticles));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override particle blending method";
                }

                @Override
                public String getMatchExpression() {
                    if (layerRegister > 0) {
                        return buildExpression(
                            push(770), // GL_SRC_ALPHA
                            push(771), // GL_ONE_MINUS_SRC_ALPHA
                            reference(INVOKESTATIC, glBlendFunc)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() throws IOException {
                    return buildCode(
                        ILOAD, layerRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "setParticleBlendMethod", "(I)V"))
                    );
                }
            }.targetMethod(renderParticles));
        }
    }

    private class EntityFireworkSparkFXMod extends ClassMod {
        EntityFireworkSparkFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new ConstSignature(0.75f));
            addClassSignature(new ConstSignature(0.9100000262260437));
        }
    }
}
