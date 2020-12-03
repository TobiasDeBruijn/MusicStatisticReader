package nl.thedutchmc.ytmusicreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import nl.thedutchmc.ytmusicreader.Http.RequestMethod;

public class YoutubeMusic {

	private static JSONTokener parser;
	private static int MAX_OUTPUT = 10;
	
	private static String API_KEY = "";
	private static boolean useApi = true;
	
	public static void main(String[] args) {
		logInfo("YTMusicReader by TheDutchMC/Dutchy76\n");
		
		//Read the API key from authkey.txt
		try {
			
			//Get the authkey file, which is in the same folder as the JAR
			final File jarPath = new File(YoutubeMusic.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			final File folderPath = new File(jarPath.getParentFile().getPath());
			final File authKeyFile = new File(folderPath, "authkey.txt");
			
			//Check if the file exists
			//if not, we wont use API checking
			//if yes, read the file
			if(!authKeyFile.exists()) {
				logWarn("Warning: No authkey.txt file found. No requests will be made to the Google APIs!");
				useApi = false;
			} else {
				BufferedReader br = new BufferedReader(new FileReader(authKeyFile));
				API_KEY = br.readLine();
				br.close();
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}

		//Check if the user provided any arguments
		//If not, we cant parse, because the user didnt provide a file for us to parse
    	if(args.length == 0) {
    		logWarn("No file path provided! Exiting");
    		logWarn("Usage: java (arguments) - jar YTmusicReader.jar <file path> [year] [max output]");
    		System.exit(1);
    	}
    	
    	String year, nextYear;
    	
    	//Check if the user provided 2 arguments
    	//This means that they will have entered a year
    	if(args.length > 1) {
    		logInfo("Using year " + args[1]);
    		year = args[1];
    		nextYear = String.valueOf(Integer.valueOf(year) + 1);
    	} else {
    		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
    		year = formatter.format(new Date());
    		nextYear = String.valueOf(Integer.valueOf(year) + 1);
    		
    		logInfo("No year provided. Using current year: " + year + "\n");
    	}
    	
    	//Check if the user provided us 3 arguments
    	//This means that they want a different size of top artist or top songs
    	if(args.length > 2) {
    		logInfo("Setting output to " + args[2]);
    		MAX_OUTPUT = Integer.valueOf(args[2]);
    	}
    	
    	//Create a File object for the user's acivity json file
    	File file = new File(args[0]);
    	    
    	//Open it in to a JSONTokener
        try {
			parser = new JSONTokener(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logWarn("Your watch history file is not a valid format! You sure this is the correct file? Exiting");
			System.exit(1);
		} catch (FileNotFoundException e) {
			logWarn("Can't find your watch history file! Exiting");
			System.exit(1);
		}
        
        logInfo("Parsing your statistics...");
        
        List<String> artists = new ArrayList<String>();
        List<String> songs = new ArrayList<String>();
        List<String> videoIdsToCheck = new ArrayList<>();
        
        JSONArray json = new JSONArray();
        
        try {
            json = (JSONArray) parser.nextValue();

            //Iterate over all JSONObjects in the file.
            for (Object o : json) {
            	
            	JSONObject jsonObj = (JSONObject) o;

            	//Check if the header is YTMusic, since the export also contains regular YouTube.
            	if(!jsonObj.get("header").equals("YouTube Music")) continue;
            	
            	//Get the title of the video.
            	//We don't care about the first 8 characters because it starts with "Watched "
                String title = ((String) jsonObj.get("title")).substring(8);
                
                //Get the date, since we only want the year specified by the user.
                Date time = getDateFromString((String) jsonObj.get("time"));

                //Thank Google for their inconsistency for this.
                //They don't always provide the song title and artist etc
                //so we got to figure that out ourselves.
                //We do this by asking the YouTube Data API
                if(title.startsWith("https://") && useApi) {                	                	
                	
                	//We don't care about the first 32 characters, because that is the url itself, we just want the video ID
                	String id = title.substring(32);
                	
                	//Check if the watch date falls within our time frame
                	//If not, continue on
                    if (!(time.after(getDateFromString(year + "-01-01T12:00:00")) && time.before(getDateFromString(nextYear + "-01-01T12:00:00")))) {
                    	continue;
                    }
                	
                    videoIdsToCheck.add(id);
                } else {
                	//Google provided us with an artist name already, so we don't have to ask the YouTube API
                	
                	//Check if the watch date falls within our time frame
                	//If not, continue on
                    if (!(time.after(getDateFromString(year + "-01-01T12:00:00")) && time.before(getDateFromString(nextYear + "-01-01T12:00:00")))) {
                    	continue;
                    }

                	//YouTube Music has the artist in a nested array called subtitles
                    JSONArray nestedJson = jsonObj.getJSONArray("subtitles");
                    
                    //Iterate over the array
                    //It's usually only 1 entry long, but we do it just in case Google does Google
                    String artist = "";
                    for(Object obj : nestedJson) {
                    	JSONObject jsonNestedObj = (JSONObject) obj;
                    	
                    	//Fix the artist name
                    	artist = fixArtistName((String) jsonNestedObj.get("name"));
                    }
                    
                    //Lastly, add the artist name and song title to the list
                	artists.add(artist);
                    songs.add(title);
                }
            }            
            
            //We don't want to send a thousand requests to the YouTube API
            //because of rate limits. So we combine IDs into groups of 50
            //Every entry in idsGrouped will be a comma-separated String.
            List<String> idsGrouped = new ArrayList<>();
            String tmp = "";
            for(int i = 0; i < videoIdsToCheck.size(); i++) {
            	String id = videoIdsToCheck.get(i);
            	tmp += id;

            	//We've got 50 IDs in the String now
            	//Add the tmp String to the list, and clear the tmp String
            	if(i % 50 == 0) {		
            		idsGrouped.add(tmp);
            		tmp = "";
            	} else {
            		//We're not at 50 yet, so we have to add a comma
            		tmp += ",";
            	}
            }

            //Let the user know how much we have to check, and let them know it might take a while
            logInfo("Checking " + videoIdsToCheck.size() + " video IDs against the YouTube-API in " + idsGrouped.size() + " requests... This may take a while!");
            
            //Iterate over the grouped IDs, and send requests
            for(String idString : idsGrouped) {
            	
            	//We need to set up the parameters
            	HashMap<String, String> params = new HashMap<>();
            	params.put("key", API_KEY);
            	params.put("id", idString);
            	params.put("part", "snippet");
            	
            	//Send a GET-Request to the YouTube Data API
            	Http.ResponseObject response = Http.request(RequestMethod.GET, "https://youtube.googleapis.com/youtube/v3/videos", params);
            	
            	//Turn the response message into a JSON Object and get the 'items' array within.
            	JSONObject jResponse = new JSONObject(response.getMessage());
            	JSONArray itemsArr = jResponse.getJSONArray("items");
                
            	//Iterate over the items
            	//Every item is another video.
            	for(Object o : itemsArr) {
            		JSONObject jO = (JSONObject) o;
            		
            		//The 'snippet' object contains the details we want
            		JSONObject snippet = jO.getJSONObject("snippet");
            		
            		//Get the song title and artist name
            		//We also want to fix the artist name.
                	String song = snippet.getString("title");
                	String artist = fixArtistName(snippet.getString("channelTitle"));

                	//Lastly, add the song title and artist name to the list
                	songs.add(song);
                	artists.add(artist);
            	}
            }
        	
        } catch (Exception e) {
        	logWarn("An exception occured: " + e.getMessage());
        }

        logInfo("Complete!\n");
        
        //Print the top artists based on how often they occurred.
        logInfo(">------- Top Artists -------<");
        int index = 1;
        for (Entry<Object, Long> s : getSortedList(artists)) {
        	logInfo(index + ". " + (String) s.getKey() + " - " + s.getValue() + " times");
            index++;
        }

        //Print the top songs based on how often they occurred
        index = 1;
        logInfo("\n>------- Top Songs -------<");
        for (Entry<Object, Long> s : getSortedList(songs)) {
            logInfo(index + ". " + (String) s.getKey() + " - " + s.getValue() + " times");
            index++;
        }
	}
	
	/**
	 * Transform a String date into a Date object
	 * Input format: yyyy-MM-dd'T'HH:mm:ss
	 * @param rawData The String date to transform
	 * @return Returns a Date object
	 * @throws ParseException Thrown when the input String isn't in the correct format
	 */
    private static Date getDateFromString(String rawData) throws ParseException {
        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return inFormat.parse(rawData);
    }
    
    /**
     * Sorts the input list
     * 
     * Besides that I've got no clue what it does, Justin made it /s
     * @param list The input list to sort
     * @return Returns the items sorted based on how often they occurred.
     */
    private static List<Entry<Object, Long>> getSortedList(List<?> list) {
        Map<Object, Long> map = list.stream().collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(MAX_OUTPUT).collect(Collectors.toList());
    }
    
    /**
     * This method is used to fix the artist's name
     * It will remove VEVO if it is in the name, fix leading and trailing spaces, and get it to the correct format
     * @param input The input to be fixed
     * @return Returns a String with the fixed name
     */
    private static String fixArtistName(String input) {
    	//YouTube does it in the format of 'ArtistName - Topic'
    	//We only care for everything before the hyphen
    	input = input.split("-")[0];
    	
    	//Remove leading and trailing spaces    	
    	input = input.trim();
    	
    	//Remove VEVO and fix spacing (based on capitalization, e.g TaylorSwiftVEVO -> Taylor Swift)                	
    	if(input.contains("VEVO")) {
    		input = input.replace("VEVO", "");
    		input = Character.toUpperCase(input.charAt(0)) + input.substring(1).replaceAll("(?<!_)(?=[A-Z])", " ");
    	}
    	
    	return input;
    }
    
    private static void logInfo(Object log) {
    	System.out.println(log.toString());
    }
    
    private static void logWarn(Object log) {
    	System.err.println(log.toString());
    }
    
}