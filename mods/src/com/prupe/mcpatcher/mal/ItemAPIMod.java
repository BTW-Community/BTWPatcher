package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

public class ItemAPIMod extends Mod {
    private final int malVersion;

    public ItemAPIMod() {
        name = MCPatcherUtils.ITEM_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("item", malVersion);

        addClassMod(new ItemMod());
        if (malVersion >= 2) {
            addClassMod(new Shared.RegistryBaseMod(this));
            addClassMod(new Shared.RegistryMod(this));
        }

        addClassFile(MCPatcherUtils.ITEM_API_CLASS);
        addClassFile(MCPatcherUtils.ITEM_API_CLASS + "$V" + malVersion);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class ItemMod extends BaseMod.ItemMod {
        ItemMod() {
            super(ItemAPIMod.this);

            if (haveItemRegistry) {
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
