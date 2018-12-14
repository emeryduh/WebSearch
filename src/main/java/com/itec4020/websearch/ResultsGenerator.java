package com.itec4020.websearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ResultsGenerator {
	
	/**
	 * Creates an output file based on the search results provided.  The output
	 * file will be placed in the {ROOT_PROJECT_DIR}/results/ folder in a file called
	 * output.txt
	 * 
	 * @param map The search results where <key, value> is <topicNum, searchResultJSON>
	 */
	public static void generateOutputFile(Map<String, String> map) {
		// Establish the results output path
		String outputPath = System.getProperty("user.dir") + "//results";
		
		// Create the output directories if they don't exist
		new File(outputPath).mkdirs();
		
		try {
			// Setup the writer to output to the file
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "//output.txt"));
			
			// Get the sorted keys for this map to be used when writing the output
			SortedSet<String> keys = new TreeSet<String>(map.keySet());
			
			// Iterate through them and output to the file
			for(Object key : keys) {
				String value = map.get(key.toString());
				String output = getOutputForTopic(key.toString(), value);
				writer.write(output);
			}
			
			// Close the writer when done
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates an output string for the topic's search results.  It is worth
	 * noting that because the output file is in plain text that we are limited
	 * on creating a uniform output file between various platforms.  If this
	 * program is run on a Windows machine it will generate a text file that
	 * will only properly display on other Windows machines.  This is due to
	 * the differences in how various OS platforms interpret new lines.  For
	 * example, 0Dh 0Ah (CRLF) is for DOS (windows), 0Dh (CR) is for older Macs,
	 * and 0Ah (LF) is for Unix/Linux OS.
	 * 
	 * @param topicNum The topic number referenced in the topics file
	 * @param data The JSON search data
	 * @return The top 5 search results in the format specified in the assignment requirements
	 */
	private static String getOutputForTopic(String topicNum, String data) {
		String groupId = "g8";
		String output = "";
		
		// Used to parse in the JSON data for the topic
		JSONParser parser = new JSONParser();
		try {
			// Parse the topic JSON into a JSONObject
			JSONObject jsonObj = (JSONObject) parser.parse(data);
			
			// Get the top-most (outer) hits object
			jsonObj = (JSONObject) jsonObj.get("hits");
			
			// Find the inner "hits" object to begin iterating over the
		    // search results
	    	JSONArray results = (JSONArray) jsonObj.get("hits");
	    	
	    	// Limit the output results to max 5, or the size of
	    	// the ArrayList, whichever is smaller
	    	for(int i = 0; i < results.size() && i < 5; i++) {
	    		// Get each search result
	    		JSONObject result = (JSONObject) results.get(i);
	    		
	    		// Get the data we want out of each result
	    		JSONObject source = (JSONObject) result.get("_source");
	    		String docno = source.get("docno").toString();
	    		String score = result.get("_score").toString();
	    		int rank = i + 1;
	    		
	    		// Build each result to an string and add it
	    		// to the output
				output += topicNum + " Q0 " + docno + " " + rank 
						+ " " + score + " " + groupId 
						+ System.getProperty("line.separator");
	    	}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return output;
	}
}
