package module2.unit5.topic4;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static edu.rice.hj.Module1.async;
import static edu.rice.hj.Module1.launchHabaneroApp;

public class SpanningTreeAtomic {
    public static void main(final String[] args) {

        int globalNumNeighbors = 200;
        int numNodes = 100_000;

        if (args.length > 0) {
            numNodes = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            globalNumNeighbors = Integer.parseInt(args[1]);
        }
        if (numNodes < 2) {
            System.out.println("Error: number of nodes must be > 1");
            return;
        }
        if (globalNumNeighbors < -1) {
            System.out.println("Error: negative number of neighbors entered\n");
            return;
        }

        final Random rand = new Random(12399);
        final Node[] nodes = new Node[numNodes];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new Node(Integer.toString(i));
        }
        for (int i = 0; i < nodes.length; i++) {
            final int numNeighbors = ((globalNumNeighbors == -1) ? rand.nextInt(10) : globalNumNeighbors);
            final Node[] neighbors = new Node[numNeighbors];
            for (int j = 0; j < neighbors.length; j++) {
                int neighborIndex = rand.nextInt(nodes.length);
                if (neighborIndex == i) {
                    neighborIndex = (neighborIndex + 1) % numNodes;
                }
                neighbors[j] = nodes[neighborIndex];
            }
            nodes[i].setNeighbors(neighbors);
        }

        for (int iter = 0; iter < 5; iter++) {
            for (int i = 0; i < nodes.length; i++) {
                nodes[i].parent.set(null);
            }
            final Node root = nodes[0];
            root.parent.set(root);
            final long start = System.currentTimeMillis();
            launchHabaneroApp(() -> {
                root.compute();
            });
            final long stop = System.currentTimeMillis();
            System.out.println("Time: " + (stop - start) + " ms");
        }

    }

    private static class Node {
        public final AtomicReference<Node> parent;
        final String name;
        Node[] neighbors;

        public Node(final String setName) {
            neighbors = new Node[0];
            name = setName;
            parent = new AtomicReference<Node>(null);
        }

        public void setNeighbors(final Node[] n) {
            neighbors = n;
        }

        boolean tryLabeling(final Node n) {
            return parent.compareAndSet(null, n);
        }

        void compute() {
            for (int i = 0; i < neighbors.length; i++) {
                final Node child = neighbors[i];
                if (child.tryLabeling(this)) {
                    async(() -> {
                        child.compute();
                    });
                }
            }
        }
    }
}