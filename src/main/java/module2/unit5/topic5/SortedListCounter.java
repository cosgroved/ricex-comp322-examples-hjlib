package module2.unit5.topic5;
/**
 * <p>SortedListCounter class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SortedListCounter {
    private int reads = 0;
    private int adds = 0;
    private int deletes = 0;
    private int successes = 0;
    private int failures = 0;

    boolean stop = false;

    void setReads(int n) {
        reads = n;
    }

    void setAdds(int n) {
        adds = n;
    }

    void setDeletes(int n) {
        deletes = n;
    }

    void setSuccesses(int n) {
        successes = n;
    }

    void setFailures(int n) {
        failures = n;
    }

    void addReads(int n) {
        reads += n;
    }

    void addAdds(int n) {
        adds += n;
    }

    void addDeletes(int n) {
        deletes += n;
    }

    void addSuccesses(int n) {
        successes += n;
    }

    void addFailures(int n) {
        failures += n;
    }

    int getReads() {
        return reads;
    }

    int getAdds() {
        return adds;
    }

    int getDeletes() {
        return deletes;
    }

    int getSuccesses() {
        return successes;
    }

    int getFailures() {
        return failures;
    }

    boolean timeToStop() {
        return stop;
    }

    void stopNow() {
        stop = true;
    }
}
