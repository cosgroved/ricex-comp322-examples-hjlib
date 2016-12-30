package module1.unit4.topic1;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.api.HjPhaser;
import edu.rice.hj.api.HjPhaserMode;
import edu.rice.hj.api.HjPhaserPair;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import java.util.Arrays;
import java.util.List;

import static edu.rice.hj.Module1.*;

/**
 * <p>PhaserSignal class.</p>
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class PhaserSignal {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {
        HjSystemProperty.abstractMetrics.set(true);

        System.out.println("\n\n Standard Barrier:");
        runStandardBarrierVersion();

        System.out.println("\n\n Fuzzy Barrier:");
		runFuzzyBarrierVersion();
    }

    protected static void runStandardBarrierVersion() {
    	launchHabaneroApp(()->{
	        finish(() -> {
	            final HjPhaser ph = newPhaser(HjPhaserMode.SIG_WAIT);
	            // Initialize phList = singleton HjPhaserPair list containing (ph, SIG_WAIT)
	            final List<HjPhaserPair> phList = Arrays.asList(ph.inMode(HjPhaserMode.SIG_WAIT));
	
	            forasyncPhased(0, 1, phList, (i) -> {
	                // Phase 0
	                doWork(1); // A(i)
	                if (i == 0) {
	                    doWork(100);
	                } // B(i)
	                next();
	                // Phase 1
	                doWork(1); // C(i)
	                if (i == 1) {
	                    doWork(100);
	                } // D(i)
	            });
	
	        });
	        final HjMetrics actualMetrics = abstractMetrics();
	        AbstractMetricsManager.dumpStatistics(actualMetrics);
	   });
    }

    protected static void runFuzzyBarrierVersion() {
    	launchHabaneroApp(()->{
		    finish(() -> {
		        final HjPhaser ph = newPhaser(HjPhaserMode.SIG_WAIT);
		        final List<HjPhaserPair> phList = Arrays.asList(ph.inMode(HjPhaserMode.SIG_WAIT));
		
		        forasyncPhased(0, 1, phList, (i) -> {
		            // Phase 0
		            doWork(1); // A(i)
		            if (i == 0) {
		                ph.signal();
		                doWork(100);
		            } // B(i)
		            next();
		            // Phase 1
		            doWork(1); // C(i)
		            if (i == 1) {
		                doWork(100);
		            } // D(i)
		        });
		
		    });
	        final HjMetrics actualMetrics = abstractMetrics();
	        AbstractMetricsManager.dumpStatistics(actualMetrics);
        });
    }
}

