package com.prupe.mcpatcher.mal.tessellator;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TessellatorFactory;

abstract public class TessellatorAPI {
    private static final TessellatorAPI instance = MAL.newInstance(TessellatorAPI.class, "tessellator");

    public static Tessellator getTessellator() {
        return instance.getTessellator_Impl();
    }

    abstract protected Tessellator getTessellator_Impl();

    final private static class V1 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return Tessellator.instance;
        }
    }

    final private static class V2 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return TessellatorFactory.getInstance().getTessellator();
        }
    }
}
