package com.itec4020.websearch;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

/**
 * Assignment 3 for ITEC 4020 WebSearch
 * 
 * Group: 8
 * 
 * This assignment can be decomposed into the following tasks: - Extracting data
 * from the .GZ compressed form - Breaking down documents into indexable parts -
 * Indexing all documents - Searching on the indexed documents - Generating
 * results from the searches to be stored
 */
@SuppressWarnings("restriction")
public class App {
	
	// Port for WebServer
	final int PORT = 8080;

	public static void main(String[] args) {
		System.out.println("Starting up");

		new App();
	}
	
	public App() {
		ElasticSearchHandler searchHandler = new ElasticSearchHandler();
		
		try {
			// Decompresses all documents into .txt files
			//searchHandler.decompress();

			// Breaks down each .txt file into a separate document and indexes them
			//searchHandler.index();

			// Run an example search against the ElasticSearch database
			// searchHandler.search("foreign minorities, Germany", "What language and cultural differences impede the integration" +
			//" of foreign minorities in Germany?");
			
			// Start up the Web Server
			HttpServer server = HttpServer.create(new InetSocketAddress(8080), 10);
		    server.createContext("/", new WebServerHandler(searchHandler));
		    server.setExecutor(null);
		    server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This function will be used to generate the .txt file for the assignment submission.
	 * It will execute the 20 topic queries as they come up in the topics file.
	 */
	public void outputSearch() {
		
	}
}
