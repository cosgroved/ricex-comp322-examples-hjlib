package module2.unit6.topic1;

import static edu.rice.hj.Module1.*;
import static edu.rice.hj.Module2.isolated;

/**
 * <p>IntegerCounterIsolated class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class IntegerCounterActorVersion {

    // Can also use atomic variables instead of isolated
    private int counter = 0;

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        launchHabaneroApp(() -> {

            final IntegerCounterActorVersion anObj = new IntegerCounterActorVersion();
            finish(() -> {
                for (int i = 0; i < 100; i++) {

                    async(anObj::foo);
                    async(anObj::bar);
                    async(anObj::foo);

                }
            });

            System.out.println("Counter = " + anObj.counter());

        });
    }

    private int counter() {
        return counter;
    }

    /**
     * <p>foo.</p>
     */
    public void foo() {
        // do something
        isolated(() -> {
            counter++;
        });
        // do something else
    }

    /**
     * <p>bar.</p>
     */
    public void bar() {
        // do something
        isolated(() -> {
            counter--;
        });
        // do something else
    }
}