package module1.unit4.topic3;

import edu.rice.hj.api.*;
import edu.rice.hj.api.HjRegion.HjRegion1D;

import java.util.Arrays;
import java.util.List;

import static edu.rice.hj.Module1.*;

/**
 * <p>OneDimAveraging class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class OneDimAveraging {

    /**
     * Constant <code>initialOutput</code>
     */
    public static double[] initialOutput;
    public double[] myNew, myVal;
    public int n;

    /**
     * <p>Constructor for OneDimAveraging.</p>
     *
     * @param n a int.
     */
    public OneDimAveraging(final int n) {
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

        final int tasks = (args.length > 0) ? Integer.parseInt(args[0]) : 4;
        final int n = (args.length > 1) ? Integer.parseInt(args[1]) : 200000;
        final int iterations = (args.length > 2) ? Integer.parseInt(args[2]) : 20000;
        final int rounds = (args.length > 3) ? Integer.parseInt(args[3]) : 5;
        printParams(tasks, n, iterations, rounds);

        {
            // initial run to set the output to be equal to the sequential run
            final OneDimAveraging initialObj = new OneDimAveraging(n);
            initialObj.runSequential(iterations);
            setOutput(initialObj);
        }

        System.out.println("Timed executions:");
        launchHabaneroApp(() -> {
            for (int r = 0; r < rounds; r++) {
                final OneDimAveraging serialBody = new OneDimAveraging(n);
                final OneDimAveraging forkJoinBody = new OneDimAveraging(n);
                final OneDimAveraging barrierBody = new OneDimAveraging(n);
                final OneDimAveraging peer2PeerBody = new OneDimAveraging(n);
                final OneDimAveraging chunkedForkJoinBody = new OneDimAveraging(n);
                final OneDimAveraging chunkedBarrierBody = new OneDimAveraging(n);
                final OneDimAveraging chunkedBarrierSingleBody = new OneDimAveraging(n);
                final OneDimAveraging chunkedPhaserBody = new OneDimAveraging(n);

                System.out.println(" Round: " + r + (r == 0 ? " [ignore: warm up for JIT]" : ""));

                timeIt("Sequential", () -> {
                    serialBody.runSequential(iterations);
                }, () -> {
                    serialBody.validateOutput();
                });

                timeIt(String.format("Fork-Join [tasks=%d]", tasks), () -> {
                    forkJoinBody.runForkJoin(iterations, tasks);
                }, () -> {
                    forkJoinBody.validateOutput();
                });

                timeIt(String.format("Barrier [tasks=%d]", tasks), () -> {
                    barrierBody.runBarrier(iterations, tasks);
                }, () -> {
                    barrierBody.validateOutput();
                });

                timeIt(String.format("Peer-To-Peer [tasks=%d]", tasks), () -> {
                    peer2PeerBody.runPeerToPeer(iterations, tasks);
                }, () -> {
                    peer2PeerBody.validateOutput();
                });

                timeIt(String.format("Chunked-ForkJoin [tasks=%d]", tasks), () -> {
                    chunkedForkJoinBody.runChunkedForkJoin(iterations, tasks);
                }, () -> {
                    chunkedForkJoinBody.validateOutput();
                });

                timeIt(String.format("Chunked-Barrier [tasks=%d]", tasks), () -> {
                    chunkedBarrierBody.runChunkedBarrier(iterations, tasks);
                }, () -> {
                    chunkedBarrierBody.validateOutput();
                });

                timeIt(String.format("Chunked-BarrierSingle [tasks=%d]", tasks), () -> {
                    chunkedBarrierSingleBody.runChunkedBarrierSingle(iterations, tasks);
                }, () -> {
                    chunkedBarrierSingleBody.validateOutput();
                });

                timeIt(String.format("Chunked-Phaser [tasks=%d]", tasks), () -> {
                    chunkedPhaserBody.runChunkedPhaser(iterations, tasks);
                }, () -> {
                    chunkedPhaserBody.validateOutput();
                });

                System.out.println();
            }
        });
    }

    /**
     * <p>setOutput.</p>
     *
     * @param oneDimAveraging a {@link edu.rice.hj.example.comp322.OneDimAveraging} object.
     */
    public static void setOutput(final OneDimAveraging oneDimAveraging) {
        if (OneDimAveraging.initialOutput != null) {
            System.out.println("Warning: initialOutput has already been set.");
        }
        OneDimAveraging.initialOutput = oneDimAveraging.myVal;
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

    private static void timeIt(
            final String label,
            final HjSuspendable actualBody,
            final Runnable postExecBody) throws SuspendableException {

        final long s = System.currentTimeMillis();
        actualBody.run();
        final long e = System.currentTimeMillis();

        postExecBody.run();
        System.out.printf("%35s Time: %6d ms. \n", label, (e - s));
    }

    private static int ceilDiv(final int n, final int d) {
        final int m = n / d;
        if (m * d == n) {
            return m;
        } else {
            return (m + 1);
        }
    }

    /*
   * Helper method to return region for chunk ii, when chunking input region r into c chunks
   */
    static int[] getChunk(final int r0, final int r1, final int c, final int ii) {
        // Assume that r is a 1D region
        final int rLo = r0;
        final int rHi = r1;
        if (rLo > rHi) {
            return new int[]{0, -1}; // Empty region
        }
        assert (c > 0); // tasks must be > 0
        assert (0 <= ii && ii < c); // ii must be in [0:c-1]
        final int chunkSize = ceilDiv(rHi - rLo + 1, c);
        final int myLo = rLo + ii * chunkSize;
        final int myHi = Math.min(rHi, rLo + (ii + 1) * chunkSize - 1);
        return new int[]{myLo, myHi};
    }

    /**
     * <p>validateOutput.</p>
     */
    public void validateOutput() {
        if (OneDimAveraging.initialOutput == null) {
            System.out.println("initialOutput is null");
            return;
        } else if (myVal == null) {
            System.out.println("myVal is null");
            return;
        }

        for (int i = 0; i < n + 2; i++) {
            final double init = OneDimAveraging.initialOutput[i];
            final double curr = myVal[i];
            final double diff = Math.abs(init - curr);
            if (diff > 1e-20) {
                System.out.println("ERROR: validation failed!");
                System.out.println("  Diff: myVal[" + i + "]=" + curr + " != initialOutput[" + i + "]=" + init);
                break;
            }
        }
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
     * <p>runForkJoin.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runForkJoin(final int iterations, final int tasks) throws SuspendableException {
        final int batchSize = ceilDiv(n, tasks);

        for (int iter = 0; iter < iterations; iter++) {
            forall(0, tasks - 1, (i) -> {

                final int start = i * batchSize + 1;
                final int end = Math.min(start + batchSize - 1, n);

                for (int j = start; j <= end; j++) {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                }
            });
            final double[] temp = myNew;
            myNew = myVal;
            myVal = temp;
        }
    }

    /**
     * <p>runBarrier.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runBarrier(final int iterations, final int tasks) throws SuspendableException {

        final HjRegion1D iterSpace = newRectangularRegion1D(1, n);

        forallPhased(0, tasks - 1, (i) -> {

            final HjRegion1D myGroup = myGroup(i, iterSpace, tasks);

            for (int iter = 0; iter < iterations; iter++) {

                forseqNb(myGroup, (j) -> {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                });

                next();
                if (i == 0) {
                    double[] temp = myNew;
                    myNew = myVal;
                    myVal = temp;
                }
                next();
            }
        });
    }

    /**
     * <p>runPeerToPeer.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runPeerToPeer(final int iterations, final int tasks) throws SuspendableException {

        finish(() -> {
            final HjPhaser[] ph = new HjPhaser[tasks + 2]; //array of phasers
            for (int i = 0; i < ph.length; i++) {
                ph[i] = newPhaser(HjPhaserMode.DEFAULT_MODE);
            }

            final HjRegion1D iterSpace = newRectangularRegion1D(1, n);

            for (int i = 1; i <= tasks; i++) {

                final HjRegion1D myGroup = myGroup(i - 1, iterSpace, tasks);

                final List<HjPhaserPair> phList = Arrays.asList(
                        ph[i + 0].inMode(HjPhaserMode.SIG),
                        ph[i - 1].inMode(HjPhaserMode.WAIT),
                        ph[i + 1].inMode(HjPhaserMode.WAIT)
                );
                asyncPhased(phList, () -> {

                    double[] lMyVal = myVal;
                    double[] lMyNew = myNew;

                    for (int iter = 0; iter < iterations; iter++) {

                        final double[] constMyVal = lMyVal;
                        final double[] constMyNew = lMyNew;
                        forseqNb(myGroup, (j) -> {
                            constMyNew[j] = (constMyVal[j - 1] + constMyVal[j + 1]) / 2.0;
                        });

                        double[] temp = lMyNew;
                        lMyNew = lMyVal;
                        lMyVal = temp;
                        next();    // Signals ph[i] and Await signals for ph[i-1] and ph[i+1]
                    }
                });
            }
        });
    }

    // Fork-join algorithm for 1-D Iterative Averaging

    /**
     * <p>runChunkedForkJoin.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runChunkedForkJoin(final int iterations, final int tasks) throws SuspendableException {
        for (int iter = 0; iter < iterations; iter++) {
            forallChunked(0, tasks - 1, (jj) -> {
                final int[] myChunk = getChunk(1, n, tasks, jj);
                for (int j = myChunk[0]; j <= myChunk[1]; j++) {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                }
            });
            final double[] temp = myNew;
            myNew = myVal;
            myVal = temp;
        } // for
    }

    // Barrier-based algorithm for 1-D Iterative Averaging using local variables, myVar and myNew

    /**
     * <p>runChunkedBarrier.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runChunkedBarrier(final int iterations, final int tasks) throws SuspendableException {
        forallPhased(0, tasks - 1, (jj) -> {
            double[] myVal = this.myVal;
            double[] myNew = this.myNew;
            for (int iter = 0; iter < iterations; iter++) {
                final int[] myChunk = getChunk(1, n, tasks, jj);
                for (int j = myChunk[0]; j <= myChunk[1]; j++) {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                }
                next();
                final double[] temp = myNew;
                myNew = myVal;
                myVal = temp;
            } // for
        }); // forallNb
    }

    // Barrier-based algorithm for 1-D Iterative Averaging using next single statement to coordinate swap of shared fields, myVar and myNew

    /**
     * <p>runChunkedBarrierSingle.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runChunkedBarrierSingle(final int iterations, final int tasks) throws SuspendableException {
        forallPhased(0, tasks - 1, (jj) -> {
            for (int iter = 0; iter < iterations; iter++) {
                final int[] myChunk = getChunk(1, n, tasks, jj);
                for (int j = myChunk[0]; j <= myChunk[1]; j++) {
                    myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                }
                next();
                if (jj == 0) {
                    final double[] temp = myNew;
                    myNew = myVal;
                    myVal = temp;
                }
                next();
            } // for
        });
    }

    // Phaser-based algorithm for 1-D Iterative Averaging with point-to-point synchronization and local variables, myVar and myNew

    /**
     * <p>runChunkedPhaser.</p>
     *
     * @param iterations a int.
     * @param tasks      a int.
     */
    public void runChunkedPhaser(final int iterations, final int tasks) throws SuspendableException {
        finish(() -> {
            final HjPhaser[] ph = new HjPhaser[tasks + 2]; //array of phasers
            for (int j = 0; j <= tasks + 1; j++) {
                ph[j] = newPhaser(HjPhaserMode.DEFAULT_MODE);
            }
            for (int i = 0; i < tasks; i++) {
                final int jj = i;
                final List<HjPhaserPair> phList = Arrays.asList(
                        ph[jj + 0].inMode(HjPhaserMode.WAIT),
                        ph[jj + 1].inMode(HjPhaserMode.SIG),
                        ph[jj + 2].inMode(HjPhaserMode.WAIT)
                );
                asyncPhased(phList, () -> {
                    double[] myVal = this.myVal;
                    double[] myNew = this.myNew;

                    for (int iter = 0; iter < iterations; iter++) {
                        final int[] myChunk = getChunk(1, n, tasks, jj);
                        for (int j = myChunk[0]; j <= myChunk[1]; j++) {
                            myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
                        }

                        next();

                        double[] temp = myNew;
                        myNew = myVal;
                        myVal = temp;
                    }
                });
            }
        }); // finish
    }

}