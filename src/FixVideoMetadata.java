import com4jitunes.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixVideoMetadata {

    public static IiTunes it;

    static int highest_season = 0;
    static int[] highest_episode = new int[100];
    static int[] lowest_episode = new int[100];
    static int[] episode_spread = new int[100];
    static int[] episodes_in_season = new int[100];
    static boolean increment_seasons = false;

    public static void main(String args[]) {
        it = ClassFactory.createiTunesApp();

        // Figure out the source for the main library
        IITSourceCollection sources = it.sources();
        IITSource source = null;
        int sourceCount = sources.count();

        // Go through sources until we find the main library
        for(int i=1; i<= sourceCount; i++) {
            if(sources.item(i).kind() == ITSourceKind.ITSourceKindLibrary) {
                source = sources.item(i);

                IITPlaylistCollection playlists = source.playlists();
                // Go through the main library until we find the playlist we want (Movies, TV Shows, etc)
                for(int j=1; j<=playlists.count(); j++) {
                    IITPlaylist playlist = playlists.item(j);
                    String playlistName = playlist.name();
//                    if(playlistName.equals("Movies")) {
                    if(playlistName.equals("TV Shows")) {
                        
                        System.out.println(j + " - " + playlistName);

                        IITTrackCollection tracks = playlist.tracks();
                        int trackCount = tracks.count();


                        // Go through the tracks in that playlist
                        for(int pass=1; pass<=2; pass++) {
                            // We do this in two passes. First, we find out the total number of episodes in each season
                            // and the total number of seasons.
                            for(int k=1; k<=trackCount; k++) {
                                IITTrack track = tracks.item(k);
    //                            IITFileOrCDTrack fileTrack = new IITFileOrCDTrack(track);
    //                            if(fileTrack.getShow().startsWith("Photoshop CS5 One-on-One Fundamentals")) {

                                // We want to match files like "0001 Welcome"
                                Pattern[] nameRegEx = new Pattern[4];
                                nameRegEx[0] = Pattern.compile("([0-9]{2})[. ]?([0-9]{2})[.]? (.*)"); // Lynda.com style 1
                                nameRegEx[1] = Pattern.compile("[0-9]{5}_([0-9]{2})_([0-9]{2})_[A-Z]{2}[0-9]{2}_(.*)"); // Lynda.com style 2
                                nameRegEx[2] = Pattern.compile("Good Eats - S([0-9]{2})[ES]([0-9P]{2}) - (.*)"); // Good Eats style
                                nameRegEx[3] = Pattern.compile("Lynda.com - Adobe After Effects CS4.* - C([0-9]{2})L([0-9]{2}) - (.*)");

                                Matcher matcher = null;
                                for(int l=0; l<nameRegEx.length; l++) {
                                    matcher = nameRegEx[l].matcher(track.name());
                                    if(matcher.find()) { // If it matches, then bail out and let the next section have it
//                                        System.out.println("Matched pattern #" + l);
                                        l=nameRegEx.length;
                                        matcher.reset();
                                    }
                                }

                                if(matcher.find()) {
                                    String artist = "Lynda.com";
                                    String showname = "Adobe After Effects CS4 Essential Training";
                                    int season_num = Integer.parseInt(matcher.group(1));
                                    int episode_num = Integer.parseInt(matcher.group(2));
                                    String rest = matcher.group(3);

                                    if(track.is(IITFileOrCDTrack.class)) {

                                        IITFileOrCDTrack fileTrack = track.queryInterface(IITFileOrCDTrack.class);

                                        String currentAlbum = fileTrack.album()==null?"":fileTrack.album();
                                        String currentArtist = fileTrack.artist()==null?"":fileTrack.artist();
                                        String currentShow = fileTrack.show()==null?"":fileTrack.show();

                                        if(currentAlbum.equals(showname) || currentArtist.equals(showname) || currentShow.equals(showname)) {

                                            if(pass==1) {
                                                // Pass 1. Find out the total number of seasons and episodes.
                                                if(season_num > highest_season)
                                                    highest_season = season_num;
                                                // If this is a lower episode than we've seen OR if this is the *first* episode we've seen
                                                // for this season, then update lowest_episode[season_num]
                                                if(episode_num < lowest_episode[season_num] || episodes_in_season[season_num] == 0) {
                                                    System.out.println("Setting lowest episode for season " + season_num + " to " + episode_num);
                                                        lowest_episode[season_num] = episode_num;
                                                }
                                                if(episode_num > highest_episode[season_num])
                                                    highest_episode[season_num] = episode_num;

                                                // Increment the total number of episodes found for this season. This might
                                                // be different from episode_spread if we're missing some episodes (or if there
                                                // are two episodes with the same season/episode numbers)
                                                episodes_in_season[season_num]++;
                                                episode_spread[season_num] = highest_episode[season_num] + 1 - lowest_episode[season_num];

                                                // If there's a season "0", then we'll need to increment the season numbers by 1
                                                if(season_num == 0)
                                                    increment_seasons = true;
                                            } else {
                                                // Pass 2. Do the tagging..
                                                String newname = capitalize(rest);

                                                //TODO
                                                episode_num+=(1-lowest_episode[season_num]);

                                                if(increment_seasons) {
                                                    // There's a season "0" in this set, which we don't want. So, we need to add 1 to the season number
                                                    season_num++;
                                                }

                                                String num_discstring = "0" + season_num;
                                                num_discstring = num_discstring.substring(num_discstring.length()-2);
                                                String alphanum_discstring = "C" + num_discstring;
                                                String num_trackstring = "0" + episode_num;
                                                num_trackstring = num_trackstring.substring(num_trackstring.length()-2);
                                                String alphanum_trackstring = "L" + num_trackstring;
                                                String num_episodeString = num_discstring + num_trackstring;
                                                String alphanum_episodeString = alphanum_discstring + alphanum_trackstring;

                                                int total_seasons = highest_season + (increment_seasons?1:0);

                                                int sequence_num = getAbsoluteSequenceNumber(season_num,episode_num);

                                                newname = num_episodeString + " " + newname;
                                                System.out.println(fileTrack.name() + "  ->  " + alphanum_episodeString + "   ->   " + newname + " -  season " + season_num + "/" + total_seasons + "  episode " + episode_num + "/" + episode_spread[season_num] + " -> " + num_discstring + num_trackstring);

                                                boolean force_one_season = true;

                                                    // Here's where we really change the metadata...
                //                                fileTrack = null;
                                                boolean TEST = false;

                                                if(fileTrack != null) {
                                                    if(! TEST) {

                                                        fileTrack.artist(artist);
                                                        fileTrack.albumArtist(artist);
                                                        fileTrack.sortArtist(artist);
                                                        fileTrack.sortAlbumArtist(artist);

    //                                                    fileTrack.setAlbum(showname);
                                                        fileTrack.show(showname);

                                                        fileTrack.setName(newname);

                                                        fileTrack.discNumber(season_num);
                                                        fileTrack.discCount(total_seasons);

                                                        if(force_one_season) {
//                                                            fileTrack.seasonNumber(1);
                                                            fileTrack.episodeNumber(sequence_num);
                                                        } else {
                                                            fileTrack.seasonNumber(season_num);
                                                            fileTrack.episodeNumber(episode_num);
                                                        }

                                                        fileTrack.trackNumber(episode_num);
                                                        fileTrack.trackCount(episode_spread[season_num]);
    //                                                    fileTrack.setEpisodeNumber(episode);
                                                        fileTrack.episodeID(alphanum_episodeString);
                                                        fileTrack.show(showname);
                                                        fileTrack.episodeID(alphanum_episodeString);
                                                        fileTrack.description(showname + " - " + alphanum_episodeString + " - " + newname);
                    //                                    fileTrack.setVideoKind(ITVideoKind.ITVideoKindTVShow);
                    //                                        fileTrack.setTrackCount(tracksondisc[discnum]);
                    //                                        fileTrack.setTrackNumber(tracknum);
                    //                                    fileTrack.setSeasonNumber(chapter);

                                                    } else {
                                                        System.out.println("In test mode. Not really doing it");
                                                    }
                                                } else {
                                                    System.out.println("fileTrack was null, for some reason");
                                                }
                                            }
                                        } else {
    //                                        System.out.println(fileTrack.getAlbum() + " doesn't equal " + showname);
                                        }
                                    } else {
                                        System.out.println("Track wasn't a FileOrCD kind");
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
//        ComThread.Release();
//        it.quit();
//        it.dispose();
        System.exit(0);
    }

    // Capitalize all big words. Lowercase the ones like "a", "the", etc..
    static String capitalize(String str) {
        ArrayList<String> lowerwords = new ArrayList<String>();
        lowerwords.add("a");
        lowerwords.add("an");
        lowerwords.add("in");
        lowerwords.add("on");
        lowerwords.add("to");
        lowerwords.add("vs");
        lowerwords.add("and");
        lowerwords.add("the");
        lowerwords.add("for");
        lowerwords.add("with");
        lowerwords.add("iPad");
        lowerwords.add("iPod");
        lowerwords.add("iPhone");

        // If there are any underscores, turn them into spaces.
        // Capitalize all big words
        String[] parts = str.split("[ _]");
        String newstr = "";
        for(int m=0; m<parts.length; m++) {
            String addthis = parts[m];
            if(lowerwords.contains(parts[m]) && m!=0) {
                newstr = newstr + addthis + " ";
            } else {
                newstr = newstr + addthis.substring(0,1).toUpperCase() + addthis.substring(1) + " ";
            }
        }
        newstr = newstr.trim();
        return newstr;
    }

    // Given a season number and an episode number, this will return what number
    // this episode is (in other words, it will return the number of episodes, including
    // those in previous seasons, plus 1).
    static int getAbsoluteSequenceNumber(int season, int episode) {
        int seq_num = 1;

        int start_season = increment_seasons?0:1;
        // Total up all of the episodes in seasons before the one we're looking at
        for(int i=start_season; i<season; i++) {
            //seq_num += episodes_in_season[i];
            seq_num += episode_spread[i];
        }
        // TODO: This has a flaw. If there are gaps in the episodes, this won't be aware of them.
        seq_num += episode - lowest_episode[season];

        System.out.println("Returning absolute sequence number of " + seq_num + " for season:" + season + " episode:" + episode);
        return seq_num;
    }

/*
//        Soundbooth
        String[][] soundbooth = new String[20][7];
        soundbooth[1][1] = "Understanding Digital Audio";
        soundbooth[1][2] = "Overview of Commonly Used Panels";
        soundbooth[1][3] = "Importing Files";
        soundbooth[1][4] = "Playing Audio";
        soundbooth[1][5] = "Audio Levels";
        soundbooth[1][6] = "Working with Premiere� & After Effects� Files";
        soundbooth[2][1] = "Normalizing Audio";
        soundbooth[2][2] = "Matching Volume among Multiple Files";
        soundbooth[2][3] = "Editing Audio";
        soundbooth[2][4] = "Working with Noise Reduction Tools";
        soundbooth[2][5] = "Time Stretching & Pitch Shifting";
        soundbooth[3][1] = "Adding Effects in the Effect Rack";
        soundbooth[3][2] = "Using the Graphic EQ Effect";
        soundbooth[3][3] = "Using Advanced Parametric EQ & Applying an Effect Preset";
        soundbooth[3][4] = "Using the Advanced Dynamics Effect";
        soundbooth[3][5] = "Using the Analog Delay & Convolution Reverb Effects";
        soundbooth[3][6] = "Using the Vocal Enhancer & Advanced Mastering Effects";
        soundbooth[4][1] = "Creating a New Multitrack Project";
        soundbooth[4][2] = "Mixing & Keyframing";
        soundbooth[4][3] = "Resource Central, Editing a Score & Exporting a Mixdown";
        soundbooth[4][4] = "Recording a Voice Over";
        soundbooth[5][1] = "Adding Metadata to Files";
        soundbooth[5][2] = "Analyzing Speech with a Reference Script";
        soundbooth[5][3] = "Adding Flash� Cue Point Markers";
        soundbooth[5][4] = "Exporting Stereo Channels to Mono Files";
        soundbooth[5][5] = "Adjusting Pitch & Timing, Saving Files";
        soundbooth[5][6] = "Credits";


        // Photoshop
        String[][] photoshopCS5OneOnOne = new String[16][7];
        photoshopCS5OneOnOne[1][1]="Pixels & Resolution";
        photoshopCS5OneOnOne[1][2]="Color";
        photoshopCS5OneOnOne[1][3]="Exploring the Interface";
        photoshopCS5OneOnOne[1][4]="Choosing File Formats";
        photoshopCS5OneOnOne[1][5]="Viewing & Navigating Images";
        photoshopCS5OneOnOne[2][1]="Setting User Preferences";
        photoshopCS5OneOnOne[2][2]="Choosing Color Settings";
        photoshopCS5OneOnOne[2][3]="Customizing the Workspace";
        photoshopCS5OneOnOne[2][4]="Essential Keyboard Shortcuts";
        photoshopCS5OneOnOne[2][5]="Using Rulers & Guides";
        photoshopCS5OneOnOne[3][1]="Understanding the Histogram & Levels";
        photoshopCS5OneOnOne[3][2]="Learning the Curves Tool";
        photoshopCS5OneOnOne[3][3]="Exploring Hue & Saturation";
        photoshopCS5OneOnOne[3][4]="Cropping & Straightening";
        photoshopCS5OneOnOne[3][5]="Adjusting Color Balance";
        photoshopCS5OneOnOne[3][6]="Creating a Black & White Image";
        photoshopCS5OneOnOne[4][1]="Manual Selection Tools";
        photoshopCS5OneOnOne[4][2]="Using the Quick Selection Tool";
        photoshopCS5OneOnOne[4][3]="Creating Color-Based Selections & Saving Selections";
        photoshopCS5OneOnOne[5][1]="Using Adjustment Layers";
        photoshopCS5OneOnOne[5][2]="Understanding Layer Masks";
        photoshopCS5OneOnOne[5][3]="Creating Gradient Masks";
        photoshopCS5OneOnOne[5][4]="Applying Layer Blend Modes";
        photoshopCS5OneOnOne[5][5]="Snapshots & the History Brush";
        photoshopCS5OneOnOne[6][1]="The Retouching Tools, including Content-Aware Fill";
        photoshopCS5OneOnOne[6][2]="Removing Noise from an Image";
        photoshopCS5OneOnOne[6][3]="Cloning with Perspective with the Vanishing Point Filter";
        photoshopCS5OneOnOne[6][4]="Adjusting Perspective with the Lens Correction Filter";
        photoshopCS5OneOnOne[6][5]="Content-Aware Scaling";
        photoshopCS5OneOnOne[7][1]="Working with Type";
        photoshopCS5OneOnOne[7][2]="Paragraph & Character Formatting Options";
        photoshopCS5OneOnOne[7][3]="Typing on a Path";
        photoshopCS5OneOnOne[7][4]="Applying Layer Styles to Type";
        photoshopCS5OneOnOne[8][1]="Vectors & the Shape Tools";
        photoshopCS5OneOnOne[8][2]="Creating Paths";
        photoshopCS5OneOnOne[8][3]="Saving & Loading Paths as Selections";
        photoshopCS5OneOnOne[8][4]="Subtracting, Adding Intersecting, & Excluding";
        photoshopCS5OneOnOne[9][1]="Vector Graphics as Smart Objects";
        photoshopCS5OneOnOne[9][2]="Creating & Replacing Smart Objects";
        photoshopCS5OneOnOne[9][3]="Creating Multiple Instances of a Smart Object";
        photoshopCS5OneOnOne[9][4]="Smart Objects Deserve Smart Filters";
        photoshopCS5OneOnOne[10][1]="Exploring the Workspace";
        photoshopCS5OneOnOne[10][2]="Searching & Organizing Files";
        photoshopCS5OneOnOne[10][3]="Leveraging Metadata";
        photoshopCS5OneOnOne[10][4]="Outputting";
        photoshopCS5OneOnOne[10][5]="Using Mini Bridge";
        photoshopCS5OneOnOne[11][1]="Using the Basic Panel";
        photoshopCS5OneOnOne[11][2]="Applying Selective Edits";
        photoshopCS5OneOnOne[11][3]="Converting to Black & White";
        photoshopCS5OneOnOne[11][4]="Repairing an Image";
        photoshopCS5OneOnOne[12][1]="Manipulating Image Layers";
        photoshopCS5OneOnOne[12][2]="Mastering the Layers Panel";
        photoshopCS5OneOnOne[12][3]="Creating Layer Groups & Comps";
        photoshopCS5OneOnOne[13][1]="Combining Images & Merging the Best Elements";
        photoshopCS5OneOnOne[13][2]="Creating Panoramas";
        photoshopCS5OneOnOne[13][3]="Auto-Aligning Layers";
        photoshopCS5OneOnOne[13][4]="Refine Edges";
        photoshopCS5OneOnOne[14][1]="Exploring the Brush Panel";
        photoshopCS5OneOnOne[14][2]="Creating a Custom Brush";
        photoshopCS5OneOnOne[14][3]="Changing Brush Dynamics";
        photoshopCS5OneOnOne[14][4]="Painting with the Mixer Brush";
        photoshopCS5OneOnOne[15][1]="Sharpening Images with Smart Sharpen";
        photoshopCS5OneOnOne[15][2]="Setting Up the Print Dialog Box";
        photoshopCS5OneOnOne[15][3]="Saving for the Web";
        photoshopCS5OneOnOne[15][4]="Credits";
*/

}