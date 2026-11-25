package androidx.core.math;

public class MathUtils {
    public static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
    public static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
