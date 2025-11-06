package concurrency.phase1;

/**
 * Exercise 1.6: Thread Lifecycle States
 * 
 * Learning Objectives:
 * - Understand all thread states in Java
 * - See how threads transition between states
 * - Learn to observe thread states programmatically
 * - Understand when each state occurs
 * - Learn Thread.State enum
 */
public class Exercise1_6_ThreadLifecycle {
    
    /**
     * Thread that demonstrates different states
     */
    static class StateDemonstrator implements Runnable {
        private final String name;
        private final Object lock;
        
        public StateDemonstrator(String name, Object lock) {
            this.name = name;
            this.lock = lock;
        }
        
        @Override
        public void run() {
            System.out.println(name + " is RUNNABLE (running)");
            
            try {
                // Transition to TIMED_WAITING
                System.out.println(name + " going to sleep (TIMED_WAITING)...");
                Thread.sleep(2000);
                
                // Transition to WAITING (waiting on lock)
                synchronized (lock) {
                    System.out.println(name + " acquired lock, now WAITING...");
                    lock.wait(); // WAITING state
                }
            } catch (InterruptedException e) {
                System.err.println(name + " was interrupted");
                Thread.currentThread().interrupt();
            }
            
            System.out.println(name + " finished (TERMINATED)");
        }
    }
    
    /**
     * Monitor thread that observes and reports state changes
     */
    static class StateMonitor implements Runnable {
        private final Thread targetThread;
        private final String monitorName;
        
        public StateMonitor(Thread targetThread, String monitorName) {
            this.targetThread = targetThread;
            this.monitorName = monitorName;
        }
        
        @Override
        public void run() {
            while (targetThread.isAlive()) {
                Thread.State state = targetThread.getState();
                System.out.println(monitorName + " observes: " + 
                    targetThread.getName() + " is in " + state + " state");
                
                try {
                    Thread.sleep(500); // Check every 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println(monitorName + " observes: " + 
                targetThread.getName() + " is now TERMINATED");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 1.6: Thread Lifecycle States ===\n");
        
        // Show all possible thread states
        System.out.println("=== Java Thread States ===");
        System.out.println("1. NEW - Thread created but not started");
        System.out.println("2. RUNNABLE - Thread is executing or ready to execute");
        System.out.println("3. BLOCKED - Thread waiting for monitor lock");
        System.out.println("4. WAITING - Thread waiting indefinitely");
        System.out.println("5. TIMED_WAITING - Thread waiting with timeout");
        System.out.println("6. TERMINATED - Thread has completed execution\n");
        
        // Scenario 1: NEW state
        System.out.println("=== Scenario 1: NEW State ===\n");
        Thread newThread = new Thread(() -> {
            System.out.println("This thread never runs");
        }, "NewThread");
        
        System.out.println("After creating thread: " + newThread.getState());
        System.out.println("Thread name: " + newThread.getName());
        System.out.println("Thread is alive: " + newThread.isAlive() + "\n");
        
        // Scenario 2: RUNNABLE state
        System.out.println("=== Scenario 2: RUNNABLE State ===\n");
        Thread runnableThread = new Thread(() -> {
            System.out.println("Thread running...");
            for (int i = 0; i < 5; i++) {
                System.out.println("  Working: " + i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "RunnableThread");
        
        System.out.println("Before start: " + runnableThread.getState());
        runnableThread.start();
        System.out.println("After start: " + runnableThread.getState());
        while (runnableThread.isAlive()) {
            System.out.println("RunnableThread is still running: " + runnableThread.getState());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
        }
        
        try {
            runnableThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("After completion: " + runnableThread.getState() + "\n");
        
        // Scenario 3: TIMED_WAITING state
        System.out.println("=== Scenario 3: TIMED_WAITING State ===\n");
        Thread timedWaitingThread = new Thread(() -> {
            try {
                System.out.println("Thread going to sleep for 2 seconds...");
                Thread.sleep(2000); // TIMED_WAITING
                System.out.println("Thread woke up!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "TimedWaitingThread");
        
        timedWaitingThread.start();
        
        // Monitor the state
        try {
            Thread.sleep(100); // Give it time to start
            System.out.println("State while sleeping: " + timedWaitingThread.getState());
            Thread.sleep(100);
            System.out.println("State still: " + timedWaitingThread.getState());
            
            timedWaitingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final state: " + timedWaitingThread.getState() + "\n");
        
        // Scenario 4: WAITING state
        System.out.println("=== Scenario 4: WAITING State ===\n");
        Object lock = new Object();
        
        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("Thread has lock, now waiting...");
                    lock.wait(); // WAITING state
                    System.out.println("Thread notified and resumed!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "WaitingThread");
        
        waitingThread.start();
        
        try {
            Thread.sleep(100);
            System.out.println("State while waiting: " + waitingThread.getState());
            
            // Notify the waiting thread
            Thread.sleep(500);
            synchronized (lock) {
                System.out.println("Notifying waiting thread...");
                lock.notify();
            }
            
            Thread.sleep(100);
            System.out.println("State after notify: " + waitingThread.getState());
            
            waitingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final state: " + waitingThread.getState() + "\n");
        
        // Scenario 5: BLOCKED state
        System.out.println("=== Scenario 5: BLOCKED State ===\n");
        Object sharedLock = new Object();
        
        Thread thread1 = new Thread(() -> {
            synchronized (sharedLock) {
                System.out.println("Thread-1 acquired lock");
                try {
                    Thread.sleep(2000); // Hold lock for 2 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Thread-1 releasing lock");
            }
        }, "Thread-1");
        
        Thread thread2 = new Thread(() -> {
            System.out.println("Thread-2 trying to acquire lock...");
            synchronized (sharedLock) {
                System.out.println("Thread-2 acquired lock");
            }
        }, "Thread-2");
        
        thread1.start();
        
        try {
            Thread.sleep(100); // Give thread1 time to acquire lock
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        thread2.start();
        
        try {
            Thread.sleep(100); // Give thread2 time to try acquiring lock
            System.out.println("Thread-2 state while waiting for lock: " + thread2.getState());
            
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Thread-2 final state: " + thread2.getState() + "\n");
        
        // Scenario 6: TERMINATED state
        System.out.println("=== Scenario 6: TERMINATED State ===\n");
        Thread terminatedThread = new Thread(() -> {
            System.out.println("Thread doing work...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread finished work");
        }, "TerminatedThread");
        
        System.out.println("Before start: " + terminatedThread.getState());
        terminatedThread.start();
        
        try {
            terminatedThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("After join: " + terminatedThread.getState());
        System.out.println("Is alive: " + terminatedThread.isAlive() + "\n");
        
        // Scenario 7: Complete lifecycle observation
        System.out.println("=== Scenario 7: Complete Lifecycle Observation ===\n");
        Object monitorLock = new Object();
        
        Thread lifecycleThread = new Thread(new StateDemonstrator("LifecycleThread", monitorLock), 
            "LifecycleThread");
        
        Thread monitor = new Thread(new StateMonitor(lifecycleThread, "Monitor"), "Monitor");
        
        System.out.println("Initial state: " + lifecycleThread.getState());
        
        lifecycleThread.start();
        monitor.start();
        
        try {
            Thread.sleep(2500); // Let it sleep and wait
            
            // Notify the waiting thread
            synchronized (monitorLock) {
                monitorLock.notify();
            }
            
            lifecycleThread.join();
            monitor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== State Transition Diagram ===");
        System.out.println("NEW → RUNNABLE → {BLOCKED | WAITING | TIMED_WAITING} → RUNNABLE → TERMINATED");
        System.out.println("\nKey Points:");
        System.out.println("- NEW: Thread created with 'new Thread()' but start() not called");
        System.out.println("- RUNNABLE: Thread is executing or ready to execute");
        System.out.println("- BLOCKED: Waiting to acquire monitor lock (synchronized)");
        System.out.println("- WAITING: Waiting indefinitely (wait(), join() without timeout)");
        System.out.println("- TIMED_WAITING: Waiting with timeout (sleep(), wait(timeout), join(timeout))");
        System.out.println("- TERMINATED: Thread has completed run() method");
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. getState() returns current Thread.State enum value");
        System.out.println("2. States are observable but cannot be directly set");
        System.out.println("3. isAlive() returns true if thread is RUNNABLE or BLOCKED/WAITING/TIMED_WAITING");
        System.out.println("4. Thread states transition automatically based on operations");
        System.out.println("5. Understanding states helps debug thread-related issues");
    }
}

