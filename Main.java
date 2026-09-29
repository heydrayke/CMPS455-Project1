//initializing classes
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Main {
    //defining all of the integers, then making the three semaphores used: readers, writers, and mutex.
    private static Semaphore readerCt;
    private static final Semaphore writerC = new Semaphore(0, true);
    private static final Semaphore RWMutex = new Semaphore(1,true);
    private static int totalReaders;
    private static int totalWriters;
    private static int maxReaders;
    private static int finishedReaders;
    private static int finishedWriters;
    private static int readersInBatch;
    private static int batchSize;
    private static final Random random = new Random();
    private static final Semaphore batchStartMutex = new Semaphore(1, true);
    private static final Semaphore allReadersStarted = new Semaphore(0, true);
    private static int readersStarted;

    public static void main(String[] args) {
        System.out.println("Reader/Writer Begin");
        task2();
    }


        public void task1() throws InterruptedException {
            Scanner scanner = new Scanner(System.in);

            System.out.println("How many philosophers are there? :");
            if (!scanner.hasNextInt()) {
                System.out.println("invalid input. exiting.");
                System.exit(1);
            }
            int P = scanner.nextInt();  // Philosopher count.
            if (P < 2) {
                System.out.println("needs at least 2 philosophers. exiting.");
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

            Semaphore[] chopsticks = createChopsticks(P);  // Create stix.
            Thread[] threads = new Thread[P];  // Thread instance loaded.

            for (int i = 0; i < P; i++) {
                Semaphore left = chopsticks[i];
                Semaphore right = chopsticks[(i+1) % P];

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
    public static void task2() {
        //this first section is going to get the values from the user for readers writers, and readers at once
        Scanner input = new Scanner(System.in);
        System.out.print("How many reader thread to create? (1-10000)");
        totalReaders = input.nextInt();
        System.out.print("how many writer threads to create? (1-10000)");
        totalWriters = input.nextInt();
        System.out.print("How many readers can read at once?");
        maxReaders = input.nextInt();
        //this section determines if there will be a full batch of readers and if it is finished with all of them
        if (maxReaders < totalReaders) {
            batchSize = maxReaders;
        }
        else{
            batchSize = totalReaders;
        }
        //makes semaphore per batch
        readerCt = new Semaphore(batchSize);
        //makes threads for readers and writers
        Thread[] readers = new Thread[totalReaders];
        Thread[] writers = new Thread[totalWriters];

        //makes the reader and writer threads
        for (int i=0;i<totalReaders;i++){
            readers[i] = new Thread(new Reader(i));
            readers[i].start();
        }
        for (int i=0;i<totalWriters;i++){
            writers[i] = new Thread(new Writer(i));
            writers[i].start();
        }
        //makes sure the threads are finished
        try{
            for (Thread reader:readers){
                reader.join();
            }
            for (Thread writer:writers){
                writer.join();
            }
        }catch(InterruptedException exception){
            Thread.currentThread().interrupt();
        }
        input.close();
        System.out.println("Writers are finished.");
        System.out.println("done");
    }
    //this handles the reader function and lets the reader semaphore activate and execute
    public static void read(int readerNum) throws InterruptedException {
        readerCt.acquire();

        System.out.println("R" + readerNum + " started reading");
        //barrier for the readers to all start actually reading at the same time. This stops
        //the readers from finishing while others are starting.
        batchStartMutex.acquire();
        readersStarted++;

        if (readersStarted == batchSize) {
            readersStarted = 0;
            allReadersStarted.release(batchSize);
        }

        batchStartMutex.release();
        allReadersStarted.acquire();
        //wait cycle
        int wait1 = random.nextInt(4) + 3;
        int count = 0;
        while (count < wait1) {
            Thread.yield();
            count++;
        }

        RWMutex.acquire();
        finishedReaders++;
        readersInBatch++;
        System.out.println("R" + readerNum + " finished reading. Total Reads:" + readersInBatch);
        // The final reader lets one writer run.
        if (readersInBatch == batchSize) {
            readersInBatch = 0;
            if (finishedWriters < totalWriters) {
                writerC.release();
            } else {
                startNextReaderBatch();
            }
        }
        RWMutex.release();
    }
    //this function keeps track of the permit for the reader control semaphore and allocates the
    //next batch of readers
    private static void startNextReaderBatch(){
        int readersRemaining = totalReaders - finishedReaders;
        if (readersRemaining > 0) {
            if (maxReaders < readersRemaining){
                batchSize = maxReaders;
            }else {
                batchSize = readersRemaining;
            }
            readerCt.release(batchSize);
        }
    }
    //this function deals with the writer and passes the permit to the readers or the next writer
    //if all the readers are done.
    public static void write(int writerNum) throws InterruptedException{
        writerC.acquire();
        System.out.println("W" + writerNum + " started writing");
        Thread.sleep(50);
        System.out.println("W" + writerNum + " finished writing");
        RWMutex.acquire();
        finishedWriters++;

        if(finishedReaders<totalReaders){
            startNextReaderBatch();
        }
        else if (finishedWriters<totalWriters){
            writerC.release();
        }
        RWMutex.release();
    }

}