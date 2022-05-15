package COM;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockCondition implements Runnable {
    public static ReentrantLock lock = new ReentrantLock();
    public static Condition condition = lock.newCondition();
    @Override
    public void run() {
        try {
            lock.lock();
            condition.await();
            System.out.println("Therad is going on");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockCondition tl = new ReentrantLockCondition();
        Thread t1 = new Thread(tl);
        Thread.sleep(2000);
        lock.lock();
        condition.signal();
        lock.unlock();
    }
}
