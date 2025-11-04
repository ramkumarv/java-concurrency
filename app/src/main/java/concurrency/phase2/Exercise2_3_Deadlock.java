package concurrency.phase2;

/**
 * Exercise 2.3: Create a deadlock scenario and then fix it
 * 
 * Learning Objectives:
 * - Understand what causes deadlocks
 * - Learn to identify deadlock-prone code
 * - See how to prevent deadlocks
 * - Understand lock ordering strategy
 * - Learn about lock timeouts and detection
 */
public class Exercise2_3_Deadlock {
    
    /**
     * Bank account class that can cause deadlock
     */
    static class DeadlockProneAccount {
        private double balance;
        private final String accountId;
        
        public DeadlockProneAccount(String accountId, double initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
        }
        
        public synchronized void deposit(double amount) {
            balance += amount;
        }
        
        public synchronized void withdraw(double amount) {
            balance -= amount;
        }
        
        public synchronized double getBalance() {
            return balance;
        }
        
        // DEADLOCK RISK: Locks are acquired in different order
        public synchronized void transfer(DeadlockProneAccount to, double amount) {
            System.out.println(Thread.currentThread().getName() + 
                " attempting to transfer " + amount + " from " + accountId + " to " + to.accountId);
            
            // This locks 'this', then tries to lock 'to'
            // If another thread is doing reverse transfer, deadlock occurs!
            synchronized (to) {
                if (balance >= amount) {
                    balance -= amount;
                    to.balance += amount;
                    System.out.println(Thread.currentThread().getName() + 
                        " transferred " + amount);
                }
            }
        }
    }
    
    /**
     * Fixed bank account with consistent lock ordering
     */
    static class DeadlockFreeAccount {
        private double balance;
        private final String accountId;
        // Note: Using synchronized blocks with consistent ordering
        
        public DeadlockFreeAccount(String accountId, double initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
        }
        
        public synchronized void deposit(double amount) {
            balance += amount;
        }
        
        public synchronized void withdraw(double amount) {
            balance -= amount;
        }
        
        public synchronized double getBalance() {
            return balance;
        }
        
        // FIXED: Lock accounts in consistent order (by accountId)
        public void transfer(DeadlockFreeAccount to, double amount) {
            // Determine lock order based on accountId (consistent ordering)
            DeadlockFreeAccount first = this;
            DeadlockFreeAccount second = to;
            
            if (accountId.compareTo(to.accountId) > 0) {
                first = to;
                second = this;
            }
            
            synchronized (first) {
                synchronized (second) {
                    System.out.println(Thread.currentThread().getName() + 
                        " transferring " + amount + " from " + accountId + " to " + to.accountId);
                    
                    if (balance >= amount) {
                        balance -= amount;
                        to.balance += amount;
                        System.out.println(Thread.currentThread().getName() + 
                            " transfer completed");
                    }
                }
            }
        }
    }
    
    /**
     * Account with timeout-based lock to detect deadlocks
     */
    static class TimeoutAccount {
        private double balance;
        private final String accountId;
        // Note: Simplified timeout implementation for demonstration
        
        public TimeoutAccount(String accountId, double initialBalance) {
            this.accountId = accountId;
            this.balance = initialBalance;
        }
        
        public synchronized void deposit(double amount) {
            balance += amount;
        }
        
        public synchronized void withdraw(double amount) {
            balance -= amount;
        }
        
        public synchronized double getBalance() {
            return balance;
        }
        
        // Using wait/notify with timeout (simplified - real implementation would use ReentrantLock)
        public boolean transfer(TimeoutAccount to, double amount, long timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            TimeoutAccount first = this;
            TimeoutAccount second = to;
            
            if (accountId.compareTo(to.accountId) > 0) {
                first = to;
                second = this;
            }
            
            // Try to acquire locks with timeout
            while (System.currentTimeMillis() < deadline) {
                if (Thread.holdsLock(first) || Thread.holdsLock(second)) {
                    try {
                        Thread.sleep(10); // Check every 10ms
                        continue;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                
                synchronized (first) {
                    if (System.currentTimeMillis() >= deadline) {
                        return false; // Timeout
                    }
                    
                    synchronized (second) {
                        if (balance >= amount) {
                            balance -= amount;
                            to.balance += amount;
                            return true;
                        }
                        return false;
                    }
                }
            }
            
            System.out.println(Thread.currentThread().getName() + 
                " transfer timed out after " + timeoutMs + "ms");
            return false;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 2.3: Deadlock Demonstration ===\n");
        
        // Scenario 1: Create deadlock
        System.out.println("Scenario 1: Creating a Deadlock (may take a moment to occur)");
        System.out.println("Watch for threads stuck waiting...\n");
        
        DeadlockProneAccount accountA = new DeadlockProneAccount("Account-A", 1000.0);
        DeadlockProneAccount accountB = new DeadlockProneAccount("Account-B", 1000.0);
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                accountA.transfer(accountB, 10.0);
            }
        }, "Thread-1");
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                accountB.transfer(accountA, 10.0);
            }
        }, "Thread-2");
        
        thread1.start();
        thread2.start();
        
        // Wait with timeout to detect deadlock
        try {
            thread1.join(3000); // Wait 3 seconds
            thread2.join(3000);
            
            if (thread1.isAlive() || thread2.isAlive()) {
                System.out.println("\n⚠ DEADLOCK DETECTED! Threads are stuck.");
                System.out.println("Thread-1 alive: " + thread1.isAlive());
                System.out.println("Thread-2 alive: " + thread2.isAlive());
                System.out.println("This is why we need consistent lock ordering!\n");
            } else {
                System.out.println("Threads completed (got lucky, no deadlock this time)");
                System.out.println("Note: Deadlocks are non-deterministic - they may or may not occur\n");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Scenario 2: Fixed version with consistent lock ordering
        System.out.println("=== Scenario 2: Deadlock-Free Implementation ===\n");
        
        DeadlockFreeAccount account1 = new DeadlockFreeAccount("Account-1", 1000.0);
        DeadlockFreeAccount account2 = new DeadlockFreeAccount("Account-2", 1000.0);
        DeadlockFreeAccount account3 = new DeadlockFreeAccount("Account-3", 1500.0);
        
        Thread[] threads = new Thread[6];
        threads[0] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account1.transfer(account2, 10.0);
            }
        }, "Transfer-1->2");
        
        threads[1] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account2.transfer(account1, 10.0);
            }
        }, "Transfer-2->1");
        
        threads[2] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account1.transfer(account3, 5.0);
            }
        }, "Transfer-1->3");
        
        threads[3] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account3.transfer(account1, 5.0);
            }
        }, "Transfer-3->1");
        
        threads[4] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account2.transfer(account3, 15.0);
            }
        }, "Transfer-2->3");
        
        threads[5] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                account3.transfer(account2, 15.0);
            }
        }, "Transfer-3->2");
        
        long startTime = System.currentTimeMillis();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nAll transfers completed without deadlock!");
        System.out.println("Account-1 balance: " + account1.getBalance());
        System.out.println("Account-2 balance: " + account2.getBalance());
        System.out.println("Account-3 balance: " + account3.getBalance());
        double total = account1.getBalance() + account2.getBalance() + account3.getBalance();
        System.out.println("Total: " + total + " (Expected: 3500.0) ✓");
        System.out.println("Time: " + (endTime - startTime) + " ms\n");
        
        System.out.println("=== Deadlock Prevention Strategies ===");
        System.out.println("1. Lock Ordering: Always acquire locks in the same order");
        System.out.println("   - Use a consistent ordering (e.g., by account ID)");
        System.out.println("   - Compare object identity hash codes");
        System.out.println("2. Lock Timeout: Use tryLock with timeout");
        System.out.println("3. Avoid Nested Locks: Minimize lock nesting");
        System.out.println("4. Lock-Free Algorithms: Use atomic operations when possible");
        System.out.println("5. Single Lock: Use one lock for multiple resources if possible");
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. Deadlocks occur when threads wait for each other indefinitely");
        System.out.println("2. Consistent lock ordering prevents deadlocks");
        System.out.println("3. Deadlocks are non-deterministic - they may not always occur");
        System.out.println("4. Use tools like jstack to detect deadlocks in production");
        System.out.println("5. Prevention is better than detection - design carefully!");
    }
}

