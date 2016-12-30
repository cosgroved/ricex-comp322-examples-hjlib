package module1.unit3.topic2;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import static edu.rice.hj.Module1.*;

/**
 * MatrixMultiplicationMetrics --- Multiplies two square matrices
 * <p>
 * The purpose of this example is to illustrate abstract metrics and Amdahl's Law using the forallNb construct on matrix
 * multiplication.
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class MatrixMultiplicationMetrics {

    private static final int DEFAULT_MATRIX_SIZE = 20;

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final int matrixSize = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MATRIX_SIZE;
        System.out.println("Matrix size: " + matrixSize + " \n");

        HjSystemProperty.abstractMetrics.set(true);
        HjSystemProperty.executionGraph.set(true);
        HjSystemProperty.speedUpGraph.set(true);
        launchHabaneroApp(() -> {

            finish(() -> {
                testMatrixMultiply(matrixSize);
            });

            final HjMetrics actualMetrics = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetrics);
        });
    }

    private static void testMatrixMultiply(final int N) throws SuspendableException {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        forall(0, N - 1, 0, N - 1, (i, j) -> {
            C[i][j] = 0;
            for (int k = 0; k < N; k++) {
                doWork(1);
                C[i][j] += A[i][k] * B[k][j];
            }
        });

        verifyComputation(C, N);
    }

    private static void verifyComputation(final int[][] dataArray, final int N) {

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                final int actual = dataArray[i][j];
                final int expected = i * j * N;
                if (actual != expected) {
                    throw new IllegalStateException("At position [" + i + ", " + j + "] expected " + expected + ", found " + actual);
                }
            }
        }

    }

}