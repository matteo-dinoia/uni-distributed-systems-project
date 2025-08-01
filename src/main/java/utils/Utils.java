package utils;

import static java.lang.System.out;

public class Utils {
    @SuppressWarnings("EmptyMethod")
    public static <T> void ignore(T ignored) {
    }

    public static void debugPrint(String s) {
        if (Config.DEBUG)
            out.println(s);
    }
}
