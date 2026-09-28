public class Reader implements Runnable{
    private final int readerNum;
    //readerNum is how I'll see that R# is being executed.
    public Reader(int readerNum){
        this.readerNum = readerNum;
    }
    public void run(){
        try{
            Main.read(readerNum);
        } catch (InterruptedException exception){
            Thread.currentThread().interrupt();
        }
    }
}
