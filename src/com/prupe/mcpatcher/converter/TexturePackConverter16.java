package com.prupe.mcpatcher.converter;

import com.prupe.mcpatcher.UserInterface;

import java.io.File;
import java.util.*;
import java.util.zip.ZipEntry;

public class TexturePackConverter16 extends TexturePackConverter {
    private static final String COLOR_PROPERTIES = "color.properties";
    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static final PlainEntry[] convertEntries = {
        // Blocks
        new TextureEntry("textures/blocks/activatorRail\\.png", "blocks/rail_activator.png"),
        new TextureEntry("textures/blocks/activatorRail_powered\\.png", "blocks/rail_activator_powered.png"),
        new TextureEntry("textures/blocks/detectorRail\\.png", "blocks/rail_detector.png"),
        new TextureEntry("textures/blocks/detectorRail_on\\.png", "blocks/rail_detector_powered.png"),
        new TextureEntry("textures/blocks/goldenRail\\.png", "blocks/rail_golden.png"),
        new TextureEntry("textures/blocks/goldenRail_powered\\.png", "blocks/rail_golden_powered.png"),
        new TextureEntry("textures/blocks/rail\\.png", "blocks/rail_normal.png"),
        new TextureEntry("textures/blocks/rail_turn\\.png", "blocks/rail_normal_turned.png"),
        new TextureEntry("textures/blocks/anvil_base\\.png", "blocks/anvil_base.png"),
        new TextureEntry("textures/blocks/anvil_top\\.png", "blocks/anvil_top_damaged_0.png"),
        new TextureEntry("textures/blocks/anvil_top_damaged_1\\.png", "blocks/anvil_top_damaged_1.png"),
        new TextureEntry("textures/blocks/anvil_top_damaged_2\\.png", "blocks/anvil_top_damaged_2.png"),
        new TextureEntry("textures/blocks/crops_0\\.png", "blocks/wheat_stage_0.png"),
        new TextureEntry("textures/blocks/crops_1\\.png", "blocks/wheat_stage_1.png"),
        new TextureEntry("textures/blocks/crops_2\\.png", "blocks/wheat_stage_2.png"),
        new TextureEntry("textures/blocks/crops_3\\.png", "blocks/wheat_stage_3.png"),
        new TextureEntry("textures/blocks/crops_4\\.png", "blocks/wheat_stage_4.png"),
        new TextureEntry("textures/blocks/crops_5\\.png", "blocks/wheat_stage_5.png"),
        new TextureEntry("textures/blocks/crops_6\\.png", "blocks/wheat_stage_6.png"),
        new TextureEntry("textures/blocks/crops_7\\.png", "blocks/wheat_stage_7.png"),
        new TextureEntry("textures/blocks/potatoes_0\\.png", "blocks/potatoes_stage_0.png"),
        new TextureEntry("textures/blocks/potatoes_1\\.png", "blocks/potatoes_stage_1.png"),
        new TextureEntry("textures/blocks/potatoes_2\\.png", "blocks/potatoes_stage_2.png"),
        new TextureEntry("textures/blocks/potatoes_3\\.png", "blocks/potatoes_stage_3.png"),
        new TextureEntry("textures/blocks/carrots_0\\.png", "blocks/carrots_stage_0.png"),
        new TextureEntry("textures/blocks/carrots_1\\.png", "blocks/carrots_stage_1.png"),
        new TextureEntry("textures/blocks/carrots_2\\.png", "blocks/carrots_stage_2.png"),
        new TextureEntry("textures/blocks/carrots_3\\.png", "blocks/carrots_stage_3.png"),
        new TextureEntry("textures/blocks/stem_bent\\.png", "blocks/pumpkin_stem_connected.png"),
        new TextureEntry("textures/blocks/stem_straight\\.png", "blocks/pumpkin_stem_disconnected.png"),
        new TextureEntry("textures/blocks/stem_bent\\.png", "blocks/melon_stem_connected.png"),
        new TextureEntry("textures/blocks/stem_straight\\.png", "blocks/melon_stem_disconnected.png"),
        new TextureEntry("textures/blocks/melon_side\\.png", "blocks/melon_side.png"),
        new TextureEntry("textures/blocks/melon_top\\.png", "blocks/melon_top.png"),
        new TextureEntry("textures/blocks/netherStalk_0\\.png", "blocks/nether_wart_stage_0.png"),
        new TextureEntry("textures/blocks/netherStalk_1\\.png", "blocks/nether_wart_stage_1.png"),
        new TextureEntry("textures/blocks/netherStalk_2\\.png", "blocks/nether_wart_stage_2.png"),
        new TextureEntry("textures/blocks/cloth_0\\.png", "blocks/wool_colored_white.png"),
        new TextureEntry("textures/blocks/cloth_1\\.png", "blocks/wool_colored_orange.png"),
        new TextureEntry("textures/blocks/cloth_2\\.png", "blocks/wool_colored_magenta.png"),
        new TextureEntry("textures/blocks/cloth_3\\.png", "blocks/wool_colored_light_blue.png"),
        new TextureEntry("textures/blocks/cloth_4\\.png", "blocks/wool_colored_yellow.png"),
        new TextureEntry("textures/blocks/cloth_5\\.png", "blocks/wool_colored_lime.png"),
        new TextureEntry("textures/blocks/cloth_6\\.png", "blocks/wool_colored_pink.png"),
        new TextureEntry("textures/blocks/cloth_7\\.png", "blocks/wool_colored_gray.png"),
        new TextureEntry("textures/blocks/cloth_8\\.png", "blocks/wool_colored_silver.png"),
        new TextureEntry("textures/blocks/cloth_9\\.png", "blocks/wool_colored_cyan.png"),
        new TextureEntry("textures/blocks/cloth_10\\.png", "blocks/wool_colored_purple.png"),
        new TextureEntry("textures/blocks/cloth_11\\.png", "blocks/wool_colored_blue.png"),
        new TextureEntry("textures/blocks/cloth_12\\.png", "blocks/wool_colored_brown.png"),
        new TextureEntry("textures/blocks/cloth_13\\.png", "blocks/wool_colored_green.png"),
        new TextureEntry("textures/blocks/cloth_14\\.png", "blocks/wool_colored_red.png"),
        new TextureEntry("textures/blocks/cloth_15\\.png", "blocks/wool_colored_black.png"),
        new TextureEntry("textures/blocks/cocoa_0\\.png", "blocks/cocoa_stage_0.png"),
        new TextureEntry("textures/blocks/cocoa_1\\.png", "blocks/cocoa_stage_1.png"),
        new TextureEntry("textures/blocks/cocoa_2\\.png", "blocks/cocoa_stage_2.png"),
        new TextureEntry("textures/blocks/clay\\.png", "blocks/clay.png"),
        new TextureEntry("textures/blocks/clayHardened\\.png", "blocks/hardened_clay.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_0\\.png", "blocks/hardened_clay_stained_white.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_1\\.png", "blocks/hardened_clay_stained_orange.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_2\\.png", "blocks/hardened_clay_stained_magenta.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_3\\.png", "blocks/hardened_clay_stained_light_blue.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_4\\.png", "blocks/hardened_clay_stained_yellow.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_5\\.png", "blocks/hardened_clay_stained_lime.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_6\\.png", "blocks/hardened_clay_stained_pink.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_7\\.png", "blocks/hardened_clay_stained_gray.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_8\\.png", "blocks/hardened_clay_stained_silver.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_9\\.png", "blocks/hardened_clay_stained_cyan.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_10\\.png", "blocks/hardened_clay_stained_purple.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_11\\.png", "blocks/hardened_clay_stained_blue.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_12\\.png", "blocks/hardened_clay_stained_brown.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_13\\.png", "blocks/hardened_clay_stained_green.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_14\\.png", "blocks/hardened_clay_stained_red.png"),
        new TextureEntry("textures/blocks/clayHardenedStained_15\\.png", "blocks/hardened_clay_stained_black.png"),
        new TextureEntry("textures/blocks/destroy_0\\.png", "blocks/destroy_stage_0.png"),
        new TextureEntry("textures/blocks/destroy_1\\.png", "blocks/destroy_stage_1.png"),
        new TextureEntry("textures/blocks/destroy_2\\.png", "blocks/destroy_stage_2.png"),
        new TextureEntry("textures/blocks/destroy_3\\.png", "blocks/destroy_stage_3.png"),
        new TextureEntry("textures/blocks/destroy_4\\.png", "blocks/destroy_stage_4.png"),
        new TextureEntry("textures/blocks/destroy_5\\.png", "blocks/destroy_stage_5.png"),
        new TextureEntry("textures/blocks/destroy_6\\.png", "blocks/destroy_stage_6.png"),
        new TextureEntry("textures/blocks/destroy_7\\.png", "blocks/destroy_stage_7.png"),
        new TextureEntry("textures/blocks/destroy_8\\.png", "blocks/destroy_stage_8.png"),
        new TextureEntry("textures/blocks/destroy_9\\.png", "blocks/destroy_stage_9.png"),
        new TextureEntry("textures/blocks/blockCoal\\.png", "blocks/coal_block.png"),
        new TextureEntry("textures/blocks/blockDiamond\\.png", "blocks/diamond_block.png"),
        new TextureEntry("textures/blocks/blockEmerald\\.png", "blocks/emerald_block.png"),
        new TextureEntry("textures/blocks/blockGold\\.png", "blocks/gold_block.png"),
        new TextureEntry("textures/blocks/blockIron\\.png", "blocks/iron_block.png"),
        new TextureEntry("textures/blocks/blockLapis\\.png", "blocks/lapis_block.png"),
        new TextureEntry("textures/blocks/blockRedstone\\.png", "blocks/redstone_block.png"),
        new TextureEntry("textures/blocks/oreCoal\\.png", "blocks/coal_ore.png"),
        new TextureEntry("textures/blocks/oreDiamond\\.png", "blocks/diamond_ore.png"),
        new TextureEntry("textures/blocks/oreEmerald\\.png", "blocks/emerald_ore.png"),
        new TextureEntry("textures/blocks/oreGold\\.png", "blocks/gold_ore.png"),
        new TextureEntry("textures/blocks/oreIron\\.png", "blocks/iron_ore.png"),
        new TextureEntry("textures/blocks/oreLapis\\.png", "blocks/lapis_ore.png"),
        new TextureEntry("textures/blocks/oreRedstone\\.png", "blocks/redstone_ore.png"),
        new TextureEntry("textures/blocks/rose\\.png", "blocks/flower_rose.png"),
        new TextureEntry("textures/blocks/flower\\.png", "blocks/flower_dandelion.png"),
        new TextureEntry("textures/blocks/fire_0\\.png", "blocks/fire_layer_0.png"),
        new TextureEntry("textures/blocks/fire_1\\.png", "blocks/fire_layer_1.png"),
        new TextureEntry("textures/blocks/leaves\\.png", "blocks/leaves_oak.png"),
        new TextureEntry("textures/blocks/leaves_opaque\\.png", "blocks/leaves_oak_opaque.png"),
        new TextureEntry("textures/blocks/leaves_jungle\\.png", "blocks/leaves_jungle.png"),
        new TextureEntry("textures/blocks/leaves_jungle_opaque\\.png", "blocks/leaves_jungle_opaque.png"),
        new TextureEntry("textures/blocks/leaves\\.png", "blocks/leaves_birch.png"),
        new TextureEntry("textures/blocks/leaves_opaque\\.png", "blocks/leaves_birch_opaque.png"),
        new TextureEntry("textures/blocks/leaves_spruce\\.png", "blocks/leaves_spruce.png"),
        new TextureEntry("textures/blocks/leaves_spruce_opaque\\.png", "blocks/leaves_spruce_opaque.png"),
        new TextureEntry("textures/blocks/tree_side\\.png", "blocks/log_oak.png"),
        new TextureEntry("textures/blocks/tree_jungle\\.png", "blocks/log_jungle.png"),
        new TextureEntry("textures/blocks/tree_birch\\.png", "blocks/log_birch.png"),
        new TextureEntry("textures/blocks/tree_spruce\\.png", "blocks/log_spruce.png"),
        new TextureEntry("textures/blocks/tree_top\\.png", "blocks/log_oak_top.png"),
        new TextureEntry("textures/blocks/tree_top\\.png", "blocks/log_jungle_top.png"),
        new TextureEntry("textures/blocks/tree_top\\.png", "blocks/log_birch_top.png"),
        new TextureEntry("textures/blocks/tree_top\\.png", "blocks/log_spruce_top.png"),
        new TextureEntry("textures/blocks/wood\\.png", "blocks/planks_oak.png"),
        new TextureEntry("textures/blocks/wood_birch\\.png", "blocks/planks_birch.png"),
        new TextureEntry("textures/blocks/wood_jungle\\.png", "blocks/planks_jungle.png"),
        new TextureEntry("textures/blocks/wood_spruce\\.png", "blocks/planks_spruce.png"),
        new TextureEntry("textures/blocks/sapling\\.png", "blocks/sapling_oak.png"),
        new TextureEntry("textures/blocks/sapling_birch\\.png", "blocks/sapling_birch.png"),
        new TextureEntry("textures/blocks/sapling_jungle\\.png", "blocks/sapling_jungle.png"),
        new TextureEntry("textures/blocks/sapling_spruce\\.png", "blocks/sapling_spruce.png"),
        new TextureEntry("textures/blocks/doorIron_lower\\.png", "blocks/door_iron_lower.png"),
        new TextureEntry("textures/blocks/doorIron_upper\\.png", "blocks/door_iron_upper.png"),
        new TextureEntry("textures/blocks/doorWood_lower\\.png", "blocks/door_wood_lower.png"),
        new TextureEntry("textures/blocks/doorWood_upper\\.png", "blocks/door_wood_upper.png"),
        new TextureEntry("textures/blocks/workbench_front\\.png", "blocks/crafting_table_front.png"),
        new TextureEntry("textures/blocks/workbench_side\\.png", "blocks/crafting_table_side.png"),
        new TextureEntry("textures/blocks/workbench_top\\.png", "blocks/crafting_table_top.png"),
        new TextureEntry("textures/blocks/bed_feet_end\\.png", "blocks/bed_feet_end.png"),
        new TextureEntry("textures/blocks/bed_feet_side\\.png", "blocks/bed_feet_side.png"),
        new TextureEntry("textures/blocks/bed_feet_top\\.png", "blocks/bed_feet_top.png"),
        new TextureEntry("textures/blocks/bed_head_end\\.png", "blocks/bed_head_end.png"),
        new TextureEntry("textures/blocks/bed_head_side\\.png", "blocks/bed_head_side.png"),
        new TextureEntry("textures/blocks/bed_head_top\\.png", "blocks/bed_head_top.png"),
        new TextureEntry("textures/blocks/beacon\\.png", "blocks/beacon.png"),
        new TextureEntry("textures/blocks/bedrock\\.png", "blocks/bedrock.png"),
        new TextureEntry("textures/blocks/bookshelf\\.png", "blocks/bookshelf.png"),
        new TextureEntry("textures/blocks/brewingStand\\.png", "blocks/brewing_stand.png"),
        new TextureEntry("textures/blocks/brewingStand_base\\.png", "blocks/brewing_stand_base.png"),
        new TextureEntry("textures/blocks/whiteStone\\.png", "blocks/end_stone.png"),
        new TextureEntry("textures/blocks/tnt_bottom\\.png", "blocks/tnt_bottom.png"),
        new TextureEntry("textures/blocks/tnt_side\\.png", "blocks/tnt_side.png"),
        new TextureEntry("textures/blocks/tnt_top\\.png", "blocks/tnt_top.png"),
        new TextureEntry("textures/blocks/sandstone_bottom\\.png", "blocks/sandstone_bottom.png"),
        new TextureEntry("textures/blocks/sandstone_carved\\.png", "blocks/sandstone_carved.png"),
        new TextureEntry("textures/blocks/sandstone_side\\.png", "blocks/sandstone_normal.png"),
        new TextureEntry("textures/blocks/sandstone_smooth\\.png", "blocks/sandstone_smooth.png"),
        new TextureEntry("textures/blocks/sandstone_top\\.png", "blocks/sandstone_top.png"),
        new TextureEntry("textures/blocks/cauldron_bottom\\.png", "blocks/cauldron_bottom.png"),
        new TextureEntry("textures/blocks/cauldron_inner\\.png", "blocks/cauldron_inner.png"),
        new TextureEntry("textures/blocks/cauldron_side\\.png", "blocks/cauldron_side.png"),
        new TextureEntry("textures/blocks/cauldron_top\\.png", "blocks/cauldron_top.png"),
        new TextureEntry("textures/blocks/cake_bottom\\.png", "blocks/cake_bottom.png"),
        new TextureEntry("textures/blocks/cake_inner\\.png", "blocks/cake_inner.png"),
        new TextureEntry("textures/blocks/cake_side\\.png", "blocks/cake_side.png"),
        new TextureEntry("textures/blocks/cake_top\\.png", "blocks/cake_top.png"),
        new TextureEntry("textures/blocks/quartzblock_bottom\\.png", "blocks/quartz_block_bottom.png"),
        new TextureEntry("textures/blocks/quartzblock_chiseled\\.png", "blocks/quartz_block_chiseled.png"),
        new TextureEntry("textures/blocks/quartzblock_chiseled_top\\.png", "blocks/quartz_block_chiseled_top.png"),
        new TextureEntry("textures/blocks/quartzblock_lines\\.png", "blocks/quartz_block_lines.png"),
        new TextureEntry("textures/blocks/quartzblock_lines_top\\.png", "blocks/quartz_block_lines_top.png"),
        new TextureEntry("textures/blocks/quartzblock_side\\.png", "blocks/quartz_block_side.png"),
        new TextureEntry("textures/blocks/quartzblock_top\\.png", "blocks/quartz_block_top.png"),
        new TextureEntry("textures/blocks/netherquartz\\.png", "blocks/quartz_ore.png"),
        new TextureEntry("textures/blocks/stonebrick\\.png", "blocks/cobblestone.png"),
        new TextureEntry("textures/blocks/stonebricksmooth\\.png", "blocks/stonebrick.png"),
        new TextureEntry("textures/blocks/stonebricksmooth_carved\\.png", "blocks/stonebrick_carved.png"),
        new TextureEntry("textures/blocks/stonebricksmooth_cracked\\.png", "blocks/stonebrick_cracked.png"),
        new TextureEntry("textures/blocks/stonebricksmooth_mossy\\.png", "blocks/stonebrick_mossy.png"),
        new TextureEntry("textures/blocks/stone\\.png", "blocks/stone.png"),
        new TextureEntry("textures/blocks/stoneMoss\\.png", "blocks/cobblestone_mossy.png"),
        new TextureEntry("textures/blocks/stoneslab_side\\.png", "blocks/stone_slab_side.png"),
        new TextureEntry("textures/blocks/stoneslab_top\\.png", "blocks/stone_slab_top.png"),
        new TextureEntry("textures/blocks/reeds\\.png", "blocks/reeds.png"),
        new TextureEntry("textures/blocks/sand\\.png", "blocks/sand.png"),
        new TextureEntry("textures/blocks/snow\\.png", "blocks/snow.png"),
        new TextureEntry("textures/blocks/snow_side\\.png", "blocks/grass_side_snowed.png"),
        new TextureEntry("textures/blocks/grass_side\\.png", "blocks/grass_side.png"),
        new TextureEntry("textures/blocks/grass_side_overlay\\.png", "blocks/grass_side_overlay.png"),
        new TextureEntry("textures/blocks/grass_top\\.png", "blocks/grass_top.png"),
        new TextureEntry("textures/blocks/mushroom_brown\\.png", "blocks/mushroom_brown.png"),
        new TextureEntry("textures/blocks/mushroom_red\\.png", "blocks/mushroom_red.png"),
        new TextureEntry("textures/blocks/mushroom_inside\\.png", "blocks/mushroom_block_inside.png"),
        new TextureEntry("textures/blocks/mushroom_skin_brown\\.png", "blocks/mushroom_block_skin_brown.png"),
        new TextureEntry("textures/blocks/mushroom_skin_red\\.png", "blocks/mushroom_block_skin_red.png"),
        new TextureEntry("textures/blocks/mushroom_skin_stem\\.png", "blocks/mushroom_block_skin_stem.png"),
        new TextureEntry("textures/blocks/redstoneLight\\.png", "blocks/redstone_lamp_off.png"),
        new TextureEntry("textures/blocks/redstoneLight_lit\\.png", "blocks/redstone_lamp_on.png"),
        new TextureEntry("textures/blocks/redstoneDust_cross\\.png", "blocks/redstone_dust_cross.png"),
        new TextureEntry("textures/blocks/redstoneDust_cross_overlay\\.png", "blocks/redstone_dust_cross_overlay.png"),
        new TextureEntry("textures/blocks/redstoneDust_line\\.png", "blocks/redstone_dust_line.png"),
        new TextureEntry("textures/blocks/redstoneDust_line_overlay\\.png", "blocks/redstone_dust_line_overlay.png"),
        new TextureEntry("textures/blocks/redtorch\\.png", "blocks/redstone_torch_off.png"),
        new TextureEntry("textures/blocks/redtorch_lit\\.png", "blocks/redstone_torch_on.png"),
        new TextureEntry("textures/blocks/repeater\\.png", "blocks/repeater_off.png"),
        new TextureEntry("textures/blocks/repeater_lit\\.png", "blocks/repeater_on.png"),
        new TextureEntry("textures/blocks/pumpkin_face\\.png", "blocks/pumpkin_face_off.png"),
        new TextureEntry("textures/blocks/pumpkin_jack\\.png", "blocks/pumpkin_face_on.png"),
        new TextureEntry("textures/blocks/pumpkin_side\\.png", "blocks/pumpkin_side.png"),
        new TextureEntry("textures/blocks/pumpkin_top\\.png", "blocks/pumpkin_top.png"),
        new TextureEntry("textures/blocks/piston_bottom\\.png", "blocks/piston_bottom.png"),
        new TextureEntry("textures/blocks/piston_inner_top\\.png", "blocks/piston_inner.png"),
        new TextureEntry("textures/blocks/piston_side\\.png", "blocks/piston_side.png"),
        new TextureEntry("textures/blocks/piston_top\\.png", "blocks/piston_top_normal.png"),
        new TextureEntry("textures/blocks/piston_top_sticky\\.png", "blocks/piston_top_sticky.png"),
        new TextureEntry("textures/blocks/farmland_dry\\.png", "blocks/farmland_dry.png"),
        new TextureEntry("textures/blocks/farmland_wet\\.png", "blocks/farmland_wet.png"),
        new TextureEntry("textures/blocks/endframe_eye\\.png", "blocks/endframe_eye.png"),
        new TextureEntry("textures/blocks/endframe_side\\.png", "blocks/endframe_side.png"),
        new TextureEntry("textures/blocks/endframe_top\\.png", "blocks/endframe_top.png"),
        new TextureEntry("textures/blocks/enchantment_bottom\\.png", "blocks/enchanting_table_bottom.png"),
        new TextureEntry("textures/blocks/enchantment_side\\.png", "blocks/enchanting_table_side.png"),
        new TextureEntry("textures/blocks/enchantment_top\\.png", "blocks/enchanting_table_top.png"),
        new TextureEntry("textures/blocks/water\\.png", "blocks/water_still.png"),
        new TextureEntry("textures/blocks/water_flow\\.png", "blocks/water_flow.png"),
        new TextureEntry("textures/blocks/lava\\.png", "blocks/lava_still.png"),
        new TextureEntry("textures/blocks/lava_flow\\.png", "blocks/lava_flow.png"),
        new TextureEntry("textures/blocks/lightgem\\.png", "blocks/glowstone.png"),
        new TextureEntry("textures/blocks/mobSpawner\\.png", "blocks/mob_spawner.png"),
        new TextureEntry("textures/blocks/hellrock\\.png", "blocks/netherrack.png"),
        new TextureEntry("textures/blocks/hellsand\\.png", "blocks/soul_sand.png"),
        new TextureEntry("textures/blocks/tripWire\\.png", "blocks/trip_wire.png"),
        new TextureEntry("textures/blocks/tripWireSource\\.png", "blocks/trip_wire_source.png"),
        new TextureEntry("textures/blocks/commandBlock\\.png", "blocks/command_block.png"),
        new TextureEntry("textures/blocks/dispenser_front\\.png", "blocks/dispenser_front_horizontal.png"),
        new TextureEntry("textures/blocks/dispenser_front_vertical\\.png", "blocks/dispenser_front_vertical.png"),
        new TextureEntry("textures/blocks/dropper_front\\.png", "blocks/dropper_front_horizontal.png"),
        new TextureEntry("textures/blocks/dropper_front_vertical\\.png", "blocks/dropper_front_vertical.png"),
        new TextureEntry("textures/blocks/furnace_front\\.png", "blocks/furnace_front_off.png"),
        new TextureEntry("textures/blocks/furnace_front_lit\\.png", "blocks/furnace_front_on.png"),
        new TextureEntry("textures/blocks/furnace_side\\.png", "blocks/furnace_side.png"),
        new TextureEntry("textures/blocks/furnace_top\\.png", "blocks/furnace_top.png"),
        new TextureEntry("textures/blocks/itemframe_back\\.png", "blocks/itemframe_background.png"),
        new TextureEntry("textures/blocks/hopper\\.png", "blocks/hopper_outside.png"),
        new TextureEntry("textures/blocks/hopper_inside\\.png", "blocks/hopper_inside.png"),
        new TextureEntry("textures/blocks/hopper_top\\.png", "blocks/hopper_top.png"),
        new TextureEntry("textures/blocks/thinglass_top\\.png", "blocks/glass_pane_top.png"),
        new TextureEntry("textures/blocks/cactus_bottom\\.png", "blocks/cactus_bottom.png"),
        new TextureEntry("textures/blocks/cactus_side\\.png", "blocks/cactus_side.png"),
        new TextureEntry("textures/blocks/cactus_top\\.png", "blocks/cactus_top.png"),
        new TextureEntry("textures/blocks/daylightDetector_side\\.png", "blocks/daylight_detector_side.png"),
        new TextureEntry("textures/blocks/daylightDetector_top\\.png", "blocks/daylight_detector_top.png"),
        new TextureEntry("textures/blocks/jukebox_top\\.png", "blocks/jukebox_top.png"),
        new TextureEntry("textures/blocks/musicBlock\\.png", "blocks/jukebox_side.png"),
        new TextureEntry("textures/blocks/musicBlock\\.png", "blocks/noteblock.png"),
        new TextureEntry("textures/blocks/comparator\\.png", "blocks/comparator_off.png"),
        new TextureEntry("textures/blocks/comparator_lit\\.png", "blocks/comparator_on.png"),
        new TextureEntry("textures/blocks/fenceIron\\.png", "blocks/iron_bars.png"),
        new TextureEntry("textures/blocks/hayBlock\\.png", "blocks/hay_block_side.png"),
        new TextureEntry("textures/blocks/hayBlock_top\\.png", "blocks/hay_block_top.png"),
        new TextureEntry("textures/blocks/brick\\.png", "blocks/brick.png"),
        new TextureEntry("textures/blocks/deadbush\\.png", "blocks/deadbush.png"),
        new TextureEntry("textures/blocks/dirt\\.png", "blocks/dirt.png"),
        new TextureEntry("textures/blocks/dragonEgg\\.png", "blocks/dragon_egg.png"),
        new TextureEntry("textures/blocks/fern\\.png", "blocks/fern.png"),
        new TextureEntry("textures/blocks/flowerPot\\.png", "blocks/flower_pot.png"),
        new TextureEntry("textures/blocks/glass\\.png", "blocks/glass.png"),
        new TextureEntry("textures/blocks/gravel\\.png", "blocks/gravel.png"),
        new TextureEntry("textures/blocks/ice\\.png", "blocks/ice.png"),
        new TextureEntry("textures/blocks/ladder\\.png", "blocks/ladder.png"),
        new TextureEntry("textures/blocks/lever\\.png", "blocks/lever.png"),
        new TextureEntry("textures/blocks/mycel_side\\.png", "blocks/mycelium_side.png"),
        new TextureEntry("textures/blocks/mycel_top\\.png", "blocks/mycelium_top.png"),
        new TextureEntry("textures/blocks/netherBrick\\.png", "blocks/nether_brick.png"),
        new TextureEntry("textures/blocks/obsidian\\.png", "blocks/obsidian.png"),
        new TextureEntry("textures/blocks/portal\\.png", "blocks/portal.png"),
        new TextureEntry("textures/blocks/sponge\\.png", "blocks/sponge.png"),
        new TextureEntry("textures/blocks/tallgrass\\.png", "blocks/tallgrass.png"),
        new TextureEntry("textures/blocks/torch\\.png", "blocks/torch_on.png"),
        new TextureEntry("textures/blocks/trapdoor\\.png", "blocks/trapdoor.png"),
        new TextureEntry("textures/blocks/vine\\.png", "blocks/vine.png"),
        new TextureEntry("textures/blocks/waterlily\\.png", "blocks/waterlily.png"),
        new TextureEntry("textures/blocks/web\\.png", "blocks/web.png"),

        // Items
        new TextureEntry("textures/items/slot_empty_boots\\.png", "items/empty_armor_slot_boots.png"),
        new TextureEntry("textures/items/slot_empty_chestplate\\.png", "items/empty_armor_slot_chestplate.png"),
        new TextureEntry("textures/items/slot_empty_helmet\\.png", "items/empty_armor_slot_helmet.png"),
        new TextureEntry("textures/items/slot_empty_leggings\\.png", "items/empty_armor_slot_leggings.png"),
        new TextureEntry("textures/items/bootsChain\\.png", "items/chainmail_boots.png"),
        new TextureEntry("textures/items/chestplateChain\\.png", "items/chainmail_chestplate.png"),
        new TextureEntry("textures/items/helmetChain\\.png", "items/chainmail_helmet.png"),
        new TextureEntry("textures/items/leggingsChain\\.png", "items/chainmail_leggings.png"),
        new TextureEntry("textures/items/bootsCloth\\.png", "items/leather_boots.png"),
        new TextureEntry("textures/items/chestplateCloth\\.png", "items/leather_chestplate.png"),
        new TextureEntry("textures/items/helmetCloth\\.png", "items/leather_helmet.png"),
        new TextureEntry("textures/items/leggingsCloth\\.png", "items/leather_leggings.png"),
        new TextureEntry("textures/items/bootsCloth_overlay\\.png", "items/leather_boots_overlay.png"),
        new TextureEntry("textures/items/chestplateCloth_overlay\\.png", "items/leather_chestplate_overlay.png"),
        new TextureEntry("textures/items/helmetCloth_overlay\\.png", "items/leather_helmet_overlay.png"),
        new TextureEntry("textures/items/leggingsCloth_overlay\\.png", "items/leather_leggings_overlay.png"),
        new TextureEntry("textures/items/bootsDiamond\\.png", "items/diamond_boots.png"),
        new TextureEntry("textures/items/chestplateDiamond\\.png", "items/diamond_chestplate.png"),
        new TextureEntry("textures/items/helmetDiamond\\.png", "items/diamond_helmet.png"),
        new TextureEntry("textures/items/leggingsDiamond\\.png", "items/diamond_leggings.png"),
        new TextureEntry("textures/items/hatchetDiamond\\.png", "items/diamond_axe.png"),
        new TextureEntry("textures/items/hoeDiamond\\.png", "items/diamond_hoe.png"),
        new TextureEntry("textures/items/pickaxeDiamond\\.png", "items/diamond_pickaxe.png"),
        new TextureEntry("textures/items/shovelDiamond\\.png", "items/diamond_shovel.png"),
        new TextureEntry("textures/items/swordDiamond\\.png", "items/diamond_sword.png"),
        new TextureEntry("textures/items/horsearmordiamond\\.png", "items/diamond_horse_armor.png"),
        new TextureEntry("textures/items/diamond\\.png", "items/diamond.png"),
        new TextureEntry("textures/items/bootsGold\\.png", "items/gold_boots.png"),
        new TextureEntry("textures/items/chestplateGold\\.png", "items/gold_chestplate.png"),
        new TextureEntry("textures/items/helmetGold\\.png", "items/gold_helmet.png"),
        new TextureEntry("textures/items/leggingsGold\\.png", "items/gold_leggings.png"),
        new TextureEntry("textures/items/hatchetGold\\.png", "items/gold_axe.png"),
        new TextureEntry("textures/items/hoeGold\\.png", "items/gold_hoe.png"),
        new TextureEntry("textures/items/pickaxeGold\\.png", "items/gold_pickaxe.png"),
        new TextureEntry("textures/items/shovelGold\\.png", "items/gold_shovel.png"),
        new TextureEntry("textures/items/swordGold\\.png", "items/gold_sword.png"),
        new TextureEntry("textures/items/horsearmorgold\\.png", "items/gold_horse_armor.png"),
        new TextureEntry("textures/items/ingotGold\\.png", "items/gold_ingot.png"),
        new TextureEntry("textures/items/goldNugget\\.png", "items/gold_nugget.png"),
        new TextureEntry("textures/items/bootsIron\\.png", "items/iron_boots.png"),
        new TextureEntry("textures/items/chestplateIron\\.png", "items/iron_chestplate.png"),
        new TextureEntry("textures/items/helmetIron\\.png", "items/iron_helmet.png"),
        new TextureEntry("textures/items/leggingsIron\\.png", "items/iron_leggings.png"),
        new TextureEntry("textures/items/hatchetIron\\.png", "items/iron_axe.png"),
        new TextureEntry("textures/items/hoeIron\\.png", "items/iron_hoe.png"),
        new TextureEntry("textures/items/pickaxeIron\\.png", "items/iron_pickaxe.png"),
        new TextureEntry("textures/items/shovelIron\\.png", "items/iron_shovel.png"),
        new TextureEntry("textures/items/swordIron\\.png", "items/iron_sword.png"),
        new TextureEntry("textures/items/horsearmormetal\\.png", "items/iron_horse_armor.png"),
        new TextureEntry("textures/items/ingotIron\\.png", "items/iron_ingot.png"),
        new TextureEntry("textures/items/hatchetStone\\.png", "items/stone_axe.png"),
        new TextureEntry("textures/items/hoeStone\\.png", "items/stone_hoe.png"),
        new TextureEntry("textures/items/pickaxeStone\\.png", "items/stone_pickaxe.png"),
        new TextureEntry("textures/items/shovelStone\\.png", "items/stone_shovel.png"),
        new TextureEntry("textures/items/swordStone\\.png", "items/stone_sword.png"),
        new TextureEntry("textures/items/hatchetWood\\.png", "items/wood_axe.png"),
        new TextureEntry("textures/items/hoeWood\\.png", "items/wood_hoe.png"),
        new TextureEntry("textures/items/pickaxeWood\\.png", "items/wood_pickaxe.png"),
        new TextureEntry("textures/items/shovelWood\\.png", "items/wood_shovel.png"),
        new TextureEntry("textures/items/swordWood\\.png", "items/wood_sword.png"),
        new TextureEntry("textures/items/dyePowder_black\\.png", "items/dye_powder_black.png"),
        new TextureEntry("textures/items/dyePowder_blue\\.png", "items/dye_powder_blue.png"),
        new TextureEntry("textures/items/dyePowder_brown\\.png", "items/dye_powder_brown.png"),
        new TextureEntry("textures/items/dyePowder_cyan\\.png", "items/dye_powder_cyan.png"),
        new TextureEntry("textures/items/dyePowder_gray\\.png", "items/dye_powder_gray.png"),
        new TextureEntry("textures/items/dyePowder_green\\.png", "items/dye_powder_green.png"),
        new TextureEntry("textures/items/dyePowder_lightBlue\\.png", "items/dye_powder_light_blue.png"),
        new TextureEntry("textures/items/dyePowder_lime\\.png", "items/dye_powder_lime.png"),
        new TextureEntry("textures/items/dyePowder_magenta\\.png", "items/dye_powder_magenta.png"),
        new TextureEntry("textures/items/dyePowder_orange\\.png", "items/dye_powder_orange.png"),
        new TextureEntry("textures/items/dyePowder_pink\\.png", "items/dye_powder_pink.png"),
        new TextureEntry("textures/items/dyePowder_purple\\.png", "items/dye_powder_purple.png"),
        new TextureEntry("textures/items/dyePowder_red\\.png", "items/dye_powder_red.png"),
        new TextureEntry("textures/items/dyePowder_silver\\.png", "items/dye_powder_silver.png"),
        new TextureEntry("textures/items/dyePowder_white\\.png", "items/dye_powder_white.png"),
        new TextureEntry("textures/items/dyePowder_yellow\\.png", "items/dye_powder_yellow.png"),
        new TextureEntry("textures/items/record_11\\.png", "items/record_11.png"),
        new TextureEntry("textures/items/record_13\\.png", "items/record_13.png"),
        new TextureEntry("textures/items/record_blocks\\.png", "items/record_blocks.png"),
        new TextureEntry("textures/items/record_cat\\.png", "items/record_cat.png"),
        new TextureEntry("textures/items/record_chirp\\.png", "items/record_chirp.png"),
        new TextureEntry("textures/items/record_far\\.png", "items/record_far.png"),
        new TextureEntry("textures/items/record_mall\\.png", "items/record_mall.png"),
        new TextureEntry("textures/items/record_mellohi\\.png", "items/record_mellohi.png"),
        new TextureEntry("textures/items/record_stal\\.png", "items/record_stal.png"),
        new TextureEntry("textures/items/record_strad\\.png", "items/record_strad.png"),
        new TextureEntry("textures/items/record_wait\\.png", "items/record_wait.png"),
        new TextureEntry("textures/items/record_ward\\.png", "items/record_ward.png"),
        new TextureEntry("textures/items/minecart\\.png", "items/minecart_normal.png"),
        new TextureEntry("textures/items/minecartChest\\.png", "items/minecart_chest.png"),
        new TextureEntry("textures/items/minecartFurnace\\.png", "items/minecart_furnace.png"),
        new TextureEntry("textures/items/minecartHopper\\.png", "items/minecart_hopper.png"),
        new TextureEntry("textures/items/minecartTnt\\.png", "items/minecart_tnt.png"),
        new TextureEntry("textures/items/skull_char\\.png", "items/skull_steve.png"),
        new TextureEntry("textures/items/skull_creeper\\.png", "items/skull_creeper.png"),
        new TextureEntry("textures/items/skull_skeleton\\.png", "items/skull_skeleton.png"),
        new TextureEntry("textures/items/skull_wither\\.png", "items/skull_wither.png"),
        new TextureEntry("textures/items/skull_zombie\\.png", "items/skull_zombie.png"),
        new TextureEntry("textures/items/bow\\.png", "items/bow_standby.png"),
        new TextureEntry("textures/items/bow_pull_0\\.png", "items/bow_pulling_0.png"),
        new TextureEntry("textures/items/bow_pull_1\\.png", "items/bow_pulling_1.png"),
        new TextureEntry("textures/items/bow_pull_2\\.png", "items/bow_pulling_2.png"),
        new TextureEntry("textures/items/doorIron\\.png", "items/door_iron.png"),
        new TextureEntry("textures/items/doorWood\\.png", "items/door_wood.png"),
        new TextureEntry("textures/items/book\\.png", "items/book_normal.png"),
        new TextureEntry("textures/items/writingBook\\.png", "items/book_writable.png"),
        new TextureEntry("textures/items/writtenBook\\.png", "items/book_written.png"),
        new TextureEntry("textures/items/enchantedBook\\.png", "items/book_enchanted.png"),
        new TextureEntry("textures/items/yellowDust\\.png", "items/glowstone_dust.png"),
        new TextureEntry("textures/items/monsterPlacer\\.png", "items/spawn_egg.png"),
        new TextureEntry("textures/items/monsterPlacer_overlay\\.png", "items/spawn_egg_overlay.png"),
        new TextureEntry("textures/items/fireworks\\.png", "items/fireworks.png"),
        new TextureEntry("textures/items/fireworksCharge\\.png", "items/fireworks_charge.png"),
        new TextureEntry("textures/items/fireworksCharge_overlay\\.png", "items/fireworks_charge_overlay.png"),
        new TextureEntry("textures/items/potion\\.png", "items/potion_bottle_drinkable.png"),
        new TextureEntry("textures/items/potion_splash\\.png", "items/potion_bottle_splash.png"),
        new TextureEntry("textures/items/potion_contents\\.png", "items/potion_overlay.png"),
        new TextureEntry("textures/items/fishingRod\\.png", "items/fishing_rod_uncast.png"),
        new TextureEntry("textures/items/fishingRod_empty\\.png", "items/fishing_rod_cast.png"),
        new TextureEntry("textures/items/melon\\.png", "items/melon.png"),
        new TextureEntry("textures/items/speckledMelon\\.png", "items/melon_speckled.png"),
        new TextureEntry("textures/items/bucket\\.png", "items/bucket_empty.png"),
        new TextureEntry("textures/items/bucketLava\\.png", "items/bucket_lava.png"),
        new TextureEntry("textures/items/bucketWater\\.png", "items/bucket_water.png"),
        new TextureEntry("textures/items/milk\\.png", "items/bucket_milk.png"),
        new TextureEntry("textures/items/carrots\\.png", "items/carrot.png"),
        new TextureEntry("textures/items/carrotGolden\\.png", "items/carrot_golden.png"),
        new TextureEntry("textures/items/seeds\\.png", "items/seeds_wheat.png"),
        new TextureEntry("textures/items/seeds_melon\\.png", "items/seeds_melon.png"),
        new TextureEntry("textures/items/seeds_pumpkin\\.png", "items/seeds_pumpkin.png"),
        new TextureEntry("textures/items/emptyMap\\.png", "items/map_empty.png"),
        new TextureEntry("textures/items/map\\.png", "items/map_filled.png"),
        new TextureEntry("textures/items/spiderEye\\.png", "items/spider_eye.png"),
        new TextureEntry("textures/items/fermentedSpiderEye\\.png", "items/spider_eye_fermented.png"),
        new TextureEntry("textures/items/glassBottle\\.png", "items/potion_bottle_empty.png"),
        new TextureEntry("textures/items/expBottle\\.png", "items/experience_bottle.png"),
        new TextureEntry("textures/items/blazePowder\\.png", "items/blaze_powder.png"),
        new TextureEntry("textures/items/blazeRod\\.png", "items/blaze_rod.png"),
        new TextureEntry("textures/items/beefCooked\\.png", "items/beef_cooked.png"),
        new TextureEntry("textures/items/beefRaw\\.png", "items/beef_raw.png"),
        new TextureEntry("textures/items/porkchopCooked\\.png", "items/porkchop_cooked.png"),
        new TextureEntry("textures/items/porkchopRaw\\.png", "items/porkchop_raw.png"),
        new TextureEntry("textures/items/potato\\.png", "items/potato.png"),
        new TextureEntry("textures/items/potatoBaked\\.png", "items/potato_baked.png"),
        new TextureEntry("textures/items/potatoPoisonous\\.png", "items/potato_poisonous.png"),
        new TextureEntry("textures/items/fishCooked\\.png", "items/fish_cooked.png"),
        new TextureEntry("textures/items/fishRaw\\.png", "items/fish_raw.png"),
        new TextureEntry("textures/items/chickenCooked\\.png", "items/chicken_cooked.png"),
        new TextureEntry("textures/items/chickenRaw\\.png", "items/chicken_raw.png"),
        new TextureEntry("textures/items/enderPearl\\.png", "items/ender_pearl.png"),
        new TextureEntry("textures/items/eyeOfEnder\\.png", "items/ender_eye.png"),
        new TextureEntry("textures/items/frame\\.png", "items/item_frame.png"),
        new TextureEntry("textures/items/redstone\\.png", "items/redstone_dust.png"),
        new TextureEntry("textures/items/sulphur\\.png", "items/gunpowder.png"),
        new TextureEntry("textures/items/netherStalkSeeds\\.png", "items/nether_wart.png"),
        new TextureEntry("textures/items/netherquartz\\.png", "items/quartz.png"),
        new TextureEntry("textures/items/leash\\.png", "items/lead.png"),
        new TextureEntry("textures/items/apple\\.png", "items/apple.png"),
        new TextureEntry("textures/items/appleGold\\.png", "items/apple_golden.png"),
        new TextureEntry("textures/items/arrow\\.png", "items/arrow.png"),
        new TextureEntry("textures/items/bed\\.png", "items/bed.png"),
        new TextureEntry("textures/items/boat\\.png", "items/boat.png"),
        new TextureEntry("textures/items/bone\\.png", "items/bone.png"),
        new TextureEntry("textures/items/bowl\\.png", "items/bowl.png"),
        new TextureEntry("textures/items/bread\\.png", "items/bread.png"),
        new TextureEntry("textures/items/brewingStand\\.png", "items/brewing_stand.png"),
        new TextureEntry("textures/items/brick\\.png", "items/brick.png"),
        new TextureEntry("textures/items/cake\\.png", "items/cake.png"),
        new TextureEntry("textures/items/carrotOnAStick\\.png", "items/carrot_on_a_stick.png"),
        new TextureEntry("textures/items/cauldron\\.png", "items/cauldron.png"),
        new TextureEntry("textures/items/charcoal\\.png", "items/charcoal.png"),
        new TextureEntry("textures/items/clay\\.png", "items/clay_ball.png"),
        new TextureEntry("textures/items/clock\\.png", "items/clock.png"),
        new TextureEntry("textures/items/coal\\.png", "items/coal.png"),
        new TextureEntry("textures/items/comparator\\.png", "items/comparator.png"),
        new TextureEntry("textures/items/compass\\.png", "items/compass.png"),
        new TextureEntry("textures/items/cookie\\.png", "items/cookie.png"),
        new TextureEntry("textures/items/diode\\.png", "items/repeater.png"),
        new TextureEntry("textures/items/egg\\.png", "items/egg.png"),
        new TextureEntry("textures/items/emerald\\.png", "items/emerald.png"),
        new TextureEntry("textures/items/feather\\.png", "items/feather.png"),
        new TextureEntry("textures/items/fireball\\.png", "items/fireball.png"),
        new TextureEntry("textures/items/flint\\.png", "items/flint.png"),
        new TextureEntry("textures/items/flintAndSteel\\.png", "items/flint_and_steel.png"),
        new TextureEntry("textures/items/flowerPot\\.png", "items/flower_pot.png"),
        new TextureEntry("textures/items/ghastTear\\.png", "items/ghast_tear.png"),
        new TextureEntry("textures/items/hopper\\.png", "items/hopper.png"),
        new TextureEntry("textures/items/leather\\.png", "items/leather.png"),
        new TextureEntry("textures/items/magmaCream\\.png", "items/magma_cream.png"),
        new TextureEntry("textures/items/mushroomStew\\.png", "items/mushroom_stew.png"),
        new TextureEntry("textures/items/nameTag\\.png", "items/name_tag.png"),
        new TextureEntry("textures/items/netherbrick\\.png", "items/netherbrick.png"),
        new TextureEntry("textures/items/netherStar\\.png", "items/nether_star.png"),
        new TextureEntry("textures/items/painting\\.png", "items/painting.png"),
        new TextureEntry("textures/items/paper\\.png", "items/paper.png"),
        new TextureEntry("textures/items/pumpkinPie\\.png", "items/pumpkin_pie.png"),
        new TextureEntry("textures/items/quiver\\.png", "items/quiver.png"),
        new TextureEntry("textures/items/reeds\\.png", "items/reeds.png"),
        new TextureEntry("textures/items/rottenFlesh\\.png", "items/rotten_flesh.png"),
        new TextureEntry("textures/items/ruby\\.png", "items/ruby.png"),
        new TextureEntry("textures/items/saddle\\.png", "items/saddle.png"),
        new TextureEntry("textures/items/shears\\.png", "items/shears.png"),
        new TextureEntry("textures/items/sign\\.png", "items/sign.png"),
        new TextureEntry("textures/items/slimeball\\.png", "items/slimeball.png"),
        new TextureEntry("textures/items/snowball\\.png", "items/snowball.png"),
        new TextureEntry("textures/items/stick\\.png", "items/stick.png"),
        new TextureEntry("textures/items/string\\.png", "items/string.png"),
        new TextureEntry("textures/items/sugar\\.png", "items/sugar.png"),
        new TextureEntry("textures/items/wheat\\.png", "items/wheat.png"),

        // Fonts
        new TextureEntry("font/alternate\\.(.*)", "font/ascii_sga.$1"),
        new TextureEntry("font/default\\.(.*)", "font/ascii.$1"),
        new TextureEntry("font/glyph_(..)\\.png", "font/unicode_page_$1.png"),
        new DeleteEntry("font/glyph_sizes.bin"),

        // Armor
        new TextureEntry("armor/chain_1\\.png", "models/armor/chainmail_layer_1.png"),
        new TextureEntry("armor/chain_2\\.png", "models/armor/chainmail_layer_2.png"),
        new TextureEntry("armor/cloth_1\\.png", "models/armor/leather_layer_1.png"),
        new TextureEntry("armor/cloth_1_b\\.png", "models/armor/leather_layer_1_overlay.png"),
        new TextureEntry("armor/cloth_2\\.png", "models/armor/leather_layer_2.png"),
        new TextureEntry("armor/cloth_2_b\\.png", "models/armor/leather_layer_2_overlay.png"),
        new TextureEntry("armor/diamond_1\\.png", "models/armor/diamond_layer_1.png"),
        new TextureEntry("armor/diamond_2\\.png", "models/armor/diamond_layer_2.png"),
        new TextureEntry("armor/gold_1\\.png", "models/armor/gold_layer_1.png"),
        new TextureEntry("armor/gold_2\\.png", "models/armor/gold_layer_2.png"),
        new TextureEntry("armor/iron_1\\.png", "models/armor/iron_layer_1.png"),
        new TextureEntry("armor/iron_2\\.png", "models/armor/iron_layer_2.png"),

        // GUI
        new TextureEntry("gui/alchemy\\.png", "gui/container/brewing_stand.png"),
        new TextureEntry("gui/allitems\\.png", "gui/container/creative_inventory/tabs.png"),
        new TextureEntry("gui/background\\.png", "gui/options_background.png"),
        new TextureEntry("gui/beacon\\.png", "gui/container/beacon.png"),
        new TextureEntry("gui/book\\.png", "gui/book.png"),
        new TextureEntry("gui/container\\.png", "gui/container/generic_54.png"),
        new TextureEntry("gui/crafting\\.png", "gui/container/crafting_table.png"),
        new DeleteEntry("gui/crash_logo\\.png"),
        new TextureEntry("gui/creative_inv/list_items\\.png", "gui/container/creative_inventory/tab_items.png"),
        new TextureEntry("gui/creative_inv/search\\.png", "gui/container/creative_inventory/tab_item_search.png"),
        new TextureEntry("gui/creative_inv/survival_inv\\.png", "gui/container/creative_inventory/tab_inventory.png"),
        new TextureEntry("gui/demo_bg\\.png", "gui/demo_background.png"),
        new TextureEntry("gui/enchant\\.png", "gui/container/enchanting_table.png"),
        new TextureEntry("gui/furnace\\.png", "gui/container/furnace.png"),
        new TextureEntry("gui/gui\\.png", "gui/widgets.png"),
        new TextureEntry("gui/hopper\\.png", "gui/container/hopper.png"),
        new TextureEntry("gui/horseinv\\.png", "gui/container/horse.png"),
        new TextureEntry("gui/icons\\.png", "gui/icons.png"),
        new TextureEntry("gui/inventory\\.png", "gui/container/inventory.png"),
        new DeleteEntry("gui/particles\\.png"),
        new TextureEntry("gui/repair\\.png", "gui/container/anvil.png"),
        new TextureEntry("gui/slot\\.png", "gui/container/stats_icons.png"),
        new TextureEntry("gui/trading\\.png", "gui/container/villager.png"),
        new TextureEntry("gui/trap\\.png", "gui/container/dispenser.png"),
        new TextureEntry("title/bg/panorama0\\.png", "gui/title/background/panorama_0.png"),
        new TextureEntry("title/bg/panorama1\\.png", "gui/title/background/panorama_1.png"),
        new TextureEntry("title/bg/panorama2\\.png", "gui/title/background/panorama_2.png"),
        new TextureEntry("title/bg/panorama3\\.png", "gui/title/background/panorama_3.png"),
        new TextureEntry("title/bg/panorama4\\.png", "gui/title/background/panorama_4.png"),
        new TextureEntry("title/bg/panorama5\\.png", "gui/title/background/panorama_5.png"),
        new DeleteEntry("title/black\\.png"),
        new PlainEntry("title/credits\\.txt", "texts/credits.txt"),
        new TextureEntry("title/mclogo\\.png", "gui/title/minecraft.png"),
        new TextureEntry("title/mojang\\.png", "gui/title/mojang.png"),
        new PlainEntry("title/splashes\\.txt", "texts/splashes.txt"),
        new PlainEntry("title/win\\.txt", "texts/end.txt"),
        new TextureEntry("achievement/bg\\.png", "gui/achievement/achievement_background.png"),
        new TextureEntry("achievement/icons\\.png", "gui/achievement/achievement_icons.png"),

        // Misc
        new TextureEntry("gui/unknown_pack\\.png", "misc/unknown_pack.png"),
        new TextureEntry("art/kz\\.png", "painting/paintings_kristoffer_zetterstrand.png"),
        new DeleteEntry("misc/dial\\.png"),
        new DeleteEntry("misc/beacon\\.png"),
        new TextureEntry("misc/explosion\\.png", "entity/explosion.png"),
        new TextureEntry("misc/foliagecolor\\.png", "colormap/foliage.png"),
        new TextureEntry("misc/footprint\\.png", "particle/footprint.png"),
        new TextureEntry("misc/glint\\.png", "misc/enchanted_item_glint.png"),
        new TextureEntry("misc/grasscolor\\.png", "colormap/grass.png"),
        new TextureEntry("misc/mapbg\\.png", "map/map_background.png"),
        new TextureEntry("misc/mapicons\\.png", "map/map_icons.png"),
        new TextureEntry("misc/particlefield\\.png", "entity/end_portal.png"),
        new TextureEntry("misc/pumpkinblur\\.png", "misc/pumpkinblur.png"),
        new TextureEntry("misc/shadow\\.png", "misc/shadow.png"),
        new TextureEntry("misc/tunnel\\.png", "environment/end_sky.png"),
        new TextureEntry("misc/vignette\\.png", "misc/vignette.png"),
        new TextureEntry("misc/water\\.png", "misc/underwater.png"),
        new DeleteEntry("misc/watercolor\\.png"),
        new TextureEntry("particles\\.png", "particle/particles.png"),

        // Non-mob entities
        new TextureEntry("item/arrows\\.png", "entity/arrow.png"),
        new TextureEntry("item/boat\\.png", "entity/boat.png"),
        new TextureEntry("item/book\\.png", "entity/enchanting_table_book.png"),
        new TextureEntry("item/cart\\.png", "entity/minecart.png"),
        new TextureEntry("item/chest\\.png", "entity/chest/normal.png"),
        new TextureEntry("item/chests/trap_large\\.png", "entity/chest/trapped_double.png"),
        new TextureEntry("item/chests/trap_small\\.png", "entity/chest/trapped.png"),
        new TextureEntry("item/enderchest\\.png", "entity/chest/ender.png"),
        new TextureEntry("item/largechest\\.png", "entity/chest/normal_double.png"),
        new TextureEntry("item/largexmaschest\\.png", "entity/chest/christmas_double.png"),
        new TextureEntry("item/xmaschest\\.png", "entity/chest/christmas.png"),
        new DeleteEntry("item/door\\.png"),
        new TextureEntry("item/knot\\.png", "entity/lead_knot.png"),
        new TextureEntry("item/sign\\.png", "entity/sign.png"),
        new DeleteEntry("item/skis\\.png"),
        new TextureEntry("item/xporb\\.png", "entity/experience_orb.png"),

        // Mob entities
        new MobTextureEntry("bat", "bat"),
        new MobTextureEntry("cat_black", "cat/black"),
        new MobTextureEntry("cat_red", "cat/red"),
        new MobTextureEntry("cat_siamese", "cat/siamese"),
        new MobTextureEntry("ozelot", "cat/ocelot"),
        new MobTextureEntry("cavespider", "spider/cave_spider"),
        new MobTextureEntry("spider", "spider/spider"),
        new MobTextureEntry("spider_eyes", "spider_eyes"),
        new MobTextureEntry("char", "steve"),
        new MobTextureEntry("chicken", "chicken"),
        new MobTextureEntry("cow", "cow/cow"),
        new MobTextureEntry("redcow", "cow/mooshroom"),
        new DeleteEntry("mob/enderdragon/body\\.png"),
        new MobTextureEntry("enderdragon/crystal", "endercrystal/endercrystal"),
        new MobTextureEntry("enderdragon/beam", "endercrystal/endercrystal_beam"),
        new DeleteEntry("mob/enderdragon/dragon\\.png"),
        new MobTextureEntry("enderdragon/ender", "enderdragon/dragon"),
        new MobTextureEntry("enderdragon/ender_eyes", "enderdragon/dragon_eyes"),
        new MobTextureEntry("enderdragon/shuffle", "enderdragon/dragon_exploding"),
        new MobTextureEntry("enderman", "enderman/enderman"),
        new MobTextureEntry("enderman_eyes", "enderman/enderman_eyes"),
        new MobTextureEntry("fire", "blaze"),
        new MobTextureEntry("ghast", "ghast/ghast"),
        new MobTextureEntry("ghast_fire", "ghast/ghast_shooting"),
        new MobTextureEntry("lava", "slime/magmacube"),
        new MobTextureEntry("slime", "slime/slime"),
        new MobTextureEntry("pig", "pig/pig"),
        new MobTextureEntry("saddle", "pig/pig_saddle"),
        new DeleteEntry("mob/pigman\\.png"),
        new MobTextureEntry("pigzombie", "zombie_pigman"),
        new MobTextureEntry("sheep", "sheep/sheep"),
        new MobTextureEntry("sheep_fur", "sheep/sheep_fur"),
        new MobTextureEntry("silverfish", "silverfish"),
        new MobTextureEntry("skeleton", "skeleton/skeleton"),
        new MobTextureEntry("skeleton_wither", "skeleton/wither_skeleton"),
        new MobTextureEntry("snowman", "snowman"),
        new MobTextureEntry("squid", "squid"),
        new MobTextureEntry("villager/butcher", "villager/butcher"),
        new MobTextureEntry("villager/farmer", "villager/farmer"),
        new MobTextureEntry("villager/librarian", "villager/librarian"),
        new MobTextureEntry("villager/priest", "villager/priest"),
        new MobTextureEntry("villager/smith", "villager/smith"),
        new MobTextureEntry("villager/villager", "villager/villager"),
        new MobTextureEntry("villager/witch", "witch"),
        new DeleteEntry("mob/villager\\.png"),
        new MobTextureEntry("villager_golem", "iron_golem"),
        new MobTextureEntry("wither", "wither/wither"),
        new MobTextureEntry("wither_invul", "wither/wither_invulnerable"),
        new MobTextureEntry("wolf", "wolf/wolf"),
        new MobTextureEntry("wolf_angry", "wolf/wolf_angry"),
        new MobTextureEntry("wolf_collar", "wolf/wolf_collar"),
        new MobTextureEntry("wolf_tame", "wolf/wolf_tame"),
        new MobTextureEntry("zombie", "zombie/zombie"),
        new MobTextureEntry("zombie_villager", "zombie/zombie_villager"),
        new MobTextureEntry("horse/armor_diamond", "horse/armor/horse_armor_diamond"),
        new MobTextureEntry("horse/armor_gold", "horse/armor/horse_armor_gold"),
        new MobTextureEntry("horse/armor_metal", "horse/armor/horse_armor_iron"),
        new MobTextureEntry("horse/donkey", "horse/donkey"),
        new MobTextureEntry("horse/horse_black", "horse/horse_black"),
        new MobTextureEntry("horse/horse_brown", "horse/horse_brown"),
        new MobTextureEntry("horse/horse_chestnut", "horse/horse_chestnut"),
        new MobTextureEntry("horse/horse_creamy", "horse/horse_creamy"),
        new MobTextureEntry("horse/horse_darkbrown", "horse/horse_darkbrown"),
        new MobTextureEntry("horse/horse_gray", "horse/horse_gray"),
        new MobTextureEntry("horse/horse_white", "horse/horse_white"),
        new MobTextureEntry("horse/mark_blackdots", "horse/horse_markings_blackdots"),
        new MobTextureEntry("horse/mark_white", "horse/horse_markings_white"),
        new MobTextureEntry("horse/mark_whitedots", "horse/horse_markings_whitedots"),
        new MobTextureEntry("horse/mark_whitefield", "horse/horse_markings_whitefield"),
        new MobTextureEntry("horse/mule", "horse/mule"),
        new MobTextureEntry("horse/skeleton", "horse/horse_skeleton"),
        new MobTextureEntry("horse/undead", "horse/horse_zombie"),
        new MobTextureEntry("creeper", "creeper/creeper"),
        new TextureEntry("misc/beam\\.png", "entity/beacon_beam.png"),
        new TextureEntry("armor/power\\.png", "entity/creeper/creeper_armor.png"),
        new TextureEntry("armor/witherarmor\\.png", "entity/wither/wither_armor.png"),

        // Environment
        new TextureEntry("environment/clouds\\.png", "environment/clouds.png"),
        new DeleteEntry("environment/light_normal\\.png"),
        new DeleteEntry("environment/moon\\.png"),
        new TextureEntry("environment/moon_phases\\.png", "environment/moon_phases.png"),
        new TextureEntry("environment/rain\\.png", "environment/rain.png"),
        new TextureEntry("environment/snow\\.png", "environment/snow.png"),
        new TextureEntry("environment/sun\\.png", "environment/sun.png"),

        // MCPatcher
        new TextureEntry("anim/(.*)", "anim/$1"),
        new TextureEntry("ctm/(.*)", "ctm/$1"),
        new TextureEntry("environment/(.*)", "environment/$1"),
        new TextureEntry("misc/(.*)", "misc/$1"),
        new MobTextureEntry("redcow_overlay", "cow/mooshroom_overlay"),
        new MobTextureEntry("snowman_overlay", "snowman_overlay"),
        new TextureEntry("color.properties", "color.properties"),
        new TextureEntry("renderpass.properties", "renderpass.properties"),
    };

    private String lastFileMessage;

    public TexturePackConverter16(File input) {
        super(input);
        File dir = new File(input.getParentFile().getParentFile(), "resourcepacks");
        dir.mkdirs();
        output = new File(dir, input.getName());
    }

    @Override
    protected void convertImpl(UserInterface ui) throws Exception {
        int progress = 0;
        for (ZipEntry entry : inEntries) {
            ui.updateProgress(++progress, inEntries.size());
            String name = entry.getName();
            String newName = mapPath(name);

            boolean handled = false;

            if (name.endsWith(".properties")) {
                Properties properties = getProperties(name);
                handled = convertProperties(name, properties);
                if (name.equals(COLOR_PROPERTIES)) {
                    handled |= convertColorProperties(name, properties);
                }
                if (handled) {
                    addEntry(newName, properties);
                }
            } else if (name.equals("pack.txt")) {
                handled = convertPackTxt(name);
            } else if (name.endsWith(".txt")) {
                handled = convertAnimation(name);
            }
            if (!handled && newName != null) {
                copyEntry(entry, newName);
            }
        }
    }

    private boolean convertProperties(String name, Properties properties) {
        Map<String, String> changes = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (value.contains("/")) {
                String newValue = mapPath(value);
                if (newValue == null) {
                    logFilename(name);
                    addMessage(0, "    remove property %s=%s", key, value);
                    changes.put(key, null);
                } else if (!value.equals(newValue)) {
                    logFilename(name);
                    addMessage(0, "    change property %s=%s -> %s", key, value, newValue);
                    changes.put(key, newValue);
                }
            }
        }
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                properties.remove(key);
            } else {
                properties.setProperty(key, value);
            }
        }
        return !changes.isEmpty();
    }

    private boolean convertColorProperties(String name, Properties properties) {
        Map<String, String> changes = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.startsWith(PALETTE_BLOCK_KEY)) {
                String path = key.substring(PALETTE_BLOCK_KEY.length());
                String newPath = mapPath(path);
                if (!newPath.equals(path)) {
                    logFilename(name);
                    String newKey = PALETTE_BLOCK_KEY + newPath;
                    addMessage(0, "    rename property %s -> %s=%s", key, newKey, value);
                    changes.put(key, null);
                    changes.put(newKey, value);
                }
            }
        }
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                properties.remove(key);
            } else {
                properties.setProperty(key, value);
            }
        }
        return !changes.isEmpty();
    }

    private boolean convertPackTxt(String name) {
        return false;
    }

    private boolean convertAnimation(String name) {
        return false;
    }

    private static String mapPath(String path) {
        path = getEntryName(path);
        for (PlainEntry entry : convertEntries) {
            if (entry.matches(path)) {
                return entry.replace(path);
            }
        }
        return path;
    }

    private void logFilename(String name) {
        if (name != null && !name.equals(lastFileMessage)) {
            addMessage(0, "  %s:", name);
            lastFileMessage = name;
        }
    }

    private static class PlainEntry {
        final String from;
        final String to;

        PlainEntry(String from, String to) {
            this.from = from;
            this.to = to;
        }

        boolean matches(String s) {
            return s.matches(from);
        }

        String replace(String s) {
            return to == null ? null : s.replaceFirst("^" + from, to);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + from + "->" + to + "}";
        }
    }
    
    private static class TextureEntry extends PlainEntry {
        TextureEntry(String from, String to) {
            super(from, to == null ? null : "assets/minecraft/textures/" + to);
        }
    }

    private static class MobTextureEntry extends TextureEntry {
        MobTextureEntry(String from, String to) {
            super("mob/" + from + "(\\d*\\..*)", to == null ? null : "entity/" + to + "$1");
        }
    }

    private static class DeleteEntry extends PlainEntry {
        DeleteEntry(String from) {
            super(from, null);
        }
    }
}
