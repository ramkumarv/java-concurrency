# Phase 2: Synchronization Basics

This phase covers synchronization mechanisms in Java to prevent race conditions and coordinate threads.

## Running the Exercises

You can run each exercise individually:

```bash
# From the java-concurrency root directory
./gradlew :app:run --args="2.1"
./gradlew :app:run --args="2.2"
./gradlew :app:run --args="2.3"
./gradlew :app:run --args="2.4"
./gradlew :app:run --args="2.5"
```

Or compile and run manually:

```bash
cd app
javac -d build/classes src/main/java/concurrency/phase2/*.java
java -cp build/classes concurrency.phase2.Exercise2_1_SynchronizedCounter
```

## Exercises

1. **Exercise2_1_SynchronizedCounter**: Fix counter using synchronized
   - Understand how `synchronized` keyword works
   - Learn synchronized methods vs synchronized blocks
   - See how synchronization fixes race conditions
   - Understand intrinsic locks (monitor locks)
   - Compare synchronized vs unsynchronized performance

2. **Exercise2_2_ThreadSafeBankAccount**: Thread-safe bank account
   - Apply synchronized to real-world scenario
   - Understand method-level vs block-level synchronization
   - Learn to identify critical sections
   - See compound operations that need synchronization
   - Understand read-modify-write operations

3. **Exercise2_3_Deadlock**: Create deadlock scenario and fix it
   - Understand what causes deadlocks
   - Learn to identify deadlock-prone code
   - See how to prevent deadlocks
   - Understand lock ordering strategy
   - Learn about lock timeouts and detection

4. **Exercise2_4_Volatile**: Use volatile to fix visibility issues
   - Understand the Java Memory Model (JMM)
   - Learn about visibility guarantees
   - See when volatile is appropriate
   - Understand the difference between volatile and synchronized
   - Learn about happens-before relationships

5. **Exercise2_5_ProducerConsumer**: Producer-consumer pattern with synchronized
   - Understand the producer-consumer pattern
   - Learn wait() and notify()/notifyAll()
   - See how to coordinate multiple threads
   - Understand when to use notify() vs notifyAll()
   - Learn about spurious wakeups and how to handle them

## Key Concepts

### Synchronized Keyword
- **synchronized method**: Locks on `this` object
- **synchronized block**: Locks on specified object (more flexible)
- **synchronized static**: Locks on Class object
- Provides both mutual exclusion and visibility guarantees
- Only one thread can hold a lock at a time

### Volatile Keyword
- Ensures **visibility** but NOT **atomicity**
- Prevents compiler optimizations that cache values
- Lighter than synchronized (no mutual exclusion)
- Use for: flags, status indicators, single-writer scenarios
- Don't use for: read-modify-write operations

### Deadlocks
- Occur when threads wait for each other indefinitely
- Prevention: consistent lock ordering
- Detection: use tools like `jstack`
- Always lock multiple resources in the same order

### wait() and notify()
- **wait()**: Releases lock and waits for notification (must be in synchronized block)
- **notify()**: Wakes ONE waiting thread
- **notifyAll()**: Wakes ALL waiting threads
- Always use **while loop**, not if statement (handles spurious wakeups)
- Producer-consumer pattern uses these for coordination

## Common Patterns

1. **Bounded Buffer**: Producer-consumer with fixed-size buffer
2. **Lock Ordering**: Always lock in consistent order to prevent deadlocks
3. **Double-Checked Locking**: Use volatile for singleton pattern (advanced)
4. **Condition Variables**: wait()/notify() for thread coordination

## Next Steps

Complete all exercises in this phase before moving to Phase 3 (Java Concurrency Utilities - Locks).

## Important Notes

- Synchronization has performance cost but ensures correctness
- Always synchronize access to shared mutable state
- Use the smallest possible critical section
- Prefer synchronized blocks over synchronized methods when possible
- volatile is for visibility, synchronized is for both visibility and atomicity

