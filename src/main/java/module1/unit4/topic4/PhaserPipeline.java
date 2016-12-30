package module1.unit4.topic4;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.api.HjPhaser;
import edu.rice.hj.api.HjPhaserMode;
import edu.rice.hj.api.HjPhaserPair;
import edu.rice.hj.api.SuspendableException;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import java.util.Arrays;
import java.util.List;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.*;
import static edu.rice.hj.api.HjPhaserMode.SIG;
import static edu.rice.hj.api.HjPhaserMode.SIG_WAIT;
import static edu.rice.hj.api.HjPhaserMode.WAIT;

/**
 * <p>PhaserPipeline class.</p>
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class PhaserPipeline {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) throws SuspendableException {
       	HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {
	
	        final int n = 100; // Number of items processed by 3-stage image processing pipeline
	
	        finish(() -> {
	            final HjPhaser ph0 = newPhaser(SIG_WAIT);
	            final HjPhaser ph1 = newPhaser(SIG_WAIT);
	
	            final List<HjPhaserPair> phList1 = Arrays.asList(ph0.inMode(SIG));
	            final List<HjPhaserPair> phList2 = Arrays.asList(ph0.inMode(WAIT), ph1.inMode(SIG));
	            final List<HjPhaserPair> phList3 = Arrays.asList(ph1.inMode(WAIT));
	
	            asyncPhased(phList1, () -> { // DENOISE stage
	                for (int i = 0; i < n; i++) {
	                    doWork(1);
	                    signal(); // same as ph0.signal(); as only ph0 is registered in this async
	                }
	            });
	
	            asyncPhased(phList2, () -> { // REGISTER stage
	                for (int i = 0; i < n; i++) {
	                    ph0.doWait();
	                    doWork(1);
	                    ph1.signal();
	                }
	            });
	
	            asyncPhased(phList3, () -> { // SEGMENT stage
	                for (int i = 0; i < n; i++) {
	                    ph1.doWait();
	                    doWork(1);
	                }
	            });
	
	        });
	
	        final HjMetrics actualMetrics = abstractMetrics();
	        AbstractMetricsManager.dumpStatistics(actualMetrics);

        });
    }
}

