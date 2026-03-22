package com.troubledev.level;

public final class WeaponMasteryTable {

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

    public static final int MAX_LEVEL = 10;
    public static final String METADATA_XP_KEY = "weapon_xp";
    public static final String METADATA_LEVEL_KEY = "weapon_level";
    public static final long XP_PER_KILL = 25L;

    private WeaponMasteryTable() {}

    public static int getLevelForXP(long totalXP) {
        if (totalXP < 0) return 1;
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (totalXP >= LEVEL_THRESHOLDS[level - 1]) {
                return level;
            }
        }
        return 1;
    }

    public static long getXPForLevel(int level) {
        if (level < 1) return 0L;
        if (level > MAX_LEVEL) return LEVEL_THRESHOLDS[MAX_LEVEL - 1];
        return LEVEL_THRESHOLDS[level - 1];
    }

    public static long getXPToNextLevel(long totalXP) {
        int level = getLevelForXP(totalXP);
        if (level >= MAX_LEVEL) return 0L;
        return LEVEL_THRESHOLDS[level] - totalXP;
    }

    public static float getProgressToNextLevel(long totalXP) {
        int level = getLevelForXP(totalXP);
        if (level >= MAX_LEVEL) return 1.0f;
        long current = LEVEL_THRESHOLDS[level - 1];
        long next = LEVEL_THRESHOLDS[level];
        return (float)(totalXP - current) / (next - current);
    }
}