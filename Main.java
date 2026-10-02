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

    public static void main(String[] args) throws InterruptedException {

        int taskSelection = checkArg(args);
        if (taskSelection < 0) {
            printTasks();
            System.exit(1);
        }

        switch (taskSelection) {
            case 0:
                System.out.println("No Arguments given, running both tasks.");
                System.out.println("\nHungry Philosophers Begin");
                DiningPhilosopher.main(args);
                System.out.println("\nReader/Writer Begin");
                task2();
                break;

            case 1:
                System.out.println("Program Arguments: " + args[0] + " " + args[1]);
                System.out.println("Hungry Philosophers Begin");
                DiningPhilosopher.main(args);
                break;

            case 2:
                System.out.println("Program Arguments: " + args[0] + " " + args[1]);
                System.out.println("Reader/Writer Begin");
                task2();
                break;
        }


        //System.out.println("Reader/Writer Begin");
        //task2();
        //System.out.println("Hungry Philosophers Begin");
        //runs the dining philosopher problem from its own file; this is what you were trying to do zayne
        //DiningPhilosopher.main(args);
    }

    public static void task2() {
        //this first section is going to get the values from the user for readers,
        //writers, and readers at once
        Scanner input = new Scanner(System.in);

        // gets value of readers and makes sure they are in parameters (graceful)
        System.out.print("How many reader threads to create? (1-10000): ");

        if (!input.hasNextInt()) {
            System.out.println("Invalid input. Reader count must be an integer.");
            System.exit(1);
        }
        totalReaders = input.nextInt();

        if (totalReaders < 1 || totalReaders > 10000) {
            System.out.println("Invalid input. Reader count must be between 1 and 10000.");
            System.exit(1);
        }

        // Gets number of writers and ensures that they are in parameters (graceful)
        System.out.print("How many writer threads to create? (1-10000): ");

        if (!input.hasNextInt()) {
            System.out.println("Invalid input. Writer count must be an integer.");
            System.exit(1);
        }
        totalWriters = input.nextInt();
        if (totalWriters < 1 || totalWriters > 10000) {
            System.out.println("Invalid input. Writer count must be between 1 and 10000.");
            System.exit(1);
        }

        // gets number of readers per batch, ensures they are in parameters (graceful)
        System.out.print("How many readers can read at once? (1-" + totalReaders + "): ");

        if (!input.hasNextInt()) {
            System.out.println("Invalid input. Maximum reader count must be an integer.");
            System.exit(1);
        }
        //makes sure max readers per batch isnt more than the total amt of readers
        maxReaders = input.nextInt();
        if (maxReaders < 1 || maxReaders > totalReaders) {
            System.out.println("Invalid input. Maximum readers must be between 1 and " + totalReaders + ".");
            System.exit(1);
        }
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
        //not closing the scanner here since that closes System.in and the philosophers need it after
        System.out.println("Readers and Writers are finished.");
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
        //wait cycle
        int wait2 = random.nextInt(4) + 3;
        int count1 = 0;
        while (count1 < wait2) {
            Thread.yield();
            count1++;
        }
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

    // Method to check input arguments
    // Returns -1 if any invalid inputs detected
    // If valid, returns input to be processed
    private static int checkArg(String[] args) {

        // Only parse if arguments are present
        if (args.length > 0) {

            // Check flag, error if not '-A'
            if (!args[0].equals("-A")) {
                System.out.println("\nError: " + args[0] + " not a known flag.");
                return -1;
            }

            // Error if flag is -A but no task number
            if (args.length < 2) {
                System.out.println("\nError: -A requires a value.");
                return -1;
            }
            //Error if flag is -A but too many arguments
            else if (args.length > 2) {
                System.out.println("\nError: Too many arguments.");
                return -1;
            }
            // Grab int from command argument
            try {
                int v = Integer.parseInt(args[1].trim());
                if (v < 1 || v > 2) {
                    System.out.println("\nError: Task must be 1 or 2.");
                    return -1;
                }
                return v; // Return task number once checked
            } catch (NumberFormatException e) {
                System.out.println("\nError: " + args[1] + " is not valid.");
                return -1;
            }
        }
        return 0;
    }

    // Print task options
    private static void printTasks() {
        System.out.println("\nPlease Choose: -A <task #>\n 1 -> Dining Philosophers\n " +
                "2 -> Readers-Writer");
    }
}