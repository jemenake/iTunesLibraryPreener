//import com4jitunes.IITArtwork;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ArtworkViewer {

    JPanel artpanel = new JPanel();
    JPanel framepanel = new JPanel();
    JPanel buttonpanel = new JPanel();
    JFrame f = new JFrame("Artwork Chooser");
    int artCount = 0;
    Vector<String> filenames = new Vector();

    // Some object to lock on while the calling thread waits for this frame to close...
    final Object lock = new Object();

    public ArtworkViewer(String title) {

//        f.addWindowListener(new WindowAdapter(){
//                public void windowClosing(WindowEvent e) {
//                    System.exit(0);
//                }
//            });

        framepanel.setLayout(new BorderLayout());

        buttonpanel.setLayout(new FlowLayout());
        JButton closeButton = new JButton("Done");
        JButton deleteAllButton = new JButton("Delete All Artwork");

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                terminate();
            }
        });

        deleteAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for(Enumeration<String> enumer = artworks.keys(); enumer.hasMoreElements();) {
                    artworks.get(enumer.nextElement()).delete();
                }
                terminate();
            }
        });

        buttonpanel.add(deleteAllButton);
        buttonpanel.add(closeButton);

        framepanel.add(BorderLayout.SOUTH, buttonpanel);

        framepanel.add(BorderLayout.NORTH, new JLabel(title));
        framepanel.add(BorderLayout.CENTER,artpanel);

        f.add(framepanel);
    }

    // keys are filenames, values are the IITArtwork objects
    // This is so that we can delete the objects from the tracks back in iTunes
    Hashtable<String,iTunesArtwork> artworks = new Hashtable();

    public void add(iTunesArtwork artwork, int index) {
//        String filebase = System.getenv("HOMEPATH") + "\\Desktop";
        String filebase = System.getenv("TEMP");
        String filename = filebase + "\\artworkfile" + index + ".jpg";
        artwork.saveArtworkToFile(filename);
        artworks.put(filename,artwork);
        artpanel.add(new ArtworkImage(filename,this));
        filenames.add(filename);
        artCount++;
    }

    void remove(ArtworkImage artimage) {
        artpanel.remove(artimage);
        if(artworks.containsKey(artimage.getFilename())) {
            (artworks.get(artimage.getFilename())).delete();
        } else {
            System.out.println("Odd. We couldn't find the IITArtwork object for this filename");
        }
        f.pack();
        if(artpanel.getComponents().length < 2)
            terminate();
    }

    void terminate() {

        // Let the calling process go.
        synchronized (lock) {
            f.setVisible(false);
            lock.notify();
        }

        // Delete the temporary files
        for(int i=0; i<filenames.size(); i++) {
            String filename = (String) filenames.elementAt(i);
            (new File(filename)).delete();
        }

        f.dispose();
    }

    public void choose() {
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        // Set up a listener to the frame so that we can
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                terminate();
            }
        });

        synchronized(lock) {
            // We're going to wait here until the frame is closed...
            while (f.isVisible())
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // At this point, we return to the calling process...
        }
    }

    class ArtworkImage extends Component {
        private BufferedImage img;
        private ArtworkViewer parent;
        private String filename;

        public ArtworkImage(String filename, ArtworkViewer parent) {
            this.filename = filename;
            this.parent = parent;
            try {
                img = ImageIO.read(new File(filename));
                System.out.println("HxW = " + img.getHeight() + "x" + img.getWidth());
            } catch (IOException e) {
            }

            this.addMouseListener( new MouseListener() {
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
                public void mouseClicked(MouseEvent e) {
                    int n = JOptionPane.showConfirmDialog(f,"Delete this artwork?","Delete?",JOptionPane.YES_NO_OPTION);
                    if(n==0) {
                        removeSelf();
                    }
                }
            });
        }

        String getFilename() {
            return filename;
        }

        void removeSelf() {
            parent.remove(this);
        }

        public void paint(Graphics g) {
            g.drawImage(img, 0, 0, null);
        }

        public Dimension getPreferredSize() {
            if (img == null) {
                 return new Dimension(100,100);
            } else {
               return new Dimension(img.getWidth(null), img.getHeight(null));
           }
        }
    }
}