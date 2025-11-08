package concurrency.phase3;

import java.util.concurrent.locks.StampedLock;

/**
 * Exercise 3.4: Implement a thread-safe counter with StampedLock
 * 
 * Learning Objectives:
 * - Understand StampedLock and its features
 * - Learn optimistic vs pessimistic locking
 * - Understand read and write stamps
 * - Learn to validate stamps
 * - See performance benefits of optimistic reads
 * - Compare StampedLock with ReadWriteLock
 */
public class Exercise3_4_StampedLock {
    
    /**
     * Thread-safe counter using synchronized (for comparison)
     */
    static class SynchronizedCounter {
        private long value = 0;
        
        public synchronized void increment() {
            value++;
        }
        
        public synchronized long get() {
            return value;
        }
        
        public synchronized void add(long delta) {
            value += delta;
        }
    }
    
    /**
     * Thread-safe counter using ReadWriteLock (for comparison)
     */
    static class ReadWriteLockCounter {
        private long value = 0;
        private final java.util.concurrent.locks.ReadWriteLock lock = 
            new java.util.concurrent.locks.ReentrantReadWriteLock();
        
        public void increment() {
            lock.writeLock().lock();
            try {
                value++;
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public long get() {
            lock.readLock().lock();
            try {
                return value;
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public void add(long delta) {
            lock.writeLock().lock();
            try {
                value += delta;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Thread-safe counter using StampedLock (pessimistic)
     */
    static class StampedLockCounter {
        private long value = 0;
        private final StampedLock lock = new StampedLock();
        
        /**
         * Write lock (pessimistic)
         */
        public void increment() {
            long stamp = lock.writeLock(); // Acquire write lock
            try {
                value++;
            } finally {
                lock.unlockWrite(stamp); // Release write lock
            }
        }
        
        /**
         * Read lock (pessimistic)
         */
        public long get() {
            long stamp = lock.readLock(); // Acquire read lock
            try {
                return value;
            } finally {
                lock.unlockRead(stamp); // Release read lock
            }
        }
        
        /**
         * Write lock for adding
         */
        public void add(long delta) {
            long stamp = lock.writeLock();
            try {
                value += delta;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Try to convert read lock to write lock
         */
        public boolean tryIncrement() {
            long stamp = lock.tryReadLock();
            if (stamp == 0) {
                return false; // Could not acquire read lock
            }
            
            try {
                // Try to convert to write lock
                long writeStamp = lock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0) {
                    // Conversion successful
                    try {
                        value++;
                        return true;
                    } finally {
                        lock.unlockWrite(writeStamp);
                    }
                } else {
                    // Conversion failed, release read lock and try write lock
                    lock.unlockRead(stamp);
                    writeStamp = lock.writeLock();
                    try {
                        value++;
                        return true;
                    } finally {
                        lock.unlockWrite(writeStamp);
                    }
                }
            } catch (Exception e) {
                lock.unlockRead(stamp);
                return false;
            }
        }
    }
    
    /**
     * Thread-safe counter using StampedLock with optimistic reads
     */
    static class OptimisticStampedLockCounter {
        private long value = 0;
        private final StampedLock lock = new StampedLock();
        
        public void increment() {
            long stamp = lock.writeLock();
            try {
                value++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Optimistic read - doesn't block, but must validate
         */
        public long getOptimistic() {
            long stamp = lock.tryOptimisticRead(); // Non-blocking optimistic read
            
            // Read the value
            long currentValue = value;
            
            // Validate stamp - if lock was acquired for write, stamp is invalid
            if (!lock.validate(stamp)) {
                // Optimistic read failed, fall back to pessimistic read lock
                stamp = lock.readLock();
                try {
                    currentValue = value;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return currentValue;
        }
        
        /**
         * Try optimistic read with timeout
         */
        public long getOptimisticWithTimeout(long timeoutMs) throws InterruptedException {
            long stamp = lock.tryOptimisticRead();
            long currentValue = value;
            
            if (lock.validate(stamp)) {
                return currentValue;
            }
            
            // Fall back to read lock with timeout
            stamp = lock.tryReadLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (stamp == 0) {
                throw new InterruptedException("Could not acquire read lock");
            }
            
            try {
                return value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Pessimistic read (for comparison)
         */
        public long get() {
            long stamp = lock.readLock();
            try {
                return value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        public void add(long delta) {
            long stamp = lock.writeLock();
            try {
                value += delta;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }
    
    /**
     * Complex counter with multiple operations using StampedLock
     */
    static class ComplexStampedLockCounter {
        private long value = 0;
        private long operations = 0;
        private final StampedLock lock = new StampedLock();
        
        public void increment() {
            long stamp = lock.writeLock();
            try {
                value++;
                operations++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public void decrement() {
            long stamp = lock.writeLock();
            try {
                value--;
                operations++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Snapshot - reads both values atomically
         */
        public long[] getSnapshot() {
            long stamp = lock.readLock();
            try {
                return new long[]{value, operations};
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Optimistic snapshot
         */
        public long[] getOptimisticSnapshot() {
            long stamp = lock.tryOptimisticRead();
            long currentValue = value;
            long currentOps = operations;
            
            if (lock.validate(stamp)) {
                return new long[]{currentValue, currentOps};
            }
            
            // Fall back to pessimistic read
            stamp = lock.readLock();
            try {
                return new long[]{value, operations};
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        public long getValue() {
            long stamp = lock.readLock();
            try {
                return value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        public long getOperations() {
            long stamp = lock.readLock();
            try {
                return operations;
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 3.4: StampedLock ===\n");
        
        // Scenario 1: Basic StampedLock usage
        System.out.println("=== Scenario 1: Basic StampedLock (Pessimistic) ===\n");
        
        StampedLockCounter stampedCounter = new StampedLockCounter();
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        
        Thread[] stampedThreads = new Thread[10];
        Thread[] syncThreads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            stampedThreads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    stampedCounter.increment();
                }
            }, "StampedThread-" + index);
            
            syncThreads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    syncCounter.increment();
                }
            }, "SyncThread-" + index);
        }
        
        long startTime = System.currentTimeMillis();
        for (Thread t : stampedThreads) t.start();
        for (Thread t : stampedThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long stampedTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        for (Thread t : syncThreads) t.start();
        for (Thread t : syncThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long syncTime = System.currentTimeMillis() - startTime;
        
        System.out.println("StampedLock counter value: " + stampedCounter.get());
        System.out.println("Synchronized counter value: " + syncCounter.get());
        System.out.println("StampedLock time: " + stampedTime + " ms");
        System.out.println("Synchronized time: " + syncTime + " ms");
        System.out.println("Expected: 10000\n");
        
        // Scenario 2: Optimistic reads
        System.out.println("=== Scenario 2: Optimistic Reads ===\n");
        System.out.println("Optimistic reads don't block but must be validated.\n");
        
        OptimisticStampedLockCounter optimisticCounter = new OptimisticStampedLockCounter();
        
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                optimisticCounter.increment();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Writer");
        
        Thread[] readers = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    long value = optimisticCounter.getOptimistic();
                    System.out.println("Reader-" + index + " read: " + value);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Reader-" + index);
        }
        
        writer.start();
        for (Thread t : readers) t.start();
        
        try {
            writer.join();
            for (Thread t : readers) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nFinal value: " + optimisticCounter.get() + "\n");
        
        // Scenario 3: Read-heavy workload comparison
        System.out.println("=== Scenario 3: Read-Heavy Workload Comparison ===\n");
        
        ReadWriteLockCounter rwCounter = new ReadWriteLockCounter();
        OptimisticStampedLockCounter optCounter = new OptimisticStampedLockCounter();
        
        // Initialize both counters
        for (int i = 0; i < 1000; i++) {
            rwCounter.increment();
            optCounter.increment();
        }
        
        Thread[] rwReaders = new Thread[20];
        Thread[] optReaders = new Thread[20];
        
        for (int i = 0; i < 20; i++) {
            final int index = i;
            rwReaders[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    rwCounter.get();
                }
            }, "RWReader-" + index);
            
            optReaders[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    optCounter.getOptimistic();
                }
            }, "OptReader-" + index);
        }
        
        startTime = System.currentTimeMillis();
        for (Thread t : rwReaders) t.start();
        for (Thread t : rwReaders) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long rwTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        for (Thread t : optReaders) t.start();
        for (Thread t : optReaders) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long optTime = System.currentTimeMillis() - startTime;
        
        System.out.println("ReadWriteLock time: " + rwTime + " ms");
        System.out.println("Optimistic StampedLock time: " + optTime + " ms");
        System.out.println("Performance improvement: " + 
            String.format("%.1f%%", ((double)(rwTime - optTime) / rwTime * 100)));
        System.out.println("Optimistic reads are faster when writes are infrequent!\n");
        
        // Scenario 4: Complex operations with snapshots
        System.out.println("=== Scenario 4: Complex Operations with Snapshots ===\n");
        
        ComplexStampedLockCounter complexCounter = new ComplexStampedLockCounter();
        
        Thread incrementer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                complexCounter.increment();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Incrementer");
        
        Thread decrementer = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                complexCounter.decrement();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Decrementer");
        
        Thread snapshotReader = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                long[] snapshot = complexCounter.getOptimisticSnapshot();
                System.out.println("Snapshot: value=" + snapshot[0] + ", ops=" + snapshot[1]);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "SnapshotReader");
        
        incrementer.start();
        decrementer.start();
        snapshotReader.start();
        
        try {
            incrementer.join();
            decrementer.join();
            snapshotReader.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nFinal value: " + complexCounter.getValue());
        System.out.println("Total operations: " + complexCounter.getOperations() + "\n");
        
        // Scenario 5: Lock conversion
        System.out.println("=== Scenario 5: Lock Conversion ===\n");
        System.out.println("StampedLock allows converting read lock to write lock.\n");
        
        StampedLockCounter convertCounter = new StampedLockCounter();
        
        Thread[] convertThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            convertThreads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    convertCounter.tryIncrement();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "ConvertThread-" + index);
        }
        
        for (Thread t : convertThreads) t.start();
        for (Thread t : convertThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        System.out.println("Final value: " + convertCounter.get() + "\n");
        
        System.out.println("=== Key Takeaways ===");
        System.out.println("1. StampedLock provides three modes: read, write, and optimistic read");
        System.out.println("2. Optimistic reads are non-blocking but must be validated");
        System.out.println("3. StampedLock can convert read locks to write locks");
        System.out.println("4. Optimistic reads are faster when writes are infrequent");
        System.out.println("5. Stamps must be validated after optimistic reads");
        System.out.println("6. Always unlock with the correct stamp");
        System.out.println("7. StampedLock is not reentrant (unlike ReentrantLock)");
        System.out.println("8. Use optimistic reads for read-heavy, write-light workloads");
    }
}

