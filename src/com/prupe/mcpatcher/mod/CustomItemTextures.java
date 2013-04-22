package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomItemTextures extends Mod {
    private static final String GLINT_PNG = "%blur%/misc/glint.png";
    private static final MethodRef glDepthFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthFunc", "(I)V");
    private static final MethodRef getEntityItem = new MethodRef("EntityItem", "getEntityItem", "()LItemStack;");
    private static final MethodRef hasEffect = new MethodRef("ItemStack", "hasEffect", "()Z");

    private static boolean haveEntityLivingSubclass;
    private static final String entityLivingSubclass = "EntityLivingSub";

    public CustomItemTextures() {
        name = MCPatcherUtils.CUSTOM_ITEM_TEXTURES;
        author = "MCPatcher";
        description = "Enables support for custom item textures, enchantments, and armor.";
        version = "0.2";

        addDependency(BaseTexturePackMod.NAME);

        addClassMod(new BaseMod.TessellatorMod());

        setupMod(this);
    }

    static void setupMod(Mod mod) {
        haveEntityLivingSubclass = getMinecraftVersion().compareTo("13w16a") >= 0;

        mod.addClassMod(new BaseMod.NBTTagCompoundMod().mapGetTagList());
        mod.addClassMod(new BaseMod.NBTTagListMod());
        mod.addClassMod(new ItemMod());
        mod.addClassMod(new ItemStackMod());
        mod.addClassMod(new EntityItemMod());
        mod.addClassMod(new ItemRendererMod());
        mod.addClassMod(new RenderItemMod());
        mod.addClassMod(new RenderLivingMod());
        mod.addClassMod(new RenderBipedMod());
        mod.addClassMod(new RenderArmorMod());
        mod.addClassMod(new EntityLivingMod());
        mod.addClassMod(new EntityLivingSubMod());
        mod.addClassMod(new EntityPlayerMod());

        mod.addClassFile(MCPatcherUtils.CIT_UTILS_CLASS);
        mod.addClassFile(MCPatcherUtils.CIT_UTILS_CLASS + "$1");
        mod.addClassFile(MCPatcherUtils.ITEM_OVERRIDE_CLASS);
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_CLASS);
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS);
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS + "$Group");
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS + "$AverageGroup");
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS + "$AverageGroup$1");
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS + "$CycleGroup");
        mod.addClassFile(MCPatcherUtils.ITEM_OVERLAY_LIST_CLASS + "$Entry");
    }

    private static class ItemMod extends BaseMod.ItemMod {
        ItemMod() {
            final MethodRef getIconIndex = new MethodRef(getDeobfClass(), "getIconIndex", "(LItemStack;)LIcon;");

            addMemberMapper(new MethodMapper(getIconIndex));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override item texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ARETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getIcon", "(LIcon;LItem;LItemStack;)LIcon;"))
                    );
                }
            }
                .setInsertBefore(true)
                .targetMethod(getIconIndex)
            );
        }
    }

    private static class ItemStackMod extends ClassMod {
        ItemStackMod() {
            final FieldRef stackSize = new FieldRef(getDeobfClass(), "stackSize", "I");
            final FieldRef itemID = new FieldRef(getDeobfClass(), "itemID", "I");
            final FieldRef itemDamage = new FieldRef(getDeobfClass(), "itemDamage", "I");
            final FieldRef stackTagCompound = new FieldRef(getDeobfClass(), "stackTagCompound", "LNBTTagCompound;");
            final MethodRef getItemDamage = new MethodRef(getDeobfClass(), "getItemDamage", "()I");

            addClassSignature(new ConstSignature("id"));
            addClassSignature(new ConstSignature("Count"));
            addClassSignature(new ConstSignature("Damage"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ILOAD_2,
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        ILOAD_3,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .setMethod(new MethodRef(getDeobfClass(), "<init>", "(III)V"))
                .addXref(1, itemID)
                .addXref(2, stackSize)
                .addXref(3, itemDamage)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IRETURN,
                        end()
                    );
                }
            }
                .setMethod(getItemDamage)
                .addXref(1, itemDamage)
            );

            addMemberMapper(new FieldMapper(stackTagCompound));
        }
    }

    private static class EntityItemMod extends ClassMod {
        EntityItemMod() {
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("Age"));
            addClassSignature(new ConstSignature("Item"));

            addMemberMapper(new MethodMapper(getEntityItem));
        }
    }

    private static class GlintSignature extends BytecodeSignature {
        GlintSignature(MethodRef method) {
            setMethod(method);
        }

        @Override
        public String getMatchExpression() {
            return buildExpression(
                push(GLINT_PNG)
            );
        }
    }

    private static class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLiving;LItemStack;I)V");
            final MethodRef renderItemIn2D = new MethodRef(getDeobfClass(), "renderItemIn2D", "(LTessellator;FFFFIIF)V");

            addClassSignature(new GlintSignature(renderItem));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() ...)
                        ALOAD_2,
                        IFNULL, any(2),
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2)
                    );
                }
            }
                .setMethod(renderItem)
                .addXref(1, hasEffect)
            );

            addMemberMapper(new MethodMapper(renderItemIn2D).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item overlay (held)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() ...)
                        ALOAD_2,
                        IFNULL, any(2),
                        ALOAD_2,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderOverlayHeld(itemStack)) {
                        ALOAD_2,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderOverlayHeld", "(LItemStack;)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItem));
        }
    }

    private static class RenderItemMod extends ClassMod {
        RenderItemMod() {
            final FieldRef zLevel = new FieldRef(getDeobfClass(), "zLevel", "F");
            final MethodRef renderDroppedItem = new MethodRef(getDeobfClass(), "renderDroppedItem", "(LEntityItem;LIcon;IFFFF)V");
            final MethodRef renderItemAndEffectIntoGUI = new MethodRef(getDeobfClass(), "renderItemAndEffectIntoGUI", "(LFontRenderer;LRenderEngine;LItemStack;II)V");

            addClassSignature(new GlintSignature(renderDroppedItem));
            addClassSignature(new GlintSignature(renderItemAndEffectIntoGUI));

            addMemberMapper(new FieldMapper(zLevel).accessFlag(AccessFlag.STATIC, false));

            addPatch(new BytecodePatch() {
                private int itemStackRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                anyALOAD,
                                reference(INVOKEVIRTUAL, getEntityItem),
                                capture(anyASTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            itemStackRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "render item overlay (dropped)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() ...)
                        ALOAD, itemStackRegister,
                        IFNULL, any(2),
                        ALOAD, itemStackRegister,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderOverlayDropped(itemStack)) {
                        ALOAD, itemStackRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderOverlayDropped", "(LItemStack;)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderDroppedItem));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item overlay (gui)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack.hasEffect())
                        ALOAD_3,
                        reference(INVOKEVIRTUAL, hasEffect),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderOverlayGUI(itemStack, x, y, this.zLevel)) {
                        ALOAD_3,
                        ILOAD, 4,
                        ILOAD, 5,
                        ALOAD_0,
                        reference(GETFIELD, zLevel),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderOverlayGUI", "(LItemStack;IIF)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItemAndEffectIntoGUI));
        }
    }

    private static class RenderLivingMod extends ClassMod {
        RenderLivingMod() {
            final MethodRef doRenderLiving = new MethodRef(getDeobfClass(), "doRenderLiving", "(LEntityLiving;DDDFF)V");

            addClassSignature(new GlintSignature(doRenderLiving));
            addClassSignature(new ConstSignature("deadmau5"));

            addPatch(new BytecodePatch() {
                private int passRegister;
                private byte[] renderModelCode;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // flags = this.shouldRenderPass(entityLiving, pass, partialTick);
                                ALOAD_0,
                                ALOAD_1,
                                capture(anyILOAD),
                                anyFLOAD,
                                anyReference(INVOKEVIRTUAL),
                                anyISTORE
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            passRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // this.renderPassModel.render(entityLiving, swing, prevSwing, rotation, yawHead - yawOffset, pitch, oneSixteenth);
                                ALOAD_0,
                                anyReference(GETFIELD),
                                ALOAD_1,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                anyFLOAD,
                                FSUB,
                                anyFLOAD,
                                anyFLOAD,
                                anyReference(INVOKEVIRTUAL)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            renderModelCode = getMatch();
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "render item overlay (armor)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if ((flags & 15) == 15) {
                        anyILOAD,
                        push(15),
                        IAND,
                        push(15),
                        IF_ICMPNE, any(2),

                        // ...
                        nonGreedy(any(0, 400)),

                        // }
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (CITUtils.setupArmorOverlays(entityLiving, pass)) {
                        ALOAD_1,
                        ILOAD, passRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "setupArmorOverlays", "(LEntityLiving;I)Z")),
                        IFEQ, branch("A"),

                        // while (CITUtils.preRenderArmorOverlay()) {
                        label("B"),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "preRenderArmorOverlay", "()Z")),
                        IFEQ, branch("C"),

                        // this.renderPassModel.render(...);
                        // CITUtils.postRenderArmorOverlay();
                        renderModelCode,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "postRenderArmorOverlay", "()V")),
                        GOTO, branch("B"),

                        // }

                        // } else {
                        label("A"),

                        // ...
                        getMatch(),

                        // }
                        label("C")
                    );
                }
            }.targetMethod(doRenderLiving));
        }
    }

    private static class RenderBipedMod extends ClassMod {
        RenderBipedMod() {
            final MethodRef loadTextureForPass = new MethodRef(getDeobfClass(), "loadTextureForPass", "(L" + entityLivingSubclass + ";IF)V");
            final MethodRef getCurrentArmor = new MethodRef(entityLivingSubclass, "getCurrentArmor", "(I)LItemStack;");

            addClassSignature(new ConstSignature("/armor/"));
            addClassSignature(new ConstSignature("_"));
            addClassSignature(new ConstSignature("_b.png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        push(3),
                        ILOAD_2,
                        ISUB,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(loadTextureForPass)
                .addXref(1, getCurrentArmor)
            );
        }
    }

    private static class RenderArmorMod extends ClassMod {
        RenderArmorMod() {
            setParentClass("RenderLiving");
            setMultipleMatchesAllowed(true);

            final MethodRef getArmorTexture = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getArmorTexture", "(Ljava/lang/String;LEntityLiving;LItemStack;)Ljava/lang/String;");
            final ClassRef sbClass = new ClassRef("java/lang/StringBuilder");
            final MethodRef sbInit0 = new MethodRef("java/lang/StringBuilder", "<init>", "()V");
            final MethodRef sbInit1 = new MethodRef("java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
            final MethodRef sbToString = new MethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

            addClassSignature(new ConstSignature("/armor/"));
            addClassSignature(new ConstSignature("_"));
            addClassSignature(new ConstSignature("_b.png"));

            addPatch(new BytecodePatch() {
                private int armorRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                begin(),
                                // itemStack = entityLiving.getCurrentArmor(3 - slot);
                                // - or -
                                // itemStack = entityPlayer.inventory.armorItemInSlot(3 - slot);
                                ALOAD_1,
                                optional(build(anyReference(GETFIELD))),
                                push(3),
                                ILOAD_2,
                                ISUB,
                                anyReference(INVOKEVIRTUAL),
                                capture(anyASTORE)
                            );
                        }

                        @Override
                        public boolean afterMatch() {
                            armorRegister = extractRegisterNum(getCaptureGroup(1));
                            return true;
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override armor texture";
                }

                @Override
                public boolean filterMethod() {
                    return getMethodInfo().getDescriptor().matches("\\(L[a-z]+;IF\\)[IV]");
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(NEW, sbClass),
                        DUP,
                        or(
                            build(
                                // new StringBuilder("/armor/")
                                push("/armor/"),
                                reference(INVOKESPECIAL, sbInit1)
                            ),
                            build(
                                // new StringBuilder().append("/armor/")
                                reference(INVOKESPECIAL, sbInit0),
                                push("/armor/")
                            )
                        ),
                        nonGreedy(any(0, 100)),
                        reference(INVOKEVIRTUAL, sbToString)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        ALOAD, armorRegister,
                        reference(INVOKESTATIC, getArmorTexture)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private static class EntityLivingMod extends ClassMod {
        EntityLivingMod() {
            setParentClass("Entity");

            addClassSignature(new ConstSignature("/mob/char.png"));
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("HurtTime"));
        }
    }

    private static class EntityLivingSubMod extends ClassMod {
        EntityLivingSubMod() {
            setParentClass(haveEntityLivingSubclass ? "EntityLiving" : "Entity");

            addClassSignature(new ConstSignature("explode"));
            addClassSignature(new ConstSignature("CanPickUpLoot"));
            addClassSignature(new ConstSignature("PersistenceRequired"));
            addClassSignature(new ConstSignature("Equipment"));
        }
    }

    private static class EntityPlayerMod extends ClassMod {
        EntityPlayerMod() {
            final MethodRef getCurrentArmor = new MethodRef(getDeobfClass(), "getCurrentArmor", "(I)LItemStack;");

            setParentClass("EntityLiving");

            addClassSignature(new ConstSignature("/mob/char.png"));
            addClassSignature(new ConstSignature("random.eat"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(GETFIELD),
                        ILOAD_1,
                        anyReference(INVOKEVIRTUAL),
                        ARETURN,
                        end()
                    );
                }
            }.setMethod(getCurrentArmor));
        }
    }
}
