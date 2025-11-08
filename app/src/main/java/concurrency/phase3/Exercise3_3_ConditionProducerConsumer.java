package concurrency.phase3;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Exercise 3.3: Use Condition to implement producer-consumer
 * 
 * Learning Objectives:
 * - Understand Condition interface vs wait()/notify()
 * - Learn to use multiple Condition objects
 * - See how Condition provides better control
 * - Understand signal() vs signalAll()
 * - Practice producer-consumer with Condition
 */
public class Exercise3_3_ConditionProducerConsumer {
    
    /**
     * Bounded buffer using synchronized with wait/notify (for comparison)
     */
    static class WaitNotifyBuffer {
        private final int[] buffer;
        private int size = 0;
        private int putIndex = 0;
        private int takeIndex = 0;
        
        public WaitNotifyBuffer(int capacity) {
            this.buffer = new int[capacity];
        }
        
        public synchronized void put(int value) throws InterruptedException {
            while (size == buffer.length) {
                System.out.println(Thread.currentThread().getName() + 
                    " waiting - buffer is full");
                wait();
            }
            
            buffer[putIndex] = value;
            putIndex = (putIndex + 1) % buffer.length;
            size++;
            
            System.out.println(Thread.currentThread().getName() + 
                " produced: " + value + " (size: " + size + ")");
            
            notifyAll(); // Wakes up all waiting threads (producers and consumers)
        }
        
        public synchronized int take() throws InterruptedException {
            while (size == 0) {
                System.out.println(Thread.currentThread().getName() + 
                    " waiting - buffer is empty");
                wait();
            }
            
            int value = buffer[takeIndex];
            takeIndex = (takeIndex + 1) % buffer.length;
            size--;
            
            System.out.println(Thread.currentThread().getName() + 
                " consumed: " + value + " (size: " + size + ")");
            
            notifyAll();
            
            return value;
        }
        
        public synchronized int getSize() {
            return size;
        }
    }
    
    /**
     * Bounded buffer using ReentrantLock with Condition
     * Uses separate conditions for notFull and notEmpty
     */
    static class ConditionBuffer {
        private final int[] buffer;
        private int size = 0;
        private int putIndex = 0;
        private int takeIndex = 0;
        
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();   // Condition for producers
        private final Condition notEmpty = lock.newCondition();  // Condition for consumers
        
        public ConditionBuffer(int capacity) {
            this.buffer = new int[capacity];
        }
        
        public void put(int value) throws InterruptedException {
            lock.lock();
            try {
                // Wait while buffer is full
                while (size == buffer.length) {
                    System.out.println(Thread.currentThread().getName() + 
                        " waiting - buffer is full");
                    notFull.await(); // Wait on notFull condition
                }
                
                // Buffer has space, add item
                buffer[putIndex] = value;
                putIndex = (putIndex + 1) % buffer.length;
                size++;
                
                System.out.println(Thread.currentThread().getName() + 
                    " produced: " + value + " (size: " + size + ")");
                
                // Signal only waiting consumers (not producers)
                notEmpty.signal(); // More efficient than signalAll()
            } finally {
                lock.unlock();
            }
        }
        
        public int take() throws InterruptedException {
            lock.lock();
            try {
                // Wait while buffer is empty
                while (size == 0) {
                    System.out.println(Thread.currentThread().getName() + 
                        " waiting - buffer is empty");
                    notEmpty.await(); // Wait on notEmpty condition
                }
                
                // Buffer has items, remove one
                int value = buffer[takeIndex];
                takeIndex = (takeIndex + 1) % buffer.length;
                size--;
                
                System.out.println(Thread.currentThread().getName() + 
                    " consumed: " + value + " (size: " + size + ")");
                
                // Signal only waiting producers (not consumers)
                notFull.signal();
                
                return value;
            } finally {
                lock.unlock();
            }
        }
        
        public int getSize() {
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Bounded buffer with multiple conditions and priority
     * Demonstrates advanced Condition usage
     */
    static class PriorityConditionBuffer {
        private final int[] buffer;
        private int size = 0;
        private int putIndex = 0;
        private int takeIndex = 0;
        
        private final Lock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();
        private final Condition priorityEmpty = lock.newCondition(); // Priority consumers
        
        private boolean priorityConsumerWaiting = false;
        
        public PriorityConditionBuffer(int capacity) {
            this.buffer = new int[capacity];
        }
        
        public void put(int value) throws InterruptedException {
            lock.lock();
            try {
                while (size == buffer.length) {
                    notFull.await();
                }
                
                buffer[putIndex] = value;
                putIndex = (putIndex + 1) % buffer.length;
                size++;
                
                System.out.println(Thread.currentThread().getName() + 
                    " produced: " + value + " (size: " + size + ")");
                
                // Signal priority consumer first, then regular consumers
                if (priorityConsumerWaiting) {
                    priorityEmpty.signal();
                } else {
                    notEmpty.signal();
                }
            } finally {
                lock.unlock();
            }
        }
        
        public int take() throws InterruptedException {
            lock.lock();
            try {
                while (size == 0) {
                    notEmpty.await();
                }
                
                int value = buffer[takeIndex];
                takeIndex = (takeIndex + 1) % buffer.length;
                size--;
                
                System.out.println(Thread.currentThread().getName() + 
                    " consumed: " + value + " (size: " + size + ")");
                
                notFull.signal();
                
                return value;
            } finally {
                lock.unlock();
            }
        }
        
        public int takePriority() throws InterruptedException {
            lock.lock();
            try {
                priorityConsumerWaiting = true;
                try {
                    while (size == 0) {
                        priorityEmpty.await();
                    }
                    
                    int value = buffer[takeIndex];
                    takeIndex = (takeIndex + 1) % buffer.length;
                    size--;
                    
                    System.out.println(Thread.currentThread().getName() + 
                        " [PRIORITY] consumed: " + value + " (size: " + size + ")");
                    
                    notFull.signal();
                    
                    return value;
                } finally {
                    priorityConsumerWaiting = false;
                }
            } finally {
                lock.unlock();
            }
        }
        
        public int getSize() {
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Producer thread
     */
    static class Producer implements Runnable {
        private final ConditionBuffer buffer;
        private final int itemsToProduce;
        private final int producerId;
        
        public Producer(ConditionBuffer buffer, int itemsToProduce, int producerId) {
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
        private final ConditionBuffer buffer;
        private final int itemsToConsume;
        private final int consumerId;
        
        public Consumer(ConditionBuffer buffer, int itemsToConsume, int consumerId) {
            this.buffer = buffer;
            this.itemsToConsume = itemsToConsume;
            this.consumerId = consumerId;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 0; i < itemsToConsume; i++) {
                    buffer.take();
                    Thread.sleep(100); // Simulate consumption time
                }
                System.out.println("Consumer-" + consumerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer-" + consumerId + " interrupted");
            }
        }
    }
    
    /**
     * Priority consumer thread
     */
    static class PriorityConsumer implements Runnable {
        private final PriorityConditionBuffer buffer;
        private final int itemsToConsume;
        private final int consumerId;
        
        public PriorityConsumer(PriorityConditionBuffer buffer, int itemsToConsume, int consumerId) {
            this.buffer = buffer;
            this.itemsToConsume = itemsToConsume;
            this.consumerId = consumerId;
        }
        
        @Override
        public void run() {
            try {
                for (int i = 0; i < itemsToConsume; i++) {
                    buffer.takePriority();
                    Thread.sleep(100);
                }
                System.out.println("PriorityConsumer-" + consumerId + " finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("PriorityConsumer-" + consumerId + " interrupted");
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 3.3: Condition Producer-Consumer ===\n");
        
        // Scenario 1: Basic Condition vs Wait/Notify
        System.out.println("=== Scenario 1: Condition vs Wait/Notify ===\n");
        System.out.println("Key differences:");
        System.out.println("- Condition allows multiple wait sets per lock");
        System.out.println("- Condition provides signal() vs signalAll() choice");
        System.out.println("- Condition is more flexible and efficient\n");
        
        ConditionBuffer buffer = new ConditionBuffer(5);
        
        Thread producer1 = new Thread(new Producer(buffer, 10, 1), "Producer-1");
        Thread producer2 = new Thread(new Producer(buffer, 10, 2), "Producer-2");
        Thread consumer1 = new Thread(new Consumer(buffer, 10, 1), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer(buffer, 10, 2), "Consumer-2");
        
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();
        
        try {
            producer1.join();
            producer2.join();
            consumer1.join();
            consumer2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nFinal buffer size: " + buffer.getSize() + "\n");
        
        // Scenario 2: Multiple producers and consumers
        System.out.println("=== Scenario 2: Multiple Producers and Consumers ===\n");
        
        ConditionBuffer buffer2 = new ConditionBuffer(3);
        
        Thread[] producers = new Thread[3];
        Thread[] consumers = new Thread[5];
        
        for (int i = 0; i < 3; i++) {
            final int index = i;
            producers[i] = new Thread(new Producer(buffer2, 5, index), "Producer-" + index);
        }
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            consumers[i] = new Thread(new Consumer(buffer2, 3, index), "Consumer-" + index);
        }
        
        for (Thread t : consumers) t.start();
        for (Thread t : producers) t.start();
        
        try {
            for (Thread t : producers) t.join();
            for (Thread t : consumers) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nFinal buffer size: " + buffer2.getSize() + "\n");
        
        // Scenario 3: Priority consumers with multiple conditions
        System.out.println("=== Scenario 3: Priority Consumers ===\n");
        System.out.println("Demonstrates multiple Condition objects for different priorities\n");
        
        PriorityConditionBuffer priorityBuffer = new PriorityConditionBuffer(5);
        
        Thread priorityProducer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    priorityBuffer.put(i);
                    Thread.sleep(100);
                }
                System.out.println("PriorityProducer finished");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PriorityProducer");
        
        Thread regularConsumer = new Thread(new Consumer(
            new ConditionBuffer(5) {
                @Override
                public int take() throws InterruptedException {
                    return priorityBuffer.take();
                }
            }, 5, 1), "RegularConsumer");
        
        Thread priorityConsumer = new Thread(new PriorityConsumer(priorityBuffer, 5, 1), 
            "PriorityConsumer");
        
        priorityProducer.start();
        regularConsumer.start();
        priorityConsumer.start();
        
        try {
            priorityProducer.join();
            regularConsumer.join();
            priorityConsumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nFinal priority buffer size: " + priorityBuffer.getSize() + "\n");
        
        // Scenario 4: signal() vs signalAll()
        System.out.println("=== Scenario 4: signal() vs signalAll() ===\n");
        System.out.println("signal(): Wakes up one waiting thread (more efficient)");
        System.out.println("signalAll(): Wakes up all waiting threads (may cause unnecessary wakeups)");
        System.out.println("With Condition, we can use signal() since we have separate conditions\n");
        
        ConditionBuffer signalBuffer = new ConditionBuffer(2);
        
        Thread signalProducer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    signalBuffer.put(i);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "SignalProducer");
        
        Thread[] signalConsumers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            signalConsumers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 2; j++) {
                        signalBuffer.take();
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "SignalConsumer-" + index);
        }
        
        signalProducer.start();
        for (Thread t : signalConsumers) t.start();
        
        try {
            signalProducer.join();
            for (Thread t : signalConsumers) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\n=== Key Takeaways ===");
        System.out.println("1. Condition provides better control than wait()/notify()");
        System.out.println("2. Multiple Condition objects allow separate wait sets");
        System.out.println("3. signal() is more efficient than signalAll() when appropriate");
        System.out.println("4. Condition works with Lock, not synchronized");
        System.out.println("5. Always await() in a while loop to handle spurious wakeups");
        System.out.println("6. Use separate conditions for different wait conditions");
        System.out.println("7. Condition allows implementing priority-based waiting");
    }
}

