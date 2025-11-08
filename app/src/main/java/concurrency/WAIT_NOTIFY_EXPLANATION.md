# Understanding wait() and notifyAll()

## Overview

`wait()` and `notifyAll()` are methods from Java's `Object` class used for thread coordination. They allow threads to wait for conditions and notify other threads when conditions change.

---

## What is `wait()`?

### Definition
- **Releases the lock** on the current object
- **Blocks the current thread** until another thread calls `notify()` or `notifyAll()` on the same object
- Can be interrupted (throws `InterruptedException`)

### Key Requirements
1. **Must be in a synchronized block/method** - Otherwise throws `IllegalMonitorStateException`
2. **Releases the lock** - This is crucial! Other threads can acquire the lock
3. **When woken up**, the thread must re-acquire the lock before continuing

### Example Flow

```java
public synchronized void put(int value) throws InterruptedException {
    while (size == buffer.length) {  // Condition check
        wait();  // 1. Releases lock, 2. Thread blocks here
    }
    // After being notified, thread wakes up and re-acquires lock
    // Continues execution here
    buffer[putIndex] = value;
}
```

**Step-by-step:**
1. Thread enters `synchronized` method → **acquires lock**
2. Checks condition (`size == buffer.length`)
3. Calls `wait()` → **releases lock**, **thread blocks**
4. Another thread can now acquire the lock
5. When notified → thread **wakes up** and **re-acquires lock**
6. Rechecks condition in `while` loop
7. If condition is met → continues execution

---

## What is `notifyAll()`?

### Definition
- **Wakes up all threads** waiting on this object's monitor
- Woken threads compete for the lock (only one gets it)
- **Does NOT release the current lock** - lock is released when synchronized block exits

### Key Points
- Must be in a synchronized block/method
- Only wakes up threads that are waiting (via `wait()`)
- Woken threads don't automatically get the lock - they compete for it

### Example

```java
public synchronized void put(int value) throws InterruptedException {
    // ... add item to buffer ...
    size++;
    
    notifyAll();  // Wakes up ALL waiting threads
    // Lock is still held until method exits
}
```

---

## wait() vs notify() vs notifyAll()

| Method | What It Does | When to Use |
|--------|--------------|-------------|
| `wait()` | Releases lock, blocks until notified | Waiting for a condition to become true |
| `notify()` | Wakes up **one** waiting thread | When you know only one thread can proceed |
| `notifyAll()` | Wakes up **all** waiting threads | When multiple threads might be able to proceed, or you're not sure |

### Example: notify() vs notifyAll()

```java
// Using notify() - wakes only ONE thread
public synchronized void put(int value) {
    buffer[putIndex] = value;
    size++;
    notify();  // Only ONE waiting thread wakes up
}

// Using notifyAll() - wakes ALL threads
public synchronized void put(int value) {
    buffer[putIndex] = value;
    size++;
    notifyAll();  // ALL waiting threads wake up
}
```

**Why notifyAll() in producer-consumer?**
- Multiple consumers might be waiting (buffer empty)
- Multiple producers might be waiting (buffer full)
- After a producer adds an item, **any consumer** can proceed
- After a consumer removes an item, **any producer** can proceed
- Using `notify()` might wake the wrong thread (e.g., wake a producer when a consumer should proceed)

---

## Why Use a `while` Loop, Not `if`?

### ⚠️ Critical: Always use `while`, never `if`!

```java
// ❌ WRONG - Don't do this!
public synchronized void put(int value) throws InterruptedException {
    if (size == buffer.length) {  // Wrong: if statement
        wait();
    }
    // Problem: When thread wakes up, condition might still be false!
    buffer[putIndex] = value;
}

// ✅ CORRECT - Always use while
public synchronized void put(int value) throws InterruptedException {
    while (size == buffer.length) {  // Correct: while loop
        wait();
    }
    // When thread wakes up, while loop rechecks condition
    buffer[putIndex] = value;
}
```

### Why?

1. **Spurious Wakeups**: Threads can wake up without being notified (rare but possible)
2. **Multiple Threads**: After `notifyAll()`, multiple threads compete for the lock. When a thread gets the lock, the condition might have changed again.
3. **Race Conditions**: Another thread might change the condition between notification and re-acquiring the lock

### Example Scenario

```java
// Thread A: Producer waiting (buffer full)
// Thread B: Producer waiting (buffer full)
// Thread C: Consumer removes item, calls notifyAll()

// Both A and B wake up
// Thread A gets lock first, adds item, buffer becomes full again
// Thread A releases lock
// Thread B gets lock, but buffer is FULL again!
// If using 'if', Thread B would try to add despite buffer being full ❌
// With 'while', Thread B rechecks and waits again ✅
```

---

## Complete Producer-Consumer Example

```java
class BoundedBuffer {
    private final int[] buffer;
    private int size = 0;
    private int putIndex = 0;
    private int takeIndex = 0;
    
    public synchronized void put(int value) throws InterruptedException {
        // Wait while buffer is full
        while (size == buffer.length) {
            wait();  // Releases lock, waits for notification
        }
        
        // Buffer has space, add item
        buffer[putIndex] = value;
        putIndex = (putIndex + 1) % buffer.length;
        size++;
        
        notifyAll();  // Wake up all waiting threads
    }
    
    public synchronized int take() throws InterruptedException {
        // Wait while buffer is empty
        while (size == 0) {
            wait();  // Releases lock, waits for notification
        }
        
        // Buffer has items, remove one
        int value = buffer[takeIndex];
        takeIndex = (takeIndex + 1) % buffer.length;
        size--;
        
        notifyAll();  // Wake up all waiting threads
        
        return value;
    }
}
```

---

## Execution Flow Example

### Scenario: Buffer Full, Producer Tries to Add

```
Time | Thread A (Producer)           | Thread B (Consumer)           | Buffer State
-----|-------------------------------|-------------------------------|-------------
T1   | Acquires lock, enters put()   | Waiting for lock              | Full (size=5)
T2   | Checks: size == 5, true       | Still waiting                 | Full
T3   | Calls wait() → releases lock  | Acquires lock                 | Full
T4   | BLOCKED (waiting)             | Enters take(), removes item   | size=4
T5   | Still blocked                 | Calls notifyAll()             | size=4
T6   | WAKES UP, competes for lock   | Releases lock (method exits)  | size=4
T7   | Re-acquires lock              |                               | size=4
T8   | Rechecks: size == 5? No (4)   |                               | size=4
T9   | Adds item, size becomes 5     |                               | Full
T10  | Calls notifyAll()             |                               | Full
T11  | Releases lock, method exits   |                               | Full
```

---

## Common Patterns

### Pattern 1: Producer-Consumer

```java
// Producer
synchronized (lock) {
    while (buffer.isFull()) {
        lock.wait();
    }
    buffer.add(item);
    lock.notifyAll();
}

// Consumer
synchronized (lock) {
    while (buffer.isEmpty()) {
        lock.wait();
    }
    item = buffer.remove();
    lock.notifyAll();
}
```

### Pattern 2: Waiting for a Condition

```java
private boolean condition = false;

public synchronized void waitForCondition() throws InterruptedException {
    while (!condition) {
        wait();
    }
    // Condition is now true
}

public synchronized void setCondition() {
    condition = true;
    notifyAll();  // Wake up all waiting threads
}
```

### Pattern 3: Countdown Latch (Simplified)

```java
private int count;

public synchronized void await() throws InterruptedException {
    while (count > 0) {
        wait();
    }
}

public synchronized void countDown() {
    count--;
    if (count == 0) {
        notifyAll();
    }
}
```

---

## Common Mistakes

### ❌ Mistake 1: Calling wait() outside synchronized block

```java
public void put(int value) {
    wait();  // IllegalMonitorStateException!
}
```

### ❌ Mistake 2: Using if instead of while

```java
if (size == buffer.length) {
    wait();  // Wrong! Use while loop
}
```

### ❌ Mistake 3: Not calling notifyAll() after changing condition

```java
public synchronized void put(int value) {
    buffer[putIndex] = value;
    size++;
    // Forgot to call notifyAll()! Waiting threads will wait forever
}
```

### ❌ Mistake 4: Calling wait() without checking condition first

```java
public synchronized void take() {
    wait();  // Wrong! Should check condition first
    // ...
}
```

---

## Key Takeaways

1. **wait()** releases the lock and blocks the thread
2. **notifyAll()** wakes up all waiting threads
3. **Always use `while`, never `if`** for condition checks
4. **Must be in synchronized block/method**
5. **Always call notifyAll()** after changing the condition
6. **wait() can throw InterruptedException** - must handle it
7. **Woken threads compete for the lock** - only one proceeds at a time
8. **Recheck conditions after waking up** - they might have changed

---

## wait() vs Condition (Phase 3)

In Phase 3, you'll learn about `Condition` from `java.util.concurrent.locks`, which is similar but more flexible:

- **wait/notify**: Works with `synchronized`
- **Condition**: Works with `Lock` (ReentrantLock)
- **Condition advantages**: Multiple condition objects, better control

But the principles are the same: wait for a condition, notify when it changes!


