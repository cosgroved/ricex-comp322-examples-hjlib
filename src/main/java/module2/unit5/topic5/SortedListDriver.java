package module2.unit5.topic5;
import edu.rice.hj.runtime.config.HjSystemProperty;

import static edu.rice.hj.Module1.finalizeHabanero;
import static edu.rice.hj.Module1.initializeHabanero;

/**
 * <p>SortedListDriver class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SortedListDriver {
    static final int DEFAULT_THREADS = 8;
    static final int DEFAULT_MILLIS = 10_000; // run for 10 seconds
    static final int DEFAULT_READ_PCT = 98;
    static final int DEFAULT_ADD_PCT = 1;
    static final int DEFAULT_DEL_PCT = 1;
    static final int DEFAULT_LOAD = 0;

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String args[]) {
        int numThreads = DEFAULT_THREADS;
        int numMillis = DEFAULT_MILLIS;
        int readPct = DEFAULT_READ_PCT;
        int addPct = DEFAULT_ADD_PCT;
        int delPct = DEFAULT_DEL_PCT;
        int load = DEFAULT_LOAD;

        String sClassName = "";
        Class benchmarkClass = null;

        // Parse and check the args
        int argc = 0;
        try {
            while (argc < args.length) {
                String option = args[argc++];
                if (option.equals("-t")) {
                    numThreads = Integer.parseInt(args[argc]);
                } else if (option.equals("-n")) {
                    numMillis = Integer.parseInt(args[argc]);
                } else if (option.equals("-b")) {
                    final String className = args[argc];
                    benchmarkClass = Class.forName(className);
                } else if (option.equals("-s")) {
                    sClassName = args[argc];
                } else if (option.equals("-r")) {
                    readPct = Integer.parseInt(args[argc]);
                } else if (option.equals("-a")) {
                    addPct = Integer.parseInt(args[argc]);
                } else if (option.equals("-d")) {
                    delPct = Integer.parseInt(args[argc]);
                } else if (option.equals("-l")) {
                    load = Integer.parseInt(args[argc]);
                } else if (option.startsWith("-D")) {
                    final String key = option.substring(2);
                    final String value = args[argc];
                    System.out.println("Setting system property: " + key + "=" + value);
                    System.setProperty(key, value);
                } else {
                    reportUsageErrorAndDie();
                }
                argc++;
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find class " + args[argc]);
            System.exit(0);
        } catch (NumberFormatException e) {
            System.out.println("Expected a number: " + args[argc]);
            System.exit(0);
        } catch (Exception e) {
            reportUsageErrorAndDie();
        }

        SortedListReadWriteMix mix = null;
        SBenchmark bm = null;
        try {
            bm = (SBenchmark) benchmarkClass.newInstance();
            mix = new SortedListReadWriteMix();
            mix.set(readPct, addPct, delPct);
        } catch (ClassCastException e) {
            System.out.println("Class " + benchmarkClass
                    + " doesn't implement SBenchmark.");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.exit(0);
        }
        if (benchmarkClass == null || !mix.validate()) {
            reportUsageErrorAndDie();
        }

        SortedListCounter[] total = new SortedListCounter[numThreads];
        Thread[] thread = new Thread[numThreads];
        SortedListCounter loadCtr = new SortedListCounter();
        Thread[] loadThread = new Thread[load];
        System.out.print("  SortedListDriver -t " + numThreads);
        System.out.print(" -n " + numMillis);
        System.out.print(" -s " + sClassName);
        System.out.print(" -r " + readPct);
        System.out.print(" -a " + addPct);
        System.out.print(" -d " + delPct);
        System.out.println(" -l " + load);

        long startTime = 0;
        try {
        	HjSystemProperty.numWorkers.set("1");
            initializeHabanero();

            // Create test threads
            for (int i = 0; i < numThreads; i++) {
                total[i] = new SortedListCounter();
                thread[i] = bm.createTestThread(sClassName, total[i], mix);
            }

            // Create load threads
            for (int i = 0; i < load; i++) {
                loadThread[i] = bm.createLoadThread(loadCtr);
            }

            // Initialize test environment
            bm.initializeTestEnvironment(128);

            // Run the test
            startTime = System.currentTimeMillis();
            for (int i = 0; i < numThreads; i++) {
                thread[i].start();
            }
            for (int i = 0; i < load; i++) {
                loadThread[i].start();
            }

            // Wait for test to finish
            Thread.sleep(numMillis);

            // Stop all threads
            for (int i = 0; i < numThreads; i++) {
                total[i].stopNow();
            }
            loadCtr.stopNow();

            for (int i = 0; i < numThreads; i++) {
                thread[i].interrupt();
                thread[i].join(10000);
            }
            for (int i = 0; i < load; i++) {
                loadThread[i].interrupt();
                loadThread[i].join(10000);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        final long stopTime = System.currentTimeMillis();
        finalizeHabanero();

        final double elapsed = (double) (stopTime - startTime) / 1000.0;

        int reads = 0;
        int adds = 0;
        int deletes = 0;
        int successes = 0;
        int failures = 0;

        for (int i = 0; i < numThreads; i++) {
            reads += total[i].getReads();
            adds += total[i].getAdds();
            deletes += total[i].getDeletes();
            successes += total[i].getSuccesses();
            failures += total[i].getFailures();
        }

        System.out.printf("   %22s: %,12d \n", "Operations per second", (reads + adds + deletes) / numMillis * 1000);
        System.out.printf("   %22s: %,12d \n", "Reads", reads);
        System.out.printf("   %22s: %,12d \n", "Adds", adds);
        System.out.printf("   %22s: %,12d \n", "Deletes", deletes);
        System.out.printf("   %22s: %,12d \n", "Successes", successes);
        System.out.printf("   %22s: %,12d \n", "Validation Failures", failures);
        System.out.printf("   %22s: %12.3f seconds.\n", "Elapsed time", elapsed);
        System.out.println("  ----------------------------------------");
    }

    private static void reportUsageErrorAndDie() {
        System.out.print("usage: SortedListDriver [-t #threads] [-n #test-time-in-ms]");
        System.out.println(" [-s set class] [-r read%] [-a add%] [-d del%]");
        System.out.println("             [-l #num load threads]");
        System.exit(0);
    }

    public interface SBenchmark {
        public Thread createTestThread(
                String sClassName, SortedListCounter ctr, SortedListReadWriteMix mix);

        public Thread createLoadThread(SortedListCounter ctr);

        public void initializeTestEnvironment(int n);
    }
}
