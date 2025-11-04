# Java Concurrency Learning Project

A structured learning project for mastering Java concurrency from thread basics to advanced topics.

## 📚 Study Plan

See [STUDY_PLAN.md](./STUDY_PLAN.md) for the complete 6-phase study plan covering:
- Phase 1: Thread Fundamentals
- Phase 2: Synchronization Basics
- Phase 3: Java Concurrency Utilities - Locks
- Phase 4: Thread-Safe Collections and Atomic Classes
- Phase 5: Executors and Thread Pools
- Phase 6: Advanced Topics

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Gradle (or use the included Gradle wrapper)

### Running Exercises

1. **View available exercises:**
   ```bash
   cd java-concurrency
   ./gradlew :app:run
   ```

2. **Run a specific exercise:**
   ```bash
   ./gradlew :app:run --args="1.1"  # Exercise 1.1: Simple Thread
   ./gradlew :app:run --args="1.2"  # Exercise 1.2: Multiple Threads
   ./gradlew :app:run --args="1.3"  # Exercise 1.3: Thread Join
   ./gradlew :app:run --args="1.4"  # Exercise 1.4: Race Condition
   ./gradlew :app:run --args="1.5"  # Exercise 1.5: Thread Interrupt
   ```

3. **Run exercises directly:**
   ```bash
   cd app
   ./gradlew build
   java -cp build/classes/java/main concurrency.phase1.Exercise1_1_SimpleThread
   ```

## 📁 Project Structure

```
app/src/main/java/concurrency/
├── App.java         # Main entry point with exercise menu
├── phase1/          # Thread Fundamentals
│   ├── Exercise1_1_SimpleThread.java
│   ├── Exercise1_2_MultipleThreads.java
│   ├── Exercise1_3_ThreadJoin.java
│   ├── Exercise1_4_RaceCondition.java
│   ├── Exercise1_5_ThreadInterrupt.java
│   └── README.md
├── phase2/          # Synchronization Basics (TODO)
├── phase3/          # Concurrency Utilities - Locks (TODO)
├── phase4/          # Thread-Safe Collections (TODO)
├── phase5/          # Executors and Thread Pools (TODO)
└── phase6/          # Advanced Topics (TODO)
```

## 📖 Learning Path

1. **Start with Phase 1** - Master thread fundamentals
2. **Complete all exercises** in each phase before moving on
3. **Experiment** - Modify code to see what happens
4. **Read the comments** - Each exercise includes learning objectives
5. **Build your own examples** - Apply concepts to real problems

## 🎯 Current Status

- ✅ Phase 1: All exercises completed (1.1 - 1.5)
- ⏳ Phase 2-6: Coming soon

## 💡 Tips

- **Code along**: Don't just read, implement everything
- **Break things**: Intentionally introduce bugs to understand concepts
- **Use a debugger**: Step through concurrent code to see thread interactions
- **Measure performance**: Compare different approaches
- **Review common patterns**: Producer-consumer, reader-writer, etc.

## 📚 Resources

- [STUDY_PLAN.md](./STUDY_PLAN.md) - Complete study plan with exercises
- [Oracle Java Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- "Java Concurrency in Practice" by Brian Goetz (Highly Recommended)

Happy Learning! 🎓

