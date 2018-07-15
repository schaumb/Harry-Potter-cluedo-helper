public class MyMain {
    public static void main(String[] args) {
        new Game(new Commander(), PrinterImpl::new).start();
    }
}
