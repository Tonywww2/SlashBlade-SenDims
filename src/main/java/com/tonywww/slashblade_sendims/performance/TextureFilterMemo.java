package com.tonywww.slashblade_sendims.performance;

/** Only consecutive known filter writes are reused; any foreign write invalidates both values. */
public final class TextureFilterMemo {
    public static final int MIN_FILTER = 0x2801;
    public static final int MAG_FILTER = 0x2800;
    private int texture = -1;
    private int min;
    private int mag;
    private int known;

    public boolean needsWrite(int boundTexture, int parameter, int value) {
        if (boundTexture < 0 || boundTexture != texture) return true;
        return switch (parameter) {
            case MIN_FILTER -> (known & 1) == 0 || min != value;
            case MAG_FILTER -> (known & 2) == 0 || mag != value;
            default -> true;
        };
    }

    public void record(int boundTexture, int parameter, int value) {
        if (texture != boundTexture) {
            texture = boundTexture;
            known = 0;
        }
        if (parameter == MIN_FILTER) { min = value; known |= 1; }
        if (parameter == MAG_FILTER) { mag = value; known |= 2; }
    }

    public void clear() { texture = -1; known = 0; }
}
