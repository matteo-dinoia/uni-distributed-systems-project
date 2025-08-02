package utils;

import static java.lang.System.out;

public class Utils {
    @SuppressWarnings("EmptyMethod")
    public static <T> void ignore(T ignored) {
    }

    public static void debugPrint(boolean debug, String s) {
        if (debug)
            out.println(s);
    }
}
