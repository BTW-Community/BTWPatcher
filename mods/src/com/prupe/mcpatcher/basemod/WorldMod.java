package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.JavaRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;

import static com.prupe.mcpatcher.BinaryRegex.any;
import static com.prupe.mcpatcher.BinaryRegex.backReference;
import static com.prupe.mcpatcher.BytecodeMatcher.anyFSTORE;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches World class.
 */
public class WorldMod extends com.prupe.mcpatcher.ClassMod {
    public static final FieldRef lightningFlash = new FieldRef("World", "lightningFlash", "I");
    public static final MethodRef getLightningFlash = new MethodRef("World", "getLightningFlash", "()I");

    public static boolean haveGetLightningFlash() {
        return Mod.getMinecraftVersion().compareTo("14w02a") >= 0;
    }

    public static int getLightningFlashOpcode() {
        return haveGetLightningFlash() ? INVOKEVIRTUAL : GETFIELD;
    }

    public static JavaRef getLightningFlashRef() {
        return haveGetLightningFlash() ? getLightningFlash : lightningFlash;
    }

    public WorldMod(Mod mod) {
        super(mod);
        setInterfaces("IBlockAccess");

        addClassSignature(new ConstSignature("ambient.cave.cave"));
        addClassSignature(new ConstSignature(0x3c6ef35f));
    }

    public WorldMod mapLightningFlash() {
        addClassSignature(new BytecodeSignature() {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // if (this.lightningFlash > 0)
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IFLE, any(2),

                    // partialFlash = (float) this.lightningFlash - partialTick;
                    ALOAD_0,
                    backReference(1),
                    I2F,
                    FLOAD_2,
                    FSUB,
                    anyFSTORE
                );
            }
        }.addXref(1, lightningFlash));

        addPatch(new MakeMemberPublicPatch(lightningFlash));

        return this;
    }
}
