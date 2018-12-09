package com.itec4020.websearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

/**
 * Assignment 3 for ITEC 4020
 * WebSearch
 * 
 * Group: 8
 * 
 * This assignment can be decomposed into the following tasks:
 * - Extracting data from the .GZ compressed form
 * - Breaking down documents into indexable parts
 * - Indexing all documents
 * - Searching on the indexed documents
 * - Generating results from the searches to be stored
 */
public class App {
	
	RestClient rest;
	
    public static void main( String[] args ) {
        System.out.println( "Starting up " );
        
        new App();
    }
    
	public App() {
    	openREST();
    	try {
			extractDocuments();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Open the REST client connection when booting up
	 */
	public void openREST() {
		// Initialize the REST client to communicate with the ElasticSearch server
    	rest = RestClient.builder(
    			new HttpHost("localhost", 9200, "http"),
    			new HttpHost("localhost", 9201, "http")).build();
	}
	
	/**
	 * Close the REST client connection when no longer needed
	 */
	public void closeREST() {
		try {
			rest.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Extract document data from compressed files
	 */
	public void extractDocuments() {
		decompressDirectory("WT01");
		decompressDirectory("WT02");
		decompressDirectory("WT03");
	}
	
	/**
	 * Decompress all .GZ files in the given folder.  Will assume that the folder name
	 * provided resides in %PROJECT_PATH%/data/{folder}.  The output will be stored in
	 * %PROJECT_PATH%/extracted/{folder}.  If the extracted folder does not exist it 
	 * will be created.
	 * 
	 * @param folder The folder name to pull data from and to extract data to.
	 */
	public void decompressDirectory(String folder) {
		// Establish paths to the given folders
		String filePath = System.getProperty("user.dir") + "\\data\\" + folder;
		String outputPath = System.getProperty("user.dir") + "\\extracted\\" + folder;
		
		// Create the output directories if they don't exist
		new File(outputPath).mkdirs();
		
		// Gather all .GZ files in the directory
		File[] files = getFilesByExt(filePath, ".GZ");
		
		// Buffer used to read in data from file
		byte[] buffer = new byte[1024];
		
		try {
			for(int i = 0; i < files.length; i++) {
				// Setup input streams to extract data from the file
				FileInputStream fis = new FileInputStream(files[i]);
				GZIPInputStream gis = new GZIPInputStream(fis);
				
				// The file name for the current File
				String fileName = files[i].getName();
				String outputName = fileName.substring(0, fileName.indexOf(".GZ")) + ".txt";
				
				// Setup output stream to push data into
				FileOutputStream fos = new FileOutputStream(outputPath + "\\" + outputName);
				
				// Stores data to be written
				int bytes_read;
				
				// Write data until the EOF has been reached
				while((bytes_read = gis.read(buffer)) > 0) {
					fos.write(buffer, 0, bytes_read);
				}
				
				// Close streams when finished
				gis.close();
				fos.close();
			}
			
			System.out.println("Decompression complete for path:  " + filePath);
		} catch(Exception e) {
			System.out.println(e);
		}
	}
	
	/**
	 * Returns a File array of all files that match a given extension filter.
	 * 
	 * @param directory The directory to search in
	 * @param extension The extension to search by
	 * @return All files that match the extension filter
	 */
	public File[] getFilesByExt(String directory, String extension) {
		// Make the search case insensitive
		final String ext = extension.toLowerCase();
		File fileDir = new File(directory);
		
		// Returns a list of all files that match the extension
		return fileDir.listFiles(new FilenameFilter() {
			// Specify the acceptance filter
			public boolean accept(File fileDir, String fileName) {
				return fileName.toLowerCase().endsWith(ext);
			}
		});
	}
	
}
