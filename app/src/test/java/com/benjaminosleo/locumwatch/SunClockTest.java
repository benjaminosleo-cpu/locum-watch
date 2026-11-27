package com.benjaminosleo.locumwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SunClockTest {
    @Test
    public void sunriseAndSunsetAreRoughlyRightForManchester() {
        double[] nov = SunClock.sun(331, 0);      // 27 November, GMT: about 07:58 and 15:57
        assertEquals(7.97, nov[0], 0.15);
        assertEquals(15.95, nov[2], 0.15);
        double[] jun = SunClock.sun(172, 1);      // 21 June, BST: about 04:40 and 21:42
        assertEquals(4.67, jun[0], 0.2);
        assertEquals(21.7, jun[2], 0.2);
    }

    @Test
    public void artworkAnchorsLineUpWithTheRealSun() {
        for (int day : new int[]{1, 80, 172, 266, 331, 365}) {
            double tz = day > 87 && day < 300 ? 1 : 0;
            double[] s = SunClock.sun(day, tz);
            assertEquals(SunClock.DAWN, SunClock.lightingHour(s[0] + 1e-6, day, tz), 0.01);
            assertEquals(SunClock.DAY, SunClock.lightingHour(s[1], day, tz), 0.01);
            assertEquals(SunClock.DUSK, SunClock.lightingHour(s[2], day, tz), 0.01);
        }
    }

    @Test
    public void lightingHourNeverJumpsOrLeavesTheDay() {
        for (int day = 1; day <= 365; day += 13) {
            float prev = SunClock.lightingHour(0, day, 0);
            for (double clock = 0; clock < 24; clock += 1 / 60.0) {
                float h = SunClock.lightingHour(clock, day, 0);
                assertTrue("in range on day " + day, h >= 0 && h < 24);
                float step = h - prev;
                if (step < -12) step += 24; // passing midnight on the artwork's clock
                assertTrue("goes forwards smoothly on day " + day + " at " + clock + ": " + step, step >= 0 && step < 0.2f);
                prev = h;
            }
        }
    }

    @Test
    public void novemberTeatimeIsAlreadyNight() {
        // 17:30 on 27 November: the sun set an hour and a half ago
        float h = SunClock.lightingHour(17.5, 331, 0);
        assertTrue("after dusk: " + h, h > 19.5f && h < 24f);
    }
}
