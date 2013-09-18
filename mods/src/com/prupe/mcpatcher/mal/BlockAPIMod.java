package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;

public class BlockAPIMod extends Mod {
    private final int malVersion;

    public BlockAPIMod() {
        name = MCPatcherUtils.BLOCK_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("block", malVersion);

        addClassMod(new BlockMod());
        addClassMod(new BaseMod.IBlockAccessMod(this));
        if (malVersion >= 2) {
            addClassMod(new Shared.RegistryBaseMod(this));
            addClassMod(new Shared.RegistryMod(this));
        }

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + malVersion);
        addClassFile(MCPatcherUtils.RENDER_PASS_API_MAL_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(BlockAPIMod.this);

            final MethodRef getShortName = new MethodRef(getDeobfClass(), "getShortName", "()Ljava/lang/String;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("tile.")
                    );
                }
            }.setMethod(getShortName));

            if (malVersion == 2) {
                final FieldRef blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");

                addMemberMapper(new FieldMapper(blockRegistry));
            }
        }
    }
}
