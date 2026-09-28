public class Writer implements Runnable{
    private final int writerNum;
    //writerNum is how it'll show "Writer W4 is running"
    public Writer(int writerNum){
        this.writerNum = writerNum;
    }
    public void run(){
        try{
            Main.write(writerNum);
        }catch(InterruptedException exception){
            Thread.currentThread().interrupt();
        }
    }
}
