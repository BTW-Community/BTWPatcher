package com.prupe.mcpatcher.mal;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class BiomeAPIMod extends Mod {
    private final int malVersion;

    public BiomeAPIMod() {
        name = MCPatcherUtils.BIOME_API_MOD;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";

        if (getMinecraftVersion().compareTo("13w36a") < 0) {
            malVersion = 1;
        } else {
            malVersion = 2;
        }
        version = String.valueOf(malVersion) + ".0";
        setMALVersion("biome", malVersion);

        addClassMod(new BaseMod.IBlockAccessMod(this));
        addClassMod(new BaseMod.MinecraftMod(this).mapWorldClient());
        addClassMod(new BaseMod.WorldMod(this));
        addClassMod(new BaseMod.WorldClientMod(this));
        addClassMod(new BiomeGenBaseMod());

        addClassFile(MCPatcherUtils.BIOME_API_CLASS);
        addClassFile(MCPatcherUtils.BIOME_API_CLASS + "$V" + malVersion);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    private class BiomeGenBaseMod extends BaseMod.BiomeGenBaseMod {
        BiomeGenBaseMod() {
            super(BiomeAPIMod.this);

            final FieldRef waterColorMultiplier = new FieldRef(getDeobfClass(), "waterColorMultiplier", "I");
            final FieldRef temperature = new FieldRef(getDeobfClass(), "temperature", "F");
            final FieldRef rainfall = new FieldRef(getDeobfClass(), "rainfall", "F");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0xffffff),
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, waterColorMultiplier)
            );

            addClassSignature(new BytecodeSignature() {
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
            }
                .matchConstructorOnly(true)
                .addXref(1, temperature)
                .addXref(2, rainfall)
            );

            addMemberMapper(new FieldMapper(color).accessFlag(AccessFlag.PUBLIC, true));

            if (malVersion == 1) {
                mapTempAndRainfall("", new int[0]);
            } else {
                mapTempAndRainfall("III", new int[]{ILOAD_1, ILOAD_2, ILOAD_3});
            }

            addPatch(new MakeMemberPublicPatch(biomeList));
        }

        private void mapTempAndRainfall(String desc, final int[] arguments) {
            final MethodRef getTemperaturef = new MethodRef(getDeobfClass(), "getTemperaturef", "(" + desc + ")F");
            final MethodRef getRainfallf = new MethodRef(getDeobfClass(), "getRainfallf", "()F");
            final MethodRef getGrassColor = new MethodRef(getDeobfClass(), "getGrassColor", "(" + desc + ")I");
            final MethodRef getFoliageColor = new MethodRef(getDeobfClass(), "getFoliageColor", "(" + desc + ")I");

            addClassSignature(new BytecodeSignature() {
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
            }
                .addXref(1, getTemperaturef)
                .addXref(2, getRainfallf)
            );

            addMemberMapper(new MethodMapper(getGrassColor, getFoliageColor)
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, false)
            );
        }
    }
}
