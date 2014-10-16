package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.ResourceLocationMod;
import com.prupe.mcpatcher.basemod.WorldMod;
import com.prupe.mcpatcher.basemod.WorldProviderMod;
import com.prupe.mcpatcher.basemod.ext18.IBlockStateMod;
import com.prupe.mcpatcher.basemod.ext18.RenderUtilsMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static com.prupe.mcpatcher.mod.cc.CustomColors.*;
import static javassist.bytecode.Opcode.*;

class CC_Entity {
    private static final FieldRef fleeceColorTable = new FieldRef("RenderSheep", "fleeceColorTable", "[[F");
    private static final MethodRef ordinal = new MethodRef("java/lang/Enum", "ordinal", "()I");

    static void setup(Mod mod) {
        mod.addClassMod(new EntityMod(mod));
        mod.addClassMod(new EntityFXMod(mod));
        mod.addClassMod(new EntityRainFXMod(mod));
        mod.addClassMod(new EntityDropParticleFXMod(mod));
        mod.addClassMod(new EntitySplashFXMod(mod));
        mod.addClassMod(new EntityBubbleFXMod(mod));
        mod.addClassMod(new EntitySuspendFXMod(mod));
        mod.addClassMod(new EntityPortalFXMod(mod));
        mod.addClassMod(new EntityAuraFXMod(mod));

        // This patch enables custom potion particle effects around players in SMP.
        // Removed because it causes beacon effect particles to become opaque for some reason.
        //mod.addClassMod(new EntityLivingBaseMod(mod));
        mod.addClassMod(new EntityRendererMod(mod));

        mod.addClassMod(new EntityReddustFXMod(mod));

        mod.addClassMod(new RenderSheepMod(mod));

        mod.addClassMod(new RenderWolfMod(mod));
        mod.addClassMod(new RecipesDyedArmorMod(mod));

        mod.addClassMod(new EntityListMod(mod));

        mod.addClassMod(new RenderXPOrbMod(mod));

        mod.setMALVersion("dyecolor", IBlockStateMod.haveClass() ? 2 : 1);
    }

    private static class EntityMod extends ClassMod {
        EntityMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("Pos"));
            addClassSignature(new ConstSignature("Motion"));
            addClassSignature(new ConstSignature("Rotation"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"));
                    addXref(1, new FieldRef(getDeobfClass(), "posX", "D"));
                    addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"));
                    addXref(3, new FieldRef(getDeobfClass(), "posY", "D"));
                    addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"));
                    addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"));
                    addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "worldObj", "LWorld;")));
        }
    }

    private static class EntityFXMod extends ClassMod {
        EntityFXMod(Mod mod) {
            super(mod);
            setParentClass("Entity");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setSize(0.2f, 0.2f);
                        ALOAD_0,
                        push(0.2f),
                        push(0.2f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // particleRed = particleGreen = particleBlue = 1.0f;
                            ALOAD_0,
                            ALOAD_0,
                            ALOAD_0,
                            FCONST_1,
                            DUP_X1,
                            captureReference(PUTFIELD),
                            DUP_X1,
                            captureReference(PUTFIELD),
                            captureReference(PUTFIELD)
                        );
                    }
                }
                    .matchConstructorOnly(true)
                    .addXref(1, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    .addXref(2, new FieldRef(getDeobfClass(), "particleGreen", "F"))
                    .addXref(3, new FieldRef(getDeobfClass(), "particleRed", "F"))
            );
        }
    }

    abstract private static class WaterFXMod extends ClassMod {
        WaterFXMod(Mod mod) {
            super(mod);
        }

        void addWaterColorPatch(final String name, final boolean includeBaseColor, final float[] particleColors) {
            addWaterColorPatch(name, includeBaseColor, particleColors, particleColors);
        }

        void addWaterColorPatch(final String name, final boolean includeBaseColor, final float[] origColors, final float[] newColors) {
            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef posX = new FieldRef(getDeobfClass(), "posX", "D");
            final FieldRef posY = new FieldRef(getDeobfClass(), "posY", "D");
            final FieldRef posZ = new FieldRef(getDeobfClass(), "posZ", "D");
            final MethodRef computeWaterColor1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeWaterColor", "(ZIII)Z");

            addPatch(new BytecodePatch() {
                {
                    if (origColors == null) {
                        setInsertBefore(true);
                    }
                }

                @Override
                public String getDescription() {
                    return "override " + name + " color";
                }

                @Override
                public String getMatchExpression() {
                    if (origColors == null) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return buildExpression(
                            // particleRed = r;
                            ALOAD_0,
                            push(origColors[0]),
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(origColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(origColors[2]),
                            reference(PUTFIELD, particleBlue)
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeBlock.computeWaterColor(includeBaseColor, (int) this.posX, (int) this.posY, (int) this.posZ)) {
                        push(includeBaseColor),
                        ALOAD_0,
                        reference(GETFIELD, posX),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posY),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posZ),
                        D2I,
                        reference(INVOKESTATIC, computeWaterColor1),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        newColors == null ? new byte[]{} : buildCode(
                            // particleRed = r;
                            ALOAD_0,
                            push(newColors[0]),
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(newColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(newColors[2]),
                            reference(PUTFIELD, particleBlue)
                        ),

                        // }
                        label("B")
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private static class EntityRainFXMod extends WaterFXMod {
        EntityRainFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");

            addClassSignature(new OrSignature(
                new ConstSignature(0.1f),
                new ConstSignature((double) 0.1f) // 14w02a+
            ));

            addClassSignature(new OrSignature(
                new ConstSignature(0.2f),
                new ConstSignature((double) 0.2f) // 14w02a+
            ));

            addClassSignature(new ConstSignature(0.30000001192092896));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (Math.random() < 0.5)
                        reference(INVOKESTATIC, random),
                        push(0.5),
                        DCMPG,
                        IFGE, any(2)
                    );
                }
            });

            addWaterColorPatch("rain drop", false, new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});
        }
    }

    private static class EntityDropParticleFXMod extends WaterFXMod {
        EntityDropParticleFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef timer = new FieldRef(getDeobfClass(), "timer", "I");
            final MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");
            final MethodRef computeLavaDropColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeLavaDropColor", "(I)Z");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(onUpdate);
                    addXref(1, new FieldRef(getDeobfClass(), "timer", "I"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        anyReference(PUTFIELD),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        anyReference(PUTFIELD),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        anyReference(PUTFIELD),

                        // ...
                        any(0, 30),

                        // 40 - age
                        push(40),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ISUB
                    );
                }
            });

            addWaterColorPatch("water drop", true, new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "remove water drop color update";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode();
                }
            }.targetMethod(onUpdate));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lava drop color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 16.0f / (float)((40 - timer) + 16);
                        ALOAD_0,
                        push(16.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(4.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Colorizer.computeLavaDropColor(40 - timer)) {
                        push(40),
                        ALOAD_0,
                        reference(GETFIELD, timer),
                        ISUB,
                        reference(INVOKESTATIC, computeLavaDropColor),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

                        // } else {
                        GOTO, branch("B"),

                        // ... original code ...
                        label("A"),
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(onUpdate));
        }
    }

    private static class EntitySplashFXMod extends WaterFXMod {
        EntitySplashFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityRainFX");

            addClassSignature(new ConstSignature(0.04f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 8,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 10,
                        push(0.10000000000000001),
                        DADD,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 12,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("splash", false, null);
        }
    }

    private static class EntityBubbleFXMod extends WaterFXMod {
        EntityBubbleFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setParticleTextureIndex(32);
                        ALOAD_0,
                        push(32),
                        anyReference(INVOKEVIRTUAL),

                        // setSize(0.02F, 0.02F);
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("bubble", false, new float[]{1.0f, 1.0f, 1.0f});
        }
    }

    private static class EntitySuspendFXMod extends ClassMod {
        EntitySuspendFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

            addClassSignature(new ConstSignature(0.4f));
            addClassSignature(new ConstSignature(0.7f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.01f),
                        push(0.01f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override underwater suspend particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        push(0.7f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.computeSuspendColor(0x6666b2, (int) x, (int) y, (int) z);
                        push(0x6666b2),
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeSuspendColor", "(IIII)V")),

                        // this.particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // this.particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // this.particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private static class EntityPortalFXMod extends ClassMod {
        private final FieldRef portalColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "portalColor", "[F");

        EntityPortalFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleGreen *= 0.3f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.3f),
                        FMUL,
                        PUTFIELD, backReference(1),

                        // particleBlue *= 0.9f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.9f),
                        FMUL,
                        PUTFIELD, backReference(2)
                    );
                }
            }.matchConstructorOnly(true));

            addPortalPatch(0.9f, 0, "red");
            addPortalPatch(0.3f, 1, "green");

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    matchConstructorOnly(true);
                }

                @Override
                public String getDescription() {
                    return "override portal particle color (blue)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETSTATIC, portalColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            });
        }

        private void addPortalPatch(final float origValue, final int index, final String color) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override portal particle color (" + color + ")";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(origValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, portalColor),
                        push(index),
                        FALOAD
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private static class EntityAuraFXMod extends ClassMod {
        EntityAuraFXMod(Mod mod) {
            super(mod);
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final MethodRef computeMyceliumParticleColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeMyceliumParticleColor", "()Z");

            addClassSignature(new ConstSignature(0.019999999552965164));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.setParticleTextureIndex(0);
                        ALOAD_0,
                        push(0),
                        anyReference(INVOKEVIRTUAL),

                        // this.setSize(0.02f, 0.02f);
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "colorize", "()LEntityAuraFX;")) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        reference(INVOKESTATIC, computeMyceliumParticleColor),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

                        label("A"),
                        ALOAD_0,
                        ARETURN
                    );
                }
            });
        }
    }

    private static class EntityLivingBaseMod extends com.prupe.mcpatcher.basemod.EntityLivingBaseMod {
        public EntityLivingBaseMod(Mod mod) {
            super(mod);

            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorld;");
            final FieldRef overridePotionColor = new FieldRef(getDeobfClass(), "overridePotionColor", "I");
            final MethodRef updatePotionEffects = new MethodRef(getDeobfClass(), "updatePotionEffects", "()V");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(600),
                        IREM
                    );
                }
            }.setMethod(updatePotionEffects));

            addPatch(new AddFieldPatch(overridePotionColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 1)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.potionsNeedUpdate) {
                        lookBehind(build(
                            ALOAD_0,
                            GETFIELD, capture(any(2)),
                            IFEQ, any(2)
                        ), true),

                        // if (!this.worldObj.isRemote) {
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        captureReference(GETFIELD),
                        IFNE, any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                    );
                }
            }.targetMethod(updatePotionEffects));

            addPatch(new BytecodePatch() {
                {
                    targetMethod(updatePotionEffects);
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 2)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.updateObject(7, Integer.valueOf(...));
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        capture(any(1, 3)),
                        reference(INVOKESTATIC, integerValueOf),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.overridePotionColor = ...;
                        ALOAD_0,
                        getCaptureGroup(1),
                        reference(PUTFIELD, overridePotionColor)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updatePotionEffects);
                }

                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 3)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.getWatchableObjectInt(7)
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getPotionEffectColor(..., this)
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getPotionEffectColor", "(ILEntityLivingBase;)I"))
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
            final MethodRef computeUnderwaterColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeUnderwaterColor", "()Z");

            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateLightmap);
                    addXref(1, new MethodRef("World", "getSunAngle", "(F)F"));
                    addXref(3, new FieldRef("World", "worldProvider", "LWorldProvider;"));
                    addXref(5, new FieldRef(getDeobfClass(), "torchFlickerX", "F"));
                    addXref(6, WorldMod.getLightningFlashRef());
                    addXref(7, WorldProviderMod.getWorldTypeRef());
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
                        captureReference(WorldMod.getLightningFlashOpcode()),
                        IFLE, any(2),

                        // ...
                        any(0, 300),

                        // older: if (world.worldProvider.worldType == 1) {
                        // 14w02a+: if (world.worldProvider.getWorldType() == 1) {
                        ALOAD_2,
                        backReference(3),
                        captureReference(WorldProviderMod.getWorldTypeOpcode()),
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.LIGHTMAP_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;[IF)Z")),
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
        }
    }

    private static class EntityReddustFXMod extends ClassMod {
        EntityReddustFXMod(Mod mod) {
            super(mod);

            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(INVOKESTATIC, random),
                        push(0.20000000298023224),
                        DMUL,
                        D2F,
                        push(0.8f),
                        FADD,
                        anyFLOAD,
                        FMUL,
                        anyFLOAD,
                        FMUL,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.0f),
                        FSTORE, 9,
                        reference(INVOKESTATIC, random)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(1.0f),
                        FSTORE, 9,

                        push(15),
                        reference(INVOKESTATIC, computeRedstoneWireColor),
                        IFEQ, branch("A"),

                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        FSTORE, 9,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        FSTORE, 10,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        FSTORE, 11,

                        label("A"),
                        reference(INVOKESTATIC, random)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private static class EntityListMod extends ClassMod {
        EntityListMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("Skipping Entity with id "));

            final MethodRef addMapping = new MethodRef(getDeobfClass(), "addMapping", "(Ljava/lang/Class;Ljava/lang/String;III)V");
            final MethodRef setupSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupSpawnerEgg", "(Ljava/lang/String;III)V");

            addMemberMapper(new MethodMapper(addMapping).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up mapping for spawnable entities";
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
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKESTATIC, setupSpawnerEgg)
                    );
                }
            }.targetMethod(addMapping));
        }
    }

    private static class RenderSheepMod extends ClassMod {
        private final MethodRef getFleeceColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getFleeceColor", "([FI)[F");

        RenderSheepMod(Mod mod) {
            super(mod);
            RenderUtilsMod.setup(this);

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            addClassSignature(new ConstSignature("mob.sheep.say"));

            addMemberMapper(new FieldMapper(fleeceColorTable)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override sheep wool color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // RenderSheep.fleeceColorTable[index]
                        reference(GETSTATIC, fleeceColorTable),
                        capture(anyILOAD),
                        AALOAD,

                        getLookAheadExpression(this)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getFleeceColor(..., index)
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getFleeceColor)
                    );
                }
            });
        }

        private void setup18() {
            addClassSignature(new ConstSignature("textures/entity/sheep/sheep_fur.png"));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override sheep wool color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // EntitySheep.getRGB(entity.getFleeceEnum())
                        capture(build(
                            ALOAD_1,
                            anyReference(INVOKEVIRTUAL)
                        )),
                        anyReference(INVOKESTATIC),

                        getLookAheadExpression(this)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getFleeceColor(..., entity.getFleeceEnum().ordinal())
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, ordinal),
                        reference(INVOKESTATIC, getFleeceColor)
                    );
                }
            });
        }

        private String getLookAheadExpression(PatchComponent patchComponent) {
            return lookAhead(build(
                // GL11.glColor3f(rgb[0], rgb[1], rgb[2]);
                anyASTORE,
                anyALOAD,
                patchComponent.push(0),
                FALOAD,
                anyALOAD,
                patchComponent.push(1),
                FALOAD,
                anyALOAD,
                patchComponent.push(2),
                FALOAD,
                RenderUtilsMod.glColor3f(patchComponent)
            ), true);
        }
    }

    private static class RenderWolfMod extends ClassMod {
        private final MethodRef getWolfCollarColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getWolfCollarColor", "([FI)[F");

        RenderWolfMod(Mod mod) {
            super(mod);
            setParentClass("RenderLivingEntity");
            RenderUtilsMod.setup(this);

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/mob/wolf_collar.png", "textures/entity/wolf/wolf_collar.png")));

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override wolf collar colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // EntitySheep.fleeceColorTable[color]
                        reference(GETSTATIC, fleeceColorTable),
                        capture(anyILOAD),
                        AALOAD
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getWolfCollarColor(..., color);
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getWolfCollarColor)
                    );
                }
            });
        }

        private void setup18() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override wolf collar colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // rgb = EntitySheep.getRGB(dyeColorEnum);
                        capture(build(
                            capture(anyALOAD),
                            anyReference(INVOKESTATIC)
                        )),

                        // GL11.glColor3f(rgb[0], rgb[1], rgb[2]);
                        lookAhead(build(
                            capture(anyASTORE),
                            capture(anyALOAD),
                            push(0),
                            FALOAD,
                            backReference(4),
                            push(1),
                            FALOAD,
                            backReference(4),
                            push(2),
                            FALOAD,
                            RenderUtilsMod.glColor3f(this)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // rgb = ColorizeEntity.getWolfCollarColor(EntitySheep.getRGB(dyeColorEnum), dyeColorEnum.ordinal())
                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        reference(INVOKEVIRTUAL, ordinal),
                        reference(INVOKESTATIC, getWolfCollarColor)
                    );
                }
            });
        }
    }

    private static class RecipesDyedArmorMod extends ClassMod {
        private final MethodRef getArmorDyeColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getArmorDyeColor", "([FI)[F");

        RecipesDyedArmorMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature(255.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // var7 = (int)((float)var7 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, capture(any()),
                        FMUL,
                        FLOAD, capture(any()),
                        FDIV,
                        F2I,
                        ISTORE, backReference(1),

                        // var8 = (int)((float)var8 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(4),

                        // var9 = (int)((float)var9 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(5)
                    );
                }
            });

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override armor dye colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // EntitySheep.fleeceColorTable[BlockColored.invertIndex(itemStack.getItemDamage())]
                        reference(GETSTATIC, fleeceColorTable),
                        capture(build(
                            anyALOAD,
                            anyReference(INVOKEVIRTUAL),
                            optional(anyReference(INVOKESTATIC))
                        )),
                        AALOAD,

                        getLookAheadExpression(this)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getArmorDyeColor(..., BlockColored.invertIndex(itemStack.getItemDamage()))
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getArmorDyeColor)
                    );
                }
            });
        }

        private void setup18() {
            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override armor dye colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // EntitySheep.getRGB(EnumDyeColor.getArmorColor(itemStack.getItemDamage()));
                        capture(build(
                            anyALOAD,
                            anyReference(INVOKEVIRTUAL),
                            anyReference(INVOKESTATIC)
                        )),
                        anyReference(INVOKESTATIC),

                        getLookAheadExpression(this)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getArmorDyeColor(..., EnumDyeColor.getArmorColor(itemStack.getItemDamage()).ordinal())
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, ordinal),
                        reference(INVOKESTATIC, getArmorDyeColor)
                    );
                }
            });
        }

        private String getLookAheadExpression(PatchComponent patchComponent) {
            return lookAhead(build(
                // (int) (rgb[0] * 255.0f)
                anyASTORE,
                anyALOAD,
                patchComponent.push(0),
                FALOAD,
                patchComponent.push(255.0f),
                FMUL,
                F2I
            ), true);
        }
    }

    private static class RenderXPOrbMod extends ClassMod {
        RenderXPOrbMod(Mod mod) {
            super(mod);

            final MethodRef colorizeXPOrb = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "colorizeXPOrb", "(IF)I");

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/item/xporb.png", "textures/entity/experience_orb.png")));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override xp orb color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        lookBehind(build(
                            // MathHelper.sin(f8 + 0.0F)
                            capture(anyFLOAD),
                            push(0.0f),
                            FADD,
                            anyReference(INVOKESTATIC),

                            // ...
                            any(0, 200)
                        ), true),

                        // tessellator.setColorRGBA_I(i1, 128);
                        capture(anyILOAD),
                        lookAhead(build(
                            push(128),
                            anyReference(INVOKEVIRTUAL)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(2),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeXPOrb)
                    );
                }
            });
        }
    }
}
