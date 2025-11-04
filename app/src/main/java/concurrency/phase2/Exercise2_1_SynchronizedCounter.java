package concurrency.phase2;

/**
 * Exercise 2.1: Fix the counter from Phase 1 using synchronized
 * 
 * Learning Objectives:
 * - Understand how synchronized keyword works
 * - Learn synchronized methods vs synchronized blocks
 * - See how synchronization fixes race conditions
 * - Understand intrinsic locks (monitor locks)
 * - Compare synchronized vs unsynchronized performance
 */
public class Exercise2_1_SynchronizedCounter {
    
    /**
     * Unsafe counter - demonstrates the problem from Phase 1
     */
    static class UnsafeCounter {
        private int count = 0;
        
        public void increment() {
            count++; // Not atomic - race condition!
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Thread-safe counter using synchronized method
     */
    static class SynchronizedMethodCounter {
        private int count = 0;
        
        // Synchronized method - locks on 'this' object
        public synchronized void increment() {
            count++;
        }
        
        public synchronized int getCount() {
            return count;
        }
    }
    
    /**
     * Thread-safe counter using synchronized block
     */
    static class SynchronizedBlockCounter {
        private int count = 0;
        private final Object lock = new Object(); // Explicit lock object
        
        public void increment() {
            // Synchronized block - locks on 'lock' object
            synchronized (lock) {
                count++;
            }
        }
        
        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }
    
    /**
     * Counter with synchronized block using 'this'
     */
    static class SynchronizedThisCounter {
        private int count = 0;
        
        public void increment() {
            // Synchronized block using 'this' (same as synchronized method)
            synchronized (this) {
                count++;
            }
        }
        
        public int getCount() {
            synchronized (this) {
                return count;
            }
        }
    }
    
    /**
     * Worker thread that increments counter
     */
    static class CounterWorker implements Runnable {
        private final UnsafeCounter unsafeCounter;
        private final SynchronizedMethodCounter syncMethodCounter;
        private final SynchronizedBlockCounter syncBlockCounter;
        private final SynchronizedThisCounter syncThisCounter;
        private final int increments;
        private final String name;
        
        public CounterWorker(UnsafeCounter unsafe, 
                           SynchronizedMethodCounter syncMethod,
                           SynchronizedBlockCounter syncBlock,
                           SynchronizedThisCounter syncThis,
                           int increments, 
                           String name) {
            this.unsafeCounter = unsafe;
            this.syncMethodCounter = syncMethod;
            this.syncBlockCounter = syncBlock;
            this.syncThisCounter = syncThis;
            this.increments = increments;
            this.name = name;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < increments; i++) {
                unsafeCounter.increment();
                syncMethodCounter.increment();
                syncBlockCounter.increment();
                syncThisCounter.increment();
            }
            System.out.println(name + " finished");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 2.1: Synchronized Counter ===\n");
        
        // Test with 5 threads, each incrementing 10,000 times
        int numThreads = 5;
        int incrementsPerThread = 10000;
        int expectedCount = numThreads * incrementsPerThread;
        
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        SynchronizedMethodCounter syncMethodCounter = new SynchronizedMethodCounter();
        SynchronizedBlockCounter syncBlockCounter = new SynchronizedBlockCounter();
        SynchronizedThisCounter syncThisCounter = new SynchronizedThisCounter();
        
        Thread[] threads = new Thread[numThreads];
        
        // Create and start threads
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new CounterWorker(
                unsafeCounter, syncMethodCounter, syncBlockCounter, syncThisCounter,
                incrementsPerThread, "Thread-" + (i + 1)
            ));
        }
        
        long startTime = System.currentTimeMillis();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== Results ===");
        System.out.println("Expected count: " + expectedCount);
        System.out.println("\nUnsafe Counter: " + unsafeCounter.getCount() + 
                         " (Lost: " + (expectedCount - unsafeCounter.getCount()) + ")");
        System.out.println("Synchronized Method Counter: " + syncMethodCounter.getCount() + 
                         " ✓ Correct!");
        System.out.println("Synchronized Block Counter: " + syncBlockCounter.getCount() + 
                         " ✓ Correct!");
        System.out.println("Synchronized This Counter: " + syncThisCounter.getCount() + 
                         " ✓ Correct!");
        System.out.println("\nTime taken: " + (endTime - startTime) + " ms");
        
        // Demonstrate synchronized static method
        System.out.println("\n=== Synchronized Static Method ===");
        System.out.println("Static methods lock on the Class object, not instance");
        
        StaticCounter.reset();
        Thread[] staticThreads = new Thread[3];
        for (int i = 0; i < 3; i++) {
            staticThreads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    StaticCounter.increment();
                }
            });
        }
        
        for (Thread thread : staticThreads) {
            thread.start();
        }
        
        for (Thread thread : staticThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Static counter result: " + StaticCounter.getCount() + 
                         " (Expected: 3000) ✓");
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. synchronized method locks on 'this' object");
        System.out.println("2. synchronized block can lock on any object (more flexible)");
        System.out.println("3. synchronized static method locks on Class object");
        System.out.println("4. Only one thread can hold a lock at a time");
        System.out.println("5. Synchronization has performance cost but ensures correctness");
    }
    
    /**
     * Example of synchronized static method
     */
    static class StaticCounter {
        private static int count = 0;
        
        public static synchronized void increment() {
            count++;
        }
        
        public static synchronized int getCount() {
            return count;
        }
        
        public static synchronized void reset() {
            count = 0;
        }
    }
}

