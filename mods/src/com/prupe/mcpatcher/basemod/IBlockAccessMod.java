package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches IBlockAccess interface and maps all of its methods.
 */
public class IBlockAccessMod extends com.prupe.mcpatcher.ClassMod {
    public static InterfaceMethodRef getBlock;
    public static InterfaceMethodRef getBlockId;
    public static InterfaceMethodRef getTileEntity;
    public static InterfaceMethodRef getLightBrightnessForSkyBlocks;
    public static InterfaceMethodRef getBrightness;
    public static InterfaceMethodRef getLightBrightness;
    public static InterfaceMethodRef getBlockMetadata;
    public static InterfaceMethodRef getBlockState;
    public static InterfaceMethodRef getBlockMaterial;
    public static InterfaceMethodRef isBlockOpaqueCube;
    public static InterfaceMethodRef isBlockNormalCube;
    public static InterfaceMethodRef isAirBlock;
    public static InterfaceMethodRef getBiomeGenAt;
    public static InterfaceMethodRef getHeight;
    public static InterfaceMethodRef extendedLevelsInChunkCache;
    public static InterfaceMethodRef doesBlockHaveSolidTopSurface;
    public static InterfaceMethodRef getWorldVec3Pool;
    public static InterfaceMethodRef isBlockProvidingPowerTo;
    public static InterfaceMethodRef getWorldType;

    public IBlockAccessMod(Mod mod) {
        super(mod);

        final int version;
        if (Mod.getMinecraftVersion().compareTo("13w38b") < 0) {
            version = 0;
        } else if (Mod.getMinecraftVersion().compareTo("14w10a") < 0) {
            version = 1;
        } else if (Mod.getMinecraftVersion().compareTo("14w10c") < 0) {
            version = 2;
        } else if (!IBlockStateMod.haveClass()) {
            version = 3;
        } else {
            version = 4;
        }
        final String p = PositionMod.getDescriptor();
        final String d = DirectionMod.getDescriptor();

        if (BlockMod.haveBlockRegistry()) {
            if (version >= 4) {
                getBlock = null;
            } else {
                getBlock = new InterfaceMethodRef("IBlockAccess", "getBlock", "(" + p + ")LBlock;");
            }
            getBlockId = null;
        } else {
            getBlock = null;
            getBlockId = new InterfaceMethodRef("IBlockAccess", "getBlockId", "(" + p + ")I");
        }
        getTileEntity = new InterfaceMethodRef("IBlockAccess", "getTileEntity", "(" + p + ")LTileEntity;");
        getLightBrightnessForSkyBlocks = new InterfaceMethodRef("IBlockAccess", "getLightBrightnessForSkyBlocks", "(" + p + "I)I");
        if (version > 0) {
            getBrightness = getLightBrightness = getBlockMaterial = isBlockOpaqueCube = isBlockNormalCube = doesBlockHaveSolidTopSurface = null;
        } else {
            getBrightness = new InterfaceMethodRef("IBlockAccess", "getBrightness", "(IIII)F");
            getLightBrightness = new InterfaceMethodRef("IBlockAccess", "getLightBrightness", "(III)F");
            getBlockMaterial = new InterfaceMethodRef("IBlockAccess", "getBlockMaterial", "(III)LMaterial;");
            isBlockOpaqueCube = new InterfaceMethodRef("IBlockAccess", "isBlockOpaqueCube", "(III)Z");
            isBlockNormalCube = new InterfaceMethodRef("IBlockAccess", "isBlockNormalCube", "(III)Z");
            doesBlockHaveSolidTopSurface = new InterfaceMethodRef("IBlockAccess", "doesBlockHaveSolidTopSurface", "(III)Z");
        }
        if (version >= 4) {
            getBlockMetadata = null;
            getBlockState = new InterfaceMethodRef("IBlockAccess", "getBlockState", "(" + p + ")" + "LIBlockState;");
        } else {
            getBlockMetadata = new InterfaceMethodRef("IBlockAccess", "getBlockMetadata", "(" + p + ")I");
            getBlockState = null;
        }
        if (version > 1) {
            getHeight = null;
        } else {
            getHeight = new InterfaceMethodRef("IBlockAccess", "getHeight", "()I");
        }
        if (version == 2) {
            isAirBlock = null;
        } else {
            isAirBlock = new InterfaceMethodRef("IBlockAccess", "isAirBlock", "(" + p + ")Z");
        }
        getBiomeGenAt = new InterfaceMethodRef("IBlockAccess", "getBiomeGenAt", "(" + PositionMod.getDescriptorIKOnly() + ")LBiomeGenBase;");
        extendedLevelsInChunkCache = new InterfaceMethodRef("IBlockAccess", "extendedLevelsInChunkCache", "()Z");
        if (Mod.getMinecraftVersion().compareTo("1.7.5") < 0) {
            getWorldVec3Pool = new InterfaceMethodRef("IBlockAccess", "getWorldVec3Pool", "()LVec3Pool;");
        } else {
            getWorldVec3Pool = null;
        }
        isBlockProvidingPowerTo = new InterfaceMethodRef("IBlockAccess", "isBlockProvidingPowerTo", "(" + p + d + ")I");
        if (version >= 4) {
            getWorldType = new InterfaceMethodRef("IBlockAccess", "getWorldType", "()LWorldType;");
        }

        List<InterfaceMethodRef> methods = new ArrayList<InterfaceMethodRef>();
        methods.add(getBlock);
        methods.add(getBlockId);
        methods.add(getTileEntity);
        methods.add(getLightBrightnessForSkyBlocks);
        methods.add(getBrightness);
        methods.add(getLightBrightness);
        methods.add(getBlockMetadata);
        methods.add(getBlockState);
        methods.add(getBlockMaterial);
        methods.add(isBlockOpaqueCube);
        methods.add(isBlockNormalCube);
        methods.add(isAirBlock);
        methods.add(getBiomeGenAt);
        methods.add(getHeight);
        methods.add(extendedLevelsInChunkCache);
        methods.add(doesBlockHaveSolidTopSurface);
        methods.add(getWorldVec3Pool);
        methods.add(isBlockProvidingPowerTo);
        methods.add(getWorldType);

        addClassSignature(new InterfaceSignature(methods.toArray(new InterfaceMethodRef[methods.size()])).setInterfaceOnly(true));
    }
}
