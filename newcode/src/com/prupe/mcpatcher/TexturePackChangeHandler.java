package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.util.*;

abstract public class TexturePackChangeHandler {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    private static final ArrayList<TexturePackChangeHandler> handlers = new ArrayList<TexturePackChangeHandler>();
    private static boolean initializing;
    private static boolean changing;
    private static long startTime;
    private static long startMem;

    private boolean updateNeeded;

    protected final String name;
    protected final int order;

    public TexturePackChangeHandler(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public void initialize() {
        beforeChange();
        afterChange();
    }

    public void refresh() {
        beforeChange();
        afterChange();
    }

    abstract public void beforeChange();

    abstract public void afterChange();

    public void afterChange2() {
    }

    protected void setUpdateNeeded(boolean updateNeeded) {
        this.updateNeeded = updateNeeded;
    }

    public static void scheduleTexturePackRefresh() {
        MCPatcherUtils.getMinecraft().scheduleTexturePackRefresh();
    }

    public static void register(TexturePackChangeHandler handler) {
        if (handler != null) {
            if (Minecraft.getInstance().getResourceBundle() != null) {
                try {
                    logger.info("initializing %s...", handler.name);
                    handler.initialize();
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.severe("%s initialization failed", handler.name);
                }
            }
            handlers.add(handler);
            logger.fine("registered texture pack handler %s, priority %d", handler.name, handler.order);
            Collections.sort(handlers, new Comparator<TexturePackChangeHandler>() {
                public int compare(TexturePackChangeHandler o1, TexturePackChangeHandler o2) {
                    return o1.order - o2.order;
                }
            });
        }
    }

    public static void earlyInitialize(String className, String methodName) {
        try {
            logger.fine("calling %s.%s", className, methodName);
            Class.forName(className).getDeclaredMethod(methodName).invoke(null);
        } catch (Throwable e) {
        }
    }

    public static void checkForTexturePackChange() {
        for (TexturePackChangeHandler handler : handlers) {
            if (handler.updateNeeded) {
                handler.updateNeeded = false;
                try {
                    logger.info("refreshing %s...", handler.name);
                    handler.refresh();
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.severe("%s refresh failed", handler.name);
                }
            }
        }
    }

    public static void beforeChange1(boolean initializing1) {
        logger.finer("beforeChange1(%s) initializing=%s changing=%s", initializing1, initializing, changing);
        if (initializing1) {
            logger.finer("skipping beforeChange1 because we are still initializing");
            initializing = true;
            return;
        }
        if (changing && !initializing) {
            new RuntimeException("unexpected recursive call to TexturePackChangeHandler").printStackTrace();
            return;
        }
        changing = true;
        startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        startMem = runtime.totalMemory() - runtime.freeMemory();
        List<IResourcePack> resourcePacks = TexturePackAPI.getResourcePacks(null);
        logger.fine("%s resource packs (%d selected):", initializing ? "initializing" : "changing", resourcePacks.size());
        for (IResourcePack pack : resourcePacks) {
            logger.fine("resource pack: %s", pack);
        }

        for (TexturePackChangeHandler handler : handlers) {
            try {
                logger.info("refreshing %s (pre)...", handler.name);
                handler.beforeChange();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.beforeChange failed", handler.name);
            }
        }

        TextureManager textureManager = MCPatcherUtils.getMinecraft().getTextureManager();
        if (textureManager != null) {
            Set<ResourceAddress> texturesToUnload = new HashSet<ResourceAddress>();
            for (Map.Entry<ResourceAddress, ITexture> entry : textureManager.texturesByName.entrySet()) {
                ResourceAddress resource = entry.getKey();
                ITexture texture = entry.getValue();
                if (texture instanceof TextureNamed && !TexturePackAPI.hasResource(resource)) {
                    texturesToUnload.add(resource);
                }
            }
            for (ResourceAddress resource : texturesToUnload) {
                TexturePackAPI.unloadTexture(resource);
            }
        }
    }

    public static void afterChange1(boolean initializing1) {
        logger.finer("afterChange1(%s) initializing=%s changing=%s", initializing1, initializing, changing);
        if (initializing && !initializing1) {
            logger.finer("deferring afterChange1 because we are still initializing");
            return;
        }
        for (TexturePackChangeHandler handler : handlers) {
            try {
                logger.info("refreshing %s (post)...", handler.name);
                handler.afterChange();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.afterChange failed", handler.name);
            }
        }

        for (int i = handlers.size() - 1; i >= 0; i--) {
            TexturePackChangeHandler handler = handlers.get(i);
            try {
                handler.afterChange2();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.afterChange2 failed", handler.name);
            }
        }

        System.gc();
        long timeDiff = System.currentTimeMillis() - startTime;
        Runtime runtime = Runtime.getRuntime();
        long memDiff = runtime.totalMemory() - runtime.freeMemory() - startMem;
        logger.info("done (%.3fs elapsed, mem usage %+.1fMB)\n", timeDiff / 1000.0, memDiff / 1048576.0);
        changing = false;
        initializing = false;
    }
}
