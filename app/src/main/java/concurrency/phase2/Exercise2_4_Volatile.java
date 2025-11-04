package concurrency.phase2;

/**
 * Exercise 2.4: Use volatile to fix visibility issues
 * 
 * Learning Objectives:
 * - Understand the Java Memory Model (JMM)
 * - Learn about visibility guarantees
 * - See when volatile is appropriate
 * - Understand the difference between volatile and synchronized
 * - Learn about happens-before relationships
 */
public class Exercise2_4_Volatile {
    
    /**
     * Counter without volatile - visibility problem
     */
    static class NonVolatileFlag {
        private boolean flag = false; // NOT volatile
        
        public void setFlag(boolean value) {
            this.flag = value;
        }
        
        public boolean getFlag() {
            return flag;
        }
    }
    
    /**
     * Counter with volatile - fixes visibility
     */
    static class VolatileFlag {
        private volatile boolean flag = false; // volatile ensures visibility
        
        public void setFlag(boolean value) {
            this.flag = value;
        }
        
        public boolean getFlag() {
            return flag;
        }
    }
    
    /**
     * Counter demonstrating volatile with non-atomic operations
     */
    static class VolatileCounter {
        private volatile int count = 0;
        
        public void increment() {
            // PROBLEM: volatile doesn't make this atomic!
            // This is read-modify-write, still needs synchronization
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    /**
     * Proper use of volatile - single writer, multiple readers
     */
    static class VolatilePublisher {
        private volatile String message = "Initial";
        private volatile int version = 0;
        
        // Single writer thread
        public void publish(String newMessage) {
            version++;
            message = newMessage; // volatile write - visible to all readers immediately
        }
        
        // Multiple reader threads
        public String read() {
            return message; // volatile read - sees latest value
        }
        
        public int getVersion() {
            return version;
        }
    }
    
    /**
     * Example where volatile is NOT enough
     */
    static class SharedCounter {
        private volatile int count = 0;
        
        public void increment() {
            // Even with volatile, this is NOT thread-safe!
            // Multiple threads can still read the same value and increment
            count++;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 2.4: Volatile Keyword ===\n");
        
        // Scenario 1: Visibility problem without volatile
        System.out.println("Scenario 1: Visibility Problem (Non-Volatile Flag)");
        System.out.println("This may hang because the reader thread may not see the flag change!\n");
        
        NonVolatileFlag nonVolatile = new NonVolatileFlag();
        
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100); // Give reader time to start
                System.out.println("Writer: Setting flag to true");
                nonVolatile.setFlag(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-Thread");
        
        Thread reader = new Thread(() -> {
            int iterations = 0;
            while (!nonVolatile.getFlag()) {
                iterations++;
                // Without volatile, this loop may never see the flag change!
                if (iterations % 1000000 == 0) {
                    System.out.println("Reader: Still waiting... (iterations: " + iterations + ")");
                }
                if (iterations > 100000000) {
                    System.out.println("Reader: Giving up after " + iterations + " iterations");
                    break;
                }
            }
            if (nonVolatile.getFlag()) {
                System.out.println("Reader: Finally saw flag change!");
            }
        }, "Reader-Thread");
        
        reader.start();
        writer.start();
        
        try {
            reader.join(2000); // Wait max 2 seconds
            writer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (reader.isAlive()) {
            System.out.println("⚠ Reader thread is still running - visibility problem!");
            System.out.println("The flag may have been set, but reader didn't see it.\n");
        } else {
            System.out.println("Reader completed (got lucky or JVM optimized)\n");
        }
        
        // Scenario 2: Volatile fixes visibility
        System.out.println("=== Scenario 2: Volatile Fixes Visibility ===\n");
        
        VolatileFlag volatileFlag = new VolatileFlag();
        
        Thread writer2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                System.out.println("Writer: Setting volatile flag to true");
                volatileFlag.setFlag(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-Thread");
        
        Thread reader2 = new Thread(() -> {
            System.out.println("Reader: Waiting for flag...");
            while (!volatileFlag.getFlag()) {
                // Volatile ensures this loop will see the change
                Thread.yield(); // Give CPU to other threads
            }
            System.out.println("Reader: Flag changed detected immediately! ✓");
        }, "Reader-Thread");
        
        reader2.start();
        writer2.start();
        
        try {
            reader2.join();
            writer2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Scenario 3: Volatile is NOT enough for atomicity
        System.out.println("\n=== Scenario 3: Volatile Doesn't Ensure Atomicity ===\n");
        
        VolatileCounter volatileCounter = new VolatileCounter();
        SharedCounter sharedCounter = new SharedCounter();
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    volatileCounter.increment();
                    sharedCounter.increment();
                }
            }, "CounterThread-" + i);
        }
        
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
        
        System.out.println("Volatile counter: " + volatileCounter.getCount() + 
                         " (Expected: 5000, Lost: " + (5000 - volatileCounter.getCount()) + ")");
        System.out.println("Shared counter: " + sharedCounter.getCount() + 
                         " (Expected: 5000, Lost: " + (5000 - sharedCounter.getCount()) + ")");
        System.out.println("⚠ Both have race conditions - volatile doesn't fix atomicity!\n");
        
        // Scenario 4: Proper use of volatile - publish-subscribe pattern
        System.out.println("=== Scenario 4: Proper Use of Volatile (Publish-Subscribe) ===\n");
        
        VolatilePublisher publisher = new VolatilePublisher();
        
        // Single writer
        Thread writer3 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try {
                    Thread.sleep(200);
                    String message = "Message " + i;
                    publisher.publish(message);
                    System.out.println("Publisher: Published '" + message + "'");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Publisher");
        
        // Multiple readers
        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int readerId = i + 1;
            readers[i] = new Thread(() -> {
                int lastVersion = -1;
                while (publisher.getVersion() < 5) {
                    int currentVersion = publisher.getVersion();
                    String message = publisher.read();
                    
                    if (currentVersion != lastVersion) {
                        System.out.println("Reader-" + readerId + ": Read '" + message + 
                                         "' (version " + currentVersion + ")");
                        lastVersion = currentVersion;
                    }
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Reader-" + (i + 1));
        }
        
        writer3.start();
        for (Thread readerThread : readers) {
            readerThread.start();
        }
        
        try {
            writer3.join();
            for (Thread readerThread : readers) {
                readerThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. volatile ensures visibility, NOT atomicity");
        System.out.println("2. volatile read happens-before volatile write");
        System.out.println("3. Use volatile for flags, status indicators (single writer)");
        System.out.println("4. Don't use volatile for read-modify-write operations");
        System.out.println("5. volatile is lighter than synchronized (no mutual exclusion)");
        System.out.println("6. Proper use: publish-subscribe, status flags, shutdown flags");
        System.out.println("7. When in doubt, use synchronized - it provides both visibility and atomicity");
    }
}

