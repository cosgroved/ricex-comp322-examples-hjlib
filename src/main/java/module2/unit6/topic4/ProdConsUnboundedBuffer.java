package module2.unit6.topic4;

import edu.rice.hj.runtime.actors.Actor;

import java.util.LinkedList;
import java.util.Queue;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.finish;

/**
 * <p>ProdConsUnboundedBuffer class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 *         Created: Mar/17/12 6:33 PM
 */
public class ProdConsUnboundedBuffer {
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {

        final int numProducers = 2;
        final int numConsumers = 3;
        final int numItemsPerProducer = 4;

        ManagerActor manager = new ManagerActor(numProducers, numConsumers, numItemsPerProducer);
		launchHabaneroApp(() -> {
	        finish(() -> {
	            manager.start();
	        });
        });
    }

    private static class DataItemMessage {
        public final Object data;

        DataItemMessage(final Object data) {
            this.data = data;
        }
    }

    private static class ProduceDataMessage {
        public static ProduceDataMessage ONLY = new ProduceDataMessage();
    }

    private static class ProducerExitMessage {
        public static ProducerExitMessage ONLY = new ProducerExitMessage();
    }

    private static class ConsumerAvailableMessage {
        public final ConsumerActor consumer;

        ConsumerAvailableMessage(ConsumerActor consumer) {
            this.consumer = consumer;
        }
    }

    private static class ConsumerExitMessage {
        public static ConsumerExitMessage ONLY = new ConsumerExitMessage();
    }

    private static class ManagerActor extends Actor<Object> {

        private final int numProducers;
        private final ProducerActor[] producers;

        private final int numConsumers;
        private final ConsumerActor[] consumers;

        private int numTerminatedProducers = 0;
        private final Queue<ConsumerActor> availableConsumers;
        private final Queue<DataItemMessage> pendingData;

        ManagerActor(int numProducers, int numConsumers, int numItemsPerProducer) {
            this.numProducers = numProducers;
            this.producers = new ProducerActor[numProducers];
            for (int i = 0; i < numProducers; i++) {
                this.producers[i] = new ProducerActor(i, this, numItemsPerProducer);
            }

            this.availableConsumers = new LinkedList<ConsumerActor>();
            this.numConsumers = numConsumers;
            this.consumers = new ConsumerActor[numConsumers];
            for (int i = 0; i < numConsumers; i++) {
                final ConsumerActor loopConsumer = new ConsumerActor(i, this);
                this.consumers[i] = loopConsumer;
                this.availableConsumers.add(loopConsumer);
            }

            this.pendingData = new LinkedList<DataItemMessage>();
        }

        @Override
        protected void onPostStart() {
            for (int i = 0; i < numConsumers; i++) {
                this.consumers[i].start();
            }
            for (int i = 0; i < numProducers; i++) {
                final ProducerActor loopProducer = this.producers[i];
                loopProducer.start();
                loopProducer.send(ProduceDataMessage.ONLY);
            }
        }

        @Override
        protected void process(final Object theMsg) {
            if (theMsg instanceof DataItemMessage) {
                DataItemMessage dm = (DataItemMessage) theMsg;
                if (availableConsumers.isEmpty()) {
                    // no consumers available, store in queue
                    pendingData.add(dm);
                } else {
                    // send data item to consumer
                    availableConsumers.poll().send(dm);
                }
            } else if (theMsg instanceof ConsumerAvailableMessage) {
                ConsumerAvailableMessage cm = (ConsumerAvailableMessage) theMsg;
                if (pendingData.isEmpty()) {
                    if (numTerminatedProducers == numProducers) {
                        // all producers have terminated and there are no more data items
                        // we can terminate all consumers and the buffer
                        for (int i = 0; i < numConsumers; i++) {
                            this.consumers[i].send(ConsumerExitMessage.ONLY);
                        }
                        exit();
                    } else {
                        // no data available, store consumer in queue
                        availableConsumers.add(cm.consumer);
                    }
                } else {
                    // send data item to consumer
                    cm.consumer.send(pendingData.poll());
                }
            } else if (theMsg instanceof ProducerExitMessage) {
                numTerminatedProducers++;
            }
        }

        @Override
        protected void onPreExit() {
            System.out.println("BufferActor exits");
        }
    }

    private static class ProducerActor extends Actor<Object> {

        private final int id;
        private final ManagerActor manager;
        private final int numItemsToProduce;

        ProducerActor(int id, ManagerActor manager, int numItemsToProduce) {
            this.id = id;
            this.manager = manager;
            this.numItemsToProduce = numItemsToProduce;
        }

        @Override
        protected void process(final Object theMsg) {
            if (theMsg instanceof ProduceDataMessage) {
                produceData();
                exit();
            }
        }

        private void produceData() {
            // produce all items for the UNBOUNDED buffer
            for (int i = 0; i < numItemsToProduce; i++) {
                // produce a data item
                final Object producedData = produceDataItem(i);
                manager.send(new DataItemMessage(producedData));
            }
        }

        protected Object produceDataItem(int localItemId) {
            // produce a data item
            final String dataItem = "p" + id + ".item" + localItemId;
            System.out.println("Producer-" + id + " produced: " + dataItem);
            return dataItem;
        }

        @Override
        protected void onPreExit() {
            manager.send(ProducerExitMessage.ONLY);
        }

        @Override
        protected void onPostExit() {
            System.out.println("Producer-" + id + " exits.");
        }
    }

    private static class ConsumerActor extends Actor<Object> {

        private final int id;
        private final ManagerActor manager;
        private final ConsumerAvailableMessage consumerAvailableMessage;

        ConsumerActor(int id, ManagerActor manager) {
            this.id = id;
            this.manager = manager;
            this.consumerAvailableMessage = new ConsumerAvailableMessage(this);
        }

        @Override
        protected void onPostStart() {
            super.onPostStart();
            System.out.println("  Consumer-" + id + " available to consume next item.");
        }

        @Override
        protected void process(final Object theMsg) {
            if (theMsg instanceof DataItemMessage) {
                DataItemMessage dm = (DataItemMessage) theMsg;
                consumeDataItem(dm.data);
                manager.send(consumerAvailableMessage);
                System.out.println("  Consumer-" + id + " available to consume next item.");
            } else if (theMsg instanceof ConsumerExitMessage) {
                exit();
            }
        }

        @Override
        protected void onPreExit() {
            System.out.println("  Consumer-" + id + " exits.");
        }

        protected void consumeDataItem(Object dataToConsume) {
            // consume to data item
            System.out.println("  Consumer-" + id + " consumed: " + dataToConsume);
        }
    }
}
