# Phase 1: Thread Fundamentals

This phase covers the basics of Java threads.

## Running the Exercises

You can run each exercise individually:

```bash
# From the java-concurrency root directory
./gradlew :app:run --args="1.1"
./gradlew :app:run --args="1.2"
```

Or compile and run manually:

```bash
cd app
javac -d build/classes src/main/java/concurrency/phase1/*.java
java -cp build/classes concurrency.phase1.Exercise1_1_SimpleThread
```

## Exercises

1. **Exercise1_1_SimpleThread**: Basic thread creation
   - Learn different ways to create threads (extending Thread, implementing Runnable, using lambdas)
   - Understand the difference between `start()` and `run()`

2. **Exercise1_2_MultipleThreads**: Multiple threads executing concurrently
   - See how multiple threads execute concurrently
   - Observe non-deterministic thread scheduling
   - Learn about thread naming

3. **Exercise1_3_ThreadJoin**: Using join() to coordinate threads
   - Understand how `join()` works
   - Learn to coordinate thread execution
   - See blocking behavior of `join()`
   - Use `join()` with timeout

4. **Exercise1_4_RaceCondition**: Demonstrating race conditions
   - Understand what race conditions are
   - See how unsynchronized access to shared data causes problems
   - Observe lost updates and inconsistent results
   - Learn why synchronization is needed (preview for Phase 2)

5. **Exercise1_5_ThreadInterrupt**: Graceful thread interruption
   - Understand how to interrupt threads properly
   - Learn to handle `InterruptedException`
   - Implement graceful thread termination
   - Understand interrupt status flags

## Key Concepts

- **Thread**: A lightweight process that can run concurrently with other threads
- **start()**: Creates a new thread and calls run()
- **run()**: The method that contains the thread's code (don't call directly!)
- **join()**: Waits for a thread to complete
- **sleep()**: Pauses the current thread for a specified time
- **interrupt()**: Sends an interrupt signal to a thread

## Next Steps

Complete all exercises in this phase before moving to Phase 2 (Synchronization Basics).

