package nl.thedutchmc.ytmusicreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class YoutubeMusic {

	private static JSONTokener parser;
	private static int MAX_OUTPUT = 10;
	
	public static void main(String[] args) {
		System.out.println("YTmusicReader by TheDutchMC/Dutchy76\n");
		
    	if(args.length == 0) {
    		System.err.println("No file path provided! Exiting");
    		System.out.println("Usage: java (arguments) - jar YTmusicReader.jar <file path> [year] [max output]");
    		System.exit(1);
    	}
    	
    	String year;
    	String nextYear;
    	
    	if(args.length > 1) {
    		System.out.println("Using year " + args[1]);
    		year = args[1];
    		nextYear = String.valueOf(Integer.valueOf(year) + 1);
    	} else {
    		System.out.println("No year provided. Using current year\n");
    		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
    		year = formatter.format(new Date());
    		nextYear = String.valueOf(Integer.valueOf(year) + 1);
    	}
    	
    	if(args.length > 2) {
    		System.out.println("Setting output to " + args[2]);
    		MAX_OUTPUT = Integer.valueOf(args[2]);
    	}
    	
    	File file = new File(args[0]);
    	    	
        try {
			parser = new JSONTokener(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("Invalid format! You sure this is a JSON file? Exiting");
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.err.println("Can't find the file! Exiting");
			System.exit(1);
		}
        
        System.out.println("Reading...");
        
        List<String> artists = new ArrayList<String>();
        List<String> songs = new ArrayList<String>();

        JSONArray json = new JSONArray();
        
        try {
            json = (JSONArray) parser.nextValue();

            for (Object o : json) {
            	
            	JSONObject jsonObj = (JSONObject) o;

            	//Check if the header is YTMusic, since the export also contains regular YouTube.
            	if(!jsonObj.get("header").equals("YouTube Music")) continue;
            	
                String title = (String) jsonObj.get("title");

                title = title.substring(8);
                
                String rawDate = (String) jsonObj.get("time");
                Date time = getDateFromString(rawDate);

                //Youtube music has the artist in a nested array called subtitles, for some reason
                JSONArray nestedJson = jsonObj.getJSONArray("subtitles");
                
                String artist = "";
                for(Object obj : nestedJson) {
                	JSONObject jsonNestedObj = (JSONObject) obj;
                	
                	artist = (String) jsonNestedObj.get("name");
                	artist = artist.split("-")[0];
                	
                	char[] ca = artist.toCharArray();
                	
                	//Remove trailing spaces
                	artist = "";
                	for(int i = 0; i < ca.length; i++) {
                		char c = ca[i];
                		
                		if(i == (ca.length -1) && c == ' ') continue;
                			
                		artist += c;
                	}
                	
                	//Remove VEVO and fix spacing (based on capitalization, e.g TaylorSwiftVEVO -> Taylor Swift)                	
                	if(artist.contains("VEVO")) {
                		artist = artist.replace("VEVO", "");
                		artist = Character.toUpperCase(artist.charAt(0)) +
                				artist.substring(1).replaceAll("(?<!_)(?=[A-Z])", " ");
                	}
                }

                //Only add it if the time is within the range
                if (time.after(getDateFromString(year + "-01-01T12:00:00")) && time.before(getDateFromString(nextYear + "-01-01T12:00:00"))) {
                	artists.add(artist);
                    songs.add(title);
                }
            }
        } catch (Exception ex) {
        }
        
        System.out.println("Done.\n");
        
        System.out.println(">------- Top Artists -------<");
        int index = 1;
        for (Entry<Object, Long> s : getSortedList(artists)) {
            System.out.println(index + ". " + (String) s.getKey());
            index++;
        }

        index = 1;
        System.out.println("\n>------- Top Songs -------<");
        for (Entry<Object, Long> s : getSortedList(songs)) {
            System.out.println(index + ". " + (String) s.getKey());
            index++;
        }
	}
	
    static Date getDateFromString(String rawData) throws ParseException {

        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return inFormat.parse(rawData);
    }
    
    static List<Entry<Object, Long>> getSortedList(List<?> list) {
        Map<Object, Long> map = list.stream().collect(Collectors.groupingBy(w -> w, Collectors.counting()));
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(MAX_OUTPUT).collect(Collectors.toList());
    }
}