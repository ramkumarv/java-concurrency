package concurrency.phase1;

/**
 * Exercise 1.1: Create a simple thread that prints numbers 1-10
 * 
 * Learning Objectives:
 * - Understand how to create a thread by extending Thread class
 * - Learn the difference between start() and run()
 * - See how threads execute concurrently
 */
public class Exercise1_1_SimpleThread {
    
    /**
     * Approach 1: Extending Thread class
     */
    static class NumberPrinter extends Thread {
        @Override
        public void run() {
            for (int i = 1; i <= 10; i++) {
                System.out.println(Thread.currentThread().getName() + ": " + i);
                try {
                    Thread.sleep(100); // Small delay to see thread execution
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                }
            }
        }
    }
    
    /**
     * Approach 2: Using Runnable interface (Recommended)
     */
    static class NumberPrinterRunnable implements Runnable {
        @Override
        public void run() {
            for (int i = 1; i <= 10; i++) {
                System.out.println(Thread.currentThread().getName() + ": " + i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.1: Simple Thread ===\n");
        
        // Approach 1: Extending Thread
        System.out.println("Approach 1: Extending Thread class");
        NumberPrinter thread1 = new NumberPrinter();
        thread1.setName("NumberPrinter-Thread");
        thread1.start(); // Start the thread
        
        // Approach 2: Using Runnable interface
        System.out.println("\nApproach 2: Using Runnable interface");
        Thread thread2 = new Thread(new NumberPrinterRunnable(), "Runnable-Thread");
        thread2.start();
        
        // Approach 3: Using Lambda expression (Modern Java)
        System.out.println("\nApproach 3: Using Lambda expression");
        Thread thread3 = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                System.out.println(Thread.currentThread().getName() + ": " + i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Lambda-Thread");
        thread3.start();
        
        // Wait for all threads to complete
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nAll threads completed!");
        
        // Important: Don't call run() directly!
        System.out.println("\n=== Demonstrating start() vs run() ===");
        System.out.println("Calling run() directly (runs in main thread):");
        new NumberPrinter().run(); // This runs in the main thread, not a new thread!
        
        System.out.println("\nCalling start() (creates new thread):");
        new NumberPrinter().start(); // This creates a new thread
    }
}

