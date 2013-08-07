package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.BaseMod;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;

public class NBTMod extends Mod {
    public NBTMod() {
        name = MCPatcherUtils.NBT_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";

        addClassMod(new BaseMod.NBTTagCompoundMod(this));
        addClassMod(new BaseMod.NBTTagListMod(this));

        addClassFile(MCPatcherUtils.NBT_RULE_CLASS);
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Exact");
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Regex");
        addClassFile(MCPatcherUtils.NBT_RULE_CLASS + "$Glob");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }
}
