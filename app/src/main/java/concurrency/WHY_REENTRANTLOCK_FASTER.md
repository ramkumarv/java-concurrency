# Why Does ReentrantLock Appear Faster in the Benchmark?

## The Benchmark Results

```
Synchronized time: 12 ms
ReentrantLock time: 2 ms
```

**Important**: This benchmark is **NOT a fair comparison** of lock performance! Here's why:

---

## Why the Benchmark Shows ReentrantLock Faster

### 1. **JVM Warmup Effect** (Most Important)

```java
// Synchronized runs FIRST (cold JVM)
long startTime = System.currentTimeMillis();
for (Thread t : syncThreads) t.start();
// ... wait for completion ...
long syncTime = System.currentTimeMillis() - startTime;  // 12 ms

// ReentrantLock runs SECOND (warm JVM)
startTime = System.currentTimeMillis();
for (Thread t : lockThreads) t.start();
// ... wait for completion ...
long lockTime = System.currentTimeMillis() - startTime;  // 2 ms
```

**What happens:**
- First run (synchronized): JVM is "cold" - bytecode interpretation, JIT compilation overhead
- Second run (ReentrantLock): JVM is "warm" - classes loaded, JIT compiler optimized code, caches warmed up

**The JVM optimizes code during execution**, so the second benchmark benefits from optimizations learned from the first!

### 2. **Console I/O Dominates the Timing**

Both methods call `System.out.println()` **inside the critical section**:

```java
public synchronized void deposit(double amount) {
    if (amount > 0) {
        balance += amount;
        System.out.println(...);  // VERY SLOW I/O operation!
    }
}
```

**The problem:**
- `System.out.println()` is **extremely slow** (I/O operations take microseconds to milliseconds)
- The actual lock acquisition/release takes **nanoseconds**
- **I/O time dominates**, not lock time
- Console I/O can be buffered or flushed differently, affecting timing

### 3. **Low Timer Precision**

```java
long startTime = System.currentTimeMillis();  // Only 1ms precision!
```

**Problem:**
- `System.currentTimeMillis()` has **1 millisecond resolution**
- Lock operations take **nanoseconds** (1,000,000x smaller!)
- For small operations, you can't accurately measure differences
- Should use `System.nanoTime()` for micro-benchmarks

### 4. **Small Sample Size**

```java
for (int j = 0; j < 10; j++) {  // Only 10 iterations per thread
    syncAccount.deposit(10.0);
    syncAccount.withdraw(5.0);
}
// Total: 5 threads × 10 iterations × 2 operations = 100 operations
```

**Problem:**
- Too few operations for meaningful statistics
- Variance can be high with small samples
- JVM startup overhead dominates

---

## Real Performance Comparison

### In Reality: They're Nearly Identical

For **simple lock/unlock operations**, `synchronized` and `ReentrantLock` have **very similar performance**:

1. **Modern JVM optimization**: JVM heavily optimizes `synchronized`
2. **Lock inflation**: JVM uses different lock mechanisms based on contention
3. **Biased locking**: JVM optimizes for single-threaded access patterns

### When They Differ

| Scenario | synchronized | ReentrantLock | Winner |
|----------|--------------|---------------|--------|
| **Simple lock/unlock** | Fast | Fast | **Tie** |
| **High contention** | Good | Good | **Tie** |
| **Need tryLock()** | ❌ No | ✅ Yes | **ReentrantLock** |
| **Need fair locks** | ❌ No | ✅ Yes | **ReentrantLock** |
| **Need interruptible** | ❌ No | ✅ Yes | **ReentrantLock** |
| **Code simplicity** | ✅ Simple | More verbose | **synchronized** |

---

## Proper Benchmark Design

### What a Good Benchmark Should Do:

```java
// 1. Warm up JVM first
for (int i = 0; i < 10000; i++) {
    // Warmup runs
}

// 2. Use System.nanoTime() for precision
long startTime = System.nanoTime();
// ... operations ...
long endTime = System.nanoTime();
long duration = endTime - startTime;  // Nanosecond precision

// 3. Remove I/O from critical section
public synchronized void deposit(double amount) {
    balance += amount;  // No System.out.println here!
}

// 4. Run multiple iterations and average
long totalTime = 0;
for (int run = 0; run < 10; run++) {
    totalTime += benchmarkRun();
}
long avgTime = totalTime / 10;

// 5. Run both tests multiple times in random order
// to eliminate warmup bias
```

### Example: Proper Micro-Benchmark

```java
public class FairBenchmark {
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 1000000;
    
    public static void main(String[] args) {
        // Warmup
        warmup();
        
        // Benchmark synchronized
        long syncTime = benchmarkSynchronized();
        
        // Benchmark ReentrantLock
        long lockTime = benchmarkReentrantLock();
        
        System.out.println("Synchronized: " + syncTime + " ns");
        System.out.println("ReentrantLock: " + lockTime + " ns");
        System.out.println("Difference: " + 
            String.format("%.2f%%", (double)(lockTime - syncTime) / syncTime * 100));
    }
    
    private static long benchmarkSynchronized() {
        Counter counter = new SynchronizedCounter();
        long start = System.nanoTime();
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < TEST_ITERATIONS / 10; j++) {
                    counter.increment();  // No I/O here!
                }
            });
        }
        
        for (Thread t : threads) t.start();
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) {}
        }
        
        return System.nanoTime() - start;
    }
}
```

---

## Why Use ReentrantLock Then?

ReentrantLock isn't chosen for **performance** - it's chosen for **functionality**:

### Advantages of ReentrantLock:

1. **tryLock()** - Non-blocking lock acquisition
   ```java
   if (lock.tryLock()) {
       try {
           // Do work
       } finally {
           lock.unlock();
       }
   }
   ```

2. **lockInterruptibly()** - Can be interrupted while waiting
   ```java
   try {
       lock.lockInterruptibly();
       // Can be interrupted
   } catch (InterruptedException e) {
       // Handle interruption
   }
   ```

3. **Fair locks** - FIFO ordering
   ```java
   Lock fairLock = new ReentrantLock(true);  // Fair ordering
   ```

4. **Multiple Condition objects** - More flexible than wait/notify
   ```java
   Condition condition = lock.newCondition();
   ```

5. **Lock status queries** - Check if locked
   ```java
   if (lock.isLocked()) { ... }
   if (lock.isHeldByCurrentThread()) { ... }
   ```

---

## Key Takeaways

1. **The benchmark is flawed** - JVM warmup, I/O operations, and low precision make it unreliable
2. **Performance is similar** - For simple cases, synchronized and ReentrantLock are nearly identical
3. **Choose by features, not performance** - Use ReentrantLock when you need its advanced features
4. **synchronized is simpler** - Prefer it when basic locking is sufficient
5. **Real differences appear under specific conditions** - High contention, specific workloads, etc.

---

## Bottom Line

**Don't choose ReentrantLock for performance gains.**
**Choose it for flexibility and advanced features.**

The 2ms vs 12ms difference in the benchmark is **benchmark artifact**, not a real performance difference!


