package com.prupe.mcpatcher.mal.resource;

import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.ResourceLocation;
import net.minecraft.src.ResourcePack;

import java.util.Comparator;
import java.util.regex.Pattern;

public class ResourceLocationWithSource extends ResourceLocation {
    private final ResourcePack source;
    private final int order;
    private final boolean isDirectory;

    public ResourceLocationWithSource(ResourcePack source, ResourceLocation resource) {
        super(resource.getNamespace(), resource.getPath().replaceFirst("/$", ""));
        this.source = source;
        order = ResourceList.getResourcePackOrder(source);
        isDirectory = resource.getPath().endsWith("/");
    }

    public ResourcePack getSource() {
        return source;
    }

    public int getOrder() {
        return order;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    static class Comparator1 implements Comparator<ResourceLocationWithSource> {
        private final boolean bySource;
        private final String suffixExpr;

        Comparator1() {
            this(false, null);
        }

        Comparator1(boolean bySource, String suffix) {
            this.bySource = bySource;
            this.suffixExpr = MCPatcherUtils.isNullOrEmpty(suffix) ? null : Pattern.quote(suffix) + "$";
        }

        @Override
        public int compare(ResourceLocationWithSource o1, ResourceLocationWithSource o2) {
            if (bySource) {
                int result = o1.getOrder() - o2.getOrder();
                if (result != 0) {
                    return result;
                }
            }
            String n1 = o1.getNamespace();
            String n2 = o2.getNamespace();
            int result = n1.compareTo(n2);
            if (result != 0) {
                return result;
            }
            String f1 = o1.getPath();
            String f2 = o2.getPath();
            if (suffixExpr != null) {
                f1 = f1.replaceAll(".*/", "").replaceFirst(suffixExpr, "");
                f2 = f2.replaceAll(".*/", "").replaceFirst(suffixExpr, "");
            }
            return f1.compareTo(f2);
        }
    }
}
