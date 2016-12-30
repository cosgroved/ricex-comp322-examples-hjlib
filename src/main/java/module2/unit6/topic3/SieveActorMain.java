package module2.unit6.topic3;

import edu.rice.hj.runtime.actors.Actor;
import edu.rice.hj.runtime.config.HjSystemProperty;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.*;

/**
 * <p>SieveActorMain class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SieveActorMain {
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        final int limit = 500_000;
        System.out.println("SieveActorMain.main: limit = " + limit);

		for (int iter = 0; iter < 2; iter++) {
			System.out.printf("Run %d\n", iter);

			for (int w = 1; w <= Runtime.getRuntime().availableProcessors(); w++) {

				final int ww = w;
				HjSystemProperty.numWorkers.set(String.valueOf(w));

				launchHabaneroApp(() -> {
					final long parStartTime = System.nanoTime();

					final SieveActor sieveActor = new SieveActor(2);
					finish(() -> {
						sieveActor.start();
						for (int i = 3; i <= limit; i += 2) {
							sieveActor.send(i);
						}
						sieveActor.send(0);
					});

					final long parExecTime = System.nanoTime() - parStartTime;
					final double execTime = parExecTime / 1e6;

					int numPrimes = 0;
					SieveActor loopActor = sieveActor;
					while (loopActor != null) {
						numPrimes += loopActor.numLocalPrimes();
						loopActor = loopActor.nextActor();
					}
					System.out.printf("  Workers-%d Completed in %9.2f ms with %d primes \n", ww, execTime, numPrimes);
				});
			}
		}
    }

    private static class SieveActor extends Actor<Object> {

        private static final int MAX_LOCAL_PRIMES = 10_000;
        private final int localPrimes[];
        private int numLocalPrimes;
        private SieveActor nextActor;

        SieveActor(final int localPrime) {
            this.localPrimes = new int[MAX_LOCAL_PRIMES];
            this.localPrimes[0] = localPrime;
            this.numLocalPrimes = 1;
            this.nextActor = null;
        }

        public SieveActor nextActor() {
            return nextActor;
        }

        public int numLocalPrimes() {
            return numLocalPrimes;
        }

        @Override
        protected void process(final Object theMsg) {
            final int candidate = (Integer) theMsg;
            if (candidate <= 0) {
                if (nextActor != null) {
                    nextActor.send(theMsg);
                }
                exit();
            } else {
                final boolean locallyPrime = isLocallyPrime(candidate);
                if (locallyPrime) {
                    if (numLocalPrimes < MAX_LOCAL_PRIMES) {
                        localPrimes[numLocalPrimes] = candidate;
                        numLocalPrimes += 1;
                    } else if (nextActor == null) {
                        nextActor = new SieveActor(candidate);
                        nextActor.start();
                    } else {
                        nextActor.send(theMsg);
                    }
                }
            }
        }

        private boolean isLocallyPrime(final int candidate) {
            final boolean[] isPrime = {true};
            checkPrimeKernel(candidate, isPrime, 0, numLocalPrimes);
            return isPrime[0];
        }

        private void checkPrimeKernel(final int candidate, final boolean[] isPrime, final int startIndex, final int endIndex) {
            for (int i = startIndex; i < endIndex; i++) {
                if (candidate % localPrimes[i] == 0) {
                    isPrime[0] = false;
                    break;
                }
            }
        }
    }
}
