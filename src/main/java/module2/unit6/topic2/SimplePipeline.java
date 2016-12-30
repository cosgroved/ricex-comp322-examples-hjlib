package module2.unit6.topic2;

import edu.rice.hj.runtime.actors.Actor;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.finish;

/**
 * <p>SimplePipeline class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 */
public class SimplePipeline {

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {

        launchHabaneroApp(()->{
	        finish(() -> {
	
	            Actor<Object> firstStage =
	                    new EvenLengthFilter(
	                            new LowerCaseFilter(
	                                    new LastStage()));
	
	            firstStage.start();
	            firstStage.send("A");
	            firstStage.send("Simple");
	            firstStage.send("pipeline");
	            firstStage.send("with");
	            firstStage.send("3");
	            firstStage.send("stages");
	
	            firstStage.send(new StopMessage());
	        });
        });
    }

    private static class StopMessage {
    }

    /**
     * Only forwards inputs with even length strings
     */
    private static class EvenLengthFilter extends Actor<Object> {
        private final Actor<Object> nextStage;

        EvenLengthFilter(final Actor<Object> nextStage) {
            this.nextStage = nextStage;
        }

        @Override
        protected void onPostStart() {
            nextStage.start();
        }

        protected void process(final Object msg) {
            if (msg instanceof StopMessage) {
                nextStage.send(msg);
                exit();
            } else if (msg instanceof String) {
                String msgStr = (String) msg;
                if (msgStr.length() % 2 == 0) {
                    nextStage.send(msgStr);
                }
            }
        }
    }

    /**
     * Only forwards inputs with all lowercase strings
     */
    private static class LowerCaseFilter extends Actor<Object> {
        private final Actor<Object> nextStage;

        LowerCaseFilter(final Actor<Object> nextStage) {
            this.nextStage = nextStage;
        }

        @Override
        protected void onPostStart() {
            nextStage.start();
        }

        protected void process(final Object msg) {
            if (msg instanceof StopMessage) {
                exit();
                nextStage.send(msg);
            } else if (msg instanceof String) {
                String msgStr = (String) msg;
                if (msgStr.toLowerCase().equals(msgStr)) {
                    nextStage.send(msgStr);
                }
            }
        }
    }

    /**
     * Prints any input strings
     */
    private static class LastStage extends Actor<Object> {
        protected void process(final Object msg) {
            if (msg instanceof StopMessage) {
                exit();
            } else if (msg instanceof String) {
                System.out.println(msg);
            }
        }
    }

}
