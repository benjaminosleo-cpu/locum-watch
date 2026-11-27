package com.benjaminosleo.locumwatch;

/**
 * Works out where the real sun is, so the cottage's lighting matches the sky outside.
 * Plain arithmetic, no Android: accurate to a few minutes, which is plenty for a picture.
 */
final class SunClock {
    private SunClock() {}

    // Where the cottage is: South Manchester.
    static final double LATITUDE = 53.42, LONGITUDE = -2.24;
    // The artwork's own anchor hours (see art.Palette): its dawn, midday and dusk.
    static final double DAWN = 6.2, DAY = 12, DUSK = 19.1;

    /** {sunrise, solar noon, sunset} as clock hours for a day of the year and a time-zone offset in hours. */
    static double[] sun(int dayOfYear, double tzHours) {
        double g = 2 * Math.PI * (dayOfYear - 81) / 364.0;
        double eot = (9.87 * Math.sin(2 * g) - 7.53 * Math.cos(g) - 1.5 * Math.sin(g)) / 60.0; // equation of time, hours
        double decl = Math.toRadians(-23.44) * Math.cos(2 * Math.PI * (dayOfYear + 10) / 365.0);
        double lat = Math.toRadians(LATITUDE);
        double cosH = (Math.sin(Math.toRadians(-0.83)) - Math.sin(lat) * Math.sin(decl)) / (Math.cos(lat) * Math.cos(decl));
        double half = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosH)))) / 15.0; // half the day length, hours
        half = Math.max(2.0, Math.min(10.0, half));
        double noon = 12 - LONGITUDE / 15.0 + tzHours - eot;
        return new double[]{noon - half, noon, noon + half};
    }

    /**
     * The "lighting hour" (0..24) for the artwork: its dawn, midday and dusk are lined up
     * with today's real sunrise, solar noon and sunset, and the night is stretched to fit.
     */
    static float lightingHour(double clock, int dayOfYear, double tzHours) {
        double[] s = sun(dayOfYear, tzHours);
        double rise = s[0], noon = s[1], set = s[2];
        double h;
        if (clock >= rise && clock < noon) h = DAWN + (clock - rise) / (noon - rise) * (DAY - DAWN);
        else if (clock >= noon && clock < set) h = DAY + (clock - noon) / (set - noon) * (DUSK - DAY);
        else {
            double sinceSet = clock >= set ? clock - set : clock + 24 - set;
            h = DUSK + sinceSet / (24 - (set - rise)) * (DAWN + 24 - DUSK);
        }
        h %= 24;
        if (h < 0) h += 24;
        return (float) h;
    }
}
