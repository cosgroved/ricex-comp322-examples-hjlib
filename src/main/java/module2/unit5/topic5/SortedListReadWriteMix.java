package module2.unit5.topic5;

/**
 * <p>SortedListReadWriteMix class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SortedListReadWriteMix {
    public int readPct;
    public int addPct;
    public int delPct;

    /**
     * <p>set.</p>
     *
     * @param r a int.
     * @param a a int.
     * @param d a int.
     */
    public void set(int r, int a, int d) {
        readPct = r;
        addPct = a;
        delPct = d;
    }

    /**
     * <p>validate.</p>
     *
     * @return a boolean.
     */
    public boolean validate() {
        return 100 == readPct + addPct + delPct;
    }
}
