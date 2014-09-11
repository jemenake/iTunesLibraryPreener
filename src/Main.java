import com4jitunes.ClassFactory;
import com4jitunes.IiTunes;

/**
 * Date: Aug 3, 2010
 * Time: 9:35:51 PM
 */

public class Main {


    public Main() {
        IiTunes it = ClassFactory.createiTunesApp();

//        Dispatch iTunesController = (Dispatch) iTunesCom.getObject();
        //Dispatch.call(iTunesController, "PlayPause");
//        Variant v = Dispatch.call(iTunesController,"Sources");

        try {
//        System.out.println(v.getString());
        } catch(Exception e) {
            System.out.println(e);
        }
//        ComThread.Release();
        System.exit(0);
    }

    public static void main(String args[]) {

        Main main = new Main();

    }
}
