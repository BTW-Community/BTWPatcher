package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
        version = "1.7";

        configPanel = new ConfigPanel();

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TESSELLATOR_API_MOD);

        addClassMod(new MinecraftMod(this).mapWorldClient());
        ResourceLocationMod.setup(this);
        RenderUtilsMod.setup(this);
        addClassMod(new TessellatorMod(this));
        addClassMod(new WorldMod());
        addClassMod(new WorldProviderMod(this));
        addClassMod(new WorldClientMod(this));
        addClassMod(new RenderGlobalMod());

        addClassMod(new EffectRendererMod());
        addClassMod(new EntityFireworkSparkFXMod());
        addClassMod(new EntityFireworkOverlayFXMod());

        addClassFiles("com.prupe.mcpatcher.sky.*");
    }

    private static class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox skyCheckBox;
        private JCheckBox unloadTexturesCheckBox;
        private JCheckBox fireworksCheckBox;
        private JSpinner horizonSpinner;

        ConfigPanel() {
            skyCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.BETTER_SKIES, "skybox", skyCheckBox.isSelected());
                }
            });

            unloadTexturesCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.BETTER_SKIES, "unloadTextures", unloadTexturesCheckBox.isSelected());
                }
            });

            fireworksCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", fireworksCheckBox.isSelected());
                }
            });

            horizonSpinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int value = 16;
                    try {
                        value = Integer.parseInt(horizonSpinner.getValue().toString());
                        value = Math.min(Math.max(-99, value), 99);
                    } catch (NumberFormatException e1) {
                    }
                    Config.set(MCPatcherUtils.BETTER_SKIES, "horizon", value);
                    horizonSpinner.setValue(value);
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
            unloadTexturesCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.BETTER_SKIES, "unloadTextures", true));
            fireworksCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.BETTER_SKIES, "brightenFireworks", true));
            horizonSpinner.setValue(Config.getInt(MCPatcherUtils.BETTER_SKIES, "horizon", 16));
        }

        @Override
        public void save() {
        }
    }

    private class WorldMod extends com.prupe.mcpatcher.basemod.WorldMod {
        WorldMod() {
            super(BetterSkies.this);

            final MethodRef getWorldTime = new MethodRef(getDeobfClass(), "getWorldTime", "()J");

            addMemberMapper(new MethodMapper(null, null, getWorldTime));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        private final MethodRef renderSky = new MethodRef(getDeobfClass(), "renderSky", "(F" + (IconMod.haveClass() ? "" : "I") + ")V");

        RenderGlobalMod() {
            final MethodRef getCelestialAngle = new MethodRef("WorldClient", "getCelestialAngle", "(F)F");
            final MethodRef getRainStrength = new MethodRef("World", "getRainStrength", "(F)F");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef worldProvider = new FieldRef("World", "worldProvider", "LWorldProvider;");
            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorldClient;");
            final FieldRef glSkyList = new FieldRef(getDeobfClass(), "glSkyList", "I");
            final FieldRef glSkyList2 = new FieldRef(getDeobfClass(), "glSkyList2", "I");
            final FieldRef glStarList = new FieldRef(getDeobfClass(), "glStarList", "I");
            final FieldRef active = new FieldRef(MCPatcherUtils.SKY_RENDERER_CLASS, "active", "Z");
            final FieldRef horizonHeight = new FieldRef(MCPatcherUtils.SKY_RENDERER_CLASS, "horizonHeight", "D");
            final MethodRef setupSky = new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setup", "(LWorld;FF)V");
            final MethodRef renderAllSky = new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "renderAll", "()V");

            RenderUtilsMod.setup(this);

            if (IconMod.haveClass()) {
                addClassSignature(new ConstSignature("smoke"));
            }
            addClassSignature(new ConstSignature(ResourceLocationMod.select(
                "/environment/clouds.png",
                "textures/environment/clouds.png"
            )));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderSky);
                    addXref(1, mc);
                    addXref(2, worldProvider);
                    addXref(3, WorldProviderMod.getWorldTypeRef());
                    addXref(4, worldObj);
                    addXref(5, getRainStrength);
                    addXref(6, getCelestialAngle);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.mc.theWorld.worldProvider.worldType == 1
                        // 14w02a+: this.mc.theWorld.worldProvider.getWorldType() == 1
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(3),
                        captureReference(GETFIELD),
                        captureReference(WorldProviderMod.getWorldTypeOpcode()),
                        push(1),

                        // ...
                        any(0, 1000),

                        // d = 1.0f - this.worldObj.getRainStrength(par1);
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

                        // GL11.glRotatef(this.worldObj.getCelestialAngle(par1) * 360.0f, 1.0f, 0.0f, 0.0f);
                        ALOAD_0,
                        backReference(4),
                        FLOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        push(360.0f),
                        FMUL,
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        RenderUtilsMod.glRotatef(this)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            ALOAD_0,
                            captureReference(GETFIELD),
                            RenderUtilsMod.glCallList(this),

                            nonGreedy(any(0, 1000)),

                            ALOAD_0,
                            captureReference(GETFIELD),
                            RenderUtilsMod.glCallList(this),

                            nonGreedy(any(0, 1000)),

                            ALOAD_0,
                            captureReference(GETFIELD),
                            RenderUtilsMod.glCallList(this)
                        );
                    }
                }
                    .setMethod(renderSky)
                    .addXref(1, glSkyList)
                    .addXref(2, glStarList)
                    .addXref(3, glSkyList2)
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
                        FLOAD_1,
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        FLOAD_1,
                        reference(INVOKEVIRTUAL, getCelestialAngle),
                        reference(INVOKESTATIC, setupSky)
                    );
                }
            }.targetMethod(renderSky));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderSky);
                }

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
                        RenderUtilsMod.glRotatef(this)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(INVOKESTATIC, renderAllSky)
                    );
                }
            });

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
                        RenderUtilsMod.glColor4f(this),

                        ALOAD_0,
                        reference(GETFIELD, glStarList),
                        RenderUtilsMod.glCallList(this)
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

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override horizon position";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glTranslatef(0.0F, -((float) (horizon - 16.0)), 0.0F);
                        capture(build(
                            push(0.0f),
                            anyDLOAD
                        )),
                        push(16.0),
                        capture(build(
                            DSUB,
                            D2F,
                            FNEG,
                            push(0.0f),
                            RenderUtilsMod.glTranslatef(this)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // GL11.glTranslatef(..., (horizon - horizonHeight), ...);
                        getCaptureGroup(1),
                        reference(GETSTATIC, horizonHeight),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(renderSky));

            addCelestialObjectPatch("sun", "sun.png");
            addCelestialObjectPatch("moon", "moon_phases.png");
        }

        private void addCelestialObjectPatch(final String objName, final String textureName) {
            final MethodRef setupCelestialObject = new MethodRef(MCPatcherUtils.SKY_RENDERER_CLASS, "setupCelestialObject", "(LResourceLocation;)LResourceLocation;");
            final FieldRef textureField;
            if (ResourceLocationMod.haveClass()) {
                textureField = new FieldRef(getDeobfClass(), objName, "LResourceLocation;");

                addClassSignature(new ResourceLocationSignature(this, textureField, "textures/environment/" + textureName));
            } else {
                textureField = null;
            }

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderSky);
                }

                @Override
                public String getDescription() {
                    return "override " + objName + " texture";
                }

                @Override
                public String getMatchExpression() {
                    if (textureField == null) {
                        return buildExpression(
                            push("/environment/" + textureName)
                        );
                    } else {
                        return buildExpression(
                            reference(GETSTATIC, textureField)
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, setupCelestialObject),
                        ResourceLocationMod.unwrap(this)
                    );
                }
            });
        }
    }

    private class EffectRendererMod extends ClassMod {
        private static final int ORIG_LAYERS = 4;
        private static final int EXTRA_LAYERS = 1;

        private int layerRegister;
        private int innerRegister;

        EffectRendererMod() {
            final boolean have2dArray = getMinecraftVersion().compareTo("14w25a") >= 0;
            final ClassRef list = new ClassRef((have2dArray ? "[" : "") + "Ljava/util/List;");
            final FieldRef fxLayers = new FieldRef(getDeobfClass(), "fxLayers", "[" + list.getClassName());
            final MethodRef renderParticles = new MethodRef(getDeobfClass(), "renderParticles", "(LEntity;F)V");
            final MethodRef addEffect = new MethodRef(getDeobfClass(), "addEffect", "(LEntityFX;)V");
            final MethodRef getFXLayer = new MethodRef("EntityFX", "getFXLayer", "()I");
            final InterfaceMethodRef listIsEmpty = new InterfaceMethodRef("java/util/List", "isEmpty", "()Z");
            final MethodRef newGetFXLayer = new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "getFXLayer", "(LEntityFX;)I");
            final MethodRef setParticleBlendMethod = new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "setParticleBlendMethod", "(II)V");
            final MethodRef skipThisLayer = new MethodRef(MCPatcherUtils.FIREWORKS_HELPER_CLASS, "skipThisLayer", "(ZI)Z");

            RenderUtilsMod.setup(this);

            if (ResourceLocationMod.haveClass()) {
                addClassSignature(new ConstSignature("textures/particle/particles.png"));
            } else {
                addClassSignature(new ConstSignature("/particles.png"));
                addClassSignature(new ConstSignature("/gui/items.png"));
            }

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, fxLayers);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(ORIG_LAYERS),
                        reference(ANEWARRAY, list),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(516),
                        push(0.003921569f),
                        RenderUtilsMod.glAlphaFunc(this)
                    );
                }
            }.setMethod(renderParticles));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(addEffect);
                    addXref(1, getFXLayer);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        ISTORE_2

                    );
                }
            });

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
                            true
                        )
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
                        reference(INVOKESTATIC, newGetFXLayer)
                    );
                }
            }.targetMethod(addEffect));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderParticles);
                }

                @Override
                public String getDescription() {
                    return "render extra fx layers";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.fxLayers[layer]([inner]).isEmpty()
                        ALOAD_0,
                        reference(GETFIELD, fxLayers),
                        capture(anyILOAD),
                        AALOAD,
                        have2dArray ? build(capture(anyILOAD), AALOAD) : "",
                        reference(INVOKEINTERFACE, listIsEmpty)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    layerRegister = extractRegisterNum(getCaptureGroup(1));
                    if (have2dArray) {
                        innerRegister = extractRegisterNum(getCaptureGroup(2));
                    }
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, skipThisLayer)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    if (have2dArray) {
                        setInsertAfter(true);
                    }
                }

                @Override
                public String getDescription() {
                    return "override particle blending method";
                }

                @Override
                public String getMatchExpression() {
                    if (layerRegister <= 0) {
                        return null;
                    } else if (have2dArray) {
                        return buildExpression(
                            push(1.0f),
                            push(1.0f),
                            push(1.0f),
                            push(1.0f),
                            RenderUtilsMod.glColor4f(this)
                        );
                    } else {
                        return buildExpression(
                            push(770), // GL_SRC_ALPHA
                            push(771), // GL_ONE_MINUS_SRC_ALPHA
                            RenderUtilsMod.glBlendFunc(this)
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ILOAD, layerRegister,
                        have2dArray ? buildCode(ILOAD, innerRegister) : buildCode(push(0)),
                        reference(INVOKESTATIC, setParticleBlendMethod)
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
