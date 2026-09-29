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