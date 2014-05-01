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
    public static final FieldRef instance = new FieldRef("Minecraft", "instance", "LMinecraft;");
    public static final MethodRef getInstance = new MethodRef("Minecraft", "getInstance", "()LMinecraft;");
    public static final MethodRef isAmbientOcclusionEnabled = new MethodRef("Minecraft", "isAmbientOcclusionEnabled", "()Z");
    public static final FieldRef thePlayer = new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;");
    public static final MethodRef getWorld = new MethodRef("Minecraft", "getWorld", "()LWorld;");
    public static final FieldRef world = new FieldRef("WorldServer", "world", "LWorld;");

    public static FieldRef theWorld;
    public static FieldRef worldServer;

    public final boolean haveGetInstance;

    public MinecraftMod(Mod mod) {
        super(mod);
        haveGetInstance = Mod.getMinecraftVersion().compareTo("1.3") >= 0;

        if (Mod.getMinecraftVersion().compareTo("12w18a") >= 0) {
            theWorld = new FieldRef("Minecraft", "theWorld", "LWorldClient;");
            worldServer = new FieldRef("Minecraft", "worldServer", "LWorldServer;");
        } else {
            theWorld = new FieldRef(getDeobfClass(), "theWorld", "LWorld;");
            worldServer = null;
        }
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
        addMemberMapper(new FieldMapper(theWorld));
        return this;
    }

    public MinecraftMod mapPlayer() {
        addMemberMapper(new FieldMapper(thePlayer));
        return this;
    }

    public MinecraftMod addWorldGetter() {
        if (Mod.getMinecraftVersion().compareTo("12w18a") >= 0) {
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
