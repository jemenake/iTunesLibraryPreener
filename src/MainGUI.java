import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainGUI {
    private JButton cleanupArtworkButton;
    private JButton syncWatchfoldersButton;
    private JButton removeDeadTracksButton;
    private JTextField a300TextField;
    private JTextField a300TextField1;
    private JTextField a1TextField;
    private JCheckBox getArtworkFromOtherCheckBox;

    public MainGUI(final iTunesInterfaceCom4J test) {
        removeDeadTracksButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                test.removeMissingFilesFromLibrary();
            }
        });
    }

    public static void main(String args[]) {

    }
}
