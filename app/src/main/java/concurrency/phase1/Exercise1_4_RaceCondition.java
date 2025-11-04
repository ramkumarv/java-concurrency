package concurrency.phase1;

/**
 * Exercise 1.4: Implement a counter with multiple threads (demonstrate race condition)
 * 
 * Learning Objectives:
 * - Understand what race conditions are
 * - See how unsynchronized access to shared data causes problems
 * - Observe non-deterministic behavior
 * - Learn why synchronization is needed
 */
public class Exercise1_4_RaceCondition {
    
    /**
     * Unsafe counter - demonstrates race condition
     */
    static class UnsafeCounter {
        private int count = 0;
        
        // This method is NOT thread-safe!
        public void increment() {
            // This is NOT atomic! It consists of:
            // 1. Read count
            // 2. Increment count
            // 3. Write count
            // Multiple threads can interleave these steps
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Thread that increments counter multiple times
     */
    static class Incrementer implements Runnable {
        private final UnsafeCounter counter;
        private final int increments;
        private final String name;
        
        public Incrementer(UnsafeCounter counter, int increments, String name) {
            this.counter = counter;
            this.increments = increments;
            this.name = name;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < increments; i++) {
                counter.increment();
                // Small sleep to increase chance of interleaving
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println(name + " finished incrementing");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.4: Race Condition Demonstration ===\n");
        
        // Demonstrate race condition
        System.out.println("Scenario 1: Unsafe counter with race condition");
        System.out.println("Creating 5 threads, each incrementing counter 1000 times");
        System.out.println("Expected result: 5000");
        System.out.println("Actual result: (likely less than 5000 due to race condition)\n");
        
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        
        Thread t1 = new Thread(new Incrementer(unsafeCounter, 1000, "Thread-1"));
        Thread t2 = new Thread(new Incrementer(unsafeCounter, 1000, "Thread-2"));
        Thread t3 = new Thread(new Incrementer(unsafeCounter, 1000, "Thread-3"));
        Thread t4 = new Thread(new Incrementer(unsafeCounter, 1000, "Thread-4"));
        Thread t5 = new Thread(new Incrementer(unsafeCounter, 1000, "Thread-5"));
        
        long startTime = System.currentTimeMillis();
        
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        
        // Wait for all threads to complete
        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
            t5.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted");
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nFinal count: " + unsafeCounter.getCount());
        System.out.println("Expected: 5000");
        System.out.println("Difference: " + (5000 - unsafeCounter.getCount()) + " (lost increments!)");
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
        
        // Demonstrate with more iterations to make race condition more obvious
        System.out.println("\n=== Scenario 2: More iterations, more visible race condition ===\n");
        
        UnsafeCounter counter2 = new UnsafeCounter();
        System.out.println("10 threads, each incrementing 10,000 times");
        System.out.println("Expected: 100,000\n");
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(new Incrementer(counter2, 10000, "Thread-" + (i + 1)));
        }
        
        startTime = System.currentTimeMillis();
        
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
        
        endTime = System.currentTimeMillis();
        
        System.out.println("\nFinal count: " + counter2.getCount());
        System.out.println("Expected: 100,000");
        System.out.println("Lost increments: " + (100000 - counter2.getCount()));
        System.out.println("Time taken: " + (endTime - startTime) + " ms");
        
        // Explain what's happening
        System.out.println("\n=== Why Race Condition Occurs ===");
        System.out.println("The increment operation (count++) is NOT atomic:");
        System.out.println("  1. Thread A reads count = 5");
        System.out.println("  2. Thread B reads count = 5  (before A writes)");
        System.out.println("  3. Thread A increments to 6 and writes");
        System.out.println("  4. Thread B increments to 6 and writes");
        System.out.println("Result: Both threads incremented, but count only went from 5 to 6!");
        System.out.println("Expected: count should be 7 (5 + 1 + 1)");
        System.out.println("\nThis is why we need synchronization (covered in Phase 2)!");
    }
}

