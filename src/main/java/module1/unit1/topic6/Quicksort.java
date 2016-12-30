package module1.unit1.topic6;

import edu.rice.hj.api.HjPoint;
import edu.rice.hj.api.SuspendableException;

import java.util.Random;

import static edu.rice.hj.Module1.*;

/**
 * Parallel Quicksort program.
 * partition() has not been parallelized.
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class Quicksort {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {
        final int size = args.length > 0 ? Integer.parseInt(args[0]) : 20_000_000;
        final int threshold = args.length > 1 ? Integer.parseInt(args[1]) : 1000;

        launchHabaneroApp(() -> {
            for (int numRun = 0; numRun < 5; numRun++) {
                System.out.printf("Run %d\n", numRun);

                Random rand = new Random(0);
                final int[] A = new int[size];

                for (int i = 0; i < size; i++) {
                    A[i] = rand.nextInt();
                }

                executeProgram(size, threshold, A);
            }
        });
    }

    private static void executeProgram(final int size, final int threshold, final int[] A) throws SuspendableException {
        // For sequential sort, set threshold = size
        final long seqStartTime = System.nanoTime();

        finish(() -> {
            quicksort(A, 0, size - 1, size);
        });

        final long seqExecTime = System.nanoTime() - seqStartTime;

        if (valid(A)) {
            System.out.println("Sequential sort completed successfully in " + seqExecTime / 1e6 + " milliseconds");
        } else {
            System.out.println("Sequential sort failed\n");
        }

        // For parallel sort, use default or user-specified threshold
        final long parStartTime = System.nanoTime();

        finish(() -> {
            quicksort(A, 0, size - 1, threshold);
        });

        final long parExecTime = System.nanoTime() - parStartTime;

        if (valid(A)) {
            System.out.println("Parallel sort completed successfully in " + parExecTime / 1e6 + " milliseconds");
        } else {
            System.out.println("Parallel sort failed\n");
        }
    }

    private static void quicksort(final int[] A, final int M, final int N, final int threshold) {
        if (M < N) {
            // We use an HJ point to serve as a box for two integers
            final HjPoint p = partition(A, M, N);
            final int I = p.get(0);
            final int J = p.get(1);

            if (N - M + 1 > threshold) {
                // parallel version
                async(() -> quicksort(A, M, J, threshold));
                async(() -> quicksort(A, I, N, threshold));
            } else {
                // sequential version
                quicksort(A, M, J, threshold);
                quicksort(A, I, N, threshold);
            }

        }
    }

    private static HjPoint partition(final int[] A, final int M, final int N) {
        int I;
        int storeIndex = M;

        final Random rand = new Random();
        final int pivot = M + rand.nextInt(N - M + 1);
        final int pivotValue = A[pivot];
        exchange(A, pivot, N);

        for (I = M; I < N; I++) {
            // Only count comparison with pivot value in abstract execution metrics
            if (A[I] <= pivotValue) {
                exchange(A, I, storeIndex);
                storeIndex++;
            }
        }

        exchange(A, storeIndex, N);

        if (storeIndex == N) {
            return newPoint(N, storeIndex - 1);
        } else if (storeIndex == M) {
            return newPoint(storeIndex + 1, M);
        }
        return newPoint(storeIndex + 1, storeIndex - 1);
    }

    private static void exchange(final int[] A, final int x, final int y) {
        int temp = A[x];
        A[x] = A[y];
        A[y] = temp;
    }

    private static boolean valid(final int[] A) {
        for (int i = 1; i < A.length; i++) {
            if (A[i] < A[i - 1]) {
                return false;
            }
        }
        return true;
    }
}
