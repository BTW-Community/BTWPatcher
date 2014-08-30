package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.FieldRef;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.MethodRef;
import com.prupe.mcpatcher.Mod;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.basemod.ext18.PositionMod;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.begin;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BiomeAPIMod extends Mod {
    private final int malVersion;

    public BiomeAPIMod() {
        name = MCPatcherUtils.BIOME_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);

        if (PositionMod.havePositionClass()) {
            malVersion = 3;
        } else if (getMinecraftVersion().compareTo("13w36a") >= 0) {
            malVersion = 2;
        } else {
            malVersion = 1;
        }
        version = String.valueOf(malVersion) + ".1";
        setMALVersion("biome", malVersion);

        ResourceLocationMod.setup(this);
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new MinecraftMod(this).mapWorldClient());
        addClassMod(new WorldMod(this));
        addClassMod(new WorldClientMod(this));
        addClassMod(new BiomeGenBaseMod());
        if (malVersion >= 3) {
            PositionMod.setup(this);
        }

        addClassFiles("com.prupe.mcpatcher.mal.biome.*");
        addClassFiles("com.prupe.mcpatcher.colormap.*");
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BiomeGenBaseMod extends com.prupe.mcpatcher.basemod.BiomeGenBaseMod {
        BiomeGenBaseMod() {
            super(BiomeAPIMod.this);

            final FieldRef waterColorMultiplier = new FieldRef(getDeobfClass(), "waterColorMultiplier", "I");
            final FieldRef temperature = new FieldRef(getDeobfClass(), "temperature", "F");
            final FieldRef rainfall = new FieldRef(getDeobfClass(), "rainfall", "F");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, waterColorMultiplier);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0xffffff),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, temperature);
                    addXref(2, rainfall);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.5f),
                        captureReference(PUTFIELD),
                        ALOAD_0,
                        push(0.5f),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addMemberMapper(new FieldMapper(color).accessFlag(AccessFlag.PUBLIC, true));

            if (malVersion == 1) {
                mapTempAndRainfall("", new byte[0]);
            } else {
                mapTempAndRainfall(PositionMod.getDescriptor(), PositionMod.passArguments(1));
            }

            addPatch(new MakeMemberPublicPatch(biomeList));
        }

        private void mapTempAndRainfall(String desc, final byte[] arguments) {
            final MethodRef getTemperaturef = new MethodRef(getDeobfClass(), "getTemperaturef", "(" + desc + ")F");
            final MethodRef getRainfallf = new MethodRef(getDeobfClass(), "getRainfallf", "()F");
            final MethodRef getGrassColor = new MethodRef(getDeobfClass(), "getGrassColor", "(" + desc + ")I");
            final MethodRef getFoliageColor = new MethodRef(getDeobfClass(), "getFoliageColor", "(" + desc + ")I");

            addClassSignature(new BytecodeSignature() {
                {
                    addXref(1, getTemperaturef);
                    addXref(2, getRainfallf);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // d = MathHelper.clampf(getTemperature(...), 0.0f, 1.0f);
                        begin(),
                        ALOAD_0,
                        arguments,
                        captureReference(INVOKEVIRTUAL),
                        push(0.0f),
                        push(1.0f),
                        anyReference(INVOKESTATIC),
                        F2D,
                        anyDSTORE,

                        // d1 = MathHelper.clampf(getRainfall(), 0.0f, 1.0f);
                        ALOAD_0,
                        captureReference(INVOKEVIRTUAL),
                        push(0.0f),
                        push(1.0f),
                        anyReference(INVOKESTATIC),
                        F2D,
                        anyDSTORE,

                        // return Colorizerxxx.yyy(d, d1);
                        anyDLOAD,
                        anyDLOAD,
                        anyReference(INVOKESTATIC),
                        IRETURN
                    );
                }
            });

            addMemberMapper(new MethodMapper(getGrassColor, getFoliageColor)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, false)
                    .accessFlag(AccessFlag.FINAL, false)
            );
        }
    }
}
