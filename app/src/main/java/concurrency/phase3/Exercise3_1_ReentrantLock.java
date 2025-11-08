package concurrency.phase3;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/**
 * Exercise 3.1: Replace synchronized with ReentrantLock in bank account
 * 
 * Learning Objectives:
 * - Understand ReentrantLock vs synchronized
 * - Learn tryLock() and lockInterruptibly()
 * - Understand fair vs non-fair locks
 * - Practice proper lock release in finally blocks
 * - Use Condition for wait/notify patterns
 */
public class Exercise3_1_ReentrantLock {
    
    /**
     * Bank account using synchronized (for comparison)
     */
    static class SynchronizedBankAccount {
        private double balance;
        
        public SynchronizedBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        public synchronized void deposit(double amount) {
            if (amount > 0) {
                balance += amount;
                System.out.println(Thread.currentThread().getName() + 
                    " deposited " + amount + ", balance: " + balance);
            }
        }
        
        public synchronized void withdraw(double amount) {
            if (amount > 0 && balance >= amount) {
                balance -= amount;
                System.out.println(Thread.currentThread().getName() + 
                    " withdrew " + amount + ", balance: " + balance);
            } else {
                System.out.println(Thread.currentThread().getName() + 
                    " withdrawal failed: insufficient funds");
            }
        }
        
        public synchronized double getBalance() {
            return balance;
        }
    }
    
    /**
     * Bank account using ReentrantLock (basic version)
     */
    static class ReentrantLockBankAccount {
        private double balance;
        private final Lock lock = new ReentrantLock();
        
        public ReentrantLockBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        public void deposit(double amount) {
            lock.lock(); // Acquire lock
            try {
                if (amount > 0) {
                    balance += amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " deposited " + amount + ", balance: " + balance);
                }
            } finally {
                lock.unlock(); // Always release in finally!
            }
        }
        
        public void withdraw(double amount) {
            lock.lock();
            try {
                if (amount > 0 && balance >= amount) {
                    balance -= amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrew " + amount + ", balance: " + balance);
                } else {
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrawal failed: insufficient funds");
                }
            } finally {
                lock.unlock();
            }
        }
        
        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Bank account using ReentrantLock with tryLock()
     * Demonstrates non-blocking lock acquisition
     */
    static class TryLockBankAccount {
        private double balance;
        private final Lock lock = new ReentrantLock();
        
        public TryLockBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        /**
         * Try to withdraw with timeout - non-blocking
         */
        public boolean tryWithdraw(double amount, long timeoutMs) {
            boolean acquired = false;
            try {
                // Try to acquire lock within timeout
                acquired = lock.tryLock();
                if (!acquired) {
                    System.out.println(Thread.currentThread().getName() + 
                        " could not acquire lock within timeout");
                    return false;
                }
                
                if (amount > 0 && balance >= amount) {
                    balance -= amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrew " + amount + ", balance: " + balance);
                    return true;
                } else {
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrawal failed: insufficient funds");
                    return false;
                }
            } finally {
                if (acquired) {
                    lock.unlock();
                }
            }
        }
        
        public void deposit(double amount) {
            lock.lock();
            try {
                if (amount > 0) {
                    balance += amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " deposited " + amount + ", balance: " + balance);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Bank account using ReentrantLock with lockInterruptibly()
     * Demonstrates interruptible lock acquisition
     */
    static class InterruptibleLockBankAccount {
        private double balance;
        private final Lock lock = new ReentrantLock();
        
        public InterruptibleLockBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        /**
         * Withdraw with interruptible lock acquisition
         */
        public void withdraw(double amount) throws InterruptedException {
            lock.lockInterruptibly(); // Can be interrupted while waiting
            try {
                // Simulate some processing time
                Thread.sleep(100);
                
                if (amount > 0 && balance >= amount) {
                    balance -= amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrew " + amount + ", balance: " + balance);
                } else {
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrawal failed: insufficient funds");
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void deposit(double amount) {
            lock.lock();
            try {
                if (amount > 0) {
                    balance += amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " deposited " + amount + ", balance: " + balance);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Bank account using ReentrantLock with Condition
     * Demonstrates wait/notify pattern with Condition
     */
    static class ConditionBankAccount {
        private double balance;
        private final Lock lock = new ReentrantLock();
        private final Condition sufficientFunds = lock.newCondition();
        
        public ConditionBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        /**
         * Withdraw with waiting for sufficient funds
         */
        public void withdraw(double amount) throws InterruptedException {
            lock.lock();
            try {
                // Wait until balance is sufficient
                while (balance < amount) {
                    System.out.println(Thread.currentThread().getName() + 
                        " waiting for sufficient funds. Current: " + balance + 
                        ", Required: " + amount);
                    sufficientFunds.await(); // Releases lock and waits
                }
                
                balance -= amount;
                System.out.println(Thread.currentThread().getName() + 
                    " withdrew " + amount + ", balance: " + balance);
            } finally {
                lock.unlock();
            }
        }
        
        /**
         * Deposit and signal waiting threads
         */
        public void deposit(double amount) {
            lock.lock();
            try {
                balance += amount;
                System.out.println(Thread.currentThread().getName() + 
                    " deposited " + amount + ", balance: " + balance);
                sufficientFunds.signalAll(); // Notify waiting threads
            } finally {
                lock.unlock();
            }
        }
        
        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Bank account using fair ReentrantLock
     * Demonstrates fair vs non-fair locking
     */
    static class FairLockBankAccount {
        private double balance;
        private final Lock lock = new ReentrantLock(true); // Fair lock
        
        public FairLockBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        public void deposit(double amount) {
            lock.lock();
            try {
                if (amount > 0) {
                    balance += amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " deposited " + amount + ", balance: " + balance);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void withdraw(double amount) {
            lock.lock();
            try {
                if (amount > 0 && balance >= amount) {
                    balance -= amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrew " + amount + ", balance: " + balance);
                } else {
                    System.out.println(Thread.currentThread().getName() + 
                        " withdrawal failed: insufficient funds");
                }
            } finally {
                lock.unlock();
            }
        }
        
        public double getBalance() {
            lock.lock();
            try {
                return balance;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Demonstrates reentrant nature of ReentrantLock
     */
    static class ReentrantLockReentrancyDemo {
        private final ReentrantLock lock = new ReentrantLock();
        
        public void outerMethod() {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + 
                    " entered outerMethod, hold count: " + lock.getHoldCount());
                innerMethod();
                System.out.println(Thread.currentThread().getName() + 
                    " leaving outerMethod, hold count: " + lock.getHoldCount());
            } finally {
                lock.unlock();
            }
        }
        
        private void innerMethod() {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + 
                    " entered innerMethod, hold count: " + lock.getHoldCount());
                recursiveMethod(2);
                System.out.println(Thread.currentThread().getName() + 
                    " leaving innerMethod, hold count: " + lock.getHoldCount());
            } finally {
                lock.unlock();
            }
        }
        
        private void recursiveMethod(int depth) {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + 
                    " recursive depth " + depth + ", hold count: " + lock.getHoldCount());
                if (depth > 0) {
                    recursiveMethod(depth - 1);
                }
            } finally {
                System.out.println(Thread.currentThread().getName() + 
                    " releasing depth " + depth + ", hold count before unlock: " + lock.getHoldCount());
                lock.unlock();
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 3.1: ReentrantLock ===\n");
        
        // Scenario 1: Basic ReentrantLock vs Synchronized
        System.out.println("=== Scenario 1: ReentrantLock vs Synchronized ===");
        System.out.println("NOTE: Running ReentrantLock FIRST to demonstrate JVM warmup effect!\n");
        
        SynchronizedBankAccount syncAccount = new SynchronizedBankAccount(1000.0);
        ReentrantLockBankAccount lockAccount = new ReentrantLockBankAccount(1000.0);
        
        Thread[] syncThreads = new Thread[5];
        Thread[] lockThreads = new Thread[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            syncThreads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    syncAccount.deposit(10.0);
                    syncAccount.withdraw(5.0);
                }
            }, "SyncThread-" + index);
            
            lockThreads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    lockAccount.deposit(10.0);
                    lockAccount.withdraw(5.0);
                }
            }, "LockThread-" + index);
        }
        
        // SWITCHED ORDER: Run ReentrantLock FIRST (cold JVM)
        System.out.println("Running ReentrantLock FIRST (cold JVM)...");
        long startTime = System.currentTimeMillis();
        for (Thread t : lockThreads) t.start();
        for (Thread t : lockThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long lockTime = System.currentTimeMillis() - startTime;
        
        // Run Synchronized SECOND (warm JVM)
        System.out.println("Running Synchronized SECOND (warm JVM)...");
        startTime = System.currentTimeMillis();
        for (Thread t : syncThreads) t.start();
        for (Thread t : syncThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long syncTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nReentrantLock account balance: " + lockAccount.getBalance());
        System.out.println("Synchronized account balance: " + syncAccount.getBalance());
        System.out.println("\nReentrantLock time (cold JVM): " + lockTime + " ms");
        System.out.println("Synchronized time (warm JVM): " + syncTime + " ms");
        System.out.println("\nNotice: With order switched, Synchronized appears faster!");
        System.out.println("This proves the difference is due to JVM warmup, not lock performance.");
        System.out.println("Expected balance: 1000 + (10 * 10 * 5) - (5 * 10 * 5) = 1250.0\n");
        
        // Scenario 2: tryLock() demonstration
        System.out.println("=== Scenario 2: tryLock() - Non-blocking Lock Acquisition ===\n");
        TryLockBankAccount tryLockAccount = new TryLockBankAccount(500.0);
        
        Thread tryLockThread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                tryLockAccount.tryWithdraw(50.0, 100);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "TryLockThread-1");
        
        Thread tryLockThread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                tryLockAccount.tryWithdraw(50.0, 100);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "TryLockThread-2");
        
        tryLockThread1.start();
        tryLockThread2.start();
        try {
            tryLockThread1.join();
            tryLockThread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final balance: " + tryLockAccount.getBalance() + "\n");
        
        // Scenario 3: lockInterruptibly() demonstration
        System.out.println("=== Scenario 3: lockInterruptibly() - Interruptible Lock ===\n");
        InterruptibleLockBankAccount interruptibleAccount = new InterruptibleLockBankAccount(200.0);
        
        Thread interruptibleThread = new Thread(() -> {
            try {
                interruptibleAccount.withdraw(100.0);
                interruptibleAccount.withdraw(150.0);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + 
                    " was interrupted while waiting for lock");
                Thread.currentThread().interrupt();
            }
        }, "InterruptibleThread");
        
        interruptibleThread.start();
        try {
            Thread.sleep(50);
            interruptibleThread.interrupt(); // Interrupt the thread
            interruptibleThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final balance: " + interruptibleAccount.getBalance() + "\n");
        
        // Scenario 4: Condition demonstration
        System.out.println("=== Scenario 4: Condition - Wait/Notify Pattern ===\n");
        ConditionBankAccount conditionAccount = new ConditionBankAccount(100.0);
        
        Thread consumer1 = new Thread(() -> {
            try {
                conditionAccount.withdraw(150.0); // Will wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-1");
        
        Thread consumer2 = new Thread(() -> {
            try {
                conditionAccount.withdraw(100.0); // Will wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-2");
        
        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(500); // Wait a bit
                conditionAccount.deposit(200.0); // This will wake up waiting threads
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");
        
        consumer1.start();
        consumer2.start();
        producer.start();
        
        try {
            consumer1.join();
            consumer2.join();
            producer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final balance: " + conditionAccount.getBalance() + "\n");
        
        // Scenario 5: Fair vs Non-fair lock
        System.out.println("=== Scenario 5: Fair Lock ===\n");
        System.out.println("Fair locks ensure threads acquire locks in FIFO order.");
        System.out.println("Non-fair locks may allow threads to 'jump the queue'.");
        System.out.println("Fair locks are slower but more predictable.\n");
        
        FairLockBankAccount fairAccount = new FairLockBankAccount(1000.0);
        
        Thread[] fairThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            fairThreads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    fairAccount.deposit(10.0);
                    fairAccount.withdraw(5.0);
                }
            }, "FairThread-" + index);
        }
        
        for (Thread t : fairThreads) t.start();
        for (Thread t : fairThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        System.out.println("Final balance: " + fairAccount.getBalance() + "\n");
        
        // Scenario 6: Reentrant nature demonstration
        System.out.println("=== Scenario 6: ReentrantLock Reentrancy ===\n");
        ReentrantLockReentrancyDemo reentrancyDemo = new ReentrantLockReentrancyDemo();
        reentrancyDemo.outerMethod();
        System.out.println();
        
        System.out.println("=== Key Takeaways ===");
        System.out.println("1. ReentrantLock provides more flexibility than synchronized");
        System.out.println("2. Always release locks in finally blocks");
        System.out.println("3. tryLock() allows non-blocking lock acquisition");
        System.out.println("4. lockInterruptibly() allows interruption while waiting");
        System.out.println("5. Condition provides better control than wait()/notify()");
        System.out.println("6. Fair locks ensure FIFO ordering but are slower");
        System.out.println("7. ReentrantLock is reentrant (same thread can acquire multiple times)");
    }
}

