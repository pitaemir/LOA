package com.troubledev.level;

public final class WeaponMasteryTable {

    private static final long[] LEVEL_THRESHOLDS = {
        0L,      // level 1
        100L,    // level 2
        250L,    // level 3
        500L,    // level 4
        900L,    // level 5
        1400L,   // level 6
        2000L,   // level 7
        2700L,   // level 8
        3500L,   // level 9
        4500L    // level 10
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