package com.prupe.mcpatcher.mal.tessellator;

import com.prupe.mcpatcher.MAL;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TessellatorFactory;

abstract public class TessellatorAPI {
    private static final TessellatorAPI instance = MAL.newInstance(TessellatorAPI.class, "tessellator");

    public static Tessellator getTessellator() {
        return instance.getTessellator_Impl();
    }

    public static int draw(Tessellator tessellator) {
        return instance.draw_Impl(tessellator);
    }

    abstract protected Tessellator getTessellator_Impl();

    abstract protected int draw_Impl(Tessellator tessellator);

    final private static class V1 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return Tessellator.instance;
        }

        @Override
        protected int draw_Impl(Tessellator tessellator) {
            return tessellator.draw();
        }
    }

    final private static class V2 extends TessellatorAPI {
        @Override
        protected Tessellator getTessellator_Impl() {
            return TessellatorFactory.getInstance().getTessellator();
        }

        @Override
        protected int draw_Impl(Tessellator tessellator) {
            return TessellatorFactory.getInstance().draw();
        }
    }
}
