/**
 * HTTP Library for sending HTTP requests
 * @author Tobias de Bruijn
 */

package nl.thedutchmc.ytmusicreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Http {

	/**
	 * Send a HTTP Request
	 * @param method Method to use (e.g POST)
	 * @param targetUrl URL to send the request to (e.g https://example.com)
	 * @param params URL Parameters to be used
	 * @return Returns a ResponseObject
	 * @throws MalformedURLException Thrown when an invalid URL is given
	 * @throws IOException Thrown when an IOException occurs
	 */
	public static ResponseObject request(RequestMethod method, String targetUrl, HashMap<String, String> params) throws MalformedURLException, IOException {
		//Turn the HashMap of parameters into a String
		final String sParams = hashMapToString(params);
		
		//Determine the request method
		String sMethod = "";
		switch(method) {
		case GET: 
			sMethod = "GET"; 
			break;
		case POST: 
			sMethod = "POST"; 
			break;
		}
		
		//Create the URL, open a connection and connect.
    	final URL url = new URL(targetUrl + sParams);
    	final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	conn.setRequestMethod(sMethod);
    	conn.connect();
    	                	
    	//Get the response message from the server
        final InputStream is = conn.getInputStream();
        final String result = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
        
        Http http = new Http();
		return http.new ResponseObject(result, conn.getResponseCode());
	}
	
	private static String hashMapToString(HashMap<String, String> input) {
		final StringBuilder result = new StringBuilder();
		int index = 1;
		
		for(Map.Entry<String, String> entry : input.entrySet()) {
			
			//If we're on the first iteration,
			//add the '?' character
			if(index == 1) {
				result.append("?");
			}
			
			//Add the key, a '=' and the value.
			result.append(entry.getKey());
			result.append("=");
			result.append(entry.getValue());
			
			//Check if we're not yet on the last iteration
			//If not, add the '&' character.
			if(index != input.size()) {
				result.append("&");
			}
			
			index++;
		}
		
		return result.toString();
	}
	
	public enum RequestMethod {
		GET,
		POST, 
	}
	
	public class ResponseObject {
		
		private String message;
		private int responseCode;
		
		public ResponseObject(String message, int responseCode) {
			this.message = message;
			this.responseCode = responseCode;
		}
		
		public String getMessage() {
			return this.message;
		}
		
		public int getResponseCode() {
			return this.responseCode;
		}
	}
}