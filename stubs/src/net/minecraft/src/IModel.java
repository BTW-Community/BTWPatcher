package net.minecraft.src;

import java.util.List;

// 14w25a+
public interface IModel {
    List<ModelFace> getFaces(Direction direction);

    List<ModelFace> getDefaultFaces();

    boolean useAO();

    boolean randomizePosition();

    boolean rotate180();

    TextureAtlasSprite getSprite();
}
