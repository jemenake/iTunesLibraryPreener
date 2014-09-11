import java.io.IOException;

public class ITunesTest2 {
    

    public ITunesTest2() {
//        Dispatch iTunesController = (Dispatch) iTunesCom.getObject();
//        DispatchEvents events = new DispatchEvents(iTunesController, new ITunesEvents());

        try {
            System.in.read();
        } catch (IOException e) {}

//        ComThread.Release();
        System.exit(0);
    }

    public class ITunesEvents {
//        public void OnPlayerPlayEvent(Variant[] args) {
//            System.out.println("OnPlayerPlayEvent");
//            for(int i=0; i<args.length; i++) {
//                System.out.println("  arg" + i);
//            }
//        }
    }

    public static void main(String args[]) throws Exception {
        ITunesTest2 test = new ITunesTest2();
    }
}