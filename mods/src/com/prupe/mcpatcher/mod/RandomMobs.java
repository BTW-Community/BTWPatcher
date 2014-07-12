package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class RandomMobs extends Mod {
    private static final String EXTRA_INFO_CLASS = MCPatcherUtils.RANDOM_MOBS_CLASS + "$ExtraInfo";

    private static final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef glDisable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDisable", "(I)V");
    private static final MethodRef glTranslatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTranslatef", "(FFF)V");
    private static final MethodRef glPushMatrix = new MethodRef(MCPatcherUtils.GL11_CLASS, "glPushMatrix", "()V");

    private static final MethodRef randomTexture = new MethodRef(MCPatcherUtils.RANDOM_MOBS_CLASS, "randomTexture", "(LEntity;LResourceLocation;)LResourceLocation;");

    private final boolean haveOverlayRenderer;

    public RandomMobs() {
        name = MCPatcherUtils.RANDOM_MOBS;
        author = "Balthichou";
        description = "Randomize mob skins if texture pack supports it. Based on Balthichou's mod.";
        website = "http://www.minecraftforum.net/topic/244172-";
        version = "1.9";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TESSELLATOR_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        haveOverlayRenderer = getMinecraftVersion().compareTo("14w05a") >= 0;

        ResourceLocationMod.setup(this);
        addClassMod(new NBTTagCompoundMod(this));
        addClassMod(new TessellatorMod(this));
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new EntityMod());
        addClassMod(new EntityLivingBaseMod());
        addClassMod(new RenderMod());
        addClassMod(new RenderLivingEntityMod());
        if (ResourceLocationMod.haveClass()) {
            addClassMod(new RenderLivingMod());
        }
        addClassMod(new RenderMiscMod("Spider", "/mob/spider_eyes.png", "textures/entity/spider_eyes.png"));
        addClassMod(new RenderMiscMod("Enderman", "/mob/enderman_eyes.png", "textures/entity/enderman/enderman_eyes.png"));
        addClassMod(new RenderMiscMod("Sheep", "/mob/sheep_fur.png", "textures/entity/sheep/sheep_fur.png"));
        addClassMod(new RenderMiscMod("Wolf", "/mob/wolf_collar.png", "textures/entity/wolf/wolf_collar.png"));
        addClassMod(new RenderSnowmanMod());
        addClassMod(new RenderMooshroomMod());
        addClassMod(new RenderFishMod());
        if (ResourceLocationMod.haveClass()) {
            addClassMod(new RenderLeashMod());
        }

        addClassFiles("com.prupe.mcpatcher.mob.*");

        TexturePackAPIMod.earlyInitialize(3, MCPatcherUtils.RANDOM_MOBS_CLASS, "init");
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            final FieldRef entityId = new FieldRef(getDeobfClass(), "entityId", "I");
            final FieldRef nextEntityID = new FieldRef(getDeobfClass(), "nextEntityID", "I");

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

    private class EntityLivingBaseMod extends com.prupe.mcpatcher.basemod.EntityLivingBaseMod {
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
            if (ResourceLocationMod.haveClass()) {
                addClassSignature(new ConstSignature("textures/misc/shadow.png"));
                setup16();
            } else {
                addClassSignature(new ConstSignature("/terrain.png"));
                addClassSignature(new ConstSignature("%clamp%/misc/shadow.png"));
            }
            addClassSignature(new ConstSignature(0.45f));
        }

        private void setup16() {
            final MethodRef loadTexture = new MethodRef(getDeobfClass(), "loadTexture", "(LResourceLocation;)V");
            final MethodRef getEntityTexture = new MethodRef(getDeobfClass(), "getEntityTexture", "(LEntity;)LResourceLocation;");

            addMemberMapper(new MethodMapper(loadTexture)
                    // 14w05a+: public
                    // older: protected
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

            addClassSignature(new ConstSignature(180.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0.0f),
                        or(build(
                                // pre-14w04a
                                push(-24.0f),
                                anyFLOAD,
                                FMUL,
                                push(0.0078125f),
                                FSUB
                            ),
                            build(
                                // 14w04a+
                                push(-1.5078125f)
                            )
                        ),
                        push(0.0f),
                        reference(INVOKESTATIC, glTranslatef)
                    );
                }
            }.setMethod(doRenderLiving));

            if (!ResourceLocationMod.haveClass()) {
                setup15();
            }
        }

        private void setup15() {
            final MethodRef getEntityTexture = new MethodRef("EntityLivingBase", "getEntityTexture", "()Ljava/lang/String;");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "replace mob texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(anyALOAD),
                        reference(INVOKEVIRTUAL, getEntityTexture)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        getMatch(),
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, randomTexture),
                        ResourceLocationMod.unwrap(this)
                    );
                }
            });
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

        RenderMiscMod(String mob, final String texture15, final String texture16) {
            this.mob = mob;

            final FieldRef miscSkin;
            if (ResourceLocationMod.haveClass()) {
                miscSkin = new FieldRef(getDeobfClass(), mob.toLowerCase() + "MiscSkin", "LResourceLocation;");

                addClassSignature(new ResourceLocationSignature(this, miscSkin, texture16));
            } else {
                miscSkin = null;

                addClassSignature(new ConstSignature(texture15));
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "randomize " + texture16;
                }

                @Override
                public String getMatchExpression() {
                    if (miscSkin == null) {
                        return buildExpression(
                            push(texture15)
                        );
                    } else if ((getMethodInfo().getAccessFlags() & AccessFlag.STATIC) == 0 &&
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
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, randomTexture),
                        ResourceLocationMod.unwrap(this)
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
            final MethodRef renderEquippedItems;
            final MethodRef renderSnowmanOverlay = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderSnowmanOverlay", "(LEntityLivingBase;)Z");

            if (haveOverlayRenderer) {
                setInterfaces("OverlayRenderer");
                renderEquippedItems = new MethodRef(getDeobfClass(), "render", "(LEntitySnowman;FFFFFFF)V");
            } else {
                setParentClass("RenderLiving");
                renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntitySnowman;F)V");
                if (ResourceLocationMod.haveClass()) {
                    addClassSignature(new ConstSignature("textures/entity/snowman.png"));
                }
            }

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
                        // 14w05b+: this.renderManager.getItemRenderer().renderItem(entity, new ItemStack(BlockList.pumpkin, 1), 0);
                        // older: renderManager.itemRenderer.renderItem(entity, itemstack, 0);
                        ALOAD_0,
                        anyReference(GETFIELD),
                        haveOverlayRenderer ? anyReference(INVOKEVIRTUAL) : "",
                        anyReference(GETFIELD),
                        ALOAD_1,
                        haveOverlayRenderer ?
                            build(
                                anyReference(NEW),
                                DUP,
                                anyReference(GETSTATIC),
                                push(1),
                                anyReference(INVOKESPECIAL)
                            ) :
                            anyALOAD,
                        push(0),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!MobOverlay.renderSnowmanOverlay(entity)) {
                        ALOAD_1,
                        reference(INVOKESTATIC, renderSnowmanOverlay),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderEquippedItems));
        }
    }

    private class RenderMooshroomMod extends ClassMod {
        RenderMooshroomMod() {
            final MethodRef renderEquippedItems;
            final FieldRef mushroomRed = new FieldRef("BlockList", "mushroomRed", "LBlockFlower;");
            final FieldRef blocksAtlas = new FieldRef("TextureAtlas", "blocks", "LResourceLocation;");
            final MethodRef renderBlockAsItem = RenderBlocksMod.haveSubclasses() ? RenderBlockManagerMod.renderBlockAsItem : RenderBlocksMod.renderBlockAsItem;
            final MethodRef setupMooshroom = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "setupMooshroom", "(LEntityLivingBase;LResourceLocation;)LResourceLocation;");
            final MethodRef renderMooshroomOverlay = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "renderMooshroomOverlay", "()Z");
            final MethodRef finishMooshroom = new MethodRef(MCPatcherUtils.MOB_OVERLAY_CLASS, "finishMooshroom", "()V");

            if (haveOverlayRenderer) {
                setInterfaces("OverlayRenderer");
                renderEquippedItems = new MethodRef(getDeobfClass(), "render", "(LEntityMooshroom;FFFFFFF)V");
            } else {
                setParentClass("RenderLiving");
                renderEquippedItems = new MethodRef(getDeobfClass(), "renderEquippedItems1", "(LEntityMooshroom;F)V");
                if (ResourceLocationMod.haveClass()) {
                    addClassSignature(new ConstSignature("textures/entity/cow/mooshroom.png"));
                }
            }

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderEquippedItems);
                    int xref = 1;
                    if (ResourceLocationMod.haveClass()) {
                        addXref(xref++, blocksAtlas);
                    }
                    addXref(xref++, mushroomRed);
                    addXref(xref, renderBlockAsItem);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 14w05b+:  this.entity.loadTexture(TextureAtlas.blocks);
                        // older: this.loadTexture(TextureAtlas.blocks);
                        // 1.5: this.loadTexture("/terrain.png");
                        ALOAD_0,
                        ResourceLocationMod.haveClass() ?
                            build(
                                haveOverlayRenderer ? anyReference(GETFIELD) : "",
                                captureReference(GETSTATIC)
                            ) :
                            build(
                                push("/terrain.png")
                            ),
                        anyReference(INVOKEVIRTUAL),

                        // GL11.glEnable(GL11.GL_CULL_FACE);
                        push(2884),
                        reference(INVOKESTATIC, glEnable),

                        // GL11.glPushMatrix();
                        reference(INVOKESTATIC, glPushMatrix),

                        // ...
                        any(0, 100),

                        // pre-14w04a: this.renderBlocks.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        // 14w04a-14w08a: RenderBlockManager.instance.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        // other: renderBlocks.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        or(
                            build(ALOAD_0, anyReference(GETFIELD)),
                            anyALOAD,
                            anyReference(GETSTATIC)
                        ),
                        captureReference(GETSTATIC),
                        push(0),
                        push(1.0f),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up custom mooshroom overlay";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ResourceLocationMod.haveClass() ?
                            reference(GETSTATIC, blocksAtlas) : push("/terrain.png")
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        getMatch(),
                        ResourceLocationMod.wrap(this),
                        reference(INVOKESTATIC, setupMooshroom),
                        ResourceLocationMod.unwrap(this)
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
                        // pre-14w04a: this.renderBlocks.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        // 14w04a-14w08a: RenderBlockManager.instance.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        // other: renderBlocks.renderBlockAsItem(BlockList.mushroomRed, 0, 1.0f);
                        or(
                            build(ALOAD_0, anyReference(GETFIELD)),
                            anyALOAD,
                            anyReference(GETSTATIC)
                        ),
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
                        reference(INVOKESTATIC, renderMooshroomOverlay),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderEquippedItems));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderEquippedItems);
                }

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
                        reference(INVOKESTATIC, finishMooshroom)
                    );
                }
            });
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
                        // dy = (double)(float) (ay1 - ay0) + by;
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
                            optional(build(anyDLOAD, DADD)), // 14w10a+
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

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/particles.png", "textures/particle/particles.png")));
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
