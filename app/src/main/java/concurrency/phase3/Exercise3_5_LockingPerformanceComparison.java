package concurrency.phase3;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Exercise 3.5: Compare performance of different locking mechanisms
 * 
 * Learning Objectives:
 * - Understand performance characteristics of different locks
 * - Learn when to use each locking mechanism
 * - See trade-offs between different approaches
 * - Measure and compare lock performance
 * - Understand read-heavy vs write-heavy scenarios
 */
public class Exercise3_5_LockingPerformanceComparison {
    
    private static final int ITERATIONS = 100000;
    private static final int THREAD_COUNT = 10;
    
    /**
     * Counter using synchronized
     */
    static class SynchronizedCounter {
        private long value = 0;
        
        public synchronized void increment() {
            value++;
        }
        
        public synchronized long get() {
            return value;
        }
    }
    
    /**
     * Counter using ReentrantLock
     */
    static class ReentrantLockCounter {
        private long value = 0;
        private final ReentrantLock lock = new ReentrantLock();
        
        public void increment() {
            lock.lock();
            try {
                value++;
            } finally {
                lock.unlock();
            }
        }
        
        public long get() {
            lock.lock();
            try {
                return value;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Counter using ReentrantLock (fair)
     */
    static class FairReentrantLockCounter {
        private long value = 0;
        private final ReentrantLock lock = new ReentrantLock(true);
        
        public void increment() {
            lock.lock();
            try {
                value++;
            } finally {
                lock.unlock();
            }
        }
        
        public long get() {
            lock.lock();
            try {
                return value;
            } finally {
                lock.unlock();
            }
        }
    }
    
    /**
     * Counter using ReadWriteLock
     */
    static class ReadWriteLockCounter {
        private long value = 0;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        
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
    }
    
    /**
     * Counter using StampedLock (pessimistic)
     */
    static class StampedLockCounter {
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
        
        public long get() {
            long stamp = lock.readLock();
            try {
                return value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }
    
    /**
     * Counter using StampedLock (optimistic reads)
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
        
        public long get() {
            long stamp = lock.tryOptimisticRead();
            long currentValue = value;
            
            if (!lock.validate(stamp)) {
                // Fall back to pessimistic read
                stamp = lock.readLock();
                try {
                    currentValue = value;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return currentValue;
        }
    }
    
    /**
     * Benchmark result
     */
    static class BenchmarkResult {
        final String name;
        final long time;
        final long value;
        
        BenchmarkResult(String name, long time, long value) {
            this.name = name;
            this.time = time;
            this.value = value;
        }
    }
    
    /**
     * Run benchmark for write-heavy workload
     */
    static BenchmarkResult benchmarkWriteHeavy(Runnable setup, Runnable increment, 
                                               Runnable verify, String name) {
        setup.run();
        
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS / THREAD_COUNT; j++) {
                    increment.run();
                }
            });
        }
        
        long startTime = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.nanoTime();
        
        verify.run();
        
        return new BenchmarkResult(name, (endTime - startTime) / 1_000_000, 
            ((SynchronizedCounter) setup).get());
    }
    
    /**
     * Run benchmark for read-heavy workload
     */
    static BenchmarkResult benchmarkReadHeavy(Runnable setup, Runnable read, 
                                              Runnable verify, String name) {
        setup.run();
        
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS / THREAD_COUNT; j++) {
                    read.run();
                }
            });
        }
        
        long startTime = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.nanoTime();
        
        verify.run();
        
        return new BenchmarkResult(name, (endTime - startTime) / 1_000_000, 0);
    }
    
    /**
     * Run benchmark for mixed workload
     */
    static BenchmarkResult benchmarkMixed(Runnable setup, Runnable increment, 
                                          Runnable read, Runnable verify, String name) {
        setup.run();
        
        Thread[] writers = new Thread[THREAD_COUNT / 2];
        Thread[] readers = new Thread[THREAD_COUNT / 2];
        
        for (int i = 0; i < THREAD_COUNT / 2; i++) {
            writers[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS / THREAD_COUNT; j++) {
                    increment.run();
                }
            });
            
            readers[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS / THREAD_COUNT; j++) {
                    read.run();
                }
            });
        }
        
        long startTime = System.nanoTime();
        for (Thread t : writers) t.start();
        for (Thread t : readers) t.start();
        for (Thread t : writers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Thread t : readers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.nanoTime();
        
        verify.run();
        
        return new BenchmarkResult(name, (endTime - startTime) / 1_000_000, 0);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 3.5: Locking Performance Comparison ===\n");
        System.out.println("Iterations per thread: " + ITERATIONS / THREAD_COUNT);
        System.out.println("Total threads: " + THREAD_COUNT);
        System.out.println("Warming up JVM...\n");
        
        // Warm up JVM
        SynchronizedCounter warmup = new SynchronizedCounter();
        for (int i = 0; i < 1000; i++) {
            warmup.increment();
        }
        
        try {
            Thread.sleep(1000); // Let JIT compiler do its work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Scenario 1: Write-heavy workload
        System.out.println("=== Scenario 1: Write-Heavy Workload ===\n");
        System.out.println("All threads are writing (incrementing).\n");
        
        SynchronizedCounter syncCounter1 = new SynchronizedCounter();
        BenchmarkResult syncResult = benchmarkWriteHeavy(
            () -> {},
            syncCounter1::increment,
            () -> System.out.println("  Synchronized: " + syncCounter1.get()),
            "Synchronized"
        );
        
        ReentrantLockCounter lockCounter1 = new ReentrantLockCounter();
        BenchmarkResult lockResult = benchmarkWriteHeavy(
            () -> {},
            lockCounter1::increment,
            () -> System.out.println("  ReentrantLock: " + lockCounter1.get()),
            "ReentrantLock"
        );
        
        FairReentrantLockCounter fairCounter1 = new FairReentrantLockCounter();
        BenchmarkResult fairResult = benchmarkWriteHeavy(
            () -> {},
            fairCounter1::increment,
            () -> System.out.println("  Fair ReentrantLock: " + fairCounter1.get()),
            "Fair ReentrantLock"
        );
        
        StampedLockCounter stampedCounter1 = new StampedLockCounter();
        BenchmarkResult stampedResult = benchmarkWriteHeavy(
            () -> {},
            stampedCounter1::increment,
            () -> System.out.println("  StampedLock: " + stampedCounter1.get()),
            "StampedLock"
        );
        
        System.out.println("\nResults:");
        System.out.printf("  Synchronized:        %6d ms\n", syncResult.time);
        System.out.printf("  ReentrantLock:       %6d ms\n", lockResult.time);
        System.out.printf("  Fair ReentrantLock:  %6d ms\n", fairResult.time);
        System.out.printf("  StampedLock:        %6d ms\n", stampedResult.time);
        System.out.println("\nFor write-heavy workloads, synchronized is often fastest.\n");
        
        // Scenario 2: Read-heavy workload
        System.out.println("=== Scenario 2: Read-Heavy Workload ===\n");
        System.out.println("All threads are reading.\n");
        
        SynchronizedCounter syncReadCounter = new SynchronizedCounter();
        ReentrantLockCounter lockReadCounter = new ReentrantLockCounter();
        ReadWriteLockCounter rwCounter = new ReadWriteLockCounter();
        OptimisticStampedLockCounter optCounter = new OptimisticStampedLockCounter();
        
        // Initialize counters
        for (int i = 0; i < 1000; i++) {
            syncReadCounter.increment();
            lockReadCounter.increment();
            rwCounter.increment();
            optCounter.increment();
        }
        
        BenchmarkResult syncReadResult = benchmarkReadHeavy(
            () -> {},
            syncReadCounter::get,
            () -> {},
            "Synchronized"
        );
        
        BenchmarkResult lockReadResult = benchmarkReadHeavy(
            () -> {},
            lockReadCounter::get,
            () -> {},
            "ReentrantLock"
        );
        
        BenchmarkResult rwReadResult = benchmarkReadHeavy(
            () -> {},
            rwCounter::get,
            () -> {},
            "ReadWriteLock"
        );
        
        BenchmarkResult optReadResult = benchmarkReadHeavy(
            () -> {},
            optCounter::get,
            () -> {},
            "Optimistic StampedLock"
        );
        
        System.out.println("\nResults:");
        System.out.printf("  Synchronized:            %6d ms\n", syncReadResult.time);
        System.out.printf("  ReentrantLock:           %6d ms\n", lockReadResult.time);
        System.out.printf("  ReadWriteLock:           %6d ms\n", rwReadResult.time);
        System.out.printf("  Optimistic StampedLock:   %6d ms\n", optReadResult.time);
        System.out.println("\nFor read-heavy workloads, ReadWriteLock and optimistic StampedLock excel.\n");
        
        // Scenario 3: Mixed workload
        System.out.println("=== Scenario 3: Mixed Workload (50% reads, 50% writes) ===\n");
        
        SynchronizedCounter syncMixedCounter1 = new SynchronizedCounter();
        BenchmarkResult syncMixedResult = benchmarkMixed(
            () -> {},
            syncMixedCounter1::increment,
            syncMixedCounter1::get,
            () -> System.out.println("  Synchronized: " + syncMixedCounter1.get()),
            "Synchronized"
        );
        
        ReentrantLockCounter lockMixedCounter1 = new ReentrantLockCounter();
        BenchmarkResult lockMixedResult = benchmarkMixed(
            () -> {},
            lockMixedCounter1::increment,
            lockMixedCounter1::get,
            () -> System.out.println("  ReentrantLock: " + lockMixedCounter1.get()),
            "ReentrantLock"
        );
        
        ReadWriteLockCounter rwMixedCounter1 = new ReadWriteLockCounter();
        BenchmarkResult rwMixedResult = benchmarkMixed(
            () -> {},
            rwMixedCounter1::increment,
            rwMixedCounter1::get,
            () -> System.out.println("  ReadWriteLock: " + rwMixedCounter1.get()),
            "ReadWriteLock"
        );
        
        OptimisticStampedLockCounter optMixedCounter1 = new OptimisticStampedLockCounter();
        BenchmarkResult optMixedResult = benchmarkMixed(
            () -> {},
            optMixedCounter1::increment,
            optMixedCounter1::get,
            () -> System.out.println("  Optimistic StampedLock: " + optMixedCounter1.get()),
            "Optimistic StampedLock"
        );
        
        System.out.println("\nResults:");
        System.out.printf("  Synchronized:            %6d ms\n", syncMixedResult.time);
        System.out.printf("  ReentrantLock:           %6d ms\n", lockMixedResult.time);
        System.out.printf("  ReadWriteLock:           %6d ms\n", rwMixedResult.time);
        System.out.printf("  Optimistic StampedLock:   %6d ms\n", optMixedResult.time);
        System.out.println("\nFor mixed workloads, results vary based on contention.\n");
        
        System.out.println("=== Key Takeaways ===");
        System.out.println("1. synchronized is often fastest for simple, write-heavy scenarios");
        System.out.println("2. ReentrantLock offers more flexibility but similar performance");
        System.out.println("3. Fair locks are slower but prevent starvation");
        System.out.println("4. ReadWriteLock excels in read-heavy scenarios");
        System.out.println("5. Optimistic StampedLock is fastest for read-heavy, write-light");
        System.out.println("6. Lock choice depends on your workload characteristics");
        System.out.println("7. Always benchmark with your actual workload patterns");
        System.out.println("8. Consider not just performance but also code clarity and maintainability");
    }
}

