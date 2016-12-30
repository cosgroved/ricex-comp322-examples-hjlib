package module2.unit5.topic5;

import static edu.rice.hj.Module2.*;

/**
 * Class SynchronizedList implements a thread-safe sorted list data structure that supports contains(), add() and remove() methods.
 * <p>
 * Thread safety is guaranteed by declaring each of the methods to be synchronized.
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SortedListReadWriteIsolatedList implements SortedListSet {

    private Entry head;

    /**
     * <p>Constructor for SortedListReadWriteIsolatedList.</p>
     */
    public SortedListReadWriteIsolatedList() {
        // Add sentinels to start and end
        this.head = new Entry(Integer.MIN_VALUE);
        this.head.next = new Entry(Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object object) {

        return isolatedWithReturn(readMode(this), () -> {

            Entry pred, curr;
            int key = object.hashCode();
            pred = this.head;
            curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            return (key == curr.key);

        });
    }

    /**
     * {@inheritDoc}
     */
    public int add(Object object) {

        return isolatedWithReturn(writeMode(this), () -> {

            Entry pred, curr;
            int key = object.hashCode();
            pred = this.head;
            curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            if (key == curr.key) {  // present
                return -1;
            } else {                // not present
                Entry entry = new Entry(object);
                entry.next = curr;
                pred.next = entry;
                return 1;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    public int remove(Object object) {

        return isolatedWithReturn(writeMode(this), () -> {

            Entry pred, curr;
            int key = object.hashCode();
            pred = this.head;
            curr = pred.next;
            while (curr.key < key) {
                pred = curr;
                curr = curr.next;
            }
            if (key == curr.key) {  // present
                pred.next = curr.next;
                return 1;
            } else {
                return -1;         // not present
            }

        });
    }

    // define list entry class
    private class Entry {
        Object object;
        int key;
        Entry next;

        Entry(Object object) {     // usual constructor
            this.object = object;
            this.key = object.hashCode();
        }

        Entry(int key) {           // sentinel constructor
            this.object = null;
            this.key = key;
        }
    }
}
