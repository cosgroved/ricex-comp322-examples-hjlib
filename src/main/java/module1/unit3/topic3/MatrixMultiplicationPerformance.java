package module1.unit3.topic3;

import edu.rice.hj.api.HjRegion.HjRegion1D;
import edu.rice.hj.api.HjRegion.HjRegion2D;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.region.RectangularRegion2D;

import java.util.stream.IntStream;

import static edu.rice.hj.Module1.*;

/**
 * MatrixMultiplicationPerformance --- Multiplies two square matrices
 * <p>
 * The purpose of this example is to illustrate the performance benefits of grouping while using forallNb.
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class MatrixMultiplicationPerformance {

    private static final int DEFAULT_MATRIX_SIZE = 1024;

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final int matrixSize = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_MATRIX_SIZE;
        System.out.println("Matrix size: " + matrixSize + " \n");

        launchHabaneroApp(() -> {

            for (int r = 0; r < 5; r++) {
                System.out.println("Iteration-" + r);

                doSequentialJavaArrayLoop(matrixSize);
                doJava8Streams1dPar(matrixSize);
                doJava8Streams2dPar(matrixSize);
                doForallChunked1D(matrixSize);
                doForallGrouped1D(matrixSize);
                doForallChunked2D(matrixSize);
                doForallGrouped2D(matrixSize);

                System.out.println();
            }
        });
    }

    private static void doSequentialJavaArrayLoop(final int N) {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                computationKernel(A, B, C, N, i, j); /* k-loop */
            }
        }
        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "seq-java-loop", (e - s) / 1e6);
    }

    private static void doForallChunked1D(final int N) throws SuspendableException {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();

        forallChunked(0, N - 1, (i) -> {
            for (int j = 0; j < N; j++) {
                computationKernel(A, B, C, N, i, j); /* k-loop */
            }
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "forallNb-chunked-1d", (e - s) / 1e6);
    }

    private static void doForallGrouped1D(final int N) throws SuspendableException {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final int tasks = numWorkerThreads();

        final long s = System.nanoTime();

        final HjRegion1D iterSpace = newRectangularRegion1D(0, N - 1);

        forall(0, tasks - 1, (t) -> {
            final HjRegion1D myGroup = myGroup(t, iterSpace, tasks);
            forseqNb(myGroup, (i) -> {
                for (int j = 0; j < N; j++) {
                    computationKernel(A, B, C, N, i, j); /* k-loop */
                }
            });
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "forallNb-grouped-1d", (e - s) / 1e6);
    }

    private static void doForallChunked2D(final int N) throws SuspendableException {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();

        final HjRegion2D hjRegion = newRectangularRegion2D(0, N - 1, 0, N - 1);
        forallChunked(hjRegion, (i, j) -> {
            computationKernel(A, B, C, N, i, j); /* k-loop */
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "forallNb-chunked-2d", (e - s) / 1e6);
    }

    private static void doForallGrouped2D(final int N) throws SuspendableException {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();

        final int tasks = numWorkerThreads();
        final HjRegion2D hjRegion = newRectangularRegion2D(0, N - 1, 0, N - 1);

        final int grid1 = tasks / 2;
        final int grid2 = 2;

        forall(0, tasks - 1, (t) -> {
            final int id1 = t / 2;
            final int id2 = t % 2;
            final RectangularRegion2D myGroup = myGroup(id1, id2, hjRegion, grid1, grid2);

            forseqNb(myGroup, (i, j) -> {
                computationKernel(A, B, C, N, i, j); /* k-loop */
            });
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "forallNb-grouped-2d", (e - s) / 1e6);
    }

    private static void doJava8Streams1dPar(final int N) {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();

        final IntStream iRange = IntStream.range(0, N);
        iRange.parallel().forEach(i -> {
            final IntStream jRange = IntStream.range(0, N);
            jRange.forEach(j -> {
                computationKernel(A, B, C, N, i, j); /* k-loop */
            });
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "java8-streams-1d-par", (e - s) / 1e6);
    }

    private static void doJava8Streams2dPar(final int N) {

        final int[][] A = new int[N][N];
        final int[][] B = new int[N][N];
        final int[][] C = new int[N][N];

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                A[i][j] = i;
                B[i][j] = j;
            }
        }

        final long s = System.nanoTime();

        final IntStream iRange = IntStream.range(0, N);
        iRange.parallel().forEach(i -> {
            final IntStream jRange = IntStream.range(0, N);
            jRange.parallel().forEach(j -> {
                computationKernel(A, B, C, N, i, j); /* k-loop */
            });
        });

        final long e = System.nanoTime();

        verifyComputation(C, N);
        System.out.printf("  %-25s Time: %10.3f ms.\n", "java8-streams-2d-par", (e - s) / 1e6);
    }

    private static void computationKernel(
            final int[][] A, final int[][] B, final int[][] C,
            final int N, final int i, final int j) {

        int sum = 0;
        for (int k = 0; k < N; k++) {
            sum += A[i][k] * B[k][j];
        }
        C[i][j] = sum;
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