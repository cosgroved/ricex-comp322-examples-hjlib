package module2.unit6.topic5;

import edu.rice.hj.runtime.actors.Actor;

import java.util.LinkedList;
import java.util.Queue;

import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.finish;

/**
 * <p>ProdConsBoundedBuffer class.</p>
 *
 * @author Shams Imam (shams@rice.edu)
 *         Created: Mar/17/1 6:33 PM
 */
public class ProdConsBoundedBuffer {
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(final String[] args) {

        // bufferSize must be greater than numProducers for this example
        final int bufferSize = 3;
        final int numProducers = 2;
        final int numConsumers = 3;
        final int numItemsPerProducer = 4;

        final ManagerActor manager = new ManagerActor(
                bufferSize, numProducers, numConsumers, numItemsPerProducer);
		launchHabaneroApp(() -> {
			finish(manager::start);
		});
    }

    private static class DataItemMessage {
        public final Object data;
        public final ProducerActor producer;

        DataItemMessage(final Object data, final ProducerActor producer) {
            this.data = data;
            this.producer = producer;
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

        ConsumerAvailableMessage(final ConsumerActor consumer) {
            this.consumer = consumer;
        }
    }

    private static class ConsumerExitMessage {
        public static ConsumerExitMessage ONLY = new ConsumerExitMessage();
    }

    private static class ManagerActor extends Actor<Object> {

        private final int adjustedBufferSize;

        private final int numProducers;
        private final ProducerActor[] producers;

        private final int numConsumers;
        private final ConsumerActor[] consumers;

        private final Queue<ProducerActor> availableProducers;
        private final Queue<ConsumerActor> availableConsumers;
        private final Queue<DataItemMessage> pendingData;

        private int numTerminatedProducers = 0;

        ManagerActor(final int bufferSize, final int numProducers, final int numConsumers, final int numItemsPerProducer) {

            this.adjustedBufferSize = bufferSize - numProducers;

            this.availableProducers = new LinkedList<ProducerActor>();
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
                final DataItemMessage dm = (DataItemMessage) theMsg;
                if (availableConsumers.isEmpty()) {
                    // no consumers available, store in queue
                    pendingData.add(dm);
                } else {
                    // send data item to consumer
                    availableConsumers.poll().send(dm);
                }
                if (pendingData.size() >= adjustedBufferSize) {
                    // buffer is full, delay next produce request
                    availableProducers.add(dm.producer);
                } else {
                    // request producer to produce next data
                    dm.producer.send(ProduceDataMessage.ONLY);
                }
            } else if (theMsg instanceof ConsumerAvailableMessage) {
                final ConsumerAvailableMessage cm = (ConsumerAvailableMessage) theMsg;
                if (pendingData.isEmpty()) {
                    if (numTerminatedProducers == numProducers) {
                        exit();
                    } else {
                        // no data available, store consumer in queue
                        availableConsumers.add(cm.consumer);
                    }
                } else {
                    // send data item to consumer
                    cm.consumer.send(pendingData.poll());
                    // request a producer to produce data
                    if (!availableProducers.isEmpty()) {
                        availableProducers.poll().send(ProduceDataMessage.ONLY);
                    }
                }
            } else if (theMsg instanceof ProducerExitMessage) {
                numTerminatedProducers++;
                if (numTerminatedProducers == numProducers &&
                        availableConsumers.size() == numConsumers) {
                    exit();
                }
            }
        }

        @Override
        protected void onPreExit() {
            // all producers have terminated and there are no more data items
            // we can terminate all consumers and the buffer
            for (int i = 0; i < numConsumers; i++) {
                this.consumers[i].send(ConsumerExitMessage.ONLY);
            }
            System.out.println("BufferActor exits");
        }
    }

    private static class ProducerActor extends Actor<Object> {

        private final int id;
        private final ManagerActor manager;
        private final int numItemsToProduce;
        private int itemsProduced;

        ProducerActor(final int id, final ManagerActor manager, final int numItemsToProduce) {
            this.id = id;
            this.manager = manager;
            this.numItemsToProduce = numItemsToProduce;
            this.itemsProduced = 0;
        }

        @Override
        protected void process(final Object theMsg) {
            if (theMsg instanceof ProduceDataMessage) {
                if (itemsProduced == numItemsToProduce) {
                    exit();
                } else {
                    produceData();
                }
            }
        }

        private void produceData() {
            // produce a data item
            final Object producedData = produceDataItem(itemsProduced);
            manager.send(new DataItemMessage(producedData, this));
            itemsProduced++;
            System.out.println("Producer-" + id + " available to produce next item.");
        }

        protected Object produceDataItem(final int localItemId) {
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

        ConsumerActor(final int id, final ManagerActor manager) {
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
                final DataItemMessage dm = (DataItemMessage) theMsg;
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

        protected void consumeDataItem(final Object dataToConsume) {
            // consume to data item
            System.out.println("  Consumer-" + id + " consumed: " + dataToConsume);
        }
    }
}
