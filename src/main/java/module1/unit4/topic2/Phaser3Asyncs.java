package module1.unit4.topic2;

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
 * <p>Phaser3Asyncs class.</p>
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class Phaser3Asyncs {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) throws SuspendableException {
    	HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {
	        finish(() -> {
	            final HjPhaser ph1 = newPhaser(SIG_WAIT);
	            final HjPhaser ph2 = newPhaser(SIG_WAIT);
	            final HjPhaser ph3 = newPhaser(SIG_WAIT);
	
	            final List<HjPhaserPair> phList1 = Arrays.asList(
	                    ph1.inMode(SIG),
	                    ph2.inMode(WAIT));
	            final List<HjPhaserPair> phList2 = Arrays.asList(
	                    ph2.inMode(SIG),
	                    ph1.inMode(WAIT),
	                    ph3.inMode(WAIT));
	            final List<HjPhaserPair> phList3 = Arrays.asList(
	                    ph3.inMode(SIG),
	                    ph2.inMode(WAIT));
	
	            asyncPhased(phList1, () -> {
	                // Phase 0
	                doWork(1); // A(1)
	                next();
	                // Phase 1
	                doWork(3); // B(1)
	            });
	
	            asyncPhased(phList2, () -> {
	                // Phase 0
	                doWork(2); // A(2)
	                next();
	                // Phase 1
	                doWork(2); // B(2)
	            });
	
	            asyncPhased(phList3, () -> {
	                // Phase 0
	                doWork(3); // A(3)
	                next();
	                // Phase 1
	                doWork(1); // B(3)
	            });
	
	        });
	
	        final HjMetrics actualMetrics = abstractMetrics();
	        AbstractMetricsManager.dumpStatistics(actualMetrics);
        });
    }
}

