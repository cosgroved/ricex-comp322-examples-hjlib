package module1.unit1.topic5;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import java.util.Random;

import static edu.rice.hj.Module1.*;

/**
 * VectorAdd --- Compute the sum of two vectors
 * <p>
 * The purpose of this example is to illustrate abstract metrics and Amdahl's Law
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class VectorAdd {
    /**
     * Constant <code>ERROR_MSG="Incorrect argument for array size (shou"{trunked}</code>
     */
    public static final String ERROR_MSG = "Incorrect argument for array size (should be > 0), assuming n = 25,000,000";
    /**
     * Constant <code>DEFAULT_N=100</code>
     */
    public static final int DEFAULT_N = 100;
    /**
     * Constant <code>DEFAULT_NUM_SEQ=10</code>
     */
    public static final int DEFAULT_NUM_SEQ = 10;

    // Add vectors X and Y and store the result in Z

    /**
     * <p>main.</p>
     *
     * @param argv an array of {@link String} objects.
     */
    public static void main(final String[] argv) {
        // Initialization
        final int n = (argv.length > 0) ? Integer.parseInt(argv[0]) : DEFAULT_N;
        final int seq = (argv.length > 1) ? Integer.parseInt(argv[1]) : DEFAULT_NUM_SEQ;

        final double[] X = new double[n];
        final double[] Y = new double[n];
        final double[] Z = new double[n];
        final Random myRand = new Random(n);
        for (int i = 0; i < n; i++) {
            X[i] = myRand.nextInt(n);
            Y[i] = myRand.nextInt(n);
        }

        HjSystemProperty.abstractMetrics.set(true);
        HjSystemProperty.executionGraph.set(true);
        HjSystemProperty.speedUpGraph.set(true);

        launchHabaneroApp(() -> {

            finish(() -> {
                vectorAdd(X, Y, Z, seq);
            });

            final HjMetrics actualMetrics = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetrics);
        });
    }

    /**
     * <p>vectorAdd.</p>
     *
     * @param X      an array of double.
     * @param Y      an array of double.
     * @param Z      an array of double.
     * @param numSeq a int.
     */
    public static void vectorAdd(final double[] X, final double[] Y, final double[] Z, final int numSeq) throws SuspendableException {
        finish(() -> {
            // Add elements numSeq to X.length-1 in parallel
            for (int i = numSeq; i < X.length; i++) {
                final int ii = i;
                async(() -> {
                    doWork(1);
                    Z[ii] = X[ii] + Y[ii];
                });
            }
            // Add the first numSeq elements sequentially
            for (int i = 0; i < numSeq; i++) {
                doWork(1);
                Z[i] = X[i] + Y[i];
            }
        });
        System.out.printf("vectorAdd completed with Z[0] = %8.1f and Z[%d] = %8.1f\n", Z[0], Z.length - 1, Z[Z.length - 1]);
    }
}
