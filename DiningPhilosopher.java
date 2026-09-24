import java.util.concurrent.Semaphore;
import java.util.Scanner;

public class DiningPhilosopher {

     static int[] questionUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("How many philosophers are there? :");
        int P = scanner.nextInt();   // Needs user validation.
        System.out.println("And how many meals are there to be eaten? :");
        int M = scanner.nextInt();

        return new int[]{P,M};  // Makes an array of both values to pass on.
    }

    public static void createPhilosophers(int pCount) {
        Semaphore[] philSemaphores = new Semaphore[pCount];
        for (int i = 0; i < pCount;i++) {
            philSemaphores[i] = new Semaphore(1);
        }
    }



}
