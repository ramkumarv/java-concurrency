package concurrency.phase1;

public class WaitWithoutSync {
    private final Object lock = new Object();

    public void callWaitImproperly() throws InterruptedException {
        System.out.println("About to call wait() without holding the lock…");
        lock.wait();  // IllegalMonitorStateException
    }

    public static void main(String[] args) {
        WaitWithoutSync demo = new WaitWithoutSync();
        try {
            demo.callWaitImproperly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
