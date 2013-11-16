package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.BaseTexturePackMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    private static final String EXTRA_INFO_CLASS = MCPatcherUtils.RANDOM_MOBS_CLASS + "$ExtraInfo";

    private static final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef glDisable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDisable", "(I)V");

    public RandomMobs() {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins if texture pack supports it. Based on Balthichou's mod.";
        website = "http://www.minecraftforum.net/topic/244172-";
        version = "1.7";

        addDependency(MCPatcherUtils.BASE_TEXTURE_PACK_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        addClassMod(new BaseMod.ResourceLocationMod(this));
        addClassMod(new BaseMod.NBTTagCompoundMod(this));
        addClassMod(new BaseMod.TessellatorMod(this));
        addClassMod(new EntityMod());
        addClassMod(new EntityLivingBaseMod());
        addClassMod(new RenderMod());
        addClassMod(new RenderLivingEntityMod());
        addClassMod(new RenderLivingMod());
        addClassMod(new RenderMiscMod("Spider", "textures/entity/spider_eyes.png"));
        addClassMod(new RenderMiscMod("Enderman", "textures/entity/enderman/enderman_eyes.png"));
        addClassMod(new RenderMiscMod("Sheep", "textures/entity/sheep/sheep_fur.png"));
        addClassMod(new RenderMiscMod("Wolf", "textures/entity/wolf/wolf_collar.png"));
        addClassMod(new RenderSnowmanMod());
        addClassMod(new RenderMooshroomMod());
        addClassMod(new RenderFishMod());
        addClassMod(new RenderLeashMod());

        addClassFile(MCPatcherUtils.RANDOM_MOBS_CLASS);
        addClassFile(MCPatcherUtils.RANDOM_MOBS_CLASS + "$1");
        addClassFile(EXTRA_INFO_CLASS);
        addClassFile(MCPatcherUtils.MOB_RULE_LIST_CLASS);
        addClassFile(MCPatcherUtils.MOB_RULE_LIST_CLASS + "$MobRuleEntry");
        addClassFile(MCPatcherUtils.MOB_OVERLAY_CLASS);
        addClassFile(MCPatcherUtils.LINE_RENDERER_CLASS);

        BaseTexturePackMod.earlyInitialize(3, MCPatcherUtils.RANDOM_MOBS_CLASS, "init");
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            final FieldRef entityId = new FieldRef(getDeobfClass(), "entityId", "I");
            final FieldRef nextEntityID = new FieldRef(getDeobfClass(), "nextEntityID", "I");

            addClassSignature(new ConstSignature("Pos"));
            addClassSignature(new ConstSignature("Motion"));
            addClassSignature(new ConstSignature("Rotation"));

            addClassSignature(new BytecodeSignature() {
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
            }
                .setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"))
                .addXref(1, new FieldRef(getDeobfClass(), "posX", "D"))
                .addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"))
                .addXref(3, new FieldRef(getDeobfClass(), "posY", "D"))
                .addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"))
                .addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"))
                .addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.entityId = Entity.nextEntityID++;
                        ALOAD_0,
                        capture(build(GETSTATIC, capture(any(2)))),
                        DUP,
                        push(1),
                        IADD,
                        PUTSTATIC, backReference(2),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, nextEntityID)
                .addXref(3, entityId)
            );

            addPatch(new MakeMemberPublicPatch(entityId));
        }
    }

    private class EntityLivingBaseMod extends BaseMod.EntityLivingBaseMod {
        EntityLivingBaseMod() {
            super(RandomMobs.this);

            final MethodRef getEntityTexture = new MethodRef(getDeobfClass(), "getEntityTexture", "()Ljava/lang/String;");
            final MethodRef writeToNBT = new MethodRef(getDeobfClass(), "writeToNBT", "(LNBTTagCompound;)V");
            final MethodRef readFromNBT = new MethodRef(getDeobfClass(), "readFromNBT", "(LNBTTagCompound;)V");

            addMemberMapper(new MethodMapper(getEntityTexture));
            addMemberMapper(new MethodMapper(writeToNBT, readFromNBT)
                .accessFlag(AccessFlag.PUBLIC, true)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "write skin to nbt";
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
                        // MobRandomizer.ExtraInfo.writeToNBT(this, nbttagcompound);
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(EXTRA_INFO_CLASS, "writeToNBT", "(LEntityLivingBase;LNBTTagCompound;)V"))
                    );
                }
            }.targetMethod(writeToNBT));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "read skin from nbt";
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
                        // MobRandomizer.ExtraInfo.readFromNBT(this, nbttagcompound);
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(EXTRA_INFO_CLASS, "readFromNBT", "(LEntityLivingBase;LNBTTagCompound;)V"))
                    );
                }
            }.targetMethod(readFromNBT));
        }
    }

    private class RenderMod extends ClassMod {
        RenderMod() {
            addClassSignature(new ConstSignature("textures/misc/shadow.png"));
            addClassSignature(new ConstSignature(0.45f));

            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(LResourceLocation;)V");
            final MethodRef getEntityTexture = new MethodRef(getDeobfClass(), "getEntityTexture", "(LEntity;)LResourceLocation;");
            final MethodRef randomTexture = new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntity;LResourceLocation;)LResourceLocation;");

            addMemberMapper(new MethodMapper(loadTexture)
                .accessFlag(AccessFlag.PROTECTED, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            addMemberMapper(new MethodMapper(getEntityTexture)
                .accessFlag(AccessFlag.PROTECTED, true)
                .accessFlag(AccessFlag.STATIC, false)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace mob texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getEntityTexture)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        getMatch(),
                        reference(INVOKESTATIC, randomTexture)
                    );
                }
            });
        }
    }

    private class RenderLivingEntityMod extends ClassMod {
        RenderLivingEntityMod() {
            setParentClass("Render");

            final MethodRef doRenderLiving = new MethodRef(getDeobfClass(), "doRenderLiving", "(LEntityLivingBase;DDDFF)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");

            addClassSignature(new ConstSignature(180.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FCONST_0,
                        push(-24.0f),
                        anyFLOAD,
                        FMUL,
                        push(0.0078125f),
                        FSUB,
                        FCONST_0,
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }
            }.setMethod(doRenderLiving));
        }
    }

    private class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            setParentClass("RenderLivingEntity");

            addClassSignature(new ConstSignature(1.6));
            addClassSignature(new ConstSignature(0.5));
            addClassSignature(new ConstSignature(0.7));
            addClassSignature(new ConstSignature(0.25));
        }
    }

    private class RenderMiscMod extends ClassMod {
        private final String mob;

        RenderMiscMod(String mob, final String texture) {
            this.mob = mob;

            final FieldRef miscSkin = new FieldRef(getDeobfClass(), mob.toLowerCase() + "MiscSkin", "LResourceLocation;");
            final MethodRef randomTexture = new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntityLivingBase;LResourceLocation;)LResourceLocation;");

            addClassSignature(new BaseMod.ResourceLocationSignature(this, miscSkin, texture));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "randomize " + texture;
                }

                @Override
                public String getMatchExpression() {
                    if ((getMethodInfo().getAccessFlags() & AccessFlag.STATIC) == 0 &&
                        getMethodInfo().getDescriptor().startsWith("(L")) {
                        return buildExpression(
                            reference(GETSTATIC, miscSkin)
                        );
                    } else {
                        return null;
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        getMatch(),
                        reference(INVOKESTATIC, randomTexture)
                    );
                }
            });
        }

        @Override
        public String getDeobfClass() {
            return "Render" + mob;
        }
    }

    private class RenderSnowmanMod extends ClassMod {
        RenderSnowmanMod() {
            setParentClass("RenderLiving");

            final MethodRef renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntitySnowman;F)V");
            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(LResourceLocation;)V");
            final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
            final FieldRef snowmanOverlayTexture = new FieldRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "snowmanOverlayTexture", "LResourceLocation;");
            final MethodRef setupSnowman = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupSnowman", "(LEntityLivingBase;)Z");
            final MethodRef renderSnowmanOverlay = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderSnowmanOverlay", "()V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f = 0.625f;
                        push(0.625f),
                        anyFSTORE,

                        // GL11.glTranslatef(0.0f, -0.34375f, 0.0f);
                        push(0.0f),
                        push(-0.34375f),
                        push(0.0f),
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }
            }.setMethod(renderEquippedItems));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render snowman overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderManager.itemRenderer.renderItem(par1EntitySnowman, itemstack, 0);
                        ALOAD_0,
                        anyReference(GETFIELD),
                        anyReference(GETFIELD),
                        ALOAD_1,
                        ALOAD_3,
                        ICONST_0,
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (setupSnowman(entityLiving)) {
                        ALOAD_1,
                        reference(INVOKESTATIC, setupSnowman),
                        IFEQ, branch("A"),

                        // loadTexture(MobOverlay.snowmanOverlayTexture);
                        ALOAD_0,
                        reference(GETSTATIC, snowmanOverlayTexture),
                        reference(INVOKEVIRTUAL, loadTexture),

                        // MobOverlay.renderSnowmanOverlay();
                        reference(INVOKESTATIC, renderSnowmanOverlay),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(renderEquippedItems));
        }
    }

    private class RenderMooshroomMod extends ClassMod {
        RenderMooshroomMod() {
            setParentClass("RenderLiving");

            final FieldRef renderBlocks = new FieldRef(getDeobfClass(), "renderBlocks", "LRenderBlocks;");
            final FieldRef mushroomRed = new FieldRef("BlockList", "mushroomRed", "LBlockFlower;");
            final FieldRef blocksAtlas = new FieldRef("TextureAtlas", "blocks", "LResourceLocation;");
            final MethodRef renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntityMooshroom;F)V");
            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(LResourceLocation;)V");
            final MethodRef glPushMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPushMatrix", "()V");
            final MethodRef renderBlockAsItem = new MethodRef("RenderBlocks", "renderBlockAsItem", "(LBlock;IF)V");

            addClassSignature(new ConstSignature("textures/entity/cow/mooshroom.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // loadTexture(TextureAtlas.blocks);
                        ALOAD_0,
                        captureReference(GETSTATIC),
                        captureReference(INVOKEVIRTUAL),

                        // GL11.glEnable(GL11.GL_CULL_FACE);
                        push(2884),
                        reference(INVOKESTATIC, glEnable),

                        // GL11.glPushMatrix();
                        reference(INVOKESTATIC, glPushMatrix),

                        // ...
                        any(0, 100),

                        // renderBlocks.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETSTATIC),
                        push(0),
                        push(1.0f),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(renderEquippedItems)
                .addXref(1, blocksAtlas)
                .addXref(2, loadTexture)
                .addXref(3, renderBlocks)
                .addXref(4, mushroomRed)
                .addXref(5, renderBlockAsItem)
            );

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up custom mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, blocksAtlas)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        getMatch(),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupMooshroom", "(LEntityLivingBase;LResourceLocation;)LResourceLocation;"))
                    );
                }
            }.targetMethod(renderEquippedItems));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // renderBlocks.renderBlockAsItem(Block.mushroomRed, 0, 1.0f);
                        ALOAD_0,
                        reference(GETFIELD, renderBlocks),
                        reference(GETSTATIC, mushroomRed),
                        push(0),
                        push(1.0f),
                        reference(INVOKEVIRTUAL, renderBlockAsItem)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!MobOverlay.renderMooshroomOverlay()) {
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderMooshroomOverlay", "()Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderEquippedItems));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "finish mooshroom overlay";
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
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "finishMooshroom", "()V"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(renderEquippedItems)
            );
        }
    }

    abstract private class RenderLineMod extends ClassMod {
        RenderLineMod(final String desc, final int type) {
            final MethodRef renderMethod = getRenderMethod();

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(anyFLOAD),
                        backReference(1),
                        FMUL,
                        backReference(1),
                        FADD
                    );
                }
            }.setMethod(renderMethod));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override " + desc + " rendering";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // dx = (double)(float) (ax1 - ax0);
                        // dy = (double)(float) (ay1 - ay0);
                        // dz = (double)(float) (az1 - az0);
                        lookBehind(build(
                            DLOAD, any(),
                            DLOAD, any(),
                            DSUB,
                            optional(build(D2F, F2D)),
                            DSTORE, capture(any()),

                            DLOAD, any(),
                            DLOAD, any(),
                            DSUB,
                            optional(build(D2F, F2D)),
                            DSTORE, capture(any()),

                            DLOAD, any(),
                            DLOAD, any(),
                            DSUB,
                            optional(build(D2F, F2D)),
                            DSTORE, capture(any())
                        ), true),

                        // GL11.glDisable(GL11.GL_TEXTURE_2D);
                        push(3553),
                        reference(INVOKESTATIC, glDisable),

                        // ...
                        any(0, 1000),

                        // GL11.glEnable(GL11.GL_TEXTURE_2D);
                        push(3553),
                        reference(INVOKESTATIC, glEnable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!LineRenderer.renderLine(type, x, y, z, dx, dy, dz)) {
                        push(type),
                        DLOAD_2,
                        DLOAD, 4,
                        DLOAD, 6,
                        DLOAD, getCaptureGroup(1),
                        DLOAD, getCaptureGroup(2),
                        DLOAD, getCaptureGroup(3),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.LINE_RENDERER_CLASS, "renderLine", "(IDDDDDD)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderMethod));
        }

        abstract MethodRef getRenderMethod();
    }

    private class RenderFishMod extends RenderLineMod {
        RenderFishMod() {
            super("fishing line", 0);

            setParentClass("Render");

            addClassSignature(new ConstSignature("textures/particle/particles.png"));
            addClassSignature(new ConstSignature(3.1415927f));
            addClassSignature(new ConstSignature(180.0f));
        }

        @Override
        MethodRef getRenderMethod() {
            return new MethodRef(getDeobfClass(), "renderFishingLine", "(LEntityFishHook;DDDFF)V");
        }
    }

    private class RenderLeashMod extends RenderLineMod {
        RenderLeashMod() {
            super("leash", 1);

            setParentClass("RenderLivingEntity");

            addClassSignature(new ConstSignature(0.01745329238474369));
            addClassSignature(new ConstSignature(1.5707963267948966));
        }

        @Override
        MethodRef getRenderMethod() {
            return new MethodRef(getDeobfClass(), "renderLeash", "(LEntityLiving;DDDFF)V");
        }
    }
}
