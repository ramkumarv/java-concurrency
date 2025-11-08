# Phase 3: Java Concurrency Utilities - Locks

This phase covers advanced locking mechanisms in the `java.util.concurrent.locks` package, providing more flexibility and control than the basic `synchronized` keyword.

## Running the Exercises

You can run each exercise individually:

```bash
# From the java-concurrency root directory
./gradlew :app:run --args="3.1"
./gradlew :app:run --args="3.2"
./gradlew :app:run --args="3.3"
./gradlew :app:run --args="3.4"
./gradlew :app:run --args="3.5"
```

Or compile and run manually:

```bash
cd app
javac -d build/classes src/main/java/concurrency/phase3/*.java
java -cp build/classes concurrency.phase3.Exercise3_1_ReentrantLock
```

## Exercises

1. **Exercise3_1_ReentrantLock**: Replace synchronized with ReentrantLock
   - Understand ReentrantLock vs synchronized
   - Learn `tryLock()` and `lockInterruptibly()`
   - Understand fair vs non-fair locks
   - Practice proper lock release in finally blocks
   - Use Condition for wait/notify patterns
   - Observe reentrancy with `ReentrantLockReentrancyDemo` (Scenario 6)

2. **Exercise3_2_ReadWriteLock**: Implement thread-safe cache using ReadWriteLock
   - Understand ReadWriteLock and ReentrantReadWriteLock
   - Learn when to use read locks vs write locks
   - Understand performance benefits of ReadWriteLock
   - Practice multiple readers, single writer pattern
   - See how ReadWriteLock improves read-heavy workloads

3. **Exercise3_3_ConditionProducerConsumer**: Use Condition to implement producer-consumer
   - Understand Condition interface vs wait()/notify()
   - Learn to use multiple Condition objects
   - See how Condition provides better control
   - Understand `signal()` vs `signalAll()`
   - Practice producer-consumer with Condition

4. **Exercise3_4_StampedLock**: Implement thread-safe counter with StampedLock
   - Understand StampedLock and its features
   - Learn optimistic vs pessimistic locking
   - Understand read and write stamps
   - Learn to validate stamps
   - See performance benefits of optimistic reads
   - Compare StampedLock with ReadWriteLock

5. **Exercise3_5_LockingPerformanceComparison**: Compare performance of different locking mechanisms
   - Understand performance characteristics of different locks
   - Learn when to use each locking mechanism
   - See trade-offs between different approaches
   - Measure and compare lock performance
   - Understand read-heavy vs write-heavy scenarios

## Key Concepts

### ReentrantLock

- **Reentrant**: Same thread can acquire the lock multiple times
- **More flexible than synchronized**: Can use `tryLock()`, `lockInterruptibly()`, etc.
- **Fair vs Non-fair**: Fair locks ensure FIFO ordering (slower but prevents starvation)
- **Always unlock in finally**: Critical to prevent deadlocks
- **Interruptible**: `lockInterruptibly()` allows interruption while waiting

### ReadWriteLock

- **Multiple readers**: Many threads can hold read lock simultaneously
- **Single writer**: Only one thread can hold write lock at a time
- **Read-heavy scenarios**: Excellent performance when reads dominate
- **Write-heavy scenarios**: Less benefit compared to simple locks
- **Lock upgrade**: Converting read to write lock requires careful handling

### Condition

- **Replaces wait/notify**: More flexible than Object's wait/notify
- **Multiple conditions**: Can create multiple condition objects per lock
- **signal() vs signalAll()**: `signal()` wakes one thread, `signalAll()` wakes all
- **Must be used with Lock**: Cannot use with synchronized blocks
- **Always await in while loop**: Handles spurious wakeups

### StampedLock

- **Three modes**: Read, write, and optimistic read
- **Optimistic reads**: Non-blocking but must validate
- **Stamps**: Returned values that must be used for unlocking
- **Lock conversion**: Can convert read lock to write lock
- **Not reentrant**: Unlike ReentrantLock
- **Best for**: Read-heavy, write-light workloads

## Comparison Table

| Lock Type | Reentrant | Fair Option | Read/Write | Optimistic | Use Case |
|-----------|-----------|-------------|------------|------------|----------|
| `synchronized` | Yes | No | No | No | Simple, general purpose |
| `ReentrantLock` | Yes | Yes | No | No | More control needed |
| `ReadWriteLock` | Yes | Yes | Yes | No | Read-heavy workloads |
| `StampedLock` | No | No | Yes | Yes | Read-heavy, write-light |

## Common Patterns

1. **Lock with Finally Block**
   ```java
   lock.lock();
   try {
       // critical section
   } finally {
       lock.unlock(); // Always release!
   }
   ```

2. **Try Lock with Timeout**
   ```java
   if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
       try {
           // operation
       } finally {
           lock.unlock();
       }
   }
   ```

3. **Condition Wait Pattern**
   ```java
   lock.lock();
   try {
       while (condition not met) {
           condition.await();
       }
       // do work
   } finally {
       lock.unlock();
   }
   ```

4. **Optimistic Read Pattern**
   ```java
   long stamp = lock.tryOptimisticRead();
   // read value
   if (!lock.validate(stamp)) {
       // fall back to pessimistic read
       stamp = lock.readLock();
       try {
           // read value again
       } finally {
           lock.unlockRead(stamp);
       }
   }
   ```

## Best Practices

1. **Always unlock in finally**: Prevents deadlocks
2. **Use tryLock() for non-blocking**: Avoid indefinite waiting
3. **Prefer signal() over signalAll()**: More efficient when appropriate
4. **Validate optimistic reads**: Always check the stamp
5. **Choose the right lock**: Match lock type to your workload
6. **Avoid lock upgrades**: They're error-prone; use separate locks if needed
7. **Fair locks are slower**: Only use when starvation is a concern
8. **StampedLock is not reentrant**: Be careful with nested calls

## Performance Considerations

- **synchronized**: Often fastest for simple scenarios
- **ReentrantLock**: Similar performance to synchronized, more flexible
- **ReadWriteLock**: Excellent for read-heavy workloads
- **StampedLock (optimistic)**: Fastest for read-heavy, write-light
- **Fair locks**: Slower but prevent thread starvation

## Next Steps

Complete all exercises in this phase before moving to Phase 4 (Thread-Safe Collections and Atomic Classes).

## Important Notes

- Locks provide more flexibility than synchronized but require careful management
- Always release locks in finally blocks to prevent deadlocks
- ReadWriteLock excels when reads significantly outnumber writes
- StampedLock's optimistic reads are fastest when writes are infrequent
- Condition provides better control than wait/notify for complex scenarios
- Performance depends on your specific workload - benchmark with real data

