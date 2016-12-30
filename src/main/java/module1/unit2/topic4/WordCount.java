package module1.unit2.topic4;

import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.mapreduce2.Aggregator;
import edu.rice.hj.runtime.mapreduce2.MapReduceTask;
import edu.rice.hj.runtime.util.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.rice.hj.Module1.launchHabaneroApp;

/**
 * <p>WordCount class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class WordCount extends MapReduceTask<Void, KeyString, KeyString, Integer, Pair<KeyString, Integer>> {

    private final char[] text;

    /**
     * <p>Constructor for WordCount.</p>
     *
     * @param numMapTasks    a int.
     * @param numReduceTasks a int.
     * @param printTimes     a boolean.
     * @param fileName       a {@link java.lang.String} object.
     */
    protected WordCount(final int numMapTasks, final int numReduceTasks, final boolean printTimes, final String fileName) {
        super(numMapTasks, numReduceTasks, printTimes);

        final File file = new File(fileName);
        final int fileSize = (int) file.length();

        // Allocate file size to avoid index-out-of-bounds complications in MapReduceBase
        text = new char[fileSize];
        try {
            // Read entire file into text array
            final FileReader fileReader = new FileReader(fileName);
            fileReader.read(text, 0, fileSize);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {

        if (args.length < 3) {
            System.out.println("Usage: run <FILE> <NUM_map_tasks> <NUM_reduce_tasks> [report?] ");
            return;
        }
        final boolean report = (args.length > 3);

        final String fileName = args[0];
        final int numMapTasks = Integer.parseInt(args[1]);
        final int numReduceTasks = Integer.parseInt(args[2]);

        launchHabaneroApp(new HjSuspendable() {
            @Override
            public void run() throws SuspendableException {
                final WordCount wordCount = new WordCount(numMapTasks, numReduceTasks, report, fileName);

                // Run instance of MapReduce
                wordCount.run();

                if (report) {
                    final List<Pair<KeyString, Integer>> results = wordCount.results();
                    report(results);
                }
            }
        });
    }

    private static void report(final List<Pair<KeyString, Integer>> results) {
        System.out.println("Result size: " + results.size());

        System.out.println("First 100 entries:");
        int index = 0;
        for (final Pair<KeyString, Integer> result : results) {
            System.out.println("  " + result.left + " : " + result.right);

            index++;
            if (index > 100) {
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Void, List<KeyString>> partition(final int partitionId) {

        final int NOT_IN_WORD = 0;
        final int IN_WORD = 1;

        // Heuristic: divide up file into _numMapTasks chunks
        // NOTE: we overlook the issue that this heuristic may split a word in the middle

        final int chunkSize = (text.length + numMapTasks - 1) / numMapTasks;
        final int startIndex = chunkSize * partitionId;
        final int endIndex = Math.min(startIndex + chunkSize, text.length) - 1;

        final List<KeyString> words = new ArrayList<>();
        {
            int state = NOT_IN_WORD;
            int curr_start = -1;

            int p = startIndex;
            while (p <= endIndex && Character.isLetter(text[p])) {
                p++;
            }

            for (; p <= endIndex; p++) {
                final char ch = Character.toUpperCase(text[p]);
                if (state == IN_WORD) {
                    text[p] = ch;
                    if (!Character.isLetter(ch) && ch != '\'') {
                        final KeyString loopString = new KeyString(text, curr_start, p - 1);
                        words.add(loopString);
                        state = NOT_IN_WORD;
                    }
                } else {
                    if (Character.isLetter(ch)) {
                        text[p] = ch;
                        state = IN_WORD;
                        curr_start = p;
                    }
                }
            }
            if (state == IN_WORD) {
                while ((Character.isLetter(text[p]) || Character.toUpperCase(text[p]) == '\'')) {
                    final char ch = Character.toUpperCase(text[p]);
                    text[p] = ch;
                    p++;
                }
                final KeyString loopString = new KeyString(text, curr_start, p - 1);
                words.add(loopString);
            }
        }
        final Map<Void, List<KeyString>> resultData = new HashMap<>();
        resultData.put(null, words);
        return resultData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(final Void inKey, final KeyString inValue, final Aggregator<KeyString, Integer> aggregator) {
        aggregator.emit(inValue, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<KeyString, Integer> reduceIdentity(final KeyString inKey) {
        return Pair.factory(inKey, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<KeyString, Integer> reduce(final KeyString inKey, final List<Integer> inValue, final Pair<KeyString, Integer> initialValue) {

        int result = initialValue.right;

        for (final Integer loopValue : inValue) {
            result += loopValue;
        }

        return Pair.factory(inKey, result);
    }
}

/**
 * A KeyString instance consists of two ints, startPos and endPos
 */
final class KeyString {
    public final char[] text;
    public final int startPos;
    public final int endPos;
    private int hash = 0;

    KeyString(final char[] text, final int startPos, final int endPos) {
        this.text = text;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    /**
     * <p>hashCode.</p>
     *
     * @return a int.
     */
    public int hashCode() {
        if (hash == 0) {
            hash = 5381;
            for (int i = startPos; i <= endPos; i++) {
                hash = ((hash << 5) + hash) + (text[i]); /* hash * 33 + c */
            }
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object o) {
        if (!(o instanceof KeyString)) {
            return false;
        }
        final KeyString k = (KeyString) o;
        if (k.endPos - k.startPos != endPos - startPos) {
            return false;
        }
        for (int i = startPos; i <= endPos; i++) {
            if (text[i] != text[k.startPos - startPos + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        String s = "";
        for (int i = startPos; i <= endPos; i++) {
            s = s + text[i];
        }
        return s + "(" + startPos + "-" + endPos + ")";
    }
}