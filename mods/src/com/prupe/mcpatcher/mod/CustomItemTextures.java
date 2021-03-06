package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomItemTextures extends Mod {
    private final String GLINT_PNG;

    private static final MethodRef glDepthFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthFunc", "(I)V");
    private static final MethodRef glRotatef = new MethodRef(MCPatcherUtils.GL11_CLASS, "glRotatef", "(FFFF)V");
    private static final MethodRef glEnable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glEnable", "(I)V");
    private static final MethodRef glAlphaFunc = new MethodRef(MCPatcherUtils.GL11_CLASS, "glAlphaFunc", "(IF)V");
    private static final MethodRef glDisable = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDisable", "(I)V");
    private static final MethodRef glColorMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColorMask", "(ZZZZ)V");
    private static final MethodRef glDepthMask = new MethodRef(MCPatcherUtils.GL11_CLASS, "glDepthMask", "(Z)V");
    private static final MethodRef glColor4f = new MethodRef(MCPatcherUtils.GL11_CLASS, "glColor4f", "(FFFF)V");

    private static final MethodRef getEntityItem = new MethodRef("EntityItem", "getEntityItem", "()LItemStack;");
    private static final FieldRef itemsList = new FieldRef("Item", "itemsList", "[LItem;");
    private static final MethodRef hasEffect = new MethodRef("ItemStack", "hasEffectVanilla", "()Z");
    private static final MethodRef hasEffectForge = new MethodRef("ItemStack", "hasEffect", "(I)Z");
    private static final MethodRef getIconFromDamageForRenderPass = new MethodRef("Item", "getIconFromDamageForRenderPass", "(II)LIcon;");

    private static final MethodRef getCITIcon = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getIcon", "(LIcon;LItemStack;I)LIcon;");
    private static final MethodRef getArmorTexture = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getArmorTexture", "(LResourceLocation;LEntityLivingBase;LItemStack;)LResourceLocation;");
    private static final MethodRef setupArmorEnchantments1 = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "setupArmorEnchantments", "(LEntityLivingBase;I)Z");
    private static final MethodRef setupArmorEnchantments2 = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "setupArmorEnchantments", "(LItemStack;)Z");
    private static final MethodRef preRenderArmorEnchantment = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "preRenderArmorEnchantment", "()Z");
    private static final MethodRef postRenderArmorEnchantment = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "postRenderArmorEnchantment", "()V");
    private static final MethodRef isArmorEnchantmentActive = new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "isArmorEnchantmentActive", "()Z");

    private final boolean newEntityRendering;

    public CustomItemTextures() {
        name = MCPatcherUtils.CUSTOM_ITEM_TEXTURES;
        author = "MCPatcher";
        description = "Enables support for custom item textures, enchantments, and armor.";
        version = "1.1";
        configPanel = new ConfigPanel();

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.TILESHEET_API_MOD);
        addDependency(MCPatcherUtils.TESSELLATOR_API_MOD);
        addDependency(MCPatcherUtils.NBT_API_MOD);
        addDependency(MCPatcherUtils.ITEM_API_MOD);

        newEntityRendering = getMinecraftVersion().compareTo("14w05a") >= 0;
        GLINT_PNG = ResourceLocationMod.select("%blur%/misc/glint.png", "textures/misc/enchanted_item_glint.png");

        ResourceLocationMod.setup(this);
        addClassMod(new TessellatorMod(this));
        addClassMod(new NBTTagCompoundMod(this));
        addClassMod(new NBTTagListMod(this));
        addClassMod(new IconMod(this));
        addClassMod(new ItemMod());
        addClassMod(new ItemArmorMod());
        addClassMod(new ItemStackMod(this));
        addClassMod(new EntityItemMod());
        addClassMod(new ItemRendererMod());
        addClassMod(new RenderItemMod());
        if (newEntityRendering) {
            addClassMod(new RenderArmor18Mod());
        } else {
            addClassMod(new RenderLivingEntityMod());
            addClassMod(new RenderBipedMod());
            if (ResourceLocationMod.haveClass()) {
                addClassMod(new RenderPlayerMod());
            } else {
                addClassMod(new RenderArmor15Mod());
            }
        }
        addClassMod(new RenderSnowballMod());
        addClassMod(new EntityLivingBaseMod());
        addClassMod(new EntityLivingMod(this));
        addClassMod(new EntityPlayerMod());
        if (ResourceLocationMod.haveClass()) {
            addClassMod(new AbstractClientPlayerMod());
        }
        addClassMod(new PotionMod(this));
        addClassMod(new PotionHelperMod(this));

        addClassFiles("com.prupe.mcpatcher.cit.*");
        removeAddedClassFile(MCPatcherUtils.CIT_UTILS18_CLASS);

        TexturePackAPIMod.earlyInitialize(2, MCPatcherUtils.CIT_UTILS_CLASS, "init");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JCheckBox itemsCheckBox;
        private JCheckBox enchantmentCheckBox;
        private JCheckBox armorCheckBox;

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            itemsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", itemsCheckBox.isSelected());
                }
            });

            enchantmentCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", enchantmentCheckBox.isSelected());
                }
            });

            armorCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", armorCheckBox.isSelected());
                }
            });
        }

        @Override
        public void save() {
            itemsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "items", true));
            enchantmentCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "enchantments", true));
            armorCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "armor", true));
        }
    }

    private static String getHasEffectSignature(BytecodePatch patch, int itemStackRegister, int renderPassRegister, String extraVanilla, String extraForge) {
        return patch.buildExpression(
            registerLoadStore(ALOAD, itemStackRegister),
            or(
                build(
                    // vanilla:
                    // itemStack.hasEffect()
                    patch.reference(INVOKEVIRTUAL, hasEffect),
                    extraVanilla
                ),
                build(
                    // forge:
                    // itemStack.hasEffectForge(renderPass)
                    registerLoadStore(ILOAD, renderPassRegister),
                    patch.reference(INVOKEVIRTUAL, hasEffectForge),
                    extraForge
                )
            )
        );
    }

    private static String getHasEffectSignature(BytecodePatch patch, int itemStackRegister, int renderPassRegister) {
        return getHasEffectSignature(patch, itemStackRegister, renderPassRegister, "", "");
    }

    private class ItemMod extends com.prupe.mcpatcher.basemod.ItemMod {
        ItemMod() {
            super(CustomItemTextures.this);

            final MethodRef getIconIndex = new MethodRef(getDeobfClass(), "getIconIndex", "(LItemStack;)LIcon;");
            final MethodRef getIconForge = new MethodRef(getDeobfClass(), "getIcon", "(LItemStack;I)LIcon;");

            addMemberMapper(new MethodMapper(getIconIndex));
            if (!haveItemRegistry()) {
                addMemberMapper(new FieldMapper(itemsList));
            }

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(getIconIndex);
                }

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
                        ALOAD_1,
                        push(0),
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(getIconForge);
                }

                @Override
                public String getDescription() {
                    return "override item texture (forge)";
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
                        ALOAD_1,
                        ILOAD_2,
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            });

            addMemberMapper(new MethodMapper(getIconFromDamageForRenderPass));
        }
    }

    private class ItemArmorMod extends ClassMod {
        ItemArmorMod() {
            addClassSignature(new ConstSignature(ResourceLocationMod.select("helmetCloth_overlay", "leather_helmet_overlay")));
            addClassSignature(new ConstSignature(ResourceLocationMod.select("slot_empty_helmet", "empty_armor_slot_helmet")));
        }
    }

    private class EntityItemMod extends ClassMod {
        EntityItemMod() {
            addClassSignature(new ConstSignature("Health"));
            addClassSignature(new ConstSignature("Age"));
            addClassSignature(new ConstSignature("Item"));

            addMemberMapper(new MethodMapper(getEntityItem));
        }
    }

    private void addGlintSignature(ClassMod classMod, MethodRef method) {
        addGlintSignature(classMod, method, build(ALOAD_0));
    }

    private void addGlintSignature(ClassMod classMod, MethodRef method, final String opcode) {
        if (ResourceLocationMod.haveClass()) {
            addGlintSignature16(classMod, method, opcode);
        } else {
            addGlintSignature15(classMod, method);
        }
    }

    private void addGlintSignature15(ClassMod classMod, MethodRef method) {
        classMod.addClassSignature(new BytecodeSignature(classMod) {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    push(GLINT_PNG)
                );
            }
        }.setMethod(method));
    }

    private void addGlintSignature16(ClassMod classMod, final MethodRef method, final String opcode) {
        final FieldRef glint = new FieldRef(classMod.getDeobfClass(), "glint", "LResourceLocation;");

        classMod.addClassSignature(new ResourceLocationSignature(classMod, glint, GLINT_PNG));

        classMod.addClassSignature(new BytecodeSignature(classMod) {
            {
                setMethod(method);
                addXref(1, glint);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    opcode,
                    any(0, 10),
                    captureReference(GETSTATIC),
                    anyReference(INVOKEVIRTUAL)
                );
            }

            @Override
            public boolean afterMatchPre() {
                // ensure GETSTATIC is from the same class
                int index = ((getCaptureGroup(1)[1] & 0xff) << 8) | (getCaptureGroup(1)[2] & 0xff);
                String cl1 = getMethodInfo().getConstPool().getFieldrefClassName(index);
                String cl2 = getClassFile().getName();
                return cl1.equals(cl2);
            }
        });
    }

    private class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItemVanilla", "(LEntityLivingBase;LItemStack;I)V");
            final MethodRef renderItemIn2D = new MethodRef(getDeobfClass(), "renderItemIn2D", "(LTessellator;FFFFIIF)V");
            final MethodRef getEntityItemIcon = new MethodRef("EntityLivingBase", "getItemIcon", "(LItemStack;I)LIcon;");
            final MethodRef renderItemForge = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLivingBase;LItemStack;ILnet/minecraftforge/client/IItemRenderer$ItemRenderType;)V");

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/misc/mapbg.png", "textures/map/map_background.png")));
            addClassSignature(new ConstSignature(ResourceLocationMod.select("/misc/water.png", "textures/misc/underwater.png")));
            // 13w24b: this.minecraft.getTextureManager.bindTexture(glint);
            // 13w25a: var4 = this.minecraft.getTextureManager(); ... var4.bindTexture(glint);
            addGlintSignature(this, renderItem, anyALOAD);

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderItem);
                    addXref(1, hasEffect);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack != null && itemStack.hasEffect() && renderPass == 0)
                        optional(build(
                            ALOAD_2,
                            IFNULL, any(2)
                        )),
                        ALOAD_2,
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2),
                        ILOAD_3,
                        IFNE, any(2)
                    );
                }
            });

            addMemberMapper(new MethodMapper(renderItemIn2D).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderItem, renderItemForge);
                }

                @Override
                public String getDescription() {
                    return "override item texture (held)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(INVOKEVIRTUAL, getEntityItemIcon)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils.getIcon(..., itemStack, renderPass);
                        ALOAD_2,
                        ILOAD_3,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getIcon", "(LIcon;LItemStack;I)LIcon;"))
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (held)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        getHasEffectSignature(this, 2, 3,
                            // vanilla:
                            // if (itemStack.hasEffect() && renderPass == 0)
                            build(
                                IFEQ, any(2),
                                ILOAD_3,
                                IFNE, any(2)
                            ),
                            // forge:
                            // if (itemStack.hasEffect(renderPass))
                            build(
                                IFEQ, any(2)
                            )
                        ),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderEnchantmentHeld(itemStack, renderPass)) {
                        ALOAD_2,
                        ILOAD_3,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentHeld", "(LItemStack;I)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItem, renderItemForge));
        }
    }

    private class RenderItemMod extends ClassMod {
        RenderItemMod() {
            final FieldRef zLevel = new FieldRef(getDeobfClass(), "zLevel", "F");
            final MethodRef renderDroppedItem = new MethodRef(getDeobfClass(), "renderDroppedItemVanilla", newEntityRendering ? "(LIcon;FFFLEntityItem;)V" : "(LEntityItem;LIcon;IFFFF)V");
            final MethodRef renderDroppedItemForge = new MethodRef(getDeobfClass(), "renderDroppedItem", "(LEntityItem;LIcon;IFFFFI)V");
            final MethodRef renderItemAndEffectIntoGUI = new MethodRef(getDeobfClass(), "renderItemAndEffectIntoGUI", "(LFontRenderer;LTextureManager;LItemStack;II)V");
            final MethodRef renderItemIntoGUI = new MethodRef(getDeobfClass(), "renderItemIntoGUIVanilla", "(LFontRenderer;LTextureManager;LItemStack;II)V");
            final MethodRef renderItemIntoGUIForge = new MethodRef(getDeobfClass(), "renderItemIntoGUI", "(LFontRenderer;LTextureManager;LItemStack;IIZ)V");
            final MethodRef renderEffectForge = new MethodRef(getDeobfClass(), "renderEffect", "(LTextureManager;II)V");
            final MethodRef renderItemOverlayIntoGUI = new MethodRef(getDeobfClass(), "renderItemOverlayIntoGUI", "(LFontRenderer;LTextureManager;LItemStack;IILjava/lang/String;)V");
            final MethodRef getMaxDamage = new MethodRef("ItemStack", "getMaxDamage", "()I");
            final boolean needAlphaTest = getMinecraftVersion().compareTo("13w42a") >= 0;

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/gui/items.png", "missingno")));
            addGlintSignature(this, renderDroppedItem);
            addGlintSignature(this, renderItemAndEffectIntoGUI, build(ALOAD_2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // GL11.glRotatef(210.0f, 1.0f, 0.0f, 0.0f);
                        push(210.0f),
                        push(1.0f),
                        push(0.0f),
                        push(0.0f),
                        reference(INVOKESTATIC, glRotatef)
                    );
                }
            }.setMethod(renderItemIntoGUI));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderItemOverlayIntoGUI);
                    addXref(1, getMaxDamage);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // 13.0 - (double) itemStack.getItemDamage() * 13.0 / (double) itemStack.getMaxDamage()
                        push(13.0),
                        ALOAD_3,
                        anyReference(INVOKEVIRTUAL),
                        I2D,
                        push(13.0),
                        DMUL,
                        ALOAD_3,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

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
                    return "render item enchantment (dropped)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack.hasEffect() ...)
                        getHasEffectSignature(this, itemStackRegister, 8),
                        IFEQ, any(2),
                        nonGreedy(any(0, 400)),
                        push(515), // GL11.GL_LEQUAL
                        reference(INVOKESTATIC, glDepthFunc)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (!CITUtils.renderEnchantmentDropped(itemStack)) {
                        ALOAD, itemStackRegister,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentDropped", "(LItemStack;)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderDroppedItem, renderDroppedItemForge));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (gui)";
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
                        // if (!CITUtils.renderEnchantmentGUI(itemStack, x, y, this.zLevel)) {
                        ALOAD_3,
                        ILOAD, 4,
                        ILOAD, 5,
                        ALOAD_0,
                        reference(GETFIELD, zLevel),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentGUI", "(LItemStack;IIF)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItemAndEffectIntoGUI));

            if (needAlphaTest) {
                addPatch(new BytecodePatch() {
                    {
                        setInsertBefore(true);
                        targetMethod(renderItemAndEffectIntoGUI);
                    }

                    @Override
                    public String getDescription() {
                        return "enable alpha test in gui";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // this.renderItemIntoGUI(fontRenderer, textureManager, itemStack, x, y);
                            ALOAD_0,
                            ALOAD_1,
                            ALOAD_2,
                            ALOAD_3,
                            ILOAD, 4,
                            ILOAD, 5,
                            or(
                                build(reference(INVOKEVIRTUAL, renderItemIntoGUI)),
                                build(reference(INVOKEVIRTUAL, renderItemIntoGUIForge))
                            )
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // GL11.glEnable(GL11.GL_ALPHA_TEST);
                            push(3008),
                            reference(INVOKESTATIC, glEnable),

                            // GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
                            push(516),
                            push(0.01f),
                            reference(INVOKESTATIC, glAlphaFunc)
                        );
                    }
                });

                addPatch(new BytecodePatch() {
                    @Override
                    public String getDescription() {
                        return "enable alpha test in gui (multiple render passes)";
                    }

                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // GL11.glColorMask(false, false, false, true);
                            push(0),
                            push(0),
                            push(0),
                            push(1),
                            reference(INVOKESTATIC, glColorMask),

                            // ...
                            any(0, 500),

                            // GL11.glColorMask(true, true, true, true);
                            push(1),
                            push(1),
                            push(1),
                            push(1),
                            reference(INVOKESTATIC, glColorMask)
                        );
                    }

                    @Override
                    public byte[] getReplacementBytes() {
                        return buildCode(
                            // GL11.glDepthMask(false);
                            push(0),
                            reference(INVOKESTATIC, glDepthMask),

                            // ...
                            getMatch(),

                            // GL11.glDepthMask(true);
                            push(1),
                            reference(INVOKESTATIC, glDepthMask)
                        );
                    }
                }.targetMethod(renderItemIntoGUI, renderItemIntoGUIForge));
            }

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (gui) (forge)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (itemStack.hasEffect(...)) {
                        ALOAD_3,
                        capture(any(1, 2)),
                        reference(INVOKEVIRTUAL, hasEffectForge),
                        IFEQ, any(2),

                        // this.renderEffect(textureManager, x, y);
                        ALOAD_0,
                        ALOAD_2,
                        ILOAD, 4,
                        ILOAD, 5,
                        reference(INVOKESPECIAL, renderEffectForge)

                        // }
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (... == 0 && !CITUtils.renderEnchantmentGUI(itemStack, x, y, this.zLevel)) {
                        getCaptureGroup(1),
                        IFNE, branch("A"),

                        ALOAD_3,
                        ILOAD, 4,
                        ILOAD, 5,
                        ALOAD_0,
                        reference(GETFIELD, zLevel),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "renderEnchantmentGUI", "(LItemStack;IIF)Z")),
                        IFNE, branch("A"),

                        // ...
                        getMatch(),

                        // }
                        label("A")
                    );
                }
            }.targetMethod(renderItemIntoGUIForge));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(renderItemIntoGUI, renderItemIntoGUIForge);
                }

                @Override
                public String getDescription() {
                    return "handle items with multiple render passes (gui)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        or(
                            build(
                                // 1.6: Item.itemsList[itemID]
                                reference(GETSTATIC, itemsList),
                                anyILOAD,
                                AALOAD
                            ),
                            build(
                                // 1.7: itemStack.getItem()
                                ALOAD_3,
                                reference(INVOKEVIRTUAL, ItemStackMod.getItem)
                            )
                        ),
                        // (...).getIconFromDamageForRenderPass(itemDamage, renderPass)
                        anyILOAD,
                        capture(anyILOAD),
                        reference(INVOKEVIRTUAL, getIconFromDamageForRenderPass)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils.getIcon(..., itemStack, renderPass)
                        ALOAD_3,
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "handle items with multiple render passes (dropped)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // icon = itemStack.getItem().getIconFromDamageForRenderPass(itemStack.getItemDamage(), renderPass);
                        // -or-
                        // icon = item.getIconFromDamageForRenderPass(itemStack.getItemDamage(), renderPass);
                        anyALOAD,
                        optional(build(reference(INVOKEVIRTUAL, ItemStackMod.getItem))),
                        capture(anyALOAD),
                        anyReference(INVOKEVIRTUAL),
                        capture(anyILOAD),
                        reference(INVOKEVIRTUAL, getIconFromDamageForRenderPass)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils.getIcon(..., itemStack, renderPass);
                        getCaptureGroup(1),
                        getCaptureGroup(2),
                        reference(INVOKESTATIC, getCITIcon)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class RenderLivingEntityMod extends ClassMod {
        RenderLivingEntityMod() {
            final MethodRef doRenderLiving = new MethodRef(getDeobfClass(), "doRenderLiving", "(LEntityLivingBase;DDDFF)V");
            final MethodRef doRenderLivingPass = new MethodRef(getDeobfClass(), "doRenderLivingPass", "(LEntityLivingBase;FFFFFFFFI)V");
            final MethodRef renderLivingPatchMethod = getMinecraftVersion().compareTo("14w04a") >= 0 ? doRenderLivingPass : doRenderLiving;

            if (getMinecraftVersion().compareTo("13w36a") < 0) {
                addClassSignature(new ConstSignature("deadmau5"));
            } else {
                addClassSignature(new ConstSignature(85.0f));
                addClassSignature(new ConstSignature(-85.0f));
            }
            addGlintSignature(this, renderLivingPatchMethod);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(2884), // GL11.GL_CULL_FACE
                        reference(INVOKESTATIC, glDisable)
                    );
                }
            }.setMethod(doRenderLiving));

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
                    return "render item enchantment (armor)";
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
                        // if (CITUtils.setupArmorEnchantments(entityLiving, pass)) {
                        ALOAD_1,
                        ILOAD, passRegister,
                        reference(INVOKESTATIC, setupArmorEnchantments1),
                        IFEQ, branch("A"),

                        // while (CITUtils.preRenderArmorEnchantment()) {
                        label("B"),
                        reference(INVOKESTATIC, preRenderArmorEnchantment),
                        IFEQ, branch("C"),

                        // this.renderPassModel.render(...);
                        // CITUtils.postRenderArmorEnchantment();
                        renderModelCode,
                        reference(INVOKESTATIC, postRenderArmorEnchantment),
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
            }.targetMethod(renderLivingPatchMethod));
        }
    }

    private class RenderBipedMod extends RenderArmorMod {
        RenderBipedMod() {
            setParentClass("RenderLiving");

            String entityLivingSubclass = ResourceLocationMod.select("EntityLivingBase", "EntityLiving");
            final MethodRef loadTextureForPass = new MethodRef(getDeobfClass(), "loadTextureForPass", "(L" + entityLivingSubclass + ";IF)V");
            final MethodRef getCurrentArmor = new MethodRef("EntityLiving", "getCurrentArmor", "(I)LItemStack;");

            if (ResourceLocationMod.haveClass()) {
                addClassSignature(new ConstSignature("textures/models/armor/%s_layer_%d%s.png"));

                addMemberMapper(new MethodMapper(getArmorTexture2).accessFlag(AccessFlag.STATIC, true));
                addMemberMapper(new MethodMapper(getArmorTexture3).accessFlag(AccessFlag.STATIC, true));
            }

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // entity.getCurrentArmor(3 - pass);
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

        @Override
        String getEntityClass() {
            return "EntityLiving";
        }
    }

    private class RenderPlayerMod extends RenderArmorMod {
        RenderPlayerMod() {
            setParentClass("RenderLivingEntity");

            addClassSignature(new ConstSignature("textures/entity/steve.png"));
        }

        @Override
        String getEntityClass() {
            return "AbstractClientPlayer";
        }
    }

    abstract private class RenderArmorMod extends ClassMod {
        protected final MethodRef getArmorTexture2 = new MethodRef("RenderBiped", "getArmorTexture2", "(LItemArmor;I)LResourceLocation;");
        protected final MethodRef getArmorTexture3 = new MethodRef("RenderBiped", "getArmorTexture3", "(LItemArmor;ILjava/lang/String;)LResourceLocation;");
        protected final MethodRef getArmorResourceForge = new MethodRef("RenderBiped", "getArmorResource", "(LEntity;LItemStack;ILjava/lang/String;)LResourceLocation;");
        private final com.prupe.mcpatcher.BytecodeSignature renderArmorSignature;

        RenderArmorMod() {
            final MethodRef renderArmor = new MethodRef(getDeobfClass(), "renderArmor", "(L" + getEntityClass() + ";IF)V");

            renderArmorSignature = new BytecodeSignature() {
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
            }.setMethod(renderArmor);

            addClassSignature(renderArmorSignature);

            if (ResourceLocationMod.haveClass()) {
                addArmorPatch();
            }
        }

        private void addArmorPatch() {
            addPatch(new BytecodePatch() {
                private int armorRegister;

                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return renderArmorSignature.getMatchExpression();
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
                    return buildExpression(or(
                        build(reference(INVOKESTATIC, getArmorTexture2)),
                        build(reference(INVOKESTATIC, getArmorTexture3)),
                        build(reference(INVOKESTATIC, getArmorResourceForge))
                    ));
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

        abstract String getEntityClass();
    }

    private class RenderArmor15Mod extends ClassMod {
        RenderArmor15Mod() {
            setParentClass("RenderLiving");
            setMultipleMatchesAllowed(true);

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
                        // "/armor/" + ...
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
                        // CITUtils.getArmorTexture(..., entity, itemArmor)
                        ResourceLocationMod.wrap(this),
                        ALOAD_1,
                        ALOAD, armorRegister,
                        reference(INVOKESTATIC, getArmorTexture),
                        ResourceLocationMod.unwrap(this)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class RenderArmor18Mod extends ClassMod {
        RenderArmor18Mod() {
            final MethodRef renderArmor = new MethodRef(getDeobfClass(), "renderArmor", "(LEntityLivingBase;FFFFFFFI)V");
            final MethodRef getArmorTexture1 = new MethodRef(getDeobfClass(), "getArmorTexture1", "(LItemArmor;Z)LResourceLocation;");
            final MethodRef getArmorTexture2 = new MethodRef(getDeobfClass(), "getArmorTexture2", "(LItemArmor;ZLjava/lang/String;)LResourceLocation;");
            final MethodRef renderEnchantment = new MethodRef(getDeobfClass(), "renderEnchantment", "(LEntityLivingBase;LModelBase;FFFFFFF)V");
            final MethodRef renderModel = new MethodRef("ModelBase", "render", "(LEntity;FFFFFF)V");

            addClassSignature(new ConstSignature("overlay"));
            addClassSignature(new ConstSignature("textures/models/armor/%s_layer_%d%s.png"));
            addGlintSignature(this, renderEnchantment);

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderArmor);
                    addXref(1, renderModel);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // model.render(entity, ...);
                        anyALOAD,
                        ALOAD_1,
                        FLOAD_2,
                        FLOAD_3,
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            addMemberMapper(new MethodMapper(getArmorTexture1));
            addMemberMapper(new MethodMapper(getArmorTexture2));

            addPatch(new BytecodePatch() {
                private int itemStackRegister;

                {
                    setInsertAfter(true);
                    targetMethod(renderArmor);

                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // itemStack = entity.getEquippedItem(slot);
                                ALOAD_0,
                                ALOAD_1,
                                ILOAD, 9,
                                anyReference(INVOKEVIRTUAL),
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
                    return "override armor texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        // this.getArmorTexture(...)
                        build(reference(INVOKESPECIAL, getArmorTexture1)),
                        build(reference(INVOKESPECIAL, getArmorTexture2))
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // CITUtils.getArmorTexture(..., entity, itemStack)
                        ALOAD_1,
                        registerLoadStore(ALOAD, itemStackRegister),
                        reference(INVOKESTATIC, getArmorTexture)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "render item enchantment (armor)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (!this.useColorTint && itemStack.hasEnchantment()) {
                        ALOAD_0,
                        anyReference(GETFIELD),
                        IFNE, any(2),
                        capture(anyALOAD),
                        anyReference(INVOKEVIRTUAL),
                        IFEQ, any(2),

                        // this.renderEnchantment(entity, model, ...);
                        ALOAD_0,
                        ALOAD_1,
                        capture(anyALOAD),
                        FLOAD_2,
                        FLOAD_3,
                        FLOAD, 4,
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        reference(INVOKESPECIAL, renderEnchantment)

                        // }
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    int itemStackRegister = extractRegisterNum(getCaptureGroup(1));
                    return buildCode(
                        // if (CITUtils.setupArmorEnchantment(itemStack)) {
                        registerLoadStore(ALOAD, itemStackRegister),
                        reference(INVOKESTATIC, setupArmorEnchantments2),
                        IFEQ, branch("A"),

                        // while (CITUtils.preRenderArmorEnchantment()) {
                        label("B"),
                        reference(INVOKESTATIC, preRenderArmorEnchantment),
                        IFEQ, branch("C"),

                        // model.render(entity, ...);
                        getCaptureGroup(2),
                        ALOAD_1,
                        FLOAD_2,
                        FLOAD_3,
                        FLOAD, 5,
                        FLOAD, 6,
                        FLOAD, 7,
                        FLOAD, 8,
                        reference(INVOKEVIRTUAL, renderModel),

                        // CITUtils.postRenderEnchantment();
                        reference(INVOKESTATIC, postRenderArmorEnchantment),

                        // }
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        // ...
                        getMatch(),

                        // }
                        label("C")
                    );
                }
            }.targetMethod(renderArmor));
        }
    }

    private class RenderSnowballMod extends ClassMod {
        RenderSnowballMod() {
            setParentClass("Render");

            final MethodRef doRender = new MethodRef(getDeobfClass(), "doRender", "(LEntity;DDDFF)V");

            addClassSignature(new ConstSignature(ResourceLocationMod.select("potion_splash", "bottle_splash")));
            addClassSignature(new ConstSignature(ResourceLocationMod.select("potion_contents", "overlay")));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(doRender);
                    addXref(1, new ClassRef("EntityPotion"));
                    addXref(2, new MethodRef("EntityPotion", "getPotionDamage", "()I"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_1,
                        captureReference(CHECKCAST),
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion entity texture";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // icon = ...;
                        begin(),
                        capture(build(
                                ALOAD_0,
                                nonGreedy(any(0, 20)))
                        ),
                        capture(anyASTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // icon = CITUtils.getEntityIcon(..., entity);
                        getCaptureGroup(1),
                        ALOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.CIT_UTILS_CLASS, "getEntityIcon", "(LIcon;LEntity;)LIcon;")),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(doRender));
        }
    }

    private class EntityLivingBaseMod extends com.prupe.mcpatcher.basemod.EntityLivingBaseMod {
        EntityLivingBaseMod() {
            super(CustomItemTextures.this);

            final MethodRef getCurrentItemOrArmor = new MethodRef(getDeobfClass(), "getCurrentItemOrArmor", "(I)LItemStack;");
            final MethodRef getItemIcon = new MethodRef(getDeobfClass(), "getItemIcon", "(LItemStack;I)LIcon;");

            addMemberMapper(new MethodMapper(getCurrentItemOrArmor));
            addMemberMapper(new MethodMapper(getItemIcon));
        }
    }

    private class EntityPlayerMod extends ClassMod {
        EntityPlayerMod() {
            final MethodRef getCurrentArmor = new MethodRef(getDeobfClass(), "getCurrentArmor", "(I)LItemStack;");

            setParentClass("EntityLivingBase");

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

    private class AbstractClientPlayerMod extends ClassMod {
        AbstractClientPlayerMod() {
            setParentClass("EntityPlayer");

            addClassSignature(new ConstSignature("http://skins.minecraft.net/MinecraftSkins/%s.png"));
        }
    }

}
