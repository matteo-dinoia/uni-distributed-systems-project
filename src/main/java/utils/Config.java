package utils;

import java.time.Duration;

public record Config(int N, int W, int R, Duration MAX_DELAY, Duration TIMEOUT, Duration TIMEOUT_PROBE, boolean DEBUG) {
    public static final boolean SHOW_ALL_LOG_IN_TESTS = true;

    public static Config defaultConfig(boolean debug) {
        final Duration maxDelay = Duration.ofMillis(50);
        final Duration timeout = Duration.ofMillis(500);
        final Duration timeoutProbe = Duration.ofMillis(1500);

        return new Config(4, 3, 2, maxDelay, timeout, timeoutProbe, debug);
    }

    public Config quorum(int n, int w, int r) {
        return new Config(n, w, r, this.MAX_DELAY, this.TIMEOUT, this.TIMEOUT_PROBE, this.DEBUG);
    }

    public Config timeoutsMs(int maxDelay, int timeout, int timeoutProbe) {
        return new Config(this.N, this.W, this.R,
                Duration.ofMillis(maxDelay), Duration.ofMillis(timeout), Duration.ofMillis(timeoutProbe),
                this.DEBUG);
    }

}
