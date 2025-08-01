package utils;

import java.time.Duration;

public class Config {

    // Test were manually configured for this parameter of N W R
    // Changing will make assert fail even though the test is correct
    // This can be changed for use in the main (can add tests with any parameter)

    public static final int N = 4;
    public static final int W = 3;
    public static final int R = 2;

    // Changing timeout,... make test fail because it is just not fast enough
    // And test assume it will pass in time

    public static final Duration MAX_DELAY = Duration.ofMillis(50);
    public static final Duration TIMEOUT = Duration.ofMillis(500);
    public static final Duration TIMOUT_PROBE = Duration.ofMillis(1500);

    // If true than it is debug level else it is only error level
    public static final boolean DEBUG = true;
}
