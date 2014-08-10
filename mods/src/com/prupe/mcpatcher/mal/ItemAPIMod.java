package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.IBlockStateMod;
import com.prupe.mcpatcher.basemod.RegistryBaseMod;
import com.prupe.mcpatcher.basemod.RegistryMod;
import javassist.bytecode.AccessFlag;

public class ItemAPIMod extends Mod {
    private final int malVersion;

    public ItemAPIMod() {
        name = MCPatcherUtils.ITEM_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (IBlockStateMod.haveClass()) {
            malVersion = 3;
        } else if (ItemMod.haveItemRegistry()) {
            malVersion = 2;
        } else {
            malVersion = 1;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("item", malVersion);

        addClassMod(new ItemMod());
        if (ItemMod.haveItemRegistry()) {
            addClassMod(new RegistryBaseMod(this));
            addClassMod(new RegistryMod(this));
        }

        addClassFiles("com.prupe.mcpatcher.mal.item.*");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class ItemMod extends com.prupe.mcpatcher.basemod.ItemMod {
        ItemMod() {
            super(ItemAPIMod.this);

            if (haveItemRegistry()) {
                final FieldRef itemRegistry = new FieldRef(getDeobfClass(), "itemRegistry", "LRegistry;");

                addMemberMapper(new FieldMapper(itemRegistry)
                        .accessFlag(AccessFlag.PUBLIC, true)
                        .accessFlag(AccessFlag.STATIC, true)
                );
            } else {
                final FieldRef itemsList = new FieldRef(getDeobfClass(), "itemsList", "[LItem;");
                final FieldRef itemID = new FieldRef(getDeobfClass(), "itemID", "I");

                addMemberMapper(new FieldMapper(itemsList)
                        .accessFlag(AccessFlag.PUBLIC, true)
                        .accessFlag(AccessFlag.STATIC, true)
                );

                addMemberMapper(new FieldMapper(itemID)
                        .accessFlag(AccessFlag.PUBLIC, true)
                        .accessFlag(AccessFlag.STATIC, false)
                        .accessFlag(AccessFlag.FINAL, true)
                );
            }
        }
    }
}
