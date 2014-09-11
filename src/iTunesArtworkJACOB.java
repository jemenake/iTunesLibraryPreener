import jacobitunes.IITArtwork;

public class iTunesArtworkJACOB extends iTunesArtwork {
    IITArtwork theArtwork;

    public iTunesArtworkJACOB(IITArtwork theArtwork) {
        this.theArtwork = theArtwork;
    }

    public void saveArtworkToFile(String filename) {
        theArtwork.saveArtworkToFile(filename);
    }

    public void delete() {
        theArtwork.delete();
    }
}
