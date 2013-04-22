package com.prupe.mcpatcher;

import net.minecraft.src.Icon;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TextureMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class TessellatorUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final Integer MAGIC_VALUE = 0x12345678;

    private static final Map<TextureMap, String> textureMapNames = new WeakHashMap<TextureMap, String>();
    private static final Map<Icon, TextureMap> iconMap = new HashMap<Icon, TextureMap>();
    private static final Map<String, Icon> iconsByName = new HashMap<String, Icon>();
    private static Field[] fieldsToCopy;

    public static boolean haveBufferSize;

    static Tessellator getTessellator(Tessellator tessellator, Icon icon) {
        TextureMap textureMap = iconMap.get(icon);
        if (textureMap == null) {
            return tessellator;
        }
        Tessellator newTessellator = tessellator.children.get(textureMap);
        if (newTessellator == null) {
            String mapName = textureMapNames.get(textureMap);
            if (mapName == null) {
                mapName = textureMap.toString();
            }
            logger.fine("new Tessellator for texture map %s gl texture %d", mapName, textureMap.getTexture().getGlTextureId());
            newTessellator = new Tessellator(0x200000);
            copyFields(tessellator, newTessellator, true);
            newTessellator.textureMap = textureMap;
            tessellator.children.put(textureMap, newTessellator);
        } else {
            copyFields(tessellator, newTessellator, false);
        }
        return newTessellator;
    }

    static void registerTextureMap(TextureMap textureMap, String name) {
        textureMapNames.put(textureMap, name);
    }

    static void registerIcon(TextureMap textureMap, Icon icon) {
        iconMap.put(icon, textureMap);
        iconsByName.put(icon.getIconName(), icon);
    }

    static Icon getIconByName(String name) {
        return iconsByName.get(name);
    }

    private static Field[] getFieldsToCopy(Tessellator tessellator) {
        int saveBufferSize;
        if (haveBufferSize) {
            saveBufferSize = tessellator.bufferSize;
            tessellator.bufferSize = MAGIC_VALUE;
        } else {
            saveBufferSize = 0;
        }
        int saveVertexCount = tessellator.vertexCount;
        int saveAddedVertices = tessellator.addedVertices;
        int saveRawBufferIndex = tessellator.rawBufferIndex;
        tessellator.vertexCount = MAGIC_VALUE;
        tessellator.addedVertices = MAGIC_VALUE;
        tessellator.rawBufferIndex = MAGIC_VALUE;
        ArrayList<Field> fields = new ArrayList<Field>();
        for (Field f : Tessellator.class.getDeclaredFields()) {
            try {
                Class<?> type = f.getType();
                int modifiers = f.getModifiers();
                if (!Modifier.isStatic(modifiers) && type.isPrimitive() && !f.getName().equals("rawBufferSize")) {
                    f.setAccessible(true);
                    if (type == Integer.TYPE && MAGIC_VALUE.equals(f.get(tessellator))) {
                        continue;
                    }
                    logger.finest("copy %s %s %s", Modifier.toString(f.getModifiers()), f.getType().toString(), f.getName());
                    fields.add(f);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        if (!haveBufferSize) {
            tessellator.bufferSize = saveBufferSize;
        }
        tessellator.vertexCount = saveVertexCount;
        tessellator.addedVertices = saveAddedVertices;
        tessellator.rawBufferIndex = saveRawBufferIndex;
        return fields.toArray(new Field[fields.size()]);
    }

    private static void copyFields(Tessellator a, Tessellator b, boolean isNew) {
        if (fieldsToCopy == null) {
            fieldsToCopy = getFieldsToCopy(a);
        }
        for (Field field : fieldsToCopy) {
            try {
                Object value = field.get(a);
                if (isNew) {
                    logger.finest("copy %s %s %s = %s", Modifier.toString(field.getModifiers()), field.getType(), field.getName(), value);
                }
                field.set(b, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (a.isDrawing && !b.isDrawing) {
            b.startDrawing(a.drawMode);
        } else if (!a.isDrawing && b.isDrawing) {
            b.reset();
        }
    }

    static void clear(Tessellator tessellator) {
        for (Tessellator child : tessellator.children.values()) {
            clear(child);
        }
        tessellator.children.clear();
        textureMapNames.clear();
        iconMap.clear();
        iconsByName.clear();
    }

    public static void resetChildren(Tessellator tessellator) {
        for (Tessellator child : tessellator.children.values()) {
            child.reset();
        }
    }

    public static int drawChildren(int sum, Tessellator tessellator) {
        for (Tessellator child : tessellator.children.values()) {
            sum += child.draw();
        }
        return sum;
    }

    public static void startDrawingChildren(Tessellator tessellator, int drawMode) {
        for (Tessellator child : tessellator.children.values()) {
            child.startDrawing(drawMode);
        }
    }

    private static String toString(Tessellator tessellator) {
        if (tessellator == null) {
            return "Tessellator{null}";
        }
        String desc = tessellator.toString();
        TextureMap textureMap = tessellator.textureMap;
        if (textureMap != null) {
            String mapName = textureMapNames.get(textureMap);
            if (mapName == null) {
                desc = textureMap.toString();
            } else {
                desc = mapName;
            }
        }
        return String.format("Tessellator{%s, isDrawing=%s, %d children}", desc, tessellator.isDrawing, tessellator.children.size());
    }
}
