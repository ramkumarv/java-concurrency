# Which Object's Lock is Released by wait()?

## Key Point: The Shared Object

**Both producer and consumer threads are calling methods on the SAME object!**

## The Shared BoundedBuffer Object

Look at the code:

```java
// Only ONE buffer object is created
BoundedBuffer buffer1 = new BoundedBuffer(5);

// Producer thread uses THIS buffer
Thread producer1 = new Thread(new Producer(buffer1, 10, 1), "Producer-1");

// Consumer thread uses THE SAME buffer
Thread consumer1 = new Thread(new Consumer(buffer1, 10, 1), "Consumer-1");
```

**Key insight**: `buffer1` is a **single shared object** that both threads use!

---

## Which Lock is Released?

### The Answer: The Lock on the BoundedBuffer Object

When `wait()` is called, it releases the lock on **the object on which the synchronized method is called**.

In this case:
- `put()` is a `synchronized` method → locks on `this` (the `BoundedBuffer` instance)
- `take()` is a `synchronized` method → locks on `this` (the SAME `BoundedBuffer` instance)
- When `wait()` is called → releases the lock on the `BoundedBuffer` object

### Visual Representation

```
┌─────────────────────────────────────────┐
│     BoundedBuffer buffer1               │
│  ┌───────────────────────────────────┐  │
│  │  synchronized void put() {        │  │
│  │      while (full) {               │  │
│  │          this.wait();  ← Locks on │  │
│  │      }                            │  │
│  │  }                                │  │
│  │                                   │  │
│  │  synchronized void take() {       │  │
│  │      while (empty) {              │  │
│  │          this.wait();  ← Locks on │  │
│  │      }                            │  │
│  │  }                                │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
           ↑                    ↑
           │                    │
    Producer Thread      Consumer Thread
    (calls put())        (calls take())
    
Both threads lock on THE SAME buffer1 object!
```

---

## How synchronized Methods Work

When you declare a method as `synchronized`:

```java
public synchronized void put(int value) {
    // This is equivalent to:
    // synchronized (this) {
    //     // method body
    // }
}
```

The lock is on `this` - the object instance on which the method is called.

### Example

```java
BoundedBuffer buffer1 = new BoundedBuffer(5);
BoundedBuffer buffer2 = new BoundedBuffer(5);

// Thread 1
buffer1.put(10);  // Locks on buffer1 object

// Thread 2
buffer1.take();   // Tries to lock on buffer1 object (same as Thread 1!)
                 // Must wait if Thread 1 has the lock

// Thread 3
buffer2.put(20);  // Locks on buffer2 object (different object!)
                 // No conflict with Thread 1 or 2!
```

---

## Step-by-Step: Lock Release

### Scenario: Producer Waiting, Consumer Takes Item

```java
// Initial state: buffer is full, producer is waiting

Thread A (Producer)                Thread B (Consumer)              buffer1 lock
──────────────────                 ──────────────────              ────────────
                                                                    
1. Calls buffer1.put(100)          
   → Acquires lock on buffer1      (Waiting to acquire lock)       LOCKED by A
   
2. Checks: buffer is full
   → Calls wait()                  
   → RELEASES lock on buffer1      (Can now acquire lock)          UNLOCKED!
   
3. BLOCKED (waiting for notify)    
                                   Calls buffer1.take()
                                   → Acquires lock on buffer1       LOCKED by B
                                   
4. Still BLOCKED                   
                                   Removes item from buffer
                                   → Calls notifyAll()              
                                   
5. WAKES UP (receives notification)
   → Competes for lock on buffer1  
                                   → Releases lock (method exits)   UNLOCKED!
   
6. Re-acquires lock on buffer1     (Method completed)              LOCKED by A
   → Rechecks condition
   → Condition is now false
   → Adds item to buffer
```

---

## Important: Producer and Consumer are NOT Different Objects

**Misconception**: "Producer and consumer are different objects"

**Reality**: 
- Producer and Consumer are **thread classes** (different threads)
- They both use the **same BoundedBuffer object** (the shared resource)
- The lock is on the **BoundedBuffer object**, not on the producer or consumer

### Code Structure

```java
// These are just wrapper classes that hold a reference to the buffer
class Producer implements Runnable {
    private final BoundedBuffer buffer;  // Reference to shared buffer
    // ...
}

class Consumer implements Runnable {
    private final BoundedBuffer buffer;  // Reference to SAME shared buffer
    // ...
}

// Main code
BoundedBuffer sharedBuffer = new BoundedBuffer(5);  // ONE object

Producer p = new Producer(sharedBuffer, ...);       // Holds reference
Consumer c = new Consumer(sharedBuffer, ...);       // Holds SAME reference

Thread producerThread = new Thread(p);
Thread consumerThread = new Thread(c);
```

---

## What if They Were Different Objects?

If producer and consumer used different buffer objects, `wait()` and `notifyAll()` wouldn't work!

### ❌ Broken Example

```java
BoundedBuffer buffer1 = new BoundedBuffer(5);  // Producer uses this
BoundedBuffer buffer2 = new BoundedBuffer(5);  // Consumer uses this (DIFFERENT!)

Thread producer = new Thread(() -> {
    buffer1.put(10);  // Waits on buffer1's lock
});

Thread consumer = new Thread(() -> {
    buffer2.take();   // Notifies on buffer2's lock (DIFFERENT OBJECT!)
});

// Problem: Producer waits on buffer1, but consumer notifies buffer2!
// Producer will wait forever! ❌
```

### ✅ Correct Example

```java
BoundedBuffer sharedBuffer = new BoundedBuffer(5);  // SAME object

Thread producer = new Thread(() -> {
    sharedBuffer.put(10);  // Waits on sharedBuffer's lock
});

Thread consumer = new Thread(() -> {
    sharedBuffer.take();   // Notifies on sharedBuffer's lock (SAME OBJECT!)
});

// Works correctly: Producer waits and consumer notifies the same object! ✅
```

---

## Explicit wait() on Specific Object

You can also explicitly call `wait()` on a specific object:

```java
public void put(int value) throws InterruptedException {
    synchronized (this) {  // Explicit lock on 'this'
        while (size == buffer.length) {
            this.wait();  // Releases lock on 'this' (the BoundedBuffer instance)
        }
        // ...
    }
}
```

Or even on a different object:

```java
private final Object lock = new Object();

public void put(int value) throws InterruptedException {
    synchronized (lock) {  // Lock on different object
        while (size == buffer.length) {
            lock.wait();  // Releases lock on 'lock' object
        }
        // ...
    }
}
```

But in our example, `synchronized` methods automatically lock on `this`.

---

## Summary

1. **Both producer and consumer use the SAME BoundedBuffer object**
2. **The lock is on the BoundedBuffer object** (the `this` reference)
3. **`wait()` releases the lock on the BoundedBuffer object**
4. **`notifyAll()` notifies threads waiting on the SAME BoundedBuffer object**
5. **Producer and Consumer are threads, not objects** - they share the buffer object

### Key Takeaway

```
wait() releases the lock on the object on which wait() is called.
In synchronized methods, this is the object instance (this).
Both put() and take() lock on the same BoundedBuffer instance,
so wait() in either method releases the lock on that same instance.
```


