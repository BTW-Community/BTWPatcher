package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.TessellatorFactoryMod;
import com.prupe.mcpatcher.basemod.TessellatorMod;

public class TessellatorAPIMod extends Mod {
    public TessellatorAPIMod() {
        name = MCPatcherUtils.TESSELLATOR_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.0";

        addClassMod(new TessellatorMod(this));
        if (TessellatorFactoryMod.haveClass()) {
            addClassMod(new TessellatorFactoryMod(this));
            setMALVersion("tessellator", 2);
        } else {
            setMALVersion("tessellator", 1);
        }

        addClassFiles("com.prupe.mcpatcher.mal.tessellator.*");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }
}
