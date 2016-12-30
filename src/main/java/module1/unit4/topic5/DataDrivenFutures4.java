package module1.unit4.topic5;

import edu.rice.hj.api.HjFuture;
import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;
import edu.rice.hj.runtime.metrics.HjMetricsImpl;

import static edu.rice.hj.Module1.*;

/**
 * Example to verify use of metrics with DDFs.
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class DataDrivenFutures4 {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {

            finish(() -> {
                final HjFuture<Integer> A = futureNb(() -> {
                    doWork(1);
                    return 1;
                });
                final HjFuture<Integer> B = futureNbAwait(A, () -> {
                    doWork(1);
                    return 1 + A.safeGet();
                });
                final HjFuture<Integer> C = futureNbAwait(A, () -> {
                    doWork(1);
                    return 1 + A.safeGet();
                });
                final HjFuture<Integer> D = futureNbAwait(B, C, () -> {
                    doWork(1);
                    return 1 + Math.max(B.safeGet(), C.safeGet());
                });
                final HjFuture<Integer> E = futureNbAwait(C, () -> {
                    doWork(1);
                    return 1 + C.safeGet();
                });
                final HjFuture<Integer> F = futureNbAwait(D, E, () -> {
                    doWork(1);
                    final int res = 1 + Math.max(D.safeGet(), E.safeGet());
                    System.out.println("Res = " + res);
                    return res;
                });
            });
        }, () -> {
            final HjMetrics actualMetrics = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetrics);

            final HjMetrics expectedMetrics = new HjMetricsImpl(6, 4, 1.5);
            if (!expectedMetrics.equals(actualMetrics)) {
                throw new IllegalStateException("Expected: " + expectedMetrics + ", found: " + actualMetrics);
            }

        });
    }

}