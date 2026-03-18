package com.troubledev.level;

public final class XPTable {

private static final long[] LEVEL_THRESHOLDS = {
    0L,      // level 1
    240L,    // level 2
    640L,    // level 3
    1160L,   // level 4
    1760L,   // level 5
    2440L,   // level 6
    3200L,   // level 7
    4000L,   // level 8
    4900L,   // level 9
    5900L,   // level 10
    7000L,   // level 11
    8200L,   // level 12
    9500L,   // level 13
    10900L,  // level 14
    12400L,  // level 15
    14000L,  // level 16
    15700L,  // level 17
    17500L,  // level 18
    19400L,  // level 19
    21400L,  // level 20
    23600L,  // level 21
    26000L,  // level 22
    28600L,  // level 23
    31400L,  // level 24
    34400L,  // level 25
    38400L,  // level 26
    43400L,  // level 27
    49400L,  // level 28
    56400L,  // level 29
    63900L   // level 30
};

    public static final int MAX_LEVEL = 30;
    public static final int START_LEVEL = 1;

    private XPTable() {}

    /**
     * Finds the highest level whose XP threshold the player has reached.
     *
     * Iterates from max level down to 1. First threshold that's <= totalXP wins.
     * Example: totalXP=150 → checks Level 10 (9000? no), Level 9 (6000? no), ...
     * Level 2 (100? yes) → returns 2.
     *
     * Simple loop is fine here - with 10 levels, binary search would be overkill.
     */
    public static int getLevelForXP(long totalXP) {
        if (totalXP < 0) return START_LEVEL;

        for (int level = MAX_LEVEL; level >= START_LEVEL; level--) {
            if (totalXP >= LEVEL_THRESHOLDS[level - 1]) {
                return level;
            }
        }
        return START_LEVEL;
    }

    public static long getXPForLevel(int level) {
        if (level < START_LEVEL) return 0L;
        if (level > MAX_LEVEL) return LEVEL_THRESHOLDS[MAX_LEVEL - 1];
        return LEVEL_THRESHOLDS[level - 1];
    }

    public static long getXPInCurrentLevel(long totalXP) {
        var level = getLevelForXP(totalXP);
        return totalXP - getXPForLevel(level);
    }

    public static long getXPToNextLevel(long totalXP) {
        var level = getLevelForXP(totalXP);
        if (level >= MAX_LEVEL) return 0L;
        return LEVEL_THRESHOLDS[level] - totalXP;
    }

    public static float getProgressToNextLevel(long totalXP) {
        var level = getLevelForXP(totalXP);
        if (level >= MAX_LEVEL) return 1.0f;

        var currentThreshold = LEVEL_THRESHOLDS[level - 1];
        var nextThreshold = LEVEL_THRESHOLDS[level];
        var xpInLevel = totalXP - currentThreshold;
        var xpNeeded = nextThreshold - currentThreshold;

        return xpNeeded == 0 ? 1.0f : (float) xpInLevel / xpNeeded;
    }
}
