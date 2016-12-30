package module2.unit5.topic5;

/**
 * <p>SortedListSet interface.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public interface SortedListSet {

    /**
     * <p>add.</p>
     *
     * @param o a {@link Object} object.
     * @return a int.
     */
    int add(Object o);

    /**
     * <p>remove.</p>
     *
     * @param o a {@link Object} object.
     * @return a int.
     */
    int remove(Object o);

    /**
     * <p>contains.</p>
     *
     * @param o a {@link Object} object.
     * @return a boolean.
     */
    boolean contains(Object o);
}
