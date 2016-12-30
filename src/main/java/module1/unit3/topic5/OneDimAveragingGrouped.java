package module1.unit3.topic5;

import edu.rice.hj.api.HjRegion;
import edu.rice.hj.api.HjSuspendable;
import edu.rice.hj.api.SuspendableException;

import static edu.rice.hj.Module1.*;

/**
 * <p>OneDimAveragingGrouped class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class OneDimAveragingGrouped {

    /**
     * Constant <code>initialOutput</code>
     */
    public static double[] initialOutput;
    public double[] myNew, myVal;
    public int n;

    /**
     * <p>Constructor for OneDimAveragingGrouped.</p>
     *
     * @param n a int.
     */
    public OneDimAveragingGrouped(final int n) {
        this.n = n;
        this.myNew = new double[n + 2];
        this.myVal = new double[n + 2];
        this.myVal[n + 1] = 1.0;
    }

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        final int tasks = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        final int n = (args.length > 1) ? Integer.parseInt(args[1]) : 200000;
        final int iterations = (args.length > 2) ? Integer.parseInt(args[2]) : 20000;
        final int rounds = (args.length > 3) ? Integer.parseInt(args[3]) : 5;
        printParams(tasks, n, iterations, rounds);

        {
            // initial run to set the output to be equal to the sequential run
            final OneDimAveragingGrouped initialObj = new OneDimAveragingGrouped(n);
            initialObj.runSequential(iterations);
            setOutput(initialObj);
        }

        System.out.println("Timed executions:");
        launchHabaneroApp(() -> {
            for (int r = 0; r < rounds; r++) {
                final OneDimAveragingGrouped serialBody = new OneDimAveragingGrouped(n);
                final OneDimAveragingGrouped forallBody = new OneDimAveragingGrouped(n);
                final OneDimAveragingGrouped forallBodyGrouped = new OneDimAveragingGrouped(n);

                System.out.println(" Round: " + r + (r == 0 ? " [ignore: warm up for JIT]" : ""));

                timeIt("Sequential", () -> {
                    serialBody.runSequential(iterations);
                }, () -> {
                    serialBody.validateOutput();
                });

                timeIt(String.format("Forall-grouped [tasks=%d]", tasks), () -> {
                    forallBodyGrouped.runForallGrouped(iterations, tasks);
                }, () -> {
                    forallBodyGrouped.validateOutput();
                });

                System.out.println();
            }
        });
    }

    /**
     * <p>printParams.</p>
     *
     * @param tasks      a int.
     * @param n          a int.
     * @param iterations a int.
     * @param rounds     a int.
     */
    public static void printParams(final int tasks, final int n, final int iterations, final int rounds) {
        System.out.println("Configuration: ");
        System.out.println("  # tasks for parallel run: " + tasks);
        System.out.println("  Array size n: " + n);
        System.out.println("  # iterations: " + iterations);
        System.out.println("  Rounds: " + rounds + " (to reduce JIT overhead)");
        System.out.println();
    }

    /**
     * <p>runSequential.</p>
     *
     * @param iterations a int.
     */
    public void runSequential(final int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            for (int j = 1; j <= n; j++) {
                myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
            }
            final double[] temp = myNew;
            myNew = myVal;
            myVal = temp;
        }
    }

    /**
     * <p>setOutput.</p>
     *
     * @param oneDimAveraging a {@link edu.rice.hj.example.comp322.OneDimAveragingGrouped} object.
     */
    public static void setOutput(final OneDimAveragingGrouped oneDimAveraging) {
        if (OneDimAveragingGrouped.initialOutput != null) {
            System.out.println("Warning: initialOutput has already been set.");
        }
        OneDimAveragingGrouped.initialOutput = oneDimAveraging.myVal;
    }

    private static void timeIt(
            final String label,
            final HjSuspendable actualBody,
            final Runnable postExecBody) throws SuspendableException {

        final long s = System.currentTimeMillis();
        actualBody.run();
        final long e = System.currentTimeMillis();

        postExecBody.run();
        System.out.printf("%25s Time: %6d ms. \n", label, (e - s));
    }

    /**
     * <p>validateOutput.</p>
     */
    public void validateOutput() {
        if (OneDimAveragingGrouped.initialOutput == null) {
            System.out.println("initialOutput is null");
            return;
        } else if (myVal == null) {
            System.out.println("myVal is null");
            return;
        }

        for (int i = 0; i < n + 2; i++) {
            final double init = OneDimAveragingGrouped.initialOutput[i];
            final double curr = myVal[i];
            if (init != curr) {
                System.out.println("Diff: myVal[" + i + "]=" + curr + " != initialOutput[" + i + "]=" + init);
            }
        }
    }

    /**
     * <p>runForallGrouped.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runForallGrouped(final int iterations, final int tasks) throws SuspendableException {
        final HjRegion.HjRegion1D iterSpace = newRectangularRegion1D(1, n);

        for (int iter = 0; iter < iterations; iter++) {
            forall(0, tasks - 1, (i) -> {
                final HjRegion.HjRegion1D myGroup = myGroup(i, iterSpace, tasks);
                forseqNb(myGroup, (j) -> {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                });
            });
            final double[] temp = myNew;
            myNew = myVal;
            myVal = temp;
        }
    }

    private int ceilDiv(final int n, final int d) {
        final int m = n / d;
        if (m * d == n) {
            return m;
        } else {
            return (m + 1);
        }
    }

    /**
     * <p>runForall.</p>
     *
     * @param iterations a int.
     */
    public void runForall(final int iterations) throws SuspendableException {
        for (int iter = 0; iter < iterations; iter++) {
            forall(1, n, (j) -> {
                myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
            });
            final double[] temp = myNew;
            myNew = myVal;
            myVal = temp;
        }
    }
}