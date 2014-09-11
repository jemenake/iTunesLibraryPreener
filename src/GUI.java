import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;


public class GUI extends JFrame {

    JButton pruneMissingFilesButton = new JButton("Prune Missing Files");
    JButton findMissingArtworkButton = new JButton("Find Missing Artwork");
    JButton findNonConformingArtworkButton = new JButton("Find Non-Conforming Artwork");
    JButton syncWatchlistsButton = new JButton("Sync Watchlists");
    JButton playAllOfPlaylistButton = new JButton("Play a Playlist");
    JTextField widthTextField = new JTextField(6);
    JTextField heightTextField = new JTextField(6);


    public GUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container cont = this.getContentPane();
        cont.setLayout(new FlowLayout());
        add(pruneMissingFilesButton);
        add(findMissingArtworkButton);
        add(findNonConformingArtworkButton);
        add(syncWatchlistsButton);
        add(playAllOfPlaylistButton);
        pack();
        setVisible(true);

        final iTunesInterface iTunesController = new iTunesInterfaceJACOB();
//        final iTunesInterface iTunesController = new iTunesInterfaceCom4J();

        pruneMissingFilesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                iTunesController.removeMissingFilesFromLibrary();
            }
        });

        findMissingArtworkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                iTunesController.findMissingArtwork();
            }
        });

        syncWatchlistsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                iTunesController.syncFolders();
            }
        });
        
        playAllOfPlaylistButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //iTunesController.playAPlaylist();
                iTunesController.quickPlay();
            }
        });
    }

    public static void main(String[] argv) {
        new GUI();
    }
    
    public static String getHostname() {
        String hostname = "UNKNOWN";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch(Exception e) {
            System.err.println("WARNING: getHostname() failed");
        }
        return(hostname);
    }
}
