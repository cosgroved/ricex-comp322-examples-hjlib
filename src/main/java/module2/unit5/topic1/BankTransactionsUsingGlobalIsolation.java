package module2.unit5.topic1;

import edu.rice.hj.runtime.config.HjSystemProperty;

import java.util.Random;

import static edu.rice.hj.Module1.*;
import static edu.rice.hj.Module2.isolated;

/**
 * <p>BankTransactionsUsingGlobalIsolation class.</p>
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class BankTransactionsUsingGlobalIsolation {
    /**
     * <p>main.</p>
     *
     * @param args an array of {@link String} objects.
     */
    public static void main(final String[] args) {

        System.out.println("BankTransactionsUsingGlobalIsolation starts...");

        final int numAccounts = 2_000;
        final int numTransactions = 500_000;

        for (int i = 0; i < 5; i++) {
            System.out.println("Iteration-" + i);

            for (int w = 1; w <= Runtime.getRuntime().availableProcessors(); w++) {
                benchmarkBody(numAccounts, numTransactions, w);
            }
        }
    }

    private static void benchmarkBody(final int numAccounts, final int numTransactions, final int numWorkers) {

        final Account[] bankAccounts = new Account[numAccounts];
        for (int i = 0; i < numAccounts; i++) {
            bankAccounts[i] = new Account(1000 * (randomIntValue(new Random(1000), numAccounts) + 1));
        }

        final long preSumOfBalances = sumBalances(bankAccounts);
        // System.out.printf("Sum of balances before execution = %d. \n", preSumOfBalances);

        HjSystemProperty.numWorkers.set(String.valueOf(numWorkers));
        final long s = System.nanoTime();
        launchHabaneroApp(() -> {

            finish(() -> {
                for (int i = 0; i < numTransactions - 1; i++) {
                    final int ii = i;
                    async(() -> kernelBody(ii, numAccounts, bankAccounts));
                }
                // async peeling
                kernelBody(numTransactions - 1, numAccounts, bankAccounts);
            });

        });
        final long e = System.nanoTime();

        System.out.printf("  %25s Time: %10.3f ms.\n", "Worker-" + numWorkers, (e - s) / 1e6);
        final long postSumOfBalances = sumBalances(bankAccounts);
        assert (preSumOfBalances == postSumOfBalances) : ("Error in checking sum of balances");
        // System.out.printf("  Sum of balances after execution  = %d. \n", postSumOfBalances);
    }

    private static int randomIntValue(final Random random, final int limit) {
        return (int) (Math.abs(random.nextDouble() * 10000) % limit);
    }

    private static long sumBalances(final Account[] bankAccounts) {
        long res = 0;
        for (final Account bankAccount : bankAccounts) {
            res += bankAccount.balance();
        }
        return res;
    }

    private static void kernelBody(final int taskId, final int numAccounts, final Account[] bankAccounts) {
        final Random myRandom = new Random(100L * (taskId + 1));

        final int srcIndex = randomIntValue(myRandom, numAccounts);
        final Account srcAccount = bankAccounts[srcIndex];

        final int destIndex = randomIntValue(myRandom, numAccounts);
        final Account destAccount = bankAccounts[destIndex];

        isolated(() -> {
            final int transferAmount = randomIntValue(myRandom, srcAccount.balance());
            final boolean success = srcAccount.withdraw(transferAmount);
            busyWork(srcIndex, destIndex);
            if (success) {
                destAccount.deposit(transferAmount);
            }
        });
    }

    private static void busyWork(final int srcIndex, final int destIndex) {
        for (int i = 0; i < srcIndex * 100; i++) {
            ;
        }
        for (int i = 0; i < destIndex * 100; i++) {
            ;
        }
    }

    private static class Account {
        private int balance;

        private Account(final int balance) {
            this.balance = balance;
        }

        public int balance() {
            return balance;
        }

        public boolean withdraw(final int amount) {
            if (amount > 0 && amount < balance) {
                balance -= amount;
                return true;
            }
            return false;
        }

        public boolean deposit(final int amount) {
            if (amount > 0) {
                balance += amount;
                return true;
            }
            return false;
        }
    }
}
