import com4jitunes.IITArtwork;


public class iTunesArtworkCom4J extends iTunesArtwork {
    IITArtwork theArtwork;

    public iTunesArtworkCom4J(IITArtwork theArtwork) {
        this.theArtwork = theArtwork;
    }

    public void saveArtworkToFile(String filename) {
        theArtwork.saveArtworkToFile(filename);
    }
    
    public void delete() {
        theArtwork.delete();
    }
}
