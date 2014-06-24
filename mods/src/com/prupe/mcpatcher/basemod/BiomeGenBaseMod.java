package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BinaryRegex.end;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Matches BiomeGenBase class.
 */
public class BiomeGenBaseMod extends com.prupe.mcpatcher.ClassMod {
    protected final FieldRef biomeList = new FieldRef(getDeobfClass(), "biomeList", "[LBiomeGenBase;");
    protected final FieldRef biomeID = new FieldRef(getDeobfClass(), "biomeID", "I");
    protected final FieldRef biomeName = new FieldRef(getDeobfClass(), "biomeName", "Ljava/lang/String;");
    protected final MethodRef setBiomeName = new MethodRef(getDeobfClass(), "setBiomeName", "(Ljava/lang/String;)LBiomeGenBase;");

    public BiomeGenBaseMod(Mod mod) {
        super(mod);

        addClassSignature(new ConstSignature("Ocean"));
        addClassSignature(new ConstSignature("Plains"));
        addClassSignature(new ConstSignature("Desert"));

        addClassSignature(new BytecodeSignature() {
            {
                matchConstructorOnly(true);
                addXref(1, biomeID);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    ALOAD_0,
                    ILOAD_1,
                    captureReference(PUTFIELD)
                );
            }
        });

        addClassSignature(new BytecodeSignature() {
            {
                setMethod(setBiomeName);
                addXref(1, biomeName);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin(),
                    ALOAD_0,
                    ALOAD_1,
                    captureReference(PUTFIELD),
                    ALOAD_0,
                    ARETURN,
                    end()
                );
            }
        });

        addMemberMapper(new FieldMapper(biomeList)
                .accessFlag(AccessFlag.STATIC, true)
        );
    }
}
