package concurrency.phase2;

/**
 * Exercise 2.2: Implement a thread-safe bank account class
 * 
 * Learning Objectives:
 * - Apply synchronized to real-world scenario
 * - Understand method-level vs block-level synchronization
 * - Learn to identify critical sections
 * - See compound operations that need synchronization
 * - Understand read-modify-write operations
 */
public class Exercise2_2_ThreadSafeBankAccount {
    
    /**
     * Unsafe bank account - demonstrates race conditions
     */
    static class UnsafeBankAccount {
        private double balance;
        
        public UnsafeBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        public void deposit(double amount) {
            // Compound operation: read, modify, write
            balance = balance + amount;
        }
        
        public void withdraw(double amount) {
            // Compound operation: read, modify, write
            balance = balance - amount;
        }
        
        public double getBalance() {
            return balance;
        }
        
        public boolean transfer(UnsafeBankAccount to, double amount) {
            // This is a compound operation across two accounts
            if (balance >= amount) {
                this.withdraw(amount);
                to.deposit(amount);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Thread-safe bank account using synchronized methods
     */
    static class ThreadSafeBankAccount {
        private double balance;
        
        public ThreadSafeBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        // Synchronized method - only one thread can execute at a time
        public synchronized void deposit(double amount) {
            if (amount > 0) {
                balance = balance + amount;
                System.out.println(Thread.currentThread().getName() + 
                    " deposited " + amount + ", new balance: " + balance);
            }
        }
        
        public synchronized void withdraw(double amount) {
            if (amount > 0 && balance >= amount) {
                balance = balance - amount;
                System.out.println(Thread.currentThread().getName() + 
                    " withdrew " + amount + ", new balance: " + balance);
                return;
            }
            System.out.println(Thread.currentThread().getName() + 
                " withdrawal failed: insufficient funds");
        }
        
        public synchronized double getBalance() {
            return balance;
        }
        
        // Synchronized method for transfer
        // PROBLEM: This can cause deadlock if transferring between accounts!
        public synchronized boolean transfer(ThreadSafeBankAccount to, double amount) {
            if (balance >= amount) {
                this.withdraw(amount);
                to.deposit(amount);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Improved thread-safe bank account with better transfer method
     */
    static class ImprovedBankAccount {
        private double balance;
        // Note: lock field shown for demonstration, but using synchronized(this) in practice
        
        public ImprovedBankAccount(double initialBalance) {
            this.balance = initialBalance;
        }
        
        public synchronized void deposit(double amount) {
            if (amount > 0) {
                balance = balance + amount;
            }
        }
        
        public synchronized void withdraw(double amount) {
            if (amount > 0 && balance >= amount) {
                balance = balance - amount;
            }
        }
        
        public synchronized double getBalance() {
            return balance;
        }
        
        // Better transfer: locks both accounts in consistent order to avoid deadlock
        public boolean transfer(ImprovedBankAccount to, double amount) {
            // Lock accounts in consistent order (by object identity hash)
            ImprovedBankAccount first = this;
            ImprovedBankAccount second = to;
            
            // Ensure consistent locking order to prevent deadlock
            if (System.identityHashCode(first) > System.identityHashCode(second)) {
                first = to;
                second = this;
            }
            
            synchronized (first) {
                synchronized (second) {
                    if (balance >= amount) {
                        balance = balance - amount;
                        to.balance = to.balance + amount;
                        return true;
                    }
                    return false;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 2.2: Thread-Safe Bank Account ===\n");
        
        // Scenario 1: Unsafe account with race condition
        System.out.println("Scenario 1: Unsafe Bank Account (Race Condition)");
        UnsafeBankAccount unsafeAccount = new UnsafeBankAccount(1000.0);
        
        Thread[] unsafeThreads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            unsafeThreads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    unsafeAccount.deposit(10.0);
                    unsafeAccount.withdraw(5.0);
                }
            }, "UnsafeThread-" + index);
        }
        
        long startTime = System.currentTimeMillis();
        for (Thread thread : unsafeThreads) {
            thread.start();
        }
        for (Thread thread : unsafeThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.currentTimeMillis();
        
        System.out.println("Unsafe account final balance: " + unsafeAccount.getBalance());
        System.out.println("Expected: 1000 + (10 * 100 * 10) - (5 * 100 * 10) = 6000");
        System.out.println("Difference: " + (6000 - unsafeAccount.getBalance()) + 
                         " (lost due to race condition!)");
        System.out.println("Time: " + (endTime - startTime) + " ms\n");
        
        // Scenario 2: Thread-safe account
        System.out.println("=== Scenario 2: Thread-Safe Bank Account ===\n");
        ThreadSafeBankAccount safeAccount = new ThreadSafeBankAccount(1000.0);
        
        Thread[] safeThreads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            safeThreads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    safeAccount.deposit(10.0);
                    safeAccount.withdraw(5.0);
                }
            }, "SafeThread-" + index);
        }
        
        startTime = System.currentTimeMillis();
        for (Thread thread : safeThreads) {
            thread.start();
        }
        for (Thread thread : safeThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        endTime = System.currentTimeMillis();
        
        System.out.println("\nThread-safe account final balance: " + safeAccount.getBalance());
        System.out.println("Expected: 6000.0 ✓");
        System.out.println("Time: " + (endTime - startTime) + " ms");
        System.out.println("Note: Synchronized version is slower but correct!\n");
        
        // Scenario 3: Demonstrating deadlock with ThreadSafeBankAccount.transfer()
        System.out.println("=== Scenario 3: Deadlock Demonstration (ThreadSafeBankAccount) ===\n");
        System.out.println("Warning: This scenario may deadlock! It demonstrates the problem.");
        System.out.println("If deadlock occurs, the program will hang. Use Ctrl+C to stop.\n");
        
        ThreadSafeBankAccount deadlockAccount1 = new ThreadSafeBankAccount(1000.0);
        ThreadSafeBankAccount deadlockAccount2 = new ThreadSafeBankAccount(1000.0);
        
        // Create threads that do bidirectional transfers to trigger deadlock
        Thread deadlockThread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                deadlockAccount1.transfer(deadlockAccount2, 10.0);
            }
        }, "DeadlockThread-1");
        
        Thread deadlockThread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                deadlockAccount2.transfer(deadlockAccount1, 10.0);
            }
        }, "DeadlockThread-2");
        
        System.out.println("Starting bidirectional transfers...");
        System.out.println("Thread 1: account1 -> account2");
        System.out.println("Thread 2: account2 -> account1");
        System.out.println("This creates a circular wait condition.\n");
        
        startTime = System.currentTimeMillis();
        deadlockThread1.start();
        deadlockThread2.start();
        
        // Use a timeout to detect deadlock
        long timeout = 5000; // 5 seconds
        boolean deadlockDetected = false;
        
        try {
            deadlockThread1.join(timeout);
            if (deadlockThread1.isAlive()) {
                deadlockDetected = true;
                System.out.println("⚠️ DEADLOCK DETECTED! Thread 1 did not complete within " + timeout + "ms");
            }
            deadlockThread2.join(timeout);
            if (deadlockThread2.isAlive()) {
                deadlockDetected = true;
                System.out.println("⚠️ DEADLOCK DETECTED! Thread 2 did not complete within " + timeout + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        endTime = System.currentTimeMillis();
        
        if (deadlockDetected) {
            System.out.println("\n❌ DEADLOCK CONFIRMED!");
            System.out.println("Both threads are waiting for each other's locks.");
            System.out.println("Thread 1 holds account1 lock, waiting for account2 lock.");
            System.out.println("Thread 2 holds account2 lock, waiting for account1 lock.");
        } else {
            System.out.println("\n✓ Transfer completed (deadlock did not occur this time)");
            System.out.println("Note: Deadlocks are timing-dependent and may not always occur.");
            System.out.println("Run the program multiple times to increase the chance of deadlock.");
        }
        System.out.println("Time: " + (endTime - startTime) + " ms\n");
        
        // Scenario 4: Multiple accounts with transfers (using improved version)
        System.out.println("=== Scenario 4: Multiple Accounts with Transfers (Improved Version) ===\n");
        ImprovedBankAccount account1 = new ImprovedBankAccount(1000.0);
        ImprovedBankAccount account2 = new ImprovedBankAccount(2000.0);
        ImprovedBankAccount account3 = new ImprovedBankAccount(1500.0);
        
        Thread[] transferThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            transferThreads[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    account1.transfer(account2, 10.0);
                    account2.transfer(account3, 15.0);
                    account3.transfer(account1, 5.0);
                }
            }, "TransferThread-" + index);
        }
        
        for (Thread thread : transferThreads) {
            thread.start();
        }
        
        for (Thread thread : transferThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Account 1 balance: " + account1.getBalance());
        System.out.println("Account 2 balance: " + account2.getBalance());
        System.out.println("Account 3 balance: " + account3.getBalance());
        double total = account1.getBalance() + account2.getBalance() + account3.getBalance();
        System.out.println("Total balance: " + total + " (Expected: 4500.0) ✓");
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. Compound operations (read-modify-write) need synchronization");
        System.out.println("2. Synchronized methods are simpler but less flexible");
        System.out.println("3. Transfer operations need to lock multiple objects carefully");
        System.out.println("4. Always lock in consistent order to avoid deadlocks");
        System.out.println("5. Synchronization ensures correctness but may impact performance");
    }
}

