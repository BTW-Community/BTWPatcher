package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import static javassist.bytecode.Opcode.*;

/**
 * Matches Minecraft class and maps the getInstance method.
 */
public class MinecraftMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LMinecraft;");
    protected final MethodRef getInstance = new MethodRef(getDeobfClass(), "getInstance", "()LMinecraft;");
    protected final boolean haveGetInstance;

    public MinecraftMod(Mod mod) {
        super(mod);
        haveGetInstance = Mod.getMinecraftVersion().compareTo("1.3") >= 0;

        if (Mod.getMinecraftVersion().compareTo("13w16a") >= 0) {
            addClassSignature(new ConstSignature("textures/gui/title/mojang.png"));
            addClassSignature(new ConstSignature("crash-reports"));
        } else {
            addClassSignature(new FilenameSignature("net/minecraft/client/Minecraft.class"));
        }

        if (haveGetInstance) {
            addMemberMapper(new MethodMapper(getInstance)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
            );
        }
    }

    public MinecraftMod mapWorldClient() {
        addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "theWorld", "LWorldClient;")));
        return this;
    }

    public MinecraftMod mapPlayer() {
        addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "thePlayer", "LEntityClientPlayerMP;")));
        return this;
    }

    public MinecraftMod addWorldGetter() {
        final MethodRef getWorld = new MethodRef(getDeobfClass(), "getWorld", "()LWorld;");

        if (Mod.getMinecraftVersion().compareTo("12w18a") >= 0) {
            final FieldRef worldServer = new FieldRef(getDeobfClass(), "worldServer", "LWorldServer;");
            final FieldRef world = new FieldRef("WorldServer", "world", "LWorld;");

            addMemberMapper(new FieldMapper(worldServer));

            addPatch(new AddMethodPatch(getWorld) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, worldServer),
                        reference(GETFIELD, world),
                        ARETURN
                    );
                }
            });
        } else {
            final FieldRef theWorld = new FieldRef(getDeobfClass(), "theWorld", "LWorld;");

            addMemberMapper(new FieldMapper(theWorld));

            addPatch(new AddMethodPatch(getWorld) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, theWorld),
                        ARETURN
                    );
                }
            });
        }
        return this;
    }
}
