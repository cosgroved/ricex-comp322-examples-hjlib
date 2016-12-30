package module1.unit2.topic1;

import edu.rice.hj.api.HjMetrics;
import edu.rice.hj.runtime.config.HjSystemProperty;
import edu.rice.hj.runtime.metrics.AbstractMetricsManager;

import static edu.rice.hj.Module1.*;

/**
 * <p>BinaryTreesSeq class.</p>
 *
 * @author Vivek Sarkar (vsarkar@rice.edu)
 */
public class BinaryTreesSeq {

    private final static int minDepth = 4;

    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {
        final int n;
        if (args.length > 0) {
            n = Integer.parseInt(args[0]);
        } else {
            n = 0;
        }

        final int maxDepth = (minDepth + 2 > n) ? minDepth + 2 : n;
        final int stretchDepth = maxDepth + 1;

        HjSystemProperty.abstractMetrics.set(true);
        launchHabaneroApp(() -> {

            finish(() -> {
                performComputation(maxDepth, stretchDepth);
            });

            final HjMetrics actualMetrics = abstractMetrics();
            AbstractMetricsManager.dumpStatistics(actualMetrics);
        });
    }

    private static void performComputation(final int maxDepth, final int stretchDepth) {
        int check = (TreeNode.bottomUpTree(0, stretchDepth)).itemCheck();
        System.out.println("stretch tree of depth " + stretchDepth + "\t check: " + check);

        final TreeNode longLivedTree = TreeNode.bottomUpTree(0, maxDepth);

        for (int depth = minDepth; depth <= maxDepth; depth += 2) {
            final int iterations = 1 << (maxDepth - depth + minDepth);
            check = 0;

            for (int i = 1; i <= iterations; i++) {
                check += (TreeNode.bottomUpTree(i, depth)).itemCheck();
                check += (TreeNode.bottomUpTree(-i, depth)).itemCheck();
            }
            System.out.println((iterations * 2) + "\t trees of depth " + depth + "\t check: " + check);
        }
        System.out.println("long lived tree of depth " + maxDepth + "\t check: " + longLivedTree.itemCheck());
    }

    private static class TreeNode {
        private final TreeNode left;
        private final TreeNode right;

        private final int item;

        TreeNode(final int item) {
            this(null, null, item);
        }

        TreeNode(final TreeNode left, final TreeNode right, final int item) {
            this.left = left;
            this.right = right;
            this.item = item;
        }

        private static TreeNode bottomUpTree(final int item, final int depth) {

            final int finalItem = item;
            final int finalDepth = depth;

            doWork(1);

            if (depth > 0) {
                final TreeNode LNode = bottomUpTree(2 * finalItem - 1, finalDepth - 1);
                final TreeNode RNode = bottomUpTree(2 * finalItem, finalDepth - 1);
                return new TreeNode(LNode, RNode, item);
            } else {
                return new TreeNode(item);
            }
        }

        private int itemCheck() {
            // if necessary deallocate here
            if (left == null) {
                return item;
            } else {
                final TreeNode leftNode = left;
                final TreeNode rightNode = right;
                return item + leftNode.itemCheck() - rightNode.itemCheck();
            }
        }
    }

}