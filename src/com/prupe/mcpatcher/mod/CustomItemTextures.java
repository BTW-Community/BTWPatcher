package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

abstract class CustomItemTextures {
    private static final String GLINT_PNG = "%blur%/misc/glint.png";

    static void setup(Mod mod) {
        mod.addClassMod(new BaseMod.NBTTagCompoundMod().mapGetTagList());
        mod.addClassMod(new BaseMod.NBTTagListMod());
        mod.addClassMod(new ItemMod());
        mod.addClassMod(new ItemStackMod());
        mod.addClassMod(new ItemRendererMod());
        mod.addClassMod(new RenderItemMod());

        mod.addClassFile(MCPatcherUtils.CIT_UTILS_CLASS);
        mod.addClassFile(MCPatcherUtils.CIT_UTILS_CLASS + "$ItemOverride");
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

    private static class ItemRendererMod extends ClassMod {
        ItemRendererMod() {
            final MethodRef renderItem = new MethodRef(getDeobfClass(), "renderItem", "(LEntityLiving;LItemStack;I)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(GLINT_PNG)
                    );
                }
            }.setMethod(renderItem));
        }
    }

    private static class RenderItemMod extends ClassMod {
        RenderItemMod() {
            final MethodRef renderDroppedItem = new MethodRef(getDeobfClass(), "renderDroppedItem", "(LEntityItem;LIcon;IFFFF)V");
            final MethodRef renderItemAndEffectIntoGUI = new MethodRef(getDeobfClass(), "renderItemAndEffectIntoGUI", "(LFontRenderer;LRenderEngine;LItemStack;II)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(GLINT_PNG)
                    );
                }
            }.setMethod(renderDroppedItem));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(GLINT_PNG)
                    );
                }
            }.setMethod(renderItemAndEffectIntoGUI));
        }
    }
}
