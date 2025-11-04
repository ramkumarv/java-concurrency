package concurrency.phase1;

/**
 * Exercise 1.3: Use join() to ensure threads complete in order
 * 
 * Learning Objectives:
 * - Understand how join() works
 * - Learn to coordinate thread execution
 * - See the difference between concurrent and sequential execution
 * - Understand blocking behavior of join()
 */
public class Exercise1_3_ThreadJoin {
    
    static class WorkerThread implements Runnable {
        private final String name;
        private final int workDuration; // milliseconds
        
        public WorkerThread(String name, int workDuration) {
            this.name = name;
            this.workDuration = workDuration;
        }
        
        @Override
        public void run() {
            System.out.println(name + " started working");
            try {
                // Simulate work
                Thread.sleep(workDuration);
                System.out.println(name + " finished working");
            } catch (InterruptedException e) {
                System.err.println(name + " was interrupted");
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.3: Thread Join ===\n");
        
        // Scenario 1: Without join() - threads run concurrently
        System.out.println("Scenario 1: Without join() - threads run concurrently");
        System.out.println("Notice how 'All workers finished' may print before threads complete:\n");
        
        Thread worker1 = new Thread(new WorkerThread("Worker-1", 1000));
        Thread worker2 = new Thread(new WorkerThread("Worker-2", 1500));
        Thread worker3 = new Thread(new WorkerThread("Worker-3", 800));
        
        worker1.start();
        worker2.start();
        worker3.start();
        
        // Main thread doesn't wait - may print this before workers finish
        System.out.println("All workers started (but may not be finished yet)\n");
        
        // Wait a bit to see the problem
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Main thread continued without waiting...\n");
        
        // Wait for all threads to actually complete
        try {
            worker1.join();
            worker2.join();
            worker3.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted while waiting");
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nAll workers actually finished now!\n");
        
        // Scenario 2: With join() - sequential-like execution
        System.out.println("=== Scenario 2: Using join() to ensure sequential completion ===\n");
        
        Thread seq1 = new Thread(new WorkerThread("Sequential-1", 500));
        Thread seq2 = new Thread(new WorkerThread("Sequential-2", 500));
        Thread seq3 = new Thread(new WorkerThread("Sequential-3", 500));
        
        // Start and wait for each thread sequentially
        seq1.start();
        try {
            seq1.join(); // Wait for seq1 to complete
            System.out.println("seq1 completed, starting seq2\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        seq2.start();
        try {
            seq2.join(); // Wait for seq2 to complete
            System.out.println("seq2 completed, starting seq3\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        seq3.start();
        try {
            seq3.join(); // Wait for seq3 to complete
            System.out.println("seq3 completed\n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("All sequential workers completed in order!");
        
        // Scenario 3: join() with timeout
        System.out.println("\n=== Scenario 3: join() with timeout ===\n");
        
        Thread longWorker = new Thread(new WorkerThread("Long-Worker", 5000));
        longWorker.start();
        
        try {
            // Wait maximum 2 seconds for the thread to complete
            longWorker.join(2000);
            
            if (longWorker.isAlive()) {
                System.out.println("Long-Worker is still running after 2 seconds");
                System.out.println("Interrupting Long-Worker...");
                longWorker.interrupt();
                longWorker.join(); // Wait for it to handle interrupt
            } else {
                System.out.println("Long-Worker completed within timeout");
            }
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted");
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. join() blocks the calling thread until the target thread completes");
        System.out.println("2. join() is essential for coordinating thread completion");
        System.out.println("3. join(timeout) allows waiting with a maximum time limit");
        System.out.println("4. Without join(), main thread may exit before worker threads complete");
    }
}

