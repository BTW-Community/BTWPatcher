package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderPassAPI;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class RenderPass {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.BETTER_GLASS);

    private static final ResourceLocation RENDERPASS_PROPERTIES = TexturePackAPI.newMCPatcherResourceLocation("renderpass.properties");

    private static final Map<Block, Integer> baseRenderPass = new IdentityHashMap<Block, Integer>();
    private static final Map<Block, Integer> extraRenderPass = new IdentityHashMap<Block, Integer>();
    private static final Map<Block, Integer> renderPassBits = new IdentityHashMap<Block, Integer>();
    private static final Set<Block> customRenderPassBlocks = new HashSet<Block>();

    private static BlendMethod blendMethod;
    private static ResourceLocation blendBlankResource;
    private static boolean enableLightmap;
    private static boolean enableColormap;
    private static final boolean[] backfaceCulling = new boolean[RenderPassAPI.NUM_RENDER_PASSES];

    private static int currentRenderPass = -1;
    private static int maxRenderPass = 1;
    private static boolean canRenderInThisPass;
    private static boolean hasCustomRenderPasses;
    private static boolean ambientOcclusion;

    static {
        RenderPassAPI.instance = new RenderPassAPI() {
            @Override
            public boolean skipDefaultRendering(Block block) {
                return currentRenderPass > MAX_BASE_RENDER_PASS;
            }

            @Override
            public boolean skipThisRenderPass(Block block, int pass) {
                if (currentRenderPass < 0) {
                    return pass > MAX_BASE_RENDER_PASS;
                }
                if (pass < 0) {
                    pass = RenderPassMap.instance.getDefaultRenderPass(block);
                }
                return pass != currentRenderPass;
            }

            @Override
            public boolean useColorMultiplierThisPass(Block block) {
                return currentRenderPass != OVERLAY_RENDER_PASS || enableColormap;
            }

            @Override
            public boolean useLightmapThisPass() {
                return currentRenderPass != OVERLAY_RENDER_PASS || enableLightmap;
            }

            @Override
            public void clear() {
                canRenderInThisPass = false;
                maxRenderPass = MAX_BASE_RENDER_PASS - 1;
                baseRenderPass.clear();
                extraRenderPass.clear();
                renderPassBits.clear();
                customRenderPassBlocks.clear();

                blendMethod = BlendMethod.ALPHA;
                blendBlankResource = blendMethod.getBlankResource();
                if (blendBlankResource == null) {
                    blendBlankResource = BlendMethod.ALPHA.getBlankResource();
                }
                enableLightmap = true;
                enableColormap = false;
                Arrays.fill(backfaceCulling, true);
                backfaceCulling[RenderPassAPI.BACKFACE_RENDER_PASS] = false;

                for (Block block : BlockAPI.getAllBlocks()) {
                    baseRenderPass.put(block, RenderPassMap.instance.getDefaultRenderPass(block));
                }
            }

            @Override
            public void refreshBlendingOptions() {
                Properties properties = TexturePackAPI.getProperties(RENDERPASS_PROPERTIES);
                if (properties != null) {
                    properties = remapProperties(properties);
                    String method = properties.getProperty("blend.overlay", "alpha").trim().toLowerCase();
                    blendMethod = BlendMethod.parse(method);
                    if (blendMethod == null) {
                        logger.error("%s: unknown blend method '%s'", RENDERPASS_PROPERTIES, method);
                        blendMethod = BlendMethod.ALPHA;
                    }
                    blendBlankResource = blendMethod.getBlankResource();
                    if (blendBlankResource == null) {
                        blendBlankResource = BlendMethod.ALPHA.getBlankResource();
                    }
                    enableLightmap = MCPatcherUtils.getBooleanProperty(properties, "enableLightmap.overlay", !blendMethod.isColorBased());
                    enableColormap = MCPatcherUtils.getBooleanProperty(properties, "enableColormap.overlay", false);
                    backfaceCulling[RenderPassAPI.OVERLAY_RENDER_PASS] = MCPatcherUtils.getBooleanProperty(properties, "backfaceCulling.overlay", true);
                    backfaceCulling[RenderPassAPI.CUTOUT_RENDER_PASS] = backfaceCulling[RenderPassMap.instance.getCutoutRenderPass()] = MCPatcherUtils.getBooleanProperty(properties, "backfaceCulling.cutout", true);
                    backfaceCulling[RenderPassAPI.CUTOUT_MIPPED_RENDER_PASS] = MCPatcherUtils.getBooleanProperty(properties, "backfaceCulling.cutout_mipped", backfaceCulling[RenderPassAPI.CUTOUT_RENDER_PASS]);
                    backfaceCulling[RenderPassAPI.TRANSLUCENT_RENDER_PASS] = MCPatcherUtils.getBooleanProperty(properties, "backfaceCulling.translucent", true);
                }
            }

            private Properties remapProperties(Properties properties) {
                Properties newProperties = new Properties();
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String) entry.getKey();
                    key = key.replaceFirst("\\.3$", ".overlay");
                    key = key.replaceFirst("\\.2$", ".backface");
                    if (!key.equals(entry.getKey())) {
                        logger.warning("%s: %s is deprecated in 1.8.  Use %s instead", RENDERPASS_PROPERTIES, entry.getKey(), key);
                    }
                    newProperties.put(key, entry.getValue());
                }
                return newProperties;
            }

            @Override
            public void setRenderPassForBlock(Block block, int pass) {
                if (block == null || pass < 0) {
                    return;
                }
                String name;
                if (pass <= MAX_BASE_RENDER_PASS) {
                    baseRenderPass.put(block, pass);
                    name = "base";
                } else {
                    extraRenderPass.put(block, pass);
                    name = "extra";
                }
                logger.fine("%s %s render pass -> %s",
                    BlockAPI.getBlockName(block), name, RenderPassAPI.instance.getRenderPassName(pass)
                );
                customRenderPassBlocks.add(block);
                maxRenderPass = Math.max(maxRenderPass, pass);
            }

            @Override
            public ResourceLocation getBlankResource(int pass) {
                return pass == OVERLAY_RENDER_PASS ? blendBlankResource : super.getBlankResource(pass);
            }

            @Override
            public ResourceLocation getBlankResource() {
                return getBlankResource(currentRenderPass);
            }
        };

        TexturePackChangeHandler.register(new TexturePackChangeHandler(MCPatcherUtils.BETTER_GLASS, 4) {
            @Override
            public void beforeChange() {
            }

            @Override
            public void afterChange() {
                for (Block block : BlockAPI.getAllBlocks()) {
                    int bits = 0;
                    Integer i = baseRenderPass.get(block);
                    if (i != null && i >= 0) {
                        bits |= (1 << i);
                    }
                    i = extraRenderPass.get(block);
                    if (i != null && i >= 0) {
                        bits |= (1 << i);
                    }
                    renderPassBits.put(block, bits);
                }
            }
        });
    }

    public static void start(int pass) {
        currentRenderPass = RenderPassMap.instance.vanillaToMCPatcher(pass);
        CTMUtils.setBlankResource();
    }

    public static void finish() {
        currentRenderPass = -1;
        CTMUtils.setBlankResource();
    }

    public static boolean skipAllRenderPasses(boolean[] skipRenderPass) {
        return skipRenderPass[0] && skipRenderPass[1] && skipRenderPass[2] && skipRenderPass[3];
    }

    public static boolean checkRenderPasses(Block block, boolean moreRenderPasses) {
        int bits = renderPassBits.get(block) >>> currentRenderPass;
        canRenderInThisPass = (bits & 1) != 0;
        hasCustomRenderPasses = customRenderPassBlocks.contains(block);
        return moreRenderPasses || (bits >>> 1) != 0;
    }

    public static boolean canRenderInThisPass(boolean canRender) {
        return hasCustomRenderPasses ? canRenderInThisPass : canRender;
    }

    // pre-14w02a
    public static boolean shouldSideBeRendered(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        if (BlockAPI.shouldSideBeRendered(block, blockAccess, i, j, k, face)) {
            return true;
        } else if (!extraRenderPass.containsKey(block)) {
            Block neighbor = BlockAPI.getBlockAt(blockAccess, i, j, k);
            return extraRenderPass.containsKey(neighbor);
        } else {
            return false;
        }
    }

    // 14w02a+
    public static boolean shouldSideBeRendered(Block block, IBlockAccess blockAccess, Position position, Direction direction) {
        if (block.shouldSideBeRendered(blockAccess, position, direction)) {
            return true;
        } else if (!extraRenderPass.containsKey(block)) {
            Block neighbor = blockAccess.getBlock(position);
            return extraRenderPass.containsKey(neighbor);
        } else {
            return false;
        }
    }

    public static boolean setAmbientOcclusion(boolean ambientOcclusion) {
        RenderPass.ambientOcclusion = ambientOcclusion;
        return ambientOcclusion;
    }

    public static float getAOBaseMultiplier(float multiplier) {
        return RenderPassAPI.instance.useLightmapThisPass() ? multiplier : 1.0f;
    }

    public static boolean useBlockShading() {
        return RenderPassAPI.instance.useLightmapThisPass();
    }

    public static boolean preRenderPass(int pass) {
        currentRenderPass = pass;
        if (pass > maxRenderPass) {
            return false;
        }
        switch (pass) {
            case RenderPassAPI.SOLID_RENDER_PASS:
            case RenderPassAPI.CUTOUT_MIPPED_RENDER_PASS:
            case RenderPassAPI.CUTOUT_RENDER_PASS:
            case RenderPassAPI.TRANSLUCENT_RENDER_PASS:
            case RenderPassAPI.BACKFACE_RENDER_PASS:
                if (!backfaceCulling[pass]) {
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }
                break;

            case RenderPassAPI.OVERLAY_RENDER_PASS:
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glPolygonOffset(-2.0f, -2.0f);
                GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                if (backfaceCulling[pass]) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                } else {
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }
                if (ambientOcclusion) {
                    GL11.glShadeModel(GL11.GL_SMOOTH);
                }
                blendMethod.applyBlending();
                break;

            default:
                break;
        }
        return true;
    }

    public static int postRenderPass(int value) {
        switch (currentRenderPass) {
            case RenderPassAPI.SOLID_RENDER_PASS:
            case RenderPassAPI.CUTOUT_MIPPED_RENDER_PASS:
            case RenderPassAPI.CUTOUT_RENDER_PASS:
            case RenderPassAPI.TRANSLUCENT_RENDER_PASS:
            case RenderPassAPI.BACKFACE_RENDER_PASS:
                if (!backfaceCulling[currentRenderPass]) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                }
                break;

            case RenderPassAPI.OVERLAY_RENDER_PASS:
                GL11.glPolygonOffset(0.0f, 0.0f);
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
                if (!backfaceCulling[currentRenderPass]) {
                    GL11.glEnable(GL11.GL_CULL_FACE);
                }
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glShadeModel(GL11.GL_FLAT);
                break;

            default:
                break;
        }
        currentRenderPass = -1;
        return value;
    }

    public static void enableDisableLightmap(EntityRenderer renderer, double partialTick) {
        if (RenderPassAPI.instance.useLightmapThisPass()) {
            renderer.enableLightmap(partialTick);
        } else {
            renderer.disableLightmap(partialTick);
        }
    }
}
