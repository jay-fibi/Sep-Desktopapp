import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple greeting program that greets the user based on the current system time.
 *
 * <p>Run directly (single-file source): {@code java Greeting.java}
 * <br>Or compile and run: {@code javac Greeting.java && java Greeting}
 * <br>Optional name: {@code java Greeting.java Jay}
 */
public class Greeting {

    /** Formatter for the time shown to the user, e.g. 07:25:03 AM. */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm:ss a");

    public static void main(String[] args) {
        String name = (args.length > 0 && !args[0].isBlank()) ? args[0].trim() : "there";
        LocalTime now = LocalTime.now();

        System.out.printf("%s, %s! The current system time is %s.%n",
                greetingFor(now), name, now.format(TIME_FORMAT));
    }

    /**
     * Returns a greeting appropriate for the given time of day.
     *
     * @param time the time of day, never {@code null}
     * @return "Good morning" before 12:00, "Good afternoon" before 17:00,
     *         otherwise "Good evening"
     */
    static String greetingFor(LocalTime time) {
        int hour = time.getHour();
        if (hour < 12) {
            return "Good morning";
        } else if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }
}
