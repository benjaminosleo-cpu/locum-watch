package com.benjaminosleo.locumwatch.art;

/** Tiny seeded random generator, so every painted detail lands in the same place each time. */
final class Rng {
    private long s;

    Rng(long seed) { seed(seed); }

    void seed(long seed) { s = seed * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L; next(); next(); }

    private long next() {
        s ^= s << 13; s ^= s >>> 7; s ^= s << 17;
        return s;
    }

    /** 0..1 */
    float f() { return ((next() >>> 11) & 0xFFFFFF) / (float) 0x1000000; }
    /** a..b */
    float r(float a, float b) { return a + (b - a) * f(); }
    /** 0..n-1 */
    int n(int n) { return (int) (f() * n) % n; }
    /** -1..1 */
    float s() { return f() * 2f - 1f; }
    boolean chance(float p) { return f() < p; }
}
