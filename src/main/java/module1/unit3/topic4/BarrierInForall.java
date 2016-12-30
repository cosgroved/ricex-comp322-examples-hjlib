package module1.unit3.topic4;

import edu.rice.hj.api.SuspendableException;

import static edu.rice.hj.Module1.*;

/**
 * <p>BarrierInForall class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class BarrierInForall {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final String[] callArgs = new String[]{"Zero", "One", "Two", "Three", "Four"};

        System.out.println("\n\n  runForallWithoutBarrier: ");
        launchHabaneroApp(() -> {
            runForallWithoutBarrier(callArgs);
        });

        System.out.println("\n\n  runForallWithBarrier: ");
        launchHabaneroApp(() -> {
            runForallWithBarrier(callArgs);
        });
    }

    private static void runForallWithoutBarrier(final String[] strings) throws SuspendableException {

        final int m = strings.length;
        forallPhased(0, m - 1, (i) -> {
            final String s = strings[i]; // "zero" for 0, "one" for 1, etc
            System.out.println("Hello " + s);
            System.out.println("Goodbye " + s);
        });
    }

    private static void runForallWithBarrier(final String[] strings) throws SuspendableException {

        final int m = strings.length;
        forallPhased(0, m - 1, (i) -> {
            final String s = strings[i]; // "zero" for 0, "one" for 1, etc
            System.out.println("Hello " + s);
            next(); // Barrier
            System.out.println("Goodbye " + s);
        });
    }
}