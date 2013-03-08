package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BetterSkies extends Mod {
    public BetterSkies() {
        name = MCPatcherUtils.BETTER_SKIES;
        author = "MCPatcher";
        description = "Adds support for custom skyboxes.";
        version = "1.3";

        configPanel = new ConfigPanel();

        addDependency(BaseTexturePackMod.NAME);

        addClassMod(new BaseMod.MinecraftMod().mapWorldClient());
        addClassMod(new WorldMod());
        addClassMod(new BaseMod.WorldClientMod());
        addClassMod(new RenderGlobalMod());

        addClassMod(new EffectRendererMod());
        addClassMod(new EntityFireworkSparkFXMod());
        addClassMod(new EntityFireworkOverlayFXMod());

        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS);
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$1");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$WorldEntry");
        addClassFile(MCPatcherUtils.SKY_RENDERER_CLASS + "$Layer");
        addClassFile(MCPatcherUtils.FIREWORKS_HELPER_CLASS);
    }

    private static class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox skyCheckBox;
        private JCheckBox fireworksCheckBox;

        ConfigPanel() {
            skyCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.BETTER_SKIES, "skybox", skyCheckBox.isSelected());
                }
            });

            fireworksCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", fireworksCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            skyCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.BETTER_SKIES, "skybox", true));
            fireworksCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", true));
        }

        @Override
        public void save() {
        }
    }

    private class WorldMod extends BaseMod.WorldMod {
        WorldMod() {
            final MethodRef getWorldTime = new MethodRef(getDeobfClass(), "getWorldTime", "()J");

            addMemberMapper(new MethodMapper(null, null, getWorldTime));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        private final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F)V");

        RenderGlobalMod() {
            final MethodRef getCelestialAngle = new MethodRef("WorldClient", "getCelestialAngle", "(F)F");
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
            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorldClient;");
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

                        // renderEngine....("/misc/tunnel.png");
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(0, 6),
                        push("/misc/tunnel.png"),
                        any(0, 8),

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
                        backReference(6),
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
                .addXref(5, tessellator)
                .addXref(6, worldObj)
                .addXref(7, getRainStrength)
                .addXref(8, getCelestialAngle)
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
                public byte[] getReplacementBytes() {
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

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "renderAll", "()V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderSky)
            );

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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, active),
                        IFNE, branch("A"),
                        getMatch(),
                        label("A")
                    );
                }
            }.targetMethod(renderSky));

            addCelestialObjectPatch("sun", "sun.png");
            addCelestialObjectPatch("moon", "moon_phases.png");
        }

        private void addCelestialObjectPatch(final String objName, final String textureName) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + objName + " texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("/environment/" + textureName)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setupCelestialObject", "(Ljava/lang/String;)Ljava/lang/String;"))
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderSky)
            );
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
            final MethodRef bindTexture = new MethodRef("RenderEngine", "bindTexture", "(Ljava/lang/String;)V");
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
                .addXref(2, bindTexture)
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
                public byte[] getReplacementBytes() {
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
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "getFXLayer", "(LEntityFX;)I"))
                    );
                }
            }.targetMethod(addEffect));

            addPatch(new BytecodePatch() {
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
                public byte[] getReplacementBytes() {
                    layerRegister = getCaptureGroup(1)[0] & 0xff;
                    return buildCode(
                        ILOAD, layerRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "skipThisLayer", "(ZI)Z"))
                    );
                }
            }
                .setInsertAfter(true)
                .targetMethod(renderParticles)
            );

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
                public byte[] getReplacementBytes() {
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

    private class EntityFireworkOverlayFXMod extends ClassMod {
        EntityFireworkOverlayFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new ConstSignature(7.1f));
            addClassSignature(new ConstSignature(0.6f));
        }
    }
}