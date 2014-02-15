package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.InterfaceMethodRef;
import com.prupe.mcpatcher.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches IBlockAccess interface and maps all of its methods.
 */
public class IBlockAccessMod extends com.prupe.mcpatcher.ClassMod {
    protected final boolean haveBlockRegistry;
    protected final boolean methodsRemoved;

    public IBlockAccessMod(Mod mod) {
        super(mod);
        haveBlockRegistry = Mod.getMinecraftVersion().compareTo("13w36a") >= 0;
        methodsRemoved = Mod.getMinecraftVersion().compareTo("13w38b") >= 0;
        final String d = PositionMod.getDescriptor();

        List<InterfaceMethodRef> tmp = new ArrayList<InterfaceMethodRef>();
        if (haveBlockRegistry) {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBlock", "(" + d + ")LBlock;"));
        } else {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBlockId", "(" + d + ")I"));
        }
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBlockTileEntity", "(" + d + ")LTileEntity;"));
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "getLightBrightnessForSkyBlocks", "(" + d + "I)I"));
        if (!methodsRemoved) {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBrightness", "(IIII)F"));
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getLightBrightness", "(III)F"));
        }
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBlockMetadata", "(" + d + ")I"));
        if (!methodsRemoved) {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBlockMaterial", "(III)LMaterial;"));
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "isBlockOpaqueCube", "(III)Z"));
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "isBlockNormalCube", "(III)Z"));
        }
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "isAirBlock", "(" + d + ")Z"));
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "getBiomeGenAt", "(" + PositionMod.getDescriptorIKOnly() + ")LBiomeGenBase;"));
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I"));
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "extendedLevelsInChunkCache", "()Z"));
        if (!methodsRemoved) {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "doesBlockHaveSolidTopSurface", "(III)Z"));
        }
        if (!PositionMod.havePositionClass()) {
            tmp.add(new InterfaceMethodRef(getDeobfClass(), "getWorldVec3Pool", "()LVec3Pool;"));
        }
        tmp.add(new InterfaceMethodRef(getDeobfClass(), "isBlockProvidingPowerTo", "(" + d + DirectionMod.getDescriptor() + ")I"));

        addClassSignature(new InterfaceSignature(tmp.toArray(new InterfaceMethodRef[tmp.size()])).setInterfaceOnly(true));
    }
}
