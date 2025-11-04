package concurrency.phase1;

/**
 * Exercise 1.5: Use interrupt() to stop a thread gracefully
 * 
 * Learning Objectives:
 * - Understand how to interrupt threads
 * - Learn to handle InterruptedException properly
 * - See the difference between interrupt() and stop() (deprecated)
 * - Implement graceful thread termination
 * - Understand interrupt status and how to check it
 */
public class Exercise1_5_ThreadInterrupt {
    
    /**
     * Thread that does long-running work
     * This version does NOT handle interrupts properly
     */
    static class NonInterruptibleWorker implements Runnable {
        private final String name;
        
        public NonInterruptibleWorker(String name) {
            this.name = name;
        }
        
        @Override
        public void run() {
            System.out.println(name + " started");
            // This thread doesn't check for interrupts - it will run to completion
            for (int i = 0; i < 100; i++) {
                System.out.println(name + ": " + i);
                try {
                    Thread.sleep(100); // Sleep doesn't check interrupt in this context
                } catch (InterruptedException e) {
                    // Even if we catch it, we're not doing anything with it
                    System.out.println(name + " caught InterruptedException but continuing...");
                }
            }
            System.out.println(name + " finished");
        }
    }
    
    /**
     * Thread that handles interrupts properly
     */
    static class InterruptibleWorker implements Runnable {
        private final String name;
        private final int maxIterations;
        
        public InterruptibleWorker(String name, int maxIterations) {
            this.name = name;
            this.maxIterations = maxIterations;
        }
        
        @Override
        public void run() {
            System.out.println(name + " started");
            try {
                for (int i = 0; i < maxIterations; i++) {
                    // Check if thread has been interrupted
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println(name + " detected interrupt flag, cleaning up...");
                        break;
                    }
                    
                    System.out.println(name + ": " + i);
                    Thread.sleep(100); // This will throw InterruptedException if interrupted
                }
                System.out.println(name + " finished normally");
            } catch (InterruptedException e) {
                // Thread was interrupted during sleep
                System.out.println(name + " was interrupted during sleep");
                // Restore interrupt status
                Thread.currentThread().interrupt();
                System.out.println(name + " cleaning up and exiting");
            }
        }
    }
    
    /**
     * Thread that does CPU-intensive work
     * Demonstrates checking interrupt status in tight loops
     */
    static class CPUIntensiveWorker implements Runnable {
        private final String name;
        
        public CPUIntensiveWorker(String name) {
            this.name = name;
        }
        
        @Override
        public void run() {
            System.out.println(name + " started (CPU-intensive task)");
            long sum = 0;
            int iterations = 0;
            
            // Tight loop without sleep - need to check interrupt status
            while (!Thread.currentThread().isInterrupted()) {
                sum += iterations;
                iterations++;
                
                // Check interrupt status periodically (every 1000 iterations)
                if (iterations % 1000 == 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println(name + " interrupted at iteration " + iterations);
                        break;
                    }
                }
                
                // Prevent infinite loop for demo
                if (iterations > 100000) {
                    break;
                }
            }
            
            System.out.println(name + " stopped after " + iterations + " iterations");
            System.out.println(name + " sum: " + sum);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.5: Thread Interrupt ===\n");
        
        // Scenario 1: Non-interruptible thread (bad practice)
        System.out.println("Scenario 1: Non-interruptible thread (BAD PRACTICE)");
        System.out.println("This thread ignores interrupts - we'll interrupt it but it continues:\n");
        
        Thread nonInterruptible = new Thread(new NonInterruptibleWorker("NonInterruptible"));
        nonInterruptible.start();
        
        // Wait a bit, then try to interrupt
        try {
            Thread.sleep(500);
            System.out.println("\nMain thread: Attempting to interrupt NonInterruptible...");
            nonInterruptible.interrupt();
            
            // Wait for it to finish (it won't stop)
            nonInterruptible.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nNotice: NonInterruptible thread didn't stop when interrupted!\n");
        
        // Scenario 2: Proper interrupt handling
        System.out.println("=== Scenario 2: Proper interrupt handling ===\n");
        
        Thread interruptible = new Thread(new InterruptibleWorker("Interruptible", 20));
        interruptible.start();
        
        try {
            Thread.sleep(1000); // Let it run for 1 second
            System.out.println("\nMain thread: Interrupting Interruptible thread...");
            interruptible.interrupt();
            interruptible.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nInterruptible thread stopped gracefully!\n");
        
        // Scenario 3: Multiple interrupts
        System.out.println("=== Scenario 3: Multiple threads with timeout ===\n");
        
        Thread worker1 = new Thread(new InterruptibleWorker("Worker-1", 50));
        Thread worker2 = new Thread(new InterruptibleWorker("Worker-2", 50));
        Thread worker3 = new Thread(new InterruptibleWorker("Worker-3", 50));
        
        worker1.start();
        worker2.start();
        worker3.start();
        
        // Interrupt all threads after 2 seconds
        try {
            Thread.sleep(2000);
            System.out.println("\nMain thread: Interrupting all workers after 2 seconds...");
            worker1.interrupt();
            worker2.interrupt();
            worker3.interrupt();
            
            worker1.join();
            worker2.join();
            worker3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Scenario 4: CPU-intensive task with interrupt checking
        System.out.println("\n=== Scenario 4: CPU-intensive task with interrupt checking ===\n");
        
        Thread cpuWorker = new Thread(new CPUIntensiveWorker("CPU-Worker"));
        cpuWorker.start();
        
        try {
            Thread.sleep(100); // Let it run briefly
            System.out.println("\nMain thread: Interrupting CPU-intensive worker...");
            cpuWorker.interrupt();
            cpuWorker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Scenario 5: Interrupt status
        System.out.println("\n=== Scenario 5: Understanding interrupt status ===\n");
        
        Thread statusDemo = new Thread(() -> {
            System.out.println("Thread interrupt status: " + Thread.currentThread().isInterrupted());
            
            // Interrupt the thread itself
            Thread.currentThread().interrupt();
            System.out.println("After self-interrupt, status: " + Thread.currentThread().isInterrupted());
            
            // Check status (this does NOT clear the flag)
            boolean status1 = Thread.currentThread().isInterrupted();
            boolean status2 = Thread.currentThread().isInterrupted();
            System.out.println("After two isInterrupted() calls: " + status1 + ", " + status2);
            System.out.println("Flag is still set: " + Thread.currentThread().isInterrupted());
            
            // interrupted() clears the flag
            boolean cleared = Thread.interrupted();
            System.out.println("After interrupted() call: " + cleared);
            System.out.println("Flag is now: " + Thread.currentThread().isInterrupted());
        });
        
        statusDemo.start();
        try {
            statusDemo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. interrupt() sets a flag - it doesn't force thread to stop");
        System.out.println("2. Threads must check isInterrupted() or handle InterruptedException");
        System.out.println("3. Always restore interrupt status: Thread.currentThread().interrupt()");
        System.out.println("4. isInterrupted() checks but doesn't clear the flag");
        System.out.println("5. interrupted() checks AND clears the flag (static method)");
        System.out.println("6. Never use Thread.stop() - it's deprecated and dangerous!");
        System.out.println("7. Sleep, wait, and join throw InterruptedException when interrupted");
    }
}

