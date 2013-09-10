package com.prupe.mcpatcher.api;

import com.prupe.mcpatcher.*;

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

        addClassMod(new BlockMod());
        addClassMod(new BaseMod.IBlockAccessMod(this));
        if (apiVersion >= 2) {
            addClassMod(new RegistryBaseMod());
            addClassMod(new RegistryMod());
        }

        addClassFile(MCPatcherUtils.BLOCK_API_CLASS);
        addClassFile(MCPatcherUtils.BLOCK_API_CLASS + "$V" + apiVersion);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BlockMod extends BaseMod.BlockMod {
        BlockMod() {
            super(BlockAPIMod.this);

            if (apiVersion == 1) {
                return;
            }

            final FieldRef blockRegistry = new FieldRef(getDeobfClass(), "blockRegistry", "LRegistry;");

            addMemberMapper(new FieldMapper(blockRegistry));
        }
    }

    private class RegistryBaseMod extends ClassMod {
        RegistryBaseMod() {
            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "newHashMap", "()Ljava/util/HashMap;"),
                new MethodRef(getDeobfClass(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getKeys", "()Ljava/util/Set;")
            ).setInterfaceOnly(false));
        }
    }

    private class RegistryMod extends ClassMod {
        RegistryMod() {
            addClassSignature(new InterfaceSignature(
                new MethodRef(getDeobfClass(), "<init>", "()V"),
                new MethodRef(getDeobfClass(), "register", "(ILjava/lang/String;Ljava/lang/Object;)V"),
                new MethodRef(getDeobfClass(), "getId", "(Ljava/lang/Object;)I"),
                new MethodRef(getDeobfClass(), "getById", "(I)Ljava/lang/Object;"),
                new MethodRef(getDeobfClass(), "getAll", "()Ljava/util/List;")
            ).setInterfaceOnly(false));
        }
    }
}
