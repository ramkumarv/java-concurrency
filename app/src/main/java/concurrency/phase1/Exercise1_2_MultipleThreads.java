package concurrency.phase1;

/**
 * Exercise 1.2: Create multiple threads, each printing a different letter
 * 
 * Learning Objectives:
 * - Understand how multiple threads execute concurrently
 * - See non-deterministic thread scheduling
 * - Learn about thread naming
 */
public class Exercise1_2_MultipleThreads {
    
    static class LetterPrinter implements Runnable {
        private final char letter;
        private final int count;
        
        public LetterPrinter(char letter, int count) {
            this.letter = letter;
            this.count = count;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < count; i++) {
                System.out.print(letter);
                try {
                    Thread.sleep(50); // Small delay to see interleaving
                } catch (InterruptedException e) {
                    System.err.println("Thread " + Thread.currentThread().getName() + " interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("\nThread " + Thread.currentThread().getName() + " finished printing " + letter);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.2: Multiple Threads ===\n");
        System.out.println("Creating 5 threads, each printing a different letter");
        System.out.println("Notice how the output is interleaved (non-deterministic):\n");
        
        // Create multiple threads
        Thread threadA = new Thread(new LetterPrinter('A', 10), "Thread-A");
        Thread threadB = new Thread(new LetterPrinter('B', 10), "Thread-B");
        Thread threadC = new Thread(new LetterPrinter('C', 10), "Thread-C");
        Thread threadD = new Thread(new LetterPrinter('D', 10), "Thread-D");
        Thread threadE = new Thread(new LetterPrinter('E', 10), "Thread-E");
        
        // Start all threads
        threadA.start();
        threadB.start();
        threadC.start();
        threadD.start();
        threadE.start();
        
        // Wait for all threads to complete
        try {
            threadA.join();
            threadB.join();
            threadC.join();
            threadD.join();
            threadE.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nAll threads completed!");
        System.out.println("\nNote: The order of output is non-deterministic.");
        System.out.println("Each run may produce different interleaving of letters.");
    }
}

