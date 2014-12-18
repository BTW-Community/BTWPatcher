package net.minecraft.src;

// Introduced in 14w25a.
public class TessellatorFactory {
    public static TessellatorFactory getInstance() {
        return null;
    }

    public Tessellator getTessellator() {
        return null;
    }

    // before 1.8.2-pre1
    public int drawInt() {
        return 0;
    }

    // 1.8.2-pre1+
    public void drawVoid() {
    }
}
