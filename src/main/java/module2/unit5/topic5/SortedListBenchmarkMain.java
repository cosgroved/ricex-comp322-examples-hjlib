package module2.unit5.topic5;

/**
 * <p>SortedListBenchmarkMain class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SortedListBenchmarkMain {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(String[] args) {
        for (int i = 1; i <= 1; i++) {
            System.out.println("Iteration-" + i);

            SortedListDriver.main("-b ListTest -s SortedListObjectIsolatedList -r 98 -a 1 -d 1".split("\\s+"));
            SortedListDriver.main("-b ListTest -Dhj.isolatedLockType ordered -s SortedListReadWriteIsolatedList -r 98 -a 1 -d 1".split("\\s+"));
            SortedListDriver.main("-b ListTest -Dhj.isolatedLockType unordered -s SortedListReadWriteIsolatedList -r 98 -a 1 -d 1".split("\\s+"));

            System.out.println();

        }
    }

}
