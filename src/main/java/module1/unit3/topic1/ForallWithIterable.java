package module1.unit3.topic1;

import edu.rice.hj.api.SuspendableException;

import java.util.Arrays;
import java.util.List;

import static edu.rice.hj.Module1.*;

/**
 * <p>ForallWithIterable class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class ForallWithIterable {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final List<Integer> myList = Arrays.asList(1, 2, 3, 4, 5);

        launchHabaneroApp(() -> {
            useFinishAndAsync(myList);
            useForall(myList);
        });

    }

    private static void useFinishAndAsync(final List<Integer> myList) throws SuspendableException {
        System.out.println("useFinishAndAsync: ");
        finish(() -> {
            for (final Integer item : myList) {
                asyncNb(() -> {
                    System.out.printf("  Executing item-%d \n", item);
                });
            }
        });
    }

    private static void useForall(final List<Integer> myList) throws SuspendableException {
        System.out.println("useForall: ");
        forall(myList, (item) -> {
            System.out.printf("  Executing item-%d \n", item);
        });
    }
}