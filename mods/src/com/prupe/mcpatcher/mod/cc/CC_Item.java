package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.IBlockStateMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static com.prupe.mcpatcher.mod.cc.CustomColors.getColorFromDamage;
import static com.prupe.mcpatcher.mod.cc.CustomColors.getMinecraftVersion;
import static javassist.bytecode.Opcode.*;

class CC_Item {
    static void setup(Mod mod) {
        mod.addClassMod(new ItemMod(mod));
        mod.addClassMod(new PotionMod(mod));
        mod.addClassMod(new PotionHelperMod(mod));
        mod.addClassMod(new MapColorMod(mod));
        mod.addClassMod(new ItemDyeMod(mod));
        mod.addClassMod(new ItemArmorMod(mod));
        mod.addClassMod(new ItemSpawnerEggMod(mod));
    }

    private static class ItemMod extends com.prupe.mcpatcher.basemod.ItemMod {
        ItemMod(Mod mod) {
            super(mod);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(0xffffff),
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(getColorFromDamage));
        }
    }

    private static class PotionMod extends ClassMod {
        PotionMod(Mod mod) {
            super(mod);

            final FieldRef potionID = new FieldRef(getDeobfClass(), "id", "I");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");
            final FieldRef origColor = new FieldRef(getDeobfClass(), "origColor", "I");
            final FieldRef potionName = new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;");
            final MethodRef setPotionName = new MethodRef(getDeobfClass(), "setPotionName", "(Ljava/lang/String;)LPotion;");
            final MethodRef setupPotion = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupPotion", "(LPotion;)V");

            addClassSignature(new ConstSignature("potion.moveSpeed"));
            addClassSignature(new ConstSignature("potion.moveSlowdown"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(setPotionName);
                    addXref(1, potionName);
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            ALOAD_1,
                            captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            end()
                        );
                    } else {
                        return null;
                    }
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, potionID);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, color);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        or(build(ILOAD_3), build(ILOAD, 4)),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addPatch(new MakeMemberPublicPatch(potionName));
            addPatch(new AddFieldPatch(origColor));

            addPatch(new MakeMemberPublicPatch(color) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(setPotionName);
                }

                @Override
                public String getDescription() {
                    return "map potions by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, setupPotion)
                    );
                }
            });
        }
    }

    private static class PotionHelperMod extends ClassMod {
        private static final int MAGIC = 0x385dc6;

        PotionHelperMod(Mod mod) {
            super(mod);

            final MethodRef getPotionColor = new MethodRef(getDeobfClass(), "getPotionColor", "(IZ)I");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            final MethodRef getPotionColorCache = new MethodRef(getDeobfClass(), "getPotionColorCache", "()Ljava/util/Map;");
            final MethodRef getWaterBottleColor = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "getWaterBottleColor", "()I");

            addClassSignature(new ConstSignature("potion.prefix.mundane"));
            addClassSignature(new ConstSignature(MAGIC));

            final int mapOpcode;
            final JavaRef mapContains;
            final FieldRef potionColorCache;

            if (getMinecraftVersion().compareTo("14w02a") >= 0) {
                mapOpcode = INVOKEINTERFACE;
                mapContains = new InterfaceMethodRef("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z");
                potionColorCache = new FieldRef(getDeobfClass(), "potionColorCache", "Ljava/util/Map;");
            } else {
                mapOpcode = INVOKEVIRTUAL;
                mapContains = new MethodRef("java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z");
                potionColorCache = new FieldRef(getDeobfClass(), "potionColorCache", "Ljava/util/HashMap;");
            }

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(getPotionColor);
                    addXref(1, potionColorCache);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // PotionHelper.potionColorCache.containsKey(id)
                        captureReference(GETSTATIC),
                        ILOAD_0,
                        reference(INVOKESTATIC, integerValueOf),
                        reference(mapOpcode, mapContains)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override water bottle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // int i = 0x385dc6;
                        begin(),
                        push(MAGIC),
                        ISTORE_1
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // int i = ColorizeItem.getWaterBottleColor();
                        reference(INVOKESTATIC, getWaterBottleColor),
                        ISTORE_1
                    );
                }
            });

            addPatch(new AddMethodPatch(getPotionColorCache, AccessFlag.PUBLIC | AccessFlag.STATIC) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        reference(GETSTATIC, potionColorCache),
                        ARETURN
                    );
                }
            });
        }
    }

    private static class ItemSpawnerEggMod extends ClassMod {
        ItemSpawnerEggMod(Mod mod) {
            super(mod);

            final MethodRef getColorFromDamage2 = new MethodRef(getDeobfClass(), getColorFromDamage.getName(), getColorFromDamage.getType());
            final MethodRef getItemNameIS = new MethodRef(getDeobfClass(), "getItemNameIS", "(LItemStack;)Ljava/lang/String;");
            final MethodRef getItemDamage = new MethodRef("ItemStack", "getItemDamage", "()I");
            final MethodRef getEntityString = new MethodRef("EntityList", "getEntityString", "(I)Ljava/lang/String;");
            final MethodRef colorizeSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "colorizeSpawnerEgg", "(III)I");

            setParentClass("Item");

            addClassSignature(new ConstSignature(".name"));
            addClassSignature(new ConstSignature("entity."));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(getItemNameIS);
                    addXref(1, getItemDamage);
                    addXref(2, getEntityString);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // s1 = EntityList.getEntityString(itemStack.getItemDamage());
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKESTATIC),
                        ASTORE_3,
                        ALOAD_3
                    );
                }
            });

            addClassSignature(new OrSignature(
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // 64 + (i * 0x24faef & 0xc0)
                            BIPUSH, 64,
                            ILOAD_1,
                            push(0x24faef),
                            IMUL,
                            push(0xc0),
                            IAND,
                            IADD
                        );
                    }
                }.setMethod(getColorFromDamage),

                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(0xffffff),
                            IRETURN
                        );
                    }
                }.setMethod(getColorFromDamage)
            ));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(getColorFromDamage2);
                }

                @Override
                public String getDescription() {
                    return "override spawner egg color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getItemDamage),
                        ILOAD_2,
                        reference(INVOKESTATIC, colorizeSpawnerEgg)
                    );
                }
            });
        }
    }

    private static class MapColorMod extends ClassMod {
        MapColorMod(Mod mod) {
            super(mod);

            final FieldRef mapColorArray = new FieldRef(getDeobfClass(), "mapColorArray", "[LMapColor;");
            final FieldRef colorValue = new FieldRef(getDeobfClass(), "colorValue", "I");
            final FieldRef colorIndex = new FieldRef(getDeobfClass(), "colorIndex", "I");
            final FieldRef origColorValue = new FieldRef(getDeobfClass(), "origColorValue", "I");

            addClassSignature(new ConstSignature(0x7fb238));
            addClassSignature(new ConstSignature(0xf7e9a3));
            addClassSignature(new ConstSignature(0xa7a7a7));
            addClassSignature(new ConstSignature(0xff0000));
            addClassSignature(new ConstSignature(0xa0a0ff));

            addMemberMapper(new FieldMapper(mapColorArray).accessFlag(AccessFlag.STATIC, true));
            addMemberMapper(new FieldMapper(colorValue, colorIndex).accessFlag(AccessFlag.STATIC, false));

            addPatch(new AddFieldPatch(origColorValue));

            addPatch(new MakeMemberPublicPatch(colorValue) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V"));
                }

                @Override
                public String getDescription() {
                    return "set map origColorValue";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, colorValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, origColorValue)
                    );
                }
            });
        }
    }

    private static class ItemDyeMod extends ClassMod {
        private final FieldRef dyeColorNames = new FieldRef(getDeobfClass(), "dyeColorNames", "[Ljava/lang/String;");
        private final FieldRef dyeColors = new FieldRef(getDeobfClass(), "dyeColors", "[I");

        ItemDyeMod(Mod mod) {
            super(mod);

            addClassSignature(new ConstSignature("black"));
            addClassSignature(new ConstSignature("purple"));
            addClassSignature(new ConstSignature("cyan"));

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            setParentClass("Item");

            addClassSignature(new ConstSignature(0x1e1b1b));

            addMemberMapper(new FieldMapper(dyeColorNames)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
            addMemberMapper(new FieldMapper(dyeColors)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
        }

        // TODO
        private void setup18() {
            setInterfaces("INamed");

            addPatch(new AddFieldPatch(dyeColorNames, AccessFlag.PUBLIC | AccessFlag.STATIC));
            addPatch(new AddFieldPatch(dyeColors, AccessFlag.PUBLIC | AccessFlag.STATIC));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize arrays";
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
                        // dyeColorNames = new String[16];
                        push(16),
                        reference(ANEWARRAY, new ClassRef("java/lang/String")),
                        reference(PUTSTATIC, dyeColorNames),

                        // dyeColors = new int[16];
                        push(16),
                        NEWARRAY, T_INT,
                        reference(PUTSTATIC, dyeColors)
                    );
                }
            }.matchStaticInitializerOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set color names";
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
                        // dyeColorNames[ordinal] = name;
                        reference(GETSTATIC, dyeColorNames),
                        ILOAD_2,
                        ALOAD, 6,
                        AASTORE
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private static class ItemArmorMod extends ClassMod {
        private final int DEFAULT_LEATHER_COLOR = 0xa06540;

        ItemArmorMod(Mod mod) {
            super(mod);

            final FieldRef undyedLeatherColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "undyedLeatherColor", "I");

            addClassSignature(new ConstSignature("display"));
            addClassSignature(new ConstSignature("color"));
            addClassSignature(new ConstSignature(DEFAULT_LEATHER_COLOR));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default leather armor color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(DEFAULT_LEATHER_COLOR)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, undyedLeatherColor)
                    );
                }
            });
        }
    }
}
