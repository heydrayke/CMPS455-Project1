import java.util.Random;
import java.util.concurrent.Semaphore;

public class Philosopher implements Runnable {

    Random random = new Random();
    int name;
    Semaphore leftChopstick, rightChopstick, enterRoom, sitDown, getUp, meals, doneEating, mayLeave;


    public Philosopher(int name, Semaphore firstChopstick, Semaphore secondChopstick, Semaphore meals, Semaphore enterRoom, Semaphore sitDown, Semaphore getUp, Semaphore doneEating, Semaphore mayLeave) {
        this.name = name;
        this.leftChopstick = firstChopstick;
        this.rightChopstick = secondChopstick;
        this.meals = meals;
        this.enterRoom = enterRoom;
        this.sitDown = sitDown;
        this.getUp = getUp;
        this.doneEating = doneEating;
        this.mayLeave = mayLeave;
    }

    public void run() {
        try {
            System.out.println(name + " has arrived!");
            enterRoom.release();  // +1 to the 0/P enterRoom in main. Once fulfilled, all may sit.
            sitDown.acquire();  // Puts each thread to sleep waiting on P permits.

            while (meals.tryAcquire()) {  // When this returns false the pot is empty and the loop stops.
                leftChopstick.acquire();
                System.out.println(name + " has their left chopstick now.");
                rightChopstick.acquire();
                System.out.println(name + " has their right chopstick now.");

                // waiting loop
                System.out.println("*" + name + " begins eating*");
                int wait = random.nextInt(4) + 3;
                int count = 0;
                while (count < wait) {
                    Thread.yield();
                    count++;
                }
                System.out.println("*" + name + " finishes eating*");

                System.out.println(name + " ate a bowl.");
                leftChopstick.release();
                System.out.println(name + " put down their left chopstick now.");
                rightChopstick.release();
                System.out.println(name + " put down their right chopstick now.");

                System.out.println("*" + name + " begins thinking*");
                int wait2 = random.nextInt(4) + 3;
                int count2 = 0;
                while (count2 < wait2) {
                    Thread.yield();
                    count2++;
                }
                System.out.println("*" + name + " finishes thinking*");
            }
            doneEating.release();
            mayLeave.acquire();
            System.out.println(name + " leaves.");
            getUp.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}