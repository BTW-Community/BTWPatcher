package com.prupe.mcpatcher.api;

import com.prupe.mcpatcher.BaseMod;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BlockAPIMod extends Mod {
    private final int apiVersion;

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            apiVersion = 1;
        } else {
            apiVersion = 2;
        }
        version = String.valueOf(apiVersion) + ".0";
        setApiVersion("block", apiVersion);

        addClassMod(new BaseMod.BlockMod(this));
        addClassMod(new BaseMod.IBlockAccessMod(this));

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + apiVersion);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }
}
