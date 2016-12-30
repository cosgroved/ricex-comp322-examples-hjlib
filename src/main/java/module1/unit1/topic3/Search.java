package module1.unit1.topic3;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import static edu.rice.hj.Module1.*;

/**
 * Reads in two strings, the pattern and the input text, and searches for the pattern in the input text.
 * <p>
 * % hj Search rabrabracad abacadabrabracabracadabrabrabracad text:    abacadabrabracabracadababacadabrabracabracadabrabrabracad
 * pattern:                                               rabrabracad
 * <p>
 * HJ version ported from Java version in http://algs4.cs.princeton.edu/53substring/Brute.java.html
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class Search {
    private static final String default_pat = "rabrabracad";
    private static final String default_txt = "abacadabrabracabracadababacadabrabracabracadabrabrabracad";

    // return number of occurrences of pattern in text

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final String pat = args.length >= 1 ? args[0] : default_pat;
        final String txt = args.length >= 2 ? args[1] : default_txt;

        final char[] pattern = pat.toCharArray();
        final char[] text = txt.toCharArray();

        System.out.println("text:    " + txt);
        System.out.println("pattern: " + pat);

        HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {
            finish(() -> {
                final boolean seqFound = searchSeq(pattern, text);
                System.out.println("Pattern found by sequential algorithm: " + seqFound);
            });
            final HjMetrics actualMetricsSeq = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetricsSeq);
        });

        HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {
            finish(() -> {
                final boolean parFound = searchPar(pattern, text);
                System.out.println("Pattern found by parallel algorithm: " + parFound);
            });
            final HjMetrics actualMetricsPar = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetricsPar);
        });
    }

    // return number of occurrences of pattern in text

    /**
     * <p>searchSeq.</p>
     *
     * @param pattern an array of char.
     * @param text    an array of char.
     * @return a boolean.
     */
    public static boolean searchSeq(final char[] pattern, final char[] text) {
        final int M = pattern.length;
        final int N = text.length;
        final boolean[] found = {false};

        for (int i = 0; i <= N - M; i++) {
            int j;
            for (j = 0; j < M; j++) {
                doWork(1); // Count each char comparison as 1 unit of work
                if (text[i + j] != pattern[j]) {
                    break;
                }
            }
            if (j == M) {
                found[0] = true;
            }
        }
        return found[0];
    }

    // test client

    /**
     * <p>searchPar.</p>
     *
     * @param pattern an array of char.
     * @param text    an array of char.
     * @return a boolean.
     */
    public static boolean searchPar(final char[] pattern, final char[] text) throws SuspendableException {
        final int M = pattern.length;
        final int N = text.length;
        final boolean[] found = {false};
        finish(() -> {
            for (int i = 0; i <= N - M; i++) {
                final int ii = i;
                async(() -> {
                    int j;
                    for (j = 0; j < M; j++) {
                        doWork(1); // Count each char comparison as 1 unit of work
                        if (text[ii + j] != pattern[j]) {
                            break;
                        }
                    }
                    if (j == M) {
                        found[0] = true;
                    }
                });
            }
        });
        return found[0];
    }
}