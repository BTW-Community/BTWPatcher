package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.MAL;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

abstract public class BlockAPI {
    private static final HashMap<String, Integer> canonicalIdByName = new HashMap<String, Integer>();

    static {
        canonicalIdByName.put("minecraft:air", 0);
        canonicalIdByName.put("minecraft:stone", 1);
        canonicalIdByName.put("minecraft:grass", 2);
        canonicalIdByName.put("minecraft:dirt", 3);
        canonicalIdByName.put("minecraft:cobblestone", 4);
        canonicalIdByName.put("minecraft:planks", 5);
        canonicalIdByName.put("minecraft:sapling", 6);
        canonicalIdByName.put("minecraft:bedrock", 7);
        canonicalIdByName.put("minecraft:flowing_water", 8);
        canonicalIdByName.put("minecraft:water", 9);
        canonicalIdByName.put("minecraft:flowing_lava", 10);
        canonicalIdByName.put("minecraft:lava", 11);
        canonicalIdByName.put("minecraft:sand", 12);
        canonicalIdByName.put("minecraft:gravel", 13);
        canonicalIdByName.put("minecraft:gold_ore", 14);
        canonicalIdByName.put("minecraft:iron_ore", 15);
        canonicalIdByName.put("minecraft:coal_ore", 16);
        canonicalIdByName.put("minecraft:log", 17);
        canonicalIdByName.put("minecraft:leaves", 18);
        canonicalIdByName.put("minecraft:sponge", 19);
        canonicalIdByName.put("minecraft:glass", 20);
        canonicalIdByName.put("minecraft:lapis_ore", 21);
        canonicalIdByName.put("minecraft:lapis_block", 22);
        canonicalIdByName.put("minecraft:dispenser", 23);
        canonicalIdByName.put("minecraft:sandstone", 24);
        canonicalIdByName.put("minecraft:noteblock", 25);
        canonicalIdByName.put("minecraft:bed", 26);
        canonicalIdByName.put("minecraft:golden_rail", 27);
        canonicalIdByName.put("minecraft:detector_rail", 28);
        canonicalIdByName.put("minecraft:sticky_piston", 29);
        canonicalIdByName.put("minecraft:web", 30);
        canonicalIdByName.put("minecraft:tallgrass", 31);
        canonicalIdByName.put("minecraft:deadbush", 32);
        canonicalIdByName.put("minecraft:piston", 33);
        canonicalIdByName.put("minecraft:piston_head", 34);
        canonicalIdByName.put("minecraft:wool", 35);
        canonicalIdByName.put("minecraft:piston_extension", 36);
        canonicalIdByName.put("minecraft:yellow_flower", 37);
        canonicalIdByName.put("minecraft:red_flower", 38);
        canonicalIdByName.put("minecraft:brown_mushroom", 39);
        canonicalIdByName.put("minecraft:red_mushroom", 40);
        canonicalIdByName.put("minecraft:gold_block", 41);
        canonicalIdByName.put("minecraft:iron_block", 42);
        canonicalIdByName.put("minecraft:double_stone_slab", 43);
        canonicalIdByName.put("minecraft:stone_slab", 44);
        canonicalIdByName.put("minecraft:brick_block", 45);
        canonicalIdByName.put("minecraft:tnt", 46);
        canonicalIdByName.put("minecraft:bookshelf", 47);
        canonicalIdByName.put("minecraft:mossy_cobblestone", 48);
        canonicalIdByName.put("minecraft:obsidian", 49);
        canonicalIdByName.put("minecraft:torch", 50);
        canonicalIdByName.put("minecraft:fire", 51);
        canonicalIdByName.put("minecraft:mob_spawner", 52);
        canonicalIdByName.put("minecraft:oak_stairs", 53);
        canonicalIdByName.put("minecraft:chest", 54);
        canonicalIdByName.put("minecraft:redstone_wire", 55);
        canonicalIdByName.put("minecraft:diamond_ore", 56);
        canonicalIdByName.put("minecraft:diamond_block", 57);
        canonicalIdByName.put("minecraft:crafting_table", 58);
        canonicalIdByName.put("minecraft:wheat", 59);
        canonicalIdByName.put("minecraft:farmland", 60);
        canonicalIdByName.put("minecraft:furnace", 61);
        canonicalIdByName.put("minecraft:lit_furnace", 62);
        canonicalIdByName.put("minecraft:standing_sign", 63);
        canonicalIdByName.put("minecraft:wooden_door", 64);
        canonicalIdByName.put("minecraft:ladder", 65);
        canonicalIdByName.put("minecraft:rail", 66);
        canonicalIdByName.put("minecraft:stone_stairs", 67);
        canonicalIdByName.put("minecraft:wall_sign", 68);
        canonicalIdByName.put("minecraft:lever", 69);
        canonicalIdByName.put("minecraft:stone_pressure_plate", 70);
        canonicalIdByName.put("minecraft:iron_door", 71);
        canonicalIdByName.put("minecraft:wooden_pressure_plate", 72);
        canonicalIdByName.put("minecraft:redstone_ore", 73);
        canonicalIdByName.put("minecraft:lit_redstone_ore", 74);
        canonicalIdByName.put("minecraft:unlit_redstone_torch", 75);
        canonicalIdByName.put("minecraft:redstone_torch", 76);
        canonicalIdByName.put("minecraft:stone_button", 77);
        canonicalIdByName.put("minecraft:snow_layer", 78);
        canonicalIdByName.put("minecraft:ice", 79);
        canonicalIdByName.put("minecraft:snow", 80);
        canonicalIdByName.put("minecraft:cactus", 81);
        canonicalIdByName.put("minecraft:clay", 82);
        canonicalIdByName.put("minecraft:reeds", 83);
        canonicalIdByName.put("minecraft:jukebox", 84);
        canonicalIdByName.put("minecraft:fence", 85);
        canonicalIdByName.put("minecraft:pumpkin", 86);
        canonicalIdByName.put("minecraft:netherrack", 87);
        canonicalIdByName.put("minecraft:soul_sand", 88);
        canonicalIdByName.put("minecraft:glowstone", 89);
        canonicalIdByName.put("minecraft:portal", 90);
        canonicalIdByName.put("minecraft:lit_pumpkin", 91);
        canonicalIdByName.put("minecraft:cake", 92);
        canonicalIdByName.put("minecraft:unpowered_repeater", 93);
        canonicalIdByName.put("minecraft:powered_repeater", 94);
        canonicalIdByName.put("minecraft:chest_locked_aprilfools_super_old_legacy_we_should_not_even_have_this", 95);
        canonicalIdByName.put("minecraft:trapdoor", 96);
        canonicalIdByName.put("minecraft:monster_egg", 97);
        canonicalIdByName.put("minecraft:stonebrick", 98);
        canonicalIdByName.put("minecraft:brown_mushroom_block", 99);
        canonicalIdByName.put("minecraft:red_mushroom_block", 100);
        canonicalIdByName.put("minecraft:iron_bars", 101);
        canonicalIdByName.put("minecraft:glass_pane", 102);
        canonicalIdByName.put("minecraft:melon_block", 103);
        canonicalIdByName.put("minecraft:pumpkin_stem", 104);
        canonicalIdByName.put("minecraft:melon_stem", 105);
        canonicalIdByName.put("minecraft:vine", 106);
        canonicalIdByName.put("minecraft:fence_gate", 107);
        canonicalIdByName.put("minecraft:brick_stairs", 108);
        canonicalIdByName.put("minecraft:stone_brick_stairs", 109);
        canonicalIdByName.put("minecraft:mycelium", 110);
        canonicalIdByName.put("minecraft:waterlily", 111);
        canonicalIdByName.put("minecraft:nether_brick", 112);
        canonicalIdByName.put("minecraft:nether_brick_fence", 113);
        canonicalIdByName.put("minecraft:nether_brick_stairs", 114);
        canonicalIdByName.put("minecraft:nether_wart", 115);
        canonicalIdByName.put("minecraft:enchanting_table", 116);
        canonicalIdByName.put("minecraft:brewing_stand", 117);
        canonicalIdByName.put("minecraft:cauldron", 118);
        canonicalIdByName.put("minecraft:end_portal", 119);
        canonicalIdByName.put("minecraft:end_portal_frame", 120);
        canonicalIdByName.put("minecraft:end_stone", 121);
        canonicalIdByName.put("minecraft:dragon_egg", 122);
        canonicalIdByName.put("minecraft:redstone_lamp", 123);
        canonicalIdByName.put("minecraft:lit_redstone_lamp", 124);
        canonicalIdByName.put("minecraft:double_wooden_slab", 125);
        canonicalIdByName.put("minecraft:wooden_slab", 126);
        canonicalIdByName.put("minecraft:cocoa", 127);
        canonicalIdByName.put("minecraft:sandstone_stairs", 128);
        canonicalIdByName.put("minecraft:emerald_ore", 129);
        canonicalIdByName.put("minecraft:ender_chest", 130);
        canonicalIdByName.put("minecraft:tripwire_hook", 131);
        canonicalIdByName.put("minecraft:tripwire", 132);
        canonicalIdByName.put("minecraft:emerald_block", 133);
        canonicalIdByName.put("minecraft:spruce_stairs", 134);
        canonicalIdByName.put("minecraft:birch_stairs", 135);
        canonicalIdByName.put("minecraft:jungle_stairs", 136);
        canonicalIdByName.put("minecraft:command_block", 137);
        canonicalIdByName.put("minecraft:beacon", 138);
        canonicalIdByName.put("minecraft:cobblestone_wall", 139);
        canonicalIdByName.put("minecraft:flower_pot", 140);
        canonicalIdByName.put("minecraft:carrots", 141);
        canonicalIdByName.put("minecraft:potatoes", 142);
        canonicalIdByName.put("minecraft:wooden_button", 143);
        canonicalIdByName.put("minecraft:skull", 144);
        canonicalIdByName.put("minecraft:anvil", 145);
        canonicalIdByName.put("minecraft:trapped_chest", 146);
        canonicalIdByName.put("minecraft:light_weighted_pressure_plate", 147);
        canonicalIdByName.put("minecraft:heavy_weighted_pressure_plate", 148);
        canonicalIdByName.put("minecraft:unpowered_comparator", 149);
        canonicalIdByName.put("minecraft:powered_comparator", 150);
        canonicalIdByName.put("minecraft:daylight_detector", 151);
        canonicalIdByName.put("minecraft:redstone_block", 152);
        canonicalIdByName.put("minecraft:quartz_ore", 153);
        canonicalIdByName.put("minecraft:hopper", 154);
        canonicalIdByName.put("minecraft:quartz_block", 155);
        canonicalIdByName.put("minecraft:quartz_stairs", 156);
        canonicalIdByName.put("minecraft:activator_rail", 157);
        canonicalIdByName.put("minecraft:dropper", 158);
        canonicalIdByName.put("minecraft:stained_hardened_clay", 159);
        canonicalIdByName.put("minecraft:hay_block", 170);
        canonicalIdByName.put("minecraft:carpet", 171);
        canonicalIdByName.put("minecraft:hardened_clay", 172);
        canonicalIdByName.put("minecraft:coal_block", 173);
        canonicalIdByName.put("minecraft:packed_ice", 174);
        canonicalIdByName.put("minecraft:double_plant", 175);
    }

    private static final BlockAPI instance = MAL.newInstance(BlockAPI.class, "block");

    public static Block getFixedBlock(String name) {
        Block block = parseBlockName(name);
        if (block == null) {
            new IllegalArgumentException("unknown block " + name).printStackTrace();
        }
        return block;
    }

    public static Block parseBlockName(String name) {
        if (MCPatcherUtils.isNullOrEmpty(name)) {
            return null;
        }
        if (name.matches("\\d+")) {
            int id = Integer.parseInt(name);
            return instance.getBlockById_Impl(id);
        }
        name = getFullName(name);
        return instance.getBlockByName_Impl(name);
    }

    public static String getBlockName(Block block) {
        return block == null ? "(null)" : instance.getBlockName_Impl(block);
    }

    public static List<Block> getAllBlocks() {
        List<Block> blocks = new ArrayList<Block>();
        for (Iterator<Block> i = instance.iterator_Impl(); i.hasNext(); ) {
            Block block = i.next();
            if (block != null && !blocks.contains(block)) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    public static Block getBlockAt(IBlockAccess blockAccess, int i, int j, int k) {
        return instance.getBlockAt_Impl(blockAccess, i, j, k);
    }

    public static int getMetadataAt(IBlockAccess blockAccess, int i, int j, int k) {
        return instance.getMetadataAt_Impl(blockAccess, i, j, k);
    }

    public static Icon getBlockIcon(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        return instance.getBlockIcon_Impl(block, blockAccess, i, j, k, face);
    }

    public static boolean shouldSideBeRendered(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        return instance.shouldSideBeRendered_Impl(block, blockAccess, i, j, k, face);
    }

    // used by custom colors ItemRenderer patch in 1.6 only
    public static Block getBlockById(int id) {
        return instance.getBlockById_Impl(id);
    }

    public static String getFullName(String name) {
        return name == null ? null : name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    public static int getBlockLightValue(Block block) {
        return instance.getBlockLightValue_Impl(block);
    }

    abstract protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected int getMetadataAt_Impl(IBlockAccess blockAccess, int i, int j, int k);

    abstract protected Icon getBlockIcon_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face);

    abstract protected boolean shouldSideBeRendered_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face);

    abstract protected Iterator<Block> iterator_Impl();

    abstract protected Block getBlockById_Impl(int id);

    abstract protected Block getBlockByName_Impl(String name);

    abstract protected String getBlockName_Impl(Block block);

    abstract protected int getBlockLightValue_Impl(Block block);

    BlockAPI() {
    }

    final private static class V1 extends BlockAPI {
        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return Block.blocksList[blockAccess.getBlockId(i, j, k)];
        }

        @Override
        protected int getMetadataAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlockMetadata(i, j, k);
        }

        @Override
        protected Icon getBlockIcon_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.getBlockIcon(blockAccess, i, j, k, face);
        }

        @Override
        protected boolean shouldSideBeRendered_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.shouldSideBeRendered(blockAccess, i, j, k, face);
        }

        @Override
        protected Iterator<Block> iterator_Impl() {
            return Arrays.asList(Block.blocksList).iterator();
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return id >= 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
        }

        @Override
        protected Block getBlockByName_Impl(String name) {
            Integer id = canonicalIdByName.get(name);
            return id == null ? null : getBlockById_Impl(id);
        }

        @Override
        protected String getBlockName_Impl(Block block) {
            int id = block.blockID;
            for (Map.Entry<String, Integer> entry : canonicalIdByName.entrySet()) {
                if (id == entry.getValue()) {
                    return entry.getKey();
                }
            }
            return String.valueOf(id);
        }

        @Override
        protected int getBlockLightValue_Impl(Block block) {
            return Block.lightValue[block.blockID];
        }
    }

    private static class V2 extends BlockAPI {
        {
            File outputFile = new File("blocks17.txt");
            if (outputFile.isFile()) {
                PrintStream ps = null;
                try {
                    ps = new PrintStream(outputFile);
                    String[] nameList = new String[4096];
                    for (String name17 : Block.blockRegistry.getKeys()) {
                        Block block = Block.blockRegistry.getValue(name17);
                        if (block != null) {
                            int id = Block.blockRegistry.getId(block);
                            if (id >= 0 && id < nameList.length) {
                                nameList[id] = name17;
                            }
                        }
                    }
                    for (int id = 0; id < nameList.length; id++) {
                        if (nameList[id] != null) {
                            ps.printf("canonicalIdByName.put(\"%s\", %d);\n", nameList[id], id);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    MCPatcherUtils.close(ps);
                }
            }
        }

        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlock(i, j, k);
        }

        @Override
        protected int getMetadataAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlockMetadata(i, j, k);
        }

        @Override
        protected Icon getBlockIcon_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.getBlockIcon(blockAccess, i, j, k, face);
        }

        @Override
        protected boolean shouldSideBeRendered_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.shouldSideBeRendered(blockAccess, i, j, k, face);
        }

        @Override
        protected Iterator<Block> iterator_Impl() {
            return Block.blockRegistry.iterator();
        }

        @Override
        protected Block getBlockById_Impl(int id) {
            return Block.blockRegistry.getById(id);
        }

        @Override
        protected Block getBlockByName_Impl(String name) {
            return Block.blockRegistry.getValue(name);
        }

        @Override
        protected String getBlockName_Impl(Block block) {
            String name = Block.blockRegistry.getKey(block);
            return name == null ? String.valueOf(Block.blockRegistry.getId(block)) : name;
        }

        @Override
        protected int getBlockLightValue_Impl(Block block) {
            return block.getLightValue();
        }
    }

    final private static class V3 extends V2 {
        private static final Direction[] DIRS = new Direction[]{
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
        };

        @Override
        protected Block getBlockAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return blockAccess.getBlockState(new Position(i, j, k)).getBlock();
        }

        @Override
        protected int getMetadataAt_Impl(IBlockAccess blockAccess, int i, int j, int k) {
            return 0; // TODO
        }

        @Override
        protected Icon getBlockIcon_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.getBlockIcon(blockAccess, new Position(i, j, k), DIRS[face]);
        }

        @Override
        protected boolean shouldSideBeRendered_Impl(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
            return block.shouldSideBeRendered(blockAccess, new Position(i, j, k), DIRS[face]);
        }
    }
}
