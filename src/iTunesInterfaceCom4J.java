import com4jitunes.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class iTunesInterfaceCom4J extends iTunesInterface {

    private IiTunes it;

    static String getTrackHash(IITTrack track) {
        String str = "";
        try {                                    
            str += ":" + track.name();
            str += ":" + track.album();
            str += track.artist();
            str += ":" + track.album();
            str += ":" + track.name();
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println(track);
            return(str);
        }
        return(track.artist() + ":" + track.album() + ":" + track.name());
    }

    static String albumHash(IITTrack track) {
        return(track.artist() + ":" + track.album());
    }

    public void quickPlay() {
        playAPlaylist();
//        it.play();
        while(true) {
            it.nextTrack();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    void playAPlaylist() {
        IITPlaylist playlist = choosePlaylist();
        if(playlist != null) {
            playAllSongs(playlist);
        }
    }

    void playAllSongs(IITPlaylist playlist) {
//        playlist = it.currentPlaylist();
        playlist.playFirstTrack();
        IITTrackCollection tracks = playlist.tracks();
        int trackCount = tracks.count();
        for(int i=0; i<trackCount; i++) {
            IITTrack track = tracks.item(i);
            if(track.kind() != ITTrackKind.ITTrackKindFile) {
                System.out.println("Not a File/CD kind: " + track.name());
            }
//                IITTrack track = tracks.itemByPlayOrder(i);
//                track.play();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void initCOM() {
//        ComThread.InitMTA(true);
//        ActiveXComponent iTunesCom = new ActiveXComponent("iTunes.Application");
//        Dispatch iTunesController = iTunesCom.getObject();
//        it = new IiTunes(iTunesController);
        it = com4jitunes.ClassFactory.createiTunesApp();
  // TODO
    }

    public iTunesInterfaceCom4J() {
        if(it == null) {
            initCOM();
        }
    }

    private IITSource getLibrarySource() {
        // Figure out the source for the main library
        IITSourceCollection sources = it.sources();
        IITSource source = null;
        int sourceCount = sources.count();
        for(int i=1; i<= sourceCount; i++) {
            ;
            if(sources.item(i).kind() == ITSourceKind.ITSourceKindLibrary) {
                source = sources.item(i);
            }
        }
        return source;
    }

    private void runAsCLI() {
/*
         for(int i=0; i<820; i++) {
            it.nextTrack();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
*/
        // First, get rid of missing entries...
        System.out.println("Step 1: Removing missing files from the global library");
        removeMissingFilesFromLibrary();

//        playAllSongs(source);
//        gracefullyExit();


        System.out.println("Step 2: Add new files in watch folders to libraries");
        syncFolders();

        System.out.println("Step 3: Fix missing artwork");
        findMissingArtwork();

        gracefullyExit();

    }

    public static void main(String args[]) {
        searchOnlineForArtwork("Robert Plant The Principle of Moments");
System.exit(0);

        iTunesInterfaceCom4J itt = new iTunesInterfaceCom4J();
        itt.runAsCLI();
    }

    static void gracefullyExit() {
//        ComThread.Release();
        System.exit(0);
    }

    IITPlaylist choosePlaylist() {
        // Display a list of playlists
        IITSource source = getLibrarySource();
        IITPlaylistCollection playlists = source.playlists();
        for(int j=1; j<=playlists.count(); j++) {
            IITPlaylist playlist = playlists.item(j);
            String playlistName = playlist.name();
            System.out.println(j + " - " + playlistName);
        }

        // Ask user which playlist to scan
        System.out.println("========================");
        System.out.print("Choose a playlist : ");
        Scanner in = new Scanner(System.in);
        int answer = 0;
        while(answer < 1 || answer > playlists.count()) {
            try {
                answer=in.nextInt();
            } catch(NoSuchElementException e) {
                System.err.println("Caught an exception in the line scanner");
                return null;
            }
        }

        // If they gave a valid playlist number, scan it
        if(answer > 0 && answer <= playlists.count()) {
            return(playlists.item(answer));
        }
        return null;
    }
    
    public void findMissingArtwork() {
        IITPlaylist playlist = choosePlaylist();
        if(playlist != null) {
            scanPlaylistForMissingArtwork(playlist);
        }
    }

    public void scanPlaylistForMissingArtwork(IITPlaylist playlist) {
        // When we come across a track without artwork, check to see if we've already
        // found a track from that album which *does* have artwork (artworkedAlbums).
        // If not, add this track to barrenTracks and add the album to barrenAlbums
        //
        // When we come across a track which *does* have artwork, check to see if
        // the album is listed in barrenAlbums. If so, remove the entry from barrenAlbums
        // and add this track to artworkedAlbums.
        Hashtable<String,IITTrack> artworkedAlbums = new Hashtable();  // use AlbumHash for the key, track for value
        Hashtable<String,IITTrack> barrenAlbums = new Hashtable();  // use AlbumHash for the key, any track from Album for value
        Hashtable<String,IITTrack> barrenTracks = new Hashtable(); // use TrackHash for the key, track for value

        IITTrackCollection tracks = playlist.tracks();
        int trackCount = tracks.count();
        for(int k=1; k<=trackCount; k++) {
            IITTrack track = tracks.item(k);
            IITArtworkCollection artworks = track.artwork();
            int artworkCount = artworks.count();
            String albumHash = albumHash(track);
            String trackHash = getTrackHash(track);
            if(artworkCount == 0) {
                // Add this track to the list of tracks to fix
                barrenTracks.put(trackHash, track);

                // If this album isn't a known-barren or a known-artworked one, then we add it to barrenAlbums
                if(! artworkedAlbums.containsKey(albumHash) && ! barrenAlbums.containsKey(albumHash)) {
                    barrenAlbums.put(albumHash,track);
                }
            } else {
                // This has artwork. Let's see if its from an album in barrenAlbums
                if(barrenAlbums.containsKey(albumHash)) {
                    // We've encountered tracks from this album before, but they didn't have artwork.
                    // So, remove the album from barrenAlbums and add it to artworkedAlbums
                    barrenAlbums.remove(albumHash);
                    artworkedAlbums.put(albumHash,track);
                }
//                if(track.is(IITFileOrCDTrack.class)) {
//                    IITFileOrCDTrack fileBasedTrack = track.queryInterface(IITFileOrCDTrack.class);

                if(track.kind() == ITTrackKind.ITTrackKindFile) {
                    IITFileOrCDTrack fileBasedTrack = track.queryInterface(IITFileOrCDTrack.class);

                    if(artworkCount != 1  && verifyTrackExists(fileBasedTrack)==true) {
                        // First off, it's possible for artwork to get removed from files and for iTunes to not
                        // be aware of it *until* it actually tries to access the artwork. So, let's make sure that
                        // all of the artwork is really there... - JAE 03-17-2011
                        try {
                        } catch(IllegalStateException e) {
                            // This sometimes happens...
                            try {
                                System.out.println("iTunes is having trouble with the file. Sleeping...");
                                Thread.sleep(2000);
                                System.out.println("4.1 " + track.artist() + " - " + track.album() + " - " + track.name());
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                System.out.println("Interrupted from sleep");
                            }
                        }
                        for(int i=1; i<=artworkCount; i++) {
                            try {
                            System.out.println("   Looking at " + fileBasedTrack.name());
                            } catch(Exception e) {
                                e.printStackTrace(System.out);
                                System.exit(1);
                            }
                            IITArtwork artwork = artworks.item(i);
                            String filebase = System.getenv("TEMP");
                            String filename = filebase + "\\temporaryartworkfile.jpg";
                            try {
                                artwork.saveArtworkToFile(filename);
                                (new File(filename)).delete();
                            } catch(java.lang.IllegalStateException e) {
                                // This is where we come if the artwork was missing...
                                System.out.println("Whoops! iTunes thought there was more artwork than there really was...");
//                            } catch(ComFailException e) {
//                                // This is where we come if the actual file is missing
//                                if(verifyTrackExists(fileBasedTrack)==false) {
//                                    System.out.println("Looks like this file has gone missing...");
//                                }
                            }
                        }

                        // Get the new artwork count...
                        artworkCount = artworks.count();

                        // Okay, now *this* time, the artworkCount should be accurate. Check for multiple artworks again
                        if(artworkCount != 1) {
                            System.out.println("    " + fileBasedTrack.name() + " has " + artworkCount + " pieces of artwork");
                            ArtworkViewer viewer = new ArtworkViewer(fileBasedTrack.name() + " from " + fileBasedTrack.album() + " by " + fileBasedTrack.artist());
                            for(int i=1; i<=artworks.count(); i++) {
                                IITArtwork artwork = artworks.item(i);
                                viewer.add(new iTunesArtworkCom4J(artwork),i);
                            }
                            viewer.choose();
        //                    gracefullyExit();
                        }
                    }
                }
            }
        }

        System.out.println();
        System.out.println("=============================");
        System.out.println("Songs needing artwork from other tracks:");
        System.out.println("=============================");
        for(IITTrack track : barrenTracks.values()) {
//        Enumeration e = barrenTracks.elements();
//        while(e.hasMoreElements()) {
//            IITTrack track = (IITTrack) e.nextElement();
            String albumHash = albumHash(track);

            if(artworkedAlbums.containsKey(albumHash)) {
                IITTrack goodTrack = artworkedAlbums.get(albumHash);
                System.out.println(track.name() + " is lacking artwork, but " + getTrackHash(goodTrack) + " from the same album has it");
            }
        }

        System.out.println();
        System.out.println("=============================");
        System.out.println("The following albums are lacking ANY track with artwork:");
        System.out.println("=============================");
//        e = barrenAlbums.keys();
//        while(e.hasMoreElements()) {
        for(String albumHash : barrenAlbums.keySet()) {
//            String albumHash = (String) e.nextElement();
            IITTrack track = barrenAlbums.get(albumHash);

            System.out.println("\"" + albumHash + "\", added (" + track.dateAdded() + ") needs artwork");
        }
    }

    public void syncFolders() {
        System.out.println("");
        System.out.println("");
        System.out.println("==================================");
        System.out.println("    Syncing watchlists...");
        System.out.println("==================================");
        IITSource source = getLibrarySource();
        if(GUI.getHostname().equals("DAMIEN")) {
            syncFolderToPlaylist(source, "H:/MP3's/Downloaded Albums", "Downloaded Albums");
            syncFolderToPlaylist(source, "H:/MP3's/Collected", "Collected");
            syncFolderToPlaylist(source, "H:/MP3's/Albums", "Albums");
        } else {
            syncFolderToPlaylist(source, "G:/MP3's/Downloaded Albums", "Downloaded Albums");
            syncFolderToPlaylist(source, "G:/MP3's/Collected", "Collected");
            syncFolderToPlaylist(source, "G:/MP3's/Albums", "Albums");
        }
    }

    public void syncFolderToPlaylist(IITSource source, String folderName, String playlistName) {
        System.out.println("");
        System.out.println("   ==========================");
        System.out.println("   Trying to sync " + playlistName + " to " + folderName);
        System.out.println("   ==========================");
        boolean found = false;
        IITPlaylistCollection playlists = source.playlists();
        for(int i=1; i<=playlists.count(); i++) {
            IITPlaylist playlist = playlists.item(i);
            if(playlist.name().equals(playlistName) && playlist.kind() == ITPlaylistKind.ITPlaylistKindUser) {
//                IITUserPlaylist userPlaylist = (IITUserPlaylist) playlist;
//                IITUserPlaylist userPlaylist =it.creat
//                        new IITUserPlaylist(playlist);

                IITUserPlaylist userPlaylist = playlist.queryInterface(IITUserPlaylist.class);

                found = true;
                System.out.println("Found playlist: " + playlistName);

                // First, get a list of all of the file locations that we know about
                Hashtable<String,IITFileOrCDTrack> trackHash = getHashtableOfTrackLocations(userPlaylist);

                // Processed all of the playlist tracks. Now, scan the hard drive folder....
                Hashtable<String,File> filesToAdd = searchFoldersForNewMusic(new File(folderName), trackHash);

                // By now, the search should have removed from trackHash all of the paths which
                // were found. That leaves only the tracks which weren't found in the search
                // folder. So, we should only have the "wayward" ones which are in strange locations
                for(Enumeration<IITFileOrCDTrack> en = trackHash.elements(); en.hasMoreElements();) {
                    IITFileOrCDTrack track = en.nextElement();
                    System.out.println(track.location() + " was in the library, but not in the folder it's supposed to be");
                }

                for(Enumeration<String> en = filesToAdd.keys(); en.hasMoreElements();) {
                    String path = en.nextElement();
                    if(TEST) {
                        System.out.println("  Not really adding : " + path);
                    } else {
                        System.out.println("  Adding : " + path);
                        addFileToPlaylist(path, userPlaylist);
                    }
                }

            }
        }
        if(! found) {
            System.out.println("Playlist wasn't found");
        }

//        searchFoldersForNewMusic(new File(folderName));
        System.out.println("Done");
    }

    public void addFileToPlaylist(String filename, IITUserPlaylist playlist) {
        IITOperationStatus status = playlist.addFile(filename);
        if(status != null) {
            try {
                while(status.inProgress()) {
                    System.out.println("Sleeping while iTunes processes the file...");
                    try {
                        Thread.currentThread().sleep(500);
                    } catch(InterruptedException e) {
                        System.out.println(e);
                        e.printStackTrace();
                    }
                }
                IITTrackCollection tracks = status.tracks();
                if(tracks.count() != 1) {
                    System.out.println("  WARNING: Something went wrong. We meant to add 1 file and we added " + tracks.count());
                }
            } catch(IllegalStateException e) {
                System.out.println("  WARNING: Wasn't able to add : " + filename);
            }
        } else {
            System.out.println("status is null. Guess we're going to have to trust iTunes...");
        }
    }

    public void removeMissingFilesFromLibrary() {

        Vector<IITFileOrCDTrack> vec = new Vector();

        IITLibraryPlaylist libraryPlaylist = it.libraryPlaylist();
        IITTrackCollection tracks = libraryPlaylist.tracks();
        for(int j=1; j<=tracks.count(); j++) {
            IITTrack track = tracks.item(j);
            if(track.kind() == ITTrackKind.ITTrackKindFile){
                IITFileOrCDTrack fileTrack = track.queryInterface(IITFileOrCDTrack.class);
                if(fileTrack != null) {
                    if(fileTrack.location() != null) {
                        if(fileTrack.location().equals("")){
                            System.out.println("Removing " + fileTrack.artist() + " - " + fileTrack.name());
                            vec.add(fileTrack); // Add to the list of things to delete.
                        } else {
                            if((new File(fileTrack.location())).exists()) {
                            } else {
                                System.out.println("File does NOT exist");
                            }
                        }
                    } else {
                        System.out.println("fileTrack location was null: " + fileTrack.name());
                    }
                }
            }
        }

        // Delete them....
        for(int i=0; i<vec.size(); i++) {
            if(TEST) {
                System.out.println("Not really deleting missing library item: " + ((IITFileOrCDTrack) vec.elementAt(i)).name());
            } else {
                System.out.println("Deleting missing library item: " + vec.elementAt(i));
                (vec.elementAt(i)).delete();
            }
        }
    }

    public boolean verifyTrackExists(IITFileOrCDTrack fileBasedTrack) {
        if(fileBasedTrack != null) {
            if(fileBasedTrack.location().equals("")){
                System.out.println("File location for " + fileBasedTrack.name() + " equals empty string");
                return false;
            } else {
                if(! (new File(fileBasedTrack.location())).exists()) {
                    System.out.println("File does NOT exist");
                    return false;
                }
            }
        }
        return true;
    }

    public static Hashtable<String,IITFileOrCDTrack> getHashtableOfTrackLocations(IITUserPlaylist playlist) {
        Hashtable<String,IITFileOrCDTrack> trackHash = new Hashtable();
        IITTrackCollection tracks = playlist.tracks();
        for(int j=1; j<=tracks.count(); j++) {
            IITTrack track = tracks.item(j);
            if(track.kind() == ITTrackKind.ITTrackKindFile){
                IITFileOrCDTrack file = track.queryInterface(IITFileOrCDTrack.class);
                if(file != null) {
                    String location = file.location();
                    if(location != null) {
                        if(location.equals("")){
                            System.out.println("File location for " + file.name() + " equals empty string");
                        } else {

                            if((new File(location)).exists()) {
                                if(trackHash != null) {
                                    if(trackHash.containsKey(location)) {
                                        // We already have this file in the library. Remove the duplicate...
                                        System.out.println("This item is in the playlist already : " + file.location());
                                    } else {
                                        trackHash.put(file.location(),file);
                                    }
                                }
                            } else {
                                System.out.println("File does NOT exist");
                            }
                        }
                    } else {
                        System.err.println("WARNING: file.location() is null for " + file);
                    }
                }
            }
        }
        return(trackHash);
    }

    public static String[] musicEndings = { ".mp3", ".m4a" };
    public static String[] nonMusicEndings = { ".fslockfile", ".jpg", ".db", ".DS_Store", ".ini"};

    public static Hashtable<String,File> searchFoldersForNewMusic(File file, Hashtable<String,IITFileOrCDTrack> trackHash) {
        Hashtable<String,File> filesToAdd = new Hashtable();
        searchFoldersForNewMusic(file, trackHash, filesToAdd);
        return(filesToAdd);
    }

    /**
     * This method actually finds discrepancies between a playlist and a folder that it's supposed to be
     * sync'd to. It needs a folder to scan and a hashtable of file paths from a playlist. As it scans
     * the folder for media, any file found is removed from trackHash, if it exists in trackHash. If it
     * was *not* in trackHash. When it is finished, trackHash should contain all file paths which were
     * *not* found during the folder scan (either because the paths exist *outside* of this folder tree or
     * because the files were deleted) and filesToAdd should contain all file paths which were not found
     * in the trackHash.
     * @param file A directory to scan
     * @param trackHash A hashtable of the paths to all of the actual files in a playlist.
     * @param filesToAdd An empty hashtable, initially.
     */
    public static void searchFoldersForNewMusic(File file, Hashtable<String,IITFileOrCDTrack> trackHash, Hashtable<String,File> filesToAdd) {
        if(file.isDirectory()) {
//            System.out.println("Searching in..." + file.name());
            String internalNames[] = file.list();
            for(int i=0; i<internalNames.length; i++) {
                searchFoldersForNewMusic(new File(file.getAbsolutePath() + "\\" + internalNames[i]), trackHash, filesToAdd);
            }
        } else {
            String abspath = file.getAbsolutePath();
            String lowerpath = abspath.toLowerCase();
            if(endsWith(lowerpath,musicEndings)) {  // Process all known music files
                if(! trackHash.containsKey(abspath)) { // This file isn't in the playlist
                    System.out.println("reached a file..." + file.getName());
                    filesToAdd.put(file.getAbsolutePath(),file);
                } else {  // Remove any found music from the trackHash
                    trackHash.remove(abspath);
                }
            } else if(endsWith(lowerpath,nonMusicEndings)) {  // Skip stuff that we know isn't music
            } else {  // Warn about anything that we're unsure about
                System.out.println("Unknown file extension : " + abspath);
            }
        }
    }

    public static boolean endsWith(String str, String endings[]) {
        for(int i=0; i<endings.length; i++) {
            if(str.endsWith(endings[i])) {
                return(true);
            }
        }
        return false;
    }

    static void searchOnlineForArtwork(String strings) {
        String baseURL = "http://www.albumartexchange.com/covers.php";
        String fullURL = baseURL + "?q=" + strings.replace(' ','+');
        try {
            URL url = new URL(fullURL);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            // Sample line that we want to match
            // <a href="?id=65865&amp;q=Robert+Plant+The+Principle+of+Moments"><img src="/gallery/images/public/rp/_rplant-princi_02.tn.jpg" width=150 height=150 border=0 alt="The Principle of Moments; Robert Plant; JPEG; 600&times;600" style="vertical-align:top"></a>
            String fullLinkRegExp = "<a href=\"(\\?id=[0-9]+[^\"]*)\">";
            String thumbLinkRegExp = "<img src=\"([^\"]+)\".*([0-9]+)&times;([0-9]+)";
            String regexp = fullLinkRegExp;
            while((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
                Pattern pattern = Pattern.compile(regexp);
                Matcher matcher = pattern.matcher(inputLine);
                while (matcher.find()) {
                    System.out.println(matcher.group(0));
                    String fullLink = matcher.group(1);
//                    String thumbLink = matcher.group(2);
//                    String width = matcher.group(3);
//                    String height = matcher.group(4);
                    System.out.println("  " + fullLink);
//                    System.out.println("  " + width + "x" + height + "   " + fullLink);
//                    System.out.println("  " + thumbLink);

                }
            }
            in.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}