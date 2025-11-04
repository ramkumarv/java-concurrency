package concurrency.phase2;

/**
 * Exercise 2.5: Implement a producer-consumer pattern with synchronized blocks
 * 
 * Learning Objectives:
 * - Understand the producer-consumer pattern
 * - Learn wait() and notify()/notifyAll()
 * - See how to coordinate multiple threads
 * - Understand when to use notify() vs notifyAll()
 * - Learn about spurious wakeups and how to handle them
 */
public class Exercise2_5_ProducerConsumer {
    
    /**
     * Simple bounded buffer using synchronized blocks
     * This is the classic producer-consumer pattern
     */
    static class BoundedBuffer {
        private final int[] buffer;
        private int size = 0;
        private int putIndex = 0;
        private int takeIndex = 0;
        
        public BoundedBuffer(int capacity) {
            this.buffer = new int[capacity];
        }
        
        public synchronized void put(int value) throws InterruptedException {
            // Wait while buffer is full
            while (size == buffer.length) {
                System.out.println(Thread.currentThread().getName() + 
                    " waiting - buffer is full (size: " + size + ")");
                wait(); // Releases lock and waits for notification
            }
            
            // Buffer has space, add item
            buffer[putIndex] = value;
            putIndex = (putIndex + 1) % buffer.length;
            size++;
            
            System.out.println(Thread.currentThread().getName() + 
                " produced: " + value + " (size: " + size + ")");
            
            notifyAll(); // Notify all waiting threads (consumers and producers)
        }
        
        public synchronized int take() throws InterruptedException {
            // Wait while buffer is empty
            while (size == 0) {
                System.out.println(Thread.currentThread().getName() + 
                    " waiting - buffer is empty");
                wait(); // Releases lock and waits for notification
            }
            
            // Buffer has items, remove one
            int value = buffer[takeIndex];
            takeIndex = (takeIndex + 1) % buffer.length;
            size--;
            
            System.out.println(Thread.currentThread().getName() + 
                " consumed: " + value + " (size: " + size + ")");
            
            notifyAll(); // Notify all waiting threads
            
            return value;
        }
        
        public synchronized int getSize() {
            return size;
        }
        
        public synchronized int getCapacity() {
            return buffer.length;
        }
    }
    
    /**
     * Producer thread
     */
    static class Producer implements Runnable {
        private final BoundedBuffer buffer;
        private final int itemsToProduce;
        private final int producerId;
        
        public Producer(BoundedBuffer buffer, int itemsToProduce, int producerId) {
            this.buffer = buffer;
            this.itemsToProduce = itemsToProduce;
            this.producerId = producerId;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 0; i < itemsToProduce; i++) {
                    int value = producerId * 1000 + i;
                    buffer.put(value);
                    Thread.sleep(50); // Simulate production time
                }
                System.out.println("Producer-" + producerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Producer-" + producerId + " interrupted");
            }
        }
    }
    
    /**
     * Consumer thread
     */
    static class Consumer implements Runnable {
        private final BoundedBuffer buffer;
        private final int itemsToConsume;
        private final int consumerId;
        
        public Consumer(BoundedBuffer buffer, int itemsToConsume, int consumerId) {
            this.buffer = buffer;
            this.itemsToConsume = itemsToConsume;
            this.consumerId = consumerId;
        }
        
        @Override
        public void run() {
            try {
                int sum = 0;
                for (int i = 0; i < itemsToConsume; i++) {
                    int value = buffer.take();
                    sum += value;
                    Thread.sleep(30); // Simulate consumption time
                }
                System.out.println("Consumer-" + consumerId + " finished, sum: " + sum);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer-" + consumerId + " interrupted");
            }
        }
    }
    
    /**
     * Incorrect implementation - using notify() instead of notifyAll()
     * This can cause deadlock in some scenarios
     */
    static class IncorrectBoundedBuffer {
        private final int[] buffer;
        private int size = 0;
        private int putIndex = 0;
        private int takeIndex = 0;
        
        public IncorrectBoundedBuffer(int capacity) {
            this.buffer = new int[capacity];
        }
        
        public synchronized void put(int value) throws InterruptedException {
            while (size == buffer.length) {
                wait();
            }
            buffer[putIndex] = value;
            putIndex = (putIndex + 1) % buffer.length;
            size++;
            notify(); // PROBLEM: Only notifies one thread, might notify wrong type
        }
        
        public synchronized int take() throws InterruptedException {
            while (size == 0) {
                wait();
            }
            int value = buffer[takeIndex];
            takeIndex = (takeIndex + 1) % buffer.length;
            size--;
            notify(); // PROBLEM: Only notifies one thread, might notify wrong type
            return value;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 2.5: Producer-Consumer Pattern ===\n");
        
        // Scenario 1: Single producer, single consumer
        System.out.println("Scenario 1: Single Producer, Single Consumer");
        BoundedBuffer buffer1 = new BoundedBuffer(5);
        
        Thread producer1 = new Thread(new Producer(buffer1, 10, 1), "Producer-1");
        Thread consumer1 = new Thread(new Consumer(buffer1, 10, 1), "Consumer-1");
        
        producer1.start();
        consumer1.start();
        
        try {
            producer1.join();
            consumer1.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final buffer size: " + buffer1.getSize() + "\n");
        
        // Scenario 2: Multiple producers, multiple consumers
        System.out.println("=== Scenario 2: Multiple Producers, Multiple Consumers ===\n");
        
        BoundedBuffer buffer2 = new BoundedBuffer(10);
        
        int numProducers = 3;
        int numConsumers = 2;
        int itemsPerProducer = 20;
        int itemsPerConsumer = 30; // Total consumption = 60, production = 60
        
        Thread[] producers = new Thread[numProducers];
        Thread[] consumers = new Thread[numConsumers];
        
        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Thread(
                new Producer(buffer2, itemsPerProducer, i + 1), 
                "Producer-" + (i + 1)
            );
        }
        
        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Thread(
                new Consumer(buffer2, itemsPerConsumer, i + 1), 
                "Consumer-" + (i + 1)
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        // Start all consumers first (they'll wait)
        for (Thread consumer : consumers) {
            consumer.start();
        }
        
        // Small delay to let consumers start waiting
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start all producers
        for (Thread producer : producers) {
            producer.start();
        }
        
        // Wait for all threads
        for (Thread producer : producers) {
            try {
                producer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        for (Thread consumer : consumers) {
            try {
                consumer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nAll producers and consumers finished!");
        System.out.println("Final buffer size: " + buffer2.getSize());
        System.out.println("Time: " + (endTime - startTime) + " ms\n");
        
        // Scenario 3: Demonstrate wait() and notify()
        System.out.println("=== Scenario 3: Understanding wait() and notify() ===\n");
        
        System.out.println("Key points about wait() and notify():");
        System.out.println("1. wait() must be called from synchronized block");
        System.out.println("2. wait() releases the lock and waits");
        System.out.println("3. notify() wakes ONE waiting thread");
        System.out.println("4. notifyAll() wakes ALL waiting threads");
        System.out.println("5. Always use while loop, not if (handles spurious wakeups)");
        System.out.println("6. Use notifyAll() when multiple thread types waiting\n");
        
        // Demonstrate spurious wakeup handling
        System.out.println("=== Handling Spurious Wakeups ===");
        System.out.println("Always use while loop, not if statement:");
        System.out.println("  while (condition) { wait(); }  ✓ Correct");
        System.out.println("  if (condition) { wait(); }     ✗ Wrong (spurious wakeup risk)");
        System.out.println("\nSpurious wakeups can occur due to JVM implementation");
        System.out.println("The while loop re-checks the condition after wakeup\n");
        
        System.out.println("=== Key Takeaways ===");
        System.out.println("1. Producer-consumer pattern coordinates threads with shared buffer");
        System.out.println("2. wait() releases lock and waits for notification");
        System.out.println("3. notifyAll() is safer than notify() for multiple thread types");
        System.out.println("4. Always use while loop with wait() to handle spurious wakeups");
        System.out.println("5. Bounded buffer prevents memory issues from unlimited production");
        System.out.println("6. This pattern is fundamental to many concurrent systems");
    }
}

