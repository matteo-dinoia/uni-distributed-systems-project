package utils;

import java.time.Duration;

public class Config {
    public static final int N = 4;
    public static final int W = 3;
    public static final int R = 2;
    public static final Duration MAX_DELAY = Duration.ofMillis(50);
    public static final Duration TIMEOUT = Duration.ofMillis(500);
    public static final Duration TIMOUT_PROBE = Duration.ofMillis(1500);
    // If true than it is debug level else it is only error level
    public static final boolean DEBUG = true;
}
