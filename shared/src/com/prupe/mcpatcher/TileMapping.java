package com.prupe.mcpatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMapping {
    public static final String[] BLOCKS = new String[]{
        /*   0:  0, 0 */ "grass_top",
        /*   1:  1, 0 */ "stone",
        /*   2:  2, 0 */ "dirt",
        /*   3:  3, 0 */ "grass_side",
        /*   4:  4, 0 */ "wood",
        /*   5:  5, 0 */ "stoneslab_side",
        /*   6:  6, 0 */ "stoneslab_top",
        /*   7:  7, 0 */ "brick",
        /*   8:  8, 0 */ "tnt_side",
        /*   9:  9, 0 */ "tnt_top",
        /*  10: 10, 0 */ "tnt_bottom",
        /*  11: 11, 0 */ "web",
        /*  12: 12, 0 */ "rose",
        /*  13: 13, 0 */ "flower",
        /*  14: 14, 0 */ "portal",
        /*  15: 15, 0 */ "sapling",
        /*  16:  0, 1 */ "stonebrick",
        /*  17:  1, 1 */ "bedrock",
        /*  18:  2, 1 */ "sand",
        /*  19:  3, 1 */ "gravel",
        /*  20:  4, 1 */ "tree_side",
        /*  21:  5, 1 */ "tree_top",
        /*  22:  6, 1 */ "blockIron",
        /*  23:  7, 1 */ "blockGold",
        /*  24:  8, 1 */ "blockDiamond",
        /*  25:  9, 1 */ "blockEmerald",
        /*  26: 10, 1 */ "blockRedstone",
        /*  27: 11, 1 */ null,
        /*  28: 12, 1 */ "mushroom_red",
        /*  29: 13, 1 */ "mushroom_brown",
        /*  30: 14, 1 */ "sapling_jungle",
        /*  31: 15, 1 */ null,
        /*  32:  0, 2 */ "oreGold",
        /*  33:  1, 2 */ "oreIron",
        /*  34:  2, 2 */ "oreCoal",
        /*  35:  3, 2 */ "bookshelf",
        /*  36:  4, 2 */ "stoneMoss",
        /*  37:  5, 2 */ "obsidian",
        /*  38:  6, 2 */ "grass_side_overlay",
        /*  39:  7, 2 */ "tallgrass",
        /*  40:  8, 2 */ null,
        /*  41:  9, 2 */ "beacon",
        /*  42: 10, 2 */ null,
        /*  43: 11, 2 */ "workbench_top",
        /*  44: 12, 2 */ "furnace_front",
        /*  45: 13, 2 */ "furnace_side",
        /*  46: 14, 2 */ "dispenser_front",
        /*  47: 15, 2 */ null,
        /*  48:  0, 3 */ "sponge",
        /*  49:  1, 3 */ "glass",
        /*  50:  2, 3 */ "oreDiamond",
        /*  51:  3, 3 */ "oreRedstone",
        /*  52:  4, 3 */ "leaves",
        /*  53:  5, 3 */ "leaves_opaque",
        /*  54:  6, 3 */ "stonebricksmooth",
        /*  55:  7, 3 */ "deadbush",
        /*  56:  8, 3 */ "fern",
        /*  57:  9, 3 */ "daylightDetector_top",
        /*  58: 10, 3 */ "daylightDetector_side",
        /*  59: 11, 3 */ "workbench_side",
        /*  60: 12, 3 */ "workbench_front",
        /*  61: 13, 3 */ "furnace_front_lit",
        /*  62: 14, 3 */ "furnace_top",
        /*  63: 15, 3 */ "sapling_spruce",
        /*  64:  0, 4 */ "cloth_0",
        /*  65:  1, 4 */ "mobSpawner",
        /*  66:  2, 4 */ "snow",
        /*  67:  3, 4 */ "ice",
        /*  68:  4, 4 */ "snow_side",
        /*  69:  5, 4 */ "cactus_top",
        /*  70:  6, 4 */ "cactus_side",
        /*  71:  7, 4 */ "cactus_bottom",
        /*  72:  8, 4 */ "clay",
        /*  73:  9, 4 */ "reeds",
        /*  74: 10, 4 */ "musicBlock",
        /*  75: 11, 4 */ "jukebox_top",
        /*  76: 12, 4 */ "waterlily",
        /*  77: 13, 4 */ "mycel_side",
        /*  78: 14, 4 */ "mycel_top",
        /*  79: 15, 4 */ "sapling_birch",
        /*  80:  0, 5 */ "torch",
        /*  81:  1, 5 */ "doorWood_upper",
        /*  82:  2, 5 */ "doorIron_upper",
        /*  83:  3, 5 */ "ladder",
        /*  84:  4, 5 */ "trapdoor",
        /*  85:  5, 5 */ "fenceIron",
        /*  86:  6, 5 */ "farmland_wet",
        /*  87:  7, 5 */ "farmland_dry",
        /*  88:  8, 5 */ "crops_0",
        /*  89:  9, 5 */ "crops_1",
        /*  90: 10, 5 */ "crops_2",
        /*  91: 11, 5 */ "crops_3",
        /*  92: 12, 5 */ "crops_4",
        /*  93: 13, 5 */ "crops_5",
        /*  94: 14, 5 */ "crops_6",
        /*  95: 15, 5 */ "crops_7",
        /*  96:  0, 6 */ "lever",
        /*  97:  1, 6 */ "doorWood_lower",
        /*  98:  2, 6 */ "doorIron_lower",
        /*  99:  3, 6 */ "redtorch_lit",
        /* 100:  4, 6 */ "stonebricksmooth_mossy",
        /* 101:  5, 6 */ "stonebricksmooth_cracked",
        /* 102:  6, 6 */ "pumpkin_top",
        /* 103:  7, 6 */ "hellrock",
        /* 104:  8, 6 */ "hellsand",
        /* 105:  9, 6 */ "lightgem",
        /* 106: 10, 6 */ "piston_top_sticky",
        /* 107: 11, 6 */ "piston_top",
        /* 108: 12, 6 */ "piston_side",
        /* 109: 13, 6 */ "piston_bottom",
        /* 110: 14, 6 */ "piston_inner_top",
        /* 111: 15, 6 */ "stem_straight",
        /* 112:  0, 7 */ "rail_turn",
        /* 113:  1, 7 */ "cloth_15",
        /* 114:  2, 7 */ "cloth_7",
        /* 115:  3, 7 */ "redtorch",
        /* 116:  4, 7 */ "tree_spruce",
        /* 117:  5, 7 */ "tree_birch",
        /* 118:  6, 7 */ "pumpkin_side",
        /* 119:  7, 7 */ "pumpkin_face",
        /* 120:  8, 7 */ "pumpkin_jack",
        /* 121:  9, 7 */ "cake_top",
        /* 122: 10, 7 */ "cake_side",
        /* 123: 11, 7 */ "cake_inner",
        /* 124: 12, 7 */ "cake_bottom",
        /* 125: 13, 7 */ "mushroom_skin_red",
        /* 126: 14, 7 */ "mushroom_skin_brown",
        /* 127: 15, 7 */ "stem_bent",
        /* 128:  0, 8 */ "rail",
        /* 129:  1, 8 */ "cloth_14",
        /* 130:  2, 8 */ "cloth_6",
        /* 131:  3, 8 */ "repeater",
        /* 132:  4, 8 */ "leaves_spruce",
        /* 133:  5, 8 */ "leaves_spruce_opaque",
        /* 134:  6, 8 */ "bed_feet_top",
        /* 135:  7, 8 */ "bed_head_top",
        /* 136:  8, 8 */ "melon_side",
        /* 137:  9, 8 */ "melon_top",
        /* 138: 10, 8 */ "cauldron_top",
        /* 139: 11, 8 */ "cauldron_inner",
        /* 140: 12, 8 */ null,
        /* 141: 13, 8 */ "mushroom_skin_stem",
        /* 142: 14, 8 */ "mushroom_inside",
        /* 143: 15, 8 */ "vine",
        /* 144:  0, 9 */ "blockLapis",
        /* 145:  1, 9 */ "cloth_13",
        /* 146:  2, 9 */ "cloth_5",
        /* 147:  3, 9 */ "repeater_lit",
        /* 148:  4, 9 */ "thinglass_top",
        /* 149:  5, 9 */ "bed_feet_end",
        /* 150:  6, 9 */ "bed_feet_side",
        /* 151:  7, 9 */ "bed_head_side",
        /* 152:  8, 9 */ "bed_head_end",
        /* 153:  9, 9 */ "tree_jungle",
        /* 154: 10, 9 */ "cauldron_side",
        /* 155: 11, 9 */ "cauldron_bottom",
        /* 156: 12, 9 */ "brewingStand_base",
        /* 157: 13, 9 */ "brewingStand",
        /* 158: 14, 9 */ "endframe_top",
        /* 159: 15, 9 */ "endframe_side",
        /* 160:  0,10 */ "oreLapis",
        /* 161:  1,10 */ "cloth_12",
        /* 162:  2,10 */ "cloth_4",
        /* 163:  3,10 */ "goldenRail",
        /* 164:  4,10 */ "redstoneDust_cross",
        /* 165:  5,10 */ "redstoneDust_line",
        /* 166:  6,10 */ "enchantment_top",
        /* 167:  7,10 */ "dragonEgg",
        /* 168:  8,10 */ "cocoa_2",
        /* 169:  9,10 */ "cocoa_1",
        /* 170: 10,10 */ "cocoa_0",
        /* 171: 11,10 */ "oreEmerald",
        /* 172: 12,10 */ "tripWireSource",
        /* 173: 13,10 */ "tripWire",
        /* 174: 14,10 */ "endframe_eye",
        /* 175: 15,10 */ "whiteStone",
        /* 176:  0,11 */ "sandstone_top",
        /* 177:  1,11 */ "cloth_11",
        /* 178:  2,11 */ "cloth_3",
        /* 179:  3,11 */ "goldenRail_powered",
        /* 180:  4,11 */ "redstoneDust_cross_overlay",
        /* 181:  5,11 */ "redstoneDust_line_overlay",
        /* 182:  6,11 */ "enchantment_side",
        /* 183:  7,11 */ "enchantment_bottom",
        /* 184:  8,11 */ "commandBlock",
        /* 185:  9,11 */ "itemframe_back",
        /* 186: 10,11 */ "flowerPot",
        /* 187: 11,11 */ "comparator",
        /* 188: 12,11 */ "comparator_lit",
        /* 189: 13,11 */ null,
        /* 190: 14,11 */ null,
        /* 191: 15,11 */ "netherquartz",
        /* 192:  0,12 */ "sandstone_side",
        /* 193:  1,12 */ "cloth_10",
        /* 194:  2,12 */ "cloth_2",
        /* 195:  3,12 */ "detectorRail",
        /* 196:  4,12 */ "leaves_jungle",
        /* 197:  5,12 */ "leaves_jungle_opaque",
        /* 198:  6,12 */ "wood_spruce",
        /* 199:  7,12 */ "wood_jungle",
        /* 200:  8,12 */ "carrots_0",
        /* 201:  9,12 */ "carrots_1",
        /* 202: 10,12 */ "carrots_2",
        /* 203: 11,12 */ "carrots_3",
        /* 204: 12,12 */ "potatoes_3",
        /* 205: 13,12 */ null,
        /* 206: 14,12 */ null,
        /* 207: 15,12 */ null,
        /* 208:  0,13 */ "sandstone_bottom",
        /* 209:  1,13 */ "cloth_9",
        /* 210:  2,13 */ "cloth_1",
        /* 211:  3,13 */ "redstoneLight",
        /* 212:  4,13 */ "redstoneLight_lit",
        /* 213:  5,13 */ "stonebricksmooth_carved",
        /* 214:  6,13 */ "wood_birch",
        /* 215:  7,13 */ "anvil_base",
        /* 216:  8,13 */ "anvil_top_damaged_1",
        /* 217:  9,13 */ null,
        /* 218: 10,13 */ null,
        /* 219: 11,13 */ null,
        /* 220: 12,13 */ null,
        /* 221: 13,13 */ null,
        /* 222: 14,13 */ null,
        /* 223: 15,13 */ null,
        /* 224:  0,14 */ "netherBrick",
        /* 225:  1,14 */ "cloth_8",
        /* 226:  2,14 */ "netherStalk_0",
        /* 227:  3,14 */ "netherStalk_1",
        /* 228:  4,14 */ "netherStalk_2",
        /* 229:  5,14 */ "sandstone_carved",
        /* 230:  6,14 */ "sandstone_smooth",
        /* 231:  7,14 */ "anvil_top",
        /* 232:  8,14 */ "anvil_top_damaged_2",
        /* 233:  9,14 */ null,
        /* 234: 10,14 */ null,
        /* 235: 11,14 */ null,
        /* 236: 12,14 */ null,
        /* 237: 13,14 */ null,
        /* 238: 14,14 */ null,
        /* 239: 15,14 */ null,
        /* 240:  0,15 */ "destroy_0",
        /* 241:  1,15 */ "destroy_1",
        /* 242:  2,15 */ "destroy_2",
        /* 243:  3,15 */ "destroy_3",
        /* 244:  4,15 */ "destroy_4",
        /* 245:  5,15 */ "destroy_5",
        /* 246:  6,15 */ "destroy_6",
        /* 247:  7,15 */ "destroy_7",
        /* 248:  8,15 */ "destroy_8",
        /* 249:  9,15 */ "destroy_9",
        /* 250: 10,15 */ null,
        /* 251: 11,15 */ null,
        /* 252: 12,15 */ null,
        /* 253: 13,15 */ null,
        /* 254: 14,15 */ null,
        /* 255: 15,15 */ null,
    };

    public static final String[] ITEMS = new String[]{
        /*   0:  0, 0 */ "helmetCloth",
        /*   1:  1, 0 */ "helmetChain",
        /*   2:  2, 0 */ "helmetIron",
        /*   3:  3, 0 */ "helmetDiamond",
        /*   4:  4, 0 */ "helmetGold",
        /*   5:  5, 0 */ "flintAndSteel",
        /*   6:  6, 0 */ "flint",
        /*   7:  7, 0 */ "coal",
        /*   8:  8, 0 */ "string",
        /*   9:  9, 0 */ "seeds",
        /*  10: 10, 0 */ "apple",
        /*  11: 11, 0 */ "appleGold",
        /*  12: 12, 0 */ "egg",
        /*  13: 13, 0 */ "sugar",
        /*  14: 14, 0 */ "snowball",
        /*  15: 15, 0 */ "slot_empty_helmet",
        /*  16:  0, 1 */ "chestplateCloth",
        /*  17:  1, 1 */ "chestplateChain",
        /*  18:  2, 1 */ "chestplateIron",
        /*  19:  3, 1 */ "chestplateDiamond",
        /*  20:  4, 1 */ "chestplateGold",
        /*  21:  5, 1 */ "bow",
        /*  22:  6, 1 */ "brick",
        /*  23:  7, 1 */ "ingotIron",
        /*  24:  8, 1 */ "feather",
        /*  25:  9, 1 */ "wheat",
        /*  26: 10, 1 */ "painting",
        /*  27: 11, 1 */ "reeds",
        /*  28: 12, 1 */ "bone",
        /*  29: 13, 1 */ "cake",
        /*  30: 14, 1 */ "slimeball",
        /*  31: 15, 1 */ "slot_empty_chestplate",
        /*  32:  0, 2 */ "leggingsCloth",
        /*  33:  1, 2 */ "leggingsChain",
        /*  34:  2, 2 */ "leggingsIron",
        /*  35:  3, 2 */ "leggingsDiamond",
        /*  36:  4, 2 */ "leggingsGold",
        /*  37:  5, 2 */ "arrow",
        /*  38:  6, 2 */ "quiver",
        /*  39:  7, 2 */ "ingotGold",
        /*  40:  8, 2 */ "sulphur",
        /*  41:  9, 2 */ "bread",
        /*  42: 10, 2 */ "sign",
        /*  43: 11, 2 */ "doorWood",
        /*  44: 12, 2 */ "doorIron",
        /*  45: 13, 2 */ "bed",
        /*  46: 14, 2 */ "fireball",
        /*  47: 15, 2 */ "slot_empty_leggings",
        /*  48:  0, 3 */ "bootsCloth",
        /*  49:  1, 3 */ "bootsChain",
        /*  50:  2, 3 */ "bootsIron",
        /*  51:  3, 3 */ "bootsDiamond",
        /*  52:  4, 3 */ "bootsGold",
        /*  53:  5, 3 */ "stick",
        /*  54:  6, 3 */ "compass",
        /*  55:  7, 3 */ "diamond",
        /*  56:  8, 3 */ "redstone",
        /*  57:  9, 3 */ "clay",
        /*  58: 10, 3 */ "paper",
        /*  59: 11, 3 */ "book",
        /*  60: 12, 3 */ "map",
        /*  61: 13, 3 */ "seeds_pumpkin",
        /*  62: 14, 3 */ "seeds_melon",
        /*  63: 15, 3 */ "slot_empty_boots",
        /*  64:  0, 4 */ "swordWood",
        /*  65:  1, 4 */ "swordStone",
        /*  66:  2, 4 */ "swordIron",
        /*  67:  3, 4 */ "swordDiamond",
        /*  68:  4, 4 */ "swordGold",
        /*  69:  5, 4 */ "fishingRod",
        /*  70:  6, 4 */ "clock",
        /*  71:  7, 4 */ "bowl",
        /*  72:  8, 4 */ "mushroomStew",
        /*  73:  9, 4 */ "yellowDust",
        /*  74: 10, 4 */ "bucket",
        /*  75: 11, 4 */ "bucketWater",
        /*  76: 12, 4 */ "bucketLava",
        /*  77: 13, 4 */ "milk",
        /*  78: 14, 4 */ "dyePowder_black",
        /*  79: 15, 4 */ "dyePowder_gray",
        /*  80:  0, 5 */ "shovelWood",
        /*  81:  1, 5 */ "shovelStone",
        /*  82:  2, 5 */ "shovelIron",
        /*  83:  3, 5 */ "shovelDiamond",
        /*  84:  4, 5 */ "shovelGold",
        /*  85:  5, 5 */ "fishingRod_empty",
        /*  86:  6, 5 */ "diode",
        /*  87:  7, 5 */ "porkchopRaw",
        /*  88:  8, 5 */ "porkchopCooked",
        /*  89:  9, 5 */ "fishRaw",
        /*  90: 10, 5 */ "fishCooked",
        /*  91: 11, 5 */ "rottenFlesh",
        /*  92: 12, 5 */ "cookie",
        /*  93: 13, 5 */ "shears",
        /*  94: 14, 5 */ "dyePowder_red",
        /*  95: 15, 5 */ "dyePowder_pink",
        /*  96:  0, 6 */ "pickaxeWood",
        /*  97:  1, 6 */ "pickaxeStone",
        /*  98:  2, 6 */ "pickaxeIron",
        /*  99:  3, 6 */ "pickaxeDiamond",
        /* 100:  4, 6 */ "pickaxeGold",
        /* 101:  5, 6 */ "bow_pull_0",
        /* 102:  6, 6 */ "carrotOnAStick",
        /* 103:  7, 6 */ "leather",
        /* 104:  8, 6 */ "saddle",
        /* 105:  9, 6 */ "beefRaw",
        /* 106: 10, 6 */ "beefCooked",
        /* 107: 11, 6 */ "enderPearl",
        /* 108: 12, 6 */ "blazeRod",
        /* 109: 13, 6 */ "melon",
        /* 110: 14, 6 */ "dyePowder_green",
        /* 111: 15, 6 */ "dyePowder_lime",
        /* 112:  0, 7 */ "hatchetWood",
        /* 113:  1, 7 */ "hatchetStone",
        /* 114:  2, 7 */ "hatchetIron",
        /* 115:  3, 7 */ "hatchetDiamond",
        /* 116:  4, 7 */ "hatchetGold",
        /* 117:  5, 7 */ "bow_pull_1",
        /* 118:  6, 7 */ "potatoBaked",
        /* 119:  7, 7 */ "potato",
        /* 120:  8, 7 */ "carrots",
        /* 121:  9, 7 */ "chickenRaw",
        /* 122: 10, 7 */ "chickenCooked",
        /* 123: 11, 7 */ "ghastTear",
        /* 124: 12, 7 */ "goldNugget",
        /* 125: 13, 7 */ "netherStalkSeeds",
        /* 126: 14, 7 */ "dyePowder_brown",
        /* 127: 15, 7 */ "dyePowder_yellow",
        /* 128:  0, 8 */ "hoeWood",
        /* 129:  1, 8 */ "hoeStone",
        /* 130:  2, 8 */ "hoeIron",
        /* 131:  3, 8 */ "hoeDiamond",
        /* 132:  4, 8 */ "hoeGold",
        /* 133:  5, 8 */ "bow_pull_2",
        /* 134:  6, 8 */ "potatoPoisonous",
        /* 135:  7, 8 */ "minecart",
        /* 136:  8, 8 */ "boat",
        /* 137:  9, 8 */ "speckledMelon",
        /* 138: 10, 8 */ "fermentedSpiderEye",
        /* 139: 11, 8 */ "spiderEye",
        /* 140: 12, 8 */ "potion",
        /* 141: 13, 8 */ "potion_contents",
        /* 142: 14, 8 */ "dyePowder_blue",
        /* 143: 15, 8 */ "dyePowder_lightBlue",
        /* 144:  0, 9 */ "helmetCloth_overlay",
        /* 145:  1, 9 */ null,
        /* 146:  2, 9 */ null,
        /* 147:  3, 9 */ null,
        /* 148:  4, 9 */ null,
        /* 149:  5, 9 */ "comparator",
        /* 150:  6, 9 */ "carrotGolden",
        /* 151:  7, 9 */ "minecartChest",
        /* 152:  8, 9 */ "pumpkinPie",
        /* 153:  9, 9 */ "monsterPlacer",
        /* 154: 10, 9 */ "potion_splash",
        /* 155: 11, 9 */ "eyeOfEnder",
        /* 156: 12, 9 */ "cauldron",
        /* 157: 13, 9 */ "blazePowder",
        /* 158: 14, 9 */ "dyePowder_purple",
        /* 159: 15, 9 */ "dyePowder_magenta",
        /* 160:  0,10 */ "chestplateCloth_overlay",
        /* 161:  1,10 */ null,
        /* 162:  2,10 */ null,
        /* 163:  3,10 */ null,
        /* 164:  4,10 */ null,
        /* 165:  5,10 */ "netherbrick",
        /* 166:  6,10 */ null,
        /* 167:  7,10 */ "minecartFurnace",
        /* 168:  8,10 */ null,
        /* 169:  9,10 */ "monsterPlacer_overlay",
        /* 170: 10,10 */ "ruby",
        /* 171: 11,10 */ "expBottle",
        /* 172: 12,10 */ "brewingStand",
        /* 173: 13,10 */ "magmaCream",
        /* 174: 14,10 */ "dyePowder_cyan",
        /* 175: 15,10 */ "dyePowder_orange",
        /* 176:  0,11 */ "leggingsCloth_overlay",
        /* 177:  1,11 */ null,
        /* 178:  2,11 */ null,
        /* 179:  3,11 */ null,
        /* 180:  4,11 */ null,
        /* 181:  5,11 */ null,
        /* 182:  6,11 */ null,
        /* 183:  7,11 */ null,
        /* 184:  8,11 */ null,
        /* 185:  9,11 */ "netherStar",
        /* 186: 10,11 */ "emerald",
        /* 187: 11,11 */ "writingBook",
        /* 188: 12,11 */ "writtenBook",
        /* 189: 13,11 */ "flowerPot",
        /* 190: 14,11 */ "dyePowder_silver",
        /* 191: 15,11 */ "dyePowder_white",
        /* 192:  0,12 */ "bootsCloth_overlay",
        /* 193:  1,12 */ null,
        /* 194:  2,12 */ null,
        /* 195:  3,12 */ null,
        /* 196:  4,12 */ null,
        /* 197:  5,12 */ null,
        /* 198:  6,12 */ null,
        /* 199:  7,12 */ null,
        /* 200:  8,12 */ null,
        /* 201:  9,12 */ "fireworks",
        /* 202: 10,12 */ "fireworksCharge",
        /* 203: 11,12 */ "fireworksCharge_overlay",
        /* 204: 12,12 */ "netherquartz",
        /* 205: 13,12 */ "emptyMap",
        /* 206: 14,12 */ "frame",
        /* 207: 15,12 */ "enchantedBook",
        /* 208:  0,13 */ null,
        /* 209:  1,13 */ null,
        /* 210:  2,13 */ null,
        /* 211:  3,13 */ null,
        /* 212:  4,13 */ null,
        /* 213:  5,13 */ null,
        /* 214:  6,13 */ null,
        /* 215:  7,13 */ null,
        /* 216:  8,13 */ null,
        /* 217:  9,13 */ null,
        /* 218: 10,13 */ null,
        /* 219: 11,13 */ null,
        /* 220: 12,13 */ null,
        /* 221: 13,13 */ null,
        /* 222: 14,13 */ null,
        /* 223: 15,13 */ null,
        /* 224:  0,14 */ "skull_skeleton",
        /* 225:  1,14 */ "skull_wither",
        /* 226:  2,14 */ "skull_zombie",
        /* 227:  3,14 */ "skull_char",
        /* 228:  4,14 */ "skull_creeper",
        /* 229:  5,14 */ null,
        /* 230:  6,14 */ null,
        /* 231:  7,14 */ null,
        /* 232:  8,14 */ null,
        /* 233:  9,14 */ null,
        /* 234: 10,14 */ null,
        /* 235: 11,14 */ null,
        /* 236: 12,14 */ null,
        /* 237: 13,14 */ null,
        /* 238: 14,14 */ null,
        /* 239: 15,14 */ null,
        /* 240:  0,15 */ "record_13",
        /* 241:  1,15 */ "record_cat",
        /* 242:  2,15 */ "record_blocks",
        /* 243:  3,15 */ "record_chirp",
        /* 244:  4,15 */ "record_far",
        /* 245:  5,15 */ "record_mall",
        /* 246:  6,15 */ "record_mellohi",
        /* 247:  7,15 */ "record_stal",
        /* 248:  8,15 */ "record_strad",
        /* 249:  9,15 */ "record_ward",
        /* 250: 10,15 */ "record_11",
        /* 251: 11,15 */ "record_wait",
        /* 252: 12,15 */ null,
        /* 253: 13,15 */ null,
        /* 254: 14,15 */ null,
        /* 255: 15,15 */ null,
    };

    private static final Map<String, TileMapping> allMappings = new HashMap<String, TileMapping>();

    private final String tilesheetName;
    private final String dirPrefix;
    private final Map<String, Integer> tiles = new HashMap<String, Integer>();

    static {
        TileMapping map = new TileMapping("/terrain.png", "/textures/blocks/");
        map.addAll(BLOCKS);

        map = new TileMapping("/gui/items.png", "/textures/items/");
        map.addAll(ITEMS);

        map = new TileMapping("/btwmodtex/btwterrain01.png", "/textures/blocks/");
        map.add(0, "fcBlockPedestalStone_top");
        map.add(0, "fcBlockColumnStone_top");
        map.add(0, "fcBlockPedestalStone_side");
        map.add(0, "fcBlockDecorativeStone");
        map.add(1, "fcBlockTableWoodOak_leg");
        map.add(1, "fcBlockTurntable_switch");
        map.add(1, "fcBlockHopper_side");
        map.add(1, "fcBlockSaw");
        map.add(1, "fcBlockGearBox");
        map.add(1, "fcBlockTableWoodOak_top");
        map.add(1, "fcBlockSlats_side");
        map.add(2, "FCBlockBlightL0_bottom");
        map.add(2, "FCBlockBlightL1_bottom");
        // /btwmodtex/btwterrain01.png,   3 -> (none)
        map.add(4, "fcBlockLightBlock");
        map.add(5, "fcBlockLightBlock_lit");
        map.add(6, "fcBlockBlockDispenser_top");
        map.add(7, "fcBlockBlockDispenser_front");
        map.add(8, "fcBlockBlockDispenser_side");
        map.add(9, "fcBlockBlockDispenser_bottom");
        map.add(9, "fcBlockHibachi_bottom");
        map.add(9, "fcBlockDetectorBlock_bottom");
        map.add(10, "fcBlockDetectorBlock_top");
        map.add(10, "fcBlockHandCrank_bottom");
        map.add(11, "fcBlockDetectorBlock_front");
        map.add(12, "fcBlockDetectorBlock_front_on");
        map.add(13, "fcBlockDetectorBlock_side");
        map.add(14, "fcBlockBlockDispenser_bottom");
        map.add(14, "fcBlockHibachi_bottom");
        map.add(14, "fcBlockDetectorBlock_bottom");
        map.add(15, "fcBlockCement");
        map.add(16, "fcBlockCement_drying");
        map.add(17, "fcBlockCauldron_top");
        map.add(18, "fcBlockCauldron_side");
        map.add(19, "fcBlockCauldron_bottom");
        map.add(19, "fcBlockCrucible_bottom");
        map.add(20, "fcBlockCompanionCube");
        map.add(21, "fcBlockCompanionCube_front");
        map.add(22, "fcBlockCompanionCube_guts");
        map.add(23, "fcBlockDetectorRailWood");
        map.add(24, "fcBlockDetectorRailObsidian");
        map.add(25, "fcBlockMillStone_top");
        map.add(26, "fcBlockMillStone_side");
        map.add(27, "fcBlockMillStone_bottom");
        map.add(27, "fcBlockTurntable_bottom");
        map.add(28, "fcBlockHandCrank_shaft");
        map.add(29, "fcBlockHandCrank_top");
        map.add(30, "fcBlockHandCrank_side");
        map.add(31, "fcBlockDetectorBlock_top");
        map.add(31, "fcBlockHandCrank_bottom");
        map.add(32, "fcBlockRope");
        map.add(33, "fcBlockAxle_side");
        // /btwmodtex/btwterrain01.png,  34 -> (none)
        map.add(35, "fcBlockTableWoodOak_leg");
        map.add(35, "fcBlockTurntable_switch");
        map.add(35, "fcBlockHopper_side");
        map.add(35, "fcBlockSaw");
        map.add(35, "fcBlockGearBox");
        map.add(35, "fcBlockTableWoodOak_top");
        map.add(35, "fcBlockSlats_side");
        map.add(36, "fcBlockScrewPump_bottom");
        map.add(36, "fcBlockGearBox_input");
        map.add(37, "fcBlockHopper_bottom");
        map.add(37, "fcBlockPulley_bottom");
        map.add(37, "fcBlockHopper_top");
        map.add(37, "fcBlockGearBox_output");
        map.add(38, "fcBlockTableWoodOak_leg");
        map.add(38, "fcBlockTurntable_switch");
        map.add(38, "fcBlockHopper_side");
        map.add(38, "fcBlockSaw");
        map.add(38, "fcBlockGearBox");
        map.add(38, "fcBlockTableWoodOak_top");
        map.add(38, "fcBlockSlats_side");
        map.add(39, "fcBlockAnchor_nub");
        map.add(40, "fcBlockAnchor_front");
        map.add(41, "fcBlockAnchor");
        map.add(42, "fcBlockCandle_c00");
        map.add(42, "fcBlockCrucible_top");
        map.add(42, "fcBlockVase_c00");
        map.add(43, "fcBlockCrucible_side");
        map.add(44, "fcBlockCauldron_bottom");
        map.add(44, "fcBlockCrucible_bottom");
        map.add(45, "fcBlockCrucible_contents");
        map.add(45, "fcBlockUnfiredPottery_cooking");
        map.add(45, "fcBlockHopper_contents");
        map.add(46, "fcBlockHopper_bottom");
        map.add(46, "fcBlockPulley_bottom");
        map.add(46, "fcBlockHopper_top");
        map.add(46, "fcBlockGearBox_output");
        map.add(47, "fcBlockTableWoodOak_leg");
        map.add(47, "fcBlockTurntable_switch");
        map.add(47, "fcBlockHopper_side");
        map.add(47, "fcBlockSaw");
        map.add(47, "fcBlockGearBox");
        map.add(47, "fcBlockTableWoodOak_top");
        map.add(47, "fcBlockSlats_side");
        map.add(48, "fcBlockHopper_bottom");
        map.add(48, "fcBlockPulley_bottom");
        map.add(48, "fcBlockHopper_top");
        map.add(48, "fcBlockGearBox_output");
        map.add(49, "fcBlockCrucible_contents");
        map.add(49, "fcBlockUnfiredPottery_cooking");
        map.add(49, "fcBlockHopper_contents");
        map.add(50, "fcBlockHopper_ladder");
        map.add(51, "fcBlockHopper_trap");
        map.add(52, "fcBlockHopper_grate");
        map.add(53, "fcBlockSlats");
        map.add(53, "fcBlockHopper_slats");
        map.add(54, "fcBlockSlabWicker");
        map.add(54, "fcBlockPlatform_bottom");
        map.add(54, "fcBlockWicker");
        map.add(54, "fcBlockHopper_wicker");
        map.add(54, "fcBlockPlatform_top");
        map.add(55, "fcBlockHopper_soulsand");
        map.add(56, "fcBlockSaw_front");
        map.add(57, "fcBlockTableWoodOak_leg");
        map.add(57, "fcBlockTurntable_switch");
        map.add(57, "fcBlockHopper_side");
        map.add(57, "fcBlockSaw");
        map.add(57, "fcBlockGearBox");
        map.add(57, "fcBlockTableWoodOak_top");
        map.add(57, "fcBlockSlats_side");
        map.add(58, "fcBlockSaw_blade");
        map.add(59, "fcBlockHibachi_top");
        map.add(60, "fcBlockHibachi_side");
        map.add(61, "fcBlockBlockDispenser_bottom");
        map.add(61, "fcBlockHibachi_bottom");
        map.add(61, "fcBlockDetectorBlock_bottom");
        map.add(62, "fcBlockPulley_top");
        map.add(62, "fcBlockBellows_bottom");
        map.add(62, "fcBlockBellows_top");
        map.add(63, "fcBlockPulley_side");
        map.add(64, "fcBlockHopper_bottom");
        map.add(64, "fcBlockPulley_bottom");
        map.add(64, "fcBlockHopper_top");
        map.add(64, "fcBlockGearBox_output");
        map.add(65, "fcBlockTurntable_top");
        map.add(66, "fcBlockTurntable_side");
        map.add(67, "fcBlockMillStone_bottom");
        map.add(67, "fcBlockTurntable_bottom");
        map.add(68, "fcBlockPulley_top");
        map.add(68, "fcBlockBellows_bottom");
        map.add(68, "fcBlockBellows_top");
        map.add(69, "fcBlockBellows_front");
        map.add(70, "fcBlockBellows_side");
        map.add(71, "fcBlockPulley_top");
        map.add(71, "fcBlockBellows_bottom");
        map.add(71, "fcBlockBellows_top");
        map.add(72, "fcBlockSlabWicker");
        map.add(72, "fcBlockPlatform_bottom");
        map.add(72, "fcBlockWicker");
        map.add(72, "fcBlockHopper_wicker");
        map.add(72, "fcBlockPlatform_top");
        map.add(73, "fcBlockPlatform_side");
        map.add(74, "fcBlockSlabWicker");
        map.add(74, "fcBlockPlatform_bottom");
        map.add(74, "fcBlockWicker");
        map.add(74, "fcBlockHopper_wicker");
        map.add(74, "fcBlockPlatform_top");
        map.add(75, "fcBlockUnfiredPottery");
        map.add(76, "fcBlockCrucible_contents");
        map.add(76, "fcBlockUnfiredPottery_cooking");
        map.add(76, "fcBlockHopper_contents");
        map.add(77, "fcBlockPlanter");
        map.add(78, "fcBlockPlanter_top_soil");
        map.add(79, "fcBlockAnvil");
        map.add(80, "fcBlockBuddyBlock");
        map.add(81, "fcBlockBuddyBlock_on");
        map.add(82, "fcBlockBuddyBlock_front");
        map.add(83, "fcBlockBuddyBlock_front_on");
        map.add(84, "fcBlockMiningCharge_top");
        map.add(85, "fcBlockMiningCharge_side");
        map.add(86, "fcBlockMiningCharge_side_vert");
        map.add(87, "fcBlockMiningCharge_bottom");
        map.add(88, "fcBlockUrn");
        map.add(89, "fcBlockPedestalStone_top");
        map.add(89, "fcBlockColumnStone_top");
        map.add(89, "fcBlockPedestalStone_side");
        map.add(89, "fcBlockDecorativeStone");
        map.add(90, "fcBlockColumnStone_side");
        map.add(91, "fcBlockPedestalStone_top");
        map.add(91, "fcBlockColumnStone_top");
        map.add(91, "fcBlockPedestalStone_side");
        map.add(91, "fcBlockDecorativeStone");
        map.add(92, "fcBlockPedestalStone_top");
        map.add(92, "fcBlockColumnStone_top");
        map.add(92, "fcBlockPedestalStone_side");
        map.add(92, "fcBlockDecorativeStone");
        map.add(93, "fcBlockTableWoodOak_leg");
        map.add(93, "fcBlockTurntable_switch");
        map.add(93, "fcBlockHopper_side");
        map.add(93, "fcBlockSaw");
        map.add(93, "fcBlockGearBox");
        map.add(93, "fcBlockTableWoodOak_top");
        map.add(93, "fcBlockSlats_side");
        map.add(94, "fcBlockTableWoodOak_leg");
        map.add(94, "fcBlockTurntable_switch");
        map.add(94, "fcBlockHopper_side");
        map.add(94, "fcBlockSaw");
        map.add(94, "fcBlockGearBox");
        map.add(94, "fcBlockTableWoodOak_top");
        map.add(94, "fcBlockSlats_side");
        map.add(95, "fcBlockSlabWicker");
        map.add(95, "fcBlockPlatform_bottom");
        map.add(95, "fcBlockWicker");
        map.add(95, "fcBlockHopper_wicker");
        map.add(95, "fcBlockPlatform_top");
        map.add(96, "fcBlockDung");
        map.add(97, "fcBlockSoulforgedSteel");
        map.add(97, "fcBlockInfernalEnchanter_bottom");
        map.add(98, "fcBlockConcentratedHellfire");
        map.add(99, "fcBlockPadding");
        map.add(100, "fcBlockStub");
        map.add(100, "fcBlockSoap_top");
        map.add(101, "fcBlockSoap");
        // /btwmodtex/btwterrain01.png, 102 -> (none)
        map.add(103, "fcBlockRope_side");
        map.add(104, "fcBlockGrate");
        map.add(105, "fcBlockVineTrap");
        map.add(106, "fcBlockBloodWood");
        map.add(107, "fcBlockBloodWood_side");
        map.add(108, "fcBlockSaplingBloodWood");
        // /btwmodtex/btwterrain01.png, 109 -> (none)
        map.add(110, "fcBlockSlabWicker");
        map.add(110, "fcBlockPlatform_bottom");
        map.add(110, "fcBlockWicker");
        map.add(110, "fcBlockHopper_wicker");
        map.add(110, "fcBlockPlatform_top");
        map.add(111, "fcBlockPlanter_top_soulsand");
        map.add(112, "fcBlockLens_output");
        map.add(113, "fcBlockLens");
        map.add(114, "fcBlockLens_input");
        map.add(115, "fcBlockLens_spotlight");
        map.add(116, "fcBlockSlats");
        map.add(116, "fcBlockHopper_slats");
        map.add(117, "fcBlockPlanter_top_grass");
        map.add(118, "fcBlockHopper_ironbars");
        // /btwmodtex/btwterrain01.png, 119 -> (none)
        // /btwmodtex/btwterrain01.png, 120 -> (none)
        map.add(121, "fcBlockInfernalEnchanter_top");
        map.add(122, "fcBlockInfernalEnchanter_side");
        map.add(123, "fcBlockSoulforgedSteel");
        map.add(123, "fcBlockInfernalEnchanter_bottom");
        map.add(124, "fcBlockGroth_bottom");
        map.add(124, "fcBlockNetherrackGrothed_top");
        map.add(125, "fcBlockNetherrackGrothed_side");
        map.add(126, "fcBlockNetherrackGrothed_bottom");
        map.add(127, "fcBlockGroth_top");
        map.add(128, "fcBlockGroth_top_grown");
        map.add(129, "fcBlockGroth_side");
        map.add(130, "fcBlockGroth_bottom");
        map.add(130, "fcBlockNetherrackGrothed_top");
        // /btwmodtex/btwterrain01.png, 131 -> (none)
        // /btwmodtex/btwterrain01.png, 132 -> (none)
        // /btwmodtex/btwterrain01.png, 133 -> (none)
        map.add(134, "FCBlockSlabDirt_grass_side");
        map.add(135, "FCBlockSlabDirt_grass_side_overlay");
        map.add(136, "FCBlockFarmlandFertilized_wet");
        map.add(137, "FCBlockFarmlandFertilized_dry");
        map.add(138, "fcBlockScrewPump_top");
        map.add(139, "fcBlockScrewPump_front");
        map.add(140, "fcBlockDecorativeWoodSpruce");
        map.add(140, "fcBlockScrewPump_side");
        map.add(141, "fcBlockScrewPump_bottom");
        map.add(141, "fcBlockGearBox_input");
        map.add(142, "fcBlockStake_top");
        map.add(143, "fcBlockStake_top_string");
        map.add(144, "fcBlockStake_side");
        map.add(145, "fcBlockStake_side_string");
        map.add(146, "fcBlockStakeString");
        map.add(147, "fcBlockPlanter_top_fertilized");
        map.add(148, "fcBlockWhiteStone");
        map.add(148, "fcBlockDecorativeWhiteStone");
        map.add(149, "fcBlockWhiteCobble");
        map.add(150, "fcBlockBarrel_top");
        map.add(150, "fcBlockPowderKeg_bottom");
        map.add(151, "fcBlockBarrel_side");
        map.add(152, "fcBlockPowderKeg_top");
        map.add(153, "fcBlockPowderKeg_side");
        map.add(154, "fcBlockBarrel_top");
        map.add(154, "fcBlockPowderKeg_bottom");
        map.add(155, "fcBlockColumnWhiteStone_side");
        map.add(156, "fcBlockChoppingBlock_dirty");
        map.add(157, "fcBlockLightningRod");
        map.add(158, "fcBlockDecorativeNetherBrick");
        map.add(159, "fcBlockColumnNetherBrick_side");
        map.add(160, "fcBlockDecorativeBrick");
        map.add(161, "fcBlockColumnBrick_side");
        map.add(162, "fcBlockDecorativeSandstone_top");
        map.add(163, "fcBlockDecorativeSandstone_side");
        map.add(164, "fcBlockDecorativeSandstone_bottom");
        map.add(165, "fcBlockColumnSandstone_side");
        map.add(166, "fcBlockColumnStoneBrick_side");
        map.add(166, "fcBlockDecorativeStoneBrick");
        map.add(167, "fcBlockColumnStoneBrick_side");
        map.add(167, "fcBlockDecorativeStoneBrick");
        map.add(168, "FCBlockDecorativeWoodOak");
        map.add(169, "fcBlockColumnWoodOak_side");
        map.add(170, "fcBlockDecorativeWoodSpruce");
        map.add(170, "fcBlockScrewPump_side");
        map.add(171, "fcBlockColumnWoodSpruce_side");
        map.add(172, "fcBlockDecorativeWoodBirch");
        map.add(173, "fcBlockColumnWoodBirch_side");
        map.add(174, "fcBlockDecorativeWoodJungle");
        map.add(175, "fcBlockColumnWoodJungle_side");
        map.add(176, "fcBlockChoppingBlock");
        map.add(177, "FCBlockBlightL0_top");
        map.add(178, "FCBlockBlightL0_side");
        map.add(179, "FCBlockBlightL0_bottom");
        map.add(179, "FCBlockBlightL1_bottom");
        map.add(180, "FCBlockBlightL1_top");
        map.add(181, "FCBlockBlightL1_side");
        map.add(182, "FCBlockBlightL0_bottom");
        map.add(182, "FCBlockBlightL1_bottom");
        map.add(183, "FCBlockBlightL2_top");
        map.add(184, "FCBlockBlightL2_roots");
        map.add(185, "FCBlockBlightL2_side");
        map.add(186, "FCBlockBlightL2_bottom");
        map.add(187, "FCBlockBlightL3_top");
        map.add(188, "FCBlockBlightL3_roots");
        map.add(189, "FCBlockBlightL3_side");
        map.add(190, "FCBlockPackedEarth");
        // /btwmodtex/btwterrain01.png, 191 -> (none)
        map.add(192, "FCBlockSlabDirt_grass_snow_side");
        map.add(208, "fcBlockCandle_c00");
        map.add(208, "fcBlockCrucible_top");
        map.add(208, "fcBlockVase_c00");
        map.add(209, "fcBlockCandle_c01");
        map.add(209, "fcBlockVase_c02");
        map.add(210, "fcBlockVase_c03");
        map.add(210, "fcBlockCandle_c02");
        map.add(211, "fcBlockCandle_c03");
        map.add(211, "fcBlockVase_c04");
        map.add(212, "fcBlockVase_c05");
        map.add(212, "fcBlockCandle_c04");
        map.add(213, "fcBlockVase_c06");
        map.add(213, "fcBlockCandle_c05");
        map.add(214, "fcBlockVase_c07");
        map.add(214, "fcBlockCandle_c06");
        map.add(215, "fcBlockVase_c08");
        map.add(215, "fcBlockCandle_c07");
        map.add(216, "fcBlockVase_c09");
        map.add(216, "fcBlockCandle_c08");
        map.add(217, "fcBlockCandle_c09");
        map.add(217, "fcBlockVase_c10");
        map.add(218, "fcBlockCandle_c10");
        map.add(218, "fcBlockVase_c11");
        map.add(219, "fcBlockCandle_c11");
        map.add(219, "fcBlockVase_c12");
        map.add(220, "fcBlockCandle_c12");
        map.add(220, "fcBlockVase_c13");
        map.add(221, "fcBlockCandle_c13");
        map.add(221, "fcBlockVase_c14");
        map.add(222, "fcBlockVase_c15");
        map.add(222, "fcBlockCandle_c14");
        map.add(223, "fcBlockCandle_c15");
        map.add(223, "fcBlockVase_c01");
        map.add(224, "fcBlockCandle_c00");
        map.add(224, "fcBlockCrucible_top");
        map.add(224, "fcBlockVase_c00");
        map.add(225, "fcBlockCandle_c15");
        map.add(225, "fcBlockVase_c01");
        map.add(226, "fcBlockCandle_c01");
        map.add(226, "fcBlockVase_c02");
        map.add(227, "fcBlockVase_c03");
        map.add(227, "fcBlockCandle_c02");
        map.add(228, "fcBlockCandle_c03");
        map.add(228, "fcBlockVase_c04");
        map.add(229, "fcBlockVase_c05");
        map.add(229, "fcBlockCandle_c04");
        map.add(230, "fcBlockVase_c06");
        map.add(230, "fcBlockCandle_c05");
        map.add(231, "fcBlockVase_c07");
        map.add(231, "fcBlockCandle_c06");
        map.add(232, "fcBlockVase_c08");
        map.add(232, "fcBlockCandle_c07");
        map.add(233, "fcBlockVase_c09");
        map.add(233, "fcBlockCandle_c08");
        map.add(234, "fcBlockCandle_c09");
        map.add(234, "fcBlockVase_c10");
        map.add(235, "fcBlockCandle_c10");
        map.add(235, "fcBlockVase_c11");
        map.add(236, "fcBlockCandle_c11");
        map.add(236, "fcBlockVase_c12");
        map.add(237, "fcBlockCandle_c12");
        map.add(237, "fcBlockVase_c13");
        map.add(238, "fcBlockCandle_c13");
        map.add(238, "fcBlockVase_c14");
        map.add(239, "fcBlockVase_c15");
        map.add(239, "fcBlockCandle_c14");
        map.add(240, "fcBlockHemp_bottom_00");
        map.add(241, "fcBlockHemp_bottom_01");
        map.add(242, "fcBlockHemp_bottom_02");
        map.add(243, "fcBlockHemp_bottom_03");
        map.add(244, "fcBlockHemp_bottom_04");
        map.add(245, "fcBlockHemp_bottom_05");
        map.add(246, "fcBlockHemp_bottom_06");
        map.add(247, "fcBlockHemp_bottom_07");
        map.add(248, "fcBlockHemp_top");

        map = new TileMapping("/btwmodtex/btwitems01.png", "/textures/items/");
        map.add(0, "fcItemBucketCement");
        map.add(1, "fcItemNethercoal");
        map.add(2, "fcItemSeedsHemp");
        map.add(3, "fcItemHemp");
        map.add(4, "fcItemGear");
        map.add(5, "fcItemFlour");
        map.add(6, "fcItemFibersHemp");
        map.add(7, "fcItemLeatherScoured");
        map.add(8, "fcItemDonut");
        map.add(9, "fcItemRope");
        map.add(10, "fcItemSlats");
        // /btwmodtex/btwitems01.png,  11 -> (none)
        map.add(12, "fcItemWaterWheel");
        map.add(13, "fcItemBladeWindMill");
        map.add(14, "fcItemWindMill");
        map.add(15, "fcItempFabric");
        map.add(16, "fcItemGrate");
        map.add(17, "fcItemWicker");
        map.add(18, "fcItemLeatherTanned");
        map.add(19, "fcItemStrap");
        map.add(20, "fcItemBelt");
        map.add(21, "fcItemFoulFood");
        map.add(22, "fcItemBladeWood");
        map.add(23, "fcItemGlue");
        map.add(24, "fcItemTallow");
        map.add(25, "fcItemHaft");
        map.add(26, "fcItemIngotSteel");
        map.add(27, "fcItemPickAxeRefined");
        map.add(28, "fcItemShovelRefined");
        map.add(29, "fcItemHoeRefined");
        map.add(30, "fcItemAxeBattle");
        map.add(31, "fcItemSwordRefined");
        map.add(32, "fcItemNetherrackGround");
        map.add(33, "fcItemDustHellfire");
        map.add(34, "fcItemConcentratedHellfire");
        map.add(35, "fcItemArmorPlate");
        // /btwmodtex/btwitems01.png,  36 -> (none)
        map.add(37, "fcItemChestplatePlate");
        map.add(38, "fcItemLeggingsPlate");
        map.add(39, "fcItemBootsPlate");
        // /btwmodtex/btwitems01.png,  40 -> (none)
        map.add(41, "fcItemArrowheadBroadhead");
        map.add(42, "fcItemArrowBroadhead");
        map.add(43, "fcItemDustCoal");
        map.add(44, "fcItemPadding");
        map.add(45, "fcItemFilament");
        map.add(46, "fcItemRedstoneEye");
        map.add(47, "fcItemUrn");
        map.add(48, "fcItemUrnSoul");
        map.add(49, "fcItemEggPoached");
        map.add(50, "fcItemPotash");
        map.add(51, "fcItemSoap");
        map.add(52, "fcItemDustSaw");
        // /btwmodtex/btwitems01.png,  53 -> (none)
        map.add(54, "fcItemChestplateLeatherTanned");
        map.add(55, "fcItemLeggingsLeatherTanned");
        map.add(56, "fcItemBootsLeatherTanned");
        // /btwmodtex/btwitems01.png,  57 -> (none)
        // /btwmodtex/btwitems01.png,  58 -> (none)
        // /btwmodtex/btwitems01.png,  59 -> (none)
        map.add(60, "fcItemDynamite");
        // /btwmodtex/btwitems01.png,  61 -> (none)
        map.add(62, "fcItemDustSoul");
        map.add(63, "fcItemMattock");
        map.add(64, "fcItemHatchetRefined");
        map.add(65, "fcItemNetherSludge");
        map.add(66, "fcItemBrickNether");
        map.add(67, "fcItemTuningFork");
        map.add(68, "fcItemScrollArcane");
        map.add(69, "fcItemCandle");
        // /btwmodtex/btwitems01.png,  70 -> (none)
        map.add(71, "fcItemMould");
        map.add(72, "fcItemCanvas");
        map.add(73, "fcItemKibble");
        map.add(74, "fcItemEggRaw");
        map.add(75, "fcItemEggFried");
        map.add(76, "fcItemScrew");
        map.add(77, "fcItemArrowRotten");
        map.add(78, "fcItemOcularOfEnder");
        map.add(79, "fcItemEnderSpectacles");
        map.add(80, "fcItemStake");
        map.add(81, "fcItemBrimstone");
        map.add(82, "fcItemNitre");
        map.add(83, "fcItemElement");
        // /btwmodtex/btwitems01.png,  84 -> (none)
        map.add(85, "fcItemBlastingOil");
        map.add(86, "fcItemWindMillVertical");
        map.add(87, "fcItemPotatoBoiled");
        map.add(88, "fcItemMuttonRaw");
        map.add(89, "fcItemMuttonCooked");
        map.add(90, "fcItemWitchWart");
        map.add(91, "fcItemCarrotCooked");
        map.add(92, "fcItemSandwichTasty");
        // /btwmodtex/btwitems01.png,  93 -> (none)
        map.add(94, "fcItemHamAndEggs");
        // /btwmodtex/btwitems01.png,  95 -> (none)
        // /btwmodtex/btwitems01.png,  96 -> (none)
        // /btwmodtex/btwitems01.png,  97 -> (none)
        // /btwmodtex/btwitems01.png,  98 -> (none)
        map.add(99, "fcItemSoupChicken");
        map.add(100, "fcItemChowder");
        map.add(101, "fcItemStewHearty");
        // /btwmodtex/btwitems01.png, 102 -> (none)
        map.add(103, "fcItemMushroomRed");
        map.add(104, "fcItemMushroomBrown");
        map.add(105, "fcItemNuggetIron");
        map.add(106, "fcItemMail");
        map.add(107, "fcItemMushroomOmletRaw");
        map.add(108, "fcItemMushroomOmletCooked");
        map.add(109, "fcItemEggScrambledRaw");
        map.add(110, "fcItemEggScrambledCooked");
        // /btwmodtex/btwitems01.png, 111 -> (none)
    }

    public static TileMapping getTileMapping(String tilesheetName) {
        return allMappings.get(tilesheetName);
    }

    public TileMapping(String tilesheetName, String dirPrefix) {
        this.tilesheetName = tilesheetName;
        this.dirPrefix = dirPrefix;
        allMappings.put(tilesheetName, this);
    }

    public String getTilesheetName() {
        return tilesheetName;
    }

    public String getDirectoryPrefix() {
        return dirPrefix;
    }

    public void add(int tileNum, String basename) {
        if (tileNum >= 0 && tileNum < 256 && basename != null) {
            tiles.put(getDirectoryPrefix() + basename + ".png", tileNum);
        }
    }

    public void add(int row, int col, String basename) {
        add(row + 16 * col, basename);
    }

    public void addAll(String[] names) {
        for (int i = 0; i < names.length; i++) {
            add(i, names[i]);
        }
    }

    public List<String> getTileNames(int tileNum) {
        List<String> matches = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : tiles.entrySet()) {
            if (tileNum == entry.getValue()) {
                matches.add(entry.getKey());
            }
        }
        return matches;
    }

    public String[][] getTileNames() {
        String[][] lists = new String[256][];
        for (Map.Entry<String, Integer> entry : tiles.entrySet()) {
            String name = entry.getKey();
            int num = entry.getValue();
            String[] oldList = lists[num];
            String[] newList;
            if (oldList == null) {
                newList = new String[]{name};
            } else {
                newList = new String[oldList.length + 1];
                System.arraycopy(oldList, 0, newList, 0, oldList.length);
                newList[oldList.length] = name;
            }
            lists[num] = newList;
        }
        return lists;
    }
}
