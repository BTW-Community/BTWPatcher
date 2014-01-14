package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

/**
 * Matches IBlockAccess interface and maps getBlockId, getBlockMetadata methods.
 */
public class IBlockAccessMod extends com.prupe.mcpatcher.ClassMod {
    protected final boolean haveBlockRegistry;
    protected final boolean methodsRemoved;

    public IBlockAccessMod(Mod mod) {
        super(mod);
        haveBlockRegistry = Mod.getMinecraftVersion().compareTo("13w36a") >= 0;
        methodsRemoved = Mod.getMinecraftVersion().compareTo("13w38b") >= 0;
        final String d = PositionMod.getDescriptor();

        addClassSignature(new InterfaceSignature(
            haveBlockRegistry ?
                new InterfaceMethodRef(getDeobfClass(), "getBlock", "(" + d + ")LBlock;") :
                new InterfaceMethodRef(getDeobfClass(), "getBlockId", "(III)I"),
            new InterfaceMethodRef(getDeobfClass(), "getBlockTileEntity", "(" + d + ")LTileEntity;"),
            new InterfaceMethodRef(getDeobfClass(), "getLightBrightnessForSkyBlocks", "(" + d + "I)I"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "getBrightness", "(IIII)F"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "getLightBrightness", "(III)F"),
            new InterfaceMethodRef(getDeobfClass(), "getBlockMetadata", "(" + d + ")I"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "getBlockMaterial", "(III)LMaterial;"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "isBlockOpaqueCube", "(III)Z"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "isBlockNormalCube", "(III)Z"),
            new InterfaceMethodRef(getDeobfClass(), "isAirBlock", "(" + d + ")Z"),
            new InterfaceMethodRef(getDeobfClass(), "getBiomeGenAt", "(" + PositionMod.getDescriptorIKOnly() + ")LBiomeGenBase;"),
            new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I"),
            new InterfaceMethodRef(getDeobfClass(), "extendedLevelsInChunkCache", "()Z"),
            methodsRemoved ?
                null : new InterfaceMethodRef(getDeobfClass(), "doesBlockHaveSolidTopSurface", "(III)Z"),
            PositionMod.havePositionClass() ?
                null : new InterfaceMethodRef(getDeobfClass(), "getWorldVec3Pool", "()LVec3Pool;"),
            new InterfaceMethodRef(getDeobfClass(), "isBlockProvidingPowerTo",
                PositionMod.havePositionClass() ? "(LPosition;LDirection;)I" : "(IIII)I")
        ).setInterfaceOnly(true));
    }
}
