import java.util.concurrent.Semaphore;
import java.util.Scanner;
import java.util.Random;

public class DiningPhilosopher {

    public static void main(String [] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("How many philosophers are there? :");
        if (!scanner.hasNextInt()) {
            System.out.println("invalid input. exiting.");
            System.exit(1);
        }
        int P = scanner.nextInt();  // Philosopher count.
        if (P < 1) {
            System.out.println("needs at least 1 philosopher. exiting.");
            System.exit(1);
        }

        System.out.println("And how many meals are there to be eaten? :");
        if (!scanner.hasNextInt()) {
            System.out.println("invalid input. exiting.");
            System.exit(1);
        }
        final int M = scanner.nextInt();  // Meal count.
        if (M < 1) {
            System.out.println("need at least 1 meal. exiting.");
            System.exit(1);
        }

        Semaphore enterRoom = new Semaphore(0);
        Semaphore sitDown = new Semaphore(0);
        Semaphore getUp = new Semaphore(0);
        Semaphore doneEating = new Semaphore(0);
        Semaphore mayLeave = new Semaphore(0);
        Semaphore meals = new Semaphore(M);

        int numChopsticks = (P == 1) ? 2 : P; // Checks for 1 philosopher case, sets P = 2 if so
        Semaphore[] chopsticks = createChopsticks(numChopsticks);  // Create stix.
        Thread[] threads = new Thread[P];  // Thread instance loaded.

        for (int i = 0; i < P; i++) {
            Semaphore left = chopsticks[i];
            Semaphore right = chopsticks[(i+1) % numChopsticks]; // modulo numChopsticks instead of P

            if (i == P - 1) {
                threads[i] = new Thread(new Philosopher(i, right, left, meals, enterRoom, sitDown, getUp, doneEating, mayLeave)); // If this is the last one, flip the grab
            } else {
                threads[i] = new Thread(new Philosopher(i, left, right, meals, enterRoom, sitDown, getUp, doneEating, mayLeave)); // Normal philosopher, not last.
            }
            threads[i].start();
        }
        enterRoom.acquire(P);  // Puts philosophers waiting until all P are there.
        System.out.println("All " + P + " philosophers have arrived. All may sit.");
        sitDown.release(P);

        doneEating.acquire(P);
        System.out.println("All philosophers are done eating. They rise to leave.");
        mayLeave.release(P);  // Releases permission slips to leave.

        getUp.acquire(P);  // Makes philosophers wait until last is done eating.
        System.out.println("Everyone is done. Goodbye!");
    }

    static Semaphore[] createChopsticks(int pCount) {
        Semaphore[] chopsticks = new Semaphore[pCount];
        for (int i = 0; i < pCount;i++) {
            chopsticks[i] = new Semaphore(1);
        }
        return chopsticks;
    }
}

class Philosopher implements Runnable {

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