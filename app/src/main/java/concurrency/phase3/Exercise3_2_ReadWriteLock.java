package concurrency.phase3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Random;

/**
 * Exercise 3.2: Implement a thread-safe cache using ReadWriteLock
 * 
 * Learning Objectives:
 * - Understand ReadWriteLock and ReentrantReadWriteLock
 * - Learn when to use read locks vs write locks
 * - Understand performance benefits of ReadWriteLock
 * - Practice multiple readers, single writer pattern
 * - See how ReadWriteLock improves read-heavy workloads
 */
public class Exercise3_2_ReadWriteLock {
    
    /**
     * Thread-safe cache using synchronized (for comparison)
     * All operations are mutually exclusive
     */
    static class SynchronizedCache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        
        public synchronized V get(K key) {
            // Simulate some read overhead
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return cache.get(key);
        }
        
        public synchronized void put(K key, V value) {
            // Simulate some write overhead
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cache.put(key, value);
            System.out.println(Thread.currentThread().getName() + 
                " put: " + key + " -> " + value);
        }
        
        public synchronized void remove(K key) {
            cache.remove(key);
            System.out.println(Thread.currentThread().getName() + 
                " removed: " + key);
        }
        
        public synchronized int size() {
            return cache.size();
        }
        
        public synchronized boolean containsKey(K key) {
            return cache.containsKey(key);
        }
    }
    
    /**
     * Thread-safe cache using ReadWriteLock
     * Multiple readers can read simultaneously
     * Only one writer at a time
     */
    static class ReadWriteLockCache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        
        public V get(K key) {
            lock.readLock().lock(); // Acquire read lock
            try {
                // Simulate some read overhead
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return cache.get(key);
            } finally {
                lock.readLock().unlock(); // Release read lock
            }
        }
        
        public void put(K key, V value) {
            lock.writeLock().lock(); // Acquire write lock
            try {
                // Simulate some write overhead
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                cache.put(key, value);
                System.out.println(Thread.currentThread().getName() + 
                    " put: " + key + " -> " + value);
            } finally {
                lock.writeLock().unlock(); // Release write lock
            }
        }
        
        public void remove(K key) {
            lock.writeLock().lock();
            try {
                cache.remove(key);
                System.out.println(Thread.currentThread().getName() + 
                    " removed: " + key);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public int size() {
            lock.readLock().lock();
            try {
                return cache.size();
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public boolean containsKey(K key) {
            lock.readLock().lock();
            try {
                return cache.containsKey(key);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        /**
         * Upgrade from read lock to write lock
         * Note: This is tricky and may cause deadlock if not done carefully
         * Better pattern: release read lock, then acquire write lock
         */
        public V putIfAbsent(K key, V value) {
            lock.readLock().lock();
            try {
                V existing = cache.get(key);
                if (existing == null) {
                    // Need to upgrade to write lock
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        // Double-check after acquiring write lock
                        existing = cache.get(key);
                        if (existing == null) {
                            cache.put(key, value);
                            System.out.println(Thread.currentThread().getName() + 
                                " putIfAbsent: " + key + " -> " + value);
                            return null;
                        }
                        return existing;
                    } finally {
                        lock.writeLock().unlock();
                        // Re-acquire read lock for symmetry
                        lock.readLock().lock();
                    }
                }
                return existing;
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    /**
     * Thread-safe cache with fair ReadWriteLock
     * Ensures threads acquire locks in FIFO order
     */
    static class FairReadWriteLockCache<K, V> {
        private final Map<K, V> cache = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock(true); // Fair lock
        
        public V get(K key) {
            lock.readLock().lock();
            try {
                return cache.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        public void put(K key, V value) {
            lock.writeLock().lock();
            try {
                cache.put(key, value);
                System.out.println(Thread.currentThread().getName() + 
                    " put: " + key + " -> " + value);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public int size() {
            lock.readLock().lock();
            try {
                return cache.size();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Exercise 3.2: ReadWriteLock ===\n");
        
        // Scenario 1: Read-heavy workload comparison
        System.out.println("=== Scenario 1: Read-Heavy Workload Comparison ===\n");
        System.out.println("Testing with 10 readers and 2 writers\n");
        
        SynchronizedCache<String, Integer> syncCache = new SynchronizedCache<>();
        ReadWriteLockCache<String, Integer> rwCache = new ReadWriteLockCache<>();
        
        // Initialize caches
        for (int i = 0; i < 10; i++) {
            syncCache.put("key" + i, i);
            rwCache.put("key" + i, i);
        }
        
        // Create read-heavy workload
        Thread[] syncReaders = new Thread[10];
        Thread[] syncWriters = new Thread[2];
        Thread[] rwReaders = new Thread[10];
        Thread[] rwWriters = new Thread[2];
        
        Random random = new Random();
        
        // Synchronized version
        for (int i = 0; i < 10; i++) {
            final int index = i;
            syncReaders[i] = new Thread(() -> {
                for (int j = 0; j < 20; j++) {
                    String key = "key" + random.nextInt(10);
                    syncCache.get(key);
                }
            }, "SyncReader-" + index);
        }
        
        for (int i = 0; i < 2; i++) {
            final int index = i;
            syncWriters[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    String key = "key" + random.nextInt(10);
                    syncCache.put(key, random.nextInt(100));
                }
            }, "SyncWriter-" + index);
        }
        
        // ReadWriteLock version
        for (int i = 0; i < 10; i++) {
            final int index = i;
            rwReaders[i] = new Thread(() -> {
                for (int j = 0; j < 20; j++) {
                    String key = "key" + random.nextInt(10);
                    rwCache.get(key);
                }
            }, "RWReader-" + index);
        }
        
        for (int i = 0; i < 2; i++) {
            final int index = i;
            rwWriters[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    String key = "key" + random.nextInt(10);
                    rwCache.put(key, random.nextInt(100));
                }
            }, "RWWriter-" + index);
        }
        
        // Run synchronized version
        long startTime = System.currentTimeMillis();
        for (Thread t : syncReaders) t.start();
        for (Thread t : syncWriters) t.start();
        for (Thread t : syncReaders) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        for (Thread t : syncWriters) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long syncTime = System.currentTimeMillis() - startTime;
        
        // Run ReadWriteLock version
        startTime = System.currentTimeMillis();
        for (Thread t : rwReaders) t.start();
        for (Thread t : rwWriters) t.start();
        for (Thread t : rwReaders) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        for (Thread t : rwWriters) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        long rwTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\nSynchronized version time: " + syncTime + " ms");
        System.out.println("ReadWriteLock version time: " + rwTime + " ms");
        System.out.println("Performance improvement: " + 
            String.format("%.1f%%", ((double)(syncTime - rwTime) / syncTime * 100)));
        System.out.println("ReadWriteLock allows multiple readers simultaneously!\n");
        
        // Scenario 2: Write-heavy workload (should be similar performance)
        System.out.println("=== Scenario 2: Write-Heavy Workload ===\n");
        System.out.println("When writes dominate, ReadWriteLock offers less benefit.\n");
        
        SynchronizedCache<String, Integer> syncWriteCache = new SynchronizedCache<>();
        ReadWriteLockCache<String, Integer> rwWriteCache = new ReadWriteLockCache<>();
        
        Thread[] syncWriteThreads = new Thread[5];
        Thread[] rwWriteThreads = new Thread[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            syncWriteThreads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    syncWriteCache.put("key" + j, random.nextInt(100));
                }
            }, "SyncWrite-" + index);
            
            rwWriteThreads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    rwWriteCache.put("key" + j, random.nextInt(100));
                }
            }, "RWWrite-" + index);
        }
        
        startTime = System.currentTimeMillis();
        for (Thread t : syncWriteThreads) t.start();
        for (Thread t : syncWriteThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        syncTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        for (Thread t : rwWriteThreads) t.start();
        for (Thread t : rwWriteThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        rwTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Synchronized version time: " + syncTime + " ms");
        System.out.println("ReadWriteLock version time: " + rwTime + " ms");
        System.out.println("Note: Write-heavy workloads see less benefit from ReadWriteLock\n");
        
        // Scenario 3: putIfAbsent demonstration
        System.out.println("=== Scenario 3: putIfAbsent with Lock Upgrade ===\n");
        ReadWriteLockCache<String, Integer> upgradeCache = new ReadWriteLockCache<>();
        
        Thread[] upgradeThreads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            upgradeThreads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    upgradeCache.putIfAbsent("key" + j, index * 100 + j);
                }
            }, "UpgradeThread-" + index);
        }
        
        for (Thread t : upgradeThreads) t.start();
        for (Thread t : upgradeThreads) {
            try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        
        System.out.println("Cache size: " + upgradeCache.size());
        for (int i = 0; i < 5; i++) {
            System.out.println("key" + i + ": " + upgradeCache.get("key" + i));
        }
        System.out.println();
        
        // Scenario 4: Fair ReadWriteLock
        System.out.println("=== Scenario 4: Fair ReadWriteLock ===\n");
        System.out.println("Fair locks ensure threads acquire locks in FIFO order.");
        System.out.println("This can prevent writer starvation but may be slower.\n");
        
        FairReadWriteLockCache<String, Integer> fairCache = new FairReadWriteLockCache<>();
        
        Thread fairWriter = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                fairCache.put("key" + i, i);
            }
        }, "FairWriter");
        
        Thread[] fairReaders = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            fairReaders[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    fairCache.get("key" + (j % 5));
                }
            }, "FairReader-" + index);
        }
        
        fairWriter.start();
        for (Thread t : fairReaders) t.start();
        
        try {
            fairWriter.join();
            for (Thread t : fairReaders) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Fair cache size: " + fairCache.size() + "\n");
        
        System.out.println("=== Key Takeaways ===");
        System.out.println("1. ReadWriteLock allows multiple readers simultaneously");
        System.out.println("2. Only one writer can hold the write lock at a time");
        System.out.println("3. ReadWriteLock is ideal for read-heavy workloads");
        System.out.println("4. Write-heavy workloads see less benefit from ReadWriteLock");
        System.out.println("5. Fair locks prevent starvation but may be slower");
        System.out.println("6. Lock upgrades (read -> write) require careful handling");
        System.out.println("7. Read locks are shared, write locks are exclusive");
    }
}

